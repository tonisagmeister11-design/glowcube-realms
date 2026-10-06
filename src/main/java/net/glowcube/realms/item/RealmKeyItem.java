package net.glowcube.realms.item;

import java.util.function.Supplier;
import net.glowcube.realms.world.RealmData;
import net.glowcube.realms.world.RealmPortalShape;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/** Lights a realm portal inside a frame of the matching block. */
public class RealmKeyItem extends LoreItem {
	private final Block frame;
	private final Supplier<Block> portal;

	public RealmKeyItem(Block frame, Supplier<Block> portal, Properties properties) {
		super(properties);
		this.frame = frame;
		this.portal = portal;
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		Level level = context.getLevel();
		BlockPos clicked = context.getClickedPos();
		if (!level.getBlockState(clicked).is(this.frame)) return InteractionResult.PASS;
		BlockPos start = clicked.relative(context.getClickedFace());
		if (level.isClientSide()) return InteractionResult.SUCCESS;
		Player player = context.getPlayer();
		if (RealmPortalShape.tryLight(level, start, this.frame, this.portal.get())) {
			level.playSound(null, start, SoundEvents.END_PORTAL_SPAWN, SoundSource.BLOCKS, 0.8F, 1.4F);
			RealmData data = RealmData.get();
			if (data != null) data.addPortal(level.dimension().identifier().toString(), start);
			if (player != null) context.getItemInHand().hurtAndBreak(1, player, context.getHand().asEquipmentSlot());
			return InteractionResult.SUCCESS;
		}
		if (player != null) player.sendOverlayMessage(Component.translatable("message.glowcube_realms.portal_invalid"));
		return InteractionResult.FAIL;
	}
}
