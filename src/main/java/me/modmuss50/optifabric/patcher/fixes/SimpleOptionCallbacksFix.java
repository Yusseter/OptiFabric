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

        MethodNode optifineCodec = findMethodByDescriptor(
                optifine,
                CODEC_DESC
        );

        if (optifineCodec == null) {
            throw new IllegalStateException(
                    "Could not find OptiFine SimpleOption.Callbacks codec method"
            );
        }

        MethodNode vanillaBridge = findMethod(
                optifine,
                vanillaCodec.name,
                vanillaCodec.desc
        );

        if (vanillaBridge == null) {
            vanillaBridge = createBridge(
                    optifine,
                    vanillaCodec,
                    optifineCodec
            );

            optifine.methods.add(vanillaBridge);
        }

        if ((optifineCodec.access & Opcodes.ACC_ABSTRACT) != 0) {
            makeDefaultBridge(
                    optifine,
                    optifineCodec,
                    vanillaBridge
            );
        }
    }

    private static MethodNode createBridge(
            ClassNode owner,
            MethodNode method,
            MethodNode target
    ) {
        MethodNode bridge = new MethodNode(
                Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNTHETIC,
                method.name,
                method.desc,
                method.signature,
                null
        );

        addBridgeInstructions(
                owner,
                bridge,
                target
        );

        return bridge;
    }

    private static void makeDefaultBridge(
            ClassNode owner,
            MethodNode method,
            MethodNode target
    ) {
        method.access &= ~Opcodes.ACC_ABSTRACT;
        method.instructions.clear();

        addBridgeInstructions(
                owner,
                method,
                target
        );
    }

    private static void addBridgeInstructions(
            ClassNode owner,
            MethodNode method,
            MethodNode target
    ) {
        method.instructions.add(
                new VarInsnNode(
                        Opcodes.ALOAD,
                        0
                )
        );

        method.instructions.add(
                new MethodInsnNode(
                        Opcodes.INVOKEINTERFACE,
                        owner.name,
                        target.name,
                        target.desc,
                        true
                )
        );

        method.instructions.add(
                new InsnNode(Opcodes.ARETURN)
        );

        method.maxStack = 1;
        method.maxLocals = 1;
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
