package net.glowcube.realms.client.mixin;

import net.glowcube.realms.GlowcubeRealms;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.LogoRenderer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Replaces the Minecraft title logo with the Glowcube's Realms logo. */
@Mixin(LogoRenderer.class)
public abstract class LogoRendererMixin {
	private static final Identifier GLOWCUBE_LOGO = GlowcubeRealms.id("textures/gui/title/glowcube_logo.png");

	@Inject(method = "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IFI)V", at = @At("HEAD"), cancellable = true)
	private void glowcube$drawLogo(GuiGraphicsExtractor graphics, int width, float alpha, int heightOffset, CallbackInfo ci) {
		int w = 300, h = 75;
		int color = ARGB.white(((LogoRenderer) (Object) this).keepLogoThroughFade() ? 1.0F : alpha);
		graphics.blit(RenderPipelines.GUI_TEXTURED, GLOWCUBE_LOGO, width / 2 - w / 2, heightOffset - 18, 0.0F, 0.0F, w, h, 1024, 256, 1024, 256, color);
		ci.cancel();
	}
}
