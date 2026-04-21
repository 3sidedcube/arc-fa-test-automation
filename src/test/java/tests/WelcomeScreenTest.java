package tests;

import com.cube.qa.framework.pages.onboarding.WelcomePage;
import com.cube.qa.framework.utils.BaseTest;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Test cases for the Welcome Screen
 * Testiny folder: ARC First Aid - v4.0.0 > Welcome Screen
 */
public class WelcomeScreenTest extends BaseTest {

    private WelcomePage welcomePage;

    @BeforeMethod(alwaysRun = true, dependsOnMethods = "setUp")
    public void setUpTest() {
        welcomePage = pages.welcomePage();
    }

    @Test(description = "TC8661 - Default display", groups = {"smoke", "regression"})
    public void TC8661() {
        // Precondition: User is not logged in — app launched fresh via fullReset
        Assert.assertTrue(welcomePage.isArcLogoVisible(), "ARC logo should be visible at top of screen");
        Assert.assertTrue(welcomePage.isSignInButtonVisible(), "Sign In button should be visible");
        Assert.assertTrue(welcomePage.isContinueAsGuestButtonVisible(), "Continue as Guest button should be visible");
        Assert.assertTrue(welcomePage.isFirstAidContentButtonVisible(), "First Aid Content button should be visible");

        log("✅ TC8661: Default display verified");
    }

}
