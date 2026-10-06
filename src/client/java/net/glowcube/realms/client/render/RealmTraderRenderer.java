package net.glowcube.realms.client.render;

import net.glowcube.realms.GlowcubeRealms;
import net.glowcube.realms.entity.RealmTrader;
import net.minecraft.client.model.monster.illager.IllagerModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.IllagerRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.monster.illager.AbstractIllager;

/** Realm merchant: villager-shaped body with a robe per realm, arms crossed like a villager. */
public class RealmTraderRenderer extends MobRenderer<RealmTrader, RealmTraderRenderer.TraderState, IllagerModel<RealmTraderRenderer.TraderState>> {
	private static final Identifier[] TEXTURES = {
			GlowcubeRealms.id("textures/entity/realm_trader_lumen.png"),
			GlowcubeRealms.id("textures/entity/realm_trader_umbral.png"),
			GlowcubeRealms.id("textures/entity/realm_trader_sculk.png")};

	public RealmTraderRenderer(EntityRendererProvider.Context context) {
		super(context, new IllagerModel<>(context.bakeLayer(RealmGuardianRenderer.LAYER)), 0.5F);
	}

	public static class TraderState extends IllagerRenderState {
		public int variant;
	}

	@Override
	public TraderState createRenderState() {
		return new TraderState();
	}

	@Override
	public void extractRenderState(RealmTrader entity, TraderState state, float partialTicks) {
		state.variant = entity.getVariant();
		super.extractRenderState(entity, state, partialTicks);
		HumanoidMobRenderer.extractHumanoidRenderState(entity, state, partialTicks, this.itemModelResolver);
		state.isRiding = entity.isPassenger();
		state.mainArm = entity.getMainArm();
		state.isAggressive = false;
		state.maxCrossbowChargeDuration = 25;
		state.ticksUsingItem = 0;
		state.armPose = AbstractIllager.IllagerArmPose.CROSSED;
	}

	@Override
	public Identifier getTextureLocation(TraderState state) {
		return TEXTURES[Math.floorMod(state.variant, TEXTURES.length)];
	}
}
