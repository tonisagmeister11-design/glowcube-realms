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

public class ShadeCrawlerModel extends EntityModel<RealmRenderState> {
	private final ModelPart head;
	private final ModelPart[] rightLegs = new ModelPart[3];
	private final ModelPart[] leftLegs = new ModelPart[3];

	public ShadeCrawlerModel(ModelPart root) {
		super(root);
		this.head = root.getChild("head");
		for (int i = 0; i < 3; i++) {
			this.rightLegs[i] = root.getChild("right_leg" + i);
			this.leftLegs[i] = root.getChild("left_leg" + i);
		}
	}

	public static LayerDefinition createLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-5, -3, -7, 10, 6, 14), PartPose.offset(0, 16, 0));
		body.addOrReplaceChild("spike0", CubeListBuilder.create().texOffs(0, 34).addBox(-1, -4, -1, 2, 4, 2), PartPose.offsetAndRotation(0, -3, -2, -0.5F, 0, 0));
		body.addOrReplaceChild("spike1", CubeListBuilder.create().texOffs(0, 34).addBox(-1, -4, -1, 2, 4, 2), PartPose.offsetAndRotation(0, -3, 2, -0.6F, 0, 0));
		body.addOrReplaceChild("spike2", CubeListBuilder.create().texOffs(0, 34).addBox(-1, -4, -1, 2, 4, 2), PartPose.offsetAndRotation(0, -3, 6, -0.8F, 0, 0));
		PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 20).addBox(-4, -3, -7, 8, 6, 7), PartPose.offset(0, 16, -7));
		head.addOrReplaceChild("mandible_r", CubeListBuilder.create().texOffs(30, 20).addBox(-1, 0, -3, 2, 2, 3), PartPose.offsetAndRotation(-2.5F, 1.5F, -7, 0, 0.3F, 0));
		head.addOrReplaceChild("mandible_l", CubeListBuilder.create().texOffs(30, 20).addBox(-1, 0, -3, 2, 2, 3), PartPose.offsetAndRotation(2.5F, 1.5F, -7, 0, -0.3F, 0));
		float[] z = {-4, 1, 6};
		float[] yRots = {0.5F, 0.0F, -0.5F};
		for (int i = 0; i < 3; i++) {
			root.addOrReplaceChild("right_leg" + i, CubeListBuilder.create().texOffs(30, 26).addBox(-12, -1, -1, 12, 2, 2),
					PartPose.offsetAndRotation(-4, 16, z[i], 0, yRots[i], -0.7F));
			root.addOrReplaceChild("left_leg" + i, CubeListBuilder.create().texOffs(30, 26).mirror().addBox(0, -1, -1, 12, 2, 2),
					PartPose.offsetAndRotation(4, 16, z[i], 0, -yRots[i], 0.7F));
		}
		return LayerDefinition.create(mesh, 64, 64);
	}

	@Override
	public void setupAnim(RealmRenderState state) {
		super.setupAnim(state);
		this.head.yRot = state.yRot * Mth.DEG_TO_RAD;
		this.head.xRot = state.xRot * Mth.DEG_TO_RAD;
		float pos = state.walkAnimationPos * 0.6662F * 2, speed = state.walkAnimationSpeed;
		for (int i = 0; i < 3; i++) {
			float phase = i * (float) Math.PI * 0.66F;
			float swing = Mth.cos(pos + phase) * 0.5F * speed;
			float lift = Math.abs(Mth.sin(pos + phase)) * 0.5F * speed;
			this.rightLegs[i].yRot += swing;
			this.rightLegs[i].zRot += lift;
			this.leftLegs[i].yRot -= swing;
			this.leftLegs[i].zRot -= lift;
		}
	}
}
