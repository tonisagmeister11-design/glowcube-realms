package net.glowcube.realms.registry;

import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.glowcube.realms.GlowcubeRealms;
import net.glowcube.realms.block.entity.AltarBlockEntity;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;

public final class ModBlockEntities {
	public static final BlockEntityType<AltarBlockEntity> ALTAR = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, GlowcubeRealms.id("altar"),
			FabricBlockEntityTypeBuilder.create(AltarBlockEntity::new, ModBlocks.GLOWKEEPER_ALTAR, ModBlocks.TYRANT_ALTAR, ModBlocks.WARDEN_ALTAR,
					ModBlocks.COLOSSUS_ALTAR, ModBlocks.HERALD_ALTAR, ModBlocks.LICH_ALTAR, ModBlocks.DRAKE_ALTAR, ModBlocks.KING_ALTAR).build());

	public static void init() {
	}

	private ModBlockEntities() {
	}
}
