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
 * Big procedural buildings. Vanilla replacements: "pyramid" (desert), "jungle", "igloo", "witch" (swamp hut), "fortress" (pillager outpost).
 * Realm places: "sky_market", "sky_ruin" (Lumen Skies), "shadow_bazaar", "umbral_mine" (Umbral Depths), "echo_camp", "echo_ruin" (Sculk Realm).
 * "fixed_y" puts the building at a fixed height (sky islands, caves), otherwise it sits on the surface.
 */
public class GrandStructure extends Structure {
	public static final MapCodec<GrandStructure> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			settingsCodec(i),
			Codec.STRING.fieldOf("kind").forGetter(s -> s.kind),
			Codec.INT.optionalFieldOf("fixed_y").forGetter(s -> s.fixedY)
	).apply(i, GrandStructure::new));

	private final String kind;
	private final Optional<Integer> fixedY;

	public GrandStructure(Structure.StructureSettings settings, String kind, Optional<Integer> fixedY) {
		super(settings);
		this.kind = kind;
		this.fixedY = fixedY;
	}

	@Override
	public Optional<Structure.GenerationStub> findGenerationPoint(Structure.GenerationContext context) {
		ChunkPos chunk = context.chunkPos();
		int x = chunk.getMiddleBlockX(), z = chunk.getMiddleBlockZ();
		int y;
		if (this.fixedY.isPresent()) {
			y = this.fixedY.get();
		} else {
			int spread = this.kind.equals("pyramid") ? 26 : this.kind.equals("jungle") ? 20 : 6;
			int sum = 0, min = Integer.MAX_VALUE;
			int[][] samples = {{0, 0}, {spread, spread}, {-spread, spread}, {spread, -spread}, {-spread, -spread}};
			for (int[] s : samples) {
				int h = context.chunkGenerator().getFirstOccupiedHeight(x + s[0], z + s[1], Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(), context.randomState());
				sum += h;
				min = Math.min(min, h);
			}
			// big buildings sit a little into the ground so no side floats
			y = this.kind.equals("witch") ? sum / samples.length : (sum / samples.length + min) / 2;
			int floor = this.kind.startsWith("echo") ? context.heightAccessor().getMinY() + 20 : context.chunkGenerator().getSeaLevel() - 1;
			if (y < floor || y < context.heightAccessor().getMinY() + 20) return Optional.empty();
		}
		BlockPos center = new BlockPos(x, y, z);
		return Optional.of(new Structure.GenerationStub(center, builder -> builder.addPiece(new GrandPiece(this.kind, center))));
	}

	@Override
	public StructureType<?> type() {
		return ModWorldgen.GRAND;
	}
}
