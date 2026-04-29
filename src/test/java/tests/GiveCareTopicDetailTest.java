package tests;

import com.cube.qa.framework.pages.home.GiveCareTabPage;
import com.cube.qa.framework.pages.home.GiveCareTopicDetailPage;
import com.cube.qa.framework.pages.home.TabPage;
import com.cube.qa.framework.pages.home.TabPage.Tab;
import com.cube.qa.framework.pages.onboarding.TooltipsPage;
import com.cube.qa.framework.testdata.loader.ContentBundleLoader;
import com.cube.qa.framework.testdata.model.Article;
import com.cube.qa.framework.testdata.model.ArticleDetail;
import com.cube.qa.framework.testdata.model.EmergencyStep;
import com.cube.qa.framework.utils.BaseTest;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.List;

/**
 * Give Care Topic Detail — emergency-article reading screen reached by tapping
 * a topic in the Give Care tab list. Renders title, numbered steps (each with
 * a heading and an optional description that may contain {@code »}-marker
 * bullet lines), a sticky Call 911 CTA, and a back chevron.
 *
 * Testiny: ARC First Aid - v4.0.0 > Give Care Topic Detail
 *
 * <p>Canonical target topic for the suite: <b>Anaphylaxis: Epinephrine
 * Auto-Injector</b>. Chosen because:
 * <ul>
 *   <li>It's first alphabetically in the Give Care list — zero scroll needed
 *       to reach it, sidesteps the issue where mid-alphabet rows can sit
 *       under the sticky Call 911 CTA when the list is scrolled.</li>
 *   <li>It has 10 numbered steps including ones with descriptions <em>and</em>
 *       {@code »} bullet markers — covers TC22028 + TC22029 from a single
 *       fixture.</li>
 * </ul>
 *
 * <p>All content assertions read expected values from the live CDN bundle via
 * {@link ContentBundleLoader#articleDetail(String)}, so renaming a topic or
 * editing step copy in CMS doesn't false-fail the suite.
 *
 * <p>TC22023 (Call 911) is skipped on iOS — folded into Give Care tab's
 * TC22019 because SpringBoard rate-limits tel:911 confirmations per bundle id
 * within a session. See {@link GiveCareTabTest#TC22019()} for details.
 */
public class GiveCareTopicDetailTest extends BaseTest {

    private static final String TARGET_TOPIC = "Anaphylaxis: Epinephrine Auto-Injector";

    private TabPage tabPage;
    private GiveCareTabPage giveCarePage;
    private GiveCareTopicDetailPage detailPage;
    private TooltipsPage tooltipsPage;

    private ArticleDetail bundleArticle;

    @BeforeMethod(alwaysRun = true, dependsOnMethods = "setUp")
    public void setUpTest() {
        tabPage = pages.tabPage();
        giveCarePage = pages.giveCareTabPage();
        detailPage = pages.giveCareTopicDetailPage();
        tooltipsPage = pages.tooltipsPage();

        // Load expected content from the live bundle once per test.
        Article article = ContentBundleLoader.emergencyArticleByTitle(TARGET_TOPIC);
        Assert.assertNotNull(article,
                "Bundle should expose an emergency article titled '" + TARGET_TOPIC + "'");
        bundleArticle = ContentBundleLoader.articleDetail(article.id);
        Assert.assertNotNull(bundleArticle,
                "Bundle should return an ArticleDetail for id=" + article.id);

        RuntimeException lastFailure = null;
        for (int attempt = 0; attempt < 2; attempt++) {
            try {
                walkOnboardingAsGuest();
                int safety = 0;
                while (safety++ < 8 && tooltipsPage.isGotItButtonPresent()) {
                    try { tooltipsPage.tapGotIt(); } catch (RuntimeException e) { break; }
                }
                if (tabPage.isTabPresent(Tab.GIVE_CARE)) tabPage.tapTab(Tab.GIVE_CARE);
                long deadline = System.currentTimeMillis() + 8000;
                while (System.currentTimeMillis() < deadline
                        && !giveCarePage.hasGiveCareTopicsHeader()) {
                    try { Thread.sleep(250); } catch (InterruptedException ignored) {}
                }
                giveCarePage.scrollToTop();

                // Land on the detail screen — every test starts here.
                giveCarePage.tapEmergencyTopic(TARGET_TOPIC);
                long detailDeadline = System.currentTimeMillis() + 6000;
                while (System.currentTimeMillis() < detailDeadline
                        && !detailPage.hasTitle()) {
                    try { Thread.sleep(250); } catch (InterruptedException ignored) {}
                }
                Assert.assertTrue(detailPage.hasTitle(),
                        "Detail screen should render its toolbar title");
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

    // ========================================================================
    // TC22022 — Title + step content match the bundle
    // ========================================================================
    @Test(description = "TC22022 - Validate Display - View emergency topic - Title and Content",
            groups = {"smoke", "regression"})
    public void TC22022() {
        // Title: case-insensitive equality against the bundle's en-US name.
        String renderedTitle = detailPage.getTitle();
        String bundleTitle = bundleArticle.nameEn();
        log("📋 Bundle title: " + bundleTitle);
        log("📋 App title:    " + renderedTitle);
        Assert.assertNotNull(bundleTitle, "Bundle should expose an en-US name");
        Assert.assertTrue(renderedTitle.equalsIgnoreCase(bundleTitle),
                "Detail title '" + renderedTitle + "' should match bundle '" + bundleTitle + "'");

        // Content: the first step's heading should render at the top of the
        // article (no scroll needed). Asserting against the bundle proves the
        // detail screen is actually showing the right article's content, not
        // just chrome.
        List<EmergencyStep> steps = bundleArticle.emergencySteps();
        Assert.assertFalse(steps.isEmpty(),
                "Bundle should expose at least one emergency step for the target topic");
        EmergencyStep first = steps.get(0);
        Assert.assertNotNull(first.titleEn, "First step should have an en-US title");
        Assert.assertTrue(detailPage.hasStepTitleMatching(first.titleEn),
                "Detail screen should render step 1 heading: '" + first.titleEn + "'");
        log("✅ TC22022: Title + step-1 heading match the bundle");
    }

    // ========================================================================
    // TC22023 — Sticky Call 911 → call sheet appears, cancel returns to detail
    //
    // Skipped on iOS: SpringBoard rate-limits tel:911 confirmations per bundle
    // id within a single app session. GiveCareTabTest.TC22019 has already
    // exercised the tel:911 path during this run, so a second tap from the
    // detail screen silently no-ops on iOS. iOS coverage of "Call 911 from a
    // detail screen" is implicit — same sticky CTA, same locator, same
    // handler. Android has no such limit and runs this independently.
    // ========================================================================
    @Test(description = "TC22023 - Validate user can call 911 from emergency topic",
            groups = {"regression"})
    public void TC22023() {
        if ("ios".equalsIgnoreCase(config.getPlatform())) {
            throw new SkipException(
                    "iOS SpringBoard rate-limits tel:911 re-prompts within a "
                  + "single app session — coverage delegated to GiveCareTabTest.TC22019.");
        }
        Assert.assertTrue(detailPage.hasStickyCall911(),
                "Sticky Call 911 button should be visible on the detail screen");

        detailPage.tapCall911();
        long deadline = System.currentTimeMillis() + 8000;
        boolean sheetSeen = false;
        while (System.currentTimeMillis() < deadline) {
            if (giveCarePage.isCallSheetPresent()) { sheetSeen = true; break; }
            try { Thread.sleep(200); } catch (InterruptedException ignored) {}
        }
        // ALWAYS dismiss before asserting — leaving a call sheet up risks an
        // accidental dial.
        giveCarePage.cancelCallSheet();
        Assert.assertTrue(sheetSeen,
                "Tapping sticky Call 911 should open the system call sheet/dialer");

        long backDeadline = System.currentTimeMillis() + 5000;
        while (System.currentTimeMillis() < backDeadline && !detailPage.hasTitle()) {
            try { Thread.sleep(250); } catch (InterruptedException ignored) {}
        }
        Assert.assertTrue(detailPage.hasTitle(),
                "Cancelling the call should leave the user back on the detail screen");
        log("✅ TC22023: Call sheet appeared from detail, Cancel returned to detail");
    }

    // ========================================================================
    // TC22024 — Back chevron returns user to the Give Care tab
    // ========================================================================
    @Test(description = "TC22024 - Validate user can go back from emergency topic",
            groups = {"regression"})
    public void TC22024() {
        Assert.assertTrue(detailPage.hasBackButton(),
                "Detail screen should expose a back chevron");
        detailPage.tapBack();

        long deadline = System.currentTimeMillis() + 5000;
        while (System.currentTimeMillis() < deadline
                && !giveCarePage.hasGiveCareTopicsHeader()) {
            try { Thread.sleep(250); } catch (InterruptedException ignored) {}
        }
        Assert.assertTrue(giveCarePage.hasGiveCareTopicsHeader(),
                "Tapping back should return to the Give Care tab");
        log("✅ TC22024: Back chevron returned to Give Care tab");
    }

    // ========================================================================
    // TC22028 — Steps display: number, heading, (optional) description match
    //           the bundle. Authored against a DRAFT Testiny case; we automate
    //           anyway because the underlying behavior is stable.
    // ========================================================================
    @Test(description = "TC22028 - Validate Display - Steps - Number, Heading, Description",
            groups = {"regression"})
    public void TC22028() {
        List<EmergencyStep> steps = bundleArticle.emergencySteps();
        Assert.assertFalse(steps.isEmpty(),
                "Bundle should expose emergency steps for the target topic");

        // Numbers: assert what's rendered at the top of the article is a
        // prefix of the bundle's step-number sequence. Comparing to the
        // bundle (rather than insisting on a strict 1..N) honors authoring
        // quirks — Anaphylaxis legitimately has two steps sharing a number
        // in the CMS, and we want the test to ratify the CMS, not fight it.
        List<Integer> bundleNumbers = steps.stream()
                .map(s -> s.number)
                .collect(java.util.stream.Collectors.toList());
        List<Integer> rendered = detailPage.getRenderedStepNumbers();
        log("📋 Bundle step numbers:               " + bundleNumbers);
        log("📋 Rendered step numbers (top view):  " + rendered);
        Assert.assertFalse(rendered.isEmpty(),
                "At least one step number should render at the top of the article");
        Assert.assertTrue(rendered.size() <= bundleNumbers.size(),
                "Rendered " + rendered.size() + " numbers but bundle only lists "
                      + bundleNumbers.size() + " steps");
        Assert.assertEquals(rendered, bundleNumbers.subList(0, rendered.size()),
                "Rendered step numbers should match the bundle's leading sequence");

        // Heading + (optional) description: scroll through and assert each
        // step's content shows up. We use scrollUntilDescriptionVisible for
        // steps with descriptions (which doubles as a visibility wait); for
        // headings we just check after the description scroll positions us.
        int verified = 0;
        for (EmergencyStep step : steps) {
            // Headings always exist — scroll until visible. Long articles need
            // multiple swipes to bring later steps into the viewport, and the
            // recycler view virtualises content so off-screen rows aren't in
            // the accessibility tree.
            boolean titleOk = detailPage.scrollUntilTitleVisible(step.titleEn, 8);
            Assert.assertTrue(titleOk,
                    "Step " + step.number + " heading should render: '" + step.titleEn + "'");

            if (step.hasDescription()) {
                boolean ok = detailPage.scrollUntilDescriptionVisible(step.descriptionEn, 8);
                Assert.assertTrue(ok,
                        "Step " + step.number + " description should render: '"
                              + step.descriptionEn + "'");
            }
            verified++;
        }
        log("✅ TC22028: " + verified + " steps verified against the bundle");
    }

    // ========================================================================
    // TC22029 — Step descriptions render their bullet markers (»)
    // ========================================================================
    @Test(description = "TC22029 - Validate Display - Steps - Display with bullet points",
            groups = {"regression"})
    public void TC22029() {
        // Sanity-check the fixture: the bundle should include at least one
        // step whose description has a » marker, otherwise this test would
        // pass vacuously when bullets disappear from the UI.
        List<EmergencyStep> steps = bundleArticle.emergencySteps();
        boolean bundleHasBullet = steps.stream().anyMatch(EmergencyStep::hasBullet);
        Assert.assertTrue(bundleHasBullet,
                "Fixture invariant: '" + TARGET_TOPIC + "' should expose at least "
                      + "one bullet (»)-marked step in the bundle");

        // Find the first bulleted step and scroll it into view, then assert
        // the marker actually rendered on screen.
        EmergencyStep bulleted = steps.stream()
                .filter(EmergencyStep::hasBullet)
                .findFirst()
                .orElseThrow();
        boolean visible = detailPage.scrollUntilDescriptionVisible(bulleted.descriptionEn, 8);
        Assert.assertTrue(visible,
                "Bulleted step description should scroll into view: '"
                      + bulleted.descriptionEn + "'");
        Assert.assertTrue(detailPage.hasBulletInStepDescription(),
                "At least one rendered step description should contain the » marker");
        log("✅ TC22029: » bullet marker rendered in step " + bulleted.number);
    }
}
