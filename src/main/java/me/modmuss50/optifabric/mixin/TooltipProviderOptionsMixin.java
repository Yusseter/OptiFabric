package me.modmuss50.optifabric.mixin;

import java.lang.reflect.Method;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.option.SimpleOption;

import me.modmuss50.optifabric.compatibility.VideoOptionCompat;

@Pseudo
@Mixin(
        targets = "net.optifine.gui.TooltipProviderOptions",
        remap = false
)
abstract class TooltipProviderOptionsMixin {

    @Inject(
            method =
                    "getTooltipLines(Lnet/minecraft/class_339;I)[Ljava/lang/String;",
            at = @At("HEAD"),
            cancellable = true,
            remap = false,
            require = 0
    )
    private void optifabric$hideStaleReplacementTooltip(
            ClickableWidget widget,
            int width,
            CallbackInfoReturnable<String[]> call
    ) {
        Object option =
                optifabric$getControlOption(widget);

        if (
                option instanceof SimpleOption
                        && VideoOptionCompat.isReplacedOption(
                                (SimpleOption<?>) option
                        )
        ) {
            call.setReturnValue(null);
        }
    }

    private static Object optifabric$getControlOption(
            ClickableWidget widget
    ) {
        try {
            Method method =
                    widget.getClass().getMethod(
                            "getControlOption"
                    );

            return method.invoke(widget);
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }
}