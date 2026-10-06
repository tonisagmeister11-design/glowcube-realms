package net.glowcube.realms.registry;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.glowcube.realms.GlowcubeRealms;
import net.glowcube.realms.entity.CrystalGolem;
import net.glowcube.realms.entity.GlowWisp;
import net.glowcube.realms.entity.RealmGuardian;
import net.glowcube.realms.entity.ShadeCrawler;
import net.glowcube.realms.entity.boss.EmberWarden;
import net.glowcube.realms.entity.boss.Glowkeeper;
import net.glowcube.realms.entity.boss.UmbralTyrant;
import net.glowcube.realms.entity.projectile.GlowShardProjectile;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public final class ModEntities {
	public static final EntityType<GlowWisp> GLOW_WISP = register("glow_wisp",
			EntityType.Builder.of(GlowWisp::new, MobCategory.CREATURE).sized(0.6F, 0.6F).clientTrackingRange(8));
	public static final EntityType<CrystalGolem> CRYSTAL_GOLEM = register("crystal_golem",
			EntityType.Builder.of(CrystalGolem::new, MobCategory.MONSTER).sized(1.3F, 2.4F).clientTrackingRange(10));
	public static final EntityType<ShadeCrawler> SHADE_CRAWLER = register("shade_crawler",
			EntityType.Builder.of(ShadeCrawler::new, MobCategory.MONSTER).sized(1.3F, 0.8F).clientTrackingRange(8));
	public static final EntityType<RealmGuardian> REALM_GUARDIAN = register("realm_guardian",
			EntityType.Builder.of(RealmGuardian::new, MobCategory.MISC).sized(0.6F, 1.95F).clientTrackingRange(10));

	public static final EntityType<Glowkeeper> GLOWKEEPER = register("glowkeeper",
			EntityType.Builder.of(Glowkeeper::new, MobCategory.MONSTER).sized(1.8F, 4.2F).fireImmune().clientTrackingRange(12));
	public static final EntityType<UmbralTyrant> UMBRAL_TYRANT = register("umbral_tyrant",
			EntityType.Builder.of(UmbralTyrant::new, MobCategory.MONSTER).sized(2.6F, 3.4F).clientTrackingRange(12));
	public static final EntityType<EmberWarden> EMBER_WARDEN = register("ember_warden",
			EntityType.Builder.of(EmberWarden::new, MobCategory.MONSTER).sized(1.0F, 3.0F).fireImmune().clientTrackingRange(12));

	public static final EntityType<net.glowcube.realms.entity.boss.InfernalColossus> INFERNAL_COLOSSUS = register("infernal_colossus",
			EntityType.Builder.of(net.glowcube.realms.entity.boss.InfernalColossus::new, MobCategory.MONSTER).sized(2.4F, 4.4F).fireImmune().clientTrackingRange(12));
	public static final EntityType<net.glowcube.realms.entity.boss.VoidHerald> VOID_HERALD = register("void_herald",
			EntityType.Builder.of(net.glowcube.realms.entity.boss.VoidHerald::new, MobCategory.MONSTER).sized(1.6F, 3.8F).clientTrackingRange(12));
	public static final EntityType<net.glowcube.realms.entity.boss.FrostLich> FROST_LICH = register("frost_lich",
			EntityType.Builder.of(net.glowcube.realms.entity.boss.FrostLich::new, MobCategory.MONSTER).sized(1.0F, 2.9F).clientTrackingRange(12));
	public static final EntityType<net.glowcube.realms.entity.boss.TempestDrake> TEMPEST_DRAKE = register("tempest_drake",
			EntityType.Builder.of(net.glowcube.realms.entity.boss.TempestDrake::new, MobCategory.MONSTER).sized(3.0F, 1.8F).clientTrackingRange(12));
	public static final EntityType<net.glowcube.realms.entity.boss.HollowKing> HOLLOW_KING = register("hollow_king",
			EntityType.Builder.of(net.glowcube.realms.entity.boss.HollowKing::new, MobCategory.MONSTER).sized(1.0F, 3.0F).clientTrackingRange(12));

	public static final EntityType<GlowShardProjectile> GLOW_SHARD = register("glow_shard",
			EntityType.Builder.<GlowShardProjectile>of(GlowShardProjectile::new, MobCategory.MISC).sized(0.3F, 0.3F).clientTrackingRange(6).updateInterval(5));

	private static <T extends Entity> EntityType<T> register(String name, EntityType.Builder<T> builder) {
		ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, GlowcubeRealms.id(name));
		return Registry.register(BuiltInRegistries.ENTITY_TYPE, key, builder.build(key));
	}

	public static void init() {
		FabricDefaultAttributeRegistry.register(GLOW_WISP, GlowWisp.createAttributes());
		FabricDefaultAttributeRegistry.register(CRYSTAL_GOLEM, CrystalGolem.createAttributes());
		FabricDefaultAttributeRegistry.register(SHADE_CRAWLER, ShadeCrawler.createAttributes());
		FabricDefaultAttributeRegistry.register(REALM_GUARDIAN, RealmGuardian.createAttributes());
		FabricDefaultAttributeRegistry.register(GLOWKEEPER, Glowkeeper.createAttributes());
		FabricDefaultAttributeRegistry.register(UMBRAL_TYRANT, UmbralTyrant.createAttributes());
		FabricDefaultAttributeRegistry.register(EMBER_WARDEN, EmberWarden.createAttributes());
		FabricDefaultAttributeRegistry.register(INFERNAL_COLOSSUS, net.glowcube.realms.entity.boss.InfernalColossus.createAttributes());
		FabricDefaultAttributeRegistry.register(VOID_HERALD, net.glowcube.realms.entity.boss.VoidHerald.createAttributes());
		FabricDefaultAttributeRegistry.register(FROST_LICH, net.glowcube.realms.entity.boss.FrostLich.createAttributes());
		FabricDefaultAttributeRegistry.register(TEMPEST_DRAKE, net.glowcube.realms.entity.boss.TempestDrake.createAttributes());
		FabricDefaultAttributeRegistry.register(HOLLOW_KING, net.glowcube.realms.entity.boss.HollowKing.createAttributes());

		net.minecraft.world.entity.SpawnPlacements.register(CRYSTAL_GOLEM, net.minecraft.world.entity.SpawnPlacementTypes.ON_GROUND,
				net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, net.minecraft.world.entity.monster.Monster::checkMonsterSpawnRules);
		net.minecraft.world.entity.SpawnPlacements.register(SHADE_CRAWLER, net.minecraft.world.entity.SpawnPlacementTypes.ON_GROUND,
				net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, net.minecraft.world.entity.monster.Monster::checkAnyLightMonsterSpawnRules);
		net.minecraft.world.entity.SpawnPlacements.register(GLOW_WISP, net.minecraft.world.entity.SpawnPlacementTypes.NO_RESTRICTIONS,
				net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, net.minecraft.world.entity.Mob::checkMobSpawnRules);
	}

	private ModEntities() {
	}
}
