package net.glowcube.realms.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** Updraft vent of the Lumen Skies: living entities above it (up to {@link #RANGE} blocks) are carried upwards. */
public class CloudVentBlock extends Block {
	public static final int RANGE = 24;

	public CloudVentBlock(Properties properties) {
		super(properties);
	}

	/** Height above a vent if the entity is in its open air column, otherwise -1. */
	public static int ventHeight(Level level, Entity entity) {
		BlockPos p = entity.blockPosition();
		for (int d = 0; d <= RANGE; d++) {
			BlockState s = level.getBlockState(p.below(d));
			if (s.getBlock() instanceof CloudVentBlock) return d;
			if (!s.getCollisionShape(level, p.below(d)).isEmpty()) return -1;
		}
		return -1;
	}

	/** Called every tick for entities in the air column of a vent: on the client for the own player, on the server for everything else. */
	public static void lift(Entity entity, int height) {
		Vec3 v = entity.getDeltaMovement();
		double strength = height < 4 ? 0.9 : 0.55;
		entity.setDeltaMovement(v.x * 0.9, Math.min(1.1, Math.max(v.y, 0.0) + strength * 0.25 + 0.12), v.z * 0.9);
		entity.resetFallDistance();
	}

	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		for (int i = 0; i < 3; i++) {
			level.addParticle(ParticleTypes.CLOUD, pos.getX() + random.nextDouble(), pos.getY() + 1.05, pos.getZ() + random.nextDouble(),
					0.0, 0.35 + random.nextDouble() * 0.3, 0.0);
		}
		if (random.nextInt(3) == 0) level.addParticle(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 0.0, 0.5, 0.0);
	}
}
