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
 * Procedurally built boss arenas and shrines.
 * "kind" is one of glowkeeper, umbral_tyrant, ember_warden, shrine.
 * "fixed_y" places the structure at a fixed height (used in the realms), otherwise it sits on the surface.
 */
public class ArenaStructure extends Structure {
	public static final MapCodec<ArenaStructure> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			settingsCodec(i),
			Codec.STRING.fieldOf("kind").forGetter(s -> s.kind),
			Codec.INT.optionalFieldOf("fixed_y").forGetter(s -> s.fixedY)
	).apply(i, ArenaStructure::new));

	private final String kind;
	private final Optional<Integer> fixedY;

	public ArenaStructure(Structure.StructureSettings settings, String kind, Optional<Integer> fixedY) {
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
			y = context.chunkGenerator().getFirstOccupiedHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, context.heightAccessor(), context.randomState());
			if (y < context.chunkGenerator().getSeaLevel() || y < context.heightAccessor().getMinY() + 8) return Optional.empty();
		}
		BlockPos center = new BlockPos(x, y, z);
		return Optional.of(new Structure.GenerationStub(center, builder -> builder.addPiece(new ArenaPiece(this.kind, center))));
	}

	@Override
	public StructureType<?> type() {
		return ModWorldgen.ARENA;
	}
}
