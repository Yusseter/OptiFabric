package me.modmuss50.optifabric.mixin;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.fabricmc.loader.api.FabricLoader;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;

@Mixin(MinecraftClient.class)
abstract class InventoryProfilesNextCompatMixin {
    private static boolean optifabric$ipnResolved;
    private static Object optifabric$ipnHandler;
    private static Method optifabric$ipnPreScreenRender;
    private static boolean optifabric$ipnWarned;

    @Inject(method = "setScreen", at = @At("TAIL"))
    private void optifabric$cleanupIpnWidgets(Screen screen, CallbackInfo info) {
        if (!FabricLoader.getInstance().isModLoaded("inventoryprofilesnext")) {
            return;
        }

        optifabric$resolveIpn();

        if (optifabric$ipnHandler == null ||
                optifabric$ipnPreScreenRender == null) {
            return;
        }

        try {
            optifabric$ipnPreScreenRender.invoke(optifabric$ipnHandler);
        } catch (ReflectiveOperationException e) {
            if (!optifabric$ipnWarned) {
                optifabric$ipnWarned = true;

                System.err.println(
                        "[OptiFabric] Failed to run Inventory Profiles Next screen cleanup"
                );

                e.printStackTrace();
            }
        }
    }

    private static synchronized void optifabric$resolveIpn() {
        if (optifabric$ipnResolved) {
            return;
        }

        optifabric$ipnResolved = true;

        try {
            Class<?> handlerClass = Class.forName(
                    "org.anti_ad.mc.ipnext.gui.inject.InsertWidgetHandler"
            );

            Field instanceField = handlerClass.getField("INSTANCE");

            optifabric$ipnHandler = instanceField.get(null);
            optifabric$ipnPreScreenRender =
                    handlerClass.getMethod("preScreenRender");
        } catch (ReflectiveOperationException e) {
            if (!optifabric$ipnWarned) {
                optifabric$ipnWarned = true;

                System.err.println(
                        "[OptiFabric] Inventory Profiles Next compatibility hook could not be resolved"
                );

                e.printStackTrace();
            }
        }
    }
}