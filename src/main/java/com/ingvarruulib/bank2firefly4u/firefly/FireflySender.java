package com.ingvarruulib.bank2firefly4u.firefly;

import com.ingvarruulib.bank2firefly4u.ApiHandler;
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

import static com.ingvarruulib.bank2firefly4u.MultiPartBody.createMultipartData;

public class FireflySender implements ApiHandler {
	private static final Logger LOGGER = Logger.getLogger(FireflySender.class.getName());
	private static final String AUTOUPLOAD = "/autoupload?secret=";
	FireflyLogin login;

	public FireflySender(FireflyLogin fireflyLogin) {
		this.login = fireflyLogin;
		if (!this.validateLogin()) {
			System.err.println("Bad .env");
			System.exit(1);
		}
	}

	public FireflySender() {
		this.login = getLogin();
		if (!this.validateLogin()) {
			System.err.println("Bad .env");
			System.exit(1);
		}
	}

	public boolean postStatementCsv(File statement) {
		URI importer;
		Path config = Path.of("config.json");

		try {
			// params set here
			importer = new URI(this.login.importerUrl() + AUTOUPLOAD + this.login.autoImportSecret());
		} catch (URISyntaxException ex) {
			throw new RuntimeException(ex);
		}

		var mp = createMultipartData(config, statement.toPath());

		try (HttpClient client = HttpClient.newHttpClient()) {
			HttpRequest req = HttpRequest.newBuilder()
					.uri(importer)
					.header("Accept", "application/json")
					.header("Authorization", "Bearer " + this.login.token())
					.header("Content-Type", mp.contentType())
					.POST(mp.body())
					.build();

			var response = client.send(req, HttpResponse.BodyHandlers.ofString());
			if (response.statusCode() != 200) {
				LOGGER.severe("Response from Firefly importer: " + response.body());
				return false;
			}

			System.out.println(response.body());
		} catch (IOException | InterruptedException e) {
			throw new RuntimeException(e);
		}

		return true;
	}

	@NotNull
	@Override
	// return the auto-import secret, the base url for the importer, and the personal access token as a record
	public FireflyLogin getLogin() {
		var dotEnv =  new DotEnv();
		String autoImportSecret = dotEnv.getEnv("AUTO_IMPORT_SECRET");
		String importerUrl = dotEnv.getEnv("DATA_IMPORTER_BASE");
		String token = dotEnv.getEnv("PERSONAL_ACCESS_TOKEN");

		if (autoImportSecret == null || importerUrl == null || token == null) {
			throw new MissingEnvException("AUTO_IMPORT_SECRET, DATA_IMPORTER_BASE and PERSONAL_ACCESS_TOKEN must be set in .env");
		}
		return new FireflyLogin(autoImportSecret, importerUrl, token);
	}

	@Override
	public boolean validateLogin() {
		try {
			var _ = new URI(this.login.importerUrl() + AUTOUPLOAD + this.login.autoImportSecret());
		} catch (URISyntaxException ex) {
			LOGGER.severe("Malformed URL in .env: " + this.login.importerUrl() + ": " + ex.getMessage());
			if (this.login.importerUrl().startsWith("\"") && this.login.importerUrl().endsWith("\"")) {
				LOGGER.severe("Don't wrap the base url in quotes!");
			}
			return false;
		}
		return ((this.login != null) && (this.login.token() != null) && (this.login.importerUrl() != null) && (this.login.autoImportSecret() != null));
	}

}
