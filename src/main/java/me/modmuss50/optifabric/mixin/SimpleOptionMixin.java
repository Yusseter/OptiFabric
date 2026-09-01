package me.modmuss50.optifabric.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.option.VideoOptionsScreen;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.SimpleOption;

import me.modmuss50.optifabric.compatibility.VideoOptionCompat;

@Mixin(SimpleOption.class)
abstract class SimpleOptionMixin {
    @Inject(
            method =
                    "createWidget(Lnet/minecraft/client/option/GameOptions;III)Lnet/minecraft/client/gui/widget/ClickableWidget;",
            at = @At("HEAD"),
            cancellable = true
    )
    private void optifabric$useModifiedVideoOption(
            GameOptions options,
            int x,
            int y,
            int width,
            CallbackInfoReturnable<ClickableWidget> call
    ) {
        MinecraftClient client =
                MinecraftClient.getInstance();

        Screen screen = client.currentScreen;

        if (screen == null) {
            return;
        }

        String screenName =
                screen.getClass().getName();

        boolean videoSettings =
                screen instanceof VideoOptionsScreen
                        || (
                                screenName.startsWith(
                                        "net.optifine.gui."
                                )
                                && screenName.contains(
                                        "Settings"
                                )
                        );

        if (!videoSettings) {
            return;
        }

        SimpleOption<?> original =
                (SimpleOption<?>) (Object) this;

        SimpleOption<?> replacement =
                VideoOptionCompat.getReplacement(
                        original,
                        options
                );

        if (replacement == original) {
            return;
        }

        call.setReturnValue(
                replacement.createWidget(
                        options,
                        x,
                        y,
                        width
                )
        );
    }
}