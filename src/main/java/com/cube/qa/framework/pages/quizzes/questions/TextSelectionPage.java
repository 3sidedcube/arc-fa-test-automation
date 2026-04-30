package com.cube.qa.framework.pages.quizzes.questions;

import com.cube.qa.framework.pages.BasePage;
import io.appium.java_client.AppiumDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.util.ArrayList;
import java.util.List;

/**
 * Quiz question screen for the {@code textSelectionQuestion} type with
 * {@code uiType=DEFAULT}. Renders a question title, hint copy
 * ("Choose the correct answer" / "Choose the correct answers"), N tappable
 * answer rows with checkbox/toggle state, a selection counter
 * ("X of Y selected" Android / "X out of Y possible answers selected" iOS),
 * and a CHECK CTA that gates progression until at least one answer is picked.
 *
 * <p>After CHECK is tapped the result modal appears (Android: bottom sheet
 * with id {@code design_bottom_sheet}; iOS: lower-half overlay): a status
 * title ("CORRECT!" / "INCORRECT") plus a NEXT CTA and a close (×) button.
 *
 * <p>Locator notes (verified live, staging build 2484):
 * <ul>
 *   <li>Android question root uses ids {@code title}, {@code hint},
 *       {@code options_container}, {@code text_answers_selected},
 *       {@code button_next} (CHECK FrameLayout; inner Button has
 *       {@code arc_button} with text="CHECK"). Each answer row sits inside a
 *       LinearLayout {@code container} and exposes a {@code checkbox}
 *       (CheckBox, attribute {@code checked="true"}/"false") and a
 *       {@code title} TextView holding the answer text.</li>
 *   <li>iOS answer rows are {@code XCUIElementTypeToggle} elements with
 *       {@code name=&lt;answer text&gt;} and {@code value} ∈
 *       {@code {"selected","not selected","correct","incorrect"}}. CHECK is
 *       a Button {@code name="CHECK"}, disabled until any toggle is
 *       selected.</li>
 *   <li>Android result modal: bottom sheet with title "CORRECT!"/"INCORRECT"
 *       (id {@code title}), close button id {@code close_button}, NEXT button
 *       text="NEXT" inside id {@code next_btn}. iOS: status StaticText
 *       "CORRECT!"/"INCORRECT", NEXT button name="NEXT", close button is the
 *       Button with child Image name="cross".</li>
 * </ul>
 */
public class TextSelectionPage extends BasePage {

    private final String platform;

    private final List<By> questionTitleLocators;
    private final List<By> hintLocators;
    private final List<By> answerRowLocators;
    private final List<By> selectionCounterLocators;
    private final List<By> checkButtonLocators;
    private final List<By> resultModalLocators;
    private final List<By> resultTitleLocators;
    private final List<By> resultNextLocators;
    private final List<By> resultCloseLocators;

    public TextSelectionPage(AppiumDriver driver, String platform) {
        super(driver);
        this.platform = platform.toLowerCase();

        if (this.platform.equals("ios")) {
            // Question title sits above the hint StaticText. Both use the same
            // element type — disambiguate by the title's larger height (>50px
            // is the question; 21px is the hint). To stay device-agnostic we
            // simply pick the first StaticText whose label/value is *not* one
            // of the known chrome strings, but it's safer to expose a getter
            // that filters by length / position. Simpler approach: pick the
            // StaticText positioned highest on screen (lowest y) that isn't
            // a navigation chrome label — see #getQuestionTitle.
            questionTitleLocators = List.of(
                    By.xpath("//XCUIElementTypeStaticText[string-length(@name) > 0]")
            );
            hintLocators = List.of(
                    By.xpath("//XCUIElementTypeStaticText[@name='Choose the correct answer'"
                            + " or @name='Choose the correct answers']")
            );
            answerRowLocators = List.of(
                    By.xpath("//XCUIElementTypeToggle")
            );
            selectionCounterLocators = List.of(
                    By.xpath("//XCUIElementTypeStaticText[contains(@name,' possible answers selected')"
                            + " or contains(@name,'possible answer selected')]")
            );
            checkButtonLocators = List.of(
                    By.xpath("//XCUIElementTypeButton[@name='CHECK']")
            );
            // Result modal lives inside a sibling window/overlay. The status
            // text ("CORRECT!"/"INCORRECT") is unique to the modal and only
            // present once the bottom sheet animates in.
            resultModalLocators = List.of(
                    By.xpath("//XCUIElementTypeStaticText[@name='CORRECT!' or @name='INCORRECT']")
            );
            resultTitleLocators = List.of(
                    By.xpath("//XCUIElementTypeStaticText[@name='CORRECT!' or @name='INCORRECT']")
            );
            resultNextLocators = List.of(
                    By.xpath("//XCUIElementTypeButton[@name='NEXT']")
            );
            // The close (×) button has no name on iOS — locate it by its
            // child Image name="cross" and click the parent Button.
            resultCloseLocators = List.of(
                    By.xpath("//XCUIElementTypeButton[XCUIElementTypeImage[@name='cross']]")
            );
        } else {
            questionTitleLocators = List.of(
                    By.xpath("//*[@resource-id='com.cube.arc.fa:id/header_container']"
                            + "/*[@resource-id='com.cube.arc.fa:id/title']")
            );
            hintLocators = List.of(
                    By.id("com.cube.arc.fa:id/hint")
            );
            // Each answer row is wrapped in an androidx.cardview.widget.CardView
            // (clickable=true) sitting between 'options_container' and the
            // inner 'id=container' LinearLayout (clickable=false). We expose
            // the CardView as the row: it's the registered tap target, and
            // its child 'title' / 'checkbox' descendants drive selection-state
            // reads. Targeting the inner LinearLayout would silently no-op on
            // click() and an absence of matches here would also (incorrectly)
            // make isDisplayed() return false.
            answerRowLocators = List.of(
                    By.xpath("//*[@resource-id='com.cube.arc.fa:id/options_container']"
                            + "//androidx.cardview.widget.CardView"
                            + "[.//*[@resource-id='com.cube.arc.fa:id/container']]")
            );
            selectionCounterLocators = List.of(
                    By.id("com.cube.arc.fa:id/text_answers_selected")
            );
            // button_next is the FrameLayout wrapper; the inner Button
            // (arc_button) carries the CHECK text and the enabled flag.
            checkButtonLocators = List.of(
                    By.id("com.cube.arc.fa:id/button_next")
            );
            resultModalLocators = List.of(
                    By.id("com.cube.arc.fa:id/design_bottom_sheet")
            );
            // The modal's title TextView holds "CORRECT!"/"INCORRECT" and
            // shares its id (`title`) with the question screen — but the
            // question screen's title is hidden behind the bottom sheet, so
            // matching by text inside the bottom sheet is unambiguous.
            resultTitleLocators = List.of(
                    By.xpath("//*[@resource-id='com.cube.arc.fa:id/design_bottom_sheet']"
                            + "//*[@resource-id='com.cube.arc.fa:id/title']")
            );
            resultNextLocators = List.of(
                    By.xpath("//*[@resource-id='com.cube.arc.fa:id/next_btn']"
                            + "//*[@resource-id='com.cube.arc.fa:id/arc_button']"),
                    By.id("com.cube.arc.fa:id/next_btn")
            );
            resultCloseLocators = List.of(
                    By.id("com.cube.arc.fa:id/close_button")
            );
        }
    }

    // ---- Question screen ----------------------------------------------------

    public boolean isDisplayed() {
        return isPresent(checkButtonLocators) && isPresent(answerRowLocators);
    }

    /**
     * Returns the question title text. On iOS we filter the universe of
     * StaticTexts to those positioned in the upper-middle of the screen
     * (between the navigation bar and the answer list) and pick the longest
     * — short labels like "Question 1 of 5" or "Choose the correct answer"
     * are reliably shorter than any real question prompt.
     */
    public String getQuestionTitle() {
        if (!platform.equals("ios")) {
            return getText(questionTitleLocators);
        }
        // iOS: filter out the known chrome strings — Back nav, the
        // "Question N of M" counter, the "Choose the correct answer(s)"
        // hint, and the "X out of N possible answer(s) selected" running
        // counter. Whatever StaticText remains is the question prompt.
        // (Earlier we picked the longest StaticText, which works for
        // long multi-clause prompts but loses when the question is a
        // short one like "What is asthma?" — the counter outranks it on
        // length.)
        String best = "";
        for (WebElement el : driver.findElements(questionTitleLocators.get(0))) {
            try {
                String s = el.getAttribute("label");
                if (s == null || s.isBlank()) s = el.getText();
                if (s == null) continue;
                String t = s.trim();
                if (t.isEmpty()) continue;
                if (t.equals("Back")) continue;
                if (t.matches("Question \\d+ of \\d+")) continue;
                if (t.equals("Choose the correct answer")
                        || t.equals("Choose the correct answers")) continue;
                if (t.matches("\\d+ out of \\d+ possible answers? selected")) continue;
                // Keep the longest survivor in case a future build adds
                // additional chrome strings we haven't enumerated — the
                // question prompt is still by far the longest unfiltered
                // line on this screen.
                if (t.length() > best.length()) best = t;
            } catch (Exception ignored) {}
        }
        return best;
    }

    public String getHint() {
        return getText(hintLocators);
    }

    /** Counter copy verbatim (e.g. "0 of 2 selected" Android, "0 out of 2 possible answers selected" iOS). */
    public String getSelectionCounter() {
        return getText(selectionCounterLocators);
    }

    /** Number of answer rows currently rendered. */
    public int getAnswerCount() {
        return driver.findElements(answerRowLocators.get(0)).size();
    }

    /** Visible answer texts in the order they appear. */
    public List<String> getAnswerTexts() {
        List<String> out = new ArrayList<>();
        if (platform.equals("ios")) {
            for (WebElement toggle : driver.findElements(answerRowLocators.get(0))) {
                try {
                    String s = toggle.getAttribute("name");
                    if (s == null || s.isBlank()) s = toggle.getAttribute("label");
                    if (s != null && !s.isBlank()) out.add(s.trim());
                } catch (Exception ignored) {}
            }
            return out;
        }
        // Android: read each container's child title TextView.
        for (WebElement row : driver.findElements(answerRowLocators.get(0))) {
            try {
                WebElement title = row.findElement(
                        By.xpath(".//*[@resource-id='com.cube.arc.fa:id/title']"));
                String t = title.getText();
                if (t != null && !t.isBlank()) out.add(t.trim());
            } catch (Exception ignored) {}
        }
        return out;
    }

    /**
     * Tap the answer row whose visible text equals {@code answerText}. We trim
     * trailing whitespace on both sides because the bundle and the iOS
     * accessibility name occasionally include a stray trailing space (e.g.
     * "Hives or swelling of the face, neck, tongue or lips  ").
     */
    public void tapAnswer(String answerText) {
        WebElement row = findAnswerRow(answerText);
        if (row == null) {
            throw new RuntimeException("Answer row not found: '" + answerText + "'");
        }
        row.click();
    }

    /** True iff the answer row's selection state reads as selected/correct. */
    public boolean isAnswerSelected(String answerText) {
        WebElement row = findAnswerRow(answerText);
        if (row == null) return false;
        if (platform.equals("ios")) {
            String value = row.getAttribute("value");
            // After CHECK iOS reports "correct"/"incorrect" — those still mean
            // the user selected the row, so callers asking "is it selected"
            // should get true.
            return value != null
                    && (value.equals("selected") || value.equals("correct")
                        || value.equals("incorrect"));
        }
        // Android: the row's CheckBox child has @checked="true|false".
        try {
            WebElement cb = row.findElement(
                    By.xpath(".//*[@resource-id='com.cube.arc.fa:id/checkbox']"));
            return "true".equals(cb.getAttribute("checked"));
        } catch (Exception e) {
            return false;
        }
    }

    /** True iff the CHECK CTA is enabled (i.e. at least one answer selected). */
    public boolean isCheckEnabled() {
        try {
            WebElement btn = driver.findElement(checkButtonLocators.get(0));
            String enabled = btn.getAttribute("enabled");
            // Android FrameLayout wrapper reports enabled correctly; iOS
            // Button reports enabled="true|false" directly.
            return enabled == null || "true".equals(enabled);
        } catch (Exception e) {
            return false;
        }
    }

    public void tapCheck() {
        tap(checkButtonLocators);
    }

    // ---- Result modal -------------------------------------------------------

    public boolean isResultModalVisible() {
        return isPresent(resultModalLocators) && isPresent(resultTitleLocators);
    }

    /** "CORRECT!" or "INCORRECT" — verbatim, including the trailing bang. */
    public String getResultTitle() {
        return getText(resultTitleLocators);
    }

    /**
     * Body / feedback copy inside the result bottom sheet — the localized
     * {@code failMessage} when INCORRECT, empty for CORRECT (the bundle does
     * not ship a correct-modal message and the live UI shows only the title
     * + NEXT button on a passed answer). Returns "" rather than throwing
     * when no body element is present so callers can branch cleanly on
     * "is there a body to assert against the bundle?".
     */
    public String getResultBody() {
        if (platform.equals("ios")) {
            // Scope to StaticTexts that follow the CORRECT!/INCORRECT title in
            // document order — the modal body sits immediately after the
            // title, while the question screen's prompt (often longer than
            // the failMessage) sits before it. An unscoped pick-the-longest
            // strategy would otherwise capture the question title from
            // behind the modal.
            String best = "";
            for (WebElement t : driver.findElements(By.xpath(
                    "//XCUIElementTypeStaticText[@name='CORRECT!' or @name='INCORRECT']"
                            + "/following::XCUIElementTypeStaticText"))) {
                try {
                    String s = t.getAttribute("label");
                    if (s == null || s.isBlank()) s = t.getText();
                    if (s == null || s.isBlank()) continue;
                    if (s.equals("CORRECT!") || s.equals("INCORRECT")
                            || s.equals("NEXT")) continue;
                    if (s.length() > best.length()) best = s;
                } catch (Exception ignored) {}
            }
            return best.trim();
        }
        // Android: scope to the bottom-sheet subtree and read TextViews;
        // skip the known chrome strings (title + button label).
        String best = "";
        for (WebElement t : driver.findElements(By.xpath(
                "//*[@resource-id='com.cube.arc.fa:id/design_bottom_sheet']"
                        + "//android.widget.TextView"))) {
            try {
                String s = t.getText();
                if (s == null || s.isBlank()) continue;
                if (s.equals("CORRECT!") || s.equals("INCORRECT")
                        || s.equals("NEXT")) continue;
                if (s.length() > best.length()) best = s;
            } catch (Exception ignored) {}
        }
        return best.trim();
    }

    public void tapResultNext() {
        tap(resultNextLocators);
    }

    public void tapResultClose() {
        tap(resultCloseLocators);
    }

    // ---- Internals ----------------------------------------------------------

    private WebElement findAnswerRow(String answerText) {
        String needle = answerText == null ? "" : answerText.trim();
        if (platform.equals("ios")) {
            // Toggle name == answer text (with possible trailing whitespace).
            // Try exact match first, then fall back to a "starts-with" predicate
            // that tolerates the bundle vs. UI whitespace mismatch.
            for (WebElement t : driver.findElements(
                    By.xpath("//XCUIElementTypeToggle[@name='" + needle + "']"))) {
                return t;
            }
            for (WebElement t : driver.findElements(
                    By.xpath("//XCUIElementTypeToggle[starts-with(normalize-space(@name),'"
                            + needle + "')]"))) {
                return t;
            }
            return null;
        }
        // Android: find the row whose child title TextView text matches.
        for (WebElement row : driver.findElements(answerRowLocators.get(0))) {
            try {
                WebElement title = row.findElement(
                        By.xpath(".//*[@resource-id='com.cube.arc.fa:id/title']"));
                String t = title.getText();
                if (t != null && t.trim().equals(needle)) return row;
            } catch (Exception ignored) {}
        }
        return null;
    }
}
