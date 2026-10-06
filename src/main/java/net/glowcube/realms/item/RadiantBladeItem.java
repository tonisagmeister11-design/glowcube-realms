package net.glowcube.realms.item;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Radiant Blade: smites undead, marks foes and unleashes a wave of light. */
public class RadiantBladeItem extends LoreItem {
	public RadiantBladeItem(Properties properties) {
		super(properties);
	}

	@Override
	public void postHurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
		super.postHurtEnemy(stack, target, attacker);
		target.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100, 0));
		if (attacker.level() instanceof ServerLevel level) {
			if (target.is(EntityTypeTags.UNDEAD)) {
				target.hurtServer(level, level.damageSources().magic(), 5.0F);
			}
			level.sendParticles(ParticleTypes.END_ROD, target.getX(), target.getY(0.5), target.getZ(), 10, 0.3, 0.4, 0.3, 0.08);
		}
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (player.getCooldowns().isOnCooldown(stack)) return InteractionResult.FAIL;
		if (level instanceof ServerLevel server) {
			Vec3 eye = player.getEyePosition();
			Vec3 look = player.getLookAngle();
			for (int i = 1; i <= 9; i++) {
				Vec3 p = eye.add(look.scale(i));
				Abilities.ring(server, ParticleTypes.END_ROD, p, 0.4 + i * 0.12, 10);
			}
			for (LivingEntity e : Abilities.targets(server, eye.add(look.scale(4.5)), 5.0, player)) {
				Vec3 to = e.position().subtract(player.position()).normalize();
				if (to.dot(look) < 0.3) continue;
				e.hurtServer(server, server.damageSources().indirectMagic(player, player), e.is(EntityTypeTags.UNDEAD) ? 16.0F : 11.0F);
				e.addEffect(new MobEffectInstance(MobEffects.GLOWING, 200, 0));
				Abilities.push(e, player.position(), 1.2, 0.35);
			}
			server.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.5F, 1.4F);
			server.playSound(null, player.blockPosition(), SoundEvents.TRIDENT_RIPTIDE_1.value(), SoundSource.PLAYERS, 1.0F, 1.6F);
			stack.hurtAndBreak(2, player, hand.asEquipmentSlot());
		}
		player.getCooldowns().addCooldown(stack, 100);
		return InteractionResult.SUCCESS;
	}
}
