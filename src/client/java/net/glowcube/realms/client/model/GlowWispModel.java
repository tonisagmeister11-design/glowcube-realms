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

public class GlowWispModel extends EntityModel<RealmRenderState> {
	private final ModelPart core;
	private final ModelPart ring;
	private final ModelPart tail;

	public GlowWispModel(ModelPart root) {
		super(root);
		this.core = root.getChild("core");
		this.ring = root.getChild("ring");
		this.tail = root.getChild("tail");
	}

	public static LayerDefinition createLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		root.addOrReplaceChild("core", CubeListBuilder.create().texOffs(0, 0).addBox(-3, -3, -3, 6, 6, 6), PartPose.offset(0, 17, 0));
		PartDefinition ring = root.addOrReplaceChild("ring", CubeListBuilder.create(), PartPose.offset(0, 17, 0));
		int i = 0;
		for (float[] o : new float[][]{{5, 0}, {-5, 0}, {0, 5}, {0, -5}}) {
			ring.addOrReplaceChild("shard" + i++, CubeListBuilder.create().texOffs(0, 12).addBox(-1, -1, -1, 2, 2, 2),
					PartPose.offsetAndRotation(o[0], 0, o[1], 0.6F, 0.6F, 0.0F));
		}
		root.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(8, 12).addBox(-1, 0, -1, 2, 4, 2), PartPose.offset(0, 20, 0));
		return LayerDefinition.create(mesh, 32, 32);
	}

	@Override
	public void setupAnim(RealmRenderState state) {
		super.setupAnim(state);
		float t = state.ageInTicks;
		float bob = Mth.sin(t * 0.12F) * 1.2F;
		this.core.y += bob;
		this.core.yRot = t * 0.06F;
		this.core.xRot = t * 0.04F;
		this.ring.y += bob;
		this.ring.yRot = -t * 0.18F;
		this.tail.y += bob;
		this.tail.zRot = Mth.sin(t * 0.2F) * 0.3F;
		this.tail.xRot = Mth.cos(t * 0.17F) * 0.3F;
	}
}
