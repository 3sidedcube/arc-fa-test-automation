package com.cube.qa.framework.pages.lessons;

import com.cube.qa.framework.pages.BasePage;
import io.appium.java_client.AppiumDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.WebElement;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * In-lesson screen — what the user sees while stepping through cards. Top
 * bar carries a close (×) button and a horizontal "snippet indicator" of
 * progress segments (one per card). The card body is a horizontally
 * swipeable pager whose current card exposes a section heading, an optional
 * media block (image/video), and zero or more content components
 * (paragraph, bullets).
 *
 * <p>The progress-bar segments are tappable: tapping the Nth segment jumps
 * to the Nth card. We use {@link #tapProgressBarSegment(int)} (1-indexed) to
 * skip to the end of long lessons in completion-flow tests, mirroring the
 * manual-tester shortcut.
 *
 * <p>The first-time tooltip ("Swipe left and right to view lesson cards")
 * appears as an overlay after the first START — assert via
 * {@link #isTooltipVisible()} and clear via {@link #dismissTooltip()}.
 */
public class LessonPage extends BasePage {

    private final String platform;

    private final List<By> closeButtonLocators;
    private final List<By> snippetIndicatorLocators;
    private final List<By> snippetPagerLocators;
    private final List<By> sectionHeadingLocators;
    private final List<By> paragraphDescriptionLocators;
    private final List<By> bulletItemLocators;
    private final List<By> cardImageLocators;
    private final List<By> cardVideoLocators;
    private final List<By> tooltipLocators;
    private final List<By> doneButtonLocators;

    // Segment locator template: indexed sub-element of snippet_indicator's
    // inner LinearLayout. Built dynamically per-N in #tapProgressBarSegment.
    public LessonPage(AppiumDriver driver, String platform) {
        super(driver);
        this.platform = platform.toLowerCase();

        if (this.platform.equals("ios")) {
            // Locators verified live via Appium against staging build 2484:
            //   close button: name="iconClose"
            //   progress segments: Buttons named "Page N of TOTAL"
            //   cards: ScrollViews laid out side-by-side at x=20, 380, 740, …
            //          (or shifted negative once the user has paged forward).
            //   section heading: first non-empty StaticText inside the active
            //                    card's ScrollView
            //   paragraph: second StaticText (in cards that have one)
            //   bullets: subsequent StaticText siblings
            //   DONE: Button name="Done" (mixed case, not "DONE")
            //
            // CRITICAL: iOS keeps multiple ScrollViews flagged visible="true"
            // — the active card AND the adjacent off-screen ones (which sit
            // at negative x once you've paged forward). Filtering only by
            // visible='true' picks up off-screen siblings and yields the
            // wrong card's heading/text. We must also constrain by x-coord
            // to the on-screen viewport. The `windowWidth` is read once from
            // the driver and embedded into the XPath template, so the rule
            // adapts to any device geometry rather than baking in 390.
            //
            // Same trap applied to iconClose / Done in the previous attempt:
            // ExpectedConditions.elementToBeClickable timed out on the
            // mirror buttons attached to off-screen card views. We now bypass
            // the wait entirely (see #iosCoordTap below); presence-only
            // queries here suffice.
            // Prefer the Button form. SwiftUI propagates the accessibility
            // name "iconClose" onto both the parent Button and its child
            // Image, so By.name() can resolve to the Image — taps on it are
            // no-ops because the gesture handler is on the Button. Listing
            // Button first guarantees we click the gesture receiver.
            closeButtonLocators = List.of(
                    By.xpath("//XCUIElementTypeButton[@name='iconClose']"),
                    By.name("iconClose")
            );
            snippetIndicatorLocators = List.of(
                    By.xpath("//XCUIElementTypeButton[starts-with(@name,'Page ') and contains(@name,' of ')]")
            );
            snippetPagerLocators = List.of(
                    By.xpath("//XCUIElementTypeScrollView")
            );
            // Pre-filter XPaths intentionally permissive — we filter to the
            // on-screen card area in Java via element.getRect() (see
            // #iosOnScreenCardElements). XPath-side filtering on @x failed
            // when XCUITest didn't expose @x on every ScrollView; rect-based
            // filtering is more reliable and stays device-agnostic (uses
            // the live window size, no hardcoded geometry).
            sectionHeadingLocators = List.of(
                    By.xpath("//XCUIElementTypeStaticText[string-length(@name) > 0]")
            );
            paragraphDescriptionLocators = List.of(
                    By.xpath("//XCUIElementTypeStaticText[string-length(@name) > 0]")
            );
            bulletItemLocators = List.of(
                    By.xpath("//XCUIElementTypeStaticText[string-length(@name) > 0]")
            );
            // iOS image cards do NOT expose the image in the a11y tree
            // (verified via target/diagnostic/hasImage-*.xml dump on staging
            // build 2484: zero XCUIElementTypeImage in the entire dump,
            // only StaticText). The image area sits between the card-top
            // (the active ScrollView) and the heading StaticText. Detection
            // is by geometry, not by element — see #currentCardHasImage.
            cardImageLocators = List.of(
                    By.xpath("//XCUIElementTypeImage")
            );
            // Video cards expose a play-button overlay with this exact
            // accessibility name (verified via hasVideo-*.xml dump). The
            // XCUIElementTypeImage thumbnail is also present alongside.
            cardVideoLocators = List.of(
                    By.xpath("//XCUIElementTypeButton[@name='Open and watch this video']"),
                    By.xpath("//XCUIElementTypeButton[contains(@name,'watch this video')]")
            );
            tooltipLocators = List.of(
                    By.xpath("//XCUIElementTypeStaticText[contains(@name,'Swipe left and right')]"),
                    By.xpath("//*[contains(@label,'Swipe left and right')]")
            );
            doneButtonLocators = List.of(
                    By.xpath("//XCUIElementTypeButton[@name='Done']"),
                    By.name("Done")
            );
        } else {
            closeButtonLocators = List.of(
                    By.id("com.cube.arc.fa:id/close_button")
            );
            snippetIndicatorLocators = List.of(
                    By.id("com.cube.arc.fa:id/snippet_indicator")
            );
            snippetPagerLocators = List.of(
                    By.id("com.cube.arc.fa:id/snippet_pager")
            );
            sectionHeadingLocators = List.of(
                    By.id("com.cube.arc.fa:id/text_section_name")
            );
            paragraphDescriptionLocators = List.of(
                    By.id("com.cube.arc.fa:id/paragraph_description")
            );
            // Each bullet renders as an individual TextView with id `title`
            // inside the content_recyclerview. The paragraph has
            // paragraph_description, so id=title is unique to bullets here.
            bulletItemLocators = List.of(
                    By.xpath("//*[@resource-id='com.cube.arc.fa:id/content_recyclerview']" +
                            "//*[@resource-id='com.cube.arc.fa:id/title']")
            );
            // Card-level media. Image lives directly inside the snippet pager
            // (distinct from the lesson hero). Video uses a player view.
            cardImageLocators = List.of(
                    By.xpath("//*[@resource-id='com.cube.arc.fa:id/snippet_pager']" +
                            "//android.widget.ImageView[@resource-id='com.cube.arc.fa:id/image']"),
                    By.xpath("//*[@resource-id='com.cube.arc.fa:id/snippet_pager']" +
                            "//android.widget.ImageView[@resource-id='com.cube.arc.fa:id/image_view']"),
                    // Permissive fallback — any ImageView with a resource-id
                    // inside the active snippet pager. Covers theme variants
                    // where the imageComponent uses a different id.
                    By.xpath("//*[@resource-id='com.cube.arc.fa:id/snippet_pager']" +
                            "//android.widget.ImageView[string-length(@resource-id) > 0]")
            );
            cardVideoLocators = List.of(
                    By.xpath("//*[@resource-id='com.cube.arc.fa:id/snippet_pager']" +
                            "//*[@resource-id='com.cube.arc.fa:id/video_player' or " +
                            "@resource-id='com.cube.arc.fa:id/video_view' or " +
                            "@resource-id='com.cube.arc.fa:id/play_button' or " +
                            "@resource-id='com.cube.arc.fa:id/exo_play' or " +
                            "@resource-id='com.cube.arc.fa:id/playerView']"),
                    By.xpath("//*[@resource-id='com.cube.arc.fa:id/snippet_pager']" +
                            "//android.view.SurfaceView"),
                    By.xpath("//*[@resource-id='com.cube.arc.fa:id/snippet_pager']" +
                            "//*[contains(@resource-id,'video') or contains(@resource-id,'player')]")
            );
            tooltipLocators = List.of(
                    By.id("com.cube.arc.fa:id/tutorial_title"),
                    By.xpath("//*[contains(@text,'Swipe left and right')]")
            );
            doneButtonLocators = List.of(
                    By.id("com.cube.arc.fa:id/done_button"),
                    By.xpath("//*[@text='DONE']")
            );
        }
    }

    public boolean isDisplayed() {
        return isPresent(closeButtonLocators) && isPresent(snippetPagerLocators);
    }

    public boolean hasCloseButton() {
        return isPresent(closeButtonLocators);
    }

    public boolean hasSlideIndicator() {
        return isPresent(snippetIndicatorLocators);
    }

    public boolean hasLessonCard() {
        return isPresent(snippetPagerLocators);
    }

    public boolean isTooltipVisible() {
        return isPresent(tooltipLocators);
    }

    /**
     * Dismiss the first-time tooltip overlay by tapping anywhere outside it.
     * The overlay traps taps and consumes the next gesture, so a center-screen
     * tap is the canonical dismiss.
     */
    public void dismissTooltip() {
        org.openqa.selenium.Dimension dim = driver.manage().window().getSize();
        if (dim == null) return;
        int cx = dim.getWidth() / 2;
        int cy = dim.getHeight() / 2;
        try {
            // Platform-specific gesture name. clickGesture is Android-only —
            // calling it on iOS silently fails (or throws), leaving the
            // tooltip overlay on screen and blocking every subsequent
            // content read in the lesson (TC25085/86/87 root cause).
            String script = platform.equals("ios") ? "mobile: tap" : "mobile: clickGesture";
            driver.executeScript(script, Map.of("x", cx, "y", cy));
        } catch (Exception ignored) {
            // Best-effort — if the gesture fails, the tooltip-presence check
            // in the next call will surface the issue.
        }
    }

    public String getCurrentSectionHeading() {
        if (platform.equals("ios")) {
            List<WebElement> texts = iosOnScreenCardElements(sectionHeadingLocators);
            if (texts.isEmpty()) throw new RuntimeException("sectionHeadingLocators not visible");
            // Heading sits above paragraph/bullets; pick topmost (lowest y).
            WebElement top = texts.get(0);
            for (WebElement el : texts) {
                if (el.getRect().getY() < top.getRect().getY()) top = el;
            }
            String s = top.getAttribute("label");
            if (s == null || s.isBlank()) s = top.getText();
            return s == null ? "" : s.trim();
        }
        return getText(sectionHeadingLocators);
    }

    public String getCurrentParagraphDescription() {
        if (platform.equals("ios")) {
            List<WebElement> texts = iosOnScreenCardElements(paragraphDescriptionLocators);
            if (texts.size() < 2) return null;
            // Paragraph is the second-topmost StaticText in the card.
            texts.sort((a, b) -> Integer.compare(a.getRect().getY(), b.getRect().getY()));
            String s = texts.get(1).getAttribute("label");
            if (s == null || s.isBlank()) s = texts.get(1).getText();
            return s == null ? null : s.trim();
        }
        if (!isPresent(paragraphDescriptionLocators)) return null;
        return getText(paragraphDescriptionLocators);
    }

    /**
     * Returns text of every bullet currently rendered in the active card.
     * Empty list when the active card has no bullets.
     */
    public List<String> getCurrentBulletTexts() {
        List<String> out = new ArrayList<>();
        if (platform.equals("ios")) {
            // iOS: superset (heading + paragraph + bullets). Tests assert
            // expected bullet strings are present, so extra entries are fine.
            for (WebElement el : iosOnScreenCardElements(bulletItemLocators)) {
                try {
                    String t = el.getAttribute("label");
                    if (t == null || t.isBlank()) t = el.getText();
                    if (t != null && !t.isBlank()) out.add(t.trim());
                } catch (Exception ignored) {}
            }
            return out;
        }
        for (By by : bulletItemLocators) {
            for (WebElement el : driver.findElements(by)) {
                try {
                    String t = el.getText();
                    if (t != null && !t.isBlank()) out.add(t.trim());
                } catch (Exception ignored) {}
            }
            if (!out.isEmpty()) break;
        }
        return out;
    }

    public boolean currentCardHasImage() {
        if (platform.equals("ios")) {
            // iOS doesn't expose the lesson-card image in the accessibility
            // tree. Detect geometrically: on a no-image card the heading
            // sits near the top of the card (~17 px gap on staging build
            // 2484); on an image card the image fills the upper portion
            // and the heading is pushed several hundred pixels down.
            // Threshold: 25% of the card height — well above the no-image
            // gap and well below typical image height. Reads geometry
            // from element.getRect() so it stays device-agnostic.
            int cardTop = -1;
            int cardHeight = -1;
            for (WebElement sv : driver.findElements(snippetPagerLocators.get(0))) {
                try {
                    Rectangle r = sv.getRect();
                    int windowWidth;
                    try { windowWidth = driver.manage().window().getSize().getWidth(); }
                    catch (Exception ignored) { windowWidth = Integer.MAX_VALUE; }
                    int cx = r.getX() + r.getWidth() / 2;
                    if (cx < 0 || cx >= windowWidth) continue; // skip off-screen siblings
                    cardTop = r.getY();
                    cardHeight = r.getHeight();
                    break;
                } catch (Exception ignored) {}
            }
            if (cardTop < 0) return false;
            List<WebElement> texts = iosOnScreenCardElements(sectionHeadingLocators);
            if (texts.isEmpty()) return false;
            int minY = Integer.MAX_VALUE;
            for (WebElement el : texts) {
                int y = el.getRect().getY();
                if (y < minY) minY = y;
            }
            int gap = minY - cardTop;
            return gap > cardHeight / 4;
        }
        return isPresent(cardImageLocators);
    }

    public boolean currentCardHasVideo() {
        if (platform.equals("ios")) {
            boolean ok = !iosOnScreenCardElements(cardVideoLocators).isEmpty();
            if (!ok) dumpIosSource("hasVideo");
            return ok;
        }
        return isPresent(cardVideoLocators);
    }

    /**
     * iOS helper: collect every match for any of the supplied locators that
     * lies within the lesson card area on screen. The card area is bounded
     * laterally by the window width and vertically by the top chrome (the
     * close button + progress bar; any element with center-y above the
     * iconClose button's bottom edge is part of chrome, not the card).
     *
     * <p>Stays device-agnostic — geometry is read from the live window size
     * and from the iconClose button's actual rect, not hardcoded pixels.
     */
    /** Diagnostic: dump iOS page source to target/diagnostic/{tag}-{ts}.xml. */
    private void dumpIosSource(String tag) {
        try {
            String src = driver.getPageSource();
            java.nio.file.Path dir = java.nio.file.Paths.get("target", "diagnostic");
            java.nio.file.Files.createDirectories(dir);
            java.nio.file.Path out = dir.resolve(tag + "-" + System.currentTimeMillis() + ".xml");
            java.nio.file.Files.writeString(out, src);
            System.out.println("📝 Dumped iOS source: " + out.toAbsolutePath());
        } catch (Exception e) {
            System.out.println("⚠️ dumpIosSource(" + tag + ") failed: " + e.getMessage());
        }
    }

    private List<WebElement> iosOnScreenCardElements(List<By> locators) {
        int windowWidth = 0;
        int windowHeight = 0;
        try {
            windowWidth = driver.manage().window().getSize().getWidth();
            windowHeight = driver.manage().window().getSize().getHeight();
        } catch (Exception ignored) {}
        if (windowWidth <= 0) windowWidth = Integer.MAX_VALUE;
        if (windowHeight <= 0) windowHeight = Integer.MAX_VALUE;

        // Compute chrome bottom from the iconClose rect (=top-right close X).
        // Anything with center-y at or below this is inside the card body.
        int chromeBottom = 0;
        try {
            for (By by : closeButtonLocators) {
                List<WebElement> btns = driver.findElements(by);
                if (!btns.isEmpty()) {
                    Rectangle r = btns.get(0).getRect();
                    chromeBottom = r.getY() + r.getHeight();
                    break;
                }
            }
        } catch (Exception ignored) {}

        List<WebElement> out = new ArrayList<>();
        for (By by : locators) {
            for (WebElement el : driver.findElements(by)) {
                try {
                    Rectangle r = el.getRect();
                    int cx = r.getX() + r.getWidth() / 2;
                    int cy = r.getY() + r.getHeight() / 2;
                    if (cx < 0 || cx >= windowWidth) continue;
                    if (cy < chromeBottom || cy >= windowHeight) continue;
                    out.add(el);
                } catch (Exception ignored) {}
            }
        }
        return out;
    }

    /**
     * Number of segments in the progress bar (= number of cards in the lesson).
     */
    public int getProgressSegmentCount() {
        if (platform.equals("android")) {
            // Inner LinearLayout holds N sibling LinearLayouts, one per card.
            By segments = By.xpath("//*[@resource-id='com.cube.arc.fa:id/snippet_indicator']" +
                    "/android.widget.LinearLayout/android.widget.LinearLayout");
            return driver.findElements(segments).size();
        }
        // iOS: each segment is exposed as a Button labelled "Page N of TOTAL".
        // Parse TOTAL from the first match.
        java.util.List<WebElement> pages = driver.findElements(
                By.xpath("//XCUIElementTypeButton[starts-with(@name,'Page ') and contains(@name,' of ')]"));
        if (pages.isEmpty()) return 0;
        try {
            String label = pages.get(0).getAttribute("name"); // "Page 1 of 4"
            return Integer.parseInt(label.substring(label.lastIndexOf(' ') + 1));
        } catch (Exception ignored) {
            return pages.size();
        }
    }

    /**
     * Tap the Nth progress-bar segment (1-indexed). Used to jump to a
     * specific card without swiping through every preceding one.
     */
    public void tapProgressBarSegment(int index) {
        if (platform.equals("android")) {
            By target = By.xpath("(//*[@resource-id='com.cube.arc.fa:id/snippet_indicator']" +
                    "/android.widget.LinearLayout/android.widget.LinearLayout)[" + index + "]");
            // Segments are thin (~63px tall) and clickable=false in the dump
            // — tap their center via raw coords for reliability.
            WebElement seg = driver.findElement(target);
            Rectangle r = seg.getRect();
            int cx = r.getX() + r.getWidth() / 2;
            int cy = r.getY() + r.getHeight() / 2;
            driver.executeScript("mobile: clickGesture", Map.of("x", cx, "y", cy));
        } else {
            // iOS: each segment is its own Button "Page N of TOTAL". Find by
            // name and tap directly — no coord math needed (verified live).
            int total = getProgressSegmentCount();
            if (total <= 0) total = index;
            String label = "Page " + index + " of " + total;
            java.util.List<WebElement> btns = driver.findElements(By.name(label));
            if (!btns.isEmpty()) {
                btns.get(0).click();
                return;
            }
            // Fallback: predicate match if label format drifts.
            java.util.List<WebElement> any = driver.findElements(
                    io.appium.java_client.AppiumBy.iOSNsPredicateString(
                            "type == 'XCUIElementTypeButton' AND name BEGINSWITH 'Page " + index + " of '"));
            if (!any.isEmpty()) any.get(0).click();
        }
    }

    public boolean hasDoneButton() {
        return isPresent(doneButtonLocators);
    }

    public void tapDone() {
        if (platform.equals("ios")) {
            iosCoordTap(doneButtonLocators, "Done");
            return;
        }
        tap(doneButtonLocators);
    }

    public void tapClose() {
        if (platform.equals("ios")) {
            iosCoordTap(closeButtonLocators, "iconClose");
            return;
        }
        tap(closeButtonLocators);
    }

    /**
     * iOS-only fallback for buttons that XCUITest reports as present but not
     * "clickable" (the in-lesson chrome — iconClose and Done — fits this).
     * The element exists and is hittable; ExpectedConditions.elementToBeClickable
     * is over-conservative here, so we skip the wait and call click() directly
     * on the resolved WebElement. Picks the largest match by area so a hidden
     * mirror element doesn't win over the on-screen button. We deliberately do
     * NOT use mobile:tap at coords — XCUITest treats coord taps as raw touches
     * that the SwiftUI button handler sometimes ignores; element.click() goes
     * through the proper accessibility action and registers reliably.
     */
    private void iosCoordTap(List<By> locators, String labelForError) {
        int windowWidth = 10000;
        int windowHeight = 10000;
        try {
            windowWidth = driver.manage().window().getSize().getWidth();
            windowHeight = driver.manage().window().getSize().getHeight();
        } catch (Exception ignored) {}

        // iOS keeps mirror chrome (iconClose, Done) attached to off-screen
        // adjacent ScrollViews. Their rects sit outside the viewport. We must
        // pick the one whose center lies inside the window — otherwise the
        // click resolves on a mirror element and the on-screen control never
        // sees the gesture.
        WebElement target = null;
        for (By by : locators) {
            for (WebElement el : driver.findElements(by)) {
                try {
                    Rectangle r = el.getRect();
                    int cx = r.getX() + r.getWidth() / 2;
                    int cy = r.getY() + r.getHeight() / 2;
                    if (cx < 0 || cx >= windowWidth) continue;
                    if (cy < 0 || cy >= windowHeight) continue;
                    target = el;
                    break;
                } catch (Exception ignored) {}
            }
            if (target != null) break;
        }
        if (target == null) {
            throw new RuntimeException(labelForError + " not on-screen to tap");
        }
        // Resolve coords ourselves and dispatch via mobile: tap. We tried
        // element.click() — it routes through XCUITest's accessibility action,
        // which silently no-ops on the in-lesson chrome buttons (verified
        // empirically: the lesson stayed open). A coord-based mobile: tap on
        // the on-screen rect center delivers a real touch that the SwiftUI
        // button handler picks up. Coordinates come from element.getRect(),
        // so this stays device-agnostic — no hardcoded pixels.
        Rectangle r = target.getRect();
        int cx = r.getX() + r.getWidth() / 2;
        int cy = r.getY() + r.getHeight() / 2;
        driver.executeScript("mobile: tap", Map.of("x", cx, "y", cy));
    }
}
