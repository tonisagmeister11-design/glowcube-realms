package net.glowcube.realms.client.render;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

/** Shared render state for all Glowcube mobs and bosses. */
public class RealmRenderState extends LivingEntityRenderState {
	public int phase = 1;
	public int attack;
	public float attackTicks;
	public boolean roaring;
	public float attackAnim;
	public boolean aggressive;
}
