package net.glowcube.realms.entity.boss;

import net.glowcube.realms.item.Abilities;
import net.glowcube.realms.registry.ModEntities;
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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The Echo Warden - a sculk-grown warden guarding the key to the Sculk Realm. Weaker than the realm bosses,
 * but with sonic attacks. Attacks: sonic boom, shriek, sculk ripple, crushing leap, tendril pull.
 */
public class EchoWarden extends RealmBoss {
	public static final int SONIC = 1, SHRIEK = 2, RIPPLE = 3, LEAP = 4, TENDRILS = 5;
	private boolean airborne;

	public EchoWarden(EntityType<? extends EchoWarden> type, Level level) {
		super(type, level);
		this.xpReward = 150;
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
				.add(Attributes.MAX_HEALTH, 200.0)
				.add(Attributes.MOVEMENT_SPEED, 0.28)
				.add(Attributes.ATTACK_DAMAGE, 10.0)
				.add(Attributes.ATTACK_KNOCKBACK, 1.0)
				.add(Attributes.ARMOR, 8.0)
				.add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
				.add(Attributes.FOLLOW_RANGE, 36.0);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
		this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.1, true) {
			@Override
			public boolean canUse() {
				return EchoWarden.this.getCurrentAttack() == 0 && super.canUse();
			}

			@Override
			public boolean canContinueToUse() {
				return EchoWarden.this.getCurrentAttack() == 0 && super.canContinueToUse();
			}
		});
		this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 24.0F));
		this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
		this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, false));
	}

	@Override
	protected String bossId() {
		return "echo_warden";
	}

	@Override
	protected BossEvent.BossBarColor barColor(int phase) {
		return phase == 1 ? BossEvent.BossBarColor.BLUE : phase == 2 ? BossEvent.BossBarColor.GREEN : BossEvent.BossBarColor.WHITE;
	}

	@Override
	protected int chooseAttack(int phase, LivingEntity target) {
		int roll = this.random.nextInt(100);
		if (phase == 3 && roll < 22) return TENDRILS;
		if (phase >= 2 && roll < 36) return SHRIEK;
		if (this.distanceTo(target) > 6 && roll < 62) return this.random.nextBoolean() ? SONIC : LEAP;
		if (roll < 82) return RIPPLE;
		return SONIC;
	}

	@Override
	protected boolean tickAttack(ServerLevel level, int attack, int tick, LivingEntity target) {
		int phase = this.getPhase();
		this.getNavigation().stop();
		switch (attack) {
			case SONIC -> {
				this.getLookControl().setLookAt(target, 30, 30);
				if (tick == 0) level.playSound(null, this.blockPosition(), SoundEvents.WARDEN_SONIC_CHARGE, SoundSource.HOSTILE, 3.0F, 1.0F);
				if (tick == 34) {
					Vec3 from = this.position().add(0, 1.6, 0);
					Vec3 dir = target.getEyePosition().subtract(from).normalize();
					for (int i = 1; i <= 20; i++) {
						Vec3 p = from.add(dir.scale(i));
						level.sendParticles(ParticleTypes.SONIC_BOOM, p.x, p.y, p.z, 1, 0, 0, 0, 0);
						for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(p, p).inflate(1.0), e -> e instanceof Player)) {
							e.hurtServer(level, this.damageSources().sonicBoom(this), 8.0F + phase);
							Abilities.push(e, this.position(), 1.4, 0.4);
						}
					}
					level.playSound(null, this.blockPosition(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 3.0F, 1.0F);
				}
				return tick >= 40;
			}
			case SHRIEK -> {
				if (tick == 6) {
					for (Player p : this.fighters(level, 24)) p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 160, 0));
					this.summonMinions(level, ModEntities.SCULK_STALKER, 2, 4, target, 4);
					Abilities.ring(level, ParticleTypes.SCULK_SOUL, this.position().add(0, 1, 0), 3, 30);
					level.playSound(null, this.blockPosition(), SoundEvents.SCULK_SHRIEKER_SHRIEK, SoundSource.HOSTILE, 3.0F, 0.7F);
				}
				return tick >= 16;
			}
			case RIPPLE -> {
				if (tick < 10) return false;
				double r = (tick - 9) * 1.2;
				Abilities.ring(level, ParticleTypes.SCULK_CHARGE_POP, this.position().add(0, 0.2, 0), r, (int) (10 + r * 8));
				for (Player p : this.fighters(level, r + 1)) {
					if (Math.abs(p.distanceTo(this) - r) < 1.1 && p.onGround()) {
						p.hurtServer(level, this.damageSources().mobAttack(this), 6.0F + phase);
						p.push(0, 0.5, 0);
						p.needsSync = true;
					}
				}
				if (tick == 10) level.playSound(null, this.blockPosition(), SoundEvents.SCULK_CATALYST_BLOOM, SoundSource.HOSTILE, 3.0F, 0.6F);
				return tick >= 20;
			}
			case LEAP -> {
				if (tick < 10) {
					this.getLookControl().setLookAt(target, 30, 30);
					return false;
				}
				if (tick == 10) {
					Vec3 d = target.position().subtract(this.position());
					Vec3 h = new Vec3(d.x, 0, d.z);
					h = h.lengthSqr() > 0.01 ? h.normalize().scale(Math.min(h.length(), 14) * 0.11) : Vec3.ZERO;
					this.setDeltaMovement(h.x, 1.0, h.z);
					this.airborne = true;
					return false;
				}
				if (this.airborne && tick > 14 && this.onGround()) {
					this.airborne = false;
					for (int r = 1; r <= 6; r++) Abilities.ring(level, ParticleTypes.SCULK_SOUL, this.position().add(0, 0.2, 0), r, 10 + r * 4);
					this.areaDamage(level, this.position(), 6.0, 8.0F + phase, 1.0, new MobEffectInstance(MobEffects.SLOWNESS, 60, 1));
					level.playSound(null, this.blockPosition(), SoundEvents.WARDEN_ATTACK_IMPACT, SoundSource.HOSTILE, 3.0F, 0.6F);
					return true;
				}
				return tick > 60;
			}
			case TENDRILS -> {
				for (Player p : this.fighters(level, 14)) {
					Vec3 pull = this.position().subtract(p.position()).normalize().scale(0.15);
					p.push(pull.x, 0.0, pull.z);
					p.needsSync = true;
					if (tick % 10 == 0) Abilities.line(level, ParticleTypes.SCULK_SOUL, this.position().add(0, 2, 0), p.position().add(0, 1, 0), 10);
				}
				if (tick == 30) this.areaDamage(level, this.position(), 3.5, 9.0F, 1.4, null);
				return tick >= 34;
			}
			default -> {
				return true;
			}
		}
	}

	@Override
	protected void onPhaseChange(ServerLevel level, int newPhase) {
		for (Player p : this.fighters(level, 24)) p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 60, 0));
	}

	@Override
	public boolean doHurtTarget(ServerLevel level, Entity target) {
		boolean hit = super.doHurtTarget(level, target);
		if (hit) level.sendParticles(ParticleTypes.SCULK_SOUL, target.getX(), target.getY(1), target.getZ(), 10, 0.3, 0.3, 0.3, 0.02);
		return hit;
	}

	@Override
	public void aiStep() {
		super.aiStep();
		if (this.level().isClientSide() && this.random.nextInt(2) == 0) {
			this.level().addParticle(ParticleTypes.SCULK_CHARGE_POP, this.getRandomX(0.6), this.getRandomY(), this.getRandomZ(0.6), 0, 0.02, 0);
		}
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.WARDEN_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.WARDEN_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.WARDEN_DEATH;
	}
}
