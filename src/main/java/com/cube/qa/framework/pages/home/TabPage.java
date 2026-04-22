package com.cube.qa.framework.pages.home;

import com.cube.qa.framework.pages.BasePage;
import io.appium.java_client.AppiumDriver;
import org.openqa.selenium.By;

import java.util.List;
import java.util.Map;

/**
 * Bottom navigation tabs on the Home screen.
 * Shared across onboarding, learn, give-care, training, and prepare test suites.
 */
public class TabPage extends BasePage {

    public enum Tab {
        LEARN("Learn"),
        GIVE_CARE("Give Care"),
        TRAINING("Training"),
        PREPARE("Prepare");

        public final String label;
        Tab(String label) { this.label = label; }
    }

    private final Map<Tab, List<By>> tabLocators;

    public TabPage(AppiumDriver driver, String platform) {
        super(driver);

        if (platform.equalsIgnoreCase("ios")) {
            // Prod iOS uppercases the tab labels (LEARN / GIVE CARE) while
            // staging/simulator builds use title case. Accept either.
            tabLocators = Map.of(
                    Tab.LEARN, List.of(
                            By.name("Learn"),
                            By.name("LEARN"),
                            By.xpath("//XCUIElementTypeButton[@label='Learn' or @label='LEARN']")
                    ),
                    Tab.GIVE_CARE, List.of(
                            By.name("Give Care"),
                            By.name("GIVE CARE"),
                            By.xpath("//XCUIElementTypeButton[@label='Give Care' or @label='GIVE CARE']")
                    ),
                    Tab.TRAINING, List.of(
                            By.name("Training"),
                            By.name("TRAINING"),
                            By.xpath("//XCUIElementTypeButton[@label='Training' or @label='TRAINING']")
                    ),
                    Tab.PREPARE, List.of(
                            By.name("Prepare"),
                            By.name("PREPARE"),
                            By.xpath("//XCUIElementTypeButton[@label='Prepare' or @label='PREPARE']")
                    )
            );
        } else {
            tabLocators = Map.of(
                    Tab.LEARN, List.of(
                            By.xpath("//*[@text='Learn']"),
                            By.xpath("//*[@text='LEARN']")
                    ),
                    Tab.GIVE_CARE, List.of(
                            By.xpath("//*[@text='Give Care']"),
                            By.xpath("//*[@text='GIVE CARE']")
                    ),
                    Tab.TRAINING, List.of(
                            By.xpath("//*[@text='Training']"),
                            By.xpath("//*[@text='TRAINING']")
                    ),
                    Tab.PREPARE, List.of(
                            By.xpath("//*[@text='Prepare']"),
                            By.xpath("//*[@text='PREPARE']")
                    )
            );
        }
    }

    public boolean isTabVisible(Tab tab) {
        return isVisible(tabLocators.get(tab));
    }

    public void tapTab(Tab tab) {
        tap(tabLocators.get(tab));
    }

    // Issue a best-effort tap without waiting for clickable — useful when an
    // overlay intercepts the tap (e.g. tooltip blocking tab navigation).
    public void attemptTapTab(Tab tab) {
        for (By locator : tabLocators.get(tab)) {
            try {
                driver.findElement(locator).click();
                return;
            } catch (Exception ignored) {}
        }
    }
}
