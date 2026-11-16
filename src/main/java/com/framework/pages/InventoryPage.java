package com.framework.pages;

import com.microsoft.playwright.Page;

/**
 * Page object for the SauceDemo inventory/products page shown after login.
 */
public class InventoryPage extends BasePage {

    private static final String PAGE_TITLE = ".title";
    private static final String INVENTORY_CONTAINER = "#inventory_container";
    private static final String CART_LINK = ".shopping_cart_link";

    public InventoryPage(Page page) {
        super(page);
    }

    public boolean isLoaded() {
        return isVisible(INVENTORY_CONTAINER);
    }

    public String getHeaderTitle() {
        waitForVisible(PAGE_TITLE);
        return textOf(PAGE_TITLE);
    }

    public int getProductCount() {
        return locator(".inventory_item").count();
    }

    public boolean isCartVisible() {
        return isVisible(CART_LINK);
    }
}
