package com.cube.qa.framework.pages;

import io.appium.java_client.AppiumDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.lang.reflect.Field;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BasePage {

    protected final AppiumDriver driver;
    private final Duration defaultTimeout = Duration.ofSeconds(30);

    public BasePage(AppiumDriver driver) {
        this.driver = driver;
    }

    // === Core wait methods ===

    protected WebElement waitForVisibility(List<By> locators) {
        String fieldName = findFieldNameFor(locators);
        for (By locator : locators) {
            try {
                System.out.println("🔍 Trying visibilityOfElementLocated: " + locator);
                return new WebDriverWait(driver, defaultTimeout)
                        .until(ExpectedConditions.visibilityOfElementLocated(locator));
            } catch (Exception e) {
                System.out.println("❌ Locator failed: " + locator + " – " + e.getMessage());
            }
        }
        throw new RuntimeException(fieldName + " not visible");
    }

    protected WebElement waitForPresence(List<By> locators) {
        String fieldName = findFieldNameFor(locators);
        for (By locator : locators) {
            try {
                System.out.println("🔍 Trying presenceOfElementLocated: " + locator);
                return new WebDriverWait(driver, defaultTimeout)
                        .until(ExpectedConditions.presenceOfElementLocated(locator));
            } catch (Exception e) {
                System.out.println("❌ Locator failed: " + locator + " – " + e.getMessage());
            }
        }
        throw new RuntimeException(fieldName + " not present");
    }

    protected WebElement waitToBeClickable(List<By> locators) {
        String fieldName = findFieldNameFor(locators);
        for (By locator : locators) {
            try {
                System.out.println("🔍 Trying elementToBeClickable: " + locator);
                return new WebDriverWait(driver, defaultTimeout)
                        .until(ExpectedConditions.elementToBeClickable(locator));
            } catch (Exception e) {
                System.out.println("❌ Locator failed: " + locator + " – " + e.getMessage());
            }
        }
        throw new RuntimeException(fieldName + " not clickable");
    }

    protected boolean waitForInvisibility(List<By> locators) {
        String fieldName = findFieldNameFor(locators);
        for (By locator : locators) {
            try {
                System.out.println("🔍 Waiting for invisibility: " + locator);
                return new WebDriverWait(driver, defaultTimeout)
                        .until(ExpectedConditions.invisibilityOfElementLocated(locator));
            } catch (Exception e) {
                System.out.println("❌ Invisibility failed: " + locator + " – " + e.getMessage());
            }
        }
        throw new RuntimeException(fieldName + " not invisible");
    }

    protected boolean waitForTextToBePresent(List<By> locators, String text) {
        String fieldName = findFieldNameFor(locators);
        for (By locator : locators) {
            try {
                System.out.println("🔍 Waiting for text '" + text + "' in: " + locator);
                return new WebDriverWait(driver, defaultTimeout)
                        .until(ExpectedConditions.textToBePresentInElementLocated(locator, text));
            } catch (Exception e) {
                System.out.println("❌ Text wait failed: " + locator + " – " + e.getMessage());
            }
        }
        throw new RuntimeException("Text '" + text + "' not found in " + fieldName);
    }

    // === Abstracted interaction methods ===

    protected void tap(List<By> locators) {
        WebElement element = waitToBeClickable(locators);
        try {
            Thread.sleep(300); // Slight pause to ensure visibility before tap
        } catch (InterruptedException ignored) {}
        element.click();
    }

    protected void enterText(List<By> locators, String text) {
        WebElement el = waitForVisibility(locators);
        el.clear();
        el.sendKeys(text);
    }

    protected boolean isVisible(List<By> locators) {
        return waitForVisibility(locators).isDisplayed(); // throws RuntimeException if not found
    }

    protected boolean isInvisible(List<By> locators) {
        return waitForInvisibility(locators); // throws RuntimeException if not found
    }

    // Non-throwing presence check — use when absence is a valid outcome (e.g.
    // a loop-termination condition or a "should NOT display" assertion).
    public boolean isPresent(List<By> locators) {
        for (By locator : locators) {
            if (!driver.findElements(locator).isEmpty()) return true;
        }
        return false;
    }

    public boolean isAbsent(List<By> locators) {
        return !isPresent(locators);
    }

    // Returns every non-empty text string currently rendered on screen. Used
    // for dynamic-content capture (tooltip copy, welcome-card bullets) where
    // the exact strings aren't known up-front.
    public List<String> visibleTexts() {
        String platform = driver.getCapabilities().getPlatformName().toString().toLowerCase();
        By locator = platform.equals("ios")
                ? By.xpath("//XCUIElementTypeStaticText")
                : By.xpath("//android.widget.TextView");
        List<String> texts = new ArrayList<>();
        for (WebElement el : driver.findElements(locator)) {
            try {
                String value = platform.equals("ios") ? el.getAttribute("label") : el.getText();
                if (value != null && !value.isBlank()) {
                    texts.add(value.trim());
                }
            } catch (Exception ignored) {}
        }
        return texts;
    }

    // True if any on-screen text contains the supplied substring (case-insensitive).
    public boolean containsText(String substring) {
        String needle = substring.toLowerCase();
        for (String text : visibleTexts()) {
            if (text.toLowerCase().contains(needle)) return true;
        }
        return false;
    }

    protected boolean hasText(List<By> locators, String expectedText) {
        return waitForTextToBePresent(locators, expectedText); // throws RuntimeException if not found
    }

    protected String getText(List<By> locators) {
        return waitForVisibility(locators).getText();
    }

    protected void clear(List<By> locators) {
        waitForVisibility(locators).clear();
    }

    protected boolean isChecked(List<By> locators) {
        return waitForVisibility(locators).isSelected();
    }

    protected boolean isUnchecked(List<By> locators) {
        return !waitForVisibility(locators).isSelected();
    }

    protected void dismissKeyboard() {
        try {
            driver.executeScript("mobile: hideKeyboard", Map.of());
        } catch (Exception ignored) {}
    }

    protected void dismissAlertIfPresent() {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(3))
                    .until(ExpectedConditions.alertIsPresent());
            driver.switchTo().alert().dismiss();
        } catch (Exception ignored) {}
    }

    // === Scrolling method ===

    @SuppressWarnings("unchecked")
    protected WebElement scrollToElement(By locator) {
        String platform = driver.getCapabilities().getPlatformName().toString().toLowerCase();
        int maxScrolls = 5;
        int scrolled = 0;

        while (scrolled < maxScrolls) {
            try {
                WebElement element = driver.findElement(locator);
                if (element.isDisplayed()) return element;
            } catch (Exception ignored) {}

            if (platform.equals("ios")) {
                driver.executeScript("mobile: swipe", Map.of("direction", "up"));
            } else if (platform.equals("android")) {
                Map<String, Object> scrollArgs = Map.of(
                        "left", 100,
                        "top", 500,
                        "width", 800,
                        "height", 1200,
                        "direction", "down",
                        "percent", 0.7
                );
                driver.executeScript("mobile: scrollGesture", scrollArgs);
            } else {
                throw new RuntimeException("Unsupported platform");
            }

            waitForSeconds(1);
            scrolled++;
        }

        throw new RuntimeException("Element not found after scrolling: " + locator);
    }




    protected void scrollToFirstVisible(List<By> locators) {
        for (By locator : locators) {
            try {
                scrollToElement(locator);
                return;
            } catch (Exception ignored) {}
        }
        throw new RuntimeException("None of the locators found after scrolling.");
    }


    /**
     * isDynamicTextVisible()
     * Checks whether a dynamically generated string is visible on screen
     * without requiring predefined locators.
     */
    public boolean isDynamicTextVisible(String text) {
        String platform = driver.getCapabilities().getPlatformName().toString().toLowerCase();
        By dynamicLocator;

        if (platform.equals("android")) {
            dynamicLocator = By.xpath("//*[contains(@text, '" + text + "')]");
        } else if (platform.equals("ios")) {
            dynamicLocator = By.xpath("//XCUIElementTypeStaticText[contains(@name, '" + text + "') or contains(@label, '" + text + "') or contains(@value, '" + text + "')]");
        } else {
            throw new RuntimeException("Unsupported platform: " + platform);
        }

        return isVisible(List.of(dynamicLocator));
    }

    private String extractText(By locator) {
        String raw = locator.toString();
        int index = raw.indexOf(":");
        if (index != -1 && index + 1 < raw.length()) {
            return raw.substring(index + 1).trim().replaceAll("[^a-zA-Z0-9\\s]", "");
        }
        return "";
    }


    // === Reflection for better error output ===

    private String findFieldNameFor(List<By> locators) {
        Class<?> cls = this.getClass();
        for (Field f : cls.getDeclaredFields()) {
            if (List.class.isAssignableFrom(f.getType())) {
                f.setAccessible(true);
                try {
                    if (f.get(this) == locators) {
                        return f.getName();
                    }
                } catch (IllegalAccessException ignored) {}
            }
        }
        return "element";
    }

    // Wait function for slow to load elements
    protected void waitForSeconds(int seconds) {
        try {
            System.out.println("⏳ Waiting for " + seconds + " seconds");
            Thread.sleep(seconds * 1000L);
        } catch (InterruptedException ignored) {
            System.out.println("⚠ Interrupted during wait");
        }
    }
}
