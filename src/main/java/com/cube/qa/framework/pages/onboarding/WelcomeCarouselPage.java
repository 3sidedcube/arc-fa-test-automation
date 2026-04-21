package com.cube.qa.framework.pages.onboarding;

import com.cube.qa.framework.pages.BasePage;
import io.appium.java_client.AppiumDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.util.ArrayList;
import java.util.List;

/**
 * The "Welcome / What's New" card shown after the user enters the app
 * (Sign In, Continue as Guest, or — per spec — NOT shown via First Aid Content).
 * The card lists feature bullets and exposes a CONTINUE CTA.
 */
public class WelcomeCarouselPage extends BasePage {

    private final String platform;

    private List<By> continueButtonLocators;
    private List<By> headlineLocators;
    private List<By> anyTextLocators;

    public WelcomeCarouselPage(AppiumDriver driver, String platform) {
        super(driver);
        this.platform = platform;

        if (platform.equalsIgnoreCase("ios")) {
            continueButtonLocators = List.of(
                    By.name("CONTINUE")
            );
            headlineLocators = List.of(
                    By.name("Welcome to the First Aid App"),
                    By.xpath("//XCUIElementTypeStaticText[contains(@label, 'Welcome')]")
            );
            anyTextLocators = List.of(
                    By.xpath("//XCUIElementTypeStaticText")
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
            anyTextLocators = List.of(
                    By.xpath("//android.widget.TextView")
            );
        }
    }

    public boolean isHeadlineVisible() {
        return isVisible(headlineLocators);
    }

    // Non-throwing — safe to use when absence is a valid outcome (TC8688).
    public boolean isHeadlinePresent() {
        for (By locator : headlineLocators) {
            if (!driver.findElements(locator).isEmpty()) return true;
        }
        return false;
    }

    public boolean isContinueButtonVisible() {
        return isVisible(continueButtonLocators);
    }

    public void tapContinue() {
        tap(continueButtonLocators);
    }

    // Returns every non-empty text string currently on screen — used to discover
    // welcome-card bullet copy before pinning strict assertions.
    public List<String> visibleTexts() {
        List<String> texts = new ArrayList<>();
        for (By locator : anyTextLocators) {
            for (WebElement el : driver.findElements(locator)) {
                try {
                    String value = platform.equalsIgnoreCase("ios")
                            ? el.getAttribute("label")
                            : el.getText();
                    if (value != null && !value.isBlank()) {
                        texts.add(value.trim());
                    }
                } catch (Exception ignored) {}
            }
        }
        return texts;
    }

    // True if any on-screen text contains the supplied substring (case-insensitive).
    public boolean isContentItemVisible(String substring) {
        String needle = substring.toLowerCase();
        for (String text : visibleTexts()) {
            if (text.toLowerCase().contains(needle)) return true;
        }
        return false;
    }
}
