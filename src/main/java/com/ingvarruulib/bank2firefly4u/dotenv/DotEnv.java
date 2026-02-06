package com.ingvarruulib.bank2firefly4u.dotenv;

import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class DotEnv {
	private final File dotEnvFile;
	private static final Logger LOGGER = Logger.getLogger(DotEnv.class.getName());

	public DotEnv() {
		dotEnvFile = new File(".env");
	}

	@Nullable
	public Map<String, String> getEnv() {
		if (!dotEnvFile.canRead()) {
			return null;
		}

		Map<String, String> env = new HashMap<>();

		try (var inputStream = new FileInputStream(dotEnvFile)) {
			List<String> content = Files.readAllLines(dotEnvFile.toPath());

			for (String line : content) {
				String[] splitLine = line.split("=");

				if (splitLine.length != 2) {
					LOGGER.warning("Invalid line in env file: " + line);
					continue;
				}

				String key = splitLine[0];
				String value = splitLine[1];

				env.put(key, value);
			}

			return env;

		} catch (FileNotFoundException e) {
			LOGGER.severe(".env file not found");
			return null;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Nullable
	public String getEnv(String name) {
		Map<String, String> env = getEnv();
		if (env == null) {
			return null;
		}

		if (!env.containsKey(name)) {
			return null;
		}

		return env.get(name);
	}
}
