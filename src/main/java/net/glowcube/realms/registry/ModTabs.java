package net.glowcube.realms.registry;

import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.glowcube.realms.GlowcubeRealms;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class ModTabs {
	public static final CreativeModeTab MAIN = Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, GlowcubeRealms.id("main"),
			FabricCreativeModeTab.builder()
					.title(Component.translatable("itemGroup.glowcube_realms.main"))
					.icon(() -> new ItemStack(ModItems.RADIANT_BLADE))
					.displayItems((params, output) -> {
						for (Item item : ModItems.ALL) output.accept(item);
						for (Item item : ModBlocks.BLOCK_ITEMS) output.accept(item);
					})
					.build());

	public static void init() {
	}

	private ModTabs() {
	}
}
