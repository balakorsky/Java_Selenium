import org.openqa.selenium.*;
import org.openqa.selenium.chrome.*;
import org.openqa.selenium.support.ui.*;
import org.testng.Assert;
import org.testng.annotations.*;
import com.aventstack.extentreports.*;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import java.io.File;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.nio.file.*;

public class TravelInsuranceTest {
    private WebDriver driver;
    private WebDriverWait wait;
    private ExtentReports extent;
    private ExtentTest test;

    @BeforeClass
    public void setup() {
        System.setProperty("webdriver.chrome.driver", "chromedriver");

        ChromeOptions options = new ChromeOptions();
        // ⚙️ Настройки для GitHub Actions / CI
        options.addArguments("--headless=new");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-gpu");
        options.addArguments("--window-size=1920,1080");
        options.addArguments("--remote-allow-origins=*");

        driver = new ChromeDriver(options);
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
        wait = new WebDriverWait(driver, Duration.ofSeconds(25));

        ExtentSparkReporter spark = new ExtentSparkReporter("ExtentReport.html");
        extent = new ExtentReports();
        extent.attachReporter(spark);
        test = extent.createTest("Travel Insurance Purchase Flow");

        driver.get("https://digital.harel-group.co.il/travel-policy");
        test.info("Opened Harel Travel Insurance page");
    }

    @Test
    public void verifyTravelInsuranceFlow() {
        try {
            // 🔹 Step 1: Click “לרכישה בפעם הראשונה”
            wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("button[data-hrl-bo='first-time-purchase']"))).click();
            test.pass("Clicked on 'לרכישה בפעם הראשונה'");

            // 🔹 Step 2: Select Country
            wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("div[data-hrl-bo='canada']"))).click();
            test.pass("Selected country: Canada");

            wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("button[data-hrl-bo='wizard-next-button']"))).click();
            test.pass("Clicked 'Next' after country selection");

            // 🔹 Step 3: Dynamic Dates (today + 29 days)
            LocalDate start = LocalDate.now();
            LocalDate end = start.plusDays(29);
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");

            String startSelector = String.format("//button[@data-hrl-bo='%s']", fmt.format(start));
            String endSelector = String.format("//button[@data-hrl-bo='%s']", fmt.format(end));

            try {
                WebElement startBtn = wait.until(ExpectedConditions.elementToBeClickable(By.xpath(startSelector)));
                ((JavascriptExecutor) driver).executeScript("arguments[0].click();", startBtn);

                WebElement endBtn = wait.until(ExpectedConditions.elementToBeClickable(By.xpath(endSelector)));
                ((JavascriptExecutor) driver).executeScript("arguments[0].click();", endBtn);

                test.pass("Selected travel dates: " + start + " → " + end);
            } catch (TimeoutException e) {
                test.warning("⚠️ Could not click date buttons, using fallback JS");
                ((JavascriptExecutor) driver).executeScript("document.querySelectorAll('button[data-hrl-bo]')[0]?.click()");
            }

            wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("button[data-hrl-bo='wizard-next-button']"))).click();
            test.pass("Clicked 'Next' after selecting dates");

            // 🔹 Step 4: Check PDF policy link
            By pdfLink = By.cssSelector("a[data-hrl-bo='policy-agreement-text-url'][href$='.pdf']");
            wait.until(ExpectedConditions.visibilityOfElementLocated(pdfLink));

            WebElement policyPdf = driver.findElement(pdfLink);
            Assert.assertTrue(policyPdf.isDisplayed(), "PDF link not visible!");
            test.pass("✅ Policy PDF found: " + policyPdf.getAttribute("href"));

        } catch (Exception e) {
            test.fail("❌ Test failed: " + e.getMessage());
            takeScreenshot("target/screenshot-failure.png");
            throw e;
        }
    }

    private void takeScreenshot(String path) {
        try {
            File src = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
            Files.copy(src.toPath(), Paths.get(path), StandardCopyOption.REPLACE_EXISTING);
            test.addScreenCaptureFromPath(path);
        } catch (Exception ex) {
            System.out.println("⚠️ Could not save screenshot: " + ex.getMessage());
        }
    }

    @AfterClass
    public void tearDown() {
        if (driver != null) driver.quit();
        extent.flush();
    }
}
