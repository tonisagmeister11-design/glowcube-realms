package net.glowcube.realms.block;

import java.util.Map;
import net.glowcube.realms.world.RealmTeleporter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Portal;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/** A portal block leading between the overworld and one of the Glowcube realms. */
public class RealmPortalBlock extends Block implements Portal {
	public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;
	private static final Map<Direction.Axis, VoxelShape> SHAPES = Shapes.rotateHorizontalAxis(Block.column(4.0, 16.0, 0.0, 16.0));

	private final ResourceKey<Level> realm;

	public RealmPortalBlock(ResourceKey<Level> realm, BlockBehaviour.Properties properties) {
		super(properties);
		this.realm = realm;
		this.registerDefaultState(this.stateDefinition.any().setValue(AXIS, Direction.Axis.X));
	}

	public ResourceKey<Level> realm() {
		return this.realm;
	}

	public Block frameBlock() {
		return this.realm == net.glowcube.realms.world.RealmDimensions.LUMEN_SKIES ? Blocks.GLOWSTONE : Blocks.CRYING_OBSIDIAN;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(AXIS);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPES.get(state.getValue(AXIS));
	}

	@Override
	protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction dir,
			BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
		Direction.Axis axis = state.getValue(AXIS);
		boolean inPlane = dir.getAxis() == axis || dir.getAxis() == Direction.Axis.Y;
		if (inPlane && !neighbourState.is(this) && !neighbourState.is(this.frameBlock())) {
			return Blocks.AIR.defaultBlockState();
		}
		return super.updateShape(state, level, ticks, pos, dir, neighbourPos, neighbourState, random);
	}

	@Override
	protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effectApplier, boolean isPrecise) {
		if (entity.canUsePortal(false)) {
			entity.setAsInsidePortal(this, pos);
		}
	}

	@Override
	public int getPortalTransitionTime(ServerLevel level, Entity entity) {
		if (entity instanceof Player player) {
			return player.getAbilities().invulnerable ? 1 : 60;
		}
		return 0;
	}

	@Override
	public Portal.Transition getLocalTransition() {
		return Portal.Transition.CONFUSION;
	}

	@Override
	public @Nullable TeleportTransition getPortalDestination(ServerLevel currentLevel, Entity entity, BlockPos portalEntryPos) {
		return RealmTeleporter.portalDestination(currentLevel, entity, this);
	}

	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		if (random.nextInt(100) == 0) {
			level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SoundEvents.PORTAL_AMBIENT, SoundSource.BLOCKS,
					0.4F, random.nextFloat() * 0.4F + 0.8F, false);
		}
		for (int i = 0; i < 3; i++) {
			double x = pos.getX() + random.nextDouble();
			double y = pos.getY() + random.nextDouble();
			double z = pos.getZ() + random.nextDouble();
			double dx = (random.nextFloat() - 0.5) * 0.4, dy = (random.nextFloat() - 0.5) * 0.4, dz = (random.nextFloat() - 0.5) * 0.4;
			level.addParticle(this.realm == net.glowcube.realms.world.RealmDimensions.LUMEN_SKIES ? ParticleTypes.END_ROD : ParticleTypes.REVERSE_PORTAL,
					x, y, z, dx * 0.2, dy * 0.2, dz * 0.2);
		}
	}
}
