package net.glowcube.realms.item;

import net.glowcube.realms.block.AltarBlock;
import net.glowcube.realms.block.entity.AltarBlockEntity;
import net.glowcube.realms.world.RealmData;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.state.BlockState;

/** Re-awakens a boss at its altar so the fight can be repeated. */
public class SigilItem extends LoreItem {
	private final AltarBlock.Boss boss;

	public SigilItem(AltarBlock.Boss boss, Properties properties) {
		super(properties);
		this.boss = boss;
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		BlockState state = context.getLevel().getBlockState(context.getClickedPos());
		Player player = context.getPlayer();
		if (!(state.getBlock() instanceof AltarBlock altar) || altar.boss() != this.boss) {
			if (player != null && !context.getLevel().isClientSide()) {
				player.sendOverlayMessage(Component.translatable("message.glowcube_realms.sigil_wrong_altar"));
			}
			return InteractionResult.FAIL;
		}
		if (!(context.getLevel() instanceof ServerLevel level)) return InteractionResult.SUCCESS;
		if (level.getBlockEntity(context.getClickedPos()) instanceof AltarBlockEntity be && be.summon(level)) {
			RealmData data = RealmData.get();
			if (data != null) data.markAwake(level.dimension().identifier().toString(), context.getClickedPos());
			context.getItemInHand().shrink(1);
			return InteractionResult.SUCCESS;
		}
		if (player != null) player.sendOverlayMessage(Component.translatable("message.glowcube_realms.boss_alive"));
		return InteractionResult.FAIL;
	}
}
