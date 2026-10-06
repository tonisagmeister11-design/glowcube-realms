package net.glowcube.realms.event;

import java.util.List;
import java.util.Set;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.glowcube.realms.GlowcubeRealms;
import net.glowcube.realms.entity.RealmGuardian;
import net.glowcube.realms.network.ModNetworking;
import net.glowcube.realms.registry.ModEntities;
import net.glowcube.realms.registry.ModItems;
import net.glowcube.realms.world.RealmData;
import net.glowcube.realms.world.RealmDimensions;
import net.glowcube.realms.world.RealmTeleporter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.EmptyLootItem;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.phys.AABB;

/** Server-side gameplay hooks: realm travel by sky and void, armor set bonuses, village guards, loot. */
public final class RealmEvents {
	private static final net.minecraft.tags.TagKey<net.minecraft.world.level.levelgen.structure.Structure> UNUSED_BOSS_ARENAS =
			net.minecraft.tags.TagKey.create(Registries.STRUCTURE, GlowcubeRealms.id("boss_arenas"));
	private static final Set<ResourceKey<LootTable>> INJECT_TARGETS = Set.of(
			BuiltInLootTables.SIMPLE_DUNGEON, BuiltInLootTables.ABANDONED_MINESHAFT, BuiltInLootTables.STRONGHOLD_CORRIDOR,
			BuiltInLootTables.DESERT_PYRAMID, BuiltInLootTables.JUNGLE_TEMPLE, BuiltInLootTables.ANCIENT_CITY,
			BuiltInLootTables.WOODLAND_MANSION, BuiltInLootTables.PILLAGER_OUTPOST, BuiltInLootTables.SHIPWRECK_TREASURE);

	public static void init() {
		ServerLifecycleEvents.SERVER_STARTED.register(RealmData::load);
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> RealmData.unload());
		ServerLifecycleEvents.AFTER_SAVE.register((server, flush, force) -> {
			RealmData data = RealmData.get();
			if (data != null) data.save(false);
		});
		ServerTickEvents.END_SERVER_TICK.register(RealmEvents::tick);
		LootTableEvents.MODIFY.register((key, builder, source, registries) -> {
			if (source.isBuiltin() && INJECT_TARGETS.contains(key)) {
				builder.withPool(LootPool.lootPool()
						.add(LootItem.lootTableItem(ModItems.LUMEN_KEY).setWeight(10))
						.add(LootItem.lootTableItem(ModItems.UMBRAL_KEY).setWeight(10))
						.add(LootItem.lootTableItem(ModItems.LUMEN_COMPASS).setWeight(4))
						.add(LootItem.lootTableItem(ModItems.UMBRAL_COMPASS).setWeight(4))
						.add(LootItem.lootTableItem(ModItems.GLOW_SHARD).setWeight(14))
						.add(LootItem.lootTableItem(ModItems.VOID_SHARD).setWeight(10))
						.add(LootItem.lootTableItem(ModItems.WARDEN_SIGIL).setWeight(2))
						.add(EmptyLootItem.emptyItem().setWeight(45)));
			}
		});
	}

	private static void tick(MinecraftServer server) {
		int t = server.getTickCount();
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			if (t % 5 == 0) travel(server, player);
			if (t % 20 == 0) setBonuses(player);
			if (t % 100 == 0) ModNetworking.sendMarkers(player);
			if (t % 400 == (player.getId() & 31)) guardVillages(player);
			if (t % 600 == (player.getId() * 37) % 600) locateArenas(player);
		}
	}

	/** Fly high enough to reach the Lumen Skies; fall out of them to come home; fall into the void to reach the Umbral Depths. */
	private static void travel(MinecraftServer server, ServerPlayer player) {
		ResourceKey<Level> dim = player.level().dimension();
		if (dim == Level.OVERWORLD && player.getY() > 400) {
			ServerLevel lumen = server.getLevel(RealmDimensions.LUMEN_SKIES);
			if (lumen != null) {
				RealmTeleporter.sendTo(player, lumen, player.getX(), player.getZ(), 240.0, true);
				player.sendOverlayMessage(Component.translatable("message.glowcube_realms.arrived_lumen_skies"));
			}
		} else if (dim == RealmDimensions.LUMEN_SKIES && player.getY() < -24) {
			ServerLevel overworld = server.overworld();
			RealmTeleporter.sendTo(player, overworld, player.getX(), player.getZ(), 330.0, true);
			player.fallDistance = 0;
			player.sendOverlayMessage(Component.translatable("message.glowcube_realms.fell_from_sky"));
		} else if (dim == Level.OVERWORLD && player.getY() < -100) {
			ServerLevel umbral = server.getLevel(RealmDimensions.UMBRAL_DEPTHS);
			if (umbral != null) {
				player.fallDistance = 0;
				RealmTeleporter.sendTo(player, umbral, player.getX(), player.getZ(), null, true);
				player.sendOverlayMessage(Component.translatable("message.glowcube_realms.arrived_umbral_depths"));
			}
		}
	}

	private static boolean wearsSet(ServerPlayer player, Item helmet, Item chest, Item legs, Item boots) {
		return player.getItemBySlot(EquipmentSlot.HEAD).is(helmet) && player.getItemBySlot(EquipmentSlot.CHEST).is(chest)
				&& player.getItemBySlot(EquipmentSlot.LEGS).is(legs) && player.getItemBySlot(EquipmentSlot.FEET).is(boots);
	}

	private static void setBonuses(ServerPlayer player) {
		ResourceKey<Level> dim = player.level().dimension();
		if (wearsSet(player, ModItems.GLOWCRYSTAL_HELMET, ModItems.GLOWCRYSTAL_CHESTPLATE, ModItems.GLOWCRYSTAL_LEGGINGS, ModItems.GLOWCRYSTAL_BOOTS)) {
			player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 260, 0, true, false));
			if (dim == RealmDimensions.LUMEN_SKIES) player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 0, true, false));
			if (player.isShiftKeyDown() && !player.onGround()) player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 40, 0, true, false));
		}
		if (wearsSet(player, ModItems.VOIDSHARD_HELMET, ModItems.VOIDSHARD_CHESTPLATE, ModItems.VOIDSHARD_LEGGINGS, ModItems.VOIDSHARD_BOOTS)) {
			player.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 60, 0, true, false));
			player.removeEffect(MobEffects.DARKNESS);
			if (dim == RealmDimensions.UMBRAL_DEPTHS) player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 60, 0, true, false));
		}
		if (GearEvents.wearsStarmetal(player)) {
			player.addEffect(new MobEffectInstance(MobEffects.JUMP_BOOST, 60, 1, true, false));
			player.addEffect(new MobEffectInstance(MobEffects.SPEED, 60, 0, true, false));
		}
	}

	/** Finds the nearest (possibly not yet generated) boss arena so it shows up on the map before it is discovered. */
	private static final String[][] ARENA_STRUCTURES = {
			// dimension, structure, boss, marker height
			{"glowcube_realms:lumen_skies", "glowkeeper_sanctum", "glowkeeper", "150"},
			{"glowcube_realms:lumen_skies", "storm_aerie", "tempest_drake", "196"},
			{"glowcube_realms:umbral_depths", "umbral_throne", "umbral_tyrant", "40"},
			{"glowcube_realms:umbral_depths", "hollow_crypt", "hollow_king", "70"},
			{"minecraft:overworld", "ember_citadel", "ember_warden", "64"},
			{"minecraft:overworld", "frozen_crypt", "frost_lich", "64"},
			{"minecraft:the_nether", "molten_forge", "infernal_colossus", "64"},
			{"minecraft:the_end", "astral_spire", "void_herald", "64"},
			{"minecraft:overworld", "sculk_sanctuary", "echo_warden", "-40"}};

	private static void locateArenas(ServerPlayer player) {
		if (!(player.level() instanceof ServerLevel level)) return;
		RealmData data = RealmData.get();
		if (data == null) return;
		String dim = level.dimension().identifier().toString();
		var registry = level.registryAccess().lookupOrThrow(Registries.STRUCTURE);
		for (String[] entry : ARENA_STRUCTURES) {
			if (!entry[0].equals(dim)) continue;
			var holder = registry.get(ResourceKey.create(Registries.STRUCTURE, GlowcubeRealms.id(entry[1])));
			if (holder.isEmpty()) continue;
			BlockPos found = level.findNearestMapStructure(net.minecraft.core.HolderSet.direct(holder.get()), player.blockPosition(), 40, false);
			if (found == null) continue;
			data.addArena(dim, new BlockPos(found.getX() + 8, Integer.parseInt(entry[3]), found.getZ() + 8), entry[2]);
		}
	}

	/** Natural villages (counted from their bell) fill up to about 20 villagers, keep an iron golem and get Realm Guardians that defend them. */
	static void guardVillages(ServerPlayer player) {
		if (!(player.level() instanceof ServerLevel level) || level.dimension() != Level.OVERWORLD) return;
		java.util.Optional<BlockPos> bell = level.getPoiManager().findClosest(poi -> poi.is(net.minecraft.world.entity.ai.village.poi.PoiTypes.MEETING),
				player.blockPosition(), 64, net.minecraft.world.entity.ai.village.poi.PoiManager.Occupancy.ANY);
		if (bell.isEmpty()) return;
		AABB area = new AABB(bell.get()).inflate(80, 32, 80);
		List<Villager> villagers = level.getEntitiesOfClass(Villager.class, area);
		if (villagers.size() < 3) return;
		Villager anchor = villagers.get(level.getRandom().nextInt(villagers.size()));
		BlockPos at = anchor.blockPosition().offset(level.getRandom().nextInt(9) - 4, 0, level.getRandom().nextInt(9) - 4);
		at = new BlockPos(at.getX(), level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, at.getX(), at.getZ()), at.getZ());
		if (at.distSqr(player.blockPosition()) < 10 * 10) return;
		// one action per check: villagers first, then the golem, then guards; dead guards are replaced over time like iron golems
		if (villagers.size() < 20) {
			spawn(level, net.minecraft.world.entity.EntityTypes.VILLAGER.create(level, EntitySpawnReason.BREEDING), at);
			return;
		}
		if (level.getEntitiesOfClass(net.minecraft.world.entity.animal.golem.IronGolem.class, area).isEmpty()) {
			spawn(level, net.minecraft.world.entity.EntityTypes.IRON_GOLEM.create(level, EntitySpawnReason.MOB_SUMMONED), at);
			return;
		}
		int guards = level.getEntitiesOfClass(RealmGuardian.class, area).size();
		int wanted = Math.max(4, Math.min(9, 2 + villagers.size() / 3));
		if (guards < wanted) spawn(level, ModEntities.REALM_GUARDIAN.create(level, EntitySpawnReason.EVENT), at);
	}

	private static void spawn(ServerLevel level, net.minecraft.world.entity.Mob mob, BlockPos at) {
		if (mob == null) return;
		mob.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, level.getRandom().nextFloat() * 360, 0);
		mob.finalizeSpawn(level, level.getCurrentDifficultyAt(at), EntitySpawnReason.EVENT, null);
		level.addFreshEntity(mob);
	}

	private RealmEvents() {
	}
}
