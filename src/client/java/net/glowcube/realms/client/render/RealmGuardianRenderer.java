package net.glowcube.realms.client.render;

import net.glowcube.realms.GlowcubeRealms;
import net.glowcube.realms.entity.RealmGuardian;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.monster.illager.IllagerModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.state.IllagerRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.monster.illager.AbstractIllager;
import net.minecraft.world.item.Items;

/**
 * Village guard drawn with the villager-shaped illager model: arms crossed by day like a villager,
 * weapon drawn (sword, axe or crossbow) at night and in fights. Armor is painted on the three uniform variants.
 */
public class RealmGuardianRenderer extends MobRenderer<RealmGuardian, RealmGuardianRenderer.GuardianState, IllagerModel<RealmGuardianRenderer.GuardianState>> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(GlowcubeRealms.id("realm_guardian"), "main");
	private static final Identifier[] TEXTURES = {
			GlowcubeRealms.id("textures/entity/realm_guardian.png"),
			GlowcubeRealms.id("textures/entity/realm_guardian_chain.png"),
			GlowcubeRealms.id("textures/entity/realm_guardian_iron.png")};

	public RealmGuardianRenderer(EntityRendererProvider.Context context) {
		super(context, new IllagerModel<>(context.bakeLayer(LAYER)), 0.5F);
		this.addLayer(new ItemInHandLayer<>(this));
	}

	public static class GuardianState extends IllagerRenderState {
		public int variant;
	}

	@Override
	public GuardianState createRenderState() {
		return new GuardianState();
	}

	@Override
	public void extractRenderState(RealmGuardian entity, GuardianState state, float partialTicks) {
		state.variant = entity.getVariant();
		super.extractRenderState(entity, state, partialTicks);
		HumanoidMobRenderer.extractHumanoidRenderState(entity, state, partialTicks, this.itemModelResolver);
		state.isRiding = entity.isPassenger();
		state.mainArm = entity.getMainArm();
		state.isAggressive = entity.isAggressive();
		state.maxCrossbowChargeDuration = 25;
		state.ticksUsingItem = 0;
		if (!entity.isOnDuty()) {
			state.armPose = AbstractIllager.IllagerArmPose.CROSSED;
		} else if (entity.getMainHandItem().is(Items.CROSSBOW)) {
			state.armPose = AbstractIllager.IllagerArmPose.CROSSBOW_HOLD;
		} else if (entity.isAggressive()) {
			state.armPose = AbstractIllager.IllagerArmPose.ATTACKING;
		} else {
			state.armPose = AbstractIllager.IllagerArmPose.NEUTRAL;
		}
	}

	@Override
	public Identifier getTextureLocation(GuardianState state) {
		return TEXTURES[Math.floorMod(state.variant, TEXTURES.length)];
	}
}
