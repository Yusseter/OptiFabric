package me.modmuss50.optifabric.compat.fabricrendererapi;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.fabricmc.loader.util.version.SemanticVersionImpl;
import net.fabricmc.loader.util.version.SemanticVersionPredicateParser;

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
		if (isMinecraftAtLeast(">=1.21")) {
			switch (mixinClassName) {
			case "me.modmuss50.optifabric.compat.fabricrendererapi.mixin.BlockModelRendererMixin":
			case "me.modmuss50.optifabric.compat.fabricrendererapi.mixin.BlockRenderManagerMixin":
			case "me.modmuss50.optifabric.compat.fabricrendererapi.mixin.BlockModelRendererNewMixin":
			case "me.modmuss50.optifabric.compat.fabricrendererapi.mixin.BlockRenderManagerNewMixin":
				log("renderer-skip target=" + targetClassName + " mixin=" + mixinClassName + " minecraft>=1.21");
				return false;
			default:
				break;
			}
		}

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
