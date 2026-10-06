package net.glowcube.realms.entity.animal;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/** Flying peaceful creature (Lantern Bug, Void Jelly). */
public class RealmFlyer extends RealmAnimal {
	public RealmFlyer(EntityType<? extends RealmFlyer> type, Level level, Kind kind) {
		super(type, level, kind);
	}

	@Override
	protected boolean flies() {
		return true;
	}
}
