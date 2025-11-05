import io.github.bonigarcia.wdm.WebDriverManager;
import io.qameta.allure.Allure;
import io.qameta.allure.Attachment;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;
import org.testng.annotations.*;
import com.aventstack.extentreports.*;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;

import java.io.ByteArrayInputStream;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class TravelInsuranceTest {

    private WebDriver driver;
    private WebDriverWait wait;
    private static ExtentReports extent;
    private ExtentTest test;

    @BeforeSuite
    public void setupReport() {
        ExtentSparkReporter spark = new ExtentSparkReporter("ExtentReport.html");
        extent = new ExtentReports();
        extent.attachReporter(spark);
    }

    @BeforeClass
    public void setup() {
        WebDriverManager.chromedriver().setup();

        ChromeOptions options = new ChromeOptions();
        if (System.getenv("CI") != null) { // для GitHub Actions
            options.addArguments("--headless=new", "--disable-gpu", "--no-sandbox", "--window-size=1920,1080");
        } else {
            options.addArguments("--start-maximized");
        }

        driver = new ChromeDriver(options);
        wait = new WebDriverWait(driver, Duration.ofSeconds(20));
        driver.get("https://digital.harel-group.co.il/travel-policy");
    }

    @Test(description = "Проверка формы страхования с выбором дат и PDF")
    public void verifyTravelInsuranceFlow() {
        test = extent.createTest("Travel Insurance Flow");

        try {
            // Кнопка "לרכישה בפעם הראשונה"
            By firstBtn = By.xpath("//button[contains(.,'לרכישה בפעם הראשונה')]");
            wait.until(ExpectedConditions.elementToBeClickable(firstBtn)).click();
            Allure.step("Нажали на 'לרכישה בפעם הראשונה'");

            // Канада
            By canada = By.cssSelector("div[data-hrl-bo='canada']");
            wait.until(ExpectedConditions.elementToBeClickable(canada)).click();
            Allure.step("Выбрали Канаду");

            // Далее
            By next = By.cssSelector("button[data-hrl-bo='wizard-next-button']");
            wait.until(ExpectedConditions.elementToBeClickable(next)).click();

            // Выбор дат
            LocalDate start = LocalDate.now();
            LocalDate end = start.plusDays(29);
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");

            WebElement startDate = wait.until(ExpectedConditions.elementToBeClickable(
                    By.xpath("//button[@data-hrl-bo='" + start.format(fmt) + "']")));
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", startDate);

            WebElement endDate = wait.until(ExpectedConditions.elementToBeClickable(
                    By.xpath("//button[@data-hrl-bo='" + end.format(fmt) + "']")));
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", endDate);

            Allure.step("Выбрали даты: " + start + " — " + end);
            wait.until(ExpectedConditions.elementToBeClickable(next)).click();

            // Проверка PDF
            By pdfLink = By.cssSelector("a[data-hrl-bo='policy-agreement-text-url'][href$='.pdf']");
            WebElement pdf = wait.until(ExpectedConditions.visibilityOfElementLocated(pdfLink));
            Assert.assertTrue(pdf.isDisplayed(), "PDF не найден");
            Allure.step("PDF найден и отображается ✅");

        } catch (Exception e) {
            takeScreenshot("Ошибка");
            Allure.step("Ошибка: " + e.getMessage());
            test.fail(e);
            Assert.fail(e.getMessage());
        }
    }

    @Attachment(value = "Screenshot", type = "image/png")
    public byte[] takeScreenshot(String name) {
        byte[] screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
        Allure.addAttachment(name, new ByteArrayInputStream(screenshot));
        return screenshot;
    }

    @AfterClass
    public void tearDown() {
        if (driver != null) driver.quit();
    }

    @AfterSuite
    public void flushReport() {
        extent.flush();
    }
}
