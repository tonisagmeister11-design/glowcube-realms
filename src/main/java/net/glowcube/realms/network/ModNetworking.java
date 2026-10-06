package net.glowcube.realms.network;

import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.glowcube.realms.world.RealmData;
import net.minecraft.server.level.ServerPlayer;

public final class ModNetworking {
	public static void init() {
		PayloadTypeRegistry.clientboundPlay().register(ArenaMarkersPayload.TYPE, ArenaMarkersPayload.CODEC);
		PayloadTypeRegistry.serverboundPlay().register(LeftClickPayload.TYPE, LeftClickPayload.CODEC);
	}

	public static void sendMarkers(ServerPlayer player) {
		RealmData data = RealmData.get();
		if (data == null || !ServerPlayNetworking.canSend(player, ArenaMarkersPayload.TYPE)) return;
		List<ArenaMarkersPayload.Marker> markers = new ArrayList<>();
		String dim = player.level().dimension().identifier().toString();
		for (RealmData.Arena a : data.arenasNear(dim, player.blockPosition(), 3000)) {
			markers.add(new ArenaMarkersPayload.Marker(a.boss(), a.pos().getX(), a.pos().getY(), a.pos().getZ(), a.defeated()));
		}
		ServerPlayNetworking.send(player, new ArenaMarkersPayload(markers));
	}

	private ModNetworking() {
	}
}
