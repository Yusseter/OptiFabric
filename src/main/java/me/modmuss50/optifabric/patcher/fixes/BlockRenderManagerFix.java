package me.modmuss50.optifabric.patcher.fixes;

import java.util.HashSet;
import java.util.Set;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

public class BlockRenderManagerFix implements ClassFixer {
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
	}

	private static MethodNode copy(MethodNode method) {
		MethodNode copy = new MethodNode(method.access, method.name, method.desc, method.signature,
				method.exceptions == null ? null : method.exceptions.toArray(new String[0]));
		method.accept(copy);
		return copy;
	}
}
