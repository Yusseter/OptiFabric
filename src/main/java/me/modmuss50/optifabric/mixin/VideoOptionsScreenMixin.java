package me.modmuss50.optifabric.mixin;

import org.objectweb.asm.Opcodes;

import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.gui.screen.option.VideoOptionsScreen;

import net.fabricmc.loader.api.FabricLoader;

@Mixin(VideoOptionsScreen.class)
class VideoOptionsScreenMixin {
	@Dynamic("OptiFine-specific video options action method")
	@SuppressWarnings("target")
	@Inject(method = "actionPerformed(Lnet/optifine/gui/GuiButtonOF;I)V",
			slice = @Slice(from = @At(value = "CONSTANT", args = "intValue=231"), to = @At(value = "NEW", target = "net/optifine/shaders/gui/GuiShaders")),
			at = @At(value = "FIELD", target = "Lnet/minecraft/client/gui/screen/option/VideoOptionsScreen;client:Lnet/minecraft/client/MinecraftClient;", opcode = Opcodes.GETFIELD, remap = true),
			cancellable = true, remap = false, require = 0)
	private void actionPerformed(CallbackInfo call) {
		if (FabricLoader.getInstance().isModLoaded("satin")) {
			Config.callShowGuiMessage("Shaders are not compatible with the Satin mod", "Please remove this mod to enable Shaders");
			call.cancel();
		}
	}
}
