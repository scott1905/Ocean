package rt.scripts.playwright;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.*;


import org.testng.Assert;
import org.testng.annotations.*;
import java.nio.file.*;
import java.util.*;
import java.util.Optional;

import com.aventstack.extentreports.*;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;

public class OccenPlaywright {
	// --- Playwright objects ---
	private Playwright playwright;
	private Browser browser;
	private BrowserContext context;
	private Page page;

	// --- Reporting ---
	ExtentReports extent;
	ExtentTest test;
	ExtentSparkReporter htmlReporter;

	public static class SharedData {
		public static String capturedOrderReleaseID;
	}

	@BeforeClass
	public void setUpAndLogin() throws Exception {
		// ===== Extent Report Setup =====
		htmlReporter = new ExtentSparkReporter("ExtentReport.html");
		extent = new ExtentReports();
		extent.attachReporter(htmlReporter);
		extent.setSystemInfo("OS", System.getProperty("os.name"));
		extent.setSystemInfo("User", System.getProperty("user.name"));
		extent.setSystemInfo("Browser", "Chrome (Playwright)");
		extent.setAnalysisStrategy(AnalysisStrategy.CLASS);
		test = extent.createTest("OCCEN Playwright Suite").assignAuthor("QA Team").assignCategory("Regression");

		// ===== Playwright Setup (assign to fields, no shadowing) =====
		this.playwright = Playwright.create();

		// Fresh user-data-dir is safer than deleting a few lock files
		Path userDataDir = Paths.get(System.getProperty("user.dir")).resolve("pw-user-data");
		if (Files.exists(userDataDir)) {
			Files.walk(userDataDir)
			.sorted(Comparator.reverseOrder())
			.forEach(p -> { try { Files.delete(p); } catch (Exception ignore) {} });
		}
		Files.createDirectories(userDataDir);

		BrowserType.LaunchPersistentContextOptions options = new BrowserType.LaunchPersistentContextOptions()
				.setChannel("chrome")
				.setHeadless(false)
				.setArgs(Arrays.asList(
						"--no-first-run",
						"--no-default-browser-check",
						"--disable-extensions",
						"--start-maximized"
						// IMPORTANT on Windows corp machines: avoid --no-sandbox
						))
				.setViewportSize(null);

		// Persistent context gives you a BrowserContext directly
		this.context = this.playwright.chromium().launchPersistentContext(userDataDir, options);
		this.browser = this.context.browser(); // optional, if you need it

		// Close any internal Chrome tabs (non-navigable)
		for (Page p : this.context.pages()) {
			String u = p.url();
			if (u.startsWith("chrome://") || u.startsWith("chrome-search://") || u.isEmpty()) {
				p.close();
			}
		}

		// Always create a fresh controllable page
		this.page = this.context.newPage();
		this.context.setDefaultTimeout(60_000);
		this.context.setDefaultNavigationTimeout(60_000);

		String url = "https://otmgtm-dev2-otmsaasna.otmgtm.us-phoenix-1.ocs.oraclecloud.com";
		test.log(Status.INFO, "Navigating to: " + url);
		this.page.bringToFront();
		this.page.navigate(url, new Page.NavigateOptions().setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
		System.out.println("Now at: " + this.page.url());

		// ===== Login Steps =====
		Locator signInBtn = this.page.locator(
				"xpath=/html/body/div/oj-idaas-signin-app-shell-rw/div[1]/div/div/div/div[2]/div/div/oj-module/div[1]/oj-module/div[1]/div/div[3]/div[1]/div/oj-button");
		signInBtn.waitFor();
		signInBtn.click();

		String user = System.getenv().getOrDefault("OTM_USER", "scott.kumar@unilever.com");
		String pass = System.getenv().getOrDefault("OTM_PASS", "Invent@appwrk11");

		Locator username = this.page.locator("[name=loginfmt]");
		username.waitFor();
		username.fill(user);
		this.page.locator("#idSIButton9").click();

		Locator password = this.page.locator("#i0118");
		password.waitFor();
		password.fill(pass);
		this.page.locator("#idSIButton9").click();

		Locator homeBtn = this.page.locator("#homeButton");
		homeBtn.waitFor();
		Assert.assertTrue(homeBtn.isVisible(), "Login failed or home button not found.");
		test.log(Status.PASS, "Login successful.");
		Thread.sleep(15000); // brief pause to ensure stability
	}
	private static final double TIMEOUT = 15_000; // ms

	// --- Priority 1 ---
	@Test(priority = 1)
	public void Order_Managment() { 
		waitForNetworkAndDom(page);

		// 2) Find the frame that contains the #label3 element (recursively searches all iframes)
		Frame targetFrame = findFirstFrameWithSelector(page, "#label3", TIMEOUT)
				.orElseThrow(() -> new RuntimeException("Could not find a frame containing #label3"));

		// 3) Click the label (#label3)
		Locator label3 = targetFrame.locator("#label3");
		label3.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(TIMEOUT));
		label3.click(new Locator.ClickOptions().setTimeout(TIMEOUT));

		// 4) Expand the tree node: //li[@id='sb_2_2']//ins[contains(@class,'oj-treeview-disclosure-icon')]
		Locator disclosure = targetFrame.locator("//li[@id='sb_2_2']//ins[contains(@class,'oj-treeview-disclosure-icon')]");
		disclosure.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(TIMEOUT));
		disclosure.click(new Locator.ClickOptions().setTimeout(TIMEOUT));

		// 5) Click the third child "Order Release": //li[@id='sb_2_2_2']//span[text()='Order Release']
		Locator orderRelease = targetFrame.locator("//li[@id='sb_2_2_2']");
		orderRelease.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(TIMEOUT));
		orderRelease.click(new Locator.ClickOptions().setTimeout(TIMEOUT));

		// Optional: verification that the click led somewhere meaningful (adjust selector to your app)
		// targetFrame.getByRole(AriaRole.HEADING, new Frame.GetByRoleOptions().setName("Order Release")).waitFor();
	}

	private static void waitForNetworkAndDom(Page page) {
		// Playwright auto-waits on actions, but a one-time page settle is handy:
		// Wait for DOM content and then network quiet.
		page.waitForLoadState(LoadState.DOMCONTENTLOADED);
		page.waitForLoadState(LoadState.NETWORKIDLE);
	}

	/**
	 * Recursively searches all frames for one that contains an element matching `selector`.
	 * Uses a BFS so we find higher-level frames first.
	 */
	private static Optional<Frame> findFirstFrameWithSelector(Page page, String selector, double timeoutMs) {
		Deque<Frame> q = new ArrayDeque<>(page.frames());
		long deadline = System.nanoTime() + (long)(timeoutMs * 1_000_000);

		while (!q.isEmpty()) {
			Frame f = q.removeFirst();
			try {
				// Try a very short wait in this frame; if it appears we’re done.
				f.waitForSelector(selector,
						new Frame.WaitForSelectorOptions()
						.setTimeout(500) // quick probe per frame
						.setState(WaitForSelectorState.ATTACHED));
				return Optional.of(f);
			} catch (PlaywrightException ignored) {
				// Not in this frame (or not yet attached)
			}
			// Enqueue children to search deeper
			q.addAll(f.childFrames());

			// Respect overall timeout
			if (System.nanoTime() > deadline) break;
		}

		// If quick probes didn’t find it, do a second, slower pass in case it just needed more time.
		q.clear();
		q.addAll(page.frames());
		while (!q.isEmpty()) {
			Frame f = q.removeFirst();
			try {
				f.waitForSelector(selector,
						new Frame.WaitForSelectorOptions()
						.setTimeout(3_000) // slower pass
						.setState(WaitForSelectorState.ATTACHED));
				return Optional.of(f);
			} catch (PlaywrightException ignored) { }
			q.addAll(f.childFrames());
			if (System.nanoTime() > deadline) break;
		}

		return Optional.empty();
	}

	// --- Example harness (if you want to run this standalone) ---
	public static void main(String[] args) {
		try (Playwright pw = Playwright.create()) {
			Browser browser = pw.chromium().launch(new BrowserType.LaunchOptions().setHeadless(false));
			BrowserContext ctx = browser.newContext(new Browser.NewContextOptions()
					.setViewportSize(1280, 800));
			Page page = ctx.newPage();

			// TODO: navigate to your AUT
			// page.navigate("https://your-app.example.com");

			run(page);
		}
	}
	private static void run(Page page2) {
		// TODO Auto-generated method stub

	}






	private Locator $(String string) {
		// TODO Auto-generated method stub
		return null;
	}


	private void ensureAppReady() {
		// TODO Auto-generated method stub

	}


	private boolean visible(Locator orderMgmt, int i) {
		// TODO Auto-generated method stub
		return false;
	}


	private Locator on(Frame mainFrame, String string, Page page2) {
		// TODO Auto-generated method stub
		return null;
	}


	private Frame pickMainFrameIfAny(Page page2) {
		// TODO Auto-generated method stub
		return null;
	}


	private void waitForAppShell(Page page2) {
		// TODO Auto-generated method stub

	}
	private static final double TIMEOUT1 = 60_000; // ms

	@Test(priority = 2)
	public void In_order_release() throws InterruptedException {

		log("In_order_release test started");
		waitAppReady(page);
		Thread.sleep(3000); // brief pause to ensure stability
		// 1) Resolve mainIFrame
		Frame main = getMainIFrame(page)
				.orElseThrow(() -> new RuntimeException("mainIFrame not found or not yet attached"));
		Thread.sleep(3000); // brief pause to ensure stability
		// 2) Find the frame (main or a child) that contains the Indicator dropdown
		String indicatorSel = "select[name='order_release/indicator'][aria-label='Indicator']";
		Frame indicatorFrame = findDescendantFrameWithSelector(main, indicatorSel, 16_000)
				.orElseThrow(() -> new RuntimeException("Indicator dropdown not found in mainIFrame or its child iframes"));
		Thread.sleep(3000); // brief pause to ensure stability
		// 3) Interact with the Indicator dropdown (select second option)
		Locator indicator = indicatorFrame.locator(indicatorSel);
		indicator.waitFor(new Locator.WaitForOptions().setTimeout(25_000).setState(WaitForSelectorState.VISIBLE));
		indicator.scrollIntoViewIfNeeded();
		// Playwright index is zero-based. 1 = second option.
		indicator.selectOption(new SelectOption().setIndex(1));
		log("Dropdown clicked and option selected.");
		Thread.sleep(3000); // brief pause to ensure stability
		// 4) Click Search
		// Try in mainIFrame first; if not found there, search the child frames; final fallback: page
		BySelectorGroup searchSelectors = new BySelectorGroup(
				// Native button inside oj-button
				"oj-button:has-text('Search') >> button",
				// Common fallbacks
				"button[aria-label='Search']",
				"#search",
				"#search_button",
				"button:has-text('Search')"
				);

		// (a) Try in mainIFrame
		if (!clickFirstMatch(main, searchSelectors, 14_000)) {
			// (b) Try in child frames of mainIFrame
			boolean clicked = false;
			for (Frame child : breadthFirst(main)) {
				if (child == main) continue;
				if (clickFirstMatch(child, searchSelectors, 8_000)) { clicked = true; break; }
			}
			// (c) Fallback: try at page level
			if (!clicked) {
				if (!clickFirstMatch(page, searchSelectors, 8_000)) {
					// Diagnostics before failing
					page.screenshot(new Page.ScreenshotOptions().setPath(Paths.get("debug_search_full.png")).setFullPage(true));
					throw new RuntimeException("Search button not found in mainIFrame, its child frames, or page.");
				}
			}
		}

		log("Search button clicked successfully.");
	}

	// ---------------- helpers ----------------

	private static void waitAppReady(Page page) {
	    page.waitForLoadState(LoadState.DOMCONTENTLOADED);
	    page.waitForTimeout(2000); // ✅ Reliable pause for Oracle OTM
	}

	/** Locate the iframe element with id=mainIFrame and return its content Frame. */
	private static Optional<Frame> getMainIFrame(Page page) {
		try {
			ElementHandle iframeEl = page.waitForSelector("iframe#mainIFrame",
					new Page.WaitForSelectorOptions().setTimeout(15_000));
			if (iframeEl == null) return Optional.empty();
			Frame f = iframeEl.contentFrame();
			return Optional.ofNullable(f);
		} catch (PlaywrightException e) {
			return Optional.empty();
		}
	}

	/** BFS over a root frame and its descendants. */
	private static List<Frame> breadthFirst(Frame root) {
		List<Frame> order = new ArrayList<>();
		Deque<Frame> q = new ArrayDeque<>();
		q.add(root);
		while (!q.isEmpty()) {
			Frame f = q.removeFirst();
			order.add(f);
			q.addAll(f.childFrames());
		}
		return order;
	}

	/** Find (within root + descendants) the first frame containing selector. */
	private static Optional<Frame> findDescendantFrameWithSelector(Frame root, String selector, double timeoutMs) {
		long deadline = System.nanoTime() + (long) (timeoutMs * 1_000_000);

		// Fast pass
		for (Frame f : breadthFirst(root)) {
			try {
				f.waitForSelector(selector, new Frame.WaitForSelectorOptions().setTimeout(400));
				return Optional.of(f);
			} catch (PlaywrightException ignore) { }
			if (System.nanoTime() > deadline) return Optional.empty();
		}

		// Slow pass
		for (Frame f : breadthFirst(root)) {
			try {
				f.waitForSelector(selector, new Frame.WaitForSelectorOptions().setTimeout(2_500));
				return Optional.of(f);
			} catch (PlaywrightException ignore) { }
			if (System.nanoTime() > deadline) break;
		}
		return Optional.empty();
	}

	/** Tries each selector in order and clicks the first visible & enabled match within the frame. */
	private static boolean clickFirstMatch(Frame frame, BySelectorGroup selectors, double timeoutMs) {
		for (String cssOrXPath : selectors.list) {
			Locator loc = frame.locator(cssOrXPath).first();
			try {
				loc.waitFor(new Locator.WaitForOptions().setTimeout(timeoutMs).setState(WaitForSelectorState.VISIBLE));
			} catch (PlaywrightException ignored) {
				continue; // try next selector
			}

			// Ensure clickable (not hidden/disabled/covered)
			try {
				// If button-like, wait until enabled
				frame.waitForFunction("el => !(el.disabled || el.getAttribute('aria-disabled') === 'true')", loc.elementHandle(),
						new Frame.WaitForFunctionOptions().setTimeout(2_000));
			} catch (PlaywrightException ignored) { /* not a button or no 'disabled' attr; proceed */ }

			try {
				loc.scrollIntoViewIfNeeded();
				loc.click(new Locator.ClickOptions().setTimeout(6_000));
				return true;
			} catch (PlaywrightException clickFail) {
				// Try force click only as a last resort for shadow overlay quirks
				try {
					loc.click(new Locator.ClickOptions().setTimeout(3_000).setForce(true));
					return true;
				} catch (PlaywrightException ignored) {
					// move on to next selector
				}
			}
		}
		return false;
	}

	/** Page-level overload. */
	private static boolean clickFirstMatch(Page page, BySelectorGroup selectors, double timeoutMs) {
		for (String cssOrXPath : selectors.list) {
			Locator loc = page.locator(cssOrXPath).first();
			try {
				loc.waitFor(new Locator.WaitForOptions().setTimeout(timeoutMs).setState(WaitForSelectorState.VISIBLE));
			} catch (PlaywrightException ignored) {
				continue;
			}
			try {
				page.waitForFunction("el => !(el.disabled || el.getAttribute('aria-disabled') === 'true')", loc.elementHandle(),
						new Page.WaitForFunctionOptions().setTimeout(2_000));
			} catch (PlaywrightException ignored) { }

			try {
				loc.scrollIntoViewIfNeeded();
				loc.click(new Locator.ClickOptions().setTimeout(6_000));
				return true;
			} catch (PlaywrightException e) {
				try {
					loc.click(new Locator.ClickOptions().setTimeout(3_000).setForce(true));
					return true;
				} catch (PlaywrightException ignored) { }
			}
		}
		return false;
	}

	private static void log(String s) { System.out.println(s); }

	// Utility holder for your selector list (CSS or XPath are both fine here)
	private static class BySelectorGroup {
		final List<String> list = new ArrayList<>();
		BySelectorGroup(String... selectors) {
			list.addAll(Arrays.asList(selectors));
		}
	}







	// ----------------- helpers -----------------

	private static void ensureAppReady(Page page) {
		page.waitForLoadState(LoadState.DOMCONTENTLOADED);
		page.waitForLoadState(LoadState.NETWORKIDLE);
	}

	/** Breadth-first search all frames for a frame whose textContent matches the pattern at least once. */




	/** Helps debug: dumps visible headings text quickly. */
	private static void dumpHeadings(Frame frame, String filename) {
		try {
			String js =
					"(() => {" +
							" const hs = Array.from(document.querySelectorAll('h1,h2,h3,h4,h5,h6,[role=\"heading\"]')); " +
							" return hs.map(h => ({tag: h.tagName || 'role=heading', name: (h.innerText||'').trim()}));" +
							"})()";
			Object res = frame.evaluate(js);
			java.nio.file.Files.writeString(Paths.get(filename), String.valueOf(res));
		} catch (Exception ignored) { }
	}








	// --- Priority 3 ---
	@Test(priority = 3)
	public void Order_Releases() throws InterruptedException {
	    System.out.println("✅ Order_Releases test started");
	    waitAppReady(page);
	    // Avoid waiting for page-level load state
	    page.waitForSelector("iframe", new Page.WaitForSelectorOptions().setTimeout(15_000));

	    // Switch to target frame (first frame or #mainIFrame)
	    Frame target = getPreferredFrame1(page)
	        .orElseThrow(() -> new RuntimeException("No suitable iframe found"));
	    Thread.sleep(3000); // brief pause to ensure stability
	    // Wait for the table to appear
	    target.waitForSelector("table", new Frame.WaitForSelectorOptions().setTimeout(15_000));

	    // Step 1: Click link 'ULA/NA' like Selenium does
	    Thread.sleep(3000); // brief pause to ensure stability
	    Locator orderLink = target.locator("//a[contains(text(), 'ULA/NA')]").first();
	    orderLink.scrollIntoViewIfNeeded();
	    orderLink.waitFor(new Locator.WaitForOptions().setTimeout(5000).setState(WaitForSelectorState.VISIBLE));
	    orderLink.click(new Locator.ClickOptions().setTimeout(5000));
	    System.out.println("✅ Clicked ULA/NA link");

	    // Step 2: Click NEW button using exact XPath from Selenium
	    page.waitForTimeout(1500); // small settle; DO NOT use NETWORKIDLE for Oracle OTM

	 // Try to get #mainIFrame again; if not found, fall back to previous target
	 Frame scope = getPreferredFrame1(page).orElse(target);Thread.sleep(3000); // brief pause to ensure stability

	 // Find a frame (scope or its descendants) that contains a “New” button
	 List<String> newSelectors = Arrays.asList(
	     // Oracle JET host + inner native button
	     "oj-button:has-text('New') >> button",
	     "oj-button:has-text('NEW') >> button",
	     // Common fallbacks
	     "button[aria-label='New']",
	     "button:has-text('New')",
	     "button:has-text('NEW')",
	     // Very last-resort: case-insensitive text engine
	     ":text-matches('(?i)^\\s*new\\s*$')"
	 );
	 Thread.sleep(3000); // brief pause to ensure stability
	 // BFS search across scope + descendants
	 Frame btnFrame = findFirstFrameWithAnySelector(scope, newSelectors, 12_000)
	     .orElseThrow(() -> new RuntimeException("Could not find a frame containing the NEW button after ULA/NA click"));

	 // Click the first matching selector that’s visible/enabled
	 boolean clickedNew = clickFirstMatchInFrame(btnFrame, newSelectors, 8_000);
	 if (!clickedNew) {
	   // Fallback: try across *all* frames on the page (just in case the button rendered outside main)
	   Optional<Frame> any = findFirstFrameWithAnySelector(page.mainFrame(), newSelectors, 8_000);
	   if (any.isPresent()) {
	     if (!clickFirstMatchInFrame(any.get(), newSelectors, 6_000)) {
	       throw new RuntimeException("NEW button became visible but couldn’t be clicked.");
	     }
	   } else {
	     // FINAL last-resort: absolute XPATH but **within the btnFrame**, not page
	     Locator lastResort = btnFrame.locator("xpath=/html/body/div[1]/table/tbody/tr/td[2]/table/tbody/tr/td[3]/div/button");
	     lastResort.scrollIntoViewIfNeeded();
	     lastResort.click(new Locator.ClickOptions().setTimeout(5_000));
	   }
	 }

	 System.out.println("✅ Clicked on NEW button in Order Releases page");
	    page.screenshot(new Page.ScreenshotOptions()
	            .setPath(Paths.get("debug_order_releases.png"))
	            .setFullPage(true));
	    for (Frame f : page.frames()) {
	        System.out.println("Frame: " + f.name() + ", URL: " + f.url());
	    }

	}


	// ---------------- helpers ----------------

	private static Optional<Frame> findFirstFrameWithAnySelector(Frame root, List<String> selectors, double timeoutMs) {
		  long deadline = System.nanoTime() + (long)(timeoutMs * 1_000_000);
		  Deque<Frame> q = new ArrayDeque<>();
		  q.add(root);
		  while (!q.isEmpty()) {
		    Frame f = q.removeFirst();
		    for (String s : selectors) {
		      try {
		        f.waitForSelector(s, new Frame.WaitForSelectorOptions().setTimeout(500).setState(WaitForSelectorState.ATTACHED));
		        return Optional.of(f);
		      } catch (PlaywrightException ignored) {}
		    }
		    q.addAll(f.childFrames());
		    if (System.nanoTime() > deadline) break;
		  }
		  return Optional.empty();
		}

		private static boolean clickFirstMatchInFrame(Frame frame, List<String> selectors, double timeoutMs) {
		  for (String s : selectors) {
		    Locator loc = frame.locator(s).first();
		    try {
		      loc.waitFor(new Locator.WaitForOptions().setTimeout(2500).setState(WaitForSelectorState.VISIBLE));
		      // best-effort enabled check
		      try {
		        frame.waitForFunction("el => !(el.disabled || el.getAttribute('aria-disabled') === 'true')",
		          loc.elementHandle(), new Frame.WaitForFunctionOptions().setTimeout(1500));
		      } catch (PlaywrightException ignored) {}
		      loc.scrollIntoViewIfNeeded();
		      loc.click(new Locator.ClickOptions().setTimeout((int) timeoutMs));
		      return true;
		    } catch (PlaywrightException ignore) { /* try next */ }
		  }
		  return false;
		}

	private static void waitPageReady1(Page page) {
		page.waitForLoadState(LoadState.DOMCONTENTLOADED);
		// Do NOT wait for NETWORKIDLE — OTM never quiets down
		page.waitForTimeout(2000); // Optional: small pause for UI stabilization
	}
	private static void waitAppReady2(Page page) {
	    page.waitForLoadState(LoadState.DOMCONTENTLOADED);
	    page.waitForTimeout(2000); // Enough for Oracle JET to render key UI
	}

	/** Prefer #mainIFrame; else use the first <iframe> on the page. */
	private static java.util.Optional<Frame> getPreferredFrame1(Page page) {
		try {
			ElementHandle mainEl = page.waitForSelector("iframe#mainIFrame",
					new Page.WaitForSelectorOptions().setTimeout(5000));
			if (mainEl != null) {
				Frame f = mainEl.contentFrame();
				if (f != null) return java.util.Optional.of(f);
			}
		} catch (PlaywrightException ignored) { }

		try {
			ElementHandle firstIframe = page.waitForSelector("iframe",
					new Page.WaitForSelectorOptions().setTimeout(5000));
			if (firstIframe != null) {
				Frame f = firstIframe.contentFrame();
				if (f != null) return java.util.Optional.of(f);
			}
		} catch (PlaywrightException ignored) { }

		return java.util.Optional.empty();
	}

	private static boolean isVisible1(Locator loc, double timeoutMs) {
		try {
			loc.waitFor(new Locator.WaitForOptions()
					.setState(WaitForSelectorState.VISIBLE)
					.setTimeout(timeoutMs));
			return true;
		} catch (PlaywrightException e) {
			return false;
		}
	}

	/** Clicks a locator with scroll and enabled wait; tries force click as a last resort. */
	private static void ensureVisibleAndClick1(Locator loc, String screenshotOnFail) {
		try {
			loc.scrollIntoViewIfNeeded();
			loc.waitFor(new Locator.WaitForOptions()
					.setTimeout(TIMEOUT)
					.setState(WaitForSelectorState.VISIBLE));

			// If it's a button-like element, wait until enabled (not disabled/aria-disabled)
			try {
				Page p = loc.page();
				p.waitForFunction("el => !(el.disabled || el.getAttribute('aria-disabled') === 'true')",
						loc.elementHandle(), new Page.WaitForFunctionOptions().setTimeout(1500));
			} catch (PlaywrightException ignored) { }

			loc.click(new Locator.ClickOptions().setTimeout(TIMEOUT));
		} catch (PlaywrightException e) {
			try {
				loc.click(new Locator.ClickOptions().setTimeout(4000).setForce(true));
			} catch (PlaywrightException e2) {
				Page page = loc.page();
				page.screenshot(new Page.ScreenshotOptions().setPath(Paths.get(screenshotOnFail)).setFullPage(true));
				throw e2;
			}
		}
	}

	/** Click a locator safely; returns true if clicked. */
	private static boolean clickSafely(Frame frame, Locator loc, double timeoutMs) {
		try {
			loc.scrollIntoViewIfNeeded();
			loc.waitFor(new Locator.WaitForOptions()
					.setTimeout(timeoutMs)
					.setState(WaitForSelectorState.VISIBLE));
			try {
				frame.waitForFunction("el => !(el.disabled || el.getAttribute('aria-disabled') === 'true')",
						loc.elementHandle(), new Frame.WaitForFunctionOptions().setTimeout(1500));
			} catch (PlaywrightException ignored) { }
			loc.click(new Locator.ClickOptions().setTimeout(timeoutMs));
			return true;
		} catch (PlaywrightException e) {
			try {
				loc.click(new Locator.ClickOptions().setTimeout(2000).setForce(true));
				return true;
			} catch (PlaywrightException ignored) {
				return false;
			}
		}
	}

	/** Clicks the first selector that resolves to a visible, clickable element within a Frame. */
	private static boolean clickFirstMatch1(Frame frame, List<String> selectors, double timeoutMs) {
		for (String s : selectors) {
			Locator loc = frame.locator(s).first();
			if (!isVisible1(loc, 800)) continue;
			if (clickSafely(frame, loc, timeoutMs)) return true;
		}
		return false;
	}

	/** Page-level overload. */
	private static boolean clickFirstMatch1(Page page, List<String> selectors, double timeoutMs) {
		for (String s : selectors) {
			Locator loc = page.locator(s).first();
			try {
				loc.waitFor(new Locator.WaitForOptions()
						.setState(WaitForSelectorState.VISIBLE)
						.setTimeout(800));
			} catch (PlaywrightException ignored) { continue; }

			try {
				page.waitForFunction("el => !(el.disabled || el.getAttribute('aria-disabled') === 'true')",
						loc.elementHandle(), new Page.WaitForFunctionOptions().setTimeout(1500));
			} catch (PlaywrightException ignored) { }

			try {
				loc.scrollIntoViewIfNeeded();
				loc.click(new Locator.ClickOptions().setTimeout(timeoutMs));
				return true;
			} catch (PlaywrightException e) {
				try {
					loc.click(new Locator.ClickOptions().setTimeout(2000).setForce(true));
					return true;
				} catch (PlaywrightException ignored) { }
			}
		}
		return false;
	}



	// ---------------- helpers ----------------

	private static void waitPageReady(Page page) {
		page.waitForLoadState(LoadState.DOMCONTENTLOADED);
		page.waitForLoadState(LoadState.NETWORKIDLE);
	}

	/** Prefer #mainIFrame; else use the first <iframe> on the page. */
	private static Optional<Frame> getPreferredFrame(Page page) {
		// Try #mainIFrame
		try {
			ElementHandle mainEl = page.waitForSelector("iframe#mainIFrame",
					new Page.WaitForSelectorOptions().setTimeout(5_000));
			if (mainEl != null) {
				Frame f = mainEl.contentFrame();
				if (f != null) return Optional.of(f);
			}
		} catch (PlaywrightException ignored) { }

		// Fallback: first iframe
		try {
			ElementHandle firstIframe = page.waitForSelector("iframe",
					new Page.WaitForSelectorOptions().setTimeout(5_000));
			if (firstIframe != null) {
				Frame f = firstIframe.contentFrame();
				if (f != null) return Optional.of(f);
			}
		} catch (PlaywrightException ignored) { }

		return Optional.empty();
	}

	private static boolean isVisible(Locator loc, double timeoutMs) {
		try {
			loc.waitFor(new Locator.WaitForOptions()
					.setState(WaitForSelectorState.VISIBLE)
					.setTimeout(timeoutMs));
			return true;
		} catch (PlaywrightException e) {
			return false;
		}
	}

	private static void ensureVisibleAndClick(Locator loc, String screenshotOnFail) {
		try {
			loc.scrollIntoViewIfNeeded();
			loc.waitFor(new Locator.WaitForOptions()
					.setTimeout(TIMEOUT)
					.setState(WaitForSelectorState.VISIBLE));
			loc.click(new Locator.ClickOptions().setTimeout(TIMEOUT));
		} catch (PlaywrightException e) {
			try {
				// Force as last resort (overlay quirks)
				loc.click(new Locator.ClickOptions().setTimeout(4_000).setForce(true));
			} catch (PlaywrightException e2) {
				// Aid debugging
				Page page = loc.page();
				page.screenshot(new Page.ScreenshotOptions().setPath(Paths.get(screenshotOnFail)).setFullPage(true));
				throw e2;
			}
		}
	}

	/** Clicks the first selector that resolves to a visible, clickable element within a Frame. */
	private static boolean clickFirstMatch(Frame frame, List<String> selectors, double timeoutMs) {
		for (String s : selectors) {
			Locator loc = toLocator(frame, s).first();
			if (!isVisible1(loc, 1_000)) continue;
			try {
				// If it's a button, wait until enabled (not disabled/aria-disabled)
				frame.waitForFunction("el => !(el.disabled || el.getAttribute('aria-disabled') === 'true')",
						loc.elementHandle(), new Frame.WaitForFunctionOptions().setTimeout(1_500));
			} catch (PlaywrightException ignored) { }
			try {
				loc.scrollIntoViewIfNeeded();
				loc.click(new Locator.ClickOptions().setTimeout(timeoutMs));
				return true;
			} catch (PlaywrightException ignored) {
				try {
					loc.click(new Locator.ClickOptions().setTimeout(2_000).setForce(true));
					return true;
				} catch (PlaywrightException ignored2) { }
			}
		}
		return false;
	}

	/** Page-level overload. */
	private static boolean clickFirstMatch(Page page, List<String> selectors, double timeoutMs) {
		for (String s : selectors) {
			Locator loc = toLocator(page, s).first();
			if (!isVisible1(loc, 1_000)) continue;
			try {
				page.waitForFunction("el => !(el.disabled || el.getAttribute('aria-disabled') === 'true')",
						loc.elementHandle(), new Page.WaitForFunctionOptions().setTimeout(1_500));
			} catch (PlaywrightException ignored) { }
			try {
				loc.scrollIntoViewIfNeeded();
				loc.click(new Locator.ClickOptions().setTimeout(timeoutMs));
				return true;
			} catch (PlaywrightException ignored) {
				try {
					loc.click(new Locator.ClickOptions().setTimeout(2_000).setForce(true));
					return true;
				} catch (PlaywrightException ignored2) { }
			}
		}
		return false;
	}

	/** Accepts either CSS/XPath or role selector strings. */
	private static Locator toLocator(Frame frame, String selector) {
		if (selector.startsWith("role=")) {
			// role=button[name='New'] style
			return roleLocator(frame, selector);
		}
		// CSS or XPath both supported by .locator()
		return frame.locator(selector);
	}

	private static Locator toLocator(Page page, String selector) {
		if (selector.startsWith("role=")) {
			return roleLocator((Frame) page, selector);
		}
		return page.locator(selector);
	}

	/** Minimal parser for role selectors like role=button[name='New'] */
	private static Locator roleLocator(Frame frame, String roleSelector) {
		// Very small parser: role=button[name='New']  -> role=button, name='New'
		String rs = roleSelector.substring("role=".length());
		String role = rs.split("\\[", 2)[0].trim();
		String name = null;
		if (rs.contains("name=")) {
			int i = rs.indexOf("name=");
			int start = rs.indexOf('\'', i) >= 0 ? rs.indexOf('\'', i) + 1 : rs.indexOf('"', i) + 1;
			int end = rs.indexOf('\'', start) >= 0 ? rs.indexOf('\'', start) : rs.indexOf('"', start);
			name = rs.substring(start, end);
		}
		if (frame instanceof Page) {
			Page p = (Page) frame;
			if (name != null) {
				return p.getByRole(toAriaRole(role), new Page.GetByRoleOptions().setName(name));
			}
			return p.getByRole(toAriaRole(role));
		} else if (frame instanceof Frame) {
			Frame f = (Frame) frame;
			if (name != null) {
				return f.getByRole(toAriaRole(role), new Frame.GetByRoleOptions().setName(name));
			}
			return f.getByRole(toAriaRole(role));
		}
		throw new IllegalArgumentException("Unsupported Locatable for roleLocator");
	}

	private static AriaRole toAriaRole(String s) {
		switch (s.toLowerCase()) {
		case "button": return AriaRole.BUTTON;
		case "link": return AriaRole.LINK;
		case "heading": return AriaRole.HEADING;
		default: throw new IllegalArgumentException("Unsupported role: " + s);
		}
	}

	/** Marker interface to allow roleLocator over Page/Frame. */
	private interface Locatable { }
	private static final double TIMEOUT3 = 20_000; // ms

  // --- Priority 4 ---
@Test(priority = 4)
public void Order_Manager_Playwright() {
  test.log(Status.INFO, "Order_Manager test started");

  // 0) Light-weight settle (OTM/Oracle JET rarely hits NETWORKIDLE)
  page.waitForLoadState(LoadState.DOMCONTENTLOADED);
  page.waitForSelector("iframe", new Page.WaitForSelectorOptions().setTimeout(15_000));

  // ===== 1) Domain Name dropdown -> "ULA/NA" =====
  String domainSel = "select[aria-label='Domain Name']";
  Frame domainFrame = findFirstFrameWithSelector(page, domainSel, 12_000)
      .orElseThrow(() -> new RuntimeException("Domain Name dropdown not found in any frame"));

  Locator domain = domainFrame.locator(domainSel);
  domain.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(8_000));
  domain.scrollIntoViewIfNeeded();
  domain.selectOption(new SelectOption().setLabel("ULA/NA"));

  // Verify selected text (closest equivalent to Selenium Select#getFirstSelectedOption)
  String domainSelectedText = domain.locator("option:checked").innerText().trim();
  test.log(Status.PASS, "Selected Domain: " + domainSelectedText);

  // ===== 2) Dates =====
  java.time.LocalDate today = java.time.LocalDate.now();

  String earlyPickup = today.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")) + " 00:00:00";
  String latePickup  = today.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")) + " 23:59:00";
  String earlyDelivery = today.plusDays(10).format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")) + " 00:00:00";
  String lateDelivery  = today.plusDays(10).format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")) + " 23:59:00";

  // Each input may live in a different iframe; resolve frame per field robustly.

  // Early Pickup Date
  fillAriaDate(page, "Early Pickup Date", earlyPickup);
  test.log(Status.PASS, "Entered Early Pickup Date: " + earlyPickup);

  // Late Pickup Date
  fillAriaDate(page, "Late Pickup Date", latePickup);
  test.log(Status.PASS, "Entered Late Pickup Date: " + latePickup);

  // Early Delivery Date
  fillAriaDate(page, "Early Delivery Date", earlyDelivery);
  test.log(Status.PASS, "Entered Early Delivery Date: " + earlyDelivery);

  // Late Delivery Date
  fillAriaDate(page, "Late Delivery Date", lateDelivery);
  test.log(Status.PASS, "Entered Late Delivery Date: " + lateDelivery);

  // ===== 3) Order Configuration -> "SHIP_UNITS" =====
  String orderCfgSel = "select[aria-label='Order Configuration']";
  Frame cfgFrame = findFirstFrameWithSelector(page, orderCfgSel, 12_000)
      .orElseThrow(() -> new RuntimeException("Order Configuration dropdown not found in any frame"));

  Locator orderCfg = cfgFrame.locator(orderCfgSel);
  orderCfg.waitFor(new Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE).setTimeout(8_000));
  orderCfg.scrollIntoViewIfNeeded();
  orderCfg.selectOption(new SelectOption().setLabel("SHIP_UNITS"));
  test.log(Status.PASS, "Selected Order Configuration: " +
      orderCfg.locator("option:checked").innerText().trim());

  // ===== 4) Source Location ID (Selenium tried id first, then aria-label fallback) =====
  // First try exact id = "order_release/source/xid" (use XPath because of slashes in id)
  Optional<Frame> sourceIdFrame = findFirstFrameWithSelector(page, "xpath=//*[@id='order_release/source/xid']", 8_000);
  if (sourceIdFrame.isPresent()) {
    Locator src = sourceIdFrame.get().locator("xpath=//*[@id='order_release/source/xid']");
    src.waitFor(new Locator.WaitForOptions().setTimeout(5_000).setState(WaitForSelectorState.VISIBLE));
    src.scrollIntoViewIfNeeded();
    clearAndType(src, "USO4", true);
  } else {
    // Fallback by aria-label
    String srcAriaSel = "input[aria-label='Source Location ID']";
    Frame srcAriaFrame = findFirstFrameWithSelector(page, srcAriaSel, 8_000)
        .orElseThrow(() -> new RuntimeException("Neither id=order_release/source/xid nor 'Source Location ID' found in any frame"));
    Locator src = srcAriaFrame.locator(srcAriaSel);
    src.waitFor(new Locator.WaitForOptions().setTimeout(5_000).setState(WaitForSelectorState.VISIBLE));
    src.scrollIntoViewIfNeeded();
    clearAndType(src, "USO4", true);
  }

  test.log(Status.PASS, "Entered Source Location ID: USO4");

  // Optional: small stabilization + diagnostics
  page.waitForTimeout(500);
  page.screenshot(new Page.ScreenshotOptions().setPath(Paths.get("order_manager_pw.png")).setFullPage(true));
}

@Test(priority = 5)
public void Order_Manager1_Playwright() throws InterruptedException {
  test.log(Status.INFO, "Order_Manager1 test started");

  // --- Accept any alert/dialog (equiv. to handleAlert(driver, true)) ---
  page.onceDialog(dialog -> {
    try { dialog.accept(); } catch (Exception ignored) {}
  });

  // Settle just a bit; Oracle JET doesn’t truly idle
  page.waitForLoadState(LoadState.DOMCONTENTLOADED);
  page.waitForSelector("iframe", new Page.WaitForSelectorOptions().setTimeout(15_000));

  // ===== 1) DESTINATION LOCATION FIELD =====
  // We’ll try multiple selectors: name, id (via XPath), and aria-label fallback.
  String byNameSel = "input[name='order_release/destination/xid']";
  String byIdXPath = "xpath=//*[@id='order_release/destination/xid']";
  String byAriaSel = "input[aria-label='Destination Location ID']";

  Locator destInput = null;

  // Try by name
  Optional<Frame> fName = findFirstFrameWithSelector(page, byNameSel, 8_000);
  if (fName.isPresent()) {
    destInput = fName.get().locator(byNameSel).first();
  } else {
    // Try by id (XPath because id contains slashes)
    Optional<Frame> fId = findFirstFrameWithSelector(page, byIdXPath, 8_000);
    if (fId.isPresent()) {
      destInput = fId.get().locator(byIdXPath).first();
    } else {
      // Fallback: aria-label
      Optional<Frame> fAria = findFirstFrameWithSelector(page, byAriaSel, 8_000);
      if (fAria.isPresent()) {
        destInput = fAria.get().locator(byAriaSel).first();
      }
    }
  }

  if (destInput == null) {
    throw new RuntimeException("Destination Location input not found in any frame (by name/id/aria-label).");
  }

  // Make it visible & fill; use gentle typing and TAB to commit in Oracle forms
  destInput.waitFor(new Locator.WaitForOptions().setTimeout(15_000).setState(WaitForSelectorState.VISIBLE));
  destInput.scrollIntoViewIfNeeded();
  clearAndType(destInput, "USB6", true);    // first try like your Selenium flow
  // If you want to mimic your fallback overwrite with "USC6", uncomment below:
  // clearAndType(destInput, "USC6", true);

  test.log(Status.PASS, "Destination Location entered");

  // ===== 2) Click the two buttons (absolute XPaths in Selenium) =====
  // /html/body/div[6]/table/tbody/tr/td[2]/div/button
  clickXPathAnywhere1(page, "/html/body/div[6]/table/tbody/tr/td[2]/div/button", 15_000);
  Thread.sleep(4000);

  // /html/body/form/div[3]/div/div[1]/table/tbody/tr[5]/td/table/tbody/tr/td/div/button
  clickXPathAnywhere1(page, "/html/body/form/div[3]/div/div[1]/table/tbody/tr[5]/td/table/tbody/tr/td/div/button", 15_000);
  Thread.sleep(4000);

  // Optional diagnostics
  page.screenshot(new Page.ScreenshotOptions().setPath(Paths.get("order_manager1_pw.png")).setFullPage(true));
}



@Test(priority = 6)
public void Order_Manager2_Playwright() {
  test.log(Status.INFO, "Order_Manager2 test started");

  // Light settle; Oracle JET rarely hits true NETWORKIDLE
  page.waitForLoadState(LoadState.DOMCONTENTLOADED);
  page.waitForSelector("iframe", new Page.WaitForSelectorOptions().setTimeout(15_000));

  // Transport Handling Unit
  fillAriaInputAcrossFrames11(page, "Transport Handling Unit", "BOX_000000000084172226", true);

  // Transport Handling Unit Count
  fillAriaInputAcrossFrames11(page, "Transport Handling Unit Count", "20", false);

  // Total Gross Weight
  fillAriaInputAcrossFrames11(page, "Total Gross Weight", "20400", false);

  // Scroll to bottom (page-level is fine)
  page.evaluate("window.scrollTo({ top: document.body.scrollHeight, behavior: 'auto' })");

  // Click "New Line Item" (class + exact text)
  boolean clicked = clickAcrossFrames11(
      page,
      Arrays.asList("button.enButton:has-text('New Line Item')", "button:has-text('New Line Item')"),
      15_000
  );
  if (!clicked) throw new RuntimeException("Failed to click 'New Line Item'.");

  test.log(Status.PASS, "Clicked New Line Item button in Order Manager");

  // Optional diagnostics
  page.screenshot(new Page.ScreenshotOptions().setPath(Paths.get("order_manager2_pw.png")).setFullPage(true));
}

/* ----------------------- helpers ----------------------- */

/** Finds an input by aria-label across frames and fills it. Optionally presses Tab to commit. */
private void fillAriaInputAcrossFrames11(Page page, String ariaLabel, String value, boolean tabOut) {
  String sel = "input[aria-label='" + ariaLabel + "']";

  Frame frame = findFirstFrameWithSelector(page, sel, 12_000)
      .orElseThrow(() -> new RuntimeException("Input with aria-label '" + ariaLabel + "' not found in any frame"));

  Locator input = frame.locator(sel).first();
  input.waitFor(new Locator.WaitForOptions().setTimeout(8_000).setState(WaitForSelectorState.VISIBLE));
  input.scrollIntoViewIfNeeded();

  // Robust clear (JET sometimes ignores plain fill(""))
  try { input.fill(""); } catch (PlaywrightException ignored) {}
  input.click(new Locator.ClickOptions().setTimeout(3_000));
  input.press("Control+A");
  input.press("Delete");

  // Gentle typing helps Oracle formatters
  input.type(value, new Locator.TypeOptions().setDelay(10));
  if (tabOut) input.press("Tab");
}

/** Clicks the first visible element matching any selector across page & frames. */
private boolean clickAcrossFrames11(Page page, List<String> selectors, int timeoutMs) {
  // Try top-level page first
  for (String s : selectors) {
    Locator loc = page.locator(s).first();
    if (isVisibleQuick1111(loc, 1500)) return safeClick111(loc, timeoutMs);
  }
  // Try each frame
  for (Frame f : page.frames()) {
    for (String s : selectors) {
      Locator loc = f.locator(s).first();
      if (isVisibleQuick1111(loc, 1500)) return safeClick111(loc, timeoutMs);
    }
  }
  // Small settle and retry (late-rendered frames)
  page.waitForTimeout(1000);
  for (Frame f : page.frames()) {
    for (String s : selectors) {
      Locator loc = f.locator(s).first();
      if (isVisibleQuick1111(loc, 1500)) return safeClick111(loc, timeoutMs);
    }
  }
  return false;
}

private boolean isVisibleQuick1111(Locator loc, int ms) {
  try {
    loc.waitFor(new Locator.WaitForOptions().setTimeout(ms).setState(WaitForSelectorState.VISIBLE));
    return true;
  } catch (PlaywrightException ignored) { return false; }
}

/** Click with enabled check + force fallback for Oracle JET quirks. */
private boolean safeClick111(Locator loc, int timeoutMs) {
  loc.scrollIntoViewIfNeeded();
  try {
    loc.page().waitForFunction(
        "el => !(el.disabled || el.getAttribute('aria-disabled') === 'true')",
        loc.elementHandle(),
        new Page.WaitForFunctionOptions().setTimeout(1500));
  } catch (PlaywrightException ignored) { /* not all elements expose disabled */ }

  try {
    loc.click(new Locator.ClickOptions().setTimeout(timeoutMs));
    return true;
  } catch (PlaywrightException e) {
    try {
      loc.click(new Locator.ClickOptions().setTimeout(Math.max(2000, timeoutMs / 2)).setForce(true));
      return true;
    } catch (PlaywrightException ignored) {
      return false;
    }
  }
}




@Test(priority = 7)
public void Ship_Unit_Line_Playwright() throws InterruptedException {
  test.log(Status.INFO, "Ship_Unit_Line test started");

  // Light settle (Oracle JET rarely hits NETWORKIDLE)
  page.waitForLoadState(LoadState.DOMCONTENTLOADED);
  page.waitForSelector("iframe", new Page.WaitForSelectorOptions().setTimeout(15_000));

  // 1) Fill inputs (by aria-label) across frames
  // Packaged Item ID
  fillAriaInputAcrossFrames11(page, "Packaged Item ID", "000000000020005492", true);
  page.waitForTimeout(2000);

  // Item ID
  fillAriaInputAcrossFrames11(page, "Item ID", "000000000068441373", true);
  page.waitForTimeout(2000);

  // Total Package Count
  fillAriaInputAcrossFrames11(page, "Total Package Count", "20", false);
  page.waitForTimeout(2000);

  // Packaging Unit
  fillAriaInputAcrossFrames11(page, "Packaging Unit", "BOX_000000000084172488", true);
  page.waitForTimeout(2000);

  // Gross Weight
  fillAriaInputAcrossFrames11(page, "Gross Weight", "20400", false);
  page.waitForTimeout(2000);

  // Gross Volume
  fillAriaInputAcrossFrames11(page, "Gross Volume", "2380", false);
  page.waitForTimeout(2000);

  // Special Service (transport)
  fillAriaInputAcrossFrames11(page, "Special Service", "TRANSPORT", true);
  page.waitForTimeout(2000);

  // 2) Click transport Save (normalize-space()='Save')
  //    Your Selenium XPath: //button[normalize-space()='Save']
  boolean clickedTransportSave = clickAcrossFrames11(
      page,
      Arrays.asList("xpath=//button[normalize-space()='Save']"),
      15_000
  );
  if (!clickedTransportSave) {
    throw new RuntimeException("Could not click transport Save button.");
  }

  // 3) Click main Save (onclick contains checkPiForItem() and text()='Save')
  boolean clickedMainSave = clickAcrossFrames11(
      page,
      Arrays.asList("xpath=//button[contains(@onclick,'checkPiForItem()') and normalize-space()='Save']"),
      15_000
  );
  if (!clickedMainSave) {
    throw new RuntimeException("Could not click main Save (checkPiForItem) button.");
  }

  // Wait similar to your Thread.sleep
  page.waitForTimeout(6000);

  // 4) Scroll to bottom (page-level is fine)
  page.evaluate("window.scrollTo({ top: document.body.scrollHeight, behavior: 'auto' })");

  // 5) Accept any alert/dialog that may appear (equivalent to handleAlert(driver, true))
  page.onceDialog(dialog -> {
    try { dialog.accept(); } catch (Exception ignored) {}
  });

  // 6) Click "Save" with onclick contains 'checkData'
  boolean clickedSaveCheckData = clickAcrossFrames11(
      page,
      Arrays.asList("xpath=//button[contains(@onclick,'checkData') and normalize-space()='Save']"),
      15_000
  );
  if (!clickedSaveCheckData) {
    throw new RuntimeException("Could not click Save button with onclick containing 'checkData'.");
  }

  page.waitForTimeout(4000);

  // 7) Click "Line Item" button
  boolean clickedLineItem = clickAcrossFrames11(
      page,
      Arrays.asList("xpath=//button[normalize-space(.)='Line Item']"),
      15_000
  );
  if (!clickedLineItem) {
    throw new RuntimeException("Could not click 'Line Item' button.");
  }

  page.waitForTimeout(2000);
  test.log(Status.PASS, "Clicked Save and Line Item buttons in Ship Unit Line");

  // Optional diagnostics
  page.screenshot(new Page.ScreenshotOptions().setPath(Paths.get("ship_unit_line_pw.png")).setFullPage(true));
}


@Test(priority = 8)
public void Ship_Unit_Line1_Playwright() throws InterruptedException {
  test.log(Status.INFO, "Ship_Unit_Line1 test started");

  page.onceDialog(d -> { try { d.accept(); } catch (Exception ignored) {} });
  page.waitForLoadState(LoadState.DOMCONTENTLOADED);
  page.waitForSelector("iframe", new Page.WaitForSelectorOptions().setTimeout(15_000));

  // Involved Parties
  if (!clickAcrossFrames11(page,
      Arrays.asList("xpath=//button[normalize-space(.)='Involved Parties']"), 15_000)) {
    throw new RuntimeException("Could not click 'Involved Parties' button.");
  }
  page.waitForTimeout(2000);

  // Involved Party Contact
  fillAriaInputAcrossFrames11(page, "Involved Party Contact", "NA_USO4_OB_PLANNER", false);

  // 1st "Involved Party Qualifier ID" -> select value LOGISTICS
  selectByAriaAcrossFrames(page, "Involved Party Qualifier ID", "LOGISTICS", null);
  page.waitForTimeout(1200);
  Thread.sleep(5000); // brief pause to ensure stability
  // Verify hidden field updated (works for hidden inputs)
  String hiddenSelector = "xpath=//input[@name='display_contact_order_release_inv_party/involved_party_qual/xid']";
  String hiddenValue = getInputValueAcrossFrames(page, hiddenSelector, 8_000);
  System.out.println("Captured hidden value: " + hiddenValue);

  // Save (enButton Save)
  if (!clickAcrossFrames11(page,
      Arrays.asList(
        "xpath=//button[@class='enButton' and normalize-space(text())='Save']",
        "xpath=//button[normalize-space()='Save']"),
      15_000)) {
    throw new RuntimeException("Could not click Save after first qualifier.");
  }
  Thread.sleep(3000); // brief pause to ensure stability
  // Involved Party Location
  fillAriaInputAcrossFrames11(page, "Involved Party Location", "USI2", false);
  page.waitForTimeout(1200);
  Thread.sleep(3000); // brief pause to ensure stability
  // 2nd "Involved Party Qualifier ID" (use :nth-match)
  selectByAriaAcrossFramesWithIndex(page, "Involved Party Qualifier ID", 2,
      "SERVPROV.0030001584", "LOGISTICS");
  Thread.sleep(3000); // brief pause to ensure stability
  String selected2 = getSelectedTextForAriaWithIndex(page, "Involved Party Qualifier ID", 2, 8_000);
  System.out.println("Selected (second dropdown): " + selected2);

  // Verify hidden again
  String hiddenValue2 = getInputValueAcrossFrames(page, hiddenSelector, 8_000);
  System.out.println("Selected qualifier value (hidden): " + hiddenValue2);

  // Final button via absolute XPath (keep as-is)
  clickXPathAnywhere1(page, "/html/body/form/div[3]/div/div[3]/table/tbody/tr[1]/th[4]/table/tbody/tr/td/div/button", 15_000);

  test.log(Status.PASS, "Ship_Unit_Line1 finished successfully");
  page.screenshot(new Page.ScreenshotOptions().setPath(Paths.get("ship_unit_line1_pw.png")).setFullPage(true));
}



@Test(priority = 9)
public void Ship_Unit_Line2_Playwright() throws Exception {
  test.log(Status.INFO, "Ship_Unit_Line2 test started");

  page.onceDialog(d -> { try { d.accept(); } catch (Exception ignored) {} });
  page.waitForLoadState(LoadState.DOMCONTENTLOADED);
  page.waitForSelector("iframe", new Page.WaitForSelectorOptions().setTimeout(15_000));

  // Other Attributes
  if (!clickAcrossFrames11(page,
      Arrays.asList("xpath=//button[normalize-space(text())='Other Attributes']"),
      15_000)) {
    throw new RuntimeException("Could not click 'Other Attributes'.");
  }
  page.waitForTimeout(1500);

  // Finished
  if (!clickAcrossFrames11(page,
      Arrays.asList("xpath=//button[normalize-space(text())='Finished']"),
      15_000)) {
    throw new RuntimeException("Could not click 'Finished'.");
  }
  page.waitForTimeout(3500);

  // Capture value: try your absolute XPATH first, then fallback pattern
  String spanXPath = "/html/body/form/div[5]/div/div/div[1]/table[3]/tbody/tr/td[2]/span";
  String capturedValue = getTextAcrossFramesByXPath(page, spanXPath, 12_000).trim();
  System.out.println("Captured Value: " + capturedValue);

  // Extract only digits
  String numericOnly = capturedValue.replaceAll("[^0-9]", "");
  System.out.println("Extracted Numeric ID: " + numericOnly);

  // Share & persist
  SharedData.capturedOrderReleaseID = numericOnly;
  java.nio.file.Files.writeString(java.nio.file.Paths.get("capturedID.txt"), numericOnly);
  System.out.println("✅ Saved to file: " + numericOnly);

  test.log(Status.PASS, "Captured Numeric ID Stored: " + SharedData.capturedOrderReleaseID);
  test.log(Status.PASS, "Saved to file: " + SharedData.capturedOrderReleaseID);

  page.screenshot(new Page.ScreenshotOptions().setPath(Paths.get("ship_unit_line2_pw.png")).setFullPage(true));
}


/* ======================= helpers ======================= */

/** Clicks the first visible element matching any selector across page & frames. */
private boolean clickAcrossFrames1(Page page, java.util.List<String> selectors, int timeoutMs) {
  // Try top-level page first
  for (String s : selectors) {
    Locator loc = page.locator(s).first();
    if (isVisibleQuick1111(loc, 1500)) return safeClick111(loc, timeoutMs);
  }
  // Try each frame
  for (Frame f : page.frames()) {
    for (String s : selectors) {
      Locator loc = f.locator(s).first();
      if (isVisibleQuick1111(loc, 1500)) return safeClick111(loc, timeoutMs);
    }
  }
  // Small settle and retry (late-rendered frames)
  page.waitForTimeout(1000);
  for (Frame f : page.frames()) {
    for (String s : selectors) {
      Locator loc = f.locator(s).first();
      if (isVisibleQuick1111(loc, 1500)) return safeClick111(loc, timeoutMs);
    }
  }
  return false;
}

/** Returns innerText of an element found by absolute XPath across page or any frame. */
/** Returns innerText for an element by absolute XPath; falls back to pattern search if layout shifts. */
private String getTextAcrossFramesByXPath(Page page, String absXPath, int timeoutMs) {
  String sel = "xpath=" + absXPath;

  for (int pass = 0; pass < 2; pass++) {
    // Try page
    Locator p = page.locator(sel);
    try {
      p.waitFor(new Locator.WaitForOptions().setTimeout(1000).setState(WaitForSelectorState.ATTACHED));
      return p.innerText().trim();
    } catch (PlaywrightException ignored) {}

    // Try frames
    for (Frame f : page.frames()) {
      Locator l = f.locator(sel);
      try {
        l.waitFor(new Locator.WaitForOptions().setTimeout(1000).setState(WaitForSelectorState.ATTACHED));
        return l.innerText().trim();
      } catch (PlaywrightException ignored) {}
    }
    page.waitForTimeout(800);
  }

  // Fallback: find span with UPPER/UPPER.NUMERIC pattern (e.g., ULA/NA.0410858884)
  for (Frame f : page.frames()) {
    String val = (String) f.evaluate("() => {" +
      "const rx = /^[A-Z]+\\/[A-Z]+\\.[0-9]+$/;" +
      "for (const s of Array.from(document.querySelectorAll('span'))) {" +
      "  const t = (s.textContent||'').trim();" +
      "  if (rx.test(t)) return t;" +
      "} return null; }");
    if (val != null && !val.isEmpty()) return val.trim();
  }

  throw new RuntimeException("Element not found (and fallback failed) for XPath: " + absXPath);
}
private Frame resolveMainIFrameOrThrow(Page page, int timeoutMs) {
	  page.waitForSelector("iframe#mainIFrame", new Page.WaitForSelectorOptions().setTimeout(timeoutMs));
	  ElementHandle el = page.querySelector("iframe#mainIFrame");
	  if (el == null) throw new RuntimeException("mainIFrame not found");
	  Frame f = el.contentFrame();
	  if (f == null) throw new RuntimeException("Could not resolve contentFrame for mainIFrame");
	  return f;
	}



private boolean isVisibleQuick111(Locator loc, int ms) {
  try {
    loc.waitFor(new Locator.WaitForOptions().setTimeout(ms).setState(WaitForSelectorState.VISIBLE));
    return true;
  } catch (PlaywrightException ignored) { return false; }
}

/** Click with enabled check + force fallback for Oracle JET quirks. */
private boolean safeClick1111(Locator loc, int timeoutMs) {
  loc.scrollIntoViewIfNeeded();
  try {
    loc.page().waitForFunction(
        "el => !(el.disabled || el.getAttribute('aria-disabled') === 'true')",
        loc.elementHandle(),
        new Page.WaitForFunctionOptions().setTimeout(1500));
  } catch (PlaywrightException ignored) { /* not all elements expose disabled */ }

  try {
    loc.click(new Locator.ClickOptions().setTimeout(timeoutMs));
    return true;
  } catch (PlaywrightException e) {
    try {
      loc.click(new Locator.ClickOptions().setTimeout(Math.max(2000, timeoutMs / 2)).setForce(true));
      return true;
    } catch (PlaywrightException ignored) {
      return false;
    }
  }
}



/* ======================= helpers ======================= */

/** Finds an input by aria-label across frames and fills it. Optionally presses Tab to commit. */
private void fillAriaInputAcrossFrames1(Page page, String ariaLabel, String value, boolean tabOut) {
  String sel = "input[aria-label='" + ariaLabel + "']";
  Frame frame = findFirstFrameWithSelector(page, sel, 12_000)
      .orElseThrow(() -> new RuntimeException("Input with aria-label '" + ariaLabel + "' not found in any frame"));

  Locator input = frame.locator(sel).first();
  input.waitFor(new Locator.WaitForOptions().setTimeout(8_000).setState(WaitForSelectorState.VISIBLE));
  input.scrollIntoViewIfNeeded();

  try { input.fill(""); } catch (PlaywrightException ignored) {}
  input.click(new Locator.ClickOptions().setTimeout(3_000));
  input.press("Control+A");
  input.press("Delete");
  input.type(value, new Locator.TypeOptions().setDelay(10)); // gentle typing helps JET
  if (tabOut) input.press("Tab");
}

/** Select by aria-label across frames; prefer value then fallback to visible text (label). */
private void selectByAriaAcrossFrames(Page page, String ariaLabel, String value, String labelFallback) {
  String sel = "select[aria-label='" + ariaLabel + "']";
  Frame frame = findFirstFrameWithSelector(page, sel, 12_000)
      .orElseThrow(() -> new RuntimeException("Select '" + ariaLabel + "' not found in any frame"));

  Locator dropdown = frame.locator(sel).first();
  dropdown.waitFor(new Locator.WaitForOptions().setTimeout(8_000).setState(WaitForSelectorState.VISIBLE));
  dropdown.scrollIntoViewIfNeeded();

  boolean selected = false;
  if (value != null) {
    try {
      dropdown.selectOption(new SelectOption().setValue(value));
      selected = true;
    } catch (PlaywrightException ignore) {}
  }
  if (!selected && labelFallback != null) {
    dropdown.selectOption(new SelectOption().setLabel(labelFallback));
  }
}

/** Same as above, but targets the Nth occurrence (1-based) when multiple selects share the same aria-label. */
private void selectByAriaAcrossFramesWithIndex(Page page, String ariaLabel, int index1Based, String value, String labelFallback) {
  String nth = ":nth-match(select[aria-label='" + ariaLabel + "']," + index1Based + ")";
  // Try page-level
  Locator pageLoc = page.locator(nth).first();
  if (isVisibleQuick1111(pageLoc, 1000)) {
    selectWithFallback(pageLoc, value, labelFallback);
    return;
  }
  // Try frames
  for (Frame f : page.frames()) {
    Locator loc = f.locator(nth).first();
    if (isVisibleQuick1111(loc, 1200)) {
      selectWithFallback(loc, value, labelFallback);
      return;
    }
  }
  // Retry late-render
  page.waitForTimeout(800);
  for (Frame f : page.frames()) {
    Locator loc = f.locator(nth).first();
    if (isVisibleQuick1111(loc, 1200)) {
      selectWithFallback(loc, value, labelFallback);
      return;
    }
  }
  throw new RuntimeException("Select '" + ariaLabel + "' occurrence #" + index1Based + " not found.");
}

private void selectWithFallback(Locator dropdown, String value, String labelFallback) {
  dropdown.scrollIntoViewIfNeeded();
  dropdown.waitFor(new Locator.WaitForOptions().setTimeout(8_000).setState(WaitForSelectorState.VISIBLE));
  boolean selected = false;
  if (value != null) {
    try {
      dropdown.selectOption(new SelectOption().setValue(value));
      selected = true;
    } catch (PlaywrightException ignore) {}
  }
  if (!selected && labelFallback != null) {
    dropdown.selectOption(new SelectOption().setLabel(labelFallback));
  }
}

/** Returns the .value of an <input> found across page/frames. */
/** Returns the .value of an <input> found across page/frames (works for hidden/readonly inputs). */
private String getInputValueAcrossFrames(Page page, String selector, int timeoutMs) {
  long deadline = System.currentTimeMillis() + timeoutMs;

  for (int pass = 0; pass < 2; pass++) {
    // Page scope
    Locator p = page.locator(selector).first();
    try {
      p.waitFor(new Locator.WaitForOptions()
          .setTimeout(800)
          .setState(WaitForSelectorState.ATTACHED)); // not VISIBLE
      return p.inputValue();
    } catch (PlaywrightException ignored) {}

    // Frames scope
    for (Frame f : page.frames()) {
      Locator l = f.locator(selector).first();
      try {
        l.waitFor(new Locator.WaitForOptions()
            .setTimeout(800)
            .setState(WaitForSelectorState.ATTACHED)); // not VISIBLE
        return l.inputValue();
      } catch (PlaywrightException ignored) {}
    }

    if (System.currentTimeMillis() > deadline) break;
    page.waitForTimeout(600);
  }
  throw new RuntimeException("Hidden/input field not found for selector: " + selector);
}


/** Get selected option text for an aria-labeled select at a specific index (1-based). */
private String getSelectedTextForAriaWithIndex(Page page, String ariaLabel, int index1Based, int timeoutMs) {
  String nth = ":nth-match(select[aria-label='" + ariaLabel + "']," + index1Based + ")";
  Locator loc = page.locator(nth).first();
  if (!isVisibleQuick1111(loc, 1000)) {
    for (Frame f : page.frames()) {
      Locator l = f.locator(nth).first();
      if (isVisibleQuick1111(l, 1000)) { loc = l; break; }
    }
  }
  loc.waitFor(new Locator.WaitForOptions().setTimeout(timeoutMs).setState(WaitForSelectorState.VISIBLE));
  return loc.locator("option:checked").innerText().trim();
}

/** Click an absolute XPath whether it’s in the page or any frame. */
private void clickXPathAnywhere1(Page page, String absoluteXPath, int timeoutMs) {
  String sel = "xpath=" + absoluteXPath;

  // Try page first
  Locator pLoc = page.locator(sel);
  if (isVisibleQuick1111(pLoc, 1200)) { safeClick111(pLoc, timeoutMs); return; }

  // Try frames
  for (Frame f : page.frames()) {
    Locator loc = f.locator(sel);
    if (isVisibleQuick1111(loc, 1200)) { safeClick111(loc, timeoutMs); return; }
  }

  // Retry late-render
  page.waitForTimeout(1000);
  for (Frame f : page.frames()) {
    Locator loc = f.locator(sel);
    if (isVisibleQuick1111(loc, 1500)) { safeClick111(loc, timeoutMs); return; }
  }
  throw new RuntimeException("Button not found for XPath: " + absoluteXPath);
}

private boolean isVisibleQuick11(Locator loc, int ms) {
  try {
    loc.waitFor(new Locator.WaitForOptions().setTimeout(ms).setState(WaitForSelectorState.VISIBLE));
    return true;
  } catch (PlaywrightException ignored) { return false; }
}

/** Click with enabled check + force fallback for Oracle JET quirks. */
private void safeClick11(Locator loc, int timeoutMs) {
  loc.scrollIntoViewIfNeeded();
  try {
    loc.page().waitForFunction(
        "el => !(el.disabled || el.getAttribute('aria-disabled') === 'true')",
        loc.elementHandle(),
        new Page.WaitForFunctionOptions().setTimeout(1500));
  } catch (PlaywrightException ignored) { /* not all elements expose disabled */ }

  try {
    loc.click(new Locator.ClickOptions().setTimeout(timeoutMs));
  } catch (PlaywrightException e) {
    // last resort
    loc.click(new Locator.ClickOptions().setTimeout(Math.max(2000, timeoutMs / 2)).setForce(true));
  }
}




/* ----------------------- helpers ----------------------- */

/** Finds an input by aria-label across frames and fills it. Optionally presses Tab to commit. */
private void fillAriaInputAcrossFrames(Page page, String ariaLabel, String value, boolean tabOut) {
  String sel = "input[aria-label='" + ariaLabel + "']";

  Frame frame = findFirstFrameWithSelector(page, sel, 12_000)
      .orElseThrow(() -> new RuntimeException("Input with aria-label '" + ariaLabel + "' not found in any frame"));

  Locator input = frame.locator(sel).first();
  input.waitFor(new Locator.WaitForOptions().setTimeout(8_000).setState(WaitForSelectorState.VISIBLE));
  input.scrollIntoViewIfNeeded();

  // Robust clear
  try { input.fill(""); } catch (PlaywrightException ignored) {}
  input.click(new Locator.ClickOptions().setTimeout(3_000));
  input.press("Control+A");
  input.press("Delete");

  // Gentle typing helps with Oracle formatters
  input.type(value, new Locator.TypeOptions().setDelay(10));
  if (tabOut) input.press("Tab");
}

/** Clicks the first visible element matching any selector across page & frames. */
private boolean clickAcrossFrames(Page page, List<String> selectors, int timeoutMs) {
  // 1) Try top-level page first
  for (String s : selectors) {
    Locator loc = page.locator(s).first();
    if (isVisibleQuick1111(loc, 1500)) {
      return safeClick111(loc, timeoutMs);
    }
  }
  // 2) Try each frame
  for (Frame f : page.frames()) {
    for (String s : selectors) {
      Locator loc = f.locator(s).first();
      if (isVisibleQuick1111(loc, 1500)) {
        return safeClick111(loc, timeoutMs);
      }
    }
  }
  // 3) Small settle and retry (late-rendered frames)
  page.waitForTimeout(1000);
  for (Frame f : page.frames()) {
    for (String s : selectors) {
      Locator loc = f.locator(s).first();
      if (isVisibleQuick1111(loc, 1500)) {
        return safeClick111(loc, timeoutMs);
      }
    }
  }
  return false;
}

private boolean isVisibleQuick(Locator loc, int ms) {
  try {
    loc.waitFor(new Locator.WaitForOptions().setTimeout(ms).setState(WaitForSelectorState.VISIBLE));
    return true;
  } catch (PlaywrightException ignored) { return false; }
}

/** Click with enabled check + force fallback for Oracle JET quirks. */
private boolean safeClick(Locator loc, int timeoutMs) {
  loc.scrollIntoViewIfNeeded();
  try {
    loc.page().waitForFunction(
        "el => !(el.disabled || el.getAttribute('aria-disabled') === 'true')",
        loc.elementHandle(),
        new Page.WaitForFunctionOptions().setTimeout(1500));
  } catch (PlaywrightException ignored) { /* not all elements expose disabled */ }

  try {
    loc.click(new Locator.ClickOptions().setTimeout(timeoutMs));
    return true;
  } catch (PlaywrightException e) {
    try {     loc.click(new Locator.ClickOptions().setTimeout(Math.max(2000, timeoutMs / 2)).setForce(true));
      return true;
    } catch (PlaywrightException ignored) {
      return false;
    }
  }
}







/* ======================= helpers ======================= */

/** Clear (best-effort) then type text; optionally press Tab to commit in Oracle/JET. */
private void clearAndType(Locator input, String text, boolean tabOut) {
  try { input.fill(""); } catch (PlaywrightException ignore) {}
  input.click(new Locator.ClickOptions().setTimeout(3_000));
  input.press("Control+A");
  input.press("Delete");
  input.type(text, new Locator.TypeOptions().setDelay(10)); // gentle typing helps formatters
  if (tabOut) input.press("Tab");
}

/** Click an absolute XPath whether it’s in the page or any frame. */
private void clickXPathAnywhere(Page page, String absoluteXPath, int timeoutMs) {
  String sel = "xpath=" + absoluteXPath;

  // 1) Try top-level page
  Locator pLoc = page.locator(sel);
  if (isVisibleQuick1111(pLoc, 1200)) {
    safeClick111(pLoc, timeoutMs);
    return;
  }

  // 2) Try each frame (BFS across all frames)
  for (Frame f : page.frames()) {
    Locator loc = f.locator(sel);
    if (isVisibleQuick1111(loc, 1200)) {
      safeClick111(loc, timeoutMs);
      return;
    }
  }

  // 3) If still not found, wait a bit and retry in frames (handles late iframes)
  page.waitForTimeout(1000);
  for (Frame f : page.frames()) {
    Locator loc = f.locator(sel);
    if (isVisibleQuick1111(loc, 1500)) {
      safeClick111(loc, timeoutMs);
      return;
    }
  }

  throw new RuntimeException("Button not found for XPath: " + absoluteXPath);
}

private boolean isVisibleQuick1(Locator loc, int ms) {
  try {
    loc.waitFor(new Locator.WaitForOptions().setTimeout(ms).setState(WaitForSelectorState.VISIBLE));
    return true;
  } catch (PlaywrightException ignored) {
    return false;
  }
}

private void safeClick1(Locator loc, int timeoutMs) {
  loc.scrollIntoViewIfNeeded();
  try {
    // best-effort enabled check for Oracle JET
    loc.page().waitForFunction(
        "el => !(el.disabled || el.getAttribute('aria-disabled') === 'true')",
        loc.elementHandle(),
        new Page.WaitForFunctionOptions().setTimeout(1500));
  } catch (PlaywrightException ignored) {}
  try {
    loc.click(new Locator.ClickOptions().setTimeout(timeoutMs));
  } catch (PlaywrightException e) {
    // last resort
    loc.click(new Locator.ClickOptions().setTimeout(Math.max(2000, timeoutMs / 2)).setForce(true));
  }
}



/* ------------------------- helpers used above ------------------------- */

/** Fill an aria-labeled date input anywhere in the page (search across all frames). */
private void fillAriaDate(Page page, String ariaLabel, String value) {
  String sel = "input[aria-label='" + ariaLabel + "']";
  Frame f = findFirstFrameWithSelector(page, sel, 12_000)
      .orElseThrow(() -> new RuntimeException("Date input '" + ariaLabel + "' not found in any frame"));
  Locator input = f.locator(sel);
  input.waitFor(new Locator.WaitForOptions().setTimeout(8_000).setState(WaitForSelectorState.VISIBLE));
  input.scrollIntoViewIfNeeded();
  // Clear then type; Playwright .fill() replaces value, but some Oracle inputs prefer a manual clear
  try { input.fill(""); } catch (PlaywrightException ignore) {}
  clearAndType(input, value, true); // tabOut=true to blur and commit
}

/** Clear (best-effort) then type text; optionally press Tab to commit in Oracle forms. */
private void clearAndType1(Locator input, String text, boolean tabOut) {
  try { input.fill(""); } catch (PlaywrightException ignore) {}
  // Extra clear (some JET inputs ignore .fill(""))
  input.click(new Locator.ClickOptions().setTimeout(3_000));
  input.press("Control+A");
  input.press("Delete");
  input.type(text, new Locator.TypeOptions().setDelay(10)); // gentle typing helps JET formatters
  if (tabOut) input.press("Tab");
}



@Test(priority = 10)
public void handleDynamicCheckboxes_Playwright() {
  test.log(Status.INFO, "handleDynamicCheckboxes (robust) started");

  // Always resolve the iframe fresh
  Frame frame = resolveMainIFrameOrThrow(page, 15_000);
  System.out.println("✅ Switched to iframe: mainIFrame");

  // Try both possible table indices (layout sometimes shifts)
  String[] paths = {
    "/html/body/form/div[5]/div/div/div[1]/table[3]/tbody/tr/td[1]/input",
    "/html/body/form/div[5]/div/div/div[1]/table[2]/tbody/tr/td[1]/input"
  };

  boolean checked = false;
  for (String xp : paths) {
    Locator cb = frame.locator("xpath=" + xp).first();
    try {
      cb.waitFor(new Locator.WaitForOptions()
          .setTimeout(5000)
          .setState(WaitForSelectorState.ATTACHED)); // not VISIBLE
    } catch (PlaywrightException ignored) { continue; }

    // 1) prefer check()
    try {
      cb.check(new Locator.CheckOptions().setTimeout(4000));
      checked = true;
    } catch (PlaywrightException e1) {
      // 2) force click
      try {
        cb.click(new Locator.ClickOptions().setTimeout(3000).setForce(true));
        checked = true;
      } catch (PlaywrightException e2) {
        // 3) last resort: set property and dispatch change
        try {
          frame.evaluate("(el) => { el.checked = true; el.dispatchEvent(new Event('change', { bubbles:true })); }",
                         cb.elementHandle());
          checked = true;
        } catch (PlaywrightException ignored2) {}
      }
    }

    if (checked) {
      cb.evaluate("el => el.style.border='3px solid red'"); // debug highlight
      test.log(Status.PASS, "Checkbox ensured to be checked");
      System.out.println("✔️ Checkbox checked.");
      break;
    }
  }

  if (!checked) throw new RuntimeException("Could not locate/check the checkbox in either table[3] or table[2].");

  page.waitForTimeout(1200);
}


  @AfterClass
  public void tearDown() {
    try {
      if (context != null) context.close();
      if (browser != null) browser.close();
      if (playwright != null) playwright.close();
      test.log(Status.INFO, "Browser closed and Playwright quit successfully.");
    } finally {
      if (extent != null) extent.flush();
    }
  }
 }

  

