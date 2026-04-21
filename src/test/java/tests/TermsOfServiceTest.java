package tests;

import com.cube.qa.framework.pages.onboarding.LocationPermissionsPage;
import com.cube.qa.framework.pages.onboarding.NotificationPermissionsPage;
import com.cube.qa.framework.pages.onboarding.TermsOfServicePage;
import com.cube.qa.framework.pages.onboarding.WelcomeCarouselPage;
import com.cube.qa.framework.pages.onboarding.WelcomePage;
import com.cube.qa.framework.utils.BaseTest;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Map;

/**
 * Test cases for the Terms of Service onboarding screen
 * Testiny folder: ARC First Aid - v4.0.0 > Onboarding
 */
public class TermsOfServiceTest extends BaseTest {

    private WelcomePage welcomePage;
    private WelcomeCarouselPage welcomeCarouselPage;
    private LocationPermissionsPage locationPermissionsPage;
    private NotificationPermissionsPage notificationPermissionsPage;
    private TermsOfServicePage termsOfServicePage;

    @BeforeMethod(alwaysRun = true, dependsOnMethods = "setUp")
    public void setUpTest() {
        welcomePage = pages.welcomePage();
        welcomeCarouselPage = pages.welcomeCarouselPage();
        locationPermissionsPage = pages.locationPermissionsPage();
        notificationPermissionsPage = pages.notificationPermissionsPage();
        termsOfServicePage = pages.termsOfServicePage();

        welcomePage.tapContinueAsGuest();
        try {
            welcomeCarouselPage.tapContinue();
        } catch (RuntimeException ignored) {
            // Carousel may not appear if app is in an authenticated keychain state
        }
        if (isIOS()) {
            // iOS has no SKIP on the permission screens — tap CONTINUE and dismiss the native alert
            locationPermissionsPage.tapContinue();
            dismissPermissions();
            notificationPermissionsPage.tapContinue();
            dismissPermissions();
        } else {
            locationPermissionsPage.tapSkip();
            notificationPermissionsPage.tapSkip();
        }
    }

    @Test(description = "TC8680 - Terms of Service", groups = {"regression"})
    public void TC8680() {
        // Precondition: User has skipped/progressed past location permissions (Android)
        // or notification permissions (iOS)

        // Step 1: Verify no skip/close button — ACCEPT AND CONTINUE is the only way forward
        Assert.assertTrue(termsOfServicePage.isAcceptAndContinueButtonVisible(),
                "ACCEPT AND CONTINUE button should be visible on Terms of Service screen");
        Assert.assertTrue(termsOfServicePage.isSkipButtonAbsent(),
                "There should be no skip button on the Terms of Service screen");

        // Step 2: Close the app and reopen — ToS screen should persist
        String appId = isAndroid() ? config.getAndroidPackageName() : config.getIosBundleId();
        String idKey = isAndroid() ? "appId" : "bundleId";

        driver.executeScript("mobile: terminateApp", Map.of(idKey, appId));
        try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
        driver.executeScript("mobile: activateApp", Map.of(idKey, appId));

        Assert.assertTrue(termsOfServicePage.isHeadlineVisible(),
                "Terms of Service screen should persist after closing and reopening the app");

        log("✅ TC8680: Terms of Service verified");
    }
}
