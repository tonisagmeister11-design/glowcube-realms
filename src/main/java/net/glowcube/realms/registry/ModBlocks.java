package net.glowcube.realms.registry;

import java.util.function.Function;
import net.glowcube.realms.GlowcubeRealms;
import net.glowcube.realms.block.AltarBlock;
import net.glowcube.realms.block.RealmPortalBlock;
import net.glowcube.realms.world.RealmDimensions;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FlowerBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.UntintedParticleLeavesBlock;
import net.minecraft.world.level.block.sounds.AmbientLeavesBlockSoundPlayer;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

public final class ModBlocks {
	public static final java.util.List<Item> BLOCK_ITEMS = new java.util.ArrayList<>();

	// ------------------------------------------------------------ Lumen Skies
	public static final Block SKYSTONE = register("skystone", Block::new, stone(MapColor.COLOR_LIGHT_BLUE, 1.5F));
	public static final Block SKYSTONE_BRICKS = register("skystone_bricks", Block::new, stone(MapColor.COLOR_LIGHT_BLUE, 2.0F));
	public static final Block CHISELED_SKYSTONE = register("chiseled_skystone", Block::new, stone(MapColor.COLOR_LIGHT_BLUE, 2.0F).lightLevel(s -> 8));
	public static final Block LUMEN_SOIL = register("lumen_soil", Block::new,
			BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(0.5F).sound(SoundType.GRAVEL));
	public static final Block LUMEN_GRASS = register("lumen_grass", Block::new,
			BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_CYAN).strength(0.6F).sound(SoundType.GRASS));
	public static final Block GLOWCRYSTAL_ORE = register("glowcrystal_ore", Block::new,
			stone(MapColor.COLOR_LIGHT_BLUE, 3.0F).lightLevel(s -> 7).requiresCorrectToolForDrops());
	public static final Block GLOWCRYSTAL_BLOCK = register("glowcrystal_block", Block::new,
			BlockBehaviour.Properties.of().mapColor(MapColor.DIAMOND).strength(3.0F).sound(SoundType.AMETHYST).lightLevel(s -> 15).requiresCorrectToolForDrops());
	public static final Block AURORA_LOG = register("aurora_log", RotatedPillarBlock::new, Blocks.logProperties(MapColor.COLOR_PURPLE, MapColor.COLOR_PURPLE, SoundType.WOOD));
	public static final Block AURORA_PLANKS = register("aurora_planks", Block::new,
			BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_MAGENTA).strength(2.0F, 3.0F).sound(SoundType.WOOD).ignitedByLava());
	public static final Block AURORA_LEAVES = register("aurora_leaves",
			p -> new UntintedParticleLeavesBlock(0.06F, ParticleTypes.END_ROD, AmbientLeavesBlockSoundPlayer.noAmbientSound(), p),
			Blocks.leavesProperties(SoundType.CHERRY_LEAVES).mapColor(MapColor.COLOR_MAGENTA).lightLevel(s -> 4));
	public static final Block LUMEN_BLOOM = register("lumen_bloom", p -> new FlowerBlock(MobEffects.GLOWING, 8.0F, p),
			plant().lightLevel(s -> 10));

	// ------------------------------------------------------------ Umbral Depths
	public static final Block UMBRAL_STONE = register("umbral_stone", Block::new, stone(MapColor.COLOR_BLACK, 2.0F).sound(SoundType.DEEPSLATE));
	public static final Block UMBRAL_BRICKS = register("umbral_bricks", Block::new, stone(MapColor.COLOR_BLACK, 2.5F).sound(SoundType.DEEPSLATE_BRICKS));
	public static final Block CHISELED_UMBRAL_STONE = register("chiseled_umbral_stone", Block::new,
			stone(MapColor.COLOR_BLACK, 2.5F).sound(SoundType.DEEPSLATE_BRICKS).lightLevel(s -> 8));
	public static final Block UMBRAL_MOSS = register("umbral_moss", Block::new,
			BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_CYAN).strength(1.0F).sound(SoundType.MOSS).lightLevel(s -> 7));
	public static final Block VOIDSHARD_ORE = register("voidshard_ore", Block::new,
			stone(MapColor.COLOR_PURPLE, 4.0F).sound(SoundType.DEEPSLATE).lightLevel(s -> 6).requiresCorrectToolForDrops());
	public static final Block VOIDSHARD_BLOCK = register("voidshard_block", Block::new,
			BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(4.0F).sound(SoundType.AMETHYST).lightLevel(s -> 12).requiresCorrectToolForDrops());
	public static final Block SHADECAP_BLOCK = register("shadecap_block", Block::new,
			BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_PURPLE).strength(0.4F).sound(SoundType.FUNGUS).lightLevel(s -> 13));
	public static final Block SHADECAP_STEM = register("shadecap_stem", RotatedPillarBlock::new,
			BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY).strength(0.6F).sound(SoundType.STEM));
	public static final Block VOIDBLOOM = register("voidbloom", p -> new FlowerBlock(MobEffects.NIGHT_VISION, 10.0F, p), plant().lightLevel(s -> 9));

	// ------------------------------------------------------------ portals + altars
	public static final Block LUMEN_PORTAL = registerNoItem("lumen_portal", p -> new RealmPortalBlock(RealmDimensions.LUMEN_SKIES, p), portal(MapColor.COLOR_LIGHT_BLUE));
	public static final Block UMBRAL_PORTAL = registerNoItem("umbral_portal", p -> new RealmPortalBlock(RealmDimensions.UMBRAL_DEPTHS, p), portal(MapColor.COLOR_PURPLE));

	public static final Block GLOWKEEPER_ALTAR = register("glowkeeper_altar", p -> new AltarBlock(AltarBlock.Boss.GLOWKEEPER, p), altar(MapColor.GOLD));
	public static final Block TYRANT_ALTAR = register("tyrant_altar", p -> new AltarBlock(AltarBlock.Boss.UMBRAL_TYRANT, p), altar(MapColor.COLOR_BLACK));
	public static final Block WARDEN_ALTAR = register("warden_altar", p -> new AltarBlock(AltarBlock.Boss.EMBER_WARDEN, p), altar(MapColor.NETHER));
	public static final Block COLOSSUS_ALTAR = register("colossus_altar", p -> new AltarBlock(AltarBlock.Boss.INFERNAL_COLOSSUS, p), altar(MapColor.NETHER));
	public static final Block HERALD_ALTAR = register("herald_altar", p -> new AltarBlock(AltarBlock.Boss.VOID_HERALD, p), altar(MapColor.COLOR_PURPLE));
	public static final Block LICH_ALTAR = register("lich_altar", p -> new AltarBlock(AltarBlock.Boss.FROST_LICH, p), altar(MapColor.ICE));
	public static final Block DRAKE_ALTAR = register("drake_altar", p -> new AltarBlock(AltarBlock.Boss.TEMPEST_DRAKE, p), altar(MapColor.COLOR_LIGHT_BLUE));
	public static final Block KING_ALTAR = register("king_altar", p -> new AltarBlock(AltarBlock.Boss.HOLLOW_KING, p), altar(MapColor.COLOR_GREEN));

	private static BlockBehaviour.Properties stone(MapColor color, float strength) {
		return BlockBehaviour.Properties.of().mapColor(color).strength(strength, 6.0F).sound(SoundType.STONE).requiresCorrectToolForDrops();
	}

	private static BlockBehaviour.Properties plant() {
		return BlockBehaviour.Properties.of().mapColor(MapColor.PLANT).noCollision().instabreak().sound(SoundType.GRASS)
				.offsetType(BlockBehaviour.OffsetType.XZ).pushReaction(PushReaction.POPPED);
	}

	private static BlockBehaviour.Properties portal(MapColor color) {
		return BlockBehaviour.Properties.of().mapColor(color).noCollision().randomTicks().strength(-1.0F).sound(SoundType.GLASS)
				.lightLevel(s -> 12).pushReaction(PushReaction.IMMOVEABLE).noLootTable();
	}

	private static BlockBehaviour.Properties altar(MapColor color) {
		return BlockBehaviour.Properties.of().mapColor(color).strength(-1.0F, 3600000.0F).sound(SoundType.STONE).lightLevel(s -> 15).noLootTable();
	}

	private static Block register(String name, Function<BlockBehaviour.Properties, Block> factory, BlockBehaviour.Properties props) {
		Block block = registerNoItem(name, factory, props);
		ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, GlowcubeRealms.id(name));
		Item item = new BlockItem(block, new Item.Properties().setId(itemKey).useBlockDescriptionPrefix());
		Registry.register(BuiltInRegistries.ITEM, itemKey, item);
		Item.BY_BLOCK.put(block, item);
		BLOCK_ITEMS.add(item);
		return block;
	}

	private static Block registerNoItem(String name, Function<BlockBehaviour.Properties, Block> factory, BlockBehaviour.Properties props) {
		ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, GlowcubeRealms.id(name));
		return Blocks.register(key, factory, props);
	}

	public static void init() {
	}

	private ModBlocks() {
	}
}
