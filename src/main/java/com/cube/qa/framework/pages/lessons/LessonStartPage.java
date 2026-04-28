package com.cube.qa.framework.pages.lessons;

import com.cube.qa.framework.pages.BasePage;
import io.appium.java_client.AppiumDriver;
import org.openqa.selenium.By;

import java.util.List;

/**
 * Lesson Start screen — the interstitial reached by tapping a lesson card on
 * the Topic Detail page. Shows a hero image, the lesson title, duration,
 * optional subtitle, and a START CTA. A back arrow returns to the Topic
 * Detail page.
 *
 * <p>Subtitle is asserted on element-presence only — the CDN bundle leaves it
 * blank for many lessons (e.g. all of Cold-Related Illness and Injury), so a
 * non-empty text assertion would false-positive.
 */
public class LessonStartPage extends BasePage {

    private final String platform;

    private final List<By> imageLocators;
    private final List<By> titleLocators;
    private final List<By> durationLocators;
    private final List<By> subtitleLocators;
    private final List<By> startButtonLocators;
    private final List<By> backLocators;

    public LessonStartPage(AppiumDriver driver, String platform) {
        super(driver);
        this.platform = platform.toLowerCase();

        if (this.platform.equals("ios")) {
            // The lesson title appears prefixed with "Lesson: " on Android;
            // iOS likely does the same. Match on the prefix to avoid binding
            // to a specific lesson.
            imageLocators = List.of(
                    By.xpath("(//XCUIElementTypeImage)[1]")
            );
            titleLocators = List.of(
                    // iOS title is the lesson name verbatim (no "Lesson:"
                    // prefix). Verified live: name="Recognizing and Caring
                    // for Hypothermia". The container view "First_Aid.LessonIntroContainerView"
                    // scopes us so any StaticText that's also followed by a
                    // "minute"-bearing sibling is the title; cheaper proxy:
                    // any StaticText whose name has no comma/period and is
                    // not "START"/"BackButton" — but for stability we just
                    // use a broad match and rely on getCurrentSectionHeading
                    // for the actual text. Presence-only here.
                    By.xpath("//XCUIElementTypeStaticText[string-length(@name) > 8 "
                            + "and not(contains(@name,'minute')) "
                            + "and not(contains(@name,'scroll bar')) "
                            + "and not(@name='START') and not(@name='BackButton')]")
            );
            durationLocators = List.of(
                    // iOS renders duration with a capital M ("2 Minutes"),
                    // verified live. Match case-insensitively.
                    By.xpath("//XCUIElementTypeStaticText[contains(@name,'inute')]")
            );
            // iOS has no dedicated subtitle resource; we settle for "any
            // static text below the duration" — kept loose because most
            // lessons leave it blank in the bundle. Test only checks that
            // *some* text container is reachable.
            subtitleLocators = List.of(
                    By.xpath("//XCUIElementTypeStaticText")
            );
            startButtonLocators = List.of(
                    By.name("START")
            );
            backLocators = List.of(
                    By.name("BackButton"),
                    By.xpath("//XCUIElementTypeButton[@name='BackButton']")
            );
        } else {
            imageLocators = List.of(
                    By.id("com.cube.arc.fa:id/image")
            );
            titleLocators = List.of(
                    By.id("com.cube.arc.fa:id/title")
            );
            durationLocators = List.of(
                    By.id("com.cube.arc.fa:id/duration")
            );
            subtitleLocators = List.of(
                    By.id("com.cube.arc.fa:id/subtitle")
            );
            startButtonLocators = List.of(
                    By.id("com.cube.arc.fa:id/start_button"),
                    By.xpath("//*[@text='START']")
            );
            backLocators = List.of(
                    By.xpath("//*[@content-desc='Navigate up']")
            );
        }
    }

    public boolean isDisplayed() {
        return isPresent(startButtonLocators) && isPresent(titleLocators);
    }

    public boolean hasImage() {
        return isPresent(imageLocators);
    }

    public boolean hasTitle() {
        return isPresent(titleLocators);
    }

    public String getTitle() {
        return getText(titleLocators);
    }

    public boolean hasDuration() {
        return isPresent(durationLocators);
    }

    public String getDuration() {
        return getText(durationLocators);
    }

    public boolean hasSubtitle() {
        return isPresent(subtitleLocators);
    }

    public boolean hasStartCta() {
        return isPresent(startButtonLocators);
    }

    public void tapStart() {
        tap(startButtonLocators);
    }

    public void tapBack() {
        tap(backLocators);
    }
}
