package net.glowcube.realms.client.map;

import com.mojang.blaze3d.platform.NativeImage;
import net.glowcube.realms.GlowcubeRealms;
import net.glowcube.realms.network.ArenaMarkersPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/** Full screen world map of everything explored in this dimension. Drag to pan, scroll to zoom, right-click a waypoint to delete it. */
public class WorldMapScreen extends Screen {
	private static final Identifier TEXTURE_ID = GlowcubeRealms.id("dynamic/worldmap");
	private static final float[] ZOOMS = {0.25F, 0.5F, 1.0F, 2.0F, 4.0F, 8.0F};
	private DynamicTexture texture;
	private int texW, texH;
	private double centerX, centerZ;
	private int zoomIndex = 2;
	private boolean dirty = true;

	public WorldMapScreen() {
		super(Component.translatable("screen.glowcube_realms.world_map"));
		Minecraft mc = Minecraft.getInstance();
		if (mc.player != null) {
			this.centerX = mc.player.getX();
			this.centerZ = mc.player.getZ();
		}
	}

	private float zoom() {
		return ZOOMS[this.zoomIndex];
	}

	@Override
	protected void init() {
		super.init();
		int w = Math.max(16, this.width - 40), h = Math.max(16, this.height - 60);
		if (this.texture == null || w != this.texW || h != this.texH) {
			if (this.texture != null) this.texture.close();
			this.texW = w;
			this.texH = h;
			this.texture = new DynamicTexture(() -> "glowcube world map", w, h, true);
			Minecraft.getInstance().getTextureManager().register(TEXTURE_ID, this.texture);
			this.dirty = true;
		}
	}

	private void redraw() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null) return;
		NativeImage img = this.texture.getPixels();
		if (img == null) return;
		String dim = mc.level.dimension().identifier().toString();
		float zoom = this.zoom();
		for (int y = 0; y < this.texH; y++) {
			for (int x = 0; x < this.texW; x++) {
				int wx = (int) Math.floor(this.centerX + (x - this.texW / 2.0) * zoom);
				int wz = (int) Math.floor(this.centerZ + (y - this.texH / 2.0) * zoom);
				int c = MapCache.get(dim, wx, wz);
				if (c == 0) {
					boolean grid = ((wx >> 4) + (wz >> 4) & 1) == 0;
					c = grid ? 0xFF120E1A : 0xFF0E0B15;
				}
				img.setPixel(x, y, c);
			}
		}
		this.texture.upload();
		this.dirty = false;
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
		if (this.dirty) this.redraw();
		Minecraft mc = Minecraft.getInstance();
		g.fill(0, 0, this.width, this.height, 0xEE07040C);
		int x0 = 20, y0 = 34;
		MapRendering.frame(g, x0, y0, this.texW, this.texH);
		g.blit(RenderPipelines.GUI_TEXTURED, TEXTURE_ID, x0, y0, 0.0F, 0.0F, this.texW, this.texH, this.texW, this.texH);
		float zoom = this.zoom();
		g.enableScissor(x0, y0, x0 + this.texW, y0 + this.texH);
		for (Waypoints.Waypoint w : Waypoints.current(mc)) {
			int sx = x0 + this.texW / 2 + (int) ((w.x - this.centerX) / zoom), sy = y0 + this.texH / 2 + (int) ((w.z - this.centerZ) / zoom);
			MapRendering.diamond(g, sx, sy, 4, 0xFF000000);
			MapRendering.diamond(g, sx, sy, 3, w.color);
			g.centeredText(this.font, w.name, sx, sy - 13, 0xFFFFFFFF);
		}
		for (ArenaMarkersPayload.Marker m : Waypoints.arenas) {
			int sx = x0 + this.texW / 2 + (int) ((m.x() - this.centerX) / zoom), sy = y0 + this.texH / 2 + (int) ((m.z() - this.centerZ) / zoom);
			MapRendering.icon(g, MapRendering.bossIcon(m), sx, sy, 14);
			Component name = Component.translatable("entity.glowcube_realms." + m.boss());
			g.centeredText(this.font, m.defeated() ? Component.translatable("hud.glowcube_realms.defeated", name) : name, sx, sy + 9,
					m.defeated() ? 0xFF8F8F8F : 0xFFFFB84D);
		}
		if (mc.player != null) {
			float sx = x0 + this.texW / 2.0F + (float) ((mc.player.getX() - this.centerX) / zoom);
			float sy = y0 + this.texH / 2.0F + (float) ((mc.player.getZ() - this.centerZ) / zoom);
			MapRendering.playerArrow(g, sx, sy, mc.player.getYRot(), 10);
		}
		g.disableScissor();

		g.centeredText(this.font, Component.translatable("screen.glowcube_realms.world_map").withStyle(s -> s.withBold(true)), this.width / 2, 8, 0xFF9FE8FF);
		g.centeredText(this.font, Component.translatable("screen.glowcube_realms.world_map.help"), this.width / 2, 20, 0xFFB0A8C8);
		if (mouseX >= x0 && mouseY >= y0 && mouseX < x0 + this.texW && mouseY < y0 + this.texH) {
			int wx = (int) Math.floor(this.centerX + (mouseX - x0 - this.texW / 2.0) * zoom);
			int wz = (int) Math.floor(this.centerZ + (mouseY - y0 - this.texH / 2.0) * zoom);
			g.text(this.font, "X " + wx + "  Z " + wz + "   (1:" + (zoom >= 1 ? (int) zoom : "1/" + (int) (1 / zoom)) + ")", x0 + 4, y0 + this.texH - 12, 0xFFFFFFFF, true);
		}
		super.extractRenderState(g, mouseX, mouseY, a);
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
		this.centerX -= dx * this.zoom();
		this.centerZ -= dy * this.zoom();
		this.dirty = true;
		return true;
	}

	@Override
	public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
		int old = this.zoomIndex;
		if (scrollY > 0 && this.zoomIndex > 0) this.zoomIndex--;
		if (scrollY < 0 && this.zoomIndex < ZOOMS.length - 1) this.zoomIndex++;
		if (old != this.zoomIndex) this.dirty = true;
		return true;
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if (event.button() == 1) {
			Minecraft mc = Minecraft.getInstance();
			int x0 = 20, y0 = 34;
			float zoom = this.zoom();
			for (Waypoints.Waypoint w : Waypoints.current(mc)) {
				int sx = x0 + this.texW / 2 + (int) ((w.x - this.centerX) / zoom), sy = y0 + this.texH / 2 + (int) ((w.z - this.centerZ) / zoom);
				if (Math.abs(event.x() - sx) < 6 && Math.abs(event.y() - sy) < 6) {
					Waypoints.remove(mc, w);
					return true;
				}
			}
		}
		return super.mouseClicked(event, doubleClick);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public void removed() {
		super.removed();
	}
}
