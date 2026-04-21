# Test Authoring Workflow

Repeatable steps for adding a Testiny test suite to this project. Assumes the four onboarding/tooltip patterns already in `src/main/java/com/cube/qa/framework/pages`.

## 1. Read the spec
- Open the Testiny CSV export. Note test IDs, preconditions, steps, expected results.
- Identify shared UI surfaces (e.g. bottom tabs, date pickers) — those belong in their own POM, not the feature-specific one.

## 2. Branch
```
git checkout -b <feature>-tests
```

## 3. Scaffold POMs
Create one page class per logical screen under the appropriate package (`onboarding/`, `home/`, …).

Rules:
- **Platform-aware locators** — constructor takes `platform`, builds separate `List<By>` for iOS vs Android.
- **Locator lists, not singletons** — always `List.of(...)` so `BasePage` can try multiple strategies.
- **Shared surfaces get their own POM.** If another suite will touch it (tabs, alerts, pickers), extract it. Use an `enum` for stable keys (see `TabPage.Tab`).
- **Two flavors of check:**
  - `is<Thing>Visible()` — throws after 30s (use for preconditions/asserts that must wait).
  - `is<Thing>Present()` — `driver.findElements().isEmpty()` (instant, non-throwing; use for loop conditions).
- **Best-effort interactions** — when an overlay may block a tap (e.g. tooltip), provide `attempt<X>()` that swallows exceptions.
- Register each new page in `PageFactory`.

## 4. Write the test class
- Extend `BaseTest`. Use `pages.<yourPage>()`.
- `@BeforeMethod(dependsOnMethods = "setUp")` walks onboarding to the screen under test. Branch on `isIOS()` vs `isAndroid()` where the flow differs (e.g. iOS has `CONTINUE` + native alert; Android has `SKIP`).
- Method naming: `TC####` verbatim from CSV; `@Test(description = "TC#### - <CSV title>")`.
- One `Assert.*` per expected result in the CSV. Include a human-readable message.
- End with `log("✅ TC####: …")`.

## 5. Wire into test suites
Edit `testng-android.xml` and `testng-ios.xml`:
- Add `<class name="tests.<YourTest>"/>`.
- Comment out unrelated suites while iterating so cycles stay fast.

## 6. Compile before running
```
mvn -q test-compile
```
Cheaper than discovering a typo mid-device-run.

## 7. Iterate on staging first
```
mvn test -DsuiteXmlFile=testng-android.xml > /tmp/run.log 2>&1
```
- **Write full logs to a file** (not `| tail`) so you can grep for diagnostic output after the fact.
- On failure, grep for `Runtime .* not visible/clickable` — the locator name in the error points straight at the field to fix.

### Common fixes we hit
- **Alert blocks tap** → use `driver.executeScript("mobile: alert", Map.of("action","accept","buttonLabel","Allow While Using App"))` instead of `driver.switchTo().alert().accept()` (which hits the first tappable element — e.g. the "Precise" toggle).
- **`isVisible()` throws inside a loop** → switch to the non-throwing `isPresent()` variant.
- **Apostrophe in Android XPath** (`Don't allow`) → match by resource-id or `contains(@text,'Don') and contains(@text,'allow')`.
- **Element hidden behind overlay** → dismiss the overlay first, then assert on the underlying element.

## 8. Capture dynamic content before asserting on it
For text you don't already know (tooltip copy, dynamic labels):
1. First pass: scrape visible text into a list, `log()` each one.
2. Read actual strings from the log.
3. Second pass: pin exact-substring assertions on the discovered copy.

Don't invent strings from training data — capture them from the running app.

## 9. Stability gate
A test counts as done only when it passes **twice per environment**:

| Env | Platform | Run 1 | Run 2 |
|-----|----------|-------|-------|
| staging | Android | ✅ | ✅ |
| staging | iOS | ✅ | ✅ |
| prod | Android | ✅ | ✅ |
| prod | iOS | ✅ | ✅ |

Swap builds by editing the `<parameter name="build">` and `buildNumber` values in each testng XML (or `sed` inline). Keep `fullReset=true` for clean-state runs.

## 10. Before commit
- Re-enable any classes you commented out in the testng XMLs.
- Reset build paths to whatever the repo's default env is.
- `mvn -q test-compile` once more.
