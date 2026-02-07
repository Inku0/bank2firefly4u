package com.ingvarruulib.bank2firefly4u;

import com.ingvarruulib.bank2firefly4u.dotenv.DotEnv;
import com.ingvarruulib.bank2firefly4u.dotenv.MissingEnvException;
import com.ingvarruulib.bank2firefly4u.firefly.FireflySender;
import com.ingvarruulib.bank2firefly4u.lhv.LhvGetter;
import com.ingvarruulib.bank2firefly4u.lhv.LhvLogin;
import com.microsoft.playwright.*;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Main {
	static void postStatement(File statement, FireflySender ffs) {
		if (statement.canRead()) {
			if (!ffs.postStatementCsv(statement)) {
				System.err.println("Failed to post statement");
				System.exit(1);
			}
			System.exit(0);
		}
	}

	public static void main(String[] args) {
		File statement = Path.of("statement.csv").toFile();

		var lhvGetter = new LhvGetter();
		var fireflySender = new FireflySender();

		postStatement(statement, fireflySender);

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

		postStatement(statement, fireflySender);
	}
}
