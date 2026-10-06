package net.glowcube.realms.item;

import net.glowcube.realms.entity.projectile.GlowShardProjectile;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/** Aurora Staff: fires seeking crystal bolts. Sneak + right-click to heal yourself and nearby players. */
public class AuroraStaffItem extends LoreItem {
	public AuroraStaffItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (player.getCooldowns().isOnCooldown(stack)) return InteractionResult.FAIL;
		if (level instanceof ServerLevel server) {
			if (player.isShiftKeyDown()) {
				for (Player p : server.getEntitiesOfClass(Player.class, new AABB(player.blockPosition()).inflate(8))) {
					p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 120, 1));
					p.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 400, 1));
					server.sendParticles(ParticleTypes.HAPPY_VILLAGER, p.getX(), p.getY(1), p.getZ(), 15, 0.4, 0.6, 0.4, 0);
				}
				Abilities.ring(server, ParticleTypes.END_ROD, player.position().add(0, 0.1, 0), 4.0, 48);
				server.playSound(null, player.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 1.0F, 1.6F);
				stack.hurtAndBreak(10, player, hand.asEquipmentSlot());
				player.getCooldowns().addCooldown(stack, 400);
				return InteractionResult.SUCCESS;
			}
			GlowShardProjectile shard = new GlowShardProjectile(server, player);
			shard.setDamage(7.0F);
			shard.setSeeking(true);
			shard.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 2.2F, 0.5F);
			server.addFreshEntity(shard);
			server.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.PLAYERS, 1.0F, 1.8F);
			stack.hurtAndBreak(1, player, hand.asEquipmentSlot());
		}
		player.getCooldowns().addCooldown(stack, 12);
		return InteractionResult.SUCCESS;
	}
}
