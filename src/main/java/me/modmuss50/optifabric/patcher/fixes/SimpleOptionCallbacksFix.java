package me.modmuss50.optifabric.patcher.fixes;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

public class SimpleOptionCallbacksFix implements ClassFixer {
    private static final String CODEC_DESC =
            "()Lcom/mojang/serialization/Codec;";

    @Override
    public void fix(ClassNode optifine, ClassNode minecraft) {
        MethodNode vanillaCodec = findMethodByDescriptor(
                minecraft,
                CODEC_DESC
        );

        if (vanillaCodec == null) {
            throw new IllegalStateException(
                    "Could not find vanilla SimpleOption.Callbacks codec method"
            );
        }

        if (
                findMethod(
                        optifine,
                        vanillaCodec.name,
                        vanillaCodec.desc
                ) != null
        ) {
            return;
        }

        MethodNode optifineCodec = findMethodByDescriptor(
                optifine,
                CODEC_DESC
        );

        if (optifineCodec == null) {
            throw new IllegalStateException(
                    "Could not find OptiFine SimpleOption.Callbacks codec method"
            );
        }

        MethodNode bridge = new MethodNode(
                Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNTHETIC,
                vanillaCodec.name,
                vanillaCodec.desc,
                vanillaCodec.signature,
                null
        );

        bridge.instructions.add(
                new VarInsnNode(
                        Opcodes.ALOAD,
                        0
                )
        );

        bridge.instructions.add(
                new MethodInsnNode(
                        Opcodes.INVOKEINTERFACE,
                        optifine.name,
                        optifineCodec.name,
                        optifineCodec.desc,
                        true
                )
        );

        bridge.instructions.add(
                new InsnNode(Opcodes.ARETURN)
        );

        bridge.maxStack = 1;
        bridge.maxLocals = 1;

        optifine.methods.add(bridge);
    }

    private static MethodNode findMethod(
            ClassNode node,
            String name,
            String desc
    ) {
        for (MethodNode method : node.methods) {
            if (
                    name.equals(method.name)
                            && desc.equals(method.desc)
            ) {
                return method;
            }
        }

        return null;
    }

    private static MethodNode findMethodByDescriptor(
            ClassNode node,
            String desc
    ) {
        MethodNode match = null;

        for (MethodNode method : node.methods) {
            if (!desc.equals(method.desc)) {
                continue;
            }

            if (match != null) {
                throw new IllegalStateException(
                        "Found multiple SimpleOption.Callbacks codec candidates"
                );
            }

            match = method;
        }

        return match;
    }
}
