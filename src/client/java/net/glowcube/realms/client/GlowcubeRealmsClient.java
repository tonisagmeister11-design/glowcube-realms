package net.glowcube.realms.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.glowcube.realms.GlowcubeRealms;
import net.glowcube.realms.client.map.MapCache;
import net.glowcube.realms.client.map.MinimapHud;
import net.glowcube.realms.client.map.Waypoints;
import net.glowcube.realms.client.map.WorldMapScreen;
import net.glowcube.realms.client.model.CreatureModels;
import net.glowcube.realms.client.model.CrystalGolemModel;
import net.glowcube.realms.client.model.EmberWardenModel;
import net.glowcube.realms.client.model.GlowWispModel;
import net.glowcube.realms.client.model.GlowkeeperModel;
import net.glowcube.realms.client.model.ShadeCrawlerModel;
import net.glowcube.realms.client.model.UmbralTyrantModel;
import net.glowcube.realms.client.render.RealmGuardianRenderer;
import net.glowcube.realms.client.render.RealmMobRenderer;
import net.glowcube.realms.network.ArenaMarkersPayload;
import net.glowcube.realms.registry.ModEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public class GlowcubeRealmsClient implements ClientModInitializer {
	public static final ModelLayerLocation GLOW_WISP = layer("glow_wisp");
	public static final ModelLayerLocation CRYSTAL_GOLEM = layer("crystal_golem");
	public static final ModelLayerLocation SHADE_CRAWLER = layer("shade_crawler");
	public static final ModelLayerLocation GLOWKEEPER = layer("glowkeeper");
	public static final ModelLayerLocation UMBRAL_TYRANT = layer("umbral_tyrant");
	public static final ModelLayerLocation EMBER_WARDEN = layer("ember_warden");
	public static final ModelLayerLocation WYVERN = layer("tempest_drake");
	public static final ModelLayerLocation ECHO_WARDEN_L = layer("echo_warden");
	public static final ModelLayerLocation DEER_L = layer("glimmer_deer");
	public static final ModelLayerLocation BUNNY_L = layer("cloud_bunny");
	public static final ModelLayerLocation TOAD_L = layer("shade_toad");
	public static final ModelLayerLocation SNAIL_L = layer("sculk_snail");
	public static final ModelLayerLocation LIZARD_L = layer("ember_salamander");
	public static final ModelLayerLocation JELLY_L = layer("void_jelly");

	private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(GlowcubeRealms.id("main"));
	private static KeyMapping toggleMap, worldMap, addWaypoint, zoomMap;

	private static ModelLayerLocation layer(String name) {
		return new ModelLayerLocation(GlowcubeRealms.id(name), "main");
	}

	private static Identifier tex(String name) {
		return GlowcubeRealms.id("textures/entity/" + name + ".png");
	}

	@Override
	public void onInitializeClient() {
		ModelLayerRegistry.registerModelLayer(GLOW_WISP, GlowWispModel::createLayer);
		ModelLayerRegistry.registerModelLayer(CRYSTAL_GOLEM, CrystalGolemModel::createLayer);
		ModelLayerRegistry.registerModelLayer(SHADE_CRAWLER, ShadeCrawlerModel::createLayer);
		ModelLayerRegistry.registerModelLayer(GLOWKEEPER, GlowkeeperModel::createLayer);
		ModelLayerRegistry.registerModelLayer(UMBRAL_TYRANT, UmbralTyrantModel::createLayer);
		ModelLayerRegistry.registerModelLayer(EMBER_WARDEN, EmberWardenModel::createLayer);
		ModelLayerRegistry.registerModelLayer(WYVERN, net.glowcube.realms.client.model.WyvernModel::createLayer);
		ModelLayerRegistry.registerModelLayer(ECHO_WARDEN_L, CreatureModels.EchoWardenModel::createLayer);
		ModelLayerRegistry.registerModelLayer(DEER_L, CreatureModels.Deer::createLayer);
		ModelLayerRegistry.registerModelLayer(BUNNY_L, CreatureModels.Bunny::createLayer);
		ModelLayerRegistry.registerModelLayer(TOAD_L, CreatureModels.Toad::createLayer);
		ModelLayerRegistry.registerModelLayer(SNAIL_L, CreatureModels.Snail::createLayer);
		ModelLayerRegistry.registerModelLayer(LIZARD_L, CreatureModels.Lizard::createLayer);
		ModelLayerRegistry.registerModelLayer(JELLY_L, CreatureModels.Jelly::createLayer);
		ModelLayerRegistry.registerModelLayer(net.glowcube.realms.client.render.SculkStalkerRenderer.LAYER,
				() -> LayerDefinition.create(HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F), 64, 64));
		ModelLayerRegistry.registerModelLayer(RealmGuardianRenderer.LAYER,
				net.minecraft.client.model.monster.illager.IllagerModel::createBodyLayer);

		EntityRendererRegistry.register(ModEntities.GLOW_WISP, ctx -> new RealmMobRenderer<>(ctx, new GlowWispModel(ctx.bakeLayer(GLOW_WISP)), 0.3F,
				tex("glow_wisp"), 1.0F, true, tex("glow_wisp_glow")));
		EntityRendererRegistry.register(ModEntities.CRYSTAL_GOLEM, ctx -> new RealmMobRenderer<>(ctx, new CrystalGolemModel(ctx.bakeLayer(CRYSTAL_GOLEM)), 0.8F,
				tex("crystal_golem"), 1.15F, false, tex("crystal_golem_glow")));
		EntityRendererRegistry.register(ModEntities.SHADE_CRAWLER, ctx -> new RealmMobRenderer<>(ctx, new ShadeCrawlerModel(ctx.bakeLayer(SHADE_CRAWLER)), 0.7F,
				tex("shade_crawler"), 1.0F, false, tex("shade_crawler_glow")));
		EntityRendererRegistry.register(ModEntities.GLOWKEEPER, ctx -> new RealmMobRenderer<>(ctx, new GlowkeeperModel(ctx.bakeLayer(GLOWKEEPER)), 1.2F,
				tex("glowkeeper"), 1.3F, true, tex("glowkeeper_glow")));
		EntityRendererRegistry.register(ModEntities.UMBRAL_TYRANT, ctx -> new RealmMobRenderer<>(ctx, new UmbralTyrantModel(ctx.bakeLayer(UMBRAL_TYRANT)), 1.6F,
				tex("umbral_tyrant"), 1.3F, false, tex("umbral_tyrant_glow")));
		EntityRendererRegistry.register(ModEntities.EMBER_WARDEN, ctx -> new RealmMobRenderer<>(ctx, new EmberWardenModel(ctx.bakeLayer(EMBER_WARDEN)), 0.8F,
				tex("ember_warden"), 1.3F, true, tex("ember_warden_glow")));
		EntityRendererRegistry.register(ModEntities.INFERNAL_COLOSSUS, ctx -> new RealmMobRenderer<>(ctx, new CrystalGolemModel(ctx.bakeLayer(CRYSTAL_GOLEM)), 1.2F,
				tex("infernal_colossus"), 1.8F, false, tex("infernal_colossus_glow")));
		EntityRendererRegistry.register(ModEntities.VOID_HERALD, ctx -> new RealmMobRenderer<>(ctx, new GlowkeeperModel(ctx.bakeLayer(GLOWKEEPER)), 1.0F,
				tex("void_herald"), 1.15F, false, tex("void_herald_glow")));
		EntityRendererRegistry.register(ModEntities.FROST_LICH, ctx -> new RealmMobRenderer<>(ctx, new EmberWardenModel(ctx.bakeLayer(EMBER_WARDEN)), 0.8F,
				tex("frost_lich"), 1.2F, false, tex("frost_lich_glow")));
		EntityRendererRegistry.register(ModEntities.HOLLOW_KING, ctx -> new RealmMobRenderer<>(ctx, new EmberWardenModel(ctx.bakeLayer(EMBER_WARDEN)), 0.8F,
				tex("hollow_king"), 1.25F, false, tex("hollow_king_glow")));
		EntityRendererRegistry.register(ModEntities.TEMPEST_DRAKE, ctx -> new RealmMobRenderer<>(ctx, new net.glowcube.realms.client.model.WyvernModel(ctx.bakeLayer(WYVERN)), 1.4F,
				tex("tempest_drake"), 1.5F, false, tex("tempest_drake_glow")));
		EntityRendererRegistry.register(ModEntities.REALM_GUARDIAN, RealmGuardianRenderer::new);
		EntityRendererRegistry.register(ModEntities.REALM_TRADER, net.glowcube.realms.client.render.RealmTraderRenderer::new);
		EntityRendererRegistry.register(ModEntities.SCULK_STALKER, net.glowcube.realms.client.render.SculkStalkerRenderer::new);
		EntityRendererRegistry.register(ModEntities.ECHO_WARDEN, ctx -> new RealmMobRenderer<>(ctx, new CreatureModels.EchoWardenModel(ctx.bakeLayer(ECHO_WARDEN_L)), 1.1F,
				tex("echo_warden"), 1.15F, false, tex("echo_warden_glow")));
		EntityRendererRegistry.register(ModEntities.GLIMMER_DEER, ctx -> new RealmMobRenderer<>(ctx, new CreatureModels.Deer(ctx.bakeLayer(DEER_L)), 0.5F,
				tex("glimmer_deer"), 1.0F, false, tex("glimmer_deer_glow")));
		EntityRendererRegistry.register(ModEntities.CLOUD_BUNNY, ctx -> new RealmMobRenderer<>(ctx, new CreatureModels.Bunny(ctx.bakeLayer(BUNNY_L)), 0.3F,
				tex("cloud_bunny"), 1.0F, false, null));
		EntityRendererRegistry.register(ModEntities.SHADE_TOAD, ctx -> new RealmMobRenderer<>(ctx, new CreatureModels.Toad(ctx.bakeLayer(TOAD_L)), 0.4F,
				tex("shade_toad"), 1.0F, false, tex("shade_toad_glow")));
		EntityRendererRegistry.register(ModEntities.SCULK_SNAIL, ctx -> new RealmMobRenderer<>(ctx, new CreatureModels.Snail(ctx.bakeLayer(SNAIL_L)), 0.4F,
				tex("sculk_snail"), 1.0F, false, tex("sculk_snail_glow")));
		EntityRendererRegistry.register(ModEntities.EMBER_SALAMANDER, ctx -> new RealmMobRenderer<>(ctx, new CreatureModels.Lizard(ctx.bakeLayer(LIZARD_L)), 0.4F,
				tex("ember_salamander"), 1.0F, true, tex("ember_salamander_glow")));
		EntityRendererRegistry.register(ModEntities.VOID_JELLY, ctx -> new RealmMobRenderer<>(ctx, new CreatureModels.Jelly(ctx.bakeLayer(JELLY_L)), 0.4F,
				tex("void_jelly"), 1.0F, true, tex("void_jelly_glow")));
		EntityRendererRegistry.register(ModEntities.LANTERN_BUG, ctx -> new RealmMobRenderer<>(ctx, new GlowWispModel(ctx.bakeLayer(GLOW_WISP)), 0.2F,
				tex("lantern_bug"), 0.7F, true, tex("lantern_bug_glow")));
		EntityRendererRegistry.register(ModEntities.GLOW_SHARD, ctx -> new ThrownItemRenderer<>(ctx, 1.25F, true));

		HudElementRegistry.addLast(GlowcubeRealms.id("minimap"), new MinimapHud());

		toggleMap = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.glowcube_realms.toggle_minimap", InputConstants.Type.KEYBOARD, InputConstants.KEY_N, CATEGORY));
		worldMap = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.glowcube_realms.world_map", InputConstants.Type.KEYBOARD, InputConstants.KEY_M, CATEGORY));
		addWaypoint = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.glowcube_realms.add_waypoint", InputConstants.Type.KEYBOARD, InputConstants.KEY_B, CATEGORY));
		zoomMap = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.glowcube_realms.zoom_minimap", InputConstants.Type.KEYBOARD, InputConstants.KEY_EQUALS, CATEGORY));

		ClientTickEvents.END_CLIENT_TICK.register(GlowcubeRealmsClient::tick);
		net.fabricmc.fabric.api.event.client.player.ClientPreAttackCallback.EVENT.register((client, player, clicks) -> {
			if (player.getMainHandItem().getItem() instanceof net.glowcube.realms.item.LeftClickAbility
					&& (client.hitResult == null || client.hitResult.getType() == net.minecraft.world.phys.HitResult.Type.MISS)
					&& !player.getCooldowns().isOnCooldown(player.getMainHandItem())) {
				ClientPlayNetworking.send(new net.glowcube.realms.network.LeftClickPayload());
			}
			return false;
		});
		ClientPlayNetworking.registerGlobalReceiver(ArenaMarkersPayload.TYPE, (payload, context) -> Waypoints.arenas = payload.markers());
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			Waypoints.arenas = java.util.List.of();
			MapCache.clear();
		});
	}

	private static void tick(Minecraft mc) {
		if (mc.player == null) return;
		MapCache.tick(mc);
		// cloud vents: the own player is moved on the client, so the updraft feels smooth
		int vent = net.glowcube.realms.block.CloudVentBlock.ventHeight(mc.player.level(), mc.player);
		if (vent >= 0 && !mc.player.getAbilities().flying && !mc.player.isShiftKeyDown()) net.glowcube.realms.block.CloudVentBlock.lift(mc.player, vent);
		while (toggleMap.consumeClick()) MinimapHud.enabled = !MinimapHud.enabled;
		while (zoomMap.consumeClick()) MinimapHud.cycleZoom();
		while (worldMap.consumeClick()) mc.gui.setScreen(new WorldMapScreen());
		while (addWaypoint.consumeClick()) {
			Waypoints.Waypoint w = Waypoints.add(mc, mc.player.getBlockX(), mc.player.getBlockY(), mc.player.getBlockZ());
			mc.player.sendOverlayMessage(Component.translatable("message.glowcube_realms.waypoint_added", w.name).withStyle(ChatFormatting.AQUA));
		}
	}
}
