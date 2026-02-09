package com.example.utils;

import com.deque.html.axecore.results.CheckedNode;
import com.deque.html.axecore.results.Results;
import com.deque.html.axecore.results.Rule;
import com.deque.html.axecore.selenium.AxeBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.qameta.allure.Allure;
import io.qameta.allure.Step;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.WebDriver;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Accessibility testing helper using axe-core.
 * Provides methods for WCAG 2.1 AA compliance checking.
 * 
 * Usage:
 *   AccessibilityHelper.verifyPageAccessibility(driver);
 *   AccessibilityHelper.verifyComponentAccessibility(driver, "#modal");
 *   AccessibilityHelper.verifyCriticalAccessibility(driver);
 */
@Slf4j
public class AccessibilityHelper {

    // WCAG 2.1 AA tags (recommended default)
    private static final List<String> WCAG_21_AA_TAGS = List.of(
        "wcag2a", "wcag2aa", "wcag21a", "wcag21aa"
    );

    // Add best-practice for additional coverage
    private static final List<String> WCAG_WITH_BEST_PRACTICE = List.of(
        "wcag2a", "wcag2aa", "wcag21a", "wcag21aa", "best-practice"
    );

    // Critical and Serious impacts that should fail CI
    private static final List<String> BLOCKING_IMPACTS = List.of("critical", "serious");

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private AccessibilityHelper() {
        // Static utility class
    }

    // =====================================================
    // Full Page Scans
    // =====================================================

    /**
     * Scan entire page for WCAG 2.1 AA violations.
     * Fails if ANY violations are found.
     * 
     * @param driver WebDriver instance with page loaded
     */
    @Step("Verify page accessibility - WCAG 2.1 AA (strict)")
    public static void verifyPageAccessibility(WebDriver driver) {
        Results results = new AxeBuilder()
            .withTags(WCAG_21_AA_TAGS)
            .analyze(driver);

        logViolations(results.getViolations());
        attachResultsToAllure(results);

        assertThat(results.violationFree())
            .as("Accessibility violations found on: %s", driver.getCurrentUrl())
            .isTrue();
    }

    /**
     * Scan page with best-practice rules included.
     * More comprehensive but may have more false positives.
     * 
     * @param driver WebDriver instance with page loaded
     */
    @Step("Verify page accessibility - WCAG 2.1 AA + Best Practice")
    public static void verifyPageAccessibilityWithBestPractice(WebDriver driver) {
        Results results = new AxeBuilder()
            .withTags(WCAG_WITH_BEST_PRACTICE)
            .analyze(driver);

        logViolations(results.getViolations());
        attachResultsToAllure(results);

        assertThat(results.violationFree())
            .as("Accessibility violations found on: %s", driver.getCurrentUrl())
            .isTrue();
    }

    /**
     * Scan page but only fail on Critical and Serious violations.
     * Logs Moderate and Minor for awareness.
     * 
     * @param driver WebDriver instance with page loaded
     */
    @Step("Verify page accessibility - Critical/Serious only")
    public static void verifyCriticalAccessibility(WebDriver driver) {
        Results results = new AxeBuilder()
            .withTags(WCAG_21_AA_TAGS)
            .analyze(driver);

        List<Rule> criticalViolations = filterByImpact(results.getViolations(), BLOCKING_IMPACTS);
        List<Rule> otherViolations = filterExcludingImpact(results.getViolations(), BLOCKING_IMPACTS);

        // Log all violations
        if (!criticalViolations.isEmpty()) {
            log.error("BLOCKING VIOLATIONS:");
            logViolations(criticalViolations);
        }
        if (!otherViolations.isEmpty()) {
            log.warn("NON-BLOCKING VIOLATIONS (review recommended):");
            logViolations(otherViolations);
        }

        attachResultsToAllure(results);

        // Only fail on critical/serious
        assertThat(criticalViolations)
            .as("Critical/Serious accessibility violations found on: %s", driver.getCurrentUrl())
            .isEmpty();
    }

    // =====================================================
    // Component Scans
    // =====================================================

    /**
     * Scan specific component(s) by CSS selector.
     * Use for modals, forms, or isolated widgets.
     * 
     * @param driver WebDriver instance with page loaded
     * @param selectors CSS selectors to include in scan
     */
    @Step("Verify component accessibility: {selectors}")
    public static void verifyComponentAccessibility(WebDriver driver, String... selectors) {
        AxeBuilder builder = new AxeBuilder()
            .withTags(WCAG_21_AA_TAGS);

        for (String selector : selectors) {
            builder.include(selector);
        }

        Results results = builder.analyze(driver);
        logViolations(results.getViolations());
        attachResultsToAllure(results);

        assertThat(results.violationFree())
            .as("Component accessibility violations found for: %s", String.join(", ", selectors))
            .isTrue();
    }

    /**
     * Scan page excluding specific selectors.
     * Use for third-party widgets or known exceptions.
     * Always document the reason for exclusions.
     * 
     * @param driver WebDriver instance with page loaded
     * @param exclusions CSS selectors to exclude from scan
     */
    @Step("Verify accessibility with exclusions")
    public static void verifyWithExclusions(WebDriver driver, List<String> exclusions) {
        AxeBuilder builder = new AxeBuilder()
            .withTags(WCAG_21_AA_TAGS);

        for (String exclusion : exclusions) {
            builder.exclude(exclusion);
            log.warn("EXCLUDING from a11y scan: {} (ensure this is documented)", exclusion);
        }

        Results results = builder.analyze(driver);
        logViolations(results.getViolations());
        attachResultsToAllure(results);

        assertThat(results.violationFree())
            .as("Accessibility violations found on: %s", driver.getCurrentUrl())
            .isTrue();
    }

    // =====================================================
    // Rule-Specific Scans
    // =====================================================

    /**
     * Run only specific rules.
     * Use when testing a specific aspect (e.g., color contrast only).
     * 
     * @param driver WebDriver instance with page loaded
     * @param rules List of rule IDs to run
     */
    @Step("Verify specific rules: {rules}")
    public static void verifySpecificRules(WebDriver driver, List<String> rules) {
        Results results = new AxeBuilder()
            .withRules(rules)
            .analyze(driver);

        logViolations(results.getViolations());
        attachResultsToAllure(results);

        assertThat(results.violationFree())
            .as("Rule violations found: %s", String.join(", ", rules))
            .isTrue();
    }

    /**
     * Scan but disable specific rules.
     * Use carefully - document reason for each disabled rule.
     * 
     * @param driver WebDriver instance with page loaded
     * @param disabledRules List of rule IDs to disable
     */
    @Step("Verify accessibility (rules disabled: {disabledRules})")
    public static void verifyWithDisabledRules(WebDriver driver, List<String> disabledRules) {
        for (String rule : disabledRules) {
            log.warn("DISABLING rule: {} (ensure this is documented)", rule);
        }

        Results results = new AxeBuilder()
            .withTags(WCAG_21_AA_TAGS)
            .disableRules(disabledRules)
            .analyze(driver);

        logViolations(results.getViolations());
        attachResultsToAllure(results);

        assertThat(results.violationFree())
            .as("Accessibility violations found on: %s", driver.getCurrentUrl())
            .isTrue();
    }

    // =====================================================
    // Results Access (without assertion)
    // =====================================================

    /**
     * Get scan results without assertion.
     * Use when you need to process results manually.
     * 
     * @param driver WebDriver instance with page loaded
     * @return Axe Results object
     */
    public static Results getAccessibilityResults(WebDriver driver) {
        return new AxeBuilder()
            .withTags(WCAG_21_AA_TAGS)
            .analyze(driver);
    }

    /**
     * Get only violations (not all results).
     * 
     * @param driver WebDriver instance with page loaded
     * @return List of Rule violations
     */
    public static List<Rule> getViolations(WebDriver driver) {
        return getAccessibilityResults(driver).getViolations();
    }

    /**
     * Get incomplete/needs-review items.
     * 
     * @param driver WebDriver instance with page loaded
     * @return List of incomplete Rules requiring manual review
     */
    public static List<Rule> getIncomplete(WebDriver driver) {
        return getAccessibilityResults(driver).getIncomplete();
    }

    // =====================================================
    // Filtering Helpers
    // =====================================================

    /**
     * Filter violations by impact levels.
     * 
     * @param violations List of Rule violations
     * @param impacts List of impact levels to include (critical, serious, moderate, minor)
     * @return Filtered list
     */
    public static List<Rule> filterByImpact(List<Rule> violations, List<String> impacts) {
        return violations.stream()
            .filter(v -> impacts.contains(v.getImpact()))
            .toList();
    }

    /**
     * Filter violations excluding specified impact levels.
     * 
     * @param violations List of Rule violations
     * @param impacts List of impact levels to exclude
     * @return Filtered list
     */
    public static List<Rule> filterExcludingImpact(List<Rule> violations, List<String> impacts) {
        return violations.stream()
            .filter(v -> !impacts.contains(v.getImpact()))
            .toList();
    }

    /**
     * Filter violations by rule IDs.
     * 
     * @param violations List of Rule violations
     * @param ruleIds List of rule IDs to include
     * @return Filtered list
     */
    public static List<Rule> filterByRuleId(List<Rule> violations, List<String> ruleIds) {
        return violations.stream()
            .filter(v -> ruleIds.contains(v.getId()))
            .toList();
    }

    /**
     * Get count of violations by impact.
     * 
     * @param violations List of Rule violations
     * @param impact Impact level (critical, serious, moderate, minor)
     * @return Count of violations with given impact
     */
    public static long countByImpact(List<Rule> violations, String impact) {
        return violations.stream()
            .filter(v -> impact.equals(v.getImpact()))
            .count();
    }

    // =====================================================
    // Logging
    // =====================================================

    /**
     * Log violations in a human-readable format.
     * Includes Help URL for developer reference.
     * 
     * @param violations List of Rule violations to log
     */
    public static void logViolations(List<Rule> violations) {
        if (violations.isEmpty()) {
            log.info("✓ No accessibility violations found");
            return;
        }

        log.error("✗ Found {} accessibility violation(s):", violations.size());
        log.error("═══════════════════════════════════════════════════════════");

        for (Rule violation : violations) {
            log.error("");
            log.error("  [{} / {}]", violation.getImpact().toUpperCase(), violation.getId());
            log.error("  Description: {}", violation.getDescription());
            log.error("  Help: {}", violation.getHelpUrl());
            log.error("  WCAG Tags: {}", String.join(", ", violation.getTags()));

            for (CheckedNode node : violation.getNodes()) {
                log.error("    ├── Target: {}", String.join(" > ", node.getTarget()));
                log.error("    ├── HTML: {}", truncate(node.getHtml(), 120));
                if (node.getFailureSummary() != null) {
                    log.error("    └── Fix: {}", truncate(node.getFailureSummary(), 200));
                }
            }
        }
        log.error("═══════════════════════════════════════════════════════════");
    }

    /**
     * Generate summary statistics.
     * 
     * @param violations List of Rule violations
     */
    public static void logSummary(List<Rule> violations) {
        if (violations.isEmpty()) {
            log.info("Summary: 0 violations");
            return;
        }

        long critical = countByImpact(violations, "critical");
        long serious = countByImpact(violations, "serious");
        long moderate = countByImpact(violations, "moderate");
        long minor = countByImpact(violations, "minor");

        log.info("Summary: {} total violations", violations.size());
        log.info("  Critical: {}", critical);
        log.info("  Serious:  {}", serious);
        log.info("  Moderate: {}", moderate);
        log.info("  Minor:    {}", minor);
    }

    // =====================================================
    // Reporting
    // =====================================================

    /**
     * Attach axe results to Allure report as JSON.
     * 
     * @param results Axe Results object
     */
    public static void attachResultsToAllure(Results results) {
        try {
            String json = objectMapper.writerWithDefaultPrettyPrinter()
                .writeValueAsString(results);
            
            Allure.addAttachment(
                "Axe Accessibility Results",
                "application/json",
                new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)),
                "json"
            );
        } catch (Exception e) {
            log.warn("Failed to attach axe results to Allure", e);
        }
    }

    /**
     * Save results to JSON file.
     * Useful for external reporting tools.
     * 
     * @param results Axe Results object
     * @param outputPath Path to save JSON file
     */
    public static void saveResultsToFile(Results results, Path outputPath) {
        try {
            Files.createDirectories(outputPath.getParent());
            String json = objectMapper.writerWithDefaultPrettyPrinter()
                .writeValueAsString(results);
            Files.writeString(outputPath, json);
            log.info("Axe results saved to: {}", outputPath);
        } catch (Exception e) {
            log.error("Failed to save axe results to file", e);
        }
    }

    /**
     * Generate HTML report from results.
     * 
     * @param results Axe Results object
     * @param outputPath Path to save HTML file
     */
    public static void saveHtmlReport(Results results, Path outputPath) {
        try {
            StringBuilder html = new StringBuilder();
            html.append("<!DOCTYPE html>\n<html lang=\"en\"><head>\n");
            html.append("<meta charset=\"UTF-8\">\n");
            html.append("<title>Accessibility Report</title>\n");
            html.append("<style>\n");
            html.append("body { font-family: system-ui, -apple-system, sans-serif; max-width: 1200px; margin: 0 auto; padding: 20px; line-height: 1.6; }\n");
            html.append("h1 { color: #1a1a1a; border-bottom: 2px solid #0066cc; padding-bottom: 10px; }\n");
            html.append(".summary { background: #f5f5f5; padding: 16px; border-radius: 8px; margin-bottom: 24px; }\n");
            html.append(".critical { border-left: 4px solid #d32f2f; background: #ffebee; }\n");
            html.append(".serious { border-left: 4px solid #f57c00; background: #fff3e0; }\n");
            html.append(".moderate { border-left: 4px solid #fbc02d; background: #fffde7; }\n");
            html.append(".minor { border-left: 4px solid #1976d2; background: #e3f2fd; }\n");
            html.append(".violation { margin: 16px 0; padding: 16px; border-radius: 4px; }\n");
            html.append(".violation h3 { margin: 0 0 8px 0; }\n");
            html.append("code { background: #e8e8e8; padding: 2px 6px; border-radius: 3px; font-size: 0.9em; }\n");
            html.append("a { color: #0066cc; }\n");
            html.append(".pass { color: #2e7d32; }\n");
            html.append("</style>\n</head><body>\n");
            
            html.append("<h1>Accessibility Report</h1>\n");
            html.append("<div class=\"summary\">\n");
            html.append("<p><strong>URL:</strong> ").append(results.getUrl()).append("</p>\n");
            html.append("<p><strong>Timestamp:</strong> ").append(results.getTimestamp()).append("</p>\n");
            html.append("<p><strong>Violations:</strong> ").append(results.getViolations().size()).append("</p>\n");
            html.append("<p><strong>Passes:</strong> <span class=\"pass\">").append(results.getPasses().size()).append("</span></p>\n");
            html.append("</div>\n");

            if (results.getViolations().isEmpty()) {
                html.append("<p class=\"pass\">&#10003; No accessibility violations found!</p>\n");
            } else {
                html.append("<h2>Violations</h2>\n");
                for (Rule violation : results.getViolations()) {
                    html.append("<div class=\"violation ").append(violation.getImpact()).append("\">\n");
                    html.append("<h3>").append(escapeHtml(violation.getId())).append(" <small>(").append(violation.getImpact()).append(")</small></h3>\n");
                    html.append("<p>").append(escapeHtml(violation.getDescription())).append("</p>\n");
                    html.append("<p><a href=\"").append(violation.getHelpUrl()).append("\" target=\"_blank\">How to fix &rarr;</a></p>\n");
                    
                    html.append("<details><summary>Affected elements (").append(violation.getNodes().size()).append(")</summary>\n");
                    html.append("<ul>\n");
                    for (CheckedNode node : violation.getNodes()) {
                        html.append("<li><code>").append(escapeHtml(String.join(" > ", node.getTarget()))).append("</code></li>\n");
                    }
                    html.append("</ul></details>\n");
                    html.append("</div>\n");
                }
            }

            html.append("</body></html>");

            Files.createDirectories(outputPath.getParent());
            Files.writeString(outputPath, html.toString());
            log.info("HTML report saved to: {}", outputPath);
        } catch (Exception e) {
            log.error("Failed to save HTML report", e);
        }
    }

    // =====================================================
    // Utilities
    // =====================================================

    private static String truncate(String text, int maxLength) {
        if (text == null) return "";
        text = text.replaceAll("\\s+", " ").trim();
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength - 3) + "...";
    }

    private static String escapeHtml(String text) {
        if (text == null) return "";
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;");
    }
}
