package com.cube.qa.framework.pages.home;

import com.cube.qa.framework.pages.BasePage;
import io.appium.java_client.AppiumDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.util.List;

/**
 * Topic Detail page — reached by tapping a card in the Learn tab's Browse
 * Topics carousel or the vertical list below it. Exposes the pin/unpin
 * toolbar action and the "You can now pin content" tooltip shown on the first
 * visit after a fresh install.
 *
 * <p>Pin state is read from the toolbar button's accessibility label:
 * <ul>
 *   <li><b>Android</b>: {@code content-desc} flips between {@code "Pin"}
 *       (currently unpinned) and {@code "Unpin"} (currently pinned) — the
 *       label describes the action the tap will perform.</li>
 *   <li><b>iOS</b>: the button's {@code name} flips between
 *       {@code "Topic Unpinned"} and {@code "Topic Pinned"} — the label
 *       describes the current state. We also key off the same strings for
 *       the toast that briefly appears after a pin/unpin action.</li>
 * </ul>
 */
public class TopicDetailPage extends BasePage {

    private final String platform;

    private final List<By> pinButtonLocators;
    private final List<By> pinnedStateLocators;
    private final List<By> unpinnedStateLocators;
    private final List<By> titleLocators;
    private final List<By> backLocators;
    // Pin-first-visit tooltip.
    private final List<By> pinTooltipTitleLocators;
    private final List<By> pinTooltipSubtitleLocators;

    public TopicDetailPage(AppiumDriver driver, String platform) {
        super(driver);
        this.platform = platform.toLowerCase();

        if (this.platform.equals("ios")) {
            // iOS toolbar pin button is named for its current state: "Topic
            // Pinned" when pinned, "Topic Unpinned" when not. The toast that
            // appears after tapping uses the same strings on a StaticText —
            // disambiguate by element type.
            pinButtonLocators = List.of(
                    By.xpath("//XCUIElementTypeButton[@name='Topic Pinned' or @name='Topic Unpinned']")
            );
            pinnedStateLocators = List.of(
                    By.xpath("//XCUIElementTypeButton[@name='Topic Pinned']")
            );
            unpinnedStateLocators = List.of(
                    By.xpath("//XCUIElementTypeButton[@name='Topic Unpinned']")
            );
            titleLocators = List.of(
                    By.xpath("//XCUIElementTypeNavigationBar//XCUIElementTypeStaticText")
            );
            backLocators = List.of(
                    By.name("BackButton"),
                    By.xpath("//XCUIElementTypeButton[@name='BackButton']")
            );
            pinTooltipTitleLocators = List.of(
                    By.name("You can now pin content")
            );
            pinTooltipSubtitleLocators = List.of(
                    By.xpath("//XCUIElementTypeStaticText[starts-with(@name,'Pin topics that you plan')]")
            );
        } else {
            // Android swaps the resource-id of the toolbar button when the
            // topic is pinned vs unpinned: `action_pin_button` (tap = pin) →
            // `action_unpin_button` (tap = unpin). The content-desc mirrors
            // the action ("Pin" / "Unpin"). Accept either id for a tap so
            // callers don't care about current state.
            pinButtonLocators = List.of(
                    By.xpath("//*[@resource-id='com.cube.arc.fa:id/action_pin_button' or " +
                            "@resource-id='com.cube.arc.fa:id/action_unpin_button']")
            );
            pinnedStateLocators = List.of(
                    By.id("com.cube.arc.fa:id/action_unpin_button")
            );
            unpinnedStateLocators = List.of(
                    By.id("com.cube.arc.fa:id/action_pin_button")
            );
            titleLocators = List.of(
                    By.id("com.cube.arc.fa:id/overview_title")
            );
            backLocators = List.of(
                    By.xpath("//*[@content-desc='Navigate up']")
            );
            pinTooltipTitleLocators = List.of(
                    By.id("com.cube.arc.fa:id/tooltip_title"),
                    By.xpath("//*[@text='You can now pin content']")
            );
            pinTooltipSubtitleLocators = List.of(
                    By.id("com.cube.arc.fa:id/tooltip_subtitle")
            );
        }
    }

    // ---- Title / navigation ------------------------------------------------

    public String getTitle() {
        WebElement el = waitForVisibility(titleLocators);
        return platform.equals("ios") ? el.getAttribute("name") : el.getText();
    }

    public void tapBack() {
        tap(backLocators);
    }

    // ---- Pin / unpin -------------------------------------------------------

    public boolean isPinButtonVisible() {
        return isVisible(pinButtonLocators);
    }

    public boolean isPinned() {
        return isPresent(pinnedStateLocators);
    }

    public boolean isUnpinned() {
        return isPresent(unpinnedStateLocators);
    }

    public void tapPin() {
        tap(pinButtonLocators);
    }

    /**
     * True while the "Topic Pinned" or "Topic Unpinned" toast is on screen.
     * Toasts disappear fast (about 2s); callers should poll right after
     * tapping pin rather than rely on long waits.
     */
    public boolean isToastPresent(String text) {
        if (platform.equals("ios")) {
            return isPresent(List.of(
                    By.xpath("//XCUIElementTypeStaticText[@name='" + text + "']")));
        }
        // Android toasts render as anonymous TextViews with the toast text —
        // we look for any element with that exact text.
        return isPresent(List.of(
                By.xpath("//*[@text='" + text + "']")));
    }

    // ---- Pin-first-visit tooltip ------------------------------------------

    public boolean isPinTooltipVisible() {
        return isVisible(pinTooltipTitleLocators);
    }

    public boolean isPinTooltipPresent() {
        return isPresent(pinTooltipTitleLocators);
    }

    public String getPinTooltipSubtitle() {
        WebElement el = waitForVisibility(pinTooltipSubtitleLocators);
        return platform.equals("ios") ? el.getAttribute("name") : el.getText();
    }
}
