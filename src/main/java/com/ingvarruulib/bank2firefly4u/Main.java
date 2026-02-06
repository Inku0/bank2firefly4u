package com.ingvarruulib.bank2firefly4u;

import com.microsoft.playwright.*;

import java.nio.file.Paths;

public class Main {
	public static void main(String[] args) {
		
		try (Playwright playwright = Playwright.create()) {
			try (Browser browser = playwright.firefox().launch(new BrowserType.LaunchOptions().setHeadless(false))) {
				Page page = browser.newPage();
				page.navigate("https://playwright.dev/");
				page.screenshot(new Page.ScreenshotOptions().setPath(Paths.get("example.png")));
			}
		}
	}
}
