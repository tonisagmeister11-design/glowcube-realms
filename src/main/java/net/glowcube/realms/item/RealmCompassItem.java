package net.glowcube.realms.item;

import net.glowcube.realms.world.RealmTeleporter;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Realm compass: instantly travels into its realm, or back to the overworld when used inside it. */
public class RealmCompassItem extends LoreItem {
	private final ResourceKey<Level> realm;

	public RealmCompassItem(ResourceKey<Level> realm, Properties properties) {
		super(properties);
		this.realm = realm;
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (player.getCooldowns().isOnCooldown(stack)) return InteractionResult.FAIL;
		if (level instanceof ServerLevel server) {
			boolean inRealm = level.dimension() == this.realm;
			ServerLevel target = server.getServer().getLevel(inRealm ? Level.OVERWORLD : this.realm);
			if (target == null) return InteractionResult.FAIL;
			server.sendParticles(ParticleTypes.REVERSE_PORTAL, player.getX(), player.getY(1), player.getZ(), 80, 0.5, 1.0, 0.5, 0.2);
			server.playSound(null, player.blockPosition(), SoundEvents.PLAYER_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.2F);
			stack.hurtAndBreak(1, player, hand.asEquipmentSlot());
			RealmTeleporter.sendTo(player, target, player.getX(), player.getZ(), null, true);
			player.sendOverlayMessage(Component.translatable(inRealm ? "message.glowcube_realms.back_home" : "message.glowcube_realms.arrived_" + this.realm.identifier().getPath()));
		}
		player.getCooldowns().addCooldown(stack, 400);
		return InteractionResult.SUCCESS;
	}
}
