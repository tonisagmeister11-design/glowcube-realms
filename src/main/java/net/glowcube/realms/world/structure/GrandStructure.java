package net.glowcube.realms.world.structure;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import net.glowcube.realms.registry.ModWorldgen;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

/**
 * Bigger replacements for the vanilla temples: "pyramid" (desert), "jungle", "igloo", "witch" (swamp hut) and "fortress" (pillager outpost).
 * The structures keep their vanilla ids, biomes and placement; only the building changes.
 */
public class GrandStructure extends Structure {
	public static final MapCodec<GrandStructure> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			settingsCodec(i),
			Codec.STRING.fieldOf("kind").forGetter(s -> s.kind)
	).apply(i, GrandStructure::new));

	private final String kind;

	public GrandStructure(Structure.StructureSettings settings, String kind) {
		super(settings);
		this.kind = kind;
	}

	@Override
	public Optional<Structure.GenerationStub> findGenerationPoint(Structure.GenerationContext context) {
		ChunkPos chunk = context.chunkPos();
		int x = chunk.getMiddleBlockX(), z = chunk.getMiddleBlockZ();
		int spread = this.kind.equals("pyramid") ? 18 : this.kind.equals("jungle") ? 14 : 6;
		int sum = 0, min = Integer.MAX_VALUE;
		int[][] samples = {{0, 0}, {spread, spread}, {-spread, spread}, {spread, -spread}, {-spread, -spread}};
		for (int[] s : samples) {
			int h = context.chunkGenerator().getFirstOccupiedHeight(x + s[0], z + s[1], Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(), context.randomState());
			sum += h;
			min = Math.min(min, h);
		}
		// big buildings sit a little into the ground so no side floats
		int y = this.kind.equals("witch") ? sum / samples.length : (sum / samples.length + min) / 2;
		if (y < context.chunkGenerator().getSeaLevel() - 1 || y < context.heightAccessor().getMinY() + 20) return Optional.empty();
		BlockPos center = new BlockPos(x, y, z);
		return Optional.of(new Structure.GenerationStub(center, builder -> builder.addPiece(new GrandPiece(this.kind, center))));
	}

	@Override
	public StructureType<?> type() {
		return ModWorldgen.GRAND;
	}
}
