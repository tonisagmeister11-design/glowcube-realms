package net.glowcube.realms.client;

import java.io.File;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Deque;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.glowcube.realms.GlowcubeRealms;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

/**
 * Development helper: with -Dglowcube.shots=1 the client (started with --quickPlaySingleplayer) shows every 3D weapon
 * in first and third person and the armor sets, saves a screenshot of each into run/screenshots/glowcube and quits.
 */
public final class DevShots {
	private static final String[] WEAPONS = {"glowcrystal_sword", "voidshard_sword", "radiant_blade", "ember_greatsword", "frostbite_blade", "sonic_blade",
			"shadow_dagger", "void_scythe", "void_reaver", "star_hammer", "infernal_maul", "thunder_spear", "sky_pike", "aurora_staff", "bone_scepter",
			"meteor_staff", "crystal_bow", "storm_bow", "glowcrystal_axe", "lumber_axe", "glowcrystal_pickaxe", "voidshard_pickaxe", "excavator_pickaxe",
			"glowcrystal_shovel"};
	private static final String[] ARMOR = {"glowcrystal", "voidshard", "starmetal"};

	private static final Deque<Runnable> steps = new ArrayDeque<>();
	private static int wait = 0;
	private static int ticksInWorld = 0;
	private static boolean planned;

	public static void init() {
		if (System.getProperty("glowcube.shots") == null) return;
		ClientTickEvents.END_CLIENT_TICK.register(DevShots::tick);
	}

	private static int idle = 0;

	private static void tick(Minecraft mc) {
		if (mc.player == null || mc.getSingleplayerServer() == null) {
			// quick play did not start the world (e.g. a first-start screen was in the way): open it ourselves
			if (++idle == 200) mc.createWorldOpenFlows().openWorld("shots", () -> {});
			return;
		}
		if (++ticksInWorld < 120) return;
		if (!planned) {
			planned = true;
			plan(mc);
		}
		if (wait > 0) {
			wait--;
			return;
		}
		Runnable step = steps.poll();
		if (step != null) step.run();
	}

	private static void server(Minecraft mc, java.util.function.Consumer<MinecraftServer> action) {
		MinecraftServer server = mc.getSingleplayerServer();
		server.execute(() -> action.accept(server));
	}

	private static void cmd(MinecraftServer server, String command) {
		server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), command);
	}

	private static ServerPlayer player(MinecraftServer server) {
		return server.getPlayerList().getPlayers().get(0);
	}

	private static void plan(Minecraft mc) {
		steps.add(() -> server(mc, s -> {
			cmd(s, "time set noon");
			cmd(s, "weather clear");
			cmd(s, "gamerule advance_time false");
			cmd(s, "gamerule advance_weather false");
			cmd(s, "fill -6 199 -6 6 199 6 minecraft:smooth_quartz");
			cmd(s, "fill -6 200 4 6 205 4 minecraft:light_blue_concrete");
			cmd(s, "tp @a 0 200 0 0 0");
			cmd(s, "kill @e[type=!player]");
		}));
		steps.add(() -> wait = 60);
		String only = System.getProperty("glowcube.shots.items");
		String[] weapons = only != null ? only.split(",") : WEAPONS;
		for (String w : weapons) {
			steps.add(() -> server(mc, s -> {
				ServerPlayer p = player(s);
				p.setItemInHand(InteractionHand.MAIN_HAND, stack(w));
				for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) p.setItemSlot(slot, ItemStack.EMPTY);
				cmd(s, "tp @a 0 200 0 180 10");
			}));
			String n = w.replace(":", "_");
			shot(mc, CameraType.FIRST_PERSON, n + "_1_ich_sicht");
			shot(mc, CameraType.THIRD_PERSON_FRONT, n + "_2_von_vorne");
		}
		for (String a : only != null ? new String[0] : ARMOR) {
			steps.add(() -> server(mc, s -> {
				ServerPlayer p = player(s);
				p.setItemSlot(EquipmentSlot.HEAD, stack(a + "_helmet"));
				p.setItemSlot(EquipmentSlot.CHEST, stack(a + "_chestplate"));
				p.setItemSlot(EquipmentSlot.LEGS, stack(a + "_leggings"));
				p.setItemSlot(EquipmentSlot.FEET, stack(a + "_boots"));
				p.setItemInHand(InteractionHand.MAIN_HAND, stack(a.equals("voidshard") ? "voidshard_sword" : a.equals("starmetal") ? "star_hammer" : "glowcrystal_sword"));
				cmd(s, "tp @a 0 200 0 180 0");
			}));
			shot(mc, CameraType.THIRD_PERSON_FRONT, "ruestung_" + a + "_vorne");
			shot(mc, CameraType.THIRD_PERSON_BACK, "ruestung_" + a + "_hinten");
		}
		steps.add(() -> mc.stop());
	}

	private static ItemStack stack(String id) {
		return new ItemStack(BuiltInRegistries.ITEM.getValue(id.contains(":") ? Identifier.parse(id) : GlowcubeRealms.id(id)));
	}

	private static void shot(Minecraft mc, CameraType camera, String name) {
		steps.add(() -> {
			mc.options.setCameraType(camera);
			wait = 25;
		});
		steps.add(() -> {
			mc.options.chatVisibility().set(net.minecraft.world.entity.player.ChatVisiblity.HIDDEN);
			Screenshot.grab(mc, false);
			wait = 15;
		});
		steps.add(() -> rename(mc, name));
	}

	/** Moves the newest screenshot to screenshots/glowcube/<name>.png. */
	private static void rename(Minecraft mc, String name) {
		File dir = new File(mc.gameDirectory, Screenshot.SCREENSHOT_DIR);
		File[] files = dir.listFiles((d, n) -> n.endsWith(".png"));
		if (files == null || files.length == 0) return;
		File newest = Arrays.stream(files).max(Comparator.comparingLong(File::lastModified)).get();
		File out = new File(dir, "glowcube");
		out.mkdirs();
		boolean ok = newest.renameTo(new File(out, name + ".png"));
		GlowcubeRealms.LOGGER.info("[Shots] {} -> {} ({})", newest.getName(), name, ok);
	}

	private DevShots() {
	}
}
