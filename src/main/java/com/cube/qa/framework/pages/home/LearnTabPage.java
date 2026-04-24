package com.cube.qa.framework.pages.home;

import com.cube.qa.framework.pages.BasePage;
import io.appium.java_client.AppiumDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Learn tab — the first tab after onboarding. Exposes the "Personalize
 * Experience" CTA plus the Browse Topics carousel and progress tracker.
 */
public class LearnTabPage extends BasePage {

    /** A single card in the Browse Topics carousel: topic name + tag ("Popular"/"Pinned"). */
    public static final class CarouselCard {
        public final String name;
        public final String tag;
        public CarouselCard(String name, String tag) { this.name = name; this.tag = tag; }
        @Override public String toString() { return name + " [" + tag + "]"; }
    }

    private final String platform;

    private final List<By> personalizeCtaLocators;
    private final List<By> emptyStateTitleLocators;
    private final List<By> emptyStateSubtitleLocators;
    private final List<By> browseTopicsHeaderLocators;
    private final List<By> progressTrackerLocators;

    // iOS-only carousel button label parser.
    private static final Pattern IOS_CARD_NAME =
            Pattern.compile("^Card\\s+\\d+\\s+of\\s+\\d+,\\s*(.+?),\\s*(popular|pinned)$",
                    Pattern.CASE_INSENSITIVE);

    public LearnTabPage(AppiumDriver driver, String platform) {
        super(driver);
        this.platform = platform.toLowerCase();

        if (platform.equalsIgnoreCase("ios")) {
            // iOS labels the button with title-cased or all-caps text depending
            // on env (matches the pattern we hit on the tab bar).
            personalizeCtaLocators = List.of(
                    By.name("PERSONALIZE EXPERIENCE"),
                    By.name("Personalize Experience"),
                    By.xpath("//XCUIElementTypeButton[@label='PERSONALIZE EXPERIENCE' or @label='Personalize Experience']")
            );
            emptyStateTitleLocators = List.of(
                    By.name("Personalize Your Experience")
            );
            emptyStateSubtitleLocators = List.of(
                    By.name("Based on your interests, we’ll personalize your dashboard!")
            );
            browseTopicsHeaderLocators = List.of(
                    By.name("Browse Topics")
            );
            // iOS renders the tracker label without a space (e.g. "0 out of 21completed").
            progressTrackerLocators = List.of(
                    By.xpath("//XCUIElementTypeStaticText[contains(@name,'out of') and contains(@name,'completed')]")
            );
        } else {
            // Android: the Personalize card on the Learn tab wraps the button
            // in take_a_class_container → arc_button. Scope the xpath so we
            // don't collide with other arc_button buttons on the page.
            personalizeCtaLocators = List.of(
                    By.xpath("//*[@resource-id='com.cube.arc.fa:id/take_a_class_container']//android.widget.Button"),
                    By.xpath("//*[@text='PERSONALIZE EXPERIENCE']")
            );
            // Title + subheading on the empty-state card (before the user has
            // personalized). Resource-ids are `take_class_*` (the card is
            // shared with the "take a class" layout).
            emptyStateTitleLocators = List.of(
                    By.id("com.cube.arc.fa:id/take_class_title")
            );
            emptyStateSubtitleLocators = List.of(
                    By.id("com.cube.arc.fa:id/take_class_subtitle")
            );
            browseTopicsHeaderLocators = List.of(
                    By.xpath("//*[@resource-id='com.cube.arc.fa:id/text_title' and @text='Browse Topics']")
            );
            progressTrackerLocators = List.of(
                    By.id("com.cube.arc.fa:id/text_progress")
            );
        }
    }

    public boolean isPersonalizeCtaVisible() {
        return isVisible(personalizeCtaLocators);
    }

    /**
     * Non-throwing presence check — use when you need a quick "is the CTA on
     * screen" answer without paying the 30s wait that {@link #isPersonalizeCtaVisible()}
     * incurs when it's not. Good for setup branches / polling.
     */
    public boolean isPersonalizeCtaPresent() {
        return isPresent(personalizeCtaLocators);
    }

    public void tapPersonalizeCta() {
        tap(personalizeCtaLocators);
    }

    public boolean isEmptyStateTitleVisible() {
        return isVisible(emptyStateTitleLocators);
    }

    public boolean isEmptyStateSubtitleVisible() {
        return isVisible(emptyStateSubtitleLocators);
    }

    // ---- Browse Topics section ---------------------------------------------

    /**
     * Scroll down a few times until the Browse Topics header comes into view.
     * The Learn tab opens at the Personalize-Experience card, so every Browse
     * Topics test needs to scroll past it first. Idempotent — returns early if
     * the header is already visible.
     */
    public void scrollToBrowseTopics() {
        // Learn tab may be anywhere — initial render (at the Personalize card)
        // or returning from a topic detail (scrolled halfway down). If the
        // header is missing we first scroll up to the very top, then scroll
        // back down until it appears. Belt-and-braces for the round-trip flow
        // in pin/unpin tests.
        if (!isPresent(browseTopicsHeaderLocators)) {
            scrollToTop();
        }
        for (int i = 0; i < 6; i++) {
            if (isPresent(browseTopicsHeaderLocators)) break;
            scrollPageDown();
            try { Thread.sleep(600); } catch (InterruptedException ignored) {}
        }
        if (!isPresent(browseTopicsHeaderLocators)) {
            throw new RuntimeException("Browse Topics header not found after scrolling");
        }
        // Header is on-screen but the carousel cards sit below it — keep
        // scrolling until at least one card title is rendered, so downstream
        // helpers see populated cards rather than skeletons.
        for (int i = 0; i < 4; i++) {
            if (isCarouselPopulated()) return;
            scrollPageDown();
            try { Thread.sleep(600); } catch (InterruptedException ignored) {}
        }
    }

    private void scrollToTop() {
        // Fling up repeatedly until the Personalize card (top of the Learn
        // tab) is back in view, or we've clearly bottomed-out on the top.
        for (int i = 0; i < 6; i++) {
            if (isPresent(personalizeCtaLocators)) return;
            if (platform.equals("ios")) {
                driver.executeScript("mobile: swipe", Map.of("direction", "down"));
            } else {
                driver.executeScript("mobile: scrollGesture", Map.of(
                        "left", 100, "top", 500, "width", 800, "height", 1200,
                        "direction", "up", "percent", 0.9));
            }
            try { Thread.sleep(500); } catch (InterruptedException ignored) {}
        }
    }

    private void scrollPageDown() {
        if (platform.equals("ios")) {
            driver.executeScript("mobile: swipe", Map.of("direction", "up"));
        } else {
            driver.executeScript("mobile: scrollGesture", Map.of(
                    "left", 100, "top", 500, "width", 800, "height", 1200,
                    "direction", "down", "percent", 0.7));
        }
    }

    private boolean isCarouselPopulated() {
        By probe = platform.equals("ios")
                ? By.xpath("//XCUIElementTypeButton[starts-with(@name,'Card ') and " +
                        "(contains(@name,', popular') or contains(@name,', pinned'))]")
                : By.id("com.cube.arc.fa:id/chevron_link_title");
        return !driver.findElements(probe).isEmpty();
    }

    public boolean isBrowseTopicsHeaderVisible() {
        return isVisible(browseTopicsHeaderLocators);
    }

    /**
     * Raw progress-tracker label (e.g. "0/21 completed" on Android or
     * "0 out of 21completed" on iOS). The two platforms render different copy;
     * tests should parse the digits rather than compare strings verbatim.
     */
    public String getProgressTrackerText() {
        WebElement el = waitForVisibility(progressTrackerLocators);
        String text = platform.equals("ios") ? el.getAttribute("name") : el.getText();
        return text == null ? "" : text.trim();
    }

    /**
     * Cards currently in the Browse Topics carousel, in render order. Each
     * card carries a topic name and a tag — "Popular" for {@code isFeatured}
     * topics and "Pinned" for user-pinned ones. Pinned cards appear first;
     * order within a group is alphabetical (spec TC31866).
     */
    public List<CarouselCard> getCarouselCards() {
        // iOS (XCUITest) exposes the entire UICollectionView in the a11y tree
        // regardless of visibility, so a single DOM read returns every
        // carousel card. No swipe needed.
        if (platform.equals("ios")) {
            Map<String, CarouselCard> ordered = new LinkedHashMap<>();
            for (CarouselCard c : iosCarouselCards()) ordered.putIfAbsent(c.name, c);
            return new ArrayList<>(ordered.values());
        }
        // Android RecyclerView virtualises off-screen cells — a single DOM
        // read only surfaces the two cards currently on-screen. Sweep the
        // carousel horizontally, deduping by name, until two consecutive
        // reads add nothing new.
        //
        // Rewind the carousel to the left edge first. After a pin/unpin
        // round-trip the carousel can reopen scrolled to the Popular section,
        // hiding Pinned cards on the left. Over-swiping at the edge is a
        // harmless no-op.
        for (int i = 0; i < 6; i++) {
            swipeCarouselRight();
            try { Thread.sleep(250); } catch (InterruptedException ignored) {}
        }
        Map<String, CarouselCard> seen = new LinkedHashMap<>();
        int noGrowStreak = 0;
        for (int pass = 0; pass < 10; pass++) {
            List<CarouselCard> batch = androidCarouselCards();
            int before = seen.size();
            for (CarouselCard c : batch) seen.putIfAbsent(c.name, c);
            if (seen.size() == before) {
                noGrowStreak++;
                if (noGrowStreak >= 2) break;
            } else {
                noGrowStreak = 0;
            }
            swipeCarouselLeft();
            try { Thread.sleep(800); } catch (InterruptedException ignored) {}
        }
        // Swipe back to the start so subsequent taps see the first card.
        for (int i = 0; i < 4; i++) {
            swipeCarouselRight();
            try { Thread.sleep(250); } catch (InterruptedException ignored) {}
        }
        return new ArrayList<>(seen.values());
    }

    // Android-only: iOS getCarouselCards() reads the full a11y tree in one
    // shot, so no swipe is needed over there.
    private void swipeCarouselLeft() {
        int y = carouselRowY();
        adbSwipe(900, y, 120, y, 300);
    }

    private void swipeCarouselRight() {
        int y = carouselRowY();
        adbSwipe(120, y, 900, y, 300);
    }

    /**
     * Run `adb shell input swipe` against the connected device. We fall back
     * to adb because {@code mobile: dragGesture} from Selenium Java silently
     * no-ops on horizontal flings over the Browse Topics carousel, despite
     * the same call succeeding via REST. ADB is the OS-level gesture source —
     * it always lands. The device UDID is pinned via -s so parallel devices
     * don't get confused.
     */
    private void adbSwipe(int x1, int y1, int x2, int y2, int durationMs) {
        String udid = System.getenv().getOrDefault("ANDROID_UDID", "33071FDH2007QH");
        try {
            Process p = new ProcessBuilder(
                    "adb", "-s", udid, "shell", "input", "swipe",
                    String.valueOf(x1), String.valueOf(y1),
                    String.valueOf(x2), String.valueOf(y2),
                    String.valueOf(durationMs))
                    .redirectErrorStream(true).start();
            p.waitFor();
        } catch (Exception e) {
            throw new RuntimeException("adb swipe failed", e);
        }
    }

    /**
     * Pixel-center Y of a currently visible carousel card. Carousel titles
     * render taller (h≈121) than the vertical-list rows below them (h≈41–61)
     * because the carousel card stretches the title block. Height is the
     * cleanest discriminator — y is not, since the carousel can scroll. Falls
     * back to 1334 (empirical row center on Pixel 7) if no title is found.
     */
    private int carouselRowY() {
        List<WebElement> titles = driver.findElements(
                By.id("com.cube.arc.fa:id/chevron_link_title"));
        for (WebElement t : titles) {
            org.openqa.selenium.Rectangle r = t.getRect();
            if (r.height > 80) return r.y + r.height / 2;
        }
        return 1334;
    }

    /** Tap a carousel card by its topic name. Throws if not present. */
    public void tapCarouselCard(String topicName) {
        if (platform.equals("ios")) {
            // iOS button name is "Card N of M, TopicName, popular".
            By card = By.xpath("//XCUIElementTypeButton[starts-with(@name,'Card ') and " +
                    "contains(@name,', " + topicName + ",')]");
            tap(List.of(card));
            return;
        }
        // Android: tap the title TextView — the parent CardView handles the click.
        By card = By.xpath(
                "//*[@resource-id='com.cube.arc.fa:id/chevron_link_title' and @text='" + topicName + "']");
        tap(List.of(card));
    }

    // ---- Internal helpers --------------------------------------------------

    private List<CarouselCard> iosCarouselCards() {
        List<CarouselCard> out = new ArrayList<>();
        List<WebElement> buttons = driver.findElements(By.xpath(
                "//XCUIElementTypeButton[starts-with(@name,'Card ') and " +
                "(contains(@name,', popular') or contains(@name,', pinned'))]"));
        for (WebElement b : buttons) {
            String name = b.getAttribute("name");
            if (name == null) continue;
            Matcher m = IOS_CARD_NAME.matcher(name);
            if (!m.matches()) continue;
            String tag = m.group(2).equalsIgnoreCase("pinned") ? "Pinned" : "Popular";
            out.add(new CarouselCard(m.group(1).trim(), tag));
        }
        return out;
    }

    private List<CarouselCard> androidCarouselCards() {
        // Carousel cards live under the recyclerView that sits directly under
        // the Browse Topics header, and each one carries both a title and a
        // tag (Popular/Pinned). The vertical list below has no text_tag, so
        // requiring both ids excludes it cleanly.
        List<WebElement> cardTitles = driver.findElements(By.xpath(
                "//androidx.recyclerview.widget.RecyclerView" +
                "//androidx.cardview.widget.CardView" +
                "[.//*[@resource-id='com.cube.arc.fa:id/chevron_link_title']" +
                " and .//*[@resource-id='com.cube.arc.fa:id/text_tag']]"));

        // Use a LinkedHashMap to dedupe in case the RecyclerView virtualisation
        // happens to surface a card twice during swipes.
        Map<String, CarouselCard> ordered = new LinkedHashMap<>();
        for (WebElement card : cardTitles) {
            String name = firstChildText(card, "com.cube.arc.fa:id/chevron_link_title");
            String tag  = firstChildText(card, "com.cube.arc.fa:id/text_tag");
            if (name == null || name.isBlank()) continue;
            ordered.putIfAbsent(name, new CarouselCard(name, tag));
        }
        return new ArrayList<>(ordered.values());
    }

    private String firstChildText(WebElement parent, String resourceId) {
        List<WebElement> matches = parent.findElements(
                By.xpath(".//*[@resource-id='" + resourceId + "']"));
        if (matches.isEmpty()) return null;
        String t = matches.get(0).getText();
        return t == null ? null : t.trim();
    }
}
