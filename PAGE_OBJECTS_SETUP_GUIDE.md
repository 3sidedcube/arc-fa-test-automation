## Page Objects and Locators – Project Guide

This guide explains how page objects, locators, and interactions are designed in this repository, and how to use them in tests.

### 1) Pages setup and `*.java` file structure

- Each page class lives under `src/main/java/com/cube/qa/framework/pages/...` and extends `BasePage`.
- Constructors accept `(AppiumDriver driver, String platform)` and initialize platform-specific locators.

Example page class skeleton:

```12:24:src/main/java/com/cube/qa/framework/pages/info/InfoPage.java
public class InfoPage extends BasePage {

    // Section Headers
    private List<By> featuredSectionHeaderLocators;
    // ...

    public InfoPage(AppiumDriver driver, String platform) {
        super(driver);

        if (platform.equalsIgnoreCase("ios")) {
            // iOS locators
        } else {
            // Android locators
        }
    }
}
```

### 2) Declaring locators

- Locators are stored as `List<By>` to support multiple fallback strategies per element.
- Name fields descriptively (what the element is, not how it’s located).
- Initialize locators inside the constructor based on platform.

```49:76:src/main/java/com/cube/qa/framework/pages/info/InfoPage.java
if (platform.equalsIgnoreCase("ios")) {
    featuredSectionHeaderLocators = List.of(
        By.xpath("//XCUIElementTypeStaticText[@name='Featured']")
    );
    // ... more iOS locators
} else {
    featuredSectionHeaderLocators = List.of(
        By.xpath("//*[@text='Featured']")
    );
    // ... more Android locators
}
```

Another example with toolbar/search and empty-state elements:

```32:46:src/main/java/com/cube/qa/framework/pages/locations/LocationsPage.java
if (platform.equalsIgnoreCase("ios")) {
    pageTitleLocators = List.of(
        By.xpath("//XCUIElementTypeNavigationBar//*[@name='Locations']")
    );
    searchFieldLocators = List.of(
        By.xpath("//XCUIElementTypeSearchField[@name='Search ZIP code or location']")
    );
    // ...
} else {
    pageTitleLocators = List.of(
        By.xpath("//*[@resource-id='com.cube.arc.hzd:id/page_name' and @text='Locations']")
    );
    searchFieldLocators = List.of(
        By.xpath("//*[@resource-id='android:id/search_src_text']")
    );
    // ...
}
```

### 3) iOS vs Android locators

- iOS commonly uses XCUI types like `XCUIElementTypeStaticText` with `@name/@label/@value`.
- Android commonly uses `@resource-id` and `@text`.
- Prefer stable attributes over brittle full XPaths; use multiple `By` entries for resilience.

```39:56:src/main/java/com/cube/qa/framework/pages/locations/LocationDetailsPage.java
if (platform.equalsIgnoreCase("ios")) {
    deleteLocationButtonLocators = List.of(
        By.xpath("//XCUIElementTypeButton[@name='Delete location']")
    );
    // ... iOS
} else {
    deleteLocationButtonLocators = List.of(
        By.xpath("//*[@resource-id='com.cube.arc.hzd:id/delete_location_button']")
    );
    // ... Android
}
```

### 4) Core interaction and wait utilities (from `BasePage`)

`BasePage` centralizes waiting and interactions and supports multiple locators per element.

```23:37:src/main/java/com/cube/qa/framework/pages/BasePage.java
// Visibility wait tries each locator until one works
protected WebElement waitForVisibility(List<By> locators) {
    // ...
}
```

```95:117:src/main/java/com/cube/qa/framework/pages/BasePage.java
// Interactions and checks
protected void tap(List<By> locators) { /* waits then click */ }
protected void enterText(List<By> locators, String text) { /* clear/send */ }
protected boolean isVisible(List<By> locators) { /* visibility */ }
protected boolean isInvisible(List<By> locators) { /* invisibility */ }
protected boolean hasText(List<By> locators, String expectedText) { /* text wait */ }
protected String getText(List<By> locators) { /* read */ }
```

Scrolling helpers:

```141:174:src/main/java/com/cube/qa/framework/pages/BasePage.java
protected WebElement scrollToElement(By locator) { /* iOS swipe / Android scrollGesture */ }
```

```179:187:src/main/java/com/cube/qa/framework/pages/BasePage.java
protected void scrollToFirstVisible(List<By> locators) { /* try each until found */ }
```

Dynamic text utility:

```191:208:src/main/java/com/cube/qa/framework/pages/BasePage.java
public boolean isDynamicTextVisible(String text) { /* builds platform-specific XPath */ }
```

### 5) Creating page functions (visibility, tap, type, scroll)

- Wrap `BasePage` utilities in clear public methods that express intent.
- Example: visibility check and tap methods in `InfoPage`:

```195:205:src/main/java/com/cube/qa/framework/pages/info/InfoPage.java
public boolean isFeaturedSectionHeaderVisible() { scrollToElement(featuredSectionHeaderLocators.get(0)); return isVisible(featuredSectionHeaderLocators); }
```

```247:249:src/main/java/com/cube/qa/framework/pages/info/InfoPage.java
public void tapTestNewFeaturesButton() { scrollToElement(testNewFeaturesButtonLocators.get(0)); tap(testNewFeaturesButtonLocators); }
```

Example: actions in `LocationDetailsPage`:

```214:227:src/main/java/com/cube/qa/framework/pages/locations/LocationDetailsPage.java
public boolean isDeleteLocationButtonVisible() { return isVisible(deleteLocationButtonLocators); }
public void tapDeleteLocationButton() { scrollToElement(deleteLocationButtonLocators.get(0)); tap(deleteLocationButtonLocators); }
public void tapDeleteConfirmYes() { tap(deleteConfirmYesButtonLocators); }
```

### 6) Page Factory

The `PageFactory` creates page instances with the active `driver` and `platform`. Access it in tests via `pages` from `BaseTest`.

```30:41:src/main/java/com/cube/qa/framework/utils/PageFactory.java
public TabsPage tabsPage() { return new TabsPage(driver, platform); }
public WelcomePage welcomePage() { return new WelcomePage(driver, platform); }
public LocationsPage locationsPage() { return new LocationsPage(driver, platform); }
// ... other pages
```

### 7) Using page functions in tests

- Tests extend `BaseTest` which initializes `driver`, `config`, and `pages` per platform.
- In `@BeforeMethod`, fetch page objects from `pages` if you want class fields, or call `pages.<page>()` inline.
- Use helper flows like `completeOnboardingFlow()`.

Example test structure (from `tests.InfoTests`):

```17:22:src/test/java/tests/InfoTests.java
@BeforeMethod(alwaysRun = true)
public void setUpTest() {
    infoPage = pages.infoPage();
    tabsPage = pages.tabsPage();
    testNewFeaturesPage = pages.testNewFeaturesPage();
}
```

```56:75:src/test/java/tests/InfoTests.java
@Test(description = "Test New Features - Display", groups = {"regression"})
public void TC_24851() {
    completeOnboardingFlow();
    tabsPage.tapInfoTab();
    infoPage.tapTestNewFeaturesButton();
    testNewFeaturesPage.isTitleVisible();
    // ... other visibility checks
}
```

### 8) Patterns to follow

- Prefer intent-revealing method names in pages (e.g., `tapDeleteLocationButton`, `isAddLocationPrimaryButtonVisible`).
- Keep platform logic inside the page constructor, not in test methods.
- When adding a new element:
  - Check the page first to avoid duplicates.
  - Add both iOS and Android locators when applicable.
  - Use multiple `By` strategies for robustness.
- For long lists or off-screen elements, call `scrollToElement` before actions/visibility checks.

### 9) Quick checklist for adding a new page element

1. Identify the correct page class.
2. Add a `private List<By> <descriptiveName>Locators;` field.
3. Initialize iOS vs Android locators in the constructor.
4. Add public methods that wrap `BasePage` utilities:
   - `isXVisible()`, `tapX()`, `enterXText(String)`, etc.
5. Use the new methods in tests via `pages.<page>()` or cached fields from `@BeforeMethod`.

### 10) Where to look in this repo

- Base utilities: `com.cube.qa.framework.pages.BasePage`
- Page factory: `com.cube.qa.framework.utils.PageFactory`
- Examples: `com.cube.qa.framework.pages.info.InfoPage`, `com.cube.qa.framework.pages.locations.LocationsPage`, `com.cube.qa.framework.pages.locations.LocationDetailsPage`
- Test usage: `src/test/java/tests/InfoTests.java`, `src/test/java/tests/LocationsTabTests.java`, `src/test/java/tests/OnboardingTest.java`


