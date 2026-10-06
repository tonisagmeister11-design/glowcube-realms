package net.glowcube.realms.entity.animal;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * Peaceful realm creature. Wanders around, flees when hurt, drops food and crafting materials (loot tables).
 * Flying variants hover and drift like bees.
 */
public class RealmAnimal extends PathfinderMob {
	public record Kind(double health, double speed, boolean flying, @Nullable ParticleOptions particle, SoundEvent ambient, SoundEvent hurt,
			SoundEvent death, float pitch, boolean fireImmune, @Nullable MobEffectInstance hitEffect) {
	}

	private final Kind kind;

	public RealmAnimal(EntityType<? extends RealmAnimal> type, Level level, Kind kind) {
		super(type, level);
		this.kind = kind;
		if (this.flies()) {
			this.moveControl = new FlyingMoveControl<>(this, 20, true);
			this.setNoGravity(true);
		}
	}

	public static AttributeSupplier.Builder attributes(double health, double speed) {
		return PathfinderMob.createMobAttributes()
				.add(Attributes.MAX_HEALTH, health)
				.add(Attributes.MOVEMENT_SPEED, speed)
				.add(Attributes.FLYING_SPEED, speed * 2)
				.add(Attributes.FOLLOW_RANGE, 16.0);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
		this.goalSelector.addGoal(1, new PanicGoal(this, 1.6));
		if (this.isFlyingKind()) this.goalSelector.addGoal(5, new WaterAvoidingRandomFlyingGoal(this, 1.0));
		else this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 1.0));
		this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 6.0F));
		this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
	}

	protected boolean flies() {
		return false;
	}

	private boolean isFlyingKind() {
		return this.flies();
	}

	@Override
	protected PathNavigation createNavigation(Level level) {
		if (this.flies()) {
			FlyingPathNavigation nav = new FlyingPathNavigation(this, level);
			nav.setCanFloat(true);
			return nav;
		}
		return super.createNavigation(level);
	}

	@Override
	public void aiStep() {
		super.aiStep();
		if (this.level().isClientSide() && this.kind != null && this.kind.particle() != null && this.random.nextInt(6) == 0) {
			this.level().addParticle(this.kind.particle(), this.getRandomX(0.5), this.getRandomY(), this.getRandomZ(0.5), 0, 0.01, 0);
		}
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		boolean hurt = super.hurtServer(level, source, damage);
		if (hurt && this.kind != null && this.kind.hitEffect() != null && source.getEntity() instanceof Player p) {
			p.addEffect(new MobEffectInstance(this.kind.hitEffect()));
		}
		return hurt;
	}

	@Override
	public boolean fireImmune() {
		return (this.kind != null && this.kind.fireImmune()) || super.fireImmune();
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return this.kind.ambient();
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return this.kind.hurt();
	}

	@Override
	protected SoundEvent getDeathSound() {
		return this.kind.death();
	}

	@Override
	public float getVoicePitch() {
		return this.kind.pitch();
	}

	@Override
	public boolean causeFallDamage(double fallDistance, float damageModifier, DamageSource source) {
		return !this.flies() && super.causeFallDamage(fallDistance, damageModifier, source);
	}

	@Override
	protected void checkFallDamage(double ya, boolean onGround, BlockState onState, BlockPos pos) {
		if (!this.flies()) super.checkFallDamage(ya, onGround, onState, pos);
	}

	@Override
	public boolean removeWhenFarAway(double distanceToClosestPlayer) {
		return false;
	}
}
