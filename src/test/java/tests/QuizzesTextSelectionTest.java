package tests;

import com.cube.qa.framework.pages.home.TabPage;
import com.cube.qa.framework.pages.home.TabPage.Tab;
import com.cube.qa.framework.pages.onboarding.TooltipsPage;
import com.cube.qa.framework.pages.quizzes.QuizzesPage;
import com.cube.qa.framework.pages.quizzes.questions.TextSelectionPage;
import com.cube.qa.framework.testdata.loader.ContentBundleLoader;
import com.cube.qa.framework.testdata.model.QuizAnswer;
import com.cube.qa.framework.testdata.model.QuizDetail;
import com.cube.qa.framework.testdata.model.QuizQuestion;
import com.cube.qa.framework.utils.BaseTest;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.List;

/**
 * Quizzes — text-selection question type ({@code uiType=DEFAULT}). Covers the
 * shared question-screen behavior (selection state, multi-select cap, CHECK
 * gating) and the result modal (CORRECT!/INCORRECT title, NEXT, ×).
 *
 * Testiny: ARC First Aid - v4.0.0 > Quizzes
 *
 * <p>Fixture strategy: tests don't hardcode a quiz id. Each test asks the
 * {@link ContentBundleLoader} for the first quiz whose Q1 matches the criteria
 * it needs (single-correct vs multi-correct). The bundle is the source of
 * truth for which answer texts to tap and how many corrects to expect, so a
 * CMS edit (renaming an answer, flipping isCorrect, adding a quiz) won't
 * false-fail the suite — it just shifts which quiz the test exercises.
 *
 * <p>Bundle assumption: at least one DEFAULT text-selection quiz with a
 * single-correct Q1 and at least one with a 2-correct Q1 must exist on the
 * active env. Verified against staging build 2484 — many single-correct
 * fixtures, "Anaphylaxis" is the canonical 2-correct fixture.
 */
public class QuizzesTextSelectionTest extends BaseTest {

    private TabPage tabPage;
    private QuizzesPage quizzesPage;
    private TextSelectionPage textSelectionPage;
    private TooltipsPage tooltipsPage;

    @BeforeMethod(alwaysRun = true, dependsOnMethods = "setUp")
    public void setUpTest() {
        tabPage = pages.tabPage();
        quizzesPage = pages.quizzesPage();
        textSelectionPage = pages.textSelectionPage();
        tooltipsPage = pages.tooltipsPage();

        RuntimeException lastFailure = null;
        for (int attempt = 0; attempt < 2; attempt++) {
            try {
                walkOnboardingAsGuest();
                int safety = 0;
                while (safety++ < 8 && tooltipsPage.isGotItButtonPresent()) {
                    try { tooltipsPage.tapGotIt(); } catch (RuntimeException e) { break; }
                }
                // Interleave tooltip dismissal with tab-presence polling for up
                // to 15s. On the real Pixel 7 the post-onboarding sequence is
                // not deterministic: a tooltip may animate in *after* the
                // initial dismissal pass, or the bottom-nav fragment may take
                // a beat to inflate after the last tooltip clears. A single
                // pre-asserted poll race-loses to those animations roughly 1
                // in 7 runs. The combined loop keeps clearing whatever
                // overlay is on top until the tab tile is queryable.
                long tabDeadline = System.currentTimeMillis() + 15000;
                while (System.currentTimeMillis() < tabDeadline) {
                    if (tooltipsPage.isGotItButtonPresent()) {
                        try { tooltipsPage.tapGotIt(); } catch (RuntimeException ignored) {}
                    }
                    if (tabPage.isTabPresent(Tab.QUIZZES)) break;
                    try { Thread.sleep(250); } catch (InterruptedException ignored) {}
                }
                Assert.assertTrue(tabPage.isTabPresent(Tab.QUIZZES),
                        "Quizzes tab should be visible after onboarding");
                tabPage.tapTab(Tab.QUIZZES);
                long deadline = System.currentTimeMillis() + 8000;
                while (System.currentTimeMillis() < deadline && !quizzesPage.isDisplayed()) {
                    try { Thread.sleep(250); } catch (InterruptedException ignored) {}
                }
                Assert.assertTrue(quizzesPage.isDisplayed(),
                        "Quizzes tab landing should render the 'Quiz Topics' header");
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
    // Helpers
    // ========================================================================

    /**
     * Land on Q1 of a fixture quiz whose Q1 is a DEFAULT text-selection with
     * exactly {@code expectedCorrect} correct answers. Returns the bundle
     * detail so tests can reference the same content the app is rendering.
     */
    private QuizDetail openQuizQ1(int expectedCorrect) {
        QuizDetail detail = ContentBundleLoader
                .firstQuizWhereQ1IsDefaultTextSelectionWithCorrectCount(expectedCorrect);
        Assert.assertNotNull(detail,
                "Bundle should expose at least one DEFAULT text-selection quiz "
                      + "whose Q1 has exactly " + expectedCorrect + " correct answer(s)");
        String quizTitle = detail.titleEn();
        Assert.assertNotNull(quizTitle, "Bundle quiz should expose an en-US title");
        log("🎯 Fixture quiz: '" + quizTitle + "' (id=" + detail.id + ", "
                + expectedCorrect + " correct on Q1)");
        quizzesPage.tapQuizByTitle(quizTitle);
        long deadline = System.currentTimeMillis() + 8000;
        while (System.currentTimeMillis() < deadline && !textSelectionPage.isDisplayed()) {
            try { Thread.sleep(250); } catch (InterruptedException ignored) {}
        }
        Assert.assertTrue(textSelectionPage.isDisplayed(),
                "Question screen should render for quiz '" + quizTitle + "'");

        // Cross-check rendered content against the bundle: the question prompt
        // must equal the localized title verbatim, and the static hint must
        // include the canonical "Choose the correct answer(s)" copy. Catches
        // CMS edits and rendering regressions that swap question text or the
        // selection-instruction label, in addition to behavior bugs.
        QuizQuestion q1 = q1(detail);
        String expectedPrompt = q1.titleEn() == null ? "" : q1.titleEn().trim();
        String actualPrompt = textSelectionPage.getQuestionTitle().trim();
        Assert.assertEquals(actualPrompt, expectedPrompt,
                "Question prompt should match bundle's titleEn for quiz '" + quizTitle + "'");
        String hint = textSelectionPage.getHint();
        Assert.assertTrue(hint != null && hint.contains("Choose the correct answer"),
                "Question hint should contain 'Choose the correct answer' (got: '"
                        + hint + "')");
        return detail;
    }

    private QuizQuestion q1(QuizDetail detail) {
        Assert.assertFalse(detail.questions == null || detail.questions.isEmpty(),
                "Fixture quiz must expose at least one question");
        return detail.questions.get(0);
    }

    private String firstCorrectAnswer(QuizQuestion q) {
        List<QuizAnswer> correct = q.correctAnswers();
        Assert.assertFalse(correct.isEmpty(), "Question must have at least one correct answer");
        return correct.get(0).titleEn;
    }

    private String firstIncorrectAnswer(QuizQuestion q) {
        List<QuizAnswer> wrong = q.incorrectAnswers();
        Assert.assertFalse(wrong.isEmpty(), "Question must have at least one incorrect answer");
        return wrong.get(0).titleEn;
    }

    // ========================================================================
    // TC22050 — User cannot proceed (Next/CHECK) without selecting any answer
    // ========================================================================
    @Test(description = "TC22050 - Validate user cannot Next without an answer selected",
            groups = {"smoke", "regression"})
    public void TC22050() {
        openQuizQ1(1);
        Assert.assertFalse(textSelectionPage.isCheckEnabled(),
                "CHECK should be disabled before any answer is selected");
        log("✅ TC22050: CHECK is gated until an answer is selected");
    }

    // ========================================================================
    // TC22051 — User can select an answer (selection state flips on)
    // ========================================================================
    @Test(description = "TC22051 - Validate user can select an answer",
            groups = {"smoke", "regression"})
    public void TC22051() {
        QuizDetail detail = openQuizQ1(1);
        QuizQuestion question = q1(detail);
        String answer = firstCorrectAnswer(question);

        Assert.assertFalse(textSelectionPage.isAnswerSelected(answer),
                "Answer '" + answer + "' should start unselected");
        textSelectionPage.tapAnswer(answer);
        Assert.assertTrue(textSelectionPage.isAnswerSelected(answer),
                "Answer '" + answer + "' should read as selected after tap");
        Assert.assertTrue(textSelectionPage.isCheckEnabled(),
                "CHECK should enable once any answer is selected");
        log("✅ TC22051: Answer selection toggles on, CHECK enables");
    }

    // ========================================================================
    // TC22052 — Single-select: tapping a second answer deselects the first
    // ========================================================================
    @Test(description = "TC22052 - Validate single-select swaps selection",
            groups = {"regression"})
    public void TC22052() {
        QuizDetail detail = openQuizQ1(1);
        QuizQuestion question = q1(detail);
        // Pick two distinct rows — the correct one and the first incorrect.
        String first = firstCorrectAnswer(question);
        String second = firstIncorrectAnswer(question);

        textSelectionPage.tapAnswer(first);
        Assert.assertTrue(textSelectionPage.isAnswerSelected(first),
                "First tap should select '" + first + "'");

        textSelectionPage.tapAnswer(second);
        Assert.assertTrue(textSelectionPage.isAnswerSelected(second),
                "Second tap should select '" + second + "'");
        Assert.assertFalse(textSelectionPage.isAnswerSelected(first),
                "Single-select: first answer should auto-deselect when a second is picked");
        log("✅ TC22052: Single-select swaps selection between answers");
    }

    // ========================================================================
    // TC22053 — Multi-select: at most N answers selectable when Q has N corrects
    // ========================================================================
    @Test(description = "TC22053 - Validate multi-select caps at the correct-answer count",
            groups = {"regression"})
    public void TC22053() {
        QuizDetail detail = openQuizQ1(2);
        QuizQuestion question = q1(detail);
        List<QuizAnswer> answers = question.answers();
        // Pick the first three answers in display order — at least one must be
        // outside the "correct count" window so we can prove the third tap is
        // capped. With 4 total and 2 correct that's always true.
        Assert.assertTrue(answers.size() >= 3,
                "Multi-select fixture must expose >=3 answer rows");

        String a = answers.get(0).titleEn;
        String b = answers.get(1).titleEn;
        String c = answers.get(2).titleEn;

        textSelectionPage.tapAnswer(a);
        textSelectionPage.tapAnswer(b);
        Assert.assertTrue(textSelectionPage.isAnswerSelected(a)
                        && textSelectionPage.isAnswerSelected(b),
                "First two taps should select both answers");

        // Third tap must not increase the selected count beyond the cap (2).
        textSelectionPage.tapAnswer(c);
        int selected = 0;
        for (QuizAnswer ans : answers) {
            if (textSelectionPage.isAnswerSelected(ans.titleEn)) selected++;
        }
        Assert.assertEquals(selected, 2,
                "Multi-select with 2 corrects should cap at 2 selected (got " + selected + ")");
        log("✅ TC22053: Selection capped at correct-answer count");
    }

    // ========================================================================
    // TC22055 — Result modal appears with CORRECT! after selecting all corrects
    // ========================================================================
    @Test(description = "TC22055 - Validate correct-answer result modal",
            groups = {"smoke", "regression"})
    public void TC22055() {
        QuizDetail detail = openQuizQ1(1);
        QuizQuestion question = q1(detail);

        for (QuizAnswer correct : question.correctAnswers()) {
            textSelectionPage.tapAnswer(correct.titleEn);
        }
        textSelectionPage.tapCheck();

        long deadline = System.currentTimeMillis() + 8000;
        while (System.currentTimeMillis() < deadline
                && !textSelectionPage.isResultModalVisible()) {
            try { Thread.sleep(250); } catch (InterruptedException ignored) {}
        }
        Assert.assertTrue(textSelectionPage.isResultModalVisible(),
                "Result modal should appear after CHECK");
        Assert.assertTrue(textSelectionPage.getResultTitle().toUpperCase().contains("CORRECT"),
                "Modal title should read CORRECT! (got: '"
                      + textSelectionPage.getResultTitle() + "')");
        log("✅ TC22055: Correct-answer modal rendered with CORRECT! title");
    }

    // ========================================================================
    // TC22056 — Incorrect-answer modal renders INCORRECT title
    // ========================================================================
    @Test(description = "TC22056 - Validate incorrect-answer result modal",
            groups = {"regression"})
    public void TC22056() {
        QuizDetail detail = openQuizQ1(1);
        QuizQuestion question = q1(detail);
        String wrong = firstIncorrectAnswer(question);

        textSelectionPage.tapAnswer(wrong);
        textSelectionPage.tapCheck();

        long deadline = System.currentTimeMillis() + 8000;
        while (System.currentTimeMillis() < deadline
                && !textSelectionPage.isResultModalVisible()) {
            try { Thread.sleep(250); } catch (InterruptedException ignored) {}
        }
        Assert.assertTrue(textSelectionPage.isResultModalVisible(),
                "Result modal should appear after CHECK");
        String title = textSelectionPage.getResultTitle();
        Assert.assertTrue(title.toUpperCase().contains("INCORRECT"),
                "Modal title should read INCORRECT (got: '" + title + "')");

        // Modal body should render the bundle's localized failMessage. We
        // compare against the trimmed en-US value — the CDN occasionally
        // ships strings with trailing whitespace and the renderer strips it.
        // Skip this check only if the bundle entry is missing/empty (older
        // questions without an explicit failMessage), to avoid false-failing
        // on legitimately empty content.
        String expectedBody = question.failMessageEn();
        if (expectedBody != null && !expectedBody.isBlank()) {
            String actualBody = textSelectionPage.getResultBody();
            Assert.assertEquals(actualBody, expectedBody.trim(),
                    "Incorrect-modal body should match bundle's failMessageEn");
        }
        log("✅ TC22056: Incorrect-answer modal rendered with INCORRECT title + bundle failMessage");
    }

    // ========================================================================
    // TC22057 — User can dismiss the result modal via the × button
    // ========================================================================
    @Test(description = "TC22057 - Validate result modal can be dismissed via cross",
            groups = {"regression"})
    public void TC22057() {
        QuizDetail detail = openQuizQ1(1);
        QuizQuestion question = q1(detail);
        // Use the correct path so this test doesn't double-cover TC22056.
        for (QuizAnswer correct : question.correctAnswers()) {
            textSelectionPage.tapAnswer(correct.titleEn);
        }
        textSelectionPage.tapCheck();

        long appearDeadline = System.currentTimeMillis() + 8000;
        while (System.currentTimeMillis() < appearDeadline
                && !textSelectionPage.isResultModalVisible()) {
            try { Thread.sleep(250); } catch (InterruptedException ignored) {}
        }
        Assert.assertTrue(textSelectionPage.isResultModalVisible(),
                "Result modal should be visible before we attempt to dismiss it");

        textSelectionPage.tapResultClose();
        long dismissDeadline = System.currentTimeMillis() + 5000;
        while (System.currentTimeMillis() < dismissDeadline
                && textSelectionPage.isResultModalVisible()) {
            try { Thread.sleep(250); } catch (InterruptedException ignored) {}
        }
        Assert.assertFalse(textSelectionPage.isResultModalVisible(),
                "Result modal should dismiss after tapping the × button");
        log("✅ TC22057: Result modal dismissed via cross");
    }
}
