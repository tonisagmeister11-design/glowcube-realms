package net.glowcube.realms.world;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.glowcube.realms.GlowcubeRealms;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

/**
 * Per-world storage for realm portals and boss arenas, kept as a small JSON file in the world folder.
 * All methods are synchronized because world generation threads register arenas too.
 */
public final class RealmData {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static RealmData instance;

	public record Arena(String dimension, BlockPos pos, String boss, boolean defeated) {
	}

	private final Path file;
	private final Map<String, List<BlockPos>> portals = new HashMap<>();
	private final List<Arena> arenas = new ArrayList<>();
	private boolean dirty;

	private RealmData(Path file) {
		this.file = file;
	}

	public static synchronized RealmData get() {
		return instance;
	}

	public static synchronized void load(MinecraftServer server) {
		Path path = server.getWorldPath(LevelResource.ROOT).resolve("glowcube_realms.json");
		RealmData data = new RealmData(path);
		if (Files.exists(path)) {
			try {
				JsonObject root = JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8)).getAsJsonObject();
				JsonObject portalObj = root.getAsJsonObject("portals");
				if (portalObj != null) {
					for (Map.Entry<String, JsonElement> e : portalObj.entrySet()) {
						List<BlockPos> list = new ArrayList<>();
						for (JsonElement p : e.getValue().getAsJsonArray()) list.add(readPos(p.getAsJsonArray()));
						data.portals.put(e.getKey(), list);
					}
				}
				JsonArray arenaArr = root.getAsJsonArray("arenas");
				if (arenaArr != null) {
					for (JsonElement el : arenaArr) {
						JsonObject o = el.getAsJsonObject();
						data.arenas.add(new Arena(o.get("dim").getAsString(), readPos(o.getAsJsonArray("pos")), o.get("boss").getAsString(),
								o.get("defeated").getAsBoolean()));
					}
				}
			} catch (Exception ex) {
				GlowcubeRealms.LOGGER.error("Could not read realm data", ex);
			}
		}
		instance = data;
	}

	public static synchronized void unload() {
		if (instance != null) instance.save(true);
		instance = null;
	}

	public synchronized void save(boolean force) {
		if (!force && !this.dirty) return;
		JsonObject root = new JsonObject();
		JsonObject portalObj = new JsonObject();
		for (Map.Entry<String, List<BlockPos>> e : this.portals.entrySet()) {
			JsonArray arr = new JsonArray();
			for (BlockPos p : e.getValue()) arr.add(writePos(p));
			portalObj.add(e.getKey(), arr);
		}
		root.add("portals", portalObj);
		JsonArray arenaArr = new JsonArray();
		for (Arena a : this.arenas) {
			JsonObject o = new JsonObject();
			o.addProperty("dim", a.dimension());
			o.add("pos", writePos(a.pos()));
			o.addProperty("boss", a.boss());
			o.addProperty("defeated", a.defeated());
			arenaArr.add(o);
		}
		root.add("arenas", arenaArr);
		try {
			Files.writeString(this.file, GSON.toJson(root), StandardCharsets.UTF_8);
			this.dirty = false;
		} catch (IOException ex) {
			GlowcubeRealms.LOGGER.error("Could not save realm data", ex);
		}
	}

	// ---------------------------------------------------------------- portals
	public synchronized void addPortal(String dimension, BlockPos pos) {
		List<BlockPos> list = this.portals.computeIfAbsent(dimension, k -> new ArrayList<>());
		for (BlockPos p : list) if (p.distManhattan(pos) < 6) return;
		list.add(pos.immutable());
		this.dirty = true;
	}

	public synchronized void removePortal(String dimension, BlockPos pos) {
		List<BlockPos> list = this.portals.get(dimension);
		if (list != null && list.remove(pos)) this.dirty = true;
	}

	public synchronized BlockPos findPortal(String dimension, BlockPos near, int maxDistance) {
		List<BlockPos> list = this.portals.get(dimension);
		if (list == null) return null;
		BlockPos best = null;
		double bestDist = (double) maxDistance * maxDistance;
		for (BlockPos p : list) {
			double dx = p.getX() - near.getX(), dz = p.getZ() - near.getZ();
			double d = dx * dx + dz * dz;
			if (d < bestDist) {
				bestDist = d;
				best = p;
			}
		}
		return best;
	}

	// ---------------------------------------------------------------- arenas
	public synchronized void addArena(String dimension, BlockPos pos, String boss) {
		for (Arena a : this.arenas) {
			if (!a.dimension().equals(dimension) || !a.boss().equals(boss)) continue;
			double dx = a.pos().getX() - pos.getX(), dz = a.pos().getZ() - pos.getZ();
			if (dx * dx + dz * dz < 48 * 48) return;
		}
		this.arenas.add(new Arena(dimension, pos.immutable(), boss, false));
		this.dirty = true;
	}

	public synchronized void markDefeated(String dimension, BlockPos near) {
		for (int i = 0; i < this.arenas.size(); i++) {
			Arena a = this.arenas.get(i);
			if (a.dimension().equals(dimension) && a.pos().distSqr(near) < 96 * 96) {
				this.arenas.set(i, new Arena(a.dimension(), a.pos(), a.boss(), true));
				this.dirty = true;
				return;
			}
		}
	}

	public synchronized void markAwake(String dimension, BlockPos near) {
		for (int i = 0; i < this.arenas.size(); i++) {
			Arena a = this.arenas.get(i);
			if (a.dimension().equals(dimension) && a.pos().distSqr(near) < 8 * 8 && a.defeated()) {
				this.arenas.set(i, new Arena(a.dimension(), a.pos(), a.boss(), false));
				this.dirty = true;
			}
		}
	}

	public synchronized List<Arena> arenasNear(String dimension, BlockPos near, int radius) {
		List<Arena> out = new ArrayList<>();
		for (Arena a : this.arenas) {
			if (!a.dimension().equals(dimension)) continue;
			double dx = a.pos().getX() - near.getX(), dz = a.pos().getZ() - near.getZ();
			if (dx * dx + dz * dz < (double) radius * radius) out.add(a);
		}
		return out;
	}

	private static BlockPos readPos(JsonArray a) {
		return new BlockPos(a.get(0).getAsInt(), a.get(1).getAsInt(), a.get(2).getAsInt());
	}

	private static JsonArray writePos(BlockPos p) {
		JsonArray a = new JsonArray();
		a.add(p.getX());
		a.add(p.getY());
		a.add(p.getZ());
		return a;
	}
}
