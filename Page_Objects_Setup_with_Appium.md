# Page Objects Setup & Appium Inspector Context

This document combines the **Page Objects Setup Guide** with additional context for a QA starting out in test automation. It explains how to capture objects using Appium Inspector, export them, and integrate them into our Page Objects framework.

---

## 1. Additional Context for New QA

### Capturing Objects with Appium Inspector
- **Appium Inspector** lets you connect to a device or emulator and inspect the UI hierarchy.
- Once connected, you can:
  - Navigate to the screen you want to test.
  - Export the visible UI tree as XML.
  - Use that XML to identify and store locators in your page classes.

### Using Cursor to Build Pages
- Paste the XML output from Appium Inspector into **Cursor**. Cursor can:
  - Build a new page class with locators and methods.
  - Suggest functions for interacting with those locators.
- You can also drag and drop a screenshot into Cursor:
  - Cursor will extract text and suggest locators/functions.
  - This works well for verifying and debugging.
- **Recommended workflow:**
  - Use both XML **and** screenshots to get robust locator definitions.
  - Always capture for **both iOS and Android**, as locators differ by platform.

### Debugging Locators
- If a locator breaks:
  - Open Appium Inspector.
  - Click on the element directly.
  - Copy the corrected locator.
- Cursor can also assist with debugging by generating updated locators from screenshots or XML.

---

## 2. Appium Inspector Capabilities

Below are the capabilities you can use for connecting Appium Inspector to devices.

### Android Capabilities
```json
{
  "platformName": "Android",
  "appium:platformVersion": "15",
  "appium:app": "/Users/tylerferrington/Desktop/Automation Projects/arc-emergency-test-automation/src/test/resources/apps/android/universal.apk",
  "appium:automationName": "uiautomator2",
  "appium:udid": "33071FDH2007QH"
}
```
**Explanation:**
- `platformName`: The platform under test (Android).
- `appium:platformVersion`: The Android OS version.
- `appium:app`: Path to the app binary under test.
- `appium:automationName`: Framework to use (`uiautomator2` for Android).
- `appium:udid`: Device identifier for connecting Appium Inspector to the device.

### iOS Capabilities
```json
{
  "platformName": "iOS",
  "appium:platformVersion": "18.3.1",
  "appium:deviceName": "TF - iPhone 12",
  "appium:udid": "00008101-000E49EA1081401E",
  "appium:automationName": "XCUITest",
  "appium:shouldUseCompactResponses": false,
  "appium:snapshotMaxDepth": 15,
  "appium:snapshotTimeout": 8000,
  "appium:app": "/Users/tylerferrington/Desktop/Automation Projects/arc-emergency-test-automation/src/test/resources/apps/ios/Emergency.ipa",
  "appium:xcodeOrgId": "25H7BM6YWK",
  "appium:xcodeSigningId": "iPhone Developer",
  "appium:noReset": true,
  "appium:useNewWDA": true,
  "appium:startIWDP": true,
  "appium:showXcodeLog": true,
  "appium:derivedDataPath": "/Users/tylerferrington/Library/Developer/Xcode/DerivedData",
  "appium:useJSONSource": true
}
```
**Explanation:**
- `platformName`: The platform under test (iOS).
- `appium:platformVersion`: The iOS version.
- `appium:deviceName`: Human-readable device name.
- `appium:udid`: Unique identifier for the device.
- `appium:automationName`: Framework to use (`XCUITest` for iOS).
- `appium:shouldUseCompactResponses`: Whether to minimize XML output.
- `appium:snapshotMaxDepth`: Depth of UI hierarchy capture.
- `appium:snapshotTimeout`: Timeout for snapshot retrieval.
- `appium:app`: Path to the iOS app binary.
- `appium:xcodeOrgId`: Apple team ID for signing.
- `appium:xcodeSigningId`: Signing certificate.
- `appium:noReset`: Keeps app state between sessions.
- `appium:useNewWDA`: Forces rebuilding of WebDriverAgent if needed.
- `appium:startIWDP`: Starts iOS WebKit Debug Proxy for hybrid apps.
- `appium:showXcodeLog`: Shows Xcode logs in output.
- `appium:derivedDataPath`: Custom path for Xcode build data.
- `appium:useJSONSource`: Ensures Inspector gets JSON-based source dumps.

---

## 3. Page Objects & Locators (Framework Guide)

### Pages Setup and File Structure
- Each page class is under `src/main/java/com/cube/qa/framework/pages/...` and extends `BasePage`.
- Constructors accept `(AppiumDriver driver, String platform)`.
- Locators are initialized per platform.

### Declaring Locators
- Use `List<By>` to define multiple strategies for a single element.
- Descriptive naming is required.
- Initialize inside constructors with iOS vs Android logic.

### Example
```java
if (platform.equalsIgnoreCase("ios")) {
    pageTitleLocators = List.of(
        By.xpath("//XCUIElementTypeNavigationBar//*[@name='Locations']")
    );
} else {
    pageTitleLocators = List.of(
        By.xpath("//*[@resource-id='com.cube.arc.hzd:id/page_name' and @text='Locations']")
    );
}
```

### iOS vs Android Locators
- **iOS:** Use XCUI types with `@name/@label/@value`.
- **Android:** Use `@resource-id` and `@text`.
- Prefer stable attributes over full brittle XPaths.

### Core Utilities in BasePage
- Waits, taps, typing, visibility checks.
- Supports multiple locators per element.

### Creating Page Functions
- Wrap locators in expressive methods (e.g., `tapDeleteLocationButton`).
- Keep platform logic in constructors, not in tests.

### Page Factory
- Provides easy access to page classes through `pages` object.
- Example: `pages.locationsPage()`.

### Using Page Functions in Tests
- Extend `BaseTest`.
- Use `@BeforeMethod` to initialize pages.
- Write tests using page methods rather than direct locators.

### Checklist for Adding a New Element
1. Find the correct page class.
2. Add a locator list field.
3. Initialize with platform-specific locators.
4. Add public methods for visibility/tap/type.
5. Use new methods in tests.

---

## 4. Recommended Workflow Summary
1. Use Appium Inspector with the provided capabilities to connect to devices.
2. Export XML and take screenshots for the target screen.
3. Paste XML and screenshots into Cursor to generate locators and page classes.
4. Add generated locators into page classes with both iOS and Android definitions.
5. Wrap them in descriptive public methods.
6. Write tests using those page functions.
7. Debug broken locators with Appium Inspector directly.

---

This guide should give the new QA both the **framework setup** and the **practical workflow** to capture and maintain page objects effectively.
