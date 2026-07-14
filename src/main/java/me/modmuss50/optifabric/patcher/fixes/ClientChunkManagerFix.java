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
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

public class ClientChunkManagerFix implements ClassFixer {
	private static final String LOAD_CHUNK_FROM_PACKET_DESC = "(IILnet/minecraft/class_2540;Ljava/util/Map;Ljava/util/function/Consumer;)Lnet/minecraft/class_2818;";

	@Override
	public void fix(ClassNode optifine, ClassNode minecraft) {
		MethodNode method = findMethod(optifine, "method_16020", LOAD_CHUNK_FROM_PACKET_DESC);
		if (method == null) {
			log("class_631 method_16020 missing, cannot add WorldChunk allocation anchor");
			return;
		}

		boolean worldChunkPresent = hasNew(method, "net/minecraft/class_2818");
		boolean chunkOfPresent = hasNew(method, "net/optifine/ChunkOF");
		boolean inserted = !worldChunkPresent && chunkOfPresent;

		if (inserted) {
			addWorldChunkAllocationAnchor(method);
		}

		log("class_631 method_16020 WorldChunk NEW present=" + worldChunkPresent + " ChunkOF NEW present=" + chunkOfPresent + " inserted=" + inserted);
	}

	private static MethodNode findMethod(ClassNode owner, String name, String desc) {
		for (MethodNode method : owner.methods) {
			if (name.equals(method.name) && desc.equals(method.desc)) {
				return method;
			}
		}

		return null;
	}

	private static boolean hasNew(MethodNode method, String type) {
		for (AbstractInsnNode insn : method.instructions) {
			if (insn.getOpcode() == Opcodes.NEW && insn instanceof TypeInsnNode) {
				TypeInsnNode typeInsn = (TypeInsnNode) insn;
				if (type.equals(typeInsn.desc)) {
					return true;
				}
			}
		}

		return false;
	}

	private static void addWorldChunkAllocationAnchor(MethodNode method) {
		for (AbstractInsnNode insn : method.instructions.toArray()) {
			if (insn.getOpcode() != Opcodes.NEW || !(insn instanceof TypeInsnNode)) {
				continue;
			}

			TypeInsnNode typeInsn = (TypeInsnNode) insn;
			if (!"net/optifine/ChunkOF".equals(typeInsn.desc)) {
				continue;
			}

			LabelNode skip = new LabelNode();
			InsnList anchor = new InsnList();
			anchor.add(new InsnNode(Opcodes.ICONST_0));
			anchor.add(new JumpInsnNode(Opcodes.IFEQ, skip));
			anchor.add(new TypeInsnNode(Opcodes.NEW, "net/minecraft/class_2818"));
			anchor.add(new InsnNode(Opcodes.DUP));
			anchor.add(new VarInsnNode(Opcodes.ALOAD, 0));
			anchor.add(new FieldInsnNode(Opcodes.GETFIELD, "net/minecraft/class_631", "field_16525", "Lnet/minecraft/class_638;"));
			anchor.add(new VarInsnNode(Opcodes.ALOAD, 8));
			anchor.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, "net/minecraft/class_2818", "<init>", "(Lnet/minecraft/class_1937;Lnet/minecraft/class_1923;)V", false));
			anchor.add(new InsnNode(Opcodes.POP));
			anchor.add(skip);

			method.instructions.insertBefore(insn, anchor);
			method.maxStack = Math.max(method.maxStack, 4);
			return;
		}

		log("class_631 method_16020 has no ChunkOF allocation, cannot add WorldChunk allocation anchor");
	}

	private static void log(String message) {
		System.err.println("[OptiFabric] ClientChunkManagerFix" + message);
		System.err.flush();
	}
}
