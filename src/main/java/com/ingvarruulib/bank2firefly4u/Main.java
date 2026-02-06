package com.ingvarruulib.bank2firefly4u;

import com.ingvarruulib.bank2firefly4u.dotenv.DotEnv;
import com.ingvarruulib.bank2firefly4u.dotenv.MissingEnvException;
import com.ingvarruulib.bank2firefly4u.firefly.FireflySender;
import com.ingvarruulib.bank2firefly4u.lhv.LhvGetter;
import com.ingvarruulib.bank2firefly4u.lhv.LhvLogin;
import com.microsoft.playwright.*;

import java.io.File;
import java.nio.file.Paths;

public class Main {
	public static void main(String[] args) {
		// TODO: add .env validation to the start
		var lhvGetter = new LhvGetter();
		File statement;

		try (Playwright playwright = Playwright.create()) {
			try (Browser browser = playwright.firefox().launch(new BrowserType.LaunchOptions().setHeadless(false))) {
				var browserContext = browser.newContext();

				Page loginPage = lhvGetter.login(browserContext);
				if (loginPage == null) {
					System.err.println("Failed to login");
					System.exit(1);
				}

				statement = lhvGetter.downloadStatementCsv(loginPage);
				if (statement == null) {
					System.err.println("Failed to download statement");
					System.exit(1);
				}
			}
		}

		var fireflySender = new FireflySender();
		fireflySender.postStatementCsv(statement);
	}
}
