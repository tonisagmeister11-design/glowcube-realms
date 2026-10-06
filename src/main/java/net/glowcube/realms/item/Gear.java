package net.glowcube.realms.item;

import java.util.ArrayList;
import java.util.List;
import net.glowcube.realms.entity.projectile.GlowShardProjectile;
import net.glowcube.realms.world.RealmTeleporter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.EvokerFangs;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.hurtingprojectile.SmallFireball;
import net.minecraft.world.entity.projectile.hurtingprojectile.WitherSkull;
import net.minecraft.world.inventory.ShulkerBoxMenu;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.Holder;
import org.jspecify.annotations.Nullable;

/** All special-ability gear added in the second Glowcube update. */
public final class Gear {
	// ------------------------------------------------------------------ shared helpers
	static boolean ready(Player player, ItemStack stack) {
		return !player.getCooldowns().isOnCooldown(stack);
	}

	static void finish(Player player, ItemStack stack, InteractionHand hand, int cooldown, int damage) {
		player.getCooldowns().addCooldown(stack, cooldown);
		if (damage > 0 && player instanceof ServerPlayer) stack.hurtAndBreak(damage, player, hand.asEquipmentSlot());
	}

	static BlockHitResult lookBlock(Player player, double range) {
		Vec3 eye = player.getEyePosition();
		Vec3 end = eye.add(player.getLookAngle().scale(range));
		return player.level().clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
	}

	public static void lightningAt(ServerLevel level, Vec3 pos, @Nullable ServerPlayer cause) {
		LightningBolt bolt = EntityTypes.LIGHTNING_BOLT.create(level, EntitySpawnReason.TRIGGERED);
		if (bolt == null) return;
		bolt.snapTo(pos.x, pos.y, pos.z);
		bolt.setCause(cause);
		level.addFreshEntity(bolt);
	}

	// ------------------------------------------------------------------ boss weapons
	/** Infernal Maul: right-click Eruption, left-click Magma Wave. */
	public static class InfernalMaul extends LoreItem implements LeftClickAbility {
		public InfernalMaul(Properties p) {
			super(p);
		}

		@Override
		public void postHurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
			super.postHurtEnemy(stack, target, attacker);
			target.igniteForSeconds(5);
		}

		@Override
		public InteractionResult use(Level level, Player player, InteractionHand hand) {
			ItemStack stack = player.getItemInHand(hand);
			if (!ready(player, stack)) return InteractionResult.FAIL;
			if (level instanceof ServerLevel server) {
				for (LivingEntity e : Abilities.targets(server, player.position(), 6.0, player)) {
					e.hurtServer(server, server.damageSources().playerAttack(player), 10.0F);
					e.igniteForSeconds(6);
					Abilities.push(e, player.position(), 0.6, 0.9);
				}
				for (int r = 1; r <= 6; r++) Abilities.ring(server, ParticleTypes.FLAME, player.position().add(0, 0.2, 0), r, 10 + r * 6);
				server.sendParticles(ParticleTypes.LAVA, player.getX(), player.getY(), player.getZ(), 30, 2, 0.2, 2, 0);
				server.playSound(null, player.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.0F, 0.6F);
			}
			finish(player, stack, hand, 140, 3);
			return InteractionResult.SUCCESS;
		}

		@Override
		public void onLeftClick(ServerPlayer player, ItemStack stack) {
			if (!ready(player, stack)) return;
			ServerLevel level = player.level();
			Vec3 dir = new Vec3(player.getLookAngle().x, 0, player.getLookAngle().z).normalize();
			for (int i = 1; i <= 10; i++) {
				Vec3 p = player.position().add(dir.scale(i * 1.2));
				level.sendParticles(ParticleTypes.FLAME, p.x, p.y + 0.3, p.z, 12, 0.3, 0.3, 0.3, 0.02);
				for (LivingEntity e : Abilities.targets(level, p, 1.6, player)) {
					e.hurtServer(level, level.damageSources().playerAttack(player), 6.0F);
					e.igniteForSeconds(4);
				}
			}
			level.playSound(null, player.blockPosition(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1.0F, 0.7F);
			finish(player, stack, InteractionHand.MAIN_HAND, 60, 1);
		}
	}

	/** Void Reaver: right-click Blink, left-click Void Orb. */
	public static class VoidReaver extends LoreItem implements LeftClickAbility {
		public VoidReaver(Properties p) {
			super(p);
		}

		@Override
		public InteractionResult use(Level level, Player player, InteractionHand hand) {
			ItemStack stack = player.getItemInHand(hand);
			if (!ready(player, stack)) return InteractionResult.FAIL;
			if (level instanceof ServerLevel server) {
				Vec3 start = player.position();
				BlockHitResult hit = lookBlock(player, 14);
				Vec3 end = hit.getType() == HitResult.Type.MISS ? hit.getLocation() : hit.getLocation().subtract(player.getLookAngle().scale(1.0));
				for (int i = 0; i <= 16; i++) {
					Vec3 p = start.lerp(end, i / 16.0);
					server.sendParticles(ParticleTypes.REVERSE_PORTAL, p.x, p.y + 1, p.z, 6, 0.2, 0.4, 0.2, 0.05);
					for (LivingEntity e : Abilities.targets(server, p.add(0, 1, 0), 1.5, player)) e.hurtServer(server, server.damageSources().playerAttack(player), 7.0F);
				}
				player.teleportTo(end.x, end.y, end.z);
				player.fallDistance = 0;
				server.playSound(null, player.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.0F);
			}
			finish(player, stack, hand, 80, 2);
			return InteractionResult.SUCCESS;
		}

		@Override
		public void onLeftClick(ServerPlayer player, ItemStack stack) {
			if (!ready(player, stack)) return;
			GlowShardProjectile orb = GlowShardProjectile.voidOrb(player.level(), player);
			orb.setDamage(8.0F);
			orb.setSeeking(true);
			orb.shootFromRotation(player, player.getXRot(), player.getYRot(), 0, 1.6F, 0.5F);
			player.level().addFreshEntity(orb);
			player.level().playSound(null, player.blockPosition(), SoundEvents.ENDER_EYE_LAUNCH, SoundSource.PLAYERS, 1.0F, 0.8F);
			finish(player, stack, InteractionHand.MAIN_HAND, 20, 1);
		}
	}

	/** Frostbite Blade: freezes on hit, right-click Frost Nova. */
	public static class FrostbiteBlade extends LoreItem {
		public FrostbiteBlade(Properties p) {
			super(p);
		}

		@Override
		public void postHurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
			super.postHurtEnemy(stack, target, attacker);
			target.setTicksFrozen(Math.min(target.getTicksRequiredToFreeze() + 40, target.getTicksFrozen() + 60));
			target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 1));
		}

		@Override
		public InteractionResult use(Level level, Player player, InteractionHand hand) {
			ItemStack stack = player.getItemInHand(hand);
			if (!ready(player, stack)) return InteractionResult.FAIL;
			if (level instanceof ServerLevel server) {
				for (LivingEntity e : Abilities.targets(server, player.position(), 7.0, player)) {
					e.hurtServer(server, server.damageSources().freeze(), 8.0F);
					e.setTicksFrozen(e.getTicksRequiredToFreeze() + 100);
					e.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 100, 4));
				}
				for (int r = 1; r <= 7; r++) Abilities.ring(server, ParticleTypes.SNOWFLAKE, player.position().add(0, 0.4, 0), r, 10 + r * 6);
				server.playSound(null, player.blockPosition(), SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.2F, 0.6F);
			}
			finish(player, stack, hand, 160, 2);
			return InteractionResult.SUCCESS;
		}
	}

	/** Thunder Spear: right-click calls lightning where you look; hits may call lightning. */
	public static class ThunderSpear extends LoreItem {
		public ThunderSpear(Properties p) {
			super(p);
		}

		@Override
		public void postHurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
			super.postHurtEnemy(stack, target, attacker);
			if (attacker.level() instanceof ServerLevel level && attacker.getRandom().nextFloat() < 0.2F) {
				lightningAt(level, target.position(), attacker instanceof ServerPlayer sp ? sp : null);
			}
		}

		@Override
		public InteractionResult use(Level level, Player player, InteractionHand hand) {
			ItemStack stack = player.getItemInHand(hand);
			if (!ready(player, stack)) return InteractionResult.FAIL;
			if (level instanceof ServerLevel server) {
				BlockHitResult hit = lookBlock(player, 40);
				Vec3 pos = hit.getLocation();
				lightningAt(server, pos, (ServerPlayer) player);
				for (LivingEntity e : Abilities.targets(server, pos, 3.0, player)) e.hurtServer(server, server.damageSources().lightningBolt(), 6.0F);
			}
			finish(player, stack, hand, 120, 2);
			return InteractionResult.SUCCESS;
		}
	}

	/** Bone Scepter: right-click fang line, left-click wither skull. */
	public static class BoneScepter extends LoreItem implements LeftClickAbility {
		public BoneScepter(Properties p) {
			super(p);
		}

		@Override
		public InteractionResult use(Level level, Player player, InteractionHand hand) {
			ItemStack stack = player.getItemInHand(hand);
			if (!ready(player, stack)) return InteractionResult.FAIL;
			if (level instanceof ServerLevel server) {
				double ang = Math.toRadians(player.getYRot() + 90);
				for (int i = 1; i <= 14; i++) {
					double x = player.getX() + Math.cos(ang) * i * 1.2, z = player.getZ() + Math.sin(ang) * i * 1.2;
					server.addFreshEntity(new EvokerFangs(server, x, player.getY(), z, (float) ang, i, player));
				}
				server.playSound(null, player.blockPosition(), SoundEvents.EVOKER_CAST_SPELL, SoundSource.PLAYERS, 1.0F, 0.8F);
			}
			finish(player, stack, hand, 60, 1);
			return InteractionResult.SUCCESS;
		}

		@Override
		public void onLeftClick(ServerPlayer player, ItemStack stack) {
			if (!ready(player, stack)) return;
			Vec3 look = player.getLookAngle();
			WitherSkull skull = new WitherSkull(player.level(), player, look);
			skull.setPos(player.getX() + look.x, player.getEyeY() - 0.1, player.getZ() + look.z);
			player.level().addFreshEntity(skull);
			player.level().playSound(null, player.blockPosition(), SoundEvents.WITHER_SHOOT, SoundSource.PLAYERS, 0.8F, 1.2F);
			finish(player, stack, InteractionHand.MAIN_HAND, 30, 1);
		}
	}

	/** Storm Bow: arrows call lightning; sneak + right-click fires a triple volley. */
	public static class StormBow extends BowItem {
		public StormBow(Properties p) {
			super(p);
		}

		@Override
		protected Projectile createProjectile(Level level, LivingEntity shooter, ItemStack weapon, ItemStack projectile, boolean isCrit) {
			Projectile p = super.createProjectile(level, shooter, weapon, projectile, true);
			p.addTag("glowcube_storm");
			return p;
		}

		@Override
		public InteractionResult use(Level level, Player player, InteractionHand hand) {
			ItemStack stack = player.getItemInHand(hand);
			if (player.isShiftKeyDown() && ready(player, stack)) {
				if (level instanceof ServerLevel server) {
					for (int i = -1; i <= 1; i++) {
						AbstractArrow arrow = new net.minecraft.world.entity.projectile.arrow.Arrow(server, player, new ItemStack(net.minecraft.world.item.Items.ARROW), stack);
						arrow.shootFromRotation(player, player.getXRot(), player.getYRot() + i * 8, 0, 3.0F, 0.5F);
						arrow.setCritArrow(true);
						arrow.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
						arrow.addTag("glowcube_storm");
						server.addFreshEntity(arrow);
					}
					server.playSound(null, player.blockPosition(), SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS, 1.0F, 1.4F);
				}
				finish(player, stack, hand, 100, 3);
				return InteractionResult.SUCCESS;
			}
			return super.use(level, player, hand);
		}

		@Override
		public void appendHoverText(ItemStack stack, TooltipContext context, net.minecraft.world.item.component.TooltipDisplay display,
				java.util.function.Consumer<Component> builder, net.minecraft.world.item.TooltipFlag flag) {
			LoreItem.addLore(stack, builder);
		}
	}

	// ------------------------------------------------------------------ utility gear
	/** Gale Fan: right-click blows enemies away and launches you up. */
	public static class GaleFan extends LoreItem {
		public GaleFan(Properties p) {
			super(p);
		}

		@Override
		public InteractionResult use(Level level, Player player, InteractionHand hand) {
			ItemStack stack = player.getItemInHand(hand);
			if (!ready(player, stack)) return InteractionResult.FAIL;
			Vec3 look = player.getLookAngle();
			if (player.isShiftKeyDown()) {
				player.setDeltaMovement(player.getDeltaMovement().x, 1.1, player.getDeltaMovement().z);
			} else {
				player.push(-look.x * 0.6, 0.5, -look.z * 0.6);
			}
			player.fallDistance = 0;
			if (level instanceof ServerLevel server) {
				player.needsSync = true;
				for (LivingEntity e : Abilities.targets(server, player.position().add(look.scale(3)), 4.0, player)) {
					e.push(look.x * 2.0, 0.6, look.z * 2.0);
					e.needsSync = true;
				}
				for (int i = 1; i <= 8; i++) Abilities.ring(server, ParticleTypes.CLOUD, player.getEyePosition().add(look.scale(i)), 0.4 + i * 0.2, 8);
				server.playSound(null, player.blockPosition(), SoundEvents.BREEZE_WIND_CHARGE_BURST.value(), SoundSource.PLAYERS, 1.0F, 1.2F);
				if (player instanceof ServerPlayer sp) sp.connection.send(new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(sp));
			}
			finish(player, stack, hand, 50, 1);
			return InteractionResult.SUCCESS;
		}
	}

	/** Sky Pike: right-click lunges forward, hitting everything on the way. */
	public static class SkyPike extends LoreItem {
		public SkyPike(Properties p) {
			super(p);
		}

		@Override
		public InteractionResult use(Level level, Player player, InteractionHand hand) {
			ItemStack stack = player.getItemInHand(hand);
			if (!ready(player, stack)) return InteractionResult.FAIL;
			Vec3 look = player.getLookAngle();
			player.setDeltaMovement(look.x * 1.8, Math.max(0.3, look.y * 1.2), look.z * 1.8);
			if (level instanceof ServerLevel server) {
				for (int i = 1; i <= 6; i++) {
					Vec3 p = player.position().add(look.scale(i));
					for (LivingEntity e : Abilities.targets(server, p.add(0, 1, 0), 1.6, player)) {
						e.hurtServer(server, server.damageSources().playerAttack(player), 9.0F);
						Abilities.push(e, player.position(), 0.8, 0.3);
					}
					server.sendParticles(ParticleTypes.CRIT, p.x, p.y + 1, p.z, 6, 0.2, 0.2, 0.2, 0.1);
				}
				if (player instanceof ServerPlayer sp) sp.connection.send(new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(sp));
				server.playSound(null, player.blockPosition(), SoundEvents.TRIDENT_RIPTIDE_2.value(), SoundSource.PLAYERS, 1.0F, 1.2F);
			}
			finish(player, stack, hand, 70, 2);
			return InteractionResult.SUCCESS;
		}
	}

	/** Chrono Hourglass: right-click slows time for every enemy around you. */
	public static class ChronoHourglass extends LoreItem {
		public ChronoHourglass(Properties p) {
			super(p);
		}

		@Override
		public InteractionResult use(Level level, Player player, InteractionHand hand) {
			ItemStack stack = player.getItemInHand(hand);
			if (!ready(player, stack)) return InteractionResult.FAIL;
			if (level instanceof ServerLevel server) {
				for (LivingEntity e : Abilities.targets(server, player.position(), 14.0, player)) {
					e.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 120, 5));
					e.addEffect(new MobEffectInstance(MobEffects.MINING_FATIGUE, 120, 2));
					e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, 1));
				}
				player.addEffect(new MobEffectInstance(MobEffects.SPEED, 120, 2));
				player.addEffect(new MobEffectInstance(MobEffects.HASTE, 120, 1));
				Abilities.ring(server, ParticleTypes.ENCHANT, player.position().add(0, 1, 0), 6, 60);
				server.playSound(null, player.blockPosition(), SoundEvents.BELL_RESONATE, SoundSource.PLAYERS, 1.0F, 0.6F);
			}
			finish(player, stack, hand, 600, 1);
			return InteractionResult.SUCCESS;
		}
	}

	/** Aurora Charm: right-click removes all bad effects and heals. */
	public static class AuroraCharm extends LoreItem {
		public AuroraCharm(Properties p) {
			super(p);
		}

		@Override
		public InteractionResult use(Level level, Player player, InteractionHand hand) {
			ItemStack stack = player.getItemInHand(hand);
			if (!ready(player, stack)) return InteractionResult.FAIL;
			if (level instanceof ServerLevel server) {
				List<Holder<MobEffect>> bad = new ArrayList<>();
				for (MobEffectInstance e : player.getActiveEffects()) if (e.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) bad.add(e.getEffect());
				for (Holder<MobEffect> h : bad) player.removeEffect(h);
				player.clearFire();
				player.setTicksFrozen(0);
				player.heal(8.0F);
				player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 1));
				server.sendParticles(ParticleTypes.HAPPY_VILLAGER, player.getX(), player.getY(1), player.getZ(), 30, 0.5, 0.8, 0.5, 0);
				server.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.5F, 1.4F);
			}
			finish(player, stack, hand, 600, 1);
			return InteractionResult.SUCCESS;
		}
	}

	/** Grappling Hook: right-click pulls you toward the block you look at. */
	public static class GrapplingHook extends LoreItem {
		public GrapplingHook(Properties p) {
			super(p);
		}

		@Override
		public InteractionResult use(Level level, Player player, InteractionHand hand) {
			ItemStack stack = player.getItemInHand(hand);
			if (!ready(player, stack)) return InteractionResult.FAIL;
			BlockHitResult hit = lookBlock(player, 32);
			if (hit.getType() == HitResult.Type.MISS) return InteractionResult.FAIL;
			Vec3 pull = hit.getLocation().subtract(player.position()).normalize().scale(Math.min(2.6, hit.getLocation().distanceTo(player.position()) * 0.18 + 0.8));
			player.setDeltaMovement(pull.x, pull.y + 0.3, pull.z);
			player.fallDistance = 0;
			if (level instanceof ServerLevel server) {
				Abilities.line(server, ParticleTypes.CRIT, player.getEyePosition(), hit.getLocation(), 20);
				if (player instanceof ServerPlayer sp) sp.connection.send(new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(sp));
				server.playSound(null, player.blockPosition(), SoundEvents.FISHING_BOBBER_THROW, SoundSource.PLAYERS, 1.0F, 0.6F);
			}
			finish(player, stack, hand, 20, 1);
			return InteractionResult.SUCCESS;
		}
	}

	/** Phoenix Feather: saves you from death once (automatically); right-click for Phoenix Flames. */
	public static class PhoenixFeather extends LoreItem {
		public PhoenixFeather(Properties p) {
			super(p);
		}

		@Override
		public InteractionResult use(Level level, Player player, InteractionHand hand) {
			ItemStack stack = player.getItemInHand(hand);
			if (!ready(player, stack)) return InteractionResult.FAIL;
			if (level instanceof ServerLevel server) {
				player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 600, 0));
				for (LivingEntity e : Abilities.targets(server, player.position(), 6.0, player)) e.igniteForSeconds(8);
				Abilities.ring(server, ParticleTypes.FLAME, player.position().add(0, 0.5, 0), 3, 40);
				server.playSound(null, player.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1.0F, 1.4F);
			}
			finish(player, stack, hand, 400, 0);
			return InteractionResult.SUCCESS;
		}
	}

	/** Meteor Staff: right-click calls a meteor shower where you look. */
	public static class MeteorStaff extends LoreItem {
		public MeteorStaff(Properties p) {
			super(p);
		}

		@Override
		public InteractionResult use(Level level, Player player, InteractionHand hand) {
			ItemStack stack = player.getItemInHand(hand);
			if (!ready(player, stack)) return InteractionResult.FAIL;
			if (level instanceof ServerLevel server) {
				Vec3 t = lookBlock(player, 40).getLocation();
				for (int i = 0; i < 8; i++) {
					double ox = (player.getRandom().nextDouble() - 0.5) * 6, oz = (player.getRandom().nextDouble() - 0.5) * 6;
					SmallFireball ball = new SmallFireball(server, t.x + ox, t.y + 20 + i * 2, t.z + oz, new Vec3(0, -1.5, 0));
					ball.setOwner(player);
					server.addFreshEntity(ball);
				}
				for (LivingEntity e : Abilities.targets(server, t, 4.0, player)) e.hurtServer(server, server.damageSources().playerAttack(player), 6.0F);
				server.playSound(null, BlockPos.containing(t), SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, SoundSource.PLAYERS, 1.5F, 0.5F);
			}
			finish(player, stack, hand, 160, 3);
			return InteractionResult.SUCCESS;
		}
	}

	/** Void Pearl: instant teleport to where you look. */
	public static class VoidPearl extends LoreItem {
		public VoidPearl(Properties p) {
			super(p);
		}

		@Override
		public InteractionResult use(Level level, Player player, InteractionHand hand) {
			ItemStack stack = player.getItemInHand(hand);
			if (!ready(player, stack)) return InteractionResult.FAIL;
			BlockHitResult hit = lookBlock(player, 48);
			if (hit.getType() == HitResult.Type.MISS) return InteractionResult.FAIL;
			if (level instanceof ServerLevel server) {
				BlockPos target = hit.getBlockPos().relative(hit.getDirection());
				server.sendParticles(ParticleTypes.PORTAL, player.getX(), player.getY(1), player.getZ(), 40, 0.3, 0.8, 0.3, 0.3);
				player.teleportTo(target.getX() + 0.5, target.getY(), target.getZ() + 0.5);
				player.fallDistance = 0;
				server.playSound(null, target, SoundEvents.PLAYER_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.2F);
			}
			finish(player, stack, hand, 40, 1);
			return InteractionResult.SUCCESS;
		}
	}

	/** Warp Crystal: sneak + right-click binds the current spot, right-click warps back (works across dimensions). */
	public static class WarpCrystal extends LoreItem {
		public WarpCrystal(Properties p) {
			super(p);
		}

		@Override
		public InteractionResult use(Level level, Player player, InteractionHand hand) {
			ItemStack stack = player.getItemInHand(hand);
			if (!(level instanceof ServerLevel server)) return InteractionResult.SUCCESS;
			if (player.isShiftKeyDown()) {
				CompoundTag tag = new CompoundTag();
				tag.putString("Dim", level.dimension().identifier().toString());
				tag.putInt("X", player.getBlockX());
				tag.putInt("Y", player.getBlockY());
				tag.putInt("Z", player.getBlockZ());
				stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
				player.sendOverlayMessage(Component.translatable("message.glowcube_realms.warp_bound", player.getBlockX(), player.getBlockY(), player.getBlockZ()));
				server.playSound(null, player.blockPosition(), SoundEvents.RESPAWN_ANCHOR_SET_SPAWN, SoundSource.PLAYERS, 1.0F, 1.2F);
				return InteractionResult.SUCCESS;
			}
			if (!ready(player, stack)) return InteractionResult.FAIL;
			CustomData data = stack.get(DataComponents.CUSTOM_DATA);
			if (data == null || !data.copyTag().contains("Dim")) {
				player.sendOverlayMessage(Component.translatable("message.glowcube_realms.warp_unbound"));
				return InteractionResult.FAIL;
			}
			CompoundTag tag = data.copyTag();
			ServerLevel target = server.getServer().getLevel(ResourceKey.create(Registries.DIMENSION, Identifier.parse(tag.getStringOr("Dim", "minecraft:overworld"))));
			if (target == null) return InteractionResult.FAIL;
			server.sendParticles(ParticleTypes.REVERSE_PORTAL, player.getX(), player.getY(1), player.getZ(), 60, 0.4, 1, 0.4, 0.2);
			RealmTeleporter.sendTo(player, target, tag.getIntOr("X", 0) + 0.5, tag.getIntOr("Z", 0) + 0.5, (double) tag.getIntOr("Y", 64), false);
			finish(player, stack, hand, 600, 1);
			return InteractionResult.SUCCESS;
		}
	}

	/** Magnet Charm: pulls nearby items while in your inventory; right-click toggles it. */
	public static class MagnetCharm extends LoreItem {
		public MagnetCharm(Properties p) {
			super(p);
		}

		static boolean active(ItemStack stack) {
			CustomData data = stack.get(DataComponents.CUSTOM_DATA);
			return data == null || !data.copyTag().getBooleanOr("Off", false);
		}

		@Override
		public InteractionResult use(Level level, Player player, InteractionHand hand) {
			ItemStack stack = player.getItemInHand(hand);
			boolean nowOn = !active(stack);
			CompoundTag tag = new CompoundTag();
			tag.putBoolean("Off", !nowOn);
			stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
			player.sendOverlayMessage(Component.translatable(nowOn ? "message.glowcube_realms.magnet_on" : "message.glowcube_realms.magnet_off"));
			level.playSound(null, player.blockPosition(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.6F, nowOn ? 1.4F : 0.8F);
			return InteractionResult.SUCCESS;
		}

		@Override
		public void inventoryTick(ItemStack stack, ServerLevel level, Entity owner, net.minecraft.world.entity.@Nullable EquipmentSlot slot) {
			if (!(owner instanceof Player player) || player.isSpectator() || level.getGameTime() % 2 != 0 || !active(stack)) return;
			for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, new AABB(player.blockPosition()).inflate(8))) {
				Vec3 d = player.position().add(0, 0.5, 0).subtract(item.position());
				item.setDeltaMovement(item.getDeltaMovement().scale(0.6).add(d.normalize().scale(0.35)));
			}
		}

		@Override
		public boolean isFoil(ItemStack stack) {
			return active(stack);
		}
	}

	/** Backpack: 27 extra slots that travel with the item. */
	public static class Backpack extends LoreItem {
		public Backpack(Properties p) {
			super(p);
		}

		@Override
		public boolean canFitInsideContainerItems() {
			return false;
		}

		@Override
		public InteractionResult use(Level level, Player player, InteractionHand hand) {
			ItemStack stack = player.getItemInHand(hand);
			if (level.isClientSide()) return InteractionResult.SUCCESS;
			SimpleContainer container = new SimpleContainer(27) {
				@Override
				public void setChanged() {
					super.setChanged();
					List<ItemStack> items = new ArrayList<>();
					for (int i = 0; i < this.getContainerSize(); i++) items.add(this.getItem(i).copy());
					stack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(items));
				}
			};
			ItemContainerContents contents = stack.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
			net.minecraft.core.NonNullList<ItemStack> list = net.minecraft.core.NonNullList.withSize(27, ItemStack.EMPTY);
			contents.copyInto(list);
			for (int i = 0; i < 27; i++) container.setItem(i, list.get(i));
			player.openMenu(new SimpleMenuProvider((id, inv, p) -> new ShulkerBoxMenu(id, inv, container), stack.getHoverName()));
			level.playSound(null, player.blockPosition(), SoundEvents.ARMOR_EQUIP_LEATHER.value(), SoundSource.PLAYERS, 1.0F, 1.0F);
			return InteractionResult.SUCCESS;
		}
	}

	/** Tools with area mining - the actual block breaking happens in RealmEvents. */
	public static class Excavator extends LoreItem {
		public Excavator(Properties p) {
			super(p);
		}
	}

	public static class LumberAxe extends LoreItem {
		public LumberAxe(Properties p) {
			super(p);
		}
	}

	private Gear() {
	}
}
