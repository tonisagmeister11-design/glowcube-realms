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
			if (cmd.startsWith("useitems ")) {
				// uses every mod item once with a fake player (right-click and left-click abilities) and logs failures
				String[] a = cmd.split(" ");
				net.minecraft.server.level.ServerLevel lvl = server.getLevel(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, net.minecraft.resources.Identifier.parse(a[1])));
				net.fabricmc.fabric.api.entity.FakePlayer fake = net.fabricmc.fabric.api.entity.FakePlayer.get(lvl);
				fake.snapTo(Double.parseDouble(a[2]), Double.parseDouble(a[3]), Double.parseDouble(a[4]), 0, 20);
				int ok = 0, failed = 0;
				for (net.minecraft.world.item.Item item : net.glowcube.realms.registry.ModItems.ALL) {
					for (boolean sneak : new boolean[]{false, true}) {
						try {
							net.minecraft.world.item.ItemStack stack = new net.minecraft.world.item.ItemStack(item);
							fake.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, stack);
							fake.setShiftKeyDown(sneak);
							fake.getCooldowns().removeCooldown(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item));
							item.use(lvl, fake, net.minecraft.world.InteractionHand.MAIN_HAND);
							if (item instanceof net.glowcube.realms.item.LeftClickAbility lc) lc.onLeftClick(fake, stack);
							ok++;
						} catch (Throwable t) {
							failed++;
							GlowcubeRealms.LOGGER.error("[SelfTest] item " + item + " sneak=" + sneak + " failed", t);
						}
					}
				}
				fake.setShiftKeyDown(false);
				GlowcubeRealms.LOGGER.info("[SelfTest] useitems done: {} ok, {} failed", ok, failed);
				return;
			}
			if (cmd.startsWith("useon ")) {
				// "useon <dim> <x> <y> <z> <item|empty>": a fake player right-clicks the block (fires UseBlockCallback like a real click)
				String[] a = cmd.split(" ");
				net.minecraft.server.level.ServerLevel lvl = server.getLevel(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, net.minecraft.resources.Identifier.parse(a[1])));
				net.minecraft.core.BlockPos bp = new net.minecraft.core.BlockPos(Integer.parseInt(a[2]), Integer.parseInt(a[3]), Integer.parseInt(a[4]));
				net.fabricmc.fabric.api.entity.FakePlayer fake = net.fabricmc.fabric.api.entity.FakePlayer.get(lvl);
				fake.snapTo(bp.getX() + 0.5, bp.getY() + 1, bp.getZ() + 2.5, 180, 30);
				net.minecraft.world.item.ItemStack stack = a[5].equals("empty") ? net.minecraft.world.item.ItemStack.EMPTY
						: new net.minecraft.world.item.ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.parse(a[5])));
				fake.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, stack);
				try {
					net.minecraft.world.InteractionResult r = fake.gameMode.useItemOn(fake, lvl, stack, net.minecraft.world.InteractionHand.MAIN_HAND,
							new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(bp), net.minecraft.core.Direction.SOUTH, bp, false));
					GlowcubeRealms.LOGGER.info("[SelfTest] useon {} {} -> {} now={} count={}", bp, a[5], r, lvl.getBlockState(bp), stack.getCount());
				} catch (Throwable t) {
					GlowcubeRealms.LOGGER.error("[SelfTest] useon failed", t);
				}
				return;
			}
			if (cmd.startsWith("village ")) {
				// "village <x> <z> <checks>": runs the village population/guard check with a fake player standing there
				String[] a = cmd.split(" ");
				net.minecraft.server.level.ServerLevel lvl = server.overworld();
				int x = Integer.parseInt(a[1]), z = Integer.parseInt(a[2]);
				net.fabricmc.fabric.api.entity.FakePlayer fake = net.fabricmc.fabric.api.entity.FakePlayer.get(lvl);
				fake.snapTo(x + 0.5, lvl.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z + 0.5, 0, 0);
				for (int i = 0; i < Integer.parseInt(a[3]); i++) RealmEvents.guardVillages(fake);
				net.minecraft.world.phys.AABB box = new net.minecraft.world.phys.AABB(fake.blockPosition()).inflate(64, 30, 64);
				GlowcubeRealms.LOGGER.info("[SelfTest] village {} {} -> villagers={} golems={} guards={}", x, z,
						lvl.getEntitiesOfClass(net.minecraft.world.entity.npc.villager.Villager.class, box).size(),
						lvl.getEntitiesOfClass(net.minecraft.world.entity.animal.golem.IronGolem.class, box).size(),
						lvl.getEntitiesOfClass(net.glowcube.realms.entity.RealmGuardian.class, box).size());
				return;
			}
			if (cmd.startsWith("spawncheck ")) {
				// "spawncheck <dim> <x> <z> <entity>...": counts standing spots around x/z where each mob passes its natural spawn rules
				String[] a = cmd.split(" ");
				net.minecraft.server.level.ServerLevel lvl = server.getLevel(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, net.minecraft.resources.Identifier.parse(a[1])));
				int x0 = Integer.parseInt(a[2]), z0 = Integer.parseInt(a[3]);
				for (int k = 4; k < a.length; k++) {
					net.minecraft.world.entity.EntityType<?> type = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getValue(net.minecraft.resources.Identifier.parse(a[k]));
					int spots = 0, ok = 0;
					net.minecraft.util.RandomSource rnd = net.minecraft.util.RandomSource.create(k);
					for (int i = 0; i < 400; i++) {
						int x = x0 + rnd.nextInt(97) - 48, z = z0 + rnd.nextInt(97) - 48;
						for (int y = lvl.getMaxY() - 1; y > lvl.getMinY(); y--) {
							net.minecraft.core.BlockPos p = new net.minecraft.core.BlockPos(x, y, z);
							if (!lvl.getBlockState(p).isAir() || !lvl.getBlockState(p.below()).isSolidRender()) continue;
							spots++;
							if (net.minecraft.world.entity.SpawnPlacements.isSpawnPositionOk(type, lvl, p)
									&& net.minecraft.world.entity.SpawnPlacements.checkSpawnRules(type, lvl, net.minecraft.world.entity.EntitySpawnReason.NATURAL, p, rnd)) ok++;
						}
					}
					GlowcubeRealms.LOGGER.info("[SelfTest] spawncheck {} {} -> {} of {} spots valid", a[1], a[k], ok, spots);
				}
				return;
			}
			if (cmd.equals("reach")) {
				// distance from the target's eyes to the boss hitbox (players hit up to 3 blocks)
				for (net.minecraft.server.level.ServerLevel lvl : server.getAllLevels())
					for (net.minecraft.world.entity.Entity e : lvl.getAllEntities())
						if (e instanceof net.glowcube.realms.entity.boss.RealmBoss boss && boss.getTarget() != null) {
							net.minecraft.world.phys.Vec3 eye = boss.getTarget().position().add(0, 1.62, 0); // as if a player stood there
							net.minecraft.world.phys.AABB bb = boss.getBoundingBox();
							double cx = Math.max(bb.minX, Math.min(eye.x, bb.maxX)), cy = Math.max(bb.minY, Math.min(eye.y, bb.maxY)), cz = Math.max(bb.minZ, Math.min(eye.z, bb.maxZ));
							double d = eye.distanceTo(new net.minecraft.world.phys.Vec3(cx, cy, cz));
							GlowcubeRealms.LOGGER.info("[SelfTest] reach {} -> {} blocks ({}), boss feet {} above target feet", boss.getType().toShortString(),
									String.format("%.2f", d), d <= 3.0 ? "HITTABLE" : "too far", String.format("%.2f", boss.getY() - boss.getTarget().getY()));
						}
				return;
			}
			if (cmd.equals("trades")) {
				// logs the offers of every realm trader
				for (net.minecraft.server.level.ServerLevel lvl : server.getAllLevels())
					for (net.minecraft.world.entity.Entity e : lvl.getAllEntities())
						if (e instanceof net.glowcube.realms.entity.RealmTrader t) {
							StringBuilder sb = new StringBuilder();
							for (var o : t.getOffers()) sb.append(o.getCostA().getCount()).append("x").append(o.getCostA().getItem()).append("->").append(o.getResult().getItem()).append(" ");
							GlowcubeRealms.LOGGER.info("[SelfTest] trader in {} variant {} at {}: {}", lvl.dimension().identifier(), t.getVariant(), t.blockPosition(), sb);
						}
				return;
			}
			if (cmd.startsWith("count ")) {
				// "count <dim>": logs mod entities per type in that dimension
				String[] a = cmd.split(" ");
				net.minecraft.server.level.ServerLevel lvl = server.getLevel(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, net.minecraft.resources.Identifier.parse(a[1])));
				java.util.Map<String, Integer> counts = new java.util.TreeMap<>();
				for (net.minecraft.world.entity.Entity e : lvl.getAllEntities()) counts.merge(e instanceof net.minecraft.world.entity.item.ItemEntity it ? "item:" + it.getItem().getItem() : e.getType().toShortString(), 1, Integer::sum);
				GlowcubeRealms.LOGGER.info("[SelfTest] count {} -> {}", a[1], counts);
				return;
			}
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
