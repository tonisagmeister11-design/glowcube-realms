package net.glowcube.realms.item;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/** Item that shows up to three lore lines from the language file ("<item key>.lore1" ...). */
public class LoreItem extends Item {
	public LoreItem(Item.Properties properties) {
		super(properties);
	}

	@Override
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
		addLore(stack, builder);
	}

	public static void addLore(ItemStack stack, Consumer<Component> builder) {
		String base = stack.getItem().getDescriptionId();
		Language lang = Language.getInstance();
		for (int i = 1; i <= 3; i++) {
			String key = base + ".lore" + i;
			if (!lang.has(key)) break;
			builder.accept(Component.translatable(key).withStyle(i == 1 ? ChatFormatting.AQUA : ChatFormatting.GRAY));
		}
	}
}
