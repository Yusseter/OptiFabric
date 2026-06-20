package me.modmuss50.optifabric.mod;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.fabricmc.tinyremapper.IMappingProvider;

public final class OptifabricRemapMain {
	private OptifabricRemapMain() {
	}

	public static void main(String[] args) throws Exception {
		try {
			if (args.length != 6) {
				throw new IllegalArgumentException("Usage: <input> <output> <namespace> <developmentEnvironment> <lambdaMappings> <libsFile>");
			}

			Path input = Paths.get(args[0]);
			Path output = Paths.get(args[1]);
			String namespace = args[2];
			boolean developmentEnvironment = Boolean.parseBoolean(args[3]);
			Path lambdaMappings = Paths.get(args[4]);
			Path libsFile = Paths.get(args[5]);

			StartupLog.record("remap-helper-main-start");
			StartupLog.record("remap-helper-before-lambda-load");
			IMappingProvider extra = loadLambdaMappings(lambdaMappings);
			StartupLog.record("remap-helper-after-lambda-load");
			StartupLog.record("remap-helper-before-mappings");
			IMappingProvider mappings = OptifineSetup.createMappings("official", namespace, extra, developmentEnvironment);
			StartupLog.record("remap-helper-after-mappings");
			Path[] libraries = readPathList(libsFile);
			StartupLog.record("remap-helper-before-remap");
			OptifineSetup.remapOptifine(input, libraries, output, mappings, developmentEnvironment);
			StartupLog.record("remap-helper-main-finished");
		} catch (Throwable t) {
			StartupLog.record("remap-helper-failed");
			StringWriter writer = new StringWriter();
			t.printStackTrace(new PrintWriter(writer));
			for (String line : writer.toString().split("\\R")) {
				StartupLog.record("remap-helper-error:" + line);
			}
			throw t;
		}
	}

	private static IMappingProvider loadLambdaMappings(Path file) throws IOException {
		List<String> lines = Files.exists(file) ? Files.readAllLines(file, StandardCharsets.UTF_8) : Collections.emptyList();
		List<Entry> entries = new ArrayList<>();
		for (String line : lines) {
			if (line.isBlank()) {
				continue;
			}

			String[] parts = line.split("\t", 4);
			if (parts.length != 4) {
				throw new IOException("Malformed lambda mapping line: " + line);
			}
			entries.add(new Entry(parts[0], parts[1], parts[2], parts[3]));
		}

		return out -> {
			for (Entry entry : entries) {
				out.acceptMethod(new IMappingProvider.Member(entry.owner, entry.name, entry.desc), entry.dstName);
			}
		};
	}

	private static Path[] readPathList(Path file) throws IOException {
		List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
		List<Path> paths = new ArrayList<>(lines.size());
		for (String line : lines) {
			if (!line.isBlank()) {
				paths.add(Paths.get(line));
			}
		}
		return paths.toArray(Path[]::new);
	}

	private static final class Entry {
		private final String owner;
		private final String name;
		private final String desc;
		private final String dstName;

		private Entry(String owner, String name, String desc, String dstName) {
			this.owner = owner;
			this.name = name;
			this.desc = desc;
			this.dstName = dstName;
		}
	}
}
