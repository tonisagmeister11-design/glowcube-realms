package net.glowcube.realms.entity;

import net.glowcube.realms.registry.ModBlocks;
import net.glowcube.realms.registry.ModItems;
import net.glowcube.realms.world.RealmDimensions;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.LookAtTradingPlayerGoal;
import net.minecraft.world.entity.ai.goal.MoveTowardsRestrictionGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.TradeWithPlayerGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTrader;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * Merchant living in the realm markets. Stays at its stall (never despawns, never drinks potions) and trades
 * realm materials for useful gear. Variant 0 = Lumen Skies, 1 = Umbral Depths, 2 = Sculk Realm.
 */
public class RealmTrader extends WanderingTrader {
	private static final EntityDataAccessor<Integer> DATA_VARIANT = SynchedEntityData.defineId(RealmTrader.class, EntityDataSerializers.INT);

	public RealmTrader(EntityType<? extends RealmTrader> type, Level level) {
		super(type, level);
		this.setDespawnDelay(0);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(DATA_VARIANT, 0);
	}

	public int getVariant() {
		return this.entityData.get(DATA_VARIANT);
	}

	public void setVariant(int v) {
		this.entityData.set(DATA_VARIANT, v);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
		this.goalSelector.addGoal(1, new TradeWithPlayerGoal(this));
		this.goalSelector.addGoal(1, new LookAtTradingPlayerGoal(this));
		this.goalSelector.addGoal(4, new MoveTowardsRestrictionGoal(this, 0.5));
		this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.35) {
			@Override
			public boolean canUse() {
				return RealmTrader.this.random.nextInt(3) == 0 && super.canUse();
			}
		});
		this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
		this.goalSelector.addGoal(9, new RandomLookAroundGoal(this));
	}

	@Override
	public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason, @Nullable SpawnGroupData data) {
		var dim = level.getLevel().dimension();
		this.setVariant(dim == RealmDimensions.UMBRAL_DEPTHS ? 1 : dim == RealmDimensions.SCULK_REALM ? 2 : 0);
		this.setHomeTo(this.blockPosition(), 6);
		this.setPersistenceRequired();
		return super.finalizeSpawn(level, difficulty, reason, data);
	}

	@Override
	public void aiStep() {
		this.setDespawnDelay(0);
		super.aiStep();
	}

	@Override
	public boolean removeWhenFarAway(double distanceToClosestPlayer) {
		return false;
	}

	@Override
	protected void updateTrades(ServerLevel level) {
		MerchantOffers offers = this.getOffers();
		switch (this.getVariant()) {
			case 1 -> {
				buy(offers, ModItems.VOID_SHARD, 6, 1);
				buy(offers, ModItems.SHADE_FANG, 5, 1);
				buy(offers, ModItems.TOAD_LEG, 8, 1);
				sell(offers, 10, ModItems.UMBRAL_KEY, 1);
				sell(offers, 6, ModItems.UMBRAL_COMPASS, 1);
				sell(offers, 3, ModItems.SHADECAP_STEW, 1);
				sell(offers, 18, ModItems.SHADOW_DAGGER, 1);
				sell(offers, 14, ModItems.VOID_PEARL, 2);
				sell(offers, 22, ModItems.VOIDSHARD_PICKAXE, 1);
				sell(offers, 4, Items.LANTERN, 4);
				swap(offers, ModItems.VOID_SHARD, 16, ModItems.TYRANT_SIGIL, 1);
			}
			case 2 -> {
				buy(offers, ModItems.ECHO_CRYSTAL, 4, 1);
				buy(offers, ModItems.SCULK_SLIME, 6, 1);
				buy(offers, ModItems.SNAIL_SHELL, 3, 1);
				sell(offers, 12, ModItems.SCULK_KEY, 1);
				sell(offers, 6, Items.ECHO_SHARD, 2);
				sell(offers, 8, Items.SCULK_CATALYST, 1);
				sell(offers, 16, Items.RECOVERY_COMPASS, 1);
				sell(offers, 20, Items.SILENCE_ARMOR_TRIM_SMITHING_TEMPLATE, 1);
				sell(offers, 5, ModItems.GLOW_JELLY, 3);
				swap(offers, ModItems.ECHO_CRYSTAL, 24, ModItems.ECHO_HORN, 1);
			}
			default -> {
				buy(offers, ModItems.GLOW_SHARD, 8, 1);
				buy(offers, ModItems.CLOUD_FLUFF, 6, 1);
				buy(offers, ModItems.WISP_ESSENCE, 4, 1);
				sell(offers, 10, ModItems.LUMEN_KEY, 1);
				sell(offers, 6, ModItems.LUMEN_COMPASS, 1);
				sell(offers, 2, ModItems.LUMEN_BERRIES, 6);
				sell(offers, 12, ModItems.CLOUD_BOTTLE, 1);
				sell(offers, 16, ModItems.GRAPPLING_HOOK, 1);
				sell(offers, 20, ModItems.GALE_FAN, 1);
				sell(offers, 6, ModBlocks.CLOUD_VENT.asItem(), 2);
				swap(offers, ModItems.GLOW_SHARD, 20, ModItems.STARMETAL_INGOT, 2);
			}
		}
	}

	/** The trader buys realm materials for emeralds. */
	private static void buy(MerchantOffers offers, Item item, int count, int emeralds) {
		offers.add(new MerchantOffer(new ItemCost(item, count), new ItemStack(Items.EMERALD, emeralds), 16, 2, 0.05F));
	}

	private static void sell(MerchantOffers offers, int emeralds, Item item, int count) {
		offers.add(new MerchantOffer(new ItemCost(Items.EMERALD, emeralds), new ItemStack(item, count), 8, 5, 0.05F));
	}

	private static void swap(MerchantOffers offers, Item cost, int count, Item result, int resultCount) {
		offers.add(new MerchantOffer(new ItemCost(cost, count), new ItemStack(result, resultCount), 4, 10, 0.05F));
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putInt("RealmVariant", this.getVariant());
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		this.setVariant(input.getIntOr("RealmVariant", 0));
	}
}
