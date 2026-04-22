package com.cube.qa.framework.utils;

import com.cube.qa.framework.config.ConfigLoader;
import com.cube.qa.framework.config.TestConfig;
import com.cube.qa.framework.pages.deviceHelpers.AndroidHelpersPage;
import com.cube.qa.framework.pages.deviceHelpers.IOSHelpersPage;
import com.cube.qa.framework.testdata.loader.UserDataLoader;

import io.appium.java_client.AppiumDriver;
import org.testng.annotations.*;
import org.testng.ITestContext;
import java.lang.reflect.Method;
import java.nio.file.Paths;
import java.util.Map;

/**
 * Enhanced BaseTest class with PageFactory pattern and performance optimizations
 * 
 * Key Features:
 * - Shared drivers for performance (once per test class)
 * - PageFactory pattern for lazy loading
 * - Platform-specific app state management
 * - Common pages and helper methods
 * - Platform detection and permission handling
 */
public class BaseTest {

    // Static variables for shared drivers and PageFactory (performance optimization)
    protected static AppiumDriver androidDriver;
    protected static AppiumDriver iosDriver;
    protected static PageFactory androidPages;
    protected static PageFactory iosPages;
    
    // Instance variables for current test
    protected PageFactory pages;
    protected AppiumDriver driver;
    protected TestConfig config;

    // Common Pages - Available to ALL test classes
    // TODO: Add common pages that will be used across most tests
    // Example: protected LoginPage loginPage;
    
    // Helper Pages - Platform-specific utilities
    protected AndroidHelpersPage androidHelpersPage;
    protected IOSHelpersPage iosHelpersPage;

    protected void log(String message) {
        String prefix = "[" + config.getPlatform().toUpperCase() +
                " | Thread-" + Thread.currentThread().getId() + "]";
        System.out.println(prefix + " " + message);
    }

    // Platform Detection Methods
    protected boolean isAndroid() {
        return config.getPlatform().equalsIgnoreCase("android");
    }

    protected boolean isIOS() {
        return config.getPlatform().equalsIgnoreCase("ios");
    }

    // Permission Handling
    public void acceptPermissions() {
        try {
            Thread.sleep(1000); // 1-second delay
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("⚠️ Interrupted during sleep: " + e.getMessage());
        }

        if(isIOS()) {
            iosHelpersPage.acceptIOSAlert();
        } else {
            androidHelpersPage.acceptAndroidPermission();
        }
    }

    public void dismissPermissions() {
        try {
            Thread.sleep(1000); // 1-second delay
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("⚠️ Interrupted during sleep: " + e.getMessage());
        }

        if(isIOS()) {
            iosHelpersPage.dismissIOSAlert();
        } else {
            androidHelpersPage.dismissAndroidPermission();
        }
    }

    // Walk the guest onboarding path: tap Continue as Guest → Welcome card →
    // permissions (iOS continues + dismisses native alerts; Android skips) →
    // Terms of Service. Lands on the first-launch Home screen with tooltips.
    protected void walkOnboardingAsGuest() {
        pages.welcomePage().tapContinueAsGuest();
        try {
            pages.welcomeCarouselPage().tapContinue();
        } catch (RuntimeException ignored) {
            // Carousel may not appear in certain keychain states
        }
        if (isIOS()) {
            pages.locationPermissionsPage().tapContinue();
            dismissPermissions();
            pages.notificationPermissionsPage().tapContinue();
            dismissPermissions();
        } else {
            pages.locationPermissionsPage().tapSkip();
            pages.notificationPermissionsPage().tapSkip();
        }
        pages.termsOfServicePage().tapAcceptAndContinue();
    }

    // Platform-specific app state management for performance optimization
    
    /**
     * Android: Fast app data clearing using mobile: clearApp
     */
    protected void resetAppState() {
        if (!isAndroid()) return;
        
        try {
            String appId = config.getAndroidPackageName();
            if (appId != null && !appId.isEmpty()) {
                driver.executeScript("mobile: clearApp", Map.of("appId", appId));
                driver.executeScript("mobile: activateApp", Map.of("appId", appId));
                log("✅ Android app state cleared using clearApp");
            } else {
                // Fallback: Terminate and activate
                driver.executeScript("mobile: terminateApp", Map.of("appId", "com.cube.arc.blood"));
                driver.executeScript("mobile: activateApp", Map.of("appId", "com.cube.arc.blood"));
                log("⚠️ Android app state cleared using fallback method");
            }
        } catch (Exception e) {
            log("❌ Failed to clear Android app state: " + e.getMessage());
            // Continue with test - don't fail due to reset issues
        }
    }

    protected void resetIOSAppState() {
        if (!isIOS() || driver == null) return;
        String bundleId = config.getIosBundleId();
        if (bundleId == null || bundleId.isEmpty()) return;

        if (config.isSimulator()) {
            // Simulators support full data clear
            try {
                driver.executeScript("mobile: clearApp", Map.of("bundleId", bundleId));
                driver.executeScript("mobile: activateApp", Map.of("bundleId", bundleId));
                log("✅ iOS app state cleared using clearApp (simulator)");
            } catch (Exception e) {
                log("❌ Failed to reset iOS simulator app state: " + e.getMessage());
            }
        } else {
            // Real devices: mobile: clearApp is not supported — uninstall + reinstall to get clean state
            try {
                String appPath = Paths.get(System.getProperty("user.dir"), config.getBuildPath()).normalize().toString();
                driver.executeScript("mobile: removeApp", Map.of("bundleId", bundleId));
                driver.executeScript("mobile: installApp", Map.of("app", appPath));
                Thread.sleep(5000); // Wait for iOS to register newly installed app with FrontBoard
                driver.executeScript("mobile: activateApp", Map.of("bundleId", bundleId));
                log("✅ iOS app reinstalled for clean state (real device)");
            } catch (Exception e) {
                log("❌ Failed to reinstall iOS app on real device: " + e.getMessage());
            }
        }
    }

    // TODO: Add common helper methods that use PageFactory (on-demand creation)
    // Example:
    // public void completeOnboardingFlow() {
    //     WelcomePage welcomePage = pages.welcomePage();
    //     LoginPage loginPage = pages.loginPage();
    //     
    //     welcomePage.tapLoginButton();
    //     loginPage.enterCredentials();
    //     
    //     if(isAndroid()) {
    //         acceptPermissions();
    //     }
    // }

    @Parameters({"platform", "build", "buildNumber", "deviceName", "udid", "fullReset", "env", "isSimulator", "platformVersion",
                "androidPackageName", "iosBundleId", "iosXcodeOrgId", "iosXcodeSigningId", "iosWdaBundleId"})
    @BeforeClass(alwaysRun = true)
    public void setUpClass(ITestContext ctx,
                          @Optional("android") String platformFromXml,
                          @Optional("") String buildFromXml,
                          @Optional("") String buildNumberFromXml,
                          @Optional("") String deviceNameFromXml,
                          @Optional("") String udidFromXml,
                          @Optional("false") String fullResetFromXml,
                          @Optional("staging") String envFromXml,
                          @Optional("false") String isSimulatorFromXml,
                          @Optional("") String platformVersionFromXml,
                          @Optional("") String androidPackageNameFromXml,
                          @Optional("") String iosBundleIdFromXml,
                          @Optional("") String iosXcodeOrgIdFromXml,
                          @Optional("") String iosXcodeSigningIdFromXml,
                          @Optional("") String iosWdaBundleIdFromXml) {

        // Load config with app-specific parameters
        config = ConfigLoader.load(platformFromXml, buildFromXml, udidFromXml, fullResetFromXml, 
                                 envFromXml, isSimulatorFromXml, deviceNameFromXml, platformVersionFromXml,
                                 androidPackageNameFromXml, iosBundleIdFromXml, iosXcodeOrgIdFromXml,
                                 iosXcodeSigningIdFromXml, iosWdaBundleIdFromXml);

        // ✅ Inject environment into UserDataLoader for environment-specific data lookup
        UserDataLoader.setEnvironment(config.getEnv());

        // Performance optimization: Create shared driver once per test class
        if (isAndroid() && androidDriver == null) {
            androidDriver = DriverManager.createDriver(
                    config.getPlatform(),
                    config.getBuildPath(),
                    config.getUdid(),
                    config.isFullReset(),
                    config.isSimulator(),
                    config.getDeviceName(),
                    config.getPlatformVersion(),
                    config.getAndroidPackageName(),
                    config.getIosBundleId(),
                    config.getIosXcodeOrgId(),
                    config.getIosXcodeSigningId(),
                    config.getIosWdaBundleId()
            );
            androidPages = new PageFactory(androidDriver, config.getPlatform());
            log("✅ Android shared driver and PageFactory created");
        } else if (isIOS() && iosDriver == null) {
            int maxRetries = 2;
            for (int attempt = 1; attempt <= maxRetries; attempt++) {
                try {
                    iosDriver = DriverManager.createDriver(
                            config.getPlatform(),
                            config.getBuildPath(),
                            config.getUdid(),
                            config.isFullReset(),
                            config.isSimulator(),
                            config.getDeviceName(),
                            config.getPlatformVersion(),
                            config.getAndroidPackageName(),
                            config.getIosBundleId(),
                            config.getIosXcodeOrgId(),
                            config.getIosXcodeSigningId(),
                            config.getIosWdaBundleId()
                    );
                    iosPages = new PageFactory(iosDriver, config.getPlatform());
                    log("✅ iOS shared driver and PageFactory created");
                    break;
                } catch (Exception e) {
                    log("❌ iOS driver creation failed (attempt " + attempt + "/" + maxRetries + "): " + e.getMessage());
                    if (attempt == maxRetries) throw e;
                    try { Thread.sleep(10000); } catch (InterruptedException ignored) {}
                }
            }
        }
    }

    @BeforeMethod(alwaysRun = true)
    public void setUp(ITestContext ctx, Method method) {
        // Assign appropriate PageFactory and driver to current test
        if (isAndroid()) {
            pages = androidPages;
            driver = androidDriver;
            // Android: Use shared driver + fast app data clearing
            resetAppState();
        } else if (isIOS()) {
            pages = iosPages;
            driver = iosDriver;
            if (driver == null) {
                throw new RuntimeException("iOS driver is null — session creation likely failed in setUpClass. Check Appium server logs.");
            }
            resetIOSAppState();
        }

        // Register driver for the Extent listener to capture screenshots
        ctx.setAttribute("driver", driver);

        // Initialize helper pages
        androidHelpersPage = new AndroidHelpersPage(driver);
        iosHelpersPage = new IOSHelpersPage(driver);

        // TODO: Initialize common pages (available to all tests)
        // Example: loginPage = pages.loginPage();

        // ✅ Automatically log the test starting
        log("▶ STARTING TEST: " + method.getName());
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown(Method method) {
        // iOS cleanup: No custom cleanup needed - Appium handles it
        // Android cleanup: No custom cleanup needed - shared driver approach
        
        log("✅ COMPLETED TEST: " + method.getName());
        
        // Note: We don't quit the driver here for performance optimization
        // Driver will be closed in @AfterClass
    }

    @AfterClass(alwaysRun = true)
    public void tearDownClass() {
        // Android: Close shared driver after each class
        if (androidDriver != null) {
            androidDriver.quit();
            androidDriver = null;
            androidPages = null;
            log("✅ Android shared driver closed");
        }
        // iOS: Driver stays alive across classes — cleaned up in @AfterSuite
    }

    @AfterSuite(alwaysRun = true)
    public void tearDownSuite() {
        if (iosDriver != null) {
            iosDriver.quit();
            iosDriver = null;
            iosPages = null;
            System.out.println("[IOS] ✅ iOS shared driver closed (suite teardown)");
        }
    }
}