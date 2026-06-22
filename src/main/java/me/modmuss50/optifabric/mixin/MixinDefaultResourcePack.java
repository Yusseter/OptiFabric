package me.modmuss50.optifabric.mixin;

import java.io.IOException;
import java.io.InputStream;

import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.resource.DefaultResourcePack;
import net.minecraft.resource.ResourceType;
import net.minecraft.util.Identifier;

import me.modmuss50.optifabric.mod.OptifineResources;

@Mixin(value = DefaultResourcePack.class, priority = 400)
abstract class MixinDefaultResourcePack {
	@Shadow
	@Dynamic("Legacy DefaultResourcePack helper used by old Minecraft versions")
	@SuppressWarnings("target")
	private static native String getPath(ResourceType type, Identifier id);

	@Dynamic("Legacy DefaultResourcePack method used by old Minecraft versions")
	@SuppressWarnings("target")
	@Inject(method = "findInputStream(Lnet/minecraft/resource/ResourceType;Lnet/minecraft/util/Identifier;)Ljava/io/InputStream;", at = @At("HEAD"), cancellable = true)
	protected void onFindInputStream(ResourceType type, Identifier id, CallbackInfoReturnable<InputStream> callback) {
		String path = getPath(type, id);

		try {
			InputStream stream = OptifineResources.INSTANCE.getResource(path);
			if (stream != null) callback.setReturnValue(stream);
		} catch (IOException e) {
			//Optifine does this if it goes wrong so we will too
			e.printStackTrace();
		}
	}

	@Dynamic("Legacy DefaultResourcePack method used by old Minecraft versions")
	@SuppressWarnings("target")
	@Inject(method = "contains(Lnet/minecraft/resource/ResourceType;Lnet/minecraft/util/Identifier;)Z", at = @At("HEAD"), cancellable = true)
	public void doesContain(ResourceType type, Identifier id, CallbackInfoReturnable<Boolean> callback) {
		String path = getPath(type, id);

		if (OptifineResources.INSTANCE.hasResource(path)) callback.setReturnValue(true);
	}
}
