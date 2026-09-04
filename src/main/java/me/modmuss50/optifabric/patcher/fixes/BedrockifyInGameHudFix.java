package me.modmuss50.optifabric.patcher.fixes;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import me.modmuss50.optifabric.util.RemappingUtils;

public class BedrockifyInGameHudFix implements ClassFixer {
    private static final String IN_GAME_HUD = "class_329";

    private static final String RENDER_HELD_ITEM_TOOLTIP = "method_1749";
    private static final String RENDER_HELD_ITEM_TOOLTIP_DESC =
            "(Lnet/minecraft/class_332;)V";

    private static final String OPTIFINE_HELPER =
            "renderSelectedItemName";
    private static final String OPTIFINE_HELPER_DESC =
            "(Lnet/minecraft/class_332;I)V";

    private static final String DRAW_CONTEXT = "class_332";
    private static final String DRAW_TEXT_WITH_BACKGROUND = "method_60649";
    private static final String DRAW_TEXT_WITH_BACKGROUND_DESC =
            "(Lnet/minecraft/class_327;Lnet/minecraft/class_2561;IIII)V";

    private static final String CURRENT_STACK = "field_2031";
    private static final String CURRENT_STACK_DESC =
            "Lnet/minecraft/class_1799;";

    private static final String BRIDGE_OWNER =
            "me/modmuss50/optifabric/patcher/fixes/BedrockifyInGameHudFixExternal";
    private static final String BRIDGE_METHOD =
            "drawCustomTooltips";
    private static final String BRIDGE_DESC =
            "(Lnet/minecraft/class_332;"
                    + "Lnet/minecraft/class_327;"
                    + "Lnet/minecraft/class_2561;"
                    + "IIII"
                    + "Lnet/minecraft/class_1799;)V";

    @Override
    public void fix(ClassNode optifine, ClassNode minecraft) {
        String wrapperName =
                RemappingUtils.getMethodName(
                        IN_GAME_HUD,
                        RENDER_HELD_ITEM_TOOLTIP,
                        RENDER_HELD_ITEM_TOOLTIP_DESC
                );

        String wrapperDesc =
                RemappingUtils.mapMethodDescriptor(
                        RENDER_HELD_ITEM_TOOLTIP_DESC
                );

        String helperDesc =
                RemappingUtils.mapMethodDescriptor(
                        OPTIFINE_HELPER_DESC
                );

        String drawOwner =
                RemappingUtils.getClassName(
                        DRAW_CONTEXT
                );

        String drawName =
                RemappingUtils.getMethodName(
                        DRAW_CONTEXT,
                        DRAW_TEXT_WITH_BACKGROUND,
                        DRAW_TEXT_WITH_BACKGROUND_DESC
                );

        String drawDesc =
                RemappingUtils.mapMethodDescriptor(
                        DRAW_TEXT_WITH_BACKGROUND_DESC
                );

        String currentStackName =
                RemappingUtils.mapFieldName(
                        IN_GAME_HUD,
                        CURRENT_STACK,
                        CURRENT_STACK_DESC
                );

        String currentStackDesc =
                RemappingUtils.mapMethodDescriptor(
                        CURRENT_STACK_DESC
                );

        String bridgeDesc =
                RemappingUtils.mapMethodDescriptor(
                        BRIDGE_DESC
                );

        MethodNode wrapper =
                findMethod(
                        optifine,
                        wrapperName,
                        wrapperDesc
                );

        if (wrapper == null) {
            throw new IllegalStateException(
                    "Could not find OptiFine InGameHud.renderHeldItemTooltip"
            );
        }

        MethodNode helper =
                findMethod(
                        optifine,
                        OPTIFINE_HELPER,
                        helperDesc
                );

        if (helper == null) {
            throw new IllegalStateException(
                    "Could not find OptiFine InGameHud.renderSelectedItemName"
            );
        }

        if (
                !hasInvoke(
                        wrapper,
                        optifine.name,
                        OPTIFINE_HELPER,
                        helperDesc
                )
        ) {
            throw new IllegalStateException(
                    "OptiFine renderHeldItemTooltip no longer delegates to renderSelectedItemName"
            );
        }

        if (
                hasInvoke(
                        wrapper,
                        drawOwner,
                        drawName,
                        drawDesc
                )
        ) {
            throw new IllegalStateException(
                    "Unexpected live BedrockIfy redirect target already exists in OptiFine wrapper"
            );
        }

        addDeadDrawAnchor(
                wrapper,
                drawOwner,
                drawName,
                drawDesc
        );

        patchOptiFineHelperDraw(
                optifine,
                helper,
                drawOwner,
                drawName,
                drawDesc,
                currentStackName,
                currentStackDesc,
                bridgeDesc
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

    private static boolean hasInvoke(
            MethodNode method,
            String owner,
            String name,
            String desc
    ) {
        for (AbstractInsnNode insn : method.instructions) {
            if (!(insn instanceof MethodInsnNode)) {
                continue;
            }

            MethodInsnNode methodInsn =
                    (MethodInsnNode) insn;

            if (
                    owner.equals(methodInsn.owner)
                            && name.equals(methodInsn.name)
                            && desc.equals(methodInsn.desc)
            ) {
                return true;
            }
        }

        return false;
    }

    private static void addDeadDrawAnchor(
            MethodNode method,
            String owner,
            String name,
            String desc
    ) {
        LabelNode skip =
                new LabelNode();

        InsnList anchor =
                new InsnList();

        anchor.add(
                new InsnNode(
                        Opcodes.ICONST_0
                )
        );

        anchor.add(
                new JumpInsnNode(
                        Opcodes.IFEQ,
                        skip
                )
        );

        anchor.add(
                new InsnNode(
                        Opcodes.ACONST_NULL
                )
        );

        anchor.add(
                new InsnNode(
                        Opcodes.ACONST_NULL
                )
        );

        anchor.add(
                new InsnNode(
                        Opcodes.ACONST_NULL
                )
        );

        anchor.add(
                new InsnNode(
                        Opcodes.ICONST_0
                )
        );

        anchor.add(
                new InsnNode(
                        Opcodes.ICONST_0
                )
        );

        anchor.add(
                new InsnNode(
                        Opcodes.ICONST_0
                )
        );

        anchor.add(
                new InsnNode(
                        Opcodes.ICONST_0
                )
        );

        anchor.add(
                new MethodInsnNode(
                        Opcodes.INVOKEVIRTUAL,
                        owner,
                        name,
                        desc,
                        false
                )
        );

        anchor.add(skip);

        method.instructions.insert(anchor);
        method.maxStack =
                Math.max(
                        method.maxStack,
                        7
                );
    }

    private static void patchOptiFineHelperDraw(
            ClassNode owner,
            MethodNode method,
            String drawOwner,
            String drawName,
            String drawDesc,
            String currentStackName,
            String currentStackDesc,
            String bridgeDesc
    ) {
        MethodInsnNode drawCall = null;
        int count = 0;

        for (AbstractInsnNode insn : method.instructions) {
            if (!(insn instanceof MethodInsnNode)) {
                continue;
            }

            MethodInsnNode methodInsn =
                    (MethodInsnNode) insn;

            if (
                    drawOwner.equals(methodInsn.owner)
                            && drawName.equals(methodInsn.name)
                            && drawDesc.equals(methodInsn.desc)
            ) {
                drawCall = methodInsn;
                count++;
            }
        }

        if (count != 1 || drawCall == null) {
            throw new IllegalStateException(
                    "Expected exactly one OptiFine selected-item text draw call, found "
                            + count
            );
        }

        InsnList currentStack =
                new InsnList();

        currentStack.add(
                new VarInsnNode(
                        Opcodes.ALOAD,
                        0
                )
        );

        currentStack.add(
                new FieldInsnNode(
                        Opcodes.GETFIELD,
                        owner.name,
                        currentStackName,
                        currentStackDesc
                )
        );

        method.instructions.insertBefore(
                drawCall,
                currentStack
        );

        method.instructions.set(
                drawCall,
                new MethodInsnNode(
                        Opcodes.INVOKESTATIC,
                        BRIDGE_OWNER,
                        BRIDGE_METHOD,
                        bridgeDesc,
                        false
                )
        );

        method.maxStack =
                Math.max(
                        method.maxStack,
                        8
                );
    }
}
