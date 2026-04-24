package com.cube.qa.framework.pages.home.profile;

import com.cube.qa.framework.pages.BasePage;
import io.appium.java_client.AppiumDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.util.ArrayList;
import java.util.List;

/**
 * Personalization card on the Profile tab.
 *
 * <p>Has two states:
 * <ul>
 *   <li><b>Empty state</b> (user has never submitted personalization): shows
 *       the same "Personalize Your Experience" title/subtitle pair that's on
 *       the Learn tab's empty state. No CTA button on Profile — the user
 *       personalizes from Learn first.</li>
 *   <li><b>Populated state</b> (after at least one submission): shows
 *       "Your selected topics" heading, a row of tag chips in
 *       {@code container_tags}, and an "EDIT YOUR PREFERENCES" CTA that
 *       reopens the same personalization modal with existing selections
 *       pre-checked.</li>
 * </ul>
 *
 * <p>The tag chips follow a "first two named, remainder icon-only" layout: up
 * to 2 chips render with both icon and label text; any beyond that render as
 * icon-only (no accessible label). {@link #getVisibleTagNames()} reflects the
 * named subset — anything beyond position 2 will not appear here by design.
 */
public class PersonalisationPage extends BasePage {

    private final String platform;

    private final List<By> emptyStateTitleLocators;
    private final List<By> emptyStateSubtitleLocators;
    private final List<By> editCtaLocators;
    private final List<By> tagContainerLocators;
    /** Named-tag chips (first + second slots). Icon-only chips excluded. */
    private final By namedTagLocator;
    /** Overflow "And N more" pill — present only when > 2 tags selected. */
    private final List<By> moreTagsPillLocators;

    public PersonalisationPage(AppiumDriver driver, String platform) {
        super(driver);
        this.platform = platform.toLowerCase();

        if (this.platform.equals("ios")) {
            // iOS locators — confirmed via Appium Inspector at run time.
            emptyStateTitleLocators = List.of(
                    By.name("Personalize Your Experience")
            );
            emptyStateSubtitleLocators = List.of(
                    By.name("Based on your interests, we’ll personalize your dashboard!")
            );
            editCtaLocators = List.of(
                    By.name("EDIT YOUR PREFERENCES"),
                    By.name("Edit Your Preferences")
            );
            // The tag strip lives under "Your selected topics". iOS shows the
            // named tags as XCUIElementTypeStaticText siblings. Constrain
            // lookup to the section to avoid matching stray static text.
            tagContainerLocators = List.of(
                    By.name("Your selected topics")
            );
            namedTagLocator = By.xpath(
                    "//XCUIElementTypeOther[@name='Your selected topics']" +
                    "//XCUIElementTypeStaticText[@name!='Your selected topics']");
            moreTagsPillLocators = List.of(
                    By.xpath("//*[starts-with(@name, 'And ') and contains(@name, ' more')]")
            );
        } else {
            emptyStateTitleLocators = List.of(
                    // Profile shares the empty-state card layout with Learn
                    // (same take_class_title/subtitle ids under personalize_card).
                    By.xpath("//*[@resource-id='com.cube.arc.fa:id/personalize_card']" +
                             "//*[@resource-id='com.cube.arc.fa:id/take_class_title']")
            );
            emptyStateSubtitleLocators = List.of(
                    By.xpath("//*[@resource-id='com.cube.arc.fa:id/personalize_card']" +
                             "//*[@resource-id='com.cube.arc.fa:id/take_class_subtitle']")
            );
            editCtaLocators = List.of(
                    By.id("com.cube.arc.fa:id/button_edit_personalization")
            );
            tagContainerLocators = List.of(
                    By.id("com.cube.arc.fa:id/container_tags")
            );
            // Named chips have dedicated resource-ids — first_tag / second_tag
            // for the visible labels. Icon-only overflow lives under
            // more_tags (content-desc "And N more") and is excluded here.
            namedTagLocator = By.xpath(
                    "//*[@resource-id='com.cube.arc.fa:id/first_tag' " +
                    "or @resource-id='com.cube.arc.fa:id/second_tag']");
            moreTagsPillLocators = List.of(
                    By.id("com.cube.arc.fa:id/more_tags")
            );
        }
    }

    // ---- Empty state -------------------------------------------------------

    public boolean isEmptyStateTitleVisible() {
        return isVisible(emptyStateTitleLocators);
    }

    public boolean isEmptyStateSubtitleVisible() {
        return isVisible(emptyStateSubtitleLocators);
    }

    // ---- Populated state ---------------------------------------------------

    public boolean isEditCtaVisible() {
        return isVisible(editCtaLocators);
    }

    public boolean isEditCtaPresent() {
        return isPresent(editCtaLocators);
    }

    public void tapEditCta() {
        tap(editCtaLocators);
    }

    public boolean isTagContainerVisible() {
        return isVisible(tagContainerLocators);
    }

    /**
     * Readable tag names currently rendered in the personalization strip.
     *
     * <p>Per spec, only the first two selected tags render with their name;
     * any additional tags show as icon-only. So the returned list size is
     * {@code min(selectedTagCount, 2)}. Order matches render order.
     */
    /**
     * True if the "And N more" overflow pill is rendered, i.e. the user has
     * selected more than 2 tags (so anything past position 2 is icon-only).
     */
    public boolean isMoreTagsPillPresent() {
        return isPresent(moreTagsPillLocators);
    }

    public List<String> getVisibleTagNames() {
        List<String> names = new ArrayList<>();
        for (WebElement el : driver.findElements(namedTagLocator)) {
            String text = platform.equals("ios") ? el.getAttribute("name") : el.getText();
            if (text != null && !text.isBlank()) {
                names.add(text.trim());
            }
        }
        return names;
    }
}
