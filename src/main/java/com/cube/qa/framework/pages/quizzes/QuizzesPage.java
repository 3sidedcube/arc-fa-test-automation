package com.cube.qa.framework.pages.quizzes;

import com.cube.qa.framework.pages.BasePage;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.AppiumDriver;
import org.openqa.selenium.By;

import java.util.List;
import java.util.Map;

/**
 * Quizzes tab landing screen — reached by tapping the QUIZZES bottom-nav tab
 * (renamed from "TRAINING" in v4.0.0; see {@code TabPage.Tab.QUIZZES}).
 *
 * <p>Layout (verified live, staging build 2484):
 * <ul>
 *   <li>Top: horizontal carousel of badge tiles (one button per quiz, name
 *       like "Anaphylaxis. Not passed.")</li>
 *   <li>"Quiz Topics" section header + "X of Y quizzes completed"</li>
 *   <li>"Next: &lt;Quiz&gt;" promo button (single row above the list)</li>
 *   <li>Alphabetical list of quiz rows — what tests target. Each row is a
 *       single tappable button; on Android the inner TextView has id
 *       {@code chevron_link_title} (shared with the promo, so we match by
 *       exact text rather than id-only); on iOS the row is an
 *       {@code XCUIElementTypeButton} with {@code name=&lt;quiz title&gt;}.</li>
 * </ul>
 *
 * <p>Locators target the alphabetical list specifically and exclude the
 * "Next:" promo and the carousel — picking either of those would land on the
 * same quiz body but skews the user-flow under test.
 */
public class QuizzesPage extends BasePage {

    private final String platform;
    private final List<By> quizTopicsHeaderLocators;

    public QuizzesPage(AppiumDriver driver, String platform) {
        super(driver);
        this.platform = platform.toLowerCase();

        if (this.platform.equals("ios")) {
            quizTopicsHeaderLocators = List.of(
                    By.xpath("//XCUIElementTypeStaticText[@name='Quiz Topics']")
            );
        } else {
            quizTopicsHeaderLocators = List.of(
                    By.id("com.cube.arc.fa:id/quiz_topic_title"),
                    By.xpath("//*[@text='Quiz Topics']")
            );
        }
    }

    public boolean isDisplayed() {
        return isPresent(quizTopicsHeaderLocators);
    }

    /**
     * Tap a quiz row in the alphabetical list. Scrolls the list to bring the
     * row on screen first — only the first ~6 quizzes are visible without
     * scrolling, but the bundle can resolve to any quiz alphabetically. The
     * text must match the bundle's en-US title exactly.
     */
    public void tapQuizByTitle(String title) {
        List<By> locators;
        if (platform.equals("ios")) {
            // Match a Button whose accessibility name equals the title exactly.
            // Excludes the "Next: ..." promo (different name) and the carousel
            // tiles (suffixed with ". Not passed."/". Passed.").
            locators = List.of(
                    By.xpath("//XCUIElementTypeButton[@name='" + title + "']"),
                    By.xpath("//XCUIElementTypeButton[@label='" + title + "']")
            );
        } else {
            // Android: target the CardView ancestor of the chevron_link_title
            // TextView. The TextView itself is clickable=false and a direct
            // click()/coord-tap on it does not reliably propagate to the row's
            // onClick. The wrapping androidx.cardview.widget.CardView is
            // clickable=true and is the registered tap target — selecting it
            // directly makes WebElement.click() fire the row handler every
            // time. Constrain by the inner title's exact text so we ignore the
            // "Next:" promo (different layout) and the carousel tiles.
            // The dump shows ~16 CardViews on the Quizzes tab — carousel tiles,
            // icon containers, and the alphabetical rows — but only the 3
            // alphabetical rows have @clickable='true'. Carousel tiles also
            // contain a chevron_link_title child with the matching text, so
            // without the clickable filter the XPath would resolve to the
            // (non-clickable) carousel tile first in document order and
            // click() would silently no-op.
            locators = List.of(
                    By.xpath("//androidx.cardview.widget.CardView[@clickable='true']"
                            + "[.//*[@resource-id='com.cube.arc.fa:id/chevron_link_title'"
                            + " and @text='" + title + "']]")
            );
        }
        // The full Quiz Topics list is ~21 rows tall and only the first
        // handful render in the viewport. Scroll until the row is on screen
        // before tapping. UiAutomator2's UiScrollable.scrollIntoView is the
        // most reliable Android approach (handles RecyclerView virtualization
        // and arbitrary list lengths in one call); iOS uses mobile: scroll.
        scrollRowIntoView(title);
        // Use BasePage.tap() — it does waitToBeClickable + 300ms settle +
        // click, matching the pattern other pages in this codebase rely on
        // for stable row taps. Bypassing this (raw findElement().click())
        // produced flaky failures on Android: the click would fire while
        // the row was still animating into its scrolled position and the
        // CardView's onClick handler wouldn't always receive it.
        tap(locators);
    }

    /**
     * Best-effort scroll to bring the row matching {@code title} into the
     * viewport. Silent on failure — tapQuizByTitle's findElements pass surfaces
     * the clearer "Quiz row not found" error if scrolling didn't suffice.
     */
    private void scrollRowIntoView(String title) {
        if (platform.equals("ios")) {
            // The row exists in the DOM even when offscreen — find it first,
            // then ask XCUITest to scroll its containing scroll view until
            // the row is on screen. Previously we used `mobile: scroll`
            // direction=down + predicateString on the whole driver, which on
            // prod build 2485 panned the list past the row (Asthma Attack
            // ended up at y=-165 visible=false because the predicate scroll
            // overshot). Driving the scroll from the target element's id
            // with toVisible=true lets XCUITest stop as soon as the row's
            // geometry intersects the visible region.
            try {
                org.openqa.selenium.WebElement row = driver.findElement(
                        By.xpath("//XCUIElementTypeButton[@name='" + title + "'"
                                + " or @label='" + title + "']"));
                String rowId = ((org.openqa.selenium.remote.RemoteWebElement) row).getId();
                driver.executeScript("mobile: scroll", Map.of(
                        "element", rowId,
                        "toVisible", true));
            } catch (Exception ignored) {}
            return;
        }
        // Android: UiScrollable handles RecyclerView lazy-loading natively.
        try {
            driver.findElement(AppiumBy.androidUIAutomator(
                    "new UiScrollable(new UiSelector().scrollable(true).instance(0))"
                            + ".scrollIntoView(new UiSelector().text(\"" + title + "\"))"));
        } catch (Exception ignored) {}
    }
}
