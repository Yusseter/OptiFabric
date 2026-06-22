package me.modmuss50.optifabric.compat.fabricrendererapi;

import org.objectweb.asm.tree.ClassNode;

import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.mixin.transformer.ClassInfo;

import me.modmuss50.optifabric.compat.EmptyMixinPlugin;
import me.modmuss50.optifabric.util.MixinUtils;

public class RendererMixinPlugin extends EmptyMixinPlugin {
	private static void log(String message) {
		System.err.println("[OptiFabric] " + message);
		System.err.flush();
	}

	@Override
	public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
		return true;
	}

	@Override
	public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
		log("renderer-preApply target=" + targetClassName + " mixin=" + mixinInfo.getName() + " classRef=" + mixinInfo.getClassRef());
		switch (mixinInfo.getName()) {
		case "BlockModelRendererMixin":
		case "BlockRenderManagerMixin":
		case "BlockModelRendererNewMixin":
		case "BlockRenderManagerNewMixin":{
			ClassInfo info = ClassInfo.forName(targetClassName);
			MixinUtils.completeClassInfo(info, targetClass.methods);
			break;
		}
		}
	}

	@Override
	public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
		log("renderer-postApply target=" + targetClassName + " mixin=" + mixinInfo.getName() + " classRef=" + mixinInfo.getClassRef());
	}
}
