package com.cube.qa.framework.pages.onboarding;

import com.cube.qa.framework.pages.BasePage;
import io.appium.java_client.AppiumDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.util.ArrayList;
import java.util.List;

/**
 * The onboarding tooltip overlays shown on first launch of the Home screen.
 * Each tooltip spotlights a bottom tab (Learn, Give Care, Training, Prepare)
 * and displays a description plus a GOT IT dismiss button.
 */
public class TooltipsPage extends BasePage {

    private final String platform;

    private List<By> gotItButtonLocators;
    // Broad matcher for any text node currently rendered — used to capture the
    // tooltip description so tests can assert on its content.
    private List<By> anyTextLocators;

    public TooltipsPage(AppiumDriver driver, String platform) {
        super(driver);
        this.platform = platform;

        if (platform.equalsIgnoreCase("ios")) {
            gotItButtonLocators = List.of(
                    By.name("GOT IT"),
                    By.name("Got It"),
                    By.xpath("//XCUIElementTypeButton[@label='GOT IT']"),
                    By.xpath("//XCUIElementTypeButton[@label='Got It']"),
                    By.xpath("//*[@label='GOT IT']")
            );
            anyTextLocators = List.of(
                    By.xpath("//XCUIElementTypeStaticText")
            );
        } else {
            gotItButtonLocators = List.of(
                    By.xpath("//*[@text='GOT IT']"),
                    By.xpath("//*[@text='Got It']"),
                    By.xpath("//*[@text='GOT IT!']")
            );
            anyTextLocators = List.of(
                    By.xpath("//android.widget.TextView")
            );
        }
    }

    public boolean isGotItButtonVisible() {
        return isVisible(gotItButtonLocators);
    }

    // Non-throwing presence check — safe for loop conditions where absence is
    // the expected terminal state (tooltip chain fully dismissed).
    public boolean isGotItButtonPresent() {
        for (By locator : gotItButtonLocators) {
            if (!driver.findElements(locator).isEmpty()) return true;
        }
        return false;
    }

    public boolean isGotItButtonAbsent() {
        return isInvisible(gotItButtonLocators);
    }

    public void tapGotIt() {
        tap(gotItButtonLocators);
    }

    /**
     * Returns every non-empty text string currently on screen. Used to capture
     * the description of the currently-displayed tooltip — the visible text
     * minus the tab labels and the GOT IT button is the tooltip copy.
     */
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

    /**
     * True if any on-screen text contains the supplied substring (case-insensitive).
     * Lets tests assert on tooltip descriptions without exact-string coupling.
     */
    public boolean isDescriptionVisible(String substring) {
        String needle = substring.toLowerCase();
        for (String text : visibleTexts()) {
            if (text.toLowerCase().contains(needle)) return true;
        }
        return false;
    }
}
