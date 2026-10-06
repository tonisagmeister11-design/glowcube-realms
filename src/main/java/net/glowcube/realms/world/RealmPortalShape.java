package net.glowcube.realms.world;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.glowcube.realms.block.RealmPortalBlock;

/** Finds an enclosed vertical area inside a frame and fills it with portal blocks. */
public final class RealmPortalShape {
	private static final int MAX_BLOCKS = 21 * 21;

	public static boolean tryLight(Level level, BlockPos start, Block frame, Block portal) {
		return tryLight(level, start, s -> s.is(frame), portal, MAX_BLOCKS, 21);
	}

	/** Generic version: any block matching {@code frame} closes the shape. */
	public static boolean tryLight(Level level, BlockPos start, Predicate<BlockState> frame, Block portal, int maxBlocks, int maxSize) {
		if (!isOpen(level.getBlockState(start))) return false;
		for (Direction.Axis axis : new Direction.Axis[]{Direction.Axis.X, Direction.Axis.Z}) {
			Set<BlockPos> inside = flood(level, start, axis, frame, maxBlocks, maxSize);
			if (inside != null && inside.size() >= 2) {
				BlockState state = portal.defaultBlockState().setValue(RealmPortalBlock.AXIS, axis);
				for (BlockPos p : inside) level.setBlock(p, state, 18);
				return true;
			}
		}
		return false;
	}

	private static boolean isOpen(BlockState s) {
		return s.isAir() || (s.canBeReplaced() && s.getFluidState().isEmpty());
	}

	private static Set<BlockPos> flood(Level level, BlockPos start, Direction.Axis axis, Predicate<BlockState> frame, int maxBlocks, int maxSize) {
		Direction a = Direction.fromAxisAndDirection(axis, Direction.AxisDirection.POSITIVE);
		Direction[] dirs = {a, a.getOpposite(), Direction.UP, Direction.DOWN};
		Set<BlockPos> seen = new HashSet<>();
		ArrayDeque<BlockPos> queue = new ArrayDeque<>();
		queue.add(start);
		seen.add(start);
		int minY = start.getY(), maxY = start.getY(), minH = 0, maxH = 0;
		while (!queue.isEmpty()) {
			BlockPos p = queue.poll();
			for (Direction d : dirs) {
				BlockPos n = p.relative(d);
				if (seen.contains(n)) continue;
				BlockState s = level.getBlockState(n);
				if (!isOpen(s)) {
					if (frame.test(s)) continue;
					return null;
				}
				seen.add(n);
				if (seen.size() > maxBlocks) return null;
				minY = Math.min(minY, n.getY());
				maxY = Math.max(maxY, n.getY());
				int h = axis == Direction.Axis.X ? n.getX() - start.getX() : n.getZ() - start.getZ();
				minH = Math.min(minH, h);
				maxH = Math.max(maxH, h);
				if (maxY - minY > maxSize || maxH - minH > maxSize) return null;
				queue.add(n);
			}
		}
		return seen;
	}

	private RealmPortalShape() {
	}
}
