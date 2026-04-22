package com.cube.qa.framework.pages.onboarding;

import com.cube.qa.framework.pages.BasePage;
import io.appium.java_client.AppiumDriver;
import org.openqa.selenium.By;
import java.util.List;

public class TermsOfServicePage extends BasePage {

    private List<By> headlineLocators;
    private List<By> acceptAndContinueButtonLocators;
    private List<By> skipButtonLocators;

    public TermsOfServicePage(AppiumDriver driver, String platform) {
        super(driver);

        if (platform.equalsIgnoreCase("ios")) {
            headlineLocators = List.of(
                    By.name("Terms of Service"),
                    By.xpath("//XCUIElementTypeStaticText[@label='Terms of Service']")
            );
            acceptAndContinueButtonLocators = List.of(
                    By.name("ACCEPT AND CONTINUE"),
                    By.xpath("//XCUIElementTypeButton[@label='ACCEPT AND CONTINUE']"),
                    By.xpath("//*[@label='ACCEPT AND CONTINUE']")
            );
            skipButtonLocators = List.of(
                    By.name("SKIP"),
                    By.name("Skip"),
                    By.xpath("//XCUIElementTypeButton[@label='SKIP']")
            );
        } else {
            headlineLocators = List.of(
                    By.id("com.cube.arc.fa:id/nav_title"),
                    By.xpath("//*[@text='Terms of Service']")
            );
            acceptAndContinueButtonLocators = List.of(
                    By.xpath("//*[@text='ACCEPT AND CONTINUE']"),
                    By.xpath("//*[contains(@text, 'ACCEPT')]")
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

    public boolean isAcceptAndContinueButtonVisible() {
        return isVisible(acceptAndContinueButtonLocators);
    }

    public void tapAcceptAndContinue() {
        tap(acceptAndContinueButtonLocators);
    }

    public boolean isSkipButtonAbsent() {
        return isAbsent(skipButtonLocators);
    }
}
