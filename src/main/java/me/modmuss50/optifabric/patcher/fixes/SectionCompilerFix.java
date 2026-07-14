package me.modmuss50.optifabric.patcher.fixes;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

public class SectionCompilerFix implements ClassFixer {
	private static final String BUILD_DESC = "(Lnet/minecraft/class_4076;Lnet/minecraft/class_853;Lnet/minecraft/class_8251;Lnet/minecraft/class_750;)Lnet/minecraft/class_9810$class_9811;";
	private static final String ITERATE_DESC = "(Lnet/minecraft/class_2338;Lnet/minecraft/class_2338;)Ljava/lang/Iterable;";
	private static final String RENDER_TYPE_DESC = "()Lnet/minecraft/class_2464;";

	@Override
	public void fix(ClassNode optifine, ClassNode minecraft) {
		MethodNode method = findMethod(optifine, "method_60904", BUILD_DESC);
		if (method == null) {
			log("class_9810 method_60904 missing, cannot add Indigo anchors");
			return;
		}

		boolean iteratePresent = hasInvoke(method, "net/minecraft/class_2338", "method_10097", ITERATE_DESC);
		boolean renderTypePresent = hasInvoke(method, "net/minecraft/class_2680", "method_26217", RENDER_TYPE_DESC);

		if (!iteratePresent || !renderTypePresent) {
			insertIndigoAnchors(method, !iteratePresent, !renderTypePresent);
		}

		log("class_9810 method_60904 iterate present=" + iteratePresent + " inserted=" + !iteratePresent
				+ " renderType present=" + renderTypePresent + " inserted=" + !renderTypePresent);
	}

	private static MethodNode findMethod(ClassNode owner, String name, String desc) {
		for (MethodNode method : owner.methods) {
			if (name.equals(method.name) && desc.equals(method.desc)) {
				return method;
			}
		}

		return null;
	}

	private static boolean hasInvoke(MethodNode method, String owner, String name, String desc) {
		for (AbstractInsnNode insn : method.instructions) {
			if (insn instanceof MethodInsnNode) {
				MethodInsnNode methodInsn = (MethodInsnNode) insn;
				if (owner.equals(methodInsn.owner) && name.equals(methodInsn.name) && desc.equals(methodInsn.desc)) {
					return true;
				}
			}
		}

		return false;
	}

	private static void insertIndigoAnchors(MethodNode method, boolean addIteration, boolean addRenderType) {
		AbstractInsnNode insertionPoint = findFirstAstore(method);
		if (insertionPoint == null) {
			log("class_9810 method_60904 has no chunk cache local store, cannot add Indigo anchors");
			return;
		}

		int sectionOrigin = method.maxLocals;
		int sectionEnd = sectionOrigin + 1;
		int blockPos = sectionOrigin + 2;
		int matrixStack = sectionOrigin + 3;
		int builderMap = sectionOrigin + 4;
		int random = sectionOrigin + 5;
		int blockState = sectionOrigin + 6;

		LabelNode start = new LabelNode();
		LabelNode skip = new LabelNode();
		LabelNode end = new LabelNode();

		InsnList anchor = new InsnList();
		anchor.add(start);
		anchor.add(new VarInsnNode(Opcodes.ALOAD, 1));
		anchor.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "net/minecraft/class_4076", "method_19767", "()Lnet/minecraft/class_2338;", false));
		anchor.add(new VarInsnNode(Opcodes.ASTORE, sectionOrigin));
		anchor.add(new VarInsnNode(Opcodes.ALOAD, sectionOrigin));
		anchor.add(new IntInsnNode(Opcodes.BIPUSH, 15));
		anchor.add(new IntInsnNode(Opcodes.BIPUSH, 15));
		anchor.add(new IntInsnNode(Opcodes.BIPUSH, 15));
		anchor.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "net/minecraft/class_2338", "method_10069", "(III)Lnet/minecraft/class_2338;", false));
		anchor.add(new VarInsnNode(Opcodes.ASTORE, sectionEnd));
		anchor.add(new VarInsnNode(Opcodes.ALOAD, sectionOrigin));
		anchor.add(new VarInsnNode(Opcodes.ASTORE, blockPos));
		anchor.add(new TypeInsnNode(Opcodes.NEW, "net/minecraft/class_4587"));
		anchor.add(new InsnNode(Opcodes.DUP));
		anchor.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "net/minecraft/class_4587", "<init>", "()V", false));
		anchor.add(new VarInsnNode(Opcodes.ASTORE, matrixStack));
		anchor.add(new TypeInsnNode(Opcodes.NEW, "java/util/EnumMap"));
		anchor.add(new InsnNode(Opcodes.DUP));
		anchor.add(new LdcInsnNode(Type.getObjectType("net/minecraft/class_11515")));
		anchor.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "java/util/EnumMap", "<init>", "(Ljava/lang/Class;)V", false));
		anchor.add(new VarInsnNode(Opcodes.ASTORE, builderMap));
		anchor.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "net/minecraft/class_5819", "method_43047", "()Lnet/minecraft/class_5819;", true));
		anchor.add(new VarInsnNode(Opcodes.ASTORE, random));
		anchor.add(new InsnNode(Opcodes.ACONST_NULL));
		anchor.add(new VarInsnNode(Opcodes.ASTORE, blockState));
		anchor.add(new InsnNode(Opcodes.ICONST_0));
		anchor.add(new JumpInsnNode(Opcodes.IFEQ, skip));

		if (addIteration) {
			anchor.add(new VarInsnNode(Opcodes.ALOAD, sectionOrigin));
			anchor.add(new VarInsnNode(Opcodes.ALOAD, sectionEnd));
			anchor.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "net/minecraft/class_2338", "method_10097", ITERATE_DESC, false));
			anchor.add(new InsnNode(Opcodes.POP));
		}

		if (addRenderType) {
			anchor.add(new VarInsnNode(Opcodes.ALOAD, blockState));
			anchor.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "net/minecraft/class_2680", "method_26217", RENDER_TYPE_DESC, false));
			anchor.add(new InsnNode(Opcodes.POP));
		}

		anchor.add(skip);
		anchor.add(end);

		method.instructions.insert(insertionPoint, anchor);
		method.maxLocals = Math.max(method.maxLocals, sectionOrigin + 7);
		method.maxStack = Math.max(method.maxStack, 3);

		addLocal(method, "sectionOrigin", "Lnet/minecraft/class_2338;", start, end, sectionOrigin);
		addLocal(method, "sectionEnd", "Lnet/minecraft/class_2338;", start, end, sectionEnd);
		addLocal(method, "blockPos", "Lnet/minecraft/class_2338;", start, end, blockPos);
		addLocal(method, "matrixStack", "Lnet/minecraft/class_4587;", start, end, matrixStack);
		addLocal(method, "builderMap", "Ljava/util/Map;", start, end, builderMap);
		addLocal(method, "random", "Lnet/minecraft/class_5819;", start, end, random);
	}

	private static AbstractInsnNode findFirstAstore(MethodNode method) {
		for (AbstractInsnNode insn : method.instructions.toArray()) {
			if (insn.getOpcode() == Opcodes.ASTORE) {
				return insn;
			}
		}

		return null;
	}

	private static void addLocal(MethodNode method, String name, String desc, LabelNode start, LabelNode end, int index) {
		if (method.localVariables == null) return;

		method.localVariables.add(new LocalVariableNode(name, desc, null, start, end, index));
	}

	private static void log(String message) {
		System.err.println("[OptiFabric] SectionCompilerFix" + message);
		System.err.flush();
	}
}
