package net.glowcube.realms.client.model;

import net.glowcube.realms.client.render.RealmRenderState;
import net.glowcube.realms.entity.boss.UmbralTyrant;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/** Horned shadow beast. */
public class UmbralTyrantModel extends EntityModel<RealmRenderState> {
	private final ModelPart body, head, jaw, tail, legFR, legFL, legBR, legBL;

	public UmbralTyrantModel(ModelPart root) {
		super(root);
		this.body = root.getChild("body");
		this.head = root.getChild("head");
		this.jaw = this.head.getChild("jaw");
		this.tail = root.getChild("tail");
		this.legFR = root.getChild("leg_fr");
		this.legFL = root.getChild("leg_fl");
		this.legBR = root.getChild("leg_br");
		this.legBL = root.getChild("leg_bl");
	}

	public static LayerDefinition createLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-10, -8, -14, 20, 16, 28), PartPose.offset(0, 4, 0));
		for (int i = 0; i < 4; i++) {
			body.addOrReplaceChild("spike" + i, CubeListBuilder.create().texOffs(108, 0).addBox(-1.5F, -9, -1.5F, 3, 9, 3),
					PartPose.offsetAndRotation(0, -8, -9 + i * 6, -0.35F, 0, 0));
		}
		body.addOrReplaceChild("spike_r", CubeListBuilder.create().texOffs(108, 0).addBox(-1.5F, -9, -1.5F, 3, 9, 3), PartPose.offsetAndRotation(-8, -8, -10, -0.2F, 0, -0.6F));
		body.addOrReplaceChild("spike_l", CubeListBuilder.create().texOffs(108, 0).addBox(-1.5F, -9, -1.5F, 3, 9, 3), PartPose.offsetAndRotation(8, -8, -10, -0.2F, 0, 0.6F));
		PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 44).addBox(-7, -6, -12, 14, 12, 12), PartPose.offset(0, 0, -14));
		head.addOrReplaceChild("jaw", CubeListBuilder.create().texOffs(52, 44).addBox(-6, 0, -11, 12, 4, 10), PartPose.offset(0, 5, -1));
		head.addOrReplaceChild("horn_r", CubeListBuilder.create().texOffs(96, 0).addBox(-1.5F, -12, -1.5F, 3, 12, 3), PartPose.offsetAndRotation(-6, -5, -6, -0.3F, 0, -0.55F));
		head.addOrReplaceChild("horn_l", CubeListBuilder.create().texOffs(96, 0).addBox(-1.5F, -12, -1.5F, 3, 12, 3), PartPose.offsetAndRotation(6, -5, -6, -0.3F, 0, 0.55F));
		PartDefinition tail = root.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(0, 68).addBox(-3, -3, 0, 6, 6, 16), PartPose.offsetAndRotation(0, 0, 14, -0.3F, 0, 0));
		tail.addOrReplaceChild("tip", CubeListBuilder.create().texOffs(44, 68).addBox(-1.5F, -1.5F, 16, 3, 3, 6), PartPose.ZERO);
		root.addOrReplaceChild("leg_fr", CubeListBuilder.create().texOffs(96, 16).addBox(-3.5F, 0, -3.5F, 7, 12, 7), PartPose.offset(-7, 12, -10));
		root.addOrReplaceChild("leg_fl", CubeListBuilder.create().texOffs(96, 16).mirror().addBox(-3.5F, 0, -3.5F, 7, 12, 7), PartPose.offset(7, 12, -10));
		root.addOrReplaceChild("leg_br", CubeListBuilder.create().texOffs(96, 16).addBox(-3.5F, 0, -3.5F, 7, 12, 7), PartPose.offset(-7, 12, 10));
		root.addOrReplaceChild("leg_bl", CubeListBuilder.create().texOffs(96, 16).mirror().addBox(-3.5F, 0, -3.5F, 7, 12, 7), PartPose.offset(7, 12, 10));
		return LayerDefinition.create(mesh, 128, 128);
	}

	@Override
	public void setupAnim(RealmRenderState state) {
		super.setupAnim(state);
		float t = state.ageInTicks;
		float pos = state.walkAnimationPos * 0.6662F, speed = Math.min(state.walkAnimationSpeed, 1.0F);
		this.head.yRot = state.yRot * Mth.DEG_TO_RAD * 0.7F;
		this.head.xRot = state.xRot * Mth.DEG_TO_RAD * 0.5F;
		this.legFR.xRot = Mth.cos(pos) * 1.1F * speed;
		this.legBL.xRot = Mth.cos(pos) * 1.1F * speed;
		this.legFL.xRot = Mth.cos(pos + Mth.PI) * 1.1F * speed;
		this.legBR.xRot = Mth.cos(pos + Mth.PI) * 1.1F * speed;
		this.tail.yRot = Mth.sin(t * 0.08F) * 0.35F;
		this.jaw.xRot = 0.05F + Math.max(0, Mth.sin(t * 0.04F)) * 0.15F;
		float breathe = Mth.sin(t * 0.06F) * 0.4F;
		this.body.y += breathe;
		float at = state.attackTicks;
		switch (state.attack) {
			case UmbralTyrant.LEAP -> {
				if (at < 12) {
					this.body.xRot = -0.25F;
					this.head.xRot = -0.5F;
					this.jaw.xRot = 0.7F;
				} else {
					this.legFR.xRot = -1.0F;
					this.legFL.xRot = -1.0F;
					this.legBR.xRot = 0.9F;
					this.legBL.xRot = 0.9F;
				}
			}
			case UmbralTyrant.CHARGE -> {
				this.head.xRot = 0.45F;
				this.jaw.xRot = 0.4F;
				if (at >= 18) {
					float c = Mth.cos(at * 1.2F) * 1.3F;
					this.legFR.xRot = c;
					this.legBL.xRot = c;
					this.legFL.xRot = -c;
					this.legBR.xRot = -c;
				}
			}
			case UmbralTyrant.ORBS, UmbralTyrant.PULSE -> {
				this.jaw.xRot = 0.9F;
				this.head.xRot = -0.4F;
			}
			case UmbralTyrant.FANGS, UmbralTyrant.SUMMON -> {
				this.body.xRot = -0.35F;
				this.head.xRot = -0.6F;
				this.jaw.xRot = 0.8F;
				this.legFR.xRot = -0.9F;
				this.legFL.xRot = -0.9F;
			}
			default -> {
			}
		}
		if (state.roaring) {
			this.head.xRot = -0.7F;
			this.jaw.xRot = 1.0F;
		}
	}
}
