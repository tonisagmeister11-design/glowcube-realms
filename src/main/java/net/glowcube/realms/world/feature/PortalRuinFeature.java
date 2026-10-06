package net.glowcube.realms.world.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.glowcube.realms.block.RealmPortalBlock;
import net.glowcube.realms.registry.ModBlocks;
import net.glowcube.realms.world.RealmData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;

/**
 * A natural, already active realm portal: an "Umbral Rift" deep in overworld caves,
 * or a "Lumen Gate" ruin on the surface. One more way into the realms.
 */
public record PortalRuinFeature(String realm) implements Feature {
	public static final MapCodec<PortalRuinFeature> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			Codec.STRING.fieldOf("realm").forGetter(PortalRuinFeature::realm)
	).apply(i, PortalRuinFeature::new));

	@Override
	public MapCodec<PortalRuinFeature> codec() {
		return CODEC;
	}

	@Override
	public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
		boolean umbral = this.realm.equals("umbral");
		BlockPos base = origin;
		if (umbral) {
			// find a cave floor with headroom
			for (int i = 0; i < 24 && !(level.getBlockState(base.below()).isSolid() && level.isEmptyBlock(base)); i++) base = base.below();
			if (!level.getBlockState(base.below()).isSolid() || !level.isEmptyBlock(base)) return false;
			for (int dy = 0; dy < 5; dy++) for (int dx = 0; dx < 4; dx++) {
				if (!level.isEmptyBlock(base.offset(dx, dy, 0)) && dy > 0) return false;
			}
		}
		Block frame = umbral ? Blocks.CRYING_OBSIDIAN : Blocks.GLOWSTONE;
		Block portal = umbral ? ModBlocks.UMBRAL_PORTAL : ModBlocks.LUMEN_PORTAL;
		BlockState portalState = portal.defaultBlockState().setValue(RealmPortalBlock.AXIS, Direction.Axis.X);
		for (int dx = 0; dx < 4; dx++) for (int dy = 0; dy < 5; dy++) {
			boolean edge = dx == 0 || dx == 3 || dy == 0 || dy == 4;
			BlockPos p = base.offset(dx, dy, 0);
			level.setBlock(p, edge ? frame.defaultBlockState() : portalState, 2);
		}
		// decoration around the gate
		BlockState deco = umbral ? ModBlocks.UMBRAL_STONE.defaultBlockState() : ModBlocks.SKYSTONE_BRICKS.defaultBlockState();
		for (int dx = -2; dx <= 5; dx++) for (int dz = -2; dz <= 2; dz++) {
			BlockPos p = base.offset(dx, -1, dz);
			if (random.nextFloat() < 0.75F) level.setBlock(p, deco, 2);
		}
		if (umbral) {
			for (int i = 0; i < 6; i++) {
				BlockPos p = base.offset(random.nextInt(8) - 2, 0, random.nextInt(5) - 2);
				if (level.isEmptyBlock(p) && !(level.getBlockState(p).getBlock() instanceof RealmPortalBlock)) level.setBlock(p, ModBlocks.VOIDBLOOM.defaultBlockState(), 2);
			}
		}
		BlockPos inside = base.offset(1, 1, 0);
		String dim = level.getLevel().dimension().identifier().toString();
		level.getLevel().getServer().execute(() -> {
			RealmData data = RealmData.get();
			if (data != null) data.addPortal(dim, inside);
		});
		return true;
	}
}
