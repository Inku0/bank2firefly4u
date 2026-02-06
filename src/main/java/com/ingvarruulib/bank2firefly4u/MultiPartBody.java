package com.ingvarruulib.bank2firefly4u;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublisher;
import java.nio.file.Path;
import java.util.UUID;

import static java.net.http.HttpRequest.BodyPublishers.*;
import static java.net.http.HttpRequest.BodyPublishers.ofString;

public class MultiPartBody {
	private static final String LINE_BREAK = "\r\n";
	private final String boundary;
	private BodyPublisher body;

	public MultiPartBody() {
		this.boundary = "JAVA_BOUNDARY" + UUID.randomUUID().toString();
	}

	public void add(String name, Path file) {
		try {
			body = concat(
				body,
				partHeader(this.boundary, name, file, "text/csv"),

				ofFile(file),
				ofString("\r\n")
				);
		} catch (FileNotFoundException ex) {
			throw new RuntimeException(ex);
		}
	}

	public BodyPublisher finish() {
		body = concat(
				body,
				ofString("\r\n--" + boundary + "--\r\n")
		);

		return body;
	}

	private static BodyPublisher partHeader(String boundary, String name, Path file, String type) {
		return ofString(
				"--" + boundary + "\r\n" +
						"Content-Disposition: form-data; name=\"" + name + "\"; filename=\"" + file.getFileName() + "\"\r\n" +
						"Content-Type: " + type + "\r\n\r\n"
		);
	}
}
