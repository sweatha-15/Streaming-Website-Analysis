import java.io.FileWriter;
import java.time.Duration;
import java.util.*;
import com.opencsv.CSVWriter;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.*;

public class CombinedCrawler {

    public static void main(String[] args) throws Exception {
        System.setProperty("webdriver.chrome.driver", "/opt/homebrew/bin/chromedriver");
        ChromeDriver driver = new ChromeDriver();
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));

        String homepage = "http://localhost:3000/index.html";
        driver.get(homepage);

        wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(
            By.cssSelector("a[href$='netflix.html'], a[href$='hulu.html'], a[href$='DiscoveryPlus.html'], a[href$='CuriosityStream.html']")
        ));

        Map<String, String> services = Map.of(
    "Netflix", "netflix.html",
    "Hulu", "hulu.html",
    "Discovery+", "DiscoveryPlus.html",  // ✅ match h2 text
    "CuriosityStream", "CuriosityStream.html"
);


        // Extract Subscription Plans
        Map<String, String> subscriptionPlans = extractSubscriptions(driver, wait, homepage);

        // Write CSV
        try (CSVWriter writer = new CSVWriter(new FileWriter("all_content.csv"))) {
            writer.writeNext(new String[]{"Website", "Genre", "Title", "Link", "ImageURL", "Subscription"});

            for (Map.Entry<String, String> svc : services.entrySet()) {
                String site = svc.getKey();
                String pageFile = svc.getValue();

                driver.get(homepage);
                WebElement link = driver.findElement(By.cssSelector("a[href$='" + pageFile + "']"));
                driver.get(link.getAttribute("href"));

                wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("section.genre-section")));
                List<WebElement> sections = driver.findElements(By.cssSelector("section.genre-section"));

                for (WebElement section : sections) {
                    String genre = section.findElement(By.tagName("h2")).getText().trim();

                    List<WebElement> cards = section.findElements(By.cssSelector("div.show-card"));
                    for (WebElement card : cards) {
                        String title = safeGetText(card, By.tagName("h3"));
                        String linkUrl = safeGetAttr(card, By.tagName("a"), "href");
                        String imageUrl = safeGetAttr(card, By.tagName("img"), "src");
                        String plan = subscriptionPlans.getOrDefault(site, "N/A");

                        writer.writeNext(new String[]{site, genre, title, linkUrl, imageUrl, plan});
                    }
                }

                System.out.println("✅  Scraped " + site);
            }
        }

        driver.quit();
        System.out.println("✔️  all_content.csv generated!");
    }

    // Helpers
    private static String safeGetText(WebElement parent, By selector) {
        try {
            return parent.findElement(selector).getText().trim();
        } catch (Exception e) {
            return "";
        }
    }

    private static String safeGetAttr(WebElement parent, By selector, String attr) {
        try {
            return parent.findElement(selector).getAttribute(attr).trim();
        } catch (Exception e) {
            return "";
        }
    }

    private static Map<String, String> extractSubscriptions(ChromeDriver driver, WebDriverWait wait, String homepage) {
        Map<String, String> plans = new HashMap<>();
        try {
            driver.get(homepage);
            WebElement cta = wait.until(ExpectedConditions.elementToBeClickable(
                By.cssSelector("a.cta-button[href$='SubscriptionPlan.html']"))
            );
            cta.click();

            wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector("div.container .card")));
            List<WebElement> cards = driver.findElements(By.cssSelector("div.container .card"));

            for (WebElement card : cards) {
                String service = safeGetText(card, By.cssSelector(".card-body h2"));
                List<WebElement> items = card.findElements(By.cssSelector(".plans-list li"));
                List<String> lines = new ArrayList<>();
                for (WebElement item : items) {
                    lines.add(item.getText().trim());
                }
                plans.put(service, String.join(" | ", lines));
                System.out.println("📦 Subscriptions for " + service + ": " + plans.get(service));
            }

        } catch (Exception e) {
            System.err.println("⚠️  Failed to extract subscriptions: " + e.getMessage());
        }
        return plans;
    }
}


