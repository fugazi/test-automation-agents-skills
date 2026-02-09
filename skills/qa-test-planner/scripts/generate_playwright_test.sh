#!/bin/bash

# Playwright Automated Test Generator
# Interactive workflow for creating Playwright .spec.ts test files

set -e

# Colors
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
MAGENTA='\033[0;35m'
CYAN='\033[0;36m'
NC='\033[0m'

echo -e "${BLUE}╔══════════════════════════════════════════════════╗${NC}"
echo -e "${BLUE}║       Playwright Automated Test Generator        ║${NC}"
echo -e "${BLUE}╚══════════════════════════════════════════════════╝${NC}"
echo ""

# Helper functions
prompt_input() {
    local prompt_text="$1"
    local var_name="$2"
    local required="$3"
    
    while true; do
        echo -e "${CYAN}${prompt_text}${NC}"
        read -r input
        
        if [ -n "$input" ]; then
            eval "$var_name=\"$input\""
            break
        elif [ "$required" != "true" ]; then
            eval "$var_name=\"\""
            break
        else
            echo -e "${RED}This field is required.${NC}"
        fi
    done
}

# Step 1: Basic Info
echo -e "${MAGENTA}━━━ Step 1: Test File Basics ━━━${NC}"
echo ""

prompt_input "Feature name (e.g., login, checkout, search):" FEATURE_NAME true
prompt_input "Test file name (without .spec.ts):" FILE_NAME true
prompt_input "Base URL (e.g., http://localhost:3000):" BASE_URL true

echo ""
echo "Test category:"
echo "1) Functional (user workflows)"
echo "2) UI/Visual (component validation)"
echo "3) Form validation"
echo "4) Navigation"
echo "5) API integration"
echo "6) Accessibility"
echo ""

prompt_input "Select category (1-6):" CATEGORY_NUM true

case $CATEGORY_NUM in
    1) CATEGORY="Functional" ;;
    2) CATEGORY="UI/Visual" ;;
    3) CATEGORY="Form Validation" ;;
    4) CATEGORY="Navigation" ;;
    5) CATEGORY="API Integration" ;;
    6) CATEGORY="Accessibility" ;;
    *) CATEGORY="Functional" ;;
esac

# Step 2: Test describe block
echo ""
echo -e "${MAGENTA}━━━ Step 2: Test Suite Info ━━━${NC}"
echo ""

prompt_input "Describe block title (e.g., Login Page):" DESCRIBE_TITLE true
prompt_input "Initial page to navigate (path after base URL, e.g., /login):" INITIAL_PAGE true

# Step 3: beforeEach setup
echo ""
echo -e "${MAGENTA}━━━ Step 3: Setup (beforeEach) ━━━${NC}"
echo ""

echo "Default setup: Navigate to ${BASE_URL}${INITIAL_PAGE}"
prompt_input "Additional setup steps (or press Enter to skip):" ADDITIONAL_SETUP false

# Step 4: Test cases
echo ""
echo -e "${MAGENTA}━━━ Step 4: Test Cases ━━━${NC}"
echo ""

echo "Define your test cases (type 'done' when finished)"
echo ""

TEST_CASES=""
TC_NUM=1

while true; do
    echo -e "${YELLOW}━━━ Test Case $TC_NUM ━━━${NC}"
    prompt_input "Test title (e.g., 'displays login form'):" TC_TITLE false
    
    if [ "$TC_TITLE" = "done" ] || [ -z "$TC_TITLE" ]; then
        break
    fi
    
    prompt_input "Test objective (what are we verifying?):" TC_OBJECTIVE true
    
    echo ""
    echo "Define test steps (format: step description | action type)"
    echo "Action types: click, fill, navigate, verify, screenshot"
    echo "Type 'next' to move to next test case"
    echo ""
    
    STEPS=""
    STEP_NUM=1
    
    while true; do
        prompt_input "Step $STEP_NUM description (or 'next'):" STEP_DESC false
        
        if [ "$STEP_DESC" = "next" ] || [ -z "$STEP_DESC" ]; then
            break
        fi
        
        echo "Action type for this step:"
        echo "1) click - Click an element"
        echo "2) fill - Fill a form field"
        echo "3) verify - Assert element state"
        echo "4) navigate - Navigate to URL"
        echo "5) screenshot - Capture screenshot"
        echo "6) custom - Custom action"
        echo ""
        
        prompt_input "Action type (1-6):" ACTION_TYPE true
        
        case $ACTION_TYPE in
            1) 
                prompt_input "Element (role and name, e.g., 'button Submit'):" ELEMENT true
                STEPS="${STEPS}      await page.getByRole('$(echo $ELEMENT | cut -d' ' -f1)', { name: '$(echo $ELEMENT | cut -d' ' -f2-)' }).click();\n"
                ;;
            2)
                prompt_input "Element (role and name, e.g., 'textbox Email'):" ELEMENT true
                prompt_input "Value to fill:" FILL_VALUE true
                STEPS="${STEPS}      await page.getByRole('$(echo $ELEMENT | cut -d' ' -f1)', { name: '$(echo $ELEMENT | cut -d' ' -f2-)' }).fill('$FILL_VALUE');\n"
                ;;
            3)
                prompt_input "Element (role and name, e.g., 'heading Welcome'):" ELEMENT true
                echo "Assertion type:"
                echo "1) toBeVisible"
                echo "2) toHaveText"
                echo "3) toBeEnabled"
                echo "4) toBeDisabled"
                echo "5) toHaveURL"
                echo ""
                prompt_input "Assertion (1-5):" ASSERTION_TYPE true
                
                case $ASSERTION_TYPE in
                    1) STEPS="${STEPS}      await expect(page.getByRole('$(echo $ELEMENT | cut -d' ' -f1)', { name: '$(echo $ELEMENT | cut -d' ' -f2-)' })).toBeVisible();\n" ;;
                    2) 
                        prompt_input "Expected text:" EXPECTED_TEXT true
                        STEPS="${STEPS}      await expect(page.getByRole('$(echo $ELEMENT | cut -d' ' -f1)', { name: '$(echo $ELEMENT | cut -d' ' -f2-)' })).toHaveText('$EXPECTED_TEXT');\n" 
                        ;;
                    3) STEPS="${STEPS}      await expect(page.getByRole('$(echo $ELEMENT | cut -d' ' -f1)', { name: '$(echo $ELEMENT | cut -d' ' -f2-)' })).toBeEnabled();\n" ;;
                    4) STEPS="${STEPS}      await expect(page.getByRole('$(echo $ELEMENT | cut -d' ' -f1)', { name: '$(echo $ELEMENT | cut -d' ' -f2-)' })).toBeDisabled();\n" ;;
                    5) 
                        prompt_input "Expected URL pattern:" URL_PATTERN true
                        STEPS="${STEPS}      await expect(page).toHaveURL(/$URL_PATTERN/);\n" 
                        ;;
                esac
                ;;
            4)
                prompt_input "URL path to navigate:" NAV_URL true
                STEPS="${STEPS}      await page.goto('$NAV_URL');\n"
                ;;
            5)
                prompt_input "Screenshot name:" SCREENSHOT_NAME true
                STEPS="${STEPS}      await page.screenshot({ path: '$SCREENSHOT_NAME.png', fullPage: true });\n"
                ;;
            6)
                prompt_input "Custom Playwright code:" CUSTOM_CODE true
                STEPS="${STEPS}      $CUSTOM_CODE\n"
                ;;
        esac
        
        ((STEP_NUM++))
    done
    
    # Build test case
    TEST_CASES="${TEST_CASES}
  test('$TC_TITLE', async ({ page }) => {
    // $TC_OBJECTIVE
    await test.step('Execute test steps', async () => {
$(echo -e "$STEPS")
    });
  });
"
    
    ((TC_NUM++))
done

# Step 5: Output directory
echo ""
echo -e "${MAGENTA}━━━ Step 5: Output Location ━━━${NC}"
echo ""

OUTPUT_DIR="./tests"
if [ ! -z "$1" ]; then
    OUTPUT_DIR="$1"
fi

prompt_input "Output directory [$OUTPUT_DIR]:" CUSTOM_DIR false
if [ -n "$CUSTOM_DIR" ]; then
    OUTPUT_DIR="$CUSTOM_DIR"
fi

# Create output directory if needed
mkdir -p "$OUTPUT_DIR"

OUTPUT_FILE="$OUTPUT_DIR/${FILE_NAME}.spec.ts"

# Generate test file
echo ""
echo -e "${BLUE}Generating Playwright test file...${NC}"
echo ""

cat > "$OUTPUT_FILE" << EOF
import { test, expect } from '@playwright/test';

/**
 * ${DESCRIBE_TITLE} - Automated Tests
 * Category: ${CATEGORY}
 * Feature: ${FEATURE_NAME}
 * Generated: $(date +%Y-%m-%d)
 */

test.describe('${DESCRIBE_TITLE}', () => {
  test.beforeEach(async ({ page }) => {
    // Navigate to the page before each test
    await page.goto('${BASE_URL}${INITIAL_PAGE}');
${ADDITIONAL_SETUP:+    // Additional setup\n    $ADDITIONAL_SETUP}
  });
${TEST_CASES}
});
EOF

echo -e "${GREEN}✅ Playwright test file generated successfully!${NC}"
echo ""
echo -e "File location: ${BLUE}$OUTPUT_FILE${NC}"
echo ""
echo -e "${YELLOW}Next steps:${NC}"
echo "1. Review generated test file"
echo "2. Refine locators using Playwright MCP validation"
echo "3. Run tests: npx playwright test ${FILE_NAME}.spec.ts"
echo "4. Debug if needed: npx playwright test --debug ${FILE_NAME}.spec.ts"
echo "5. View report: npx playwright show-report"
echo ""
echo -e "${CYAN}Playwright MCP Validation Commands:${NC}"
echo "  \"Navigate to ${BASE_URL}${INITIAL_PAGE}\""
echo "  \"Get the accessibility snapshot\""
echo "  \"Take a screenshot\""
echo ""
echo -e "${MAGENTA}Tip: Use Page Object Model for complex pages${NC}"
echo ""
