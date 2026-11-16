package com.framework.driver;

import com.framework.config.ConfigManager;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;

import java.nio.file.Paths;
import java.util.List;

/**
 * Thread-safe factory that creates and tears down Playwright objects.
 *
 * <p>Each test thread gets its own {@link Playwright}, {@link Browser},
 * {@link BrowserContext} and {@link Page}, so scenarios can run in parallel
 * without sharing state. Instances are held in {@link ThreadLocal} slots and
 * accessed through the static getters.</p>
 */
public final class PlaywrightFactory {

    private static final ThreadLocal<Playwright> PLAYWRIGHT = new ThreadLocal<>();
    private static final ThreadLocal<Browser> BROWSER = new ThreadLocal<>();
    private static final ThreadLocal<BrowserContext> CONTEXT = new ThreadLocal<>();
    private static final ThreadLocal<Page> PAGE = new ThreadLocal<>();

    private PlaywrightFactory() {
    }

    /**
     * Initializes Playwright for the current thread and returns a ready-to-use
     * {@link Page}. Reads browser, headless mode, viewport and timeout from
     * configuration.
     */
    public static Page initBrowser() {
        Playwright playwright = Playwright.create();
        PLAYWRIGHT.set(playwright);

        String browserName = ConfigManager.get("browser", "chromium").toLowerCase();
        boolean headless = ConfigManager.getBoolean("headless", true);
        int slowMo = ConfigManager.getInt("slowMo", 0);

        BrowserType.LaunchOptions launchOptions = new BrowserType.LaunchOptions()
                .setHeadless(headless)
                .setSlowMo(slowMo);

        BrowserType browserType = switch (browserName) {
            case "firefox" -> playwright.firefox();
            case "webkit", "safari" -> playwright.webkit();
            case "chrome" -> {
                launchOptions.setChannel("chrome");
                yield playwright.chromium();
            }
            case "edge", "msedge" -> {
                launchOptions.setChannel("msedge");
                yield playwright.chromium();
            }
            default -> playwright.chromium();
        };

        Browser browser = browserType.launch(launchOptions);
        BROWSER.set(browser);

        Browser.NewContextOptions contextOptions = new Browser.NewContextOptions()
                .setViewportSize(
                        ConfigManager.getInt("viewport.width", 1920),
                        ConfigManager.getInt("viewport.height", 1080));

        if (ConfigManager.getBoolean("recordVideo", false)) {
            contextOptions.setRecordVideoDir(Paths.get("target/videos"));
        }

        BrowserContext context = browser.newContext(contextOptions);

        if (ConfigManager.getBoolean("trace", false)) {
            context.tracing().start(new com.microsoft.playwright.Tracing.StartOptions()
                    .setScreenshots(true).setSnapshots(true).setSources(true));
        }

        int defaultTimeout = ConfigManager.getInt("timeout", 30000);
        context.setDefaultTimeout(defaultTimeout);
        CONTEXT.set(context);

        Page page = context.newPage();
        PAGE.set(page);
        return page;
    }

    public static Page getPage() {
        return PAGE.get();
    }

    public static BrowserContext getContext() {
        return CONTEXT.get();
    }

    /** Captures a full-page PNG screenshot as a byte array for report embedding. */
    public static byte[] takeScreenshot() {
        Page page = PAGE.get();
        if (page == null) {
            return new byte[0];
        }
        return page.screenshot(new Page.ScreenshotOptions().setFullPage(true));
    }

    /**
     * Closes all Playwright resources for the current thread and clears the
     * {@link ThreadLocal} slots. Optionally exports a trace zip when tracing
     * is enabled.
     */
    public static void closeBrowser(String scenarioName) {
        try {
            BrowserContext context = CONTEXT.get();
            if (context != null && ConfigManager.getBoolean("trace", false)) {
                String safeName = scenarioName == null ? "trace"
                        : scenarioName.replaceAll("[^a-zA-Z0-9-_]", "_");
                context.tracing().stop(new com.microsoft.playwright.Tracing.StopOptions()
                        .setPath(Paths.get("target/traces/" + safeName + ".zip")));
            }
        } finally {
            closeQuietly(CONTEXT.get(), List.of(
                    () -> { if (CONTEXT.get() != null) CONTEXT.get().close(); },
                    () -> { if (BROWSER.get() != null) BROWSER.get().close(); },
                    () -> { if (PLAYWRIGHT.get() != null) PLAYWRIGHT.get().close(); }));
            PAGE.remove();
            CONTEXT.remove();
            BROWSER.remove();
            PLAYWRIGHT.remove();
        }
    }

    private static void closeQuietly(Object ignored, List<Runnable> closers) {
        for (Runnable closer : closers) {
            try {
                closer.run();
            } catch (RuntimeException e) {
                // Best-effort cleanup; log and continue closing remaining resources.
                System.err.println("Error during Playwright teardown: " + e.getMessage());
            }
        }
    }
}
