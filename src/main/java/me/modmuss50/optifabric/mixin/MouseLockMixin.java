package me.modmuss50.optifabric.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.client.input.SystemKeycodes;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.Window;

import me.modmuss50.optifabric.mod.StartupLog;

@Mixin(Mouse.class)
abstract class MouseLockMixin {
	@Shadow
	@Final
	private MinecraftClient client;

	@Shadow
	private boolean cursorLocked;

	@Shadow
	private double x;

	@Shadow
	private double y;

	@Shadow
	private boolean hasResolutionChanged;

	private static boolean optifabric$loggedFocusFallback;

	@Inject(method = "lockCursor", at = @At("HEAD"), cancellable = true)
	private void optifabric$lockCursorWhenFocusFlagIsStale(CallbackInfo info) {
		if (client.isWindowFocused()) {
			return;
		}

		if (client.currentScreen != null || client.world == null || client.player == null) {
			return;
		}

		if (!optifabric$loggedFocusFallback) {
			optifabric$loggedFocusFallback = true;
			StartupLog.record("mouse-lock-focus-fallback");
		}

		if (!cursorLocked) {
			if (SystemKeycodes.UPDATE_PRESSED_STATE_ON_MOUSE_GRAB) {
				KeyBinding.updatePressedStates();
			}

			cursorLocked = true;
			Window window = client.getWindow();
			x = window.getWidth() / 2D;
			y = window.getHeight() / 2D;
			InputUtil.setCursorParameters(window, InputUtil.GLFW_CURSOR_DISABLED, x, y);
			client.setScreen(null);
			((MinecraftClientAccess) client).optifabric$setAttackCooldown(10000);
			hasResolutionChanged = true;
		}

		info.cancel();
	}
}
