package me.modmuss50.optifabric.mod;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import net.fabricmc.loader.api.FabricLoader;

final class StartupLog {
	private StartupLog() {
	}

	static void record(String message) {
		try {
			Path logFile = getLogFile();
			Files.writeString(logFile, message + System.lineSeparator(), StandardCharsets.UTF_8,
					StandardOpenOption.CREATE, StandardOpenOption.APPEND);
		} catch (IOException | RuntimeException ignored) {
			// Best effort only.
		}
	}

	private static Path getLogFile() {
		String gameDir = System.getProperty("optifabric.gameDir");
		if (gameDir != null && !gameDir.isBlank()) {
			return Paths.get(gameDir).resolve("optifabric-startup.log");
		}

		try {
			return FabricLoader.getInstance().getGameDirectory().toPath().resolve("optifabric-startup.log");
		} catch (RuntimeException e) {
			return Paths.get(System.getProperty("user.dir", ".")).toAbsolutePath().resolve("optifabric-startup.log");
		}
	}
}
