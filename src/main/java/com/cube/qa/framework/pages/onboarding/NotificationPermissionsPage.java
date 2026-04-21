package com.cube.qa.framework.pages.onboarding;

import com.cube.qa.framework.pages.BasePage;
import io.appium.java_client.AppiumDriver;
import org.openqa.selenium.By;
import java.util.List;

public class NotificationPermissionsPage extends BasePage {

    private List<By> headlineLocators;
    private List<By> enablePermissionsButtonLocators;
    private List<By> skipButtonLocators;

    public NotificationPermissionsPage(AppiumDriver driver, String platform) {
        super(driver);

        if (platform.equalsIgnoreCase("ios")) {
            headlineLocators = List.of(
                    By.name("Enable Notification Permissions"),
                    By.xpath("//XCUIElementTypeStaticText[@label='Enable Notification Permissions']")
            );
            // iOS: primary CTA is labeled CONTINUE; no separate skip button
            enablePermissionsButtonLocators = List.of(
                    By.name("CONTINUE"),
                    By.xpath("//XCUIElementTypeButton[@label='CONTINUE']"),
                    By.xpath("//*[@label='CONTINUE']")
            );
            skipButtonLocators = List.of();
        } else {
            headlineLocators = List.of(
                    By.id("com.cube.arc.fa:id/headline"),
                    By.xpath("//*[@text='Enable Notification Permissions']")
            );
            enablePermissionsButtonLocators = List.of(
                    By.id("com.cube.arc.fa:id/enable_permissions_button"),
                    By.xpath("//*[@text='ENABLE PERMISSIONS']")
            );
            skipButtonLocators = List.of(
                    By.id("com.cube.arc.fa:id/action_skip"),
                    By.xpath("//*[@text='SKIP']")
            );
        }
    }

    public boolean isHeadlineVisible() {
        return isVisible(headlineLocators);
    }

    public void tapEnablePermissions() {
        tap(enablePermissionsButtonLocators);
    }

    public void tapContinue() {
        tap(enablePermissionsButtonLocators);
    }

    public void tapSkip() {
        tap(skipButtonLocators);
    }
}
