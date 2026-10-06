package net.glowcube.realms.client.map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.loader.api.FabricLoader;
import net.glowcube.realms.network.ArenaMarkersPayload;
import net.minecraft.client.Minecraft;

/** Client-side waypoints (saved per world + dimension) and the boss arena markers received from the server. */
public final class Waypoints {
	public static final class Waypoint {
		public String name;
		public int x, y, z;
		public int color;

		public Waypoint(String name, int x, int y, int z, int color) {
			this.name = name;
			this.x = x;
			this.y = y;
			this.z = z;
			this.color = color;
		}
	}

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("glowcube_realms_waypoints.json");
	private static final int[] COLORS = {0xFF5CF2DD, 0xFFFFD34D, 0xFFFF6B8A, 0xFFB36BFF, 0xFF7CFF6B, 0xFFFF9F43};
	private static Map<String, List<Waypoint>> data = new HashMap<>();
	private static boolean loaded;
	public static volatile List<ArenaMarkersPayload.Marker> arenas = List.of();

	public static String worldKey(Minecraft mc) {
		String world = mc.getSingleplayerServer() != null ? "sp:" + mc.getSingleplayerServer().getWorldData().getLevelName()
				: mc.getCurrentServer() != null ? "mp:" + mc.getCurrentServer().ip : "unknown";
		String dim = mc.level != null ? mc.level.dimension().identifier().toString() : "none";
		return world + "|" + dim;
	}

	public static List<Waypoint> current(Minecraft mc) {
		load();
		return data.computeIfAbsent(worldKey(mc), k -> new ArrayList<>());
	}

	public static Waypoint add(Minecraft mc, int x, int y, int z) {
		List<Waypoint> list = current(mc);
		Waypoint w = new Waypoint("Wegpunkt " + (list.size() + 1), x, y, z, COLORS[list.size() % COLORS.length]);
		list.add(w);
		save();
		return w;
	}

	public static void remove(Minecraft mc, Waypoint w) {
		current(mc).remove(w);
		save();
	}

	private static void load() {
		if (loaded) return;
		loaded = true;
		try {
			if (Files.exists(FILE)) {
				Map<String, List<Waypoint>> read = GSON.fromJson(Files.readString(FILE, StandardCharsets.UTF_8),
						new TypeToken<Map<String, List<Waypoint>>>() {}.getType());
				if (read != null) data = new HashMap<>(read);
			}
		} catch (Exception ignored) {
		}
	}

	private static void save() {
		try {
			Files.writeString(FILE, GSON.toJson(data), StandardCharsets.UTF_8);
		} catch (Exception ignored) {
		}
	}

	private Waypoints() {
	}
}
