import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Writes all JSON assets and data for Glowcube's Realms.
 * Run from the project root: java tools/DataGen.java
 */
public class DataGen {
	static final String NS = "glowcube_realms";
	static final Path ASSETS = Path.of("src/main/resources/assets/" + NS);
	static final Path DATA = Path.of("src/main/resources/data/" + NS);
	static final Path MC_DATA = Path.of("src/main/resources/data/minecraft");

	// ---------------------------------------------------------------- content lists
	static final String[] CUBE_BLOCKS = {"skystone", "skystone_bricks", "chiseled_skystone", "lumen_soil", "glowcrystal_ore", "glowcrystal_block",
			"aurora_planks", "aurora_leaves", "umbral_stone", "umbral_bricks", "chiseled_umbral_stone", "voidshard_ore", "voidshard_block", "shadecap_block"};
	static final String[] PILLARS = {"aurora_log", "shadecap_stem"};
	static final String[] PLANTS = {"lumen_bloom", "voidbloom"};
	static final String[] ALTARS = {"glowkeeper_altar", "tyrant_altar", "warden_altar"};

	static final String[] GENERATED_ITEMS = {"glow_shard", "void_shard", "starmetal_ingot", "wisp_essence", "shade_fang", "golem_fragment",
			"glowkeeper_core", "tyrant_heart", "ember_heart", "lumen_key", "umbral_key", "lumen_compass", "umbral_compass", "glowkeeper_sigil",
			"tyrant_sigil", "warden_sigil", "glowcrystal_helmet", "glowcrystal_chestplate", "glowcrystal_leggings", "glowcrystal_boots",
			"voidshard_helmet", "voidshard_chestplate", "voidshard_leggings", "voidshard_boots", "glow_wisp_spawn_egg", "crystal_golem_spawn_egg",
			"shade_crawler_spawn_egg", "realm_guardian_spawn_egg", "glowkeeper_spawn_egg", "umbral_tyrant_spawn_egg", "ember_warden_spawn_egg"};
	static final String[] HANDHELD_ITEMS = {"glowcrystal_sword", "glowcrystal_pickaxe", "glowcrystal_axe", "glowcrystal_shovel", "voidshard_sword",
			"voidshard_pickaxe", "radiant_blade", "void_scythe", "star_hammer", "shadow_dagger", "ember_greatsword", "aurora_staff"};

	public static void main(String[] args) throws IOException {
		blockAssets();
		itemAssets();
		equipment();
		lang();
		lootTables();
		recipes();
		tags();
		worldgen();
		splashes();
		Update2.all();
		Update3.all();
		Update4.all();
		Update5.all();
		Update6.all();
		flushTags();
		checkReferences();
		System.out.println("Data written.");
	}

	// ================================================================ assets
	static void blockAssets() throws IOException {
		for (String b : CUBE_BLOCKS) {
			if (b.equals("aurora_leaves")) {
				write(ASSETS.resolve("models/block/" + b + ".json"), "{\"parent\":\"minecraft:block/leaves\",\"textures\":{\"all\":\"" + NS + ":block/" + b + "\"}}");
			} else {
				write(ASSETS.resolve("models/block/" + b + ".json"), "{\"parent\":\"minecraft:block/cube_all\",\"textures\":{\"all\":\"" + NS + ":block/" + b + "\"}}");
			}
			write(ASSETS.resolve("blockstates/" + b + ".json"), "{\"variants\":{\"\":{\"model\":\"" + NS + ":block/" + b + "\"}}}");
			blockItem(b);
		}
		// grass-like blocks
		grassBlock("lumen_grass", "lumen_soil", "lumen_grass_top", "lumen_grass_side");
		grassBlock("umbral_moss", "umbral_stone", "umbral_moss_top", "umbral_moss_side");
		for (String p : PILLARS) {
			String top = p.equals("aurora_log") ? "aurora_log_top" : "shadecap_stem";
			write(ASSETS.resolve("models/block/" + p + ".json"), "{\"parent\":\"minecraft:block/cube_column\",\"textures\":{\"end\":\"" + NS + ":block/" + top
					+ "\",\"side\":\"" + NS + ":block/" + p + "\"}}");
			write(ASSETS.resolve("models/block/" + p + "_horizontal.json"), "{\"parent\":\"minecraft:block/cube_column_horizontal\",\"textures\":{\"end\":\"" + NS
					+ ":block/" + top + "\",\"side\":\"" + NS + ":block/" + p + "\"}}");
			write(ASSETS.resolve("blockstates/" + p + ".json"), "{\"variants\":{\"axis=x\":{\"model\":\"" + NS + ":block/" + p + "_horizontal\",\"x\":90,\"y\":90},"
					+ "\"axis=y\":{\"model\":\"" + NS + ":block/" + p + "\"},\"axis=z\":{\"model\":\"" + NS + ":block/" + p + "_horizontal\",\"x\":90}}}");
			blockItem(p);
		}
		for (String p : PLANTS) {
			write(ASSETS.resolve("models/block/" + p + ".json"), "{\"parent\":\"minecraft:block/cross\",\"textures\":{\"cross\":\"" + NS + ":block/" + p + "\"}}");
			write(ASSETS.resolve("blockstates/" + p + ".json"), "{\"variants\":{\"\":{\"model\":\"" + NS + ":block/" + p + "\"}}}");
			write(ASSETS.resolve("models/item/" + p + ".json"), "{\"parent\":\"minecraft:item/generated\",\"textures\":{\"layer0\":\"" + NS + ":block/" + p + "\"}}");
			itemDef(p, NS + ":item/" + p);
		}
		for (String p : new String[]{"lumen_portal", "umbral_portal"}) {
			String elementsNs = "{\"from\":[0,0,6],\"to\":[16,16,10],\"faces\":{\"north\":{\"uv\":[0,0,16,16],\"texture\":\"#portal\"},\"south\":{\"uv\":[0,0,16,16],\"texture\":\"#portal\"}}}";
			String elementsEw = "{\"from\":[6,0,0],\"to\":[10,16,16],\"faces\":{\"east\":{\"uv\":[0,0,16,16],\"texture\":\"#portal\"},\"west\":{\"uv\":[0,0,16,16],\"texture\":\"#portal\"}}}";
			String tex = "\"textures\":{\"particle\":\"" + NS + ":block/" + p + "\",\"portal\":\"" + NS + ":block/" + p + "\"}";
			write(ASSETS.resolve("models/block/" + p + "_ns.json"), "{" + tex + ",\"elements\":[" + elementsNs + "]}");
			write(ASSETS.resolve("models/block/" + p + "_ew.json"), "{" + tex + ",\"elements\":[" + elementsEw + "]}");
			write(ASSETS.resolve("blockstates/" + p + ".json"), "{\"variants\":{\"axis=x\":{\"model\":\"" + NS + ":block/" + p + "_ns\"},\"axis=z\":{\"model\":\""
					+ NS + ":block/" + p + "_ew\"}}}");
		}
		for (String a : ALTARS) {
			String base = a.replace("_altar", "");
			String texBase = base.equals("warden") ? "glowkeeper" : base;
			String top = NS + ":block/" + texBase + "_altar_top", side = NS + ":block/" + texBase + "_altar_side";
			String bottom = base.equals("tyrant") ? NS + ":block/umbral_bricks" : base.equals("warden") ? "minecraft:block/polished_blackstone_bricks" : NS + ":block/skystone_bricks";
			if (base.equals("warden")) {
				top = NS + ":block/warden_altar_top";
				side = NS + ":block/warden_altar_side";
			}
			write(ASSETS.resolve("models/block/" + a + ".json"), "{\"parent\":\"minecraft:block/cube_bottom_top\",\"textures\":{\"top\":\"" + top
					+ "\",\"side\":\"" + side + "\",\"bottom\":\"" + bottom + "\"}}");
			write(ASSETS.resolve("blockstates/" + a + ".json"), "{\"variants\":{\"enabled=false\":{\"model\":\"" + NS + ":block/" + a + "\"},\"enabled=true\":{\"model\":\""
					+ NS + ":block/" + a + "\"}}}");
			blockItem(a);
		}
	}

	static void grassBlock(String name, String bottom, String top, String side) throws IOException {
		write(ASSETS.resolve("models/block/" + name + ".json"), "{\"parent\":\"minecraft:block/cube_bottom_top\",\"textures\":{\"top\":\"" + NS + ":block/" + top
				+ "\",\"side\":\"" + NS + ":block/" + side + "\",\"bottom\":\"" + NS + ":block/" + bottom + "\"}}");
		write(ASSETS.resolve("blockstates/" + name + ".json"), "{\"variants\":{\"\":[{\"model\":\"" + NS + ":block/" + name + "\"},{\"model\":\"" + NS + ":block/" + name
				+ "\",\"y\":90},{\"model\":\"" + NS + ":block/" + name + "\",\"y\":180},{\"model\":\"" + NS + ":block/" + name + "\",\"y\":270}]}}");
		blockItem(name);
	}

	static void blockItem(String b) throws IOException {
		itemDef(b, NS + ":block/" + b);
	}

	static void itemDef(String name, String model) throws IOException {
		write(ASSETS.resolve("items/" + name + ".json"), "{\"model\":{\"type\":\"minecraft:model\",\"model\":\"" + model + "\"}}");
	}

	static void itemAssets() throws IOException {
		for (String i : GENERATED_ITEMS) {
			write(ASSETS.resolve("models/item/" + i + ".json"), "{\"parent\":\"minecraft:item/generated\",\"textures\":{\"layer0\":\"" + NS + ":item/" + i + "\"}}");
			itemDef(i, NS + ":item/" + i);
		}
		for (String i : HANDHELD_ITEMS) {
			write(ASSETS.resolve("models/item/" + i + ".json"), "{\"parent\":\"minecraft:item/handheld\",\"textures\":{\"layer0\":\"" + NS + ":item/" + i + "\"}}");
			itemDef(i, NS + ":item/" + i);
		}
		// bow with pull states
		for (String s : new String[]{"crystal_bow", "crystal_bow_pulling_0", "crystal_bow_pulling_1", "crystal_bow_pulling_2"}) {
			write(ASSETS.resolve("models/item/" + s + ".json"), "{\"parent\":\"minecraft:item/bow\",\"textures\":{\"layer0\":\"" + NS + ":item/" + s + "\"}}");
		}
		write(ASSETS.resolve("items/crystal_bow.json"), "{\"model\":{\"type\":\"minecraft:condition\",\"on_false\":{\"type\":\"minecraft:model\",\"model\":\"" + NS
				+ ":item/crystal_bow\"},\"on_true\":{\"type\":\"minecraft:range_dispatch\",\"entries\":[{\"model\":{\"type\":\"minecraft:model\",\"model\":\"" + NS
				+ ":item/crystal_bow_pulling_1\"},\"threshold\":0.65},{\"model\":{\"type\":\"minecraft:model\",\"model\":\"" + NS
				+ ":item/crystal_bow_pulling_2\"},\"threshold\":0.9}],\"fallback\":{\"type\":\"minecraft:model\",\"model\":\"" + NS
				+ ":item/crystal_bow_pulling_0\"},\"property\":\"minecraft:use_duration\",\"scale\":0.05},\"property\":\"minecraft:using_item\"}}");
	}

	static void equipment() throws IOException {
		for (String m : new String[]{"glowcrystal", "voidshard"}) {
			write(ASSETS.resolve("equipment/" + m + ".json"), "{\"layers\":{\"humanoid\":[{\"texture\":\"" + NS + ":" + m + "\"}],\"humanoid_leggings\":[{\"texture\":\""
					+ NS + ":" + m + "\"}]}}");
		}
	}

	// ================================================================ language
	static void lang() throws IOException {
		Map<String, String[]> t = new LinkedHashMap<>(); // key -> {en, de}
		// blocks
		b(t, "skystone", "Skystone", "Himmelsstein");
		b(t, "skystone_bricks", "Skystone Bricks", "Himmelssteinziegel");
		b(t, "chiseled_skystone", "Chiseled Skystone", "Gemeißelter Himmelsstein");
		b(t, "lumen_soil", "Lumen Soil", "Lumenerde");
		b(t, "lumen_grass", "Lumen Grass", "Lumengras");
		b(t, "glowcrystal_ore", "Glowcrystal Ore", "Glühkristallerz");
		b(t, "glowcrystal_block", "Block of Glowcrystal", "Glühkristallblock");
		b(t, "aurora_log", "Aurora Log", "Aurorastamm");
		b(t, "aurora_planks", "Aurora Planks", "Auroraholzbretter");
		b(t, "aurora_leaves", "Aurora Leaves", "Aurorablätter");
		b(t, "lumen_bloom", "Lumen Bloom", "Lumenblüte");
		b(t, "umbral_stone", "Umbral Stone", "Umbralstein");
		b(t, "umbral_bricks", "Umbral Bricks", "Umbralziegel");
		b(t, "chiseled_umbral_stone", "Chiseled Umbral Stone", "Gemeißelter Umbralstein");
		b(t, "umbral_moss", "Umbral Moss", "Umbralmoos");
		b(t, "voidshard_ore", "Voidshard Ore", "Leerensplittererz");
		b(t, "voidshard_block", "Block of Voidshard", "Leerensplitterblock");
		b(t, "shadecap_block", "Shadecap Block", "Schattenkappenblock");
		b(t, "shadecap_stem", "Shadecap Stem", "Schattenkappenstiel");
		b(t, "voidbloom", "Voidbloom", "Leerenblüte");
		b(t, "lumen_portal", "Lumen Portal", "Lumenportal");
		b(t, "umbral_portal", "Umbral Portal", "Umbralportal");
		b(t, "glowkeeper_altar", "Glowkeeper Altar", "Altar des Glowkeepers");
		b(t, "tyrant_altar", "Tyrant Altar", "Altar des Tyrannen");
		b(t, "warden_altar", "Ember Warden Altar", "Altar des Glutwächters");
		// items
		i(t, "glow_shard", "Glow Shard", "Glühsplitter", "Pure light, frozen into crystal.", "Reines Licht, zu Kristall erstarrt.");
		i(t, "void_shard", "Void Shard", "Leerensplitter", "It hums with the silence of the deep.", "Es summt mit der Stille der Tiefe.");
		i(t, "starmetal_ingot", "Starmetal Ingot", "Sternenmetallbarren", "Forged from fallen stars.", "Aus gefallenen Sternen geschmiedet.");
		i(t, "wisp_essence", "Wisp Essence", "Irrlichtessenz", null, null);
		i(t, "shade_fang", "Shade Fang", "Schattenzahn", null, null);
		i(t, "golem_fragment", "Golem Fragment", "Golemfragment", null, null);
		i(t, "glowkeeper_core", "Glowkeeper Core", "Kern des Glowkeepers", "The still-beating heart of the Glowkeeper.", "Das noch schlagende Herz des Glowkeepers.");
		i(t, "tyrant_heart", "Heart of the Tyrant", "Herz des Tyrannen", "Darkness pulses within.", "Dunkelheit pulsiert darin.");
		i(t, "ember_heart", "Ember Heart", "Glutherz", "Still burning.", "Brennt noch immer.");
		i(t, "lumen_key", "Lumen Key", "Lumenschlüssel", "Use on a glowstone portal frame.", "Auf einen Glowstone-Portalrahmen anwenden.");
		i(t, "umbral_key", "Umbral Key", "Umbralschlüssel", "Use on a crying obsidian portal frame.", "Auf einen Rahmen aus weinendem Obsidian anwenden.");
		i(t, "lumen_compass", "Lumen Compass", "Lumenkompass", "Right-click: travel to the Lumen Skies (and back).", "Rechtsklick: Reise in die Lumen Skies (und zurück).");
		i(t, "umbral_compass", "Umbral Compass", "Umbralkompass", "Right-click: travel to the Umbral Depths (and back).", "Rechtsklick: Reise in die Umbral Depths (und zurück).");
		i(t, "glowkeeper_sigil", "Glowkeeper Sigil", "Siegel des Glowkeepers", "Use on its altar to summon the Glowkeeper again.", "Am Altar benutzen, um den Glowkeeper erneut zu rufen.");
		i(t, "tyrant_sigil", "Tyrant Sigil", "Siegel des Tyrannen", "Use on its altar to summon the Umbral Tyrant again.", "Am Altar benutzen, um den Umbral Tyrant erneut zu rufen.");
		i(t, "warden_sigil", "Ember Warden Sigil", "Siegel des Glutwächters", "Use on its altar to summon the Ember Warden again.", "Am Altar benutzen, um den Glutwächter erneut zu rufen.");
		i(t, "glowcrystal_sword", "Glowcrystal Sword", "Glühkristallschwert", null, null);
		i(t, "glowcrystal_pickaxe", "Glowcrystal Pickaxe", "Glühkristallspitzhacke", null, null);
		i(t, "glowcrystal_axe", "Glowcrystal Axe", "Glühkristallaxt", null, null);
		i(t, "glowcrystal_shovel", "Glowcrystal Shovel", "Glühkristallschaufel", null, null);
		i(t, "voidshard_sword", "Voidshard Sword", "Leerensplitterschwert", null, null);
		i(t, "voidshard_pickaxe", "Voidshard Pickaxe", "Leerensplitterspitzhacke", null, null);
		i(t, "radiant_blade", "Radiant Blade", "Strahlende Klinge", "Right-click: Radiant Wave. Smites the undead.", "Rechtsklick: Strahlenwelle. Vernichtet Untote.");
		i(t, "void_scythe", "Void Scythe", "Leerensense", "Reaps everything around the target and steals life. Right-click: Void Pull.", "Trifft alles um das Ziel herum und stiehlt Leben. Rechtsklick: Leerensog.");
		i(t, "star_hammer", "Star Hammer", "Sternenhammer", "Right-click: Meteor Stomp.", "Rechtsklick: Meteorstampfer.");
		i(t, "shadow_dagger", "Shadow Dagger", "Schattendolch", "Double damage from behind. Right-click: Shadow Step.", "Doppelter Schaden von hinten. Rechtsklick: Schattenschritt.");
		i(t, "ember_greatsword", "Ember Greatsword", "Glut-Großschwert", "Sets foes ablaze. Right-click: Inferno Slash.", "Setzt Gegner in Brand. Rechtsklick: Infernohieb.");
		i(t, "crystal_bow", "Crystal Bow", "Kristallbogen", "Faster arrows that always crit.", "Schnellere Pfeile, immer kritisch.");
		i(t, "aurora_staff", "Aurora Staff", "Aurorastab", "Fires seeking crystal bolts. Sneak + right-click: Aurora Heal.", "Verschießt zielsuchende Kristallblitze. Schleichen + Rechtsklick: Auroraheilung.");
		i(t, "glowcrystal_helmet", "Glowcrystal Helmet", "Glühkristallhelm", "Set bonus: night vision, regeneration in the Lumen Skies.", "Setbonus: Nachtsicht, Regeneration in den Lumen Skies.");
		i(t, "glowcrystal_chestplate", "Glowcrystal Chestplate", "Glühkristallbrustpanzer", "Set bonus: sneak in the air to glide.", "Setbonus: In der Luft schleichen zum Gleiten.");
		i(t, "glowcrystal_leggings", "Glowcrystal Leggings", "Glühkristallbeinschutz", null, null);
		i(t, "glowcrystal_boots", "Glowcrystal Boots", "Glühkristallstiefel", null, null);
		i(t, "voidshard_helmet", "Voidshard Helmet", "Leerensplitterhelm", "Set bonus: strength, immune to darkness.", "Setbonus: Stärke, immun gegen Dunkelheit.");
		i(t, "voidshard_chestplate", "Voidshard Chestplate", "Leerensplitterbrustpanzer", "Set bonus: resistance in the Umbral Depths.", "Setbonus: Resistenz in den Umbral Depths.");
		i(t, "voidshard_leggings", "Voidshard Leggings", "Leerensplitterbeinschutz", null, null);
		i(t, "voidshard_boots", "Voidshard Boots", "Leerensplitterstiefel", null, null);
		i(t, "glow_wisp_spawn_egg", "Glow Wisp Spawn Egg", "Glüh-Irrlicht-Spawn-Ei", null, null);
		i(t, "crystal_golem_spawn_egg", "Crystal Golem Spawn Egg", "Kristallgolem-Spawn-Ei", null, null);
		i(t, "shade_crawler_spawn_egg", "Shade Crawler Spawn Egg", "Schattenkriecher-Spawn-Ei", null, null);
		i(t, "realm_guardian_spawn_egg", "Realm Guardian Spawn Egg", "Reichswächter-Spawn-Ei", null, null);
		i(t, "glowkeeper_spawn_egg", "Glowkeeper Spawn Egg", "Glowkeeper-Spawn-Ei", null, null);
		i(t, "umbral_tyrant_spawn_egg", "Umbral Tyrant Spawn Egg", "Umbral-Tyrant-Spawn-Ei", null, null);
		i(t, "ember_warden_spawn_egg", "Ember Warden Spawn Egg", "Glutwächter-Spawn-Ei", null, null);
		// entities
		e(t, "glow_wisp", "Glow Wisp", "Glüh-Irrlicht");
		e(t, "crystal_golem", "Crystal Golem", "Kristallgolem");
		e(t, "shade_crawler", "Shade Crawler", "Schattenkriecher");
		e(t, "realm_guardian", "Realm Guardian", "Reichswächter");
		e(t, "glowkeeper", "The Glowkeeper", "Der Glowkeeper");
		e(t, "umbral_tyrant", "The Umbral Tyrant", "Der Umbral Tyrant");
		e(t, "ember_warden", "The Ember Warden", "Der Glutwächter");
		e(t, "glow_shard", "Crystal Bolt", "Kristallblitz");
		// bosses
		for (String[] boss : new String[][]{
				{"glowkeeper", "The Glowkeeper awakens", "Der Glowkeeper erwacht", "Guardian of the Lumen Skies", "Wächter der Lumen Skies",
						"Crystal reinforcements!", "Kristall-Verstärkung!", "SUPERNOVA", "SUPERNOVA"},
				{"umbral_tyrant", "The Umbral Tyrant rises", "Der Umbral Tyrant erhebt sich", "Ruler of the Umbral Depths", "Herrscher der Umbral Depths",
						"The swarm awakens!", "Der Schwarm erwacht!", "Eternal darkness", "Ewige Dunkelheit"},
				{"ember_warden", "The Ember Warden stands", "Der Glutwächter erhebt sein Schwert", "Keeper of the Ember Citadel", "Hüter der Glutzitadelle",
						"Meteors fall!", "Meteore fallen!", "Inferno unleashed", "Inferno entfesselt"}}) {
			t.put("boss." + NS + "." + boss[0] + ".awakens", new String[]{boss[1], boss[2]});
			t.put("boss." + NS + "." + boss[0] + ".subtitle", new String[]{boss[3], boss[4]});
			t.put("boss." + NS + "." + boss[0] + ".phase2", new String[]{boss[5], boss[6]});
			t.put("boss." + NS + "." + boss[0] + ".phase3", new String[]{boss[7], boss[8]});
			t.put("boss." + NS + "." + boss[0] + ".defeated", new String[]{"VICTORY!", "SIEG!"});
		}
		t.put("boss." + NS + ".phase", new String[]{"PHASE %s", "PHASE %s"});
		t.put("boss." + NS + ".reward", new String[]{"A reward chest appeared at the altar", "Eine Belohnungstruhe ist am Altar erschienen"});
		// misc
		t.put("itemGroup." + NS + ".main", new String[]{"Glowcube's Realms", "Glowcube's Realms"});
		t.put("message." + NS + ".portal_invalid", new String[]{"The frame is not complete.", "Der Rahmen ist nicht vollständig."});
		t.put("message." + NS + ".arrived_lumen_skies", new String[]{"Welcome to the Lumen Skies", "Willkommen in den Lumen Skies"});
		t.put("message." + NS + ".arrived_umbral_depths", new String[]{"Welcome to the Umbral Depths", "Willkommen in den Umbral Depths"});
		t.put("message." + NS + ".back_home", new String[]{"Back in the Overworld", "Zurück in der Oberwelt"});
		t.put("message." + NS + ".fell_from_sky", new String[]{"You fell from the Lumen Skies!", "Du bist aus den Lumen Skies gefallen!"});
		t.put("message." + NS + ".sigil_wrong_altar", new String[]{"Use this sigil on its boss altar.", "Benutze das Siegel am passenden Boss-Altar."});
		t.put("message." + NS + ".boss_alive", new String[]{"The boss is still alive!", "Der Boss lebt noch!"});
		t.put("message." + NS + ".waypoint_added", new String[]{"Waypoint added: %s", "Wegpunkt gesetzt: %s"});
		t.put("hud." + NS + ".nearest_boss", new String[]{"%s: %sm", "%s: %sm"});
		t.put("hud." + NS + ".defeated", new String[]{"%s (defeated)", "%s (besiegt)"});
		t.put("screen." + NS + ".world_map", new String[]{"World Map", "Weltkarte"});
		t.put("screen." + NS + ".world_map.help", new String[]{"Drag: move  |  Scroll: zoom  |  Right-click waypoint: delete",
				"Ziehen: bewegen  |  Mausrad: zoomen  |  Rechtsklick auf Wegpunkt: löschen"});
		t.put("key.category." + NS + ".main", new String[]{"Glowcube's Realms", "Glowcube's Realms"});
		t.put("key." + NS + ".toggle_minimap", new String[]{"Toggle Minimap", "Minimap ein/aus"});
		t.put("key." + NS + ".world_map", new String[]{"Open World Map", "Weltkarte öffnen"});
		t.put("key." + NS + ".add_waypoint", new String[]{"Add Waypoint", "Wegpunkt setzen"});
		t.put("key." + NS + ".zoom_minimap", new String[]{"Minimap Zoom", "Minimap-Zoom"});
		t.put("title." + NS + ".credit", new String[]{"Glowcube's Realms - made by Glowcube", "Glowcube's Realms - von Glowcube"});
		// biomes
		for (String[] bio : new String[][]{{"lumen_meadows", "Lumen Meadows", "Lumenwiesen"}, {"aurora_forest", "Aurora Forest", "Aurorawald"},
				{"crystal_peaks", "Crystal Peaks", "Kristallgipfel"}, {"umbral_caverns", "Umbral Caverns", "Umbralhöhlen"},
				{"shadecap_forest", "Shadecap Forest", "Schattenkappenwald"}, {"void_rift", "Void Rift", "Leerenspalt"}}) {
			t.put("biome." + NS + "." + bio[0], new String[]{bio[1], bio[2]});
		}
		Update2.lang(t);
		Update3.lang(t);
		Update5.lang(t);
		StringBuilder en = new StringBuilder("{\n"), de = new StringBuilder("{\n");
		int n = 0;
		for (Map.Entry<String, String[]> entry : t.entrySet()) {
			String sep = ++n < t.size() ? ",\n" : "\n";
			en.append("  ").append(q(entry.getKey())).append(": ").append(q(entry.getValue()[0])).append(sep);
			de.append("  ").append(q(entry.getKey())).append(": ").append(q(entry.getValue()[1])).append(sep);
		}
		write(ASSETS.resolve("lang/en_us.json"), en.append("}\n").toString());
		write(ASSETS.resolve("lang/de_de.json"), de.append("}\n").toString());
	}

	static void b(Map<String, String[]> t, String id, String en, String de) {
		t.put("block." + NS + "." + id, new String[]{en, de});
	}

	static void e(Map<String, String[]> t, String id, String en, String de) {
		t.put("entity." + NS + "." + id, new String[]{en, de});
	}

	static void i(Map<String, String[]> t, String id, String en, String de, String loreEn, String loreDe) {
		t.put("item." + NS + "." + id, new String[]{en, de});
		if (loreEn != null) t.put("item." + NS + "." + id + ".lore1", new String[]{loreEn, loreDe});
	}

	// ================================================================ loot tables
	static void lootTables() throws IOException {
		String[] selfDrop = {"skystone", "skystone_bricks", "chiseled_skystone", "lumen_soil", "glowcrystal_block", "aurora_log", "aurora_planks",
				"lumen_bloom", "umbral_stone", "umbral_bricks", "chiseled_umbral_stone", "voidshard_block", "shadecap_block", "shadecap_stem", "voidbloom"};
		for (String b : selfDrop) {
			write(DATA.resolve("loot_table/blocks/" + b + ".json"), "{\"type\":\"minecraft:block\",\"pools\":[{\"condition\":{\"type\":\"minecraft:survives_explosion\"},"
					+ "\"entries\":[{\"type\":\"minecraft:item\",\"name\":\"" + NS + ":" + b + "\"}],\"rolls\":1}],\"random_sequence\":\"" + NS + ":blocks/" + b + "\"}");
		}
		silkOr("lumen_grass", NS + ":lumen_soil", 1, 1);
		silkOr("umbral_moss", NS + ":umbral_stone", 1, 1);
		silkOr("glowcrystal_ore", NS + ":glow_shard", 1, 3);
		silkOr("voidshard_ore", NS + ":void_shard", 1, 2);
		write(DATA.resolve("loot_table/blocks/aurora_leaves.json"), "{\"type\":\"minecraft:block\",\"pools\":[{\"entries\":[{\"type\":\"minecraft:alternatives\","
				+ "\"children\":[{\"type\":\"minecraft:item\",\"condition\":\"minecraft:tool/can_silk_touch\",\"name\":\"" + NS + ":aurora_leaves\"},"
				+ "{\"type\":\"minecraft:item\",\"condition\":{\"type\":\"minecraft:random_chance\",\"chance\":0.05},\"name\":\"" + NS + ":glow_shard\"}]}],\"rolls\":1}],"
				+ "\"random_sequence\":\"" + NS + ":blocks/aurora_leaves\"}");

		entityLoot("glow_wisp", new String[][]{{NS + ":wisp_essence", "0", "2"}, {"minecraft:glowstone_dust", "0", "2"}});
		entityLoot("crystal_golem", new String[][]{{NS + ":glow_shard", "1", "4"}, {NS + ":golem_fragment", "0", "2"}});
		entityLoot("shade_crawler", new String[][]{{NS + ":shade_fang", "0", "2"}, {NS + ":void_shard", "0", "1"}, {"minecraft:string", "0", "2"}});
		entityLoot("realm_guardian", new String[][]{{"minecraft:emerald", "0", "1"}});
		entityLoot("glowkeeper", new String[][]{{NS + ":glowkeeper_core", "1", "1"}, {NS + ":starmetal_ingot", "3", "6"}, {NS + ":glow_shard", "8", "16"},
				{NS + ":glowkeeper_sigil", "1", "1"}});
		entityLoot("umbral_tyrant", new String[][]{{NS + ":tyrant_heart", "1", "1"}, {NS + ":void_shard", "8", "16"}, {NS + ":shade_fang", "4", "8"},
				{NS + ":tyrant_sigil", "1", "1"}});
		entityLoot("ember_warden", new String[][]{{NS + ":ember_heart", "1", "1"}, {"minecraft:blaze_rod", "4", "8"}, {"minecraft:netherite_scrap", "1", "2"},
				{NS + ":warden_sigil", "1", "1"}});

		chest("glowkeeper_sanctum", new Object[][]{{NS + ":glow_shard", 30, 3, 8}, {NS + ":starmetal_ingot", 10, 1, 2}, {NS + ":lumen_compass", 6, 1, 1},
				{NS + ":aurora_staff", 4, 1, 1}, {NS + ":crystal_bow", 5, 1, 1}, {"minecraft:golden_apple", 10, 1, 2}, {"minecraft:diamond", 10, 1, 3},
				{NS + ":glowcrystal_helmet", 3, 1, 1}, {NS + ":glowcrystal_boots", 3, 1, 1}}, 4, 7);
		chest("umbral_throne", new Object[][]{{NS + ":void_shard", 30, 3, 8}, {NS + ":shade_fang", 15, 1, 4}, {NS + ":umbral_compass", 6, 1, 1},
				{NS + ":shadow_dagger", 4, 1, 1}, {"minecraft:echo_shard", 8, 1, 3}, {"minecraft:netherite_scrap", 4, 1, 1}, {"minecraft:diamond", 10, 1, 3},
				{NS + ":voidshard_helmet", 3, 1, 1}, {NS + ":voidshard_boots", 3, 1, 1}}, 4, 7);
		chest("ember_citadel", new Object[][]{{"minecraft:blaze_rod", 20, 2, 6}, {"minecraft:gold_ingot", 20, 3, 8}, {"minecraft:diamond", 10, 1, 3},
				{"minecraft:golden_apple", 10, 1, 2}, {NS + ":lumen_key", 6, 1, 1}, {NS + ":umbral_key", 6, 1, 1}, {"minecraft:netherite_scrap", 4, 1, 1},
				{"minecraft:fire_charge", 15, 2, 6}}, 4, 7);
		chest("glowcube_shrine", new Object[][]{{NS + ":lumen_key", 25, 1, 1}, {NS + ":umbral_key", 15, 1, 1}, {NS + ":glow_shard", 30, 2, 5},
				{"minecraft:glowstone", 25, 2, 4}, {NS + ":lumen_compass", 6, 1, 1}, {"minecraft:map", 10, 1, 1}, {"minecraft:bread", 20, 2, 5}}, 3, 5);
		chest("glowkeeper_reward", new Object[][]{{NS + ":starmetal_ingot", 20, 2, 4}, {NS + ":radiant_blade", 6, 1, 1}, {NS + ":star_hammer", 6, 1, 1},
				{NS + ":glowcrystal_chestplate", 8, 1, 1}, {NS + ":glowcrystal_leggings", 8, 1, 1}, {"minecraft:enchanted_golden_apple", 4, 1, 1},
				{NS + ":glowcrystal_block", 15, 2, 4}, {"minecraft:diamond_block", 6, 1, 2}}, 4, 6);
		chest("umbral_tyrant_reward", new Object[][]{{NS + ":voidshard_block", 15, 2, 4}, {NS + ":void_scythe", 6, 1, 1}, {NS + ":shadow_dagger", 8, 1, 1},
				{NS + ":voidshard_chestplate", 8, 1, 1}, {NS + ":voidshard_leggings", 8, 1, 1}, {"minecraft:enchanted_golden_apple", 4, 1, 1},
				{"minecraft:netherite_ingot", 4, 1, 1}, {"minecraft:diamond_block", 6, 1, 2}}, 4, 6);
		chest("ember_warden_reward", new Object[][]{{NS + ":ember_greatsword", 8, 1, 1}, {"minecraft:netherite_ingot", 6, 1, 2},
				{"minecraft:enchanted_golden_apple", 6, 1, 1}, {NS + ":lumen_compass", 8, 1, 1}, {NS + ":umbral_compass", 8, 1, 1},
				{"minecraft:diamond_block", 8, 1, 2}, {"minecraft:totem_of_undying", 4, 1, 1}}, 4, 6);
	}

	static void silkOr(String block, String drop, int min, int max) throws IOException {
		write(DATA.resolve("loot_table/blocks/" + block + ".json"), "{\"type\":\"minecraft:block\",\"pools\":[{\"entries\":[{\"type\":\"minecraft:alternatives\","
				+ "\"children\":[{\"type\":\"minecraft:item\",\"condition\":\"minecraft:tool/can_silk_touch\",\"name\":\"" + NS + ":" + block + "\"},"
				+ "{\"type\":\"minecraft:item\",\"modifier\":[{\"type\":\"minecraft:set_count\",\"count\":{\"type\":\"minecraft:uniform\",\"max\":" + max + ",\"min\":" + min + "}},"
				+ "{\"type\":\"minecraft:apply_bonus\",\"enchantment\":\"minecraft:fortune\",\"formula\":\"minecraft:ore_drops\"},{\"type\":\"minecraft:explosion_decay\"}],"
				+ "\"name\":\"" + drop + "\"}]}],\"rolls\":1}],\"random_sequence\":\"" + NS + ":blocks/" + block + "\"}");
	}

	static void entityLoot(String entity, String[][] drops) throws IOException {
		List<String> pools = new ArrayList<>();
		for (String[] d : drops) {
			pools.add("{\"entries\":[{\"type\":\"minecraft:item\",\"modifier\":[{\"type\":\"minecraft:set_count\",\"count\":{\"type\":\"minecraft:uniform\",\"max\":" + d[2]
					+ ",\"min\":" + d[1] + "}},{\"type\":\"minecraft:enchanted_count_increase\",\"count\":{\"type\":\"minecraft:uniform\",\"max\":1.0,\"min\":0.0},"
					+ "\"enchantment\":\"minecraft:looting\"}],\"name\":\"" + d[0] + "\"}],\"rolls\":1}");
		}
		write(DATA.resolve("loot_table/entities/" + entity + ".json"), "{\"type\":\"minecraft:entity\",\"pools\":[" + String.join(",", pools)
				+ "],\"random_sequence\":\"" + NS + ":entities/" + entity + "\"}");
	}

	static void chest(String name, Object[][] entries, int minRolls, int maxRolls) throws IOException {
		List<String> list = new ArrayList<>();
		for (Object[] e : entries) {
			list.add("{\"type\":\"minecraft:item\",\"modifier\":{\"type\":\"minecraft:set_count\",\"count\":{\"type\":\"minecraft:uniform\",\"max\":" + e[3] + ",\"min\":" + e[2]
					+ "}},\"name\":\"" + e[0] + "\",\"weight\":" + e[1] + "}");
		}
		write(DATA.resolve("loot_table/chests/" + name + ".json"), "{\"type\":\"minecraft:chest\",\"pools\":[{\"entries\":[" + String.join(",", list)
				+ "],\"rolls\":{\"type\":\"minecraft:uniform\",\"max\":" + maxRolls + ",\"min\":" + minRolls + "}}],\"random_sequence\":\"" + NS + ":chests/" + name + "\"}");
	}

	// ================================================================ recipes
	static void recipes() throws IOException {
		shaped("glowcrystal_block", 1, new String[]{"###", "###", "###"}, "#", NS + ":glow_shard");
		shapeless("glow_shard_from_block", NS + ":glow_shard", 9, NS + ":glowcrystal_block");
		shaped("voidshard_block", 1, new String[]{"###", "###", "###"}, "#", NS + ":void_shard");
		shapeless("void_shard_from_block", NS + ":void_shard", 9, NS + ":voidshard_block");
		shapeless("aurora_planks", NS + ":aurora_planks", 4, NS + ":aurora_log");
		shaped("skystone_bricks", 4, new String[]{"##", "##"}, "#", NS + ":skystone");
		shaped("umbral_bricks", 4, new String[]{"##", "##"}, "#", NS + ":umbral_stone");
		shaped("chiseled_skystone", 1, new String[]{"#", "G", "#"}, "#", NS + ":skystone_bricks", "G", NS + ":glow_shard");
		shaped("chiseled_umbral_stone", 1, new String[]{"#", "V", "#"}, "#", NS + ":umbral_bricks", "V", NS + ":void_shard");
		// tools
		tool("glowcrystal", NS + ":glow_shard");
		shaped("voidshard_sword", 1, new String[]{"X", "X", "#"}, "X", NS + ":void_shard", "#", "minecraft:blaze_rod");
		shaped("voidshard_pickaxe", 1, new String[]{"XXX", " # ", " # "}, "X", NS + ":void_shard", "#", "minecraft:blaze_rod");
		armor("glowcrystal", NS + ":glow_shard");
		armor("voidshard", NS + ":void_shard");
		// travel
		shaped("lumen_key", 1, new String[]{" GD", " IG", "I  "}, "G", "minecraft:glowstone_dust", "D", "minecraft:diamond", "I", "minecraft:gold_ingot");
		shaped("umbral_key", 1, new String[]{" OE", " IO", "I  "}, "O", "minecraft:crying_obsidian", "E", "minecraft:ender_pearl", "I", "minecraft:iron_ingot");
		shaped("lumen_compass", 1, new String[]{"GDG", "DCD", "GDG"}, "G", "minecraft:glowstone", "D", "minecraft:diamond", "C", "minecraft:compass");
		shaped("umbral_compass", 1, new String[]{"OAO", "ACA", "OAO"}, "O", "minecraft:crying_obsidian", "A", "minecraft:amethyst_shard", "C", "minecraft:compass");
		// sigils
		shaped("glowkeeper_sigil", 1, new String[]{"GSG", "SBS", "GSG"}, "G", "minecraft:gold_ingot", "S", NS + ":glow_shard", "B", NS + ":glowcrystal_block");
		shaped("tyrant_sigil", 1, new String[]{"FSF", "SBS", "FSF"}, "F", NS + ":shade_fang", "S", NS + ":void_shard", "B", NS + ":voidshard_block");
		shaped("warden_sigil", 1, new String[]{"GBG", "BMB", "GBG"}, "G", "minecraft:gold_block", "B", "minecraft:blaze_powder", "M", "minecraft:magma_block");
		// legendary
		shaped("radiant_blade", 1, new String[]{" S ", "SCS", " B "}, "S", NS + ":starmetal_ingot", "C", NS + ":glowkeeper_core", "B", NS + ":glowcrystal_sword");
		shaped("star_hammer", 1, new String[]{"SSS", "SCS", " R "}, "S", NS + ":starmetal_ingot", "C", NS + ":glowcrystal_block", "R", "minecraft:breeze_rod");
		shaped("void_scythe", 1, new String[]{"VVH", " R ", "R  "}, "V", NS + ":voidshard_block", "H", NS + ":tyrant_heart", "R", "minecraft:blaze_rod");
		shaped("shadow_dagger", 1, new String[]{" F", "V "}, "F", NS + ":shade_fang", "V", NS + ":void_shard");
		shaped("ember_greatsword", 1, new String[]{" N ", "NHN", " R "}, "N", "minecraft:netherite_ingot", "H", NS + ":ember_heart", "R", "minecraft:blaze_rod");
		shaped("crystal_bow", 1, new String[]{" GS", "P S", " GS"}, "G", NS + ":glow_shard", "S", "minecraft:string", "P", NS + ":aurora_planks");
		shaped("aurora_staff", 1, new String[]{" WG", " LW", "L  "}, "W", NS + ":wisp_essence", "G", NS + ":glowcrystal_block", "L", NS + ":aurora_log");
		// smelting
		write(DATA.resolve("recipe/glow_shard_from_smelting.json"), "{\"type\":\"minecraft:smelting\",\"cookingtime\":200,\"experience\":1.0,"
				+ "\"ingredient\":\"" + NS + ":glowcrystal_ore\",\"result\":{\"id\":\"" + NS + ":glow_shard\"}}");
		write(DATA.resolve("recipe/void_shard_from_smelting.json"), "{\"type\":\"minecraft:smelting\",\"cookingtime\":200,\"experience\":1.0,"
				+ "\"ingredient\":\"" + NS + ":voidshard_ore\",\"result\":{\"id\":\"" + NS + ":void_shard\"}}");
	}

	static void tool(String mat, String item) throws IOException {
		shaped(mat + "_sword", 1, new String[]{"X", "X", "#"}, "X", item, "#", "minecraft:stick");
		shaped(mat + "_pickaxe", 1, new String[]{"XXX", " # ", " # "}, "X", item, "#", "minecraft:stick");
		shaped(mat + "_axe", 1, new String[]{"XX", "X#", " #"}, "X", item, "#", "minecraft:stick");
		shaped(mat + "_shovel", 1, new String[]{"X", "#", "#"}, "X", item, "#", "minecraft:stick");
	}

	static void armor(String mat, String item) throws IOException {
		shaped(mat + "_helmet", 1, new String[]{"XXX", "X X"}, "X", item);
		shaped(mat + "_chestplate", 1, new String[]{"X X", "XXX", "XXX"}, "X", item);
		shaped(mat + "_leggings", 1, new String[]{"XXX", "X X", "X X"}, "X", item);
		shaped(mat + "_boots", 1, new String[]{"X X", "X X"}, "X", item);
	}

	static void shaped(String name, int count, String[] pattern, String... keys) throws IOException {
		StringBuilder k = new StringBuilder();
		for (int i = 0; i < keys.length; i += 2) {
			if (i > 0) k.append(",");
			k.append(q(keys[i])).append(":").append(q(keys[i + 1]));
		}
		StringBuilder p = new StringBuilder();
		for (int i = 0; i < pattern.length; i++) p.append(i > 0 ? "," : "").append(q(pattern[i]));
		write(DATA.resolve("recipe/" + name + ".json"), "{\"type\":\"minecraft:crafting_shaped\",\"category\":\"misc\",\"key\":{" + k + "},\"pattern\":[" + p
				+ "],\"result\":{\"count\":" + count + ",\"id\":\"" + NS + ":" + name + "\"}}");
	}

	static void shapeless(String file, String result, int count, String... ingredients) throws IOException {
		StringBuilder in = new StringBuilder();
		for (int i = 0; i < ingredients.length; i++) in.append(i > 0 ? "," : "").append(q(ingredients[i]));
		write(DATA.resolve("recipe/" + file + ".json"), "{\"type\":\"minecraft:crafting_shapeless\",\"category\":\"misc\",\"ingredients\":[" + in
				+ "],\"result\":{\"count\":" + count + ",\"id\":\"" + result + "\"}}");
	}

	// ================================================================ tags
	static void tags() throws IOException {
		tag(MC_DATA.resolve("tags/block/mineable/pickaxe.json"), "skystone", "skystone_bricks", "chiseled_skystone", "glowcrystal_ore", "glowcrystal_block",
				"umbral_stone", "umbral_bricks", "chiseled_umbral_stone", "voidshard_ore", "voidshard_block");
		tag(MC_DATA.resolve("tags/block/mineable/shovel.json"), "lumen_soil", "lumen_grass");
		tag(MC_DATA.resolve("tags/block/mineable/axe.json"), "aurora_log", "aurora_planks", "shadecap_block", "shadecap_stem");
		tag(MC_DATA.resolve("tags/block/mineable/hoe.json"), "aurora_leaves", "umbral_moss");
		tag(MC_DATA.resolve("tags/block/needs_iron_tool.json"), "glowcrystal_ore", "glowcrystal_block");
		tag(MC_DATA.resolve("tags/block/needs_diamond_tool.json"), "voidshard_ore", "voidshard_block");
		tag(MC_DATA.resolve("tags/block/logs.json"), "aurora_log");
		tag(MC_DATA.resolve("tags/block/logs_that_burn.json"), "aurora_log");
		tag(MC_DATA.resolve("tags/block/leaves.json"), "aurora_leaves");
		tag(MC_DATA.resolve("tags/block/planks.json"), "aurora_planks");
		tag(MC_DATA.resolve("tags/block/dirt.json"), "lumen_soil", "lumen_grass", "umbral_moss");
		tag(MC_DATA.resolve("tags/block/small_flowers.json"), "lumen_bloom", "voidbloom");
		tag(MC_DATA.resolve("tags/block/base_stone_overworld.json"), "skystone", "umbral_stone");
		tag(MC_DATA.resolve("tags/item/logs.json"), "aurora_log");
		tag(MC_DATA.resolve("tags/item/planks.json"), "aurora_planks");
		tag(MC_DATA.resolve("tags/item/leaves.json"), "aurora_leaves");
		tag(MC_DATA.resolve("tags/item/swords.json"), "glowcrystal_sword", "voidshard_sword", "radiant_blade", "void_scythe", "shadow_dagger", "ember_greatsword");
		tag(MC_DATA.resolve("tags/item/pickaxes.json"), "glowcrystal_pickaxe", "voidshard_pickaxe");
		tag(MC_DATA.resolve("tags/item/axes.json"), "glowcrystal_axe", "star_hammer");
		tag(MC_DATA.resolve("tags/item/shovels.json"), "glowcrystal_shovel");
		tag(MC_DATA.resolve("tags/item/head_armor.json"), "glowcrystal_helmet", "voidshard_helmet");
		tag(MC_DATA.resolve("tags/item/chest_armor.json"), "glowcrystal_chestplate", "voidshard_chestplate");
		tag(MC_DATA.resolve("tags/item/leg_armor.json"), "glowcrystal_leggings", "voidshard_leggings");
		tag(MC_DATA.resolve("tags/item/foot_armor.json"), "glowcrystal_boots", "voidshard_boots");
		tag(MC_DATA.resolve("tags/item/bow_enchantable.json"), "crystal_bow");
		tag(DATA.resolve("tags/item/glowcrystal_tool_materials.json"), "glow_shard");
		tag(DATA.resolve("tags/item/voidshard_tool_materials.json"), "void_shard");
		tag(MC_DATA.resolve("tags/entity_type/undead.json"));
		// biome tags for structures
		write(DATA.resolve("tags/worldgen/structure/boss_arenas.json"), "{\"values\":[\"" + NS + ":glowkeeper_sanctum\",\"" + NS + ":umbral_throne\",\"" + NS + ":ember_citadel\"]}");
		write(DATA.resolve("tags/worldgen/biome/has_sanctum.json"), "{\"values\":[\"" + NS + ":lumen_meadows\",\"" + NS + ":crystal_peaks\"]}");
		write(DATA.resolve("tags/worldgen/biome/has_throne.json"), "{\"values\":[\"" + NS + ":umbral_caverns\",\"" + NS + ":void_rift\"]}");
		write(DATA.resolve("tags/worldgen/biome/has_citadel.json"), "{\"values\":[\"#minecraft:is_badlands\",\"minecraft:desert\",\"minecraft:savanna\","
				+ "\"minecraft:savanna_plateau\",\"minecraft:windswept_savanna\",\"minecraft:plains\",\"minecraft:sunflower_plains\",\"#minecraft:is_forest\",\"#minecraft:is_taiga\",\"minecraft:meadow\",\"minecraft:snowy_plains\"]}");
		write(DATA.resolve("tags/worldgen/biome/has_shrine.json"), "{\"values\":[\"#minecraft:is_forest\",\"minecraft:plains\",\"minecraft:meadow\","
				+ "\"#minecraft:is_taiga\",\"minecraft:cherry_grove\",\"minecraft:savanna\"]}");
	}

	static final Map<Path, List<String>> TAGS = new LinkedHashMap<>();

	/** Collects tag entries; all tags are written at the end so later updates can add to them. */
	static void tag(Path path, String... ids) {
		List<String> list = TAGS.computeIfAbsent(path, k -> new ArrayList<>());
		for (String id : ids) if (!list.contains(id)) list.add(id);
	}

	static void flushTags() throws IOException {
		for (Map.Entry<Path, List<String>> e : TAGS.entrySet()) {
			if (e.getValue().isEmpty()) continue;
			StringBuilder sb = new StringBuilder("{\"replace\":false,\"values\":[");
			for (int i = 0; i < e.getValue().size(); i++) sb.append(i > 0 ? "," : "").append(q(NS + ":" + e.getValue().get(i)));
			write(e.getKey(), sb.append("]}").toString());
		}
	}

	// ================================================================ worldgen
	static void worldgen() throws IOException {
		// ---- dimension types
		write(DATA.resolve("dimension_type/lumen_skies.json"), """
				{
				  "ambient_light": 0.15,
				  "attributes": {
				    "minecraft:visual/sky_color": "#8fd6ff",
				    "minecraft:visual/fog_color": "#c8b8ff",
				    "minecraft:visual/cloud_color": "#ccf0f8ff",
				    "minecraft:visual/cloud_height": 96.0,
				    "minecraft:gameplay/bed_rule": {"can_set_spawn": "always", "can_sleep": "when_dark", "error_message": {"translate": "block.minecraft.bed.no_sleep"}},
				    "minecraft:gameplay/respawn_anchor_works": false
				  },
				  "coordinate_scale": 1.0,
				  "default_clock": "minecraft:overworld",
				  "has_ceiling": false,
				  "has_ender_dragon_fight": false,
				  "has_skylight": true,
				  "height": 320,
				  "infiniburn": "#minecraft:infiniburn_overworld",
				  "logical_height": 320,
				  "min_y": 0,
				  "monster_spawn_block_light_limit": 0,
				  "monster_spawn_light_level": {"type": "minecraft:uniform", "max_inclusive": 7, "min_inclusive": 0},
				  "timelines": "#minecraft:in_overworld"
				}
				""");
		write(DATA.resolve("dimension_type/umbral_depths.json"), """
				{
				  "ambient_light": 0.25,
				  "attributes": {
				    "minecraft:gameplay/bed_rule": {"can_set_spawn": "never", "can_sleep": "never", "destroy_on_use": true},
				    "minecraft:gameplay/respawn_anchor_works": true,
				    "minecraft:gameplay/sky_light_level": 2.0,
				    "minecraft:visual/ambient_light_color": "#1a0f2e",
				    "minecraft:visual/fog_color": "#120a24",
				    "minecraft:visual/fog_start_distance": 24.0,
				    "minecraft:visual/fog_end_distance": 140.0,
				    "minecraft:visual/sky_light_color": "#6a4aff",
				    "minecraft:visual/sky_light_factor": 0.0,
				    "minecraft:audio/ambient_sounds": {"mood": {"block_search_extent": 8, "offset": 2.0, "sound": "minecraft:ambient.cave", "tick_delay": 3000}}
				  },
				  "cardinal_light": "nether",
				  "coordinate_scale": 1.0,
				  "has_ceiling": true,
				  "has_ender_dragon_fight": false,
				  "has_fixed_time": true,
				  "has_skylight": false,
				  "height": 128,
				  "infiniburn": "#minecraft:infiniburn_nether",
				  "logical_height": 128,
				  "min_y": 0,
				  "monster_spawn_block_light_limit": 15,
				  "monster_spawn_light_level": 11,
				  "skybox": "none",
				  "timelines": "#minecraft:in_nether"
				}
				""");

		// ---- dimensions
		write(DATA.resolve("dimension/lumen_skies.json"), """
				{
				  "type": "glowcube_realms:lumen_skies",
				  "generator": {
				    "type": "minecraft:noise",
				    "settings": "glowcube_realms:lumen_skies",
				    "biome_source": {
				      "type": "minecraft:multi_noise",
				      "biomes": [
				        {"biome": "glowcube_realms:lumen_meadows", "parameters": {"temperature": [-1.0, 0.0], "humidity": [-1.0, 1.0], "continentalness": 0.0, "erosion": 0.0, "weirdness": 0.0, "depth": 0.0, "offset": 0.0}},
				        {"biome": "glowcube_realms:aurora_forest", "parameters": {"temperature": [0.0, 0.45], "humidity": [-1.0, 1.0], "continentalness": 0.0, "erosion": 0.0, "weirdness": 0.0, "depth": 0.0, "offset": 0.0}},
				        {"biome": "glowcube_realms:crystal_peaks", "parameters": {"temperature": [0.45, 1.0], "humidity": [-1.0, 1.0], "continentalness": 0.0, "erosion": 0.0, "weirdness": 0.0, "depth": 0.0, "offset": 0.0}}
				      ]
				    }
				  }
				}
				""");
		write(DATA.resolve("dimension/umbral_depths.json"), """
				{
				  "type": "glowcube_realms:umbral_depths",
				  "generator": {
				    "type": "minecraft:noise",
				    "settings": "glowcube_realms:umbral_depths",
				    "biome_source": {
				      "type": "minecraft:multi_noise",
				      "biomes": [
				        {"biome": "glowcube_realms:umbral_caverns", "parameters": {"temperature": [-1.0, 0.1], "humidity": 0.0, "continentalness": 0.0, "erosion": 0.0, "weirdness": 0.0, "depth": 0.0, "offset": 0.0}},
				        {"biome": "glowcube_realms:shadecap_forest", "parameters": {"temperature": [0.1, 0.5], "humidity": 0.0, "continentalness": 0.0, "erosion": 0.0, "weirdness": 0.0, "depth": 0.0, "offset": 0.0}},
				        {"biome": "glowcube_realms:void_rift", "parameters": {"temperature": [0.5, 1.0], "humidity": 0.0, "continentalness": 0.0, "erosion": 0.0, "weirdness": 0.0, "depth": 0.0, "offset": 0.0}}
				      ]
				    }
				  }
				}
				""");

		// ---- noise settings (derived from the vanilla floating islands and nether generators)
		write(DATA.resolve("worldgen/noise_settings/lumen_skies.json"), LUMEN_NOISE);
		noiseFromVanilla("nether", "umbral_depths", new String[][]{
				{"\"default_block\": \"minecraft:netherrack\"", "\"default_block\": \"glowcube_realms:umbral_stone\""},
				{"\"material_rule\": \"minecraft:nether\"", "\"material_rule\": \"glowcube_realms:umbral_depths\""},
				{"\"sea_level\": 32", "\"sea_level\": 18"}});

		// ---- material rules (surface)
		write(DATA.resolve("worldgen/material_rule/lumen_skies.json"), """
				{
				  "type": "minecraft:sequence",
				  "sequence": [
				    {"type": "minecraft:condition", "if_true": {"type": "minecraft:biome", "biome_is": ["glowcube_realms:crystal_peaks"]},
				      "then_run": {"type": "minecraft:condition", "if_true": "minecraft:on_floor", "then_run": {"type": "minecraft:block", "result_state": "glowcube_realms:skystone_bricks"}}},
				    {"type": "minecraft:condition", "if_true": "minecraft:on_floor", "then_run": {"type": "minecraft:block", "result_state": "glowcube_realms:lumen_grass"}},
				    {"type": "minecraft:condition", "if_true": "minecraft:under_floor", "then_run": {"type": "minecraft:block", "result_state": "glowcube_realms:lumen_soil"}}
				  ]
				}
				""");
		write(DATA.resolve("worldgen/material_rule/umbral_depths.json"), """
				{
				  "type": "minecraft:sequence",
				  "sequence": [
				    "minecraft:bedrock_floor",
				    "minecraft:bedrock_roof",
				    {"type": "minecraft:condition", "if_true": {"type": "minecraft:biome", "biome_is": ["glowcube_realms:void_rift"]},
				      "then_run": {"type": "minecraft:condition", "if_true": "minecraft:on_floor", "then_run": {"type": "minecraft:block", "result_state": "glowcube_realms:umbral_bricks"}}},
				    {"type": "minecraft:condition", "if_true": "minecraft:on_floor", "then_run": {"type": "minecraft:block", "result_state": "glowcube_realms:umbral_moss"}}
				  ]
				}
				""");

		// ---- block features
		feature("glowcrystal_ore", ore(NS + ":glowcrystal_ore", NS + ":skystone", 6));
		feature("voidshard_ore", ore(NS + ":voidshard_ore", NS + ":umbral_stone", 5));
		feature("crystal_spike", "{\"type\":\"" + NS + ":crystal_spike\",\"block\":\"" + NS + ":glowcrystal_block\"}");
		feature("void_spike", "{\"type\":\"" + NS + ":crystal_spike\",\"block\":\"" + NS + ":voidshard_block\"}");
		feature("void_stalactite", "{\"type\":\"" + NS + ":crystal_spike\",\"block\":\"" + NS + ":voidshard_block\",\"hanging\":true}");
		feature("lumen_bloom", "{\"type\":\"minecraft:simple_block\",\"to_place\":{\"id\":\"" + NS + ":lumen_bloom\"}}");
		feature("voidbloom", "{\"type\":\"minecraft:simple_block\",\"to_place\":{\"id\":\"" + NS + ":voidbloom\"}}");
		feature("umbral_rift", "{\"type\":\"" + NS + ":portal_ruin\",\"realm\":\"umbral\"}");
		feature("lumen_gate", "{\"type\":\"" + NS + ":portal_ruin\",\"realm\":\"lumen\"}");
		feature("aurora_tree", tree(NS + ":aurora_log", NS + ":aurora_leaves",
				"{\"type\":\"minecraft:fancy_trunk_placer\",\"base_height\":8,\"height_rand_a\":6,\"height_rand_b\":2}",
				"{\"type\":\"minecraft:fancy_foliage_placer\",\"height\":4,\"offset\":4,\"radius\":2}",
				"{\"type\":\"minecraft:two_layers_feature_size\",\"limit\":0,\"min_clipped_height\":4,\"upper_size\":0}"));
		feature("giant_aurora_tree", tree(NS + ":aurora_log", NS + ":aurora_leaves",
				"{\"type\":\"minecraft:mega_jungle_trunk_placer\",\"base_height\":16,\"height_rand_a\":6,\"height_rand_b\":14}",
				"{\"type\":\"minecraft:jungle_foliage_placer\",\"height\":3,\"offset\":0,\"radius\":4}",
				"{\"type\":\"minecraft:two_layers_feature_size\",\"lower_size\":1,\"upper_size\":2}"));
		feature("giant_oak", tree("minecraft:oak_log", "minecraft:oak_leaves",
				"{\"type\":\"minecraft:fancy_trunk_placer\",\"base_height\":16,\"height_rand_a\":10,\"height_rand_b\":4}",
				"{\"type\":\"minecraft:fancy_foliage_placer\",\"height\":5,\"offset\":4,\"radius\":3}",
				"{\"type\":\"minecraft:two_layers_feature_size\",\"limit\":0,\"min_clipped_height\":6,\"upper_size\":0}"));
		feature("giant_spruce", tree("minecraft:spruce_log", "minecraft:spruce_leaves",
				"{\"type\":\"minecraft:giant_trunk_placer\",\"base_height\":22,\"height_rand_a\":8,\"height_rand_b\":12}",
				"{\"type\":\"minecraft:mega_pine_foliage_placer\",\"crown_height\":{\"type\":\"minecraft:uniform\",\"max_inclusive\":17,\"min_inclusive\":13},\"offset\":0,\"radius\":0}",
				"{\"type\":\"minecraft:two_layers_feature_size\",\"lower_size\":1,\"upper_size\":2}"));
		feature("huge_shadecap", "{\"type\":\"minecraft:huge_red_mushroom\",\"can_place_on\":{\"type\":\"minecraft:matching_block_tag\",\"tag\":\"minecraft:dirt\"},"
				+ "\"cap_provider\":{\"id\":\"" + NS + ":shadecap_block\"},\"stem_provider\":{\"id\":\"" + NS + ":shadecap_stem\",\"properties\":{\"axis\":\"y\"}}}");

		// ---- placed features
		placed("glowcrystal_ore", NS + ":glowcrystal_ore", count(12) + "," + inSquare() + "," + heightRange(0, 220) + "," + biome());
		placed("voidshard_ore", NS + ":voidshard_ore", count(10) + "," + inSquare() + "," + heightRange(4, 120) + "," + biome());
		placed("crystal_spikes_rare", NS + ":crystal_spike", rarity(4) + "," + inSquare() + "," + heightmap("WORLD_SURFACE_WG") + "," + biome());
		placed("crystal_spikes_dense", NS + ":crystal_spike", count(2) + "," + inSquare() + "," + heightmap("WORLD_SURFACE_WG") + "," + biome());
		placed("void_spikes", NS + ":void_spike", count(3) + "," + inSquare() + "," + heightRange(10, 100) + ","
				+ scanDown() + "," + biome());
		placed("void_stalactites", NS + ":void_stalactite", count(4) + "," + inSquare() + "," + heightRange(30, 120) + ","
				+ scanUp() + "," + biome());
		placed("lumen_blooms", NS + ":lumen_bloom", count(28) + "," + inSquare() + "," + heightmap("MOTION_BLOCKING") + "," + biome() + ","
				+ "{\"type\":\"minecraft:block_predicate_filter\",\"predicate\":{\"type\":\"minecraft:would_survive\",\"state\":\"" + NS + ":lumen_bloom\"}}");
		placed("voidblooms", NS + ":voidbloom", count(40) + "," + inSquare() + "," + heightRange(8, 110) + ","
				+ scanDown() + "," + biome());
		placed("aurora_trees_sparse", NS + ":aurora_tree", count(1) + "," + inSquare() + "," + heightmap("MOTION_BLOCKING") + "," + biome() + "," + survive(NS + ":lumen_bloom"));
		placed("aurora_trees_dense", NS + ":aurora_tree", count(6) + "," + inSquare() + "," + heightmap("MOTION_BLOCKING") + "," + biome() + "," + survive(NS + ":lumen_bloom"));
		placed("giant_aurora_trees", NS + ":giant_aurora_tree", count(2) + "," + inSquare() + "," + heightmap("MOTION_BLOCKING") + "," + biome() + "," + survive(NS + ":lumen_bloom"));
		placed("huge_shadecaps", NS + ":huge_shadecap", count(6) + "," + inSquare() + "," + heightRange(10, 100) + ","
				+ scanDown() + "," + biome());
		placed("giant_oak", NS + ":giant_oak", rarity(5) + "," + inSquare() + "," + heightmap("OCEAN_FLOOR") + "," + biome() + "," + survive("minecraft:oak_sapling"));
		placed("giant_spruce", NS + ":giant_spruce", rarity(4) + "," + inSquare() + "," + heightmap("OCEAN_FLOOR") + "," + biome() + "," + survive("minecraft:spruce_sapling"));
		placed("umbral_rift", NS + ":umbral_rift", rarity(70) + "," + inSquare() + "," + heightRange(-54, 10) + "," + biome());
		placed("lumen_gate", NS + ":lumen_gate", rarity(260) + "," + inSquare() + "," + heightmap("WORLD_SURFACE_WG") + "," + biome());

		// ---- biomes
		biome("lumen_meadows", "#8fd6ff", "#c8b8ff", "#46dcc0", "#5cf2dd", 0.7, true,
				new String[]{"glowcrystal_ore"}, new String[]{"lumen_blooms", "aurora_trees_sparse", "crystal_spikes_rare"},
				spawn("monster", NS + ":crystal_golem", 100, 1, 2), spawn("creature", NS + ":glow_wisp", 60, 2, 4),
				spawn("creature", NS + ":glimmer_deer", 50, 2, 4), spawn("creature", NS + ":cloud_bunny", 60, 2, 3));
		biome("aurora_forest", "#a0c8ff", "#e0b0ff", "#b558d4", "#d681ea", 0.8, true,
				new String[]{"glowcrystal_ore"}, new String[]{"giant_aurora_trees", "aurora_trees_dense", "lumen_blooms"},
				spawn("monster", NS + ":crystal_golem", 70, 1, 2), spawn("creature", NS + ":glow_wisp", 90, 2, 5),
				spawn("creature", NS + ":glimmer_deer", 70, 2, 4));
		biome("crystal_peaks", "#b8f0ff", "#ffffff", "#7ff5da", "#9fe8ff", 0.5, true,
				new String[]{"glowcrystal_ore"}, new String[]{"crystal_spikes_dense", "lumen_blooms"},
				spawn("monster", NS + ":crystal_golem", 120, 1, 3), spawn("creature", NS + ":glow_wisp", 40, 1, 3),
				spawn("creature", NS + ":cloud_bunny", 50, 2, 4));
		biome("umbral_caverns", "#1a0f2e", "#1a0f2e", "#15504f", "#27857a", 0.0, false,
				new String[]{"voidshard_ore"}, new String[]{"voidblooms", "void_stalactites", "void_spikes"},
				spawn("monster", NS + ":shade_crawler", 100, 1, 3), spawn("monster", "minecraft:skeleton", 50, 1, 3), spawn("monster", "minecraft:spider", 40, 1, 2), spawn("monster", "minecraft:zombie", 50, 2, 4), spawn("monster", "minecraft:cave_spider", 20, 1, 3), spawn("monster", "minecraft:enderman", 8, 1, 1),
				spawn("creature", NS + ":shade_toad", 60, 2, 4), spawn("ambient", NS + ":lantern_bug", 80, 2, 5));
		biome("shadecap_forest", "#2a1440", "#2a1440", "#58287e", "#70369a", 0.0, false,
				new String[]{"voidshard_ore"}, new String[]{"huge_shadecaps", "voidblooms", "void_stalactites"},
				spawn("monster", NS + ":shade_crawler", 90, 1, 3), spawn("monster", "minecraft:skeleton", 40, 1, 2), spawn("monster", "minecraft:spider", 50, 1, 2), spawn("monster", "minecraft:witch", 5, 1, 1),
				spawn("creature", NS + ":shade_toad", 50, 2, 3), spawn("ambient", NS + ":lantern_bug", 100, 3, 6));
		biome("void_rift", "#0a0418", "#0a0418", "#2a0a4a", "#7225b8", 0.0, false,
				new String[]{"voidshard_ore"}, new String[]{"void_spikes", "void_stalactites"},
				spawn("monster", NS + ":shade_crawler", 120, 2, 4), spawn("monster", "minecraft:enderman", 20, 1, 2));

		// ---- structures
		structure("glowkeeper_sanctum", "glowkeeper", "#" + NS + ":has_sanctum", "surface_structures", 150);
		structure("umbral_throne", "umbral_tyrant", "#" + NS + ":has_throne", "underground_decoration", 40);
		structure("ember_citadel", "ember_warden", "#" + NS + ":has_citadel", "surface_structures", null);
		structure("glowcube_shrine", "shrine", "#" + NS + ":has_shrine", "surface_structures", null);
		structureSet("glowkeeper_sanctum", 22, 9, 7731201);
		structureSet("umbral_throne", 20, 8, 9918273);
		structureSet("ember_citadel", 30, 12, 4471929);
		structureSet("glowcube_shrine", 24, 10, 1239871);
	}

	/** Floating islands: 3D blob noise, masked by a 2D island noise and squeezed into a height band. */
	static final String LUMEN_NOISE = """
			{
			  "default_block": "glowcube_realms:skystone",
			  "default_fluid": "minecraft:water",
			  "disable_mob_generation": false,
			  "legacy_random_source": false,
			  "material_rule": "glowcube_realms:lumen_skies",
			  "noise": {"height": 256, "min_y": 0},
			  "noise_router": {
			    "chunk_surface_level": 0.0,
			    "continents": 0.0,
			    "depth": 0.0,
			    "erosion": 0.0,
			    "ridges": 0.0,
			    "temperature": {"type": "minecraft:noise", "noise": "minecraft:nether/temperature", "xz_scale": 0.2, "y_scale": 0.0},
			    "vegetation": {"type": "minecraft:noise", "noise": "minecraft:nether/vegetation", "xz_scale": 0.2, "y_scale": 0.0},
			    "final_density": FINAL_DENSITY
			  },
			  "sea_level": 0,
			  "spawn_target": []
			}
			""".replace("FINAL_DENSITY", islands());

	/** One island layer: 2D mask + 3D roughness, solid below "top" with a long cone taper down to "bottom". */
	static String layer(String maskNoise, double maskScale, double maskMul, double bias, int bottom, int top) {
		return "{\"type\":\"minecraft:add\",\"left\":{\"type\":\"minecraft:mul\",\"left\":" + maskMul + ",\"right\":{\"type\":\"minecraft:noise\",\"noise\":\"" + maskNoise
				+ "\",\"xz_scale\":" + maskScale + ",\"y_scale\":0.0}},\"right\":{\"type\":\"minecraft:add\",\"left\":{\"type\":\"minecraft:mul\",\"left\":0.3,\"right\":"
				+ "{\"type\":\"minecraft:old_blended_noise\",\"smear_scale_multiplier\":6.0,\"xz_factor\":110.0,\"xz_scale\":0.22,\"y_factor\":90.0,\"y_scale\":0.3}},"
				+ "\"right\":{\"type\":\"minecraft:add\",\"left\":" + bias + ",\"right\":{\"type\":\"minecraft:add\",\"left\":{\"type\":\"minecraft:gradient\",\"axis\":\"y\",\"from_coordinate\":"
				+ bottom + ",\"from_value\":-0.42,\"to_coordinate\":" + top + ",\"to_value\":0.0},\"right\":{\"type\":\"minecraft:add\",\"left\":{\"type\":\"minecraft:gradient\",\"axis\":\"y\",\"from_coordinate\":" + top
				+ ",\"from_value\":0.0,\"to_coordinate\":" + (top + 14) + ",\"to_value\":-3.0},\"right\":{\"type\":\"minecraft:gradient\",\"axis\":\"y\",\"from_coordinate\":" + (bottom - 12)
				+ ",\"from_value\":-3.0,\"to_coordinate\":" + bottom + ",\"to_value\":0.0}}}}}}";
	}

	static String islands() {
		String main = layer("minecraft:nether/vegetation", 1.4, 1.1, -0.40, 40, 128);
		String high = layer("minecraft:nether/temperature", 2.4, 1.0, -0.52, 120, 186);
		String low = layer("minecraft:temperature", 1.1, 1.0, -0.50, 20, 84);
		return "{\"type\":\"minecraft:squeeze\",\"input\":{\"type\":\"minecraft:interpolated\",\"cell_size_xz\":4,\"cell_size_y\":4,\"input\":{\"type\":\"minecraft:max\",\"left\":"
				+ main + ",\"right\":{\"type\":\"minecraft:max\",\"left\":" + high + ",\"right\":" + low + "}}}}";
	}

	static void noiseFromVanilla(String vanilla, String out, String[][] replacements) throws IOException {
		Path jar = Path.of(System.getProperty("user.home"), ".gradle/caches/fabric-loom/26.3/minecraft-common.jar");
		try (java.util.zip.ZipFile zip = new java.util.zip.ZipFile(jar.toFile())) {
			String json = new String(zip.getInputStream(zip.getEntry("data/minecraft/worldgen/noise_settings/" + vanilla + ".json")).readAllBytes(), StandardCharsets.UTF_8);
			for (String[] r : replacements) {
				String before = json;
				json = json.replace(r[0], r[1]);
				if (before.equals(json)) throw new IllegalStateException("pattern not found: " + r[0]);
			}
			write(DATA.resolve("worldgen/noise_settings/" + out + ".json"), json);
		}
	}

	static String ore(String ore, String target, int size) {
		return "{\"type\":\"minecraft:ore\",\"discard_chance_on_air_exposure\":0.0,\"size\":" + size + ",\"targets\":[{\"state\":\"" + ore + "\",\"target\":{\"predicate_type\":\"minecraft:block_match\",\"block\":\"" + target + "\"}}]}";
	}

	static String tree(String log, String leaves, String trunk, String foliage, String size) {
		return "{\"type\":\"minecraft:tree\",\"below_trunk_provider\":\"minecraft:soil_beneath_tree\",\"decorators\":[],\"foliage_placer\":" + foliage
				+ ",\"foliage_provider\":{\"id\":\"" + leaves + "\",\"properties\":{\"distance\":\"7\",\"persistent\":\"false\",\"waterlogged\":\"false\"}},"
				+ "\"ignore_vines\":true,\"minimum_size\":" + size + ",\"trunk_placer\":" + trunk + ",\"trunk_provider\":{\"id\":\"" + log
				+ "\",\"properties\":{\"axis\":\"y\"}}}";
	}

	static void feature(String name, String json) throws IOException {
		write(DATA.resolve("worldgen/feature/" + name + ".json"), json);
	}

	static void placed(String name, String feature, String placement) throws IOException {
		write(DATA.resolve("worldgen/placed_feature/" + name + ".json"), "{\"feature\":\"" + feature + "\",\"placement\":[" + placement + "]}");
	}

	static String scanDown() {
		return "{\"type\":\"minecraft:environment_scan\",\"allowed_search_condition\":{\"type\":\"minecraft:matching_block_tag\",\"tag\":\"minecraft:air\"},\"direction_of_search\":\"down\",\"max_steps\":16,\"target_condition\":{\"type\":\"minecraft:has_sturdy_face\",\"direction\":\"up\"}},{\"type\":\"minecraft:offset\",\"x\":0,\"y\":1,\"z\":0}";
	}

	static String scanUp() {
		return "{\"type\":\"minecraft:environment_scan\",\"allowed_search_condition\":{\"type\":\"minecraft:matching_block_tag\",\"tag\":\"minecraft:air\"},\"direction_of_search\":\"up\",\"max_steps\":16,\"target_condition\":{\"type\":\"minecraft:has_sturdy_face\",\"direction\":\"down\"}},{\"type\":\"minecraft:offset\",\"x\":0,\"y\":-1,\"z\":0}";
	}

	static String count(int n) {
		return "{\"type\":\"minecraft:count\",\"count\":" + n + "}";
	}

	static String rarity(int n) {
		return "{\"type\":\"minecraft:rarity_filter\",\"chance\":" + n + "}";
	}

	static String inSquare() {
		return "{\"type\":\"minecraft:in_square\"}";
	}

	static String biome() {
		return "{\"type\":\"minecraft:biome\"}";
	}

	static String heightmap(String type) {
		return "{\"type\":\"minecraft:heightmap\",\"heightmap\":\"" + type + "\"}";
	}

	static String heightRange(int min, int max) {
		return "{\"type\":\"minecraft:height_range\",\"height\":{\"type\":\"minecraft:uniform\",\"max_inclusive\":{\"absolute\":" + max
				+ "},\"min_inclusive\":{\"absolute\":" + min + "}}}";
	}

	static String survive(String block) {
		return "{\"type\":\"minecraft:block_predicate_filter\",\"predicate\":{\"type\":\"minecraft:would_survive\",\"state\":\"" + block + "\"}}";
	}

	static String spawn(String category, String type, int weight, int min, int max) {
		return category + "|{\"type\":\"" + type + "\",\"count\":{\"type\":\"minecraft:uniform\",\"max_inclusive\":" + max + ",\"min_inclusive\":" + min
				+ "},\"weight\":" + weight + "}";
	}

	static void biome(String name, String sky, String fog, String grass, String foliage, double downfall, boolean precipitation,
			String[] ores, String[] vegetation, String... spawns) throws IOException {
		Map<String, List<String>> byCat = new LinkedHashMap<>();
		for (String s : spawns) {
			String[] parts = s.split("\\|", 2);
			byCat.computeIfAbsent(parts[0], k -> new ArrayList<>()).add(parts[1]);
		}
		StringBuilder spawnJson = new StringBuilder();
		int c = 0;
		for (Map.Entry<String, List<String>> e : byCat.entrySet()) {
			spawnJson.append(c++ > 0 ? "," : "").append(q(e.getKey())).append(":[").append(String.join(",", e.getValue())).append("]");
		}
		String oreList = joinPlaced(ores), vegList = joinPlaced(vegetation);
		String json = "{\"attributes\":{\"minecraft:gameplay/natural_mob_spawns\":{\"argument\":{\"spawn_costs\":{},\"spawns_by_category\":{" + spawnJson
				+ "}},\"modifier\":\"overlay\"},\"minecraft:visual/sky_color\":\"" + sky + "\",\"minecraft:visual/fog_color\":\"" + fog + "\","
				+ "\"minecraft:visual/water_fog_color\":\"" + fog + "\"},"
				+ "\"carvers\":[],\"downfall\":" + downfall + ",\"effects\":{\"foliage_color\":\"" + foliage + "\",\"grass_color\":\"" + grass
				+ "\",\"water_color\":\"#5cdfff\"},\"features\":[[],[],[],[],[],[],[" + oreList + "],[],[],[" + vegList + "],[]],\"has_precipitation\":" + precipitation
				+ ",\"temperature\":0.7}";
		write(DATA.resolve("worldgen/biome/" + name + ".json"), json);
	}

	static final List<String> FEATURE_ORDER = List.of("giant_aurora_trees", "aurora_trees_dense", "aurora_trees_sparse", "crystal_spikes_dense",
			"crystal_spikes_rare", "lumen_blooms", "huge_shadecaps", "void_spikes", "void_stalactites", "voidblooms", "glowcrystal_ore", "voidshard_ore");

	static String joinPlaced(String[] names) {
		names = names.clone();
		java.util.Arrays.sort(names, java.util.Comparator.comparingInt(FEATURE_ORDER::indexOf));
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < names.length; i++) sb.append(i > 0 ? "," : "").append(q(NS + ":" + names[i]));
		return sb.toString();
	}

	static void structure(String name, String kind, String biomes, String step, Integer fixedY) throws IOException {
		write(DATA.resolve("worldgen/structure/" + name + ".json"), "{\"type\":\"" + NS + ":arena\",\"biomes\":\"" + biomes + "\",\"kind\":\"" + kind + "\","
				+ (fixedY != null ? "\"fixed_y\":" + fixedY + "," : "") + "\"spawn_overrides\":{},\"step\":\"" + step + "\",\"terrain_adaptation\":\"none\"}");
	}

	static void structureSet(String name, int spacing, int separation, int salt) throws IOException {
		write(DATA.resolve("worldgen/structure_set/" + name + ".json"), "{\"placement\":{\"type\":\"minecraft:random_spread\",\"salt\":" + salt
				+ ",\"separation\":" + separation + ",\"spacing\":" + spacing + "},\"structures\":[{\"structure\":\"" + NS + ":" + name + "\",\"weight\":1}]}");
	}

	static void splashes() throws IOException {
		String s = String.join("\n",
				"Made by Glowcube!", "Glowcube's Realms!", "Now with three bosses!", "Beware the Glowkeeper!", "The Umbral Tyrant awaits...",
				"Fly higher than the clouds!", "Crystal Bow go brrr", "Have you found a Lumen Gate?", "Glowcube approved!", "Shadow Step!",
				"Supernova incoming!", "Check your minimap!", "Press M for the world map!", "Guards protect the village!", "Radiant!",
				"Floating islands!", "Giant trees everywhere!", "26.3 edition!", "Glow up!", "Don't fall into the void... or do?");
		write(Path.of("src/main/resources/assets/minecraft/texts/splashes.txt"), s + "\n");
	}

	// ================================================================ helpers
	static String q(String s) {
		return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
	}

	static void write(Path p, String content) throws IOException {
		Files.createDirectories(p.getParent());
		Files.writeString(p, content, StandardCharsets.UTF_8);
	}

	// ================================================================ UPDATE 2
	static class Update2 {
		static final String[] GENERATED = {"magma_core", "herald_eye", "frost_heart", "storm_feather", "hollow_crown", "colossus_sigil", "herald_sigil",
				"lich_sigil", "drake_sigil", "king_sigil", "chrono_hourglass", "aurora_charm", "phoenix_feather", "void_pearl", "warp_crystal",
				"magnet_charm", "backpack", "starmetal_helmet", "starmetal_chestplate", "starmetal_leggings", "starmetal_boots", "lumen_berries",
				"aurora_fruit", "shadecap_stew", "starfruit_pie", "ember_pepper", "infernal_colossus_spawn_egg", "void_herald_spawn_egg",
				"frost_lich_spawn_egg", "tempest_drake_spawn_egg", "hollow_king_spawn_egg"};
		static final String[] HANDHELD = {"infernal_maul", "void_reaver", "frostbite_blade", "thunder_spear", "bone_scepter", "sky_pike", "meteor_staff",
				"gale_fan", "grappling_hook", "excavator_pickaxe", "lumber_axe"};
		static final String[][] ALTARS = {{"colossus_altar", "minecraft:block/blackstone"}, {"herald_altar", "minecraft:block/end_stone_bricks"},
				{"lich_altar", "minecraft:block/packed_ice"}, {"drake_altar", NS + ":block/skystone_bricks"}, {"king_altar", "minecraft:block/deepslate_tiles"}};

		static void all() throws IOException {
			assets();
			loot();
			recipes();
			tags();
			worldgen();
		}

		static void assets() throws IOException {
			for (String i : GENERATED) {
				write(ASSETS.resolve("models/item/" + i + ".json"), "{\"parent\":\"minecraft:item/generated\",\"textures\":{\"layer0\":\"" + NS + ":item/" + i + "\"}}");
				itemDef(i, NS + ":item/" + i);
			}
			for (String i : HANDHELD) {
				write(ASSETS.resolve("models/item/" + i + ".json"), "{\"parent\":\"minecraft:item/handheld\",\"textures\":{\"layer0\":\"" + NS + ":item/" + i + "\"}}");
				itemDef(i, NS + ":item/" + i);
			}
			for (String s : new String[]{"storm_bow", "storm_bow_pulling_0", "storm_bow_pulling_1", "storm_bow_pulling_2"}) {
				write(ASSETS.resolve("models/item/" + s + ".json"), "{\"parent\":\"minecraft:item/bow\",\"textures\":{\"layer0\":\"" + NS + ":item/" + s + "\"}}");
			}
			write(ASSETS.resolve("items/storm_bow.json"), "{\"model\":{\"type\":\"minecraft:condition\",\"on_false\":{\"type\":\"minecraft:model\",\"model\":\"" + NS
					+ ":item/storm_bow\"},\"on_true\":{\"type\":\"minecraft:range_dispatch\",\"entries\":[{\"model\":{\"type\":\"minecraft:model\",\"model\":\"" + NS
					+ ":item/storm_bow_pulling_1\"},\"threshold\":0.65},{\"model\":{\"type\":\"minecraft:model\",\"model\":\"" + NS
					+ ":item/storm_bow_pulling_2\"},\"threshold\":0.9}],\"fallback\":{\"type\":\"minecraft:model\",\"model\":\"" + NS
					+ ":item/storm_bow_pulling_0\"},\"property\":\"minecraft:use_duration\",\"scale\":0.05},\"property\":\"minecraft:using_item\"}}");
			for (String[] a : ALTARS) {
				write(ASSETS.resolve("models/block/" + a[0] + ".json"), "{\"parent\":\"minecraft:block/cube_bottom_top\",\"textures\":{\"top\":\"" + NS + ":block/" + a[0]
						+ "_top\",\"side\":\"" + NS + ":block/" + a[0] + "_side\",\"bottom\":\"" + a[1] + "\"}}");
				write(ASSETS.resolve("blockstates/" + a[0] + ".json"), "{\"variants\":{\"enabled=false\":{\"model\":\"" + NS + ":block/" + a[0] + "\"},\"enabled=true\":{\"model\":\""
						+ NS + ":block/" + a[0] + "\"}}}");
				itemDef(a[0], NS + ":block/" + a[0]);
			}
			write(ASSETS.resolve("equipment/starmetal.json"), "{\"layers\":{\"humanoid\":[{\"texture\":\"" + NS + ":starmetal\"}],\"humanoid_leggings\":[{\"texture\":\""
					+ NS + ":starmetal\"}]}}");
		}

		static void lang(Map<String, String[]> t) {
			b(t, "colossus_altar", "Colossus Altar", "Altar des Kolosses");
			b(t, "herald_altar", "Herald Altar", "Altar des Herolds");
			b(t, "lich_altar", "Lich Altar", "Altar des Lichs");
			b(t, "drake_altar", "Drake Altar", "Altar des Drachen");
			b(t, "king_altar", "Hollow King Altar", "Altar des Hohlen Königs");
			e(t, "infernal_colossus", "The Infernal Colossus", "Der Infernale Koloss");
			e(t, "void_herald", "The Void Herald", "Der Leerenherold");
			e(t, "frost_lich", "The Frost Lich", "Der Frostlich");
			e(t, "tempest_drake", "The Tempest Drake", "Der Sturmdrache");
			e(t, "hollow_king", "The Hollow King", "Der Hohle König");
			i(t, "magma_core", "Magma Core", "Magmakern", "Dropped by the Infernal Colossus.", "Droppt vom Infernalen Koloss.");
			i(t, "herald_eye", "Herald's Eye", "Auge des Herolds", "Dropped by the Void Herald.", "Droppt vom Leerenherold.");
			i(t, "frost_heart", "Frost Heart", "Frostherz", "Dropped by the Frost Lich.", "Droppt vom Frostlich.");
			i(t, "storm_feather", "Storm Feather", "Sturmfeder", "Dropped by the Tempest Drake.", "Droppt vom Sturmdrachen.");
			i(t, "hollow_crown", "Hollow Crown", "Hohle Krone", "Dropped by the Hollow King.", "Droppt vom Hohlen König.");
			for (String[] s : new String[][]{{"colossus", "Colossus", "des Kolosses"}, {"herald", "Herald", "des Herolds"}, {"lich", "Lich", "des Lichs"},
					{"drake", "Drake", "des Drachen"}, {"king", "Hollow King", "des Hohlen Königs"}}) {
				i(t, s[0] + "_sigil", s[1] + " Sigil", "Siegel " + s[2], "Use on its altar to summon the boss again.", "Am Altar benutzen, um den Boss erneut zu rufen.");
			}
			i(t, "infernal_maul", "Infernal Maul", "Infernaler Hammer", "Right-click: Eruption. Left-click into the air: Magma Wave.",
					"Rechtsklick: Eruption. Linksklick in die Luft: Magmawelle.");
			i(t, "void_reaver", "Void Reaver", "Leerenschnitter", "Right-click: Blink. Left-click into the air: Void Orb.",
					"Rechtsklick: Blinzeln (Teleport). Linksklick in die Luft: Leerenkugel.");
			i(t, "frostbite_blade", "Frostbite Blade", "Frostbiss-Klinge", "Freezes on hit. Right-click: Frost Nova.", "Friert bei Treffern ein. Rechtsklick: Frostnova.");
			i(t, "thunder_spear", "Thunder Spear", "Donnerspeer", "Right-click: lightning where you look. Hits may call lightning.",
					"Rechtsklick: Blitz dort, wo du hinschaust. Treffer rufen manchmal Blitze.");
			i(t, "bone_scepter", "Bone Scepter", "Knochenzepter", "Right-click: fang line. Left-click into the air: wither skull.",
					"Rechtsklick: Fangzahn-Linie. Linksklick in die Luft: Witherschädel.");
			i(t, "storm_bow", "Storm Bow", "Sturmbogen", "Arrows call lightning. Sneak + right-click: triple volley.", "Pfeile rufen Blitze. Schleichen + Rechtsklick: Dreifachschuss.");
			i(t, "sky_pike", "Sky Pike", "Himmelslanze", "Right-click: lunge forward through enemies.", "Rechtsklick: Sturmangriff nach vorne durch Gegner.");
			i(t, "meteor_staff", "Meteor Staff", "Meteorstab", "Right-click: meteor shower where you look.", "Rechtsklick: Meteorregen dort, wo du hinschaust.");
			i(t, "gale_fan", "Gale Fan", "Sturmfächer", "Right-click: gust that blows enemies away. Sneak: jump high.",
					"Rechtsklick: Windstoß, der Gegner wegbläst. Schleichen: hoher Sprung.");
			i(t, "chrono_hourglass", "Chrono Hourglass", "Chrono-Sanduhr", "Right-click: slows time for all enemies nearby.", "Rechtsklick: verlangsamt die Zeit für alle Gegner in der Nähe.");
			i(t, "aurora_charm", "Aurora Charm", "Aurora-Amulett", "Right-click: removes bad effects and heals.", "Rechtsklick: entfernt schlechte Effekte und heilt.");
			i(t, "grappling_hook", "Grappling Hook", "Enterhaken", "Right-click: pulls you to the block you look at.", "Rechtsklick: zieht dich zum anvisierten Block.");
			i(t, "phoenix_feather", "Phoenix Feather", "Phönixfeder", "Saves you from death once (in your inventory). Right-click: Phoenix Flames.",
					"Rettet dich einmal vor dem Tod (im Inventar). Rechtsklick: Phönixflammen.");
			i(t, "void_pearl", "Void Pearl", "Leerenperle", "Right-click: instant teleport to where you look.", "Rechtsklick: sofortiger Teleport dorthin, wo du hinschaust.");
			i(t, "warp_crystal", "Warp Crystal", "Warpkristall", "Sneak + right-click: save location. Right-click: warp back (any dimension).",
					"Schleichen + Rechtsklick: Ort speichern. Rechtsklick: zurückwarpen (jede Dimension).");
			i(t, "magnet_charm", "Magnet Charm", "Magnet-Amulett", "Pulls items to you while in your inventory. Right-click: on/off.",
					"Zieht Items zu dir, solange es im Inventar ist. Rechtsklick: an/aus.");
			i(t, "backpack", "Backpack", "Rucksack", "Right-click: 27 extra slots.", "Rechtsklick: 27 zusätzliche Plätze.");
			i(t, "excavator_pickaxe", "Excavator Pickaxe", "Bagger-Spitzhacke", "Mines 3x3. Sneak to mine a single block.", "Baut 3x3 ab. Schleichen für einen einzelnen Block.");
			i(t, "lumber_axe", "Lumber Axe", "Holzfäller-Axt", "Fells whole trees. Sneak to chop a single log.", "Fällt ganze Bäume. Schleichen für einen einzelnen Stamm.");
			i(t, "starmetal_helmet", "Starmetal Helmet", "Sternenmetallhelm", "Set bonus: jump boost, speed, no fall damage.", "Setbonus: Sprungkraft, Tempo, kein Fallschaden.");
			i(t, "starmetal_chestplate", "Starmetal Chestplate", "Sternenmetallbrustpanzer", null, null);
			i(t, "starmetal_leggings", "Starmetal Leggings", "Sternenmetallbeinschutz", null, null);
			i(t, "starmetal_boots", "Starmetal Boots", "Sternenmetallstiefel", null, null);
			i(t, "lumen_berries", "Lumen Berries", "Lumenbeeren", "Night vision.", "Nachtsicht.");
			i(t, "aurora_fruit", "Aurora Fruit", "Aurorafrucht", "Regeneration.", "Regeneration.");
			i(t, "shadecap_stew", "Shadecap Stew", "Schattenkappen-Eintopf", "Strength and night vision.", "Stärke und Nachtsicht.");
			i(t, "starfruit_pie", "Starfruit Pie", "Sternfruchtkuchen", "Absorption and speed.", "Absorption und Tempo.");
			i(t, "ember_pepper", "Ember Pepper", "Glutpfeffer", "Fire resistance and haste.", "Feuerresistenz und Eile.");
			for (String[] egg : new String[][]{{"infernal_colossus", "Infernal Colossus", "Infernaler-Koloss"}, {"void_herald", "Void Herald", "Leerenherold"},
					{"frost_lich", "Frost Lich", "Frostlich"}, {"tempest_drake", "Tempest Drake", "Sturmdrachen"}, {"hollow_king", "Hollow King", "Hohler-König"}}) {
				i(t, egg[0] + "_spawn_egg", egg[1] + " Spawn Egg", egg[2] + "-Spawn-Ei", null, null);
			}
			for (String[] boss : new String[][]{
					{"infernal_colossus", "The Infernal Colossus awakens", "Der Infernale Koloss erwacht", "Heart of the Molten Forge", "Herz der Glutschmiede",
							"Magma rises!", "Das Magma steigt!", "Molten fury", "Geschmolzene Wut"},
					{"void_herald", "The Void Herald appears", "Der Leerenherold erscheint", "Voice of the End", "Stimme des Endes",
							"The void calls!", "Die Leere ruft!", "Void rain", "Leerenregen"},
					{"frost_lich", "The Frost Lich rises", "Der Frostlich erhebt sich", "Lord of the Frozen Crypt", "Herr der Frostgruft",
							"The cold bites!", "Die Kälte beißt!", "Absolute zero", "Absoluter Nullpunkt"},
					{"tempest_drake", "The Tempest Drake descends", "Der Sturmdrache stürzt herab", "Ruler of the Storm Aerie", "Herrscher des Sturmhorsts",
							"Thunder roars!", "Der Donner grollt!", "Eye of the storm", "Auge des Sturms"},
					{"hollow_king", "The Hollow King awakens", "Der Hohle König erwacht", "Undying lord of the crypt", "Unsterblicher Herr der Gruft",
							"Rise, my servants!", "Erhebt euch, Diener!", "The final curse", "Der letzte Fluch"}}) {
				t.put("boss." + NS + "." + boss[0] + ".awakens", new String[]{boss[1], boss[2]});
				t.put("boss." + NS + "." + boss[0] + ".subtitle", new String[]{boss[3], boss[4]});
				t.put("boss." + NS + "." + boss[0] + ".phase2", new String[]{boss[5], boss[6]});
				t.put("boss." + NS + "." + boss[0] + ".phase3", new String[]{boss[7], boss[8]});
				t.put("boss." + NS + "." + boss[0] + ".defeated", new String[]{"VICTORY!", "SIEG!"});
			}
			t.put("message." + NS + ".warp_bound", new String[]{"Warp point saved: %s %s %s", "Warp-Punkt gespeichert: %s %s %s"});
			t.put("message." + NS + ".warp_unbound", new String[]{"No warp point saved yet (sneak + right-click)", "Noch kein Warp-Punkt (Schleichen + Rechtsklick)"});
			t.put("message." + NS + ".magnet_on", new String[]{"Magnet: ON", "Magnet: AN"});
			t.put("message." + NS + ".magnet_off", new String[]{"Magnet: OFF", "Magnet: AUS"});
			t.put("message." + NS + ".phoenix", new String[]{"The Phoenix Feather saved your life!", "Die Phönixfeder hat dein Leben gerettet!"});
			t.put("message." + NS + ".grave", new String[]{"Your items are safe in a grave at %s %s %s (%s)", "Deine Items liegen sicher in einem Grab bei %s %s %s (%s)"});
		}

		static void loot() throws IOException {
			entityLoot("infernal_colossus", new String[][]{{NS + ":magma_core", "1", "1"}, {"minecraft:magma_cream", "4", "8"}, {"minecraft:netherite_scrap", "1", "2"},
					{NS + ":colossus_sigil", "1", "1"}});
			entityLoot("void_herald", new String[][]{{NS + ":herald_eye", "1", "1"}, {"minecraft:ender_pearl", "4", "8"}, {"minecraft:shulker_shell", "2", "4"},
					{NS + ":herald_sigil", "1", "1"}});
			entityLoot("frost_lich", new String[][]{{NS + ":frost_heart", "1", "1"}, {"minecraft:blue_ice", "4", "8"}, {"minecraft:diamond", "2", "4"},
					{NS + ":lich_sigil", "1", "1"}});
			entityLoot("tempest_drake", new String[][]{{NS + ":storm_feather", "1", "1"}, {NS + ":starmetal_ingot", "2", "4"}, {"minecraft:phantom_membrane", "3", "6"},
					{NS + ":drake_sigil", "1", "1"}});
			entityLoot("hollow_king", new String[][]{{NS + ":hollow_crown", "1", "1"}, {"minecraft:wither_skeleton_skull", "1", "1"}, {NS + ":void_shard", "6", "12"},
					{NS + ":king_sigil", "1", "1"}});
			chest("molten_forge", new Object[][]{{"minecraft:gold_ingot", 20, 3, 8}, {"minecraft:netherite_scrap", 6, 1, 2}, {NS + ":ember_pepper", 15, 2, 5},
					{NS + ":phoenix_feather", 3, 1, 1}, {"minecraft:magma_cream", 15, 2, 5}, {NS + ":meteor_staff", 3, 1, 1}}, 4, 7);
			chest("astral_spire", new Object[][]{{"minecraft:ender_pearl", 20, 2, 6}, {NS + ":void_pearl", 6, 1, 1}, {NS + ":warp_crystal", 6, 1, 1},
					{"minecraft:diamond", 10, 1, 3}, {NS + ":chrono_hourglass", 3, 1, 1}, {"minecraft:shulker_shell", 8, 1, 2}}, 4, 7);
			chest("frozen_crypt", new Object[][]{{"minecraft:blue_ice", 20, 2, 6}, {"minecraft:diamond", 10, 1, 3}, {NS + ":aurora_charm", 6, 1, 1},
					{"minecraft:golden_apple", 10, 1, 2}, {NS + ":backpack", 6, 1, 1}, {NS + ":lumen_key", 6, 1, 1}}, 4, 7);
			chest("storm_aerie", new Object[][]{{NS + ":starmetal_ingot", 15, 1, 3}, {NS + ":glow_shard", 25, 3, 8}, {NS + ":gale_fan", 6, 1, 1},
					{NS + ":grappling_hook", 6, 1, 1}, {NS + ":starfruit_pie", 15, 1, 3}, {"minecraft:phantom_membrane", 10, 1, 3}}, 4, 7);
			chest("hollow_crypt", new Object[][]{{"minecraft:bone", 25, 4, 10}, {NS + ":void_shard", 20, 2, 6}, {NS + ":shadecap_stew", 10, 1, 1},
					{NS + ":magnet_charm", 6, 1, 1}, {"minecraft:diamond", 10, 1, 3}, {NS + ":excavator_pickaxe", 3, 1, 1}}, 4, 7);
			chest("infernal_colossus_reward", new Object[][]{{NS + ":infernal_maul", 8, 1, 1}, {"minecraft:netherite_ingot", 6, 1, 2}, {NS + ":phoenix_feather", 6, 1, 1},
					{"minecraft:enchanted_golden_apple", 4, 1, 1}, {NS + ":meteor_staff", 6, 1, 1}}, 3, 5);
			chest("void_herald_reward", new Object[][]{{NS + ":void_reaver", 8, 1, 1}, {NS + ":chrono_hourglass", 6, 1, 1}, {NS + ":warp_crystal", 6, 1, 1},
					{"minecraft:enchanted_golden_apple", 4, 1, 1}, {"minecraft:elytra", 2, 1, 1}}, 3, 5);
			chest("frost_lich_reward", new Object[][]{{NS + ":frostbite_blade", 8, 1, 1}, {NS + ":aurora_charm", 6, 1, 1}, {"minecraft:diamond_block", 6, 1, 2},
					{"minecraft:enchanted_golden_apple", 4, 1, 1}, {NS + ":backpack", 6, 1, 1}}, 3, 5);
			chest("tempest_drake_reward", new Object[][]{{NS + ":thunder_spear", 8, 1, 1}, {NS + ":storm_bow", 6, 1, 1}, {NS + ":starmetal_chestplate", 6, 1, 1},
					{NS + ":starmetal_leggings", 6, 1, 1}, {"minecraft:enchanted_golden_apple", 4, 1, 1}}, 3, 5);
			chest("hollow_king_reward", new Object[][]{{NS + ":bone_scepter", 8, 1, 1}, {NS + ":excavator_pickaxe", 6, 1, 1}, {NS + ":lumber_axe", 6, 1, 1},
					{"minecraft:enchanted_golden_apple", 4, 1, 1}, {"minecraft:totem_of_undying", 4, 1, 1}}, 3, 5);
			// aurora leaves also drop berries and fruit
			write(DATA.resolve("loot_table/blocks/aurora_leaves.json"), "{\"type\":\"minecraft:block\",\"pools\":[{\"entries\":[{\"type\":\"minecraft:alternatives\","
					+ "\"children\":[{\"type\":\"minecraft:item\",\"condition\":\"minecraft:tool/can_silk_touch\",\"name\":\"" + NS + ":aurora_leaves\"},"
					+ "{\"type\":\"minecraft:item\",\"condition\":{\"type\":\"minecraft:random_chance\",\"chance\":0.08},\"name\":\"" + NS + ":lumen_berries\"},"
					+ "{\"type\":\"minecraft:item\",\"condition\":{\"type\":\"minecraft:random_chance\",\"chance\":0.04},\"name\":\"" + NS + ":aurora_fruit\"},"
					+ "{\"type\":\"minecraft:item\",\"condition\":{\"type\":\"minecraft:random_chance\",\"chance\":0.05},\"name\":\"" + NS + ":glow_shard\"}]}],\"rolls\":1}],"
					+ "\"random_sequence\":\"" + NS + ":blocks/aurora_leaves\"}");
		}

		static void recipes() throws IOException {
			shaped("infernal_maul", 1, new String[]{"MCM", " R ", " R "}, "M", "minecraft:magma_block", "C", NS + ":magma_core", "R", "minecraft:blaze_rod");
			shaped("void_reaver", 1, new String[]{" V", "E ", "R "}, "V", NS + ":voidshard_block", "E", NS + ":herald_eye", "R", "minecraft:end_rod");
			shaped("frostbite_blade", 1, new String[]{" I ", " H ", " S "}, "I", "minecraft:blue_ice", "H", NS + ":frost_heart", "S", NS + ":glowcrystal_sword");
			shaped("thunder_spear", 1, new String[]{"  F", " S ", "S  "}, "F", NS + ":storm_feather", "S", NS + ":starmetal_ingot");
			shaped("bone_scepter", 1, new String[]{" C ", " B ", " B "}, "C", NS + ":hollow_crown", "B", "minecraft:bone_block");
			shaped("storm_bow", 1, new String[]{" FS", "B S", " FS"}, "F", NS + ":storm_feather", "S", "minecraft:string", "B", NS + ":crystal_bow");
			shaped("sky_pike", 1, new String[]{"  G", " S ", "S  "}, "G", NS + ":glowcrystal_block", "S", "minecraft:stick");
			shaped("meteor_staff", 1, new String[]{" EM", " RE", "R  "}, "E", NS + ":ember_heart", "M", NS + ":magma_core", "R", "minecraft:blaze_rod");
			shaped("gale_fan", 1, new String[]{"WFW", " S ", " S "}, "W", NS + ":wisp_essence", "F", "minecraft:feather", "S", "minecraft:stick");
			shaped("chrono_hourglass", 1, new String[]{"GGG", " E ", "GGG"}, "G", "minecraft:gold_ingot", "E", NS + ":herald_eye");
			shaped("aurora_charm", 1, new String[]{" S ", "SWS", " F "}, "S", "minecraft:string", "W", NS + ":wisp_essence", "F", NS + ":aurora_fruit");
			shaped("grappling_hook", 1, new String[]{" II", " SI", "S  "}, "I", "minecraft:iron_ingot", "S", "minecraft:string");
			shaped("phoenix_feather", 1, new String[]{"BMB", "MFM", "BMB"}, "B", "minecraft:blaze_powder", "M", NS + ":magma_core", "F", "minecraft:feather");
			shaped("void_pearl", 1, new String[]{"VVV", "VEV", "VVV"}, "V", NS + ":void_shard", "E", "minecraft:ender_pearl");
			shaped("warp_crystal", 1, new String[]{" G ", "GEG", " G "}, "G", NS + ":glowcrystal_block", "E", "minecraft:ender_eye");
			shaped("magnet_charm", 1, new String[]{"I I", "R R", " I "}, "I", "minecraft:iron_ingot", "R", "minecraft:redstone");
			shaped("backpack", 1, new String[]{"LSL", "LCL", "LLL"}, "L", "minecraft:leather", "S", "minecraft:string", "C", "minecraft:chest");
			shaped("excavator_pickaxe", 1, new String[]{"VVV", " P ", " S "}, "V", NS + ":voidshard_block", "P", NS + ":voidshard_pickaxe", "S", "minecraft:stick");
			shaped("lumber_axe", 1, new String[]{"GG ", "GA ", " S "}, "G", NS + ":glowcrystal_block", "A", NS + ":glowcrystal_axe", "S", "minecraft:stick");
			armor("starmetal", NS + ":starmetal_ingot");
			shapeless("shadecap_stew", NS + ":shadecap_stew", 1, NS + ":shadecap_block", NS + ":voidbloom", "minecraft:bowl");
			shapeless("starfruit_pie", NS + ":starfruit_pie", 1, NS + ":aurora_fruit", "minecraft:sugar", "minecraft:egg", NS + ":wisp_essence");
			shapeless("ember_pepper", NS + ":ember_pepper", 2, NS + ":lumen_berries", "minecraft:blaze_powder");
			shaped("colossus_sigil", 1, new String[]{"GMG", "MBM", "GMG"}, "G", "minecraft:gold_ingot", "M", "minecraft:magma_cream", "B", "minecraft:magma_block");
			shaped("herald_sigil", 1, new String[]{"PEP", "ESE", "PEP"}, "P", "minecraft:purpur_block", "E", "minecraft:ender_pearl", "S", "minecraft:shulker_shell");
			shaped("lich_sigil", 1, new String[]{"IBI", "BDB", "IBI"}, "I", "minecraft:blue_ice", "B", "minecraft:bone", "D", "minecraft:diamond");
			shaped("drake_sigil", 1, new String[]{"FGF", "GSG", "FGF"}, "F", "minecraft:feather", "G", NS + ":glow_shard", "S", NS + ":starmetal_ingot");
			shaped("king_sigil", 1, new String[]{"BSB", "SGS", "BSB"}, "B", "minecraft:bone_block", "S", NS + ":void_shard", "G", "minecraft:gold_block");
		}

		static void tags() throws IOException {
			tag(MC_DATA.resolve("tags/item/swords.json"), "void_reaver", "frostbite_blade", "thunder_spear", "sky_pike");
			tag(MC_DATA.resolve("tags/item/axes.json"), "infernal_maul", "lumber_axe");
			tag(MC_DATA.resolve("tags/item/pickaxes.json"), "excavator_pickaxe");
			tag(MC_DATA.resolve("tags/item/head_armor.json"), "starmetal_helmet");
			tag(MC_DATA.resolve("tags/item/chest_armor.json"), "starmetal_chestplate");
			tag(MC_DATA.resolve("tags/item/leg_armor.json"), "starmetal_leggings");
			tag(MC_DATA.resolve("tags/item/foot_armor.json"), "starmetal_boots");
			tag(MC_DATA.resolve("tags/item/bow_enchantable.json"), "storm_bow");
			tag(DATA.resolve("tags/item/starmetal_materials.json"), "starmetal_ingot");
			write(DATA.resolve("tags/worldgen/biome/has_forge.json"), "{\"values\":[\"#minecraft:is_nether\"]}");
			write(DATA.resolve("tags/worldgen/biome/has_spire.json"), "{\"values\":[\"minecraft:end_highlands\",\"minecraft:end_midlands\"]}");
			write(DATA.resolve("tags/worldgen/biome/has_frozen_crypt.json"), "{\"values\":[\"minecraft:snowy_plains\",\"minecraft:ice_spikes\",\"minecraft:snowy_taiga\","
					+ "\"minecraft:grove\",\"minecraft:snowy_slopes\",\"minecraft:frozen_peaks\",\"minecraft:jagged_peaks\",\"minecraft:frozen_river\"]}");
			write(DATA.resolve("tags/worldgen/biome/has_aerie.json"), "{\"values\":[\"" + NS + ":lumen_meadows\",\"" + NS + ":aurora_forest\",\"" + NS + ":crystal_peaks\"]}");
			write(DATA.resolve("tags/worldgen/biome/has_hollow_crypt.json"), "{\"values\":[\"" + NS + ":shadecap_forest\",\"" + NS + ":umbral_caverns\"]}");
			write(DATA.resolve("tags/worldgen/structure/boss_arenas.json"), "{\"values\":[\"" + NS + ":glowkeeper_sanctum\",\"" + NS + ":umbral_throne\",\"" + NS
					+ ":ember_citadel\",\"" + NS + ":molten_forge\",\"" + NS + ":astral_spire\",\"" + NS + ":frozen_crypt\",\"" + NS + ":storm_aerie\",\"" + NS + ":hollow_crypt\"]}");
		}

		static void worldgen() throws IOException {
			structure("molten_forge", "infernal_colossus", "#" + NS + ":has_forge", "underground_decoration", 70);
			structure("astral_spire", "void_herald", "#" + NS + ":has_spire", "surface_structures", null);
			structure("frozen_crypt", "frost_lich", "#" + NS + ":has_frozen_crypt", "surface_structures", null);
			structure("storm_aerie", "tempest_drake", "#" + NS + ":has_aerie", "surface_structures", 196);
			structure("hollow_crypt", "hollow_king", "#" + NS + ":has_hollow_crypt", "underground_decoration", 72);
			structureSet("molten_forge", 24, 10, 6619241);
			structureSet("astral_spire", 20, 8, 3317799);
			structureSet("frozen_crypt", 32, 12, 8812345);
			structureSet("storm_aerie", 26, 10, 5550123);
			structureSet("hollow_crypt", 22, 8, 7766551);
		}
	}

	// ================================================================ UPDATE 3: Sculk Realm, creatures, bigger villages
	static class Update3 {
		static final String[] GENERATED = {"sculk_key", "echo_crystal", "echo_heart", "echo_horn", "glimmer_venison", "cooked_glimmer_venison",
				"glimmer_antler", "cloud_fluff", "cloud_bottle", "toad_leg", "glow_jelly", "snail_shell", "sculk_slime", "salamander_scale",
				"salamander_charm", "void_jelly", "echo_warden_spawn_egg", "sculk_stalker_spawn_egg", "glimmer_deer_spawn_egg", "cloud_bunny_spawn_egg",
				"shade_toad_spawn_egg", "lantern_bug_spawn_egg", "sculk_snail_spawn_egg", "ember_salamander_spawn_egg", "void_jelly_spawn_egg"};

		static void all() throws IOException {
			assets();
			loot();
			recipes();
			tags();
			worldgen();
			villages();
		}

		static void cube(String b) throws IOException {
			write(ASSETS.resolve("models/block/" + b + ".json"), "{\"parent\":\"minecraft:block/cube_all\",\"textures\":{\"all\":\"" + NS + ":block/" + b + "\"}}");
			write(ASSETS.resolve("blockstates/" + b + ".json"), "{\"variants\":{\"\":{\"model\":\"" + NS + ":block/" + b + "\"}}}");
			blockItem(b);
		}

		static void assets() throws IOException {
			cube("echo_planks");
			cube("echo_crystal_ore");
			cube("echo_crystal_block");
			write(ASSETS.resolve("models/block/echo_leaves.json"), "{\"parent\":\"minecraft:block/leaves\",\"textures\":{\"all\":\"" + NS + ":block/echo_leaves\"}}");
			write(ASSETS.resolve("blockstates/echo_leaves.json"), "{\"variants\":{\"\":{\"model\":\"" + NS + ":block/echo_leaves\"}}}");
			blockItem("echo_leaves");
			write(ASSETS.resolve("models/block/echo_log.json"), "{\"parent\":\"minecraft:block/cube_column\",\"textures\":{\"end\":\"" + NS + ":block/echo_log_top\",\"side\":\"" + NS + ":block/echo_log\"}}");
			write(ASSETS.resolve("models/block/echo_log_horizontal.json"), "{\"parent\":\"minecraft:block/cube_column_horizontal\",\"textures\":{\"end\":\"" + NS
					+ ":block/echo_log_top\",\"side\":\"" + NS + ":block/echo_log\"}}");
			write(ASSETS.resolve("blockstates/echo_log.json"), "{\"variants\":{\"axis=x\":{\"model\":\"" + NS + ":block/echo_log_horizontal\",\"x\":90,\"y\":90},"
					+ "\"axis=y\":{\"model\":\"" + NS + ":block/echo_log\"},\"axis=z\":{\"model\":\"" + NS + ":block/echo_log_horizontal\",\"x\":90}}}");
			blockItem("echo_log");
			write(ASSETS.resolve("models/block/echo_bloom.json"), "{\"parent\":\"minecraft:block/cross\",\"textures\":{\"cross\":\"" + NS + ":block/echo_bloom\"}}");
			write(ASSETS.resolve("blockstates/echo_bloom.json"), "{\"variants\":{\"\":{\"model\":\"" + NS + ":block/echo_bloom\"}}}");
			write(ASSETS.resolve("models/item/echo_bloom.json"), "{\"parent\":\"minecraft:item/generated\",\"textures\":{\"layer0\":\"" + NS + ":block/echo_bloom\"}}");
			itemDef("echo_bloom", NS + ":item/echo_bloom");
			write(ASSETS.resolve("models/block/sculk_keyhole.json"), "{\"parent\":\"minecraft:block/cube_all\",\"textures\":{\"all\":\"" + NS + ":block/sculk_keyhole\"}}");
			write(ASSETS.resolve("models/block/sculk_keyhole_filled.json"), "{\"parent\":\"minecraft:block/cube_all\",\"textures\":{\"all\":\"" + NS + ":block/sculk_keyhole_filled\"}}");
			write(ASSETS.resolve("blockstates/sculk_keyhole.json"), "{\"variants\":{\"lit=false\":{\"model\":\"" + NS + ":block/sculk_keyhole\"},\"lit=true\":{\"model\":\""
					+ NS + ":block/sculk_keyhole_filled\"}}}");
			blockItem("sculk_keyhole");
			String p = "sculk_portal";
			String tex = "\"textures\":{\"particle\":\"" + NS + ":block/" + p + "\",\"portal\":\"" + NS + ":block/" + p + "\"}";
			write(ASSETS.resolve("models/block/" + p + "_ns.json"), "{" + tex + ",\"elements\":[{\"from\":[0,0,6],\"to\":[16,16,10],\"faces\":{\"north\":{\"uv\":[0,0,16,16],\"texture\":\"#portal\"},\"south\":{\"uv\":[0,0,16,16],\"texture\":\"#portal\"}}}]}");
			write(ASSETS.resolve("models/block/" + p + "_ew.json"), "{" + tex + ",\"elements\":[{\"from\":[6,0,0],\"to\":[10,16,16],\"faces\":{\"east\":{\"uv\":[0,0,16,16],\"texture\":\"#portal\"},\"west\":{\"uv\":[0,0,16,16],\"texture\":\"#portal\"}}}]}");
			write(ASSETS.resolve("blockstates/" + p + ".json"), "{\"variants\":{\"axis=x\":{\"model\":\"" + NS + ":block/" + p + "_ns\"},\"axis=z\":{\"model\":\"" + NS + ":block/" + p + "_ew\"}}}");
			for (String i : GENERATED) {
				write(ASSETS.resolve("models/item/" + i + ".json"), "{\"parent\":\"minecraft:item/generated\",\"textures\":{\"layer0\":\"" + NS + ":item/" + i + "\"}}");
				itemDef(i, NS + ":item/" + i);
			}
			write(ASSETS.resolve("models/item/sonic_blade.json"), "{\"parent\":\"minecraft:item/handheld\",\"textures\":{\"layer0\":\"" + NS + ":item/sonic_blade\"}}");
			itemDef("sonic_blade", NS + ":item/sonic_blade");
		}

		static void lang(Map<String, String[]> t) {
			b(t, "sculk_portal", "Sculk Portal", "Skulk-Portal");
			b(t, "sculk_keyhole", "Sculk Keyhole", "Skulk-Schlüsselloch");
			b(t, "echo_log", "Echo Log", "Echostamm");
			b(t, "echo_planks", "Echo Planks", "Echoholzbretter");
			b(t, "echo_leaves", "Echo Leaves", "Echoblätter");
			b(t, "echo_bloom", "Echo Bloom", "Echoblüte");
			b(t, "echo_crystal_ore", "Echo Crystal Ore", "Echokristallerz");
			b(t, "echo_crystal_block", "Block of Echo Crystal", "Echokristallblock");
			i(t, "sculk_key", "Sculk Key", "Skulk-Schlüssel", "Use on the keyhole of a sculk gate to open the Sculk Realm.",
					"Am Schlüsselloch eines Skulk-Tors benutzen, um die Skulk-Dimension zu öffnen.");
			i(t, "echo_crystal", "Echo Crystal", "Echokristall", null, null);
			i(t, "echo_heart", "Echo Heart", "Echoherz", "Dropped by the Echo Warden.", "Droppt vom Echo Warden.");
			i(t, "sonic_blade", "Sonic Blade", "Schallklinge", "Right-click: sonic boom that ignores armor.", "Rechtsklick: Schallexplosion, die Rüstung ignoriert.");
			i(t, "echo_horn", "Echo Horn", "Echohorn", "Right-click: shriek - weakens and reveals enemies.", "Rechtsklick: Schrei - schwächt und markiert Gegner.");
			i(t, "glimmer_venison", "Raw Glimmer Venison", "Rohes Glimmerhirsch-Fleisch", "Speed.", "Tempo.");
			i(t, "cooked_glimmer_venison", "Cooked Glimmer Venison", "Gebratenes Glimmerhirsch-Fleisch", "Speed and regeneration.", "Tempo und Regeneration.");
			i(t, "glimmer_antler", "Glimmer Antler", "Glimmergeweih", "Crafting material.", "Crafting-Zutat.");
			i(t, "cloud_fluff", "Cloud Fluff", "Wolkenflaum", "Crafting material.", "Crafting-Zutat.");
			i(t, "cloud_bottle", "Cloud in a Bottle", "Wolke in der Flasche", "Right-click: double jump in mid-air.", "Rechtsklick: Doppelsprung in der Luft.");
			i(t, "toad_leg", "Toad Leg", "Krötenschenkel", "Jump boost.", "Sprungkraft.");
			i(t, "glow_jelly", "Glow Jelly", "Leuchtgelee", "Night vision.", "Nachtsicht.");
			i(t, "snail_shell", "Snail Shell", "Schneckenhaus", "Crafting material.", "Crafting-Zutat.");
			i(t, "sculk_slime", "Sculk Slime", "Skulkschleim", "Crafting material.", "Crafting-Zutat.");
			i(t, "salamander_scale", "Salamander Scale", "Salamanderschuppe", "Crafting material.", "Crafting-Zutat.");
			i(t, "salamander_charm", "Salamander Charm", "Salamander-Amulett", "Right-click: fire immunity and strength.", "Rechtsklick: Feuerimmunität und Stärke.");
			i(t, "void_jelly", "Void Jelly", "Leerengelee", "Slow falling and regeneration.", "Sanfter Fall und Regeneration.");
			for (String[] e : new String[][]{{"echo_warden", "The Echo Warden", "Der Echo Warden"}, {"sculk_stalker", "Sculk Stalker", "Skulkpirscher"},
					{"glimmer_deer", "Glimmer Deer", "Glimmerhirsch"}, {"cloud_bunny", "Cloud Bunny", "Wolkenhase"}, {"shade_toad", "Shade Toad", "Schattenkröte"},
					{"lantern_bug", "Lantern Bug", "Laternenkäfer"}, {"sculk_snail", "Sculk Snail", "Skulkschnecke"}, {"ember_salamander", "Ember Salamander", "Glutsalamander"},
					{"void_jelly", "Void Jelly", "Leerenqualle"}}) {
				e(t, e[0], e[1], e[2]);
				i(t, e[0] + "_spawn_egg", e[1].replace("The ", "") + " Spawn Egg", e[2].replace("Der ", "") + "-Spawn-Ei", null, null);
			}
			t.put("boss." + NS + ".echo_warden.awakens", new String[]{"The Echo Warden emerges", "Der Echo Warden erhebt sich"});
			t.put("boss." + NS + ".echo_warden.subtitle", new String[]{"Guardian of the Sculk Key", "Wächter des Skulk-Schlüssels"});
			t.put("boss." + NS + ".echo_warden.phase2", new String[]{"It can hear you!", "Er kann dich hören!"});
			t.put("boss." + NS + ".echo_warden.phase3", new String[]{"Silence...", "Stille..."});
			t.put("boss." + NS + ".echo_warden.defeated", new String[]{"VICTORY!", "SIEG!"});
			t.put("message." + NS + ".keyhole_used", new String[]{"This keyhole already holds a key.", "In diesem Schlüsselloch steckt schon ein Schlüssel."});
			t.put("message." + NS + ".sculk_portal_open", new String[]{"The way to the Sculk Realm is open!", "Der Weg in die Skulk-Dimension ist offen!"});
			for (String[] bio : new String[][]{{"sculk_plains", "Sculk Plains", "Skulkebene"}, {"echo_forest", "Echo Forest", "Echowald"},
					{"sculk_spires", "Sculk Spires", "Skulktürme"}}) {
				t.put("biome." + NS + "." + bio[0], new String[]{bio[1], bio[2]});
			}
		}

		static void loot() throws IOException {
			for (String b : new String[]{"echo_log", "echo_planks", "echo_bloom", "echo_crystal_block"}) {
				write(DATA.resolve("loot_table/blocks/" + b + ".json"), "{\"type\":\"minecraft:block\",\"pools\":[{\"condition\":{\"type\":\"minecraft:survives_explosion\"},"
						+ "\"entries\":[{\"type\":\"minecraft:item\",\"name\":\"" + NS + ":" + b + "\"}],\"rolls\":1}],\"random_sequence\":\"" + NS + ":blocks/" + b + "\"}");
			}
			silkOr("echo_crystal_ore", NS + ":echo_crystal", 1, 3);
			write(DATA.resolve("loot_table/blocks/echo_leaves.json"), "{\"type\":\"minecraft:block\",\"pools\":[{\"entries\":[{\"type\":\"minecraft:alternatives\","
					+ "\"children\":[{\"type\":\"minecraft:item\",\"condition\":\"minecraft:tool/can_silk_touch\",\"name\":\"" + NS + ":echo_leaves\"},"
					+ "{\"type\":\"minecraft:item\",\"condition\":{\"type\":\"minecraft:random_chance\",\"chance\":0.05},\"name\":\"" + NS + ":echo_crystal\"}]}],\"rolls\":1}],"
					+ "\"random_sequence\":\"" + NS + ":blocks/echo_leaves\"}");
			entityLoot("echo_warden", new String[][]{{NS + ":echo_heart", "1", "1"}, {NS + ":echo_crystal", "4", "8"}, {"minecraft:echo_shard", "1", "3"}});
			entityLoot("sculk_stalker", new String[][]{{NS + ":sculk_slime", "0", "2"}, {NS + ":echo_crystal", "0", "1"}});
			entityLoot("glimmer_deer", new String[][]{{NS + ":glimmer_venison", "1", "2"}, {NS + ":glimmer_antler", "0", "1"}});
			entityLoot("cloud_bunny", new String[][]{{NS + ":cloud_fluff", "1", "2"}});
			entityLoot("shade_toad", new String[][]{{NS + ":toad_leg", "1", "1"}});
			entityLoot("lantern_bug", new String[][]{{NS + ":glow_jelly", "1", "2"}});
			entityLoot("sculk_snail", new String[][]{{NS + ":snail_shell", "0", "1"}, {NS + ":sculk_slime", "1", "2"}});
			entityLoot("ember_salamander", new String[][]{{NS + ":salamander_scale", "1", "2"}});
			entityLoot("void_jelly", new String[][]{{NS + ":void_jelly", "1", "2"}});
			// the reliquary always holds the key, plus some treasure
			write(DATA.resolve("loot_table/chests/sculk_reliquary.json"), "{\"type\":\"minecraft:chest\",\"pools\":[{\"entries\":[{\"type\":\"minecraft:item\",\"name\":\""
					+ NS + ":sculk_key\"}],\"rolls\":1},{\"entries\":[{\"type\":\"minecraft:item\",\"modifier\":{\"type\":\"minecraft:set_count\",\"count\":{\"type\":\"minecraft:uniform\","
					+ "\"max\":4,\"min\":1}},\"name\":\"minecraft:echo_shard\",\"weight\":10},{\"type\":\"minecraft:item\",\"modifier\":{\"type\":\"minecraft:set_count\",\"count\":"
					+ "{\"type\":\"minecraft:uniform\",\"max\":6,\"min\":2}},\"name\":\"" + NS + ":echo_crystal\",\"weight\":15},{\"type\":\"minecraft:item\",\"name\":\"minecraft:golden_apple\","
					+ "\"weight\":8},{\"type\":\"minecraft:item\",\"name\":\"minecraft:disc_fragment_5\",\"weight\":6}],\"rolls\":{\"type\":\"minecraft:uniform\",\"max\":4,\"min\":2}}],"
					+ "\"random_sequence\":\"" + NS + ":chests/sculk_reliquary\"}");
			chest("echo_warden_reward", new Object[][]{{NS + ":sonic_blade", 10, 1, 1}, {NS + ":echo_horn", 8, 1, 1}, {NS + ":echo_crystal_block", 10, 1, 3},
					{"minecraft:enchanted_golden_apple", 4, 1, 1}, {"minecraft:echo_shard", 10, 2, 4}}, 3, 5);
		}

		static void recipes() throws IOException {
			shapeless("echo_planks", NS + ":echo_planks", 4, NS + ":echo_log");
			shaped("echo_crystal_block", 1, new String[]{"###", "###", "###"}, "#", NS + ":echo_crystal");
			shaped("sonic_blade", 1, new String[]{" C ", " H ", " S "}, "C", NS + ":echo_crystal_block", "H", NS + ":echo_heart", "S", "minecraft:diamond_sword");
			shaped("echo_horn", 1, new String[]{" A ", "SCS"}, "A", NS + ":glimmer_antler", "S", NS + ":snail_shell", "C", NS + ":echo_crystal");
			shaped("cloud_bottle", 1, new String[]{"FFF", " B "}, "F", NS + ":cloud_fluff", "B", "minecraft:glass_bottle");
			shaped("salamander_charm", 1, new String[]{" S ", "XGX", " X "}, "S", "minecraft:string", "X", NS + ":salamander_scale", "G", "minecraft:gold_ingot");
			shaped("sculk_key", 1, new String[]{" C ", "CHC", " C "}, "C", NS + ":echo_crystal", "H", NS + ":echo_heart");
			write(DATA.resolve("recipe/cooked_glimmer_venison.json"), "{\"type\":\"minecraft:smelting\",\"cookingtime\":200,\"experience\":0.35,"
					+ "\"ingredient\":\"" + NS + ":glimmer_venison\",\"result\":{\"id\":\"" + NS + ":cooked_glimmer_venison\"}}");
			write(DATA.resolve("recipe/cooked_glimmer_venison_from_smoking.json"), "{\"type\":\"minecraft:smoking\",\"cookingtime\":100,\"experience\":0.35,"
					+ "\"ingredient\":\"" + NS + ":glimmer_venison\",\"result\":{\"id\":\"" + NS + ":cooked_glimmer_venison\"}}");
		}

		static void tags() throws IOException {
			tag(MC_DATA.resolve("tags/block/mineable/pickaxe.json"), "echo_crystal_ore", "echo_crystal_block", "sculk_keyhole");
			tag(MC_DATA.resolve("tags/block/mineable/axe.json"), "echo_log", "echo_planks");
			tag(MC_DATA.resolve("tags/block/mineable/hoe.json"), "echo_leaves");
			tag(MC_DATA.resolve("tags/block/needs_iron_tool.json"), "echo_crystal_ore", "echo_crystal_block");
			tag(MC_DATA.resolve("tags/block/logs.json"), "echo_log");
			tag(MC_DATA.resolve("tags/block/logs_that_burn.json"), "echo_log");
			tag(MC_DATA.resolve("tags/block/leaves.json"), "echo_leaves");
			tag(MC_DATA.resolve("tags/block/planks.json"), "echo_planks");
			tag(MC_DATA.resolve("tags/block/small_flowers.json"), "echo_bloom");
			tag(MC_DATA.resolve("tags/item/logs.json"), "echo_log");
			tag(MC_DATA.resolve("tags/item/planks.json"), "echo_planks");
			tag(MC_DATA.resolve("tags/item/leaves.json"), "echo_leaves");
			tag(MC_DATA.resolve("tags/item/swords.json"), "sonic_blade");
			tag(MC_DATA.resolve("tags/item/meat.json"), "glimmer_venison", "cooked_glimmer_venison", "toad_leg");
			write(DATA.resolve("tags/worldgen/biome/has_sculk_sanctuary.json"), "{\"values\":[\"minecraft:deep_dark\"]}");
			write(DATA.resolve("tags/worldgen/structure/boss_arenas.json"), "{\"values\":[\"" + NS + ":glowkeeper_sanctum\",\"" + NS + ":umbral_throne\",\"" + NS
					+ ":ember_citadel\",\"" + NS + ":molten_forge\",\"" + NS + ":astral_spire\",\"" + NS + ":frozen_crypt\",\"" + NS + ":storm_aerie\",\"" + NS
					+ ":hollow_crypt\",\"" + NS + ":sculk_sanctuary\"]}");
		}

		static final String SCULK_TYPE = """
				{
				  "ambient_light": 0.2,
				  "attributes": {
				    "minecraft:audio/ambient_sounds": {"mood": {"block_search_extent": 8, "offset": 2.0, "sound": "minecraft:ambient.cave", "tick_delay": 3000}},
				    "minecraft:gameplay/bed_rule": {"can_set_spawn": "never", "can_sleep": "never", "destroy_on_use": true},
				    "minecraft:gameplay/respawn_anchor_works": true,
				    "minecraft:visual/ambient_light_color": "#1f4a4a",
				    "minecraft:visual/fog_color": "#06181c",
				    "minecraft:visual/sky_color": "#030c10",
				    "minecraft:visual/sky_light_color": "#3ad6d6",
				    "minecraft:visual/sky_light_factor": 0.0,
				    "minecraft:visual/fog_start_distance": 20.0,
				    "minecraft:visual/fog_end_distance": 160.0
				  },
				  "coordinate_scale": 1.0,
				  "default_clock": "minecraft:the_end",
				  "has_ceiling": false,
				  "has_ender_dragon_fight": false,
				  "has_fixed_time": true,
				  "has_skylight": true,
				  "height": 384,
				  "infiniburn": "#minecraft:infiniburn_overworld",
				  "logical_height": 384,
				  "min_y": -64,
				  "monster_spawn_block_light_limit": 0,
				  "monster_spawn_light_level": 15,
				  "skybox": "end",
				  "timelines": "#minecraft:in_end"
				}
				""";

		static String biomeEntry(String biome, String temperature) {
			return "{\"biome\":\"" + NS + ":" + biome + "\",\"parameters\":{\"temperature\":" + temperature + ",\"humidity\":[-1.0,1.0],\"continentalness\":[-1.2,1.2],"
					+ "\"erosion\":[-1.0,1.0],\"weirdness\":[-1.0,1.0],\"depth\":[0.0,1.0],\"offset\":0.0}}";
		}

		static void worldgen() throws IOException {
			write(DATA.resolve("dimension_type/sculk_realm.json"), SCULK_TYPE);
			write(DATA.resolve("dimension/sculk_realm.json"), "{\"type\":\"" + NS + ":sculk_realm\",\"generator\":{\"type\":\"minecraft:noise\",\"settings\":\"" + NS
					+ ":sculk_realm\",\"biome_source\":{\"type\":\"minecraft:multi_noise\",\"biomes\":[" + biomeEntry("sculk_plains", "[-1.0,-0.15]") + ","
					+ biomeEntry("echo_forest", "[-0.15,0.35]") + "," + biomeEntry("sculk_spires", "[0.35,1.0]") + "]}}}");
			noiseFromVanilla("overworld", "sculk_realm", new String[][]{
					{"\"default_block\": \"minecraft:stone\"", "\"default_block\": \"minecraft:deepslate\""},
					{"\"material_rule\": \"minecraft:overworld\"", "\"material_rule\": \"glowcube_realms:sculk_realm\""}});
			write(DATA.resolve("worldgen/material_rule/sculk_realm.json"), "{\"type\":\"minecraft:sequence\",\"sequence\":[\"minecraft:bedrock_floor\","
					+ "{\"type\":\"minecraft:condition\",\"if_true\":{\"type\":\"minecraft:biome\",\"biome_is\":[\"" + NS + ":echo_forest\"]},\"then_run\":{\"type\":\"minecraft:condition\","
					+ "\"if_true\":\"minecraft:on_floor\",\"then_run\":{\"type\":\"minecraft:block\",\"result_state\":\"minecraft:moss_block\"}}},"
					+ "{\"type\":\"minecraft:condition\",\"if_true\":\"minecraft:on_floor\",\"then_run\":{\"type\":\"minecraft:block\",\"result_state\":\"minecraft:sculk\"}},"
					+ "{\"type\":\"minecraft:condition\",\"if_true\":\"minecraft:under_floor\",\"then_run\":{\"type\":\"minecraft:block\",\"result_state\":\"minecraft:sculk\"}}]}");
			feature("echo_crystal_ore", ore(NS + ":echo_crystal_ore", "minecraft:deepslate", 6));
			feature("echo_spike", "{\"type\":\"" + NS + ":crystal_spike\",\"block\":\"" + NS + ":echo_crystal_block\"}");
			feature("echo_bloom", "{\"type\":\"minecraft:simple_block\",\"to_place\":{\"id\":\"" + NS + ":echo_bloom\"}}");
			feature("echo_tree", tree(NS + ":echo_log", NS + ":echo_leaves",
					"{\"type\":\"minecraft:fancy_trunk_placer\",\"base_height\":9,\"height_rand_a\":6,\"height_rand_b\":2}",
					"{\"type\":\"minecraft:fancy_foliage_placer\",\"height\":4,\"offset\":4,\"radius\":2}",
					"{\"type\":\"minecraft:two_layers_feature_size\",\"limit\":0,\"min_clipped_height\":4,\"upper_size\":0}"));
			feature("giant_echo_tree", tree(NS + ":echo_log", NS + ":echo_leaves",
					"{\"type\":\"minecraft:mega_jungle_trunk_placer\",\"base_height\":14,\"height_rand_a\":6,\"height_rand_b\":10}",
					"{\"type\":\"minecraft:jungle_foliage_placer\",\"height\":3,\"offset\":0,\"radius\":4}",
					"{\"type\":\"minecraft:two_layers_feature_size\",\"lower_size\":1,\"upper_size\":2}"));
			placed("echo_crystal_ore", NS + ":echo_crystal_ore", count(10) + "," + inSquare() + "," + heightRange(-60, 120) + "," + biome());
			placed("echo_spikes", NS + ":echo_spike", count(2) + "," + inSquare() + "," + heightmap("WORLD_SURFACE_WG") + "," + biome());
			placed("echo_spikes_rare", NS + ":echo_spike", rarity(3) + "," + inSquare() + "," + heightmap("WORLD_SURFACE_WG") + "," + biome());
			placed("echo_blooms", NS + ":echo_bloom", count(24) + "," + inSquare() + "," + heightmap("MOTION_BLOCKING") + "," + biome() + "," + survive(NS + ":echo_bloom"));
			placed("echo_trees_sparse", NS + ":echo_tree", rarity(2) + "," + inSquare() + "," + heightmap("MOTION_BLOCKING") + "," + biome());
			placed("echo_trees_dense", NS + ":echo_tree", count(5) + "," + inSquare() + "," + heightmap("MOTION_BLOCKING") + "," + biome());
			placed("giant_echo_trees", NS + ":giant_echo_tree", count(1) + "," + inSquare() + "," + heightmap("MOTION_BLOCKING") + "," + biome());
			// keep one global feature order across these biomes
			sculkBiome("sculk_plains", q(NS + ":echo_trees_sparse") + "," + q(NS + ":echo_spikes_rare") + "," + q("minecraft:sculk_patch_deep_dark") + "," + q("minecraft:sculk_vein"));
			sculkBiome("echo_forest", q(NS + ":giant_echo_trees") + "," + q(NS + ":echo_trees_dense") + "," + q(NS + ":echo_blooms") + "," + q("minecraft:sculk_vein"));
			sculkBiome("sculk_spires", q(NS + ":echo_spikes") + "," + q("minecraft:sculk_patch_deep_dark") + "," + q("minecraft:sculk_vein"));
			structure("sculk_sanctuary", "echo_warden", "#" + NS + ":has_sculk_sanctuary", "underground_decoration", -40);
			structureSet("sculk_sanctuary", 14, 5, 2468013);
			structureSet("frozen_crypt", 24, 9, 8812345);
		}

		static void sculkBiome(String name, String vegetation) throws IOException {
			String json = "{\"attributes\":{\"minecraft:gameplay/natural_mob_spawns\":{\"argument\":{\"spawn_costs\":{},\"spawns_by_category\":{"
					+ "\"creature\":[{\"type\":\"" + NS + ":sculk_snail\",\"count\":{\"type\":\"minecraft:uniform\",\"max_inclusive\":3,\"min_inclusive\":1},\"weight\":60}],"
					+ "\"monster\":[{\"type\":\"" + NS + ":sculk_stalker\",\"count\":{\"type\":\"minecraft:uniform\",\"max_inclusive\":3,\"min_inclusive\":1},\"weight\":100}]}},"
					+ "\"modifier\":\"overlay\"},\"minecraft:visual/sky_color\":\"#030c10\",\"minecraft:visual/fog_color\":\"#06181c\",\"minecraft:visual/water_fog_color\":\"#06181c\"},"
					+ "\"carvers\":[\"minecraft:cave\",\"minecraft:cave_extra_underground\",\"minecraft:canyon\"],\"downfall\":0.4,"
					+ "\"effects\":{\"foliage_color\":\"#1ab0a8\",\"grass_color\":\"#1ab0a8\",\"water_color\":\"#0a3a40\"},"
					+ "\"features\":[[],[],[],[],[],[],[\"" + NS + ":echo_crystal_ore\"],[],[],[" + vegetation + "],[]],\"has_precipitation\":false,\"temperature\":0.5}";
			write(DATA.resolve("worldgen/biome/" + name + ".json"), json);
		}

		/** Bigger villages: more jigsaw steps and a larger radius than vanilla. */
		static void villages() throws IOException {
			Path jar = Path.of(System.getProperty("user.home"), ".gradle/caches/fabric-loom/26.3/minecraft-common.jar");
			try (java.util.zip.ZipFile zip = new java.util.zip.ZipFile(jar.toFile())) {
				for (String v : new String[]{"plains", "desert", "savanna", "snowy", "taiga"}) {
					String json = new String(zip.getInputStream(zip.getEntry("data/minecraft/worldgen/structure/village_" + v + ".json")).readAllBytes(), StandardCharsets.UTF_8);
					json = json.replace("\"size\": 6", "\"size\": 10").replace("\"max_distance_from_center\": 80", "\"max_distance_from_center\": 116");
					write(MC_DATA.resolve("worldgen/structure/village_" + v + ".json"), json);
				}
			}
		}
	}

	// ================================================================ UPDATE 4: grand structures
	static class Update4 {
		static final Path MC = Path.of("src/main/resources/data/minecraft");

		static void all() throws IOException {
			grand("desert_pyramid", "pyramid", "{}");
			grand("jungle_pyramid", "jungle", "{}");
			grand("igloo", "igloo", "{}");
			grand("swamp_hut", "witch", "{\"creature\":{\"bounding_box\":\"piece\",\"spawns\":[{\"type\":\"minecraft:cat\",\"count\":1,\"weight\":1}]},"
					+ "\"monster\":{\"bounding_box\":\"piece\",\"spawns\":[{\"type\":\"minecraft:witch\",\"count\":1,\"weight\":1}]}}");
			grand("pillager_outpost", "fortress", "{\"monster\":{\"bounding_box\":\"full\",\"spawns\":[{\"type\":\"minecraft:pillager\",\"count\":1,\"weight\":1}]}}");
			treasure("grand_pyramid_treasure", "minecraft:chests/desert_pyramid", new Object[][]{{"minecraft:gold_ingot", 20, 3, 9}, {"minecraft:diamond", 8, 1, 3},
					{"minecraft:emerald", 10, 2, 6}, {"minecraft:golden_apple", 6, 1, 2}, {"minecraft:enchanted_golden_apple", 1, 1, 1}, {"minecraft:gold_block", 3, 1, 2},
					{"minecraft:lapis_lazuli", 10, 4, 12}, {NS + ":glow_shard", 8, 2, 5}, {NS + ":starmetal_ingot", 4, 1, 2}, {"minecraft:dune_armor_trim_smithing_template", 2, 1, 1}});
			treasure("jungle_treasure", "minecraft:chests/jungle_temple", new Object[][]{{"minecraft:emerald", 18, 3, 9}, {"minecraft:diamond", 8, 1, 3},
					{"minecraft:gold_ingot", 14, 2, 7}, {"minecraft:emerald_block", 3, 1, 2}, {"minecraft:golden_apple", 5, 1, 2}, {"minecraft:enchanted_golden_apple", 1, 1, 1},
					{"minecraft:cocoa_beans", 8, 3, 8}, {NS + ":void_shard", 6, 1, 4}, {"minecraft:wild_armor_trim_smithing_template", 2, 1, 1}});
			treasure("witch_manor", null, new Object[][]{{"minecraft:glass_bottle", 15, 2, 6}, {"minecraft:nether_wart", 12, 2, 6}, {"minecraft:redstone", 12, 3, 8},
					{"minecraft:glowstone_dust", 12, 3, 8}, {"minecraft:spider_eye", 10, 1, 4}, {"minecraft:fermented_spider_eye", 6, 1, 2}, {"minecraft:sugar", 10, 2, 6},
					{"minecraft:gunpowder", 8, 1, 4}, {"minecraft:blaze_powder", 4, 1, 2}, {"minecraft:rabbit_foot", 3, 1, 1}, {"minecraft:magma_cream", 5, 1, 3},
					{"minecraft:golden_carrot", 5, 1, 3}, {"minecraft:experience_bottle", 4, 1, 3}, {NS + ":glow_jelly", 6, 1, 3}});
			treasure("igloo_lab", "minecraft:chests/igloo_chest", new Object[][]{{"minecraft:golden_apple", 10, 1, 2}, {"minecraft:emerald", 8, 1, 4},
					{"minecraft:book", 8, 1, 3}, {"minecraft:glass_bottle", 8, 1, 4}, {"minecraft:diamond", 3, 1, 2}, {"minecraft:snowball", 10, 4, 16},
					{"minecraft:blue_ice", 6, 2, 6}, {"minecraft:experience_bottle", 4, 1, 3}});
			treasure("fortress_armory", "minecraft:chests/pillager_outpost", new Object[][]{{"minecraft:crossbow", 8, 1, 1}, {"minecraft:arrow", 15, 6, 20},
					{"minecraft:iron_ingot", 12, 2, 7}, {"minecraft:iron_chestplate", 4, 1, 1}, {"minecraft:iron_helmet", 4, 1, 1}, {"minecraft:emerald", 10, 2, 6},
					{"minecraft:dark_oak_log", 10, 2, 8}, {"minecraft:goat_horn", 2, 1, 1}, {"minecraft:sentry_armor_trim_smithing_template", 2, 1, 1}});
		}

		/** Keeps the vanilla id, biomes and spacing, but builds the bigger version. */
		static void grand(String id, String kind, String spawnOverrides) throws IOException {
			String biomes = "#minecraft:has_structure/" + (id.equals("jungle_pyramid") ? "jungle_temple" : id);
			write(MC.resolve("worldgen/structure/" + id + ".json"), "{\"type\":\"" + NS + ":grand\",\"biomes\":\"" + biomes + "\",\"kind\":\"" + kind
					+ "\",\"spawn_overrides\":" + spawnOverrides + ",\"step\":\"surface_structures\",\"terrain_adaptation\":\"none\"}");
		}

		/** Own treasure pool plus (optionally) one roll of a vanilla chest table. */
		static void treasure(String name, String vanilla, Object[][] entries) throws IOException {
			List<String> list = new ArrayList<>();
			for (Object[] e : entries) {
				list.add("{\"type\":\"minecraft:item\",\"modifier\":{\"type\":\"minecraft:set_count\",\"count\":{\"type\":\"minecraft:uniform\",\"max\":" + e[3] + ",\"min\":" + e[2]
						+ "}},\"name\":\"" + e[0] + "\",\"weight\":" + e[1] + "}");
			}
			String pools = "{\"entries\":[" + String.join(",", list) + "],\"rolls\":{\"type\":\"minecraft:uniform\",\"max\":7,\"min\":4}}";
			if (vanilla != null) pools += ",{\"entries\":[{\"type\":\"minecraft:loot_table\",\"value\":\"" + vanilla + "\"}],\"rolls\":1}";
			write(DATA.resolve("loot_table/chests/" + name + ".json"), "{\"type\":\"minecraft:chest\",\"pools\":[" + pools + "],\"random_sequence\":\"" + NS + ":chests/" + name + "\"}");
		}
	}

	// ================================================================ UPDATE 5: realm places, traders, cloud vents
	static class Update5 {
		static final String LUMEN = "[\"" + NS + ":lumen_meadows\",\"" + NS + ":aurora_forest\",\"" + NS + ":crystal_peaks\"]";
		static final String UMBRAL = "[\"" + NS + ":umbral_caverns\",\"" + NS + ":shadecap_forest\",\"" + NS + ":void_rift\"]";
		static final String SCULK = "[\"" + NS + ":sculk_plains\",\"" + NS + ":echo_forest\",\"" + NS + ":sculk_spires\"]";

		static void all() throws IOException {
			place("sky_market", "sky_market", LUMEN, "surface_structures", 150, 30, 11, 5501701);
			place("sky_ruin", "sky_ruin", LUMEN, "surface_structures", 175, 11, 4, 5501702);
			place("shadow_bazaar", "shadow_bazaar", UMBRAL, "underground_structures", 40, 26, 9, 5501703);
			place("umbral_mine", "umbral_mine", UMBRAL, "underground_structures", 22, 18, 7, 5501704);
			place("echo_camp", "echo_camp", SCULK, "surface_structures", null, 18, 6, 5501705);
			place("echo_ruin", "echo_ruin", SCULK, "surface_structures", null, 16, 6, 5501706);
			Update4.treasure("sky_market", null, new Object[][]{{NS + ":lumen_berries", 14, 2, 6}, {NS + ":glow_shard", 14, 2, 6}, {NS + ":cloud_fluff", 10, 1, 4},
					{"minecraft:emerald", 10, 1, 4}, {NS + ":aurora_fruit", 6, 1, 3}, {"minecraft:feather", 8, 2, 6}, {"minecraft:phantom_membrane", 4, 1, 2},
					{NS + ":cloud_vent", 4, 1, 2}, {NS + ":lumen_compass", 2, 1, 1}});
			Update4.treasure("sky_ruin", null, new Object[][]{{NS + ":glow_shard", 16, 2, 7}, {NS + ":starmetal_ingot", 6, 1, 2}, {NS + ":wisp_essence", 8, 1, 3},
					{"minecraft:diamond", 4, 1, 2}, {"minecraft:emerald", 10, 2, 5}, {NS + ":cloud_bottle", 2, 1, 1}, {NS + ":lumen_key", 3, 1, 1},
					{"minecraft:golden_apple", 4, 1, 1}, {NS + ":cloud_vent", 5, 1, 2}});
			Update4.treasure("shadow_bazaar", null, new Object[][]{{NS + ":void_shard", 14, 2, 6}, {NS + ":shadecap_stew", 6, 1, 2}, {NS + ":shade_fang", 10, 1, 4},
					{"minecraft:emerald", 10, 1, 4}, {"minecraft:soul_lantern", 8, 1, 4}, {NS + ":umbral_compass", 2, 1, 1}, {"minecraft:ender_pearl", 5, 1, 3}});
			Update4.treasure("umbral_mine", "minecraft:chests/abandoned_mineshaft", new Object[][]{{NS + ":void_shard", 16, 2, 8}, {"minecraft:iron_ingot", 12, 2, 6},
					{"minecraft:gold_ingot", 8, 1, 4}, {"minecraft:diamond", 4, 1, 2}, {"minecraft:rail", 10, 4, 12}, {"minecraft:torch", 10, 4, 16},
					{NS + ":voidshard_pickaxe", 1, 1, 1}, {NS + ":excavator_pickaxe", 1, 1, 1}, {"minecraft:tnt", 4, 1, 3}});
			Update4.treasure("echo_camp", null, new Object[][]{{NS + ":echo_crystal", 12, 1, 4}, {NS + ":glow_jelly", 8, 1, 3}, {"minecraft:bread", 12, 2, 5},
					{"minecraft:emerald", 10, 1, 4}, {"minecraft:map", 4, 1, 1}, {"minecraft:compass", 3, 1, 1}, {"minecraft:candle", 8, 1, 4}});
			Update4.treasure("echo_ruin", "minecraft:chests/ancient_city", new Object[][]{{NS + ":echo_crystal", 16, 2, 6}, {"minecraft:echo_shard", 10, 1, 3},
					{NS + ":sculk_key", 2, 1, 1}, {"minecraft:disc_fragment_5", 6, 1, 3}, {"minecraft:silence_armor_trim_smithing_template", 2, 1, 1},
					{NS + ":echo_crystal_block", 3, 1, 1}, {"minecraft:experience_bottle", 6, 1, 4}});
			// cloud vent block
			write(ASSETS.resolve("models/block/cloud_vent.json"), "{\"parent\":\"minecraft:block/cube_bottom_top\",\"textures\":{\"top\":\"" + NS
					+ ":block/cloud_vent_top\",\"bottom\":\"" + NS + ":block/skystone_bricks\",\"side\":\"" + NS + ":block/cloud_vent_side\"}}");
			write(ASSETS.resolve("blockstates/cloud_vent.json"), "{\"variants\":{\"\":{\"model\":\"" + NS + ":block/cloud_vent\"}}}");
			blockItem("cloud_vent");
			write(DATA.resolve("loot_table/blocks/cloud_vent.json"), "{\"type\":\"minecraft:block\",\"pools\":[{\"condition\":{\"type\":\"minecraft:survives_explosion\"},"
					+ "\"entries\":[{\"type\":\"minecraft:item\",\"name\":\"" + NS + ":cloud_vent\"}],\"rolls\":1}],\"random_sequence\":\"" + NS + ":blocks/cloud_vent\"}");
			shaped("cloud_vent", 2, new String[]{"FFF", "SGS", "SSS"}, "F", NS + ":cloud_fluff", "S", NS + ":skystone_bricks", "G", NS + ":glow_shard");
			write(ASSETS.resolve("models/item/realm_trader_spawn_egg.json"), "{\"parent\":\"minecraft:item/generated\",\"textures\":{\"layer0\":\"" + NS + ":item/realm_trader_spawn_egg\"}}");
			itemDef("realm_trader_spawn_egg", NS + ":item/realm_trader_spawn_egg");
		}

		static void place(String name, String kind, String biomes, String step, Integer fixedY, int spacing, int separation, int salt) throws IOException {
			write(DATA.resolve("worldgen/structure/" + name + ".json"), "{\"type\":\"" + NS + ":grand\",\"biomes\":" + biomes + ",\"kind\":\"" + kind + "\","
					+ (fixedY != null ? "\"fixed_y\":" + fixedY + "," : "") + "\"spawn_overrides\":{},\"step\":\"" + step + "\",\"terrain_adaptation\":\"none\"}");
			structureSet(name, spacing, separation, salt);
		}

		static void lang(Map<String, String[]> t) {
			b(t, "cloud_vent", "Cloud Vent", "Wolkendüse");
			e(t, "realm_trader", "Realm Trader", "Reichshändler");
			i(t, "realm_trader_spawn_egg", "Realm Trader Spawn Egg", "Reichshändler-Spawn-Ei", null, null);
			t.put("block." + NS + ".cloud_vent.lore1", new String[]{"Carries you up to 24 blocks into the sky. Sneak to stand on it.",
					"Trägt dich bis zu 24 Blöcke in den Himmel. Schleichen, um darauf zu stehen."});
		}
	}

	// ================================================================ UPDATE 6 (1.5.0): 3D held weapons
	/**
	 * Every weapon and tool gets an element model (models/item/<name>_3d.json, geometry defined in
	 * TextureGen.models3d() next to its painted texture sheet). The item definition shows the flat sprite in
	 * inventories, on the ground, in item frames and on shelves, and the 3D model everywhere else (hands, armor stands).
	 */
	static class Update6 {
		static final String FLAT = "[\"gui\",\"ground\",\"fixed\",\"on_shelf\"]";

		static void all() throws IOException {
			List<String> names = new ArrayList<>();
			for (TextureGen.Model3D m : TextureGen.models3d()) {
				write(ASSETS.resolve("models/item/" + m.name + "_3d.json"), m.json(NS));
				names.add(m.name);
			}
			for (String name : names) {
				if (name.contains("_pulling_")) continue;
				String flat, held;
				if (name.endsWith("_bow")) {
					flat = bowChain(name, "");
					held = bowChain(name, "_3d");
				} else {
					flat = model(name);
					held = model(name + "_3d");
				}
				write(ASSETS.resolve("items/" + name + ".json"), "{\"model\":{\"type\":\"minecraft:select\",\"property\":\"minecraft:display_context\",\"cases\":[{\"when\":"
						+ FLAT + ",\"model\":" + flat + "}],\"fallback\":" + held + "}}");
			}
		}

		static String model(String m) {
			return "{\"type\":\"minecraft:model\",\"model\":\"" + NS + ":item/" + m + "\"}";
		}

		static String bowChain(String bow, String suffix) {
			return "{\"type\":\"minecraft:condition\",\"property\":\"minecraft:using_item\",\"on_false\":" + model(bow + suffix) + ",\"on_true\":{\"type\":\"minecraft:range_dispatch\","
					+ "\"property\":\"minecraft:use_duration\",\"scale\":0.05,\"entries\":[{\"threshold\":0.65,\"model\":" + model(bow + "_pulling_1" + suffix) + "},{\"threshold\":0.9,\"model\":"
					+ model(bow + "_pulling_2" + suffix) + "}],\"fallback\":" + model(bow + "_pulling_0" + suffix) + "}}";
		}
	}

	// ================================================================ reference check
	/** Fails the run if any JSON asset points to a model or texture of this mod that does not exist. */
	static void checkReferences() throws IOException {
		java.util.regex.Pattern ref = java.util.regex.Pattern.compile("\"([a-z_0-9]+)\"\\s*:\\s*\"" + NS + ":([a-z_0-9/]+)\"");
		List<String> missing = new ArrayList<>();
		int checked = 0;
		for (String dir : new String[]{"items", "blockstates", "models", "equipment"}) {
			Path base = ASSETS.resolve(dir);
			if (!Files.exists(base)) continue;
			List<Path> files;
			try (var s = Files.walk(base)) {
				files = s.filter(p -> p.toString().endsWith(".json")).toList();
			}
			for (Path file : files) {
				java.util.regex.Matcher m = ref.matcher(Files.readString(file));
				while (m.find()) {
					String key = m.group(1), id = m.group(2);
					List<Path> want = new ArrayList<>();
					if (key.equals("model") || key.equals("parent")) want.add(ASSETS.resolve("models/" + id + ".json"));
					else if (dir.equals("equipment")) {
						want.add(ASSETS.resolve("textures/entity/equipment/humanoid/" + id + ".png"));
						want.add(ASSETS.resolve("textures/entity/equipment/humanoid_leggings/" + id + ".png"));
					} else want.add(ASSETS.resolve("textures/" + id + ".png"));
					for (Path w : want) {
						checked++;
						if (!Files.exists(w)) missing.add(ASSETS.relativize(file) + " -> " + key + " " + id);
					}
				}
			}
		}
		System.out.println("Reference check: " + checked + " references, " + missing.size() + " missing.");
		for (String s : missing) System.out.println("  MISSING " + s);
		if (!missing.isEmpty()) throw new IllegalStateException("missing model/texture references");
	}
}
