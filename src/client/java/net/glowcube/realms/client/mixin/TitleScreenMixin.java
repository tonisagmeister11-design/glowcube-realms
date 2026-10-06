package net.glowcube.realms.client.mixin;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Adds the Glowcube credit line to the title screen. */
@Mixin(TitleScreen.class)
public abstract class TitleScreenMixin extends Screen {
	protected TitleScreenMixin(Component title) {
		super(title);
	}

	@Inject(method = "extractRenderState", at = @At("TAIL"))
	private void glowcube$credits(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a, CallbackInfo ci) {
		float pulse = 0.65F + 0.35F * (float) Math.sin(System.currentTimeMillis() / 400.0);
		int alpha = (int) (pulse * 255) << 24;
		graphics.text(this.font, Component.translatable("title.glowcube_realms.credit"), 2, this.height - 20, alpha | 0x5CDFFF, true);
	}
}
