package me.modmuss50.optifabric.patcher.fixes;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import me.modmuss50.optifabric.util.RemappingUtils;

public class VideoOptionsScreenFix implements ClassFixer {
    private static final String VIDEO_OPTIONS_SCREEN = "class_446";
    private static final String GAME_OPTIONS_SCREEN = "class_4667";

    private static final String OPTIONS_DESC =
            "(Lnet/minecraft/class_315;)[Lnet/minecraft/class_7172;";
    private static final String ADD_OPTIONS_DESC = "()V";

    private static final String COMPONENT_DESC =
            "Lnet/minecraft/class_2561;";
    private static final String BODY_DESC =
            "Lnet/minecraft/class_353;";
    private static final String GAME_OPTIONS_DESC =
            "Lnet/minecraft/class_315;";
    private static final String SCREEN_DESC =
            "Lnet/minecraft/class_437;";

    private static final String[] HEADER_FIELDS = {
            "field_63538",
            "field_63539",
            "field_63540"
    };

    @Override
    public void fix(ClassNode optifine, ClassNode minecraft) {
        int restored = 0;
        int compatibilityMembers = 0;

        restored += restoreOptionMethod(
                optifine,
                minecraft,
                "method_75372",
                "optifabric$getBaselineQualityOptions",
                "optifabric$getQualityOptions"
        );

        restored += restoreOptionMethod(
                optifine,
                minecraft,
                "method_75373",
                "optifabric$getBaselineDisplayOptions",
                "optifabric$getDisplayOptions"
        );

        restored += restoreOptionMethod(
                optifine,
                minecraft,
                "method_75374",
                "optifabric$getBaselineInterfaceOptions",
                "optifabric$getInterfaceOptions"
        );

        String componentDesc =
                RemappingUtils.mapMethodDescriptor(COMPONENT_DESC);

        String[] headerNames =
                new String[HEADER_FIELDS.length];

        for (int i = 0; i < HEADER_FIELDS.length; i++) {
            String headerName = RemappingUtils.mapFieldName(
                    VIDEO_OPTIONS_SCREEN,
                    HEADER_FIELDS[i],
                    COMPONENT_DESC
            );

            headerNames[i] = headerName;

            if (addFieldIfMissing(
                    optifine,
                    Opcodes.ACC_PRIVATE
                            | Opcodes.ACC_STATIC
                            | Opcodes.ACC_FINAL,
                    headerName,
                    componentDesc
            )) {
                compatibilityMembers++;
            }
        }

        String bodyDesc =
                RemappingUtils.mapMethodDescriptor(BODY_DESC);
        String bodyName = RemappingUtils.mapFieldName(
                GAME_OPTIONS_SCREEN,
                "field_51824",
                BODY_DESC
        );

        if (addFieldIfMissing(
                optifine,
                Opcodes.ACC_PROTECTED,
                bodyName,
                bodyDesc
        )) {
            compatibilityMembers++;
        }

        String gameOptionsDesc =
                RemappingUtils.mapMethodDescriptor(GAME_OPTIONS_DESC);
        String gameOptionsName = RemappingUtils.mapFieldName(
                GAME_OPTIONS_SCREEN,
                "field_21336",
                GAME_OPTIONS_DESC
        );

        boolean addedGameOptions = addFieldIfMissing(
                optifine,
                Opcodes.ACC_PROTECTED | Opcodes.ACC_FINAL,
                gameOptionsName,
                gameOptionsDesc
        );

        if (addedGameOptions) {
            compatibilityMembers++;
        }

        String screenDesc =
                RemappingUtils.mapMethodDescriptor(SCREEN_DESC);
        String parentName = RemappingUtils.mapFieldName(
                GAME_OPTIONS_SCREEN,
                "field_21335",
                SCREEN_DESC
        );

        boolean addedParent = addFieldIfMissing(
                optifine,
                Opcodes.ACC_PROTECTED | Opcodes.ACC_FINAL,
                parentName,
                screenDesc
        );

        if (addedParent) {
            compatibilityMembers++;
        }

        if (addedGameOptions || addedParent) {
            initialiseCompatibilityFields(
                    optifine,
                    addedGameOptions,
                    gameOptionsName,
                    gameOptionsDesc,
                    addedParent,
                    parentName,
                    screenDesc
            );
        }

        String addOptionsName = RemappingUtils.getMethodName(
                GAME_OPTIONS_SCREEN,
                "method_60325",
                ADD_OPTIONS_DESC
        );

        if (!hasMethod(
                optifine,
                addOptionsName,
                ADD_OPTIONS_DESC
        )) {
            MethodNode method = new MethodNode(
                    Opcodes.ACC_PROTECTED,
                    addOptionsName,
                    ADD_OPTIONS_DESC,
                    null,
                    null
            );

            /*
             * These accesses reproduce the vanilla extension points without
             * rebuilding the vanilla screen itself. Third-party mixins can
             * inject their additional video settings here.
             */
            for (String headerName : headerNames) {
                method.instructions.add(
                        new FieldInsnNode(
                                Opcodes.GETSTATIC,
                                optifine.name,
                                headerName,
                                componentDesc
                        )
                );

                method.instructions.add(
                        new InsnNode(Opcodes.POP)
                );
            }

            method.instructions.add(
                    new InsnNode(Opcodes.RETURN)
            );

            method.maxStack = 1;
            method.maxLocals = 1;

            optifine.methods.add(method);
            compatibilityMembers++;
        }

        System.err.println(
                "[OptiFabric] VideoOptionsScreenFix restored "
                        + restored
                        + " vanilla option builders and added "
                        + compatibilityMembers
                        + " compatibility members"
        );
    }

    private static void initialiseCompatibilityFields(
            ClassNode optifine,
            boolean initialiseGameOptions,
            String gameOptionsName,
            String gameOptionsDesc,
            boolean initialiseParent,
            String parentName,
            String parentDesc
    ) {
        String clientName =
                RemappingUtils.getClassName("class_310");

        String getInstanceDesc =
                "()Lnet/minecraft/class_310;";

        String getInstanceName =
                RemappingUtils.getMethodName(
                        "class_310",
                        "method_1551",
                        getInstanceDesc
                );

        String mappedGetInstanceDesc =
                RemappingUtils.mapMethodDescriptor(
                        getInstanceDesc
                );

        String clientOptionsName =
                RemappingUtils.mapFieldName(
                        "class_310",
                        "field_1690",
                        GAME_OPTIONS_DESC
                );

        String screenName =
                RemappingUtils.getClassName("class_437");

        for (MethodNode method : optifine.methods) {
            if (!"<init>".equals(method.name)) {
                continue;
            }

            Type[] arguments =
                    Type.getArgumentTypes(method.desc);

            boolean firstArgumentIsScreen =
                    arguments.length > 0
                            && arguments[0].getSort()
                                    == Type.OBJECT
                            && screenName.equals(
                                    arguments[0].getInternalName()
                            );

            for (
                    AbstractInsnNode instruction =
                            method.instructions.getFirst();
                    instruction != null;
                    instruction = instruction.getNext()
            ) {
                if (instruction.getOpcode()
                        != Opcodes.RETURN) {
                    continue;
                }

                InsnList patch = new InsnList();

                if (initialiseGameOptions) {
                    patch.add(
                            new VarInsnNode(
                                    Opcodes.ALOAD,
                                    0
                            )
                    );

                    patch.add(
                            new MethodInsnNode(
                                    Opcodes.INVOKESTATIC,
                                    clientName,
                                    getInstanceName,
                                    mappedGetInstanceDesc,
                                    false
                            )
                    );

                    patch.add(
                            new FieldInsnNode(
                                    Opcodes.GETFIELD,
                                    clientName,
                                    clientOptionsName,
                                    gameOptionsDesc
                            )
                    );

                    patch.add(
                            new FieldInsnNode(
                                    Opcodes.PUTFIELD,
                                    optifine.name,
                                    gameOptionsName,
                                    gameOptionsDesc
                            )
                    );
                }

                if (
                        initialiseParent
                                && firstArgumentIsScreen
                ) {
                    patch.add(
                            new VarInsnNode(
                                    Opcodes.ALOAD,
                                    0
                            )
                    );

                    patch.add(
                            new VarInsnNode(
                                    Opcodes.ALOAD,
                                    1
                            )
                    );

                    patch.add(
                            new FieldInsnNode(
                                    Opcodes.PUTFIELD,
                                    optifine.name,
                                    parentName,
                                    parentDesc
                            )
                    );
                }

                method.instructions.insertBefore(
                        instruction,
                        patch
                );

                method.maxStack =
                        Math.max(method.maxStack, 2);
            }
        }
    }

    private static int restoreOptionMethod(
            ClassNode optifine,
            ClassNode minecraft,
            String intermediaryName,
            String baselineName,
            String bridgeName
    ) {
        String runtimeName =
                RemappingUtils.getMethodName(
                        VIDEO_OPTIONS_SCREEN,
                        intermediaryName,
                        OPTIONS_DESC
                );

        String runtimeDesc =
                RemappingUtils.mapMethodDescriptor(
                        OPTIONS_DESC
                );

        MethodNode vanilla = findMethod(
                minecraft,
                runtimeName,
                runtimeDesc
        );

        if (vanilla == null) {
            throw new IllegalStateException(
                    "Could not find vanilla VideoOptionsScreen method "
                            + intermediaryName
            );
        }

        removeMethod(
                optifine,
                runtimeName,
                runtimeDesc
        );

        removeMethod(
                optifine,
                baselineName,
                runtimeDesc
        );

        removeMethod(
                optifine,
                bridgeName,
                runtimeDesc
        );

        optifine.methods.add(
                cloneMethod(
                        vanilla,
                        runtimeName,
                        vanilla.access
                )
        );

        int bridgeAccess =
                (vanilla.access
                        & ~(Opcodes.ACC_PRIVATE
                                | Opcodes.ACC_PROTECTED))
                        | Opcodes.ACC_PUBLIC
                        | Opcodes.ACC_STATIC
                        | Opcodes.ACC_SYNTHETIC;

        optifine.methods.add(
                cloneMethod(
                        vanilla,
                        baselineName,
                        bridgeAccess
                )
        );

        MethodNode bridge = new MethodNode(
                Opcodes.ACC_PUBLIC
                        | Opcodes.ACC_STATIC
                        | Opcodes.ACC_SYNTHETIC,
                bridgeName,
                runtimeDesc,
                null,
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
                        Opcodes.INVOKESTATIC,
                        optifine.name,
                        runtimeName,
                        runtimeDesc,
                        false
                )
        );

        bridge.instructions.add(
                new InsnNode(Opcodes.ARETURN)
        );

        bridge.maxStack = 1;
        bridge.maxLocals = 1;

        optifine.methods.add(bridge);

        return 1;
    }

    private static MethodNode cloneMethod(
            MethodNode source,
            String name,
            int access
    ) {
        String[] exceptions =
                source.exceptions == null
                        ? null
                        : source.exceptions.toArray(
                                new String[
                                        source.exceptions.size()
                                ]
                        );

        MethodNode copy = new MethodNode(
                access,
                name,
                source.desc,
                source.signature,
                exceptions
        );

        source.accept(copy);

        return copy;
    }

    private static boolean addFieldIfMissing(
            ClassNode node,
            int access,
            String name,
            String desc
    ) {
        if (hasField(node, name, desc)) {
            return false;
        }

        node.fields.add(
                new FieldNode(
                        access,
                        name,
                        desc,
                        null,
                        null
                )
        );

        return true;
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

    private static void removeMethod(
            ClassNode node,
            String name,
            String desc
    ) {
        MethodNode method =
                findMethod(node, name, desc);

        if (method != null) {
            node.methods.remove(method);
        }
    }

    private static boolean hasMethod(
            ClassNode node,
            String name,
            String desc
    ) {
        return findMethod(node, name, desc) != null;
    }

    private static boolean hasField(
            ClassNode node,
            String name,
            String desc
    ) {
        for (FieldNode field : node.fields) {
            if (
                    name.equals(field.name)
                            && desc.equals(field.desc)
            ) {
                return true;
            }
        }

        return false;
    }
}