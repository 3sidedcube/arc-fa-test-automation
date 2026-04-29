package com.cube.qa.framework.pages.home;

import com.cube.qa.framework.pages.BasePage;
import io.appium.java_client.AppiumDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.WebElement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Give Care emergency-topic detail screen — reached by tapping a row in the
 * alphabetised list on the Give Care tab. Renders the topic title, an
 * ordered list of numbered steps (each with a heading and an optional
 * description that may contain {@code »}-marker bullet lines), a sticky
 * Call 911 button at the bottom, and a back chevron in the toolbar.
 *
 * <p>This is intentionally a separate page object from the Learn-tab
 * {@link TopicDetailPage}: emergency topics have no pin button, no FAQs,
 * no Lessons section, and a different toolbar shape.
 *
 * <p>Step content is asserted against the article body fetched by
 * {@link com.cube.qa.framework.testdata.loader.ContentBundleLoader#articleDetail(String)}
 * — locator-bound test code lives here, content expectations live in tests.
 */
public class GiveCareTopicDetailPage extends BasePage {

    private final String platform;

    private final List<By> titleLocators;
    private final List<By> backButtonLocators;
    private final List<By> stickyCall911Locators;
    private final List<By> stepNumberLocators;
    private final List<By> stepTitleLocators;
    private final List<By> stepDescriptionLocators;

    public GiveCareTopicDetailPage(AppiumDriver driver, String platform) {
        super(driver);
        this.platform = platform.toLowerCase();

        if (this.platform.equals("ios")) {
            // The toolbar carries both a NavigationBar.name and a Header
            // StaticText with the same value — read the StaticText so we
            // get a node we can call getText() on.
            titleLocators = List.of(
                    By.xpath("//XCUIElementTypeNavigationBar"
                          + "/XCUIElementTypeStaticText")
            );
            backButtonLocators = List.of(
                    By.name("BackButton"),
                    By.xpath("//XCUIElementTypeButton[@name='BackButton']")
            );
            // The detail screen has exactly one Call 911 button (no list of
            // tools competing for the name like the Give Care tab header
            // does), so the bare name is enough.
            stickyCall911Locators = List.of(
                    By.name("Call 911"),
                    By.xpath("//XCUIElementTypeButton[@name='Call 911']")
            );
            // Step numbers render as "1.", "2.", … StaticTexts.
            stepNumberLocators = List.of(
                    By.xpath("//XCUIElementTypeStaticText[substring(@name,"
                          + "string-length(@name))='.' and string-length(@name)<=4]")
            );
            // No dedicated id for step title/description on iOS — tests pull
            // the matching StaticText by its expected content, which keeps
            // assertions tied to the bundle rather than to layout order.
            stepTitleLocators = Collections.emptyList();
            stepDescriptionLocators = Collections.emptyList();
        } else {
            // Android resource-ids confirmed via uiautomator dump on v4.0.0
            // prod APK: each step is an annotation/title/description trio
            // inside the recycler_view_article_content scroll view.
            titleLocators = List.of(
                    By.xpath("//*[@resource-id='com.cube.arc.fa:id/toolbar']"
                          + "//android.widget.TextView")
            );
            backButtonLocators = List.of(
                    By.xpath("//*[@content-desc='Navigate back to main screen']")
            );
            stickyCall911Locators = List.of(
                    By.id("com.cube.arc.fa:id/emergency_button")
            );
            stepNumberLocators = List.of(
                    By.id("com.cube.arc.fa:id/annotation")
            );
            stepTitleLocators = List.of(
                    By.xpath("//*[@resource-id='com.cube.arc.fa:id/recycler_view_article_content']"
                          + "//*[@resource-id='com.cube.arc.fa:id/title']")
            );
            stepDescriptionLocators = List.of(
                    By.xpath("//*[@resource-id='com.cube.arc.fa:id/recycler_view_article_content']"
                          + "//*[@resource-id='com.cube.arc.fa:id/description']")
            );
        }
    }

    // ---- Top-level chrome --------------------------------------------------

    /** The toolbar topic title text. */
    public String getTitle() {
        try {
            WebElement el = waitForVisibility(titleLocators);
            String t = platform.equals("ios") ? el.getAttribute("name") : el.getText();
            return t == null ? "" : t.trim();
        } catch (Exception e) {
            return "";
        }
    }

    public boolean hasTitle() {
        return !getTitle().isEmpty();
    }

    public boolean hasBackButton() {
        return isPresent(backButtonLocators);
    }

    public void tapBack() {
        tap(backButtonLocators);
    }

    // ---- Sticky Call 911 ---------------------------------------------------

    public boolean hasStickyCall911() {
        if (!platform.equals("ios")) {
            return isPresent(stickyCall911Locators);
        }
        // On iOS, verify the button's center sits in the lower portion of the
        // viewport — guards against a misnamed inline button accidentally
        // matching, e.g. an article-embedded "Call 911" widget.
        for (WebElement el : driver.findElements(stickyCall911Locators.get(0))) {
            try {
                Rectangle r = el.getRect();
                Dimension size = driver.manage().window().getSize();
                if (r.getY() + r.getHeight() / 2 > size.getHeight() * 0.7) return true;
            } catch (Exception ignored) {}
        }
        return false;
    }

    public void tapCall911() {
        tap(stickyCall911Locators);
    }

    // ---- Steps -------------------------------------------------------------

    /**
     * Step numbers as displayed on screen, e.g. {@code [1, 2, 3, …]}.
     * Deduped while preserving order — on iOS the same logical "1." can be
     * exposed via multiple StaticText nodes (container + label), and we only
     * care about the distinct sequence.
     */
    public List<Integer> getRenderedStepNumbers() {
        List<Integer> out = new ArrayList<>();
        List<WebElement> els = driver.findElements(stepNumberLocators.get(0));
        Integer prev = null;
        for (WebElement e : els) {
            try {
                String raw = platform.equals("ios") ? e.getAttribute("name") : e.getText();
                if (raw == null) continue;
                String digits = raw.replaceAll("[^0-9]", "");
                if (digits.isEmpty()) continue;
                int n = Integer.parseInt(digits);
                if (prev != null && prev == n) continue; // collapse adjacent dupes
                out.add(n);
                prev = n;
            } catch (Exception ignored) {}
        }
        return out;
    }

    /**
     * True if any rendered step description contains the "{@code »}" bullet
     * marker. Used by TC22029 — emergency-step descriptions don't use HTML
     * bullets, they prefix bullet lines with U+00BB.
     */
    public boolean hasBulletInStepDescription() {
        if (platform.equals("ios")) {
            // No description-specific id on iOS; scan all visible StaticText
            // and look for the marker. Cheap because the marker is rare —
            // it never appears in chrome strings.
            for (WebElement el : driver.findElements(
                    By.xpath("//XCUIElementTypeStaticText"))) {
                try {
                    String n = el.getAttribute("name");
                    if (n != null && n.contains("\u00BB")) return true;
                } catch (Exception ignored) {}
            }
            return false;
        }
        for (WebElement el : driver.findElements(stepDescriptionLocators.get(0))) {
            try {
                String t = el.getText();
                if (t != null && t.contains("\u00BB")) return true;
            } catch (Exception ignored) {}
        }
        return false;
    }

    /**
     * True if some step description on screen exactly matches (after trim)
     * the expected text from the bundle. iOS reports the same content via
     * {@code @name}; Android via {@code getText()}.
     */
    public boolean hasStepDescriptionMatching(String expected) {
        if (expected == null || expected.isBlank()) return false;
        String needle = expected.trim();
        if (platform.equals("ios")) {
            for (WebElement el : driver.findElements(
                    By.xpath("//XCUIElementTypeStaticText"))) {
                try {
                    String n = el.getAttribute("name");
                    if (n != null && n.trim().equals(needle)) return true;
                } catch (Exception ignored) {}
            }
            return false;
        }
        for (WebElement el : driver.findElements(stepDescriptionLocators.get(0))) {
            try {
                String t = el.getText();
                if (t != null && t.trim().equals(needle)) return true;
            } catch (Exception ignored) {}
        }
        return false;
    }

    /**
     * True if a step heading matching {@code expected} (trimmed, case-
     * insensitive) is rendered. On Android we scope to the recycler view's
     * step-title resource-id; on iOS we match any visible StaticText.
     */
    public boolean hasStepTitleMatching(String expected) {
        if (expected == null || expected.isBlank()) return false;
        String needle = expected.trim();
        if (platform.equals("ios")) {
            for (WebElement el : driver.findElements(
                    By.xpath("//XCUIElementTypeStaticText"))) {
                try {
                    String n = el.getAttribute("name");
                    if (n != null && n.trim().equalsIgnoreCase(needle)) return true;
                } catch (Exception ignored) {}
            }
            return false;
        }
        for (WebElement el : driver.findElements(stepTitleLocators.get(0))) {
            try {
                String t = el.getText();
                if (t != null && t.trim().equalsIgnoreCase(needle)) return true;
            } catch (Exception ignored) {}
        }
        return false;
    }

    // ---- Scroll helpers ----------------------------------------------------

    public void scrollDown() {
        if (platform.equals("ios")) {
            driver.executeScript("mobile: swipe", Map.of("direction", "up"));
        } else {
            driver.executeScript("mobile: scrollGesture", Map.of(
                    "left", 100, "top", 500, "width", 800, "height", 1200,
                    "direction", "down", "percent", 0.7));
        }
    }

    /**
     * Scroll downwards until the rendered description matching
     * {@code expected} comes into view, or {@code maxPasses} swipes have
     * elapsed. Useful for steps near the bottom of a long article.
     */
    public boolean scrollUntilDescriptionVisible(String expected, int maxPasses) {
        for (int i = 0; i < maxPasses; i++) {
            if (hasStepDescriptionMatching(expected)) return true;
            scrollDown();
            try { Thread.sleep(400); } catch (InterruptedException ignored) {}
        }
        return hasStepDescriptionMatching(expected);
    }

    /**
     * Scroll downwards until a step heading matching {@code expected} comes
     * into view, or {@code maxPasses} swipes have elapsed.
     */
    public boolean scrollUntilTitleVisible(String expected, int maxPasses) {
        for (int i = 0; i < maxPasses; i++) {
            if (hasStepTitleMatching(expected)) return true;
            scrollDown();
            try { Thread.sleep(400); } catch (InterruptedException ignored) {}
        }
        return hasStepTitleMatching(expected);
    }

    // ---- Call sheet (delegated) -------------------------------------------
    //
    // The call-sheet detection / cancellation logic is identical to the Give
    // Care tab's flow (same tel: link, same SpringBoard / Android-dialer
    // dance). Tests instantiate GiveCareTabPage alongside this page object
    // and reuse its `isCallSheetPresent()` / `cancelCallSheet()` helpers
    // — keeping that platform-fork logic in one place rather than copying it.
}
