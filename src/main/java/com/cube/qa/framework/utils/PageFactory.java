package com.cube.qa.framework.utils;

import com.cube.qa.framework.pages.onboarding.LocationPermissionsPage;
import com.cube.qa.framework.pages.onboarding.NotificationPermissionsPage;
import com.cube.qa.framework.pages.onboarding.SignInPage;
import com.cube.qa.framework.pages.home.LearnTabPage;
import com.cube.qa.framework.pages.home.PersonalizeExperiencePage;
import com.cube.qa.framework.pages.home.TabPage;
import com.cube.qa.framework.pages.onboarding.TermsOfServicePage;
import com.cube.qa.framework.pages.onboarding.TooltipsPage;
import com.cube.qa.framework.pages.onboarding.WelcomeCarouselPage;
import com.cube.qa.framework.pages.onboarding.WelcomePage;
import io.appium.java_client.AppiumDriver;

/**
 * PageFactory class for lazy loading of page objects
 * Provides efficient page object instantiation with platform awareness
 * 
 * Key Features:
 * - Lazy Loading: Pages are created only when requested
 * - Single Responsibility: Only creates page objects
 * - Platform Aware: Passes platform to each page
 * - Memory Efficient: No unused page objects
 */
public class PageFactory {
    private final AppiumDriver driver;
    private final String platform;

    public PageFactory(AppiumDriver driver, String platform) {
        this.driver = driver;
        this.platform = platform;
    }

    // Factory methods for each page - lazy instantiation
    
    public WelcomePage welcomePage() {
        return new WelcomePage(driver, platform);
    }

    public SignInPage signInPage() {
        return new SignInPage(driver, platform);
    }

    public WelcomeCarouselPage welcomeCarouselPage() {
        return new WelcomeCarouselPage(driver, platform);
    }

    public LocationPermissionsPage locationPermissionsPage() {
        return new LocationPermissionsPage(driver, platform);
    }

    public NotificationPermissionsPage notificationPermissionsPage() {
        return new NotificationPermissionsPage(driver, platform);
    }

    public TermsOfServicePage termsOfServicePage() {
        return new TermsOfServicePage(driver, platform);
    }

    public TooltipsPage tooltipsPage() {
        return new TooltipsPage(driver, platform);
    }

    public TabPage tabPage() {
        return new TabPage(driver, platform);
    }

    public LearnTabPage learnTabPage() {
        return new LearnTabPage(driver, platform);
    }

    public PersonalizeExperiencePage personalizeExperiencePage() {
        return new PersonalizeExperiencePage(driver, platform);
    }
}