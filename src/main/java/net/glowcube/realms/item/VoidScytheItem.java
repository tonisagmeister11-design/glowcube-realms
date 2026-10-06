package net.glowcube.realms.item;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Void Scythe: every swing reaps everything around the target and steals life. Right-click pulls enemies in. */
public class VoidScytheItem extends LoreItem {
	public VoidScytheItem(Properties properties) {
		super(properties);
	}

	@Override
	public void postHurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
		super.postHurtEnemy(stack, target, attacker);
		if (attacker.level() instanceof ServerLevel level) {
			int hit = 0;
			for (LivingEntity e : Abilities.targets(level, target.position(), 3.5, attacker)) {
				if (e == target) continue;
				e.hurtServer(level, level.damageSources().indirectMagic(attacker, attacker), 7.0F);
				hit++;
			}
			attacker.heal(2.0F + hit);
			Abilities.ring(level, ParticleTypes.REVERSE_PORTAL, target.position().add(0, 0.6, 0), 2.5, 24);
			level.sendParticles(ParticleTypes.SWEEP_ATTACK, target.getX(), target.getY(0.5), target.getZ(), 1, 0, 0, 0, 0);
		}
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (player.getCooldowns().isOnCooldown(stack)) return InteractionResult.FAIL;
		if (level instanceof ServerLevel server) {
			for (LivingEntity e : Abilities.targets(server, player.position(), 11.0, player)) {
				Vec3 pull = player.position().subtract(e.position()).normalize().scale(1.4);
				e.setDeltaMovement(pull.x, 0.35, pull.z);
				e.needsSync = true;
				e.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 2));
				e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0));
				Abilities.line(server, ParticleTypes.PORTAL, e.position().add(0, 1, 0), player.position().add(0, 1, 0), 12);
			}
			Abilities.ring(server, ParticleTypes.REVERSE_PORTAL, player.position().add(0, 0.2, 0), 6.0, 60);
			server.playSound(null, player.blockPosition(), SoundEvents.WARDEN_SONIC_CHARGE, SoundSource.PLAYERS, 1.0F, 1.5F);
			stack.hurtAndBreak(3, player, hand.asEquipmentSlot());
		}
		player.getCooldowns().addCooldown(stack, 160);
		return InteractionResult.SUCCESS;
	}
}
