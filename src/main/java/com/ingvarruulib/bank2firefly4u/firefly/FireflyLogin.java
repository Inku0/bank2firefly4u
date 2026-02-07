package com.ingvarruulib.bank2firefly4u.firefly;

import java.util.Objects;

public record FireflyLogin(String autoImportSecret, String importerUrl, String token) {
	public FireflyLogin {
		Objects.requireNonNull(autoImportSecret);
		Objects.requireNonNull(importerUrl);
		Objects.requireNonNull(token);
	}
}
