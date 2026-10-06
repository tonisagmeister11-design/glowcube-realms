package net.glowcube.realms.entity.boss;

import net.glowcube.realms.entity.projectile.GlowShardProjectile;
import net.glowcube.realms.item.Abilities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ShulkerBullet;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** The Void Herald - a floating end sorcerer atop the Astral Spire. */
public class VoidHerald extends RealmBoss {
	public static final int BULLETS = 1, BLINK = 2, GRAVITY = 3, SUMMON = 4, VOID_RAIN = 5;
	private double angle;

	public VoidHerald(EntityType<? extends VoidHerald> type, Level level) {
		super(type, level);
		this.setNoGravity(true);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
				.add(Attributes.MAX_HEALTH, 320.0)
				.add(Attributes.FLYING_SPEED, 0.5)
				.add(Attributes.ATTACK_DAMAGE, 8.0)
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
		return "void_herald";
	}

	@Override
	protected BossEvent.BossBarColor barColor(int phase) {
		return phase == 1 ? BossEvent.BossBarColor.PURPLE : phase == 2 ? BossEvent.BossBarColor.PINK : BossEvent.BossBarColor.WHITE;
	}

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		LivingEntity target = this.getTarget();
		Vec3 anchor = target != null ? target.position() : (this.home != null ? Vec3.atCenterOf(this.home) : this.position());
		this.angle += 0.025;
		int atk = this.getCurrentAttack();
		double height = atk == GRAVITY || this.isRoaring() ? 1.0 : 3.2;
		this.hover(anchor, this.angle, atk == GRAVITY ? 0.0 : 6.5, height, 0.06);
		if (target != null) this.getLookControl().setLookAt(target, 30, 30);
	}

	@Override
	protected int chooseAttack(int phase, LivingEntity target) {
		int roll = this.random.nextInt(100);
		if (phase == 3 && roll < 22) return VOID_RAIN;
		if (phase >= 2 && roll < 36) return SUMMON;
		if (roll < 55) return BULLETS;
		if (roll < 78) return BLINK;
		return GRAVITY;
	}

	@Override
	protected boolean tickAttack(ServerLevel level, int attack, int tick, LivingEntity target) {
		int phase = this.getPhase();
		switch (attack) {
			case BULLETS -> {
				if (tick % 10 == 0 && tick < 10 * (phase + 1)) {
					for (Player p : this.fighters(level, 32)) {
						ShulkerBullet bullet = new ShulkerBullet(level, this, p, Direction.Axis.Y);
						bullet.setPos(this.getX(), this.getY(0.6), this.getZ());
						level.addFreshEntity(bullet);
					}
					level.playSound(null, this.blockPosition(), SoundEvents.SHULKER_SHOOT, SoundSource.HOSTILE, 2.0F, 0.7F);
				}
				return tick >= 10 * (phase + 1) + 4;
			}
			case BLINK -> {
				if (tick == 0) level.sendParticles(ParticleTypes.REVERSE_PORTAL, this.getX(), this.getY(0.5), this.getZ(), 60, 0.6, 1.0, 0.6, 0.2);
				if (tick == 10) {
					Vec3 behind = target.position().subtract(Vec3.directionFromRotation(0, target.getYRot()).scale(3)).add(0, 1.5, 0);
					this.teleportTo(behind.x, behind.y, behind.z);
					level.playSound(null, this.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 2.0F, 0.6F);
					level.sendParticles(ParticleTypes.PORTAL, this.getX(), this.getY(0.5), this.getZ(), 80, 1.0, 1.0, 1.0, 0.5);
				}
				if (tick == 16) {
					this.areaDamage(level, this.position(), 4.0, 7.0F + phase, 0.9, new MobEffectInstance(MobEffects.BLINDNESS, 40, 0));
					Abilities.ring(level, ParticleTypes.WITCH, this.position(), 4.0, 40);
				}
				return tick >= 20;
			}
			case GRAVITY -> {
				Abilities.ring(level, ParticleTypes.PORTAL, this.position().add(0, 0.5, 0), 10.0 - tick * 0.2, 40);
				for (Player p : this.fighters(level, 14)) {
					Vec3 pull = this.position().subtract(p.position()).normalize().scale(0.12);
					p.push(pull.x, 0.02, pull.z);
					p.needsSync = true;
				}
				if (tick == 40) {
					this.areaDamage(level, this.position(), 3.5, 10.0F + phase, 1.6, new MobEffectInstance(MobEffects.LEVITATION, 30, 0));
					level.sendParticles(ParticleTypes.EXPLOSION, this.getX(), this.getY(), this.getZ(), 3, 1, 0.2, 1, 0);
					level.playSound(null, this.blockPosition(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 2.5F, 1.2F);
				}
				return tick >= 44;
			}
			case SUMMON -> {
				if (tick == 8) {
					this.summonMinions(level, EntityTypes.ENDERMITE, 3, 6, target, 4);
					if (phase == 3) this.summonMinions(level, EntityTypes.ENDERMAN, 1, 2, target, 6);
					level.playSound(null, this.blockPosition(), SoundEvents.EVOKER_PREPARE_SUMMON, SoundSource.HOSTILE, 2.0F, 0.6F);
				}
				return tick >= 16;
			}
			case VOID_RAIN -> {
				if (tick % 4 == 0) {
					for (Player p : this.fighters(level, 32)) {
						GlowShardProjectile orb = GlowShardProjectile.voidOrb(level, this);
						orb.setPos(p.getX() + (this.random.nextDouble() - 0.5) * 8, p.getY() + 14, p.getZ() + (this.random.nextDouble() - 0.5) * 8);
						orb.shoot(0, -1, 0, 0.9F, 2.0F);
						orb.setDamage(6.0F);
						level.addFreshEntity(orb);
					}
				}
				return tick >= 48;
			}
			default -> {
				return true;
			}
		}
	}

	@Override
	protected void onPhaseChange(ServerLevel level, int newPhase) {
		for (Player p : this.fighters(level, 32)) p.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 20, 0));
	}

	@Override
	public void aiStep() {
		super.aiStep();
		if (this.level().isClientSide()) {
			this.level().addParticle(ParticleTypes.PORTAL, this.getRandomX(0.8), this.getRandomY() - 0.25, this.getRandomZ(0.8), 0, 0, 0);
		}
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.ENDERMAN_AMBIENT;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.ENDERMAN_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.ENDER_DRAGON_DEATH;
	}

	@Override
	public float getVoicePitch() {
		return 0.6F;
	}

	@Override
	protected void checkFallDamage(double ya, boolean onGround, BlockState onState, BlockPos pos) {
	}
}
