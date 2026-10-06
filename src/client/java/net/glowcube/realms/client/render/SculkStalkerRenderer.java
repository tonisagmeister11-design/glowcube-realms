package net.glowcube.realms.client.render;

import net.glowcube.realms.GlowcubeRealms;
import net.glowcube.realms.entity.SculkStalker;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

/** Hunched sculk zombie with glowing sculk patches. */
public class SculkStalkerRenderer extends HumanoidMobRenderer<SculkStalker, HumanoidRenderState, HumanoidModel<HumanoidRenderState>> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(GlowcubeRealms.id("sculk_stalker"), "main");
	private static final Identifier TEXTURE = GlowcubeRealms.id("textures/entity/sculk_stalker.png");
	private static final RenderType GLOW = RenderTypes.eyes(GlowcubeRealms.id("textures/entity/sculk_stalker_glow.png"));

	public SculkStalkerRenderer(EntityRendererProvider.Context context) {
		super(context, new HumanoidModel<>(context.bakeLayer(LAYER)), 0.5F);
		this.addLayer(new EyesLayer<>(this) {
			@Override
			public RenderType renderType() {
				return GLOW;
			}
		});
	}

	@Override
	public HumanoidRenderState createRenderState() {
		return new HumanoidRenderState();
	}

	@Override
	public Identifier getTextureLocation(HumanoidRenderState state) {
		return TEXTURE;
	}
}
