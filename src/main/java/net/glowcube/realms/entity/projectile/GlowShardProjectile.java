package net.glowcube.realms.entity.projectile;

import java.util.List;
import net.glowcube.realms.item.Abilities;
import net.glowcube.realms.registry.ModEntities;
import net.glowcube.realms.registry.ModItems;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Crystal bolt used by the Aurora Staff and the Glowkeeper. Can optionally home in on targets. */
public class GlowShardProjectile extends ThrowableItemProjectile {
	private float damage = 6.0F;
	private boolean seeking;
	private boolean voidOrb;
	private boolean frost;

	public GlowShardProjectile(EntityType<? extends GlowShardProjectile> type, Level level) {
		super(type, level);
	}

	public GlowShardProjectile(Level level, LivingEntity owner) {
		super(ModEntities.GLOW_SHARD, owner, level, new ItemStack(ModItems.GLOW_SHARD));
	}

	public static GlowShardProjectile voidOrb(Level level, LivingEntity owner) {
		GlowShardProjectile p = new GlowShardProjectile(level, owner);
		p.voidOrb = true;
		p.setItem(new ItemStack(ModItems.VOID_SHARD));
		return p;
	}

	public static GlowShardProjectile frostShard(Level level, LivingEntity owner) {
		GlowShardProjectile p = new GlowShardProjectile(level, owner);
		p.frost = true;
		p.setItem(new ItemStack(net.minecraft.world.item.Items.BLUE_ICE));
		return p;
	}

	public void setDamage(float damage) {
		this.damage = damage;
	}

	public void setSeeking(boolean seeking) {
		this.seeking = seeking;
	}

	@Override
	protected Item getDefaultItem() {
		return ModItems.GLOW_SHARD;
	}

	@Override
	protected double getDefaultGravity() {
		return 0.0;
	}

	@Override
	public void tick() {
		super.tick();
		if (this.level() instanceof ServerLevel level) {
			ParticleOptions trail = this.frost ? ParticleTypes.SNOWFLAKE : this.voidOrb ? ParticleTypes.REVERSE_PORTAL : ParticleTypes.END_ROD;
			level.sendParticles(trail, this.getX(), this.getY(), this.getZ(), 1, 0.02, 0.02, 0.02, 0.0);
			if (this.seeking && this.tickCount > 4 && this.getOwner() instanceof LivingEntity owner) {
				LivingEntity best = null;
				double bestDist = 14 * 14;
				List<LivingEntity> list = Abilities.targets(level, this.position(), 14.0, owner);
				for (LivingEntity e : list) {
					double d = e.distanceToSqr(this);
					if (d < bestDist) {
						bestDist = d;
						best = e;
					}
				}
				if (best != null) {
					Vec3 want = best.getEyePosition().subtract(this.position()).normalize().scale(this.getDeltaMovement().length());
					this.setDeltaMovement(this.getDeltaMovement().lerp(want, 0.18));
				}
			}
			if (this.tickCount > 120) this.discard();
		}
	}

	@Override
	protected void onHitEntity(EntityHitResult hit) {
		super.onHitEntity(hit);
		Entity target = hit.getEntity();
		if (!(this.level() instanceof ServerLevel level)) return;
		Entity owner = this.getOwner();
		if (target == owner) return;
		if (owner instanceof LivingEntity livingOwner && target instanceof LivingEntity lt && !Abilities.isValidTarget(lt, livingOwner)) return;
		target.hurtServer(level, level.damageSources().thrown(this, owner), this.damage);
		if (target instanceof LivingEntity living) {
			if (this.frost) {
				living.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 2));
				living.setTicksFrozen(Math.min(living.getTicksRequiredToFreeze() + 60, living.getTicksFrozen() + 80));
			} else if (this.voidOrb) {
				living.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 80, 0));
				living.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 25, 0));
			} else {
				living.addEffect(new MobEffectInstance(MobEffects.GLOWING, 80, 0));
			}
		}
	}

	@Override
	protected void onHit(HitResult result) {
		super.onHit(result);
		if (this.level() instanceof ServerLevel level) {
			level.sendParticles(this.voidOrb ? ParticleTypes.PORTAL : ParticleTypes.END_ROD, this.getX(), this.getY(), this.getZ(), 16, 0.15, 0.15, 0.15, 0.1);
			this.discard();
		}
	}
}
