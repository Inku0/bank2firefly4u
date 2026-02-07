package com.ingvarruulib.bank2firefly4u.Multipart;

import java.io.FileNotFoundException;
import java.net.http.HttpRequest.BodyPublisher;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.net.http.HttpRequest.BodyPublishers.*;
import static java.net.http.HttpRequest.BodyPublishers.ofString;

public class MultipartBody {
	private static final Logger LOGGER = Logger.getLogger(MultipartBody.class.getName());
	private static final String LINE_BREAK = "\r\n";
	private final String boundary;
	private BodyPublisher body;

	public MultipartBody() {
		this.boundary = "JAVA_BOUNDARY" + UUID.randomUUID();
		LOGGER.setLevel(Level.FINEST);
	}

	public String boundary() {
		return boundary;
	}

	public String contentType() {
		return "multipart/form-data; boundary=" + boundary;
	}

	public void add(String name, Path file) {
		try {
			BodyPublisher next = concat(
					partHeader(this.boundary, name, file, detectContentType(name)),
					ofFile(file),
					ofString(LINE_BREAK)
			);

			body = (body == null) ? next : concat(body, next);
		} catch (FileNotFoundException ex) {
			throw new RuntimeException(ex);
		}
	}

	public BodyPublisher finish() {
		if (body == null) {
			body = noBody();
		}
		body = concat(
				body,
				ofString(LINE_BREAK + "--" + boundary + "--" + LINE_BREAK)
		);

		return body;
	}

	public static Multipart createMultipartData(Path... files) {
		var multipart = new MultipartBody();

		for (Path file : files) {
			if (!file.toFile().canRead()) {
				throw new RuntimeException("Cannot read file: " + file);
			}
			if (file.toString().contains("statement")) {
				multipart.add("importable", file);
				LOGGER.fine("added importable field");
			} else if (file.toString().contains("config")) {
				multipart.add("json", file);
				LOGGER.fine("added json field");
			} else {
				LOGGER.severe("are you sure " + file + " is correct for Firefly? Can't deduce name.");
				multipart.add(String.valueOf(ThreadLocalRandom.current().nextInt()), file);
			}
		}

		return new Multipart(multipart.contentType(), multipart.finish());
	}

	private static BodyPublisher partHeader(String boundary, String name, Path file, String type) {
		return ofString(
				"--" + boundary + "\r\n" +
						"Content-Disposition: form-data; name=\"" + name + "\"; filename=\"" + file.getFileName() + "\"" + LINE_BREAK +
						"Content-Type: " + type + LINE_BREAK + LINE_BREAK
		);
	}

	private static String detectContentType(String fieldName) {
		if ("json".equals(fieldName)) {
			return "application/json";
		}
		return "text/csv";
	}
}
