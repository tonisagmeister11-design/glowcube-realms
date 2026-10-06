package net.glowcube.realms.world;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.glowcube.realms.block.RealmPortalBlock;

/** Finds an enclosed vertical rectangle-ish area inside a frame and fills it with portal blocks. */
public final class RealmPortalShape {
	private static final int MAX_BLOCKS = 21 * 21;

	public static boolean tryLight(Level level, BlockPos start, Block frame, Block portal) {
		if (!level.getBlockState(start).isAir()) return false;
		for (Direction.Axis axis : new Direction.Axis[]{Direction.Axis.X, Direction.Axis.Z}) {
			Set<BlockPos> inside = flood(level, start, axis, frame);
			if (inside != null && inside.size() >= 2) {
				BlockState state = portal.defaultBlockState().setValue(RealmPortalBlock.AXIS, axis);
				for (BlockPos p : inside) level.setBlock(p, state, 18);
				return true;
			}
		}
		return false;
	}

	private static Set<BlockPos> flood(Level level, BlockPos start, Direction.Axis axis, Block frame) {
		Direction a = Direction.fromAxisAndDirection(axis, Direction.AxisDirection.POSITIVE);
		Direction[] dirs = {a, a.getOpposite(), Direction.UP, Direction.DOWN};
		Set<BlockPos> seen = new HashSet<>();
		ArrayDeque<BlockPos> queue = new ArrayDeque<>();
		queue.add(start);
		seen.add(start);
		int minY = start.getY(), maxY = start.getY();
		while (!queue.isEmpty()) {
			BlockPos p = queue.poll();
			for (Direction d : dirs) {
				BlockPos n = p.relative(d);
				if (seen.contains(n)) continue;
				BlockState s = level.getBlockState(n);
				if (s.is(frame)) continue;
				if (!s.isAir()) return null;
				seen.add(n);
				if (seen.size() > MAX_BLOCKS) return null;
				minY = Math.min(minY, n.getY());
				maxY = Math.max(maxY, n.getY());
				if (maxY - minY > 21) return null;
				queue.add(n);
			}
		}
		return seen;
	}

	private RealmPortalShape() {
	}
}
