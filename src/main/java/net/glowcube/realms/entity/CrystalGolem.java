package net.glowcube.realms.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
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
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/** Hulking crystal construct. Shrugs off arrows, slams with crystal fists. */
public class CrystalGolem extends Monster {
	public int attackAnim;

	public CrystalGolem(EntityType<? extends CrystalGolem> type, Level level) {
		super(type, level);
		this.xpReward = 12;
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
				.add(Attributes.MAX_HEALTH, 30.0)
				.add(Attributes.MOVEMENT_SPEED, 0.22)
				.add(Attributes.ATTACK_DAMAGE, 5.0)
				.add(Attributes.ARMOR, 6.0)
				.add(Attributes.KNOCKBACK_RESISTANCE, 0.8)
				.add(Attributes.FOLLOW_RANGE, 24.0);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
		this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, true));
		this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.6));
		this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 10.0F));
		this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
		this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
		this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
	}

	@Override
	public boolean doHurtTarget(ServerLevel level, Entity target) {
		this.attackAnim = 10;
		level.broadcastEntityEvent(this, (byte) 4);
		boolean hit = super.doHurtTarget(level, target);
		if (hit) {
			target.push(0, 0.45, 0);
			if (target instanceof LivingEntity living) living.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 1));
			level.sendParticles(ParticleTypes.END_ROD, target.getX(), target.getY(0.5), target.getZ(), 12, 0.3, 0.3, 0.3, 0.1);
		}
		this.playSound(SoundEvents.AMETHYST_BLOCK_HIT, 1.5F, 0.6F);
		return hit;
	}

	@Override
	public void handleEntityEvent(byte id) {
		if (id == 4) this.attackAnim = 10;
		else super.handleEntityEvent(id);
	}

	@Override
	public void aiStep() {
		super.aiStep();
		if (this.attackAnim > 0) this.attackAnim--;
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		if (source.is(DamageTypeTags.IS_PROJECTILE)) damage *= 0.35F;
		return super.hurtServer(level, source, damage);
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return SoundEvents.AMETHYST_BLOCK_CHIME;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.AMETHYST_BLOCK_HIT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.AMETHYST_BLOCK_BREAK;
	}
}
