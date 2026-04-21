package tests;

import com.cube.qa.framework.pages.home.TabPage;
import com.cube.qa.framework.pages.home.TabPage.Tab;
import com.cube.qa.framework.pages.onboarding.LocationPermissionsPage;
import com.cube.qa.framework.pages.onboarding.NotificationPermissionsPage;
import com.cube.qa.framework.pages.onboarding.TermsOfServicePage;
import com.cube.qa.framework.pages.onboarding.TooltipsPage;
import com.cube.qa.framework.pages.onboarding.WelcomeCarouselPage;
import com.cube.qa.framework.pages.onboarding.WelcomePage;
import com.cube.qa.framework.utils.BaseTest;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Test cases for the Onboarding Tooltips shown on first launch home screen
 * Testiny folder: ARC First Aid - v4.0.0 > Onboarding > Tooltips
 */
public class TooltipsTest extends BaseTest {

    private WelcomePage welcomePage;
    private WelcomeCarouselPage welcomeCarouselPage;
    private LocationPermissionsPage locationPermissionsPage;
    private NotificationPermissionsPage notificationPermissionsPage;
    private TermsOfServicePage termsOfServicePage;
    private TooltipsPage tooltipsPage;
    private TabPage tabPage;

    @BeforeMethod(alwaysRun = true, dependsOnMethods = "setUp")
    public void setUpTest() {
        welcomePage = pages.welcomePage();
        welcomeCarouselPage = pages.welcomeCarouselPage();
        locationPermissionsPage = pages.locationPermissionsPage();
        notificationPermissionsPage = pages.notificationPermissionsPage();
        termsOfServicePage = pages.termsOfServicePage();
        tooltipsPage = pages.tooltipsPage();
        tabPage = pages.tabPage();

        // Walk through full onboarding to land on the home screen with tooltips
        welcomePage.tapContinueAsGuest();
        try {
            welcomeCarouselPage.tapContinue();
        } catch (RuntimeException ignored) {
            // Carousel may not appear in certain keychain states
        }
        if (isIOS()) {
            locationPermissionsPage.tapContinue();
            dismissPermissions();
            notificationPermissionsPage.tapContinue();
            dismissPermissions();
        } else {
            locationPermissionsPage.tapSkip();
            notificationPermissionsPage.tapSkip();
        }
        termsOfServicePage.tapAcceptAndContinue();
    }

    @Test(description = "TC8689 - New user launches app and views tooltips", groups = {"regression"})
    public void TC8689() {
        // Precondition: Fresh install, first landing on Home screen
        // Expected: tooltips highlight the Learn and Give Care tabs, each with a description

        Assert.assertTrue(tooltipsPage.isGotItButtonVisible(),
                "A tooltip overlay (GOT IT dismiss button) should be visible on first home-screen launch");

        // Walk through the full tooltip chain, capturing the visible copy for each
        // so we can assert that Learn and Give Care were both surfaced with descriptive text.
        List<String> capturedDescriptions = new ArrayList<>();
        int safety = 0;
        while (safety++ < 8 && tooltipsPage.isGotItButtonPresent()) {
            List<String> texts = tooltipsPage.visibleTexts();
            String joined = String.join(" | ", texts);
            log("📝 Tooltip " + safety + " content: " + joined);
            capturedDescriptions.add(joined);
            try {
                tooltipsPage.tapGotIt();
            } catch (RuntimeException e) {
                break;
            }
        }

        // Expect exactly 2 tooltips in the chain — one for Learn, one for Give Care.
        Assert.assertEquals(capturedDescriptions.size(), 2,
                "Expected 2 tooltips (Learn, Give Care) but got " + capturedDescriptions.size()
                        + ". Captured: " + capturedDescriptions);

        // Tooltip 1: Learn Tab — verify title and description copy
        String learn = capturedDescriptions.get(0);
        Assert.assertTrue(learn.contains("Learn Tab"),
                "Learn tooltip should contain the 'Learn Tab' title. Got: " + learn);
        Assert.assertTrue(learn.contains("quick actions")
                        && learn.contains("Emergency scenarios")
                        && learn.toLowerCase().contains("learn") && learn.contains("resources"),
                "Learn tooltip description should describe quick actions and learn resources. Got: " + learn);

        // Tooltip 2: Give Care — verify title and description copy
        String giveCare = capturedDescriptions.get(1);
        Assert.assertTrue(giveCare.contains("Give Care"),
                "Give Care tooltip should contain the 'Give Care' title. Got: " + giveCare);
        Assert.assertTrue(giveCare.contains("easy-to-follow guides")
                        && giveCare.contains("emergency situations"),
                "Give Care tooltip description should mention easy-to-follow guides. Got: " + giveCare);

        // After the chain, the tabs should be reachable on the home screen.
        Assert.assertTrue(tabPage.isTabVisible(Tab.LEARN),
                "Learn tab should be visible after tooltips dismiss");
        Assert.assertTrue(tabPage.isTabVisible(Tab.GIVE_CARE),
                "Give Care tab should be visible after tooltips dismiss");

        log("✅ TC8689: Tooltips shown on first launch, highlight Learn + Give Care tabs, descriptions captured");
    }

    @Test(description = "TC8692 - Navigate to another tab while tooltips visible", groups = {"regression"})
    public void TC8692() {
        // Precondition: tooltip is displaying
        Assert.assertTrue(tooltipsPage.isGotItButtonVisible(),
                "Tooltip should be visible before attempting tab navigation");

        // Attempt to navigate to a different tab — overlay should block the tap
        tabPage.attemptTapTab(Tab.LEARN);

        // Expected: user remains on current tooltip; GOT IT still visible
        Assert.assertTrue(tooltipsPage.isGotItButtonPresent(),
                "Tooltip overlay should still be displayed — tab navigation must be blocked");

        log("✅ TC8692: Navigation blocked while tooltip visible");
    }

    @Test(description = "TC8693 - User can dismiss the tooltips", groups = {"regression"})
    public void TC8693() {
        // Precondition: tooltips are displaying on screen
        Assert.assertTrue(tooltipsPage.isGotItButtonVisible(),
                "Tooltip GOT IT button should be visible before dismissal");

        // Walk the full tooltip chain — the CSV spec focuses on the Training Tab
        // tooltip specifically, but the chain must end with every tooltip gone.
        int safety = 0;
        while (safety++ < 8) {
            try {
                if (!tooltipsPage.isGotItButtonPresent()) break;
                tooltipsPage.tapGotIt();
            } catch (RuntimeException e) {
                break;
            }
        }

        // Expected: the tooltip is dismissed
        Assert.assertTrue(tooltipsPage.isGotItButtonAbsent(),
                "Tooltip should be dismissed after tapping GOT IT");

        log("✅ TC8693: Tooltips dismissed successfully");
    }
}
