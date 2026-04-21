package com.cube.qa.framework.pages.onboarding;

import com.cube.qa.framework.pages.BasePage;
import io.appium.java_client.AppiumDriver;
import org.openqa.selenium.By;
import java.util.List;

public class WelcomePage extends BasePage {

    // Locators
    private List<By> arcLogoLocators;
    private List<By> titleLocators;
    private List<By> subtitleLocators;
    private List<By> signInButtonLocators;
    private List<By> continueAsGuestButtonLocators;
    private List<By> firstAidContentButtonLocators;

    public WelcomePage(AppiumDriver driver, String platform) {
        super(driver);

        if (platform.equalsIgnoreCase("ios")) {
            arcLogoLocators = List.of(
                    By.name("arcLogo"),
                    By.name("American Red Cross")
            );
            titleLocators = List.of(
                    By.name("First Aid")
            );
            subtitleLocators = List.of(
                    By.name("Learn Lifesaving Skills")
            );
            signInButtonLocators = List.of(
                    By.name("SIGN IN"),
                    By.name("Sign In")
            );
            continueAsGuestButtonLocators = List.of(
                    By.name("CONTINUE AS GUEST"),
                    By.name("Continue as Guest")
            );
            firstAidContentButtonLocators = List.of(
                    By.name("FIRST AID CONTENT"),
                    By.name("First Aid Content")
            );

        } else {
            arcLogoLocators = List.of(
                    By.id("com.cube.arc.fa:id/image_arc_icon")
            );
            titleLocators = List.of(
                    By.id("com.cube.arc.fa:id/text_title")
            );
            subtitleLocators = List.of(
                    By.id("com.cube.arc.fa:id/text_subtitle")
            );
            // Buttons share arc_button resource-id internally — use wrapper xpath to target each uniquely
            signInButtonLocators = List.of(
                    By.xpath("//*[@resource-id='com.cube.arc.fa:id/sign_in_button']//android.widget.Button"),
                    By.xpath("//*[@text='SIGN IN']")
            );
            continueAsGuestButtonLocators = List.of(
                    By.xpath("//*[@resource-id='com.cube.arc.fa:id/continue_as_guest_button']//android.widget.Button"),
                    By.xpath("//*[@text='CONTINUE AS GUEST']")
            );
            firstAidContentButtonLocators = List.of(
                    By.xpath("//*[@resource-id='com.cube.arc.fa:id/first_aid_content_button']//android.widget.Button"),
                    By.xpath("//*[@text='FIRST AID CONTENT']")
            );
        }
    }

    // ARC Logo
    public boolean isArcLogoVisible() {
        return isVisible(arcLogoLocators);
    }

    // Title
    public boolean isTitleVisible() {
        return isVisible(titleLocators);
    }

    // Subtitle
    public boolean isSubtitleVisible() {
        return isVisible(subtitleLocators);
    }

    // Sign In Button
    public boolean isSignInButtonVisible() {
        return isVisible(signInButtonLocators);
    }
    public void tapSignIn() {
        tap(signInButtonLocators);
    }

    // Continue as Guest Button
    public boolean isContinueAsGuestButtonVisible() {
        return isVisible(continueAsGuestButtonLocators);
    }
    public void tapContinueAsGuest() {
        tap(continueAsGuestButtonLocators);
    }

    // First Aid Content Button
    public boolean isFirstAidContentButtonVisible() {
        return isVisible(firstAidContentButtonLocators);
    }
    public void tapFirstAidContent() {
        tap(firstAidContentButtonLocators);
    }
}
