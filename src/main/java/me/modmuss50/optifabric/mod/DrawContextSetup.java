package me.modmuss50.optifabric.mod;

import com.chocohead.mm.api.ClassTinkerers;
import me.modmuss50.optifabric.util.RemappingUtils;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

public class DrawContextSetup implements Runnable {
    @Override
    public void run() {
        if (OptifabricSetup.isPresent("minecraft", ">=1.20")) {
            // changes call to DrawContext.drawTextWithShadow(LTextRenderer;String;III)I
            ClassTinkerers.addTransformation("me/modmuss50/optifabric/mod/DrawContext", node -> {
                String drawTextWithShadowOwner = RemappingUtils.getClassName("class_332");
                String drawTextWithShadowName = RemappingUtils.getMethodName("class_332", "method_25303", "(Lnet/minecraft/class_327;Ljava/lang/String;III)I");
                String drawTextWithShadowDesc = RemappingUtils.mapMethodDescriptor("(Lnet/minecraft/class_327;Ljava/lang/String;III)I");
                for (MethodNode method : node.methods) {
                    if ("drawTextWithShadow".equals(method.name)) {
                        for (AbstractInsnNode insn : method.instructions) {
                            // swap stack order of TextRenderer and DrawContext
                            if (insn.getOpcode() == Opcodes.ALOAD && ((VarInsnNode) insn).var == 1) {
                                method.instructions.remove(insn.getPrevious()); // ALOAD 0
                                method.instructions.insert(insn.getNext(), new VarInsnNode(Opcodes.ALOAD, 0));
                            // cast to DrawContext instead of MatrixStack
                            } else if (insn.getOpcode() == Opcodes.CHECKCAST) {
                                ((TypeInsnNode) insn).desc = drawTextWithShadowOwner;
                            // change method
                            } else if (insn.getOpcode() == Opcodes.INVOKEVIRTUAL) {
                                ((MethodInsnNode) insn).owner = drawTextWithShadowOwner;
                                ((MethodInsnNode) insn).desc = drawTextWithShadowDesc;
                                ((MethodInsnNode) insn).name = drawTextWithShadowName;
                            // method takes ints instead of floats
                            } else if (insn.getOpcode() == Opcodes.I2F) {
                                method.instructions.remove(insn);
                            }
                        }
                        break;
                    }
                }
            });
        }
    }
}
