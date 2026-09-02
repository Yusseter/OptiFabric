package me.modmuss50.optifabric.patcher.fixes;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import me.modmuss50.optifabric.util.RemappingUtils;

public class ImmediatelyFastRenderTypesFix implements ClassFixer {
    private static final String RENDER_TYPES = "class_12249";
    private static final String RENDER_TYPE_METHOD_DESC =
            "(Lnet/minecraft/class_2960;)Lnet/minecraft/class_1921;";

    private static final String BUILDER = "class_12247$class_12248";
    private static final String SORT_ON_UPLOAD = "method_75937";
    private static final String SORT_ON_UPLOAD_DESC =
            "()Lnet/minecraft/class_12247$class_12248;";

    private static final String[] TARGET_METHODS = {
            "method_75949",
            "method_75948",
            "method_75946"
    };

    @Override
    public void fix(ClassNode optifine, ClassNode minecraft) {
        String targetDesc =
                RemappingUtils.mapMethodDescriptor(RENDER_TYPE_METHOD_DESC);

        String builder =
                RemappingUtils.getClassName(BUILDER);

        String sortOnUpload =
                RemappingUtils.getMethodName(
                        BUILDER,
                        SORT_ON_UPLOAD,
                        SORT_ON_UPLOAD_DESC
                );

        String sortOnUploadDesc =
                RemappingUtils.mapMethodDescriptor(SORT_ON_UPLOAD_DESC);

        for (String target : TARGET_METHODS) {
            String targetName =
                    RemappingUtils.getMethodName(
                            RENDER_TYPES,
                            target,
                            RENDER_TYPE_METHOD_DESC
                    );

            MethodNode method =
                    findMethod(
                            optifine,
                            targetName,
                            targetDesc
                    );

            if (method == null) {
                throw new IllegalStateException(
                        "Could not find RenderTypes method " + target
                );
            }

            if (
                    hasInvoke(
                            method,
                            builder,
                            sortOnUpload,
                            sortOnUploadDesc
                    )
            ) {
                continue;
            }

            addDeadSortOnUploadAnchor(
                    method,
                    builder,
                    sortOnUpload,
                    sortOnUploadDesc
            );
        }
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

    private static void addDeadSortOnUploadAnchor(
            MethodNode method,
            String owner,
            String name,
            String desc
    ) {
        LabelNode skip = new LabelNode();

        InsnList anchor = new InsnList();
        anchor.add(new InsnNode(Opcodes.ICONST_0));
        anchor.add(new JumpInsnNode(Opcodes.IFEQ, skip));
        anchor.add(new InsnNode(Opcodes.ACONST_NULL));
        anchor.add(
                new MethodInsnNode(
                        Opcodes.INVOKEVIRTUAL,
                        owner,
                        name,
                        desc,
                        false
                )
        );
        anchor.add(new InsnNode(Opcodes.POP));
        anchor.add(skip);

        method.instructions.insert(anchor);
        method.maxStack = Math.max(method.maxStack, 1);
    }
}
