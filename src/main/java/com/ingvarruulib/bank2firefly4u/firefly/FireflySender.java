package com.ingvarruulib.bank2firefly4u.firefly;

import com.ingvarruulib.bank2firefly4u.MultiPartBody;
import com.ingvarruulib.bank2firefly4u.dotenv.DotEnv;
import com.ingvarruulib.bank2firefly4u.dotenv.MissingEnvException;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.logging.Logger;

public class FireflySender {
	private static final Logger LOGGER = Logger.getLogger(FireflySender.class.getName());
	private static final String AUTOUPLOAD = "/autoupload?secret=";
	FireflyLogin login;

	public FireflySender(FireflyLogin fireflyLogin) {
		this.login = fireflyLogin;
	}

	public FireflySender() {
		this.login = getLogin();
	}

	public HttpRequest.BodyPublisher createMultipartData(Path... files) {
		var multipart = new MultiPartBody();

		for (Path file : files) {
			if (file.toString().contains("statement")) {
				multipart.add("importable", file);
			} else if (file.toString().contains("config")) {
				multipart.add("json", file);
			}
		}

		return multipart.finish();
	}

	public boolean postStatementCsv(File statement) {
		URI importer;
		Path config = Path.of("config.json");

		try {
			// params set here
			importer = new URI(this.login.importerUrl() + AUTOUPLOAD + this.login.autoImportSecret());
		} catch (URISyntaxException ex) {
			LOGGER.severe("Malformed URL in .env: " + this.login.importerUrl());
			return false;
//			throw new RuntimeException(ex);
		}

		try (HttpClient client = HttpClient.newHttpClient()) {
			HttpRequest req = HttpRequest.newBuilder()
					.uri(importer)
					.header("Accept", "application/json")
					.header("Authorization", "Bearer " + this.login.token())
					.POST(createMultipartData(config, statement.toPath()))
					.build();

			var response = client.send(req, HttpResponse.BodyHandlers.ofString());
			System.out.println(response.body());
		} catch (IOException | InterruptedException e) {
			throw new RuntimeException(e);
		}

		return true;
	}

	@NotNull
	public static FireflyLogin getLogin() {
		var dotEnv =  new DotEnv();
		String autoImportSecret = dotEnv.getEnv("AUTO_IMPORT_SECRET");
		String importerUrl = dotEnv.getEnv("DATA_IMPORTER_BASE");
		String token = dotEnv.getEnv("PERSONAL_ACCESS_TOKEN");

		if (autoImportSecret == null || importerUrl == null || token == null) {
			throw new MissingEnvException("AUTO_IMPORT_SECRET, DATA_IMPORTER_BASE and PERSONAL_ACCESS_TOKEN must be set in .env");
		}
		return new FireflyLogin(autoImportSecret, importerUrl, token);
	}
}
