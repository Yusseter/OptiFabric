package me.modmuss50.optifabric.mod;

import java.lang.reflect.Modifier;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.MethodNode;

import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;

import net.fabricmc.loader.api.FabricLoader;

import com.chocohead.mm.api.ClassTinkerers;
import org.objectweb.asm.ClassWriter;

import me.modmuss50.optifabric.patcher.ClassCache;
import me.modmuss50.optifabric.patcher.fixes.OptifineFixer;
import me.modmuss50.optifabric.util.ASMUtils;

public class OptifineInjector {
	private static Set<String> patched = new HashSet<>();
	private static final Map<String, List<String>> WATCHED_METHODS = Map.of(
			"net/minecraft/class_1092", List.of("method_65750(Ljava/util/Map$Entry;)Lcom/mojang/datafixers/util/Pair;"),
			"net/minecraft/class_761", List.of("method_62214(Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lnet/minecraft/class_11658;Lnet/minecraft/class_3695;Lorg/joml/Matrix4f;Lnet/minecraft/class_9925;Lnet/minecraft/class_9925;ZLnet/minecraft/class_9925;Lnet/minecraft/class_9925;)V"),
			"net/minecraft/class_776", List.of(
					"method_3351()Lnet/minecraft/class_773;",
					"method_3349(Lnet/minecraft/class_2680;)Lnet/minecraft/class_1087;",
					"method_3353(Lnet/minecraft/class_2680;Lnet/minecraft/class_4587;Lnet/minecraft/class_4597;II)V",
					"method_23071(Lnet/minecraft/class_2680;Lnet/minecraft/class_2338;Lnet/minecraft/class_1920;Lnet/minecraft/class_4587;Lnet/minecraft/class_4588;)V",
					"method_3355(Lnet/minecraft/class_2680;Lnet/minecraft/class_2338;Lnet/minecraft/class_1920;Lnet/minecraft/class_4587;Lnet/minecraft/class_4588;ZLjava/util/List;)V",
					"method_3352(Lnet/minecraft/class_2338;Lnet/minecraft/class_1920;Lnet/minecraft/class_4588;Lnet/minecraft/class_2680;Lnet/minecraft/class_3610;)V",
					"method_3350()Lnet/minecraft/class_778;"),
			"net/minecraft/class_309", List.of("method_1466(JILnet/minecraft/class_11908;)V", "method_1457(JLnet/minecraft/class_11905;)V"));
	private final ClassCache classCache;

	private static void log(String message) {
		System.err.println("[OptiFabric] " + message);
		System.err.flush();
	}

	public OptifineInjector(ClassCache classCache) {
		this.classCache = classCache;
	}

	public Optional<ClassNode> predictFuture(String className) {
		byte[] bytes = classCache.getClass(className);
		return bytes != null ? Optional.of(ASMUtils.readClass(bytes)) : Optional.empty();
	}

	public void setup() {
		StartupLog.record("injector-setup-start");
		Consumer<ClassNode> transformer = target -> {
			StartupLog.record("patch-start-" + target.name);
			log("Patching class " + target.name);

			//Avoid double patching things, not that this should happen
			if (!patched.add(target.name)) {
				StartupLog.record("patch-skip-duplicate-" + target.name);
				System.err.println("Already patched " + target.name);
				return;
			}

			//Skip applying class patches we veto
			if (OptifineFixer.INSTANCE.shouldSkip(target.name)) {
				StartupLog.record("patch-skip-veto-" + target.name);
				log("Skipping vetoed class " + target.name);
				return;
			}

			//Remember the access we started with
			Object2IntMap<String> memberToAccess = new Object2IntArrayMap<>(target.methods.size());
			memberToAccess.defaultReturnValue(-1);
			for (MethodNode method : target.methods) {
				memberToAccess.put(method.name + method.desc, method.access);
			}
			for (FieldNode field : target.fields) {
				memberToAccess.put(field.name + ' ' + field.desc, field.access);
			}

			//I cannot imagine this being very good at all
			ClassNode source = getSourceClassNode(target);
			StartupLog.record("patch-source-loaded-" + target.name);

			//Patch the class if required
			OptifineFixer.INSTANCE.getFixers(target.name).forEach(classFixer -> {
				String fixerName = classFixer.getClass().getSimpleName();
				StartupLog.record("patch-fixer-start-" + target.name + "-" + fixerName);
				log("Applying fixer " + fixerName + " to " + target.name);
				try {
					classFixer.fix(source, target);
				} catch (RuntimeException | Error e) {
					StartupLog.record("patch-fixer-fail-" + target.name + "-" + fixerName);
					log("Fixer " + fixerName + " failed for " + target.name + ": " + e);
					throw e;
				}
				StartupLog.record("patch-fixer-end-" + target.name + "-" + fixerName);
			});

			logWatchedMethods("after-fix", target.name, source);

			target.methods = source.methods;
			target.fields = source.fields;
			target.interfaces = source.interfaces;
			target.superName = source.superName;

			//Classes should be read with frames expanded (as Mixin itself does it), in which case this should all be fine
			for (MethodNode methodNode : target.methods) {
				for (AbstractInsnNode insnNode : methodNode.instructions.toArray()) {
					if (insnNode instanceof FrameNode) {
						FrameNode frameNode = (FrameNode) insnNode;
						if (frameNode.local == null) {
							throw new IllegalStateException("Null locals in " + frameNode.type + " frame @ " + source.name + "#" + methodNode.name + methodNode.desc);
						}
					}
				}
			}

			// Lets make every class we touch match the access it used to have
			target.access = widerAccess(target.access, source.access);
			for (MethodNode method : target.methods) {
				int access = memberToAccess.getInt(method.name + method.desc);
				if (access != -1) method.access = widerAccess(access, method.access);
			}
			for (FieldNode field : target.fields) {
				int access = memberToAccess.getInt(field.name + ' ' + field.desc);
				if (access != -1) field.access = widerAccess(access, field.access);
			}

			StartupLog.record("patch-end-" + target.name);
			log("Finished patching class " + target.name);
			logWatchedMethods("final", target.name, target);
			dumpWatchedClass(target);
		};

		for (String name : classCache.getClasses()) {
			StartupLog.record("injector-register-" + name);
			log("Registering replacement for " + name);
			ClassTinkerers.addReplacement(name, transformer);
		}
		StartupLog.record("injector-setup-end");
	}

	private static int widerAccess(int origin, int target) {
		if (!Modifier.isFinal(origin)) target &= ~Modifier.FINAL;

		switch (target & 0x7) {
		case Modifier.PUBLIC:
			return target;

		case Modifier.PROTECTED:
			return Modifier.isPublic(origin) ? (target & (~0x7)) | Modifier.PUBLIC : target;

		case 0:
			return Modifier.isPrivate(origin) ? target : (target & (~0x7)) | (origin & 0x7);

		case Modifier.PRIVATE:
			return (target & (~0x7)) | (origin & 0x7);

		default:
			if (FabricLoader.getInstance().isDevelopmentEnvironment()) {
				throw new AssertionError("Unexpected access: " + target + " (transformed from " + origin + ')');
			}

			return target;
		}
	}

	private ClassNode getSourceClassNode(ClassNode classNode) {
		byte[] bytes = classCache.popClass(classNode.name);
		if(bytes == null) {
			throw new RuntimeException("Failed to find patched class for: " + classNode.name);
		}
		return ASMUtils.readClass(bytes);
	}

	private static void logWatchedMethods(String phase, String className, ClassNode node) {
		List<String> watched = WATCHED_METHODS.get(className);
		if (watched == null) return;

		StringBuilder message = new StringBuilder();
		message.append("watch-").append(phase).append('-').append(className).append(" methods=").append(node.methods.size());
		message.append(" all=");
		boolean firstMethod = true;
		for (MethodNode method : node.methods) {
			if (!firstMethod) message.append(',');
			firstMethod = false;
			message.append(method.name).append(method.desc);
		}
		for (String signature : watched) {
			boolean present = node.methods.stream().anyMatch(method -> (method.name + method.desc).equals(signature));
			message.append(' ').append(signature).append('=').append(present);
		}
		String text = message.toString();
		StartupLog.record(text);
		log(text);
	}

	private static void dumpWatchedClass(ClassNode node) {
		if (!WATCHED_METHODS.containsKey(node.name)) return;

		try {
			Path dumpDir = FabricLoader.getInstance().getGameDirectory().toPath().resolve("optifabric-debug");
			Files.createDirectories(dumpDir);

			Path dumpFile = dumpDir.resolve(node.name.replace('/', '_').replace('$', '_') + ".class");
			ClassWriter writer = new ClassWriter(0);
			node.accept(writer);
			Files.write(dumpFile, writer.toByteArray());
			log("dumped " + node.name + " to " + dumpFile);
		} catch (IOException | RuntimeException e) {
			log("failed to dump " + node.name + ": " + e);
		}
	}
}
