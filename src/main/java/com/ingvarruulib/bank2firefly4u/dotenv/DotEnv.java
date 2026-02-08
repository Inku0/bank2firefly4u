package com.ingvarruulib.bank2firefly4u.dotenv;

import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class DotEnv {
	private static final Logger LOGGER = Logger.getLogger(DotEnv.class.getName());
	private final File dotEnvFile;
	private final Map<String, String> dotEnv;

	public DotEnv() {
		this.dotEnvFile = new File(".env");
		if (!this.dotEnvFile.exists()) {
			throw new MissingEnvException(".env file not found");
		}

		this.dotEnv = new HashMap<>();

		try {
			List<String> content = Files.readAllLines(dotEnvFile.toPath());

			for (String rawLine : content) {
				String line = rawLine.trim();
				if (line.isEmpty() || line.startsWith("#")) {
					continue;
				}

				int idx = line.indexOf('=');
				if (idx <= 0) {
					LOGGER.warning("Invalid line in env file: " + line);
					continue;
				}

				String key = line.substring(0, idx).trim();
				String value = line.substring(idx + 1).trim();

				if ((value.startsWith("\"") && value.endsWith("\"")) || (value.startsWith("'") && value.endsWith("'"))) {
					value = value.substring(1, value.length() - 1);
				}

				dotEnv.put(key, value);
			}

		} catch (IOException e) {
			throw new RuntimeException(e);
		}

	}

	@Nullable
	public Map<String, String> getEnv() {
		return dotEnv;
	}

	@Nullable
	public String getEnv(String name) {
		if (dotEnv == null) {
			return null;
		}

		if (!dotEnv.containsKey(name)) {
			return null;
		}

		return dotEnv.get(name);
	}
}
