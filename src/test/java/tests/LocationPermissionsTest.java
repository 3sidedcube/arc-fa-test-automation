package tests;

import com.cube.qa.framework.pages.onboarding.LocationPermissionsPage;
import com.cube.qa.framework.pages.onboarding.NotificationPermissionsPage;
import com.cube.qa.framework.pages.onboarding.WelcomeCarouselPage;
import com.cube.qa.framework.pages.onboarding.WelcomePage;
import com.cube.qa.framework.utils.BaseTest;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Test cases for the Location Permissions onboarding screen
 * Testiny folder: ARC First Aid - v4.0.0 > Onboarding
 */
public class LocationPermissionsTest extends BaseTest {

    private WelcomePage welcomePage;
    private WelcomeCarouselPage welcomeCarouselPage;
    private LocationPermissionsPage locationPermissionsPage;
    private NotificationPermissionsPage notificationPermissionsPage;

    @BeforeMethod(alwaysRun = true, dependsOnMethods = "setUp")
    public void setUpTest() {
        welcomePage = pages.welcomePage();
        welcomeCarouselPage = pages.welcomeCarouselPage();
        locationPermissionsPage = pages.locationPermissionsPage();
        notificationPermissionsPage = pages.notificationPermissionsPage();

        welcomePage.tapContinueAsGuest();
        try {
            welcomeCarouselPage.tapContinue();
        } catch (RuntimeException ignored) {
            // Carousel may not appear if app is in an authenticated keychain state
        }
    }

    @Test(description = "TC8677 - Deny location permissions", groups = {"regression"})
    public void TC8677() {
        // Precondition: Fresh install, user not logged in, completed Welcome card
        if (isIOS()) {
            locationPermissionsPage.tapContinue();
        } else {
            locationPermissionsPage.tapEnablePermissions();
        }
        dismissPermissions();

        if (isAndroid()) {
            // Expected 2: in-app dialog explains why location is needed
            Assert.assertTrue(locationPermissionsPage.isInAppDenyDialogVisible(),
                    "In-app rationale dialog should appear after denying location on Android");
            // Tap OK — returns to Location Permissions screen (step 3: tap Enable Permissions again)
            locationPermissionsPage.tapInAppDialogOk();
            // Expected 3: tapping Enable Permissions again shows exact-reason rationale dialog
            locationPermissionsPage.tapEnablePermissions();
            Assert.assertTrue(locationPermissionsPage.isDynamicTextVisible("hospitals"),
                    "Exact-reason rationale dialog should appear on second Enable Permissions tap");
            // Step 4: tap OK — OS permission dialog appears again (expected 4)
            locationPermissionsPage.tapInAppDialogOk();
            acceptPermissions();
        } else {
            // iOS: denying location skips directly to Notification Permissions screen
            Assert.assertTrue(notificationPermissionsPage.isHeadlineVisible(),
                    "Notification Permissions screen should appear after denying location on iOS");
        }

        log("✅ TC8677: Deny location permissions verified");
    }

    @Test(description = "TC8679 - Allow location permissions", groups = {"regression"})
    public void TC8679() {
        // Precondition: Fresh install, user not logged in, completed Welcome card
        if (isIOS()) {
            locationPermissionsPage.tapContinue();
        } else {
            locationPermissionsPage.tapEnablePermissions();
        }
        acceptPermissions();

        // After granting location, app advances to Notification Permissions
        Assert.assertTrue(notificationPermissionsPage.isHeadlineVisible(),
                "Notification Permissions screen should appear after granting location permissions");

        log("✅ TC8679: Allow location permissions verified");
    }

    @Test(description = "TC8681 - Skip permissions", groups = {"regression"})
    public void TC8681() {
        if (isIOS()) throw new SkipException("iOS: Location Permissions screen has no SKIP button (Android-only behavior)");
        // Precondition: User not logged in, progressed past Welcome card
        locationPermissionsPage.tapSkip();

        // Verify we've advanced past the Location Permissions screen
        Assert.assertTrue(locationPermissionsPage.isHeadlineInvisible(),
                "Location Permissions screen should be dismissed after tapping skip");

        log("✅ TC8681: Skip permissions verified");
    }
}
