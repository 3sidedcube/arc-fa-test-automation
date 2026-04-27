package com.cube.qa.framework.pages.home;

import com.cube.qa.framework.pages.BasePage;
import io.appium.java_client.AppiumDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Topic Detail page — reached by tapping a card in the Learn tab's Browse
 * Topics carousel or the vertical list below it. Exposes the pin/unpin
 * toolbar action and the "You can now pin content" tooltip shown on the first
 * visit after a fresh install.
 *
 * <p>Pin state is read from the toolbar button's accessibility label:
 * <ul>
 *   <li><b>Android</b>: {@code content-desc} flips between {@code "Pin"}
 *       (currently unpinned) and {@code "Unpin"} (currently pinned) — the
 *       label describes the action the tap will perform.</li>
 *   <li><b>iOS</b>: the button's {@code name} flips between
 *       {@code "Topic Unpinned"} and {@code "Topic Pinned"} — the label
 *       describes the current state. We also key off the same strings for
 *       the toast that briefly appears after a pin/unpin action.</li>
 * </ul>
 */
public class TopicDetailPage extends BasePage {

    private final String platform;

    private final List<By> pinButtonLocators;
    private final List<By> pinnedStateLocators;
    private final List<By> unpinnedStateLocators;
    private final List<By> titleLocators;
    private final List<By> backLocators;
    // Pin-first-visit tooltip.
    private final List<By> pinTooltipTitleLocators;
    private final List<By> pinTooltipSubtitleLocators;

    // Section presence — used by TC25075 (display) + TC25100 (FAQs hidden).
    private final List<By> heroImageLocators;
    private final List<By> overviewSectionLocators;
    private final List<By> overviewBodyLocators;
    private final List<By> lessonsSectionLocators;
    private final List<By> relatedArticlesSectionLocators;
    private final List<By> faqsSectionLocators;
    private final List<By> faqTitleLocators;
    private final List<By> faqDescriptionLocators;

    public TopicDetailPage(AppiumDriver driver, String platform) {
        super(driver);
        this.platform = platform.toLowerCase();

        if (this.platform.equals("ios")) {
            // iOS toolbar pin button is named for its current state: "Topic
            // Pinned" when pinned, "Topic Unpinned" when not. The toast that
            // appears after tapping uses the same strings on a StaticText —
            // disambiguate by element type.
            pinButtonLocators = List.of(
                    By.xpath("//XCUIElementTypeButton[@name='Topic Pinned' or @name='Topic Unpinned']")
            );
            pinnedStateLocators = List.of(
                    By.xpath("//XCUIElementTypeButton[@name='Topic Pinned']")
            );
            unpinnedStateLocators = List.of(
                    By.xpath("//XCUIElementTypeButton[@name='Topic Unpinned']")
            );
            titleLocators = List.of(
                    By.xpath("//XCUIElementTypeNavigationBar//XCUIElementTypeStaticText")
            );
            backLocators = List.of(
                    By.name("BackButton"),
                    By.xpath("//XCUIElementTypeButton[@name='BackButton']")
            );
            pinTooltipTitleLocators = List.of(
                    By.name("You can now pin content")
            );
            pinTooltipSubtitleLocators = List.of(
                    By.xpath("//XCUIElementTypeStaticText[starts-with(@name,'Pin topics that you plan')]")
            );
            // iOS section headers are static text labelled by section title.
            // The hero image lives in an XCUIElementTypeImage at the top of
            // the scroll view.
            heroImageLocators = List.of(
                    By.xpath("(//XCUIElementTypeScrollView//XCUIElementTypeImage)[1]"),
                    By.xpath("//XCUIElementTypeImage")
            );
            overviewSectionLocators = List.of(
                    By.xpath("//XCUIElementTypeStaticText[@name='Overview']")
            );
            overviewBodyLocators = List.of(
                    // Overview body sits directly below the 'Overview' header.
                    By.xpath("//XCUIElementTypeStaticText[@name='Overview']/following-sibling::XCUIElementTypeStaticText[1]")
            );
            lessonsSectionLocators = List.of(
                    By.xpath("//XCUIElementTypeStaticText[@name='Lessons']")
            );
            relatedArticlesSectionLocators = List.of(
                    By.xpath("//XCUIElementTypeStaticText[@name='Related Articles']")
            );
            faqsSectionLocators = List.of(
                    By.xpath("//XCUIElementTypeStaticText[@name='Frequently Asked Questions']")
            );
            // iOS FAQ rows are buttons/cells named by their question text.
            // We don't know question strings up-front, so callers locate them
            // dynamically via faqRowLocators(question).
            faqTitleLocators = List.of();
            faqDescriptionLocators = List.of();
        } else {
            // Android swaps the resource-id of the toolbar button when the
            // topic is pinned vs unpinned: `action_pin_button` (tap = pin) →
            // `action_unpin_button` (tap = unpin). The content-desc mirrors
            // the action ("Pin" / "Unpin"). Accept either id for a tap so
            // callers don't care about current state.
            pinButtonLocators = List.of(
                    By.xpath("//*[@resource-id='com.cube.arc.fa:id/action_pin_button' or " +
                            "@resource-id='com.cube.arc.fa:id/action_unpin_button']")
            );
            pinnedStateLocators = List.of(
                    By.id("com.cube.arc.fa:id/action_unpin_button")
            );
            unpinnedStateLocators = List.of(
                    By.id("com.cube.arc.fa:id/action_pin_button")
            );
            // Topic title in the toolbar — the inner TextView has no
            // resource-id, so we anchor on the toolbar id and grab its
            // first TextView child. (overview_title is the in-content
            // 'Overview' section heading, NOT the topic title.)
            titleLocators = List.of(
                    By.xpath("//*[@resource-id='com.cube.arc.fa:id/toolbar']//android.widget.TextView")
            );
            backLocators = List.of(
                    By.xpath("//*[@content-desc='Navigate up']")
            );
            pinTooltipTitleLocators = List.of(
                    By.id("com.cube.arc.fa:id/tooltip_title"),
                    By.xpath("//*[@text='You can now pin content']")
            );
            pinTooltipSubtitleLocators = List.of(
                    By.id("com.cube.arc.fa:id/tooltip_subtitle")
            );
            heroImageLocators = List.of(
                    By.id("com.cube.arc.fa:id/image_view")
            );
            overviewSectionLocators = List.of(
                    By.id("com.cube.arc.fa:id/overview_title")
            );
            overviewBodyLocators = List.of(
                    By.id("com.cube.arc.fa:id/overview_body")
            );
            lessonsSectionLocators = List.of(
                    By.id("com.cube.arc.fa:id/lessons_title")
            );
            // 'Related Articles' and 'FAQs' both render through
            // text_section_name — disambiguate by exact text. The 'FAQs'
            // section header label in the app is 'Frequently Asked Questions'.
            relatedArticlesSectionLocators = List.of(
                    By.xpath("//*[@resource-id='com.cube.arc.fa:id/text_section_name' and @text='Related Articles']")
            );
            faqsSectionLocators = List.of(
                    By.xpath("//*[@resource-id='com.cube.arc.fa:id/text_section_name' and @text='Frequently Asked Questions']")
            );
            faqTitleLocators = List.of(
                    By.id("com.cube.arc.fa:id/faq_title")
            );
            faqDescriptionLocators = List.of(
                    By.id("com.cube.arc.fa:id/faq_description")
            );
        }
    }

    // ---- Title / navigation ------------------------------------------------

    public String getTitle() {
        WebElement el = waitForVisibility(titleLocators);
        return platform.equals("ios") ? el.getAttribute("name") : el.getText();
    }

    public void tapBack() {
        tap(backLocators);
    }

    // ---- Pin / unpin -------------------------------------------------------

    public boolean isPinButtonVisible() {
        return isVisible(pinButtonLocators);
    }

    public boolean isPinned() {
        return isPresent(pinnedStateLocators);
    }

    public boolean isUnpinned() {
        return isPresent(unpinnedStateLocators);
    }

    public void tapPin() {
        tap(pinButtonLocators);
    }

    /**
     * True while the "Topic Pinned" or "Topic Unpinned" toast is on screen.
     * Toasts disappear fast (about 2s); callers should poll right after
     * tapping pin rather than rely on long waits.
     */
    public boolean isToastPresent(String text) {
        if (platform.equals("ios")) {
            return isPresent(List.of(
                    By.xpath("//XCUIElementTypeStaticText[@name='" + text + "']")));
        }
        // Android toasts render as anonymous TextViews with the toast text —
        // we look for any element with that exact text.
        return isPresent(List.of(
                By.xpath("//*[@text='" + text + "']")));
    }

    // ---- Pin-first-visit tooltip ------------------------------------------

    public boolean isPinTooltipVisible() {
        return isVisible(pinTooltipTitleLocators);
    }

    public boolean isPinTooltipPresent() {
        return isPresent(pinTooltipTitleLocators);
    }

    public String getPinTooltipSubtitle() {
        WebElement el = waitForVisibility(pinTooltipSubtitleLocators);
        return platform.equals("ios") ? el.getAttribute("name") : el.getText();
    }

    // ---- Sections (TC25075 display) ---------------------------------------
    //
    // The Topic Detail screen is a long vertical scroller. Section headers
    // live on different parts of the page — Overview/image at top, Lessons
    // mid-page, Related Articles + FAQs further down. We expose presence
    // checks that don't fail if the section is below the fold (using
    // isPresent rather than isVisible) once it's been scrolled into the
    // accessibility tree, plus a `scrollDown` helper for tests that need to
    // walk the whole page.

    public boolean hasHeroImage() {
        return isPresent(heroImageLocators);
    }

    public boolean hasOverviewSection() {
        return isPresent(overviewSectionLocators);
    }

    public String getOverviewBody() {
        WebElement el = waitForVisibility(overviewBodyLocators);
        return platform.equals("ios") ? el.getAttribute("name") : el.getText();
    }

    public boolean hasLessonsSection() {
        return isPresent(lessonsSectionLocators);
    }

    public boolean hasRelatedArticlesSection() {
        return isPresent(relatedArticlesSectionLocators);
    }

    public boolean hasFaqsSection() {
        return isPresent(faqsSectionLocators);
    }

    /**
     * Page down by ~70% of the viewport. Topic Detail is rendered inside a
     * RecyclerView (Android) / scroll view (iOS); FAQ rows for content-heavy
     * topics sit several screens down so callers typically loop this 3-5
     * times to bring the FAQ section into the a11y tree.
     */
    public void scrollDown() {
        if (platform.equals("ios")) {
            driver.executeScript("mobile: swipe", Map.of("direction", "up"));
        } else {
            driver.executeScript("mobile: scrollGesture", Map.of(
                    "left", 100, "top", 500, "width", 800, "height", 1500,
                    "direction", "down", "percent", 0.8));
        }
        try { Thread.sleep(500); } catch (InterruptedException ignored) {}
    }

    /** Scroll until the FAQ section header is in the a11y tree, or give up. */
    public boolean scrollToFaqsSection() {
        for (int i = 0; i < 8; i++) {
            if (hasFaqsSection()) return true;
            scrollDown();
        }
        return hasFaqsSection();
    }

    // ---- FAQ list (TC25098 / TC25099 / TC25100) ---------------------------

    /**
     * Collect every FAQ question currently in the a11y tree. On Android
     * each FAQ row exposes the question via a {@code faq_title} TextView;
     * on iOS rows are StaticText elements whose accessibility name IS the
     * question. iOS has no resource-id equivalent, so callers should use
     * {@link #isFaqQuestionVisible(String)} when they have known questions
     * to look for.
     */
    public List<String> visibleFaqQuestions() {
        List<String> out = new ArrayList<>();
        if (platform.equals("ios")) {
            // iOS exposes each FAQ row as a Button (and a duplicate
            // StaticText) whose accessibility name is the question text
            // followed by ". collapsible collapsed" or "...expanded". Strip
            // that VoiceOver suffix to get the bare question.
            for (WebElement el : driver.findElements(
                    By.xpath("//XCUIElementTypeButton[contains(@name,'collapsible')]"))) {
                String name = el.getAttribute("name");
                if (name == null) continue;
                int idx = name.lastIndexOf(". collapsible");
                String q = (idx > 0 ? name.substring(0, idx) : name).trim();
                if (q.endsWith("?") && q.length() > 1) out.add(q);
            }
        } else {
            for (WebElement el : driver.findElements(
                    By.id("com.cube.arc.fa:id/faq_title"))) {
                String text = el.getText();
                if (text != null && !text.isBlank()) out.add(text.trim());
            }
        }
        return out;
    }

    /**
     * True if the given FAQ question is currently in the a11y tree. Use
     * this when you have a known question string (e.g. from the bundle)
     * and just need a presence check; it's more reliable on iOS than
     * harvesting every StaticText that ends in '?', because the iOS a11y
     * tree sometimes exposes question text on a parent cell rather than a
     * StaticText.
     */
    public boolean isFaqQuestionVisible(String question) {
        String q = question.trim();
        if (platform.equals("ios")) {
            // iOS appends ". collapsible collapsed" / ". collapsible expanded"
            // to the row's accessibility name, so an exact-match never works.
            // Prefix-match instead — every row starts with the question text.
            return isPresent(List.of(
                    By.xpath("//*[starts-with(@name, " + xpathLiteral(q) + ") " +
                            "or starts-with(@label, " + xpathLiteral(q) + ")]")));
        }
        return isPresent(List.of(
                By.xpath("//*[@resource-id='com.cube.arc.fa:id/faq_title' and " +
                        "normalize-space(@text)=" + xpathLiteral(q) + "]")));
    }

    /**
     * Build an XPath string literal that safely contains both single and
     * double quotes — bundle FAQ text routinely uses both. Standard trick:
     * split on apostrophes and join with concat().
     */
    private static String xpathLiteral(String s) {
        if (!s.contains("'")) return "'" + s + "'";
        if (!s.contains("\"")) return "\"" + s + "\"";
        StringBuilder sb = new StringBuilder("concat(");
        String[] parts = s.split("'", -1);
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) sb.append(", \"'\", ");
            sb.append("'").append(parts[i]).append("'");
        }
        sb.append(")");
        return sb.toString();
    }

    /**
     * Tap the FAQ row whose question text matches. On Android the row's
     * clickable parent has no resource-id, so we walk up from the title
     * TextView to its first clickable ancestor.
     */
    public void tapFaq(String question) {
        // Trim trailing whitespace from the supplied question — bundle data
        // and the rendered text both sometimes carry a trailing space.
        String q = question.trim();
        if (platform.equals("ios")) {
            // iOS row is exposed as a Button whose name starts with the
            // question text (with a ". collapsible collapsed" suffix).
            // Bypass the elementToBeClickable wait because XCUITest reports
            // these buttons as not-clickable when the centre of the element
            // is just below the viewport — a direct click() works fine and
            // also auto-scrolls.
            By locator = By.xpath(
                    "//XCUIElementTypeButton[starts-with(@name, " + xpathLiteral(q) + ")]");
            driver.findElement(locator).click();
        } else {
            By locator = By.xpath(
                    "//*[@resource-id='com.cube.arc.fa:id/faq_title' and " +
                    "normalize-space(@text)=" + xpathLiteral(q) + "]/ancestor::*[@clickable='true'][1]");
            tap(List.of(locator));
        }
        try { Thread.sleep(500); } catch (InterruptedException ignored) {}
    }

    /**
     * True if any FAQ description currently in the a11y tree contains the
     * supplied substring (case-sensitive, trimmed). FAQs expand inline on
     * tap — there's no separate detail screen.
     */
    public boolean isFaqAnswerVisible(String answerSubstring) {
        String needle = answerSubstring.trim();
        if (platform.equals("ios")) {
            return isPresent(List.of(
                    By.xpath("//XCUIElementTypeStaticText[contains(@name, \"" + needle + "\")]")));
        }
        for (WebElement el : driver.findElements(faqDescriptionLocators.get(0))) {
            String text = el.getText();
            if (text != null && text.contains(needle)) return true;
        }
        return false;
    }

    /**
     * Collects the text of every rendered FAQ answer (the inline expanded
     * description). On Android each expanded row exposes a
     * {@code faq_description} TextView; on iOS the answer appears as a
     * StaticText sibling of the FAQ row's button. iOS has no dedicated
     * accessibility id, so we widen to all StaticTexts and let the caller
     * filter — the question rows themselves end with a "?" suffix that
     * answers don't, so collisions are unlikely in practice.
     */
    public List<String> visibleFaqAnswerTexts() {
        List<String> out = new ArrayList<>();
        if (platform.equals("ios")) {
            for (WebElement el : driver.findElements(
                    By.xpath("//XCUIElementTypeStaticText"))) {
                String name = el.getAttribute("name");
                if (name == null) continue;
                String trimmed = name.trim();
                // Filter obvious row-headers (questions end in '?') and the
                // VoiceOver hint strings on collapsed/expanded buttons.
                if (trimmed.isEmpty()) continue;
                if (trimmed.contains(". collapsible")) continue;
                if (trimmed.endsWith("?")) continue;
                out.add(trimmed);
            }
        } else {
            for (WebElement el : driver.findElements(faqDescriptionLocators.get(0))) {
                String text = el.getText();
                if (text != null && !text.isBlank()) out.add(text);
            }
        }
        return out;
    }
}
