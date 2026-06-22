package me.modmuss50.optifabric.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.MinecraftClient;

import me.modmuss50.optifabric.mod.StartupLog;

@Mixin(MinecraftClient.class)
abstract class MinecraftClientDiagnosticsMixin {
	@Inject(method = "scheduleStop", at = @At("HEAD"))
	private void optifabric$logScheduleStop(CallbackInfo info) {
		StartupLog.recordStack("minecraft-schedule-stop");
	}

	@Inject(method = "stop", at = @At("HEAD"))
	private void optifabric$logStop(CallbackInfo info) {
		StartupLog.recordStack("minecraft-stop");
	}

	@Inject(method = "close", at = @At("HEAD"))
	private void optifabric$logClose(CallbackInfo info) {
		StartupLog.recordStack("minecraft-close");
	}
}
