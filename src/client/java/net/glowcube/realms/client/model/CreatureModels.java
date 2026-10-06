package net.glowcube.realms.client.model;

import net.glowcube.realms.client.render.RealmRenderState;
import net.glowcube.realms.entity.boss.EchoWarden;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/** Models for the update-3 creatures. The texture generator paints the same UV boxes. */
public final class CreatureModels {

	/** Glimmer Deer: graceful deer with glowing crystal antlers. Texture 64x64. */
	public static class Deer extends EntityModel<RealmRenderState> {
		private final ModelPart neck, legFR, legFL, legBR, legBL, tail;

		public Deer(ModelPart root) {
			super(root);
			this.neck = root.getChild("neck");
			this.legFR = root.getChild("leg_fr");
			this.legFL = root.getChild("leg_fl");
			this.legBR = root.getChild("leg_br");
			this.legBL = root.getChild("leg_bl");
			this.tail = root.getChild("tail");
		}

		public static LayerDefinition createLayer() {
			MeshDefinition mesh = new MeshDefinition();
			PartDefinition root = mesh.getRoot();
			root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-4, -4, -8, 8, 8, 16), PartPose.offset(0, 10, 0));
			PartDefinition neck = root.addOrReplaceChild("neck", CubeListBuilder.create().texOffs(0, 24).addBox(-2, -9, -2, 4, 9, 4), PartPose.offsetAndRotation(0, 8, -7, 0.35F, 0, 0));
			PartDefinition head = neck.addOrReplaceChild("head", CubeListBuilder.create().texOffs(16, 24).addBox(-3, -4, -7, 6, 5, 8), PartPose.offsetAndRotation(0, -8, 0, -0.35F, 0, 0));
			head.addOrReplaceChild("antler_r", CubeListBuilder.create().texOffs(44, 24).addBox(-0.5F, -8, -3, 1, 8, 6), PartPose.offsetAndRotation(-2, -3, -1, 0, 0, -0.4F));
			head.addOrReplaceChild("antler_l", CubeListBuilder.create().texOffs(44, 24).mirror().addBox(-0.5F, -8, -3, 1, 8, 6), PartPose.offsetAndRotation(2, -3, -1, 0, 0, 0.4F));
			head.addOrReplaceChild("ear_r", CubeListBuilder.create().texOffs(0, 37).addBox(-3, -1, 0, 3, 2, 1), PartPose.offsetAndRotation(-3, -3, -1, 0, 0, 0.3F));
			head.addOrReplaceChild("ear_l", CubeListBuilder.create().texOffs(0, 37).mirror().addBox(0, -1, 0, 3, 2, 1), PartPose.offsetAndRotation(3, -3, -1, 0, 0, -0.3F));
			root.addOrReplaceChild("leg_fr", CubeListBuilder.create().texOffs(0, 40).addBox(-1, 0, -1, 2, 10, 2), PartPose.offset(-2.5F, 14, -6));
			root.addOrReplaceChild("leg_fl", CubeListBuilder.create().texOffs(0, 40).mirror().addBox(-1, 0, -1, 2, 10, 2), PartPose.offset(2.5F, 14, -6));
			root.addOrReplaceChild("leg_br", CubeListBuilder.create().texOffs(8, 40).addBox(-1, 0, -1, 2, 10, 2), PartPose.offset(-2.5F, 14, 6));
			root.addOrReplaceChild("leg_bl", CubeListBuilder.create().texOffs(8, 40).mirror().addBox(-1, 0, -1, 2, 10, 2), PartPose.offset(2.5F, 14, 6));
			root.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(16, 40).addBox(-1, 0, 0, 2, 3, 2), PartPose.offsetAndRotation(0, 7, 8, 0.6F, 0, 0));
			return LayerDefinition.create(mesh, 64, 64);
		}

		@Override
		public void setupAnim(RealmRenderState s) {
			super.setupAnim(s);
			float p = s.walkAnimationPos * 0.6662F, sp = s.walkAnimationSpeed;
			this.legFR.xRot = Mth.cos(p) * 1.2F * sp;
			this.legBL.xRot = Mth.cos(p) * 1.2F * sp;
			this.legFL.xRot = Mth.cos(p + Mth.PI) * 1.2F * sp;
			this.legBR.xRot = Mth.cos(p + Mth.PI) * 1.2F * sp;
			this.neck.yRot = s.yRot * Mth.DEG_TO_RAD * 0.6F;
			this.tail.xRot = 0.6F + Mth.sin(s.ageInTicks * 0.3F) * 0.15F;
		}
	}

	/** Cloud Bunny: round fluffy bunny with long ears. Texture 32x32. */
	public static class Bunny extends EntityModel<RealmRenderState> {
		private final ModelPart body, head, earR, earL, legBR, legBL;

		public Bunny(ModelPart root) {
			super(root);
			this.body = root.getChild("body");
			this.head = root.getChild("head");
			this.earR = this.head.getChild("ear_r");
			this.earL = this.head.getChild("ear_l");
			this.legBR = root.getChild("leg_br");
			this.legBL = root.getChild("leg_bl");
		}

		public static LayerDefinition createLayer() {
			MeshDefinition mesh = new MeshDefinition();
			PartDefinition root = mesh.getRoot();
			root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-3, -3, -4, 6, 6, 8), PartPose.offset(0, 19, 1));
			PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 14).addBox(-2.5F, -4, -4, 5, 5, 5), PartPose.offset(0, 17, -3));
			head.addOrReplaceChild("ear_r", CubeListBuilder.create().texOffs(20, 14).addBox(-1, -6, -0.5F, 2, 6, 1), PartPose.offsetAndRotation(-1.2F, -3.5F, -1, -0.2F, 0, -0.15F));
			head.addOrReplaceChild("ear_l", CubeListBuilder.create().texOffs(20, 14).mirror().addBox(-1, -6, -0.5F, 2, 6, 1), PartPose.offsetAndRotation(1.2F, -3.5F, -1, -0.2F, 0, 0.15F));
			root.addOrReplaceChild("leg_br", CubeListBuilder.create().texOffs(0, 24).addBox(-1, 0, -2, 2, 3, 4), PartPose.offset(-2, 21, 3));
			root.addOrReplaceChild("leg_bl", CubeListBuilder.create().texOffs(0, 24).mirror().addBox(-1, 0, -2, 2, 3, 4), PartPose.offset(2, 21, 3));
			root.addOrReplaceChild("leg_f", CubeListBuilder.create().texOffs(12, 24).addBox(-2.5F, 0, -1, 5, 3, 2), PartPose.offset(0, 21, -2));
			root.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(28, 0).addBox(-1.5F, -1.5F, 0, 3, 3, 2), PartPose.offset(0, 18, 5));
			return LayerDefinition.create(mesh, 64, 32);
		}

		@Override
		public void setupAnim(RealmRenderState s) {
			super.setupAnim(s);
			float hop = Math.abs(Mth.sin(s.walkAnimationPos * 0.8F)) * s.walkAnimationSpeed * 3.0F;
			this.body.y -= hop;
			this.head.y -= hop;
			this.legBR.xRot = -hop * 0.4F;
			this.legBL.xRot = -hop * 0.4F;
			this.head.yRot = s.yRot * Mth.DEG_TO_RAD;
			this.earR.xRot = -0.2F + Mth.sin(s.ageInTicks * 0.1F) * 0.1F;
			this.earL.xRot = -0.2F + Mth.cos(s.ageInTicks * 0.1F) * 0.1F;
		}
	}

	/** Shade Toad: wide toad with bulging eyes. Texture 64x32. */
	public static class Toad extends EntityModel<RealmRenderState> {
		private final ModelPart body, legBR, legBL, throat;

		public Toad(ModelPart root) {
			super(root);
			this.body = root.getChild("body");
			this.throat = this.body.getChild("throat");
			this.legBR = root.getChild("leg_br");
			this.legBL = root.getChild("leg_bl");
		}

		public static LayerDefinition createLayer() {
			MeshDefinition mesh = new MeshDefinition();
			PartDefinition root = mesh.getRoot();
			PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-5, -5, -6, 10, 6, 11), PartPose.offset(0, 21, 0));
			body.addOrReplaceChild("eye_r", CubeListBuilder.create().texOffs(42, 0).addBox(-1.5F, -2, -1.5F, 3, 2, 3), PartPose.offset(-3, -5, -4));
			body.addOrReplaceChild("eye_l", CubeListBuilder.create().texOffs(42, 0).mirror().addBox(-1.5F, -2, -1.5F, 3, 2, 3), PartPose.offset(3, -5, -4));
			body.addOrReplaceChild("throat", CubeListBuilder.create().texOffs(42, 6).addBox(-3, 0, -2, 6, 2, 3), PartPose.offset(0, 0, -4));
			root.addOrReplaceChild("leg_br", CubeListBuilder.create().texOffs(0, 17).addBox(-2, 0, -3, 4, 3, 6), PartPose.offset(-5, 21, 2));
			root.addOrReplaceChild("leg_bl", CubeListBuilder.create().texOffs(0, 17).mirror().addBox(-2, 0, -3, 4, 3, 6), PartPose.offset(5, 21, 2));
			root.addOrReplaceChild("leg_fr", CubeListBuilder.create().texOffs(20, 17).addBox(-1, 0, -1, 2, 3, 2), PartPose.offset(-4, 21, -4));
			root.addOrReplaceChild("leg_fl", CubeListBuilder.create().texOffs(20, 17).mirror().addBox(-1, 0, -1, 2, 3, 2), PartPose.offset(4, 21, -4));
			return LayerDefinition.create(mesh, 64, 32);
		}

		@Override
		public void setupAnim(RealmRenderState s) {
			super.setupAnim(s);
			float hop = Math.abs(Mth.sin(s.walkAnimationPos * 0.6F)) * s.walkAnimationSpeed * 2.5F;
			this.body.y -= hop;
			this.body.xRot = -hop * 0.08F;
			this.legBR.xRot = hop * 0.5F;
			this.legBL.xRot = hop * 0.5F;
			this.throat.y += Mth.sin(s.ageInTicks * 0.4F) * 0.3F;
		}
	}

	/** Sculk Snail: glowing spiral shell on a slimy foot. Texture 64x32. */
	public static class Snail extends EntityModel<RealmRenderState> {
		private final ModelPart foot, shell, eyeR, eyeL;

		public Snail(ModelPart root) {
			super(root);
			this.foot = root.getChild("foot");
			this.shell = root.getChild("shell");
			this.eyeR = root.getChild("eye_r");
			this.eyeL = root.getChild("eye_l");
		}

		public static LayerDefinition createLayer() {
			MeshDefinition mesh = new MeshDefinition();
			PartDefinition root = mesh.getRoot();
			root.addOrReplaceChild("foot", CubeListBuilder.create().texOffs(0, 0).addBox(-3, -3, -8, 6, 3, 14), PartPose.offset(0, 24, 0));
			root.addOrReplaceChild("shell", CubeListBuilder.create().texOffs(0, 17).addBox(-4, -8, -4, 8, 8, 8), PartPose.offsetAndRotation(0, 21, 1, 0.25F, 0, 0));
			root.addOrReplaceChild("eye_r", CubeListBuilder.create().texOffs(40, 0).addBox(-0.5F, -5, -0.5F, 1, 5, 1), PartPose.offsetAndRotation(-1.5F, 21, -7, -0.3F, 0, -0.2F));
			root.addOrReplaceChild("eye_l", CubeListBuilder.create().texOffs(40, 0).mirror().addBox(-0.5F, -5, -0.5F, 1, 5, 1), PartPose.offsetAndRotation(1.5F, 21, -7, -0.3F, 0, 0.2F));
			return LayerDefinition.create(mesh, 64, 64);
		}

		@Override
		public void setupAnim(RealmRenderState s) {
			super.setupAnim(s);
			float t = s.ageInTicks;
			this.foot.zScale = 1.0F + Mth.sin(s.walkAnimationPos * 0.5F) * 0.08F * s.walkAnimationSpeed;
			this.shell.yRot = Mth.sin(t * 0.05F) * 0.05F;
			this.eyeR.xRot = -0.3F + Mth.sin(t * 0.15F) * 0.2F;
			this.eyeL.xRot = -0.3F + Mth.cos(t * 0.13F) * 0.2F;
		}
	}

	/** Ember Salamander: low lizard with a long flaming tail. Texture 64x32. */
	public static class Lizard extends EntityModel<RealmRenderState> {
		private final ModelPart head, tail, legFR, legFL, legBR, legBL;

		public Lizard(ModelPart root) {
			super(root);
			this.head = root.getChild("head");
			this.tail = root.getChild("tail");
			this.legFR = root.getChild("leg_fr");
			this.legFL = root.getChild("leg_fl");
			this.legBR = root.getChild("leg_br");
			this.legBL = root.getChild("leg_bl");
		}

		public static LayerDefinition createLayer() {
			MeshDefinition mesh = new MeshDefinition();
			PartDefinition root = mesh.getRoot();
			root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-3, -2, -7, 6, 4, 14), PartPose.offset(0, 20, 0));
			root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 18).addBox(-2.5F, -2, -6, 5, 4, 6), PartPose.offset(0, 20, -7));
			root.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(22, 18).addBox(-1.5F, -1.5F, 0, 3, 3, 12), PartPose.offsetAndRotation(0, 20, 7, -0.1F, 0, 0));
			root.addOrReplaceChild("leg_fr", CubeListBuilder.create().texOffs(40, 0).addBox(-4, 0, -1, 4, 2, 2), PartPose.offsetAndRotation(-3, 20, -5, 0, 0, -0.5F));
			root.addOrReplaceChild("leg_fl", CubeListBuilder.create().texOffs(40, 0).mirror().addBox(0, 0, -1, 4, 2, 2), PartPose.offsetAndRotation(3, 20, -5, 0, 0, 0.5F));
			root.addOrReplaceChild("leg_br", CubeListBuilder.create().texOffs(40, 0).addBox(-4, 0, -1, 4, 2, 2), PartPose.offsetAndRotation(-3, 20, 5, 0, 0, -0.5F));
			root.addOrReplaceChild("leg_bl", CubeListBuilder.create().texOffs(40, 0).mirror().addBox(0, 0, -1, 4, 2, 2), PartPose.offsetAndRotation(3, 20, 5, 0, 0, 0.5F));
			return LayerDefinition.create(mesh, 64, 64);
		}

		@Override
		public void setupAnim(RealmRenderState s) {
			super.setupAnim(s);
			float p = s.walkAnimationPos * 0.9F, sp = s.walkAnimationSpeed;
			this.legFR.yRot = Mth.cos(p) * 0.8F * sp;
			this.legBL.yRot = Mth.cos(p) * 0.8F * sp;
			this.legFL.yRot = -Mth.cos(p) * 0.8F * sp;
			this.legBR.yRot = -Mth.cos(p) * 0.8F * sp;
			this.tail.yRot = Mth.sin(s.ageInTicks * 0.15F) * 0.35F + Mth.cos(p) * 0.3F * sp;
			this.head.yRot = s.yRot * Mth.DEG_TO_RAD;
		}
	}

	/** Void Jelly: floating end jellyfish with swaying tentacles. Texture 64x64. */
	public static class Jelly extends EntityModel<RealmRenderState> {
		private final ModelPart bell;
		private final ModelPart[] tentacles = new ModelPart[8];

		public Jelly(ModelPart root) {
			super(root);
			this.bell = root.getChild("bell");
			for (int i = 0; i < 8; i++) this.tentacles[i] = root.getChild("tentacle" + i);
		}

		public static LayerDefinition createLayer() {
			MeshDefinition mesh = new MeshDefinition();
			PartDefinition root = mesh.getRoot();
			root.addOrReplaceChild("bell", CubeListBuilder.create().texOffs(0, 0).addBox(-6, -6, -6, 12, 7, 12).texOffs(0, 19).addBox(-4, 1, -4, 8, 3, 8), PartPose.offset(0, 6, 0));
			for (int i = 0; i < 8; i++) {
				double a = i * Math.PI / 4;
				root.addOrReplaceChild("tentacle" + i, CubeListBuilder.create().texOffs(48, 0).addBox(-0.5F, 0, -0.5F, 1, 14, 1),
						PartPose.offset((float) Math.cos(a) * 4.5F, 9, (float) Math.sin(a) * 4.5F));
			}
			return LayerDefinition.create(mesh, 64, 64);
		}

		@Override
		public void setupAnim(RealmRenderState s) {
			super.setupAnim(s);
			float t = s.ageInTicks;
			float pulse = Mth.sin(t * 0.12F);
			this.bell.xScale = 1.0F + pulse * 0.08F;
			this.bell.zScale = 1.0F + pulse * 0.08F;
			this.bell.yScale = 1.0F - pulse * 0.06F;
			for (int i = 0; i < 8; i++) {
				this.tentacles[i].xRot = Mth.sin(t * 0.1F + i) * 0.25F;
				this.tentacles[i].zRot = Mth.cos(t * 0.09F + i * 1.3F) * 0.25F;
			}
		}
	}

	/** Echo Warden: sculk-grown warden with glowing ribcage and antler tendrils. Texture 128x128. */
	public static class EchoWardenModel extends EntityModel<RealmRenderState> {
		private final ModelPart body, head, tendrilR, tendrilL, rightArm, leftArm, rightLeg, leftLeg;

		public EchoWardenModel(ModelPart root) {
			super(root);
			this.body = root.getChild("body");
			this.head = this.body.getChild("head");
			this.tendrilR = this.head.getChild("tendril_r");
			this.tendrilL = this.head.getChild("tendril_l");
			this.rightArm = this.body.getChild("right_arm");
			this.leftArm = this.body.getChild("left_arm");
			this.rightLeg = root.getChild("right_leg");
			this.leftLeg = root.getChild("left_leg");
		}

		public static LayerDefinition createLayer() {
			MeshDefinition mesh = new MeshDefinition();
			PartDefinition root = mesh.getRoot();
			PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-9, -21, -5, 18, 21, 11), PartPose.offset(0, 11, 0));
			PartDefinition head = body.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 32).addBox(-8, -16, -5, 16, 16, 10), PartPose.offset(0, -21, 0));
			head.addOrReplaceChild("tendril_r", CubeListBuilder.create().texOffs(60, 40).addBox(-16, -13, 0, 16, 16, 0.01F), PartPose.offset(-8, -12, 0));
			head.addOrReplaceChild("tendril_l", CubeListBuilder.create().texOffs(60, 56).addBox(0, -13, 0, 16, 16, 0.01F), PartPose.offset(8, -12, 0));
			body.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(58, 0).addBox(-4, 0, -4, 8, 28, 8), PartPose.offset(-13, -18, 1));
			body.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(90, 0).addBox(-4, 0, -4, 8, 28, 8), PartPose.offset(13, -18, 1));
			root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(0, 60).addBox(-3, 0, -3, 6, 13, 6), PartPose.offset(-5.9F, 11, 0));
			root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(24, 60).addBox(-3, 0, -3, 6, 13, 6), PartPose.offset(5.9F, 11, 0));
			return LayerDefinition.create(mesh, 128, 128);
		}

		@Override
		public void setupAnim(RealmRenderState s) {
			super.setupAnim(s);
			float p = s.walkAnimationPos * 0.6662F, sp = Math.min(s.walkAnimationSpeed, 1.0F), t = s.ageInTicks;
			this.rightLeg.xRot = Mth.cos(p) * 1.0F * sp;
			this.leftLeg.xRot = Mth.cos(p + Mth.PI) * 1.0F * sp;
			this.rightArm.xRot = Mth.cos(p + Mth.PI) * 0.8F * sp;
			this.leftArm.xRot = Mth.cos(p) * 0.8F * sp;
			this.head.yRot = s.yRot * Mth.DEG_TO_RAD;
			this.head.xRot = s.xRot * Mth.DEG_TO_RAD;
			float wiggle = Mth.sin(t * 0.35F) * 0.12F;
			this.tendrilR.yRot = 0.3F + wiggle;
			this.tendrilL.yRot = -0.3F - wiggle;
			this.body.zRot = Mth.sin(p * 0.5F) * 0.06F * sp;
			if (s.attackAnim > 0) {
				float a = Mth.sin(s.attackAnim * Mth.PI);
				this.rightArm.xRot = -2.0F + a * 1.6F;
				this.leftArm.xRot = -2.0F + a * 1.6F;
			}
			switch (s.attack) {
				case EchoWarden.SONIC -> {
					this.head.xRot = -0.2F;
					this.body.xRot = Math.min(s.attackTicks / 34.0F, 1.0F) * -0.3F;
					this.tendrilR.yRot = 0.9F;
					this.tendrilL.yRot = -0.9F;
				}
				case EchoWarden.SHRIEK, EchoWarden.TENDRILS -> {
					this.rightArm.zRot = 0.9F;
					this.leftArm.zRot = -0.9F;
					this.head.xRot = -0.5F;
				}
				case EchoWarden.LEAP, EchoWarden.RIPPLE -> {
					this.rightArm.xRot = -2.6F;
					this.leftArm.xRot = -2.6F;
				}
				default -> {
				}
			}
			if (s.roaring) this.head.xRot = -0.6F;
		}
	}

	private CreatureModels() {
	}
}
