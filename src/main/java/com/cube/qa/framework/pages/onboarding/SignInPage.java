package com.cube.qa.framework.pages.onboarding;

import com.cube.qa.framework.pages.BasePage;
import io.appium.java_client.AppiumDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import java.util.List;

public class SignInPage extends BasePage {

    // Locators
    private List<By> emailFieldLocators;
    private List<By> passwordFieldLocators;
    private List<By> emailLabelLocators;
    private List<By> invalidEmailLabelLocators;
    private List<By> continueButtonLocators;
    private List<By> forgotPasswordLocators;

    public SignInPage(AppiumDriver driver, String platform) {
        super(driver);

        if (platform.equalsIgnoreCase("ios")) {
            emailFieldLocators = List.of(
                    By.xpath("//XCUIElementTypeTextField[@placeholderValue='Email']")
            );
            passwordFieldLocators = List.of(
                    By.xpath("//XCUIElementTypeSecureTextField[@placeholderValue='Password']")
            );
            emailLabelLocators = List.of(
                    By.name("Email")
            );
            invalidEmailLabelLocators = List.of(
                    By.name("Invalid Email")
            );
            continueButtonLocators = List.of(
                    By.name("CONTINUE")
            );
            forgotPasswordLocators = List.of(
                    By.name("Forgot Your Password?")
            );

        } else {
            // Email and password fields share the same resource-id — use parent wrapper to differentiate
            emailFieldLocators = List.of(
                    By.xpath("//*[@resource-id='com.cube.arc.fa:id/input_email']//*[@resource-id='com.cube.arc.fa:id/input']")
            );
            passwordFieldLocators = List.of(
                    By.xpath("//*[@resource-id='com.cube.arc.fa:id/input_password']//*[@resource-id='com.cube.arc.fa:id/input']")
            );
            emailLabelLocators = List.of(
                    By.xpath("//*[@resource-id='com.cube.arc.fa:id/input_email']//*[@resource-id='com.cube.arc.fa:id/header_title']")
            );
            invalidEmailLabelLocators = List.of(
                    By.xpath("//*[@resource-id='com.cube.arc.fa:id/input_email']//*[@resource-id='com.cube.arc.fa:id/header_error_message']")
            );
            // Continue and Forgot Password share arc_button resource-id — use parent wrapper
            continueButtonLocators = List.of(
                    By.xpath("//*[@resource-id='com.cube.arc.fa:id/continue_button']//android.widget.Button")
            );
            forgotPasswordLocators = List.of(
                    By.xpath("//*[@resource-id='com.cube.arc.fa:id/forgot']//android.widget.Button")
            );
        }
    }

    // Email field
    public boolean isEmailFieldVisible() {
        return isVisible(emailFieldLocators);
    }
    public boolean isEmailFieldInvisible() {
        return isInvisible(emailFieldLocators);
    }
    public void enterEmail(String email) {
        enterText(emailFieldLocators, email);
    }

    // Password field
    public boolean isPasswordFieldVisible() {
        return isVisible(passwordFieldLocators);
    }
    // Focus the password field. Previously this also dismissed the keyboard,
    // but that prevented subsequent enterPassword() calls from typing on iOS
    // real devices — callers that need the keyboard gone should dismiss it
    // explicitly after reading whatever state they need.
    public void tapPasswordField() {
        tap(passwordFieldLocators);
        dismissAlertIfPresent();
    }
    // Public hook so tests can dismiss the soft keyboard when it covers
    // assertions (e.g. the validation label beneath the email field).
    public void dismissSoftKeyboard() {
        dismissKeyboard();
    }
    public void enterPassword(String password) {
        enterText(passwordFieldLocators, password);
    }

    // Continue button
    public boolean isContinueButtonVisible() {
        return isVisible(continueButtonLocators);
    }
    public boolean isContinueButtonDisabled() {
        // Use presence rather than visibility: iOS marks the disabled CONTINUE
        // button with visible="false" in the accessibility tree, so a visibility
        // wait times out even though the element is in the hierarchy.
        WebElement button = waitForPresence(continueButtonLocators);
        return !button.isEnabled();
    }
    public void tapContinue() {
        tap(continueButtonLocators);
    }

    // Forgot Password
    public boolean isForgotPasswordVisible() {
        return isVisible(forgotPasswordLocators);
    }
    public void tapForgotPassword() {
        tap(forgotPasswordLocators);
    }

    // Email field label — waits for the "Invalid Email" validation state to
    // appear. Uses presence rather than isDisplayed(): iOS surfaces the label
    // in the accessibility tree even when Appium's isDisplayed() reports false,
    // so presence is the more reliable signal that validation fired. Needs a
    // proper wait (not a one-shot findElements) because staging iOS can take a
    // few seconds to render the error label after focus leaves the email field.
    public boolean isInvalidEmailLabelVisible() {
        try {
            return waitForPresence(invalidEmailLabelLocators) != null;
        } catch (RuntimeException e) {
            return false;
        }
    }
}
