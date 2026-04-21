package com.cube.qa.framework.pages.deviceHelpers;

import com.cube.qa.framework.pages.BasePage;
import io.appium.java_client.AppiumDriver;
import org.openqa.selenium.By;

import java.time.LocalDate;
import java.util.List;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import java.time.Duration;


public class AndroidHelpersPage extends BasePage {

    private List<By> currentYearLocators;
    private List<By> previousMonthButtonLocators;
    private List<By> okButtonLocators;
    private List<By> currentDayLocators;

    public AndroidHelpersPage(AppiumDriver driver) {
        super(driver);

        String currentYear = String.valueOf(LocalDate.now().getYear());
        String todayDay = String.valueOf(LocalDate.now().getDayOfMonth());

        currentYearLocators = List.of(
                By.xpath("//*[@resource-id=\"android:id/text1\" and @text=\"2025\"]")
        );
        previousMonthButtonLocators = List.of(
                By.xpath("//*[@resource-id='android:id/prev']")
        );
        okButtonLocators = List.of(
                By.xpath("//*[@resource-id='android:id/button1']")
        );
        currentDayLocators = List.of(
                By.xpath("//*[@text='" + todayDay + "']")
        );
    }

    // Current Year
    public boolean isCurrentYearVisible() {
        return isVisible(currentYearLocators);
    }
    public void tapCurrentYear() {
        tap(currentYearLocators);
    }

    // Previous Month Button
    public boolean isPreviousMonthButtonVisible() {
        return isVisible(previousMonthButtonLocators);
    }
    public void tapPreviousMonthButton() {
        tap(previousMonthButtonLocators);
    }

    // OK Button
    public boolean isOkButtonVisible() {
        return isVisible(okButtonLocators);
    }
    public void tapOkButton() {
        tap(okButtonLocators);
    }

    // Current Day
    public boolean isCurrentDayVisible() {
        return isVisible(currentDayLocators);
    }
    public void tapCurrentDay() {
        tap(currentDayLocators);
    }

    // Set DOB by going back a given number of years
    // FIXME: This method of setting the date is SLOW! Takes a couple mins to go back 16 years, we can improve this however it will do for now
    public void setDateAndroid(int yearsInPast) {
        System.out.println("Setting date on Android");
        int tapCount = yearsInPast * 12;
        int adjustedDay = LocalDate.now().getDayOfMonth();
        if (adjustedDay > 29) {
            adjustedDay -= 3;
        }

        // Tap year to open year selector
        tapCurrentYear();

        // Tap back through months
        for (int i = 0; i < tapCount; i++) {
            tapPreviousMonthButton();
        }

        // Tap on the adjusted day
        tap(List.of(By.xpath("//*[@text='" + adjustedDay + "']")));

        // Confirm selection
        tapOkButton();
    }

    // Android Permission Handler
    public void acceptAndroidPermission() {
        try {
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));
            List<String> permissionOptions = List.of(
                    "Allow", "ALLOW", "While using the app", "WHILE USING THE APP", "Allow only while using the app"
            );

            for (String option : permissionOptions) {
                List<WebElement> matches = driver.findElements(By.xpath("//*[@text='" + option + "']"));
                if (!matches.isEmpty()) {
                    matches.get(0).click();
                    System.out.println("✅ Accepted Android permission: " + option);
                    return;
                }
            }

            System.out.println("ℹ️ No Android permission prompt detected.");
        } catch (Exception e) {
            System.out.println("⚠️ Error while trying to accept Android permission: " + e.getMessage());
        }
    }

    // Android Permission Dismiss
    public void dismissAndroidPermission() {
        try {
            // Primary: use resource-id (avoids apostrophe XPath issues on API 33+)
            List<WebElement> denyById = driver.findElements(
                    By.id("com.android.permissioncontroller:id/permission_deny_button"));
            if (!denyById.isEmpty()) {
                denyById.get(0).click();
                System.out.println("❌ Dismissed Android permission via resource-id");
                return;
            }

            // Fallback: contains-based XPath to avoid apostrophe malformation
            List<WebElement> denyByContains = driver.findElements(
                    By.xpath("//*[contains(@text, 'Don') and contains(@text, 'allow')]"));
            if (!denyByContains.isEmpty()) {
                denyByContains.get(0).click();
                System.out.println("❌ Dismissed Android permission via contains xpath");
                return;
            }

            // Last resort: explicit text options without apostrophes
            for (String option : List.of("Deny", "DENY")) {
                List<WebElement> matches = driver.findElements(By.xpath("//*[@text='" + option + "']"));
                if (!matches.isEmpty()) {
                    matches.get(0).click();
                    System.out.println("❌ Dismissed Android permission: " + option);
                    return;
                }
            }

            System.out.println("ℹ️ No Android permission prompt detected to dismiss.");
        } catch (Exception e) {
            System.out.println("⚠️ Error while trying to dismiss Android permission: " + e.getMessage());
        }
    }


}
