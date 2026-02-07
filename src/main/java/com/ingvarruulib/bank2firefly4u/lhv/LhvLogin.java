package com.ingvarruulib.bank2firefly4u.lhv;

import java.util.Objects;

public record LhvLogin(String kasutajaTunnus, String isikukood) {
	public LhvLogin {
		Objects.requireNonNull(kasutajaTunnus);
		Objects.requireNonNull(isikukood);
	}
}
