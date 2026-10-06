package net.glowcube.realms.client.map;

import com.mojang.blaze3d.platform.NativeImage;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.glowcube.realms.GlowcubeRealms;
import net.glowcube.realms.network.ArenaMarkersPayload;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;

/** The minimap in the top-right corner: terrain, player arrow, nearby entities, waypoints and boss arenas. */
public final class MinimapHud implements HudElement {
	public static final Identifier TEXTURE_ID = GlowcubeRealms.id("dynamic/minimap");
	private static final int TEX = 128;
	public static boolean enabled = true;
	public static int zoomIndex = 1;
	private static final float[] ZOOMS = {0.5F, 1.0F, 2.0F};

	private DynamicTexture texture;
	private long lastUpdate = -1;

	public static void cycleZoom() {
		zoomIndex = (zoomIndex + 1) % ZOOMS.length;
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, DeltaTracker delta) {
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer player = mc.player;
		if (!enabled || player == null || mc.level == null || mc.getDebugOverlay().showDebugScreen()) return;
		if (this.texture == null) {
			this.texture = new DynamicTexture(() -> "glowcube minimap", TEX, TEX, true);
			mc.getTextureManager().register(TEXTURE_ID, this.texture);
		}
		float zoom = ZOOMS[zoomIndex];
		long gameTime = mc.level.getGameTime();
		if (gameTime != this.lastUpdate && gameTime % 4 == 0) {
			this.lastUpdate = gameTime;
			this.refresh(mc, player, zoom);
		}

		int size = 104;
		int x = g.guiWidth() - size - 8, y = 8;
		MapRendering.frame(g, x, y, size, size);
		g.blit(RenderPipelines.GUI_TEXTURED, TEXTURE_ID, x, y, 0.0F, 0.0F, size, size, TEX, TEX, TEX, TEX);

		double px = player.getX(), pz = player.getZ();
		float scale = size / (TEX * zoom); // gui pixels per block
		int cx = x + size / 2, cy = y + size / 2;

		// entities radar
		for (Entity e : mc.level.entitiesForRendering()) {
			if (e == player || !(e instanceof LivingEntity)) continue;
			double dx = (e.getX() - px) * scale, dz = (e.getZ() - pz) * scale;
			if (Math.abs(dx) > size / 2.0 - 2 || Math.abs(dz) > size / 2.0 - 2) continue;
			int color = e instanceof Player ? 0xFFFFFFFF : e instanceof Enemy ? 0xFFFF4A5A : 0xFF7CFF6B;
			int ex = cx + (int) dx, ez = cy + (int) dz;
			g.fill(ex - 1, ez - 1, ex + 1, ez + 1, color);
		}
		// waypoints
		for (Waypoints.Waypoint w : Waypoints.current(mc)) {
			int[] p = clampToMap(cx, cy, size, (w.x - px) * scale, (w.z - pz) * scale);
			MapRendering.diamond(g, p[0], p[1], 3, 0xFF000000);
			MapRendering.diamond(g, p[0], p[1], 2, w.color);
		}
		// boss arenas
		for (ArenaMarkersPayload.Marker m : Waypoints.arenas) {
			int[] p = clampToMap(cx, cy, size, (m.x() - px) * scale, (m.z() - pz) * scale);
			MapRendering.icon(g, MapRendering.bossIcon(m), p[0], p[1], p[2] == 1 ? 7 : 10);
		}
		MapRendering.playerArrow(g, cx, cy, player.getYRot(), 8);

		// compass letter and info text
		Font font = mc.font;
		g.centeredText(font, "N", cx, y - 1, 0xFFFFE58A);
		BlockPos pos = player.blockPosition();
		String coords = pos.getX() + ", " + pos.getY() + ", " + pos.getZ();
		g.centeredText(font, coords, cx, y + size + 5, 0xFFFFFFFF);
		Component biome = mc.level.getBiome(pos).unwrapKey()
				.map(k -> (Component) Component.translatable("biome." + k.identifier().getNamespace() + "." + k.identifier().getPath()))
				.orElse(Component.empty());
		g.centeredText(font, biome, cx, y + size + 15, 0xFF9FE8FF);
		ArenaMarkersPayload.Marker nearest = null;
		double best = Double.MAX_VALUE;
		for (ArenaMarkersPayload.Marker m : Waypoints.arenas) {
			if (m.defeated()) continue;
			double d = Math.hypot(m.x() - px, m.z() - pz);
			if (d < best) {
				best = d;
				nearest = m;
			}
		}
		if (nearest != null) {
			g.centeredText(font, Component.translatable("hud.glowcube_realms.nearest_boss",
					Component.translatable("entity.glowcube_realms." + nearest.boss()), (int) best), cx, y + size + 25, 0xFFFFB84D);
		}
	}

	/** Returns screen position, third value 1 if clamped to the edge. */
	private static int[] clampToMap(int cx, int cy, int size, double dx, double dz) {
		double half = size / 2.0 - 4;
		double m = Math.max(Math.abs(dx), Math.abs(dz));
		if (m > half) {
			dx = dx / m * half;
			dz = dz / m * half;
			return new int[]{cx + (int) dx, cy + (int) dz, 1};
		}
		return new int[]{cx + (int) dx, cy + (int) dz, 0};
	}

	private void refresh(Minecraft mc, LocalPlayer player, float zoom) {
		NativeImage img = this.texture.getPixels();
		if (img == null) return;
		String dim = mc.level.dimension().identifier().toString();
		int px = player.getBlockX(), pz = player.getBlockZ();
		for (int ty = 0; ty < TEX; ty++) {
			for (int tx = 0; tx < TEX; tx++) {
				int wx = px + (int) Math.floor((tx - TEX / 2) * zoom), wz = pz + (int) Math.floor((ty - TEX / 2) * zoom);
				int c = MapCache.get(dim, wx, wz);
				img.setPixel(tx, ty, c == 0 ? MapColors.UNKNOWN : c);
			}
		}
		this.texture.upload();
	}
}
