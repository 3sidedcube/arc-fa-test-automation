package com.cube.qa.framework.pages.lessons;

import com.cube.qa.framework.pages.BasePage;
import io.appium.java_client.AppiumDriver;
import org.openqa.selenium.By;

import java.util.List;

/**
 * Lesson Complete bottom sheet — appears after tapping DONE on the final
 * card of a lesson. Title and description are randomised between four
 * variants (en-US strings live in app-strings.json under
 * {@code _LESSON_COMPLETE_TITLE_{1,2,3,ALL}} and
 * {@code _LESSON_COMPLETE_DESCRIPTION_{1,2,3,ALL}}); tests assert the
 * rendered text matches one of the four.
 *
 * <p><b>Streak modal note</b>: tapping {@link #tapBackToTopic()} routes back
 * to the Topic page where the Streak modal subsequently fires (first lesson
 * of the day). This page deliberately does <i>not</i> auto-dismiss the
 * streak — that's the responsibility of the calling test, so future Streaks
 * tests can keep the modal up to inspect it.
 */
public class LessonCompletePage extends BasePage {

    private final List<By> imageLocators;
    private final List<By> titleLocators;
    private final List<By> subtitleLocators;
    private final List<By> nextLessonLocators;
    private final List<By> backToTopicLocators;

    public LessonCompletePage(AppiumDriver driver, String platform) {
        super(driver);

        if (platform.equalsIgnoreCase("ios")) {
            imageLocators = List.of(
                    By.xpath("//XCUIElementTypeImage")
            );
            // Lesson Complete is rendered as an overlay over the still-
            // mounted lesson view. The off-screen lesson cards keep their
            // StaticText elements in the DOM (at negative x), and the
            // naive "(//XCUIElementTypeStaticText)[1]" selector resolves
            // to a hidden one — Selenium then waits 30 s for visibility
            // and times out. Filter to Header-trait StaticTexts only;
            // there's exactly one on the Lesson Complete sheet (the
            // title), and it sits at positive x. Subtitle is the next
            // StaticText sibling immediately below the header — selected
            // via XPath following-sibling axis on the Header. (Verified
            // via target/diagnostic/lessonCompleteTitle-*.xml.)
            titleLocators = List.of(
                    By.xpath("//XCUIElementTypeStaticText[contains(@traits,'Header')]")
            );
            subtitleLocators = List.of(
                    By.xpath("//XCUIElementTypeStaticText[contains(@traits,'Header')]" +
                            "/following::XCUIElementTypeStaticText[1]")
            );
            nextLessonLocators = List.of(
                    By.name("NEXT LESSON")
            );
            backToTopicLocators = List.of(
                    By.name("BACK TO TOPIC PAGE")
            );
        } else {
            imageLocators = List.of(
                    By.id("com.cube.arc.fa:id/container_animation")
            );
            titleLocators = List.of(
                    By.xpath("//*[@resource-id='com.cube.arc.fa:id/design_bottom_sheet']" +
                            "//*[@resource-id='com.cube.arc.fa:id/title']")
            );
            subtitleLocators = List.of(
                    By.xpath("//*[@resource-id='com.cube.arc.fa:id/design_bottom_sheet']" +
                            "//*[@resource-id='com.cube.arc.fa:id/subtitle']")
            );
            nextLessonLocators = List.of(
                    By.id("com.cube.arc.fa:id/next_button"),
                    By.xpath("//*[@text='NEXT LESSON']")
            );
            backToTopicLocators = List.of(
                    By.id("com.cube.arc.fa:id/back_button"),
                    By.xpath("//*[@text='BACK TO TOPIC PAGE']")
            );
        }
    }

    public boolean isDisplayed() {
        return isPresent(titleLocators) && isPresent(backToTopicLocators);
    }

    public boolean hasImage() {
        return isPresent(imageLocators);
    }

    public boolean hasTitle() {
        return isPresent(titleLocators);
    }

    public String getTitle() {
        if (isIos()) return iosOnScreenText(titleLocators, "Lesson Complete title");
        return getText(titleLocators);
    }

    public boolean hasSubtitle() {
        return isPresent(subtitleLocators);
    }

    public String getSubtitle() {
        if (isIos()) return iosOnScreenText(subtitleLocators, "Lesson Complete subtitle");
        return getText(subtitleLocators);
    }

    private boolean isIos() {
        return driver.getCapabilities().getPlatformName().toString().equalsIgnoreCase("ios");
    }

    /**
     * iOS-only: the Lesson Complete sheet is a modal overlay above the
     * still-mounted lesson view. Off-screen lesson cards keep their
     * StaticText elements in the DOM at negative x, and naive position-
     * indexed XPath ((//StaticText)[1]) resolves to a hidden one. This
     * helper iterates all matches and returns the text of the first
     * whose rect center sits inside the on-screen window — purely
     * geometric, no hardcoded device pixels.
     */
    private String iosOnScreenText(List<org.openqa.selenium.By> locators, String labelForError) {
        int windowWidth = Integer.MAX_VALUE;
        int windowHeight = Integer.MAX_VALUE;
        try {
            windowWidth = driver.manage().window().getSize().getWidth();
            windowHeight = driver.manage().window().getSize().getHeight();
        } catch (Exception ignored) {}
        for (org.openqa.selenium.By by : locators) {
            for (org.openqa.selenium.WebElement el : driver.findElements(by)) {
                try {
                    // The lesson view stays mounted underneath the sheet —
                    // its in-card headings have rect centers that ARE on-
                    // screen but iOS marks them visible="false" because
                    // they're occluded by the modal. Filter both rect AND
                    // the iOS @visible attribute so we don't return the
                    // hidden lesson-card heading instead of the sheet's.
                    String visible = el.getAttribute("visible");
                    if (!"true".equalsIgnoreCase(visible)) continue;
                    org.openqa.selenium.Rectangle r = el.getRect();
                    int cx = r.getX() + r.getWidth() / 2;
                    int cy = r.getY() + r.getHeight() / 2;
                    if (cx < 0 || cx >= windowWidth) continue;
                    if (cy < 0 || cy >= windowHeight) continue;
                    String s = el.getAttribute("label");
                    if (s == null || s.isBlank()) s = el.getText();
                    if (s != null) return s.trim();
                } catch (Exception ignored) {}
            }
        }
        throw new RuntimeException(labelForError + " not on screen");
    }

    public boolean hasNextLessonCta() {
        return isPresent(nextLessonLocators);
    }

    public boolean hasBackToTopicCta() {
        return isPresent(backToTopicLocators);
    }

    public void tapNextLesson() {
        tap(nextLessonLocators);
    }

    /**
     * Tap "Back to Topic Page". Does <b>not</b> dismiss the streak modal that
     * appears after returning to the topic — call {@code StreaksPage.closeIfPresent()}
     * explicitly from the test if that's required.
     */
    public void tapBackToTopic() {
        tap(backToTopicLocators);
    }
}
