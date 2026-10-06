package net.glowcube.realms.client.map;

import net.glowcube.realms.GlowcubeRealms;
import net.glowcube.realms.network.ArenaMarkersPayload;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;

/** Shared drawing helpers for minimap and world map. */
public final class MapRendering {
	public static final Identifier PLAYER_ICON = GlowcubeRealms.id("textures/gui/map/player.png");

	public static Identifier bossIcon(ArenaMarkersPayload.Marker m) {
		if (m.defeated()) return GlowcubeRealms.id("textures/gui/map/boss_defeated.png");
		return GlowcubeRealms.id("textures/gui/map/boss_" + m.boss() + ".png");
	}

	public static void icon(GuiGraphicsExtractor g, Identifier tex, int cx, int cy, int size) {
		g.blit(RenderPipelines.GUI_TEXTURED, tex, cx - size / 2, cy - size / 2, 0.0F, 0.0F, size, size, size, size);
	}

	public static void playerArrow(GuiGraphicsExtractor g, float cx, float cy, float yaw, int size) {
		g.pose().pushMatrix();
		g.pose().translate(cx, cy);
		g.pose().rotate((float) Math.toRadians(yaw + 180.0F));
		g.blit(RenderPipelines.GUI_TEXTURED, PLAYER_ICON, -size / 2, -size / 2, 0.0F, 0.0F, size, size, size, size);
		g.pose().popMatrix();
	}

	public static void diamond(GuiGraphicsExtractor g, int cx, int cy, int r, int color) {
		for (int dy = -r; dy <= r; dy++) {
			int w = r - Math.abs(dy);
			g.fill(cx - w, cy + dy, cx + w + 1, cy + dy + 1, color);
		}
	}

	/** Ornate double border with glowing corners. */
	public static void frame(GuiGraphicsExtractor g, int x, int y, int w, int h) {
		g.fill(x - 3, y - 3, x + w + 3, y + h + 3, 0xFF0B0414);
		g.fillGradient(x - 2, y - 2, x + w + 2, y + h + 2, 0xFF5CDFFF, 0xFFB36BFF);
		g.fill(x - 1, y - 1, x + w + 1, y + h + 1, 0xFF0B0414);
		for (int[] c : new int[][]{{x - 2, y - 2}, {x + w + 1, y - 2}, {x - 2, y + h + 1}, {x + w + 1, y + h + 1}}) {
			diamond(g, c[0], c[1], 3, 0xFFFFE58A);
			g.fill(c[0], c[1], c[0] + 1, c[1] + 1, 0xFFFFFFFF);
		}
	}

	private MapRendering() {
	}
}
