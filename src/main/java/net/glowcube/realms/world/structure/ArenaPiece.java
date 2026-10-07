package net.glowcube.realms.world.structure;

import net.glowcube.realms.GlowcubeRealms;
import net.glowcube.realms.block.AltarBlock;
import net.glowcube.realms.registry.ModBlocks;
import net.glowcube.realms.registry.ModWorldgen;
import net.glowcube.realms.world.RealmData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.storage.loot.LootTable;

/** One big procedural piece. Every chunk only places the blocks inside its own bounding box. */
public class ArenaPiece extends StructurePiece {
	private static final int R = 18;
	private final String kind;
	private final BlockPos center;

	public ArenaPiece(String kind, BlockPos center) {
		super(ModWorldgen.ARENA_PIECE, 0, new BoundingBox(center.getX() - R - 3, center.getY() - 24, center.getZ() - R - 3,
				// the sculk sanctuary reaches up to the surface with its shaft and marker
				center.getX() + R + 3, center.getY() + (kind.equals("echo_warden") ? 380 : 26), center.getZ() + R + 3));
		this.kind = kind;
		this.center = center;
	}

	public ArenaPiece(CompoundTag tag) {
		super(ModWorldgen.ARENA_PIECE, tag);
		this.kind = tag.getStringOr("Kind", "shrine");
		this.center = new BlockPos(tag.getIntOr("CX", 0), tag.getIntOr("CY", 64), tag.getIntOr("CZ", 0));
	}

	@Override
	protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
		tag.putString("Kind", this.kind);
		tag.putInt("CX", this.center.getX());
		tag.putInt("CY", this.center.getY());
		tag.putInt("CZ", this.center.getZ());
	}

	@Override
	public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator, RandomSource random,
			BoundingBox chunkBB, ChunkPos chunkPos, BlockPos referencePos) {
		switch (this.kind) {
			case "glowkeeper" -> this.buildSanctum(level, chunkBB, random);
			case "umbral_tyrant" -> this.buildThrone(level, chunkBB, random);
			case "ember_warden" -> this.buildCitadel(level, chunkBB, random);
			case "infernal_colossus" -> this.buildDome(level, chunkBB, random, Blocks.BLACKSTONE.defaultBlockState(), Blocks.NETHER_BRICKS.defaultBlockState(),
					Blocks.CHISELED_NETHER_BRICKS.defaultBlockState(), Blocks.BASALT.defaultBlockState(), Blocks.SHROOMLIGHT.defaultBlockState(), true,
					ModBlocks.COLOSSUS_ALTAR.defaultBlockState(), "molten_forge");
			case "hollow_king" -> this.buildDome(level, chunkBB, random, ModBlocks.UMBRAL_STONE.defaultBlockState(), Blocks.DEEPSLATE_TILES.defaultBlockState(),
					Blocks.CHISELED_DEEPSLATE.defaultBlockState(), Blocks.BONE_BLOCK.defaultBlockState(), Blocks.SOUL_LANTERN.defaultBlockState(), false,
					ModBlocks.KING_ALTAR.defaultBlockState(), "hollow_crypt");
			case "void_herald" -> this.buildSpire(level, chunkBB, random);
			case "echo_warden" -> this.buildSculkSanctuary(level, chunkBB, random);
			case "frost_lich" -> this.buildFrozenCrypt(level, chunkBB, random);
			case "tempest_drake" -> this.buildAerie(level, chunkBB, random);
			default -> this.buildShrine(level, chunkBB, random);
		}
		if (!this.kind.equals("shrine") && chunkBB.isInside(this.center)) {
			MinecraftServer server = level.getLevel().getServer();
			String dim = level.getLevel().dimension().identifier().toString();
			BlockPos c = this.center;
			String boss = this.kind;
			server.execute(() -> {
				RealmData data = RealmData.get();
				if (data != null) data.addArena(dim, c, boss);
			});
		}
	}

	// ------------------------------------------------------------------ helpers
	private void set(WorldGenLevel level, BoundingBox bb, int dx, int dy, int dz, BlockState state) {
		BlockPos p = this.center.offset(dx, dy, dz);
		if (bb.isInside(p)) level.setBlock(p, state, 2);
	}

	private BlockState get(WorldGenLevel level, BoundingBox bb, int dx, int dy, int dz) {
		BlockPos p = this.center.offset(dx, dy, dz);
		return bb.isInside(p) ? level.getBlockState(p) : Blocks.AIR.defaultBlockState();
	}

	private void chest(WorldGenLevel level, BoundingBox bb, RandomSource random, int dx, int dy, int dz, String table) {
		BlockPos p = this.center.offset(dx, dy, dz);
		if (!bb.isInside(p)) return;
		level.setBlock(p, Blocks.CHEST.defaultBlockState(), 2);
		ResourceKey<LootTable> key = ResourceKey.create(Registries.LOOT_TABLE, GlowcubeRealms.id("chests/" + table));
		RandomizableContainer.setBlockEntityLootTable(level, random, p, key);
	}

	private void altar(WorldGenLevel level, BoundingBox bb, BlockState altar) {
		this.set(level, bb, 0, 1, 0, altar.setValue(AltarBlock.AWAKE, true));
	}

	private static double noise(int x, int z) {
		long h = x * 3129871L ^ z * 116129781L;
		h = h * h * 42317861L + h * 11L;
		return ((h >> 16) & 0xFFFF) / 65535.0;
	}

	// ------------------------------------------------------------------ Glowkeeper Sanctum (Lumen Skies)
	private void buildSanctum(WorldGenLevel level, BoundingBox bb, RandomSource random) {
		BlockState stone = ModBlocks.SKYSTONE.defaultBlockState();
		BlockState bricks = ModBlocks.SKYSTONE_BRICKS.defaultBlockState();
		BlockState chiseled = ModBlocks.CHISELED_SKYSTONE.defaultBlockState();
		BlockState crystal = ModBlocks.GLOWCRYSTAL_BLOCK.defaultBlockState();
		BlockState grass = ModBlocks.LUMEN_GRASS.defaultBlockState();
		BlockState soil = ModBlocks.LUMEN_SOIL.defaultBlockState();
		for (int dx = -R; dx <= R; dx++) for (int dz = -R; dz <= R; dz++) {
			double d = Math.hypot(dx, dz);
			if (d > R + 0.5) continue;
			int wx = this.center.getX() + dx, wz = this.center.getZ() + dz;
			// clear the sky above the platform
			for (int dy = 1; dy <= 22; dy++) this.set(level, bb, dx, dy, dz, Blocks.AIR.defaultBlockState());
			// floor
			BlockState floor;
			if (d < 2.5) floor = chiseled;
			else if (d > R - 2) floor = grass;
			else if (((int) d) % 4 == 0) floor = bricks;
			else if (Math.abs(dx) == Math.abs(dz) && d < 12) floor = chiseled;
			else floor = stone;
			this.set(level, bb, dx, 0, dz, floor);
			// inverted cone underneath
			int depth = (int) ((R - d) * 1.05 + noise(wx, wz) * 4) + 2;
			for (int dy = 1; dy <= depth; dy++) {
				BlockState s = dy <= 2 && d > R - 3 ? soil : stone;
				if (dy > 3 && noise(wx + dy, wz - dy) > 0.93) s = ModBlocks.GLOWCRYSTAL_ORE.defaultBlockState();
				this.set(level, bb, dx, -dy, dz, s);
			}
		}
		// light ring in the floor
		for (int i = 0; i < 8; i++) {
			double a = i * Math.PI / 4;
			this.set(level, bb, (int) Math.round(Math.cos(a) * 8), 0, (int) Math.round(Math.sin(a) * 8), crystal);
		}
		// pillars with crystal crowns
		for (int i = 0; i < 8; i++) {
			double a = i * Math.PI / 4 + Math.PI / 8;
			int px = (int) Math.round(Math.cos(a) * 13), pz = (int) Math.round(Math.sin(a) * 13);
			int h = 7 + (i % 2) * 3;
			for (int dy = 1; dy <= h; dy++) {
				for (int ox = 0; ox <= 1; ox++) for (int oz = 0; oz <= 1; oz++) {
					this.set(level, bb, px + ox, dy, pz + oz, dy == h ? chiseled : bricks);
				}
			}
			this.set(level, bb, px, h + 1, pz, crystal);
			this.set(level, bb, px + 1, h + 1, pz + 1, crystal);
			this.set(level, bb, px, h + 2, pz, crystal);
		}
		this.altar(level, bb, ModBlocks.GLOWKEEPER_ALTAR.defaultBlockState());
		this.chest(level, bb, random, -3, 1, -10, "glowkeeper_sanctum");
		this.chest(level, bb, random, 3, 1, -10, "glowkeeper_sanctum");
	}

	// ------------------------------------------------------------------ Umbral Throne (Umbral Depths)
	private void buildThrone(WorldGenLevel level, BoundingBox bb, RandomSource random) {
		BlockState stone = ModBlocks.UMBRAL_STONE.defaultBlockState();
		BlockState bricks = ModBlocks.UMBRAL_BRICKS.defaultBlockState();
		BlockState chiseled = ModBlocks.CHISELED_UMBRAL_STONE.defaultBlockState();
		BlockState voidBlock = ModBlocks.VOIDSHARD_BLOCK.defaultBlockState();
		BlockState cap = ModBlocks.SHADECAP_BLOCK.defaultBlockState();
		BlockState lava = Blocks.LAVA.defaultBlockState();
		for (int dx = -R - 2; dx <= R + 2; dx++) for (int dz = -R - 2; dz <= R + 2; dz++) {
			double d = Math.hypot(dx, dz);
			for (int dy = -3; dy <= 20; dy++) {
				double d3 = Math.sqrt(dx * dx + dz * dz + (dy * 1.25) * (dy * 1.25));
				if (dy > 0 && d3 < R) this.set(level, bb, dx, dy, dz, Blocks.AIR.defaultBlockState());
				else if (dy > 0 && d3 < R + 1.5) this.set(level, bb, dx, dy, dz, stone);
			}
			if (d > R + 1.5) continue;
			boolean moat = d > 14 && d < 15.6 && Math.abs(dx) > 2 && Math.abs(dz) > 2;
			BlockState floor = d < 2.5 ? chiseled : moat ? lava : (((int) d) % 3 == 0 ? chiseled : bricks);
			this.set(level, bb, dx, 0, dz, floor);
			for (int dy = 1; dy <= 3; dy++) this.set(level, bb, dx, -dy, dz, stone);
		}
		for (int i = 0; i < 6; i++) {
			double a = i * Math.PI / 3;
			int px = (int) Math.round(Math.cos(a) * 10), pz = (int) Math.round(Math.sin(a) * 10);
			for (int dy = 1; dy <= 14; dy++) {
				if (!this.get(level, bb, px, dy, pz).isAir() && dy > 3) break;
				this.set(level, bb, px, dy, pz, dy % 5 == 0 ? cap : voidBlock);
			}
		}
		// throne behind the altar
		for (int dx = -2; dx <= 2; dx++) for (int dy = 1; dy <= 5; dy++) {
			if (Math.abs(dx) == 2 || dy == 5) this.set(level, bb, dx, dy, -5, chiseled);
			else if (dy == 1) this.set(level, bb, dx, dy, -5, bricks);
		}
		this.altar(level, bb, ModBlocks.TYRANT_ALTAR.defaultBlockState());
		this.chest(level, bb, random, -4, 1, -8, "umbral_throne");
		this.chest(level, bb, random, 4, 1, -8, "umbral_throne");
	}

	// ------------------------------------------------------------------ Ember Citadel (Overworld)
	private void buildCitadel(WorldGenLevel level, BoundingBox bb, RandomSource random) {
		int r = 15;
		BlockState floorA = Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
		BlockState floorB = Blocks.BLACKSTONE.defaultBlockState();
		BlockState wall = Blocks.NETHER_BRICKS.defaultBlockState();
		BlockState tower = Blocks.POLISHED_BLACKSTONE.defaultBlockState();
		for (int dx = -r - 3; dx <= r + 3; dx++) for (int dz = -r - 3; dz <= r + 3; dz++) {
			double d = Math.hypot(dx, dz);
			if (d > r + 3) continue;
			for (int dy = 1; dy <= 18; dy++) this.set(level, bb, dx, dy, dz, Blocks.AIR.defaultBlockState());
			if (d > r + 1.5) continue;
			this.set(level, bb, dx, 0, dz, d < 2.5 ? Blocks.CHISELED_POLISHED_BLACKSTONE.defaultBlockState()
					: (d > 6.5 && d < 7.5) ? Blocks.MAGMA_BLOCK.defaultBlockState() : ((dx + dz) % 2 == 0 ? floorA : floorB));
			for (int dy = 1; dy <= 14; dy++) {
				BlockState below = this.get(level, bb, dx, -dy, dz);
				if (below.isSolid() && dy > 1) break;
				this.set(level, bb, dx, -dy, dz, Blocks.BLACKSTONE.defaultBlockState());
			}
			boolean gate = dz > 0 && Math.abs(dx) <= 2;
			if (d >= r - 0.5 && d <= r + 1.2 && !gate) {
				for (int dy = 1; dy <= 7; dy++) this.set(level, bb, dx, dy, dz, wall);
				if ((dx + dz) % 2 == 0) this.set(level, bb, dx, 8, dz, Blocks.NETHER_BRICK_FENCE.defaultBlockState());
			}
		}
		for (int i = 0; i < 4; i++) {
			double a = i * Math.PI / 2 + Math.PI / 4;
			int tx = (int) Math.round(Math.cos(a) * r), tz = (int) Math.round(Math.sin(a) * r);
			for (int ox = -2; ox <= 2; ox++) for (int oz = -2; oz <= 2; oz++) {
				if (ox * ox + oz * oz > 5) continue;
				for (int dy = 1; dy <= 12; dy++) this.set(level, bb, tx + ox, dy, tz + oz, tower);
			}
			this.set(level, bb, tx, 13, tz, Blocks.NETHERRACK.defaultBlockState());
			this.set(level, bb, tx, 14, tz, Blocks.FIRE.defaultBlockState());
		}
		for (int i = 0; i < 4; i++) {
			double a = i * Math.PI / 2;
			int bx = (int) Math.round(Math.cos(a) * 10), bz = (int) Math.round(Math.sin(a) * 10);
			this.set(level, bb, bx, 1, bz, Blocks.NETHERRACK.defaultBlockState());
			this.set(level, bb, bx, 2, bz, Blocks.FIRE.defaultBlockState());
		}
		this.altar(level, bb, ModBlocks.WARDEN_ALTAR.defaultBlockState());
		this.chest(level, bb, random, -3, 1, -11, "ember_citadel");
		this.chest(level, bb, random, 3, 1, -11, "ember_citadel");
	}

	// ------------------------------------------------------------------ generic carved dome (Molten Forge, Hollow Crypt)
	private void buildDome(WorldGenLevel level, BoundingBox bb, RandomSource random, BlockState shell, BlockState floor, BlockState accent,
			BlockState pillar, BlockState light, boolean lavaMoat, BlockState altar, String chestTable) {
		int r = 16;
		for (int dx = -r - 2; dx <= r + 2; dx++) for (int dz = -r - 2; dz <= r + 2; dz++) {
			double d = Math.hypot(dx, dz);
			for (int dy = -3; dy <= 18; dy++) {
				double d3 = Math.sqrt(dx * dx + dz * dz + (dy * 1.3) * (dy * 1.3));
				if (dy > 0 && d3 < r) this.set(level, bb, dx, dy, dz, Blocks.AIR.defaultBlockState());
				else if (dy > 0 && d3 < r + 1.5) this.set(level, bb, dx, dy, dz, shell);
			}
			if (d > r + 1.5) continue;
			boolean moat = lavaMoat && d > 12.5 && d < 14 && Math.abs(dx) > 2 && Math.abs(dz) > 2;
			this.set(level, bb, dx, 0, dz, d < 2.5 ? accent : moat ? Blocks.LAVA.defaultBlockState() : (((int) d) % 4 == 0 ? accent : floor));
			for (int dy = 1; dy <= 3; dy++) this.set(level, bb, dx, -dy, dz, shell);
		}
		for (int i = 0; i < 8; i++) {
			double a = i * Math.PI / 4;
			int px = (int) Math.round(Math.cos(a) * 9), pz = (int) Math.round(Math.sin(a) * 9);
			for (int dy = 1; dy <= 12; dy++) {
				if (dy > 3 && !this.get(level, bb, px, dy, pz).isAir()) break;
				this.set(level, bb, px, dy, pz, dy % 4 == 0 ? light : pillar);
			}
		}
		this.altar(level, bb, altar);
		this.chest(level, bb, random, -3, 1, -7, chestTable);
		this.chest(level, bb, random, 3, 1, -7, chestTable);
	}

	// ------------------------------------------------------------------ Astral Spire (End)
	private void buildSpire(WorldGenLevel level, BoundingBox bb, RandomSource random) {
		int r = 13;
		BlockState bricks = Blocks.END_STONE_BRICKS.defaultBlockState();
		BlockState purpur = Blocks.PURPUR_BLOCK.defaultBlockState();
		BlockState pillar = Blocks.PURPUR_PILLAR.defaultBlockState();
		for (int dx = -r; dx <= r; dx++) for (int dz = -r; dz <= r; dz++) {
			double d = Math.hypot(dx, dz);
			if (d > r + 0.5) continue;
			for (int dy = 1; dy <= 16; dy++) this.set(level, bb, dx, dy, dz, Blocks.AIR.defaultBlockState());
			this.set(level, bb, dx, 0, dz, d < 3 ? purpur : (((int) d) % 3 == 0 ? purpur : bricks));
			int depth = (int) ((r - d) * 0.8) + 2;
			for (int dy = 1; dy <= depth; dy++) this.set(level, bb, dx, -dy, dz, Blocks.END_STONE.defaultBlockState());
		}
		for (int i = 0; i < 6; i++) {
			double a = i * Math.PI / 3;
			int px = (int) Math.round(Math.cos(a) * 10), pz = (int) Math.round(Math.sin(a) * 10);
			int h = 6 + (i % 2) * 4;
			for (int dy = 1; dy <= h; dy++) this.set(level, bb, px, dy, pz, dy == h ? Blocks.END_ROD.defaultBlockState() : pillar);
			this.set(level, bb, px, h + 1, pz, Blocks.CRYING_OBSIDIAN.defaultBlockState());
		}
		for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) this.set(level, bb, dx, 0, dz, Blocks.OBSIDIAN.defaultBlockState());
		this.altar(level, bb, ModBlocks.HERALD_ALTAR.defaultBlockState());
		this.chest(level, bb, random, -3, 1, -8, "astral_spire");
		this.chest(level, bb, random, 3, 1, -8, "astral_spire");
	}

	// ------------------------------------------------------------------ Frozen Crypt (snowy Overworld)
	private void buildFrozenCrypt(WorldGenLevel level, BoundingBox bb, RandomSource random) {
		int r = 13;
		BlockState packed = Blocks.PACKED_ICE.defaultBlockState();
		BlockState blue = Blocks.BLUE_ICE.defaultBlockState();
		BlockState bricks = Blocks.STONE_BRICKS.defaultBlockState();
		for (int dx = -r - 2; dx <= r + 2; dx++) for (int dz = -r - 2; dz <= r + 2; dz++) {
			double d = Math.hypot(dx, dz);
			if (d > r + 2) continue;
			for (int dy = 1; dy <= 14; dy++) this.set(level, bb, dx, dy, dz, Blocks.AIR.defaultBlockState());
			if (d > r + 0.5) continue;
			this.set(level, bb, dx, 0, dz, d < 2.5 ? blue : (((dx + dz) & 3) == 0 ? packed : Blocks.SNOW_BLOCK.defaultBlockState()));
			for (int dy = 1; dy <= 10; dy++) {
				if (this.get(level, bb, dx, -dy, dz).isSolid() && dy > 1) break;
				this.set(level, bb, dx, -dy, dz, bricks);
			}
			if (d > r - 1 && !(dz > 0 && Math.abs(dx) <= 2)) {
				for (int dy = 1; dy <= 5; dy++) this.set(level, bb, dx, dy, dz, dy == 5 ? Blocks.SNOW_BLOCK.defaultBlockState() : packed);
			}
		}
		for (int i = 0; i < 6; i++) {
			double a = i * Math.PI / 3 + 0.3;
			int px = (int) Math.round(Math.cos(a) * 8), pz = (int) Math.round(Math.sin(a) * 8);
			int h = 5 + (int) (noise(px, pz) * 6);
			for (int dy = 1; dy <= h; dy++) this.set(level, bb, px, dy, pz, dy > h - 2 ? blue : packed);
			this.set(level, bb, px, h + 1, pz, Blocks.SEA_LANTERN.defaultBlockState());
		}
		this.altar(level, bb, ModBlocks.LICH_ALTAR.defaultBlockState());
		this.chest(level, bb, random, -3, 1, -8, "frozen_crypt");
		this.chest(level, bb, random, 3, 1, -8, "frozen_crypt");
	}

	// ------------------------------------------------------------------ Storm Aerie (high Lumen Skies)
	private void buildAerie(WorldGenLevel level, BoundingBox bb, RandomSource random) {
		int r = 15;
		BlockState log = ModBlocks.AURORA_LOG.defaultBlockState();
		BlockState leaves = ModBlocks.AURORA_LEAVES.defaultBlockState().setValue(net.minecraft.world.level.block.LeavesBlock.PERSISTENT, true);
		BlockState stone = ModBlocks.SKYSTONE.defaultBlockState();
		for (int dx = -r; dx <= r; dx++) for (int dz = -r; dz <= r; dz++) {
			double d = Math.hypot(dx, dz);
			if (d > r + 0.5) continue;
			for (int dy = 1; dy <= 18; dy++) this.set(level, bb, dx, dy, dz, Blocks.AIR.defaultBlockState());
			this.set(level, bb, dx, 0, dz, d < 3 ? ModBlocks.CHISELED_SKYSTONE.defaultBlockState() : d > r - 3 ? log : ModBlocks.AURORA_PLANKS.defaultBlockState());
			// woven nest rim
			if (d > r - 2.5) {
				int h = 1 + (int) (noise(dx * 3, dz * 5) * 3);
				for (int dy = 1; dy <= h; dy++) this.set(level, bb, dx, dy, dz, (dx + dz + dy) % 3 == 0 ? leaves : log);
			}
			int depth = (int) ((r - d) * 0.9 + noise(dx, dz) * 3) + 1;
			for (int dy = 1; dy <= depth; dy++) this.set(level, bb, dx, -dy, dz, dy < 3 ? ModBlocks.LUMEN_SOIL.defaultBlockState() : stone);
		}
		for (int i = 0; i < 5; i++) {
			double a = i * Math.PI * 2 / 5;
			int px = (int) Math.round(Math.cos(a) * 9), pz = (int) Math.round(Math.sin(a) * 9);
			for (int dy = 1; dy <= 4 + i % 3; dy++) this.set(level, bb, px, dy, pz, ModBlocks.GLOWCRYSTAL_BLOCK.defaultBlockState());
		}
		this.altar(level, bb, ModBlocks.DRAKE_ALTAR.defaultBlockState());
		this.chest(level, bb, random, -3, 1, -9, "storm_aerie");
		this.chest(level, bb, random, 3, 1, -9, "storm_aerie");
	}

	// ------------------------------------------------------------------ Sculk Sanctuary (Deep Dark)
	private void buildSculkSanctuary(WorldGenLevel level, BoundingBox bb, RandomSource random) {
		int r = 13;
		BlockState tiles = Blocks.DEEPSLATE_TILES.defaultBlockState();
		BlockState bricks = Blocks.DEEPSLATE_BRICKS.defaultBlockState();
		BlockState sculk = Blocks.SCULK.defaultBlockState();
		BlockState frame = Blocks.REINFORCED_DEEPSLATE.defaultBlockState();
		for (int dx = -r - 1; dx <= r + 1; dx++) for (int dz = -r - 1; dz <= r + 1; dz++) {
			double d = Math.hypot(dx, dz);
			for (int dy = -2; dy <= 14; dy++) {
				double d3 = Math.sqrt(dx * dx + dz * dz + (dy * 1.15) * (dy * 1.15));
				if (dy > 0 && d3 < r) this.set(level, bb, dx, dy, dz, Blocks.AIR.defaultBlockState());
				else if (dy > 0 && d3 < r + 1.3) {
					// glowing echo crystal ribs make the dome visible from far away in the dark caves
					boolean rib = Math.abs(dx) <= 0 || Math.abs(dz) <= 0 || Math.abs(Math.abs(dx) - Math.abs(dz)) <= 0;
					this.set(level, bb, dx, dy, dz, rib ? net.glowcube.realms.registry.ModBlocks.ECHO_CRYSTAL_BLOCK.defaultBlockState()
							: noise(dx * 7 + dy, dz * 5) > 0.6 ? sculk : bricks);
				}
			}
			if (d > r + 1) continue;
			this.set(level, bb, dx, 0, dz, d < 2 ? Blocks.CHISELED_DEEPSLATE.defaultBlockState() : noise(dx, dz) > 0.55 ? sculk : tiles);
			for (int dy = 1; dy <= 2; dy++) this.set(level, bb, dx, -dy, dz, bricks);
		}
		// tunnels so the sanctuary connects to the surrounding caves
		for (int sign : new int[]{-1, 1}) {
			for (int t = r - 1; t <= r + 22; t++) {
				for (int w = -1; w <= 1; w++) for (int dy = 1; dy <= 3; dy++) this.set(level, bb, sign * t, dy, w, Blocks.AIR.defaultBlockState());
				this.set(level, bb, sign * t, 0, 0, tiles);
				if (t % 4 == 0) this.set(level, bb, sign * t, 3, 1, Blocks.SOUL_LANTERN.defaultBlockState());
			}
		}
		// the sculk gate: reinforced deepslate frame with the keyhole in the middle of the bottom row
		for (int dx = -2; dx <= 2; dx++) for (int dy = 1; dy <= 6; dy++) {
			boolean edge = Math.abs(dx) == 2 || dy == 1 || dy == 6;
			this.set(level, bb, dx, dy, -5, edge ? frame : Blocks.AIR.defaultBlockState());
		}
		this.set(level, bb, 0, 1, -5, net.glowcube.realms.registry.ModBlocks.SCULK_KEYHOLE.defaultBlockState());
		for (int dx = -3; dx <= 3; dx++) this.set(level, bb, dx, 0, -5, Blocks.CHISELED_DEEPSLATE.defaultBlockState());
		// glowing pillars and sculk decorations
		for (int[] p : new int[][]{{-5, -2}, {5, -2}, {-4, 4}, {4, 4}}) {
			for (int dy = 1; dy <= 4; dy++) this.set(level, bb, p[0], dy, p[1], dy == 4 ? Blocks.SCULK_CATALYST.defaultBlockState() : Blocks.POLISHED_DEEPSLATE_WALL.defaultBlockState());
			this.set(level, bb, p[0], 5, p[1], Blocks.SOUL_LANTERN.defaultBlockState());
		}
		this.set(level, bb, -2, 1, 3, Blocks.SCULK_SHRIEKER.defaultBlockState());
		this.set(level, bb, 2, 1, 3, Blocks.SCULK_SHRIEKER.defaultBlockState());
		this.set(level, bb, -1, 1, 4, Blocks.SCULK_SENSOR.defaultBlockState());
		this.set(level, bb, 1, 1, 4, Blocks.SCULK_SENSOR.defaultBlockState());
		this.set(level, bb, 0, 0, 3, Blocks.CHISELED_DEEPSLATE.defaultBlockState());
		this.chest(level, bb, random, 0, 1, 3, "sculk_reliquary");
		this.sanctuaryBeacon(level, bb);
	}

	/**
	 * Makes the sanctuary findable: a lit shaft with a ladder from the dome up to the surface and a glowing
	 * sculk monument around its top. Only built on dry land (a shaft under the sea would flood).
	 */
	private void sanctuaryBeacon(WorldGenLevel level, BoundingBox bb) {
		int sx = this.center.getX(), sz = this.center.getZ() + 10;
		if (sx < bb.minX() || sx > bb.maxX() || sz < bb.minZ() || sz > bb.maxZ()) return;
		int surface = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE_WG, sx, sz);
		int floor = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.OCEAN_FLOOR_WG, sx, sz);
		int top = surface - this.center.getY();
		if (surface != floor || top < 14) return;
		BlockState bricks = Blocks.DEEPSLATE_BRICKS.defaultBlockState(), glow = ModBlocks.ECHO_CRYSTAL_BLOCK.defaultBlockState();
		BlockState frame = Blocks.REINFORCED_DEEPSLATE.defaultBlockState(), sculk = Blocks.SCULK.defaultBlockState();
		BlockState ladder = Blocks.LADDER.defaultBlockState().setValue(net.minecraft.world.level.block.LadderBlock.FACING, net.minecraft.core.Direction.NORTH);
		for (int dy = 1; dy < top; dy++) {
			for (int dx = -2; dx <= 2; dx++) for (int dz = 8; dz <= 12; dz++) {
				boolean ring = Math.abs(dx) == 2 || dz == 8 || dz == 12;
				if (!ring) this.set(level, bb, dx, dy, dz, Blocks.AIR.defaultBlockState());
				else if (dy <= 3 && dz == 8) this.set(level, bb, dx, dy, dz, Blocks.AIR.defaultBlockState()); // doorway into the dome
				else this.set(level, bb, dx, dy, dz, dy % 6 == 0 && dx == 0 ? glow : bricks);
			}
			this.set(level, bb, 0, dy, 11, ladder);
		}
		// monument on the surface
		for (int dx = -5; dx <= 5; dx++) for (int dz = 5; dz <= 15; dz++) {
			double d = Math.hypot(dx, dz - 10);
			if (d > 5.5 || (Math.abs(dx) <= 2 && dz >= 8 && dz <= 12)) continue;
			this.set(level, bb, dx, top - 1, dz, d < 3.5 ? Blocks.POLISHED_DEEPSLATE.defaultBlockState() : noise(dx, dz) > 0.4 ? sculk : Blocks.DEEPSLATE_TILES.defaultBlockState());
			for (int dy = 0; dy <= 8; dy++) this.set(level, bb, dx, top + dy, dz, Blocks.AIR.defaultBlockState());
		}
		for (int[] p : new int[][]{{-3, 7}, {3, 7}, {-3, 13}, {3, 13}}) {
			for (int dy = 0; dy <= 5; dy++) this.set(level, bb, p[0], top + dy, p[1], frame);
			this.set(level, bb, p[0], top + 6, p[1], Blocks.SOUL_LANTERN.defaultBlockState());
		}
		for (int dx = -3; dx <= 3; dx++) this.set(level, bb, dx, top + 5, 10, frame);
		this.set(level, bb, 0, top + 6, 10, glow);
		this.set(level, bb, 0, top + 7, 10, glow);
		this.set(level, bb, 0, top + 8, 10, Blocks.SCULK_CATALYST.defaultBlockState());
		for (int[] l : new int[][]{{-5, 10}, {5, 10}, {0, 5}, {0, 15}}) this.set(level, bb, l[0], top, l[1], Blocks.SOUL_LANTERN.defaultBlockState());
	}

	// ------------------------------------------------------------------ Glowcube Shrine (Overworld)
	private void buildShrine(WorldGenLevel level, BoundingBox bb, RandomSource random) {
		BlockState bricks = ModBlocks.SKYSTONE_BRICKS.defaultBlockState();
		BlockState mossy = Blocks.MOSSY_STONE_BRICKS.defaultBlockState();
		for (int dx = -4; dx <= 4; dx++) for (int dz = -4; dz <= 4; dz++) {
			for (int dy = 1; dy <= 7; dy++) this.set(level, bb, dx, dy, dz, Blocks.AIR.defaultBlockState());
			this.set(level, bb, dx, 0, dz, noise(dx, dz) > 0.6 ? mossy : bricks);
			for (int dy = 1; dy <= 6; dy++) {
				if (this.get(level, bb, dx, -dy, dz).isSolid()) break;
				this.set(level, bb, dx, -dy, dz, Blocks.STONE_BRICKS.defaultBlockState());
			}
			boolean corner = Math.abs(dx) == 4 && Math.abs(dz) == 4;
			if (corner) for (int dy = 1; dy <= 3 + (int) (noise(dx * 3, dz * 7) * 3); dy++) this.set(level, bb, dx, dy, dz, bricks);
		}
		// broken glowstone portal frame - just needs a few blocks and a Lumen Key
		int[][] frame = {{-1, 1}, {0, 1}, {1, 1}, {2, 1}, {-1, 2}, {2, 2}, {-1, 3}, {2, 3}, {-1, 4}, {2, 4}, {-1, 5}, {0, 5}};
		for (int[] f : frame) this.set(level, bb, f[0], f[1], 2, Blocks.GLOWSTONE.defaultBlockState());
		this.set(level, bb, 0, 1, -2, ModBlocks.CHISELED_SKYSTONE.defaultBlockState());
		this.chest(level, bb, random, 0, 2, -2, "glowcube_shrine");
		this.set(level, bb, 2, 0, -3, ModBlocks.LUMEN_GRASS.defaultBlockState());
		this.set(level, bb, -2, 0, -3, ModBlocks.LUMEN_GRASS.defaultBlockState());
		this.set(level, bb, 2, 1, -3, ModBlocks.LUMEN_BLOOM.defaultBlockState());
		this.set(level, bb, -2, 1, -3, ModBlocks.LUMEN_BLOOM.defaultBlockState());
	}
}
