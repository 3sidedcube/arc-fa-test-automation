package com.cube.qa.framework.pages.deviceHelpers;

import com.cube.qa.framework.pages.BasePage;
import io.appium.java_client.AppiumDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

public class IOSHelpersPage extends BasePage {

    private String month;
    private String day;
    private String year;
    private String dobYear;

    private List<By> monthLocators;
    private List<By> dayLocators;
    private List<By> yearLocators;
    private List<By> dobYearLocators;

    public IOSHelpersPage(AppiumDriver driver) {
        super(driver);

        this.month = LocalDate.now().getMonth().name().charAt(0) + LocalDate.now().getMonth().name().substring(1).toLowerCase();
        this.day = String.valueOf(LocalDate.now().getDayOfMonth());
        this.year = String.valueOf(LocalDate.now().getYear());
        this.dobYear = String.valueOf(LocalDate.now().minusYears(16).getYear());

        setLocators();
    }

    private void setLocators() {
        monthLocators = List.of(
                By.xpath("//XCUIElementTypePickerWheel[@value='" + month + "']")
        );
        dayLocators = List.of(
                By.xpath("//XCUIElementTypePickerWheel[@value='" + day + "']")
        );
        yearLocators = List.of(
                By.xpath("//XCUIElementTypePickerWheel[@value='" + year + "']")
        );
        dobYearLocators = List.of(
                By.xpath("//XCUIElementTypePickerWheel[@value='" + dobYear + "']")
        );
    }

    // General Date Picker
    public void enterMonth(String newMonth) {
        enterText(monthLocators, newMonth);
        this.month = newMonth;
        setLocators();
    }

    public void enterDay(String newDay) {
        enterText(dayLocators, newDay);
        this.day = newDay;
        setLocators();
    }

    public void enterYear(String newYear) {
        enterText(yearLocators, newYear);
        this.year = newYear;
        setLocators();
    }

    public void enterDate(String month, String day, String year) {
        enterMonth(month);
        enterDay(day);
        enterYear(year);
    }

    // DOB-specific Picker
    public void enterDobYear(String year) {
        enterText(dobYearLocators, year);
        this.dobYear = year;
        setLocators();
    }

    public void enterDobDate(String month, String day, String year) {
        enterMonth(month);
        enterDay(day);
        enterDobYear(year);
    }

    // Location & Notification Permissions (System Alerts)
    //
    // The iOS location alert contains a "Precise: On" toggle *before* the action buttons,
    // so driver.switchTo().alert().accept() (which taps the first interactive element)
    // toggles precision instead of granting. Target the button by label directly.
    private static final List<String> ACCEPT_LABELS = List.of(
            "Allow While Using App",
            "Allow Once",
            "Allow",
            "OK"
    );
    private static final List<String> DISMISS_LABELS = List.of(
            "Don’t Allow", // curly apostrophe — iOS renders this
            "Don't Allow", // straight apostrophe fallback
            "Cancel"
    );

    private boolean tapAlertButtonByLabel(List<String> labels) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));
        try {
            wait.until(ExpectedConditions.alertIsPresent());
        } catch (TimeoutException e) {
            return false;
        }
        // Preferred: use Appium's `mobile: alert` script with a specific button label.
        // Directly targets the named alert button, bypassing the "Precise: On" toggle
        // that sits before the action buttons in the iOS location prompt.
        for (String label : labels) {
            try {
                driver.executeScript("mobile: alert",
                        java.util.Map.of("action", "accept", "buttonLabel", label));
                System.out.println("📲 Tapped iOS alert button via mobile:alert: " + label);
                return true;
            } catch (Exception ignored) {
                // try next label
            }
        }
        // Fallback: find the button by label in the UI tree
        for (String label : labels) {
            List<WebElement> matches = driver.findElements(
                    By.xpath("//XCUIElementTypeButton[@name='" + label + "']"));
            if (matches.isEmpty()) {
                matches = driver.findElements(
                        By.xpath("//XCUIElementTypeButton[@label='" + label + "']"));
            }
            if (!matches.isEmpty()) {
                matches.get(0).click();
                System.out.println("📲 Tapped iOS alert button via xpath: " + label);
                return true;
            }
        }
        return false;
    }

    public void acceptIOSAlert() {
        if (tapAlertButtonByLabel(ACCEPT_LABELS)) {
            System.out.println("✅ Accepted iOS system alert.");
            return;
        }
        // Fallback: native accept (works for alerts without a precision toggle)
        try {
            driver.switchTo().alert().accept();
            System.out.println("✅ Accepted iOS system alert (fallback).");
        } catch (Exception e) {
            System.out.println("⚠️ No iOS system alert appeared to accept.");
        }
    }

    public void dismissIOSAlert() {
        if (tapAlertButtonByLabel(DISMISS_LABELS)) {
            System.out.println("❌ Dismissed iOS system alert.");
            return;
        }
        try {
            driver.switchTo().alert().dismiss();
            System.out.println("❌ Dismissed iOS system alert (fallback).");
        } catch (Exception e) {
            System.out.println("⚠️ No iOS system alert appeared to dismiss.");
        }
    }
}
