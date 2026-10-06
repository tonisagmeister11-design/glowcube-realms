package net.glowcube.realms.world;

import net.glowcube.realms.block.RealmPortalBlock;
import net.glowcube.realms.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public final class RealmTeleporter {
	public static @Nullable TeleportTransition portalDestination(ServerLevel current, Entity entity, RealmPortalBlock portal) {
		ResourceKey<Level> targetKey = current.dimension() == portal.realm() ? Level.OVERWORLD : portal.realm();
		ServerLevel target = current.getServer().getLevel(targetKey);
		if (target == null) return null;
		BlockPos approx = target.getWorldBorder().clampToBounds(entity.getX(), entity.getY(), entity.getZ());
		RealmData data = RealmData.get();
		String dimId = targetKey.identifier().toString();
		BlockPos exit = data == null ? null : data.findPortal(dimId, approx, 128);
		if (exit != null) {
			target.getChunk(exit);
			if (!(target.getBlockState(exit).getBlock() instanceof RealmPortalBlock)) {
				data.removePortal(dimId, exit);
				exit = null;
			}
		}
		if (exit == null) {
			if (entity.isSpectator()) return null;
			Block portalBlock = portal;
			exit = buildPortal(target, approx, portalBlock, portal.frameBlock());
			if (data != null) data.addPortal(dimId, exit);
		}
		Vec3 dest = new Vec3(exit.getX() + 0.5, exit.getY(), exit.getZ() + 0.5);
		return new TeleportTransition(target, dest, Vec3.ZERO, entity.getYRot(), entity.getXRot(),
				TeleportTransition.PLAY_PORTAL_SOUND.then(TeleportTransition.PLACE_PORTAL_TICKET));
	}

	/** Teleport without portal (compass, sky/void travel). Finds a safe spot near x/z. */
	public static void sendTo(Entity entity, ServerLevel target, double x, double z, @Nullable Double fixedY, boolean slowFall) {
		BlockPos spot;
		if (fixedY != null) spot = BlockPos.containing(x, fixedY, z);
		else spot = safeSpot(target, BlockPos.containing(x, 64, z));
		Vec3 dest = new Vec3(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5);
		entity.teleport(new TeleportTransition(target, dest, Vec3.ZERO, entity.getYRot(), entity.getXRot(), TeleportTransition.PLAY_PORTAL_SOUND));
		if (slowFall && entity instanceof LivingEntity living) {
			living.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 400, 0, false, true));
		}
	}

	/** Returns a position where an entity can stand. Builds a small platform if none is found. */
	public static BlockPos safeSpot(ServerLevel level, BlockPos near) {
		level.getChunk(near);
		boolean ceiling = level.dimensionType().hasCeiling();
		int x = near.getX(), z = near.getZ();
		if (!ceiling) {
			int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
			if (y > level.getMinY() + 2) return new BlockPos(x, y, z);
			BlockPos platform = new BlockPos(x, 110, z);
			buildPlatform(level, platform.below(), ModBlocks.SKYSTONE_BRICKS.defaultBlockState(), 2);
			return platform;
		}
		// caves: search the column (and a few neighbours) for a dry floor with headroom, preferring the middle heights
		int[] order = {40, 32, 48, 24, 56, 64, 16, 72, 80, 8, 88, 96};
		for (int ox = 0; ox <= 24; ox += 6) for (int oz = 0; oz <= 24; oz += 6) {
			for (int start : order) {
				for (int y = start; y < start + 8; y++) {
					BlockPos p = new BlockPos(x + ox, y, z + oz);
					if (isDryStandingSpot(level, p)) return p;
				}
			}
		}
		BlockPos room = new BlockPos(x, 50, z);
		carveRoom(level, room, 3);
		buildPlatform(level, room.below(), ModBlocks.UMBRAL_BRICKS.defaultBlockState(), 2);
		return room;
	}

	private static boolean isDryStandingSpot(ServerLevel level, BlockPos p) {
		BlockState below = level.getBlockState(p.below());
		return below.isSolid() && below.getFluidState().isEmpty() && level.getBlockState(p).isAir() && level.getBlockState(p.above()).isAir()
				&& level.getBlockState(p.above(2)).isAir() && level.getFluidState(p.below(2)).isEmpty();
	}

	/** Builds a 4x5 portal frame and returns the lower interior portal block position. */
	public static BlockPos buildPortal(ServerLevel level, BlockPos near, Block portal, Block frame) {
		BlockPos base = safeSpot(level, near);
		Direction.Axis axis = Direction.Axis.X;
		BlockState frameState = frame.defaultBlockState();
		BlockState floor = level.dimension() == RealmDimensions.LUMEN_SKIES ? ModBlocks.SKYSTONE_BRICKS.defaultBlockState()
				: level.dimension() == RealmDimensions.UMBRAL_DEPTHS ? ModBlocks.UMBRAL_BRICKS.defaultBlockState()
				: Blocks.STONE_BRICKS.defaultBlockState();
		// clear space
		for (int dx = -2; dx <= 5; dx++) for (int dy = 0; dy <= 5; dy++) for (int dz = -2; dz <= 2; dz++) {
			level.setBlock(base.offset(dx - 1, dy, dz), Blocks.AIR.defaultBlockState(), 3);
		}
		buildPlatform(level, base.offset(1, -1, 0), floor, 3);
		for (int dx = 0; dx < 4; dx++) for (int dy = 0; dy < 5; dy++) {
			boolean edge = dx == 0 || dx == 3 || dy == 0 || dy == 4;
			BlockPos p = base.offset(dx - 1, dy, 0);
			if (edge) level.setBlock(p, frameState, 3);
		}
		BlockState portalState = portal.defaultBlockState().setValue(RealmPortalBlock.AXIS, axis);
		for (int dx = 1; dx <= 2; dx++) for (int dy = 1; dy <= 3; dy++) {
			level.setBlock(base.offset(dx - 1, dy, 0), portalState, 18);
		}
		return base.offset(0, 1, 0);
	}

	private static void buildPlatform(ServerLevel level, BlockPos center, BlockState state, int radius) {
		for (int dx = -radius; dx <= radius; dx++) for (int dz = -radius; dz <= radius; dz++) {
			BlockPos p = center.offset(dx, 0, dz);
			if (!level.getBlockState(p).isSolid()) level.setBlock(p, state, 3);
		}
	}

	private static void carveRoom(ServerLevel level, BlockPos center, int radius) {
		for (int dx = -radius; dx <= radius; dx++) for (int dy = 0; dy <= 4; dy++) for (int dz = -radius; dz <= radius; dz++) {
			level.setBlock(center.offset(dx, dy, dz), Blocks.AIR.defaultBlockState(), 3);
		}
	}

	private RealmTeleporter() {
	}
}
