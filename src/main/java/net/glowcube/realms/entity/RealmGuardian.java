package net.glowcube.realms.entity;

import java.util.EnumSet;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

/** Village guard. Patrols around its home, fights monsters and anyone who hurts villagers. */
public class RealmGuardian extends PathfinderMob implements RangedAttackMob {
	private BlockPos home;

	public RealmGuardian(EntityType<? extends RealmGuardian> type, Level level) {
		super(type, level);
		this.xpReward = 0;
	}

	public static AttributeSupplier.Builder createAttributes() {
		return PathfinderMob.createMobAttributes()
				.add(Attributes.MAX_HEALTH, 36.0)
				.add(Attributes.MOVEMENT_SPEED, 0.32)
				.add(Attributes.ATTACK_DAMAGE, 5.0)
				.add(Attributes.ARMOR, 6.0)
				.add(Attributes.FOLLOW_RANGE, 32.0);
	}

	public boolean isArcher() {
		return this.getMainHandItem().is(Items.BOW) || this.getMainHandItem().is(Items.CROSSBOW);
	}

	private static final net.minecraft.network.syncher.EntityDataAccessor<Integer> DATA_VARIANT =
			net.minecraft.network.syncher.SynchedEntityData.defineId(RealmGuardian.class, net.minecraft.network.syncher.EntityDataSerializers.INT);

	@Override
	protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(DATA_VARIANT, 0);
	}

	public int getVariant() {
		return this.entityData.get(DATA_VARIANT);
	}

	public void setVariant(int v) {
		this.entityData.set(DATA_VARIANT, v);
	}

	/** At night (or in a fight) guards draw their weapons; by day they stroll with crossed arms like villagers. */
	public boolean isOnDuty() {
		return this.getTarget() != null || this.isAggressive() || !this.level().isBrightOutside();
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
		this.goalSelector.addGoal(2, new BowGoal());
		this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.15, true) {
			@Override
			public boolean canUse() {
				return !RealmGuardian.this.isArcher() && super.canUse();
			}
		});
		this.goalSelector.addGoal(4, new ReturnHomeGoal());
		this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.6));
		this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8.0F));
		this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
		this.targetSelector.addGoal(1, new HurtByTargetGoal(this, RealmGuardian.class).setAlertOthers());
		this.targetSelector.addGoal(2, new DefendVillagersGoal());
		this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Mob.class, 5, true, false,
				(e, level) -> e instanceof Enemy && !(e instanceof Creeper)));
	}

	@Override
	public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason,
			@Nullable SpawnGroupData groupData) {
		SpawnGroupData data = super.finalizeSpawn(level, difficulty, reason, groupData);
		this.home = this.blockPosition();
		float roll = this.random.nextFloat();
		if (roll < 0.4F) {
			this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.CROSSBOW));
		} else if (roll < 0.55F) {
			this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_AXE));
		} else {
			this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(this.random.nextFloat() < 0.15F ? Items.DIAMOND_SWORD : Items.IRON_SWORD));
		}
		// some guards wear armor (painted on their uniform texture as well)
		int armor = this.random.nextInt(3);
		this.setVariant(armor);
		if (armor >= 1) this.setItemSlot(EquipmentSlot.CHEST, new ItemStack(armor == 2 ? Items.IRON_CHESTPLATE : Items.CHAINMAIL_CHESTPLATE));
		if (armor == 2) this.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
		for (EquipmentSlot slot : EquipmentSlot.values()) this.setDropChance(slot, 0.0F);
		this.setPersistenceRequired();
		return data;
	}

	@Override
	public void performRangedAttack(LivingEntity target, float power) {
		ItemStack bow = this.getItemInHand(ProjectileUtil.getWeaponHoldingHand(this, Items.BOW));
		ItemStack ammo = new ItemStack(Items.ARROW);
		AbstractArrow arrow = ProjectileUtil.getMobArrow(this, ammo, power, bow);
		arrow.setBaseDamage(3.5 + power * 2.0);
		double xd = target.getX() - this.getX();
		double yd = target.getY(0.3333333333333333) - arrow.getY();
		double zd = target.getZ() - this.getZ();
		double dist = Math.sqrt(xd * xd + zd * zd);
		if (this.level() instanceof ServerLevel serverLevel) {
			Projectile.spawnProjectileUsingShoot(arrow, serverLevel, ammo, xd, yd + dist * 0.2F, zd, 1.7F, 4.0F);
		}
		this.playSound(SoundEvents.ARROW_SHOOT, 1.0F, 1.0F / (this.getRandom().nextFloat() * 0.4F + 0.8F));
	}

	@Override
	public boolean canAttack(LivingEntity target) {
		if (target instanceof AbstractVillager || target instanceof RealmGuardian) return false;
		return super.canAttack(target);
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		if (this.home != null) output.store("Home", BlockPos.CODEC, this.home);
		output.putInt("Variant", this.getVariant());
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		this.home = input.read("Home", BlockPos.CODEC).orElse(null);
		this.setVariant(input.getIntOr("Variant", 0));
	}

	@Override
	public boolean removeWhenFarAway(double distanceToClosestPlayer) {
		return false;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.VILLAGER_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return SoundEvents.VILLAGER_DEATH;
	}

	@Override
	public float getVoicePitch() {
		return 0.75F;
	}

	public InteractionHand weaponHand() {
		return InteractionHand.MAIN_HAND;
	}

	/** Archer behaviour: keep distance, aim and shoot. */
	class BowGoal extends Goal {
		private int cooldown;

		BowGoal() {
			this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
		}

		@Override
		public boolean canUse() {
			LivingEntity t = RealmGuardian.this.getTarget();
			return RealmGuardian.this.isArcher() && t != null && t.isAlive();
		}

		@Override
		public void stop() {
			RealmGuardian.this.setAggressive(false);
		}

		@Override
		public boolean requiresUpdateEveryTick() {
			return true;
		}

		@Override
		public void tick() {
			LivingEntity t = RealmGuardian.this.getTarget();
			if (t == null) return;
			RealmGuardian.this.setAggressive(true);
			double dist = RealmGuardian.this.distanceToSqr(t);
			boolean sees = RealmGuardian.this.getSensing().hasLineOfSight(t);
			if (dist > 14 * 14 || !sees) RealmGuardian.this.getNavigation().moveTo(t, 1.0);
			else if (dist < 5 * 5) {
				net.minecraft.world.phys.Vec3 away = RealmGuardian.this.position().subtract(t.position()).normalize().scale(4);
				RealmGuardian.this.getNavigation().moveTo(RealmGuardian.this.getX() + away.x, RealmGuardian.this.getY(), RealmGuardian.this.getZ() + away.z, 1.1);
			} else RealmGuardian.this.getNavigation().stop();
			RealmGuardian.this.getLookControl().setLookAt(t, 30.0F, 30.0F);
			if (--this.cooldown <= 0 && sees && dist < 16 * 16) {
				RealmGuardian.this.performRangedAttack(t, 1.0F);
				this.cooldown = 25;
			}
		}
	}

	/** Walks back to the village when it strays too far. */
	class ReturnHomeGoal extends Goal {
		ReturnHomeGoal() {
			this.setFlags(EnumSet.of(Goal.Flag.MOVE));
		}

		@Override
		public boolean canUse() {
			return RealmGuardian.this.home != null && RealmGuardian.this.getTarget() == null
					&& RealmGuardian.this.blockPosition().distSqr(RealmGuardian.this.home) > 28 * 28;
		}

		@Override
		public void start() {
			BlockPos h = RealmGuardian.this.home;
			RealmGuardian.this.getNavigation().moveTo(h.getX() + 0.5, h.getY(), h.getZ() + 0.5, 0.8);
		}

		@Override
		public boolean canContinueToUse() {
			return !RealmGuardian.this.getNavigation().isDone() && RealmGuardian.this.getTarget() == null;
		}
	}

	/** Targets whoever hurt a villager nearby, including players. */
	class DefendVillagersGoal extends TargetGoal {
		private LivingEntity attacker;

		DefendVillagersGoal() {
			super(RealmGuardian.this, false, true);
			this.setFlags(EnumSet.of(Goal.Flag.TARGET));
		}

		@Override
		public boolean canUse() {
			if (RealmGuardian.this.tickCount % 10 != 0) return false;
			List<AbstractVillager> villagers = RealmGuardian.this.level().getEntitiesOfClass(AbstractVillager.class,
					new AABB(RealmGuardian.this.blockPosition()).inflate(16.0, 8.0, 16.0));
			for (AbstractVillager v : villagers) {
				LivingEntity a = v.getLastHurtByMob();
				if (a != null && a.isAlive() && !(a instanceof RealmGuardian) && !(a instanceof Player p && p.isCreative())) {
					this.attacker = a;
					return true;
				}
			}
			return false;
		}

		@Override
		public void start() {
			RealmGuardian.this.setTarget(this.attacker);
			super.start();
		}
	}
}
