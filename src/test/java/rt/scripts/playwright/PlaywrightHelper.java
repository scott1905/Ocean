package rt.scripts.playwright;

import com.microsoft.playwright.ElementHandle;
import com.microsoft.playwright.Frame;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.LoadState;

import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;

public class PlaywrightHelper {

    private final Page page;

    public PlaywrightHelper(Page page) {
        this.page = page;
    }

    public void waitForLoadState() {
        page.waitForLoadState(LoadState.LOAD);
    }

    public void screenshot(String name) {
        page.screenshot(new Page.ScreenshotOptions().setPath(Paths.get("screenshots", name + ".png")).setFullPage(true));
    }

    public Locator withinFrameById(String frameId, String selector) {
        return page.frameLocator("iframe#" + frameId).locator(selector);
    }

    public Locator withinAnyFrame(String selector) {
        List<Frame> frames = page.frames();
        for (Frame f : frames) {
            Locator loc = f.locator(selector);
            if (Boolean.TRUE.equals(loc.isVisible())) {
                return loc;
            }
        }
        // fallback to default
        return page.locator(selector);
    }

    public void clickWithScroll(Locator locator) {
        locator.scrollIntoViewIfNeeded();
        locator.click();
    }

    public void typeAndTab(Locator locator, String text) {
        locator.fill("");
        locator.type(text, new Locator.TypeOptions().setDelay(10));
        locator.press("Tab");
    }

    public void waitForTimeoutMillis(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException ignored) { }
    }
}


