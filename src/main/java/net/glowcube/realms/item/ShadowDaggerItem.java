package net.glowcube.realms.item;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

/** Shadow Dagger: double damage from behind. Right-click to step through the shadows behind your target. */
public class ShadowDaggerItem extends LoreItem {
	public ShadowDaggerItem(Properties properties) {
		super(properties);
	}

	@Override
	public void postHurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
		super.postHurtEnemy(stack, target, attacker);
		if (attacker.level() instanceof ServerLevel level) {
			Vec3 facing = Vec3.directionFromRotation(0, target.getYRot());
			Vec3 toAttacker = attacker.position().subtract(target.position()).normalize();
			if (facing.dot(toAttacker) < -0.4) {
				target.hurtServer(level, level.damageSources().magic(), 7.0F);
				level.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY(0.6), target.getZ(), 25, 0.3, 0.3, 0.3, 0.3);
				level.playSound(null, target.blockPosition(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.0F, 0.6F);
			}
			target.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 0));
		}
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (player.getCooldowns().isOnCooldown(stack)) return InteractionResult.FAIL;
		Vec3 eye = player.getEyePosition();
		Vec3 end = eye.add(player.getLookAngle().scale(16.0));
		EntityHitResult hit = ProjectileUtil.getEntityHitResult(player, eye, end, new AABB(eye, end).inflate(1.0),
				e -> e instanceof LivingEntity le && Abilities.isValidTarget(le, player), 16.0 * 16.0);
		if (hit == null) return InteractionResult.FAIL;
		if (level instanceof ServerLevel server) {
			Entity target = hit.getEntity();
			Vec3 behind = target.position().subtract(Vec3.directionFromRotation(0, target.getYRot()).scale(1.6));
			server.sendParticles(ParticleTypes.LARGE_SMOKE, player.getX(), player.getY(1), player.getZ(), 30, 0.3, 0.6, 0.3, 0.02);
			player.teleportTo(behind.x, target.getY(), behind.z);
			player.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, target.getEyePosition());
			player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 40, 0, false, false));
			player.addEffect(new MobEffectInstance(MobEffects.SPEED, 60, 1, false, false));
			server.sendParticles(ParticleTypes.REVERSE_PORTAL, behind.x, target.getY(1), behind.z, 40, 0.3, 0.6, 0.3, 0.1);
			server.playSound(null, player.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 0.6F);
			stack.hurtAndBreak(1, player, hand.asEquipmentSlot());
		}
		player.getCooldowns().addCooldown(stack, 90);
		return InteractionResult.SUCCESS;
	}
}
