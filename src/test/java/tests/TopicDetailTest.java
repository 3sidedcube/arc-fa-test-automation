package tests;

import com.cube.qa.framework.pages.home.LearnTabPage;
import com.cube.qa.framework.pages.home.TabPage;
import com.cube.qa.framework.pages.home.TabPage.Tab;
import com.cube.qa.framework.pages.home.TopicDetailPage;
import com.cube.qa.framework.pages.onboarding.TooltipsPage;
import com.cube.qa.framework.testdata.loader.ContentBundleLoader;
import com.cube.qa.framework.testdata.model.Faq;
import com.cube.qa.framework.testdata.model.LearnTopic;
import com.cube.qa.framework.testdata.model.LearnTopicDetail;
import com.cube.qa.framework.utils.BaseTest;
import org.openqa.selenium.By;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Topic Detail screen tests.
 * Testiny: ARC First Aid - v4.0.0 > Learn Tab (With Microlearning) > Topic Detail / FAQs
 *
 * <p>Topics are picked from the live CDN content bundle so the same test
 * code works against any environment. The Topic Detail page renders six
 * sections (title, hero image, Overview, Lessons, Related Articles, FAQs);
 * not every topic has every section, so picker helpers below filter the
 * bundle for a topic that satisfies each test's preconditions.
 *
 * <p>FAQs expand inline on tap rather than opening a separate detail screen,
 * so TC25099 verifies the answer text appears within the same view after
 * tapping a question — there's no FaqDetailPage.
 */
public class TopicDetailTest extends BaseTest {

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

        // Same retry pattern as BrowseTopicsTest — onboarding + Learn-tab
        // landing is the slowest/most flaky stretch, so a single resetAppState
        // retry catches transient native dialogs without masking real
        // failures.
        RuntimeException lastFailure = null;
        for (int attempt = 0; attempt < 3; attempt++) {
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

    // ---- Bundle pickers ----------------------------------------------------

    /** Topic with all four optional sections populated — used by TC25075. */
    private LearnTopicDetail topicWithFullDisplay() {
        for (LearnTopic t : ContentBundleLoader.allTopics()) {
            LearnTopicDetail d = ContentBundleLoader.topicDetail(t.id);
            if (!d.lessons.isEmpty()
                    && !d.relatedArticleIds.isEmpty()
                    && !d.faqs.isEmpty()) {
                return d;
            }
        }
        throw new RuntimeException(
                "No topic in bundle has all of {lessons, related articles, FAQs}");
    }

    /**
     * A reliably-reachable topic — used by TC25076 (back nav doesn't care
     * about contents, just needs the topic to open). Picks the first
     * featured topic so it's always at the top of the Browse Topics
     * carousel, with no scrolling required.
     */
    private LearnTopicDetail anyTopic() {
        for (LearnTopic t : ContentBundleLoader.allTopics()) {
            if (t.isFeatured) return ContentBundleLoader.topicDetail(t.id);
        }
        // Fallback: bundle has no featured topics (unexpected) — first listed.
        LearnTopic t = ContentBundleLoader.allTopics().iterator().next();
        return ContentBundleLoader.topicDetail(t.id);
    }

    /** First topic with at least one FAQ — used by TC25098/TC25099. */
    private LearnTopicDetail topicWithFaqs() {
        for (LearnTopic t : ContentBundleLoader.allTopics()) {
            LearnTopicDetail d = ContentBundleLoader.topicDetail(t.id);
            if (!d.faqs.isEmpty()) return d;
        }
        throw new RuntimeException("No topic in bundle has FAQs");
    }

    /**
     * First topic with zero FAQs — used by TC25100. May not exist in every
     * env (every staging topic currently has FAQs); callers should
     * SkipException when null.
     */
    private LearnTopicDetail topicWithoutFaqs() {
        for (LearnTopic t : ContentBundleLoader.allTopics()) {
            LearnTopicDetail d = ContentBundleLoader.topicDetail(t.id);
            if (d.faqs == null || d.faqs.isEmpty()) return d;
        }
        return null;
    }

    // ---- Flow helpers ------------------------------------------------------

    /**
     * Tap the topic with the given title from the Learn tab — carousel if
     * featured, vertical Browse Topics list otherwise. Mirrors
     * BrowseTopicsTest.openAnyTopic so iOS/Android both reach the detail
     * screen reliably.
     */
    private void openTopic(String title) {
        By locator = isAndroid()
                ? By.xpath("//*[@resource-id='com.cube.arc.fa:id/chevron_link_title' and @text='" + title + "']")
                : By.name(title);
        if (driver.findElements(locator).isEmpty()) {
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
        if (driver.findElements(locator).isEmpty()) {
            // Fall back to carousel tap (handles popular topics that don't
            // appear in the vertical list at this scroll position).
            learnTabPage.tapCarouselCard(title);
        } else {
            driver.findElement(locator).click();
        }
        waitForTopicDetail();
        // First-visit tooltip would block all our presence checks.
        if (topicDetailPage.isPinTooltipPresent()) {
            try { tooltipsPage.tapGotIt(); } catch (RuntimeException ignored) {}
        }
    }

    private void waitForTopicDetail() {
        long deadline = System.currentTimeMillis() + 10_000;
        while (System.currentTimeMillis() < deadline) {
            if (topicDetailPage.isPinTooltipPresent()
                    || topicDetailPage.isPinButtonVisible()) {
                return;
            }
            try { Thread.sleep(250); } catch (InterruptedException ignored) {}
        }
        throw new RuntimeException("Topic Detail page never appeared");
    }

    /**
     * Walk the FAQ list, scrolling between snapshots, ticking off each
     * expected question as it appears in the a11y tree. Driving the scroll
     * loop with the bundle's known question set is more reliable than
     * "harvest everything that looks like a question" — especially on iOS,
     * where FAQ rows may surface as buttons/cells without a tidy
     * StaticText-per-question shape.
     */
    private Set<String> findAllFaqQuestions(List<String> expected) {
        topicDetailPage.scrollToFaqsSection();
        Set<String> seen = new LinkedHashSet<>();
        int stableScrolls = 0;
        for (int i = 0; i < 30 && seen.size() < expected.size() && stableScrolls < 3; i++) {
            int before = seen.size();
            for (String q : expected) {
                if (!seen.contains(q) && topicDetailPage.isFaqQuestionVisible(q)) {
                    seen.add(q);
                }
            }
            // Also harvest any "loose" questions in the tree (Android only —
            // useful for the "did we miss one in the bundle" diagnostic).
            for (String q : topicDetailPage.visibleFaqQuestions()) seen.add(q);
            if (seen.size() == before) stableScrolls++;
            else stableScrolls = 0;
            topicDetailPage.scrollDown();
        }
        return seen;
    }

    // ========================================================================
    // TC25075 — Topic Detail displays Title / Image / Overview / Lessons /
    //           Related First Aid / FAQs
    // ========================================================================
    @Test(description = "TC25075 - Validate display - Topic Detail", groups = {"regression"})
    public void TC25075() {
        LearnTopicDetail topic = topicWithFullDisplay();
        String title = topic.titleEn();
        log("📋 Using topic: " + title + " (id=" + topic.id + ")");

        openTopic(title);

        // Title is in the toolbar from the moment the screen renders.
        Assert.assertEquals(topicDetailPage.getTitle(), title,
                "Toolbar should show the topic title");

        // Image + Overview live near the top — visible without scrolling.
        Assert.assertTrue(topicDetailPage.hasHeroImage(),
                "Topic Detail should display a hero image");
        Assert.assertTrue(topicDetailPage.hasOverviewSection(),
                "Topic Detail should display the Overview section");
        Assert.assertFalse(topicDetailPage.getOverviewBody().isBlank(),
                "Overview body should be non-empty");

        // Walk the page so RecyclerView lazily inflates the lower sections.
        boolean lessonsSeen = false, relatedSeen = false, faqsSeen = false;
        for (int i = 0; i < 10 && !(lessonsSeen && relatedSeen && faqsSeen); i++) {
            if (topicDetailPage.hasLessonsSection()) lessonsSeen = true;
            if (topicDetailPage.hasRelatedArticlesSection()) relatedSeen = true;
            if (topicDetailPage.hasFaqsSection()) faqsSeen = true;
            if (lessonsSeen && relatedSeen && faqsSeen) break;
            topicDetailPage.scrollDown();
        }

        Assert.assertTrue(lessonsSeen,
                "Topic Detail should display the Lessons section");
        Assert.assertTrue(relatedSeen,
                "Topic Detail should display the Related Articles section");
        Assert.assertTrue(faqsSeen,
                "Topic Detail should display the Frequently Asked Questions section");

        log("✅ TC25075: Title, Image, Overview, Lessons, Related Articles, FAQs all present");
    }

    // ========================================================================
    // TC25076 — Back navigation from Topic Detail
    // ========================================================================
    @Test(description = "TC25076 - Verify users can navigate back from Topic Detail", groups = {"regression"})
    public void TC25076() {
        // Use a featured topic — guaranteed to be reachable from the carousel
        // at the top of Browse Topics, no extra scrolling needed.
        LearnTopicDetail topic = anyTopic();
        openTopic(topic.titleEn());

        // The pin-first-visit tooltip may still be up if openTopic's tapGotIt
        // raced with rendering — accept either tooltip OR pin button as proof
        // that we're on Topic Detail. Back-nav works from either state.
        Assert.assertTrue(
                topicDetailPage.isPinTooltipPresent() || topicDetailPage.isPinButtonVisible(),
                "Pre-condition: should be on Topic Detail screen");

        topicDetailPage.tapBack();
        try { Thread.sleep(800); } catch (InterruptedException ignored) {}

        // Back from Topic Detail should land us back on the Learn tab.
        // isPinButtonVisible() throws when the button is gone (it wraps
        // waitForVisibility), so check raw presence via the live driver.
        boolean pinStillThere = !driver.findElements(isAndroid()
                ? By.xpath("//*[@resource-id='com.cube.arc.fa:id/action_pin_button' or " +
                        "@resource-id='com.cube.arc.fa:id/action_unpin_button']")
                : By.xpath("//XCUIElementTypeButton[@name='Topic Pinned' or @name='Topic Unpinned']"))
                .isEmpty();
        Assert.assertFalse(pinStillThere,
                "Pin button should be gone after back nav");
        Assert.assertTrue(tabPage.isTabPresent(Tab.LEARN),
                "Should be back on a tabbed screen (Learn) after back nav");

        log("✅ TC25076: Back from Topic Detail returned to Learn tab");
    }

    // ========================================================================
    // TC25098 — Correct FAQs appear (validate against bundle)
    // ========================================================================
    @Test(description = "TC25098 - Validate the correct FAQs appear within a topic", groups = {"regression"})
    public void TC25098() {
        LearnTopicDetail topic = topicWithFaqs();
        log("📋 Using topic: " + topic.titleEn() + " — " + topic.faqs.size() + " FAQs in bundle");

        openTopic(topic.titleEn());

        // Build the expected list (en-US, trimmed) directly from the bundle.
        List<String> expected = new java.util.ArrayList<>();
        for (Faq f : topic.faqs) {
            String q = f.questionEn();
            Assert.assertNotNull(q, "Bundle FAQ should have an en-US question");
            expected.add(q.trim());
        }

        Set<String> seen = findAllFaqQuestions(expected);
        log("📋 Rendered FAQ count: " + seen.size() + " / " + expected.size());

        // Diagnose any miss before failing — easier than re-running.
        List<String> missing = new java.util.ArrayList<>();
        for (String q : expected) if (!seen.contains(q)) missing.add(q);
        Assert.assertTrue(missing.isEmpty(),
                "Bundle FAQ questions not found on screen:\n  - " +
                String.join("\n  - ", missing) +
                "\nRendered: " + seen);

        log("✅ TC25098: All " + expected.size() + " bundle FAQs rendered on screen");
    }

    // ========================================================================
    // TC25099 — Tap an FAQ → question + answer both display
    // ========================================================================
    @Test(description = "TC25099 - Verify users can view an FAQ", groups = {"regression"})
    public void TC25099() {
        LearnTopicDetail topic = topicWithFaqs();
        Faq target = topic.faqs.get(0);
        String question = target.questionEn();
        String answer = target.answerEn();
        Assert.assertNotNull(question, "Bundle FAQ should have an en-US question");
        Assert.assertNotNull(answer, "Bundle FAQ should have an en-US answer");
        log("📋 Using topic '" + topic.titleEn() + "', first FAQ: '" + question + "'");

        openTopic(topic.titleEn());
        topicDetailPage.scrollToFaqsSection();

        // The question must already be visible (it's the row header). We
        // pick faqs.get(0) so it's the topmost row — should be in view as
        // soon as the FAQ section header is.
        Assert.assertTrue(topicDetailPage.isFaqQuestionVisible(question),
                "FAQ question should be visible before tap: '" + question + "'");

        // Use a substring of the answer for the visibility check — bundle
        // answer text is sometimes multi-paragraph; matching the first
        // sentence is enough to prove the inline expansion happened.
        String answerNeedle = answerSnippet(answer);
        Assert.assertFalse(topicDetailPage.isFaqAnswerVisible(answerNeedle),
                "Sanity: FAQ answer should NOT be visible before tap");

        topicDetailPage.tapFaq(question);
        try { Thread.sleep(700); } catch (InterruptedException ignored) {}

        Assert.assertTrue(topicDetailPage.isFaqAnswerVisible(answerNeedle),
                "FAQ answer (snippet '" + answerNeedle + "') should appear after tapping the question");

        log("✅ TC25099: FAQ '" + question + "' expanded to reveal answer");
    }

    /**
     * Pick a short, distinctive substring of the answer for visibility
     * checks — full answer text can wrap, contain smart-quotes, or span
     * paragraphs that may not all land on screen at once.
     */
    private String answerSnippet(String answer) {
        String[] sentences = answer.split("(?<=[.!?])\\s+");
        String first = sentences.length > 0 ? sentences[0] : answer;
        // Cap at 60 chars to stay well within whatever's rendered.
        return first.length() > 60 ? first.substring(0, 60).trim() : first.trim();
    }

    // ========================================================================
    // TC25100 — FAQ section hidden when no FAQs are added to a topic
    // ========================================================================
    @Test(description = "TC25100 - Verify the FAQ section is hidden when no FAQs are added within a topic", groups = {"regression"})
    public void TC25100() {
        LearnTopicDetail topic = topicWithoutFaqs();
        if (topic == null) {
            throw new SkipException(
                    "No topic in bundle has zero FAQs — cannot exercise the empty-state. " +
                    "(All " + ContentBundleLoader.allTopics().size() +
                    " topics in this env have at least one FAQ.)");
        }
        log("📋 Using topic with no FAQs: " + topic.titleEn());

        openTopic(topic.titleEn());

        // Scroll the whole detail screen so the FAQ section, if it existed,
        // would be in the a11y tree by the time we assert absence.
        for (int i = 0; i < 10; i++) topicDetailPage.scrollDown();

        Assert.assertFalse(topicDetailPage.hasFaqsSection(),
                "FAQ section header should not render for a topic with no FAQs");

        log("✅ TC25100: FAQ section hidden for topic '" + topic.titleEn() + "'");
    }
}
