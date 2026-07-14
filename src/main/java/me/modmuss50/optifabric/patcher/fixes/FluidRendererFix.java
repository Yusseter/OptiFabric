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
import org.objectweb.asm.tree.VarInsnNode;

import me.modmuss50.optifabric.util.RemappingUtils;

public class FluidRendererFix implements ClassFixer {
	//Add a little decoy so Fabric injects a little earlier, then patch the result
	private static final String OLD_RENDER_DESC = "(Lnet/minecraft/class_1920;Lnet/minecraft/class_2338;Lnet/minecraft/class_4588;Lnet/minecraft/class_3610;)Z";
	private static final String NEW_RENDER_DESC = "(Lnet/minecraft/class_1920;Lnet/minecraft/class_2338;Lnet/minecraft/class_4588;Lnet/minecraft/class_2680;Lnet/minecraft/class_3610;)V";

	@Override
	public void fix(ClassNode optifine, ClassNode minecraft) {
		String oldRender = RemappingUtils.getMethodName("class_775", "method_3347", OLD_RENDER_DESC);
		String oldRenderDesc = RemappingUtils.mapMethodDescriptor(OLD_RENDER_DESC);
		String newRender = RemappingUtils.getMethodName("class_775", "method_3347", NEW_RENDER_DESC);
		String newRenderDesc = RemappingUtils.mapMethodDescriptor(NEW_RENDER_DESC);

		for (MethodNode method : optifine.methods) {
			boolean isOldRender = oldRender.equals(method.name) && oldRenderDesc.equals(method.desc);
			boolean isNewRender = newRender.equals(method.name) && newRenderDesc.equals(method.desc);
			if (isOldRender || isNewRender) {
				JumpInsnNode setTint = null;

				for (AbstractInsnNode node : method.instructions) {
					if (node.getType() == AbstractInsnNode.METHOD_INSN) {
						MethodInsnNode methodInsn = (MethodInsnNode) node;

						if ("net/optifine/CustomColors".equals(methodInsn.owner) && "getFluidColor".equals(methodInsn.name)) {
							do {
								node = node.getPrevious();
							} while (node != null && node.getType() != AbstractInsnNode.JUMP_INSN);
							if (node == null) {
								throw new IllegalStateException("Unable to find injection point in " + optifine.name + '#' + method.name + method.desc);
							}

							setTint = (JumpInsnNode) node;
							break;
						}
					}
				}

				if (setTint != null) {
					LabelNode needsOptiFine = new LabelNode();
					LabelNode noNeed = setTint.label;

					InsnList extra = new InsnList();
					method.instructions.insertBefore(setTint, extra);

					setTint.label = needsOptiFine;
					setTint.setOpcode(Opcodes.IFLT);

					extra.add(new VarInsnNode(Opcodes.ALOAD, isNewRender ? 5 : 4));
					extra.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "me/modmuss50/optifabric/patcher/fixes/FluidRendererFixExternal",
							"needsOptiFine", RemappingUtils.mapMethodDescriptor("(Ljava/lang/Object;)Z")));
					extra.add(new JumpInsnNode(Opcodes.IFEQ, noNeed));
					extra.add(needsOptiFine);
					method.instructions.insert(setTint, extra);
					log("class_775 render patched using CustomColors#getFluidColor anchor");
				} else {
					log("class_775 render patched using dead BiomeColors#getWaterColor anchor");
				}

				if (!hasBiomeColorAnchor(method)) {
					appendBiomeColorAnchor(method);
					log("class_775 render patched with Fabric water color injection anchor");
				}
				break;
			}
		}
	}

	private static boolean hasBiomeColorAnchor(MethodNode method) {
		String biomeColors = RemappingUtils.getClassName("class_1163");
		String getWaterColor = RemappingUtils.getMethodName("class_1163", "method_4961",
				"(Lnet/minecraft/class_1920;Lnet/minecraft/class_2338;)I");
		String getWaterColorDesc = RemappingUtils.mapMethodDescriptor("(Lnet/minecraft/class_1920;Lnet/minecraft/class_2338;)I");

		for (AbstractInsnNode insn : method.instructions) {
			if (insn instanceof MethodInsnNode) {
				MethodInsnNode methodInsn = (MethodInsnNode) insn;
				if (biomeColors.equals(methodInsn.owner) && getWaterColor.equals(methodInsn.name) && getWaterColorDesc.equals(methodInsn.desc)) {
					return true;
				}
			}
		}

		return false;
	}

	private static void appendBiomeColorAnchor(MethodNode method) {
		AbstractInsnNode returnInsn = findReturn(method);
		if (returnInsn == null) {
			throw new IllegalStateException("Unable to find return in " + method.name + method.desc);
		}

		LabelNode skip = new LabelNode();
		InsnList anchor = new InsnList();
		anchor.add(new InsnNode(Opcodes.ICONST_0));
		anchor.add(new JumpInsnNode(Opcodes.IFEQ, skip));
		anchor.add(new VarInsnNode(Opcodes.ALOAD, 1));
		anchor.add(new VarInsnNode(Opcodes.ALOAD, 2));
		anchor.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "net/minecraft/class_1163", "method_4961",
				RemappingUtils.mapMethodDescriptor("(Lnet/minecraft/class_1920;Lnet/minecraft/class_2338;)I"), false));
		anchor.add(new InsnNode(Opcodes.POP));
		anchor.add(skip);

		method.instructions.insertBefore(returnInsn, anchor);
		method.maxStack = Math.max(method.maxStack, 2);
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
		System.err.println("[OptiFabric] FluidRendererFix " + message);
		System.err.flush();
	}
}
