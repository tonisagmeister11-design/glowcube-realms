package net.glowcube.realms.client.map;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.level.ChunkPos;

/** Remembers explored chunk colors per dimension so the world map can show everything you have seen. */
public final class MapCache {
	private static final java.util.Map<String, Long2ObjectOpenHashMap<int[]>> DIMENSIONS = new java.util.HashMap<>();
	private static int scanIndex, innerIndex;

	public static Long2ObjectOpenHashMap<int[]> forDimension(String dim) {
		return DIMENSIONS.computeIfAbsent(dim, d -> new Long2ObjectOpenHashMap<>());
	}

	public static void clear() {
		DIMENSIONS.clear();
	}

	public static void put(String dim, int x, int z, int color) {
		long key = ChunkPos.pack(x >> 4, z >> 4);
		Long2ObjectOpenHashMap<int[]> map = forDimension(dim);
		int[] chunk = map.get(key);
		if (chunk == null) {
			chunk = new int[256];
			map.put(key, chunk);
		}
		chunk[(z & 15) * 16 + (x & 15)] = color;
	}

	public static int get(String dim, int x, int z) {
		int[] chunk = forDimension(dim).get(ChunkPos.pack(x >> 4, z >> 4));
		return chunk == null ? 0 : chunk[(z & 15) * 16 + (x & 15)];
	}

	/** Called every client tick: scans a couple of loaded chunks around the player into the cache. */
	public static void tick(Minecraft mc) {
		ClientLevel level = mc.level;
		LocalPlayer player = mc.player;
		if (level == null || player == null) return;
		String dim = level.dimension().identifier().toString();
		boolean cave = level.dimensionType().hasCeiling();
		int radius = Math.min(mc.options.getEffectiveRenderDistance(), 12);
		int side = radius * 2 + 1;
		int pcx = player.blockPosition().getX() >> 4, pcz = player.blockPosition().getZ() >> 4;
		for (int n = 0; n < 3; n++) {
			int cx, cz;
			if (n < 2) {
				innerIndex = (innerIndex + 1) % 81;
				cx = pcx - 4 + innerIndex % 9;
				cz = pcz - 4 + innerIndex / 9;
			} else {
				scanIndex = (scanIndex + 1) % (side * side);
				cx = pcx - radius + scanIndex % side;
				cz = pcz - radius + scanIndex / side;
			}
			if (!level.hasChunk(cx, cz)) continue;
			int prev[] = new int[16];
			for (int lz = 0; lz < 16; lz++) {
				for (int lx = 0; lx < 16; lx++) {
					int x = (cx << 4) + lx, z = (cz << 4) + lz;
					int c = MapColors.column(level, x, z, cave, player.getBlockY());
					int h = MapColors.lastHeight;
					if (c != 0) put(dim, x, z, lz == 0 ? c : MapColors.shade(c, h, prev[lx]));
					prev[lx] = h;
				}
			}
		}
	}

	private MapCache() {
	}
}
