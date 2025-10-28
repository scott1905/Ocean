package rt.scripts.playwright;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.Status;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.FrameLocator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Tracing;
import com.microsoft.playwright.options.SelectOption;
import org.testng.Assert;
import org.testng.annotations.*;

import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import com.microsoft.playwright.options.AriaRole;

public class OccenPlaywrightTest {

    private Playwright playwright;
    private Browser browser;
    private Page page;
    private PlaywrightHelper helper;
    private ExtentReports extent;
    private ExtentTest test;

    @BeforeClass
    public void setUp() {
        playwright = Playwright.create();
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(false));
        page = browser.newPage();
        page.setDefaultTimeout(60000);
        helper = new PlaywrightHelper(page);
        // Start tracing (enable sources only if PLAYWRIGHT_JAVA_SRC is set)
        String srcRoot = System.getenv("PLAYWRIGHT_JAVA_SRC");
        Tracing.StartOptions startOpts = new Tracing.StartOptions()
            .setScreenshots(true)
            .setSnapshots(true);
        if (srcRoot != null && !srcRoot.isEmpty()) {
            startOpts.setSources(true);
        }
        page.context().tracing().start(startOpts);
        ExtentSparkReporter reporter = new ExtentSparkReporter("ExtentReport.html");
        extent = new ExtentReports();
        extent.attachReporter(reporter);
        extent.setSystemInfo("OS", System.getProperty("os.name"));
        extent.setSystemInfo("User", System.getProperty("user.name"));
        extent.setSystemInfo("Browser", "Chromium (Playwright)");
        test = extent.createTest("Occen Playwright Test Suite");

        // Login flow (converted from Selenium @BeforeClass)
        test.log(Status.INFO, "Running Login Test...");
        page.navigate("https://otmgtm-dev2-otmsaasna.otmgtm.us-phoenix-1.ocs.oraclecloud.com");
        helper.waitForLoadState();

        // Click Sign In and detect if a new auth window opens
        List<Page> before = page.context().pages();
        page.locator("xpath=/html/body/div/oj-idaas-signin-app-shell-rw/div[1]/div/div/div/div[2]/div/div/oj-module/div[1]/oj-module/div[1]/div/div[3]/div[1]/div/oj-button").click();
        Page authPage = page;
        for (int i = 0; i < 20; i++) { // wait up to ~10s for new page
            List<Page> now = page.context().pages();
            if (now.size() > before.size()) {
                authPage = now.get(now.size() - 1);
                break;
            }
            helper.waitForTimeoutMillis(500);
        }
        authPage.waitForLoadState();
        try {
            authPage.waitForURL(url -> url.contains("login") || url.contains("microsoftonline"), new Page.WaitForURLOptions().setTimeout(60000));
        } catch (Throwable ignored) { }

        // Username
        Locator username = authPage.locator("input[name='loginfmt']");
        username.waitFor();
        username.fill("scott.kumar@unilever.com");
        authPage.locator("#idSIButton9").click();

        // Password
        Locator password = authPage.locator("#i0118");
        password.waitFor();
        password.fill("Invent@appwrk11");
        authPage.locator("#idSIButton9").click();

        // Home button validation in main page
        Locator homeButton = page.locator("#homeButton");
        homeButton.waitFor();
        homeButton.click();
        Assert.assertTrue(homeButton.isVisible(), "Login failed or home button not found.");
        test.log(Status.PASS, "Login successful.");
    }

    @Test(priority = 1)
    public void orderManagement() {
        test.log(Status.INFO, "Order_Managment test started");
        helper.waitForLoadState();
        // Find #label3 within mainIFrame, any iframe, or default content and scope subsequent actions accordingly
        FrameLocator scope = null;
        Locator label3 = page.frameLocator("iframe#mainIFrame").locator("#label3");
        if (!Boolean.TRUE.equals(label3.first().isVisible())) {
            int iframeCount = page.locator("iframe").count();
            if (iframeCount > 0) {
                scope = page.frameLocator("iframe").first();
                label3 = scope.locator("#label3");
            } else {
                scope = null;
                label3 = page.locator("#label3");
            }
        } else {
            scope = page.frameLocator("iframe#mainIFrame");
        }

        label3.first().waitFor();
        label3.first().click();
        helper.waitForTimeoutMillis(6000);

        Locator disclosure = (scope != null)
            ? scope.locator("xpath=//li[@id='sb_2_2']//ins[contains(@class,'oj-treeview-disclosure-icon')]")
            : page.locator("xpath=//li[@id='sb_2_2']//ins[contains(@class,'oj-treeview-disclosure-icon')]");
        disclosure.click();

        helper.waitForTimeoutMillis(5000);

        Locator orderRelease = (scope != null)
            ? scope.locator("xpath=//li[@id='sb_2_2_2']//span[text()='Order Release']")
            : page.locator("xpath=//li[@id='sb_2_2_2']//span[text()='Order Release']");
        orderRelease.click();
        test.log(Status.PASS, "Clicked Order Release child");
    }

    @Test(priority = 2, dependsOnMethods = "orderManagement")
    public void inOrderRelease() {
        test.log(Status.INFO, "In_order_release test started");
        // Wait for main iframe to attach/appear
        page.locator("iframe#mainIFrame").waitFor();

        // Anchor by the 'Indicator' cell, then grab the control in the next cell (works for table-based forms)
        FrameLocator mainFrame = page.frameLocator("iframe#mainIFrame");
        Locator indicatorCell = mainFrame.locator("xpath=//td[normalize-space()='Indicator']").first();
        if (!Boolean.TRUE.equals(indicatorCell.isVisible())) {
            // Fallback if label is wrapped inside <label>
            indicatorCell = mainFrame.locator("xpath=//td[.//label[normalize-space()='Indicator']]").first();
        }
        indicatorCell.waitFor();
        // Control is typically in the following sibling cell
        Locator indicatorControl = indicatorCell.locator("xpath=following::td[1]//select | following::td[1]//*[@role='combobox']").first();
        indicatorControl.waitFor();
        indicatorControl.scrollIntoViewIfNeeded();
        indicatorControl.click();
        // Try native select first
        try {
            indicatorControl.locator("xpath=./option[2]").click();
        } catch (Throwable ignore) {
            // If ARIA combobox, use keys
            try { indicatorControl.press("ArrowDown"); indicatorControl.press("Enter"); } catch (Throwable ignored) { }
        }

        // Click Search button with similar retry
        String searchXpath = "//oj-button[.//span[normalize-space()='Search']]//button | //button[@aria-label='Search' or @id='search' or @id='search_button' or normalize-space()='Search']";
        boolean searchClicked = false;
        for (int attempt = 0; attempt < 5 && !searchClicked; attempt++) {
            try {
                Locator searchBtn = mainFrame.locator("xpath=" + searchXpath).first();
                searchBtn.waitFor(new Locator.WaitForOptions().setTimeout(5000));
                searchBtn.scrollIntoViewIfNeeded();
                searchBtn.click();
                searchClicked = true;
            } catch (Throwable t) {
                helper.waitForTimeoutMillis(1000);
            }
        }
        if (!searchClicked) {
            Locator fallback = page.locator("xpath=" + searchXpath).first();
            fallback.waitFor(new Locator.WaitForOptions().setTimeout(5000));
            fallback.click();
        }
        test.log(Status.PASS, "Search button clicked successfully.");
    }

    @Test(priority = 3, dependsOnMethods = "inOrderRelease")
    public void orderReleases() {
        test.log(Status.INFO, "Order_Releases test started");
        FrameLocator frame0 = page.frameLocator("iframe").first();
        frame0.locator("table").waitFor();
        Locator link = frame0.locator("xpath=//a[contains(text(),'ULA/NA')]");
        link.scrollIntoViewIfNeeded();
        link.click();
        frame0.locator("xpath=/html/body/div[1]/table/tbody/tr/td[2]/table/tbody/tr/td[3]/div/button").click();
        test.log(Status.PASS, "Clicked on NEW button in Order Releases page");
    }

    @Test(priority = 4, dependsOnMethods = "orderReleases")
    public void orderManager() {
        test.log(Status.INFO, "Order_Manager test started");
        Locator domain = page.locator("xpath=//select[@aria-label='Domain Name']");
        domain.selectOption(new SelectOption().setLabel("ULA/NA"));
        test.log(Status.PASS, "Selected Domain: ULA/NA");

        LocalDate today = LocalDate.now();
        String earlyPickup = today.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + " 00:00:00";
        String latePickup = today.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + " 23:59:00";
        String earlyDelivery = today.plusDays(10).format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + " 00:00:00";
        String lateDelivery = today.plusDays(10).format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + " 23:59:00";

        helper.typeAndTab(page.locator("xpath=//input[@aria-label='Early Pickup Date']"), earlyPickup);
        test.log(Status.PASS, "Entered Early Pickup Date: " + earlyPickup);

        helper.typeAndTab(page.locator("xpath=//input[@aria-label='Late Pickup Date']"), latePickup);
        test.log(Status.PASS, "Entered Late Pickup Date: " + latePickup);

        helper.typeAndTab(page.locator("xpath=//input[@aria-label='Early Delivery Date']"), earlyDelivery);
        test.log(Status.PASS, "Entered Early Delivery Date: " + earlyDelivery);

        helper.typeAndTab(page.locator("xpath=//input[@aria-label='Late Delivery Date']"), lateDelivery);
        test.log(Status.PASS, "Entered Late Delivery Date: " + lateDelivery);

        Locator orderConfig = page.locator("xpath=//select[@aria-label='Order Configuration']");
        orderConfig.selectOption("SHIP_UNITS");
        test.log(Status.PASS, "Selected Order Configuration: SHIP_UNITS");

        // Try source location by id then by aria-label
        Locator sourceById = page.locator("#order_release/source/xid");
        if (Boolean.TRUE.equals(sourceById.isVisible())) {
            sourceById.fill("USO4");
        } else {
            Locator sourceByLabel = page.locator("xpath=//input[@aria-label='Source Location ID']");
            sourceByLabel.fill("");
            sourceByLabel.type("USO4");
        }
    }

    @Test(priority = 5, dependsOnMethods = "orderManager")
    public void orderManagerDestination() {
        test.log(Status.INFO, "Order_Manager1 test started");
        Locator dest = page.locator("#order_release/destination/xid");
        if (!Boolean.TRUE.equals(dest.isVisible())) {
            dest = page.locator("xpath=//input[@aria-label='Destination Location ID']");
        }
        dest.fill("");
        dest.type("USC6");

        page.locator("xpath=/html/body/div[6]/table/tbody/tr/td[2]/div/button").click();
        helper.waitForTimeoutMillis(4000);
        page.locator("xpath=/html/body/form/div[3]/div/div[1]/table/tbody/tr[5]/td/table/tbody/tr/td/div/button").click();
        helper.waitForTimeoutMillis(4000);
        test.log(Status.PASS, "Destination Location entered and continued");
    }

    @Test(priority = 6, dependsOnMethods = "orderManagerDestination")
    public void orderManagerShipUnit() {
        test.log(Status.INFO, "Order_Manager2 test started");
        Locator thu = page.locator("xpath=//input[@aria-label='Transport Handling Unit']");
        helper.typeAndTab(thu, "BOX_000000000084172226");

        page.locator("xpath=//input[@aria-label='Transport Handling Unit Count']").fill("20");
        page.locator("xpath=//input[@aria-label='Total Gross Weight']").fill("20400");
        page.evaluate("window.scrollTo(0, document.body.scrollHeight)");
        Locator newLine = page.locator("xpath=//button[@class='enButton' and text()='New Line Item']");
        helper.clickWithScroll(newLine);
        test.log(Status.PASS, "Clicked New Line Item button in Order Manager");
    }

    @Test(priority = 7, dependsOnMethods = "orderManagerShipUnit")
    public void shipUnitLine() {
        test.log(Status.INFO, "Ship_Unit_Line test started");
        page.locator("xpath=//input[@aria-label='Packaged Item ID']").type("000000000020005492");
        page.locator("xpath=//input[@aria-label='Item ID']").type("000000000068441373");
        page.locator("xpath=//input[@aria-label='Total Package Count']").type("20");
        page.locator("xpath=//input[@aria-label='Packaging Unit']").type("BOX_000000000084172488");
        page.locator("xpath=//input[@aria-label='Gross Weight']").type("20400");
        page.locator("xpath=//input[@aria-label='Gross Volume']").type("2380");
        page.locator("xpath=//input[@aria-label='Special Service']").type("TRANSPORT");
        page.locator("xpath=//button[normalize-space()='Save']").click();
        page.locator("xpath=//button[contains(@onclick, 'checkPiForItem()') and text()='Save']").click();
        helper.waitForTimeoutMillis(6000);
        page.evaluate("window.scrollTo(0, document.body.scrollHeight)");
        page.locator("xpath=//button[contains(@onclick,'checkData') and text()='Save']").click();
        page.locator("xpath=//button[normalize-space(.)='Line Item']").click();
        test.log(Status.PASS, "Clicked Save button in Ship Unit Line");
    }

    @Test(priority = 8, dependsOnMethods = "shipUnitLine")
    public void shipUnitLineInvolvedParties() {
        test.log(Status.INFO, "Ship_Unit_Line1 test started");
        page.locator("xpath=//button[normalize-space(.)='Involved Parties']").click();
        page.locator("xpath=//input[@aria-label='Involved Party Contact']").type("NA_USO4_OB_PLANNER");
        Locator partyQ = page.locator("xpath=//select[@aria-label='Involved Party Qualifier ID']");
        partyQ.selectOption("LOGISTICS");
        page.locator("xpath=//button[@class='enButton' and normalize-space(text())='Save']").click();
        page.locator("xpath=//input[@aria-label='Involved Party Location']").type("USI2");
        Locator partyQ2 = page.locator("xpath=(//select[@aria-label='Involved Party Qualifier ID'])[2]");
        partyQ2.selectOption(new SelectOption().setLabel("LOGISTICS"));
        page.locator("xpath=/html/body/form/div[3]/div/div[3]/table/tbody/tr[1]/th[4]/table/tbody/tr/td/div/button").click();
    }

    @Test(priority = 9, dependsOnMethods = "shipUnitLineInvolvedParties")
    public void shipUnitLineFinishAndCaptureId() {
        test.log(Status.INFO, "Ship_Unit_Line2 test started");
        page.locator("xpath=//button[normalize-space(text())='Other Attributes']").click();
        page.locator("xpath=//button[normalize-space(text())='Finished']").click();
        Locator span = page.locator("xpath=/html/body/form/div[5]/div/div/div[1]/table[3]/tbody/tr/td[2]/span");
        String captured = span.textContent().trim();
        String numericOnly = captured.replaceAll("[^0-9]", "");
        System.setProperty("capturedOrderReleaseID", numericOnly);
        test.log(Status.PASS, "Captured Numeric ID: " + numericOnly);
    }

    @Test(priority = 10, dependsOnMethods = "shipUnitLineFinishAndCaptureId")
    public void handleDynamicCheckboxes() {
        test.log(Status.INFO, "handleDynamicCheckboxes test started");
        Locator checkbox = page.frameLocator("iframe#mainIFrame").locator("xpath=/html/body/form/div[5]/div/div/div[1]/table[3]/tbody/tr/td[1]/input");
        if (!Boolean.TRUE.equals(checkbox.isChecked())) {
            checkbox.check();
            test.log(Status.PASS, "Checkbox clicked successfully!");
        }
    }

    @Test(priority = 11, dependsOnMethods = "handleDynamicCheckboxes")
    public void actionsAndOrderManagementProcess() {
        test.log(Status.INFO, "Actions test started");
        page.locator("xpath=//button[normalize-space(text())='Actions']").click();
        helper.waitForTimeoutMillis(10000);

        // Enter frames and click paths similar to Selenium flow
        // actionFrame inside mainIFrame
        Locator orderMgmt = page.frameLocator("iframe#mainIFrame").frameLocator("iframe[name='actionFrame']").locator("xpath=//span[@id='actionTree.1_7.l']");
        if (!Boolean.TRUE.equals(orderMgmt.isVisible())) {
            orderMgmt = page.frameLocator("iframe#mainIFrame").frameLocator("iframe#actionFrame").locator("xpath=//span[@id='actionTree.1_7.l']");
        }
        orderMgmt.dispatchEvent("click");
        helper.waitForTimeoutMillis(2000);

        Locator utilities = page.frameLocator("iframe#mainIFrame").frameLocator("iframe[name='actionFrame']").locator("xpath=//span[@id='actionTree.1_7_8.l' or normalize-space(text())='Utilities']");
        utilities.dispatchEvent("click");
        Locator copyOrderRelease = page.frameLocator("iframe#mainIFrame").frameLocator("iframe[name='actionFrame']").locator("xpath=//a[@id='actionTree.1_7_8_4.k' or normalize-space(text())='Copy Order Release' or contains(@href,'copy_order_release')]");
        copyOrderRelease.dispatchEvent("click");

        // Handle popup window
        Page popup = page.waitForPopup(() -> {});
        popup.waitForLoadState();
        popup.bringToFront();
        // click Ship Unit
        Locator shipUnit = popup.locator("xpath=//button[normalize-space(text())='Ship Unit']");
        if (!Boolean.TRUE.equals(shipUnit.isVisible())) {
            shipUnit = popup.locator("xpath=//button[contains(text(),'Ship Unit')]");
        }
        shipUnit.scrollIntoViewIfNeeded();
        shipUnit.click();
        popup.locator("xpath=//button[@class='enButton' and normalize-space(text())='Finished']").click();
        popup.close();

        // Click Home
        Locator homeBtn = page.locator("xpath=//oj-button[@id='homeButton']");
        if (!Boolean.TRUE.equals(homeBtn.isVisible())) {
            homeBtn = page.locator("xpath=//button[@aria-labelledby='homeButton_oj27|text']");
        }
        homeBtn.click();
        test.log(Status.PASS, "Clicked Home button");
    }

    @AfterClass(alwaysRun = true)
    public void tearDown() {
        if (page != null) {
            page.context().tracing().stop(new Tracing.StopOptions().setPath(Paths.get("trace.zip")));
        }
        if (browser != null) browser.close();
        if (playwright != null) playwright.close();
        if (extent != null) extent.flush();
    }
}


