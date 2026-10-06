package net.glowcube.realms.entity.boss;

import java.util.ArrayList;
import java.util.List;
import net.glowcube.realms.item.Abilities;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.entity.projectile.EvokerFangs;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** The Hollow King - an undead ruler sleeping in the Hollow Crypt of the Umbral Depths. */
public class HollowKing extends RealmBoss {
	public static final int SPEAR = 1, SOULFIRE = 2, SUMMON = 3, CURSE = 4, FANGS = 5;
	private final List<Vec3> spots = new ArrayList<>();
	private Vec3 dashDir = Vec3.ZERO;

	public HollowKing(EntityType<? extends HollowKing> type, Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
				.add(Attributes.MAX_HEALTH, 300.0)
				.add(Attributes.MOVEMENT_SPEED, 0.3)
				.add(Attributes.ATTACK_DAMAGE, 10.0)
				.add(Attributes.ARMOR, 10.0)
				.add(Attributes.KNOCKBACK_RESISTANCE, 0.9)
				.add(Attributes.FOLLOW_RANGE, 40.0);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
		this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.1, true) {
			@Override
			public boolean canUse() {
				return HollowKing.this.getCurrentAttack() == 0 && super.canUse();
			}

			@Override
			public boolean canContinueToUse() {
				return HollowKing.this.getCurrentAttack() == 0 && super.canContinueToUse();
			}
		});
		this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 24.0F));
		this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
		this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, false));
	}

	@Override
	protected String bossId() {
		return "hollow_king";
	}

	@Override
	protected BossEvent.BossBarColor barColor(int phase) {
		return phase == 1 ? BossEvent.BossBarColor.GREEN : phase == 2 ? BossEvent.BossBarColor.BLUE : BossEvent.BossBarColor.RED;
	}

	@Override
	protected int chooseAttack(int phase, LivingEntity target) {
		int roll = this.random.nextInt(100);
		if (phase >= 2 && roll < 18) return CURSE;
		if (roll < 34) return SUMMON;
		if (this.distanceTo(target) > 7 && roll < 60) return SPEAR;
		if (roll < 80) return FANGS;
		return SOULFIRE;
	}

	@Override
	protected boolean tickAttack(ServerLevel level, int attack, int tick, LivingEntity target) {
		int phase = this.getPhase();
		this.getNavigation().stop();
		switch (attack) {
			case SPEAR -> {
				if (tick < 12) {
					this.getLookControl().setLookAt(target, 30, 30);
					Vec3 d = target.position().subtract(this.position());
					this.dashDir = new Vec3(d.x, 0, d.z).normalize();
					level.sendParticles(ParticleTypes.SOUL, this.getX(), this.getY() + 0.3, this.getZ(), 4, 0.4, 0.1, 0.4, 0.02);
					return false;
				}
				this.setDeltaMovement(this.dashDir.x * 1.2, this.getDeltaMovement().y, this.dashDir.z * 1.2);
				level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, this.getX(), this.getY(0.5), this.getZ(), 6, 0.4, 0.5, 0.4, 0.02);
				for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(1.0), e -> e instanceof Player)) {
					e.hurtServer(level, this.damageSources().mobAttack(this), 8.0F + phase);
					e.addEffect(new MobEffectInstance(MobEffects.WITHER, 60, 0));
					Abilities.push(e, this.position(), 1.4, 0.4);
				}
				return tick > 26 || (this.horizontalCollision && tick > 14);
			}
			case SOULFIRE -> {
				if (tick == 0) {
					this.spots.clear();
					for (int i = 0; i < 10; i++) {
						double a = i * Math.PI / 5;
						this.spots.add(Glowkeeper.groundBelow(level, this.position().add(Math.cos(a) * 5, 1, Math.sin(a) * 5)));
					}
				}
				for (Vec3 s : this.spots) level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, s.x, s.y + 0.2 + (tick % 10) * 0.2, s.z, 2, 0.1, 0.1, 0.1, 0.01);
				if (tick % 10 == 0) for (Vec3 s : this.spots) this.areaDamage(level, s, 1.4, 4.0F + phase, 0.2, null);
				return tick >= 40;
			}
			case SUMMON -> {
				if (tick == 8) {
					if (phase >= 2) this.summonMinions(level, EntityTypes.WITHER_SKELETON, 2, 4, target, 4);
					else this.summonMinions(level, EntityTypes.SKELETON, 3, 5, target, 4);
					level.playSound(null, this.blockPosition(), SoundEvents.EVOKER_PREPARE_SUMMON, SoundSource.HOSTILE, 2.0F, 0.5F);
				}
				return tick >= 16;
			}
			case CURSE -> {
				if (tick == 10) {
					for (Player p : this.fighters(level, 24)) {
						p.addEffect(new MobEffectInstance(MobEffects.WITHER, 80, 0));
						p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, 0));
						p.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 0));
						level.sendParticles(ParticleTypes.SCULK_SOUL, p.getX(), p.getY(1), p.getZ(), 20, 0.4, 0.6, 0.4, 0.02);
					}
					level.playSound(null, this.blockPosition(), SoundEvents.WITHER_AMBIENT, SoundSource.HOSTILE, 2.0F, 0.6F);
				}
				return tick >= 16;
			}
			case FANGS -> {
				if (tick == 8) {
					Vec3 d = target.position().subtract(this.position());
					double base = Math.atan2(d.z, d.x);
					int lines = phase == 3 ? 3 : 1;
					for (int l = 0; l < lines; l++) {
						double ang = base + (l - (lines - 1) / 2.0) * 0.4;
						for (int i = 1; i <= 12; i++) {
							double x = this.getX() + Math.cos(ang) * i * 1.25, z = this.getZ() + Math.sin(ang) * i * 1.25;
							BlockPos ground = BlockPos.containing(Glowkeeper.groundBelow(level, new Vec3(x, this.getY() + 2, z)));
							level.addFreshEntity(new EvokerFangs(level, x, ground.getY(), z, (float) ang, i, this));
						}
					}
					level.playSound(null, this.blockPosition(), SoundEvents.EVOKER_PREPARE_ATTACK, SoundSource.HOSTILE, 2.0F, 0.6F);
				}
				return tick >= 20;
			}
			default -> {
				return true;
			}
		}
	}

	@Override
	protected void onPhaseChange(ServerLevel level, int newPhase) {
		if (newPhase == 3) this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.36);
	}

	@Override
	public boolean doHurtTarget(ServerLevel level, Entity target) {
		boolean hit = super.doHurtTarget(level, target);
		if (hit && target instanceof LivingEntity living) living.addEffect(new MobEffectInstance(MobEffects.WITHER, 40, 0));
		return hit;
	}

	@Override
	public void aiStep() {
		super.aiStep();
		if (this.level().isClientSide() && this.random.nextInt(3) == 0) {
			this.level().addParticle(ParticleTypes.SOUL, this.getRandomX(0.6), this.getRandomY(), this.getRandomZ(0.6), 0, 0.02, 0);
		}
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.WITHER_SKELETON_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.WITHER_SKELETON_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.WITHER_DEATH;
	}

	@Override
	public float getVoicePitch() {
		return 0.6F;
	}
}
