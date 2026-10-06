package net.glowcube.realms.registry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import net.glowcube.realms.GlowcubeRealms;
import net.glowcube.realms.block.AltarBlock;
import net.glowcube.realms.item.AuroraStaffItem;
import net.glowcube.realms.item.CrystalBowItem;
import net.glowcube.realms.item.EmberGreatswordItem;
import net.glowcube.realms.item.LoreItem;
import net.glowcube.realms.item.RadiantBladeItem;
import net.glowcube.realms.item.RealmCompassItem;
import net.glowcube.realms.item.RealmKeyItem;
import net.glowcube.realms.item.ShadowDaggerItem;
import net.glowcube.realms.item.SigilItem;
import net.glowcube.realms.item.StarHammerItem;
import net.glowcube.realms.item.VoidScytheItem;
import net.glowcube.realms.world.RealmDimensions;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.glowcube.realms.item.Gear;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.level.block.Blocks;

public final class ModItems {
	public static final List<Item> ALL = new ArrayList<>();

	public static final TagKey<Item> GLOWCRYSTAL_MATERIALS = TagKey.create(Registries.ITEM, GlowcubeRealms.id("glowcrystal_tool_materials"));
	public static final TagKey<Item> VOIDSHARD_MATERIALS = TagKey.create(Registries.ITEM, GlowcubeRealms.id("voidshard_tool_materials"));

	public static final ToolMaterial GLOWCRYSTAL_TOOL = new ToolMaterial(BlockTags.INCORRECT_FOR_DIAMOND_TOOL, 1800, 9.0F, 3.5F, 18, GLOWCRYSTAL_MATERIALS);
	public static final ToolMaterial VOIDSHARD_TOOL = new ToolMaterial(BlockTags.INCORRECT_FOR_NETHERITE_TOOL, 2400, 10.0F, 4.5F, 16, VOIDSHARD_MATERIALS);
	public static final ToolMaterial LEGENDARY = new ToolMaterial(BlockTags.INCORRECT_FOR_NETHERITE_TOOL, 3200, 10.0F, 5.0F, 22, GLOWCRYSTAL_MATERIALS);

	public static final ResourceKey<EquipmentAsset> GLOWCRYSTAL_ASSET = ResourceKey.create(EquipmentAssets.ROOT_ID, GlowcubeRealms.id("glowcrystal"));
	public static final ResourceKey<EquipmentAsset> VOIDSHARD_ASSET = ResourceKey.create(EquipmentAssets.ROOT_ID, GlowcubeRealms.id("voidshard"));

	public static final ArmorMaterial GLOWCRYSTAL_ARMOR = new ArmorMaterial(35, Map.of(
			ArmorType.BOOTS, 3, ArmorType.LEGGINGS, 6, ArmorType.CHESTPLATE, 8, ArmorType.HELMET, 3, ArmorType.BODY, 11),
			20, SoundEvents.ARMOR_EQUIP_DIAMOND, 2.5F, 0.0F, GLOWCRYSTAL_MATERIALS, GLOWCRYSTAL_ASSET);
	public static final ResourceKey<EquipmentAsset> STARMETAL_ASSET = ResourceKey.create(EquipmentAssets.ROOT_ID, GlowcubeRealms.id("starmetal"));
	public static final TagKey<Item> STARMETAL_MATERIALS = TagKey.create(Registries.ITEM, GlowcubeRealms.id("starmetal_materials"));
	public static final ArmorMaterial STARMETAL_ARMOR = new ArmorMaterial(38, Map.of(
			ArmorType.BOOTS, 3, ArmorType.LEGGINGS, 6, ArmorType.CHESTPLATE, 8, ArmorType.HELMET, 3, ArmorType.BODY, 11),
			22, SoundEvents.ARMOR_EQUIP_IRON, 3.0F, 0.05F, STARMETAL_MATERIALS, STARMETAL_ASSET);
	public static final ArmorMaterial VOIDSHARD_ARMOR =new ArmorMaterial(40, Map.of(
			ArmorType.BOOTS, 3, ArmorType.LEGGINGS, 7, ArmorType.CHESTPLATE, 9, ArmorType.HELMET, 4, ArmorType.BODY, 13),
			16, SoundEvents.ARMOR_EQUIP_NETHERITE, 3.5F, 0.1F, VOIDSHARD_MATERIALS, VOIDSHARD_ASSET);

	// ------------------------------------------------------------ materials
	public static final Item GLOW_SHARD = register("glow_shard", LoreItem::new, new Item.Properties());
	public static final Item VOID_SHARD = register("void_shard", LoreItem::new, new Item.Properties());
	public static final Item STARMETAL_INGOT = register("starmetal_ingot", LoreItem::new, new Item.Properties().rarity(Rarity.UNCOMMON));
	public static final Item WISP_ESSENCE = register("wisp_essence", LoreItem::new, new Item.Properties());
	public static final Item SHADE_FANG = register("shade_fang", LoreItem::new, new Item.Properties());
	public static final Item GOLEM_FRAGMENT = register("golem_fragment", LoreItem::new, new Item.Properties());
	public static final Item GLOWKEEPER_CORE = register("glowkeeper_core", LoreItem::new, new Item.Properties().rarity(Rarity.EPIC).fireResistant());
	public static final Item TYRANT_HEART = register("tyrant_heart", LoreItem::new, new Item.Properties().rarity(Rarity.EPIC).fireResistant());
	public static final Item EMBER_HEART = register("ember_heart", LoreItem::new, new Item.Properties().rarity(Rarity.EPIC).fireResistant());

	// ------------------------------------------------------------ travel
	public static final Item LUMEN_KEY = register("lumen_key", p -> new RealmKeyItem(Blocks.GLOWSTONE, () -> ModBlocks.LUMEN_PORTAL, p),
			new Item.Properties().durability(16).rarity(Rarity.UNCOMMON));
	public static final Item UMBRAL_KEY = register("umbral_key", p -> new RealmKeyItem(Blocks.CRYING_OBSIDIAN, () -> ModBlocks.UMBRAL_PORTAL, p),
			new Item.Properties().durability(16).rarity(Rarity.UNCOMMON));
	public static final Item LUMEN_COMPASS = register("lumen_compass", p -> new RealmCompassItem(RealmDimensions.LUMEN_SKIES, p),
			new Item.Properties().durability(8).rarity(Rarity.RARE));
	public static final Item UMBRAL_COMPASS = register("umbral_compass", p -> new RealmCompassItem(RealmDimensions.UMBRAL_DEPTHS, p),
			new Item.Properties().durability(8).rarity(Rarity.RARE));

	// ------------------------------------------------------------ boss sigils
	public static final Item GLOWKEEPER_SIGIL = register("glowkeeper_sigil", p -> new SigilItem(AltarBlock.Boss.GLOWKEEPER, p),
			new Item.Properties().stacksTo(1).rarity(Rarity.EPIC));
	public static final Item TYRANT_SIGIL = register("tyrant_sigil", p -> new SigilItem(AltarBlock.Boss.UMBRAL_TYRANT, p),
			new Item.Properties().stacksTo(1).rarity(Rarity.EPIC));
	public static final Item WARDEN_SIGIL = register("warden_sigil", p -> new SigilItem(AltarBlock.Boss.EMBER_WARDEN, p),
			new Item.Properties().stacksTo(1).rarity(Rarity.EPIC));

	// ------------------------------------------------------------ tools
	public static final Item GLOWCRYSTAL_SWORD = register("glowcrystal_sword", LoreItem::new, new Item.Properties().sword(GLOWCRYSTAL_TOOL, 3.0F, -2.4F));
	public static final Item GLOWCRYSTAL_PICKAXE = register("glowcrystal_pickaxe", LoreItem::new, new Item.Properties().pickaxe(GLOWCRYSTAL_TOOL, 1.0F, -2.8F));
	public static final Item GLOWCRYSTAL_AXE = register("glowcrystal_axe", LoreItem::new, new Item.Properties().axe(GLOWCRYSTAL_TOOL, 5.0F, -3.0F));
	public static final Item GLOWCRYSTAL_SHOVEL = register("glowcrystal_shovel", LoreItem::new, new Item.Properties().shovel(GLOWCRYSTAL_TOOL, 1.5F, -3.0F));
	public static final Item VOIDSHARD_SWORD = register("voidshard_sword", LoreItem::new, new Item.Properties().sword(VOIDSHARD_TOOL, 3.0F, -2.4F).fireResistant());
	public static final Item VOIDSHARD_PICKAXE = register("voidshard_pickaxe", LoreItem::new, new Item.Properties().pickaxe(VOIDSHARD_TOOL, 1.0F, -2.8F).fireResistant());

	// ------------------------------------------------------------ legendary weapons
	public static final Item RADIANT_BLADE = register("radiant_blade", RadiantBladeItem::new,
			new Item.Properties().sword(LEGENDARY, 4.0F, -2.2F).rarity(Rarity.EPIC).fireResistant());
	public static final Item VOID_SCYTHE = register("void_scythe", VoidScytheItem::new,
			new Item.Properties().sword(LEGENDARY, 6.0F, -2.9F).rarity(Rarity.EPIC).fireResistant());
	public static final Item STAR_HAMMER = register("star_hammer", StarHammerItem::new,
			new Item.Properties().axe(LEGENDARY, 7.0F, -3.3F).rarity(Rarity.EPIC).fireResistant());
	public static final Item SHADOW_DAGGER = register("shadow_dagger", ShadowDaggerItem::new,
			new Item.Properties().sword(VOIDSHARD_TOOL, 0.5F, -1.4F).rarity(Rarity.RARE));
	public static final Item EMBER_GREATSWORD = register("ember_greatsword", EmberGreatswordItem::new,
			new Item.Properties().sword(LEGENDARY, 7.0F, -3.0F).rarity(Rarity.EPIC).fireResistant());
	public static final Item CRYSTAL_BOW = register("crystal_bow", CrystalBowItem::new,
			new Item.Properties().durability(1200).enchantable(18).rarity(Rarity.RARE));
	public static final Item AURORA_STAFF = register("aurora_staff", AuroraStaffItem::new,
			new Item.Properties().durability(900).enchantable(20).rarity(Rarity.RARE));

	// ------------------------------------------------------------ armor
	public static final Item GLOWCRYSTAL_HELMET = register("glowcrystal_helmet", LoreItem::new, new Item.Properties().humanoidArmor(GLOWCRYSTAL_ARMOR, ArmorType.HELMET));
	public static final Item GLOWCRYSTAL_CHESTPLATE = register("glowcrystal_chestplate", LoreItem::new, new Item.Properties().humanoidArmor(GLOWCRYSTAL_ARMOR, ArmorType.CHESTPLATE));
	public static final Item GLOWCRYSTAL_LEGGINGS = register("glowcrystal_leggings", LoreItem::new, new Item.Properties().humanoidArmor(GLOWCRYSTAL_ARMOR, ArmorType.LEGGINGS));
	public static final Item GLOWCRYSTAL_BOOTS = register("glowcrystal_boots", LoreItem::new, new Item.Properties().humanoidArmor(GLOWCRYSTAL_ARMOR, ArmorType.BOOTS));
	public static final Item VOIDSHARD_HELMET = register("voidshard_helmet", LoreItem::new, new Item.Properties().humanoidArmor(VOIDSHARD_ARMOR, ArmorType.HELMET).fireResistant());
	public static final Item VOIDSHARD_CHESTPLATE = register("voidshard_chestplate", LoreItem::new, new Item.Properties().humanoidArmor(VOIDSHARD_ARMOR, ArmorType.CHESTPLATE).fireResistant());
	public static final Item VOIDSHARD_LEGGINGS = register("voidshard_leggings", LoreItem::new, new Item.Properties().humanoidArmor(VOIDSHARD_ARMOR, ArmorType.LEGGINGS).fireResistant());
	public static final Item VOIDSHARD_BOOTS = register("voidshard_boots", LoreItem::new, new Item.Properties().humanoidArmor(VOIDSHARD_ARMOR, ArmorType.BOOTS).fireResistant());

	// ------------------------------------------------------------ update 2: boss drops + sigils
	public static final Item MAGMA_CORE = register("magma_core", LoreItem::new, new Item.Properties().rarity(Rarity.EPIC).fireResistant());
	public static final Item HERALD_EYE = register("herald_eye", LoreItem::new, new Item.Properties().rarity(Rarity.EPIC));
	public static final Item FROST_HEART = register("frost_heart", LoreItem::new, new Item.Properties().rarity(Rarity.EPIC));
	public static final Item STORM_FEATHER = register("storm_feather", LoreItem::new, new Item.Properties().rarity(Rarity.EPIC));
	public static final Item HOLLOW_CROWN = register("hollow_crown", LoreItem::new, new Item.Properties().rarity(Rarity.EPIC));
	public static final Item COLOSSUS_SIGIL = register("colossus_sigil", p -> new SigilItem(AltarBlock.Boss.INFERNAL_COLOSSUS, p), new Item.Properties().stacksTo(1).rarity(Rarity.EPIC));
	public static final Item HERALD_SIGIL = register("herald_sigil", p -> new SigilItem(AltarBlock.Boss.VOID_HERALD, p), new Item.Properties().stacksTo(1).rarity(Rarity.EPIC));
	public static final Item LICH_SIGIL = register("lich_sigil", p -> new SigilItem(AltarBlock.Boss.FROST_LICH, p), new Item.Properties().stacksTo(1).rarity(Rarity.EPIC));
	public static final Item DRAKE_SIGIL = register("drake_sigil", p -> new SigilItem(AltarBlock.Boss.TEMPEST_DRAKE, p), new Item.Properties().stacksTo(1).rarity(Rarity.EPIC));
	public static final Item KING_SIGIL = register("king_sigil", p -> new SigilItem(AltarBlock.Boss.HOLLOW_KING, p), new Item.Properties().stacksTo(1).rarity(Rarity.EPIC));

	// ------------------------------------------------------------ update 2: weapons with abilities
	public static final Item INFERNAL_MAUL = register("infernal_maul", Gear.InfernalMaul::new, new Item.Properties().axe(LEGENDARY, 8.0F, -3.3F).rarity(Rarity.EPIC).fireResistant());
	public static final Item VOID_REAVER = register("void_reaver", Gear.VoidReaver::new, new Item.Properties().sword(LEGENDARY, 5.0F, -2.4F).rarity(Rarity.EPIC));
	public static final Item FROSTBITE_BLADE = register("frostbite_blade", Gear.FrostbiteBlade::new, new Item.Properties().sword(LEGENDARY, 4.5F, -2.4F).rarity(Rarity.EPIC));
	public static final Item THUNDER_SPEAR = register("thunder_spear", Gear.ThunderSpear::new, new Item.Properties().sword(LEGENDARY, 5.0F, -2.7F).rarity(Rarity.EPIC));
	public static final Item BONE_SCEPTER = register("bone_scepter", Gear.BoneScepter::new, new Item.Properties().durability(800).rarity(Rarity.EPIC));
	public static final Item STORM_BOW = register("storm_bow", Gear.StormBow::new, new Item.Properties().durability(1400).enchantable(18).rarity(Rarity.EPIC));
	public static final Item SKY_PIKE = register("sky_pike", Gear.SkyPike::new, new Item.Properties().sword(GLOWCRYSTAL_TOOL, 4.0F, -2.8F).rarity(Rarity.RARE));
	public static final Item METEOR_STAFF = register("meteor_staff", Gear.MeteorStaff::new, new Item.Properties().durability(600).rarity(Rarity.EPIC).fireResistant());

	// ------------------------------------------------------------ update 2: utility gear
	public static final Item GALE_FAN = register("gale_fan", Gear.GaleFan::new, new Item.Properties().durability(500).rarity(Rarity.RARE));
	public static final Item CHRONO_HOURGLASS = register("chrono_hourglass", Gear.ChronoHourglass::new, new Item.Properties().durability(60).rarity(Rarity.EPIC));
	public static final Item AURORA_CHARM = register("aurora_charm", Gear.AuroraCharm::new, new Item.Properties().durability(80).rarity(Rarity.RARE));
	public static final Item GRAPPLING_HOOK = register("grappling_hook", Gear.GrapplingHook::new, new Item.Properties().durability(400).rarity(Rarity.UNCOMMON));
	public static final Item PHOENIX_FEATHER = register("phoenix_feather", Gear.PhoenixFeather::new, new Item.Properties().stacksTo(4).rarity(Rarity.EPIC).fireResistant());
	public static final Item VOID_PEARL = register("void_pearl", Gear.VoidPearl::new, new Item.Properties().durability(32).rarity(Rarity.RARE));
	public static final Item WARP_CRYSTAL = register("warp_crystal", Gear.WarpCrystal::new, new Item.Properties().durability(24).rarity(Rarity.RARE));
	public static final Item MAGNET_CHARM = register("magnet_charm", Gear.MagnetCharm::new, new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));
	public static final Item BACKPACK = register("backpack", Gear.Backpack::new, new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));
	public static final Item EXCAVATOR_PICKAXE = register("excavator_pickaxe", Gear.Excavator::new, new Item.Properties().pickaxe(VOIDSHARD_TOOL, 1.0F, -3.0F).rarity(Rarity.RARE));
	public static final Item LUMBER_AXE = register("lumber_axe", Gear.LumberAxe::new, new Item.Properties().axe(GLOWCRYSTAL_TOOL, 6.0F, -3.1F).rarity(Rarity.RARE));

	// ------------------------------------------------------------ update 2: starmetal armor
	public static final Item STARMETAL_HELMET = register("starmetal_helmet", LoreItem::new, new Item.Properties().humanoidArmor(STARMETAL_ARMOR, ArmorType.HELMET));
	public static final Item STARMETAL_CHESTPLATE = register("starmetal_chestplate", LoreItem::new, new Item.Properties().humanoidArmor(STARMETAL_ARMOR, ArmorType.CHESTPLATE));
	public static final Item STARMETAL_LEGGINGS = register("starmetal_leggings", LoreItem::new, new Item.Properties().humanoidArmor(STARMETAL_ARMOR, ArmorType.LEGGINGS));
	public static final Item STARMETAL_BOOTS = register("starmetal_boots", LoreItem::new, new Item.Properties().humanoidArmor(STARMETAL_ARMOR, ArmorType.BOOTS));

	// ------------------------------------------------------------ update 2: food
	public static final Item LUMEN_BERRIES = register("lumen_berries", LoreItem::new, food(3, 0.4F, true, 64, eff(MobEffects.NIGHT_VISION, 600, 0)));
	public static final Item AURORA_FRUIT = register("aurora_fruit", LoreItem::new, food(4, 0.6F, true, 64, eff(MobEffects.REGENERATION, 160, 1)));
	public static final Item SHADECAP_STEW = register("shadecap_stew", LoreItem::new,
			food(8, 0.8F, false, 1, eff(MobEffects.STRENGTH, 600, 0), eff(MobEffects.NIGHT_VISION, 600, 0)).usingConvertsTo(net.minecraft.world.item.Items.BOWL));
	public static final Item STARFRUIT_PIE = register("starfruit_pie", LoreItem::new, food(8, 1.0F, false, 64, eff(MobEffects.ABSORPTION, 1200, 1), eff(MobEffects.SPEED, 600, 0)));
	public static final Item EMBER_PEPPER = register("ember_pepper", LoreItem::new, food(2, 0.3F, true, 64, eff(MobEffects.FIRE_RESISTANCE, 1200, 0), eff(MobEffects.HASTE, 600, 0)));

	// ------------------------------------------------------------ update 3: Sculk Realm
	public static final Item SCULK_KEY = register("sculk_key", net.glowcube.realms.item.SculkKeyItem::new, new Item.Properties().stacksTo(1).rarity(Rarity.EPIC));
	public static final Item ECHO_CRYSTAL = register("echo_crystal", LoreItem::new, new Item.Properties());
	public static final Item ECHO_HEART = register("echo_heart", LoreItem::new, new Item.Properties().rarity(Rarity.EPIC));
	public static final Item SONIC_BLADE = register("sonic_blade", Gear.SonicBlade::new, new Item.Properties().sword(LEGENDARY, 4.5F, -2.4F).rarity(Rarity.EPIC));
	public static final Item ECHO_HORN = register("echo_horn", Gear.EchoHorn::new, new Item.Properties().durability(64).rarity(Rarity.RARE));

	// ------------------------------------------------------------ update 3: creature drops
	public static final Item GLIMMER_VENISON = register("glimmer_venison", LoreItem::new, food(3, 0.3F, false, 64, eff(MobEffects.SPEED, 200, 0)));
	public static final Item COOKED_GLIMMER_VENISON = register("cooked_glimmer_venison", LoreItem::new,
			food(8, 0.8F, false, 64, eff(MobEffects.SPEED, 600, 0), eff(MobEffects.REGENERATION, 100, 0)));
	public static final Item GLIMMER_ANTLER = register("glimmer_antler", LoreItem::new, new Item.Properties());
	public static final Item CLOUD_FLUFF = register("cloud_fluff", LoreItem::new, new Item.Properties());
	public static final Item CLOUD_BOTTLE = register("cloud_bottle", Gear.CloudBottle::new, new Item.Properties().durability(200).rarity(Rarity.UNCOMMON));
	public static final Item TOAD_LEG = register("toad_leg", LoreItem::new, food(4, 0.5F, false, 64, eff(MobEffects.JUMP_BOOST, 600, 1)));
	public static final Item GLOW_JELLY = register("glow_jelly", LoreItem::new, food(2, 0.3F, true, 64, eff(MobEffects.NIGHT_VISION, 1200, 0), eff(MobEffects.GLOWING, 200, 0)));
	public static final Item SNAIL_SHELL = register("snail_shell", LoreItem::new, new Item.Properties());
	public static final Item SCULK_SLIME = register("sculk_slime", LoreItem::new, new Item.Properties());
	public static final Item SALAMANDER_SCALE = register("salamander_scale", LoreItem::new, new Item.Properties().fireResistant());
	public static final Item SALAMANDER_CHARM = register("salamander_charm", Gear.SalamanderCharm::new, new Item.Properties().durability(40).rarity(Rarity.RARE).fireResistant());
	public static final Item VOID_JELLY_ITEM = register("void_jelly", LoreItem::new,
			food(4, 0.6F, true, 64, eff(MobEffects.SLOW_FALLING, 400, 0), eff(MobEffects.REGENERATION, 100, 0)));

	// ------------------------------------------------------------ spawn eggs
	public static final Item GLOW_WISP_SPAWN_EGG = egg("glow_wisp_spawn_egg", ModEntities.GLOW_WISP);
	public static final Item CRYSTAL_GOLEM_SPAWN_EGG = egg("crystal_golem_spawn_egg", ModEntities.CRYSTAL_GOLEM);
	public static final Item SHADE_CRAWLER_SPAWN_EGG = egg("shade_crawler_spawn_egg", ModEntities.SHADE_CRAWLER);
	public static final Item REALM_GUARDIAN_SPAWN_EGG = egg("realm_guardian_spawn_egg", ModEntities.REALM_GUARDIAN);
	public static final Item GLOWKEEPER_SPAWN_EGG = egg("glowkeeper_spawn_egg", ModEntities.GLOWKEEPER);
	public static final Item UMBRAL_TYRANT_SPAWN_EGG = egg("umbral_tyrant_spawn_egg", ModEntities.UMBRAL_TYRANT);
	public static final Item EMBER_WARDEN_SPAWN_EGG = egg("ember_warden_spawn_egg", ModEntities.EMBER_WARDEN);

	public static final Item INFERNAL_COLOSSUS_SPAWN_EGG = egg("infernal_colossus_spawn_egg", ModEntities.INFERNAL_COLOSSUS);
	public static final Item VOID_HERALD_SPAWN_EGG = egg("void_herald_spawn_egg", ModEntities.VOID_HERALD);
	public static final Item FROST_LICH_SPAWN_EGG = egg("frost_lich_spawn_egg", ModEntities.FROST_LICH);
	public static final Item TEMPEST_DRAKE_SPAWN_EGG = egg("tempest_drake_spawn_egg", ModEntities.TEMPEST_DRAKE);
	public static final Item HOLLOW_KING_SPAWN_EGG = egg("hollow_king_spawn_egg", ModEntities.HOLLOW_KING);

	public static final Item ECHO_WARDEN_SPAWN_EGG = egg("echo_warden_spawn_egg", ModEntities.ECHO_WARDEN);
	public static final Item SCULK_STALKER_SPAWN_EGG = egg("sculk_stalker_spawn_egg", ModEntities.SCULK_STALKER);
	public static final Item GLIMMER_DEER_SPAWN_EGG = egg("glimmer_deer_spawn_egg", ModEntities.GLIMMER_DEER);
	public static final Item CLOUD_BUNNY_SPAWN_EGG = egg("cloud_bunny_spawn_egg", ModEntities.CLOUD_BUNNY);
	public static final Item SHADE_TOAD_SPAWN_EGG = egg("shade_toad_spawn_egg", ModEntities.SHADE_TOAD);
	public static final Item LANTERN_BUG_SPAWN_EGG = egg("lantern_bug_spawn_egg", ModEntities.LANTERN_BUG);
	public static final Item SCULK_SNAIL_SPAWN_EGG = egg("sculk_snail_spawn_egg", ModEntities.SCULK_SNAIL);
	public static final Item EMBER_SALAMANDER_SPAWN_EGG = egg("ember_salamander_spawn_egg", ModEntities.EMBER_SALAMANDER);
	public static final Item VOID_JELLY_SPAWN_EGG = egg("void_jelly_spawn_egg", ModEntities.VOID_JELLY);

	private static MobEffectInstance eff(net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effect, int ticks, int amplifier) {
		return new MobEffectInstance(effect, ticks, amplifier);
	}

	private static Item.Properties food(int nutrition, float saturation, boolean alwaysEdible, int stackSize, MobEffectInstance... effects) {
		net.minecraft.world.food.FoodProperties.Builder fb = new net.minecraft.world.food.FoodProperties.Builder().nutrition(nutrition).saturationModifier(saturation);
		if (alwaysEdible) fb.alwaysEdible();
		net.minecraft.world.item.component.Consumable consumable = net.minecraft.world.item.component.Consumables.defaultFood()
				.onConsume(new net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect(java.util.List.of(effects))).build();
		return new Item.Properties().stacksTo(stackSize).food(fb.build(), consumable);
	}

	private static Item egg(String name, EntityType<?> type) {
		return register(name, SpawnEggItem::new, new Item.Properties().spawnEgg(type));
	}

	private static Item register(String name, Function<Item.Properties, Item> factory, Item.Properties props) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, GlowcubeRealms.id(name));
		Item item = factory.apply(props.setId(key));
		Registry.register(BuiltInRegistries.ITEM, key, item);
		ALL.add(item);
		return item;
	}

	public static void init() {
	}

	private ModItems() {
	}
}
