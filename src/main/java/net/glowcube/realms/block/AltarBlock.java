package net.glowcube.realms.block;

import com.mojang.serialization.MapCodec;
import java.util.function.Supplier;
import net.glowcube.realms.block.entity.AltarBlockEntity;
import net.glowcube.realms.registry.ModBlockEntities;
import net.glowcube.realms.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import org.jspecify.annotations.Nullable;

/** Boss altar. While {@link #AWAKE} is true it waits for a challenger and summons its boss. */
public class AltarBlock extends BaseEntityBlock {
	public static final BooleanProperty AWAKE = BlockStateProperties.ENABLED;

	public enum Boss {
		GLOWKEEPER("glowkeeper", () -> ModEntities.GLOWKEEPER),
		UMBRAL_TYRANT("umbral_tyrant", () -> ModEntities.UMBRAL_TYRANT),
		EMBER_WARDEN("ember_warden", () -> ModEntities.EMBER_WARDEN),
		INFERNAL_COLOSSUS("infernal_colossus", () -> ModEntities.INFERNAL_COLOSSUS),
		VOID_HERALD("void_herald", () -> ModEntities.VOID_HERALD),
		FROST_LICH("frost_lich", () -> ModEntities.FROST_LICH),
		TEMPEST_DRAKE("tempest_drake", () -> ModEntities.TEMPEST_DRAKE),
		HOLLOW_KING("hollow_king", () -> ModEntities.HOLLOW_KING);

		public final String id;
		private final Supplier<EntityType<? extends Mob>> type;

		Boss(String id, Supplier<EntityType<? extends Mob>> type) {
			this.id = id;
			this.type = type;
		}

		public EntityType<? extends Mob> entityType() {
			return this.type.get();
		}

		public static Boss byId(String id) {
			for (Boss b : values()) if (b.id.equals(id)) return b;
			return GLOWKEEPER;
		}
	}

	private final Boss boss;

	public AltarBlock(Boss boss, BlockBehaviour.Properties properties) {
		super(properties);
		this.boss = boss;
		this.registerDefaultState(this.stateDefinition.any().setValue(AWAKE, true));
	}

	public Boss boss() {
		return this.boss;
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(AWAKE);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new AltarBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return level.isClientSide() ? null : createTickerHelper(type, ModBlockEntities.ALTAR, AltarBlockEntity::serverTick);
	}

	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		if (!state.getValue(AWAKE)) return;
		for (int i = 0; i < 2; i++) {
			double a = random.nextDouble() * Math.PI * 2;
			level.addParticle(switch (this.boss) {
						case EMBER_WARDEN, INFERNAL_COLOSSUS -> ParticleTypes.FLAME;
						case GLOWKEEPER, TEMPEST_DRAKE -> ParticleTypes.END_ROD;
						case FROST_LICH -> ParticleTypes.SNOWFLAKE;
						case HOLLOW_KING -> ParticleTypes.SOUL;
						default -> ParticleTypes.REVERSE_PORTAL;
					},
					pos.getX() + 0.5 + Math.cos(a) * 0.7, pos.getY() + 1.1, pos.getZ() + 0.5 + Math.sin(a) * 0.7, 0.0, 0.06, 0.0);
		}
	}
}
