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

import net.fabricmc.loader.api.FabricLoader;

import me.modmuss50.optifabric.util.RemappingUtils;

public class FluidRendererFix implements ClassFixer {
	//Add a little decoy so Fabric injects a little earlier, then patch the result
	private static final String OLD_RENDER_DESC = "(Lnet/minecraft/class_1920;Lnet/minecraft/class_2338;Lnet/minecraft/class_4588;Lnet/minecraft/class_3610;)Z";
	private static final String NEW_RENDER_DESC = "(Lnet/minecraft/class_1920;Lnet/minecraft/class_2338;Lnet/minecraft/class_4588;Lnet/minecraft/class_2680;Lnet/minecraft/class_3610;)V";

    private static final String VERTEX_VANILLA_DESC =
            "(Lnet/minecraft/class_4588;FFFFFFFFIF)V";

	@Override
	public void fix(ClassNode optifine, ClassNode minecraft) {
		String oldRender = RemappingUtils.getMethodName("class_775", "method_3347", OLD_RENDER_DESC);
		String oldRenderDesc = RemappingUtils.mapMethodDescriptor(OLD_RENDER_DESC);
		String newRender = RemappingUtils.getMethodName("class_775", "method_3347", NEW_RENDER_DESC);
		String newRenderDesc = RemappingUtils.mapMethodDescriptor(NEW_RENDER_DESC);

        if (FabricLoader.getInstance().isModLoaded("naturalwaters")) {
            patchNaturalWatersAlpha(optifine);
        }

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
                    VarInsnNode colorLoad = findColorLoad(setTint);

                    InsnList extra = new InsnList();
                    extra.add(new VarInsnNode(Opcodes.ALOAD, 1));
                    extra.add(new VarInsnNode(Opcodes.ALOAD, 2));
                    extra.add(new VarInsnNode(Opcodes.ALOAD, isNewRender ? 5 : 4));
                    extra.add(new VarInsnNode(Opcodes.ILOAD, colorLoad.var));
                    extra.add(new MethodInsnNode(
                            Opcodes.INVOKESTATIC,
                            "me/modmuss50/optifabric/patcher/fixes/FluidRendererFixExternal",
                            "getFabricColor",
                            "(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;I)I",
                            false
                    ));
                    extra.add(new VarInsnNode(Opcodes.ISTORE, colorLoad.var));

                    method.instructions.insertBefore(colorLoad, extra);
                    method.maxStack = Math.max(method.maxStack, 4);

                    log("class_775 render patched with live Fabric fluid color bridge");
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

    private static void patchNaturalWatersAlpha(ClassNode optifine) {
        String vertexVanillaDesc =
                RemappingUtils.mapMethodDescriptor(VERTEX_VANILLA_DESC);

        MethodNode vertexVanilla = null;

        for (MethodNode method : optifine.methods) {
            if (!"vertexVanilla".equals(method.name) ||
                    !vertexVanillaDesc.equals(method.desc)) {
                continue;
            }

            if (vertexVanilla != null) {
                throw new IllegalStateException(
                        "Found multiple OptiFine vertexVanilla methods"
                );
            }

            vertexVanilla = method;
        }

        if (vertexVanilla == null) {
            log(
                    "class_775 vertexVanilla missing, " +
                            "skipping Natural Waters alpha bridge"
            );
            return;
        }

        boolean hasAlphaLoad = false;

        for (AbstractInsnNode node : vertexVanilla.instructions) {
            if (node instanceof VarInsnNode &&
                    node.getOpcode() == Opcodes.FLOAD &&
                    ((VarInsnNode) node).var == 11) {
                hasAlphaLoad = true;
                break;
            }
        }

        if (!hasAlphaLoad) {
            throw new IllegalStateException(
                    "OptiFine vertexVanilla does not load alpha parameter"
            );
        }

        AbstractInsnNode first =
                firstRealInstruction(vertexVanilla);

        if (first == null) {
            throw new IllegalStateException(
                    "OptiFine vertexVanilla has no instructions"
            );
        }

        InsnList bridge = new InsnList();
        bridge.add(new VarInsnNode(Opcodes.ALOAD, 0));
        bridge.add(new VarInsnNode(Opcodes.FLOAD, 11));
        bridge.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                "me/modmuss50/optifabric/patcher/fixes/FluidRendererFixExternal",
                "getNaturalWatersAlpha",
                "(Ljava/lang/Object;F)F",
                false
        ));
        bridge.add(new VarInsnNode(Opcodes.FSTORE, 11));

        vertexVanilla.instructions.insertBefore(
                first,
                bridge
        );

        vertexVanilla.maxStack =
                Math.max(vertexVanilla.maxStack, 2);

        log(
                "class_775 vertexVanilla patched with " +
                        "Natural Waters alpha bridge"
        );
    }

    private static AbstractInsnNode firstRealInstruction(
            MethodNode method
    ) {
        AbstractInsnNode node =
                method.instructions.getFirst();

        while (
                node != null &&
                (
                        node.getType() == AbstractInsnNode.LABEL ||
                        node.getType() == AbstractInsnNode.LINE ||
                        node.getType() == AbstractInsnNode.FRAME
                )
        ) {
            node = node.getNext();
        }

        return node;
    }
    private static VarInsnNode findColorLoad(JumpInsnNode branch) {
        AbstractInsnNode node = branch.getPrevious();

        while (node != null &&
                (node.getType() == AbstractInsnNode.LABEL ||
                        node.getType() == AbstractInsnNode.LINE ||
                        node.getType() == AbstractInsnNode.FRAME)) {
            node = node.getPrevious();
        }

        if (!(node instanceof VarInsnNode) ||
                node.getOpcode() != Opcodes.ILOAD) {
            throw new IllegalStateException(
                    "Unable to find fluid color local before OptiFine color branch"
            );
        }

        return (VarInsnNode) node;
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
