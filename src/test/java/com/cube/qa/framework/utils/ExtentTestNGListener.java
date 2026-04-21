// src/test/java/com/cube/qa/framework/utils/ExtentTestNGListener.java
package com.cube.qa.framework.utils;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.aventstack.extentreports.reporter.configuration.Theme;
import io.appium.java_client.AppiumDriver;
import org.testng.*;
import org.testng.xml.XmlSuite;
import org.testng.xml.XmlTest;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.List;

public class ExtentTestNGListener implements ITestListener, ISuiteListener {
    private static ExtentReports extent;
    private static ThreadLocal<ExtentTest> testThread = new ThreadLocal<>();
    private static boolean sysInfoSet = false;

    @Override
    public void onStart(ISuite suite) {
        XmlSuite xmlSuite = suite.getXmlSuite();
        List<XmlTest> xmlTests = xmlSuite.getTests();
        String suitePlatform = "unknown";
        if (xmlTests != null && !xmlTests.isEmpty()) {
            String p = xmlTests.get(0).getParameter("platform");
            if (p != null && !p.isEmpty()) suitePlatform = p;
        }

        String reportFile = String.format("target/extent-report-%s.html", suitePlatform);
        ExtentSparkReporter spark = new ExtentSparkReporter(reportFile);
        spark.config().setTheme(Theme.STANDARD);
        spark.config().setDocumentTitle("ARC Blood Test Report");
        spark.config().setReportName("Automated Test Results (" + suitePlatform + ")");

        extent = new ExtentReports();
        extent.attachReporter(spark);
    }

    @Override
    public void onFinish(ISuite suite) {
        extent.flush();
    }

    @Override
    public void onTestStart(ITestResult result) {
        if (!sysInfoSet) {
            ITestContext ctx    = result.getTestContext();
            XmlTest xmlTest     = ctx.getCurrentXmlTest();

            // 1) Platform, Environment, Build Number from XML
            String platform    = xmlTest.getParameter("platform");
            String env         = xmlTest.getParameter("env");
            String buildNumber = xmlTest.getParameter("buildNumber");
            if (platform    != null) extent.setSystemInfo("Platform", platform);
            if (env         != null) extent.setSystemInfo("Environment", env);
            if (buildNumber != null) extent.setSystemInfo("Build Number", buildNumber);

            // 2) Device model: simulators via simctl, real via ideviceinfo, Android via caps
            Object drvObj = ctx.getAttribute("driver");
            if (drvObj instanceof AppiumDriver) {
                AppiumDriver driver = (AppiumDriver) drvObj;
                String deviceLabel = "unknown";

                if ("ios".equalsIgnoreCase(platform)) {
                    // attempt simulator lookup
                    Object udidCap = driver.getCapabilities().getCapability("udid");
                    if (udidCap != null) {
                        String simModel = lookupIosSimulatorName(udidCap.toString());
                        if (simModel != null) {
                            deviceLabel = simModel;
                        }
                    }
                    if ("unknown".equals(deviceLabel)) {
                        // attempt real-device lookup
                        if (udidCap != null) {
                            String realModel = lookupIosRealDeviceModel(udidCap.toString());
                            if (realModel != null) {
                                deviceLabel = realModel;
                            }
                        }
                    }
                    if ("unknown".equals(deviceLabel)) {
                        Object dn = driver.getCapabilities().getCapability("deviceName");
                        deviceLabel = dn != null ? dn.toString() : "iOS device";
                    }

                } else {
                    Object modelCap = driver.getCapabilities().getCapability("appium:deviceModel");
                    if (modelCap == null) modelCap = driver.getCapabilities().getCapability("deviceModel");
                    if (modelCap == null) modelCap = driver.getCapabilities().getCapability("deviceName");
                    deviceLabel = modelCap != null ? modelCap.toString() : "Android device";
                }
                extent.setSystemInfo("Device", deviceLabel);

                // 3) OS
                Object plat = driver.getCapabilities().getCapability("platformName");
                Object ver  = driver.getCapabilities().getCapability("platformVersion");
                if (plat != null && ver != null) {
                    extent.setSystemInfo("OS", plat + " " + ver);
                }
            }

            sysInfoSet = true;
        }

        ExtentTest t = extent.createTest(result.getMethod().getMethodName());
        testThread.set(t);
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        testThread.get().pass("Test passed");
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        testThread.get().skip(result.getThrowable());
    }

    @Override
    public void onTestFailure(ITestResult result) {
        ExtentTest t = testThread.get();
        Throwable ex = result.getThrowable();

        String msg = ex.getMessage() != null
                ? ex.getMessage()
                : result.getMethod().getMethodName() + " failed";
        t.fail(msg);
        t.fail(ex);

        Object drvObj = result.getTestContext().getAttribute("driver");
        if (drvObj instanceof AppiumDriver) {
            AppiumDriver driver = (AppiumDriver) drvObj;
            String relPath = ScreenshotHelper.capture(driver, result.getMethod().getMethodName());
            if (relPath != null) {
                t.addScreenCaptureFromPath(relPath);
            }
        }
    }

    // ---- Helper methods ----

    private static String lookupIosSimulatorName(String udid) {
        try {
            Process p = new ProcessBuilder("xcrun", "simctl", "list", "devices").start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.contains("(" + udid + ")")) {
                        return line.trim().split(" \\(" + udid)[0];
                    }
                }
            }
            p.waitFor();
        } catch (Exception ignored) {}
        return null;
    }

    private static String lookupIosRealDeviceModel(String udid) {
        try {
            Process p = new ProcessBuilder("ideviceinfo", "-u", udid, "-k", "ProductType").start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String prodType = reader.readLine();
                p.waitFor();
                if (prodType != null && prodType.contains(",")) {
                    String raw = prodType.split(",")[0];
                    return raw.replaceAll("([a-zA-Z]+)(\\d+)", "$1 $2");
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    // unused stubs
    @Override public void onTestFailedButWithinSuccessPercentage(ITestResult r) {}
    @Override public void onStart(ITestContext ctx) {}
    @Override public void onFinish(ITestContext ctx) {}
}
