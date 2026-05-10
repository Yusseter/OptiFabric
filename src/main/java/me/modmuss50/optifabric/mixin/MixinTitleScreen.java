package me.modmuss50.optifabric.mixin;

import java.io.File;

import it.unimi.dsi.fastutil.booleans.BooleanConsumer;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;

import net.fabricmc.loader.api.FabricLoader;

import me.modmuss50.optifabric.mod.DrawContext;
import me.modmuss50.optifabric.mod.OptifabricError;
import me.modmuss50.optifabric.mod.OptifabricSetup;
import me.modmuss50.optifabric.mod.OptifineVersion;
import me.modmuss50.optifabric.mod.Text;

@Mixin(TitleScreen.class)
public abstract class MixinTitleScreen extends Screen {
	@Shadow
	private @Final boolean doBackgroundFade;
	@Shadow
	private long backgroundFadeStart;

	protected MixinTitleScreen() {
		super(null);
	}

	@SuppressWarnings("deprecation")
	@Inject(method = "init", at = @At("RETURN"))
	private void init(CallbackInfo info) {
		if (OptifabricError.hasError()) {
			String actionButtonText, helpButtonText;
			BooleanConsumer action;
			switch (OptifineVersion.jarType) {
			case SOMETHING_ELSE: //Valid jar states, we shouldn't be here
			case OPTIFINE_INSTALLER:
			case OPTIFINE_MOD:
				throw new IllegalStateException("No error to show!");

			case MISSING: //Errors relating to the OptiFine jar, link the mods folder
			case CORRUPT_ZIP:
			case INCOMPATIBLE:
			case DUPLICATED:
				actionButtonText = "Open mods folder";
				helpButtonText = "Open help";
				action = help -> {
					if (help) {
						Util.getOperatingSystem().open("https://github.com/Chocohead/OptiFabric/blob/master/README.md");
					} else {
						Util.getOperatingSystem().open(new File(FabricLoader.getInstance().getGameDirectory(), "mods"));
					}
				};
				break;

			case INTERNAL_ERROR: //Something wrong with OptiFabric itself
			default: {
				String stack = OptifabricError.getErrorLog();
				actionButtonText = stack != null ? "Copy stack-trace" : "Open logs folder";
				helpButtonText = "Open issues";
				action = help -> {
					if (help) {
						Util.getOperatingSystem().open("https://github.com/Chocohead/OptiFabric/issues");
					} else if (stack != null) {
						client.keyboard.setClipboard(stack);
					} else {
						Util.getOperatingSystem().open(new File(FabricLoader.getInstance().getGameDirectory(), "logs"));
					}
				};
				break;
			}
			}

			client.setScreen(new ConfirmScreen(action, Text.literal("There was an error loading OptiFabric!", Formatting.RED),
					Text.literal(OptifabricError.getError()), Text.literal(helpButtonText, Formatting.GREEN), Text.literal(actionButtonText)));
		}
	}

	@Inject(method = {"render(Lnet/minecraft/client/util/math/MatrixStack;IIF)V", "method_25394(Lnet/minecraft/class_332;IIF)V"}, at = @At("RETURN"), require = 1)
	private void render(@Coerce Object matricesOrContext, int mouseX, int mouseY, float delta, CallbackInfo info) {
		if (!OptifabricError.hasError()) {
			float fadeTime = doBackgroundFade ? (Util.getMeasuringTimeMs() - backgroundFadeStart) / 1000F : 1F;
			float fadeColor = doBackgroundFade ? MathHelper.clamp(fadeTime - 1F, 0F, 1F) : 1F;

			int alpha = MathHelper.ceil(fadeColor * 255F) << 24;
			if ((alpha & 0xFC000000) != 0) {
				DrawContext.drawTextWithShadow(textRenderer, matricesOrContext, OptifineVersion.version, 2, height -20, 0xFFFFFF | alpha);
			}
		}
	}
}
