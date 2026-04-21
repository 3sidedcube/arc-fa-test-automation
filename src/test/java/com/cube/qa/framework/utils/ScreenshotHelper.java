// src/main/java/com/cube/qa/framework/utils/ScreenshotHelper.java
package com.cube.qa.framework.utils;

import io.appium.java_client.AppiumDriver;
import org.openqa.selenium.OutputType;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Captures a screenshot from the driver and writes it under
 *   target/screenshots/{platform}/{testName}.png
 * Returns a relative path ("screenshots/{platform}/{testName}.png")
 * for ExtentReports to embed.
 */
public class ScreenshotHelper {

    public static String capture(AppiumDriver driver, String testName) {
        try {
            byte[] data = driver.getScreenshotAs(OutputType.BYTES);

            // determine platform subfolder
            Object platCap = driver.getCapabilities().getCapability("platformName");
            String platform = platCap != null
                    ? platCap.toString().toLowerCase()
                    : "unknown";

            // create directory target/screenshots/{platform}
            Path destDir = Paths.get("target", "screenshots", platform);
            Files.createDirectories(destDir);

            // write file
            String fileName = testName + ".png";
            Path img = destDir.resolve(fileName);
            Files.write(img, data);
            System.out.println("✅ Screenshot saved: " + img);

            // return path relative to target/
            return String.format("screenshots/%s/%s", platform, fileName);

        } catch (Exception e) {
            System.err.println("❌ Failed to capture screenshot: " + e.getMessage());
            return null;
        }
    }
}
