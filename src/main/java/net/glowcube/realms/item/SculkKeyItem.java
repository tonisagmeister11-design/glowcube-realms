package net.glowcube.realms.item;

import net.glowcube.realms.block.SculkKeyholeBlock;
import net.glowcube.realms.registry.ModBlocks;
import net.glowcube.realms.world.RealmData;
import net.glowcube.realms.world.RealmPortalShape;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The Sculk Key. Right-click the keyhole in the bottom row of a sculk gate (or a block of the
 * Ancient City frame) to insert it and open a portal to the Sculk Realm.
 */
public class SculkKeyItem extends LoreItem {
	public SculkKeyItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		BlockPos clicked = context.getClickedPos();
		BlockState state = context.getLevel().getBlockState(clicked);
		boolean keyhole = state.is(ModBlocks.SCULK_KEYHOLE);
		if (!keyhole && !state.is(Blocks.REINFORCED_DEEPSLATE)) return InteractionResult.PASS;
		if (!(context.getLevel() instanceof ServerLevel level)) return InteractionResult.SUCCESS;
		Player player = context.getPlayer();
		if (keyhole && state.getValue(SculkKeyholeBlock.FILLED)) {
			if (player != null) player.sendOverlayMessage(Component.translatable("message.glowcube_realms.keyhole_used"));
			return InteractionResult.FAIL;
		}
		// try the space above the clicked block first, then the side the player clicked
		BlockPos[] starts = {clicked.above(), clicked.relative(context.getClickedFace()), clicked.above(2)};
		boolean lit = false;
		BlockPos litAt = null;
		for (BlockPos start : starts) {
			if (RealmPortalShape.tryLight(level, start, s -> !s.isAir() && !s.canBeReplaced(), ModBlocks.SCULK_PORTAL, 600, 30)) {
				lit = true;
				litAt = start;
				break;
			}
		}
		if (!lit) {
			if (player != null) player.sendOverlayMessage(Component.translatable("message.glowcube_realms.portal_invalid"));
			return InteractionResult.FAIL;
		}
		if (keyhole) level.setBlock(clicked, state.setValue(SculkKeyholeBlock.FILLED, true), 3);
		level.playSound(null, clicked, SoundEvents.SCULK_SHRIEKER_SHRIEK, SoundSource.BLOCKS, 1.5F, 0.8F);
		level.playSound(null, clicked, SoundEvents.END_PORTAL_SPAWN, SoundSource.BLOCKS, 0.8F, 0.6F);
		level.sendParticles(ParticleTypes.SCULK_SOUL, clicked.getX() + 0.5, clicked.getY() + 1.5, clicked.getZ() + 0.5, 60, 1.0, 1.5, 1.0, 0.05);
		RealmData data = RealmData.get();
		if (data != null) data.addPortal(level.dimension().identifier().toString(), litAt);
		if (player != null && !player.isCreative()) context.getItemInHand().shrink(1);
		if (player != null) player.sendOverlayMessage(Component.translatable("message.glowcube_realms.sculk_portal_open"));
		return InteractionResult.SUCCESS;
	}
}
