package net.glowcube.realms.event;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.glowcube.realms.item.LeftClickAbility;
import net.glowcube.realms.network.LeftClickPayload;
import net.glowcube.realms.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRules;

/** Gameplay hooks for update 2: phoenix revive, graves, storm arrows, area mining, tree felling, left-click abilities. */
public final class GearEvents {
	private static boolean breaking;

	public static void init() {
		ServerLivingEntityEvents.ALLOW_DEATH.register((entity, source, amount) -> {
			if (!(entity instanceof ServerPlayer player)) return true;
			if (tryPhoenix(player)) return false;
			makeGrave(player);
			return true;
		});
		ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
			if (source.is(DamageTypeTags.IS_FALL) && entity instanceof Player p && wearsStarmetal(p)) return false;
			return true;
		});
		ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamage, damageTaken, blocked) -> {
			Entity direct = source.getDirectEntity();
			if (direct != null && direct.entityTags().contains("glowcube_storm") && entity.level() instanceof ServerLevel level) {
				direct.removeTag("glowcube_storm");
				net.glowcube.realms.item.Gear.lightningAt(level, entity.position(), source.getEntity() instanceof ServerPlayer sp ? sp : null);
			}
		});
		PlayerBlockBreakEvents.AFTER.register((level, player, pos, state, blockEntity) -> {
			if (breaking || !(level instanceof ServerLevel server) || player.isShiftKeyDown()) return;
			ItemStack tool = player.getMainHandItem();
			breaking = true;
			try {
				if (tool.is(ModItems.EXCAVATOR_PICKAXE)) excavate(server, player, pos, tool);
				else if (tool.is(ModItems.LUMBER_AXE) && state.is(BlockTags.LOGS)) fellTree(server, player, pos, tool);
			} finally {
				breaking = false;
			}
		});
		net.fabricmc.fabric.api.entity.event.v1.ServerEntityLevelChangeEvents.AFTER_PLAYER_CHANGE_LEVEL.register((player, origin, destination) -> {
			if (destination.dimension() == net.glowcube.realms.world.RealmDimensions.UMBRAL_DEPTHS) {
				player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 1200, 0));
			}
		});
		// opening the Sculk Reliquary wakes the Echo Warden five blocks away
		net.fabricmc.fabric.api.event.player.UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
			if (level instanceof ServerLevel server && !player.isSpectator()
					&& server.getBlockEntity(hit.getBlockPos()) instanceof ChestBlockEntity chest
					&& SCULK_RELIQUARY.equals(chest.getLootTable())) {
				spawnEchoWarden(server, hit.getBlockPos(), player);
			}
			return net.minecraft.world.InteractionResult.PASS;
		});
		ServerPlayNetworking.registerGlobalReceiver(LeftClickPayload.TYPE, (payload, context) -> {
			ServerPlayer player = context.player();
			ItemStack stack = player.getMainHandItem();
			if (stack.getItem() instanceof LeftClickAbility ability) ability.onLeftClick(player, stack);
		});
	}

	public static final net.minecraft.resources.ResourceKey<net.minecraft.world.level.storage.loot.LootTable> SCULK_RELIQUARY = net.minecraft.resources.ResourceKey.create(
			net.minecraft.core.registries.Registries.LOOT_TABLE, net.glowcube.realms.GlowcubeRealms.id("chests/sculk_reliquary"));

	private static void spawnEchoWarden(ServerLevel level, BlockPos chest, Player opener) {
		net.glowcube.realms.entity.boss.EchoWarden boss = net.glowcube.realms.registry.ModEntities.ECHO_WARDEN.create(level,
				net.minecraft.world.entity.EntitySpawnReason.TRIGGERED);
		if (boss == null) return;
		BlockPos spawn = null;
		for (Direction d : new Direction[]{Direction.SOUTH, Direction.NORTH, Direction.EAST, Direction.WEST}) {
			BlockPos p = chest.relative(d, 5);
			for (int dy = 2; dy >= -3 && spawn == null; dy--) {
				BlockPos q = p.above(dy);
				if (level.getBlockState(q.below()).isSolid() && level.getBlockState(q).isAir() && level.getBlockState(q.above()).isAir()
						&& level.getBlockState(q.above(2)).isAir()) spawn = q;
			}
			if (spawn != null) break;
		}
		if (spawn == null) spawn = chest.above();
		boss.snapTo(spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5, 0, 0);
		boss.setHome(chest);
		boss.setTarget(opener);
		boss.setPersistenceRequired();
		level.addFreshEntity(boss);
		level.sendParticles(ParticleTypes.SCULK_SOUL, spawn.getX() + 0.5, spawn.getY() + 1, spawn.getZ() + 0.5, 80, 0.6, 1.2, 0.6, 0.05);
		level.playSound(null, spawn, SoundEvents.WARDEN_EMERGE, SoundSource.HOSTILE, 3.0F, 0.8F);
		for (ServerPlayer p : level.getPlayers(p -> p.distanceToSqr(chest.getX(), chest.getY(), chest.getZ()) < 48 * 48)) {
			p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket(
					Component.translatable("boss.glowcube_realms.echo_warden.awakens").withStyle(ChatFormatting.DARK_AQUA, ChatFormatting.BOLD)));
			p.connection.send(new net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket(
					Component.translatable("boss.glowcube_realms.echo_warden.subtitle").withStyle(ChatFormatting.AQUA)));
		}
	}

	public static boolean wearsStarmetal(Player p) {
		return p.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.STARMETAL_HELMET) && p.getItemBySlot(EquipmentSlot.CHEST).is(ModItems.STARMETAL_CHESTPLATE)
				&& p.getItemBySlot(EquipmentSlot.LEGS).is(ModItems.STARMETAL_LEGGINGS) && p.getItemBySlot(EquipmentSlot.FEET).is(ModItems.STARMETAL_BOOTS);
	}

	/** A Phoenix Feather anywhere in the inventory saves the player once. */
	private static boolean tryPhoenix(ServerPlayer player) {
		Inventory inv = player.getInventory();
		for (int i = 0; i < inv.getContainerSize(); i++) {
			ItemStack s = inv.getItem(i);
			if (!s.is(ModItems.PHOENIX_FEATHER)) continue;
			s.shrink(1);
			player.setHealth(player.getMaxHealth() * 0.5F);
			player.removeAllEffects();
			player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 1));
			player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 800, 0));
			player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 400, 2));
			ServerLevel level = player.level();
			level.sendParticles(ParticleTypes.FLAME, player.getX(), player.getY(1), player.getZ(), 120, 0.6, 1.0, 0.6, 0.15);
			level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, player.getX(), player.getY(1), player.getZ(), 60, 0.5, 1.0, 0.5, 0.4);
			level.playSound(null, player.blockPosition(), SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 1.0F, 1.2F);
			player.sendOverlayMessage(Component.translatable("message.glowcube_realms.phoenix").withStyle(ChatFormatting.GOLD));
			return true;
		}
		return false;
	}

	/** Stores the inventory in chests where the player died, so nothing despawns. */
	private static void makeGrave(ServerPlayer player) {
		ServerLevel level = player.level();
		if (level.getGameRules().get(GameRules.KEEP_INVENTORY)) return;
		Inventory inv = player.getInventory();
		List<Integer> slots = new ArrayList<>();
		for (int i = 0; i < inv.getContainerSize(); i++) if (!inv.getItem(i).isEmpty()) slots.add(i);
		if (slots.isEmpty()) return;
		BlockPos pos = player.blockPosition();
		int y = Math.max(level.getMinY() + 1, Math.min(level.getMaxY() - 2, pos.getY()));
		pos = new BlockPos(pos.getX(), y, pos.getZ());
		for (int i = 0; i < 12 && !(canReplace(level, pos) && canReplace(level, pos.east())); i++) pos = pos.above();
		if (!canReplace(level, pos)) return;
		int chests = slots.size() > 27 ? 2 : 1;
		int moved = 0;
		for (int c = 0; c < chests; c++) {
			BlockPos cp = c == 0 ? pos : pos.east();
			if (!canReplace(level, cp)) break;
			level.setBlock(cp, Blocks.CHEST.defaultBlockState(), 3);
			if (!(level.getBlockEntity(cp) instanceof ChestBlockEntity chest)) break;
			for (int s = 0; s < 27 && moved < slots.size(); s++, moved++) {
				int slot = slots.get(moved);
				chest.setItem(s, inv.getItem(slot).copy());
				inv.setItem(slot, ItemStack.EMPTY);
			}
		}
		if (canReplace(level, pos.above())) level.setBlock(pos.above(), Blocks.SOUL_LANTERN.defaultBlockState(), 3);
		player.sendSystemMessage(Component.translatable("message.glowcube_realms.grave", pos.getX(), pos.getY(), pos.getZ(),
				level.dimension().identifier().toString()).withStyle(ChatFormatting.AQUA));
	}

	private static boolean canReplace(Level level, BlockPos pos) {
		BlockState s = level.getBlockState(pos);
		return s.isAir() || s.canBeReplaced();
	}

	/** 3x3 mining perpendicular to where the player looks. */
	private static void excavate(ServerLevel level, Player player, BlockPos center, ItemStack tool) {
		Direction.Axis axis;
		float pitch = player.getXRot();
		if (Math.abs(pitch) > 50) axis = Direction.Axis.Y;
		else axis = player.getDirection().getAxis();
		for (int a = -1; a <= 1; a++) for (int b = -1; b <= 1; b++) {
			if (a == 0 && b == 0) continue;
			BlockPos p = switch (axis) {
				case Y -> center.offset(a, 0, b);
				case X -> center.offset(0, a, b);
				default -> center.offset(a, b, 0);
			};
			BlockState s = level.getBlockState(p);
			if (s.isAir() || s.getDestroySpeed(level, p) < 0 || !tool.isCorrectToolForDrops(s)) continue;
			level.destroyBlock(p, !player.isCreative(), player, 512);
			if (!player.isCreative()) tool.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
			if (tool.isEmpty()) return;
		}
	}

	/** Breaks a whole tree of connected logs. */
	private static void fellTree(ServerLevel level, Player player, BlockPos start, ItemStack tool) {
		ArrayDeque<BlockPos> queue = new ArrayDeque<>();
		Set<BlockPos> seen = new HashSet<>();
		queue.add(start);
		seen.add(start);
		int broken = 0;
		while (!queue.isEmpty() && broken < 160) {
			BlockPos p = queue.poll();
			for (int dx = -1; dx <= 1; dx++) for (int dy = 0; dy <= 1; dy++) for (int dz = -1; dz <= 1; dz++) {
				BlockPos n = p.offset(dx, dy, dz);
				if (seen.contains(n) || !level.getBlockState(n).is(BlockTags.LOGS)) continue;
				seen.add(n);
				queue.add(n);
				level.destroyBlock(n, !player.isCreative(), player, 512);
				broken++;
				if (!player.isCreative() && broken % 2 == 0) tool.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
				if (tool.isEmpty()) return;
			}
		}
	}

	private GearEvents() {
	}
}
