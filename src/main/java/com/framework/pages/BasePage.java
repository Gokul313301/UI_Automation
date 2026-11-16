package com.framework.pages;

import com.framework.config.ConfigManager;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.LoadState;

/**
 * Base class for all page objects. Wraps the thread's {@link Page} and offers
 * common, reusable interactions so page objects stay concise.
 */
public abstract class BasePage {

    protected final Page page;

    protected BasePage(Page page) {
        this.page = page;
    }

    /** Navigates to a path relative to the configured base URL, or an absolute URL. */
    protected void navigate(String pathOrUrl) {
        String target = pathOrUrl.startsWith("http")
                ? pathOrUrl
                : ConfigManager.get("baseUrl") + pathOrUrl;
        page.navigate(target);
        page.waitForLoadState(LoadState.DOMCONTENTLOADED);
    }

    protected void click(String selector) {
        page.locator(selector).click();
    }

    protected void type(String selector, String text) {
        page.locator(selector).fill(text);
    }

    protected String textOf(String selector) {
        return page.locator(selector).innerText();
    }

    protected boolean isVisible(String selector) {
        return page.locator(selector).isVisible();
    }

    protected void waitForVisible(String selector) {
        page.locator(selector).waitFor();
    }

    protected Locator locator(String selector) {
        return page.locator(selector);
    }

    public String getTitle() {
        return page.title();
    }

    public String getCurrentUrl() {
        return page.url();
    }
}
