package net.glowcube.realms.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.glowcube.realms.entity.CrystalGolem;
import net.glowcube.realms.entity.boss.RealmBoss;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Mob;
import org.jspecify.annotations.Nullable;

/** Generic renderer for all Glowcube creatures: custom model, optional scale, full-bright and emissive glow layer. */
public class RealmMobRenderer<T extends Mob, M extends EntityModel<RealmRenderState>> extends MobRenderer<T, RealmRenderState, M> {
	private final Identifier texture;
	private final float scale;
	private final boolean fullBright;

	public RealmMobRenderer(EntityRendererProvider.Context context, M model, float shadow, Identifier texture, float scale, boolean fullBright,
			@Nullable Identifier glowTexture) {
		super(context, model, shadow * scale);
		this.texture = texture;
		this.scale = scale;
		this.fullBright = fullBright;
		if (glowTexture != null) {
			RenderType glow = RenderTypes.eyes(glowTexture);
			this.addLayer(new EyesLayer<>(this) {
				@Override
				public RenderType renderType() {
					return glow;
				}
			});
		}
	}

	@Override
	public RealmRenderState createRenderState() {
		return new RealmRenderState();
	}

	@Override
	public void extractRenderState(T entity, RealmRenderState state, float partialTicks) {
		super.extractRenderState(entity, state, partialTicks);
		state.aggressive = entity.isAggressive();
		state.attackAnim = entity.getSwingAnimation(partialTicks);
		if (entity instanceof RealmBoss boss) {
			state.phase = boss.getPhase();
			state.attack = boss.getCurrentAttack();
			state.attackTicks = boss.clientAttackTicks + partialTicks;
			state.roaring = state.attack == RealmBoss.ROAR;
		} else if (entity instanceof CrystalGolem golem) {
			state.attackAnim = golem.attackAnim > 0 ? golem.attackAnim - partialTicks : 0;
		}
	}

	@Override
	protected void scale(RealmRenderState state, PoseStack poseStack) {
		poseStack.scale(this.scale, this.scale, this.scale);
	}

	@Override
	protected int getBlockLightLevel(T entity, BlockPos pos) {
		return this.fullBright ? 15 : super.getBlockLightLevel(entity, pos);
	}

	@Override
	public Identifier getTextureLocation(RealmRenderState state) {
		return this.texture;
	}
}
