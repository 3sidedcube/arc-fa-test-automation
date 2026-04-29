package tests;

import com.cube.qa.framework.pages.home.GiveCareTabPage;
import com.cube.qa.framework.pages.home.TabPage;
import com.cube.qa.framework.pages.home.TabPage.Tab;
import com.cube.qa.framework.pages.onboarding.TooltipsPage;
import com.cube.qa.framework.testdata.loader.ContentBundleLoader;
import com.cube.qa.framework.testdata.model.Article;
import com.cube.qa.framework.utils.BaseTest;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Give Care tab — emergency tools, Call 911 component, and the alphabetised
 * list of emergency topics.
 *
 * Testiny: ARC First Aid - v4.0.0 > Give Care Tab
 *
 * <p>Each test starts from a fresh install, walks the guest onboarding,
 * dismisses home-screen tooltips, and lands on the Give Care tab. Emergency
 * topic titles are validated against the live CDN article manifest
 * ({@code type:"emergencyArticle"}, tabLocation contains {@code "EMERGENCY"})
 * so renaming a topic in CMS won't false-fail the suite.
 *
 * <p>TC22019 / TC22020 deliberately stop at "call sheet appears" and cancel —
 * placing an actual 911 call from automation is not safe. On iOS, TC22020
 * is folded into TC22019 because SpringBoard rate-limits tel:911 re-prompts
 * within a single app session; see TC22020 for details.
 */
public class GiveCareTabTest extends BaseTest {

    private TabPage tabPage;
    private GiveCareTabPage giveCarePage;
    private TooltipsPage tooltipsPage;

    @BeforeMethod(alwaysRun = true, dependsOnMethods = "setUp")
    public void setUpTest() {
        tabPage = pages.tabPage();
        giveCarePage = pages.giveCareTabPage();
        tooltipsPage = pages.tooltipsPage();

        RuntimeException lastFailure = null;
        for (int attempt = 0; attempt < 2; attempt++) {
            try {
                walkOnboardingAsGuest();
                int safety = 0;
                while (safety++ < 8 && tooltipsPage.isGotItButtonPresent()) {
                    try { tooltipsPage.tapGotIt(); } catch (RuntimeException e) { break; }
                }
                if (tabPage.isTabPresent(Tab.GIVE_CARE)) tabPage.tapTab(Tab.GIVE_CARE);
                // Settle: wait for the Give Care Topics section header.
                long deadline = System.currentTimeMillis() + 8000;
                while (System.currentTimeMillis() < deadline
                        && !giveCarePage.hasGiveCareTopicsHeader()) {
                    try { Thread.sleep(250); } catch (InterruptedException ignored) {}
                }
                // The iOS suite keeps the app between tests (noReset=true), so
                // a previous test's scroll position may persist. Reset to top
                // so every test starts with the inline header CTA in view —
                // critical for TC22020 which taps Call 911 on a fresh layout.
                giveCarePage.scrollToTop();
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

    /**
     * en-US titles of every {@code emergencyArticle} whose tabLocation
     * includes "EMERGENCY". This is the source of truth for the Give Care
     * topics list. The app sorts client-side, so we sort here too when
     * comparing display order.
     */
    private List<String> bundleEmergencyTopicTitles() {
        Collection<Article> articles = ContentBundleLoader.articlesForTab("EMERGENCY");
        return articles.stream()
                .filter(a -> "emergencyArticle".equals(a.type))
                .map(Article::titleEn)
                .filter(t -> t != null && !t.isBlank())
                .collect(Collectors.toList());
    }

    private List<String> bundleEmergencyTopicsAlphabetical() {
        List<String> all = new ArrayList<>(bundleEmergencyTopicTitles());
        all.sort(Comparator.comparing(s -> s.toLowerCase(Locale.ENGLISH)));
        return all;
    }

    /**
     * Filter the rendered titles down to bundle-known emergency topics. The
     * Give Care tab also shows tool labels and the Emergency Services CTA in
     * the same accessibility tree; intersecting with the bundle isolates the
     * topic rows reliably without binding to private resource-ids.
     */
    private List<String> renderedEmergencyTopicsInBundleOrder(List<String> rendered) {
        List<String> bundleTitles = bundleEmergencyTopicTitles();
        List<String> out = new ArrayList<>();
        for (String r : rendered) {
            if (bundleTitles.stream().anyMatch(b -> b.equalsIgnoreCase(r))) {
                out.add(r);
            }
        }
        return out;
    }

    // ========================================================================
    // TC31808 — Emergency topics list is alphabetical
    // ========================================================================
    @Test(description = "TC31808 - Validate Emergency Steps Sorted Alphabetically",
            groups = {"regression"})
    public void TC31808() {
        List<String> rendered = renderedEmergencyTopicsInBundleOrder(
                giveCarePage.getEmergencyTopicTitles());
        Assert.assertFalse(rendered.isEmpty(),
                "At least one emergency topic should render");

        List<String> expectedSorted = new ArrayList<>(rendered);
        expectedSorted.sort(Comparator.comparing(s -> s.toLowerCase(Locale.ENGLISH)));

        log("📋 Rendered order: " + rendered);
        log("📋 Sorted order:   " + expectedSorted);
        Assert.assertEquals(rendered, expectedSorted,
                "Emergency topics should be displayed in alphabetical order");
        log("✅ TC31808: " + rendered.size() + " emergency topics rendered alphabetically");
    }

    // ========================================================================
    // TC31809 — Emergency tools row in the header (Call 911, Audible Metronome,
    //           Hospital Finder)
    // ========================================================================
    @Test(description = "TC31809 - Validate Emergency Tools Displayed in Header",
            groups = {"regression"})
    public void TC31809() {
        Assert.assertTrue(giveCarePage.hasInlineCall911Cta(),
                "Header should expose a Call 911 tool");
        Assert.assertTrue(giveCarePage.hasAudibleMetronome(),
                "Header should expose Audible Metronome");
        Assert.assertTrue(giveCarePage.hasHospitalFinder(),
                "Header should expose Hospital Finder");
        log("✅ TC31809: Call 911 / Audible Metronome / Hospital Finder all present");
    }

    // ========================================================================
    // TC22017 — Unscrolled state of the Call 911 affordance
    //
    // Authored against v3.4.0, when Give Care had a dedicated "Emergency
    // Services" card with title + description + inline CTA. v4.0.0 dropped
    // that card and hoisted the Call 911 CTA into the header toolbar. We
    // assert the v4 reality: header CTA visible, sticky CTA NOT yet shown.
    // ========================================================================
    @Test(description = "TC22017 - Validate Display - Call 911 - Not Scrolled",
            groups = {"regression"})
    public void TC22017() {
        // We're at the top of the screen courtesy of @BeforeMethod.
        Assert.assertTrue(giveCarePage.hasInlineCall911Cta(),
                "Header Call 911 CTA should render at the top of Give Care");
        Assert.assertFalse(giveCarePage.hasStickyCall911(),
                "Sticky Call 911 button should not appear before the user scrolls");
        Assert.assertTrue(giveCarePage.hasGiveCareTopicsHeader(),
                "'Give Care Topics' section heading should render below the header");
        log("✅ TC22017: header Call 911 visible + no sticky CTA (unscrolled)");
    }

    // ========================================================================
    // TC22018 — Call 911 sticks to the bottom once the screen is scrolled
    // ========================================================================
    @Test(description = "TC22018 - Validate Display - Call 911 - Scrolled",
            groups = {"regression"})
    public void TC22018() {
        // A couple of swipes scrolls past the inline CTA into the topic list.
        for (int i = 0; i < 2; i++) {
            giveCarePage.scrollDown();
            try { Thread.sleep(400); } catch (InterruptedException ignored) {}
        }
        Assert.assertTrue(giveCarePage.hasStickyCall911(),
                "Call 911 should pin to the bottom of the screen once scrolled");
        log("✅ TC22018: Sticky Call 911 button visible at bottom after scroll");
    }

    // ========================================================================
    // TC22019 — Tap Call 911 → call sheet/dialer opens, Cancel returns user
    //           to Give Care (cancelled, never actually placed).
    //
    // On iOS this single test also satisfies TC22020: SpringBoard rate-limits
    // tel:911 confirmations per bundle id, so a second tap within the same
    // app session silently no-ops and we'd never see the sheet again. We do
    // one real tel: cycle here and assert the full path; TC22020 below
    // skips on iOS for that reason. Android dialer has no such rate limit,
    // so TC22020 still runs there as an independent cancel-pathway probe.
    // ========================================================================
    @Test(description = "TC22019 - Verify users can call 911", groups = {"smoke", "regression"})
    public void TC22019() {
        giveCarePage.tapCall911();
        // Native sheet/dialer takes a moment to surface.
        long deadline = System.currentTimeMillis() + 5000;
        boolean sheetSeen = false;
        while (System.currentTimeMillis() < deadline) {
            if (giveCarePage.isCallSheetPresent()) { sheetSeen = true; break; }
            try { Thread.sleep(200); } catch (InterruptedException ignored) {}
        }
        // ALWAYS dismiss before asserting — leaving a call sheet up risks an
        // accidental dial if the test runner moves on.
        giveCarePage.cancelCallSheet();
        Assert.assertTrue(sheetSeen,
                "Tapping Call 911 should open the system call sheet/dialer");

        // Settle: re-activating FA / dismissing the dialer takes a beat.
        long backDeadline = System.currentTimeMillis() + 5000;
        while (System.currentTimeMillis() < backDeadline
                && !giveCarePage.hasGiveCareTopicsHeader()) {
            try { Thread.sleep(250); } catch (InterruptedException ignored) {}
        }
        Assert.assertTrue(giveCarePage.hasGiveCareTopicsHeader(),
                "Cancelling the call should leave the user back on Give Care");
        log("✅ TC22019: Call sheet appeared, Cancel returned to Give Care");
    }

    // ========================================================================
    // TC22020 — Tap Call 911 → Cancel returns to Give Care
    //
    // Skipped on iOS: SpringBoard rate-limits tel:911 confirmations per
    // bundle id, so once TC22019 has confirmed-and-cancelled, a second tap
    // in the same session silently no-ops (no SpringBoard sheet appears).
    // TC22019 has been extended to cover the "back on Give Care after
    // cancel" assertion that this case is about, so iOS coverage is intact.
    // Android has no such rate limit and runs this independently.
    // ========================================================================
    @Test(description = "TC22020 - Verify users can cancel calling 911", groups = {"regression"})
    public void TC22020() {
        if ("ios".equalsIgnoreCase(config.getPlatform())) {
            throw new org.testng.SkipException(
                    "iOS SpringBoard rate-limits tel:911 re-prompts within a "
                  + "single app session — folded into TC22019.");
        }
        giveCarePage.tapCall911();
        // 8s window — on real iOS the SpringBoard call confirmation can take
        // a beat longer to surface than on Android's dialer.
        long deadline = System.currentTimeMillis() + 8000;
        while (System.currentTimeMillis() < deadline
                && !giveCarePage.isCallSheetPresent()) {
            try { Thread.sleep(200); } catch (InterruptedException ignored) {}
        }
        Assert.assertTrue(giveCarePage.isCallSheetPresent(),
                "Precondition: call sheet should be open before we cancel");

        giveCarePage.cancelCallSheet();
        // Give the system a moment to dismiss the sheet.
        try { Thread.sleep(1500); } catch (InterruptedException ignored) {}

        Assert.assertFalse(giveCarePage.isCallSheetPresent(),
                "Call sheet should be dismissed after Cancel");
        Assert.assertTrue(giveCarePage.hasGiveCareTopicsHeader(),
                "Should be back on Give Care tab after cancelling");
        log("✅ TC22020: Cancel dismissed the call sheet and returned to Give Care");
    }

    // ========================================================================
    // TC22021 — Emergency topics list matches the CMS bundle
    // ========================================================================
    @Test(description = "TC22021 - Validate list of Emergency Topics", groups = {"regression"})
    public void TC22021() {
        List<String> rendered = renderedEmergencyTopicsInBundleOrder(
                giveCarePage.getEmergencyTopicTitles());

        // Compare on case-insensitive sets — order is asserted separately by
        // TC31808; this test is about content parity with the CDN.
        List<String> renderedSorted = new ArrayList<>(rendered);
        renderedSorted.sort(Comparator.comparing(s -> s.toLowerCase(Locale.ENGLISH)));
        List<String> expectedSorted = bundleEmergencyTopicsAlphabetical();

        log("📋 Bundle (" + expectedSorted.size() + "): " + expectedSorted);
        log("📋 App    (" + renderedSorted.size() + "): " + renderedSorted);
        Assert.assertEquals(renderedSorted, expectedSorted,
                "Emergency topic list should match the bundle's emergencyArticle/EMERGENCY set");
        log("✅ TC22021: " + rendered.size() + " emergency topics match the CDN bundle");
    }
}
