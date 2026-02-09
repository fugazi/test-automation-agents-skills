---
name: webapp-selenium-testing
description: Browser automation toolkit using Selenium WebDriver 4+ with Java 21+ and JUnit 5. Use when asked to create, debug, or run Selenium tests, implement Page Object Model, handle explicit waits with WebDriverWait, capture screenshots, verify UI elements with AssertJ assertions, test forms, validate user flows, or set up Maven test projects. Supports Chrome, Firefox, and Edge browsers.
---

# Web Application Testing with Selenium WebDriver & Java

This skill enables comprehensive browser-based test automation for web applications using Selenium WebDriver within a Java/Maven environment. It provides patterns for Page Object Model, explicit waits, fluent assertions, and CI-ready test infrastructure.

> **Activation:** This skill is triggered when you need to create Selenium tests, debug browser automation, implement Page Objects, or set up Java test infrastructure.

## When to Use This Skill

Use this skill when you need to:

- Create Selenium WebDriver tests using Java (JUnit 5)
- Implement Page Object Model (POM) architecture
- Handle synchronization with Explicit Waits (`WebDriverWait`)
- Verify UI behavior with AssertJ Soft Assertions
- Debug failing browser tests or DOM interactions
- Set up Maven test infrastructure for a new project
- Capture screenshots for reporting or debugging
- Validate complex user flows and form submissions
- Test across multiple browsers (Chrome, Firefox, Edge)
- Integrate with Allure reporting

## Prerequisites

| Component | Version | Purpose |
|-----------|---------|---------|
| Java JDK | 21+ | Runtime with modern features (Records, Pattern Matching) |
| Maven | 3.9+ | Dependency management and build |
| Selenium WebDriver | 4.x | Browser automation (includes Selenium Manager) |
| JUnit 5 | 5.10+ | Test framework |
| AssertJ | 3.x | Fluent assertions with Soft Assertions |
| Lombok | 1.18+ | Boilerplate reduction (@Slf4j, @Builder) |

> **Note:** Selenium Manager (included in Selenium 4.6+) automatically handles browser driver binaries - no manual driver setup required.

---

## Selenium WebDriver Tools Reference

### Navigation & Browser Control

| Method | Purpose | Example |
|--------|---------|---------|
| `driver.get(url)` | Navigate to URL | `driver.get("http://localhost:3000")` |
| `driver.navigate().to(url)` | Navigate with history | `driver.navigate().to(url)` |
| `driver.navigate().back()` | Go back in history | Browser back button |
| `driver.navigate().refresh()` | Refresh page | Reload current page |
| `driver.switchTo().window(handle)` | Switch tabs/windows | Multi-window handling |
| `driver.switchTo().frame(element)` | Switch to iframe | Iframe interactions |
| `driver.switchTo().alert()` | Handle alerts | Accept/dismiss dialogs |

### Element Interaction

| Method | Purpose | Example |
|--------|---------|---------|
| `element.click()` | Click element | Button, link clicks |
| `element.sendKeys(text)` | Enter text | Input fields |
| `element.clear()` | Clear field | Clear before typing |
| `new Select(element)` | Dropdown handling | Select by value/text |
| `new Actions(driver)` | Complex actions | Drag-drop, hover, right-click |

### Verification

| Method | Purpose | Example |
|--------|---------|---------|
| `element.isDisplayed()` | Check visibility | Verify element shown |
| `element.isEnabled()` | Check enabled state | Verify button clickable |
| `element.getText()` | Get text content | Read element text |
| `element.getAttribute(name)` | Get attribute | Read href, class, etc. |
| `driver.getTitle()` | Get page title | Verify page loaded |
| `driver.getCurrentUrl()` | Get current URL | Verify navigation |

### Screenshots & Debugging

| Method | Purpose | Example |
|--------|---------|---------|
| `TakesScreenshot` | Capture screenshot | Evidence on failure |
| `driver.getPageSource()` | Get HTML source | DOM analysis |
| `LogEntries` | Browser console logs | Debug JS errors |

---

## Core Capabilities

### 1. Browser Automation
- Navigate URLs, handle tabs/windows, manage history
- Click, type, clear, submit forms
- Handle dropdowns (`Select`), drag-drop, hover
- Switch frames/iframes, handle alerts
- Manage cookies

### 2. Verification (AssertJ)
- **Soft Assertions**: Validate multiple fields, report all failures
- Element state: `isDisplayed()`, `isEnabled()`, `isSelected()`
- Text content, attributes, URL, page title
- Collection assertions for lists/tables

### 3. Synchronization
- **Explicit Waits**: `WebDriverWait` + `ExpectedConditions`
- Wait for visibility, clickability, presence
- Custom wait conditions
- **Never use `Thread.sleep()`**

### 4. Reporting & Debugging
- Screenshots on failure (Allure integration)
- Browser console log capture
- Page source extraction
- `@Step` annotations for Allure reports

---

## Your Role

You coordinate the entire Selenium WebDriver test creation process:

1. **Analyze**: Understand test requirements and data needs
2. **Inspect**: Identify locator strategies (prioritize `id`, `data-testid`, `cssSelector`)
3. **Design**: Apply Page Object Model strictly
4. **Implement**: Generate test with error handling and logging
5. **Verify**: Ensure assertions use AssertJ fluent style

---

## Step-by-Step Workflows

### Workflow 1: Create New Selenium Test

1. **Analyze the requirement**
   - Identify the user flow to test
   - List elements to interact with
   - Define expected outcomes

2. **Set up Page Objects** (see [Page Object Model Guide](references/page_object_model.md))
   - Create BasePage with common methods
   - Create specific Page class with locators
   - Implement action methods with `@Step`

3. **Implement test class**
   - Extend BaseTest
   - Use `@DisplayName`, `@Tag`, `@Severity`
   - Use Soft Assertions for validations

4. **Run and validate**
   ```bash
   mvn test -Dtest=YourTest -Dheadless=false
   ```

### Workflow 2: Debug Failing Test

1. **Run in non-headless mode**
   ```bash
   mvn test -Dtest=FailingTest -Dheadless=false
   ```

2. **Add screenshot capture**
   ```java
   ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
   ```

3. **Check browser console logs**
   ```java
   driver.manage().logs().get(LogType.BROWSER);
   ```

4. **Verify locator** using browser DevTools
   ```javascript
   document.querySelector('[data-testid="element"]')
   ```

5. **Check wait conditions** - increase timeout or change ExpectedCondition

### Workflow 3: Set Up New Project

1. **Create Maven project structure**
   ```
   Run: scripts/setup-maven-project.ps1 -ProjectName "my-tests"
   ```

2. **Configure dependencies in pom.xml**
   - See [scripts/pom-template.xml](scripts/pom-template.xml)

3. **Create base classes**
   - BasePage, BaseTest, WebDriverFactory
   - See [Page Object Model Guide](references/page_object_model.md)

4. **Configure parallel execution**
   ```properties
   # src/test/resources/junit-platform.properties
   junit.jupiter.execution.parallel.enabled=true
   ```

---

## Troubleshooting

| Problem | Cause | Solution |
|---------|-------|----------|
| Element not found | Not loaded yet | Use `WebDriverWait` with `visibilityOfElementLocated` |
| Stale element reference | DOM changed | Re-locate element before interaction |
| Click intercepted | Overlay blocking | Scroll into view or wait for overlay to close |
| Timeout exception | Element never visible | Verify locator, check for iframes |
| Session not created | Driver/browser mismatch | Selenium Manager handles this automatically |
| Flaky tests | Race conditions | Add proper waits, use stable locators |

---

## Best Practices Checklist

✅ **Never use `Thread.sleep()`** - Use explicit waits with `WebDriverWait`
✅ **Implement Page Object Model** - Separate locators from test logic
✅ **Use Soft Assertions** - Report all failures in one test run
✅ **Prefer stable locators** - `id`, `data-testid`, semantic CSS
✅ **Add `@Step` annotations** - Document actions in Allure reports
✅ **Clean up resources** - Close driver in `@AfterEach`
✅ **Keep tests independent** - Each test runs in isolation
✅ **Use `@DisplayName`** - Human-readable test descriptions
✅ **Generate dynamic data** - Use `Faker` for test data

---

## Running Tests

### Maven Commands

| Command | Purpose |
|---------|---------|
| `mvn test` | Run all tests |
| `mvn test -Dtest=LoginTest` | Run specific class |
| `mvn test -Dtest=LoginTest#shouldLoginSuccessfully` | Run specific method |
| `mvn test -Dgroups=smoke` | Run tagged tests |
| `mvn test -Dheadless=true` | Run headless (CI) |
| `mvn allure:serve` | Generate and view Allure report |

### CI/CD Integration

```yaml
# GitHub Actions example
- name: Run Selenium Tests
  run: mvn clean test -Dheadless=true -Dbrowser=chrome
  
- name: Generate Allure Report
  run: mvn allure:report
```

---

## References

- [Locator Strategies Guide](references/locator_strategies.md) - Selector priority and patterns
- [Page Object Model Guide](references/page_object_model.md) - POM implementation with Java
- [Wait Strategies Guide](references/wait_strategies.md) - Explicit waits and ExpectedConditions
- [Maven POM Template](scripts/pom-template.xml) - Standard dependency configuration
- [Project Setup Script](scripts/setup-maven-project.ps1) - Scaffold new test project

---

## Quick Commands

| Task | Command/Pattern |
|------|-----------------|
| Find by ID | `By.id("elementId")` |
| Find by test ID | `By.cssSelector("[data-testid='name']")` |
| Wait for visible | `wait.until(ExpectedConditions.visibilityOfElementLocated(by))` |
| Click safely | `wait.until(ExpectedConditions.elementToBeClickable(by)).click()` |
| Soft assert | `SoftAssertions.assertSoftly(s -> { ... })` |
| Take screenshot | `((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE)` |
| Run test | `mvn test -Dtest=ClassName` |
