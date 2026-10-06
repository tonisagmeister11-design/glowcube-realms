package net.glowcube.realms.entity.boss;

import net.glowcube.realms.entity.projectile.GlowShardProjectile;
import net.glowcube.realms.item.Abilities;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** The Frost Lich - an ice sorcerer ruling the Frozen Crypt in snowy lands. */
public class FrostLich extends RealmBoss {
	public static final int VOLLEY = 1, BLIZZARD = 2, PRISON = 3, SUMMON = 4, NOVA = 5;

	public FrostLich(EntityType<? extends FrostLich> type, Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
				.add(Attributes.MAX_HEALTH, 280.0)
				.add(Attributes.MOVEMENT_SPEED, 0.28)
				.add(Attributes.ATTACK_DAMAGE, 8.0)
				.add(Attributes.ARMOR, 8.0)
				.add(Attributes.KNOCKBACK_RESISTANCE, 0.8)
				.add(Attributes.FOLLOW_RANGE, 40.0);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
		this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, true) {
			@Override
			public boolean canUse() {
				return FrostLich.this.getCurrentAttack() == 0 && super.canUse();
			}

			@Override
			public boolean canContinueToUse() {
				return FrostLich.this.getCurrentAttack() == 0 && super.canContinueToUse();
			}
		});
		this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 24.0F));
		this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
		this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, false));
	}

	@Override
	protected String bossId() {
		return "frost_lich";
	}

	@Override
	protected BossEvent.BossBarColor barColor(int phase) {
		return phase == 1 ? BossEvent.BossBarColor.BLUE : phase == 2 ? BossEvent.BossBarColor.WHITE : BossEvent.BossBarColor.PURPLE;
	}

	@Override
	protected int chooseAttack(int phase, LivingEntity target) {
		int roll = this.random.nextInt(100);
		if (phase == 3 && roll < 22) return NOVA;
		if (phase >= 2 && roll < 36) return SUMMON;
		if (roll < 55) return VOLLEY;
		if (roll < 78) return BLIZZARD;
		return PRISON;
	}

	@Override
	protected boolean tickAttack(ServerLevel level, int attack, int tick, LivingEntity target) {
		int phase = this.getPhase();
		this.getNavigation().stop();
		this.getLookControl().setLookAt(target, 30, 30);
		switch (attack) {
			case VOLLEY -> {
				if (tick % 5 == 0 && tick < 25) {
					for (int i = -1; i <= 1; i++) {
						GlowShardProjectile shard = GlowShardProjectile.frostShard(level, this);
						shard.setPos(this.getX(), this.getEyeY(), this.getZ());
						Vec3 dir = target.getEyePosition().subtract(shard.position()).normalize().yRot((float) Math.toRadians(i * 10));
						shard.shoot(dir.x, dir.y, dir.z, 1.3F, 2.0F);
						shard.setDamage(5.0F + phase);
						level.addFreshEntity(shard);
					}
					level.playSound(null, this.blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 1.5F, 1.4F);
				}
				return tick >= 28;
			}
			case BLIZZARD -> {
				level.sendParticles(ParticleTypes.SNOWFLAKE, this.getX(), this.getY() + 4, this.getZ(), 40, 8, 2, 8, 0.05);
				if (tick % 10 == 0) {
					for (Player p : this.fighters(level, 10)) {
						p.hurtServer(level, this.damageSources().freeze(), 2.0F + phase * 0.5F);
						p.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 40, 1));
						p.setTicksFrozen(Math.min(p.getTicksRequiredToFreeze(), p.getTicksFrozen() + 30));
					}
				}
				if (tick == 0) level.playSound(null, this.blockPosition(), SoundEvents.POWDER_SNOW_STEP, SoundSource.HOSTILE, 3.0F, 0.5F);
				return tick >= 60;
			}
			case PRISON -> {
				if (tick < 20) {
					Abilities.ring(level, ParticleTypes.SNOWFLAKE, target.position().add(0, 0.2, 0), 1.6, 16);
				} else if (tick == 20) {
					if (target.distanceTo(this) < 32) {
						target.hurtServer(level, this.damageSources().indirectMagic(this, this), 6.0F + phase);
						target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 5));
						target.setTicksFrozen(target.getTicksRequiredToFreeze() + 60);
						level.sendParticles(ParticleTypes.ITEM_SNOWBALL, target.getX(), target.getY(1), target.getZ(), 40, 0.4, 0.8, 0.4, 0.1);
					}
					level.playSound(null, target.blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 2.0F, 0.6F);
				}
				return tick >= 24;
			}
			case SUMMON -> {
				if (tick == 8) {
					this.summonMinions(level, EntityTypes.STRAY, 3, 5, target, 4);
					level.playSound(null, this.blockPosition(), SoundEvents.EVOKER_PREPARE_SUMMON, SoundSource.HOSTILE, 2.0F, 1.2F);
				}
				return tick >= 16;
			}
			case NOVA -> {
				if (tick < 16) {
					level.sendParticles(ParticleTypes.SNOWFLAKE, this.getX(), this.getY(1), this.getZ(), 10, 1, 1, 1, 0.02);
					return false;
				}
				double r = (tick - 15) * 1.4;
				Abilities.ring(level, ParticleTypes.SNOWFLAKE, this.position().add(0, 0.3, 0), r, (int) (10 + r * 8));
				for (Player p : this.fighters(level, r + 1)) {
					double d = p.distanceTo(this);
					if (Math.abs(d - r) < 1.2) {
						p.hurtServer(level, this.damageSources().freeze(), 7.0F);
						p.setTicksFrozen(p.getTicksRequiredToFreeze() + 40);
						Abilities.push(p, this.position(), 1.0, 0.4);
					}
				}
				return tick >= 26;
			}
			default -> {
				return true;
			}
		}
	}

	@Override
	protected void onPhaseChange(ServerLevel level, int newPhase) {
		this.areaDamage(level, this.position(), 6.0, 4.0F, 1.2, new MobEffectInstance(MobEffects.SLOWNESS, 60, 2));
	}

	@Override
	public boolean doHurtTarget(ServerLevel level, Entity target) {
		boolean hit = super.doHurtTarget(level, target);
		if (hit && target instanceof LivingEntity living) living.setTicksFrozen(living.getTicksRequiredToFreeze() + 20);
		return hit;
	}

	@Override
	public void aiStep() {
		super.aiStep();
		if (this.level().isClientSide()) {
			this.level().addParticle(ParticleTypes.SNOWFLAKE, this.getRandomX(0.6), this.getRandomY(), this.getRandomZ(0.6), 0, -0.02, 0);
		}
	}

	@Override
	public boolean canFreeze() {
		return false;
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.STRAY_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.STRAY_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.STRAY_DEATH;
	}

	@Override
	public float getVoicePitch() {
		return 0.6F;
	}
}
