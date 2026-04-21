package com.cube.qa.framework.utils;

import io.appium.java_client.AppiumDriver;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.ios.IOSDriver;
import org.openqa.selenium.remote.DesiredCapabilities;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Paths;

public class DriverManager {

    public static AppiumDriver createDriver(String platform, String buildPath, String udid, boolean fullReset, boolean isSimulator, String deviceName, String platformVersion, String androidPackageName, String iosBundleId, String iosXcodeOrgId, String iosXcodeSigningId, String iosWdaBundleId) {
        DesiredCapabilities caps = new DesiredCapabilities();

        try {
            URL appiumServerUrl = URI.create("http://127.0.0.1:4723").toURL();

            if (platform.equalsIgnoreCase("android")) {
                caps.setCapability("platformName", "Android");
                caps.setCapability("deviceName", "Android Device");
                caps.setCapability("automationName", "UiAutomator2");

                if (isSimulator) {
                    caps.setCapability("isSimulator", true);
                }

                caps.setCapability("udid", udid);

                caps.setCapability("fullReset", fullReset);
                caps.setCapability("noReset", !fullReset);

                String fullApkPath = Paths.get(System.getProperty("user.dir"), buildPath).normalize().toString();
                caps.setCapability("app", fullApkPath);

                System.out.println("📡 Appium Server URL: " + appiumServerUrl);
                System.out.println("📦 APK Path: " + fullApkPath);
                System.out.println("📲 Device UDID: " + udid);
                System.out.println("🧠 Capabilities: " + caps);

                File appFile = new File(fullApkPath);
                System.out.println("🔍 File exists? " + appFile.exists());

                return new AndroidDriver(appiumServerUrl, caps);

            } else if (platform.equalsIgnoreCase("ios")) {
                caps.setCapability("platformName", "iOS");
                caps.setCapability("deviceName", "iPhone");
                caps.setCapability("automationName", "XCUITest");

                if (isSimulator) {
                    caps.setCapability("isSimulator", true);
                    caps.setCapability("deviceName", deviceName);
                    caps.setCapability("platformVersion", platformVersion);
                    // Omit udid if running on a simulator
                } else {
                    caps.setCapability("deviceName", deviceName);
                    caps.setCapability("udid", udid);
                }  // Add support for real iOS device too

                caps.setCapability("fullReset", fullReset);
                caps.setCapability("noReset", !fullReset);
                caps.setCapability("autoAcceptAlerts", false);

                // iOS: Simplified approach - only add optional capabilities if provided
                // No bundle ID required - Appium will handle app installation/launching automatically
                if (iosXcodeOrgId != null && !iosXcodeOrgId.isEmpty()) {
                    caps.setCapability("xcodeOrgId", iosXcodeOrgId);
                }
                if (iosXcodeSigningId != null && !iosXcodeSigningId.isEmpty()) {
                    caps.setCapability("xcodeSigningId", iosXcodeSigningId);
                }
                if (iosWdaBundleId != null && !iosWdaBundleId.isEmpty()) {
                    caps.setCapability("wdaBundleId", iosWdaBundleId);
                }

                String fullAppPath = Paths.get(System.getProperty("user.dir"), buildPath).normalize().toString();
                caps.setCapability("app", fullAppPath);

                System.out.println("📡 Appium Server URL: " + appiumServerUrl);
                System.out.println("📦 iOS App Path: " + fullAppPath);
                System.out.println("📲 Device UDID: " + udid);
                System.out.println("🧠 Capabilities: " + caps);

                return new IOSDriver(appiumServerUrl, caps);

            } else {
                throw new RuntimeException("Unsupported platform: " + platform);
            }

        } catch (MalformedURLException e) {
            throw new RuntimeException("Invalid Appium server URL", e);
        }
    }
}