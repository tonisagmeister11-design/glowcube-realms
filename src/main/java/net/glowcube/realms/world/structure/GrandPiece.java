package net.glowcube.realms.world.structure;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import net.glowcube.realms.registry.ModWorldgen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BrushableBlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * One large procedural building. The layout is derived from a seed made of the position, so every chunk
 * computes the same plan and only places the blocks inside its own bounding box.
 */
public class GrandPiece extends StructurePiece {
	private final String kind;
	private final BlockPos center;

	public GrandPiece(String kind, BlockPos center) {
		super(ModWorldgen.GRAND_PIECE, 0, box(kind, center));
		this.kind = kind;
		this.center = center;
	}

	public GrandPiece(CompoundTag tag) {
		super(ModWorldgen.GRAND_PIECE, tag);
		this.kind = tag.getStringOr("Kind", "pyramid");
		this.center = new BlockPos(tag.getIntOr("CX", 0), tag.getIntOr("CY", 64), tag.getIntOr("CZ", 0));
	}

	private static BoundingBox box(String kind, BlockPos c) {
		int r, lo, hi;
		switch (kind) {
			case "pyramid" -> { r = 62; lo = -16; hi = 47; }
			case "jungle" -> { r = 44; lo = -14; hi = 40; }
			case "igloo" -> { r = 17; lo = -13; hi = 10; }
			case "witch" -> { r = 11; lo = -12; hi = 23; }
			case "sky_market" -> { r = 26; lo = -21; hi = 26; }
			case "sky_ruin" -> { r = 11; lo = -13; hi = 10; }
			case "shadow_bazaar" -> { r = 25; lo = -44; hi = 12; }
			case "umbral_mine" -> { r = 42; lo = -3; hi = 5; }
			case "echo_camp" -> { r = 11; lo = -9; hi = 9; }
			case "echo_ruin" -> { r = 19; lo = -11; hi = 15; }
			default -> { r = 25; lo = -10; hi = 34; }
		}
		return new BoundingBox(c.getX() - r, c.getY() + lo, c.getZ() - r, c.getX() + r, c.getY() + hi, c.getZ() + r);
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
		B b = new B(level, chunkBB, random);
		switch (this.kind) {
			case "pyramid" -> this.pyramid(b);
			case "jungle" -> this.jungle(b);
			case "igloo" -> this.igloo(b);
			case "witch" -> this.witch(b);
			case "sky_market" -> this.skyMarket(b);
			case "sky_ruin" -> this.skyRuin(b);
			case "shadow_bazaar" -> this.shadowBazaar(b);
			case "umbral_mine" -> this.umbralMine(b);
			case "echo_camp" -> this.echoCamp(b);
			case "echo_ruin" -> this.echoRuin(b);
			default -> this.fortress(b);
		}
	}

	/** Same sequence in every chunk. */
	private RandomSource layout() {
		return RandomSource.create(this.center.asLong() * 31L + this.kind.hashCode());
	}

	private static double noise(int x, int y, int z) {
		long h = x * 3129871L ^ z * 116129781L ^ y * 42317861L;
		h = h * h * 42317861L + h * 11L;
		return ((h >> 16) & 0xFFFF) / 65535.0;
	}

	private static BlockState st(Block b) {
		return b.defaultBlockState();
	}

	private static BlockState facing(Block b, Direction d) {
		return b.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, d);
	}

	// ================================================================== builder helpers
	final class B {
		final WorldGenLevel level;
		final BoundingBox bb;
		final RandomSource loot;

		B(WorldGenLevel level, BoundingBox bb, RandomSource loot) {
			this.level = level;
			this.bb = bb;
			this.loot = loot;
		}

		BlockPos pos(int x, int y, int z) {
			return GrandPiece.this.center.offset(x, y, z);
		}

		boolean inside(int x, int y, int z) {
			return this.bb.isInside(this.pos(x, y, z));
		}

		void set(int x, int y, int z, BlockState s) {
			BlockPos p = this.pos(x, y, z);
			if (this.bb.isInside(p)) this.level.setBlock(p, s, 2);
		}

		BlockState get(int x, int y, int z) {
			BlockPos p = this.pos(x, y, z);
			return this.bb.isInside(p) ? this.level.getBlockState(p) : Blocks.STONE.defaultBlockState();
		}

		boolean solid(int x, int y, int z) {
			BlockState s = this.get(x, y, z);
			return !s.isAir() && s.getFluidState().isEmpty() && !s.canBeReplaced() && !s.is(net.minecraft.tags.BlockTags.LEAVES) && !s.is(net.minecraft.tags.BlockTags.LOGS);
		}

		void fill(int x1, int y1, int z1, int x2, int y2, int z2, BlockState s) {
			for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++)
				for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++)
					for (int y = Math.min(y1, y2); y <= Math.max(y1, y2); y++) this.set(x, y, z, s);
		}

		void air(int x1, int y1, int z1, int x2, int y2, int z2) {
			this.fill(x1, y1, z1, x2, y2, z2, Blocks.AIR.defaultBlockState());
		}

		/** Hollow box: walls, floor and ceiling of s, inside air. */
		void room(int x1, int y1, int z1, int x2, int y2, int z2, BlockState s) {
			this.fill(x1, y1, z1, x2, y2, z2, s);
			this.air(x1 + 1, y1 + 1, z1 + 1, x2 - 1, y2 - 1, z2 - 1);
		}

		/** Fills downwards from y until solid ground is reached (at most max blocks). */
		void foundation(int x, int z, int y, BlockState s, int max) {
			for (int i = 0; i < max && !this.solid(x, y - i, z); i++) this.set(x, y - i, z, s);
		}

		void chest(int x, int y, int z, Direction face, String table) {
			BlockPos p = this.pos(x, y, z);
			if (!this.bb.isInside(p)) return;
			this.level.setBlock(p, facing(Blocks.CHEST, face), 2);
			RandomizableContainer.setBlockEntityLootTable(this.level, this.loot, p, ResourceKey.create(Registries.LOOT_TABLE, Identifier.parse(table)));
		}

		void barrel(int x, int y, int z, String table) {
			BlockPos p = this.pos(x, y, z);
			if (!this.bb.isInside(p)) return;
			this.level.setBlock(p, Blocks.BARREL.defaultBlockState().setValue(BlockStateProperties.FACING, Direction.UP), 2);
			RandomizableContainer.setBlockEntityLootTable(this.level, this.loot, p, ResourceKey.create(Registries.LOOT_TABLE, Identifier.parse(table)));
		}

		/** Arrow dispenser like in the vanilla jungle temple. */
		void dispenser(int x, int y, int z, Direction face) {
			BlockPos p = this.pos(x, y, z);
			if (!this.bb.isInside(p)) return;
			this.level.setBlock(p, Blocks.DISPENSER.defaultBlockState().setValue(BlockStateProperties.FACING, face), 2);
			RandomizableContainer.setBlockEntityLootTable(this.level, this.loot, p,
					ResourceKey.create(Registries.LOOT_TABLE, Identifier.parse("minecraft:chests/jungle_temple_dispenser")));
		}

		void spawner(int x, int y, int z, EntityType<?> type) {
			BlockPos p = this.pos(x, y, z);
			if (!this.bb.isInside(p)) return;
			this.level.setBlock(p, Blocks.SPAWNER.defaultBlockState(), 2);
			if (this.level.getBlockEntity(p) instanceof SpawnerBlockEntity spawner) spawner.setEntityId(type, this.loot);
		}

		void suspicious(int x, int y, int z, Block block, String table) {
			BlockPos p = this.pos(x, y, z);
			if (!this.bb.isInside(p)) return;
			this.level.setBlock(p, block.defaultBlockState(), 2);
			if (this.level.getBlockEntity(p) instanceof BrushableBlockEntity brush)
				brush.setLootTable(ResourceKey.create(Registries.LOOT_TABLE, Identifier.parse(table)), p.asLong());
		}

		void mob(EntityType<? extends Mob> type, int x, int y, int z) {
			BlockPos p = this.pos(x, y, z);
			if (!this.bb.isInside(p)) return;
			Mob mob = type.create(this.level.getLevel(), EntitySpawnReason.STRUCTURE);
			if (mob == null) return;
			mob.snapTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5, 0, 0);
			mob.setPersistenceRequired();
			mob.finalizeSpawn(this.level, this.level.getCurrentDifficultyAt(p), EntitySpawnReason.STRUCTURE, null);
			this.level.addFreshEntityWithPassengers(mob);
		}

		/** Tripwire across a corridor between two hooks, with arrow dispensers above both hooks. */
		void tripwireTrap(int x1, int y, int z1, int x2, int z2) {
			boolean alongX = z1 == z2;
			Direction a = alongX ? Direction.EAST : Direction.SOUTH;
			this.set(x1, y, z1, facing(Blocks.TRIPWIRE_HOOK, a).setValue(BlockStateProperties.ATTACHED, true));
			this.set(x2, y, z2, facing(Blocks.TRIPWIRE_HOOK, a.getOpposite()).setValue(BlockStateProperties.ATTACHED, true));
			BlockState wire = Blocks.TRIPWIRE.defaultBlockState().setValue(BlockStateProperties.ATTACHED, true)
					.setValue(alongX ? BlockStateProperties.EAST : BlockStateProperties.SOUTH, true)
					.setValue(alongX ? BlockStateProperties.WEST : BlockStateProperties.NORTH, true);
			if (alongX) for (int x = x1 + 1; x < x2; x++) this.set(x, y, z1, wire);
			else for (int z = z1 + 1; z < z2; z++) this.set(x1, y, z, wire);
			this.dispenser(x1, y + 1, z1, a);
			this.dispenser(x2, y + 1, z2, a.getOpposite());
		}

		/** Pressure plates over TNT, like the vanilla desert temple. */
		void tntPlate(int x, int y, int z) {
			this.set(x, y, z, st(Blocks.STONE_PRESSURE_PLATE));
			this.set(x, y - 2, z, st(Blocks.TNT));
		}

		void ladder(int x, int y1, int y2, int z, Direction face) {
			for (int y = y1; y <= y2; y++) this.set(x, y, z, facing(Blocks.LADDER, face));
		}

		void bed(int x, int y, int z, Direction dir, Block bed) {
			this.set(x, y, z, facing(bed, dir).setValue(BlockStateProperties.BED_PART, BedPart.FOOT));
			this.set(x + dir.getStepX(), y, z + dir.getStepZ(), facing(bed, dir).setValue(BlockStateProperties.BED_PART, BedPart.HEAD));
		}

		void door(int x, int y, int z, Direction face, Block door) {
			this.set(x, y, z, facing(door, face).setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.LOWER));
			this.set(x, y + 1, z, facing(door, face).setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER));
		}

		void stairs(int x, int y, int z, Block stair, Direction up) {
			this.set(x, y, z, facing(stair, up));
		}

		/** Row of fences or bars connected along one axis. */
		void rail(int x1, int y, int z1, int x2, int z2, Block block) {
			boolean alongX = z1 == z2;
			BlockState s = block.defaultBlockState().setValue(alongX ? BlockStateProperties.EAST : BlockStateProperties.NORTH, true)
					.setValue(alongX ? BlockStateProperties.WEST : BlockStateProperties.SOUTH, true);
			if (alongX) for (int x = Math.min(x1, x2); x <= Math.max(x1, x2); x++) this.set(x, y, z1, s);
			else for (int z = Math.min(z1, z2); z <= Math.max(z1, z2); z++) this.set(x1, y, z, s);
		}

		void vine(int x, int y, int z, Direction towardWall, int length) {
			var prop = switch (towardWall) {
				case NORTH -> BlockStateProperties.NORTH;
				case SOUTH -> BlockStateProperties.SOUTH;
				case EAST -> BlockStateProperties.EAST;
				default -> BlockStateProperties.WEST;
			};
			for (int i = 0; i < length; i++) {
				if (!this.get(x, y - i, z).isAir()) return;
				this.set(x, y - i, z, Blocks.VINE.defaultBlockState().setValue(prop, true));
			}
		}
	}

	// ================================================================== maze helper
	/**
	 * Carves an n x n maze of 3-wide corridors (cells every 4 blocks starting at base) between y and y+h-1,
	 * starting at cell (si, sj). Returns the number of openings of every cell (1 = dead end).
	 */
	private int[] maze(B b, RandomSource r, int n, int base, int y, int h, int si, int sj, int loops, BlockState floor, BlockState accent) {
		boolean[][] seen = new boolean[n][n];
		List<int[]> passages = new ArrayList<>();
		ArrayDeque<int[]> stack = new ArrayDeque<>();
		stack.push(new int[]{si, sj});
		seen[si][sj] = true;
		int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
		int[] links = new int[n * n];
		while (!stack.isEmpty()) {
			int[] c = stack.peek();
			List<int[]> open = new ArrayList<>();
			for (int[] d : dirs) {
				int ni = c[0] + d[0], nj = c[1] + d[1];
				if (ni >= 0 && nj >= 0 && ni < n && nj < n && !seen[ni][nj]) open.add(new int[]{ni, nj});
			}
			if (open.isEmpty()) {
				stack.pop();
				continue;
			}
			int[] nx = open.get(r.nextInt(open.size()));
			seen[nx[0]][nx[1]] = true;
			passages.add(new int[]{c[0], c[1], nx[0], nx[1]});
			links[c[0] * n + c[1]]++;
			links[nx[0] * n + nx[1]]++;
			stack.push(nx);
		}
		for (int k = 0; k < loops; k++) {
			int i = r.nextInt(n - 1), j = r.nextInt(n);
			passages.add(r.nextBoolean() ? new int[]{i, j, i + 1, j} : new int[]{j, i, j, i + 1});
		}
		for (int i = 0; i < n; i++) for (int j = 0; j < n; j++) {
			int cx = base + 4 * i, cz = base + 4 * j;
			b.air(cx - 1, y, cz - 1, cx + 1, y + h - 1, cz + 1);
			for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++)
				b.set(cx + dx, y - 1, cz + dz, noise(cx + dx, y, cz + dz) > 0.84 ? accent : floor);
		}
		for (int[] p : passages) {
			int x1 = base + 4 * p[0], z1 = base + 4 * p[1], x2 = base + 4 * p[2], z2 = base + 4 * p[3];
			int mx = (x1 + x2) / 2, mz = (z1 + z2) / 2;
			if (x1 == x2) b.air(mx - 1, y, mz, mx + 1, y + h - 1, mz);
			else b.air(mx, y, mz - 1, mx, y + h - 1, mz + 1);
		}
		return links;
	}

	// ================================================================== Grand Desert Pyramid
	private void pyramid(B b) {
		RandomSource r = this.layout();
		BlockState ss = st(Blocks.SANDSTONE), smooth = st(Blocks.SMOOTH_SANDSTONE), cut = st(Blocks.CUT_SANDSTONE), chis = st(Blocks.CHISELED_SANDSTONE);
		BlockState orange = st(Blocks.DYED_TERRACOTTA.pick(net.minecraft.world.item.DyeColor.ORANGE));
		BlockState blue = st(Blocks.DYED_TERRACOTTA.pick(net.minecraft.world.item.DyeColor.BLUE)), gold = st(Blocks.GOLD_BLOCK);
		final int H = 44;
		final String TREASURE = "glowcube_realms:chests/grand_pyramid_treasure", PYR = "minecraft:chests/desert_pyramid";
		// foundation and an underground plinth that holds the crypt level
		for (int dx = -H; dx <= H; dx++) for (int dz = -H; dz <= H; dz++) b.foundation(dx, dz, -1, ss, 16);
		b.fill(-37, -14, -37, 37, -1, 37, ss);
		// stepped body with terracotta bands and a golden cap
		for (int y = 0; y <= H; y++) {
			int s = H - y;
			for (int dx = -s; dx <= s; dx++) for (int dz = -s; dz <= s; dz++) {
				int m = Math.max(Math.abs(dx), Math.abs(dz));
				BlockState state = ss;
				if (m >= s - 1) state = y % 7 == 3 && m == s ? orange : y % 7 == 4 && m == s ? cut : smooth;
				if (y >= H - 2) state = gold;
				b.set(dx, y, dz, state);
			}
		}
		// forecourt with an avenue of obelisks and the gate (north)
		for (int dx = -9; dx <= 9; dx++) for (int dz = -61; dz <= -44; dz++) {
			b.foundation(dx, dz, -1, ss, 12);
			b.set(dx, 0, dz, Math.abs(dx) <= 2 ? ((dz % 3 == 0) ? orange : cut) : smooth);
			if (dz <= -45) b.air(dx, 1, dz, dx, 14, dz);
		}
		for (int sx : new int[]{-8, 8}) for (int oz : new int[]{-58, -52}) {
			b.fill(sx, 1, oz, sx, 11, oz, cut);
			b.set(sx, 5, oz, orange);
			b.set(sx, 12, oz, chis);
			b.set(sx, 13, oz, gold);
		}
		b.fill(-5, 0, -50, 5, 10, -43, cut);
		b.fill(-5, 9, -50, 5, 9, -50, orange);
		b.set(-3, 7, -50, chis);
		b.set(3, 7, -50, chis);
		b.set(0, 10, -50, blue);
		b.air(-1, 1, -51, 1, 4, -33);

		// level 1: big maze (17 x 17 cells)
		int n = 17;
		int[] links = this.maze(b, r, n, -32, 1, 4, 8, 0, 30, smooth, orange);
		int chests = 0;
		for (int i = 0; i < n; i++) for (int j = 0; j < n; j++) {
			if (links[i * n + j] != 1 || (Math.abs(i - 8) <= 2 && Math.abs(j - 8) <= 2) || (i == 16 && j == 16) || (i == 8 && j == 0)) continue;
			int cx = -32 + 4 * i, cz = -32 + 4 * j;
			switch (r.nextInt(5)) {
				case 0 -> {
					if (chests++ < 10) b.chest(cx, 1, cz, Direction.NORTH, PYR);
				}
				case 1 -> {
					b.suspicious(cx, 0, cz, Blocks.SUSPICIOUS_SAND, "minecraft:archaeology/desert_pyramid");
					b.suspicious(cx + 1, 0, cz, Blocks.SUSPICIOUS_SAND, "minecraft:archaeology/desert_pyramid");
				}
				case 2 -> {
					b.set(cx, 1, cz, st(Blocks.DECORATED_POT));
					b.set(cx - 1, 1, cz + 1, st(Blocks.DECORATED_POT));
				}
				case 3 -> {
					// mummy niche: sarcophagus with a husk spawner
					b.set(cx, 1, cz, st(Blocks.SMOOTH_SANDSTONE));
					b.spawner(cx, 2, cz, EntityTypes.HUSK);
				}
				default -> b.set(cx, 1, cz, st(Blocks.LANTERN));
			}
		}
		for (int k = 0; k < 26; k++) b.set(-32 + 4 * r.nextInt(n) + 1, 1, -32 + 4 * r.nextInt(n) + 1, st(Blocks.LANTERN));

		// great hall with the TNT star trap
		b.air(-9, 1, -9, 9, 14, 9);
		for (int dx = -9; dx <= 9; dx++) for (int dz = -9; dz <= 9; dz++) {
			int m = Math.abs(dx) + Math.abs(dz);
			b.set(dx, 0, dz, m <= 1 ? blue : m <= 5 && (dx == 0 || dz == 0) ? orange : (dx + dz) % 2 == 0 ? smooth : cut);
		}
		for (int[] p : new int[][]{{-4, -4}, {4, -4}, {-4, 4}, {4, 4}, {-8, -8}, {8, -8}, {-8, 8}, {8, 8}}) {
			b.fill(p[0], 1, p[1], p[0], 14, p[1], cut);
			b.set(p[0], 1, p[1], chis);
			b.set(p[0], 14, p[1], chis);
			b.set(p[0], 7, p[1], orange);
		}
		b.tntPlate(0, 1, 0);
		for (int[] t : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}, {1, 1}, {-1, -1}, {1, -1}, {-1, 1}}) b.set(t[0], -1, t[1], st(Blocks.TNT));
		b.chest(-9, 1, -2, Direction.EAST, PYR);
		b.chest(9, 1, 2, Direction.WEST, PYR);
		b.chest(2, 1, -9, Direction.SOUTH, PYR);
		b.chest(-2, 1, 9, Direction.NORTH, PYR);
		for (int[] l : new int[][]{{-3, -5}, {3, 5}, {5, -3}, {-5, 3}, {-7, 0}, {7, 0}}) b.set(l[0], 1, l[1], st(Blocks.LANTERN));

		// level 2: king's chamber above the hall (ladder on a pillar)
		b.air(-8, 17, -8, 8, 23, 8);
		b.fill(-4, 15, -4, -4, 17, -4, cut);
		b.ladder(-3, 1, 17, -4, Direction.EAST);
		for (int dx = -8; dx <= 8; dx++) for (int dz = -8; dz <= 8; dz++) if ((dx + dz) % 3 == 0 && (dx != -3 || dz != -4)) b.set(dx, 16, dz, orange);
		for (int d = -8; d <= 8; d++) {
			b.set(d, 20, -9, chis);
			b.set(d, 20, 9, chis);
			b.set(-9, 20, d, chis);
			b.set(9, 20, d, chis);
		}
		b.fill(-1, 17, -2, 1, 17, 2, smooth);
		b.fill(-1, 18, -2, 1, 18, 2, st(Blocks.CUT_SANDSTONE_SLAB));
		b.set(0, 18, -2, chis);
		b.set(0, 18, 2, gold);
		for (int[] g : new int[][]{{-7, -7}, {7, -7}, {-7, 7}, {7, 7}}) {
			b.set(g[0], 17, g[1], gold);
			b.set(g[0], 18, g[1], st(Blocks.LANTERN));
		}
		b.chest(6, 17, 0, Direction.WEST, TREASURE);
		b.chest(-6, 17, 1, Direction.EAST, TREASURE);
		b.chest(0, 17, -7, Direction.SOUTH, PYR);

		// level 3: the sun gallery maze, reached by a ladder from the king's chamber
		b.fill(6, 17, 6, 6, 26, 6, cut);
		b.ladder(5, 17, 26, 6, Direction.WEST);
		int[] upper = this.maze(b, r, 7, -12, 26, 4, 4, 4, 4, smooth, orange);
		b.air(5, 26, 6, 5, 29, 6);
		b.ladder(5, 17, 26, 6, Direction.WEST);
		int found = 0;
		for (int i = 0; i < 7; i++) for (int j = 0; j < 7; j++) {
			if (upper[i * 7 + j] != 1 || (i == 3 && j == 3)) continue;
			int cx = -12 + 4 * i, cz = -12 + 4 * j;
			if (found++ % 2 == 0) b.chest(cx, 26, cz, Direction.NORTH, TREASURE);
			else b.suspicious(cx, 25, cz, Blocks.SUSPICIOUS_SAND, "minecraft:archaeology/desert_pyramid");
			b.set(cx + 1, 26, cz + 1, st(Blocks.LANTERN));
		}
		// level 4: the sun chamber near the top with an enchanting altar
		b.fill(0, 26, 0, 0, 32, 0, cut);
		b.ladder(1, 26, 32, 0, Direction.WEST);
		b.air(-5, 32, -5, 5, 36, 5);
		for (int dx = -5; dx <= 5; dx++) for (int dz = -5; dz <= 5; dz++) b.set(dx, 31, dz, (dx + dz) % 2 == 0 ? gold : cut);
		b.set(1, 31, 0, facing(Blocks.LADDER, Direction.WEST));
		b.set(0, 32, -3, st(Blocks.ENCHANTING_TABLE));
		for (int dx = -2; dx <= 2; dx++) {
			b.set(dx, 32, -5, st(Blocks.BOOKSHELF));
			b.set(dx, 33, -5, st(Blocks.BOOKSHELF));
		}
		b.set(-2, 32, -4, st(Blocks.BOOKSHELF));
		b.set(2, 32, -4, st(Blocks.BOOKSHELF));
		b.chest(-4, 32, 3, Direction.EAST, TREASURE);
		b.chest(4, 32, 3, Direction.WEST, TREASURE);
		b.set(-4, 32, -4, st(Blocks.LANTERN));
		b.set(4, 32, -4, st(Blocks.LANTERN));

		// crypt: stairs from the far corner of the maze down to the trap corridors
		for (int k = 0; k <= 9; k++) {
			int x = 30 - k, fy = -k;
			b.air(x, fy + 1, 31, x, fy + 4, 33);
			for (int z = 31; z <= 33; z++) b.stairs(x, fy, z, Blocks.SANDSTONE_STAIRS, Direction.EAST);
		}
		b.air(-1, -8, 31, 20, -5, 33);
		b.fill(-1, -9, 31, 20, -9, 33, smooth);
		b.tripwireTrap(16, -8, 30, 16, 34);
		b.tripwireTrap(10, -8, 30, 10, 34);
		b.tripwireTrap(19, -8, 30, 19, 34);
		b.set(13, -5, 32, st(Blocks.LANTERN).setValue(BlockStateProperties.HANGING, true));
		b.set(7, -5, 32, st(Blocks.LANTERN).setValue(BlockStateProperties.HANGING, true));
		b.air(-1, -8, 10, 1, -5, 30);
		b.fill(-1, -9, 10, 1, -9, 30, smooth);
		// lava pit to jump over
		b.air(-1, -11, 21, 1, -9, 23);
		b.fill(-1, -12, 21, 1, -12, 23, st(Blocks.LAVA));
		// side crypts with mummies
		for (int side : new int[]{-1, 1}) for (int cz : new int[]{15, 27}) {
			int x1 = side < 0 ? -8 : 3, x2 = side < 0 ? -3 : 8;
			b.air(x1, -8, cz - 2, x2, -5, cz + 2);
			b.air(2 * side, -8, cz, 2 * side, -6, cz);
			int mx = side * 6;
			b.fill(mx - 1, -8, cz - 1, mx + 1, -8, cz + 1, smooth);
			b.set(mx, -7, cz, chis);
			b.spawner(mx, -8, cz - 2, EntityTypes.HUSK);
			b.chest(mx + side, -7, cz + 1, side < 0 ? Direction.EAST : Direction.WEST, PYR);
			b.set(side * 4, -8, cz - 2, st(Blocks.LANTERN));
		}
		// pressure plates over TNT in front of the treasure door
		for (int dx = -1; dx <= 1; dx++) {
			b.tntPlate(dx, -8, 12);
			b.set(dx, -10, 11, st(Blocks.TNT));
		}
		// treasure chamber
		b.air(-9, -8, -9, 9, -3, 9);
		b.air(-1, -8, 9, 1, -6, 10);
		for (int dx = -9; dx <= 9; dx++) for (int dz = -9; dz <= 9; dz++) {
			int m = Math.max(Math.abs(dx), Math.abs(dz));
			b.set(dx, -9, dz, m % 2 == 0 ? blue : (dx + dz) % 2 == 0 ? gold : orange);
		}
		for (int[] p : new int[][]{{-5, -5}, {5, -5}, {-5, 5}, {5, 5}}) {
			b.fill(p[0], -8, p[1], p[0], -3, p[1], cut);
			b.set(p[0], -8, p[1], chis);
			b.set(p[0], -3, p[1], chis);
		}
		b.fill(-2, -8, -3, 2, -7, 2, smooth);
		b.fill(-2, -6, -3, 2, -6, 2, st(Blocks.SMOOTH_SANDSTONE_SLAB));
		b.set(0, -6, -3, gold);
		b.set(0, -6, 2, gold);
		b.spawner(-7, -8, -7, EntityTypes.HUSK);
		b.spawner(7, -8, -7, EntityTypes.HUSK);
		b.chest(-3, -8, -9, Direction.SOUTH, TREASURE);
		b.chest(3, -8, -9, Direction.SOUTH, TREASURE);
		b.chest(-9, -8, 0, Direction.EAST, TREASURE);
		b.chest(9, -8, 0, Direction.WEST, TREASURE);
		b.chest(0, -8, -9, Direction.SOUTH, PYR);
		for (int[] g : new int[][]{{-8, -8}, {8, -8}, {-8, 7}, {8, 7}, {-8, -6}, {8, -6}, {-6, -8}, {6, -8}}) b.set(g[0], -8, g[1], r.nextBoolean() ? gold : st(Blocks.RAW_GOLD_BLOCK));
		for (int[] l : new int[][]{{-3, 6}, {3, 6}, {-4, -4}, {4, -4}, {-7, 3}, {7, 3}}) b.set(l[0], -8, l[1], st(Blocks.LANTERN));
	}

	// ================================================================== Grand Jungle Temple
	private BlockState mossy(int x, int y, int z) {
		double n = noise(x, y, z);
		if (n < 0.42) return st(Blocks.MOSSY_COBBLESTONE);
		if (n < 0.70) return st(Blocks.COBBLESTONE);
		if (n < 0.86) return st(Blocks.MOSSY_STONE_BRICKS);
		if (n < 0.95) return st(Blocks.STONE_BRICKS);
		return st(Blocks.CRACKED_STONE_BRICKS);
	}

	private void mossyFill(B b, int x1, int y1, int z1, int x2, int y2, int z2) {
		for (int x = x1; x <= x2; x++) for (int z = z1; z <= z2; z++) for (int y = y1; y <= y2; y++) b.set(x, y, z, this.mossy(x, y, z));
	}

	private void jungle(B b) {
		RandomSource r = this.layout();
		BlockState chis = st(Blocks.CHISELED_STONE_BRICKS), bricks = st(Blocks.MOSSY_STONE_BRICKS), gold = st(Blocks.GOLD_BLOCK), emerald = st(Blocks.EMERALD_BLOCK);
		final String TEMPLE = "minecraft:chests/jungle_temple", TREASURE = "glowcube_realms:chests/jungle_treasure";
		for (int dx = -32; dx <= 32; dx++) for (int dz = -32; dz <= 32; dz++)
			for (int i = 1; i <= 16 && !b.solid(dx, -i, dz); i++) b.set(dx, -i, dz, this.mossy(dx, -i, dz));
		this.mossyFill(b, -28, -13, -28, 28, -1, 28);
		// five stepped tiers
		for (int t = 0; t < 5; t++) {
			int half = 32 - 4 * t;
			this.mossyFill(b, -half, 6 * t, -half, half, 6 * t + 5, half);
			for (int d = -half; d <= half; d += 4) {
				b.set(d, 6 * t + 5, -half, chis);
				b.set(d, 6 * t + 5, half, chis);
				b.set(-half, 6 * t + 5, d, chis);
				b.set(half, 6 * t + 5, d, chis);
			}
		}
		// grand stair up the north face
		for (int k = 0; k <= 30; k++) {
			int z = -43 + k;
			for (int dx = -3; dx <= 3; dx++) {
				for (int y = 0; y < k; y++) if (z < -32 || !b.solid(dx, y, z)) b.set(dx, y, z, this.mossy(dx, y, z));
				b.foundation(dx, z, -1, this.mossy(dx, 0, z), 10);
				b.stairs(dx, k, z, Math.abs(dx) == 3 ? Blocks.STONE_BRICK_STAIRS : Blocks.MOSSY_COBBLESTONE_STAIRS, Direction.SOUTH);
				b.air(dx, k + 1, z, dx, k + 4, z);
			}
			if (k % 6 == 5) {
				b.set(-4, k + 1, z, st(Blocks.LANTERN));
				b.set(4, k + 1, z, st(Blocks.LANTERN));
			}
		}
		b.air(-3, 30, -12, 3, 33, -10);
		// shrine on the top
		b.room(-9, 30, -9, 9, 39, 9, bricks);
		b.fill(-10, 39, -10, 10, 39, 10, st(Blocks.MOSSY_STONE_BRICK_SLAB));
		b.air(-1, 31, -9, 1, 34, -9);
		for (int[] p : new int[][]{{-5, -5}, {5, -5}, {-5, 5}, {5, 5}}) b.fill(p[0], 31, p[1], p[0], 38, p[1], chis);
		b.fill(-2, 31, 5, 2, 31, 7, chis);
		b.set(0, 32, 6, gold);
		b.set(0, 33, 6, emerald);
		b.set(-1, 32, 6, st(Blocks.EMERALD_BLOCK));
		b.set(1, 32, 6, st(Blocks.GOLD_BLOCK));
		b.chest(-2, 32, 6, Direction.NORTH, TEMPLE);
		b.chest(2, 32, 6, Direction.NORTH, TREASURE);
		b.tntPlate(0, 31, 3);
		for (int[] l : new int[][]{{-7, 0}, {7, 0}, {-7, -7}, {7, -7}}) b.set(l[0], 31, l[1], st(Blocks.LANTERN));

		// ground level: entrance (south), ring corridor and the central hall
		b.fill(-4, 0, 31, 4, 7, 34, chis);
		b.air(-1, 1, 20, 1, 4, 34);
		for (int dx = -23; dx <= 23; dx++) for (int dz = -23; dz <= 23; dz++) {
			int m = Math.max(Math.abs(dx), Math.abs(dz));
			if (m >= 21) b.air(dx, 1, dz, dx, 4, dz);
		}
		b.air(-12, 1, -12, 12, 10, 12);
		b.air(-1, 1, 13, 1, 4, 20);
		b.air(-1, 1, -20, 1, 4, -13);
		b.air(13, 1, -1, 20, 4, 1);
		b.air(-20, 1, -1, -13, 4, 1);
		b.tripwireTrap(20, 1, 8, 24, 8);
		b.tripwireTrap(-24, 1, -8, -20, -8);
		b.tripwireTrap(8, 1, -24, 8, -20);
		b.tripwireTrap(-8, 1, 20, -8, 24);
		b.tripwireTrap(-1, 1, 16, 1, 16);
		for (int[] c : new int[][]{{-22, -22}, {22, -22}, {-22, 22}, {22, 22}}) b.chest(c[0], 1, c[1], Direction.NORTH, TEMPLE);
		// hall
		for (int px : new int[]{-8, 8}) for (int pz : new int[]{-8, 8}) b.fill(px, 1, pz, px, 10, pz, chis);
		for (int px : new int[]{-4, 4}) for (int pz : new int[]{-4, 4}) {
			b.fill(px, 1, pz, px, 3, pz, chis);
			b.set(px, 4, pz, st(Blocks.GLOWSTONE));
		}
		for (int dx = -12; dx <= 12; dx++) for (int dz = -12; dz <= 12; dz++) if (noise(dx, 0, dz) > 0.6) b.set(dx, 0, dz, bricks);
		b.chest(-11, 1, -4, Direction.EAST, TEMPLE);
		b.chest(11, 1, 4, Direction.WEST, TEMPLE);
		for (int[] g : new int[][]{{-11, -11}, {11, 11}, {-11, 11}, {11, -11}}) b.set(g[0], 1, g[1], st(Blocks.GLOWSTONE));
		b.air(-1, -9, -1, 1, 0, 1);
		for (int dx = -1; dx <= 1; dx++) b.vine(dx, 0, -1, Direction.NORTH, 9);

		// upper gallery on the third tier, reached by a ladder from the hall
		b.ladder(12, 1, 13, 6, Direction.WEST);
		b.air(12, 13, 5, 16, 16, 7);
		b.ladder(12, 1, 13, 6, Direction.WEST);
		for (int dx = -19; dx <= 19; dx++) for (int dz = -19; dz <= 19; dz++) {
			int m = Math.max(Math.abs(dx), Math.abs(dz));
			if (m >= 17) b.air(dx, 13, dz, dx, 16, dz);
		}
		b.tripwireTrap(16, 13, -6, 20, -6);
		b.tripwireTrap(-20, 13, 6, -16, 6);
		b.tripwireTrap(-6, 13, -20, -6, -16);
		b.spawner(0, 13, 18, EntityTypes.ZOMBIE);
		b.spawner(0, 13, -18, EntityTypes.SKELETON);
		for (int[] c : new int[][]{{-18, -18}, {18, -18}, {-18, 18}, {18, 18}}) {
			b.chest(c[0], 13, c[1], Direction.NORTH, c[0] > 0 && c[1] > 0 ? TREASURE : TEMPLE);
			b.set(c[0] + (c[0] > 0 ? -1 : 1), 13, c[1], st(Blocks.LANTERN));
		}

		// spider den below the hall
		b.air(-10, -10, -10, 10, -4, 10);
		for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) b.set(dx, -11, dz, st(Blocks.WATER));
		b.spawner(-8, -10, -8, EntityTypes.CAVE_SPIDER);
		b.spawner(8, -10, 8, EntityTypes.CAVE_SPIDER);
		for (int k = 0; k < 44; k++) {
			int x = r.nextInt(21) - 10, z = r.nextInt(21) - 10, y = -10 + r.nextInt(6);
			if (Math.abs(x) > 1 || Math.abs(z) > 1) b.set(x, y, z, st(Blocks.COBWEB));
		}
		b.chest(0, -10, -9, Direction.SOUTH, TEMPLE);
		b.set(-4, -10, 4, st(Blocks.GLOWSTONE));
		b.set(4, -10, -4, st(Blocks.GLOWSTONE));
		// corridor east to the hidden vault: tripwire, TNT plate and a cracked "secret" wall
		b.air(11, -10, -1, 13, -7, 1);
		b.tripwireTrap(11, -10, -2, 11, 2);
		b.tntPlate(12, -10, 0);
		b.set(12, -12, 1, st(Blocks.TNT));
		b.set(12, -12, -1, st(Blocks.TNT));
		b.fill(14, -10, -1, 14, -7, 1, st(Blocks.CRACKED_STONE_BRICKS));
		b.set(14, -6, 0, chis);
		// the vault
		b.air(15, -10, -6, 24, -5, 6);
		for (int x = 15; x <= 24; x++) for (int z = -6; z <= 6; z++) b.set(x, -11, z, (x + z) % 2 == 0 ? gold : chis);
		b.fill(20, -10, -1, 21, -10, 1, chis);
		b.set(20, -9, 0, gold);
		b.set(21, -9, 0, emerald);
		b.set(20, -8, 0, st(Blocks.EMERALD_BLOCK));
		b.chest(23, -10, -5, Direction.NORTH, TREASURE);
		b.chest(23, -10, 5, Direction.SOUTH, TREASURE);
		b.chest(17, -10, -5, Direction.NORTH, TREASURE);
		b.chest(17, -10, 5, Direction.SOUTH, TEMPLE);
		for (int[] g : new int[][]{{24, -6}, {24, 6}, {15, 6}, {15, -6}, {24, 0}}) b.set(g[0], -10, g[1], r.nextBoolean() ? emerald : gold);
		b.set(18, -10, 3, st(Blocks.GLOWSTONE));
		b.set(18, -10, -3, st(Blocks.GLOWSTONE));
		// way back up: ladder shaft into the ring corridor
		b.air(22, -10, 7, 22, 0, 7);
		b.ladder(22, -10, 0, 7, Direction.NORTH);

		// overgrowth: vines down the tier walls and bushes on the terraces
		for (int t = 0; t < 5; t++) {
			int half = 32 - 4 * t, top = 6 * t + 5;
			for (int d = -half; d <= half; d++) {
				if (noise(d, t, 1) > 0.55) b.vine(d, top, -half - 1, Direction.SOUTH, 2 + r.nextInt(5));
				if (noise(d, t, 2) > 0.55) b.vine(d, top, half + 1, Direction.NORTH, 2 + r.nextInt(5));
				if (noise(d, t, 3) > 0.55) b.vine(-half - 1, top, d, Direction.EAST, 2 + r.nextInt(5));
				if (noise(d, t, 4) > 0.55) b.vine(half + 1, top, d, Direction.WEST, 2 + r.nextInt(5));
			}
			if (t < 4) for (int k = 0; k < 14; k++) {
				int a = half - 1 - r.nextInt(2), c = r.nextInt(2 * half - 1) - half + 1;
				int[][] spots = {{c, -a}, {c, a}, {-a, c}, {a, c}};
				int[] s = spots[r.nextInt(4)];
				if (Math.abs(s[0]) <= 4 && s[1] < 0) continue;
				b.set(s[0], top + 1, s[1], Blocks.JUNGLE_LEAVES.defaultBlockState().setValue(BlockStateProperties.PERSISTENT, true));
			}
		}
	}

	// ================================================================== realm places
	/** Floating island with a flat top at y=0 (grass, soil, skystone), round and tapering downwards. */
	private void skyIsland(B b, int radius, int depth) {
		for (int dx = -radius; dx <= radius; dx++) for (int dz = -radius; dz <= radius; dz++) {
			double d = Math.sqrt(dx * dx + dz * dz) / radius;
			if (d > 1.0) continue;
			int bottom = (int) Math.round((1 - d * d) * depth + noise(dx, 0, dz) * 3);
			for (int y = -bottom; y <= 0; y++) {
				BlockState s = y == 0 ? st(net.glowcube.realms.registry.ModBlocks.LUMEN_GRASS) : y >= -3 ? st(net.glowcube.realms.registry.ModBlocks.LUMEN_SOIL)
						: noise(dx, y, dz) > 0.93 ? st(net.glowcube.realms.registry.ModBlocks.GLOWCRYSTAL_ORE) : st(net.glowcube.realms.registry.ModBlocks.SKYSTONE);
				b.set(dx, y, dz, s);
			}
		}
	}

	private void stall(B b, int x, int z, net.minecraft.world.item.DyeColor color, String loot) {
		for (int[] c : new int[][]{{-2, -2}, {2, -2}, {-2, 2}, {2, 2}}) b.fill(x + c[0], 1, z + c[1], x + c[0], 3, z + c[1], st(Blocks.SPRUCE_FENCE));
		b.fill(x - 2, 4, z - 2, x + 2, 4, z + 2, Blocks.WOOL_SLAB.pick(color).defaultBlockState());
		b.fill(x - 1, 1, z - 2, x + 1, 1, z - 2, st(Blocks.SPRUCE_PLANKS));
		b.barrel(x - 1, 1, z + 2, loot);
		b.set(x + 1, 1, z + 2, st(Blocks.CRAFTING_TABLE));
		b.set(x, 3, z, st(Blocks.LANTERN).setValue(BlockStateProperties.HANGING, true));
	}

	/** Lumen Skies: a market on its own floating island with two merchants, stalls and updraft vents. */
	private void skyMarket(B b) {
		BlockState bricks = st(net.glowcube.realms.registry.ModBlocks.SKYSTONE_BRICKS), chis = st(net.glowcube.realms.registry.ModBlocks.CHISELED_SKYSTONE);
		BlockState crystal = st(net.glowcube.realms.registry.ModBlocks.GLOWCRYSTAL_BLOCK);
		this.skyIsland(b, 24, 16);
		b.air(-24, 1, -24, 24, 14, 24);
		for (int dx = -13; dx <= 13; dx++) for (int dz = -13; dz <= 13; dz++) if (dx * dx + dz * dz <= 169) b.set(dx, 0, dz, (dx + dz) % 4 == 0 ? chis : bricks);
		// fountain
		b.fill(-2, 1, -2, 2, 1, 2, bricks);
		b.fill(-1, 1, -1, 1, 1, 1, st(Blocks.WATER));
		b.fill(0, 1, 0, 0, 3, 0, chis);
		b.set(0, 4, 0, crystal);
		this.stall(b, -8, -6, net.minecraft.world.item.DyeColor.LIGHT_BLUE, "glowcube_realms:chests/sky_market");
		this.stall(b, 8, -6, net.minecraft.world.item.DyeColor.MAGENTA, "glowcube_realms:chests/sky_market");
		this.stall(b, -8, 7, net.minecraft.world.item.DyeColor.YELLOW, "glowcube_realms:chests/sky_market");
		this.stall(b, 8, 7, net.minecraft.world.item.DyeColor.CYAN, "glowcube_realms:chests/sky_market");
		for (int a = 0; a < 8; a++) {
			int lx = (int) Math.round(Math.cos(a * Math.PI / 4) * 12), lz = (int) Math.round(Math.sin(a * Math.PI / 4) * 12);
			b.fill(lx, 1, lz, lx, 2, lz, st(Blocks.SPRUCE_FENCE));
			b.set(lx, 3, lz, st(Blocks.LANTERN));
		}
		// updraft vents at the edge and a lookout platform high above
		b.set(-17, 0, 0, st(net.glowcube.realms.registry.ModBlocks.CLOUD_VENT));
		b.set(17, 0, 0, st(net.glowcube.realms.registry.ModBlocks.CLOUD_VENT));
		b.fill(18, 20, -3, 23, 20, 3, bricks);

		b.chest(22, 21, 2, Direction.WEST, "glowcube_realms:chests/sky_ruin");
		b.set(19, 21, -2, st(Blocks.LANTERN));
		b.mob(net.glowcube.realms.registry.ModEntities.REALM_TRADER, -8, 1, -4);
		b.mob(net.glowcube.realms.registry.ModEntities.REALM_TRADER, 8, 1, 5);
	}

	/** Lumen Skies: small floating ruin with a vent in the middle and some treasure. */
	private void skyRuin(B b) {
		RandomSource r = this.layout();
		BlockState bricks = st(net.glowcube.realms.registry.ModBlocks.SKYSTONE_BRICKS), chis = st(net.glowcube.realms.registry.ModBlocks.CHISELED_SKYSTONE);
		this.skyIsland(b, 10, 9);
		b.air(-10, 1, -10, 10, 12, 10);
		for (int dx = -6; dx <= 6; dx++) for (int dz = -6; dz <= 6; dz++) if (noise(dx, 1, dz) > 0.25) b.set(dx, 0, dz, bricks);
		for (int[] p : new int[][]{{-5, -5}, {5, -5}, {-5, 5}, {5, 5}, {0, -6}, {0, 6}, {-6, 0}, {6, 0}}) {
			int h = 2 + r.nextInt(5);
			b.fill(p[0], 1, p[1], p[0], h, p[1], (p[0] + p[1]) % 2 == 0 ? chis : bricks);
		}
		b.fill(-5, 7, -5, 5, 7, -5, bricks);
		b.air(-1 + r.nextInt(3), 7, -5, 2 + r.nextInt(3), 7, -5);
		b.set(0, 0, 0, st(net.glowcube.realms.registry.ModBlocks.CLOUD_VENT));
		b.chest(3, 1, 3, Direction.NORTH, "glowcube_realms:chests/sky_ruin");
		b.set(-3, 1, -3, st(net.glowcube.realms.registry.ModBlocks.GLOWCRYSTAL_BLOCK));
		b.set(-3, 1, 3, st(net.glowcube.realms.registry.ModBlocks.LUMEN_BLOOM));
	}

	/** Umbral Depths: a market in a carved cavern with merchants and soul lanterns. */
	private void shadowBazaar(B b) {
		BlockState stone = st(net.glowcube.realms.registry.ModBlocks.UMBRAL_STONE), bricks = st(net.glowcube.realms.registry.ModBlocks.UMBRAL_BRICKS);
		BlockState chis = st(net.glowcube.realms.registry.ModBlocks.CHISELED_UMBRAL_STONE);
		for (int dx = -17; dx <= 17; dx++) for (int dz = -17; dz <= 17; dz++) for (int dy = -2; dy <= 11; dy++) {
			double e = (dx * dx + dz * dz) / 289.0 + (dy > 0 ? dy * dy / 121.0 : 0);
			if (e > 1.0) continue;
			if (dy == -2) b.foundation(dx, dz, -3, stone, 40);
			if (dy <= 0) b.set(dx, dy, dz, dy == 0 ? ((dx + dz) % 3 == 0 ? chis : bricks) : stone);
			else b.set(dx, dy, dz, e > 0.86 ? stone : st(Blocks.AIR));
		}
		// stalls, a shrine and hanging lights
		this.stall(b, -8, -6, net.minecraft.world.item.DyeColor.PURPLE, "glowcube_realms:chests/shadow_bazaar");
		this.stall(b, 8, -6, net.minecraft.world.item.DyeColor.BLACK, "glowcube_realms:chests/shadow_bazaar");
		this.stall(b, 0, 9, net.minecraft.world.item.DyeColor.MAGENTA, "glowcube_realms:chests/shadow_bazaar");
		b.fill(-1, 1, -1, 1, 1, 1, chis);
		b.set(0, 2, 0, st(net.glowcube.realms.registry.ModBlocks.VOIDSHARD_BLOCK));
		b.set(0, 3, 0, st(Blocks.SOUL_LANTERN));
		for (int a = 0; a < 10; a++) {
			int lx = (int) Math.round(Math.cos(a * Math.PI / 5) * 13), lz = (int) Math.round(Math.sin(a * Math.PI / 5) * 13);
			b.fill(lx, 1, lz, lx, 3, lz, st(Blocks.DARK_OAK_FENCE));
			b.set(lx, 4, lz, st(Blocks.SOUL_LANTERN));
		}
		// tunnels out of the cavern in four directions
		b.air(-1, 1, 15, 1, 3, 24);
		b.air(-1, 1, -24, 1, 3, -15);
		b.air(15, 1, -1, 24, 3, 1);
		b.air(-24, 1, -1, -15, 3, 1);
		b.mob(net.glowcube.realms.registry.ModEntities.REALM_TRADER, -8, 1, -4);
		b.mob(net.glowcube.realms.registry.ModEntities.REALM_TRADER, 8, 1, -4);
		b.mob(net.glowcube.realms.registry.ModEntities.REALM_TRADER, 0, 1, 7);
	}

	/** Umbral Depths: an abandoned mine with long tunnels, rails, supports, ore veins and a crawler nest. */
	private void umbralMine(B b) {
		RandomSource r = this.layout();
		BlockState planks = st(Blocks.DARK_OAK_PLANKS), fence = st(Blocks.DARK_OAK_FENCE), ore = st(net.glowcube.realms.registry.ModBlocks.VOIDSHARD_ORE);
		BlockState stone = st(net.glowcube.realms.registry.ModBlocks.UMBRAL_STONE);
		int[][] tunnels = {{-40, 0, 40, 0}, {0, -40, 0, 40}, {-24, -20, 24, -20}, {-24, 20, 24, 20}, {-20, -24, -20, 24}, {20, -24, 20, 24}};
		for (int[] t : tunnels) {
			boolean alongX = t[1] == t[3];
			int len = alongX ? t[2] - t[0] : t[3] - t[1];
			for (int i = 0; i <= len; i++) {
				int x = alongX ? t[0] + i : t[0], z = alongX ? t[1] : t[1] + i;
				for (int w = -1; w <= 1; w++) {
					int wx = alongX ? x : x + w, wz = alongX ? z + w : z;
					b.set(wx, -1, wz, noise(wx, -1, wz) > 0.75 ? st(Blocks.GRAVEL) : stone);
					b.air(wx, 0, wz, wx, 2, wz);
					// ore veins in the walls
					if (noise(wx, 3, wz) > 0.92) b.set(wx, 3, wz, ore);
				}
				int sx = alongX ? 0 : 2, sz = alongX ? 2 : 0;
				if (noise(x + sx, 1, z + sz) > 0.88) b.set(x + sx, 1, z + sz, ore);
				if (noise(x - sx, 1, z - sz) > 0.88) b.set(x - sx, 1, z - sz, ore);
				b.set(x, 0, z, Blocks.RAIL.defaultBlockState().setValue(BlockStateProperties.RAIL_SHAPE,
						alongX ? net.minecraft.world.level.block.state.properties.RailShape.EAST_WEST : net.minecraft.world.level.block.state.properties.RailShape.NORTH_SOUTH));
				if (i % 5 == 0) {
					int ax = alongX ? x : x - 1, az = alongX ? z - 1 : z, bx = alongX ? x : x + 1, bz = alongX ? z + 1 : z;
					b.fill(ax, 0, az, ax, 1, az, fence);
					b.fill(bx, 0, bz, bx, 1, bz, fence);
					for (int w = -1; w <= 1; w++) b.set(alongX ? x : x + w, 2, alongX ? z + w : z, planks);
					if (i % 10 == 0) b.set(ax, 1, az, st(Blocks.SOUL_LANTERN));
				}
			}
		}
		// crossings get no rails, so carts do not derail into walls
		for (int[] c : new int[][]{{0, 0}, {0, -20}, {0, 20}, {-20, 0}, {20, 0}, {-20, -20}, {20, -20}, {-20, 20}, {20, 20}}) b.set(c[0], 0, c[1], st(Blocks.AIR));
		// alcoves with chests and supplies
		for (int k = 0; k < 8; k++) {
			int[] t = tunnels[r.nextInt(tunnels.length)];
			boolean alongX = t[1] == t[3];
			int i = 3 + r.nextInt(Math.max(1, (alongX ? t[2] - t[0] : t[3] - t[1]) - 6));
			int x = alongX ? t[0] + i : t[0] + 2, z = alongX ? t[1] + 2 : t[1] + i;
			b.air(x, 0, z, x, 1, z);
			b.chest(x, 0, z, alongX ? Direction.NORTH : Direction.WEST, "glowcube_realms:chests/umbral_mine");
		}
		// foreman's room in the middle-north and a crawler nest in the south
		b.room(-5, -1, -32, 5, 4, -26, planks);
		b.air(-1, 0, -26, 1, 2, -26);
		b.air(-1, 0, -32, 1, 2, -32);
		b.set(-3, 0, -30, st(Blocks.CRAFTING_TABLE));
		b.set(-2, 0, -30, st(Blocks.FURNACE));
		b.chest(3, 0, -30, Direction.WEST, "glowcube_realms:chests/umbral_mine");
		b.barrel(3, 0, -28, "glowcube_realms:chests/umbral_mine");
		b.set(0, 3, -29, st(Blocks.SOUL_LANTERN).setValue(BlockStateProperties.HANGING, true));
		b.air(-4, -1, 26, 4, 3, 32);
		b.fill(-4, -2, 26, 4, -2, 32, stone);
		b.spawner(0, -1, 29, net.glowcube.realms.registry.ModEntities.SHADE_CRAWLER);
		for (int k = 0; k < 14; k++) b.set(r.nextInt(9) - 4, r.nextInt(4) - 1, 26 + r.nextInt(7), st(Blocks.COBWEB));
		b.chest(3, -1, 31, Direction.WEST, "glowcube_realms:chests/umbral_mine");
		b.fill(-4, 4, 30, 4, 4, 30, ore);
	}

	/** Sculk Realm: explorers' camp with a merchant, tents and a campfire. */
	private void echoCamp(B b) {
		for (int dx = -10; dx <= 10; dx++) for (int dz = -10; dz <= 10; dz++) {
			b.foundation(dx, dz, -1, st(Blocks.DEEPSLATE), 8);
			b.set(dx, 0, dz, noise(dx, 0, dz) > 0.5 ? st(Blocks.MOSS_BLOCK) : st(Blocks.SCULK));
			b.air(dx, 1, dz, dx, 8, dz);
		}
		b.set(0, 1, 0, st(Blocks.SOUL_CAMPFIRE));
		for (int[] l : new int[][]{{2, 0}, {-2, 0}, {0, 2}, {0, -2}}) b.set(l[0], 1, l[1], st(Blocks.SPRUCE_LOG));
		for (int[] t : new int[][]{{-6, -5}, {6, -5}, {-6, 6}}) {
			for (int k = 0; k < 3; k++) {
				b.fill(t[0] - 2 + k, 1 + k, t[1] - 2, t[0] - 2 + k, 1 + k, t[1] + 2, Blocks.WOOL.pick(net.minecraft.world.item.DyeColor.CYAN).defaultBlockState());
				b.fill(t[0] + 2 - k, 1 + k, t[1] - 2, t[0] + 2 - k, 1 + k, t[1] + 2, Blocks.WOOL.pick(net.minecraft.world.item.DyeColor.CYAN).defaultBlockState());
			}
			b.bed(t[0], 1, t[1] - 1, Direction.SOUTH, Blocks.BED.pick(net.minecraft.world.item.DyeColor.CYAN));
		}
		b.barrel(6, 1, 5, "glowcube_realms:chests/echo_camp");
		b.barrel(7, 1, 5, "glowcube_realms:chests/echo_camp");
		b.set(6, 1, 7, st(Blocks.CARTOGRAPHY_TABLE));
		b.set(5, 1, 7, st(Blocks.LECTERN));
		for (int[] l : new int[][]{{-9, 0}, {9, 0}, {0, 9}, {0, -9}}) {
			b.fill(l[0], 1, l[1], l[0], 2, l[1], st(Blocks.SPRUCE_FENCE));
			b.set(l[0], 3, l[1], st(Blocks.SOUL_LANTERN));
		}
		b.mob(net.glowcube.realms.registry.ModEntities.REALM_TRADER, 3, 1, 3);
	}

	/** Sculk Realm: ruins of an ancient city with shriekers, stalker spawners and echo treasure. */
	private void echoRuin(B b) {
		RandomSource r = this.layout();
		BlockState bricks = st(Blocks.DEEPSLATE_BRICKS), tiles = st(Blocks.DEEPSLATE_TILES), polished = st(Blocks.POLISHED_DEEPSLATE);
		BlockState cracked = st(Blocks.CRACKED_DEEPSLATE_BRICKS), reinforced = st(Blocks.REINFORCED_DEEPSLATE), sculk = st(Blocks.SCULK);
		for (int dx = -18; dx <= 18; dx++) for (int dz = -18; dz <= 18; dz++) {
			b.foundation(dx, dz, -1, bricks, 10);
			b.set(dx, 0, dz, noise(dx, 0, dz) > 0.7 ? sculk : (dx + dz) % 2 == 0 ? tiles : polished);
			b.air(dx, 1, dz, dx, 14, dz);
		}
		// broken walls around the plaza
		for (int d = -18; d <= 18; d++) for (int[] p : new int[][]{{d, -18}, {d, 18}, {-18, d}, {18, d}}) {
			int h = (int) (noise(p[0], 2, p[1]) * 7);
			if (Math.abs(d) <= 2) continue;
			b.fill(p[0], 1, p[1], p[0], h, p[1], noise(p[0], 3, p[1]) > 0.5 ? cracked : bricks);
		}
		// the great frame in the middle (an old portal of the realm)
		b.fill(-5, 1, -1, 5, 1, 1, polished);
		for (int y = 1; y <= 11; y++) {
			b.set(-5, y, 0, reinforced);
			b.set(5, y, 0, reinforced);
		}
		b.fill(-5, 11, 0, 5, 12, 0, reinforced);
		b.set(0, 12, 0, st(net.glowcube.realms.registry.ModBlocks.ECHO_CRYSTAL_BLOCK));
		// side halls with treasure, sensors and shriekers
		for (int side : new int[]{-1, 1}) {
			int cx = side * 11;
			b.room(cx - 4, 0, -6, cx + 4, 6, 6, bricks);
			b.air(cx - side * 4, 1, -1, cx - side * 4, 3, 1);
			for (int k = 0; k < 5; k++) b.set(cx - 4 + 2 * k, 6, -6 + r.nextInt(13), st(Blocks.AIR));
			b.chest(cx + side * 3, 1, 0, side < 0 ? Direction.EAST : Direction.WEST, "glowcube_realms:chests/echo_ruin");
			b.chest(cx, 1, 5, Direction.NORTH, "glowcube_realms:chests/echo_ruin");
			b.set(cx - 2, 1, -4, st(Blocks.SCULK_SHRIEKER));
			b.set(cx + 2, 1, -4, st(Blocks.SCULK_SENSOR));
			b.set(cx, 1, -4, st(Blocks.SOUL_LANTERN));
			b.spawner(cx, 1, 3, net.glowcube.realms.registry.ModEntities.SCULK_STALKER);
		}
		for (int k = 0; k < 10; k++) b.set(r.nextInt(31) - 15, 1, r.nextInt(31) - 15, k % 3 == 0 ? st(Blocks.SCULK_SENSOR) : st(Blocks.CANDLE).setValue(BlockStateProperties.LIT, true));
		for (int[] l : new int[][]{{-8, -12}, {8, -12}, {-8, 12}, {8, 12}}) {
			b.fill(l[0], 1, l[1], l[0], 4, l[1], st(Blocks.POLISHED_DEEPSLATE_WALL));
			b.set(l[0], 5, l[1], st(Blocks.SOUL_LANTERN));
		}
		b.chest(0, 1, -10, Direction.SOUTH, "glowcube_realms:chests/echo_ruin");
	}

	// ================================================================== Grand Igloo
	private static boolean inDome(int x, int y, int z, int cx, int cz, double rad, double h) {
		double dx = (x - cx) / rad, dz = (z - cz) / rad, dy = y / h;
		return y >= 0 && dx * dx + dy * dy + dz * dz <= 1.0;
	}

	private void igloo(B b) {
		BlockState snow = st(Blocks.SNOW_BLOCK), packed = st(Blocks.PACKED_ICE), bricks = st(Blocks.STONE_BRICKS), spruce = st(Blocks.SPRUCE_PLANKS);
		int[][] domes = {{0, 0, 8, 6}, {11, 1, 5, 4}, {-11, 1, 5, 4}};
		for (int x = -17; x <= 17; x++) for (int z = -13; z <= 13; z++) {
			boolean any = false;
			for (int[] d : domes) any |= inDome(x, 0, z, d[0], d[1], d[2], d[3]);
			if (any) b.foundation(x, z, -1, snow, 8);
			for (int y = 0; y <= 6; y++) {
				boolean outer = false, inner = false;
				for (int[] d : domes) {
					outer |= inDome(x, y, z, d[0], d[1], d[2], d[3]);
					inner |= inDome(x, y, z, d[0], d[1], d[2] - 1.2, d[3] - 1.0);
				}
				if (!outer) continue;
				if (y == 0) b.set(x, 0, z, inner ? spruce : packed);
				else b.set(x, y, z, inner ? st(Blocks.AIR) : y == 3 && noise(x, y, z) > 0.8 ? st(Blocks.ICE) : snow);
			}
		}
		// doorways between the domes and the entrance tunnel
		b.air(5, 1, 0, 8, 2, 1);
		b.air(-8, 1, 0, -5, 2, 1);
		b.fill(-2, 0, -12, 2, 3, -7, snow);
		b.air(-1, 1, -13, 1, 2, -6);
		b.fill(-1, 0, -12, 1, 0, -7, packed);
		// main room
		b.bed(4, 1, 2, Direction.SOUTH, Blocks.BED.pick(net.minecraft.world.item.DyeColor.RED));
		b.set(-4, 1, 2, facing(Blocks.FURNACE, Direction.EAST));
		b.set(-4, 1, 3, st(Blocks.CRAFTING_TABLE));
		b.set(3, 1, -4, st(Blocks.LANTERN));
		b.set(-3, 1, -4, st(Blocks.LANTERN));
		b.fill(-2, 1, -2, 2, 1, 1, st(Blocks.CARPET.pick(net.minecraft.world.item.DyeColor.WHITE)));
		b.set(0, 1, -2, st(Blocks.CARPET.pick(net.minecraft.world.item.DyeColor.RED)));
		// east dome: library, west dome: storage
		b.fill(14, 1, -1, 14, 2, 3, st(Blocks.BOOKSHELF));
		b.set(12, 1, 3, facing(Blocks.LECTERN, Direction.NORTH));
		b.set(12, 1, -1, st(Blocks.LANTERN));
		b.barrel(-14, 1, 0, "minecraft:chests/igloo_chest");
		b.barrel(-14, 1, 2, "minecraft:chests/igloo_chest");
		b.chest(-13, 1, 3, Direction.NORTH, "glowcube_realms:chests/igloo_lab");
		b.set(-12, 1, -1, st(Blocks.LANTERN));
		// hidden trapdoor and ladder down to the laboratory
		b.set(0, 0, 5, Blocks.SPRUCE_TRAPDOOR.defaultBlockState().setValue(BlockStateProperties.HALF, Half.TOP)
				.setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH));
		b.fill(-8, -11, 3, 8, -1, 19, bricks);
		b.air(0, -9, 5, 0, -1, 5);
		b.ladder(0, -9, -1, 5, Direction.SOUTH);
		b.air(-7, -9, 6, 7, -6, 18);
		for (int x = -7; x <= 7; x++) for (int z = 6; z <= 18; z++) if (noise(x, -10, z) > 0.7) b.set(x, -10, z, st(Blocks.MOSSY_STONE_BRICKS));
		b.air(0, -9, 5, 0, -8, 6);
		// lab equipment
		b.set(-6, -9, 8, st(Blocks.BREWING_STAND));
		b.set(-6, -9, 9, st(Blocks.CAULDRON));
		b.set(-6, -9, 10, st(Blocks.BREWING_STAND));
		b.fill(6, -9, 7, 6, -8, 11, st(Blocks.BOOKSHELF));
		b.set(-2, -9, 10, st(Blocks.CRAFTING_TABLE));
		b.set(2, -9, 10, st(Blocks.SMITHING_TABLE));
		b.chest(0, -9, 18, Direction.NORTH, "glowcube_realms:chests/igloo_lab");
		b.chest(-1, -9, 18, Direction.NORTH, "minecraft:chests/igloo_chest");
		for (int[] l : new int[][]{{-4, 7}, {4, 7}, {-4, 16}, {4, 16}}) b.set(l[0], -9, l[1], st(Blocks.LANTERN));
		// two cells: a villager and a zombie villager, like the vanilla basement
		b.fill(-7, -9, 13, -3, -6, 13, bricks);
		b.fill(3, -9, 13, 7, -6, 13, bricks);
		b.rail(-6, -9, 13, -4, 13, Blocks.IRON_BARS);
		b.rail(-6, -8, 13, -4, 13, Blocks.IRON_BARS);
		b.rail(4, -9, 13, 6, 13, Blocks.IRON_BARS);
		b.rail(4, -8, 13, 6, 13, Blocks.IRON_BARS);
		b.fill(-3, -9, 14, -3, -6, 17, bricks);
		b.fill(3, -9, 14, 3, -6, 17, bricks);
		b.mob(EntityTypes.VILLAGER, -5, -9, 15);
		b.mob(EntityTypes.ZOMBIE_VILLAGER, 5, -9, 15);
	}

	// ================================================================== Witch Manor (swamp hut)
	private void witch(B b) {
		BlockState spruce = st(Blocks.SPRUCE_PLANKS), dark = st(Blocks.DARK_OAK_PLANKS), log = st(Blocks.DARK_OAK_LOG), oakLog = st(Blocks.OAK_LOG);
		final int F = 4;
		// stilts
		for (int[] p : new int[][]{{-6, -5}, {6, -5}, {-6, 5}, {6, 5}, {0, -5}, {0, 5}, {-6, 0}, {6, 0}, {-3, -8}, {3, -8}}) {
			b.fill(p[0], 0, p[1], p[0], F - 1, p[1], oakLog);
			b.foundation(p[0], p[1], -1, oakLog, 12);
		}
		// floors, porch and ladder
		b.fill(-6, F, -5, 6, F, 5, spruce);
		b.fill(-3, F, -8, 3, F, -6, dark);
		for (int y = F - 1; y >= F - 16 && !b.solid(0, y, -9); y--) b.set(0, y, -9, facing(Blocks.LADDER, Direction.NORTH));
		b.set(0, F, -9, facing(Blocks.LADDER, Direction.NORTH));
		for (int y = F - 1; y >= F - 16 && !b.solid(0, y, -8); y--) b.set(0, y, -8, oakLog);
		for (int dx : new int[]{-3, -2, -1, 1, 2, 3}) b.set(dx, F + 1, -8, st(Blocks.DARK_OAK_FENCE));
		b.set(-3, F + 1, -7, st(Blocks.DARK_OAK_FENCE));
		b.set(3, F + 1, -7, st(Blocks.DARK_OAK_FENCE));
		b.set(-3, F + 2, -8, st(Blocks.LANTERN));
		b.set(3, F + 2, -8, st(Blocks.LANTERN));
		// two storeys of walls with a dark oak frame
		for (int level = 0; level < 2; level++) {
			int y0 = F + 1 + level * 6;
			for (int y = y0; y < y0 + 5; y++) for (int dx = -5; dx <= 5; dx++) for (int dz = -4; dz <= 4; dz++) {
				boolean wall = Math.abs(dx) == 5 || Math.abs(dz) == 4;
				if (!wall) {
					b.set(dx, y, dz, st(Blocks.AIR));
					continue;
				}
				boolean post = (Math.abs(dx) == 5 && Math.abs(dz) == 4) || (Math.abs(dz) == 4 && dx % 3 == 0) || (Math.abs(dx) == 5 && dz == 0);
				boolean window = !post && (y == y0 + 1 || y == y0 + 2) && (Math.abs(dz) == 4 ? Math.abs(dx) == 2 : Math.abs(dz) == 2);
				b.set(dx, y, dz, post ? log : window ? st(Blocks.STAINED_GLASS.pick(net.minecraft.world.item.DyeColor.PURPLE)) : spruce);
			}
			b.fill(-5, y0 + 5, -4, 5, y0 + 5, 4, dark);
		}
		b.door(0, F + 1, -4, Direction.NORTH, Blocks.DARK_OAK_DOOR);
		// gable roof along x
		for (int k = 0; k <= 5; k++) {
			int y = F + 13 + k;
			for (int dx = -6; dx <= 6; dx++) {
				b.stairs(dx, y, -6 + k, Blocks.DARK_OAK_STAIRS, Direction.SOUTH);
				b.stairs(dx, y, 6 - k, Blocks.DARK_OAK_STAIRS, Direction.NORTH);
			}
			for (int z = -5 + k; z <= 5 - k; z++) {
				b.set(-5, y, z, spruce);
				b.set(5, y, z, spruce);
			}
		}
		b.fill(-6, F + 18, 0, 6, F + 18, 0, dark);
		b.fill(-6, F + 19, 0, 6, F + 19, 0, Blocks.DARK_OAK_SLAB.defaultBlockState().setValue(BlockStateProperties.SLAB_TYPE, SlabType.BOTTOM));
		b.set(0, F + 13, 0, st(Blocks.LANTERN).setValue(BlockStateProperties.HANGING, false));
		// ground floor: potion workshop
		b.set(-4, F + 1, -3, st(Blocks.CAULDRON));
		b.set(-3, F + 1, -3, st(Blocks.BREWING_STAND));
		b.set(-4, F + 1, -2, st(Blocks.BREWING_STAND));
		b.fill(4, F + 1, -3, 4, F + 2, 1, st(Blocks.BOOKSHELF));
		b.set(4, F + 1, 3, st(Blocks.CRAFTING_TABLE));
		b.chest(3, F + 1, 3, Direction.WEST, "glowcube_realms:chests/witch_manor");
		b.set(-2, F + 1, 3, st(Blocks.POTTED_RED_MUSHROOM));
		b.set(-1, F + 1, 3, st(Blocks.POTTED_BROWN_MUSHROOM));
		b.set(0, F + 1, 3, st(Blocks.POTTED_DEAD_BUSH));
		b.set(-3, F + 1, 3, st(Blocks.CANDLE).setValue(BlockStateProperties.LIT, true).setValue(BlockStateProperties.CANDLES, 3));
		b.set(2, F + 1, -3, st(Blocks.CAULDRON));
		// ladder to the upper floor
		b.ladder(-4, F + 1, F + 6, 2, Direction.EAST);
		b.set(-4, F + 6, 2, facing(Blocks.LADDER, Direction.EAST));
		// upper floor: bedroom and herb store
		int u = F + 7;
		b.bed(2, u, -2, Direction.SOUTH, Blocks.BED.pick(net.minecraft.world.item.DyeColor.PURPLE));
		b.chest(4, u, 3, Direction.WEST, "glowcube_realms:chests/witch_manor");
		b.set(-1, u, -3, st(Blocks.CAULDRON));
		b.barrel(4, u, -3, "glowcube_realms:chests/witch_manor");
		b.set(-2, u, 3, st(Blocks.COBWEB));
		b.set(0, u, 3, st(Blocks.POTTED_RED_MUSHROOM));
		b.set(-4, u, -3, st(Blocks.LANTERN));
		b.mob(EntityTypes.WITCH, -1, F + 1, 0);
		b.mob(EntityTypes.CAT, 1, u, 1);
	}

	// ================================================================== Pillager Fortress (outpost)
	private void fortress(B b) {
		RandomSource r = this.layout();
		BlockState log = st(Blocks.DARK_OAK_LOG), planks = st(Blocks.DARK_OAK_PLANKS), cobble = st(Blocks.COBBLESTONE), mossyC = st(Blocks.MOSSY_COBBLESTONE);
		BlockState birch = st(Blocks.BIRCH_PLANKS), grass = st(Blocks.GRASS_BLOCK), path = st(Blocks.DIRT_PATH);
		// level the yard
		for (int dx = -22; dx <= 22; dx++) for (int dz = -22; dz <= 22; dz++) {
			b.foundation(dx, dz, -1, st(Blocks.DIRT), 12);
			b.set(dx, 0, dz, Math.abs(dx) <= 1 || Math.abs(dz) <= 1 ? path : grass);
			b.air(dx, 1, dz, dx, 14, dz);
		}
		// palisade with a gate on the north side
		for (int d = -22; d <= 22; d++) {
			int h = 5 + (d % 2 == 0 ? 1 : 0);
			for (int[] p : new int[][]{{d, -22}, {d, 22}, {-22, d}, {22, d}}) {
				b.foundation(p[0], p[1], -1, cobble, 10);
				b.fill(p[0], 1, p[1], p[0], h, p[1], log);
			}
		}
		b.air(-2, 1, -22, 2, 4, -22);
		b.fill(-3, 5, -22, 3, 6, -22, cobble);
		// corner watchtowers
		for (int sx : new int[]{-19, 19}) for (int sz : new int[]{-19, 19}) {
			b.room(sx - 2, 0, sz - 2, sx + 2, 13, sz + 2, planks);
			for (int[] c : new int[][]{{-2, -2}, {2, -2}, {-2, 2}, {2, 2}}) b.fill(sx + c[0], 0, sz + c[1], sx + c[0], 14, sz + c[1], log);
			b.fill(sx - 2, 0, sz - 2, sx + 2, 2, sz + 2, cobble);
			b.air(sx - 1, 1, sz - 1, sx + 1, 12, sz + 1);
			b.fill(sx - 3, 13, sz - 3, sx + 3, 13, sz + 3, planks);
			for (int d = -3; d <= 3; d += 2) {
				b.set(sx + d, 14, sz - 3, log);
				b.set(sx + d, 14, sz + 3, log);
				b.set(sx - 3, 14, sz + d, log);
				b.set(sx + 3, 14, sz + d, log);
			}
			int doorX = sx + (sx < 0 ? 2 : -2);
			b.air(doorX, 1, sz, doorX, 2, sz);
			b.ladder(sx, 1, 13, sz + (sz < 0 ? -1 : 1), sz < 0 ? Direction.SOUTH : Direction.NORTH);
			b.air(sx, 13, sz + (sz < 0 ? -1 : 1), sx, 13, sz + (sz < 0 ? -1 : 1));
			b.ladder(sx, 13, 13, sz + (sz < 0 ? -1 : 1), sz < 0 ? Direction.SOUTH : Direction.NORTH);
			b.chest(sx, 1, sz, Direction.NORTH, "minecraft:chests/pillager_outpost");
		}
		// central keep: four floors and a roof platform
		b.fill(-6, 0, -6, 6, 3, 6, cobble);
		b.fill(-6, 4, -6, 6, 29, 6, planks);
		for (int[] c : new int[][]{{-6, -6}, {6, -6}, {-6, 6}, {6, 6}, {0, -6}, {0, 6}, {-6, 0}, {6, 0}}) b.fill(c[0], 0, c[1], c[0], 31, c[1], log);
		for (int f = 0; f < 4; f++) {
			int y0 = f * 7;
			b.air(-5, y0 + 1, -5, 5, y0 + 6, 5);
			b.fill(-5, y0, -5, 5, y0, 5, f == 0 ? cobble : birch);
			for (int d = -4; d <= 4; d += 4) {
				b.air(d, y0 + 3, -6, d, y0 + 4, -6);
				b.air(d, y0 + 3, 6, d, y0 + 4, 6);
				if (d != 0) b.air(-6, y0 + 3, d, -6, y0 + 4, d);
				b.air(6, y0 + 3, d, 6, y0 + 4, d);
			}
			b.set(3, y0 + 1, 3, st(Blocks.LANTERN));
			b.set(-3, y0 + 1, -3, st(Blocks.LANTERN));
		}
		b.fill(-6, 28, -6, 6, 28, 6, birch);
		b.fill(-7, 29, -7, 7, 29, 7, planks);
		for (int d = -7; d <= 7; d += 2) {
			b.set(d, 30, -7, log);
			b.set(d, 30, 7, log);
			b.set(-7, 30, d, log);
			b.set(7, 30, d, log);
		}
		b.door(0, 1, -6, Direction.NORTH, Blocks.DARK_OAK_DOOR);
		b.air(-5, 1, 0, -5, 29, 0);
		b.ladder(-5, 1, 29, 0, Direction.EAST);
		// floor 0: hall
		b.set(4, 1, -4, st(Blocks.CRAFTING_TABLE));
		b.set(4, 1, -3, st(Blocks.STONECUTTER));
		b.barrel(4, 1, 4, "glowcube_realms:chests/fortress_armory");
		b.barrel(3, 1, 4, "minecraft:chests/pillager_outpost");
		// floor 1: sleeping quarters
		for (int k = 0; k < 4; k++) b.bed(-3 + 2 * k, 8, 3, Direction.SOUTH, Blocks.BED.pick(net.minecraft.world.item.DyeColor.GRAY));
		b.chest(4, 8, -4, Direction.WEST, "minecraft:chests/pillager_outpost");
		// floor 2: armory
		b.chest(4, 15, -4, Direction.WEST, "glowcube_realms:chests/fortress_armory");
		b.chest(4, 15, 4, Direction.WEST, "glowcube_realms:chests/fortress_armory");
		b.set(-3, 15, 4, st(Blocks.FLETCHING_TABLE));
		b.set(-2, 15, 4, st(Blocks.SMITHING_TABLE));
		b.set(0, 15, 4, st(Blocks.ANVIL));
		// floor 3: captain's room
		b.chest(0, 22, 4, Direction.NORTH, "glowcube_realms:chests/fortress_armory");
		b.chest(1, 22, 4, Direction.NORTH, "minecraft:chests/pillager_outpost");
		b.set(-3, 22, 4, st(Blocks.CARTOGRAPHY_TABLE));
		b.set(3, 22, -4, st(Blocks.WOOL.pick(net.minecraft.world.item.DyeColor.BLACK)));
		// cages: an iron golem and allays, like the outpost
		this.cage(b, 12, -12, 3);
		this.cage(b, -12, -12, 2);
		this.cage(b, -12, 12, 2);
		b.mob(EntityTypes.IRON_GOLEM, 12, 1, -12);
		b.mob(EntityTypes.ALLAY, -12, 1, -12);
		b.mob(EntityTypes.ALLAY, -12, 1, 12);
		// tents and target practice
		for (int[] t : new int[][]{{12, 8}, {12, 15}}) {
			for (int k = 0; k < 3; k++) {
				b.fill(t[0] - 2 + k, 1 + k, t[1] - 2, t[0] - 2 + k, 1 + k, t[1] + 2, st(Blocks.WOOL.pick(net.minecraft.world.item.DyeColor.WHITE)));
				b.fill(t[0] + 2 - k, 1 + k, t[1] - 2, t[0] + 2 - k, 1 + k, t[1] + 2, st(Blocks.WOOL.pick(net.minecraft.world.item.DyeColor.WHITE)));
			}
			b.set(t[0], 1, t[1] - 1, st(Blocks.CRAFTING_TABLE));
		}
		for (int k = 0; k < 3; k++) {
			b.set(-16 + 3 * k, 1, 5, st(Blocks.HAY_BLOCK));
			b.set(-16 + 3 * k, 2, 5, st(Blocks.TARGET));
		}
		for (int k = 0; k < 6; k++) b.set(r.nextInt(30) - 15, 1, r.nextInt(30) - 15, mossyC);
		b.mob(EntityTypes.PILLAGER, 2, 1, 2);
		b.mob(EntityTypes.PILLAGER, -2, 15, -2);
		b.mob(EntityTypes.VINDICATOR, 2, 22, -2);
		b.mob(EntityTypes.PILLAGER, 0, 30, 0);
	}

	private void cage(B b, int cx, int cz, int half) {
		for (int y = 1; y <= half + 1; y++) {
			b.rail(cx - half, y, cz - half, cx + half, cz - half, Blocks.DARK_OAK_FENCE);
			b.rail(cx - half, y, cz + half, cx + half, cz + half, Blocks.DARK_OAK_FENCE);
			b.rail(cx - half, y, cz - half, cx - half, cz + half, Blocks.DARK_OAK_FENCE);
			b.rail(cx + half, y, cz - half, cx + half, cz + half, Blocks.DARK_OAK_FENCE);
		}
		b.fill(cx - half, half + 2, cz - half, cx + half, half + 2, cz + half, Blocks.DARK_OAK_SLAB.defaultBlockState());
	}
}
