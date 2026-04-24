package tests;

import com.cube.qa.framework.pages.home.LearnTabPage;
import com.cube.qa.framework.pages.home.LearnTabPage.CarouselCard;
import com.cube.qa.framework.pages.home.TabPage;
import com.cube.qa.framework.pages.home.TabPage.Tab;
import com.cube.qa.framework.pages.home.TopicDetailPage;
import com.cube.qa.framework.pages.onboarding.TooltipsPage;
import com.cube.qa.framework.testdata.loader.ContentBundleLoader;
import com.cube.qa.framework.testdata.model.LearnTopic;
import com.cube.qa.framework.utils.BaseTest;
import org.openqa.selenium.By;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Browse Topics carousel + pinning flows on the Learn tab.
 * Testiny: ARC First Aid - v4.0.0 > Learn Tab (With Microlearning) > Browse Topics
 *
 * <p>Every test starts from a fresh install (`fullReset=true`), walks the
 * guest onboarding, dismisses the home-screen tooltips, and scrolls the Learn
 * tab down to the Browse Topics section. Pin state and popular flags are
 * read off the live bundle — no hardcoded topic names — so the tests work
 * against any env.
 *
 * <p>TC31863 (empty-state carousel) is skipped: every env we test against
 * has at least one {@code isFeatured} topic, so the "no carousel" state
 * cannot be reached without content-side changes.
 */
public class BrowseTopicsTest extends BaseTest {

    private TabPage tabPage;
    private LearnTabPage learnTabPage;
    private TopicDetailPage topicDetailPage;
    private TooltipsPage tooltipsPage;

    @BeforeMethod(alwaysRun = true, dependsOnMethods = "setUp")
    public void setUpTest() {
        tabPage = pages.tabPage();
        learnTabPage = pages.learnTabPage();
        topicDetailPage = pages.topicDetailPage();
        tooltipsPage = pages.tooltipsPage();

        // Onboarding+scroll is occasionally flaky between tests — a stray
        // native dialog or a late-rendering Learn tab can leave us somewhere
        // that doesn't expose the Browse Topics header. Retry once with a
        // fresh app reset before giving up.
        RuntimeException lastFailure = null;
        for (int attempt = 0; attempt < 2; attempt++) {
            try {
                walkOnboardingAsGuest();
                int safety = 0;
                while (safety++ < 8 && tooltipsPage.isGotItButtonPresent()) {
                    try { tooltipsPage.tapGotIt(); } catch (RuntimeException e) { break; }
                }
                if (tabPage.isTabPresent(Tab.LEARN)) tabPage.tapTab(Tab.LEARN);
                learnTabPage.scrollToBrowseTopics();
                return;
            } catch (RuntimeException e) {
                lastFailure = e;
                log("⚠️ setUpTest attempt " + (attempt + 1) + " failed: " + e.getMessage()
                        + " — resetting app and retrying");
                resetAppState();
            }
        }
        throw lastFailure;
    }

    // ---- Bundle helpers ----------------------------------------------------

    private List<LearnTopic> featuredTopics() {
        return ContentBundleLoader.allTopics().stream()
                .filter(t -> t.isFeatured)
                .sorted(Comparator.comparing(LearnTopic::titleEn))
                .collect(Collectors.toList());
    }

    private List<LearnTopic> nonFeaturedTopicsAlphabetical() {
        return ContentBundleLoader.allTopics().stream()
                .filter(t -> !t.isFeatured)
                .sorted(Comparator.comparing(LearnTopic::titleEn))
                .collect(Collectors.toList());
    }

    // ---- Flow helpers ------------------------------------------------------

    /**
     * Tap the named topic in the carousel. Throws if the topic isn't in the
     * carousel — used for popular topics that are guaranteed to be visible.
     */
    private void openCarouselTopic(String name) {
        learnTabPage.tapCarouselCard(name);
        waitForTopicDetail();
    }

    /**
     * Tap the named topic wherever it appears on the Learn tab — carousel if
     * popular/pinned, otherwise the Browse Topics list below. Necessary for
     * reaching non-featured topics to pin them.
     */
    private void openAnyTopic(String name) {
        By locator = isAndroid()
                ? By.xpath("//*[@resource-id='com.cube.arc.fa:id/chevron_link_title' and @text='" + name + "']")
                : By.name(name);
        if (driver.findElements(locator).isEmpty()) {
            // Scroll further if it's below the current viewport.
            for (int i = 0; i < 6 && driver.findElements(locator).isEmpty(); i++) {
                if (isAndroid()) {
                    driver.executeScript("mobile: scrollGesture", java.util.Map.of(
                            "left", 100, "top", 500, "width", 800, "height", 1200,
                            "direction", "down", "percent", 0.7));
                } else {
                    driver.executeScript("mobile: swipe", java.util.Map.of("direction", "up"));
                }
                try { Thread.sleep(600); } catch (InterruptedException ignored) {}
            }
        }
        driver.findElement(locator).click();
        waitForTopicDetail();
    }

    /** Wait until the topic detail toolbar (with the pin button) is rendered. */
    private void waitForTopicDetail() {
        // The pin tooltip overlay may be in front of the detail screen on
        // first visit — isPinTooltipPresent covers that; isPinButtonVisible
        // covers the normal case. One or the other implies we've left the
        // Learn tab.
        long deadline = System.currentTimeMillis() + 10_000;
        while (System.currentTimeMillis() < deadline) {
            if (topicDetailPage.isPinTooltipPresent()
                    || !driver.findElements(byPinButton()).isEmpty()) {
                return;
            }
            try { Thread.sleep(250); } catch (InterruptedException ignored) {}
        }
        throw new RuntimeException("Topic Detail page never appeared");
    }

    private By byPinButton() {
        return isAndroid()
                ? By.xpath("//*[@resource-id='com.cube.arc.fa:id/action_pin_button' or " +
                        "@resource-id='com.cube.arc.fa:id/action_unpin_button']")
                : By.xpath("//XCUIElementTypeButton[@name='Topic Pinned' or @name='Topic Unpinned']");
    }

    /** Dismiss the first-visit pin tooltip if it's blocking the pin button. */
    private void dismissPinTooltipIfPresent() {
        if (topicDetailPage.isPinTooltipPresent()) {
            try { tooltipsPage.tapGotIt(); } catch (RuntimeException ignored) {}
        }
    }

    /**
     * Open a topic, ensure it ends up pinned, and return to the Learn tab.
     * Handles the first-visit tooltip. Used by TC31866, TC31867, TC31871.
     */
    private void pinTopic(String name) {
        openAnyTopic(name);
        dismissPinTooltipIfPresent();
        if (!topicDetailPage.isPinned()) {
            topicDetailPage.tapPin();
        }
        // Give the pin state a moment to flip + the toast to appear/dismiss.
        try { Thread.sleep(1500); } catch (InterruptedException ignored) {}
        Assert.assertTrue(topicDetailPage.isPinned(),
                "Expected '" + name + "' to be pinned after tapPin()");
        topicDetailPage.tapBack();
        // scrollToBrowseTopics handles the "header already scrolled past"
        // case by first flinging back to the top, so no manual reset needed.
        learnTabPage.scrollToBrowseTopics();
    }

    private List<String> names(List<CarouselCard> cards) {
        List<String> out = new ArrayList<>();
        for (CarouselCard c : cards) out.add(c.name);
        return out;
    }

    // ========================================================================
    // TC25101 — Pin tooltip on first topic visit (fresh install)
    // ========================================================================
    @Test(description = "TC25101 - Verify pin tooltip displays", groups = {"regression"})
    public void TC25101() {
        // Any carousel card will do — the tooltip is tied to first-ever topic
        // view after fresh install, not to a specific topic.
        String firstPopular = featuredTopics().get(0).titleEn();
        openCarouselTopic(firstPopular);

        Assert.assertTrue(topicDetailPage.isPinTooltipVisible(),
                "Pin tooltip should display on first topic view after fresh install");
        Assert.assertTrue(topicDetailPage.getPinTooltipSubtitle().toLowerCase().contains("pin"),
                "Pin tooltip subtitle should describe the pin feature");

        log("✅ TC25101: Pin tooltip 'You can now pin content' displayed");
    }

    // ========================================================================
    // TC25102 — Pin a topic → toast + icon flips to pinned state
    // ========================================================================
    @Test(description = "TC25102 - Verify users can pin a topic", groups = {"regression"})
    public void TC25102() {
        String target = featuredTopics().get(0).titleEn();
        openCarouselTopic(target);
        dismissPinTooltipIfPresent();

        Assert.assertTrue(topicDetailPage.isUnpinned(),
                "Topic should start unpinned on first visit");

        topicDetailPage.tapPin();
        // Toast is short-lived — check it quickly but don't fail the test if
        // we missed it (state flip is the strict assertion).
        boolean toastSeen = false;
        long deadline = System.currentTimeMillis() + 2500;
        while (!toastSeen && System.currentTimeMillis() < deadline) {
            toastSeen = topicDetailPage.isToastPresent("Topic Pinned");
            if (!toastSeen) try { Thread.sleep(150); } catch (InterruptedException ignored) {}
        }

        Assert.assertTrue(topicDetailPage.isPinned(),
                "Pin button should flip to the 'pinned' state after tap");
        if (toastSeen) {
            log("✅ TC25102: 'Topic Pinned' toast seen + pin state flipped");
        } else {
            log("⚠️ TC25102: toast not observed within 2.5s (timing-sensitive); " +
                    "pin state flipped correctly");
        }
    }

    // ========================================================================
    // TC25103 — Unpin a topic → toast + icon flips back + no longer pinned
    // ========================================================================
    @Test(description = "TC25103 - Verify users can unpin a topic", groups = {"regression"})
    public void TC25103() {
        String target = nonFeaturedTopicsAlphabetical().get(0).titleEn();

        // Pin first, then unpin from the same screen.
        openAnyTopic(target);
        dismissPinTooltipIfPresent();
        topicDetailPage.tapPin();
        try { Thread.sleep(1200); } catch (InterruptedException ignored) {}
        Assert.assertTrue(topicDetailPage.isPinned(), "Setup: should be pinned");

        topicDetailPage.tapPin();  // unpin
        boolean toastSeen = false;
        long deadline = System.currentTimeMillis() + 2500;
        while (!toastSeen && System.currentTimeMillis() < deadline) {
            toastSeen = topicDetailPage.isToastPresent("Topic Unpinned");
            if (!toastSeen) try { Thread.sleep(150); } catch (InterruptedException ignored) {}
        }

        Assert.assertTrue(topicDetailPage.isUnpinned(),
                "Pin button should flip to the 'unpinned' state after tapping again");

        // Bounce back to Learn tab and confirm the previously-pinned topic is
        // no longer in the Pinned section of the carousel.
        topicDetailPage.tapBack();
        learnTabPage.scrollToBrowseTopics();

        List<CarouselCard> cards = learnTabPage.getCarouselCards();
        boolean stillPinned = cards.stream()
                .anyMatch(c -> c.name.equals(target) && "Pinned".equals(c.tag));
        Assert.assertFalse(stillPinned,
                "'" + target + "' should no longer carry the 'Pinned' tag in the carousel");

        if (toastSeen) {
            log("✅ TC25103: 'Topic Unpinned' toast seen, state reverted, topic dropped from Pinned group");
        } else {
            log("⚠️ TC25103: toast not observed within 2.5s; other assertions held");
        }
    }

    // ========================================================================
    // TC31864 — Carousel card shows icon + name + tag
    // ========================================================================
    @Test(description = "TC31864 - Validate topic card contents in carousel", groups = {"regression"})
    public void TC31864() {
        List<CarouselCard> cards = learnTabPage.getCarouselCards();
        Assert.assertFalse(cards.isEmpty(),
                "Carousel should have at least one card when popular topics exist");

        for (CarouselCard card : cards) {
            Assert.assertNotNull(card.name, "Card should have a topic name");
            Assert.assertFalse(card.name.isBlank(), "Card topic name should be non-empty");
            Assert.assertTrue(
                    "Popular".equals(card.tag) || "Pinned".equals(card.tag),
                    "Card tag should be 'Popular' or 'Pinned' (got '" + card.tag + "')");
        }
        log("✅ TC31864: All " + cards.size() + " cards have name + valid tag (" + cards + ")");
    }

    // ========================================================================
    // TC31865 — Popular topics appear in the carousel
    // ========================================================================
    @Test(description = "TC31865 - Validate popular topics display in carousel", groups = {"regression"})
    public void TC31865() {
        List<String> featured = featuredTopics().stream()
                .map(LearnTopic::titleEn).collect(Collectors.toList());
        Assert.assertFalse(featured.isEmpty(),
                "Bundle precondition: at least one isFeatured topic must exist");

        List<CarouselCard> cards = learnTabPage.getCarouselCards();
        List<String> popularNames = cards.stream()
                .filter(c -> "Popular".equals(c.tag))
                .map(c -> c.name).collect(Collectors.toList());

        Assert.assertEquals(popularNames, featured,
                "Carousel's 'Popular' cards should match the bundle's isFeatured topics, alphabetically");
        log("✅ TC31865: Popular carousel matches bundle: " + popularNames);
    }

    // ========================================================================
    // TC31866 — Pinned first (alphabetical), then popular (alphabetical)
    // ========================================================================
    @Test(description = "TC31866 - Validate pinned topics appear first in carousel", groups = {"regression"})
    public void TC31866() {
        // Pin the two alphabetically-first non-featured topics so there's no
        // overlap with the Popular set — that way the expected ordering is
        // unambiguous: [pinned A, pinned B, popular1, popular2, ...].
        List<LearnTopic> nonFeat = nonFeaturedTopicsAlphabetical();
        Assert.assertTrue(nonFeat.size() >= 2,
                "Bundle precondition: need at least 2 non-featured topics to pin");
        String pin1 = nonFeat.get(0).titleEn();
        String pin2 = nonFeat.get(1).titleEn();

        pinTopic(pin1);
        pinTopic(pin2);

        // Expected carousel order: pinned topics alpha, then popular topics alpha.
        List<String> expected = new ArrayList<>();
        expected.add(pin1);
        expected.add(pin2);
        for (LearnTopic t : featuredTopics()) expected.add(t.titleEn());

        List<CarouselCard> cards = learnTabPage.getCarouselCards();
        List<String> actual = names(cards);

        log("📋 Expected order: " + expected);
        log("📋 Actual order:   " + actual);
        Assert.assertEquals(actual, expected,
                "Carousel order should be [pinned alphabetical] + [popular alphabetical]");
        // Also sanity-check the tag placement.
        Assert.assertEquals(cards.get(0).tag, "Pinned");
        Assert.assertEquals(cards.get(1).tag, "Pinned");
        Assert.assertEquals(cards.get(2).tag, "Popular");

        log("✅ TC31866: Carousel ordered pinned-first, alphabetically within each group");
    }

    // ========================================================================
    // TC31867 — Pinned topic displays pin tag in the carousel
    // ========================================================================
    @Test(description = "TC31867 - Validate pinned topic icon/tag in carousel", groups = {"regression"})
    public void TC31867() {
        String target = nonFeaturedTopicsAlphabetical().get(0).titleEn();
        pinTopic(target);

        List<CarouselCard> cards = learnTabPage.getCarouselCards();
        CarouselCard pinnedCard = cards.stream()
                .filter(c -> c.name.equals(target))
                .findFirst().orElse(null);

        Assert.assertNotNull(pinnedCard,
                "'" + target + "' should appear in carousel after being pinned");
        Assert.assertEquals(pinnedCard.tag, "Pinned",
                "Pinned topic's carousel tag should be 'Pinned'");
        log("✅ TC31867: Pinned topic '" + target + "' shows 'Pinned' tag in carousel");
    }

    // ========================================================================
    // TC31868 — Tracker reads "0 / X completed" on fresh install
    // ========================================================================
    @Test(description = "TC31868 - Validate topic completion tracker - no completions", groups = {"regression"})
    public void TC31868() {
        int totalTopics = ContentBundleLoader.allTopics().size();
        String text = learnTabPage.getProgressTrackerText();
        log("📋 Tracker text: '" + text + "' (bundle total=" + totalTopics + ")");

        // Android: "0/21 completed" — iOS: "0 out of 21completed" (no space).
        // Strip non-digits, keep the first pair.
        int[] nums = extractFirstTwoInts(text);
        Assert.assertEquals(nums[0], 0, "Completion count should be 0 on fresh install");
        Assert.assertEquals(nums[1], totalTopics,
                "Total in tracker should equal bundle topic count");
        log("✅ TC31868: Tracker reads 0/" + totalTopics);
    }

    private int[] extractFirstTwoInts(String s) {
        List<Integer> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        for (char c : s.toCharArray()) {
            if (Character.isDigit(c)) {
                cur.append(c);
            } else if (cur.length() > 0) {
                out.add(Integer.parseInt(cur.toString()));
                cur.setLength(0);
            }
        }
        if (cur.length() > 0) out.add(Integer.parseInt(cur.toString()));
        if (out.size() < 2) throw new RuntimeException("Tracker text missing two numbers: '" + s + "'");
        return new int[] { out.get(0), out.get(1) };
    }

    // ========================================================================
    // TC31871 — Pinned tag overrides Popular when a topic is both
    // ========================================================================
    @Test(description = "TC31871 - Validate pinned tag overrides popular tag", groups = {"regression"})
    public void TC31871() {
        String target = featuredTopics().get(0).titleEn();  // is popular + pinnable
        pinTopic(target);

        List<CarouselCard> cards = learnTabPage.getCarouselCards();
        CarouselCard card = cards.stream()
                .filter(c -> c.name.equals(target))
                .findFirst().orElse(null);

        Assert.assertNotNull(card, "Pinned popular topic should still be in carousel");
        Assert.assertEquals(card.tag, "Pinned",
                "Topic that's both pinned AND popular should show ONLY the 'Pinned' tag");

        // The topic should not also appear as a separate 'Popular' card.
        long popularHits = cards.stream()
                .filter(c -> c.name.equals(target) && "Popular".equals(c.tag))
                .count();
        Assert.assertEquals(popularHits, 0,
                "Topic should not appear with a 'Popular' tag while also pinned");
        log("✅ TC31871: Popular+pinned topic '" + target + "' shows only 'Pinned' tag");
    }
}
