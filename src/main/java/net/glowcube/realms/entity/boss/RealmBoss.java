package net.glowcube.realms.entity.boss;

import java.util.List;
import net.glowcube.realms.GlowcubeRealms;
import net.glowcube.realms.world.RealmData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Base class for all Glowcube bosses: three phases, phase-colored boss bar, an arena leash,
 * an attack scheduler and a reward chest on death.
 */
public abstract class RealmBoss extends Monster {
	public static final int ROAR = 99;
	private static final boolean DEBUG = System.getProperty("glowcube.selftest") != null;
	private static final EntityDataAccessor<Integer> DATA_PHASE = SynchedEntityData.defineId(RealmBoss.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> DATA_ATTACK = SynchedEntityData.defineId(RealmBoss.class, EntityDataSerializers.INT);

	protected final ServerBossEvent bossEvent;
	protected BlockPos home;
	protected int attackCooldown = 60;
	protected int attackTick;
	protected int roarTicks;
	private int idleTicks;
	/** Client side: ticks since the current attack started, for animation. */
	public int clientAttackTicks;

	protected RealmBoss(EntityType<? extends RealmBoss> type, Level level) {
		super(type, level);
		this.bossEvent = new ServerBossEvent(Mth.createInsecureUUID(this.random), this.getDisplayName(), this.barColor(1), BossEvent.BossBarOverlay.NOTCHED_10);
		this.bossEvent.setDarkenScreen(true);
		this.xpReward = 250;
		this.setPersistenceRequired();
	}

	protected abstract BossEvent.BossBarColor barColor(int phase);

	/** Picks the next attack id (> 0) for the given phase. */
	protected abstract int chooseAttack(int phase, LivingEntity target);

	/** Advances the given attack. Return true when the attack has finished. */
	protected abstract boolean tickAttack(ServerLevel level, int attack, int tick, LivingEntity target);

	protected abstract void onPhaseChange(ServerLevel level, int newPhase);

	protected abstract String bossId();

	protected int cooldownAfterAttack(int phase) {
		return phase == 3 ? 25 : phase == 2 ? 40 : 55;
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(DATA_PHASE, 1);
		builder.define(DATA_ATTACK, 0);
	}

	public int getPhase() {
		return this.entityData.get(DATA_PHASE);
	}

	public int getCurrentAttack() {
		return this.entityData.get(DATA_ATTACK);
	}

	protected void setCurrentAttack(int attack) {
		this.entityData.set(DATA_ATTACK, attack);
	}

	public boolean isRoaring() {
		return this.roarTicks > 0;
	}

	public void setHome(BlockPos pos) {
		this.home = pos.immutable();
	}

	@Override
	public void onSyncedDataUpdated(EntityDataAccessor<?> accessor) {
		super.onSyncedDataUpdated(accessor);
		if (DATA_ATTACK.equals(accessor)) this.clientAttackTicks = 0;
	}

	@Override
	public void tick() {
		super.tick();
		if (this.level().isClientSide()) this.clientAttackTicks++;
	}

	@Override
	protected void customServerAiStep(ServerLevel level) {
		super.customServerAiStep(level);
		this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());
		if (DEBUG && this.tickCount % 100 == 0) GlowcubeRealms.LOGGER.info("[SelfTest] {} at {} hp {} phase {} target {}", this.bossId(), this.blockPosition(), (int) this.getHealth(), this.getPhase(), this.getTarget());

		// phase transitions
		float ratio = this.getHealth() / this.getMaxHealth();
		int wanted = ratio <= 0.33F ? 3 : ratio <= 0.66F ? 2 : 1;
		if (wanted > this.getPhase()) {
			this.entityData.set(DATA_PHASE, wanted);
			this.bossEvent.setColor(this.barColor(wanted));
			this.roarTicks = 40;
			this.setCurrentAttack(ROAR);
			this.attackTick = 0;
			level.playSound(null, this.blockPosition(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 3.0F, 0.7F + wanted * 0.1F);
			level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, this.getX(), this.getY(0.5), this.getZ(), 1, 0, 0, 0, 0);
			for (LivingEntity e : level.getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(8))) {
				Vec3 push = e.position().subtract(this.position()).normalize().scale(1.4);
				e.push(push.x, 0.6, push.z);
				e.needsSync = true;
			}
			this.title(level, Component.translatable("boss.glowcube_realms.phase", wanted).withStyle(ChatFormatting.RED, ChatFormatting.BOLD),
					Component.translatable("boss.glowcube_realms." + this.bossId() + ".phase" + wanted).withStyle(ChatFormatting.GOLD));
			this.onPhaseChange(level, wanted);
		}
		if (this.roarTicks > 0) {
			if (--this.roarTicks == 0 && this.getCurrentAttack() == ROAR) this.setCurrentAttack(0);
			this.getNavigation().stop();
			return;
		}

		// leash to the arena
		if (this.home != null && this.distanceToSqr(Vec3.atCenterOf(this.home)) > 40 * 40) {
			this.teleportTo(this.home.getX() + 0.5, this.home.getY() + 3, this.home.getZ() + 0.5);
			level.sendParticles(ParticleTypes.REVERSE_PORTAL, this.getX(), this.getY(1), this.getZ(), 60, 1, 1, 1, 0.2);
		}

		// reset when nobody fights
		// only regenerate when the arena has really been abandoned for 30 seconds, and slowly
		boolean playersNear = !level.getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(64), p -> !p.isSpectator()).isEmpty();
		if (!playersNear) {
			if (++this.idleTicks > 600 && this.tickCount % 20 == 0) this.heal(this.getMaxHealth() * 0.02F);
		} else {
			this.idleTicks = 0;
		}

		LivingEntity target = this.getTarget();
		int attack = this.getCurrentAttack();
		if (attack != 0) {
			if (target == null || !target.isAlive()) {
				this.setCurrentAttack(0);
				return;
			}
			if (this.tickAttack(level, attack, this.attackTick++, target)) {
				this.setCurrentAttack(0);
				this.attackTick = 0;
				this.attackCooldown = this.cooldownAfterAttack(this.getPhase());
			}
		} else if (target != null && target.isAlive()) {
			if (--this.attackCooldown <= 0) {
				this.attackTick = 0;
				this.setCurrentAttack(this.chooseAttack(this.getPhase(), target));
				if (DEBUG) GlowcubeRealms.LOGGER.info("[SelfTest] {} attack {} (phase {}) vs {}", this.bossId(), this.getCurrentAttack(), this.getPhase(), target.getType().toShortString());
			}
		}
	}

	/** Smoothly flies toward a point circling the anchor (used by flying bosses). */
	protected void hover(Vec3 anchor, double angle, double radius, double height, double speed) {
		Vec3 want = anchor.add(Math.cos(angle) * radius, height, Math.sin(angle) * radius);
		Vec3 delta = want.subtract(this.position());
		if (delta.lengthSqr() < 1.0E-4) return;
		this.setDeltaMovement(this.getDeltaMovement().scale(0.85).add(delta.normalize().scale(Math.min(delta.length(), 1.0) * speed)));
	}

	/** Spawns up to {@code count} minions of a type around a position, capped by how many already exist nearby. */
	protected <M extends net.minecraft.world.entity.Mob> void summonMinions(ServerLevel level, EntityType<M> type, int count, int cap, LivingEntity target, double radius) {
		int existing = level.getEntitiesOfClass(net.minecraft.world.entity.Mob.class, this.getBoundingBox().inflate(32), e -> e.getType() == type).size();
		for (int i = 0; i < count && existing + i < cap; i++) {
			M mob = type.create(level, net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED);
			if (mob == null) continue;
			double a = this.random.nextDouble() * Math.PI * 2;
			Vec3 p = Glowkeeper.groundBelow(level, this.position().add(Math.cos(a) * radius, 2, Math.sin(a) * radius));
			mob.snapTo(p.x, p.y, p.z, this.random.nextFloat() * 360, 0);
			mob.setTarget(target);
			level.addFreshEntity(mob);
			level.sendParticles(ParticleTypes.POOF, p.x, p.y + 0.5, p.z, 12, 0.3, 0.5, 0.3, 0.02);
		}
	}

	protected void title(ServerLevel level, Component title, Component subtitle) {
		for (ServerPlayer p : level.getPlayers(p -> p.distanceToSqr(this) < 64 * 64)) {
			p.connection.send(new ClientboundSetTitleTextPacket(title));
			p.connection.send(new ClientboundSetSubtitleTextPacket(subtitle));
		}
	}

	protected List<Player> fighters(ServerLevel level, double radius) {
		return level.getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(radius), p -> !p.isSpectator() && !p.isCreative() && p.isAlive());
	}

	/** Damages every non-boss living entity in a sphere. */
	protected void areaDamage(ServerLevel level, Vec3 center, double radius, float damage, double knock, MobEffectInstance effect) {
		AABB box = new AABB(center, center).inflate(radius);
		for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box, e -> e != this && !(e instanceof RealmBoss) && e.isAlive())) {
			if (e.position().distanceTo(center) > radius + 0.5) continue;
			if (e instanceof Monster && !(e instanceof Player)) continue;
			e.hurtServer(level, this.damageSources().mobAttack(this), damage);
			if (knock > 0) {
				Vec3 push = e.position().subtract(center);
				push = new Vec3(push.x, 0, push.z);
				if (push.lengthSqr() < 1.0E-4) push = new Vec3(0.1, 0, 0);
				push = push.normalize().scale(knock);
				e.push(push.x, knock * 0.5, push.z);
				e.needsSync = true;
			}
			if (effect != null) e.addEffect(new MobEffectInstance(effect));
		}
	}

	@Override
	public void startSeenByPlayer(ServerPlayer player) {
		super.startSeenByPlayer(player);
		this.bossEvent.addPlayer(player);
	}

	@Override
	public void stopSeenByPlayer(ServerPlayer player) {
		super.stopSeenByPlayer(player);
		this.bossEvent.removePlayer(player);
	}

	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
		if (this.roarTicks > 0) return false;
		Entity attacker = source.getEntity();
		if (attacker instanceof RealmBoss) return false;
		return super.hurtServer(level, source, damage);
	}

	@Override
	public void die(DamageSource source) {
		super.die(source);
		if (this.level() instanceof ServerLevel level) {
			RealmData data = RealmData.get();
			BlockPos anchor = this.home != null ? this.home : this.blockPosition();
			if (data != null) data.markDefeated(level.dimension().identifier().toString(), anchor);
			this.spawnRewardChest(level, anchor);
			this.title(level, Component.translatable("boss.glowcube_realms." + this.bossId() + ".defeated").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
					Component.translatable("boss.glowcube_realms.reward").withStyle(ChatFormatting.AQUA));
			level.playSound(null, this.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.HOSTILE, 2.0F, 1.0F);
		}
	}

	private void spawnRewardChest(ServerLevel level, BlockPos anchor) {
		BlockPos pos = anchor.above();
		for (int i = 0; i < 6 && !level.getBlockState(pos).isAir(); i++) pos = pos.above();
		BlockState chest = Blocks.CHEST.defaultBlockState();
		level.setBlock(pos, chest, 3);
		if (level.getBlockEntity(pos) instanceof ChestBlockEntity be) {
			ResourceKey<LootTable> table = ResourceKey.create(Registries.LOOT_TABLE, GlowcubeRealms.id("chests/" + this.bossId() + "_reward"));
			be.setLootTable(table, level.getRandom().nextLong());
		}
		level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, 80, 0.5, 1.0, 0.5, 0.5);
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putInt("Phase", this.getPhase());
		if (this.home != null) output.store("Home", BlockPos.CODEC, this.home);
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		this.entityData.set(DATA_PHASE, input.getIntOr("Phase", 1));
		this.home = input.read("Home", BlockPos.CODEC).orElse(null);
		if (this.hasCustomName()) this.bossEvent.setName(this.getDisplayName());
		this.bossEvent.setColor(this.barColor(this.getPhase()));
	}

	@Override
	public void checkDespawn() {
	}

	@Override
	public boolean removeWhenFarAway(double distanceToClosestPlayer) {
		return false;
	}

	@Override
	public boolean canUsePortal(boolean ignorePassenger) {
		return false;
	}

	@Override
	protected boolean canRide(Entity vehicle) {
		return false;
	}

	@Override
	public boolean causeFallDamage(double fallDistance, float damageModifier, DamageSource source) {
		return false;
	}
}
