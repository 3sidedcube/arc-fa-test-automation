package com.cube.qa.framework.pages.streaks;

import com.cube.qa.framework.pages.BasePage;
import io.appium.java_client.AppiumDriver;
import org.openqa.selenium.By;

import java.util.List;

/**
 * Streaks modal — a bottom sheet that fires the first time a user completes
 * a lesson on a given day. Returns to the Topic page once dismissed.
 *
 * <p>Lessons tests treat this purely as an interrupt and call
 * {@link #closeIfPresent()} after {@code LessonCompletePage.tapBackToTopic()}.
 * Future Streaks tests will assert against the modal's contents directly.
 */
public class StreaksPage extends BasePage {

    private final List<By> modalIndicatorLocators;
    private final List<By> closeButtonLocators;
    private final List<By> continueButtonLocators;

    public StreaksPage(AppiumDriver driver, String platform) {
        super(driver);

        if (platform.equalsIgnoreCase("ios")) {
            modalIndicatorLocators = List.of(
                    By.xpath("//XCUIElementTypeStaticText[contains(@name,'Streak')]")
            );
            closeButtonLocators = List.of(
                    By.name("CloseButton"),
                    By.xpath("//XCUIElementTypeButton[@name='CloseButton' or @name='Close']")
            );
            continueButtonLocators = List.of(
                    By.name("CONTINUE")
            );
        } else {
            modalIndicatorLocators = List.of(
                    By.xpath("//*[@resource-id='com.cube.arc.fa:id/text_title' " +
                            "and contains(@text,'Streak')]")
            );
            closeButtonLocators = List.of(
                    By.id("com.cube.arc.fa:id/close_button")
            );
            continueButtonLocators = List.of(
                    By.id("com.cube.arc.fa:id/button_confirm"),
                    By.xpath("//*[@text='CONTINUE']")
            );
        }
    }

    public boolean isPresent() {
        return isPresent(modalIndicatorLocators);
    }

    /**
     * Dismiss the streak modal if it's currently up. No-op when absent — the
     * modal only fires on the first lesson completion of the day, so on a
     * fresh-install run it appears once and never again.
     */
    public void closeIfPresent() {
        if (!isPresent()) return;
        // Prefer the primary CTA over the close icon — both dismiss, but the
        // CTA is the supported "user accepts" path.
        if (isPresent(continueButtonLocators)) {
            tap(continueButtonLocators);
        } else if (isPresent(closeButtonLocators)) {
            tap(closeButtonLocators);
        }
        waitForSeconds(1);
    }
}
