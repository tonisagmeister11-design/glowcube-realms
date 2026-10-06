package net.glowcube.realms.client.model;

import net.glowcube.realms.client.render.RealmRenderState;
import net.glowcube.realms.entity.boss.TempestDrake;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/** Storm wyvern with two-segment flapping wings. */
public class WyvernModel extends EntityModel<RealmRenderState> {
	private final ModelPart body, neck, head, rightWing, leftWing, rightTip, leftTip, tail;

	public WyvernModel(ModelPart root) {
		super(root);
		this.body = root.getChild("body");
		this.neck = root.getChild("neck");
		this.head = this.neck.getChild("head");
		this.rightWing = root.getChild("right_wing");
		this.leftWing = root.getChild("left_wing");
		this.rightTip = this.rightWing.getChild("tip");
		this.leftTip = this.leftWing.getChild("tip");
		this.tail = root.getChild("tail");
	}

	public static LayerDefinition createLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-6, -5, -10, 12, 10, 20), PartPose.offset(0, 8, 0));
		PartDefinition neck = root.addOrReplaceChild("neck", CubeListBuilder.create().texOffs(64, 0).addBox(-3, -3, -8, 6, 6, 8), PartPose.offsetAndRotation(0, 5, -10, -0.3F, 0, 0));
		PartDefinition head = neck.addOrReplaceChild("head", CubeListBuilder.create().texOffs(64, 14).addBox(-4, -4, -10, 8, 7, 10), PartPose.offsetAndRotation(0, 0, -7, 0.3F, 0, 0));
		head.addOrReplaceChild("horn_r", CubeListBuilder.create().texOffs(100, 0).addBox(-1, -6, 0, 2, 6, 2), PartPose.offsetAndRotation(-3, -3, -2, -0.7F, 0, -0.2F));
		head.addOrReplaceChild("horn_l", CubeListBuilder.create().texOffs(100, 0).addBox(-1, -6, 0, 2, 6, 2), PartPose.offsetAndRotation(3, -3, -2, -0.7F, 0, 0.2F));
		PartDefinition rw = root.addOrReplaceChild("right_wing", CubeListBuilder.create().texOffs(0, 30).addBox(-24, -1, -8, 24, 2, 16), PartPose.offset(-6, 4, -2));
		rw.addOrReplaceChild("tip", CubeListBuilder.create().texOffs(0, 48).addBox(-20, -0.5F, -6, 20, 1, 12), PartPose.offset(-24, 0, 0));
		PartDefinition lw = root.addOrReplaceChild("left_wing", CubeListBuilder.create().texOffs(0, 30).mirror().addBox(0, -1, -8, 24, 2, 16), PartPose.offset(6, 4, -2));
		lw.addOrReplaceChild("tip", CubeListBuilder.create().texOffs(0, 48).mirror().addBox(0, -0.5F, -6, 20, 1, 12), PartPose.offset(24, 0, 0));
		PartDefinition tail = root.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(100, 14).addBox(-2, -2, 0, 4, 4, 10), PartPose.offsetAndRotation(0, 8, 10, -0.15F, 0, 0));
		tail.addOrReplaceChild("tip", CubeListBuilder.create().texOffs(86, 31).addBox(-1.5F, -1.5F, 10, 3, 3, 8), PartPose.ZERO);
		root.addOrReplaceChild("leg_r", CubeListBuilder.create().texOffs(108, 0).addBox(-1.5F, 0, -1.5F, 3, 6, 3), PartPose.offsetAndRotation(-4, 13, 3, 0.6F, 0, 0));
		root.addOrReplaceChild("leg_l", CubeListBuilder.create().texOffs(108, 0).addBox(-1.5F, 0, -1.5F, 3, 6, 3), PartPose.offsetAndRotation(4, 13, 3, 0.6F, 0, 0));
		return LayerDefinition.create(mesh, 128, 64);
	}

	@Override
	public void setupAnim(RealmRenderState state) {
		super.setupAnim(state);
		float t = state.ageInTicks;
		float speed = state.attack == TempestDrake.DIVE ? 0.0F : 0.28F;
		float flap = Mth.sin(t * speed + 0.3F) * 0.6F;
		this.rightWing.zRot = flap;
		this.leftWing.zRot = -flap;
		this.rightTip.zRot = flap * 0.7F;
		this.leftTip.zRot = -flap * 0.7F;
		if (state.attack == TempestDrake.DIVE) {
			this.rightWing.zRot = -0.5F;
			this.leftWing.zRot = 0.5F;
			this.rightWing.yRot = 0.5F;
			this.leftWing.yRot = -0.5F;
			this.body.xRot = 0.4F;
		}
		this.body.y += Mth.sin(t * 0.28F) * 0.8F;
		this.neck.yRot = state.yRot * Mth.DEG_TO_RAD * 0.5F;
		this.head.xRot = 0.3F + state.xRot * Mth.DEG_TO_RAD * 0.5F;
		this.tail.yRot = Mth.sin(t * 0.1F) * 0.3F;
		if (state.attack == TempestDrake.LIGHTNING || state.attack == TempestDrake.STORM || state.roaring) this.head.xRot = -0.4F;
	}
}
