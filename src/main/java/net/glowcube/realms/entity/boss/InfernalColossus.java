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
import net.minecraft.world.damagesource.DamageSource;
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
import net.minecraft.world.entity.projectile.hurtingprojectile.SmallFireball;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** The Infernal Colossus - a giant magma golem guarding the Molten Forge in the Nether. */
public class InfernalColossus extends RealmBoss {
	public static final int SLAM = 1, BARRAGE = 2, ERUPTION = 3, SUMMON = 4, AURA = 5;
	private final List<Vec3> spots = new ArrayList<>();

	public InfernalColossus(EntityType<? extends InfernalColossus> type, Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
				.add(Attributes.MAX_HEALTH, 360.0)
				.add(Attributes.MOVEMENT_SPEED, 0.24)
				.add(Attributes.ATTACK_DAMAGE, 12.0)
				.add(Attributes.ATTACK_KNOCKBACK, 1.5)
				.add(Attributes.ARMOR, 10.0)
				.add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
				.add(Attributes.STEP_HEIGHT, 1.5)
				.add(Attributes.FOLLOW_RANGE, 40.0);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
		this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, true) {
			@Override
			public boolean canUse() {
				return InfernalColossus.this.getCurrentAttack() == 0 && super.canUse();
			}

			@Override
			public boolean canContinueToUse() {
				return InfernalColossus.this.getCurrentAttack() == 0 && super.canContinueToUse();
			}
		});
		this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 24.0F));
		this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
		this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, false));
	}

	@Override
	protected String bossId() {
		return "infernal_colossus";
	}

	@Override
	protected BossEvent.BossBarColor barColor(int phase) {
		return phase == 1 ? BossEvent.BossBarColor.RED : phase == 2 ? BossEvent.BossBarColor.YELLOW : BossEvent.BossBarColor.WHITE;
	}

	@Override
	protected int chooseAttack(int phase, LivingEntity target) {
		int roll = this.random.nextInt(100);
		if (phase == 3 && roll < 20) return AURA;
		if (phase >= 2 && roll < 34) return SUMMON;
		if (roll < 55) return SLAM;
		if (roll < 80) return BARRAGE;
		return ERUPTION;
	}

	@Override
	protected boolean tickAttack(ServerLevel level, int attack, int tick, LivingEntity target) {
		int phase = this.getPhase();
		this.getNavigation().stop();
		switch (attack) {
			case SLAM -> {
				if (tick < 14) {
					this.getLookControl().setLookAt(target, 30, 30);
					level.sendParticles(ParticleTypes.LAVA, this.getX(), this.getY() + 3, this.getZ(), 2, 0.6, 0.4, 0.6, 0);
					if (tick == 0) level.playSound(null, this.blockPosition(), SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 2.5F, 0.5F);
					return false;
				}
				if (tick == 14) {
					for (int r = 1; r <= 8; r++) Abilities.ring(level, ParticleTypes.FLAME, this.position().add(0, 0.2, 0), r, 10 + r * 5);
					level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, this.getX(), this.getY(), this.getZ(), 1, 0, 0, 0, 0);
					this.areaDamage(level, this.position(), 7.0, 9.0F + phase, 1.1, null);
					for (LivingEntity e : this.fighters(level, 7)) e.igniteForSeconds(4);
					level.playSound(null, this.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 2.5F, 0.6F);
				}
				return tick >= 22;
			}
			case BARRAGE -> {
				if (tick % 6 == 0 && tick < 30) {
					for (Player p : this.fighters(level, 30)) {
						Vec3 dir = p.position().add(0, 1, 0).subtract(this.position().add(0, 3, 0)).normalize();
						SmallFireball ball = new SmallFireball(level, this, dir.add(0, 0.15, 0));
						ball.setPos(this.getX(), this.getY() + 3.2, this.getZ());
						level.addFreshEntity(ball);
					}
					level.playSound(null, this.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 2.0F, 0.5F);
				}
				return tick >= 32;
			}
			case ERUPTION -> {
				if (tick == 0) {
					this.spots.clear();
					for (Player p : this.fighters(level, 32)) {
						this.spots.add(Glowkeeper.groundBelow(level, p.position().add(0, 1, 0)));
						for (int i = 0; i < 2 + phase; i++) {
							double a = this.random.nextDouble() * Math.PI * 2, r = 2 + this.random.nextDouble() * 5;
							this.spots.add(Glowkeeper.groundBelow(level, p.position().add(Math.cos(a) * r, 1, Math.sin(a) * r)));
						}
					}
				}
				if (tick < 24) {
					for (Vec3 s : this.spots) level.sendParticles(ParticleTypes.SMOKE, s.x, s.y + 0.1, s.z, 3, 0.5, 0.05, 0.5, 0.01);
				} else if (tick == 24) {
					for (Vec3 s : this.spots) {
						for (int h = 0; h < 6; h++) level.sendParticles(ParticleTypes.LAVA, s.x, s.y + h * 0.6, s.z, 2, 0.2, 0.2, 0.2, 0);
						level.sendParticles(ParticleTypes.FLAME, s.x, s.y + 1, s.z, 30, 0.3, 1.2, 0.3, 0.05);
						this.areaDamage(level, s, 1.8, 7.0F + phase, 0.4, null);
					}
					level.playSound(null, this.blockPosition(), SoundEvents.LAVA_POP, SoundSource.HOSTILE, 3.0F, 0.5F);
				}
				return tick >= 28;
			}
			case SUMMON -> {
				if (tick == 8) {
					this.summonMinions(level, EntityTypes.MAGMA_CUBE, 3, 5, target, 5);
					level.playSound(null, this.blockPosition(), SoundEvents.MAGMA_CUBE_SQUISH, SoundSource.HOSTILE, 3.0F, 0.5F);
				}
				return tick >= 16;
			}
			case AURA -> {
				Abilities.ring(level, ParticleTypes.FLAME, this.position().add(0, 0.5, 0), 5.0, 30);
				if (tick % 10 == 0) {
					this.areaDamage(level, this.position(), 5.0, 4.0F, 0.2, null);
					for (LivingEntity e : this.fighters(level, 5)) e.igniteForSeconds(3);
				}
				return tick >= 60;
			}
			default -> {
				return true;
			}
		}
	}

	@Override
	protected void onPhaseChange(ServerLevel level, int newPhase) {
		if (newPhase == 3) this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.3);
	}

	@Override
	public boolean doHurtTarget(ServerLevel level, Entity target) {
		boolean hit = super.doHurtTarget(level, target);
		if (hit) target.igniteForSeconds(5);
		return hit;
	}

	@Override
	public void aiStep() {
		super.aiStep();
		if (this.level().isClientSide() && this.random.nextInt(2) == 0) {
			this.level().addParticle(ParticleTypes.LAVA, this.getRandomX(0.8), this.getRandomY(), this.getRandomZ(0.8), 0, 0, 0);
		}
	}

	@Override
	public boolean fireImmune() {
		return true;
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.BLAZE_BURN;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.IRON_GOLEM_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.IRON_GOLEM_DEATH;
	}

	@Override
	public float getVoicePitch() {
		return 0.5F;
	}
}
