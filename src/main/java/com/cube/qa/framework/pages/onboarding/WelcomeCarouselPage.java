package com.cube.qa.framework.pages.onboarding;

import com.cube.qa.framework.pages.BasePage;
import io.appium.java_client.AppiumDriver;
import org.openqa.selenium.By;

import java.util.List;

/**
 * The "Welcome / What's New" card shown after the user enters the app
 * (Sign In, Continue as Guest, or — per spec — NOT shown via First Aid Content).
 * The card lists feature bullets and exposes a CONTINUE CTA.
 */
public class WelcomeCarouselPage extends BasePage {

    private List<By> continueButtonLocators;
    private List<By> headlineLocators;

    public WelcomeCarouselPage(AppiumDriver driver, String platform) {
        super(driver);

        if (platform.equalsIgnoreCase("ios")) {
            continueButtonLocators = List.of(
                    By.name("CONTINUE")
            );
            headlineLocators = List.of(
                    By.name("Welcome to the First Aid App"),
                    By.xpath("//XCUIElementTypeStaticText[contains(@label, 'Welcome')]")
            );
        } else {
            continueButtonLocators = List.of(
                    By.xpath("//*[@resource-id='com.cube.arc.fa:id/continue_button']//android.widget.Button"),
                    By.xpath("//*[@text='CONTINUE']")
            );
            headlineLocators = List.of(
                    By.id("com.cube.arc.fa:id/welcome_headline"),
                    By.xpath("//*[contains(@text, 'Welcome')]")
            );
        }
    }

    public boolean isHeadlineVisible() {
        return isVisible(headlineLocators);
    }

    public boolean isHeadlinePresent() {
        return isPresent(headlineLocators);
    }

    public boolean isContinueButtonVisible() {
        return isVisible(continueButtonLocators);
    }

    public void tapContinue() {
        tap(continueButtonLocators);
    }
}
