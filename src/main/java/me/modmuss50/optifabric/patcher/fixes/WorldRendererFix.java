package me.modmuss50.optifabric.patcher.fixes;

import java.util.HashSet;
import java.util.Set;

import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

public class WorldRendererFix implements ClassFixer {
    private static final String WR = "net/minecraft/class_761";
    private static final String EVENTS =
            "net/fabricmc/fabric/api/client/rendering/v1/world/WorldRenderEvents";
    private static final String EVENT = "net/fabricmc/fabric/api/event/Event";
    private static final String CONTEXT =
            "net/fabricmc/fabric/impl/client/rendering/world/WorldRenderContextImpl";

    private static final String EVENT_DESC =
            "Lnet/fabricmc/fabric/api/event/Event;";
    private static final String CONTEXT_DESC =
            "Lnet/fabricmc/fabric/impl/client/rendering/world/WorldRenderContextImpl;";
    private static final String WORLD_CONTEXT_DESC =
            "(Lnet/fabricmc/fabric/api/client/rendering/v1/world/WorldRenderContext;)V";
    private static final String TERRAIN_CONTEXT_DESC =
            "(Lnet/fabricmc/fabric/api/client/rendering/v1/world/WorldTerrainRenderContext;)V";

    private static final String VANILLA_RENDER_DESC =
            "(Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lnet/minecraft/class_11658;Lnet/minecraft/class_3695;Lorg/joml/Matrix4f;Lnet/minecraft/class_9925;Lnet/minecraft/class_9925;ZLnet/minecraft/class_9925;Lnet/minecraft/class_9925;)V";
    private static final String OPTIFINE_RENDER_DESC =
            "(Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lnet/minecraft/class_11658;Lnet/minecraft/class_3695;Lorg/joml/Matrix4f;Lnet/minecraft/class_9925;Lnet/minecraft/class_9925;ZLnet/minecraft/class_9925;Lnet/minecraft/class_9925;Lnet/minecraft/class_9779;)V";
    private static final String SECTION_STATE_DESC =
            "(Lorg/joml/Matrix4fc;DDD)Lnet/minecraft/class_11532;";
    private static final String RENDER_LAYER_DESC =
            "(Lnet/minecraft/class_11531;Lnet/minecraft/class_12137;)V";
    private static final String PREPARE_DESC =
            "(Lnet/minecraft/class_757;Lnet/minecraft/class_761;Lnet/minecraft/class_11658;Lnet/minecraft/class_11532;Lnet/minecraft/class_11659;Lnet/minecraft/class_4597;)V";

    @Override
    public void fix(ClassNode optifine, ClassNode minecraft) {
        Set<String> present = new HashSet<>();
        for (MethodNode method : optifine.methods) present.add(method.name + method.desc);

        int added = 0;
        for (MethodNode method : minecraft.methods) {
            if ("<init>".equals(method.name) || "<clinit>".equals(method.name)) continue;

            if (present.add(method.name + method.desc)) {
                optifine.methods.add(copy(method));
                added++;
            }
        }

        if (added > 0) log("added " + added + " missing methods");
        bridgeFabricMainRender(optifine);
    }

    private static MethodNode copy(MethodNode method) {
        MethodNode copy = new MethodNode(
                method.access, method.name, method.desc, method.signature,
                method.exceptions == null ? null : method.exceptions.toArray(new String[0])
        );
        method.accept(copy);
        return copy;
    }

    private static void bridgeFabricMainRender(ClassNode owner) {
        if (!FabricLoader.getInstance().isModLoaded("fabric-rendering-v1")) return;

        requireMethod(owner, "method_62214", VANILLA_RENDER_DESC);
        MethodNode render = requireMethod(owner, "method_62214", OPTIFINE_RENDER_DESC);

        int existing = countMainEvents(render);
        if (existing == 5) {
            log("Fabric main render event bridge already present");
            return;
        }
        if (existing != 0) {
            throw new IllegalStateException(
                    "Partial Fabric world render event bridge found: " + existing
            );
        }

        VarInsnNode sectionStore =
                storeAfterInvoke(render, WR, "method_72157", SECTION_STATE_DESC);

        MethodInsnNode startMain =
                firstInvokeAfter(
                        sectionStore,
                        "net/minecraft/class_11532",
                        "method_72170",
                        RENDER_LAYER_DESC
                );

        VarInsnNode matrixStore = matrixStackStore(render);

        LdcInsnNode beforeEntities =
                profilerAnchor(render, "submitEntities", "method_15405");

        MethodInsnNode afterEntities =
                firstInvokeAfter(
                        beforeEntities,
                        "net/minecraft/class_4618",
                        "method_23285",
                        "()V"
                );

        LdcInsnNode beforeTranslucent =
                profilerAnchor(render, "translucent", "method_15396");

        MethodInsnNode endMain =
                lastInvoke(
                        render,
                        "net/minecraft/class_4597$class_4598",
                        "method_22993",
                        "()V"
                );

        render.instructions.insert(sectionStore, contextPrepare(sectionStore.var));

        render.instructions.insertBefore(
                startMain,
                event("START_MAIN", "StartMain", "startMain", TERRAIN_CONTEXT_DESC)
        );

        render.instructions.insert(matrixStore, setMatrixStack(matrixStore.var));

        render.instructions.insertBefore(
                beforeEntities,
                event("BEFORE_ENTITIES", "BeforeEntities", "beforeEntities", WORLD_CONTEXT_DESC)
        );

        render.instructions.insert(
                afterEntities,
                event("AFTER_ENTITIES", "AfterEntities", "afterEntities", WORLD_CONTEXT_DESC)
        );

        render.instructions.insertBefore(
                beforeTranslucent,
                event(
                        "BEFORE_TRANSLUCENT",
                        "BeforeTranslucent",
                        "beforeTranslucent",
                        WORLD_CONTEXT_DESC
                )
        );

        render.instructions.insertBefore(
                endMain,
                event("END_MAIN", "EndMain", "endMain", WORLD_CONTEXT_DESC)
        );

        render.maxStack = Math.max(render.maxStack, 12);

        log(
                "bridged Fabric main render events into method_62214"
                        + render.desc
                        + " sectionStateLocal=" + sectionStore.var
                        + " matrixStackLocal=" + matrixStore.var
        );
    }

    private static MethodNode requireMethod(ClassNode owner, String name, String desc) {
        for (MethodNode method : owner.methods) {
            if (name.equals(method.name) && desc.equals(method.desc)) return method;
        }

        throw new IllegalStateException(
                "Expected WorldRenderer method missing: " + name + desc
        );
    }

    private static VarInsnNode storeAfterInvoke(
            MethodNode method, String owner, String name, String desc
    ) {
        VarInsnNode found = null;
        int matches = 0;

        for (AbstractInsnNode insn : method.instructions.toArray()) {
            if (!isInvoke(insn, owner, name, desc)) continue;

            matches++;
            AbstractInsnNode next = nextReal(insn.getNext());

            if (!(next instanceof VarInsnNode) || next.getOpcode() != Opcodes.ASTORE) {
                throw new IllegalStateException(
                        "Expected ASTORE after " + owner + "." + name + desc
                );
            }

            found = (VarInsnNode) next;
        }

        if (matches != 1) {
            throw new IllegalStateException(
                    "Expected exactly one "
                            + owner + "." + name + desc
                            + ", found " + matches
            );
        }

        return found;
    }

    private static VarInsnNode matrixStackStore(MethodNode method) {
        VarInsnNode found = null;
        int matches = 0;

        for (AbstractInsnNode insn : method.instructions.toArray()) {
            if (!(insn instanceof TypeInsnNode)
                    || insn.getOpcode() != Opcodes.NEW
                    || !"net/minecraft/class_4587".equals(((TypeInsnNode) insn).desc)) {
                continue;
            }

            AbstractInsnNode cursor = nextReal(insn.getNext());

            if (cursor != null && cursor.getOpcode() == Opcodes.DUP) {
                cursor = nextReal(cursor.getNext());
            }

            if (!isInvoke(cursor, "net/minecraft/class_4587", "<init>", "()V")) continue;

            cursor = nextReal(cursor.getNext());

            if (!(cursor instanceof VarInsnNode) || cursor.getOpcode() != Opcodes.ASTORE) {
                throw new IllegalStateException(
                        "Expected ASTORE after MatrixStack construction"
                );
            }

            matches++;
            found = (VarInsnNode) cursor;
        }

        if (matches != 1) {
            throw new IllegalStateException(
                    "Expected exactly one main MatrixStack construction, found " + matches
            );
        }

        return found;
    }

    private static MethodInsnNode firstInvokeAfter(
            AbstractInsnNode start, String owner, String name, String desc
    ) {
        for (
                AbstractInsnNode insn = start.getNext();
                insn != null;
                insn = insn.getNext()
        ) {
            if (isInvoke(insn, owner, name, desc)) return (MethodInsnNode) insn;
        }

        throw new IllegalStateException(
                "Unable to find " + owner + "." + name + desc + " after anchor"
        );
    }

    private static LdcInsnNode profilerAnchor(
            MethodNode method, String marker, String methodName
    ) {
        LdcInsnNode found = null;
        int matches = 0;

        for (AbstractInsnNode insn : method.instructions.toArray()) {
            if (!(insn instanceof LdcInsnNode)
                    || !marker.equals(((LdcInsnNode) insn).cst)) {
                continue;
            }

            if (!isInvoke(
                    nextReal(insn.getNext()),
                    "net/minecraft/class_3695",
                    methodName,
                    "(Ljava/lang/String;)V"
            )) {
                continue;
            }

            matches++;
            found = (LdcInsnNode) insn;
        }

        if (matches != 1) {
            throw new IllegalStateException(
                    "Expected exactly one profiler marker '"
                            + marker + "', found " + matches
            );
        }

        return found;
    }

    private static MethodInsnNode lastInvoke(
            MethodNode method, String owner, String name, String desc
    ) {
        MethodInsnNode found = null;

        for (AbstractInsnNode insn : method.instructions.toArray()) {
            if (isInvoke(insn, owner, name, desc)) {
                found = (MethodInsnNode) insn;
            }
        }

        if (found == null) {
            throw new IllegalStateException(
                    "Unable to find final " + owner + "." + name + desc
            );
        }

        return found;
    }

    private static int countMainEvents(MethodNode method) {
        Set<String> found = new HashSet<>();

        for (AbstractInsnNode insn : method.instructions.toArray()) {
            if (!(insn instanceof FieldInsnNode)
                    || insn.getOpcode() != Opcodes.GETSTATIC) {
                continue;
            }

            FieldInsnNode field = (FieldInsnNode) insn;

            if (!EVENTS.equals(field.owner) || !isMainEvent(field.name)) continue;

            if (!found.add(field.name)) {
                throw new IllegalStateException(
                        "Duplicate Fabric world render event bridge: " + field.name
                );
            }
        }

        return found.size();
    }

    private static boolean isMainEvent(String name) {
        return "START_MAIN".equals(name)
                || "BEFORE_ENTITIES".equals(name)
                || "AFTER_ENTITIES".equals(name)
                || "BEFORE_TRANSLUCENT".equals(name)
                || "END_MAIN".equals(name);
    }

    private static InsnList contextPrepare(int sectionLocal) {
        InsnList list = new InsnList();

        list.add(load(0));
        list.add(field(Opcodes.GETFIELD, WR, "renderContext", CONTEXT_DESC));

        list.add(load(0));
        list.add(field(Opcodes.GETFIELD, WR, "field_4088", "Lnet/minecraft/class_310;"));
        list.add(field(
                Opcodes.GETFIELD,
                "net/minecraft/class_310",
                "field_1773",
                "Lnet/minecraft/class_757;"
        ));

        list.add(load(0));

        list.add(load(0));
        list.add(field(
                Opcodes.GETFIELD,
                WR,
                "field_61737",
                "Lnet/minecraft/class_11658;"
        ));

        list.add(load(sectionLocal));

        list.add(load(0));
        list.add(field(
                Opcodes.GETFIELD,
                WR,
                "field_61738",
                "Lnet/minecraft/class_11661;"
        ));

        list.add(load(0));
        list.add(field(
                Opcodes.GETFIELD,
                WR,
                "field_20951",
                "Lnet/minecraft/class_4599;"
        ));

        list.add(call(
                Opcodes.INVOKEVIRTUAL,
                "net/minecraft/class_4599",
                "method_23000",
                "()Lnet/minecraft/class_4597$class_4598;",
                false
        ));

        list.add(call(
                Opcodes.INVOKEVIRTUAL,
                CONTEXT,
                "prepare",
                PREPARE_DESC,
                false
        ));

        return list;
    }

    private static InsnList setMatrixStack(int matrixLocal) {
        InsnList list = new InsnList();

        list.add(load(0));
        list.add(field(Opcodes.GETFIELD, WR, "renderContext", CONTEXT_DESC));
        list.add(load(matrixLocal));
        list.add(call(
                Opcodes.INVOKEVIRTUAL,
                CONTEXT,
                "setMatrixStack",
                "(Lnet/minecraft/class_4587;)V",
                false
        ));

        return list;
    }

    private static InsnList event(
            String eventField,
            String listenerName,
            String method,
            String desc
    ) {
        String listener = EVENTS + "$" + listenerName;
        InsnList list = new InsnList();

        list.add(field(Opcodes.GETSTATIC, EVENTS, eventField, EVENT_DESC));
        list.add(call(
                Opcodes.INVOKEVIRTUAL,
                EVENT,
                "invoker",
                "()Ljava/lang/Object;",
                false
        ));
        list.add(new TypeInsnNode(Opcodes.CHECKCAST, listener));
        list.add(load(0));
        list.add(field(Opcodes.GETFIELD, WR, "renderContext", CONTEXT_DESC));
        list.add(call(Opcodes.INVOKEINTERFACE, listener, method, desc, true));

        return list;
    }

    private static VarInsnNode load(int local) {
        return new VarInsnNode(Opcodes.ALOAD, local);
    }

    private static FieldInsnNode field(
            int opcode, String owner, String name, String desc
    ) {
        return new FieldInsnNode(opcode, owner, name, desc);
    }

    private static MethodInsnNode call(
            int opcode,
            String owner,
            String name,
            String desc,
            boolean itf
    ) {
        return new MethodInsnNode(opcode, owner, name, desc, itf);
    }

    private static AbstractInsnNode nextReal(AbstractInsnNode insn) {
        while (insn != null && insn.getOpcode() < 0) {
            insn = insn.getNext();
        }

        return insn;
    }

    private static boolean isInvoke(
            AbstractInsnNode insn,
            String owner,
            String name,
            String desc
    ) {
        if (!(insn instanceof MethodInsnNode)) return false;

        MethodInsnNode call = (MethodInsnNode) insn;

        return owner.equals(call.owner)
                && name.equals(call.name)
                && desc.equals(call.desc);
    }

    private static void log(String message) {
        System.err.println("[OptiFabric] WorldRendererFix " + message);
        System.err.flush();
    }
}
