package net.glowcube.realms.entity.boss;

import java.util.ArrayList;
import java.util.List;
import net.glowcube.realms.entity.CrystalGolem;
import net.glowcube.realms.entity.GlowWisp;
import net.glowcube.realms.entity.projectile.GlowShardProjectile;
import net.glowcube.realms.registry.ModEntities;
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
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The Glowkeeper - guardian of the Lumen Skies. A floating crystal titan.
 * Attacks: crystal volley, prism beam, starfall, crystal reinforcements, supernova.
 */
public class Glowkeeper extends RealmBoss {
	public static final int VOLLEY = 1, BEAM = 2, STARFALL = 3, SUMMON = 4, NOVA = 5;
	private final List<Vec3> starfallSpots = new ArrayList<>();
	private Vec3 beamTarget;
	private double orbitAngle;

	public Glowkeeper(EntityType<? extends Glowkeeper> type, Level level) {
		super(type, level);
		this.setNoGravity(true);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
				.add(Attributes.MAX_HEALTH, 320.0)
				.add(Attributes.MOVEMENT_SPEED, 0.3)
				.add(Attributes.FLYING_SPEED, 0.5)
				.add(Attributes.ATTACK_DAMAGE, 10.0)
				.add(Attributes.ARMOR, 8.0)
				.add(Attributes.ARMOR_TOUGHNESS, 4.0)
				.add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
				.add(Attributes.FOLLOW_RANGE, 48.0);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 32.0F));
		this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
		this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
		this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, false));
	}

	@Override
	protected String bossId() {
		return "glowkeeper";
	}

	@Override
	protected BossEvent.BossBarColor barColor(int phase) {
		return phase == 1 ? BossEvent.BossBarColor.BLUE : phase == 2 ? BossEvent.BossBarColor.PURPLE : BossEvent.BossBarColor.WHITE;
	}

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		LivingEntity target = this.getTarget();
		// hover and orbit around the target
		Vec3 anchor = target != null ? target.position() : (this.home != null ? Vec3.atCenterOf(this.home) : this.position());
		this.orbitAngle += this.getPhase() == 3 ? 0.035 : 0.02;
		int atk = this.getCurrentAttack();
		double radius = atk == NOVA ? 0.0 : 6.0;
		// lands during starfall and summoning so melee players get a window to hit it
		double height = atk == NOVA ? 6.0 : (atk == STARFALL || atk == SUMMON || this.isRoaring()) ? 0.6 : 2.6 + Math.sin(this.tickCount * 0.05) * 0.8;
		Vec3 want = anchor.add(Math.cos(this.orbitAngle) * radius, height, Math.sin(this.orbitAngle) * radius);
		Vec3 delta = want.subtract(this.position());
		double speed = this.getCurrentAttack() == BEAM ? 0.02 : 0.06;
		this.setDeltaMovement(this.getDeltaMovement().scale(0.85).add(delta.normalize().scale(Math.min(delta.length(), 1.0) * speed)));
		if (target != null) this.getLookControl().setLookAt(target, 30.0F, 30.0F);
		if (this.tickCount % 3 == 0) {
			level.sendParticles(ParticleTypes.END_ROD, this.getX(), this.getY(0.3), this.getZ(), 2, 0.8, 0.6, 0.8, 0.01);
		}
	}

	@Override
	protected int chooseAttack(int phase, LivingEntity target) {
		int roll = this.random.nextInt(100);
		if (phase == 3 && roll < 22) return NOVA;
		if (phase >= 2 && roll < 38 && this.countMinions() < 4) return SUMMON;
		if (roll < 60) return VOLLEY;
		if (roll < 80) return BEAM;
		return STARFALL;
	}

	private int countMinions() {
		AABB box = this.getBoundingBox().inflate(32);
		return this.level().getEntitiesOfClass(CrystalGolem.class, box).size() + this.level().getEntitiesOfClass(GlowWisp.class, box).size() / 2;
	}

	@Override
	protected boolean tickAttack(ServerLevel level, int attack, int tick, LivingEntity target) {
		int phase = this.getPhase();
		switch (attack) {
			case VOLLEY -> {
				int interval = phase == 3 ? 2 : phase == 2 ? 3 : 4;
				if (tick % interval == 0) {
					int count = phase >= 2 ? 3 : 1;
					for (int i = 0; i < count; i++) {
						GlowShardProjectile shard = new GlowShardProjectile(level, this);
						shard.setPos(this.getX(), this.getY(0.6), this.getZ());
						Vec3 dir = target.getEyePosition().subtract(shard.position()).normalize();
						dir = dir.yRot((float) Math.toRadians((i - (count - 1) / 2.0) * 12));
						shard.shoot(dir.x, dir.y, dir.z, 1.4F, 2.0F);
						shard.setDamage(7.0F + phase);
						shard.setSeeking(phase == 3);
						level.addFreshEntity(shard);
					}
					level.playSound(null, this.blockPosition(), SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.HOSTILE, 1.5F, 1.4F);
				}
				return tick >= 36;
			}
			case BEAM -> {
				if (tick < 30) {
					this.beamTarget = target.getEyePosition().add(0, -0.3, 0);
					Vec3 eye = this.position().add(0, this.getBbHeight() * 0.7, 0);
					net.glowcube.realms.item.Abilities.line(level, ParticleTypes.ELECTRIC_SPARK, eye, this.beamTarget, 24);
					if (tick == 0) level.playSound(null, this.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.HOSTILE, 2.0F, 1.8F);
				} else if (tick < 44) {
					Vec3 eye = this.position().add(0, this.getBbHeight() * 0.7, 0);
					Vec3 dir = this.beamTarget.subtract(eye).normalize();
					Vec3 end = eye.add(dir.scale(40));
					net.glowcube.realms.item.Abilities.line(level, ParticleTypes.END_ROD, eye, end, 60);
					if (tick % 4 == 0) {
						for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(eye, end).inflate(1.5), e -> e instanceof Player)) {
							Vec3 rel = e.getBoundingBox().getCenter().subtract(eye);
							double along = rel.dot(dir);
							if (along < 0) continue;
							Vec3 closest = eye.add(dir.scale(along));
							if (closest.distanceTo(e.getBoundingBox().getCenter()) < 1.4) {
								e.hurtServer(level, this.damageSources().indirectMagic(this, this), 3.0F + phase);
								e.igniteForSeconds(4);
							}
						}
						level.playSound(null, this.blockPosition(), SoundEvents.BEACON_AMBIENT, SoundSource.HOSTILE, 2.0F, 2.0F);
					}
				}
				return tick >= 46;
			}
			case STARFALL -> {
				if (tick == 0) {
					this.starfallSpots.clear();
					int n = 4 + phase * 2;
					for (Player p : this.fighters(level, 40)) {
						this.starfallSpots.add(groundBelow(level, p.position()));
						for (int i = 0; i < n / 2; i++) {
							double a = this.random.nextDouble() * Math.PI * 2, r = 2 + this.random.nextDouble() * 5;
							this.starfallSpots.add(groundBelow(level, p.position().add(Math.cos(a) * r, 0, Math.sin(a) * r)));
						}
					}
					level.playSound(null, this.blockPosition(), SoundEvents.ILLUSIONER_PREPARE_BLINDNESS, SoundSource.HOSTILE, 2.0F, 1.2F);
				}
				if (tick < 30) {
					for (Vec3 s : this.starfallSpots) {
						net.glowcube.realms.item.Abilities.ring(level, ParticleTypes.END_ROD, s.add(0, 0.1, 0), 2.5 * (1 - tick / 30.0) + 0.5, 12);
						level.sendParticles(ParticleTypes.FIREWORK, s.x, s.y + 12 - tick * 0.4, s.z, 1, 0.1, 0.1, 0.1, 0.0);
					}
				} else if (tick == 30) {
					for (Vec3 s : this.starfallSpots) {
						level.sendParticles(ParticleTypes.EXPLOSION, s.x, s.y + 0.5, s.z, 2, 0.5, 0.2, 0.5, 0);
						level.sendParticles(ParticleTypes.END_ROD, s.x, s.y + 0.5, s.z, 30, 0.3, 1.5, 0.3, 0.2);
						this.areaDamage(level, s, 2.6, 7.0F + phase, 0.6, null);
					}
					level.playSound(null, this.blockPosition(), SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, SoundSource.HOSTILE, 3.0F, 0.6F);
				}
				return tick >= 34;
			}
			case SUMMON -> {
				if (tick == 10) {
					BlockPos base = this.home != null ? this.home : target.blockPosition();
					for (int i = 0; i < 2; i++) {
						CrystalGolem golem = ModEntities.CRYSTAL_GOLEM.create(level, EntitySpawnReason.MOB_SUMMONED);
						if (golem == null) continue;
						Vec3 p = groundBelow(level, Vec3.atCenterOf(base).add((i * 2 - 1) * 6, 4, 3));
						golem.snapTo(p.x, p.y, p.z, 0, 0);
						golem.setTarget(target);
						level.addFreshEntity(golem);
						level.sendParticles(ParticleTypes.END_ROD, p.x, p.y + 1, p.z, 40, 0.5, 1, 0.5, 0.1);
					}
					for (int i = 0; i < 3; i++) {
						GlowWisp wisp = ModEntities.GLOW_WISP.create(level, EntitySpawnReason.MOB_SUMMONED);
						if (wisp == null) continue;
						wisp.snapTo(this.getX(), this.getY(), this.getZ(), 0, 0);
						wisp.setTarget(target);
						level.addFreshEntity(wisp);
					}
					level.playSound(null, this.blockPosition(), SoundEvents.EVOKER_PREPARE_SUMMON, SoundSource.HOSTILE, 2.0F, 1.3F);
				}
				return tick >= 20;
			}
			case NOVA -> {
				if (tick < 24) {
					level.sendParticles(ParticleTypes.END_ROD, this.getX(), this.getY(0.5), this.getZ(), 6, 2.0 - tick * 0.08, 2.0 - tick * 0.08, 2.0 - tick * 0.08, 0.0);
					if (tick == 0) level.playSound(null, this.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.HOSTILE, 3.0F, 0.6F);
				} else {
					double r = (tick - 23) * 1.5;
					Vec3 ground = groundBelow(level, this.position());
					net.glowcube.realms.item.Abilities.ring(level, ParticleTypes.END_ROD, ground.add(0, 0.3, 0), r, (int) (12 + r * 8));
					AABB box = new AABB(ground, ground).inflate(r + 1, 4, r + 1);
					for (Player p : level.getEntitiesOfClass(Player.class, box, p -> !p.isCreative() && !p.isSpectator())) {
						double d = Math.hypot(p.getX() - ground.x, p.getZ() - ground.z);
						if (Math.abs(d - r) < 1.2 && p.getY() - ground.y < 1.6) {
							p.hurtServer(level, this.damageSources().indirectMagic(this, this), 9.0F);
							p.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0));
							net.glowcube.realms.item.Abilities.push(p, ground, 1.5, 0.5);
						}
					}
					if (tick == 24) level.playSound(null, this.blockPosition(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 3.0F, 1.4F);
				}
				return tick >= 34;
			}
			default -> {
				return true;
			}
		}
	}

	@Override
	protected void onPhaseChange(ServerLevel level, int newPhase) {
		this.heal(this.getMaxHealth() * 0.04F);
		if (newPhase == 3) {
			this.getAttribute(Attributes.ARMOR).setBaseValue(18.0);
		}
	}

	static Vec3 groundBelow(ServerLevel level, Vec3 pos) {
		BlockPos.MutableBlockPos p = BlockPos.containing(pos).mutable();
		for (int i = 0; i < 24 && level.getBlockState(p.below()).isAir() && p.getY() > level.getMinY(); i++) p.move(0, -1, 0);
		return new Vec3(pos.x, p.getY(), pos.z);
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.AMETHYST_BLOCK_RESONATE;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.AMETHYST_BLOCK_HIT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.WITHER_DEATH;
	}

	@Override
	protected void checkFallDamage(double ya, boolean onGround, BlockState onState, BlockPos pos) {
	}

	@Override
	public boolean fireImmune() {
		return true;
	}
}
