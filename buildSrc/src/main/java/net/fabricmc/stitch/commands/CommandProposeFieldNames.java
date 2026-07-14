package net.fabricmc.stitch.commands;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import net.fabricmc.mappings.EntryTriple;

public class CommandProposeFieldNames {
	public interface NameAcceptor {
		boolean use(EntryTriple field, String oldName, String newName);
	}

	public String getHelpString() {
		return "";
	}

	public boolean isArgumentCountValid(int count) {
		return false;
	}

	public void run(String[] args) {
		throw new UnsupportedOperationException("Field name proposal is disabled");
	}

	public static void run(File minecraftJar, File inputMappings, File outputMappings, String from, String to,
			NameAcceptor acceptor) throws IOException {
		Files.copy(inputMappings.toPath(), outputMappings.toPath(), StandardCopyOption.REPLACE_EXISTING);
	}
}
