package tests;

import com.cube.qa.framework.pages.home.LearnTabPage;
import com.cube.qa.framework.pages.home.TabPage;
import com.cube.qa.framework.pages.home.TabPage.Tab;
import com.cube.qa.framework.pages.home.TopicDetailPage;
import com.cube.qa.framework.pages.lessons.LessonCompletePage;
import com.cube.qa.framework.pages.lessons.LessonPage;
import com.cube.qa.framework.pages.lessons.LessonStartPage;
import com.cube.qa.framework.pages.onboarding.TooltipsPage;
import com.cube.qa.framework.pages.streaks.StreaksPage;
import com.cube.qa.framework.testdata.loader.ContentBundleLoader;
import com.cube.qa.framework.testdata.model.LearnTopic;
import com.cube.qa.framework.testdata.model.LearnTopicDetail;
import com.cube.qa.framework.testdata.model.Lesson;
import com.cube.qa.framework.testdata.model.LessonCard;
import com.cube.qa.framework.testdata.model.LessonContentComponent;
import com.cube.qa.framework.utils.BaseTest;
import org.openqa.selenium.By;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Lessons tests.
 * Testiny: ARC First Aid - v4.0.0 > Learn Tab (With Microlearning) > Topics > Lessons
 *
 * <p>Lessons live inside Topics: Topic Detail → Lesson Carousel → Lesson Start
 * → in-lesson cards → Lesson Complete. Topics, lessons, and lesson card
 * structure are all sourced from the live CDN bundle so the same code works
 * across dev/staging/prod.
 *
 * <p>Streak modal: completing a lesson on a fresh install fires the streak
 * sheet after returning to the Topic page. Each test that completes a
 * lesson explicitly calls {@code streaksPage.closeIfPresent()} after
 * {@code lessonCompletePage.tapBackToTopic()} — that responsibility lives in
 * tests, not in {@link LessonCompletePage}, so future Streaks tests can
 * inspect the modal directly.
 *
 * <p>Fast lesson completion: {@code LessonPage.tapProgressBarSegment(N)}
 * jumps directly to the Nth card, bypassing the swipe-by-swipe walk —
 * mirrors the manual-tester shortcut.
 */
public class LessonsTest extends BaseTest {

    private TabPage tabPage;
    private LearnTabPage learnTabPage;
    private TopicDetailPage topicDetailPage;
    private TooltipsPage tooltipsPage;
    private LessonStartPage lessonStartPage;
    private LessonPage lessonPage;
    private LessonCompletePage lessonCompletePage;
    private StreaksPage streaksPage;

    @BeforeMethod(alwaysRun = true, dependsOnMethods = "setUp")
    public void setUpTest() {
        tabPage = pages.tabPage();
        learnTabPage = pages.learnTabPage();
        topicDetailPage = pages.topicDetailPage();
        tooltipsPage = pages.tooltipsPage();
        lessonStartPage = pages.lessonStartPage();
        lessonPage = pages.lessonPage();
        lessonCompletePage = pages.lessonCompletePage();
        streaksPage = pages.streaksPage();

        // Same retry pattern as TopicDetailTest / BrowseTopicsTest.
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

    /** First topic with ≥1 lesson — covers most lesson tests. */
    private LearnTopicDetail anyTopicWithLessons() {
        for (LearnTopic t : ContentBundleLoader.allTopics()) {
            LearnTopicDetail d = ContentBundleLoader.topicDetail(t.id);
            if (!d.lessons.isEmpty()) return d;
        }
        throw new RuntimeException("No topic in bundle has lessons");
    }

    /**
     * First topic that contains at least one lesson with a non-empty en-US
     * description in the bundle — used by TC25078 to actually exercise the
     * description text-match path. Falls back to {@link #anyTopicWithLessons()}
     * if no described lesson exists in the bundle (in which case TC25078's
     * text-match assertion is skipped and only presence is checked).
     */
    private LearnTopicDetail topicWithDescribedLesson() {
        for (LearnTopic t : ContentBundleLoader.allTopics()) {
            LearnTopicDetail d = ContentBundleLoader.topicDetail(t.id);
            for (Lesson l : d.lessons) {
                String desc = l.descriptionEn();
                if (desc != null && !desc.isBlank()) return d;
            }
        }
        return anyTopicWithLessons();
    }

    /**
     * First lesson within {@code topic} that has a non-empty en-US description,
     * or {@code topic.lessons.get(0)} if none do.
     */
    private Lesson firstDescribedLessonOrFirst(LearnTopicDetail topic) {
        for (Lesson l : topic.lessons) {
            String desc = l.descriptionEn();
            if (desc != null && !desc.isBlank()) return l;
        }
        return topic.lessons.get(0);
    }

    /**
     * Topic with ≥2 lessons — used for carousel swipe (TC25077). We prefer
     * the first ≥2-lesson topic in bundle order. Threshold is ≥2 (not ≥3)
     * because that's the minimum needed to assert "swipe reveals another
     * lesson", and on staging the first ≥3-lesson topic ("Heat-related
     * Illness") has a Topic Detail layout that doesn't expose the carousel
     * within the on-screen scrollable region — best-effort safe choice.
     */
    private LearnTopicDetail topicWithMultipleLessons() {
        for (LearnTopic t : ContentBundleLoader.allTopics()) {
            LearnTopicDetail d = ContentBundleLoader.topicDetail(t.id);
            if (d.lessons.size() >= 2) return d;
        }
        throw new RuntimeException("No topic has ≥2 lessons in bundle");
    }

    /**
     * Picks the first lesson whose first card matches a card-shape predicate.
     * Returns a {@code [topic, lesson, cardIndex]} tuple, or {@code null} if
     * nothing matches in this env's bundle.
     */
    private LessonPick findLessonWithCard(java.util.function.Predicate<LessonCard> wantedCard) {
        for (LearnTopic t : ContentBundleLoader.allTopics()) {
            LearnTopicDetail d = ContentBundleLoader.topicDetail(t.id);
            for (Lesson l : d.lessons) {
                for (int i = 0; i < l.cards.size(); i++) {
                    if (wantedCard.test(l.cards.get(i))) {
                        return new LessonPick(d, l, i);
                    }
                }
            }
        }
        return null;
    }

    private record LessonPick(LearnTopicDetail topic, Lesson lesson, int cardIndex) {}

    // ---- Bundle-vs-rendered text assertions --------------------------------

    /**
     * Assert the rendered card heading matches the bundle's expected heading
     * exactly (after normalisation). Skips silently if the bundle has no
     * heading text for this card — without expected text there's nothing to
     * compare against, and presence-only is already asserted by the caller.
     */
    private void assertHeadingMatchesBundle(LessonCard card, String renderedHeading) {
        String expected = card.expectedHeading();
        if (expected == null || expected.isBlank()) return;
        Assert.assertEquals(normalize(renderedHeading), normalize(expected),
                "Card heading text should match bundle.\n"
                        + "Expected: '" + expected + "'\n"
                        + "Rendered: '" + renderedHeading + "'");
    }

    /**
     * Assert the rendered paragraph block contains the bundle's expected
     * paragraph text (substring match after normalisation). Substring rather
     * than equality because Android's {@code paragraph_description} TextView
     * may concatenate multiple paragraph components, and iOS picks the
     * second-topmost StaticText which can include trailing whitespace from
     * sibling layout. Skips when bundle has no paragraph text.
     */
    private void assertParagraphMatchesBundle(LessonCard card, String renderedParagraph) {
        String expected = card.expectedFirstParagraph();
        if (expected == null || expected.isBlank()) return;
        String renderedNorm = normalize(renderedParagraph);
        String expectedNorm = normalize(expected);
        Assert.assertTrue(renderedNorm.contains(expectedNorm),
                "Card paragraph should contain bundle text.\n"
                        + "Expected substring: '" + expected + "'\n"
                        + "Rendered: '" + renderedParagraph + "'");
    }

    // ---- Flow helpers ------------------------------------------------------

    /** Tap a topic by title, mirroring TopicDetailTest.openTopic. */
    private void openTopic(String title) {
        By locator = isAndroid()
                ? By.xpath("//*[@resource-id='com.cube.arc.fa:id/chevron_link_title' and @text='" + title + "']")
                : By.name(title);
        if (driver.findElements(locator).isEmpty()) {
            for (int i = 0; i < 6 && driver.findElements(locator).isEmpty(); i++) {
                if (isAndroid()) {
                    driver.executeScript("mobile: scrollGesture", Map.of(
                            "left", 100, "top", 500, "width", 800, "height", 1200,
                            "direction", "down", "percent", 0.7));
                } else {
                    driver.executeScript("mobile: swipe", Map.of("direction", "up"));
                }
                try { Thread.sleep(600); } catch (InterruptedException ignored) {}
            }
        }
        if (driver.findElements(locator).isEmpty()) {
            learnTabPage.tapCarouselCard(title);
        } else {
            driver.findElement(locator).click();
        }
        waitForTopicDetail();
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
     * Tap the lesson card with the given title on Topic Detail. Scrolls down
     * if the carousel isn't yet on screen.
     */
    private void openLesson(String lessonTitle) {
        // First, ensure the lessons carousel is on screen. The carousel only
        // exposes one card at a time, so we need to swipe the carousel
        // horizontally to surface non-first lesson titles.
        topicDetailPage.scrollToLessonsSection();

        By exact = isAndroid()
                ? By.xpath("//*[@resource-id='com.cube.arc.fa:id/view_pager']" +
                        "//*[@resource-id='com.cube.arc.fa:id/title' and @text='"
                        + lessonTitle + "']")
                : By.name(lessonTitle);
        // Lenient fallback: any title in the carousel containing a stable
        // prefix of the bundle title. Covers off-screen pager siblings and
        // smart-quote / whitespace differences.
        String safe = lessonTitle.replace("'", "");
        String needle = safe.substring(0, Math.min(40, safe.length()));
        By contains = isAndroid()
                ? By.xpath("//*[@resource-id='com.cube.arc.fa:id/title' and contains(@text,\""
                        + needle + "\")]")
                : By.xpath("//XCUIElementTypeStaticText[contains(@name,\"" + needle + "\")]");

        java.util.function.Supplier<org.openqa.selenium.WebElement> finder = () -> {
            java.util.List<org.openqa.selenium.WebElement> els = driver.findElements(exact);
            if (!els.isEmpty()) return els.get(0);
            els = driver.findElements(contains);
            return els.isEmpty() ? null : els.get(0);
        };

        // Swipe the carousel up to N times to find the desired lesson.
        // End-of-carousel detection: snapshot the carousel signature
        // (iOS: Adjustable.value, includes "X of N"; Android: visible titles)
        // and break when two consecutive swipes don't change it. This stops
        // burning swipes once we've hit the last card.
        String prevSig = topicDetailPage.carouselSignature();
        int stableSwipes = 0;
        boolean visible = topicDetailPage.isLessonCardVisible(lessonTitle);
        for (int i = 0; i < 12 && !visible && finder.get() == null; i++) {
            topicDetailPage.swipeLessonsCarousel();
            try { Thread.sleep(500); } catch (InterruptedException ignored) {}
            String sig = topicDetailPage.carouselSignature();
            if (sig.equals(prevSig)) {
                if (++stableSwipes >= 2) break;
            } else {
                stableSwipes = 0;
                prevSig = sig;
            }
            visible = topicDetailPage.isLessonCardVisible(lessonTitle);
        }
        if (isAndroid()) {
            org.openqa.selenium.WebElement target = finder.get();
            if (target == null) {
                throw new RuntimeException("Lesson card not found: " + lessonTitle);
            }
            target.click();
        } else {
            // iOS: the Adjustable carousel container isn't tappable as an
            // element — coord-tap its center to open Lesson Start.
            if (!topicDetailPage.tapVisibleLessonCard(lessonTitle)) {
                throw new RuntimeException("iOS lesson card not visible to tap: " + lessonTitle);
            }
        }
        // Lesson Start screen — wait for START CTA.
        long deadline = System.currentTimeMillis() + 10_000;
        while (System.currentTimeMillis() < deadline) {
            if (lessonStartPage.isDisplayed()) return;
            try { Thread.sleep(250); } catch (InterruptedException ignored) {}
        }
        throw new RuntimeException("Lesson Start screen never appeared for: " + lessonTitle);
    }

    /**
     * Drive a lesson from Start through to the Lesson Complete sheet via the
     * progress-bar shortcut: tap START, dismiss the tooltip if present, then
     * jump straight to the final card and tap DONE.
     */
    private void completeLessonViaShortcut() {
        lessonStartPage.tapStart();
        try { Thread.sleep(800); } catch (InterruptedException ignored) {}
        if (lessonPage.isTooltipVisible()) {
            lessonPage.dismissTooltip();
            try { Thread.sleep(500); } catch (InterruptedException ignored) {}
        }
        int total = lessonPage.getProgressSegmentCount();
        if (total > 1) {
            lessonPage.tapProgressBarSegment(total);
            try { Thread.sleep(800); } catch (InterruptedException ignored) {}
        }
        Assert.assertTrue(lessonPage.hasDoneButton(),
                "DONE button should be present on the final card");
        lessonPage.tapDone();
        try { Thread.sleep(1500); } catch (InterruptedException ignored) {}
    }

    private String normalize(String s) {
        if (s == null) return "";
        return s.replace('\u00A0', ' ').replaceAll("\\s+", " ").trim();
    }

    // ========================================================================
    // TC25077 — Lessons carousel swipes horizontally
    // ========================================================================
    @Test(description = "TC25077 - Lessons carousel", groups = {"regression"})
    public void TC25077() {
        LearnTopicDetail topic = topicWithMultipleLessons();
        log("📋 Using topic with " + topic.lessons.size() + " lessons: " + topic.titleEn());

        openTopic(topic.titleEn());
        topicDetailPage.scrollToLessonsSection();

        String firstTitle = topic.lessons.get(0).titleEn();
        Assert.assertTrue(topicDetailPage.isLessonCardVisible(firstTitle),
                "First lesson card should be visible before swipe: " + firstTitle);

        topicDetailPage.swipeLessonsCarousel();
        try { Thread.sleep(700); } catch (InterruptedException ignored) {}

        // After swipe, at least one *other* lesson title should now be on
        // screen — if the only one we ever see is the first, the carousel
        // didn't move.
        boolean otherSeen = false;
        for (int i = 1; i < topic.lessons.size(); i++) {
            if (topicDetailPage.isLessonCardVisible(topic.lessons.get(i).titleEn())) {
                otherSeen = true;
                break;
            }
        }
        Assert.assertTrue(otherSeen,
                "Another lesson card should be visible after swiping the carousel");

        log("✅ TC25077: Lessons carousel swiped to reveal another lesson");
    }

    // ========================================================================
    // TC25078 — Lesson card display on Topic Detail
    // ========================================================================
    @Test(description = "TC25078 - Lesson card display", groups = {"regression"})
    public void TC25078() {
        // Prefer a lesson that exposes a non-empty bundle description so the
        // text-match path below actually runs. Falls back to first-with-lessons
        // if no lesson in the bundle has a description.
        LearnTopicDetail topic = topicWithDescribedLesson();
        Lesson l = firstDescribedLessonOrFirst(topic);
        openTopic(topic.titleEn());
        topicDetailPage.scrollToLessonsSection();

        Assert.assertTrue(topicDetailPage.lessonCardHasImage(),
                "Lesson card should display an image");
        Assert.assertTrue(topicDetailPage.isLessonCardVisible(l.titleEn()),
                "Lesson card should display the title: " + l.titleEn());
        // Description: presence is always asserted. When the bundle provides
        // a non-empty en-US description, also assert the rendered text matches
        // — Android only, since iOS does not expose the per-card description
        // string in the a11y tree (see TopicDetailPage#getLessonCardDescription).
        Assert.assertTrue(topicDetailPage.lessonCardHasDescriptionElement(),
                "Lesson card should expose a description element");
        String bundleDesc = l.descriptionEn();
        if (isAndroid() && bundleDesc != null && !bundleDesc.isBlank()) {
            String rendered = topicDetailPage.getLessonCardDescription(l.titleEn());
            Assert.assertNotNull(rendered,
                    "Lesson card subtitle should not be empty when bundle has description: '"
                            + bundleDesc + "'");
            Assert.assertEquals(normalize(rendered), normalize(bundleDesc),
                    "Lesson card description text should match bundle.");
        }
        Assert.assertTrue(topicDetailPage.lessonCardHasDuration(),
                "Lesson card should display the duration (X minutes)");
        // Tighten: when the bundle exposes a positive durationInMinutes,
        // assert the rendered minutes match exactly. Both platforms are
        // covered by getLessonCardDurationMinutes.
        if (l.durationInMinutes > 0) {
            Integer renderedMin = topicDetailPage.getLessonCardDurationMinutes(l.titleEn());
            Assert.assertNotNull(renderedMin,
                    "Lesson card duration should expose a parseable minute count");
            Assert.assertEquals(renderedMin.intValue(), l.durationInMinutes,
                    "Lesson card duration should match bundle for: " + l.titleEn());
        }
        Assert.assertTrue(topicDetailPage.lessonCardHasChevron(),
                "Lesson card should display the chevron");

        log("✅ TC25078: Lesson card shows image, title, description"
                + (isAndroid() && bundleDesc != null && !bundleDesc.isBlank() ? " (matches bundle)" : " (slot)")
                + ", duration=" + l.durationInMinutes + "m, chevron");
    }

    // ========================================================================
    // TC25079 — Lesson carousel contents match the CMS (= bundle)
    // ========================================================================
    @Test(description = "TC25079 - Validate carousel against CMS", groups = {"regression"})
    public void TC25079() {
        LearnTopicDetail topic = topicWithMultipleLessons();
        openTopic(topic.titleEn());
        topicDetailPage.scrollToLessonsSection();

        Set<String> seen = new LinkedHashSet<>();
        // Walk the carousel: look for each title, swipe forward until we've
        // collected them all or the carousel stops yielding new ones.
        int stable = 0;
        for (int i = 0; i < topic.lessons.size() * 3 && stable < 3; i++) {
            int before = seen.size();
            for (Lesson l : topic.lessons) {
                String t = l.titleEn();
                if (!seen.contains(t) && topicDetailPage.isLessonCardVisible(t)) {
                    seen.add(t);
                }
            }
            if (seen.size() == before) stable++;
            else stable = 0;
            if (seen.size() < topic.lessons.size()) {
                topicDetailPage.swipeLessonsCarousel();
                try { Thread.sleep(500); } catch (InterruptedException ignored) {}
            }
        }

        List<String> expected = new ArrayList<>();
        for (Lesson l : topic.lessons) expected.add(l.titleEn());

        List<String> missing = new ArrayList<>();
        for (String e : expected) if (!seen.contains(e)) missing.add(e);

        Assert.assertTrue(missing.isEmpty(),
                "Bundle lesson titles not seen on carousel: " + missing
                        + "\nRendered: " + seen);

        log("✅ TC25079: All " + expected.size() + " bundle lessons seen on carousel");
    }

    // ========================================================================
    // TC25080 — Lesson Start screen elements
    // ========================================================================
    @Test(description = "TC25080 - Lesson start page", groups = {"regression"})
    public void TC25080() {
        LearnTopicDetail topic = anyTopicWithLessons();
        Lesson l = topic.lessons.get(0);
        openTopic(topic.titleEn());
        openLesson(l.titleEn());

        Assert.assertTrue(lessonStartPage.hasImage(), "Hero image should be present");
        Assert.assertTrue(lessonStartPage.hasTitle(), "Lesson title should be present");
        Assert.assertTrue(
                lessonStartPage.getTitle().contains(l.titleEn()),
                "Lesson title should contain bundle title: '" + l.titleEn()
                        + "', got: '" + lessonStartPage.getTitle() + "'");
        Assert.assertTrue(lessonStartPage.hasDuration(), "Duration should be present");
        Assert.assertTrue(
                lessonStartPage.getDuration().toLowerCase().contains("minute"),
                "Duration text should mention minutes, got: " + lessonStartPage.getDuration());
        // Tighten: when bundle has a positive durationInMinutes, assert the
        // rendered minute count matches exactly (catches bundle drift /
        // wrong-lesson rendering).
        if (l.durationInMinutes > 0) {
            Integer renderedMin = lessonStartPage.getDurationMinutes();
            Assert.assertNotNull(renderedMin,
                    "Lesson Start duration should parse to a minute count, got: "
                            + lessonStartPage.getDuration());
            Assert.assertEquals(renderedMin.intValue(), l.durationInMinutes,
                    "Lesson Start duration should match bundle.");
        }
        Assert.assertTrue(lessonStartPage.hasSubtitle(),
                "Subtitle element should be present (text may be empty per bundle)");
        Assert.assertTrue(lessonStartPage.hasStartCta(), "START CTA should be present");

        log("✅ TC25080: Lesson Start displays image, title, duration, subtitle slot, START");
    }

    // ========================================================================
    // TC25081 — Back button on Lesson Start returns to Topic Detail
    // ========================================================================
    @Test(description = "TC25081 - Go back from lesson start page", groups = {"regression"})
    public void TC25081() {
        LearnTopicDetail topic = anyTopicWithLessons();
        Lesson l = topic.lessons.get(0);
        openTopic(topic.titleEn());
        openLesson(l.titleEn());

        Assert.assertTrue(lessonStartPage.isDisplayed(),
                "Pre-condition: should be on Lesson Start screen");

        lessonStartPage.tapBack();
        try { Thread.sleep(800); } catch (InterruptedException ignored) {}

        Assert.assertTrue(
                topicDetailPage.isPinButtonVisible() || topicDetailPage.hasOverviewSection(),
                "Should be back on Topic Detail after tapping back");

        log("✅ TC25081: Back from Lesson Start returned to Topic Detail");
    }

    // ========================================================================
    // TC25082 — Tooltip animation appears on first lesson START
    // ========================================================================
    @Test(description = "TC25082 - Tooltip animation appears first time", groups = {"regression"})
    public void TC25082() {
        LearnTopicDetail topic = anyTopicWithLessons();
        Lesson l = topic.lessons.get(0);
        openTopic(topic.titleEn());
        openLesson(l.titleEn());

        lessonStartPage.tapStart();
        try { Thread.sleep(1500); } catch (InterruptedException ignored) {}

        Assert.assertTrue(lessonPage.isTooltipVisible(),
                "Swipe tooltip should be visible the first time the user starts a lesson");

        log("✅ TC25082: First-time swipe tooltip displayed");
    }

    // ========================================================================
    // TC25083 — Tooltip animation does NOT appear on subsequent lessons.
    // fullReset=true gives us a fresh install per test, so we synthesise
    // "user has started a lesson before" by starting → exiting → re-starting
    // within the same test.
    // ========================================================================
    @Test(description = "TC25083 - Tooltip animation does not appear subsequent times", groups = {"regression"})
    public void TC25083() {
        LearnTopicDetail topic = anyTopicWithLessons();
        Lesson l = topic.lessons.get(0);
        openTopic(topic.titleEn());
        openLesson(l.titleEn());

        // First START → expect tooltip → dismiss → cross out.
        lessonStartPage.tapStart();
        try { Thread.sleep(1500); } catch (InterruptedException ignored) {}
        Assert.assertTrue(lessonPage.isTooltipVisible(),
                "Pre-condition: tooltip should appear on the first START");
        lessonPage.dismissTooltip();
        try { Thread.sleep(600); } catch (InterruptedException ignored) {}
        lessonPage.tapClose();
        try { Thread.sleep(800); } catch (InterruptedException ignored) {}

        // Re-enter the lesson and START again — tooltip should no longer fire.
        openLesson(l.titleEn());
        lessonStartPage.tapStart();
        try { Thread.sleep(1500); } catch (InterruptedException ignored) {}

        Assert.assertFalse(lessonPage.isTooltipVisible(),
                "Tooltip should NOT appear on subsequent lesson starts");

        log("✅ TC25083: Tooltip suppressed on second lesson start within session");
    }

    // ========================================================================
    // TC25084 — In-lesson display: cross button, slide indicator, lesson card
    // ========================================================================
    @Test(description = "TC25084 - Validate lesson display", groups = {"regression"})
    public void TC25084() {
        LearnTopicDetail topic = anyTopicWithLessons();
        Lesson l = topic.lessons.get(0);
        openTopic(topic.titleEn());
        openLesson(l.titleEn());
        lessonStartPage.tapStart();
        try { Thread.sleep(1500); } catch (InterruptedException ignored) {}
        if (lessonPage.isTooltipVisible()) {
            lessonPage.dismissTooltip();
            try { Thread.sleep(500); } catch (InterruptedException ignored) {}
        }

        Assert.assertTrue(lessonPage.hasCloseButton(), "Cross (close) button should be present");
        Assert.assertTrue(lessonPage.hasSlideIndicator(), "Slide indicator should be present");
        Assert.assertTrue(lessonPage.hasLessonCard(), "Lesson card should be present");

        log("✅ TC25084: Lesson display shows cross, slide indicator, lesson card");
    }

    // ========================================================================
    // TC25085 — Card without image renders title + description only
    // ========================================================================
    @Test(description = "TC25085 - Validate content without image", groups = {"regression"})
    public void TC25085() {
        LessonPick pick = findLessonWithCard(c -> !c.hasImage() && !c.hasVideo());
        if (pick == null) {
            throw new SkipException("No lesson card without image/video found in bundle.");
        }
        log("📋 Using card '" + pick.lesson.titleEn() + "' #" + pick.cardIndex);
        navigateToCard(pick);

        LessonCard card = pick.lesson.cards.get(pick.cardIndex);
        String renderedHeading = lessonPage.getCurrentSectionHeading();
        Assert.assertNotNull(renderedHeading,
                "Card should display a title (section heading)");
        assertHeadingMatchesBundle(card, renderedHeading);

        String renderedParagraph = lessonPage.getCurrentParagraphDescription();
        Assert.assertNotNull(renderedParagraph,
                "Card should display a description");
        assertParagraphMatchesBundle(card, renderedParagraph);

        Assert.assertFalse(lessonPage.currentCardHasImage(),
                "No-image card should not render an image");

        log("✅ TC25085: No-image card displays title + description only");
    }

    // ========================================================================
    // TC25086 — Card with image renders image + title + description
    // ========================================================================
    @Test(description = "TC25086 - Validate content with image", groups = {"regression"})
    public void TC25086() {
        LessonPick pick = findLessonWithCard(LessonCard::hasImage);
        if (pick == null) {
            throw new SkipException("No lesson card with imageComponent found in bundle.");
        }
        log("📋 Using card '" + pick.lesson.titleEn() + "' #" + pick.cardIndex);
        navigateToCard(pick);

        LessonCard card = pick.lesson.cards.get(pick.cardIndex);
        Assert.assertTrue(lessonPage.currentCardHasImage(),
                "Image card should render an image");
        String renderedHeading = lessonPage.getCurrentSectionHeading();
        Assert.assertNotNull(renderedHeading,
                "Image card should still display a title");
        assertHeadingMatchesBundle(card, renderedHeading);
        // Description is optional on image cards — assert presence only if
        // bundle has a paragraph component on this card.
        boolean cardHasParagraph = card.data.content.stream()
                .anyMatch(c -> "paragraph".equals(c.type));
        if (cardHasParagraph) {
            String renderedParagraph = lessonPage.getCurrentParagraphDescription();
            Assert.assertNotNull(renderedParagraph,
                    "Image card with paragraph component should display description text");
            assertParagraphMatchesBundle(card, renderedParagraph);
        }

        log("✅ TC25086: Image card displays image, title, description (where present)");
    }

    // ========================================================================
    // TC25087 — Card with video renders video + title + description
    // ========================================================================
    @Test(description = "TC25087 - Validate content with video", groups = {"regression"})
    public void TC25087() {
        LessonPick pick = findLessonWithCard(LessonCard::hasVideo);
        if (pick == null) {
            throw new SkipException("No lesson card with videoComponent found in bundle.");
        }
        log("📋 Using card '" + pick.lesson.titleEn() + "' #" + pick.cardIndex);
        navigateToCard(pick);

        LessonCard card = pick.lesson.cards.get(pick.cardIndex);
        Assert.assertTrue(lessonPage.currentCardHasVideo(),
                "Video card should render a video element");
        String renderedHeading = lessonPage.getCurrentSectionHeading();
        Assert.assertNotNull(renderedHeading,
                "Video card should display a title");
        assertHeadingMatchesBundle(card, renderedHeading);

        log("✅ TC25087: Video card displays video + title");
    }

    // ========================================================================
    // TC25089 — Unordered bullets render one item per bundle entry
    // ========================================================================
    @Test(description = "TC25089 - Verify component display - Unordered bullets", groups = {"regression"})
    public void TC25089() {
        LessonPick pick = findLessonWithCard(LessonCard::hasBullets);
        if (pick == null) {
            throw new SkipException("No lesson card with bullets found in bundle.");
        }
        log("📋 Using card '" + pick.lesson.titleEn() + "' #" + pick.cardIndex);
        navigateToCard(pick);

        LessonCard card = pick.lesson.cards.get(pick.cardIndex);
        String renderedHeading = lessonPage.getCurrentSectionHeading();
        Assert.assertNotNull(renderedHeading,
                "Bullet card should display a section heading");
        assertHeadingMatchesBundle(card, renderedHeading);

        // Build expected bullet text list from the bundle.
        List<String> expected = new ArrayList<>();
        for (LessonContentComponent c : card.data.content) {
            if ("unorderedListItemComponent".equals(c.type) && c.titleEn() != null) {
                expected.add(normalize(c.titleEn()));
            }
        }
        Assert.assertFalse(expected.isEmpty(), "Bundle card should expose at least one bullet");

        List<String> rendered = new ArrayList<>();
        for (String r : lessonPage.getCurrentBulletTexts()) rendered.add(normalize(r));

        // Each expected bullet must appear among rendered text. We don't
        // require exact element-count parity — the card may render the
        // section heading + paragraph as TextViews too; the assertion is
        // bundle-bullets-rendered, not strict-equality.
        List<String> missing = new ArrayList<>();
        for (String e : expected) {
            if (!rendered.contains(e)) missing.add(e);
        }
        Assert.assertTrue(missing.isEmpty(),
                "Bullets in bundle not found rendered: " + missing
                        + "\nRendered text: " + rendered);

        log("✅ TC25089: All " + expected.size() + " bundle bullets rendered");
    }

    // ========================================================================
    // TC25090 — Cross button exits the lesson back to Topic Detail
    // ========================================================================
    @Test(description = "TC25090 - Verify users can exit a lesson", groups = {"regression"})
    public void TC25090() {
        LearnTopicDetail topic = anyTopicWithLessons();
        Lesson l = topic.lessons.get(0);
        openTopic(topic.titleEn());
        openLesson(l.titleEn());
        lessonStartPage.tapStart();
        try { Thread.sleep(1500); } catch (InterruptedException ignored) {}
        if (lessonPage.isTooltipVisible()) {
            lessonPage.dismissTooltip();
            try { Thread.sleep(500); } catch (InterruptedException ignored) {}
        }

        Assert.assertTrue(lessonPage.isDisplayed(),
                "Pre-condition: should be inside the lesson");

        lessonPage.tapClose();
        try { Thread.sleep(1000); } catch (InterruptedException ignored) {}

        Assert.assertTrue(
                topicDetailPage.isPinButtonVisible() || topicDetailPage.hasLessonsSection(),
                "Should be back on Topic Detail after exiting the lesson");

        log("✅ TC25090: Cross button exits lesson and returns to Topic Detail");
    }

    // ========================================================================
    // TC25096 — Lesson Complete screen displays image, title (random),
    //           description (random), Next Lesson CTA, Back To Topic CTA.
    //           Uses a multi-lesson topic so Next Lesson CTA is meaningful.
    // ========================================================================
    @Test(description = "TC25096 - Validate display - Lesson Complete - Next Lesson Available",
          groups = {"regression"})
    public void TC25096() {
        LearnTopicDetail topic = topicWithMultipleLessons();
        Lesson l = topic.lessons.get(0);
        openTopic(topic.titleEn());
        openLesson(l.titleEn());
        completeLessonViaShortcut();

        Assert.assertTrue(lessonCompletePage.isDisplayed(),
                "Lesson Complete sheet should be displayed");
        Assert.assertTrue(lessonCompletePage.hasImage(),
                "Lesson Complete should display an image/animation");

        // Title should match one of the four bundle variants.
        String renderedTitle = normalize(lessonCompletePage.getTitle());
        List<String> expectedTitles = List.of(
                normalize(ContentBundleLoader.appString("_LESSON_COMPLETE_TITLE_1")),
                normalize(ContentBundleLoader.appString("_LESSON_COMPLETE_TITLE_2")),
                normalize(ContentBundleLoader.appString("_LESSON_COMPLETE_TITLE_3")),
                normalize(ContentBundleLoader.appString("_LESSON_COMPLETE_TITLE_ALL"))
        );
        Assert.assertTrue(expectedTitles.contains(renderedTitle),
                "Lesson Complete title should match one of the bundle variants.\n"
                        + "Rendered: '" + renderedTitle + "'\n"
                        + "Expected one of: " + expectedTitles);

        // Description likewise — one of the four randomised bundle strings.
        String renderedDesc = normalize(lessonCompletePage.getSubtitle());
        List<String> expectedDescs = List.of(
                normalize(ContentBundleLoader.appString("_LESSON_COMPLETE_DESCRIPTION_1")),
                normalize(ContentBundleLoader.appString("_LESSON_COMPLETE_DESCRIPTION_2")),
                normalize(ContentBundleLoader.appString("_LESSON_COMPLETE_DESCRIPTION_3")),
                normalize(ContentBundleLoader.appString("_LESSON_COMPLETE_DESCRIPTION_ALL"))
        );
        Assert.assertTrue(expectedDescs.contains(renderedDesc),
                "Lesson Complete description should match one of the bundle variants.\n"
                        + "Rendered: '" + renderedDesc + "'\n"
                        + "Expected one of: " + expectedDescs);

        Assert.assertTrue(lessonCompletePage.hasNextLessonCta(),
                "Next Lesson CTA should be present (multi-lesson topic)");
        Assert.assertTrue(lessonCompletePage.hasBackToTopicCta(),
                "Back To Topic Page CTA should be present");

        log("✅ TC25096: Lesson Complete shows image, title='" + renderedTitle
                + "', desc='" + renderedDesc + "', both CTAs");
    }

    // ========================================================================
    // TC25097 — Back To Topic Page returns the user to Topic Detail
    // ========================================================================
    @Test(description = "TC25097 - Verify users can navigate back to topic page",
          groups = {"regression"})
    public void TC25097() {
        LearnTopicDetail topic = anyTopicWithLessons();
        Lesson l = topic.lessons.get(0);
        openTopic(topic.titleEn());
        openLesson(l.titleEn());
        completeLessonViaShortcut();

        Assert.assertTrue(lessonCompletePage.isDisplayed(),
                "Pre-condition: should be on Lesson Complete sheet");

        lessonCompletePage.tapBackToTopic();
        try { Thread.sleep(1500); } catch (InterruptedException ignored) {}

        // Streak modal interrupts the return — dismiss it before asserting.
        // (Streak content is asserted in future Streaks tests; here it's noise.)
        streaksPage.closeIfPresent();

        Assert.assertTrue(
                topicDetailPage.isPinButtonVisible() || topicDetailPage.hasLessonsSection(),
                "Should be back on Topic Detail after tapping Back To Topic Page");

        log("✅ TC25097: Back To Topic Page returned to Topic Detail");
    }

    // ---- Card-level navigation helper ---------------------------------------

    /**
     * From Learn tab, drives all the way to the {@code pick.cardIndex}'th
     * card of the chosen lesson. Uses the progress-bar shortcut to jump to
     * the target card without swiping through every preceding one.
     */
    private void navigateToCard(LessonPick pick) {
        openTopic(pick.topic.titleEn());
        openLesson(pick.lesson.titleEn());
        lessonStartPage.tapStart();
        try { Thread.sleep(1500); } catch (InterruptedException ignored) {}
        if (lessonPage.isTooltipVisible()) {
            lessonPage.dismissTooltip();
            try { Thread.sleep(500); } catch (InterruptedException ignored) {}
        }
        // Card index is 0-based in the bundle; progress bar is 1-indexed.
        if (pick.cardIndex > 0) {
            lessonPage.tapProgressBarSegment(pick.cardIndex + 1);
            try { Thread.sleep(800); } catch (InterruptedException ignored) {}
        }
    }
}
