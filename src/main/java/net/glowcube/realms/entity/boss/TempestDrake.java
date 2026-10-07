package net.glowcube.realms.entity.boss;

import java.util.ArrayList;
import java.util.List;
import net.glowcube.realms.entity.projectile.GlowShardProjectile;
import net.glowcube.realms.item.Abilities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** The Tempest Drake - a storm wyvern nesting in the Storm Aerie above the Lumen Skies. */
public class TempestDrake extends RealmBoss {
	public static final int DIVE = 1, LIGHTNING = 2, GUST = 3, FEATHERS = 4, STORM = 5;
	private final List<Vec3> strikes = new ArrayList<>();
	private Vec3 diveDir = Vec3.ZERO;
	private double angle;

	public TempestDrake(EntityType<? extends TempestDrake> type, Level level) {
		super(type, level);
		this.setNoGravity(true);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
				.add(Attributes.MAX_HEALTH, 300.0)
				.add(Attributes.FLYING_SPEED, 0.6)
				.add(Attributes.ATTACK_DAMAGE, 9.0)
				.add(Attributes.ARMOR, 8.0)
				.add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
				.add(Attributes.FOLLOW_RANGE, 48.0);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 32.0F));
		this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
		this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, false));
	}

	@Override
	protected String bossId() {
		return "tempest_drake";
	}

	@Override
	protected BossEvent.BossBarColor barColor(int phase) {
		return phase == 1 ? BossEvent.BossBarColor.BLUE : phase == 2 ? BossEvent.BossBarColor.YELLOW : BossEvent.BossBarColor.WHITE;
	}

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		LivingEntity target = this.getTarget();
		int atk = this.getCurrentAttack();
		if (atk == DIVE && this.attackTick > 14) return;
		Vec3 anchor = target != null ? target.position() : (this.home != null ? Vec3.atCenterOf(this.home) : this.position());
		this.angle += 0.03;
		// low, close circles so melee players can reach it; it still swoops in with the dive
		double height = this.isRoaring() || atk == FEATHERS ? 0.4 : 2.0;
		this.hover(anchor, this.angle, 3.8, height, 0.07);
		Vec3 v = this.getDeltaMovement();
		if (v.horizontalDistanceSqr() > 1.0E-4) {
			this.setYRot((float) (Math.atan2(v.z, v.x) * (180F / Math.PI)) - 90.0F);
			this.yBodyRot = this.getYRot();
		}
	}

	@Override
	protected int chooseAttack(int phase, LivingEntity target) {
		int roll = this.random.nextInt(100);
		if (phase == 3 && roll < 20) return STORM;
		if (roll < 35) return DIVE;
		if (roll < 60) return LIGHTNING;
		if (roll < 80) return FEATHERS;
		return GUST;
	}

	@Override
	protected boolean tickAttack(ServerLevel level, int attack, int tick, LivingEntity target) {
		int phase = this.getPhase();
		switch (attack) {
			case DIVE -> {
				if (tick < 14) {
					level.sendParticles(ParticleTypes.CLOUD, this.getX(), this.getY(), this.getZ(), 4, 0.6, 0.3, 0.6, 0.02);
					if (tick == 0) level.playSound(null, this.blockPosition(), SoundEvents.PHANTOM_SWOOP, SoundSource.HOSTILE, 3.0F, 0.6F);
					this.diveDir = target.position().add(0, 0.5, 0).subtract(this.position()).normalize();
					return false;
				}
				this.setDeltaMovement(this.diveDir.scale(1.3));
				for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(1.2), e -> e instanceof Player)) {
					e.hurtServer(level, this.damageSources().mobAttack(this), 8.0F + phase);
					Abilities.push(e, this.position(), 1.2, 0.6);
				}
				return tick > 30 || this.horizontalCollision || this.verticalCollision;
			}
			case LIGHTNING -> {
				if (tick == 0) {
					this.strikes.clear();
					for (Player p : this.fighters(level, 40)) {
						this.strikes.add(Glowkeeper.groundBelow(level, p.position().add(0, 1, 0)));
						for (int i = 0; i < phase; i++) {
							double a = this.random.nextDouble() * Math.PI * 2, r = 2 + this.random.nextDouble() * 4;
							this.strikes.add(Glowkeeper.groundBelow(level, p.position().add(Math.cos(a) * r, 1, Math.sin(a) * r)));
						}
					}
				}
				if (tick < 24) {
					for (Vec3 s : this.strikes) Abilities.ring(level, ParticleTypes.ELECTRIC_SPARK, s.add(0, 0.1, 0), 1.5, 10);
				} else if (tick == 24) {
					for (Vec3 s : this.strikes) strike(level, s);
				}
				return tick >= 28;
			}
			case GUST -> {
				if (tick == 10) {
					Vec3 look = target.position().subtract(this.position()).normalize();
					for (Player p : this.fighters(level, 16)) {
						if (p.position().subtract(this.position()).normalize().dot(look) < 0.4) continue;
						p.push(look.x * 1.8, 0.6, look.z * 1.8);
						p.needsSync = true;
						p.hurtServer(level, this.damageSources().mobAttack(this), 4.0F);
					}
					for (int i = 1; i <= 12; i++) Abilities.ring(level, ParticleTypes.CLOUD, this.position().add(look.scale(i)), 0.5 + i * 0.25, 10);
					level.playSound(null, this.blockPosition(), SoundEvents.BREEZE_WIND_CHARGE_BURST.value(), SoundSource.HOSTILE, 3.0F, 0.6F);
				}
				return tick >= 16;
			}
			case FEATHERS -> {
				if (tick % 4 == 0 && tick < 24) {
					for (int i = -1; i <= 1; i++) {
						GlowShardProjectile shard = new GlowShardProjectile(level, this);
						shard.setPos(this.getX(), this.getY(0.5), this.getZ());
						Vec3 dir = target.getEyePosition().subtract(shard.position()).normalize().yRot((float) Math.toRadians(i * 14));
						shard.shoot(dir.x, dir.y, dir.z, 1.4F, 3.0F);
						shard.setDamage(4.0F + phase);
						level.addFreshEntity(shard);
					}
					level.playSound(null, this.blockPosition(), SoundEvents.PHANTOM_FLAP, SoundSource.HOSTILE, 2.0F, 1.2F);
				}
				return tick >= 26;
			}
			case STORM -> {
				if (tick % 8 == 0) {
					BlockPos c = this.home != null ? this.home : this.blockPosition();
					for (int i = 0; i < 2; i++) {
						Vec3 s = Glowkeeper.groundBelow(level, new Vec3(c.getX() + (this.random.nextDouble() - 0.5) * 30, c.getY() + 4, c.getZ() + (this.random.nextDouble() - 0.5) * 30));
						strike(level, s);
					}
				}
				return tick >= 64;
			}
			default -> {
				return true;
			}
		}
	}

	private void strike(ServerLevel level, Vec3 pos) {
		LightningBolt bolt = EntityTypes.LIGHTNING_BOLT.create(level, EntitySpawnReason.TRIGGERED);
		if (bolt != null) {
			bolt.snapTo(pos.x, pos.y, pos.z);
			bolt.setVisualOnly(true);
			level.addFreshEntity(bolt);
		}
		this.areaDamage(level, pos, 2.2, 8.0F + this.getPhase(), 0.4, null);
	}

	@Override
	protected void onPhaseChange(ServerLevel level, int newPhase) {
		for (Player p : this.fighters(level, 20)) strike(level, p.position());
	}

	@Override
	public boolean isInvulnerableTo(ServerLevel level, DamageSource source) {
		return source.is(net.minecraft.tags.DamageTypeTags.IS_LIGHTNING) || super.isInvulnerableTo(level, source);
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.PHANTOM_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.PHANTOM_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.ENDER_DRAGON_DEATH;
	}

	@Override
	public float getVoicePitch() {
		return 0.5F;
	}

	@Override
	protected void checkFallDamage(double ya, boolean onGround, BlockState onState, BlockPos pos) {
	}
}
