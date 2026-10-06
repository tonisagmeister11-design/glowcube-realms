package net.glowcube.realms.entity.boss;

import java.util.ArrayList;
import java.util.List;
import net.glowcube.realms.item.Abilities;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.hurtingprojectile.SmallFireball;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;
import net.glowcube.realms.registry.ModItems;
import org.jspecify.annotations.Nullable;

/**
 * The Ember Warden - a burning knight guarding the Ember Citadel in the overworld.
 * Attacks: fireball barrage, flame pillars, meteor rain, blaze guard, inferno whirlwind, blazing dash.
 */
public class EmberWarden extends RealmBoss {
	public static final int BARRAGE = 1, PILLARS = 2, METEORS = 3, SUMMON = 4, WHIRL = 5, DASH = 6;
	private final List<Vec3> pillarSpots = new ArrayList<>();
	private Vec3 dashDir = Vec3.ZERO;

	public EmberWarden(EntityType<? extends EmberWarden> type, Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
				.add(Attributes.MAX_HEALTH, 300.0)
				.add(Attributes.MOVEMENT_SPEED, 0.32)
				.add(Attributes.ATTACK_DAMAGE, 10.0)
				.add(Attributes.ARMOR, 10.0)
				.add(Attributes.ARMOR_TOUGHNESS, 4.0)
				.add(Attributes.KNOCKBACK_RESISTANCE, 0.9)
				.add(Attributes.FOLLOW_RANGE, 40.0);
	}

	@Override
	public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason, @Nullable SpawnGroupData data) {
		SpawnGroupData d = super.finalizeSpawn(level, difficulty, reason, data);
		this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(ModItems.EMBER_GREATSWORD));
		this.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
		return d;
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
		this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.15, true) {
			@Override
			public boolean canUse() {
				return EmberWarden.this.getCurrentAttack() == 0 && !EmberWarden.this.isRoaring() && super.canUse();
			}

			@Override
			public boolean canContinueToUse() {
				return EmberWarden.this.getCurrentAttack() == 0 && super.canContinueToUse();
			}
		});
		this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 24.0F));
		this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
		this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, false));
	}

	@Override
	protected String bossId() {
		return "ember_warden";
	}

	@Override
	protected BossEvent.BossBarColor barColor(int phase) {
		return phase == 1 ? BossEvent.BossBarColor.YELLOW : phase == 2 ? BossEvent.BossBarColor.RED : BossEvent.BossBarColor.WHITE;
	}

	@Override
	protected int chooseAttack(int phase, LivingEntity target) {
		int roll = this.random.nextInt(100);
		double dist = this.distanceTo(target);
		if (phase == 3 && roll < 20) return WHIRL;
		if (phase >= 2 && roll < 32 && this.level().getEntitiesOfClass(Blaze.class, this.getBoundingBox().inflate(32)).size() < 3) return SUMMON;
		if (phase >= 2 && roll < 50) return METEORS;
		if (dist > 8 && roll < 70) return DASH;
		if (roll < 82) return BARRAGE;
		return PILLARS;
	}

	@Override
	protected boolean tickAttack(ServerLevel level, int attack, int tick, LivingEntity target) {
		int phase = this.getPhase();
		switch (attack) {
			case BARRAGE -> {
				this.getNavigation().stop();
				this.getLookControl().setLookAt(target, 30, 30);
				if (tick % 5 == 0 && tick < 25) {
					Vec3 dir = target.getEyePosition().subtract(this.getEyePosition()).normalize();
					for (int i = -(phase - 1); i <= phase - 1; i++) {
						Vec3 d = dir.yRot((float) Math.toRadians(i * 10));
						SmallFireball ball = new SmallFireball(level, this, d);
						ball.setPos(this.getX() + d.x, this.getEyeY() - 0.3, this.getZ() + d.z);
						level.addFreshEntity(ball);
					}
					level.playSound(null, this.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 2.0F, 0.7F);
				}
				return tick >= 28;
			}
			case PILLARS -> {
				this.getNavigation().stop();
				if (tick == 0) {
					this.pillarSpots.clear();
					for (Player p : this.fighters(level, 32)) {
						this.pillarSpots.add(Glowkeeper.groundBelow(level, p.position().add(0, 1, 0)));
						for (int i = 0; i < 2 + phase; i++) {
							double a = this.random.nextDouble() * Math.PI * 2, r = 2 + this.random.nextDouble() * 4;
							this.pillarSpots.add(Glowkeeper.groundBelow(level, p.position().add(Math.cos(a) * r, 1, Math.sin(a) * r)));
						}
					}
					level.playSound(null, this.blockPosition(), SoundEvents.FIRECHARGE_USE, SoundSource.HOSTILE, 2.0F, 0.6F);
				}
				if (tick < 25) {
					for (Vec3 s : this.pillarSpots) {
						level.sendParticles(ParticleTypes.SMOKE, s.x, s.y + 0.1, s.z, 3, 0.6, 0.05, 0.6, 0.0);
						Abilities.ring(level, ParticleTypes.FLAME, s.add(0, 0.1, 0), 1.5, 8);
					}
				} else if (tick == 25) {
					for (Vec3 s : this.pillarSpots) {
						for (int h = 0; h < 7; h++) level.sendParticles(ParticleTypes.FLAME, s.x, s.y + h * 0.7, s.z, 10, 0.3, 0.3, 0.3, 0.05);
						level.sendParticles(ParticleTypes.LAVA, s.x, s.y + 0.5, s.z, 6, 0.3, 0.3, 0.3, 0.0);
						this.areaDamage(level, s, 1.8, 8.0F + phase, 0.3, null);
						for (LivingEntity e : level.getEntitiesOfClass(Player.class, new net.minecraft.world.phys.AABB(s, s).inflate(1.8, 4, 1.8))) e.igniteForSeconds(6);
					}
					level.playSound(null, this.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 3.0F, 0.4F);
				}
				return tick >= 30;
			}
			case METEORS -> {
				this.getNavigation().stop();
				if (tick % 3 == 0) {
					for (Player p : this.fighters(level, 32)) {
						double ox = (this.random.nextDouble() - 0.5) * 12, oz = (this.random.nextDouble() - 0.5) * 12;
						SmallFireball ball = new SmallFireball(level, p.getX() + ox, p.getY() + 18, p.getZ() + oz, new Vec3(0, -1.3, 0));
						ball.setOwner(this);
						level.addFreshEntity(ball);
					}
				}
				if (tick == 0) level.playSound(null, this.blockPosition(), SoundEvents.GHAST_WARN, SoundSource.HOSTILE, 3.0F, 0.6F);
				return tick >= 45;
			}
			case SUMMON -> {
				this.getNavigation().stop();
				if (tick == 10) {
					for (int i = 0; i < 2; i++) {
						Blaze blaze = EntityTypes.BLAZE.create(level, EntitySpawnReason.MOB_SUMMONED);
						if (blaze == null) continue;
						double a = this.random.nextDouble() * Math.PI * 2;
						blaze.snapTo(this.getX() + Math.cos(a) * 5, this.getY() + 2, this.getZ() + Math.sin(a) * 5, 0, 0);
						blaze.setTarget(target);
						level.addFreshEntity(blaze);
						level.sendParticles(ParticleTypes.FLAME, blaze.getX(), blaze.getY() + 1, blaze.getZ(), 40, 0.4, 0.8, 0.4, 0.05);
					}
					level.playSound(null, this.blockPosition(), SoundEvents.BLAZE_AMBIENT, SoundSource.HOSTILE, 3.0F, 0.6F);
				}
				return tick >= 20;
			}
			case WHIRL -> {
				this.getNavigation().stop();
				this.setYRot(this.getYRot() + 36.0F);
				this.yBodyRot = this.getYRot();
				Abilities.ring(level, ParticleTypes.FLAME, this.position().add(0, 1.0, 0), 3.5, 20);
				if (tick % 5 == 0) {
					this.areaDamage(level, this.position(), 4.0, 7.0F, 0.9, null);
					for (LivingEntity e : level.getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(4))) e.igniteForSeconds(4);
					level.playSound(null, this.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 2.0F, 0.6F);
				}
				Vec3 toward = target.position().subtract(this.position()).normalize().scale(0.18);
				this.setDeltaMovement(toward.x, this.getDeltaMovement().y, toward.z);
				return tick >= 40;
			}
			case DASH -> {
				this.getNavigation().stop();
				if (tick < 12) {
					this.getLookControl().setLookAt(target, 30, 30);
					Vec3 d = target.position().subtract(this.position());
					this.dashDir = new Vec3(d.x, 0, d.z).normalize();
					level.sendParticles(ParticleTypes.FLAME, this.getX(), this.getY() + 0.2, this.getZ(), 6, 0.4, 0.1, 0.4, 0.02);
					return false;
				}
				this.setDeltaMovement(this.dashDir.x * 1.3, this.getDeltaMovement().y, this.dashDir.z * 1.3);
				level.sendParticles(ParticleTypes.FLAME, this.getX(), this.getY(0.5), this.getZ(), 8, 0.5, 0.5, 0.5, 0.02);
				for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(1.2), e -> e instanceof Player)) {
					e.hurtServer(level, this.damageSources().mobAttack(this), 9.0F + phase);
					e.igniteForSeconds(5);
					Abilities.push(e, this.position(), 1.6, 0.5);
				}
				if (tick == 12) level.playSound(null, this.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 2.0F, 1.2F);
				return tick > 26 || (this.horizontalCollision && tick > 14);
			}
			default -> {
				return true;
			}
		}
	}

	@Override
	protected void onPhaseChange(ServerLevel level, int newPhase) {
		this.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 100000, 0));
		if (newPhase == 3) {
			this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.4);
			this.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 100000, 0));
		}
		this.areaDamage(level, this.position(), 6.0, 6.0F, 1.2, null);
	}

	@Override
	public boolean doHurtTarget(ServerLevel level, Entity target) {
		boolean hit = super.doHurtTarget(level, target);
		if (hit) {
			target.igniteForSeconds(6);
			level.sendParticles(ParticleTypes.FLAME, target.getX(), target.getY(0.5), target.getZ(), 20, 0.3, 0.5, 0.3, 0.05);
		}
		return hit;
	}

	@Override
	public void aiStep() {
		super.aiStep();
		if (this.level().isClientSide()) {
			for (int i = 0; i < 2; i++) {
				this.level().addParticle(ParticleTypes.FLAME, this.getRandomX(0.6), this.getRandomY(), this.getRandomZ(0.6), 0, 0.03, 0);
			}
		}
	}

	@Override
	public boolean fireImmune() {
		return true;
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.BLAZE_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.NETHERITE_BLOCK_HIT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.BLAZE_DEATH;
	}

	@Override
	public float getVoicePitch() {
		return 0.5F;
	}
}
