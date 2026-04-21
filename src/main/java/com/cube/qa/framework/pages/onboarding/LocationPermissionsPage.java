package com.cube.qa.framework.pages.onboarding;

import com.cube.qa.framework.pages.BasePage;
import io.appium.java_client.AppiumDriver;
import org.openqa.selenium.By;
import java.util.List;

public class LocationPermissionsPage extends BasePage {

    private List<By> headlineLocators;
    private List<By> enablePermissionsButtonLocators;
    private List<By> skipButtonLocators;
    private List<By> rationaleOkButtonLocators;

    public LocationPermissionsPage(AppiumDriver driver, String platform) {
        super(driver);

        if (platform.equalsIgnoreCase("ios")) {
            headlineLocators = List.of(
                    By.name("Enable Location Permissions"),
                    By.xpath("//XCUIElementTypeStaticText[@label='Enable Location Permissions']")
            );
            // iOS: button says CONTINUE; there is no separate skip button
            enablePermissionsButtonLocators = List.of(
                    By.name("CONTINUE"),
                    By.xpath("//XCUIElementTypeButton[@label='CONTINUE']"),
                    By.xpath("//*[@label='CONTINUE']")
            );
            skipButtonLocators = List.of(); // iOS has no skip on this screen
            rationaleOkButtonLocators = List.of(); // Android only

        } else {
            headlineLocators = List.of(
                    By.id("com.cube.arc.fa:id/headline"),
                    By.xpath("//*[@text='Enable Location Permissions']")
            );
            enablePermissionsButtonLocators = List.of(
                    By.id("com.cube.arc.fa:id/enable_permissions_button"),
                    By.xpath("//*[@text='ENABLE PERMISSIONS']")
            );
            skipButtonLocators = List.of(
                    By.id("com.cube.arc.fa:id/action_skip"),
                    By.xpath("//*[@text='SKIP']")
            );
            rationaleOkButtonLocators = List.of(
                    By.id("android:id/button1"),
                    By.xpath("//*[@text='OK']")
            );
        }
    }

    public boolean isHeadlineVisible() {
        return isVisible(headlineLocators);
    }

    public void tapEnablePermissions() {
        tap(enablePermissionsButtonLocators);
    }

    // iOS: primary CTA is labeled CONTINUE (no separate ENABLE PERMISSIONS button)
    public void tapContinue() {
        tap(enablePermissionsButtonLocators);
    }

    public void tapSkip() {
        tap(skipButtonLocators);
    }

    public void tapRationaleOk() {
        tap(rationaleOkButtonLocators);
    }

    public boolean isEnablePermissionsButtonVisible() {
        return isVisible(enablePermissionsButtonLocators);
    }

    public boolean isHeadlineInvisible() {
        return isInvisible(headlineLocators);
    }

    public boolean isSkipButtonAbsent() {
        for (By locator : skipButtonLocators) {
            if (!driver.findElements(locator).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    // Taps OK on the in-app dialogs that appear in the Android deny/rationale flow
    public void tapInAppDialogOk() {
        tap(List.of(By.xpath("//*[@text='OK']")));
    }

    public boolean isInAppDenyDialogVisible() {
        return !driver.findElements(By.xpath("//*[contains(@text, 'unavailable')]")).isEmpty();
    }
}
