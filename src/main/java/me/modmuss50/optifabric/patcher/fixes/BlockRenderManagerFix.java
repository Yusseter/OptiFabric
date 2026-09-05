package me.modmuss50.optifabric.patcher.fixes;

import java.util.HashSet;
import java.util.Set;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

public class BlockRenderManagerFix implements ClassFixer {
    private static final String RENDER_DAMAGE_DESC =
            "(Lnet/minecraft/class_2680;Lnet/minecraft/class_2338;Lnet/minecraft/class_1920;Lnet/minecraft/class_4587;Lnet/minecraft/class_4588;)V";

    private static final String RENDER_BLOCK_DESC =
            "(Lnet/minecraft/class_2680;Lnet/minecraft/class_4587;Lnet/minecraft/class_4597;II)V";

    private static final String CONTAINS_RENDERER_KEY =
            "fabric-renderer-api-v1:contains_renderer";

    @Override
    public void fix(ClassNode optifine, ClassNode minecraft) {
        Set<String> presentMethods =
                new HashSet<>();

        for (MethodNode method : optifine.methods) {
            presentMethods.add(
                    method.name + method.desc
            );
        }

        int added = 0;

        for (MethodNode method : minecraft.methods) {
            if (
                    "<init>".equals(method.name)
                            || "<clinit>".equals(method.name)
            ) {
                continue;
            }

            String key =
                    method.name + method.desc;

            if (!presentMethods.contains(key)) {
                optifine.methods.add(
                        copy(method)
                );

                added++;
            }
        }

        if (added > 0) {
            log(
                    "added "
                            + added
                            + " missing methods"
            );
        }

        if (isIndigoApplicable()) {
            boolean restoredDamage =
                    restoreMethod(
                            optifine,
                            minecraft,
                            "method_23071",
                            RENDER_DAMAGE_DESC
                    );

            boolean restoredBlock =
                    restoreMethod(
                            optifine,
                            minecraft,
                            "method_3353",
                            RENDER_BLOCK_DESC
                    );

            log(
                    "restored Indigo render entry points: "
                            + "method_23071="
                            + restoredDamage
                            + ", method_3353="
                            + restoredBlock
            );
        } else {
            log(
                    "Indigo renderer mixins are not applicable; "
                            + "leaving OptiFine render entry points intact"
            );
        }
    }

    private static boolean isIndigoApplicable() {
        FabricLoader loader =
                FabricLoader.getInstance();

        if (
                !loader.isModLoaded(
                        "fabric-renderer-indigo"
                )
        ) {
            return false;
        }

        for (
                ModContainer mod :
                loader.getAllMods()
        ) {
            if (
                    mod.getMetadata()
                            .containsCustomValue(
                                    CONTAINS_RENDERER_KEY
                            )
            ) {
                return false;
            }
        }

        return true;
    }

    private static boolean restoreMethod(
            ClassNode optifine,
            ClassNode minecraft,
            String name,
            String desc
    ) {
        MethodNode vanilla =
                findMethod(
                        minecraft,
                        name,
                        desc
                );

        if (vanilla == null) {
            throw new IllegalStateException(
                    "Vanilla BlockRenderManager method missing: "
                            + name
                            + desc
            );
        }

        MethodNode transformed =
                findMethod(
                        optifine,
                        name,
                        desc
                );

        if (transformed == null) {
            throw new IllegalStateException(
                    "OptiFine BlockRenderManager method missing: "
                            + name
                            + desc
            );
        }

        int index =
                optifine.methods.indexOf(
                        transformed
                );

        if (index < 0) {
            throw new IllegalStateException(
                    "Unable to locate OptiFine BlockRenderManager method index: "
                            + name
                            + desc
            );
        }

        optifine.methods.set(
                index,
                copy(vanilla)
        );

        return true;
    }

    private static MethodNode findMethod(
            ClassNode owner,
            String name,
            String desc
    ) {
        for (MethodNode method : owner.methods) {
            if (
                    name.equals(method.name)
                            && desc.equals(method.desc)
            ) {
                return method;
            }
        }

        return null;
    }

    private static MethodNode copy(
            MethodNode method
    ) {
        MethodNode copy =
                new MethodNode(
                        method.access,
                        method.name,
                        method.desc,
                        method.signature,
                        method.exceptions == null
                                ? null
                                : method.exceptions.toArray(
                                        new String[0]
                                )
                );

        method.accept(copy);

        return copy;
    }

    private static void log(
            String message
    ) {
        System.err.println(
                "[OptiFabric] BlockRenderManagerFix "
                        + message
        );

        System.err.flush();
    }
}
