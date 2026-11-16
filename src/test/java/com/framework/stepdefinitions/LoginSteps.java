package com.framework.stepdefinitions;

import com.framework.config.ConfigManager;
import com.framework.driver.PlaywrightFactory;
import com.framework.pages.InventoryPage;
import com.framework.pages.LoginPage;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Step definitions for the login feature. Page objects are created against the
 * current thread's Playwright {@link com.microsoft.playwright.Page}.
 */
public class LoginSteps {

    private final LoginPage loginPage = new LoginPage(PlaywrightFactory.getPage());
    private final InventoryPage inventoryPage = new InventoryPage(PlaywrightFactory.getPage());

    @Given("the user is on the login page")
    public void the_user_is_on_the_login_page() {
        loginPage.open();
    }

    @When("the user logs in with valid credentials")
    public void the_user_logs_in_with_valid_credentials() {
        loginPage.login(ConfigManager.get("username"), ConfigManager.get("password"));
    }

    @When("the user logs in with username {string} and password {string}")
    public void the_user_logs_in_with(String username, String password) {
        loginPage.login(username, password);
    }

    @Then("the inventory page is displayed")
    public void the_inventory_page_is_displayed() {
        assertTrue(inventoryPage.isLoaded(), "Inventory container should be visible");
        assertEquals("Products", inventoryPage.getHeaderTitle());
    }

    @Then("the products are listed")
    public void the_products_are_listed() {
        assertTrue(inventoryPage.getProductCount() > 0, "Expected at least one product");
        assertTrue(inventoryPage.isCartVisible(), "Cart should be visible");
    }

    @Then("a login error is shown")
    public void a_login_error_is_shown() {
        assertTrue(loginPage.isErrorDisplayed(), "Error message should be visible");
    }

    @Then("the error message contains {string}")
    public void the_error_message_contains(String expected) {
        assertTrue(loginPage.getErrorMessage().contains(expected),
                "Error message should contain: " + expected);
    }

    @Then("the inventory page is not displayed")
    public void the_inventory_page_is_not_displayed() {
        assertFalse(inventoryPage.isLoaded(), "Inventory should not load on failed login");
    }
}
