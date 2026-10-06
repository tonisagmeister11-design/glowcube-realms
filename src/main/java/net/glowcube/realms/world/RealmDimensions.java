package net.glowcube.realms.world;

import net.glowcube.realms.GlowcubeRealms;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

public final class RealmDimensions {
	public static final ResourceKey<Level> LUMEN_SKIES = ResourceKey.create(Registries.DIMENSION, GlowcubeRealms.id("lumen_skies"));
	public static final ResourceKey<Level> UMBRAL_DEPTHS = ResourceKey.create(Registries.DIMENSION, GlowcubeRealms.id("umbral_depths"));
	public static final ResourceKey<Level> SCULK_REALM = ResourceKey.create(Registries.DIMENSION, GlowcubeRealms.id("sculk_realm"));

	public static boolean isRealm(ResourceKey<Level> key) {
		return key == LUMEN_SKIES || key == UMBRAL_DEPTHS || key == SCULK_REALM;
	}

	private RealmDimensions() {
	}
}
