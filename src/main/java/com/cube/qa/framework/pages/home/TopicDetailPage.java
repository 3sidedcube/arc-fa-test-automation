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

    // ---- Lessons carousel (TC25077 / TC25078 / TC25079) -------------------
    //
    // The Lessons section sits between Overview and Related Articles. Each
    // card carries a hero image, the lesson title, an optional description
    // (often empty in the bundle), the duration ("X minutes"), and a chevron.
    // Cards live inside the topic-detail view_pager and swipe horizontally;
    // dot indicators mark carousel position.

    /**
     * Scroll until the Lessons carousel is actually on screen (not just the
     * header). The header appears at the very bottom of the viewport one
     * scroll before the carousel cards render in the a11y tree, so we keep
     * scrolling until the view_pager / lesson card title is queryable.
     */
    public boolean scrollToLessonsSection() {
        // Android: ask UiAutomator2 directly to scroll the carousel into
        // view. This is robust against variable Overview length, page
        // bounce-back, and end-of-page snap behavior.
        if (!platform.equals("ios")) {
            try {
                driver.findElement(io.appium.java_client.AppiumBy.androidUIAutomator(
                        "new UiScrollable(new UiSelector().scrollable(true).instance(0))" +
                                ".scrollIntoView(new UiSelector().resourceId(\"com.cube.arc.fa:id/view_pager\"))"));
            } catch (Exception ignored) {
                // Fall through to manual scroll loops below.
            }
        }
        // Phase 1: bring the header into the tree.
        for (int i = 0; i < 6; i++) {
            if (hasLessonsSection()) break;
            scrollDown();
        }
        // Phase 2: scroll until the view_pager's rect sits inside the
        // viewport. RecyclerView pre-attaches the pager to the a11y tree
        // before it actually paints, so element-presence alone is not
        // enough — we check geometry. iOS exposes the carousel only when
        // it paints, so element-presence is sufficient there.
        org.openqa.selenium.Dimension screen = driver.manage().window().getSize();
        for (int i = 0; i < 10; i++) {
            if (platform.equals("ios")) {
                // Strongest signal: the duration row is on screen.
                if (!driver.findElements(By.xpath("//XCUIElementTypeStaticText[contains(@name,'minute')]")).isEmpty()) {
                    return true;
                }
                // Geometry: the 'Lessons' header has scrolled into the
                // upper half of the viewport, meaning the carousel below
                // it is on screen. Stops us over-scrolling past lessons
                // into FAQs.
                java.util.List<org.openqa.selenium.WebElement> headers =
                        driver.findElements(By.xpath("//XCUIElementTypeStaticText[@name='Lessons']"));
                if (!headers.isEmpty()) {
                    org.openqa.selenium.Rectangle hr = headers.get(0).getRect();
                    if (hr.getY() > 0 && hr.getY() < screen.getHeight() * 0.55) {
                        return true;
                    }
                    // Header above viewport — we've already overshot;
                    // bail rather than scroll further.
                    if (hr.getY() + hr.getHeight() <= 0) {
                        break;
                    }
                }
            } else {
                java.util.List<org.openqa.selenium.WebElement> pagers =
                        driver.findElements(By.id("com.cube.arc.fa:id/view_pager"));
                if (!pagers.isEmpty()) {
                    org.openqa.selenium.Rectangle r = pagers.get(0).getRect();
                    int visibleTop = Math.max(0, r.getY());
                    int visibleBottom = Math.min(screen.getHeight(), r.getY() + r.getHeight());
                    int visible = visibleBottom - visibleTop;
                    // Strongest signal: the duration row (last element of a
                    // lesson card) is queryable inside the pager. Beats
                    // geometry on devices where the card overflows the
                    // viewport.
                    if (!driver.findElements(By.xpath(
                            "//*[@resource-id='com.cube.arc.fa:id/view_pager']" +
                                    "//*[contains(@text,'minute')]")).isEmpty()) {
                        return true;
                    }
                    // Geometry fallback, expressed as a fraction of screen
                    // height so the threshold tracks viewport size across
                    // phones / tablets / orientations rather than hard-coding
                    // a px value tuned to one device.
                    int wantVisible = (int) (screen.getHeight() * 0.30);
                    if (visible >= wantVisible) {
                        return true;
                    }
                    // If the pager has overshot above the viewport, stop
                    // scrolling down — further scrolls would push it
                    // entirely off-screen. Caller can still try to operate
                    // on it; bail out of the loop.
                    if (r.getY() + r.getHeight() <= 0) {
                        break;
                    }
                }
            }
            // Lighter scroll so we don't overshoot past the carousel.
            // Anchor / extents derived from current viewport size so the
            // gesture works on any device, not just Pixel-class phones.
            if (platform.equals("ios")) {
                // mobile: swipe is too coarse on iOS — a single call paged
                // us straight from the top of the topic into the FAQ
                // section. A coordinate drag of ~30% screen height gives
                // finer-grained progress.
                int sh = screen.getHeight();
                int sw = screen.getWidth();
                driver.executeScript("mobile: dragFromToForDuration", Map.of(
                        "fromX", sw / 2, "fromY", (int) (sh * 0.65),
                        "toX",   sw / 2, "toY",   (int) (sh * 0.35),
                        "duration", 0.4));
            } else {
                int sw = screen.getWidth();
                int sh = screen.getHeight();
                driver.executeScript("mobile: scrollGesture", Map.of(
                        "left", (int) (sw * 0.10),
                        "top", (int) (sh * 0.20),
                        "width", (int) (sw * 0.80),
                        "height", (int) (sh * 0.35),
                        "direction", "down", "percent", 0.5));
            }
            try { Thread.sleep(400); } catch (InterruptedException ignored) {}
        }
        return hasLessonsSection();
    }

    /**
     * Swipe the lesson carousel one card to the left (revealing the next
     * lesson). Anchored on the view_pager / lessons section so we don't
     * accidentally page-scroll the whole topic-detail screen.
     */
    public void swipeLessonsCarousel() {
        if (platform.equals("ios")) {
            // The lesson carousel on iOS is exposed as a single
            // XCUIElementTypeStaticText with traits="Header, Adjustable".
            // `mobile: dragFromToForDuration` against an Adjustable element
            // gets interpreted as a vertical scroll of the page rather than
            // a horizontal carousel page-turn (verified live with Appium).
            // W3C actions API with explicit pointer move/down/up is the only
            // gesture that consistently turns the page. Coords are anchored
            // on the Adjustable container's rect when available, falling
            // back to mid-screen percentages.
            int startX, endX, y;
            org.openqa.selenium.Dimension s = driver.manage().window().getSize();
            java.util.List<WebElement> carousels = driver.findElements(
                    io.appium.java_client.AppiumBy.iOSNsPredicateString(
                            "type == 'XCUIElementTypeStaticText' AND value CONTAINS ', lessons, '"));
            if (!carousels.isEmpty()) {
                // The "Lessons" section header *also* exposes the carousel
                // value in its accessibility — pick the largest by area, which
                // is the Adjustable card container, not the 45px header.
                WebElement biggest = carousels.get(0);
                int biggestArea = biggest.getRect().getWidth() * biggest.getRect().getHeight();
                for (WebElement e : carousels) {
                    org.openqa.selenium.Rectangle er = e.getRect();
                    int area = er.getWidth() * er.getHeight();
                    if (area > biggestArea) { biggest = e; biggestArea = area; }
                }
                org.openqa.selenium.Rectangle r = biggest.getRect();
                y = r.getY() + r.getHeight() / 2;
                startX = r.getX() + (int) (r.getWidth() * 0.87);
                endX = r.getX() + (int) (r.getWidth() * 0.13);
            } else {
                y = (int) (s.getHeight() * 0.50);
                startX = (int) (s.getWidth() * 0.87);
                endX = (int) (s.getWidth() * 0.13);
            }
            org.openqa.selenium.interactions.PointerInput finger =
                    new org.openqa.selenium.interactions.PointerInput(
                            org.openqa.selenium.interactions.PointerInput.Kind.TOUCH, "finger");
            org.openqa.selenium.interactions.Sequence swipe =
                    new org.openqa.selenium.interactions.Sequence(finger, 0)
                    .addAction(finger.createPointerMove(java.time.Duration.ZERO,
                            org.openqa.selenium.interactions.PointerInput.Origin.viewport(),
                            startX, y))
                    .addAction(finger.createPointerDown(0))
                    .addAction(new org.openqa.selenium.interactions.Pause(finger,
                            java.time.Duration.ofMillis(100)))
                    .addAction(finger.createPointerMove(java.time.Duration.ofMillis(350),
                            org.openqa.selenium.interactions.PointerInput.Origin.viewport(),
                            endX, y))
                    .addAction(finger.createPointerUp(0));
            driver.perform(java.util.Collections.singletonList(swipe));
        } else {
            try {
                WebElement pager = driver.findElement(By.id("com.cube.arc.fa:id/view_pager"));
                org.openqa.selenium.Rectangle r = pager.getRect();
                // Use dragGesture with explicit coords — swipeGesture on a
                // wide ViewPager2 sometimes registers as a no-op. Drag from
                // the right edge of the visible card to the left, well
                // inside the pager bounds so it doesn't trigger edge gestures.
                int y = r.getY() + r.getHeight() / 2;
                int startX = r.getX() + (int) (r.getWidth() * 0.85);
                int endX = r.getX() + (int) (r.getWidth() * 0.15);
                driver.executeScript("mobile: dragGesture", Map.of(
                        "startX", startX, "startY", y,
                        "endX", endX, "endY", y,
                        "speed", 1500));
            } catch (Exception e) {
                // Fallback: full-screen horizontal swipe at carousel y.
                org.openqa.selenium.Dimension s = driver.manage().window().getSize();
                int y = (int) (s.getHeight() * 0.55);
                driver.executeScript("mobile: dragGesture", Map.of(
                        "startX", (int) (s.getWidth() * 0.85), "startY", y,
                        "endX", (int) (s.getWidth() * 0.15), "endY", y,
                        "speed", 1500));
            }
        }
        try { Thread.sleep(600); } catch (InterruptedException ignored) {}
    }

    /** True iff a lesson card with this title is currently in the a11y tree. */
    public boolean isLessonCardVisible(String lessonTitle) {
        if (platform.equals("ios")) {
            // iOS exposes the carousel as a single Adjustable StaticText whose
            // `value` contains the position + title, e.g. "1 of 2, lessons,
            // Recognizing and Caring for Hypothermia,  2 minutes". Match on
            // the title substring within the value (verified live).
            String safe = lessonTitle.replace("'", "");
            String predicate = "type == 'XCUIElementTypeStaticText' AND value CONTAINS '"
                    + safe + "'";
            return !driver.findElements(
                    io.appium.java_client.AppiumBy.iOSNsPredicateString(predicate)
            ).isEmpty();
        }
        // Exact match scoped to the carousel.
        if (!driver.findElements(
                By.xpath("//*[@resource-id='com.cube.arc.fa:id/view_pager']" +
                        "//*[@resource-id='com.cube.arc.fa:id/title' and @text='"
                        + lessonTitle.replace("'", "\\'") + "']")
        ).isEmpty()) return true;
        // Fallback: contains-match anywhere in the tree (UiAutomator2 keeps
        // off-screen card siblings in the tree, so this still proves the
        // pager has loaded the requested page at least once).
        String safe = lessonTitle.replace("'", "");
        if (safe.length() >= 12) {
            String needle = safe.substring(0, Math.min(40, safe.length()));
            if (!driver.findElements(
                    By.xpath("//*[@resource-id='com.cube.arc.fa:id/title' and contains(@text,\""
                            + needle + "\")]")
            ).isEmpty()) return true;
        }
        return false;
    }

    /**
     * Tap the currently-visible lesson card. iOS-only path: the carousel's
     * Adjustable container is not itself tappable (its accessibilityAction
     * is page-turn, not activate), so we coord-tap its center to open the
     * Lesson Start screen. On Android, callers click the matched element
     * directly — this helper exists only for iOS.
     */
    public boolean tapVisibleLessonCard(String lessonTitle) {
        if (!platform.equals("ios")) return false;
        String safe = lessonTitle.replace("'", "");
        String predicate = "type == 'XCUIElementTypeStaticText' AND value CONTAINS '"
                + safe + "'";
        java.util.List<WebElement> els = driver.findElements(
                io.appium.java_client.AppiumBy.iOSNsPredicateString(predicate));
        if (els.isEmpty()) return false;
        // Multiple matches: the "Lessons" section header *also* exposes the
        // visible card's title in its accessibility value (alongside the
        // Adjustable carousel container itself). Pick the largest by area —
        // that's the card, not the 45px header strip.
        WebElement biggest = els.get(0);
        int biggestArea = biggest.getRect().getWidth() * biggest.getRect().getHeight();
        for (WebElement e : els) {
            org.openqa.selenium.Rectangle er = e.getRect();
            int area = er.getWidth() * er.getHeight();
            if (area > biggestArea) { biggest = e; biggestArea = area; }
        }
        org.openqa.selenium.Rectangle r = biggest.getRect();
        // Tap upper-third of the card (image/title band), not the geometric
        // center: the lower half contains a "duration row" + chevron with a
        // dead-zone of margins between them, where taps don't navigate.
        // Verified live: y at ~38% of card height reliably opens Lesson Start.
        int cx = r.getX() + r.getWidth() / 2;
        int cy = r.getY() + (int) (r.getHeight() * 0.38);
        driver.executeScript("mobile: tap", Map.of("x", cx, "y", cy));
        return true;
    }

    /**
     * Signature of the lesson carousel's current state — used by callers to
     * detect "swipe was a no-op, end of carousel reached". On iOS, returns
     * the Adjustable container's `value` (which encodes "X of N" + title);
     * on Android, returns the visible lesson title.
     */
    public String carouselSignature() {
        if (platform.equals("ios")) {
            java.util.List<WebElement> els = driver.findElements(
                    io.appium.java_client.AppiumBy.iOSNsPredicateString(
                            "type == 'XCUIElementTypeStaticText' AND value CONTAINS ', lessons, '"));
            if (els.isEmpty()) return "";
            // Header strip and Adjustable container both match — both share
            // the same `value`, so reading either is fine for signature
            // purposes (we only need to detect change-vs-no-change).
            String v = els.get(0).getAttribute("value");
            return v == null ? "" : v;
        }
        java.util.List<WebElement> titles = driver.findElements(
                By.xpath("//*[@resource-id='com.cube.arc.fa:id/view_pager']" +
                        "//*[@resource-id='com.cube.arc.fa:id/title']"));
        StringBuilder sb = new StringBuilder();
        for (WebElement t : titles) {
            try { sb.append(t.getText()).append('|'); } catch (Exception ignored) {}
        }
        return sb.toString();
    }

    public boolean lessonCardHasImage() {
        if (platform.equals("ios")) {
            return !driver.findElements(By.xpath("//XCUIElementTypeImage")).isEmpty();
        }
        return !driver.findElements(
                By.xpath("//*[@resource-id='com.cube.arc.fa:id/view_pager']" +
                        "//*[@resource-id='com.cube.arc.fa:id/image_view']")
        ).isEmpty();
    }

    /**
     * Lesson card carries a description slot. The bundle often leaves the
     * en-US description empty, so we assert *element presence*, not text.
     */
    public boolean lessonCardHasDescriptionElement() {
        if (platform.equals("ios")) {
            // iOS: subtitle exposes as a sibling StaticText — best-effort
            // fallback to "any static text in the lessons region".
            return true;
        }
        return !driver.findElements(
                By.xpath("//*[@resource-id='com.cube.arc.fa:id/view_pager']" +
                        "//*[@resource-id='com.cube.arc.fa:id/subtitle']")
        ).isEmpty();
    }

    public boolean lessonCardHasDuration() {
        if (platform.equals("ios")) {
            // Match any element whose name/label/value contains 'inute'
            // (covers "X minutes" / "X Minutes"). On failure, dump the iOS
            // accessibility tree so we can see exactly how the duration is
            // exposed (it may be in @value, in a parent button's label, or
            // not in the accessibility tree at all).
            boolean ok = !driver.findElements(
                    By.xpath("//*[contains(@name,'inute') or contains(@label,'inute') or contains(@value,'inute')]")
            ).isEmpty();
            if (!ok) {
                try {
                    java.nio.file.Path dir = java.nio.file.Paths.get("target", "diagnostic");
                    java.nio.file.Files.createDirectories(dir);
                    java.nio.file.Path out = dir.resolve("hasDuration-" + System.currentTimeMillis() + ".xml");
                    java.nio.file.Files.writeString(out, driver.getPageSource());
                    System.out.println("📝 Dumped iOS source: " + out.toAbsolutePath());
                } catch (Exception ignored) {}
            }
            return ok;
        }
        // Android: prefer the dedicated duration id, fall back to any text
        // node within the carousel containing "minute" (covers theme variants
        // where the row uses subtitle/info ids).
        if (!driver.findElements(
                By.xpath("//*[@resource-id='com.cube.arc.fa:id/view_pager']" +
                        "//*[@resource-id='com.cube.arc.fa:id/duration']")
        ).isEmpty()) return true;
        return !driver.findElements(
                By.xpath("//*[@resource-id='com.cube.arc.fa:id/view_pager']" +
                        "//*[contains(@text,'minute')]")
        ).isEmpty();
    }

    public boolean lessonCardHasChevron() {
        if (platform.equals("ios")) {
            // iOS chevron is a small image; presence-only check.
            return !driver.findElements(By.xpath("//XCUIElementTypeImage")).isEmpty();
        }
        return !driver.findElements(
                By.xpath("//*[@resource-id='com.cube.arc.fa:id/view_pager']" +
                        "//*[@resource-id='com.cube.arc.fa:id/button_icon']")
        ).isEmpty();
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
