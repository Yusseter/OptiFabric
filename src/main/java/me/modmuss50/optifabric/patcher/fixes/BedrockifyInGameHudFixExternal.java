package me.modmuss50.optifabric.patcher.fixes;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

public final class BedrockifyInGameHudFixExternal {
    private static final String ITEM_TOOLTIPS_MIXIN =
            "me.juancarloscp52.bedrockify.mixin.client.features.heldItemTooltips.ItemTooltipsMixin";

    private static volatile Method featureEnabledMethod;
    private static volatile Method getInstanceMethod;
    private static volatile Field heldItemTooltipsField;
    private static volatile Method drawCustomTooltipsMethod;

    private BedrockifyInGameHudFixExternal() {
    }

    public static void drawCustomTooltips(
            DrawContext drawContext,
            TextRenderer textRenderer,
            Text text,
            int x,
            int y,
            int width,
            int color,
            ItemStack currentStack
    ) {
        if (!isBedrockifyTooltipMixinEnabled()) {
            drawContext.drawTextWithBackground(
                    textRenderer,
                    text,
                    x,
                    y,
                    width,
                    color
            );
            return;
        }

        try {
            Object client =
                    getBedrockifyClientInstanceMethod()
                            .invoke(null);

            if (client == null) {
                drawContext.drawTextWithBackground(
                        textRenderer,
                        text,
                        x,
                        y,
                        width,
                        color
                );
                return;
            }

            Object heldItemTooltips =
                    getHeldItemTooltipsField()
                            .get(client);

            if (heldItemTooltips == null) {
                drawContext.drawTextWithBackground(
                        textRenderer,
                        text,
                        x,
                        y,
                        width,
                        color
                );
                return;
            }

            int bedrockY =
                    MinecraftClient.getInstance()
                            .getWindow()
                            .getScaledHeight()
                            - 38;

            getDrawCustomTooltipsMethod(
                    heldItemTooltips.getClass()
            ).invoke(
                    heldItemTooltips,
                    drawContext,
                    textRenderer,
                    text,
                    (float) x,
                    (float) bedrockY,
                    color,
                    currentStack
            );
        } catch (
                IllegalAccessException |
                InvocationTargetException e
        ) {
            throw new IllegalStateException(
                    "Unable to invoke BedrockIfy held item tooltip renderer",
                    e
            );
        }
    }

    private static boolean isBedrockifyTooltipMixinEnabled() {
        if (
                FabricLoader.getInstance()
                        .isModLoaded(
                                "held-item-info"
                        )
        ) {
            return false;
        }

        try {
            Object enabled =
                    getFeatureEnabledMethod()
                            .invoke(
                                    null,
                                    ITEM_TOOLTIPS_MIXIN
                            );

            return Boolean.TRUE.equals(enabled);
        } catch (
                IllegalAccessException |
                InvocationTargetException e
        ) {
            throw new IllegalStateException(
                    "Unable to read BedrockIfy held item tooltip feature state",
                    e
            );
        }
    }

    private static Method getFeatureEnabledMethod() {
        Method method =
                featureEnabledMethod;

        if (method != null) {
            return method;
        }

        synchronized (BedrockifyInGameHudFixExternal.class) {
            method =
                    featureEnabledMethod;

            if (method != null) {
                return method;
            }

            try {
                Class<?> featureManager =
                        Class.forName(
                                "me.juancarloscp52.bedrockify.mixin.featureManager.MixinFeatureManager"
                        );

                method =
                        featureManager.getMethod(
                                "isFeatureEnabled",
                                String.class
                        );

                featureEnabledMethod =
                        method;

                return method;
            } catch (
                    ClassNotFoundException |
                    NoSuchMethodException e
            ) {
                throw new IllegalStateException(
                        "Unable to resolve BedrockIfy mixin feature manager",
                        e
                );
            }
        }
    }

    private static Method getBedrockifyClientInstanceMethod() {
        Method method =
                getInstanceMethod;

        if (method != null) {
            return method;
        }

        synchronized (BedrockifyInGameHudFixExternal.class) {
            method =
                    getInstanceMethod;

            if (method != null) {
                return method;
            }

            try {
                Class<?> client =
                        Class.forName(
                                "me.juancarloscp52.bedrockify.client.BedrockifyClient"
                        );

                method =
                        client.getMethod(
                                "getInstance"
                        );

                getInstanceMethod =
                        method;

                return method;
            } catch (
                    ClassNotFoundException |
                    NoSuchMethodException e
            ) {
                throw new IllegalStateException(
                        "Unable to resolve BedrockIfy client accessor",
                        e
                );
            }
        }
    }

    private static Field getHeldItemTooltipsField() {
        Field field =
                heldItemTooltipsField;

        if (field != null) {
            return field;
        }

        synchronized (BedrockifyInGameHudFixExternal.class) {
            field =
                    heldItemTooltipsField;

            if (field != null) {
                return field;
            }

            try {
                Class<?> client =
                        Class.forName(
                                "me.juancarloscp52.bedrockify.client.BedrockifyClient"
                        );

                field =
                        client.getField(
                                "heldItemTooltips"
                        );

                heldItemTooltipsField =
                        field;

                return field;
            } catch (
                    ClassNotFoundException |
                    NoSuchFieldException e
            ) {
                throw new IllegalStateException(
                        "Unable to resolve BedrockIfy heldItemTooltips field",
                        e
                );
            }
        }
    }

    private static Method getDrawCustomTooltipsMethod(
            Class<?> tooltipsClass
    ) {
        Method method =
                drawCustomTooltipsMethod;

        if (method != null) {
            return method;
        }

        synchronized (BedrockifyInGameHudFixExternal.class) {
            method =
                    drawCustomTooltipsMethod;

            if (method != null) {
                return method;
            }

            try {
                method =
                        tooltipsClass.getMethod(
                                "drawItemWithCustomTooltips",
                                DrawContext.class,
                                TextRenderer.class,
                                Text.class,
                                float.class,
                                float.class,
                                int.class,
                                ItemStack.class
                        );

                drawCustomTooltipsMethod =
                        method;

                return method;
            } catch (NoSuchMethodException e) {
                throw new IllegalStateException(
                        "Unable to resolve BedrockIfy held item tooltip renderer",
                        e
                );
            }
        }
    }
}
