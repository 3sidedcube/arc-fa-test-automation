package tests;

import com.cube.qa.framework.pages.onboarding.SignInPage;
import com.cube.qa.framework.pages.onboarding.WelcomePage;
import com.cube.qa.framework.testdata.loader.UserDataLoader;
import com.cube.qa.framework.testdata.model.User;
import com.cube.qa.framework.utils.BaseTest;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Test cases for the Sign In screen
 * Testiny folder: ARC First Aid - v4.0.0 > Welcome Screen > Sign In
 */
public class SignInScreenTest extends BaseTest {

    private WelcomePage welcomePage;
    private SignInPage signInPage;

    @BeforeMethod(alwaysRun = true, dependsOnMethods = "setUp")
    public void setUpTest() {
        welcomePage = pages.welcomePage();
        signInPage = pages.signInPage();
        // Navigate from Welcome Screen to Sign In for all tests in this class
        welcomePage.tapSignIn();
    }

    @Test(description = "TC8662 - Default display", groups = {"smoke", "regression"})
    public void TC8662() {
        // Precondition: Logged out, viewing sign in screen
        Assert.assertTrue(signInPage.isEmailFieldVisible(), "Email field should be visible");
        Assert.assertTrue(signInPage.isPasswordFieldVisible(), "Password field should be visible");
        Assert.assertTrue(signInPage.isContinueButtonVisible(), "Continue button should be visible");
        Assert.assertTrue(signInPage.isContinueButtonDisabled(), "Continue button should be disabled when fields are empty");
        Assert.assertTrue(signInPage.isForgotPasswordVisible(), "Forgot Your Password? link should be visible");

        log("✅ TC8662: Default display verified");
    }

    @Test(description = "TC8665 - Sign in - email validation", groups = {"regression"})
    public void TC8665() {
        // Precondition: Not logged in, viewing sign in screen
        signInPage.enterEmail("invalid@test");
        signInPage.tapPasswordField();

        Assert.assertTrue(signInPage.isInvalidEmailLabelVisible(), "Email field label should change to 'Invalid Email'");
        Assert.assertTrue(signInPage.isContinueButtonDisabled(), "Continue button should remain disabled with invalid email");

        log("✅ TC8665: Sign in - email validation verified");
    }

    @Test(description = "TC8666 - Sign in - password validation (field not empty)", groups = {"regression"})
    public void TC8666() {
        // Precondition: Not logged in, viewing sign in screen
        signInPage.enterEmail("valid@email.com");
        // Password field left empty

        Assert.assertTrue(signInPage.isContinueButtonDisabled(), "Continue button should remain disabled when password field is empty");

        log("✅ TC8666: Sign in - password validation (field not empty) verified");
    }

    @Test(description = "TC8667 - Log in", groups = {"smoke"})
    public void TC8667() {
        // Precondition: Fresh install, not logged in, user has not passed onboarding, app open on sign in screen
        User user = UserDataLoader.loadUsers().get(0);
        signInPage.enterEmail(user.username);
        signInPage.enterPassword(user.password);
        signInPage.tapContinue();

        Assert.assertTrue(signInPage.isEmailFieldInvisible(), "Email field should no longer be visible after successful login");

        log("✅ TC8667: Log in verified");
    }
}
