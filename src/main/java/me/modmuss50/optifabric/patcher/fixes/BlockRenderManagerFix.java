package me.modmuss50.optifabric.patcher.fixes;

import java.util.HashSet;
import java.util.Set;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

public class BlockRenderManagerFix implements ClassFixer {
	private static final String RENDER_DAMAGE_DESC = "(Lnet/minecraft/class_2680;Lnet/minecraft/class_2338;Lnet/minecraft/class_1920;Lnet/minecraft/class_4587;Lnet/minecraft/class_4588;)V";
	private static final String RENDER_BLOCK_DESC = "(Lnet/minecraft/class_2680;Lnet/minecraft/class_4587;Lnet/minecraft/class_4597;II)V";
	private static final String GET_MODEL_DESC = "(Lnet/minecraft/class_2680;)Lnet/minecraft/class_1087;";
	private static final String RENDER_MODEL_DESC = "(Lnet/minecraft/class_4587$class_4665;Lnet/minecraft/class_4588;Lnet/minecraft/class_1087;FFFII)V";

	@Override
	public void fix(ClassNode optifine, ClassNode minecraft) {
		Set<String> presentMethods = new HashSet<>();
		for (MethodNode method : optifine.methods) {
			presentMethods.add(method.name + method.desc);
		}

		int added = 0;
		for (MethodNode method : minecraft.methods) {
			if ("<init>".equals(method.name) || "<clinit>".equals(method.name)) continue;

			String key = method.name + method.desc;
			if (!presentMethods.contains(key)) {
				optifine.methods.add(copy(method));
				added++;
			}
		}

		if (added > 0) {
			System.err.println("[OptiFabric] BlockRenderManagerFix added " + added + " missing methods");
		}

		addIndigoAnchors(optifine);
	}

	private static MethodNode copy(MethodNode method) {
		MethodNode copy = new MethodNode(method.access, method.name, method.desc, method.signature,
				method.exceptions == null ? null : method.exceptions.toArray(new String[0]));
		method.accept(copy);
		return copy;
	}

	private static void addIndigoAnchors(ClassNode optifine) {
		MethodNode renderDamage = findMethod(optifine, "method_23071", RENDER_DAMAGE_DESC);
		if (renderDamage != null) {
			boolean present = hasInvokeBeforeFirstReturn(renderDamage, "net/minecraft/class_773", "method_3335", GET_MODEL_DESC);
			if (!present) {
				appendGetModelAnchor(optifine, renderDamage);
			}
			log("class_776 method_23071 getModel anchor present=" + present + " inserted=" + !present);
		} else {
			log("class_776 method_23071 missing, cannot add getModel anchor");
		}

		MethodNode renderBlock = findMethod(optifine, "method_3353", RENDER_BLOCK_DESC);
		if (renderBlock != null) {
			boolean present = hasInvokeBeforeFirstReturn(renderBlock, "net/minecraft/class_778", "method_3367", RENDER_MODEL_DESC);
			if (!present) {
				appendRenderModelAnchor(renderBlock);
			}
			log("class_776 method_3353 renderModel anchor present=" + present + " inserted=" + !present);
		} else {
			log("class_776 method_3353 missing, cannot add renderModel anchor");
		}
	}

	private static MethodNode findMethod(ClassNode owner, String name, String desc) {
		for (MethodNode method : owner.methods) {
			if (name.equals(method.name) && desc.equals(method.desc)) {
				return method;
			}
		}

		return null;
	}

	private static boolean hasInvokeBeforeFirstReturn(MethodNode method, String owner, String name, String desc) {
		for (AbstractInsnNode insn : method.instructions) {
			if (insn.getOpcode() == Opcodes.RETURN) {
				return false;
			}

			if (insn instanceof MethodInsnNode) {
				MethodInsnNode methodInsn = (MethodInsnNode) insn;
				if (owner.equals(methodInsn.owner) && name.equals(methodInsn.name) && desc.equals(methodInsn.desc)) {
					return true;
				}
			}
		}

		return false;
	}

	private static void appendGetModelAnchor(ClassNode owner, MethodNode method) {
		int modelLocal = method.maxLocals;
		AbstractInsnNode returnInsn = findReturn(method);
		if (returnInsn == null) {
			log("class_776 method_23071 has no return, cannot add getModel anchor");
			return;
		}

		LabelNode modelStart = new LabelNode();
		LabelNode skip = new LabelNode();

		InsnList anchor = new InsnList();
		anchor.add(new InsnNode(Opcodes.ICONST_0));
		anchor.add(new JumpInsnNode(Opcodes.IFEQ, skip));
		anchor.add(new VarInsnNode(Opcodes.ALOAD, 0));
		anchor.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, owner.name, "method_3351", "()Lnet/minecraft/class_773;", false));
		anchor.add(new VarInsnNode(Opcodes.ALOAD, 1));
		anchor.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "net/minecraft/class_773", "method_3335", GET_MODEL_DESC, false));
		anchor.add(new VarInsnNode(Opcodes.ASTORE, modelLocal));
		anchor.add(modelStart);
		anchor.add(new InsnNode(Opcodes.RETURN));
		anchor.add(skip);

		method.instructions.insertBefore(returnInsn, anchor);
		method.localVariables.add(new LocalVariableNode("model", "Lnet/minecraft/class_1087;", null, modelStart, skip, modelLocal));
		method.maxLocals = Math.max(method.maxLocals, modelLocal + 1);
		method.maxStack = Math.max(method.maxStack, 2);
	}

	private static void appendRenderModelAnchor(MethodNode method) {
		AbstractInsnNode returnInsn = findReturn(method);
		if (returnInsn == null) {
			log("class_776 method_3353 has no return, cannot add renderModel anchor");
			return;
		}

		LabelNode skip = new LabelNode();

		InsnList anchor = new InsnList();
		anchor.add(new InsnNode(Opcodes.ICONST_0));
		anchor.add(new JumpInsnNode(Opcodes.IFEQ, skip));
		anchor.add(new InsnNode(Opcodes.ACONST_NULL));
		anchor.add(new InsnNode(Opcodes.ACONST_NULL));
		anchor.add(new InsnNode(Opcodes.ACONST_NULL));
		anchor.add(new InsnNode(Opcodes.FCONST_0));
		anchor.add(new InsnNode(Opcodes.FCONST_0));
		anchor.add(new InsnNode(Opcodes.FCONST_0));
		anchor.add(new InsnNode(Opcodes.ICONST_0));
		anchor.add(new InsnNode(Opcodes.ICONST_0));
		anchor.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "net/minecraft/class_778", "method_3367", RENDER_MODEL_DESC, false));
		anchor.add(new InsnNode(Opcodes.RETURN));
		anchor.add(skip);

		method.instructions.insertBefore(returnInsn, anchor);
		method.maxStack = Math.max(method.maxStack, 8);
	}

	private static AbstractInsnNode findReturn(MethodNode method) {
		for (AbstractInsnNode insn : method.instructions.toArray()) {
			if (insn.getOpcode() == Opcodes.RETURN) {
				return insn;
			}
		}

		return null;
	}

	private static void log(String message) {
		System.err.println("[OptiFabric] BlockRenderManagerFix " + message);
		System.err.flush();
	}
}
