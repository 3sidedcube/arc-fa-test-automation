package com.cube.qa.framework.pages.home;

import com.cube.qa.framework.pages.BasePage;
import io.appium.java_client.AppiumDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Give Care tab — emergency-first content. Layout (v4.0.0), top to bottom:
 *
 * <ol>
 *   <li>Header toolbar: Call 911 / Audible Metronome / Hospital Finder.</li>
 *   <li>"Give Care Topics" section heading.</li>
 *   <li>Alphabetised list of {@code type:"emergencyArticle"} content from the
 *       CDN article manifest.</li>
 *   <li>Once scrolled, a sticky CALL 911 button pins to the bottom.</li>
 * </ol>
 *
 * <p>The Emergency Services / Emergency Number component shown in the v3.4.0
 * test plan no longer exists in v4.0.0 — the inline Call 911 CTA was hoisted
 * into the header. Tests reference the live layout, not the legacy one.
 *
 * <p>Tool labels are app-strings — per product they rarely change so we keep
 * them hardcoded rather than wiring a loader. Topic titles are dynamic content
 * and must be validated against the CDN bundle.
 */
public class GiveCareTabPage extends BasePage {

    private final String platform;

    private final List<By> giveCareTopicsHeaderLocators;
    private final List<By> inlineCall911CtaLocators;
    private final List<By> stickyCall911CtaLocators;
    private final List<By> audibleMetronomeLocators;
    private final List<By> hospitalFinderLocators;

    public GiveCareTabPage(AppiumDriver driver, String platform) {
        super(driver);
        this.platform = platform.toLowerCase();

        if (this.platform.equals("ios")) {
            giveCareTopicsHeaderLocators = List.of(
                    By.name("Give Care Topics"),
                    By.xpath("//XCUIElementTypeStaticText[@name='Give Care Topics']")
            );
            // Header CTA and sticky CTA share the "Call 911" accessibility name
            // on iOS — they're disambiguated by rect (sticky lives in the lower
            // 25% of the viewport).
            inlineCall911CtaLocators = List.of(
                    By.name("Call 911"),
                    By.xpath("//XCUIElementTypeButton[@name='Call 911']")
            );
            stickyCall911CtaLocators = inlineCall911CtaLocators;
            audibleMetronomeLocators = List.of(
                    By.name("Audible Metronome"),
                    By.xpath("//*[@name='Audible Metronome']")
            );
            hospitalFinderLocators = List.of(
                    By.name("Hospital Finder"),
                    By.xpath("//*[@name='Hospital Finder']")
            );
        } else {
            // Resource-ids confirmed via uiautomator dump on v4.0.0 prod APK.
            giveCareTopicsHeaderLocators = List.of(
                    By.id("com.cube.arc.fa:id/emergency_topics_title")
            );
            inlineCall911CtaLocators = List.of(
                    By.id("com.cube.arc.fa:id/call_911_cta")
            );
            stickyCall911CtaLocators = List.of(
                    By.id("com.cube.arc.fa:id/sticky_call_911_btn")
            );
            audibleMetronomeLocators = List.of(
                    By.id("com.cube.arc.fa:id/audible_metronome_cta")
            );
            hospitalFinderLocators = List.of(
                    By.id("com.cube.arc.fa:id/hospital_finder_cta")
            );
        }
    }

    // ---- Header tool row ---------------------------------------------------

    public boolean hasInlineCall911Cta() {
        return isPresent(inlineCall911CtaLocators);
    }

    public boolean hasAudibleMetronome() {
        return isPresent(audibleMetronomeLocators);
    }

    public boolean hasHospitalFinder() {
        return isPresent(hospitalFinderLocators);
    }

    /** "Give Care Topics" section header — used as the "tab loaded" signal. */
    public boolean hasGiveCareTopicsHeader() {
        return isPresent(giveCareTopicsHeaderLocators);
    }

    /** Tap the inline (header) Call 911 CTA. */
    public void tapCall911() {
        tap(inlineCall911CtaLocators);
    }

    // ---- Sticky CTA --------------------------------------------------------

    /**
     * On Android, the sticky CTA has a dedicated resource-id
     * ({@code sticky_call_911_btn}) so a plain presence check is enough — and
     * it correctly returns false before the user has scrolled.
     *
     * <p>On iOS, both buttons share {@code @name="Call 911"} so we fall back
     * to a rect filter: a button whose center sits in the lower 25% of the
     * viewport.
     */
    public boolean hasStickyCall911() {
        if (!platform.equals("ios")) {
            return isPresent(stickyCall911CtaLocators);
        }
        List<WebElement> hits = driver.findElements(stickyCall911CtaLocators.get(0));
        if (hits.isEmpty()) return false;
        org.openqa.selenium.Dimension size = driver.manage().window().getSize();
        int stickyZoneTop = (int) (size.height * 0.75);
        for (WebElement el : hits) {
            try {
                org.openqa.selenium.Rectangle r = el.getRect();
                int center = r.y + r.height / 2;
                if (center >= stickyZoneTop) return true;
            } catch (Exception ignored) {}
        }
        return false;
    }

    // ---- Emergency Topics list --------------------------------------------

    /**
     * Titles of the emergency topics rendered in the list, in display order.
     * Collects from current scroll position (caller is responsible for being
     * scrolled to the top of the list) and sweeps downwards, deduping by
     * title, until two consecutive scrolls reveal nothing new.
     *
     * <p>Used by TC31808 (alphabetical sort) and TC22021 (CMS parity) — the
     * caller decides whether to compare to the bundle as-is or sorted.
     */
    /**
     * Scroll the emergency-topics list until a row matching {@code titleEn}
     * is on screen, then tap it. Caller is responsible for being on the
     * Give Care tab — see {@code GiveCareTabTest#setUpTest}.
     *
     * @throws RuntimeException if the topic doesn't appear within a bounded
     *         number of scroll passes (likely a CMS rename or a typo in the
     *         test's expected title).
     */
    public void tapEmergencyTopic(String titleEn) {
        if (titleEn == null || titleEn.isBlank()) {
            throw new IllegalArgumentException("titleEn must not be blank");
        }
        for (int pass = 0; pass < 10; pass++) {
            for (String rendered : currentTopicTitles()) {
                if (titleEn.equalsIgnoreCase(rendered)) {
                    tapTopicRow(rendered);
                    return;
                }
            }
            scrollDown();
            try { Thread.sleep(400); } catch (InterruptedException ignored) {}
        }
        throw new RuntimeException(
                "Emergency topic '" + titleEn + "' was not found in the list "
              + "after 10 scroll passes — check the bundle title or whether "
              + "the row needs longer to render.");
    }

    private void tapTopicRow(String titleEn) {
        if (platform.equals("ios")) {
            // The row is the parent button of the matching StaticText; the
            // StaticText itself usually accepts taps too on iOS, so try it
            // directly first, then fall back to the parent button by name.
            List<By> locators = List.of(
                    By.xpath("//XCUIElementTypeButton[XCUIElementTypeImage[@name='iconChevronListItem']]"
                          + "/XCUIElementTypeStaticText[@name=" + xpathLiteral(titleEn) + "]"),
                    By.xpath("//XCUIElementTypeButton[contains(@name," + xpathLiteral(titleEn) + ")]"));
            tap(locators);
        } else {
            tap(List.of(
                    By.xpath("//*[@resource-id='com.cube.arc.fa:id/rv_emergency_articles']"
                          + "//*[@resource-id='com.cube.arc.fa:id/chevron_link_title' and @text="
                          + xpathLiteral(titleEn) + "]")));
        }
    }

    /**
     * XPath 1.0 has no string escape — wrap a value containing both quote
     * styles via {@code concat(...)}. Apostrophes appear in topic titles
     * like {@code "Choking: Adult and Child"} (none today, but futureproof).
     */
    private static String xpathLiteral(String s) {
        if (!s.contains("'")) return "'" + s + "'";
        if (!s.contains("\"")) return "\"" + s + "\"";
        StringBuilder sb = new StringBuilder("concat(");
        String[] parts = s.split("'");
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) sb.append(",\"'\",");
            sb.append("'").append(parts[i]).append("'");
        }
        return sb.append(")").toString();
    }

    public List<String> getEmergencyTopicTitles() {
        Map<String, Boolean> seen = new LinkedHashMap<>();
        int noGrowStreak = 0;
        for (int pass = 0; pass < 10; pass++) {
            int before = seen.size();
            for (String t : currentTopicTitles()) {
                if (t == null || t.isBlank()) continue;
                seen.putIfAbsent(t.trim(), true);
            }
            if (seen.size() == before) {
                if (++noGrowStreak >= 2) break;
            } else {
                noGrowStreak = 0;
            }
            scrollDown();
            try { Thread.sleep(500); } catch (InterruptedException ignored) {}
        }
        return new ArrayList<>(seen.keySet());
    }

    private List<String> currentTopicTitles() {
        List<String> out = new ArrayList<>();
        if (platform.equals("ios")) {
            // Topic rows are XCUIElementTypeButton with an iconChevronListItem
            // image child. Read the inner StaticText (clean title) rather than
            // the button's own @name — some rows are prefixed with a badge name
            // like "CPR Badge, Cardiac Arrest: ..." which would never match the
            // bundle title.
            List<WebElement> els = driver.findElements(By.xpath(
                    "//XCUIElementTypeButton[XCUIElementTypeImage[@name='iconChevronListItem']]"
                  + "/XCUIElementTypeStaticText"));
            for (WebElement e : els) {
                try {
                    String n = e.getAttribute("name");
                    if (n != null) out.add(n);
                } catch (Exception ignored) {}
            }
        } else {
            // Scope to rv_emergency_articles so we don't pick up
            // chevron_link_title elements rendered elsewhere on screen.
            List<WebElement> els = driver.findElements(By.xpath(
                    "//*[@resource-id='com.cube.arc.fa:id/rv_emergency_articles']"
                  + "//*[@resource-id='com.cube.arc.fa:id/chevron_link_title']"));
            for (WebElement e : els) {
                try {
                    String t = e.getText();
                    if (t != null) out.add(t);
                } catch (Exception ignored) {}
            }
        }
        return out;
    }

    // ---- Scroll helpers ---------------------------------------------------

    public void scrollDown() {
        if (platform.equals("ios")) {
            driver.executeScript("mobile: swipe", Map.of("direction", "up"));
        } else {
            driver.executeScript("mobile: scrollGesture", Map.of(
                    "left", 100, "top", 500, "width", 800, "height", 1200,
                    "direction", "down", "percent", 0.7));
        }
    }

    /** Fling back to the top of the Give Care tab. */
    public void scrollToTop() {
        for (int i = 0; i < 6; i++) {
            if (hasInlineCall911Cta() && !hasStickyCall911()) return;
            if (platform.equals("ios")) {
                driver.executeScript("mobile: swipe", Map.of("direction", "down"));
            } else {
                driver.executeScript("mobile: scrollGesture", Map.of(
                        "left", 100, "top", 500, "width", 800, "height", 1200,
                        "direction", "up", "percent", 0.9));
            }
            try { Thread.sleep(300); } catch (InterruptedException ignored) {}
        }
    }

    // ---- Call sheet detection (911 dialer) --------------------------------

    /**
     * After tapping Call 911 the OS opens its native call sheet (iOS) or
     * dialer activity (Android). We never want an actual outbound 911 call
     * during automation — the test flow taps Call → asserts the sheet/dialer
     * appears → cancels.
     */
    public boolean isCallSheetPresent() {
        if (platform.equals("ios")) {
            // The system call-confirmation on real iOS 26 lives in SpringBoard
            // and overlays FA without backgrounding it (queryAppState stays 4),
            // so we can't lean on app state. The clean signal is the
            // XCUIElementTypeApplication root's `visible` attribute — it flips
            // to "false" while SpringBoard owns the screen and back to "true"
            // once the user dismisses the sheet.
            try {
                List<WebElement> apps = driver.findElements(
                        By.xpath("//XCUIElementTypeApplication"));
                if (!apps.isEmpty()) {
                    String vis = apps.get(0).getAttribute("visible");
                    if ("false".equalsIgnoreCase(vis)) return true;
                }
            } catch (Exception ignored) {}
            // Fallback: in-app alert/sheet (covers simulator builds where the
            // tel: confirmation renders inside FA's own tree).
            return !driver.findElements(By.xpath(
                    "//XCUIElementTypeAlert | //XCUIElementTypeSheet")).isEmpty();
        }
        // Android: FA's own Give Care tab contains "911" text so a generic
        // text probe is useless. We rely on foreground package — anything
        // outside FA (typically the dialer) means the call sheet is up.
        try {
            Object pkg = driver.executeScript("mobile: getCurrentPackage", Map.of());
            String currentPkg = pkg == null ? "" : pkg.toString();
            return !currentPkg.startsWith("com.cube.arc.fa") && !currentPkg.isEmpty();
        } catch (Exception ignored) {}
        return false;
    }

    /**
     * Cancel out of the call sheet/dialer without placing a call. On iOS we
     * tap the Cancel button. On Android we force-stop the dialer activity —
     * the OS takes us straight back to the previous foreground app, which
     * avoids leaving the dialer in front for the next test's
     * {@code mobile: clearApp} (which only clears the FA app, not the dialer).
     */
    public void cancelCallSheet() {
        if (platform.equals("ios")) {
            // First try the in-app Cancel button — this works on simulator
            // builds where the tel: confirmation renders inside FA's tree.
            for (By by : List.of(By.name("Cancel"),
                    By.xpath("//XCUIElementTypeButton[@name='Cancel']"))) {
                List<WebElement> hits = driver.findElements(by);
                if (!hits.isEmpty()) {
                    try { hits.get(0).click(); return; } catch (Exception ignored) {}
                }
            }
            // Real-device path: the call confirmation is owned by SpringBoard
            // and unreachable from FA's session. Re-activating FA brings it
            // back to the foreground and dismisses the system sheet — same
            // user-visible result as tapping Cancel.
            try {
                driver.executeScript("mobile: activateApp",
                        Map.of("bundleId", "com.americanredcross.firstaid"));
            } catch (Exception ignored) {}
            return;
        }
        // Android: force-stop dialer packages via adb, then bring the FA app
        // back to the foreground. We can't lean on mobile:shell because the
        // Appium server isn't started with --relaxed-security; ADB on the
        // host is the reliable escape hatch (same approach as LearnTabPage
        // for adb swipes). Re-activating FA is what the user expects when
        // they "cancel" the dial — back to where they tapped Call 911.
        String udid = System.getenv().getOrDefault("ANDROID_UDID", "33071FDH2007QH");
        for (String pkg : List.of("com.google.android.dialer", "com.android.dialer")) {
            try {
                Process p = new ProcessBuilder(
                        "adb", "-s", udid, "shell", "am", "force-stop", pkg)
                        .redirectErrorStream(true).start();
                p.waitFor();
            } catch (Exception ignored) {}
        }
        try {
            driver.executeScript("mobile: activateApp",
                    Map.of("appId", "com.cube.arc.fa"));
        } catch (Exception ignored) {}
    }
}
