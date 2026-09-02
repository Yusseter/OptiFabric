package me.modmuss50.optifabric.patcher.fixes;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

public class ResourcePackOverridesOptionsFix implements ClassFixer {
    private static final String LOAD_METHOD = "method_1636";
    private static final String LOAD_DESC = "()V";
    private static final String KEY_MAPPING_CLASS = "net/minecraft/class_304";

    @Override
    public void fix(ClassNode optifine, ClassNode minecraft) {
        MethodNode vanillaLoad =
                findMethod(
                        minecraft,
                        LOAD_METHOD,
                        LOAD_DESC
                );

        MethodNode optifineLoad =
                findMethod(
                        optifine,
                        LOAD_METHOD,
                        LOAD_DESC
                );

        if (vanillaLoad == null || optifineLoad == null) {
            throw new IllegalStateException(
                    "Could not find Options.load method"
            );
        }

        MethodInsnNode vanillaReset =
                findKeyMappingReset(vanillaLoad);

        if (vanillaReset == null) {
            throw new IllegalStateException(
                    "Could not find vanilla KeyMapping reset call"
            );
        }

        if (
                containsCall(
                        optifineLoad,
                        vanillaReset
                )
        ) {
            return;
        }

        AbstractInsnNode returnInsn =
                findSingleReturn(optifineLoad);

        optifineLoad.instructions.insertBefore(
                returnInsn,
                new MethodInsnNode(
                        vanillaReset.getOpcode(),
                        vanillaReset.owner,
                        vanillaReset.name,
                        vanillaReset.desc,
                        vanillaReset.itf
                )
        );
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

    private static MethodInsnNode findKeyMappingReset(
            MethodNode method
    ) {
        MethodInsnNode match = null;

        for (
                AbstractInsnNode instruction =
                        method.instructions.getFirst();
                instruction != null;
                instruction = instruction.getNext()
        ) {
            if (!(instruction instanceof MethodInsnNode)) {
                continue;
            }

            MethodInsnNode call =
                    (MethodInsnNode) instruction;

            if (
                    call.getOpcode() != Opcodes.INVOKESTATIC
                            || !KEY_MAPPING_CLASS.equals(call.owner)
                            || !"()V".equals(call.desc)
            ) {
                continue;
            }

            if (match != null) {
                throw new IllegalStateException(
                        "Found multiple vanilla KeyMapping reset candidates"
                );
            }

            match = call;
        }

        return match;
    }

    private static boolean containsCall(
            MethodNode method,
            MethodInsnNode target
    ) {
        for (
                AbstractInsnNode instruction =
                        method.instructions.getFirst();
                instruction != null;
                instruction = instruction.getNext()
        ) {
            if (!(instruction instanceof MethodInsnNode)) {
                continue;
            }

            MethodInsnNode call =
                    (MethodInsnNode) instruction;

            if (
                    call.getOpcode() == target.getOpcode()
                            && target.owner.equals(call.owner)
                            && target.name.equals(call.name)
                            && target.desc.equals(call.desc)
            ) {
                return true;
            }
        }

        return false;
    }

    private static AbstractInsnNode findSingleReturn(
            MethodNode method
    ) {
        AbstractInsnNode match = null;

        for (
                AbstractInsnNode instruction =
                        method.instructions.getFirst();
                instruction != null;
                instruction = instruction.getNext()
        ) {
            if (instruction.getOpcode() != Opcodes.RETURN) {
                continue;
            }

            if (match != null) {
                throw new IllegalStateException(
                        "Found multiple Options.load returns"
                );
            }

            match = instruction;
        }

        if (match == null) {
            throw new IllegalStateException(
                    "Could not find Options.load return"
            );
        }

        return match;
    }
}
