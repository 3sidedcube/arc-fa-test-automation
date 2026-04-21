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
    // Non-throwing presence check — use when absence is a valid outcome
    // (e.g. iOS auto-submits the sign-in form via the keyboard's "Go" key).
    public boolean isEmailFieldPresent() {
        for (By locator : emailFieldLocators) {
            if (!driver.findElements(locator).isEmpty()) return true;
        }
        return false;
    }
    public void enterEmail(String email) {
        enterText(emailFieldLocators, email);
    }

    // Password field
    public boolean isPasswordFieldVisible() {
        return isVisible(passwordFieldLocators);
    }
    public void tapPasswordField() {
        tap(passwordFieldLocators);
        dismissKeyboard();
        dismissAlertIfPresent();
    }
    // Focus the password field without dismissing the keyboard — use before
    // enterPassword on iOS so sendKeys types into the right input on real devices.
    public void focusPasswordField() {
        tap(passwordFieldLocators);
    }
    public void enterPassword(String password) {
        enterText(passwordFieldLocators, password);
    }

    // Continue button
    public boolean isContinueButtonVisible() {
        return isVisible(continueButtonLocators);
    }
    public boolean isContinueButtonDisabled() {
        WebElement button = waitForVisibility(continueButtonLocators);
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

    // Email field label — waits for "Invalid Email" validation state to appear
    public boolean isInvalidEmailLabelVisible() {
        return isVisible(invalidEmailLabelLocators);
    }
}
