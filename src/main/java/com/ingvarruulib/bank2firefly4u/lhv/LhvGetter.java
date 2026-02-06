package com.ingvarruulib.bank2firefly4u.lhv;

import com.ingvarruulib.bank2firefly4u.dotenv.DotEnv;
import com.ingvarruulib.bank2firefly4u.dotenv.MissingEnvException;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.AriaRole;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.opentest4j.AssertionFailedError;

import java.io.File;
import java.nio.file.Path;
import java.util.logging.Logger;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public class LhvGetter {
	private static final Logger LOGGER = Logger.getLogger(LhvGetter.class.getName());
	private static final String LHV_URL = "https://lhv.ee";
	LhvLogin login;

	public LhvGetter() {
		this.login = getLogin();
	}

	public LhvGetter(LhvLogin login) {
		this.login = login;
	}

	@NotNull
	public static LhvLogin getLogin() {
		var dotEnv =  new DotEnv();
		String kasutajaTunnus = dotEnv.getEnv("KASUTAJATUNNUS");
		String isikukood = dotEnv.getEnv("ISIKUKOOD");
		if (kasutajaTunnus == null || isikukood == null) {
			throw new MissingEnvException("KASUTAJATUNNUS and ISIKUKOOD must be set in .env");
		}
		return new LhvLogin(kasutajaTunnus, isikukood);
	}

	@Nullable
	public Page login(BrowserContext ctx) {
		var page = ctx.newPage();
		page.navigate(LHV_URL);
		// other languages?
		var cookieButton = page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Keeldun kõigist"));
		if (cookieButton.count() > 0) {
			cookieButton.click();
		}
		page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Sisene")).click();
		page.getByLabel("Kasutajanimi").click();
		page.getByLabel("Kasutajanimi").fill(login.kasutajaTunnus());
		page.getByLabel("Isikukood").click();
		page.getByLabel("Isikukood").fill(login.isikukood());
		page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Sisene")).click();

		var logout = page.locator("#header-logout");
		logout.waitFor(new Locator.WaitForOptions().setTimeout(60000));

		try {
			assertThat(page.locator("#header-logout")).containsText("Välju");
			LOGGER.fine("Logged in successfully");
			return page;
		} catch (AssertionFailedError ex) {
			LOGGER.severe("Failed to login (timed out waiting for log-out button to appear)");
			return null;
		}
	}

	@Nullable
	public File downloadStatementCsv(Page page) {
		Path statement = Path.of("statement.csv");

		// get to statement view
		page.navigate("https://www.lhv.ee/ibank/cf/portfolio/view");
		page.locator("li")
				.filter(new Locator.FilterOptions()
				.setHasText("Varad ja kohustused"))
				.locator("[id=\"menu\\.accountStatement\"]")
				.click();

		// open saving menu
		page.locator("iframe[title=\"Content\"]")
				.contentFrame()
				.getByRole(AriaRole.BUTTON, new FrameLocator.GetByRoleOptions()
				.setName("Salvesta…"))
				.click();

		Download download = page.waitForDownload(() -> page.locator(
				"iframe[title=\"Content\"]").contentFrame().getByRole(AriaRole.LINK,
				new FrameLocator.GetByRoleOptions().setName("CSV")).click()
		);

		download.saveAs(statement);

		if (!statement.toFile().canRead()) {
			return null;
		}

		return statement.toFile();
	}
}
