package me.modmuss50.optifabric.compat.full_slabs;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.fabricmc.loader.util.version.SemanticVersionImpl;
import net.fabricmc.loader.util.version.SemanticVersionPredicateParser;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import net.fabricmc.tinyremapper.IMappingProvider.Member;

import me.modmuss50.optifabric.compat.InterceptingMixinPlugin;
import me.modmuss50.optifabric.util.RemappingUtils;

public class FullSlabsMixinPlugin extends InterceptingMixinPlugin {
	@Override
	public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
		if (isMinecraftAtLeast(">=1.21")) {
			log("full-slabs-skip target=" + targetClassName + " mixin=" + mixinClassName + " minecraft>=1.21");
			return false;
		}

		return true;
	}

	@Override
	public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
		if ("BlockRenderManagerMixin".equals(mixinInfo.getName())) {//BlockModels, getModel, (BlockState)BakedModel
			Member getModel = RemappingUtils.mapMethod("class_773", "method_3335", "(Lnet/minecraft/class_2680;)Lnet/minecraft/class_1087;");
			String renderDamageDesc = "(Lnet/minecraft/class_2680;Lnet/minecraft/class_2338;Lnet/minecraft/class_1920;Lnet/minecraft/class_4587;Lnet/minecraft/class_4588;)V";
			String renderDamage = RemappingUtils.getMethodName("class_776", "method_23071", renderDamageDesc); //(BlockState, BlockPos, BlockRenderView, MatrixStack, VertexConsumer)
			renderDamageDesc = RemappingUtils.mapMethodDescriptor(renderDamageDesc); //^ BlockRenderManager, renderDamage 

			for (MethodNode method : targetClass.methods) {
				if (renderDamage.equals(method.name) && renderDamageDesc.equals(method.desc)) {
					LabelNode skip = new LabelNode();

					InsnList extra = new InsnList();
					extra.add(new JumpInsnNode(Opcodes.GOTO, skip));
					extra.add(new InsnNode(Opcodes.NULL));
					extra.add(new InsnNode(Opcodes.NULL));
					extra.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, getModel.owner, getModel.name, getModel.desc, false));
					extra.add(new InsnNode(Opcodes.POP));
					extra.add(skip);

					method.instructions.insertBefore(method.instructions.getLast(), extra);
					break;
				}
			}
		}

		super.preApply(targetClassName, targetClass, mixinClassName, mixinInfo);
	}

	private static void log(String message) {
		System.err.println("[OptiFabric] " + message);
		System.err.flush();
	}

	private static boolean isMinecraftAtLeast(String versionRange) {
		ModContainer minecraft = FabricLoader.getInstance().getModContainer("minecraft").orElse(null);
		if (minecraft == null) return false;

		ModMetadata metadata = minecraft.getMetadata();
		try {
			SemanticVersionImpl version = new SemanticVersionImpl(metadata.getVersion().getFriendlyString(), false);
			return SemanticVersionPredicateParser.create(versionRange).test(version);
		} catch (RuntimeException e) {
			return false;
		}
	}
}
