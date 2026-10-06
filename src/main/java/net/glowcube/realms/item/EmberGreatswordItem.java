package net.glowcube.realms.item;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.hurtingprojectile.SmallFireball;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Ember Greatsword: sets everything ablaze and hurls a fan of fireballs. */
public class EmberGreatswordItem extends LoreItem {
	public EmberGreatswordItem(Properties properties) {
		super(properties);
	}

	@Override
	public void postHurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
		super.postHurtEnemy(stack, target, attacker);
		target.igniteForSeconds(8.0F);
		if (attacker.level() instanceof ServerLevel level) {
			level.sendParticles(ParticleTypes.FLAME, target.getX(), target.getY(0.5), target.getZ(), 20, 0.3, 0.5, 0.3, 0.05);
			level.sendParticles(ParticleTypes.LAVA, target.getX(), target.getY(0.5), target.getZ(), 4, 0.2, 0.2, 0.2, 0.0);
		}
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (player.getCooldowns().isOnCooldown(stack)) return InteractionResult.FAIL;
		if (level instanceof ServerLevel server) {
			Vec3 look = player.getLookAngle();
			for (int i = -2; i <= 2; i++) {
				Vec3 dir = look.yRot((float) Math.toRadians(i * 9.0));
				SmallFireball ball = new SmallFireball(server, player, dir.scale(1.0));
				ball.setPos(player.getX() + dir.x, player.getEyeY() - 0.2, player.getZ() + dir.z);
				server.addFreshEntity(ball);
			}
			server.playSound(null, player.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1.2F, 0.8F);
			stack.hurtAndBreak(2, player, hand.asEquipmentSlot());
		}
		player.getCooldowns().addCooldown(stack, 110);
		return InteractionResult.SUCCESS;
	}
}
