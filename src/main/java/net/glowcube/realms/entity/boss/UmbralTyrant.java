package net.glowcube.realms.entity.boss;

import net.glowcube.realms.entity.ShadeCrawler;
import net.glowcube.realms.entity.projectile.GlowShardProjectile;
import net.glowcube.realms.item.Abilities;
import net.glowcube.realms.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
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
import net.minecraft.world.entity.projectile.EvokerFangs;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The Umbral Tyrant - ruler of the Umbral Depths. A colossal horned shadow beast.
 * Attacks: crushing leap, shadow charge, void orbs, fang eruption, crawler swarm, dark pulse.
 */
public class UmbralTyrant extends RealmBoss {
	public static final int LEAP = 1, CHARGE = 2, ORBS = 3, FANGS = 4, SUMMON = 5, PULSE = 6;
	private Vec3 chargeDir = Vec3.ZERO;
	private boolean leapAirborne;

	public UmbralTyrant(EntityType<? extends UmbralTyrant> type, Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
				.add(Attributes.MAX_HEALTH, 380.0)
				.add(Attributes.MOVEMENT_SPEED, 0.3)
				.add(Attributes.ATTACK_DAMAGE, 12.0)
				.add(Attributes.ATTACK_KNOCKBACK, 1.5)
				.add(Attributes.ARMOR, 10.0)
				.add(Attributes.ARMOR_TOUGHNESS, 6.0)
				.add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
				.add(Attributes.STEP_HEIGHT, 1.5)
				.add(Attributes.FOLLOW_RANGE, 48.0);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
		this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.1, true) {
			@Override
			public boolean canUse() {
				return UmbralTyrant.this.getCurrentAttack() == 0 && !UmbralTyrant.this.isRoaring() && super.canUse();
			}

			@Override
			public boolean canContinueToUse() {
				return UmbralTyrant.this.getCurrentAttack() == 0 && super.canContinueToUse();
			}
		});
		this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 24.0F));
		this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
		this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, false));
	}

	@Override
	protected String bossId() {
		return "umbral_tyrant";
	}

	@Override
	protected BossEvent.BossBarColor barColor(int phase) {
		return phase == 1 ? BossEvent.BossBarColor.PURPLE : phase == 2 ? BossEvent.BossBarColor.PINK : BossEvent.BossBarColor.RED;
	}

	@Override
	protected int chooseAttack(int phase, LivingEntity target) {
		double dist = this.distanceTo(target);
		int roll = this.random.nextInt(100);
		if (phase == 3 && roll < 18) return PULSE;
		if (phase >= 2 && roll < 32 && this.level().getEntitiesOfClass(ShadeCrawler.class, this.getBoundingBox().inflate(32)).size() < 4) return SUMMON;
		if (phase >= 2 && roll < 52) return FANGS;
		if (dist > 9 && roll < 75) return this.random.nextBoolean() ? LEAP : CHARGE;
		if (roll < 85) return ORBS;
		return dist > 5 ? CHARGE : LEAP;
	}

	@Override
	protected boolean tickAttack(ServerLevel level, int attack, int tick, LivingEntity target) {
		int phase = this.getPhase();
		switch (attack) {
			case LEAP -> {
				this.getNavigation().stop();
				if (tick < 12) {
					this.getLookControl().setLookAt(target, 30, 30);
					level.sendParticles(ParticleTypes.SQUID_INK, this.getX(), this.getY() + 0.2, this.getZ(), 6, 1.0, 0.1, 1.0, 0.02);
					if (tick == 0) level.playSound(null, this.blockPosition(), SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 2.5F, 0.6F);
					return false;
				}
				if (tick == 12) {
					Vec3 d = target.position().subtract(this.position());
					Vec3 h = new Vec3(d.x, 0, d.z);
					double len = Math.min(h.length(), 18.0);
					h = h.lengthSqr() > 0.01 ? h.normalize().scale(len * 0.11) : Vec3.ZERO;
					this.setDeltaMovement(h.x, 1.15, h.z);
					this.leapAirborne = true;
					return false;
				}
				if (this.leapAirborne && tick > 16 && this.onGround()) {
					this.leapAirborne = false;
					for (int r = 1; r <= 8; r++) Abilities.ring(level, ParticleTypes.LARGE_SMOKE, this.position().add(0, 0.2, 0), r, 10 + r * 5);
					level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, this.getX(), this.getY(), this.getZ(), 1, 0, 0, 0, 0);
					this.areaDamage(level, this.position(), 7.5, 10.0F + phase, 1.3, new MobEffectInstance(MobEffects.SLOWNESS, 80, 2));
					level.playSound(null, this.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 3.0F, 0.6F);
					return true;
				}
				return tick > 70;
			}
			case CHARGE -> {
				this.getNavigation().stop();
				if (tick < 18) {
					this.getLookControl().setLookAt(target, 30, 30);
					Vec3 d = target.position().subtract(this.position());
					this.chargeDir = new Vec3(d.x, 0, d.z).normalize();
					this.setYRot((float) (Mth.atan2(this.chargeDir.z, this.chargeDir.x) * (180F / Math.PI)) - 90.0F);
					this.yBodyRot = this.getYRot();
					level.sendParticles(ParticleTypes.SMOKE, this.getX(), this.getY() + 0.3, this.getZ(), 8, 0.6, 0.1, 0.6, 0.05);
					if (tick == 0) level.playSound(null, this.blockPosition(), SoundEvents.RAVAGER_STUNNED, SoundSource.HOSTILE, 2.0F, 0.5F);
					return false;
				}
				double speed = 0.9 + phase * 0.15;
				this.setDeltaMovement(this.chargeDir.x * speed, this.getDeltaMovement().y, this.chargeDir.z * speed);
				level.sendParticles(ParticleTypes.REVERSE_PORTAL, this.getX(), this.getY(0.5), this.getZ(), 10, 0.8, 0.8, 0.8, 0.05);
				for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(1.0), e -> e instanceof Player)) {
					e.hurtServer(level, this.damageSources().mobAttack(this), 10.0F + phase);
					Abilities.push(e, this.position(), 2.0, 0.7);
				}
				if (this.horizontalCollision && tick > 20) {
					level.playSound(null, this.blockPosition(), SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR, SoundSource.HOSTILE, 2.0F, 0.5F);
					this.roarTicks = 30;
					return true;
				}
				return tick > 40;
			}
			case ORBS -> {
				this.getNavigation().stop();
				int count = phase == 3 ? 6 : 3;
				if (tick % 6 == 0 && tick / 6 < count) {
					GlowShardProjectile orb = GlowShardProjectile.voidOrb(level, this);
					orb.setPos(this.getX(), this.getY(0.8), this.getZ());
					Vec3 dir = target.getEyePosition().subtract(orb.position()).normalize();
					orb.shoot(dir.x, dir.y + 0.15, dir.z, 0.7F, 6.0F);
					orb.setDamage(8.0F + phase);
					orb.setSeeking(true);
					level.addFreshEntity(orb);
					level.playSound(null, this.blockPosition(), SoundEvents.ENDER_EYE_LAUNCH, SoundSource.HOSTILE, 2.0F, 0.5F);
				}
				return tick >= count * 6 + 4;
			}
			case FANGS -> {
				this.getNavigation().stop();
				if (tick == 6) {
					int lines = phase == 3 ? 5 : 3;
					Vec3 d = target.position().subtract(this.position());
					double base = Math.atan2(d.z, d.x);
					for (int l = 0; l < lines; l++) {
						double ang = base + (l - (lines - 1) / 2.0) * 0.35;
						for (int i = 1; i <= 14; i++) {
							double x = this.getX() + Math.cos(ang) * i * 1.25, z = this.getZ() + Math.sin(ang) * i * 1.25;
							BlockPos ground = BlockPos.containing(Glowkeeper.groundBelow(level, new Vec3(x, this.getY() + 2, z)));
							level.addFreshEntity(new EvokerFangs(level, x, ground.getY(), z, (float) ang, i, this));
						}
					}
					if (phase == 3) {
						for (int i = 0; i < 12; i++) {
							double a = i * Math.PI / 6;
							level.addFreshEntity(new EvokerFangs(level, this.getX() + Math.cos(a) * 3, this.getY(), this.getZ() + Math.sin(a) * 3, (float) a, 3, this));
						}
					}
					level.playSound(null, this.blockPosition(), SoundEvents.EVOKER_PREPARE_ATTACK, SoundSource.HOSTILE, 2.0F, 0.6F);
				}
				return tick >= 20;
			}
			case SUMMON -> {
				this.getNavigation().stop();
				if (tick == 10) {
					for (int i = 0; i < 3; i++) {
						ShadeCrawler crawler = ModEntities.SHADE_CRAWLER.create(level, EntitySpawnReason.MOB_SUMMONED);
						if (crawler == null) continue;
						double a = this.random.nextDouble() * Math.PI * 2;
						crawler.snapTo(this.getX() + Math.cos(a) * 4, this.getY() + 0.5, this.getZ() + Math.sin(a) * 4, 0, 0);
						crawler.setTarget(target);
						level.addFreshEntity(crawler);
						level.sendParticles(ParticleTypes.SQUID_INK, crawler.getX(), crawler.getY() + 0.5, crawler.getZ(), 30, 0.4, 0.4, 0.4, 0.05);
					}
					level.playSound(null, this.blockPosition(), SoundEvents.WARDEN_ROAR, SoundSource.HOSTILE, 2.0F, 1.2F);
				}
				return tick >= 20;
			}
			case PULSE -> {
				this.getNavigation().stop();
				if (tick < 20) {
					level.sendParticles(ParticleTypes.SCULK_SOUL, this.getX(), this.getY(0.5), this.getZ(), 4, 1.5, 1.5, 1.5, 0.02);
					return false;
				}
				if (tick == 20) {
					for (Player p : this.fighters(level, 26)) {
						p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 160, 0));
						p.addEffect(new MobEffectInstance(MobEffects.WITHER, 60, 1));
					}
					for (int r = 2; r <= 14; r += 2) Abilities.ring(level, ParticleTypes.SCULK_SOUL, this.position().add(0, 0.5, 0), r, r * 6);
					this.heal(20.0F);
					level.playSound(null, this.blockPosition(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 3.0F, 0.5F);
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
		if (newPhase == 3) {
			this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.38);
			this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(20.0);
		}
		for (Player p : this.fighters(level, 32)) p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 60, 0));
	}

	@Override
	public boolean doHurtTarget(ServerLevel level, net.minecraft.world.entity.Entity target) {
		boolean hit = super.doHurtTarget(level, target);
		if (hit && target instanceof LivingEntity living) {
			living.addEffect(new MobEffectInstance(MobEffects.WITHER, 60, 0));
			level.sendParticles(ParticleTypes.SWEEP_ATTACK, target.getX(), target.getY(0.5), target.getZ(), 1, 0, 0, 0, 0);
		}
		return hit;
	}

	@Override
	public void aiStep() {
		super.aiStep();
		if (this.level().isClientSide() && this.random.nextInt(2) == 0) {
			this.level().addParticle(ParticleTypes.SMOKE, this.getRandomX(1.0), this.getRandomY(), this.getRandomZ(1.0), 0, 0.02, 0);
		}
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.WARDEN_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.RAVAGER_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.WARDEN_DEATH;
	}

	@Override
	public float getVoicePitch() {
		return 0.6F;
	}
}
