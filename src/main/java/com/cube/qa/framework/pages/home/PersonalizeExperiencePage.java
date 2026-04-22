package com.cube.qa.framework.pages.home;

import com.cube.qa.framework.pages.BasePage;
import io.appium.java_client.AppiumDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * Personalize Experience bottom-sheet modal, opened from the Learn tab.
 * Renders a title, subtitle, a question prompt, a scrollable list of topic
 * tags (each a checkbox), and a primary "PERSONALIZE EXPERIENCE" CTA that
 * stays disabled until at least one tag is selected.
 *
 * <p>The tag list is scrollable — on Android the view recycles off-screen
 * items out of the accessibility hierarchy, so {@link #collectAllTopicTagsInOrder()}
 * swipes progressively and dedupes while preserving render order.
 */
public class PersonalizeExperiencePage extends BasePage {

    private final String platform;

    private final List<By> modalContainerLocators;
    private final List<By> titleLocators;
    private final List<By> subtitleLocators;
    private final List<By> questionTitleLocators;
    private final List<By> questionSubtitleLocators;
    private final List<By> confirmCtaLocators;
    private final List<By> closeButtonLocators;

    /** Locator used to enumerate the tag rows currently attached to the view. */
    private final By tagRowLocator;

    public PersonalizeExperiencePage(AppiumDriver driver, String platform) {
        super(driver);
        this.platform = platform.toLowerCase();

        if (this.platform.equals("ios")) {
            modalContainerLocators = List.of(
                    By.name("Personalize your experience!")
            );
            titleLocators = List.of(
                    By.name("Personalize your experience!")
            );
            subtitleLocators = List.of(
                    By.name("You can change your preferences anytime in your profile.")
            );
            questionTitleLocators = List.of(
                    By.name("What would you like to learn about?")
            );
            questionSubtitleLocators = List.of(
                    By.name("Select all that apply")
            );
            // The Learn-tab CTA and the modal's primary CTA both surface as
            // XCUIElementTypeButton[name='PERSONALIZE EXPERIENCE'] — only the
            // in-modal one has visible='true' while the sheet is up. Filter
            // on that so presence/enabled checks target the right button.
            confirmCtaLocators = List.of(
                    By.xpath("//XCUIElementTypeButton[@name='PERSONALIZE EXPERIENCE' and @visible='true']")
            );
            closeButtonLocators = List.of(
                    By.name("Close")
            );
            // Each topic tag has an XCUIElementTypeSwitch row whose name is
            // "{Tag Name}, double tap to select" — 7 of them, in CMS order.
            // Switches are the cleanest anchor; the "double tap" suffix is
            // stripped in readRowLabel below.
            tagRowLocator = By.xpath(
                    "//XCUIElementTypeSwitch[contains(@name, 'double tap to select')]");
        } else {
            modalContainerLocators = List.of(
                    By.id("com.cube.arc.fa:id/design_bottom_sheet"),
                    By.id("com.cube.arc.fa:id/text_title")
            );
            titleLocators = List.of(By.id("com.cube.arc.fa:id/text_title"));
            subtitleLocators = List.of(By.id("com.cube.arc.fa:id/text_subtitle"));
            questionTitleLocators = List.of(By.id("com.cube.arc.fa:id/text_question_title"));
            questionSubtitleLocators = List.of(By.id("com.cube.arc.fa:id/text_question_subtitle"));
            confirmCtaLocators = List.of(By.id("com.cube.arc.fa:id/button_confirm"));
            closeButtonLocators = List.of(By.id("com.cube.arc.fa:id/close_button"));
            // The options container hosts a flat list of CheckBox children,
            // one per topic tag. Use class to enumerate; scope by the
            // options_container ancestor to avoid picking up stray CheckBoxes.
            tagRowLocator = By.xpath(
                    "//*[@resource-id='com.cube.arc.fa:id/options_container']//android.widget.CheckBox");
        }
    }

    // ---- Visibility checks --------------------------------------------------

    public boolean isModalVisible() {
        return isVisible(modalContainerLocators);
    }

    public boolean isTitleVisible() {
        return isVisible(titleLocators);
    }

    public boolean isSubtitleVisible() {
        return isVisible(subtitleLocators);
    }

    public boolean isQuestionTitleVisible() {
        return isVisible(questionTitleLocators);
    }

    public boolean isQuestionSubtitleVisible() {
        return isVisible(questionSubtitleLocators);
    }

    public boolean isConfirmCtaVisible() {
        return isVisible(confirmCtaLocators);
    }

    /**
     * Confirm CTA is present but disabled. Mirrors {@code SignInPage}'s pattern
     * of using presence (not visibility) because platforms sometimes mark
     * disabled buttons with visible=false in the accessibility tree.
     */
    public boolean isConfirmCtaDisabled() {
        WebElement button = waitForPresence(confirmCtaLocators);
        return !button.isEnabled();
    }

    public void tapClose() {
        tap(closeButtonLocators);
    }

    // ---- Tag collection -----------------------------------------------------

    /**
     * Walks the scrollable option list from top to bottom, collecting the
     * label of every topic tag exactly once in the order the UI renders them.
     * Safe to call repeatedly — it returns to the top of the sheet before
     * collecting so callers don't have to manage scroll state.
     *
     * <p>Algorithm: read whatever tag rows are currently in the hierarchy,
     * append any new ones to an ordered set, then swipe the sheet up by one
     * page and repeat. Stop when two consecutive reads add nothing (we've
     * bottomed out) or a hard safety cap is hit.
     */
    public List<String> collectAllTopicTagsInOrder() {
        // Don't pre-scroll. iOS keeps all switches in the accessibility tree
        // from the first render, so one read captures all 7 in order. Android
        // virtualizes, so we scroll-reveal until we stop seeing new entries.
        // A "reset to top" swipe risks dismissing the bottom sheet on iOS —
        // and is unnecessary as long as the modal opens at its top anchor.
        LinkedHashSet<String> seen = new LinkedHashSet<>();
        int emptyPasses = 0;
        int safety = 0;
        while (safety++ < 15 && emptyPasses < 2) {
            int before = seen.size();
            for (WebElement row : driver.findElements(tagRowLocator)) {
                String label = readRowLabel(row);
                if (label != null && !label.isBlank()) {
                    seen.add(label);
                }
            }
            if (seen.size() == before) {
                emptyPasses++;
            } else {
                emptyPasses = 0;
            }
            revealMoreBelow();
        }
        return new ArrayList<>(seen);
    }

    /**
     * Scroll the sheet to reveal items that were below the fold. Android's
     * scrollGesture and iOS's swipe use inverted direction semantics — this
     * helper hides the difference. "Below" means items the user would reach
     * by scrolling further down the list.
     */
    private void revealMoreBelow() {
        if (platform.equals("ios")) {
            // iOS: finger moves up → content scrolls up → items from below appear.
            driver.executeScript("mobile: swipe", Map.of("direction", "up"));
        } else {
            // Android scrollGesture direction="down" means the scroll content
            // moves down → reveals items that were below.
            driver.executeScript("mobile: scrollGesture", Map.of(
                    "left", 100,
                    "top", 500,
                    "width", 800,
                    "height", 1200,
                    "direction", "down",
                    "percent", 0.7
            ));
        }
        waitForSeconds(1);
    }

    private String readRowLabel(WebElement row) {
        String raw;
        if (platform.equals("ios")) {
            // XCUIElementTypeSwitch.getText() returns "0" / "1" (the toggle
            // value). The tag title lives on the accessibility name/label.
            raw = row.getAttribute("name");
            if (raw == null || raw.isBlank()) raw = row.getAttribute("label");
        } else {
            // Android CheckBox exposes its label via getText().
            raw = row.getText();
            if (raw == null || raw.isBlank()) raw = row.getAttribute("content-desc");
        }
        if (raw == null) return null;
        // iOS VoiceOver suffix — strip so we return the tag title only.
        int comma = raw.indexOf(", double tap");
        if (comma > 0) raw = raw.substring(0, comma);
        return raw.trim();
    }

}
