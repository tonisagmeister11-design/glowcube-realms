package net.glowcube.realms.item;

import java.util.List;
import net.glowcube.realms.entity.RealmGuardian;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Shared helpers for weapon abilities. */
public final class Abilities {
	/** Hostile-ish living entities around a point that a player's ability may hit. */
	public static List<LivingEntity> targets(ServerLevel level, Vec3 center, double radius, LivingEntity owner) {
		AABB box = new AABB(center, center).inflate(radius);
		return level.getEntitiesOfClass(LivingEntity.class, box, e -> isValidTarget(e, owner) && e.position().distanceTo(center) <= radius);
	}

	public static boolean isValidTarget(LivingEntity e, LivingEntity owner) {
		if (e == owner || !e.isAlive()) return false;
		if (owner instanceof Player) {
			if (e instanceof Player) return false;
			if (e instanceof AbstractVillager || e instanceof RealmGuardian) return false;
			if (e instanceof TamableAnimal tame && tame.isTame()) return false;
			return e instanceof Enemy || (e instanceof Mob mob && mob.getTarget() == owner);
		}
		return true;
	}

	public static void line(ServerLevel level, ParticleOptions particle, Vec3 from, Vec3 to, int count) {
		for (int i = 0; i <= count; i++) {
			Vec3 p = from.lerp(to, i / (double) count);
			level.sendParticles(particle, p.x, p.y, p.z, 1, 0.02, 0.02, 0.02, 0.0);
		}
	}

	public static void ring(ServerLevel level, ParticleOptions particle, Vec3 center, double radius, int count) {
		for (int i = 0; i < count; i++) {
			double a = i * Math.PI * 2 / count;
			level.sendParticles(particle, center.x + Math.cos(a) * radius, center.y, center.z + Math.sin(a) * radius, 1, 0, 0.02, 0, 0.01);
		}
	}

	public static void push(Entity target, Vec3 from, double strength, double up) {
		Vec3 dir = target.position().subtract(from);
		dir = new Vec3(dir.x, 0, dir.z);
		if (dir.lengthSqr() < 1.0E-4) dir = new Vec3(0, 0, 1);
		dir = dir.normalize().scale(strength);
		target.push(dir.x, up, dir.z);
		target.needsSync = true;
	}

	private Abilities() {
	}
}
