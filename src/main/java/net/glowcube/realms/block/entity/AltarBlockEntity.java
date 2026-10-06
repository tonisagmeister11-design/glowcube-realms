package net.glowcube.realms.block.entity;

import java.util.Optional;
import java.util.UUID;
import net.glowcube.realms.block.AltarBlock;
import net.glowcube.realms.entity.boss.RealmBoss;
import net.glowcube.realms.registry.ModBlockEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class AltarBlockEntity extends BlockEntity {
	private static final double TRIGGER_RANGE = 14.0;
	private UUID bossId;
	private int tickCounter;

	public AltarBlockEntity(BlockPos pos, BlockState state) {
		super(ModBlockEntities.ALTAR, pos, state);
	}

	public static void serverTick(Level level, BlockPos pos, BlockState state, AltarBlockEntity altar) {
		if (!(level instanceof ServerLevel serverLevel)) return;
		if (++altar.tickCounter % 20 != 0) return;
		if (!state.getValue(AltarBlock.AWAKE)) return;
		ServerPlayer challenger = null;
		for (ServerPlayer player : serverLevel.players()) {
			if (!player.isSpectator() && !player.isCreative() && player.distanceToSqr(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5) < TRIGGER_RANGE * TRIGGER_RANGE) {
				challenger = player;
				break;
			}
		}
		if (challenger != null) {
			altar.summon(serverLevel);
		}
	}

	public boolean hasLivingBoss(ServerLevel level) {
		if (this.bossId == null) return false;
		Entity e = level.getEntity(this.bossId);
		return e != null && e.isAlive();
	}

	/** Summons this altar's boss. Returns false if one is already alive. */
	public boolean summon(ServerLevel level) {
		if (hasLivingBoss(level)) return false;
		BlockState state = this.getBlockState();
		if (!(state.getBlock() instanceof AltarBlock altarBlock)) return false;
		AltarBlock.Boss boss = altarBlock.boss();
		Mob mob = boss.entityType().create(level, EntitySpawnReason.TRIGGERED);
		if (mob == null) return false;
		BlockPos pos = this.getBlockPos();
		mob.snapTo(pos.getX() + 0.5, pos.getY() + 2.0, pos.getZ() + 4.5, 180.0F, 0.0F);
		mob.setPersistenceRequired();
		if (mob instanceof RealmBoss realmBoss) realmBoss.setHome(pos);
		level.addFreshEntity(mob);
		this.bossId = mob.getUUID();
		level.setBlock(pos, state.setValue(AltarBlock.AWAKE, false), 3);
		this.setChanged();

		level.playSound(null, pos, SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 3.0F, 0.8F);
		level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, pos.getX() + 0.5, pos.getY() + 2.0, pos.getZ() + 4.5, 1, 0, 0, 0, 0);
		level.sendParticles(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 2.0, pos.getZ() + 0.5, 120, 2.0, 2.0, 2.0, 0.25);
		Component title = Component.translatable("boss.glowcube_realms." + boss.id + ".awakens").withStyle(ChatFormatting.BOLD, ChatFormatting.GOLD);
		Component subtitle = Component.translatable("boss.glowcube_realms." + boss.id + ".subtitle").withStyle(ChatFormatting.LIGHT_PURPLE);
		for (ServerPlayer player : level.players()) {
			if (player.distanceToSqr(pos.getX(), pos.getY(), pos.getZ()) < 64 * 64) {
				player.connection.send(new ClientboundSetTitleTextPacket(title));
				player.connection.send(new ClientboundSetSubtitleTextPacket(subtitle));
			}
		}
		return true;
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		if (this.bossId != null) output.store("Boss", UUIDUtil.CODEC, this.bossId);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		Optional<UUID> id = input.read("Boss", UUIDUtil.CODEC);
		this.bossId = id.orElse(null);
	}
}
