package net.glowcube.realms.client.map;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;

/** Computes a shaded top-down map color for a world column, like a living map item. */
public final class MapColors {
	public static final int UNKNOWN = 0xFF14101C;

	/** Returns ARGB color and also the height in the low bits through {@link #lastHeight}. */
	public static int lastHeight;

	public static int column(Level level, int x, int z, boolean caveMode, int caveY) {
		if (!level.hasChunk(x >> 4, z >> 4)) return 0;
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, 0, z);
		int y;
		if (caveMode) {
			y = -1;
			boolean airAbove = false;
			for (int yy = caveY + 2; yy > caveY - 32 && yy > level.getMinY(); yy--) {
				pos.setY(yy);
				BlockState s = level.getBlockState(pos);
				if (s.isAir()) airAbove = true;
				else if (airAbove) {
					y = yy;
					break;
				}
			}
			if (y < 0 && !airAbove) {
				lastHeight = caveY;
				return 0xFF0A0710;
			}
			if (y < 0) {
				lastHeight = caveY - 32;
				return 0xFF050308;
			}
		} else {
			y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) - 1;
			if (y <= level.getMinY()) {
				lastHeight = level.getMinY();
				return 0xFF0B0816;
			}
		}
		pos.setY(y);
		BlockState state = level.getBlockState(pos);
		int waterDepth = 0;
		while (!state.getFluidState().isEmpty() && waterDepth < 12 && pos.getY() > level.getMinY()) {
			pos.move(0, -1, 0);
			state = level.getBlockState(pos);
			waterDepth++;
		}
		MapColor mc = state.getMapColor(level, pos);
		int rgb = mc == MapColor.NONE ? 0x1A1426 : mc.col;
		if (waterDepth > 0) {
			int water = MapColor.WATER.col;
			float t = Math.min(0.85F, 0.45F + waterDepth * 0.05F);
			rgb = mix(rgb, water, t);
		}
		lastHeight = y;
		return 0xFF000000 | rgb;
	}

	public static int shade(int argb, int height, int northHeight) {
		float f = height > northHeight ? 1.12F : height < northHeight ? 0.82F : 1.0F;
		int r = Math.min(255, (int) (((argb >> 16) & 255) * f));
		int g = Math.min(255, (int) (((argb >> 8) & 255) * f));
		int b = Math.min(255, (int) ((argb & 255) * f));
		return (argb & 0xFF000000) | (r << 16) | (g << 8) | b;
	}

	public static int mix(int a, int b, float t) {
		int r = (int) (((a >> 16) & 255) * (1 - t) + ((b >> 16) & 255) * t);
		int g = (int) (((a >> 8) & 255) * (1 - t) + ((b >> 8) & 255) * t);
		int bl = (int) ((a & 255) * (1 - t) + (b & 255) * t);
		return (r << 16) | (g << 8) | bl;
	}

	private MapColors() {
	}
}
