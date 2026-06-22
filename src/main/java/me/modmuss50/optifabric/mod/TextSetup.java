package me.modmuss50.optifabric.mod;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

import me.modmuss50.optifabric.util.RemappingUtils;

import com.chocohead.mm.api.ClassTinkerers;

public class TextSetup implements Runnable {
	@Override
	public void run() {
		if (OptifabricSetup.isPresent("minecraft", ">=1.19")) {
			ClassTinkerers.addTransformation("me/modmuss50/optifabric/mod/Text", node -> {
				String mutableText = RemappingUtils.getClassName("class_5250");

				for (MethodNode method : node.methods) {
					if ("literal".equals(method.name) && method.desc.startsWith("(Ljava/lang/String;)")) {
						InsnList instructions = new InsnList();

						instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
						String literalOwner = RemappingUtils.getClassName("class_2561");
						String literalName = RemappingUtils.getMethodName("class_2561", "method_43470", "(Ljava/lang/String;)Lnet/minecraft/class_5250;");
						String literalDesc = RemappingUtils.mapMethodDescriptor("(Ljava/lang/String;)Lnet/minecraft/class_5250;");
						instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, literalOwner, literalName, literalDesc, true));
						instructions.add(new InsnNode(Opcodes.ARETURN));

						method.instructions = instructions;
					} else {
						for (AbstractInsnNode insn : method.instructions) {
							if (insn.getType() == AbstractInsnNode.METHOD_INSN) {
								MethodInsnNode minsn = ((MethodInsnNode) insn);

								if (mutableText.equals(minsn.owner)) {
									if (minsn.getOpcode() != Opcodes.INVOKESTATIC) minsn.setOpcode(Opcodes.INVOKEVIRTUAL);
									minsn.itf = false;
								}
							}
						}
					}
				}
			});
		}
	}
}
