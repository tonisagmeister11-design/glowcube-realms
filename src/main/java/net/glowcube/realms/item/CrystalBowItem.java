package net.glowcube.realms.item;

import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/** Crystal Bow: faster, harder hitting arrows that always crit and pierce. */
public class CrystalBowItem extends BowItem {
	public CrystalBowItem(Properties properties) {
		super(properties);
	}

	@Override
	protected Projectile createProjectile(Level level, LivingEntity shooter, ItemStack weapon, ItemStack projectile, boolean isCrit) {
		Projectile p = super.createProjectile(level, shooter, weapon, projectile, true);
		if (p instanceof AbstractArrow arrow) {
			arrow.setBaseDamage(4.5);
			arrow.setCritArrow(true);
		}
		return p;
	}

	@Override
	protected void shootProjectile(LivingEntity shooter, Projectile projectileEntity, int index, float power, float uncertainty, float angle,
			@Nullable LivingEntity target) {
		super.shootProjectile(shooter, projectileEntity, index, power * 1.35F, uncertainty * 0.3F, angle, target);
	}

	/** Sneak + right-click: Crystal Barrage, a fan of five glowing arrows. */
	@Override
	public net.minecraft.world.InteractionResult use(Level level, net.minecraft.world.entity.player.Player player, net.minecraft.world.InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (player.isShiftKeyDown() && !player.getCooldowns().isOnCooldown(stack)) {
			if (level instanceof net.minecraft.server.level.ServerLevel server) {
				for (int i = -2; i <= 2; i++) {
					AbstractArrow arrow = new net.minecraft.world.entity.projectile.arrow.SpectralArrow(server, player,
							new ItemStack(net.minecraft.world.item.Items.SPECTRAL_ARROW), stack);
					arrow.shootFromRotation(player, player.getXRot(), player.getYRot() + i * 7, 0, 3.0F, 0.4F);
					arrow.setBaseDamage(4.0);
					arrow.setCritArrow(true);
					arrow.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
					server.addFreshEntity(arrow);
				}
				server.playSound(null, player.blockPosition(), net.minecraft.sounds.SoundEvents.AMETHYST_CLUSTER_BREAK, net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.6F);
				stack.hurtAndBreak(3, player, hand.asEquipmentSlot());
			}
			player.getCooldowns().addCooldown(stack, 100);
			return net.minecraft.world.InteractionResult.SUCCESS;
		}
		return super.use(level, player, hand);
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
		LoreItem.addLore(stack, builder);
	}
}
