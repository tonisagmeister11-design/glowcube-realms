package net.glowcube.realms;

import net.fabricmc.api.ModInitializer;
import net.glowcube.realms.event.RealmEvents;
import net.glowcube.realms.network.ModNetworking;
import net.glowcube.realms.registry.ModBlockEntities;
import net.glowcube.realms.registry.ModBlocks;
import net.glowcube.realms.registry.ModEntities;
import net.glowcube.realms.registry.ModItems;
import net.glowcube.realms.registry.ModTabs;
import net.glowcube.realms.registry.ModWorldgen;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GlowcubeRealms implements ModInitializer {
	public static final String MOD_ID = "glowcube_realms";
	public static final Logger LOGGER = LoggerFactory.getLogger("Glowcube's Realms");

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	@Override
	public void onInitialize() {
		ModEntities.init();
		ModBlocks.init();
		ModItems.init();
		ModBlockEntities.init();
		ModWorldgen.init();
		ModTabs.init();
		ModNetworking.init();
		RealmEvents.init();
		net.glowcube.realms.event.GearEvents.init();
		net.glowcube.realms.event.DevSelfTest.init();
		LOGGER.info("Glowcube's Realms geladen - viel Spass in den Realms!");
	}
}
