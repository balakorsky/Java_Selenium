import org.openqa.selenium.*;
import org.openqa.selenium.chrome.*;
import org.openqa.selenium.support.ui.*;
import org.testng.Assert;
import org.testng.annotations.*;
import com.aventstack.extentreports.*;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import java.time.*;
import java.time.format.DateTimeFormatter;

public class TravelInsuranceTest {
    private WebDriver driver;
    private WebDriverWait wait;
    private ExtentReports extent;
    private ExtentTest test;

    @BeforeClass
    public void setup() {
        System.setProperty("webdriver.chrome.driver", "chromedriver");

        ChromeOptions options = new ChromeOptions();
        // ⚙️ Настройки для GitHub Actions / Linux CI
        options.addArguments("--headless=new");
        options.addArguments("--no-sandbox");
        options.addArguments("--disable-dev-shm-usage");
        options.addArguments("--disable-gpu");
        options.addArguments("--window-size=1920,1080");
        options.addArguments("--remote-allow-origins=*");

        driver = new ChromeDriver(options);
        wait = new WebDriverWait(driver, Duration.ofSeconds(15));

        ExtentSparkReporter spark = new ExtentSparkReporter("ExtentReport.html");
        extent = new ExtentReports();
        extent.attachReporter(spark);
        test = extent.createTest("Travel Insurance Purchase Flow");

        driver.get("https://digital.harel-group.co.il/travel-policy");
        test.info("Opened travel insurance page");
    }

    @Test
    public void verifyTravelInsuranceFlow() {
        try {
            // 🔹 Шаг 1: Нажать “לרכישה בפעם הראשונה”
            wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("button[data-hrl-bo='first-time-purchase']"))).click();
            test.pass("Clicked on 'לרכישה בפעם הראשונה'");

            // 🔹 Шаг 2: Выбор страны
            wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("div[data-hrl-bo='canada']"))).click();
            test.pass("Selected country: Canada");

            // 🔹 Нажать “הבא”
            wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("button[data-hrl-bo='wizard-next-button']"))).click();
            test.pass("Clicked next after selecting country");

            // 🔹 Шаг 3: Выбор дат (сегодня + 29 дней)
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

                test.pass("Selected dynamic dates: " + start + " → " + end);
            } catch (TimeoutException e) {
                test.warning("⚠️ Could not click date buttons, attempting JS fallback...");
                ((JavascriptExecutor) driver).executeScript(
                        "const today = document.querySelectorAll('button[data-hrl-bo]')[0]; if(today) today.click();"
                );
            }

            // 🔹 Клик “הבא”
            wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("button[data-hrl-bo='wizard-next-button']"))).click();
            test.pass("Clicked next after selecting dates");

            // 🔹 Проверка наличия PDF ссылки на תנאי הפוליסה
            By pdfLink = By.cssSelector("a[data-hrl-bo='policy-agreement-text-url'][href$='.pdf']");
            wait.until(ExpectedConditions.visibilityOfElementLocated(pdfLink));

            WebElement policyPdf = driver.findElement(pdfLink);
            Assert.assertTrue(policyPdf.isDisplayed(), "PDF link is not visible!");
            test.pass("✅ Policy agreement PDF link found: " + policyPdf.getAttribute("href"));

        } catch (Exception e) {
            test.fail("❌ Test failed: " + e.getMessage());
            throw e;
        }
    }

    @AfterClass
    public void tearDown() {
        if (driver != null) driver.quit();
        extent.flush();
    }
}
