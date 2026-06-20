package me.modmuss50.optifabric.mod;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.fabricmc.loader.util.version.SemanticVersionImpl;
import net.fabricmc.loader.util.version.SemanticVersionPredicateParser;
import net.fabricmc.loader.util.version.VersionParsingException;

import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;

public class CrashReportMixinPlugin implements IMixinConfigPlugin {
	@Override
	public void onLoad(String mixinPackage) {
	}

	@Override
	public String getRefMapperConfig() {
		return null;
	}

	@Override
	public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
		if ("me.modmuss50.optifabric.mixin.CrashReportMixin".equals(mixinClassName) && isMinecraftAtLeast(">=1.21")) {
			return false;
		}
		return true;
	}

	@Override
	public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
	}

	@Override
	public List<String> getMixins() {
		return Collections.emptyList();
	}

	@Override
	public void preApply(String targetClassName, org.objectweb.asm.tree.ClassNode targetClass, String mixinClassName, org.spongepowered.asm.mixin.extensibility.IMixinInfo mixinInfo) {
	}

	@Override
	public void postApply(String targetClassName, org.objectweb.asm.tree.ClassNode targetClass, String mixinClassName, org.spongepowered.asm.mixin.extensibility.IMixinInfo mixinInfo) {
	}

	private static boolean isMinecraftAtLeast(String versionRange) {
		ModContainer minecraft = FabricLoader.getInstance().getModContainer("minecraft").orElse(null);
		if (minecraft == null) return false;

		ModMetadata metadata = minecraft.getMetadata();
		try {
			SemanticVersionImpl version = new SemanticVersionImpl(metadata.getVersion().getFriendlyString(), false);
			return SemanticVersionPredicateParser.create(versionRange).test(version);
		} catch (VersionParsingException e) {
			return false;
		}
	}
}
