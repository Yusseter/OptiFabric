package me.modmuss50.optifabric.patcher.fixes;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.VarInsnNode;

public class BasicItemModelFix implements ClassFixer {
	private static final String VANILLA_CONSTRUCTOR = "(Ljava/util/List;Ljava/util/List;Lnet/minecraft/class_10809;Ljava/util/function/Function;)V";
	private static final String OPTIFINE_CONSTRUCTOR = "(Ljava/util/List;Ljava/util/List;Lnet/minecraft/class_10809;Ljava/util/function/Function;Lnet/minecraft/class_2960;Z)V";
	private static final String STATE_DESC = "Lnet/minecraft/class_10444$class_10446;";

	@Override
	public void fix(ClassNode optifine, ClassNode minecraft) {
		boolean hasVanillaConstructor = hasMethod(optifine, "<init>", VANILLA_CONSTRUCTOR);
		boolean hasOptifineConstructor = hasMethod(optifine, "<init>", OPTIFINE_CONSTRUCTOR);

		if (!hasVanillaConstructor || hasOptifineConstructor) {
			log("vanillaConstructor=" + hasVanillaConstructor + " optifineConstructor=" + hasOptifineConstructor + " added=false");
		} else {
			MethodNode constructor = new MethodNode(Opcodes.ACC_PUBLIC, "<init>", OPTIFINE_CONSTRUCTOR, null, null);
			constructor.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
			constructor.instructions.add(new VarInsnNode(Opcodes.ALOAD, 1));
			constructor.instructions.add(new VarInsnNode(Opcodes.ALOAD, 2));
			constructor.instructions.add(new VarInsnNode(Opcodes.ALOAD, 3));
			constructor.instructions.add(new VarInsnNode(Opcodes.ALOAD, 4));
			constructor.instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, optifine.name, "<init>", VANILLA_CONSTRUCTOR, false));
			constructor.instructions.add(new InsnNode(Opcodes.RETURN));
			constructor.maxStack = 5;
			constructor.maxLocals = 7;
			optifine.methods.add(constructor);

			log("vanillaConstructor=true optifineConstructor=false added=true");
		}

		ensureRenderStateLocal(optifine);
	}

	private static boolean hasMethod(ClassNode owner, String name, String desc) {
		for (MethodNode method : owner.methods) {
			if (name.equals(method.name) && desc.equals(method.desc)) {
				return true;
			}
		}

		return false;
	}

	private static void log(String message) {
		System.err.println("[OptiFabric] BasicItemModelFix " + message);
		System.err.flush();
	}

	private static void ensureRenderStateLocal(ClassNode owner) {
		MethodNode method = findMethod(owner, "method_65584", "(Lnet/minecraft/class_10444;Lnet/minecraft/class_1799;Lnet/minecraft/class_10442;Lnet/minecraft/class_811;Lnet/minecraft/class_638;Lnet/minecraft/class_11566;I)V");
		if (method == null) {
			log("method_65584 missing, cannot restore render state local");
			return;
		}

		int localIndex = method.maxLocals;
		LabelNode start = new LabelNode();
		LabelNode end = new LabelNode();

		InsnList prefix = new InsnList();
		prefix.add(start);
		prefix.add(new VarInsnNode(Opcodes.ALOAD, 1));
		prefix.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "net/minecraft/class_10444", "method_65601", "()Lnet/minecraft/class_10444$class_10446;", false));
		prefix.add(new VarInsnNode(Opcodes.ASTORE, localIndex));
		method.instructions.insert(prefix);

		InsnList suffix = new InsnList();
		suffix.add(end);
		method.instructions.insertBefore(findReturn(method), suffix);
		if (method.localVariables != null) {
			method.localVariables.add(new LocalVariableNode("renderState", STATE_DESC, null, start, end, localIndex));
		}
		method.maxLocals = localIndex + 1;
		method.maxStack = Math.max(method.maxStack, 1);

		log("restored renderState local at slot " + localIndex);
	}

	private static MethodNode findMethod(ClassNode owner, String name, String desc) {
		for (MethodNode method : owner.methods) {
			if (name.equals(method.name) && desc.equals(method.desc)) {
				return method;
			}
		}

		return null;
	}

	private static InsnNode findReturn(MethodNode method) {
		for (AbstractInsnNode insn : method.instructions.toArray()) {
			if (insn.getOpcode() == Opcodes.RETURN) {
				return (InsnNode) insn;
			}
		}

		throw new IllegalStateException("No return in " + method.name + method.desc);
	}
}
