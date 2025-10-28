package RT_scripts.RT_scripts1;

import java.time.Duration;
import java.util.List;
import java.util.NoSuchElementException;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.NoSuchFrameException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.io.FileWriter;
import java.io.IOException;
import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.Status;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;



public class Occen {

	private static final String priority = null;
	ChromeDriver driver;
	WebDriverWait wait;
	ExtentReports extent;
	ExtentTest test;
	ExtentSparkReporter htmlReporter;


	
	@BeforeClass
	public void setUp() throws InterruptedException {
		driver = new ChromeDriver();
		driver.manage().window().maximize();
		wait = new WebDriverWait(driver, Duration.ofSeconds(600));
		htmlReporter = new ExtentSparkReporter("ExtentReport.html");
		extent = new ExtentReports();
		extent.attachReporter(htmlReporter);
		extent.setSystemInfo("OS", System.getProperty("os.name"));
		extent.setSystemInfo("User", System.getProperty("user.name"));
		extent.setSystemInfo("Browser", "Chrome");
		extent.setAnalysisStrategy(com.aventstack.extentreports.AnalysisStrategy.CLASS);
		test = extent.createTest("Occen Test Suite").assignAuthor("QA Team").assignCategory("Regression");



		test.log(Status.INFO, "Running Login Test...");

		driver.get("https://otmgtm-dev2-otmsaasna.otmgtm.us-phoenix-1.ocs.oraclecloud.com");
		Thread.sleep(5000);
		// Wait for and click Sign In button (adjust locator if needed)
		WebElement signInBtn = wait.until(ExpectedConditions.elementToBeClickable(
				By.xpath("/html/body/div/oj-idaas-signin-app-shell-rw/div[1]/div/div/div/div[2]/div/div/oj-module/div[1]/oj-module/div[1]/div/div[3]/div[1]/div/oj-button")));
		signInBtn.click();

		// Enter username
		WebElement username = wait.until(ExpectedConditions.visibilityOfElementLocated(By.name("loginfmt")));
		username.sendKeys("scott.kumar@unilever.com"); // Hard coded for temporary purpose
		//click on next
		driver.findElement(By.id("idSIButton9")).click();
		// Enter password
		WebElement password = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("i0118")));
		password.sendKeys("Invent@appwrk11"); // Hard coded for temporary purpose

		// Click submit (assuming it's a button or input type submit)
		WebElement submitBtn = wait.until(ExpectedConditions.elementToBeClickable(
				By.id("idSIButton9")
				));
		submitBtn.click();

		wait.until(ExpectedConditions.elementToBeClickable(By.id("homeButton"))).click();
		driver.findElement(By.id("homeButton")).click();
		// Verify login by checking for a known post-login element
		WebElement homeButton = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("homeButton")));
		Assert.assertTrue(homeButton.isDisplayed(), "Login failed or home button not found.");
		test.log(Status.PASS, "Login successful.");
		System.out.println("Login successful.");
		Thread.sleep(2000);

	}

	// Inside your class Occen

	@Test(priority = 1)
	public void Order_Managment() throws InterruptedException {
		test.log(Status.INFO, "Order_Managment test started");
		WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

		// Initialize helper
		RobustTestHelper helper = new RobustTestHelper(driver);

		// Test steps
		helper.waitForPageLoad();
		helper.switchToFrameContainingAny(By.id("label3"));
		helper.waitAndClick(By.id("label3"));
		Thread.sleep(6000);
		wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//li[@id='sb_2_2']//ins[contains(@class,'oj-treeview-disclosure-icon')]\r\n"
				+ ""))).click();
		Thread.sleep(5000);

		// Or if you want the 3rd child (Order Release)
		WebElement orderReleaseChild = wait.until(
				ExpectedConditions.elementToBeClickable(
						By.xpath("//li[@id='sb_2_2_2']//span[text()='Order Release']")
						)
				); 
		Thread.sleep(1000);		    
		orderReleaseChild.click();
		test.log(Status.PASS, "Clicked Order Release child");
		//	logStep("Clicked Order Release child");
	}

	@Test(priority = 2)
	public void In_order_release() throws InterruptedException {
		test.log(Status.INFO, "In_order_release test started");
		WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(60));
		RobustTestHelper helper = new RobustTestHelper(driver);
		helper.waitForPageLoad();

		// Switch to main iframe
		driver.switchTo().defaultContent();
		wait.until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(By.id("mainIFrame")));

		// Try to find the indicator dropdown in mainIFrame or its child iframes
		boolean dropdownClicked = false;
		List<WebElement> allFrames = driver.findElements(By.tagName("iframe"));

		for (int i = -1; i < allFrames.size(); i++) {
			try {
				driver.switchTo().defaultContent();
				wait.until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(By.id("mainIFrame")));

				if (i >= 0) driver.switchTo().frame(i);  // skip -1 iteration

				WebElement indicatorDropdown = wait.until(ExpectedConditions.visibilityOfElementLocated(
						By.xpath("//select[@name='order_release/indicator' and @aria-label='Indicator']")
						));

				((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'});", indicatorDropdown);
				indicatorDropdown.click();

				// Select the second option (change if needed)
				WebElement option = indicatorDropdown.findElement(By.xpath("./option[2]"));
				option.click();
				test.log(Status.PASS, "Dropdown clicked and option selected.");
				System.out.println("Dropdown clicked and option selected.");
				dropdownClicked = true;
				break;
			} catch (Exception e) {
				// continue to next frame
			}
		}

		if (!dropdownClicked) {
			throw new RuntimeException("Indicator dropdown not found in any iframe!");
		}

		// Now locate and click the Search button
		By searchBtn = By.xpath(
				"//oj-button[.//span[normalize-space()='Search']]//button | " +
						"//button[@aria-label='Search' or @id='search' or @id='search_button' or normalize-space()='Search']"
				);

		driver.switchTo().defaultContent();
		wait.until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(By.id("mainIFrame")));

		List<WebElement> searchFrames = driver.findElements(By.tagName("iframe"));
		boolean searchClicked = false;

		// 1) Try in mainIFrame
		try {
			WebElement btn = wait.until(ExpectedConditions.elementToBeClickable(searchBtn));
			((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'});", btn);
			btn.click();
			searchClicked = true;
		} catch (Exception ignore) {}

		// 2) Try child iframes
		if (!searchClicked) {
			for (WebElement frame : searchFrames) {
				try {
					driver.switchTo().defaultContent();
					wait.until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(By.id("mainIFrame")));
					driver.switchTo().frame(frame);

					WebElement btn = wait.until(ExpectedConditions.elementToBeClickable(searchBtn));
					((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'});", btn);
					btn.click();
					searchClicked = true;
					break;
				} catch (Exception ignore) {}
			}
		}

		// 3) Fallback: try default content
		if (!searchClicked) {
			driver.switchTo().defaultContent();
			WebElement btn = wait.until(ExpectedConditions.elementToBeClickable(searchBtn));
			btn.click();
		}

		test.log(Status.PASS, "Search button clicked successfully.");
		System.out.println("Search button clicked successfully.");

	}


	//page where we need to click on new button
	@Test(priority = 3)
	public void Order_Releases() {
		test.log(Status.INFO, "Order_Releases test started");

		//		List<WebElement> allLinks = driver.findElements(By.tagName("a"));
		//		System.out.println("Total links found: " + allLinks.size());
		//		for (WebElement link : allLinks) {
		//		    System.out.println("Link text: '" + link.getText() + "'");
		//		}

		WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));

		// 1) Switch to frame if needed (example with index 0, adjust if multiple)
		driver.switchTo().defaultContent(); 
		driver.switchTo().frame(0);  

		// 2) Wait for table or parent container first
		wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//table")));

		// 3) Find link by partial match


		WebElement orderLink = wait.until(ExpectedConditions.presenceOfElementLocated(
				By.xpath("//a[contains(text(),'ULA/NA')]")));

		// 4) Scroll & click
		((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", orderLink);
		wait.until(ExpectedConditions.elementToBeClickable(orderLink)).click();



		//Click on NEW
		WebElement element11 = wait.until(ExpectedConditions.elementToBeClickable(
				By.xpath("/html/body/div[1]/table/tbody/tr/td[2]/table/tbody/tr/td[3]/div/button")
				));

		element11.click();
		test.log(Status.PASS, "Clicked on NEW button in Order Releases page");
		//	logStep("Clicked on NEW button in Order Releases page");
	}
	@Test(priority = 4)
	public void Order_Manager() throws InterruptedException { 
		test.log(Status.INFO, "Order_Manager test started");
		WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));

		//		WebElement orderReleaseInput = wait.until(
		//				ExpectedConditions.visibilityOfElementLocated(
		//						By.xpath("/html/body/form/div[3]/div/div[1]/table/tbody/tr[1]/td[1]/div/input[2]")
		//						)
		//				);
		//
		//		// now safe to interact
		//		orderReleaseInput.sendKeys("20211005-0003");

		// Locate the dropdown
		WebElement domainDropdown = wait.until(
				ExpectedConditions.visibilityOfElementLocated(By.xpath("//select[@aria-label='Domain Name']"))
				);

		// Wrap with Select class
		Select selectDomain = new Select(domainDropdown);

		// Select by visible text
		selectDomain.selectByVisibleText("ULA/NA");

		// ✅ Verify selected
		test.log(Status.PASS, "Selected Domain: " + selectDomain.getFirstSelectedOption().getText());

		LocalDate today = LocalDate.now();
		String formattedDate = today.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + " 00:00:00";
		// Locate the Early Pickup Date input
		WebElement earlyPickupDate = wait.until(ExpectedConditions.visibilityOfElementLocated(
				By.xpath("//input[@aria-label='Early Pickup Date']")
				));

		// Clear any existing value
		earlyPickupDate.clear();

		// Send the formatted date
		earlyPickupDate.sendKeys(formattedDate);

		test.log(Status.PASS, "Entered Early Pickup Date: " + formattedDate);



		LocalDate today1 = LocalDate.now();
		String latePickupDate = today1.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + " 23:59:00";
		// Locate the Late Pickup Date input
		WebElement latePickup = wait.until(ExpectedConditions.visibilityOfElementLocated(
				By.xpath("//input[@aria-label='Late Pickup Date']")
				));

		// Clear any old value
		latePickup.clear();

		// Send today’s date with 23:59:00
		latePickup.sendKeys(latePickupDate);

		// Optional: Tab out so OTM registers it
		latePickup.sendKeys(Keys.TAB);

		test.log(Status.PASS, "Entered Late Pickup Date: " + latePickupDate);


		LocalDate today12 = LocalDate.now();
		String earlyDeliveryDate = today.plusDays(10)
				.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + " 00:00:00";
		// Locate Early Delivery Date input
		WebElement earlyDelivery = wait.until(ExpectedConditions.visibilityOfElementLocated(
				By.xpath("//input[@aria-label='Early Delivery Date']")
				));

		// Clear existing value
		earlyDelivery.clear();

		// Send calculated date
		earlyDelivery.sendKeys(earlyDeliveryDate);

		// Optional: tab out
		earlyDelivery.sendKeys(Keys.TAB);

		test.log(Status.PASS, "Entered Early Delivery Date: " + earlyDeliveryDate);



		LocalDate today3 = LocalDate.now();
		String lateDeliveryDate = today.plusDays(10)
				.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + " 23:59:00";
		// Locate Late Delivery Date input
		WebElement lateDelivery = wait.until(ExpectedConditions.visibilityOfElementLocated(
				By.xpath("//input[@aria-label='Late Delivery Date']")
				));

		// Clear old value
		lateDelivery.clear();

		// Send the calculated date
		lateDelivery.sendKeys(lateDeliveryDate);

		// Optional: blur/tab out so OTM registers the value
		lateDelivery.sendKeys(Keys.TAB);

		test.log(Status.PASS, "Entered Late Delivery Date: " + lateDeliveryDate);


		///////////////////////////////////////////////////////////////////////////



		WebDriverWait wait1 = new WebDriverWait(driver, Duration.ofSeconds(20));

		// wait until the dropdown is visible
		WebElement orderConfigDropdown = wait1.until(
				ExpectedConditions.visibilityOfElementLocated(
						By.xpath("//select[@aria-label='Order Configuration']")
						)
				);

		// create a Select object
		Select orderConfig = new Select(orderConfigDropdown);

		// Example 1: select by visible text
		orderConfig.selectByVisibleText("SHIP_UNITS");
		test.log(Status.PASS, "Selected by visible text");


		WebDriverWait wait11 = new WebDriverWait(driver, Duration.ofSeconds(20));

		// Example: replace with your actual iframe XPath or id

		// Get all iframes on the page
		// Check if any iframes exist
		List<WebElement> frames = driver.findElements(By.tagName("iframe"));

		if (frames.size() > 0) {
			System.out.println("Iframe found. Switching to the first one for example...");

			// Optionally, you can loop and find the correct iframe by id or src
			boolean switched = false;
			for (WebElement frame : frames) {
				try {
					driver.switchTo().frame(frame);
					// Check if the target element exists in this frame
					if (driver.findElements(By.id("order_release/source/xid")).size() > 0) {
						System.out.println("Found element in this iframe.");
						switched = true;
						break;
					} else {
						// Not this frame, switch back
						driver.switchTo().defaultContent();
					}
				} catch (NoSuchFrameException e) {
					System.out.println("Unable to switch to iframe: " + e.getMessage());
				}
			}

			if (!switched) {
				System.out.println("Element not found in any iframe.");
			}

		} else {
			System.out.println("No iframes found. Searching in default content...");
		}

		// Now, try to interact with the element in the current context
		try {
			WebElement sourceInput = new WebDriverWait(driver, Duration.ofSeconds(5))
					.until(ExpectedConditions.visibilityOfElementLocated(By.id("order_release/source/xid")));
			sourceInput.sendKeys("USO4");
		} catch (TimeoutException e) {
			System.out.println("Element not found in page: " + e.getMessage());



			WebElement sourceLocation = wait.until(ExpectedConditions.visibilityOfElementLocated(
					By.xpath("//input[@aria-label='Source Location ID']\r\n"
							+ "")
					));
			sourceLocation.clear();

			sourceLocation.sendKeys("USO4");

			driver.switchTo().defaultContent();

		}  
	}


	@Test(priority = 5)
	public void Order_Manager1() throws InterruptedException { 
		test.log(Status.INFO, "Order_Manager1 test started");
		WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
		String msg = handleAlert(driver, true); 
		// Helper function to switch to the iframe containing the element
		List<WebElement> frames = driver.findElements(By.tagName("iframe"));
		boolean found = false;

		for (WebElement frame : frames) {
			driver.switchTo().frame(frame);
			if (driver.findElements(By.name("order_release/destination/xid")).size() > 0) {
				found = true;
				break;
			} else {
				driver.switchTo().defaultContent();
			}
		}

		if (!found) {
			System.out.println("Element not found in any iframe, staying in default content.");
		}

		// DESTINATION LOCATION FIELD
		By destinationLocator = By.id("order_release/destination/xid");

		try {
			WebElement destInput = new WebDriverWait(driver, Duration.ofSeconds(15))
					.until(ExpectedConditions.visibilityOfElementLocated(destinationLocator));
			destInput.sendKeys("USB6");
		} catch (TimeoutException e) {
			System.out.println("Element not found in default content: " + e.getMessage());

			// Try switching to the iframe containing the element
			switchToFrameContainingElement1(destinationLocator);

			try {
				WebElement destInputFrame = new WebDriverWait(driver, Duration.ofSeconds(15))
						.until(ExpectedConditions.visibilityOfElementLocated(destinationLocator));
				destInputFrame.clear();
				destInputFrame.sendKeys("USC6");
			} catch (TimeoutException ex) {
				System.out.println("Element not found even in iframe: " + ex.getMessage());
			}

			driver.switchTo().defaultContent();
		}
		test.log(Status.PASS, "Destination Location entered");
		WebDriverWait wait1 = new WebDriverWait(driver, Duration.ofSeconds(10));

		driver.findElement(By.xpath("/html/body/div[6]/table/tbody/tr/td[2]/div/button")).click();
		Thread.sleep(4000);
		driver.findElement(By.xpath("/html/body/form/div[3]/div/div[1]/table/tbody/tr[5]/td/table/tbody/tr/td/div/button")).click();
		Thread.sleep(4000);
	}

	// Move this outside the test method
	private void switchToFrameContainingElement1(By destinationLocator) {

	}

	@Test(priority = 6)

	//Ship unit > Transport Handling Unit
	public void Order_Manager2() throws InterruptedException {
		test.log(Status.INFO, "Order_Manager2 test started");
		WebDriverWait localWait = new WebDriverWait(driver, Duration.ofSeconds(30));
		//		WebElement shipUnitId = localWait
		//				.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//input[@aria-label='Ship Unit ID']")));
		//		shipUnitId.clear();
		//				shipUnitId.sendKeys("20211005-0001");
		//				shipUnitId.sendKeys(Keys.TAB);


		WebDriverWait shortWait = new WebDriverWait(driver, Duration.ofSeconds(20));
		WebElement thuInput = localWait
				.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//input[@aria-label='Transport Handling Unit']")));
		thuInput.clear();
		thuInput.sendKeys("BOX_000000000084172226");
		thuInput.sendKeys(Keys.TAB);


		WebElement thuCountInput = shortWait
				.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//input[@aria-label='Transport Handling Unit Count']")));
		thuCountInput.clear();
		thuCountInput.sendKeys("20");

		WebElement grossWeightInput = shortWait
				.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//input[@aria-label='Total Gross Weight']")));
		grossWeightInput.clear();
		grossWeightInput.sendKeys("20400");
		((JavascriptExecutor) driver).executeScript("window.scrollTo({ top: document.body.scrollHeight, behavior: 'smooth' });");
		WebElement newLineItem = shortWait
				.until(ExpectedConditions.elementToBeClickable(By.xpath("//button[@class='enButton' and text()='New Line Item']")));
		((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'});", newLineItem);
		newLineItem.click();

		test.log(Status.PASS, "Clicked New Line Item button in Order Manager");
		//		logStep("Clicked New Line Item button in Order Manager");
	}

	@Test(priority = 7)
	//Ship unit > Transport Handling Unit ||SCROLL|| > New Line Item
	public void Ship_Unit_Line() throws InterruptedException {
		test.log(Status.INFO, "Ship_Unit_Line test started");
		WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));
		//Packaged Item ID
		WebElement itemInput = wait.until(
				ExpectedConditions.visibilityOfElementLocated(
						By.xpath("//input[@aria-label='Packaged Item ID']\r\n" + "")));
		itemInput.sendKeys("000000000020005492");   //This need to change as per data
		Thread.sleep(2000);
		//Item ID
		WebElement Item_ID = wait.until(
				ExpectedConditions.visibilityOfElementLocated(
						By.xpath("//input[@aria-label='Item ID']" + "")));
		Item_ID.sendKeys("000000000068441373");   //This need to change as per data
		Thread.sleep(2000);
		//'Total Package Count' 
		WebElement Total_Package_Count = wait.until(
				ExpectedConditions.visibilityOfElementLocated(
						By.xpath("//input[@aria-label='Total Package Count']" + "")));
		Total_Package_Count.sendKeys("20");
		Thread.sleep(2000);
		//Packaging Unit

		WebElement Packaging_Unit = wait.until(
				ExpectedConditions.visibilityOfElementLocated(
						By.xpath("//input[@aria-label='Packaging Unit']\r\n"
								+ "" + "")));
		Packaging_Unit.sendKeys("BOX_000000000084172488");
		Thread.sleep(2000);
		//Gross Weight
		WebElement Gross_Weight = wait.until(
				ExpectedConditions.visibilityOfElementLocated(
						By.xpath("//input[@aria-label='Gross Weight']\r\n"
								+ "")));
		Gross_Weight.sendKeys("20400");
		Thread.sleep(2000);
		//Volume 2380
		WebElement Volume = wait.until(
				ExpectedConditions.visibilityOfElementLocated(
						By.xpath("//input[@aria-label='Gross Volume']\r\n"
								+ "")));
		Volume.sendKeys("2380");
		Thread.sleep(2000);

		WebElement transport = wait.until(
				ExpectedConditions.visibilityOfElementLocated(
						By.xpath("//input[@aria-label='Special Service']\r\n"
								+ "")));
		transport.sendKeys("TRANSPORT");
		Thread.sleep(2000);

		//click on transport save
		WebElement transport1 = wait.until(
				ExpectedConditions.visibilityOfElementLocated(
						By.xpath("//button[normalize-space()='Save']\r\n"
								+ "")));
		transport1.click();
		//Click on Save
		WebElement Save = wait.until(
				ExpectedConditions.visibilityOfElementLocated(
						By.xpath("//button[contains(@onclick,\"checkPiForItem()\") and text()='Save']\r\n"
								+ "")));
		Save.click();	
		Thread.sleep(6000);
		//scroll to bottom
		((JavascriptExecutor) driver).executeScript("window.scrollTo({ top: document.body.scrollHeight, behavior: 'smooth' });");

		String msg = handleAlert(driver, true); 
		//click on new line item
		driver.findElement(By.xpath("//button[contains(@onclick,'checkData') and text()='Save']"
				+ "")).click();		
		Thread.sleep(4000);
		WebElement Save1 = wait.until(
				ExpectedConditions.visibilityOfElementLocated((By.xpath("//button[normalize-space(.)='Line Item']\r\n"
						+ ""))));
		Save1.click();
		Thread.sleep(2000);
		test.log(Status.PASS, "Clicked Save button in Ship Unit Line");
	}
	@Test(priority = 8)
	//Ship unit > Transport Handling Unit ||SCROLL|| > New Line Item 
	public void Ship_Unit_Line1() throws InterruptedException {
		test.log(Status.INFO, "Ship_Unit_Line1 test started");
		WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));
		String msg = handleAlert(driver, true); 
		// Involved Party Contact
		WebElement Save = wait.until(
				ExpectedConditions.visibilityOfElementLocated(
						By.xpath("//button[normalize-space(.)='Involved Parties']\r\n"
								+ ""))); 
		Save.click();	
		Thread.sleep(2000);

		//'Involved Party Contact'
		WebElement Involved_Party_Contact = wait.until(
				ExpectedConditions.visibilityOfElementLocated(
						By.xpath("//input[@aria-label='Involved Party Contact']\r\n"
								+ "")));
		Involved_Party_Contact.sendKeys("NA_USO4_OB_PLANNER");

		//Involved Party Qualifier ID
		//		WebElement Involved_Party_Qualifier_ID = wait111.until(
		//				ExpectedConditions.visibilityOfElementLocated(
		//						By.xpath("//select[@aria-label='Involved Party Qualifier ID']"
		//								+ "")));
		//		          Involved_Party_Qualifier_ID.click();

		// Wait until the dropdown is visible
		WebElement partyQualifierDropdown = wait.until(
				ExpectedConditions.visibilityOfElementLocated(
						By.xpath("//select[@aria-label='Involved Party Qualifier ID']")
						)
				);

		// Create Select object
		Select select = new Select(partyQualifierDropdown);

		// Option 1: Select by value (note: use the full value attribute, not just digits)
		select.selectByValue("LOGISTICS");

		// Option 2: Select by visible text (the text shown in dropdown)
		// select.selectByVisibleText("0030001584");

		// Small wait if needed to allow onchange JS to update hidden field
		Thread.sleep(2000);

		// Verify the hidden field updated
		WebElement hiddenField = driver.findElement(
				By.xpath("//input[@name='display_contact_order_release_inv_party/involved_party_qual/xid']")
				);
		String hiddenValue = hiddenField.getAttribute("value");
		System.out.println("Captured hidden value: " + hiddenValue);



		String msg11 = handleAlert(driver, true); 
		//		driver.findElement(By.xpath("/select[@aria-label='Involved Party Qualifier ID']/option[2]")).click();
		Thread.sleep(2000);

		driver.findElement(By.xpath("//button[@class='enButton' and normalize-space(text())='Save']")).click(); 

		// Involved Party Location
		WebElement Involved_Party_Location = wait.until(
				ExpectedConditions.visibilityOfElementLocated(
						By.xpath("//input[@aria-label='Involved Party Location']")));
		Involved_Party_Location.sendKeys("USI2");
		Thread.sleep(2000);
		// Wait until dropdown is visible
		// Click to open dropdown

		// Wait for the dropdown to be visible
		WebElement partyQualifierDropdown1 = wait.until(
				ExpectedConditions.visibilityOfElementLocated(
						By.xpath("(//select[@aria-label='Involved Party Qualifier ID'])[2]")
						)
				);

		// Create Select object
		Select select1 = new Select(partyQualifierDropdown1);

		// Option 1: Select by value
		select1.selectByValue("SERVPROV.0030001584");

		// Option 2: Select by visible text (if you want just "0030001584")
		select1.selectByVisibleText("LOGISTICS");

		// Optional: print selected option
		System.out.println("Selected: " + select1.getFirstSelectedOption().getText());

		Thread.sleep(2000);




		// Confirm value updated in hidden field
		WebElement hiddenField1 = driver.findElement(
				By.xpath("//input[@name='display_contact_order_release_inv_party/involved_party_qual/xid']")
				);
		String hiddenValue1 = hiddenField1.getAttribute("value");
		System.out.println("Selected qualifier value: " + hiddenValue1);

		driver.findElement(By.xpath("/html/body/form/div[3]/div/div[3]/table/tbody/tr[1]/th[4]/table/tbody/tr/td/div/button")).click();
		//Contacts
	}
	@Test(priority = 9)
	public void Ship_Unit_Line2() throws InterruptedException, IOException {
		test.log(Status.INFO, "Ship_Unit_Line2 test started");
		WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));
		String msg = handleAlert(driver, true); 
		//other attributes
		WebElement Contacts = wait.until(
				ExpectedConditions.visibilityOfElementLocated(
						By.xpath("//button[normalize-space(text())='Other Attributes']\r\n"
								+ "")));
		// button clicked other attributes 		
		Contacts.click();
		Thread.sleep(2000);
		WebElement Save1 = wait.until(
				ExpectedConditions.visibilityOfElementLocated(
						By.xpath("//button[normalize-space(text())='Finished']\r\n" + "")));
		Save1.click();
		Thread.sleep(5000);	

		// Locate the span and capture the value
		WebDriverWait wait1 = new WebDriverWait(driver, Duration.ofSeconds(20));

		// ✅ Point to the <span>, not /text()
		WebElement orderReleaseElement = wait1.until(
				ExpectedConditions.visibilityOfElementLocated(
						By.xpath("/html/body/form/div[5]/div/div/div[1]/table[3]/tbody/tr/td[2]/span")
						)
				);

		// Get the full text inside the span
		String capturedValue = orderReleaseElement.getText().trim();  // e.g. "ULA/NA.0410858884"
		System.out.println("Captured Value: " + capturedValue);

		// Extract only numbers
		String numericOnly = capturedValue.replaceAll("[^0-9]", "");
		System.out.println("Extracted Numeric ID: " + numericOnly);

		// Store in shared variable
		SharedData.capturedOrderReleaseID = numericOnly;
		System.out.println("Captured Numeric ID Stored: " + SharedData.capturedOrderReleaseID);

		try (FileWriter fw = new FileWriter("capturedID.txt")) {
			fw.write(numericOnly);
		}
		System.out.println("✅ Saved to file: " + numericOnly);
		test.log(Status.PASS, "Captured Numeric ID Stored: " + SharedData.capturedOrderReleaseID);
		test.log(Status.PASS, "Saved to file: " + SharedData.capturedOrderReleaseID);
	}
	public class SharedData {
		public static String capturedOrderReleaseID; // Public & static for global access
	}

	private String handleAlert(ChromeDriver driver2, boolean b) {

		return null;
	}

	@Test(priority = 10)
	public void handleDynamicCheckboxes() throws InterruptedException {
		test.log(Status.INFO, "handleDynamicCheckboxes test started");
		WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));
		// Step 1: Switch to iframe mainIFrame
		driver.switchTo().defaultContent();
		wait.until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(By.id("mainIFrame")));
		System.out.println("✅ Switched to iframe: mainIFrame");

		// Step 2: Locate the checkbox using the given XPath
		WebElement checkbox = wait.until(ExpectedConditions.elementToBeClickable(

				//check box xpath 

				//				By.xpath("/html/body/form/div[5]/div/div/div[1]/table[2]/tbody/tr/td[1]/input")
				//				));       

				By.xpath("/html/body/form/div[5]/div/div/div[1]/table[3]/tbody/tr/td[1]/input")
				));
		// Step 3: Click if not already selected
		if (!checkbox.isSelected()) {
			checkbox.click();
			test.log(Status.PASS, "Checkbox clicked successfully!");
			System.out.println("✔️ Checkbox clicked successfully!");
		} else {
			System.out.println("ℹ️ Checkbox already selected.");
		}

		// Optional: highlight for debugging
		((JavascriptExecutor) driver).executeScript("arguments[0].style.border='3px solid red'", checkbox);

		Thread.sleep(2000);
	}
	@Test(priority = 11)
	public void Actions() throws InterruptedException {
		test.log(Status.INFO, "Actions test started");
		WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(30));

		// ✅ Actions
		WebElement actionsButton = wait.until(ExpectedConditions.elementToBeClickable(
				By.xpath("//button[normalize-space(text())='Actions']")));
		actionsButton.click();
		test.log(Status.PASS, "Clicked Actions button");

		Thread.sleep(10000);
		//	logStep("Clicked Actions button");
	}


	@SuppressWarnings("unused")
	@Test(priority = 12)
	public void Order_Management_Process() throws InterruptedException {
		test.log(Status.INFO, "Order_Management_Process test started");
		// ✅ Order Management
		WebDriverWait wait1 = new WebDriverWait(driver, Duration.ofSeconds(30));

		try {
			// 1. ALWAYS start from the main page to avoid confusion.
			driver.switchTo().defaultContent();
			System.out.println("Switched to default content.");

			// 2. Switch to the main content frame first.
			wait1.until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(By.id("mainIFrame")));
			System.out.println("Switched to mainIFrame.");

			// 3. NOW, look for the actionFrame. Let's try finding it by ID first.
			try {
				wait1.until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(By.id("actionFrame")));
				System.out.println("✅ SUCCESS: Switched to actionFrame using ID.");
			} catch (TimeoutException e) {
				// 4. If ID fails, try finding it by NAME. This is our most likely fix.
				System.out.println("Could not find actionFrame by ID, trying by NAME...");
				wait1.until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(By.name("actionFrame")));
				System.out.println("✅ SUCCESS: Switched to actionFrame using NAME.");
			}

			// By now, we should be inside the correct frame.

			// 5. Locate the "Order Management" span you want to click.
			WebElement orderManagement = wait1.until(
					ExpectedConditions.visibilityOfElementLocated(By.xpath("//span[@id='actionTree.1_7.l']"))
					);

			// 6. Click the element.
			((JavascriptExecutor) driver).executeScript("arguments[0].click();", orderManagement);
			System.out.println("✅ Clicked on Order Management.");

			Thread.sleep(2000);

		} catch (TimeoutException e) {
			System.err.println("CRITICAL ERROR: Could not find the 'actionFrame' by ID or Name even after switching to 'mainIFrame'.");
			// Add page source logging for final debugging if this fails
			// System.out.println(driver.getPageSource()); 
			throw e;
		} finally {
			// 7. No matter what happens, switch back to the main page.
			driver.switchTo().defaultContent();
			System.out.println("Switched back to default content.");
		}


		// ✅ Utilities and Copy Order Release (robust frame switching + resilient locators)
		WebDriverWait wait11 = new WebDriverWait(driver, Duration.ofSeconds(30));

		try {
			// 1) Always reset to the top-level document
			driver.switchTo().defaultContent();
			System.out.println("Switched to default content.");

			// 2) Enter main content frame first
			wait11.until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(By.id("mainIFrame")));
			System.out.println("Switched to mainIFrame.");

			// 3) Enter actionFrame by ID, fallback to NAME
			try {
				wait11.until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(By.id("actionFrame")));
				System.out.println("✅ SUCCESS: Switched to actionFrame using ID.");
			} catch (TimeoutException e) {
				System.out.println("Could not find actionFrame by ID, trying by NAME...");
				wait11.until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(By.name("actionFrame")));
				System.out.println("✅ SUCCESS: Switched to actionFrame using NAME.");
			}

			// 4) Click Utilities (prefer exact id, fallback to text)
			By utilitiesById = By.xpath("//span[@id='actionTree.1_7_8.l']");
			By utilitiesByText = By.xpath("//span[normalize-space(text())='Utilities']");
			WebElement utilitiesEl;
			try {
				utilitiesEl = wait11.until(ExpectedConditions.visibilityOfElementLocated(utilitiesById));
			} catch (TimeoutException e) {
				utilitiesEl = wait11.until(ExpectedConditions.visibilityOfElementLocated(utilitiesByText));
			}
			((JavascriptExecutor) driver).executeScript("arguments[0].click();", utilitiesEl);
			System.out.println("✔️ Clicked Utilities");
			Thread.sleep(1000);

			// 5) Click Copy Order Release (prefer stable id, fallback to link text/href)
			By corById = By.xpath("//a[@id='actionTree.1_7_8_4.k']");
			By corByText = By.xpath("//a[normalize-space(text())='Copy Order Release']");
			By corByHref = By.xpath("//a[contains(@href,'copy_order_release')]");
			WebElement copyOrderReleaseEl;
			try {
				copyOrderReleaseEl = wait11.until(ExpectedConditions.elementToBeClickable(corById));
			} catch (TimeoutException e) {
				try {
					copyOrderReleaseEl = wait11.until(ExpectedConditions.elementToBeClickable(corByText));
				} catch (TimeoutException e2) {
					copyOrderReleaseEl = wait11.until(ExpectedConditions.elementToBeClickable(corByHref));
				}
			}
			((JavascriptExecutor) driver).executeScript("arguments[0].click();", copyOrderReleaseEl);
			System.out.println("✔️ Clicked Copy Order Release");
			Thread.sleep(1000);

		} catch (TimeoutException e) {
			System.err.println("CRITICAL: Could not reach Utilities/Copy Order Release after robust frame switching.");
			throw e;
		} finally {
			// Optional: return to default content
			driver.switchTo().defaultContent();
			System.out.println("Switched back to default content.");
		}


		// ✅ Handle new popup window
		String parentWindow = driver.getWindowHandle();
		wait11.until(ExpectedConditions.numberOfWindowsToBe(2));

		for (String windowHandle : driver.getWindowHandles()) {
			if (!windowHandle.equals(parentWindow)) {
				driver.switchTo().window(windowHandle);
				break;
			}
		}

		driver.manage().window().maximize();
		System.out.println("✅ Switched to popup window");
		Thread.sleep(5000);
		// ✅ ROBUST SHIP UNIT BUTTON CLICK (with null safety)
		RobustTestHelper helper = null;
		try {
			// Initialize helper if not already done
			if (helper == null) {
				helper = new RobustTestHelper(driver);
				System.out.println("Initialized RobustTestHelper");
			}

			// Wait for popup window to be fully loaded
			System.out.println("Waiting for popup window to load completely...");
			if (helper != null) {
				helper.waitForPageLoad();
			} else {
				new WebDriverWait(driver, Duration.ofSeconds(30))
				.until(webDriver -> ((JavascriptExecutor) webDriver)
						.executeScript("return document.readyState").equals("complete"));
			}
			Thread.sleep(5000);  // Additional wait for dynamic content

			// Debug: Print current window info
			System.out.println("Current window handle: " + driver.getWindowHandle());
			System.out.println("Current URL: " + driver.getCurrentUrl());
			System.out.println("Page title: " + driver.getTitle());

			// Check if we need to switch frames within the popup
			driver.switchTo().defaultContent(); // Reset to top-level in popup

			// Try to find any iframes in the popup
			List<WebElement> popupFrames = driver.findElements(By.tagName("iframe"));
			System.out.println("Found " + popupFrames.size() + " iframes in popup");

			boolean buttonFound = false;
			WebElement shipUnitButton = null;

			// Strategy 1: Try to find button in main popup content
			try {
				System.out.println("Strategy 1: Looking for Ship Unit button in main popup content...");
				shipUnitButton = wait1.until(ExpectedConditions.elementToBeClickable(
						By.xpath("//button[normalize-space(text())='Ship Unit']")
						));
				buttonFound = true;
				System.out.println("✅ Found Ship Unit button in main content");
			} catch (TimeoutException e) {
				System.out.println("Strategy 1 failed: " + e.getMessage());
			}

			// Strategy 2: Try alternative locators in main content
			//			if (!buttonFound && helper != null) {
			//				try {
			//					System.out.println("Strategy 2: Trying alternative locators...");
			//					shipUnitButton = helper.findElementWithFallback(
			//							By.xpath("//button[@class='enButton' and normalize-space()='Ship Unit']")
			//							);
			//					buttonFound = true;
			//					System.out.println("✅ Found Ship Unit button with alternative locators");
			//				} catch (Exception e) {
			//					System.out.println("Strategy 2 failed: " + e.getMessage());
			//				}
			//			} else if (!buttonFound) {
			//				// Fallback without helper
			//				try {
			//					System.out.println("Strategy 2 (fallback): Trying basic locators...");
			//					shipUnitButton = driver.findElement(By.xpath("//button[contains(text(), 'Ship Unit')]"));
			//					buttonFound = true;
			//					System.out.println("✅ Found Ship Unit button with basic locator");
			//				} catch (Exception e) {
			//					System.out.println("Strategy 2 fallback failed: " + e.getMessage());
			//				}
			//			}

			// Strategy 3: Try each iframe in the popup
			if (!buttonFound && popupFrames.size() > 0) {
				for (int i = 0; i < popupFrames.size(); i++) {
					try {
						System.out.println("Strategy 3: Checking iframe " + (i + 1) + " of " + popupFrames.size());
						driver.switchTo().frame(popupFrames.get(i));
						Thread.sleep(1000); // Wait for frame content to load

						try {
							shipUnitButton = wait1.until(ExpectedConditions.elementToBeClickable(
									By.xpath("//button[normalize-space(text())='Ship Unit']")
									));
							buttonFound = true;
							System.out.println("✅ Found Ship Unit button in iframe " + (i + 1));
							break;
						} catch (TimeoutException e) {
							// Try alternative locators in this frame
							try {
								shipUnitButton = driver.findElement(By.xpath("//button[contains(text(), 'Ship Unit')]"));
								buttonFound = true;
								System.out.println("✅ Found Ship Unit button in iframe " + (i + 1) + " with alternative locators");
								break;
							} catch (Exception e2) {
								System.out.println("No Ship Unit button in iframe " + (i + 1));
							}
						}

						driver.switchTo().defaultContent(); // Return to popup root
					} catch (Exception e) {
						System.out.println("Error checking iframe " + (i + 1) + ": " + e.getMessage());
						driver.switchTo().defaultContent(); // Ensure we're back at popup root
					}
				}
			}

			// Strategy 4: Debug - print all buttons on the page
			if (!buttonFound) {
				System.out.println("Strategy 4: Debugging - printing all buttons on page...");
				driver.switchTo().defaultContent();
				List<WebElement> allButtons = driver.findElements(By.xpath("//button | //input[@type='button'] | //input[@type='submit']"));
				System.out.println("Found " + allButtons.size() + " buttons/inputs on page:");

				for (int i = 0; i < Math.min(allButtons.size(), 10); i++) {
					try {
						WebElement btn = allButtons.get(i);
						String text = btn.getText().trim();
						String value = btn.getAttribute("value");
						String className = btn.getAttribute("class");
						System.out.println("Button " + (i + 1) + ": text='" + text + "', value='" + value + "', class='" + className + "'");
					} catch (Exception e) {
						System.out.println("Button " + (i + 1) + ": Error reading attributes - " + e.getMessage());
					}
				}
			}

			if (buttonFound && shipUnitButton != null) {
				// Scroll to button and click
				if (helper != null) {
					helper.scrollToElement(shipUnitButton);
				} else {
					((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", shipUnitButton);
				}
				((JavascriptExecutor) driver).executeScript("arguments[0].click();", shipUnitButton);
				System.out.println("✅ Successfully clicked Ship Unit button");

				// Wait for any processing after Ship Unit click
				if (helper != null) {
					helper.waitForPageLoad();
				} else {
					Thread.sleep(3000);
				}
				Thread.sleep(2000);
			} else {
				throw new NoSuchElementException("Could not find Ship Unit button in popup window after trying all strategies");
			}

		} catch (Exception e) {
			System.err.println("Failed to click Ship Unit button: " + e.getMessage());

			// Safe error handling - check if helper exists before using it
			try {
				if (helper != null) {
					helper.takeScreenshot("ship_unit_button_failure");
					helper.debugPageStructure();
				} else {
					System.out.println("Helper is null, cannot take screenshot or debug page structure");


					System.out.println("Basic screenshot taken");
				}
			} catch (Exception debugException) {
				System.err.println("Failed to take screenshot or debug: " + debugException.getMessage());
			}

			throw e;
		}

		WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));

		// safest - using name
		WebElement finishedButton = wait.until(
				ExpectedConditions.elementToBeClickable(By.xpath("//button[@class='enButton' and normalize-space(text())='Finished']"))
				);

		finishedButton.click();

		driver.close(); // Close the popup window

		driver.switchTo().window(parentWindow); // Switch back to parent

		// ✅ Step 3: Re-enter ULA frame after popup
		wait11.until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(By.id("mainIFrame")));
		WebDriverWait wait111 = new WebDriverWait(driver, Duration.ofSeconds(10));
		// ✅ Step 4: Click Home
		try {
			// ✅ First try with <oj-button> locator
			WebElement orderReleaseChild = wait.until(
					ExpectedConditions.elementToBeClickable(
							By.xpath("//oj-button[@id='homeButton']"))
					);
			((JavascriptExecutor) driver).executeScript(
					"arguments[0].scrollIntoView({block:'center'});", orderReleaseChild);
			orderReleaseChild.click();
			System.out.println("✅ Clicked Home button using <oj-button> locator");
		} catch (Exception e1) {
			System.out.println("⚠️ Primary locator failed: " + e1.getMessage());
			try {
				// ✅ Fallback: use the <button> with aria-labelledby
				WebElement click = wait.until(
						ExpectedConditions.elementToBeClickable(
								By.xpath("//button[@aria-labelledby='homeButton_oj27|text']"))
						);
				((JavascriptExecutor) driver).executeScript(
						"arguments[0].scrollIntoView({block:'center'});", click);
				click.click();
				System.out.println("✅ Clicked Home button using fallback locator");
			} catch (Exception e2) {
				System.out.println("❌ Home button not found or not clickable: " + e2.getMessage());
			}
		}

		test.log(Status.PASS, "Clicked Home button");
	}
	@AfterClass
	public void tearDown() {
		if (driver != null) {
			driver.quit();
			test.log(Status.INFO, "Browser closed and WebDriver quit successfully.");
		}
		extent.flush(); // Save the report
	}
}
