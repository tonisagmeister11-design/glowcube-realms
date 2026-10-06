package net.glowcube.realms.client.model;

import net.glowcube.realms.client.render.RealmRenderState;
import net.glowcube.realms.entity.boss.EmberWarden;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/** Burning knight with cape and a giant greatsword. */
public class EmberWardenModel extends EntityModel<RealmRenderState> {
	private final ModelPart head, body, cape, rightArm, leftArm, rightLeg, leftLeg;

	public EmberWardenModel(ModelPart root) {
		super(root);
		this.head = root.getChild("head");
		this.body = root.getChild("body");
		this.cape = this.body.getChild("cape");
		this.rightArm = root.getChild("right_arm");
		this.leftArm = root.getChild("left_arm");
		this.rightLeg = root.getChild("right_leg");
		this.leftLeg = root.getChild("left_leg");
	}

	public static LayerDefinition createLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 0).addBox(-4, -9, -4, 8, 9, 8), PartPose.ZERO);
		head.addOrReplaceChild("crest", CubeListBuilder.create().texOffs(32, 0).addBox(-1, -14, -4, 2, 5, 9), PartPose.ZERO);
		PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 17).addBox(-5, 0, -3, 10, 12, 6), PartPose.ZERO);
		body.addOrReplaceChild("cape", CubeListBuilder.create().texOffs(84, 18).addBox(-5, 0, 0, 10, 16, 1), PartPose.offsetAndRotation(0, 0, 3, 0.1F, 0, 0));
		PartDefinition ra = root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(54, 0).addBox(-3, -2, -2, 4, 13, 4), PartPose.offset(-6, 2, 0));
		ra.addOrReplaceChild("pauldron", CubeListBuilder.create().texOffs(86, 0).addBox(-4, -4, -3, 6, 4, 6), PartPose.ZERO);
		ra.addOrReplaceChild("blade", CubeListBuilder.create().texOffs(40, 18).addBox(-2, 9, -24, 2, 4, 20), PartPose.ZERO);
		ra.addOrReplaceChild("guard", CubeListBuilder.create().texOffs(106, 18).addBox(-3.5F, 8, -5, 5, 6, 1), PartPose.ZERO);
		PartDefinition la = root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(70, 0).mirror().addBox(-1, -2, -2, 4, 13, 4), PartPose.offset(6, 2, 0));
		la.addOrReplaceChild("pauldron", CubeListBuilder.create().texOffs(86, 0).mirror().addBox(-2, -4, -3, 6, 4, 6), PartPose.ZERO);
		root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(0, 36).addBox(-2.5F, 0, -2.5F, 5, 12, 5), PartPose.offset(-2.5F, 12, 0));
		root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(20, 36).mirror().addBox(-2.5F, 0, -2.5F, 5, 12, 5), PartPose.offset(2.5F, 12, 0));
		return LayerDefinition.create(mesh, 128, 64);
	}

	@Override
	public void setupAnim(RealmRenderState state) {
		super.setupAnim(state);
		float pos = state.walkAnimationPos * 0.6662F, speed = state.walkAnimationSpeed;
		float t = state.ageInTicks;
		this.head.yRot = state.yRot * Mth.DEG_TO_RAD;
		this.head.xRot = state.xRot * Mth.DEG_TO_RAD;
		this.rightLeg.xRot = Mth.cos(pos) * 1.2F * speed;
		this.leftLeg.xRot = Mth.cos(pos + Mth.PI) * 1.2F * speed;
		this.leftArm.xRot = Mth.cos(pos) * 0.8F * speed;
		this.rightArm.xRot = -0.35F + Mth.cos(pos + Mth.PI) * 0.4F * speed;
		this.cape.xRot = 0.1F + speed * 0.6F + Mth.sin(t * 0.1F) * 0.05F;
		if (state.attackAnim > 0) {
			float a = Mth.sin(state.attackAnim * Mth.PI);
			this.rightArm.xRot = -2.2F + a * 2.0F;
			this.body.yRot = a * 0.4F;
		}
		float at = state.attackTicks;
		switch (state.attack) {
			case EmberWarden.BARRAGE, EmberWarden.METEORS -> {
				this.leftArm.xRot = -1.7F + Mth.sin(at * 0.5F) * 0.2F;
				this.rightArm.xRot = -0.4F;
			}
			case EmberWarden.PILLARS, EmberWarden.SUMMON -> {
				this.rightArm.xRot = -3.0F;
				this.leftArm.xRot = -3.0F;
			}
			case EmberWarden.WHIRL -> {
				this.rightArm.xRot = -1.57F;
				this.rightArm.yRot = -0.8F;
				this.leftArm.xRot = -1.2F;
			}
			case EmberWarden.DASH -> {
				this.rightArm.xRot = at < 12 ? -2.6F : -1.2F;
				this.body.xRot = 0.25F;
			}
			default -> {
			}
		}
		if (state.roaring) {
			this.head.xRot = -0.4F;
			this.rightArm.xRot = -3.0F;
			this.leftArm.zRot = -0.8F;
		}
	}
}
