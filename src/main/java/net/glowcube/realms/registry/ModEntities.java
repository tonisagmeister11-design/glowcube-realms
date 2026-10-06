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
import net.glowcube.realms.entity.animal.RealmAnimal;
import net.glowcube.realms.entity.animal.RealmFlyer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;

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

	// ------------------------------------------------------------ update 3: Sculk Realm + creatures
	public static final EntityType<net.glowcube.realms.entity.boss.EchoWarden> ECHO_WARDEN = register("echo_warden",
			EntityType.Builder.of(net.glowcube.realms.entity.boss.EchoWarden::new, MobCategory.MONSTER).sized(1.0F, 3.0F).clientTrackingRange(12));
	public static final EntityType<net.glowcube.realms.entity.SculkStalker> SCULK_STALKER = register("sculk_stalker",
			EntityType.Builder.of(net.glowcube.realms.entity.SculkStalker::new, MobCategory.MONSTER).sized(0.6F, 1.95F).clientTrackingRange(8));

	private static RealmAnimal.Kind kind(double hp, double speed, boolean fly, net.minecraft.core.particles.ParticleOptions particle,
			net.minecraft.sounds.SoundEvent ambient, net.minecraft.sounds.SoundEvent hurt, net.minecraft.sounds.SoundEvent death, float pitch,
			boolean fireImmune, net.minecraft.world.effect.MobEffectInstance hitEffect) {
		return new RealmAnimal.Kind(hp, speed, fly, particle, ambient, hurt, death, pitch, fireImmune, hitEffect);
	}

	public static final RealmAnimal.Kind DEER_KIND = kind(14, 0.25, false, ParticleTypes.END_ROD, SoundEvents.GOAT_AMBIENT, SoundEvents.GOAT_HURT,
			SoundEvents.GOAT_DEATH, 1.3F, false, null);
	public static final RealmAnimal.Kind BUNNY_KIND = kind(6, 0.32, false, ParticleTypes.CLOUD, SoundEvents.RABBIT_AMBIENT, SoundEvents.RABBIT_HURT,
			SoundEvents.RABBIT_DEATH, 1.2F, false, null);
	public static final RealmAnimal.Kind TOAD_KIND = kind(10, 0.2, false, null, SoundEvents.FROG_AMBIENT, SoundEvents.FROG_HURT,
			SoundEvents.FROG_DEATH, 0.7F, false, null);
	public static final RealmAnimal.Kind BUG_KIND = kind(4, 0.3, true, ParticleTypes.GLOW, SoundEvents.AMETHYST_BLOCK_CHIME, SoundEvents.BEE_HURT,
			SoundEvents.BEE_DEATH, 1.4F, false, null);
	public static final RealmAnimal.Kind SNAIL_KIND = kind(12, 0.12, false, ParticleTypes.SCULK_CHARGE_POP, SoundEvents.SCULK_CLICKING,
			SoundEvents.SLIME_HURT_SMALL, SoundEvents.SLIME_DEATH_SMALL, 0.8F, false, null);
	public static final RealmAnimal.Kind SALAMANDER_KIND = kind(12, 0.26, false, ParticleTypes.SMALL_FLAME, SoundEvents.FROG_AMBIENT, SoundEvents.FROG_HURT,
			SoundEvents.FROG_DEATH, 0.8F, true, null);
	public static final RealmAnimal.Kind JELLY_KIND = kind(10, 0.15, true, ParticleTypes.PORTAL, SoundEvents.SQUID_AMBIENT, SoundEvents.SQUID_HURT,
			SoundEvents.SQUID_DEATH, 1.3F, false, new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.LEVITATION, 30, 0));

	public static final EntityType<RealmAnimal> GLIMMER_DEER = animal("glimmer_deer", DEER_KIND, 0.9F, 1.5F);
	public static final EntityType<RealmAnimal> CLOUD_BUNNY = animal("cloud_bunny", BUNNY_KIND, 0.5F, 0.6F);
	public static final EntityType<RealmAnimal> SHADE_TOAD = animal("shade_toad", TOAD_KIND, 0.8F, 0.6F);
	public static final EntityType<RealmAnimal> SCULK_SNAIL = animal("sculk_snail", SNAIL_KIND, 0.8F, 0.8F);
	public static final EntityType<RealmAnimal> EMBER_SALAMANDER = animal("ember_salamander", SALAMANDER_KIND, 0.8F, 0.5F);
	public static final EntityType<RealmFlyer> LANTERN_BUG = flyer("lantern_bug", BUG_KIND, 0.4F, 0.4F);
	public static final EntityType<RealmFlyer> VOID_JELLY = flyer("void_jelly", JELLY_KIND, 0.9F, 1.2F);

	private static EntityType<RealmAnimal> animal(String name, RealmAnimal.Kind kind, float w, float h) {
		EntityType.Builder<RealmAnimal> b = EntityType.Builder.<RealmAnimal>of((type, level) -> new RealmAnimal(type, level, kind), MobCategory.CREATURE)
				.sized(w, h).clientTrackingRange(8);
		if (kind.fireImmune()) b = b.fireImmune();
		return register(name, b);
	}

	private static EntityType<RealmFlyer> flyer(String name, RealmAnimal.Kind kind, float w, float h) {
		return register(name, EntityType.Builder.<RealmFlyer>of((type, level) -> new RealmFlyer(type, level, kind), MobCategory.AMBIENT)
				.sized(w, h).clientTrackingRange(8));
	}

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

		FabricDefaultAttributeRegistry.register(ECHO_WARDEN, net.glowcube.realms.entity.boss.EchoWarden.createAttributes());
		FabricDefaultAttributeRegistry.register(SCULK_STALKER, net.glowcube.realms.entity.SculkStalker.createAttributes());
		for (var e : java.util.List.of(java.util.Map.entry(GLIMMER_DEER, DEER_KIND), java.util.Map.entry(CLOUD_BUNNY, BUNNY_KIND),
				java.util.Map.entry(SHADE_TOAD, TOAD_KIND), java.util.Map.entry(SCULK_SNAIL, SNAIL_KIND), java.util.Map.entry(EMBER_SALAMANDER, SALAMANDER_KIND))) {
			FabricDefaultAttributeRegistry.register(e.getKey(), RealmAnimal.attributes(e.getValue().health(), e.getValue().speed()));
			net.minecraft.world.entity.SpawnPlacements.register(e.getKey(), net.minecraft.world.entity.SpawnPlacementTypes.ON_GROUND,
					net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, net.minecraft.world.entity.Mob::checkMobSpawnRules);
		}
		for (var e : java.util.List.of(java.util.Map.entry(LANTERN_BUG, BUG_KIND), java.util.Map.entry(VOID_JELLY, JELLY_KIND))) {
			FabricDefaultAttributeRegistry.register(e.getKey(), RealmAnimal.attributes(e.getValue().health(), e.getValue().speed()));
			net.minecraft.world.entity.SpawnPlacements.register(e.getKey(), net.minecraft.world.entity.SpawnPlacementTypes.NO_RESTRICTIONS,
					net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, net.minecraft.world.entity.Mob::checkMobSpawnRules);
		}
		net.minecraft.world.entity.SpawnPlacements.register(SCULK_STALKER, net.minecraft.world.entity.SpawnPlacementTypes.ON_GROUND,
				net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, net.minecraft.world.entity.monster.Monster::checkMonsterSpawnRules);
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
