package me.modmuss50.optifabric.mod;

import javax.lang.model.SourceVersion;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import net.fabricmc.tinyremapper.ClassInstance;
import net.fabricmc.tinyremapper.InputTag;
import net.fabricmc.tinyremapper.MemberInstance;
import net.fabricmc.tinyremapper.api.TrMember.MemberType;

public class Preloader {
	public static void preloadTinyRemapper() {
		String packageName = ClassInstance.class.getPackage().getName();
		ClassInstance.class.getClass();
		InputTag.class.getClass();
		MemberInstance.class.getClass();
		MemberType.class.getClass();
		SourceVersion.class.getClass(); //Classes in javax don't get the special treatment java ones do

		ClassLoader classLoader = Preloader.class.getClassLoader();
		for (String type : new String[] {
				ClassInstance.class.getName(),
				InputTag.class.getName(),
				MemberInstance.class.getName(),
				MemberType.class.getName(),
				packageName + ".AsmClassRemapper$AsmAnnotationRemapper",
				packageName + ".AsmClassRemapper$AsmAnnotationRemapper$AsmArrayAttributeAnnotationRemapper",
				packageName + ".AsmClassRemapper$AsmRecordComponentRemapper",
				packageName + ".AsmClassRemapper$AsmFieldRemapper",
				packageName + ".AsmClassRemapper$AsmMethodRemapper",
				packageName + ".Propagator",
				packageName + ".TinyRemapper$2",
				packageName + ".TinyRemapper$Direction",
				packageName + ".VisitTrackingClassRemapper$VisitKind",
		}) {
			preloadClass(classLoader, type);
		}

		for (String type : discoverTinyRemapperClasses()) {
			preloadClass(classLoader, type);
		}
	}

	private static void preloadClass(ClassLoader classLoader, String type) {
		try {
			Class.forName(type, false, classLoader);
		} catch (ClassNotFoundException e) {
			System.err.println("Failed to preload " + type);
			e.printStackTrace();
		}
	}

	private static List<String> discoverTinyRemapperClasses() {
		String packageName = ClassInstance.class.getPackage().getName();
		String packagePath = packageName.replace('.', '/');
		URL location = ClassInstance.class.getProtectionDomain().getCodeSource().getLocation();

		try {
			Path root = Paths.get(location.toURI());
			List<String> classes = new ArrayList<>();

			if (Files.isDirectory(root)) {
				Path packageRoot = root.resolve(packagePath);
				if (Files.exists(packageRoot)) {
					Files.walkFileTree(packageRoot, new SimpleFileVisitor<Path>() {
						@Override
						public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
							if (file.getFileName().toString().endsWith(".class")) {
								classes.add(toClassName(root, file));
							}
							return FileVisitResult.CONTINUE;
						}
					});
				}
			} else {
				try (JarFile jarFile = new JarFile(root.toFile())) {
					Enumeration<JarEntry> entries = jarFile.entries();
					while (entries.hasMoreElements()) {
						JarEntry entry = entries.nextElement();
						String name = entry.getName();
						if (!entry.isDirectory() && name.startsWith(packagePath) && name.endsWith(".class")) {
							classes.add(name.substring(0, name.length() - 6).replace('/', '.'));
						}
					}
				}
			}

			classes.removeIf(name -> name.endsWith(".module-info") || name.endsWith(".package-info"));
			classes.sort(Comparator
					.comparingInt(Preloader::nestingDepth)
					.thenComparing(String::compareTo));
			return Collections.unmodifiableList(classes);
		} catch (IOException | URISyntaxException e) {
			System.err.println("Failed to discover TinyRemapper classes");
			e.printStackTrace();
			return Collections.emptyList();
		}
	}

	private static String toClassName(Path root, Path file) {
		Path relative = root.relativize(file);
		String name = relative.toString();
		return name.substring(0, name.length() - 6).replace('/', '.').replace('\\', '.');
	}

	private static int nestingDepth(String className) {
		int depth = 0;
		for (int i = 0; i < className.length(); i++) {
			if (className.charAt(i) == '$') {
				depth++;
			}
		}
		return depth;
	}
}
