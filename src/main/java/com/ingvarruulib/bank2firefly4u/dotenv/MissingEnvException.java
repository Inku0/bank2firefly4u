package com.ingvarruulib.bank2firefly4u.dotenv;

public class MissingEnvException extends RuntimeException {
	public MissingEnvException(String message) {
		super(message);
	}
}
