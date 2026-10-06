package net.glowcube.realms.world.feature;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;

/** Tall, slightly leaning crystal spire growing out of the ground (or hanging from a ceiling). */
public record CrystalSpikeFeature(BlockState block, boolean hanging) implements Feature {
	public static final MapCodec<CrystalSpikeFeature> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			BlockState.CODEC.fieldOf("block").forGetter(CrystalSpikeFeature::block),
			com.mojang.serialization.Codec.BOOL.optionalFieldOf("hanging", false).forGetter(CrystalSpikeFeature::hanging)
	).apply(i, CrystalSpikeFeature::new));

	@Override
	public MapCodec<CrystalSpikeFeature> codec() {
		return CODEC;
	}

	@Override
	public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
		int dir = this.hanging ? -1 : 1;
		BlockPos base = origin;
		if (!this.hanging) {
			while (base.getY() > level.getMinY() + 2 && level.isEmptyBlock(base.below())) base = base.below();
			if (!level.getBlockState(base.below()).isSolid()) return false;
		} else {
			if (!level.getBlockState(base.above()).isSolid()) return false;
		}
		int height = 5 + random.nextInt(9);
		double radius = 1.4 + random.nextDouble() * 1.4;
		double leanX = (random.nextDouble() - 0.5) * 0.5, leanZ = (random.nextDouble() - 0.5) * 0.5;
		for (int h = -1; h < height; h++) {
			double t = Math.max(0, h) / (double) height;
			double r = radius * (1.0 - t);
			int cx = (int) Math.round(leanX * h), cz = (int) Math.round(leanZ * h);
			int ri = (int) Math.ceil(r);
			for (int dx = -ri; dx <= ri; dx++) for (int dz = -ri; dz <= ri; dz++) {
				if (dx * dx + dz * dz > r * r + 0.3) continue;
				BlockPos p = base.offset(cx + dx, h * dir, cz + dz);
				BlockState cur = level.getBlockState(p);
				if (cur.isAir() || cur.canBeReplaced() || h < 0) level.setBlock(p, this.block, 2);
			}
		}
		return true;
	}
}
