package net.glowcube.realms.network;

import java.util.ArrayList;
import java.util.List;
import net.glowcube.realms.GlowcubeRealms;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Server -> client: boss arenas in the player's dimension, shown on the minimap. */
public record ArenaMarkersPayload(List<Marker> markers) implements CustomPacketPayload {
	public static final CustomPacketPayload.Type<ArenaMarkersPayload> TYPE = new CustomPacketPayload.Type<>(GlowcubeRealms.id("arena_markers"));

	public record Marker(String boss, int x, int y, int z, boolean defeated) {
	}

	public static final StreamCodec<RegistryFriendlyByteBuf, ArenaMarkersPayload> CODEC = StreamCodec.of(
			(buf, payload) -> {
				buf.writeVarInt(payload.markers.size());
				for (Marker m : payload.markers) {
					buf.writeUtf(m.boss());
					buf.writeInt(m.x());
					buf.writeInt(m.y());
					buf.writeInt(m.z());
					buf.writeBoolean(m.defeated());
				}
			},
			buf -> {
				int n = buf.readVarInt();
				List<Marker> list = new ArrayList<>(n);
				for (int i = 0; i < n; i++) list.add(new Marker(buf.readUtf(), buf.readInt(), buf.readInt(), buf.readInt(), buf.readBoolean()));
				return new ArenaMarkersPayload(list);
			});

	@Override
	public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
