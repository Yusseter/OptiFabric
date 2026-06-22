package me.modmuss50.optifabric.patcher.fixes;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

public class InGameOverlayRendererFix implements ClassFixer {
	private static final String GET_IN_WALL_BLOCK_STATE_DESC = "(Lnet/minecraft/class_1657;)Lnet/minecraft/class_2680;";

	@Override
	public void fix(ClassNode optifine, ClassNode minecraft) {
		MethodNode vanillaMethod = findMethod(minecraft, "method_24225", GET_IN_WALL_BLOCK_STATE_DESC);
		if (vanillaMethod == null) {
			log("vanilla method_24225 missing, cannot restore mutable local");
			return;
		}

		MethodNode oldMethod = findMethod(optifine, "method_24225", GET_IN_WALL_BLOCK_STATE_DESC);
		if (oldMethod == null) {
			optifine.methods.add(copy(vanillaMethod));
			log("method_24225 missing, copied vanilla method");
			return;
		}

		int index = optifine.methods.indexOf(oldMethod);
		MethodNode replacement = copy(vanillaMethod);
		replacement.access = oldMethod.access;
		optifine.methods.set(index, replacement);
		log("method_24225 replaced with vanilla method to restore BlockPos.Mutable local");
	}

	private static MethodNode findMethod(ClassNode owner, String name, String desc) {
		for (MethodNode method : owner.methods) {
			if (name.equals(method.name) && desc.equals(method.desc)) {
				return method;
			}
		}

		return null;
	}

	private static MethodNode copy(MethodNode method) {
		MethodNode copy = new MethodNode(method.access, method.name, method.desc, method.signature,
				method.exceptions == null ? null : method.exceptions.toArray(new String[0]));
		method.accept(copy);
		return copy;
	}

	private static void log(String message) {
		System.err.println("[OptiFabric] InGameOverlayRendererFix " + message);
		System.err.flush();
	}
}
