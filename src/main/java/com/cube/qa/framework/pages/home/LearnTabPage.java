package com.cube.qa.framework.pages.home;

import com.cube.qa.framework.pages.BasePage;
import io.appium.java_client.AppiumDriver;
import org.openqa.selenium.By;

import java.util.List;

/**
 * Learn tab — the first tab after onboarding. For now this only exposes the
 * "Personalize Experience" CTA, which is the entry point to the personalization
 * modal. More Learn-tab helpers can be added here as tests land.
 */
public class LearnTabPage extends BasePage {

    private final List<By> personalizeCtaLocators;

    public LearnTabPage(AppiumDriver driver, String platform) {
        super(driver);

        if (platform.equalsIgnoreCase("ios")) {
            // iOS labels the button with title-cased or all-caps text depending
            // on env (matches the pattern we hit on the tab bar).
            personalizeCtaLocators = List.of(
                    By.name("PERSONALIZE EXPERIENCE"),
                    By.name("Personalize Experience"),
                    By.xpath("//XCUIElementTypeButton[@label='PERSONALIZE EXPERIENCE' or @label='Personalize Experience']")
            );
        } else {
            // Android: the Personalize card on the Learn tab wraps the button
            // in take_a_class_container → arc_button. Scope the xpath so we
            // don't collide with other arc_button buttons on the page.
            personalizeCtaLocators = List.of(
                    By.xpath("//*[@resource-id='com.cube.arc.fa:id/take_a_class_container']//android.widget.Button"),
                    By.xpath("//*[@text='PERSONALIZE EXPERIENCE']")
            );
        }
    }

    public boolean isPersonalizeCtaVisible() {
        return isVisible(personalizeCtaLocators);
    }

    public void tapPersonalizeCta() {
        tap(personalizeCtaLocators);
    }
}
