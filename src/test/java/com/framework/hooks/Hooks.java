package com.framework.hooks;

import com.framework.driver.PlaywrightFactory;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import io.qameta.allure.Allure;

import java.io.ByteArrayInputStream;

/**
 * Cucumber lifecycle hooks. Runs before/after every scenario to spin up and
 * tear down the browser, and attaches a screenshot to the Allure report on
 * failure.
 */
public class Hooks {

    @Before(order = 0)
    public void setUp(Scenario scenario) {
        System.out.println("Starting scenario: " + scenario.getName());
        PlaywrightFactory.initBrowser();
    }

    @After(order = 1)
    public void tearDown(Scenario scenario) {
        try {
            if (scenario.isFailed()) {
                byte[] screenshot = PlaywrightFactory.takeScreenshot();
                if (screenshot.length > 0) {
                    // Attach to both the Cucumber and Allure reports.
                    scenario.attach(screenshot, "image/png", "failure-screenshot");
                    Allure.addAttachment("failure-screenshot", "image/png",
                            new ByteArrayInputStream(screenshot), "png");
                }
            }
        } finally {
            PlaywrightFactory.closeBrowser(scenario.getName());
        }
    }
}
