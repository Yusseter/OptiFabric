package me.modmuss50.optifabric.patcher.fixes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.ModMetadata;
import net.fabricmc.loader.util.version.SemanticVersionImpl;
import net.fabricmc.loader.util.version.SemanticVersionPredicateParser;

import me.modmuss50.optifabric.util.RemappingUtils;

public class OptifineFixer {

	public static final OptifineFixer INSTANCE = new OptifineFixer();

	private final Map<String, List<ClassFixer>> classFixes = new HashMap<>();
	private final Set<String> skippedClass = new HashSet<>();

	private OptifineFixer() {
		//net/minecraft/client/render/chunk/ChunkBuilder$ChunkData
		registerFix("class_846$class_849", new ChunkDataFix());

		//net/minecraft/client/render/chunk/ChunkBuilder$BuiltChunk$RebuildTask
		registerFix("class_846$class_851$class_4578", new ChunkRendererFix());

		//net/minecraft/client/render/block/BlockModelRenderer$AmbientOcclusionCalculator
		registerFix("class_778$class_780", new AmbientOcclusionCalculatorFix());

		//net/minecraft/client/Keyboard
		if (FabricLoader.getInstance().isModLoaded("replaymod")) {
			registerFix("class_309", new KeyboardFix());
		}

		//net/minecraft/client/texture/SpriteAtlasTexture
		registerFix("class_1059", new SpriteAtlasTextureFix());

		//net/minecraft/client/particle/ParticleManager
		registerFix("class_702", new ParticleManagerFix());

		//net/minecraft/client/render/model/BakedModelManager
		registerFix("class_1092", new BakedModelManagerFix());
		registerFix("class_1092$1", new BakedModelManagerInnerFix());

		//net/minecraft/client/render/item/model/BasicItemModel
		registerFix("class_10430", new BasicItemModelFix());

		//net/minecraft/client/render/block/BlockRenderManager
		registerFix("class_776", new BlockRenderManagerFix());

		//net/minecraft/client/render/WorldRenderer
		registerFix("class_761", new WorldRendererFix());

		//net/minecraft/client/gui/hud/InGameOverlayRenderer
		registerFix("class_4603", new InGameOverlayRendererFix());

		//net/minecraft/client/render/model/json/ModelOverrideList
		registerFix("class_806", new ModelOverrideListFix());

		if (FabricLoader.getInstance().isModLoaded("fabric-rendering-fluids-v1")) {
			registerFix("class_775", new FluidRendererFix());
		}

		if (FabricLoader.getInstance().isModLoaded("uglyscoreboardfix")) {
			registerFix("class_329", "me.modmuss50.optifabric.compat.uglyscoreboardfix.InGameHudFix");
		}

		//net/minecraft/block/entity/BlockEntity
		skipClass("class_2586");
	}

	private void registerFix(String className, ClassFixer classFixer) {
		classFixes.computeIfAbsent(RemappingUtils.getClassName(className), s -> new ArrayList<>()).add(classFixer);
	}

	private void registerFix(String className, String classFixerName) {
		try {
			Class<?> type = Class.forName(classFixerName);
			registerFix(className, (ClassFixer) type.getDeclaredConstructor().newInstance());
		} catch (ReflectiveOperationException e) {
			throw new IllegalStateException("Failed to load " + classFixerName, e);
		}
	}

	@SuppressWarnings("SameParameterValue") //Might be useful in future
	private void skipClass(String className) {
		skippedClass.add(RemappingUtils.getClassName(className));
	}

	private static boolean isMinecraftBefore(String versionRange) {
		ModContainer minecraft = FabricLoader.getInstance().getModContainer("minecraft").orElse(null);
		if (minecraft == null) return false;

		ModMetadata metadata = minecraft.getMetadata();
		try {
			SemanticVersionImpl version = new SemanticVersionImpl(metadata.getVersion().getFriendlyString(), false);
			return SemanticVersionPredicateParser.create(versionRange).test(version);
		} catch (net.fabricmc.loader.util.version.VersionParsingException e) {
			return false;
		}
	}

	public boolean shouldSkip(String className) {
		return skippedClass.contains(className);
	}

	public List<ClassFixer> getFixers(String className) {
		return classFixes.getOrDefault(className, Collections.emptyList());
	}
}
