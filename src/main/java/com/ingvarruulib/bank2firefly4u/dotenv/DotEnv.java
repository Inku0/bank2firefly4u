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

			for (String line : content) {
				String[] splitLine = line.split("=");

				if (splitLine.length != 2) {
					LOGGER.warning("Invalid line in env file: " + line);
					continue;
				}

				String key = splitLine[0];
				String value = splitLine[1];

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
