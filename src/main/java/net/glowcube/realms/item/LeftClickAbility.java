package net.glowcube.realms.item;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/** Items with an ability on left-click into the air (sent from the client). */
public interface LeftClickAbility {
	void onLeftClick(ServerPlayer player, ItemStack stack);
}
