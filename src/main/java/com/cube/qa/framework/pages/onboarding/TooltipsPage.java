package com.cube.qa.framework.pages.onboarding;

import com.cube.qa.framework.pages.BasePage;
import io.appium.java_client.AppiumDriver;
import org.openqa.selenium.By;

import java.util.List;

/**
 * The onboarding tooltip overlays shown on first launch of the Home screen.
 * Each tooltip spotlights a bottom tab (Learn, Give Care, Training, Prepare)
 * and displays a description plus a GOT IT dismiss button.
 */
public class TooltipsPage extends BasePage {

    private List<By> gotItButtonLocators;

    public TooltipsPage(AppiumDriver driver, String platform) {
        super(driver);

        if (platform.equalsIgnoreCase("ios")) {
            gotItButtonLocators = List.of(
                    By.name("GOT IT"),
                    By.name("Got It"),
                    By.xpath("//XCUIElementTypeButton[@label='GOT IT']"),
                    By.xpath("//XCUIElementTypeButton[@label='Got It']"),
                    By.xpath("//*[@label='GOT IT']")
            );
        } else {
            gotItButtonLocators = List.of(
                    By.xpath("//*[@text='GOT IT']"),
                    By.xpath("//*[@text='Got It']"),
                    By.xpath("//*[@text='GOT IT!']")
            );
        }
    }

    public boolean isGotItButtonVisible() {
        return isVisible(gotItButtonLocators);
    }

    public boolean isGotItButtonPresent() {
        return isPresent(gotItButtonLocators);
    }

    public boolean isGotItButtonAbsent() {
        return isInvisible(gotItButtonLocators);
    }

    public void tapGotIt() {
        tap(gotItButtonLocators);
    }
}
