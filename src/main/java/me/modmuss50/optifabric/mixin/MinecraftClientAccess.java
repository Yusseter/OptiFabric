package me.modmuss50.optifabric.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.MinecraftClient;

@Mixin(MinecraftClient.class)
interface MinecraftClientAccess {
	@Accessor("attackCooldown")
	void optifabric$setAttackCooldown(int attackCooldown);
}
