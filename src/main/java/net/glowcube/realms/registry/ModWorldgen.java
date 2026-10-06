package net.glowcube.realms.registry;

import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.glowcube.realms.GlowcubeRealms;
import net.glowcube.realms.world.feature.CrystalSpikeFeature;
import net.glowcube.realms.world.feature.PortalRuinFeature;
import net.glowcube.realms.world.structure.ArenaPiece;
import net.glowcube.realms.world.structure.ArenaStructure;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;

public final class ModWorldgen {
	public static final StructureType<ArenaStructure> ARENA = Registry.register(BuiltInRegistries.STRUCTURE_TYPE, GlowcubeRealms.id("arena"),
			() -> ArenaStructure.CODEC);
	public static final StructurePieceType ARENA_PIECE = Registry.register(BuiltInRegistries.STRUCTURE_PIECE, GlowcubeRealms.id("arena_piece"),
			(StructurePieceType.ContextlessType) ArenaPiece::new);

	public static void init() {
		Registry.register(BuiltInRegistries.FEATURE_TYPE, GlowcubeRealms.id("crystal_spike"), CrystalSpikeFeature.CODEC);
		Registry.register(BuiltInRegistries.FEATURE_TYPE, GlowcubeRealms.id("portal_ruin"), PortalRuinFeature.CODEC);

		// overworld: giant trees in forests, natural realm gates
		BiomeModifications.addFeature(BiomeSelectors.tag(BiomeTags.IS_FOREST), GenerationStep.Decoration.VEGETAL_DECORATION, placed("giant_oak"));
		BiomeModifications.addFeature(BiomeSelectors.tag(BiomeTags.IS_TAIGA), GenerationStep.Decoration.VEGETAL_DECORATION, placed("giant_spruce"));
		BiomeModifications.addFeature(BiomeSelectors.foundInOverworld(), GenerationStep.Decoration.UNDERGROUND_DECORATION, placed("umbral_rift"));
		BiomeModifications.addFeature(BiomeSelectors.foundInOverworld(), GenerationStep.Decoration.SURFACE_STRUCTURES, placed("lumen_gate"));
	}

	private static ResourceKey<PlacedFeature> placed(String name) {
		return ResourceKey.create(Registries.PLACED_FEATURE, GlowcubeRealms.id(name));
	}

	private ModWorldgen() {
	}
}
