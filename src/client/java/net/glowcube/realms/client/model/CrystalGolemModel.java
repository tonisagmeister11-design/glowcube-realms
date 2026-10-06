package net.glowcube.realms.client.model;

import net.glowcube.realms.client.render.RealmRenderState;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

public class CrystalGolemModel extends EntityModel<RealmRenderState> {
	private final ModelPart head, rightArm, leftArm, rightLeg, leftLeg;

	public CrystalGolemModel(ModelPart root) {
		super(root);
		ModelPart body = root.getChild("body");
		this.head = root.getChild("head");
		this.rightArm = root.getChild("right_arm");
		this.leftArm = root.getChild("left_arm");
		this.rightLeg = root.getChild("right_leg");
		this.leftLeg = root.getChild("left_leg");
	}

	public static LayerDefinition createLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-7, 0, -4, 14, 13, 8), PartPose.ZERO);
		body.addOrReplaceChild("spike0", CubeListBuilder.create().texOffs(88, 0).addBox(-1.5F, -7, -1.5F, 3, 7, 3), PartPose.offsetAndRotation(0, 2, 4, -0.5F, 0, 0));
		body.addOrReplaceChild("spike1", CubeListBuilder.create().texOffs(88, 0).addBox(-1.5F, -7, -1.5F, 3, 7, 3), PartPose.offsetAndRotation(-4, 3, 4, -0.4F, 0, -0.4F));
		body.addOrReplaceChild("spike2", CubeListBuilder.create().texOffs(88, 0).addBox(-1.5F, -7, -1.5F, 3, 7, 3), PartPose.offsetAndRotation(4, 3, 4, -0.4F, 0, 0.4F));
		PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 22).addBox(-4, -8, -4, 8, 8, 8), PartPose.offset(0, 0, -1));
		head.addOrReplaceChild("horn", CubeListBuilder.create().texOffs(32, 22).addBox(-1, -13, -2, 2, 5, 2), PartPose.ZERO);
		PartDefinition ra = root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(48, 0).addBox(-5, -1, -2.5F, 5, 18, 5), PartPose.offset(-7, 2, 0));
		ra.addOrReplaceChild("fist", CubeListBuilder.create().texOffs(100, 0).addBox(-6, 16, -3.5F, 7, 6, 7), PartPose.ZERO);
		PartDefinition la = root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(68, 0).mirror().addBox(0, -1, -2.5F, 5, 18, 5), PartPose.offset(7, 2, 0));
		la.addOrReplaceChild("fist", CubeListBuilder.create().texOffs(100, 0).mirror().addBox(-1, 16, -3.5F, 7, 6, 7), PartPose.ZERO);
		root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(0, 40).addBox(-3, 0, -3, 6, 11, 6), PartPose.offset(-3.5F, 13, 0));
		root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(24, 40).mirror().addBox(-3, 0, -3, 6, 11, 6), PartPose.offset(3.5F, 13, 0));
		return LayerDefinition.create(mesh, 128, 64);
	}

	@Override
	public void setupAnim(RealmRenderState state) {
		super.setupAnim(state);
		float pos = state.walkAnimationPos, speed = state.walkAnimationSpeed;
		this.head.yRot = state.yRot * Mth.DEG_TO_RAD;
		this.head.xRot = state.xRot * Mth.DEG_TO_RAD;
		this.rightLeg.xRot = -1.2F * Mth.triangleWave(pos, 13.0F) * speed;
		this.leftLeg.xRot = 1.2F * Mth.triangleWave(pos, 13.0F) * speed;
		if (state.attackAnim > 0) {
			float a = Mth.triangleWave(state.attackAnim, 10.0F);
			this.rightArm.xRot = -2.0F + 1.5F * a;
			this.leftArm.xRot = -2.0F + 1.5F * a;
		} else {
			this.rightArm.xRot = (-0.2F + 1.2F * Mth.triangleWave(pos, 13.0F)) * speed;
			this.leftArm.xRot = (-0.2F - 1.2F * Mth.triangleWave(pos, 13.0F)) * speed;
		}
		this.rightArm.zRot = 0.08F + Mth.sin(state.ageInTicks * 0.05F) * 0.03F;
		this.leftArm.zRot = -0.08F - Mth.sin(state.ageInTicks * 0.05F) * 0.03F;
	}
}
