package net.glowcube.realms.client.model;

import net.glowcube.realms.client.render.RealmRenderState;
import net.glowcube.realms.entity.boss.Glowkeeper;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/** Floating crystal titan with orbiting shards and a halo. */
public class GlowkeeperModel extends EntityModel<RealmRenderState> {
	private final ModelPart body, head, halo, rightArm, leftArm, orbit;

	public GlowkeeperModel(ModelPart root) {
		super(root);
		this.body = root.getChild("body");
		this.head = root.getChild("head");
		this.halo = this.head.getChild("halo");
		this.rightArm = root.getChild("right_arm");
		this.leftArm = root.getChild("left_arm");
		this.orbit = root.getChild("orbit");
	}

	public static LayerDefinition createLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-8, -14, -4.5F, 16, 14, 9), PartPose.offset(0, -6, 0));
		body.addOrReplaceChild("skirt", CubeListBuilder.create().texOffs(0, 24).addBox(-6, 0, -3.5F, 12, 8, 7)
				.texOffs(40, 24).addBox(-3.5F, 8, -2, 7, 7, 4)
				.texOffs(62, 24).addBox(-1.5F, 15, -1.5F, 3, 6, 3), PartPose.ZERO);
		PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 40).addBox(-5, -10, -5, 10, 10, 10), PartPose.offset(0, -20, 0));
		head.addOrReplaceChild("crown0", CubeListBuilder.create().texOffs(40, 40).addBox(-1, -6, -1, 2, 6, 2), PartPose.offset(0, -10, 0));
		head.addOrReplaceChild("crown1", CubeListBuilder.create().texOffs(40, 40).addBox(-1, -6, -1, 2, 6, 2), PartPose.offsetAndRotation(-3.5F, -10, 0, 0, 0, -0.35F));
		head.addOrReplaceChild("crown2", CubeListBuilder.create().texOffs(40, 40).addBox(-1, -6, -1, 2, 6, 2), PartPose.offsetAndRotation(3.5F, -10, 0, 0, 0, 0.35F));
		head.addOrReplaceChild("crown3", CubeListBuilder.create().texOffs(40, 40).addBox(-1, -5, -1, 2, 5, 2), PartPose.offsetAndRotation(0, -10, -3.5F, 0.35F, 0, 0));
		PartDefinition halo = head.addOrReplaceChild("halo", CubeListBuilder.create(), PartPose.offset(0, -15, 0));
		halo.addOrReplaceChild("bar0", CubeListBuilder.create().texOffs(0, 62).addBox(-8, -0.5F, -0.5F, 16, 1, 1), PartPose.offset(0, 0, 8));
		halo.addOrReplaceChild("bar1", CubeListBuilder.create().texOffs(0, 62).addBox(-8, -0.5F, -0.5F, 16, 1, 1), PartPose.offset(0, 0, -8));
		halo.addOrReplaceChild("bar2", CubeListBuilder.create().texOffs(0, 62).addBox(-8, -0.5F, -0.5F, 16, 1, 1), PartPose.offsetAndRotation(8, 0, 0, 0, Mth.HALF_PI, 0));
		halo.addOrReplaceChild("bar3", CubeListBuilder.create().texOffs(0, 62).addBox(-8, -0.5F, -0.5F, 16, 1, 1), PartPose.offsetAndRotation(-8, 0, 0, 0, Mth.HALF_PI, 0));
		PartDefinition ra = root.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(56, 0).addBox(-6, -2, -3, 6, 16, 6), PartPose.offset(-8, -18, 0));
		ra.addOrReplaceChild("fist", CubeListBuilder.create().texOffs(64, 40).addBox(-7, 14, -4, 8, 8, 8), PartPose.ZERO);
		ra.addOrReplaceChild("pauldron", CubeListBuilder.create().texOffs(48, 60).addBox(-7, -4, -4, 8, 5, 8), PartPose.ZERO);
		PartDefinition la = root.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(80, 0).mirror().addBox(0, -2, -3, 6, 16, 6), PartPose.offset(8, -18, 0));
		la.addOrReplaceChild("fist", CubeListBuilder.create().texOffs(64, 40).mirror().addBox(-1, 14, -4, 8, 8, 8), PartPose.ZERO);
		la.addOrReplaceChild("pauldron", CubeListBuilder.create().texOffs(48, 60).mirror().addBox(-1, -4, -4, 8, 5, 8), PartPose.ZERO);
		PartDefinition orbit = root.addOrReplaceChild("orbit", CubeListBuilder.create(), PartPose.offset(0, -14, 0));
		int i = 0;
		for (float[] o : new float[][]{{15, 0}, {-15, 0}, {0, 15}, {0, -15}}) {
			orbit.addOrReplaceChild("shard" + i++, CubeListBuilder.create().texOffs(100, 24).addBox(-1.5F, -5, -1.5F, 3, 10, 3),
					PartPose.offsetAndRotation(o[0], 0, o[1], 0.2F, 0.7F, 0.25F));
		}
		return LayerDefinition.create(mesh, 128, 128);
	}

	@Override
	public void setupAnim(RealmRenderState state) {
		super.setupAnim(state);
		float t = state.ageInTicks;
		float bob = Mth.sin(t * 0.08F) * 1.5F;
		this.body.y += bob;
		this.head.y += bob;
		this.rightArm.y += bob;
		this.leftArm.y += bob;
		this.orbit.y += bob;
		this.head.yRot = state.yRot * Mth.DEG_TO_RAD;
		this.head.xRot = state.xRot * Mth.DEG_TO_RAD * 0.6F;
		this.halo.yRot = t * 0.05F;
		this.orbit.yRot = t * (0.06F + state.phase * 0.04F);
		this.rightArm.zRot = 0.15F + Mth.sin(t * 0.06F) * 0.08F;
		this.leftArm.zRot = -0.15F - Mth.sin(t * 0.06F) * 0.08F;
		float at = state.attackTicks;
		switch (state.attack) {
			case Glowkeeper.VOLLEY -> {
				float s = Mth.sin(at * 0.9F);
				this.rightArm.xRot = -1.4F + s * 0.4F;
				this.leftArm.xRot = -1.4F - s * 0.4F;
			}
			case Glowkeeper.BEAM -> {
				this.rightArm.xRot = -1.6F;
				this.leftArm.xRot = -1.6F;
				this.rightArm.yRot = 0.4F;
				this.leftArm.yRot = -0.4F;
			}
			case Glowkeeper.STARFALL, Glowkeeper.SUMMON -> {
				this.rightArm.xRot = -2.8F;
				this.leftArm.xRot = -2.8F;
				this.rightArm.zRot = 0.5F;
				this.leftArm.zRot = -0.5F;
			}
			case Glowkeeper.NOVA -> {
				float charge = Math.min(at / 24.0F, 1.0F);
				this.rightArm.zRot = 0.15F + charge * 1.3F;
				this.leftArm.zRot = -0.15F - charge * 1.3F;
				this.orbit.yRot = t * 0.4F;
			}
			default -> {
			}
		}
		if (state.roaring) {
			this.head.xRot = -0.5F;
			this.rightArm.zRot = 1.2F;
			this.leftArm.zRot = -1.2F;
		}
	}
}
