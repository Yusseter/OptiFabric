package me.modmuss50.optifabric.mixin;

import java.util.LinkedHashSet;
import java.util.Set;

import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.resource.ResourcePack;
import net.minecraft.resource.ResourceType;

@Pseudo
@Mixin(targets = "net.optifine.util.ResUtils", remap = false)
abstract class ResUtilsMixin {
    private static final String FABRIC_NIO_PACK =
            "net.fabricmc.fabric.impl.resource.pack.ModNioPackResources";

    @Dynamic("OptiFine resource pack file collector")
    @SuppressWarnings("target")
    @Inject(
            method = "collectFiles(Lnet/minecraft/class_3262;[Ljava/lang/String;[Ljava/lang/String;[Ljava/lang/String;)[Ljava/lang/String;",
            at = @At("HEAD"),
            cancellable = true,
            remap = false,
            require = 0)
    private static void collectFabricModResources(
            ResourcePack pack,
            String[] prefixes,
            String[] suffixes,
            String[] defaultPaths,
            CallbackInfoReturnable<String[]> callback) {

        if (!FABRIC_NIO_PACK.equals(pack.getClass().getName())) {
            return;
        }

        ResourceType type = ResourceType.CLIENT_RESOURCES;

        if (!pack.getNamespaces(type).contains("minecraft")) {
            callback.setReturnValue(new String[0]);
            return;
        }

        Set<String> files = new LinkedHashSet<>();

        pack.findResources(type, "minecraft", "", (id, supplier) -> {
            String path = id.getPath();

            if (startsWith(path, prefixes) && endsWith(path, suffixes)) {
                files.add(path);
            }
        });

        callback.setReturnValue(files.toArray(new String[0]));
    }

    private static boolean startsWith(String value, String[] prefixes) {
        if (prefixes == null || prefixes.length == 0) {
            return true;
        }

        for (String prefix : prefixes) {
            if (value.startsWith(prefix)) {
                return true;
            }
        }

        return false;
    }

    private static boolean endsWith(String value, String[] suffixes) {
        if (suffixes == null || suffixes.length == 0) {
            return true;
        }

        for (String suffix : suffixes) {
            if (value.endsWith(suffix)) {
                return true;
            }
        }

        return false;
    }
}