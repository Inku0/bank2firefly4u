package com.ingvarruulib.bank2firefly4u.firefly;

import com.ingvarruulib.bank2firefly4u.ApiHandler;
import com.ingvarruulib.bank2firefly4u.dotenv.DotEnv;
import com.ingvarruulib.bank2firefly4u.dotenv.MissingEnvException;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

import static com.ingvarruulib.bank2firefly4u.Multipart.MultipartBody.createMultipartData;

public class FireflySender implements ApiHandler {
	private static final Logger LOGGER = Logger.getLogger(FireflySender.class.getName());
	private static final String AUTO_UPLOAD = "/autoupload?secret=";
	private final HttpClient client;
	private final FireflyLogin login;

	public FireflySender(FireflyLogin fireflyLogin) {
		this.login = fireflyLogin;
		if (!this.validateLogin()) {
			throw new MissingEnvException("Bad .env");
		}
		this.client = HttpClient.newHttpClient();
	}

	public FireflySender() {
		this.login = getLogin();
		if (!this.validateLogin()) {
			throw new MissingEnvException("Bad .env");
		}
		this.client = HttpClient.newHttpClient();
	}

	public boolean postStatementCsv(File statement) {
		URI importer;
		Path config = Path.of("config.json");

		try {
			// params set here
			importer = new URI(this.login.importerUrl() + AUTO_UPLOAD + this.login.autoImportSecret());
		} catch (URISyntaxException ex) {
			throw new RuntimeException(ex);
		}

		AtomicBoolean spinning = new AtomicBoolean(true);
		Thread spinner = new Thread(() -> {
			String[] frames = {"⠋","⠙","⠹","⠸","⠼","⠴","⠦","⠧","⠇","⠏"};
			int i = 0;
			while (spinning.get() && !Thread.currentThread().isInterrupted()) {
				System.out.printf("\rUploading... %s", frames[i++ % frames.length]);
				System.out.flush();
				try {
					Thread.sleep(120);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					break;
				}
			}
		}, "firefly-upload-spinner");

		var mp = createMultipartData(config, statement.toPath());
		HttpResponse<String> response;

		try {
			HttpRequest req = HttpRequest.newBuilder()
					.uri(importer)
					.header("Accept", "application/json")
					.header("Authorization", "Bearer " + this.login.token())
					.header("Content-Type", mp.contentType())
					.POST(mp.body())
					.build();

			spinner.start();

			response = this.client.send(req, HttpResponse.BodyHandlers.ofString());
		} catch (IOException | InterruptedException e) {
			throw new RuntimeException(e);
		} finally {
			spinning.set(false);
			spinner.interrupt();
			try {
				spinner.join(500);
			} catch (InterruptedException ignored) {
				Thread.currentThread().interrupt();
			}
		}

		if (response.statusCode() != 200) {
			LOGGER.severe("\n" + "Bad status code (" + response.statusCode() + ")! Response from Firefly importer: " + response.body());
			System.out.print("\r");
			System.out.println("Uploading... failed!");

			return false;
		}

		System.out.println("\n" + response.body());
		System.out.print("\r");
		System.out.println("Uploading... done");
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
			var _ = new URI(this.login.importerUrl() + AUTO_UPLOAD + this.login.autoImportSecret());
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
