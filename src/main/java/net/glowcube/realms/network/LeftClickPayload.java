package net.glowcube.realms.network;

import net.glowcube.realms.GlowcubeRealms;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client -> server: the player left-clicked into the air while holding a left-click ability item. */
public record LeftClickPayload() implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<LeftClickPayload> TYPE = new CustomPacketPayload.Type<>(GlowcubeRealms.id("left_click"));
	public static final StreamCodec<RegistryFriendlyByteBuf, LeftClickPayload> CODEC = StreamCodec.unit(new LeftClickPayload());

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
