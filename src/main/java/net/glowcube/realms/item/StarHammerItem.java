package net.glowcube.realms.item;

import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** Star Hammer: crushing blows that launch enemies, right-click to slam the ground with a meteor shockwave. */
public class StarHammerItem extends LoreItem {
	public StarHammerItem(Properties properties) {
		super(properties);
	}

	@Override
	public void postHurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
		super.postHurtEnemy(stack, target, attacker);
		Abilities.push(target, attacker.position(), 1.1, 0.45);
		if (attacker.level() instanceof ServerLevel level && attacker.getRandom().nextFloat() < 0.2F) {
			level.sendParticles(ParticleTypes.FIREWORK, target.getX(), target.getY() + 2.5, target.getZ(), 30, 0.2, 0.6, 0.2, 0.15);
			target.hurtServer(level, level.damageSources().magic(), 6.0F);
			level.playSound(null, target.blockPosition(), SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.PLAYERS, 1.0F, 0.8F);
		}
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (player.getCooldowns().isOnCooldown(stack)) return InteractionResult.FAIL;
		if (level instanceof ServerLevel server) {
			BlockState ground = server.getBlockState(player.blockPosition().below());
			for (LivingEntity e : Abilities.targets(server, player.position(), 6.0, player)) {
				double dist = e.distanceTo(player);
				e.hurtServer(server, server.damageSources().playerAttack(player), (float) (13.0 - dist));
				Abilities.push(e, player.position(), 0.9, 0.9);
			}
			for (int r = 1; r <= 6; r++) {
				Abilities.ring(server, ParticleTypes.FIREWORK, player.position().add(0, 0.2, 0), r, 10 + r * 6);
				if (!ground.isAir()) {
					Abilities.ring(server, new BlockParticleOption(ParticleTypes.BLOCK, ground), player.position().add(0, 0.2, 0), r, 6 + r * 4);
				}
			}
			server.sendParticles(ParticleTypes.EXPLOSION, player.getX(), player.getY(), player.getZ(), 3, 1.0, 0.1, 1.0, 0);
			server.playSound(null, player.blockPosition(), SoundEvents.MACE_SMASH_GROUND_HEAVY, SoundSource.PLAYERS, 1.6F, 0.8F);
			server.playSound(null, player.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.6F, 1.4F);
			stack.hurtAndBreak(3, player, hand.asEquipmentSlot());
		}
		player.getCooldowns().addCooldown(stack, 140);
		return InteractionResult.SUCCESS;
	}
}
