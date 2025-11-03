import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.aventstack.extentreports.reporter.configuration.Theme;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;
import org.testng.annotations.*;

import java.io.File;
import java.time.Duration;
import java.time.LocalDate;

public class TravelInsuranceTest {
    WebDriver driver;
    WebDriverWait wait;
    ExtentReports extent;
    ExtentTest test;

    @BeforeClass
    public void setup() {
        // === Настройка Extent Reports ===
        ExtentSparkReporter reporter = new ExtentSparkReporter(new File("ExtentReport.html"));
        reporter.config().setDocumentTitle("Harel Travel Insurance Automation Report");
        reporter.config().setReportName("Travel Insurance Test Flow");
        reporter.config().setTheme(Theme.DARK);

        extent = new ExtentReports();
        extent.attachReporter(reporter);
        extent.setSystemInfo("Tester", "Michael Balakorski");
        extent.setSystemInfo("Browser", "Chrome");
        extent.setSystemInfo("Language", "Java + Selenium + TestNG");

        // === Настройка WebDriver ===
        WebDriverManager.chromedriver().setup();
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--start-maximized");
        driver = new ChromeDriver(options);
        wait = new WebDriverWait(driver, Duration.ofSeconds(30));
    }

    private void slowDown(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Test
    public void verifyTravelInsuranceFlow() {
        test = extent.createTest("Travel Insurance Purchase Flow Test");

        try {
            test.info("Opening website...");
            driver.get("https://digital.harel-group.co.il/travel-policy");
            slowDown(2000);

            test.info("Clicking 'לרכישה בפעם הראשונה'");
            WebElement firstPurchaseBtn = wait.until(ExpectedConditions.elementToBeClickable(
                    By.xpath("//button[contains(., 'לרכישה בפעם הראשונה')]")));
            firstPurchaseBtn.click();
            slowDown(2000);

            test.info("Selecting destination: Canada");
            WebElement canada = wait.until(ExpectedConditions.elementToBeClickable(
                    By.cssSelector("div[data-hrl-bo='canada']")));
            canada.click();
            slowDown(2000);

            test.info("Clicking Next");
            WebElement nextBtn = wait.until(ExpectedConditions.elementToBeClickable(
                    By.cssSelector("button[data-hrl-bo='wizard-next-button']")));
            nextBtn.click();
            slowDown(2000);

            test.info("Selecting travel dates dynamically");
            LocalDate today = LocalDate.now();
            LocalDate endDateCalc = today.plusDays(29);

            By startDate = By.xpath("//button[@data-hrl-bo='" + today + "']");
            By endDate = By.xpath("//button[@data-hrl-bo='" + endDateCalc + "']");

            wait.until(ExpectedConditions.presenceOfElementLocated(startDate));
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();",
                    driver.findElement(startDate));
            slowDown(1500);

            wait.until(ExpectedConditions.presenceOfElementLocated(endDate));
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();",
                    driver.findElement(endDate));
            slowDown(1500);

            test.info("Verifying total days count = 30");
            WebElement totalDays = wait.until(ExpectedConditions.visibilityOfElementLocated(
                    By.cssSelector("[data-hrl-bo='total-days']")));
            Assert.assertTrue(totalDays.getText().contains("30"), "Total days incorrect!");
            slowDown(2000);

            test.info("Clicking Next again...");
            WebElement continueBtn = wait.until(ExpectedConditions.elementToBeClickable(
                    By.cssSelector("button[data-hrl-bo='wizard-next-button']")));
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", continueBtn);
            slowDown(3000);

            test.info("Checking PDF link presence...");
            By pdfLinkSelector = By.cssSelector("a[data-hrl-bo='policy-agreement-text-url'][href$='.pdf']");
            try {
                WebElement pdfLink = wait.until(ExpectedConditions.visibilityOfElementLocated(pdfLinkSelector));
                Assert.assertTrue(pdfLink.isDisplayed(), "PDF link not visible!");
                test.pass("✅ PDF link found: " + pdfLink.getAttribute("href"));
            } catch (TimeoutException e) {
                test.warning("PDF link not found — checking fallback via JS...");
                boolean exists = (boolean) ((JavascriptExecutor) driver)
                        .executeScript("return document.querySelector('a[data-hrl-bo=\"policy-agreement-text-url\"][href$=\".pdf\"]') !== null;");
                Assert.assertTrue(exists, "PDF link missing even after fallback check!");
            }

            test.pass("🎯 Travel insurance flow executed successfully.");

        } catch (Exception e) {
            test.fail("❌ Test failed: " + e.getMessage());
            File screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);
            test.addScreenCaptureFromPath(screenshot.getAbsolutePath());
            throw e;
        }
    }

    @AfterClass
    public void tearDown() {
        test.info("Closing browser...");
        if (driver != null) {
            driver.quit();
        }
        extent.flush();
        System.out.println("📊 HTML report generated: ExtentReport.html");
    }
}
