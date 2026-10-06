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
			case "pyramid" -> { r = 42; lo = -16; hi = 33; }
			case "jungle" -> { r = 32; lo = -14; hi = 31; }
			case "igloo" -> { r = 17; lo = -13; hi = 10; }
			case "witch" -> { r = 11; lo = -12; hi = 23; }
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

	// ================================================================== Grand Desert Pyramid
	private void pyramid(B b) {
		RandomSource r = this.layout();
		BlockState ss = st(Blocks.SANDSTONE), smooth = st(Blocks.SMOOTH_SANDSTONE), cut = st(Blocks.CUT_SANDSTONE), chis = st(Blocks.CHISELED_SANDSTONE);
		BlockState orange = st(Blocks.DYED_TERRACOTTA.pick(net.minecraft.world.item.DyeColor.ORANGE)), blue = st(Blocks.DYED_TERRACOTTA.pick(net.minecraft.world.item.DyeColor.BLUE)), gold = st(Blocks.GOLD_BLOCK);
		final int H = 30;
		// foundation and an underground plinth that holds the crypt levels
		for (int dx = -H; dx <= H; dx++) for (int dz = -H; dz <= H; dz++) b.foundation(dx, dz, -1, ss, 16);
		b.fill(-23, -13, -23, 23, -1, 23, ss);
		// stepped body with terracotta bands
		for (int y = 0; y <= H; y++) {
			int s = H - y;
			for (int dx = -s; dx <= s; dx++) for (int dz = -s; dz <= s; dz++) {
				int m = Math.max(Math.abs(dx), Math.abs(dz));
				BlockState state = ss;
				if (m >= s - 1) state = y % 6 == 3 && m == s ? orange : y % 6 == 4 && m == s ? cut : smooth;
				b.set(dx, y, dz, state);
			}
		}
		b.set(0, H + 1, 0, gold);

		// forecourt with obelisks and the gate (north)
		for (int dx = -6; dx <= 6; dx++) for (int dz = -42; dz <= -30; dz++) {
			b.foundation(dx, dz, -1, ss, 12);
			b.set(dx, 0, dz, (dx + dz) % 4 == 0 ? orange : cut);
			if (dz < -30 - (6 - Math.abs(dx)) / 6) b.air(dx, 1, dz, dx, 9, dz);
		}
		for (int sx : new int[]{-5, 5}) {
			b.fill(sx, 1, -39, sx, 8, -39, cut);
			b.set(sx, 9, -39, chis);
			b.set(sx, 10, -39, gold);
		}
		b.fill(-3, 0, -33, 3, 7, -29, cut);
		b.fill(-3, 6, -33, 3, 6, -33, orange);
		b.set(-2, 5, -33, chis);
		b.set(2, 5, -33, chis);
		b.set(0, 6, -33, blue);
		b.air(-1, 1, -34, 1, 4, -21);

		// maze: 11 x 11 cells of 3-wide corridors on the ground level
		final int N = 11;
		boolean[][] seen = new boolean[N][N];
		List<int[]> passages = new ArrayList<>();
		ArrayDeque<int[]> stack = new ArrayDeque<>();
		stack.push(new int[]{5, 0});
		seen[5][0] = true;
		int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
		int[] links = new int[N * N];
		while (!stack.isEmpty()) {
			int[] c = stack.peek();
			List<int[]> open = new ArrayList<>();
			for (int[] d : dirs) {
				int ni = c[0] + d[0], nj = c[1] + d[1];
				if (ni >= 0 && nj >= 0 && ni < N && nj < N && !seen[ni][nj]) open.add(new int[]{ni, nj});
			}
			if (open.isEmpty()) {
				stack.pop();
				continue;
			}
			int[] n = open.get(r.nextInt(open.size()));
			seen[n[0]][n[1]] = true;
			passages.add(new int[]{c[0], c[1], n[0], n[1]});
			links[c[0] * N + c[1]]++;
			links[n[0] * N + n[1]]++;
			stack.push(n);
		}
		// a few extra openings make loops, so the maze is not a single path
		for (int k = 0; k < 14; k++) {
			int i = r.nextInt(N - 1), j = r.nextInt(N);
			passages.add(r.nextBoolean() ? new int[]{i, j, i + 1, j} : new int[]{j, i, j, i + 1});
		}
		for (int i = 0; i < N; i++) for (int j = 0; j < N; j++) {
			int cx = -20 + 4 * i, cz = -20 + 4 * j;
			b.air(cx - 1, 1, cz - 1, cx + 1, 4, cz + 1);
			for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++)
				b.set(cx + dx, 0, cz + dz, noise(cx + dx, 0, cz + dz) > 0.82 ? orange : smooth);
		}
		for (int[] p : passages) {
			int x1 = -20 + 4 * p[0], z1 = -20 + 4 * p[1], x2 = -20 + 4 * p[2], z2 = -20 + 4 * p[3];
			int mx = (x1 + x2) / 2, mz = (z1 + z2) / 2;
			if (x1 == x2) b.air(mx - 1, 1, mz, mx + 1, 4, mz);
			else b.air(mx, 1, mz - 1, mx, 4, mz + 1);
		}
		// dead ends hold small finds: chests, brushable sand, husk crypts
		int deadEnds = 0;
		for (int i = 0; i < N; i++) for (int j = 0; j < N; j++) {
			if (links[i * N + j] != 1 || (Math.abs(i - 5) <= 1 && Math.abs(j - 5) <= 1) || (i == 10 && j == 10) || (i == 5 && j == 0)) continue;
			int cx = -20 + 4 * i, cz = -20 + 4 * j;
			int roll = r.nextInt(4);
			if (roll == 0 && deadEnds < 6) {
				b.chest(cx, 1, cz, Direction.NORTH, "minecraft:chests/desert_pyramid");
				deadEnds++;
			} else if (roll == 1) {
				b.suspicious(cx, 0, cz, Blocks.SUSPICIOUS_SAND, "minecraft:archaeology/desert_pyramid");
				b.suspicious(cx + 1, 0, cz, Blocks.SUSPICIOUS_SAND, "minecraft:archaeology/desert_pyramid");
			} else if (roll == 2) {
				b.set(cx, 1, cz, st(Blocks.DECORATED_POT));
				b.set(cx - 1, 1, cz + 1, st(Blocks.DECORATED_POT));
			}
		}
		// a few lanterns so the corridors are not pitch black everywhere
		for (int k = 0; k < 12; k++) b.set(-20 + 4 * r.nextInt(N) + 1, 1, -20 + 4 * r.nextInt(N) + 1, st(Blocks.LANTERN));

		// great hall in the middle, with the classic TNT trap under the star
		b.air(-5, 1, -5, 5, 10, 5);
		for (int dx = -5; dx <= 5; dx++) for (int dz = -5; dz <= 5; dz++) {
			int m = Math.abs(dx) + Math.abs(dz);
			b.set(dx, 0, dz, m <= 1 ? blue : m <= 3 && (dx == 0 || dz == 0) ? orange : (dx + dz) % 2 == 0 ? smooth : cut);
		}
		for (int px : new int[]{-3, 3}) for (int pz : new int[]{-3, 3}) {
			b.fill(px, 1, pz, px, 10, pz, cut);
			b.set(px, 1, pz, chis);
			b.set(px, 10, pz, chis);
			b.set(px, 6, pz, orange);
		}
		b.tntPlate(0, 1, 0);
		for (int[] t : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) b.set(t[0], -1, t[1], st(Blocks.TNT));
		b.chest(-5, 1, -1, Direction.EAST, "minecraft:chests/desert_pyramid");
		b.chest(5, 1, 1, Direction.WEST, "minecraft:chests/desert_pyramid");
		b.chest(1, 1, -5, Direction.SOUTH, "minecraft:chests/desert_pyramid");
		b.chest(-1, 1, 5, Direction.NORTH, "minecraft:chests/desert_pyramid");
		for (int[] l : new int[][]{{-2, -3}, {2, 3}, {3, -2}, {-3, 2}}) b.set(l[0], 1, l[1], st(Blocks.LANTERN));

		// king's chamber above the hall, reached by a ladder on a pillar
		b.air(-5, 12, -5, 5, 17, 5);
		b.air(-2, 11, -3, -2, 11, -3);
		b.set(-3, 11, -3, cut);
		b.set(-3, 12, -3, cut);
		b.ladder(-2, 1, 12, -3, Direction.EAST);
		for (int dx = -5; dx <= 5; dx++) for (int dz = -5; dz <= 5; dz++) if ((dx + dz) % 3 == 0 && (dx != -2 || dz != -3)) b.set(dx, 11, dz, orange);
		for (int dx = -5; dx <= 5; dx++) {
			b.set(dx, 15, -6, chis);
			b.set(dx, 15, 6, chis);
			b.set(-6, 15, dx, chis);
			b.set(6, 15, dx, chis);
		}
		b.fill(-1, 12, -1, 1, 12, 2, smooth);
		b.fill(-1, 13, -1, 1, 13, 2, st(Blocks.CUT_SANDSTONE_SLAB));
		b.set(0, 13, -1, chis);
		b.set(0, 13, 2, chis);
		for (int[] g : new int[][]{{-5, -5}, {5, -5}, {-5, 5}, {5, 5}}) {
			b.set(g[0], 12, g[1], gold);
			b.set(g[0], 13, g[1], st(Blocks.LANTERN));
		}
		b.chest(4, 12, 0, Direction.WEST, "glowcube_realms:chests/grand_pyramid_treasure");
		b.chest(-4, 12, 1, Direction.EAST, "minecraft:chests/desert_pyramid");

		// stairs from the far maze corner down into the crypt
		for (int k = 0; k <= 9; k++) {
			int x = 18 - k, fy = -k;
			b.air(x, fy + 1, 19, x, fy + 4, 21);
			for (int z = 19; z <= 21; z++) b.stairs(x, fy, z, Blocks.SANDSTONE_STAIRS, Direction.EAST);
		}
		// trap corridor 1: tripwire arrow traps
		b.air(-1, -8, 19, 8, -5, 21);
		b.fill(-1, -9, 19, 8, -9, 21, smooth);
		b.tripwireTrap(6, -8, 18, 6, 22);
		b.tripwireTrap(2, -8, 18, 2, 22);
		b.set(4, -5, 20, st(Blocks.LANTERN).setValue(BlockStateProperties.HANGING, true));
		// trap corridor 2: lava pit to jump over, then pressure plates over TNT before the door
		b.air(-1, -8, 7, 1, -5, 18);
		b.fill(-1, -9, 7, 1, -9, 18, smooth);
		b.air(-1, -11, 12, 1, -9, 14);
		b.fill(-1, -12, 12, 1, -12, 14, st(Blocks.LAVA));
		for (int dx = -1; dx <= 1; dx++) {
			b.tntPlate(dx, -8, 8);
			b.set(dx, -10, 8, st(Blocks.TNT));
		}
		// treasure chamber
		b.air(-6, -8, -6, 6, -3, 6);
		for (int dx = -6; dx <= 6; dx++) for (int dz = -6; dz <= 6; dz++) {
			int m = Math.max(Math.abs(dx), Math.abs(dz));
			b.set(dx, -9, dz, m % 2 == 0 ? blue : (dx + dz) % 2 == 0 ? gold : orange);
		}
		b.air(-1, -8, 6, 1, -6, 7);
		for (int px : new int[]{-4, 4}) for (int pz : new int[]{-4, 4}) {
			b.fill(px, -8, pz, px, -3, pz, cut);
			b.set(px, -8, pz, chis);
			b.set(px, -3, pz, chis);
		}
		b.fill(-1, -8, -2, 1, -7, 1, smooth);
		b.fill(-1, -6, -2, 1, -6, 1, st(Blocks.SMOOTH_SANDSTONE_SLAB));
		b.set(0, -6, -2, gold);
		b.spawner(0, -8, -5, EntityTypes.HUSK);
		b.chest(-3, -8, -6, Direction.SOUTH, "glowcube_realms:chests/grand_pyramid_treasure");
		b.chest(3, -8, -6, Direction.SOUTH, "glowcube_realms:chests/grand_pyramid_treasure");
		b.chest(-6, -8, 0, Direction.EAST, "glowcube_realms:chests/grand_pyramid_treasure");
		b.chest(6, -8, 0, Direction.WEST, "minecraft:chests/desert_pyramid");
		for (int[] g : new int[][]{{-5, -5}, {5, -5}, {-5, 4}, {5, 4}, {-5, -4}, {5, -4}}) b.set(g[0], -8, g[1], r.nextBoolean() ? gold : st(Blocks.RAW_GOLD_BLOCK));
		for (int[] l : new int[][]{{-2, 4}, {2, 4}, {-3, -3}, {3, -3}}) b.set(l[0], -8, l[1], st(Blocks.LANTERN));
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
		for (int dx = -22; dx <= 22; dx++) for (int dz = -22; dz <= 22; dz++)
			for (int i = 1; i <= 16 && !b.solid(dx, -i, dz); i++) b.set(dx, -i, dz, this.mossy(dx, -i, dz));
		this.mossyFill(b, -20, -13, -20, 20, -1, 20);
		// four stepped tiers
		for (int t = 0; t < 4; t++) {
			int half = 22 - 4 * t;
			this.mossyFill(b, -half, 6 * t, -half, half, 6 * t + 5, half);
			for (int d = -half; d <= half; d += 4) {
				b.set(d, 6 * t + 5, -half, chis);
				b.set(d, 6 * t + 5, half, chis);
				b.set(-half, 6 * t + 5, d, chis);
				b.set(half, 6 * t + 5, d, chis);
			}
		}
		// grand stair up the north face to the shrine
		for (int k = 0; k <= 24; k++) {
			int z = -31 + k;
			for (int dx = -2; dx <= 2; dx++) {
				for (int y = 0; y < k; y++) if (z < -22 || !b.solid(dx, y, z)) b.set(dx, y, z, this.mossy(dx, y, z));
				b.foundation(dx, z, -1, this.mossy(dx, 0, z), 10);
				b.stairs(dx, k, z, Math.abs(dx) == 2 ? Blocks.STONE_BRICK_STAIRS : Blocks.MOSSY_COBBLESTONE_STAIRS, Direction.SOUTH);
				b.air(dx, k + 1, z, dx, k + 4, z);
			}
		}
		// shrine on the top
		b.room(-7, 24, -7, 7, 31, 7, bricks);
		b.fill(-8, 31, -8, 8, 31, 8, st(Blocks.MOSSY_STONE_BRICK_SLAB));
		b.air(-1, 25, -7, 1, 27, -7);
		for (int[] p : new int[][]{{-4, -4}, {4, -4}, {-4, 4}, {4, 4}}) b.fill(p[0], 25, p[1], p[0], 30, p[1], chis);
		b.fill(-1, 25, 3, 1, 25, 4, chis);
		b.set(0, 26, 4, gold);
		b.set(0, 27, 4, st(Blocks.EMERALD_BLOCK));
		b.chest(0, 25, 2, Direction.NORTH, "minecraft:chests/jungle_temple");
		b.set(-3, 25, 0, st(Blocks.LANTERN));
		b.set(3, 25, 0, st(Blocks.LANTERN));

		// ground level: entrance (south), ring corridor and the central hall
		b.fill(-3, 0, 21, 3, 6, 23, chis);
		b.air(-1, 1, 15, 1, 4, 23);
		for (int dx = -17; dx <= 17; dx++) for (int dz = -17; dz <= 17; dz++) {
			int m = Math.max(Math.abs(dx), Math.abs(dz));
			if (m >= 15) b.air(dx, 1, dz, dx, 4, dz);
		}
		b.air(-9, 1, -9, 9, 8, 9);
		for (int[] d : new int[][]{{0, 1}, {0, -1}, {1, 0}, {-1, 0}}) {
			if (d[0] == 0) b.air(-1, 1, Math.min(10 * d[1], 14 * d[1]), 1, 4, Math.max(10 * d[1], 14 * d[1]));
			else b.air(Math.min(10 * d[0], 14 * d[0]), 1, -1, Math.max(10 * d[0], 14 * d[0]), 4, 1);
		}
		// arrow traps in the ring
		b.tripwireTrap(14, 1, 6, 18, 6);
		b.tripwireTrap(-18, 1, -6, -14, -6);
		b.tripwireTrap(6, 1, -18, 6, -14);
		b.tripwireTrap(-6, 1, 14, -6, 18);
		// hall: pillars, idols and a pit down into the spider den
		for (int px : new int[]{-6, 6}) for (int pz : new int[]{-6, 6}) b.fill(px, 1, pz, px, 8, pz, chis);
		for (int dx = -9; dx <= 9; dx++) for (int dz = -9; dz <= 9; dz++) if (noise(dx, 0, dz) > 0.6) b.set(dx, 0, dz, st(Blocks.MOSSY_STONE_BRICKS));
		b.chest(-8, 1, 0, Direction.EAST, "minecraft:chests/jungle_temple");
		b.chest(8, 1, 0, Direction.WEST, "minecraft:chests/jungle_temple");
		b.set(-8, 1, -8, st(Blocks.GLOWSTONE));
		b.set(8, 1, 8, st(Blocks.GLOWSTONE));
		b.set(-8, 1, 8, st(Blocks.GLOWSTONE));
		b.set(8, 1, -8, st(Blocks.GLOWSTONE));
		b.air(-1, -9, -1, 1, 0, 1);
		for (int dx = -1; dx <= 1; dx++) b.vine(dx, 0, -1, Direction.NORTH, 9);

		// spider den below
		b.air(-8, -10, -8, 8, -4, 8);
		for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) b.set(dx, -11, dz, st(Blocks.WATER));
		b.spawner(-6, -10, -6, EntityTypes.CAVE_SPIDER);
		b.spawner(6, -10, 6, EntityTypes.CAVE_SPIDER);
		for (int k = 0; k < 30; k++) {
			int x = r.nextInt(17) - 8, z = r.nextInt(17) - 8, y = -10 + r.nextInt(6);
			if (Math.abs(x) > 1 || Math.abs(z) > 1) b.set(x, y, z, st(Blocks.COBWEB));
		}
		b.chest(0, -10, -7, Direction.SOUTH, "minecraft:chests/jungle_temple");
		b.set(-3, -10, 3, st(Blocks.GLOWSTONE));
		// corridor east to the hidden vault, behind a cracked "secret" wall and more traps
		b.air(9, -10, -1, 11, -7, 1);
		b.fill(12, -10, -1, 12, -7, 1, st(Blocks.CRACKED_STONE_BRICKS));
		b.set(12, -6, 0, chis);
		b.tntPlate(10, -10, 0);
		b.set(10, -12, 1, st(Blocks.TNT));
		b.set(10, -12, -1, st(Blocks.TNT));
		b.tripwireTrap(9, -10, -2, 9, 2);
		// the vault
		b.air(13, -10, -5, 20, -5, 5);
		for (int x = 13; x <= 20; x++) for (int z = -5; z <= 5; z++) b.set(x, -11, z, (x + z) % 2 == 0 ? gold : chis);
		b.fill(17, -10, -1, 18, -10, 1, chis);
		b.set(17, -9, 0, gold);
		b.set(18, -9, 0, emerald);
		b.set(17, -8, 0, st(Blocks.EMERALD_BLOCK));
		b.chest(19, -10, -4, Direction.NORTH, "glowcube_realms:chests/jungle_treasure");
		b.chest(19, -10, 4, Direction.SOUTH, "glowcube_realms:chests/jungle_treasure");
		b.chest(14, -10, -4, Direction.NORTH, "minecraft:chests/jungle_temple");
		for (int[] g : new int[][]{{20, -5}, {20, 5}, {13, 5}, {13, -5}}) b.set(g[0], -10, g[1], r.nextBoolean() ? emerald : gold);
		b.set(15, -10, 3, st(Blocks.GLOWSTONE));
		b.set(15, -10, -3, st(Blocks.GLOWSTONE));
		// way back up: ladder shaft into the ring corridor
		b.air(16, -10, 6, 16, 0, 6);
		b.ladder(16, -10, 0, 6, Direction.NORTH);
		b.set(16, -4, 7, bricks);

		// overgrowth: vines down the tier walls and bushes on the terraces
		for (int t = 0; t < 4; t++) {
			int half = 22 - 4 * t, top = 6 * t + 5;
			for (int d = -half; d <= half; d++) {
				if (noise(d, t, 1) > 0.55) b.vine(d, top, -half - 1, Direction.SOUTH, 2 + r.nextInt(5));
				if (noise(d, t, 2) > 0.55) b.vine(d, top, half + 1, Direction.NORTH, 2 + r.nextInt(5));
				if (noise(d, t, 3) > 0.55) b.vine(-half - 1, top, d, Direction.EAST, 2 + r.nextInt(5));
				if (noise(d, t, 4) > 0.55) b.vine(half + 1, top, d, Direction.WEST, 2 + r.nextInt(5));
			}
			if (t < 3) for (int k = 0; k < 10; k++) {
				int a = half - 1 - r.nextInt(2), c = r.nextInt(2 * half - 1) - half + 1;
				int[][] spots = {{c, -a}, {c, a}, {-a, c}, {a, c}};
				int[] s = spots[r.nextInt(4)];
				if (Math.abs(s[0]) <= 2 && s[1] < 0) continue;
				b.set(s[0], top + 1, s[1], Blocks.JUNGLE_LEAVES.defaultBlockState().setValue(BlockStateProperties.PERSISTENT, true));
			}
		}
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
