package com.framework.pages;

import com.microsoft.playwright.Page;

/**
 * Page object for the SauceDemo login screen (https://www.saucedemo.com).
 */
public class LoginPage extends BasePage {

    private static final String USERNAME_INPUT = "#user-name";
    private static final String PASSWORD_INPUT = "#password";
    private static final String LOGIN_BUTTON = "#login-button";
    private static final String ERROR_MESSAGE = "[data-test='error']";

    public LoginPage(Page page) {
        super(page);
    }

    public LoginPage open() {
        navigate("/");
        waitForVisible(LOGIN_BUTTON);
        return this;
    }

    public void login(String username, String password) {
        type(USERNAME_INPUT, username);
        type(PASSWORD_INPUT, password);
        click(LOGIN_BUTTON);
    }

    public boolean isErrorDisplayed() {
        return isVisible(ERROR_MESSAGE);
    }

    public String getErrorMessage() {
        waitForVisible(ERROR_MESSAGE);
        return textOf(ERROR_MESSAGE);
    }
}
