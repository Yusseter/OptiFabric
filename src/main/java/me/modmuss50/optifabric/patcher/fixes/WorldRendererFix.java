package me.modmuss50.optifabric.patcher.fixes;

import java.util.HashSet;
import java.util.Set;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import net.fabricmc.loader.api.FabricLoader;

public class WorldRendererFix implements ClassFixer {
	@Override
	public void fix(ClassNode optifine, ClassNode minecraft) {
		Set<String> presentMethods = new HashSet<>();
		for (MethodNode method : optifine.methods) {
			presentMethods.add(method.name + method.desc);
		}

		for (MethodNode method : minecraft.methods) {
			if ("<init>".equals(method.name) || "<clinit>".equals(method.name)) continue;

			String key = method.name + method.desc;
			if (!presentMethods.contains(key)) {
				optifine.methods.add(copy(method));
			}
		}

		prepareFabricRenderContextBeforeBlockOutline(optifine);
	}

	private static MethodNode copy(MethodNode method) {
		MethodNode copy = new MethodNode(method.access, method.name, method.desc, method.signature,
				method.exceptions == null ? null : method.exceptions.toArray(new String[0]));
		method.accept(copy);
		return copy;
	}

	private static void prepareFabricRenderContextBeforeBlockOutline(ClassNode optifine) {
		if (!FabricLoader.getInstance().isModLoaded("fabric-rendering-v1")) return;

		for (MethodNode method : optifine.methods) {
			if (!"method_62214".equals(method.name)) continue;

			int sectionStateLocal = getSectionStateLocal(method.desc);
			if (sectionStateLocal < 0) continue;

			int inserted = 0;
			for (AbstractInsnNode insn : method.instructions.toArray()) {
				if (!isInvoke(insn, "net/minecraft/class_761", "method_62210",
						"(Lnet/minecraft/class_4597$class_4598;Lnet/minecraft/class_4587;ZLnet/minecraft/class_11658;)V")) {
					continue;
				}

				if (hasContextPrepareBefore(insn)) continue;

				method.instructions.insertBefore(insn, makeContextPrepare(sectionStateLocal));
				inserted++;
			}

			if (inserted > 0) {
				method.maxStack = Math.max(method.maxStack, 8);
				log("method_62214" + method.desc + " inserted Fabric render context prepares=" + inserted);
			}
		}
	}

	private static int getSectionStateLocal(String desc) {
		if ("(Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lnet/minecraft/class_11658;Lnet/minecraft/class_3695;Lorg/joml/Matrix4f;Lnet/minecraft/class_9925;Lnet/minecraft/class_9925;ZLnet/minecraft/class_9925;Lnet/minecraft/class_9925;Lnet/minecraft/class_9779;)V".equals(desc)) {
			return 20;
		}

		if ("(Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lnet/minecraft/class_11658;Lnet/minecraft/class_3695;Lorg/joml/Matrix4f;Lnet/minecraft/class_9925;Lnet/minecraft/class_9925;ZLnet/minecraft/class_9925;Lnet/minecraft/class_9925;)V".equals(desc)) {
			return 17;
		}

		return -1;
	}

	private static InsnList makeContextPrepare(int sectionStateLocal) {
		InsnList insns = new InsnList();
		insns.add(new VarInsnNode(Opcodes.ALOAD, 0));
		insns.add(new FieldInsnNode(Opcodes.GETFIELD, "net/minecraft/class_761", "renderContext",
				"Lnet/fabricmc/fabric/impl/client/rendering/world/WorldRenderContextImpl;"));
		insns.add(new VarInsnNode(Opcodes.ALOAD, 0));
		insns.add(new FieldInsnNode(Opcodes.GETFIELD, "net/minecraft/class_761", "field_4088",
				"Lnet/minecraft/class_310;"));
		insns.add(new FieldInsnNode(Opcodes.GETFIELD, "net/minecraft/class_310", "field_1773",
				"Lnet/minecraft/class_757;"));
		insns.add(new VarInsnNode(Opcodes.ALOAD, 0));
		insns.add(new VarInsnNode(Opcodes.ALOAD, 2));
		insns.add(new VarInsnNode(Opcodes.ALOAD, sectionStateLocal));
		insns.add(new VarInsnNode(Opcodes.ALOAD, 0));
		insns.add(new FieldInsnNode(Opcodes.GETFIELD, "net/minecraft/class_761", "field_61738",
				"Lnet/minecraft/class_11661;"));
		insns.add(new VarInsnNode(Opcodes.ALOAD, 0));
		insns.add(new FieldInsnNode(Opcodes.GETFIELD, "net/minecraft/class_761", "field_20951",
				"Lnet/minecraft/class_4599;"));
		insns.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "net/minecraft/class_4599", "method_23000",
				"()Lnet/minecraft/class_4597$class_4598;", false));
		insns.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "net/fabricmc/fabric/impl/client/rendering/world/WorldRenderContextImpl", "prepare",
				"(Lnet/minecraft/class_757;Lnet/minecraft/class_761;Lnet/minecraft/class_11658;Lnet/minecraft/class_11532;Lnet/minecraft/class_11659;Lnet/minecraft/class_4597;)V", false));
		return insns;
	}

	private static boolean hasContextPrepareBefore(AbstractInsnNode insn) {
		AbstractInsnNode previous = insn.getPrevious();
		for (int i = 0; i < 20 && previous != null; i++) {
			if (isInvoke(previous, "net/fabricmc/fabric/impl/client/rendering/world/WorldRenderContextImpl", "prepare",
					"(Lnet/minecraft/class_757;Lnet/minecraft/class_761;Lnet/minecraft/class_11658;Lnet/minecraft/class_11532;Lnet/minecraft/class_11659;Lnet/minecraft/class_4597;)V")) {
				return true;
			}

			previous = previous.getPrevious();
		}

		return false;
	}

	private static boolean isInvoke(AbstractInsnNode insn, String owner, String name, String desc) {
		if (!(insn instanceof MethodInsnNode)) return false;

		MethodInsnNode methodInsn = (MethodInsnNode) insn;
		return owner.equals(methodInsn.owner) && name.equals(methodInsn.name) && desc.equals(methodInsn.desc);
	}

	private static void log(String message) {
		System.err.println("[OptiFabric] WorldRendererFix" + message);
		System.err.flush();
	}
}
