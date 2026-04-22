package tests;

import com.cube.qa.framework.pages.onboarding.SignInPage;
import com.cube.qa.framework.pages.onboarding.TermsOfServicePage;
import com.cube.qa.framework.pages.onboarding.TooltipsPage;
import com.cube.qa.framework.pages.onboarding.WelcomeCarouselPage;
import com.cube.qa.framework.pages.onboarding.WelcomePage;
import com.cube.qa.framework.testdata.loader.UserDataLoader;
import com.cube.qa.framework.testdata.model.User;
import com.cube.qa.framework.utils.BaseTest;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.List;

/**
 * Test cases for the Welcome / What's New Cards shown on first launch.
 * Testiny folder: ARC First Aid - v4.0.0 > Onboarding > Welcome / What's New Cards
 */
public class WelcomeCardTest extends BaseTest {

    private WelcomePage welcomePage;
    private SignInPage signInPage;
    private WelcomeCarouselPage welcomeCardPage;
    private TermsOfServicePage termsOfServicePage;
    private TooltipsPage tooltipsPage;

    @BeforeMethod(alwaysRun = true, dependsOnMethods = "setUp")
    public void setUpTest() {
        welcomePage = pages.welcomePage();
        signInPage = pages.signInPage();
        welcomeCardPage = pages.welcomeCarouselPage();
        termsOfServicePage = pages.termsOfServicePage();
        tooltipsPage = pages.tooltipsPage();
    }

    @Test(description = "TC8684 - Welcome Card - New User - Sign In", groups = {"regression"})
    public void TC8684() {
        // Precondition: Fresh install, Welcome screen visible
        Assert.assertTrue(welcomePage.isSignInButtonVisible(),
                "Sign In button should be visible on fresh launch");

        // Action: tap Sign In and sign in with a scenario 1.1 user
        welcomePage.tapSignIn();

        User user = UserDataLoader.findUser(u -> "1.1".equals(u.scenario));
        signInPage.enterEmail(user.username);
        // iOS: focus the password field before typing. sendKeys alone relies on
        // the email keyboard's Next affordance, which doesn't always shift focus
        // on real devices (seen on prod iPhone 12 iOS 26).
        if (isIOS()) {
            signInPage.tapPasswordField();
        }
        signInPage.enterPassword(user.password);
        signInPage.tapContinue();

        // Expected: Welcome to The First Aid App card with 4 bullets + CTA
        Assert.assertTrue(welcomeCardPage.isHeadlineVisible(),
                "Welcome card headline should be visible after sign in");

        List<String> texts = welcomeCardPage.visibleTexts();
        log("📝 Welcome card (Sign In) content: " + String.join(" | ", texts));

        Assert.assertTrue(welcomeCardPage.containsText("Learn Life"),
                "Welcome card should list 'Learn Life Saving Skills'. Got: " + texts);
        Assert.assertTrue(welcomeCardPage.containsText("Track"),
                "Signed-in welcome card should list 'Track Your Certificates'. Got: " + texts);
        Assert.assertTrue(welcomeCardPage.containsText("Quiz"),
                "Welcome card should list 'Take Quizzes'. Got: " + texts);
        Assert.assertTrue(welcomeCardPage.containsText("Purchase"),
                "Welcome card should list 'Purchase Classes and Supplies'. Got: " + texts);

        Assert.assertTrue(welcomeCardPage.isContinueButtonVisible(),
                "Welcome card should show a CTA (CONTINUE) button");

        log("✅ TC8684: Signed-in welcome card shows 4 bullets + CTA");
    }

    @Test(description = "TC8685 - Welcome Card - New User - Guest", groups = {"regression"})
    public void TC8685() {
        // Precondition: Fresh install, Welcome screen visible
        Assert.assertTrue(welcomePage.isContinueAsGuestButtonVisible(),
                "Continue as Guest button should be visible on fresh launch");

        // Action: tap Continue as Guest
        welcomePage.tapContinueAsGuest();

        // Expected: Welcome card with 3 bullets (no "Track Your Certificates") + CTA
        Assert.assertTrue(welcomeCardPage.isHeadlineVisible(),
                "Welcome card headline should be visible after Continue as Guest");

        List<String> texts = welcomeCardPage.visibleTexts();
        log("📝 Welcome card (Guest) content: " + String.join(" | ", texts));

        Assert.assertTrue(welcomeCardPage.containsText("Learn Life"),
                "Welcome card should list 'Learn Life Saving Skills'. Got: " + texts);
        Assert.assertTrue(welcomeCardPage.containsText("Quiz"),
                "Welcome card should list 'Take Quizzes'. Got: " + texts);
        Assert.assertTrue(welcomeCardPage.containsText("Purchase"),
                "Welcome card should list 'Purchase Classes and Supplies'. Got: " + texts);

        // Guest path must NOT surface the Track Your Certificates bullet
        Assert.assertFalse(welcomeCardPage.containsText("Track"),
                "Guest welcome card should NOT list 'Track Your Certificates'. Got: " + texts);

        Assert.assertTrue(welcomeCardPage.isContinueButtonVisible(),
                "Welcome card should show a CTA (CONTINUE) button");

        log("✅ TC8685: Guest welcome card shows 3 bullets (no Track Certificates) + CTA");
    }

    @Test(description = "TC8688 - Welcome Card - First Aid Content", groups = {"regression"})
    public void TC8688() {
        // Precondition: Fresh install, Welcome screen visible
        Assert.assertTrue(welcomePage.isFirstAidContentButtonVisible(),
                "First Aid Content button should be visible on fresh launch");

        // Action: tap First Aid Content
        welcomePage.tapFirstAidContent();

        // Expected: Welcome / What's New card should NOT appear for this path.
        // ToS should still appear next. Walk to it and assert — if the welcome
        // card had shown, ToS would be behind it and this wait would fail.
        Assert.assertFalse(welcomeCardPage.isHeadlinePresent(),
                "Welcome card should NOT display on First Aid Content path");

        // Per spec: permission screens are skipped on this path — ToS is next.
        Assert.assertTrue(termsOfServicePage.isAcceptAndContinueButtonVisible(),
                "Terms of Service should still display on First Aid Content path");
        termsOfServicePage.tapAcceptAndContinue();

        Assert.assertTrue(tooltipsPage.isGotItButtonVisible(),
                "Tooltips should still display on First Aid Content path");

        log("✅ TC8688: Welcome card skipped on First Aid Content; ToS + Tooltips still shown");
    }

}
