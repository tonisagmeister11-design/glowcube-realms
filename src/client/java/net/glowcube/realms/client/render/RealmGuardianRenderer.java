package net.glowcube.realms.client.render;

import net.glowcube.realms.GlowcubeRealms;
import net.glowcube.realms.entity.RealmGuardian;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.ArmorModelSet;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Village guard: humanoid with armor and weapons in hand, bow pose when aiming. */
public class RealmGuardianRenderer extends HumanoidMobRenderer<RealmGuardian, HumanoidRenderState, HumanoidModel<HumanoidRenderState>> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(GlowcubeRealms.id("realm_guardian"), "main");
	private static final Identifier TEXTURE = GlowcubeRealms.id("textures/entity/realm_guardian.png");

	public RealmGuardianRenderer(EntityRendererProvider.Context context) {
		super(context, new HumanoidModel<>(context.bakeLayer(LAYER)), 0.5F);
		this.addLayer(new HumanoidArmorLayer<>(this, ArmorModelSet.bake(ModelLayers.ZOMBIE_ARMOR, context.getModelSet(), HumanoidModel::new),
				context.getEquipmentRenderer()));
	}

	@Override
	public HumanoidRenderState createRenderState() {
		return new HumanoidRenderState();
	}

	@Override
	protected HumanoidModel.ArmPose getArmPose(RealmGuardian mob, HumanoidArm arm) {
		ItemStack held = mob.getItemHeldByArm(arm);
		if (held.is(Items.BOW) && mob.isAggressive()) return HumanoidModel.ArmPose.BOW_AND_ARROW;
		if (held.is(Items.SHIELD) && mob.isBlocking()) return HumanoidModel.ArmPose.BLOCK;
		return super.getArmPose(mob, arm);
	}

	@Override
	public Identifier getTextureLocation(HumanoidRenderState state) {
		return TEXTURE;
	}
}
