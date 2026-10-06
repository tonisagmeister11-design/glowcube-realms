package net.glowcube.realms.event;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.glowcube.realms.GlowcubeRealms;

/**
 * Development helper: when started with -Dglowcube.selftest=<file>, runs every line of the file as a
 * server command after startup (one per tick) and stops the server afterwards. Inactive in normal play.
 */
public final class DevSelfTest {
	private static List<String> commands;
	private static int index;
	private static int waitTicks;

	public static void init() {
		String file = System.getProperty("glowcube.selftest");
		if (file == null) return;
		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			try {
				commands = Files.readAllLines(Path.of(file));
				GlowcubeRealms.LOGGER.info("[SelfTest] {} commands loaded", commands.size());
			} catch (Exception e) {
				GlowcubeRealms.LOGGER.error("[SelfTest] cannot read " + file, e);
			}
		});
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			if (commands == null) return;
			if (waitTicks > 0) {
				waitTicks--;
				return;
			}
			if (index >= commands.size()) {
				GlowcubeRealms.LOGGER.info("[SelfTest] finished");
				commands = null;
				server.halt(false);
				return;
			}
			String cmd = commands.get(index++).trim();
			if (cmd.isEmpty()) return;
			if (cmd.startsWith("wait ")) {
				waitTicks = Integer.parseInt(cmd.substring(5).trim());
				return;
			}
			GlowcubeRealms.LOGGER.info("[SelfTest] > {}", cmd);
			if (cmd.startsWith("safespot ")) {
				String[] a = cmd.split(" ");
				net.minecraft.server.level.ServerLevel lvl = server.getLevel(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, net.minecraft.resources.Identifier.parse(a[1])));
				net.minecraft.core.BlockPos p = net.glowcube.realms.world.RealmTeleporter.safeSpot(lvl, new net.minecraft.core.BlockPos(Integer.parseInt(a[2]), 64, Integer.parseInt(a[3])));
				GlowcubeRealms.LOGGER.info("[SelfTest] safespot -> {} below={} at={} above={} light={}", p, lvl.getBlockState(p.below()), lvl.getBlockState(p), lvl.getBlockState(p.above()), lvl.getMaxLocalRawBrightness(p));
				return;
			}
			if (cmd.equals("target")) {
				for (net.minecraft.server.level.ServerLevel lvl : server.getAllLevels()) {
					for (net.minecraft.world.entity.Entity e : lvl.getAllEntities()) {
						if (!(e instanceof net.glowcube.realms.entity.boss.RealmBoss boss)) continue;
						net.minecraft.world.entity.LivingEntity best = null;
						for (net.minecraft.world.entity.animal.golem.IronGolem g : lvl.getEntitiesOfClass(net.minecraft.world.entity.animal.golem.IronGolem.class, boss.getBoundingBox().inflate(40))) best = g;
						boss.setTarget(best);
						GlowcubeRealms.LOGGER.info("[SelfTest] target {} -> {}", boss.getType().toShortString(), best);
					}
				}
				return;
			}
			if (cmd.startsWith("light ")) {
				String[] a = cmd.split(" ");
				net.minecraft.server.level.ServerLevel lvl = server.getLevel(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, net.minecraft.resources.Identifier.parse(a[1])));
				net.minecraft.core.BlockPos bp = new net.minecraft.core.BlockPos(Integer.parseInt(a[2]), Integer.parseInt(a[3]), Integer.parseInt(a[4]));
				boolean umbral = a[5].equals("umbral");
				boolean ok = net.glowcube.realms.world.RealmPortalShape.tryLight(lvl, bp, umbral ? net.minecraft.world.level.block.Blocks.CRYING_OBSIDIAN : net.minecraft.world.level.block.Blocks.GLOWSTONE,
						umbral ? net.glowcube.realms.registry.ModBlocks.UMBRAL_PORTAL : net.glowcube.realms.registry.ModBlocks.LUMEN_PORTAL);
				GlowcubeRealms.LOGGER.info("[SelfTest] light {} -> {}", bp, ok);
				return;
			}
			if (cmd.startsWith("render ") || cmd.startsWith("slice ")) {
				try {
					render(server, cmd);
				} catch (Exception e) {
					GlowcubeRealms.LOGGER.error("[SelfTest] render failed", e);
				}
				return;
			}
			try {
				server.getCommands().performPrefixedCommand(server.createCommandSourceStack(), cmd);
			} catch (Exception e) {
				GlowcubeRealms.LOGGER.error("[SelfTest] command failed: " + cmd, e);
			}
		});
	}

	/** "render <dim> <x> <z> <radius> <file>" top-down map, "slice <dim> <x> <z> <radius> <file>" vertical cut along X. */
	private static void render(net.minecraft.server.MinecraftServer server, String cmd) throws Exception {
		String[] p = cmd.split(" ");
		net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> key = net.minecraft.resources.ResourceKey.create(
				net.minecraft.core.registries.Registries.DIMENSION, net.minecraft.resources.Identifier.parse(p[1]));
		net.minecraft.server.level.ServerLevel level = server.getLevel(key);
		int cx = Integer.parseInt(p[2]), cz = Integer.parseInt(p[3]), r = Integer.parseInt(p[4]);
		boolean slice = p[0].equals("slice");
		int h = level.getHeight();
		java.awt.image.BufferedImage img = new java.awt.image.BufferedImage(r * 2, slice ? h : r * 2, java.awt.image.BufferedImage.TYPE_INT_RGB);
		net.minecraft.core.BlockPos.MutableBlockPos pos = new net.minecraft.core.BlockPos.MutableBlockPos();
		for (int a = 0; a < r * 2; a++) {
			for (int b = 0; b < (slice ? h : r * 2); b++) {
				int x = cx - r + a;
				int color;
				if (slice) {
					int y = level.getMaxY() - b;
					pos.set(x, y, cz);
					net.minecraft.world.level.block.state.BlockState s = level.getBlockState(pos);
					color = s.isAir() ? 0x101020 : s.getMapColor(level, pos).col;
					if (!s.getFluidState().isEmpty()) color = 0x3050ff;
				} else {
					int z = cz - r + b;
					boolean ceiling = level.dimensionType().hasCeiling();
					int y = -1;
					if (ceiling) {
						for (int yy = 70; yy > level.getMinY(); yy--) {
							pos.set(x, yy, z);
							if (!level.getBlockState(pos).isAir() && level.getBlockState(pos.above()).isAir()) {
								y = yy;
								break;
							}
						}
					} else {
						y = level.getChunk(x >> 4, z >> 4).getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE, x & 15, z & 15);
					}
					pos.set(x, y, z);
					net.minecraft.world.level.block.state.BlockState s = level.getBlockState(pos);
					color = y < level.getMinY() + 1 ? 0x101020 : s.getMapColor(level, pos).col;
					float shade = 0.55F + Math.min(1.0F, Math.max(0.0F, (y - level.getMinY()) / (float) h)) * 0.6F;
					color = ((int) (((color >> 16) & 255) * shade) << 16) | ((int) (((color >> 8) & 255) * shade) << 8) | (int) ((color & 255) * shade);
				}
				img.setRGB(a, b, color);
			}
		}
		javax.imageio.ImageIO.write(img, "png", new java.io.File(p[5]));
		GlowcubeRealms.LOGGER.info("[SelfTest] wrote {}", p[5]);
	}

	private DevSelfTest() {
	}
}
