package models;

import org.openqa.selenium.By;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by SpereShelde on 2018/6/9.
 */
public class NexusPHP implements Runnable {

    private String domain, passkey, url;
    private ArrayList<String> urls = new ArrayList<String>();
    private ArrayList<String> newUrls = new ArrayList<String>();
    private double min, max, down, up;
    private HtmlUnitDriver driver;
    private Map<String, String> qbConfig;
    private QBittorrent qBittorrent;

    public NexusPHP(String url, double min, double max, double down, double up, HtmlUnitDriver driver, Map qbConfig) {
        this.url = url;
        this.domain = url.substring(url.indexOf("//") + 2, url.indexOf("/", url.indexOf("//") + 2));
        this.min = min;
        this.max = max;
        this.down = down;
        this.up  = up;
        this.driver = driver;
        this.qbConfig = qbConfig;
        this.getPasskey();
        qBittorrent = new QBittorrent(this.qbConfig.get("webUI"), this.qbConfig.get("sessionID"), url, this.urls, this.down, this.up);
    }

    private void getPasskey() {
        String source;
        driver.get(url);
        source = driver.getPageSource();
        Pattern passkeylink = Pattern.compile("href=.*details.php\\?id=[0-9]*.*hit=1");
        Matcher sizeMatcher = passkeylink.matcher(source);
        if  (sizeMatcher.find()){
            driver.get("https://" + this.domain + "/" + sizeMatcher.group().substring(6));
        }
        if (driver.getPageSource().contains("passkey=")){
            source = driver.getPageSource();
            passkey = source.substring(source.indexOf("passkey=") + 8, source.indexOf("passkey=") + 40);
        } else {
            System.out.println("Cannot acquire passkey");
        }
    }

    private void getFreeIDs() {

        ArrayList<String> urls = new ArrayList<>();
        String originalString = null;
        WebDriverWait webDriverWait = new WebDriverWait(driver, 10);
        driver.get(url);
        String s = driver.getPageSource();

        System.out.println("Searching torrent links from " + driver.getCurrentUrl() + "...");
        try {
            webDriverWait.until(ExpectedConditions.presenceOfElementLocated(By.className("torrents")));
            originalString = driver.findElementByClassName("torrents").getAttribute("outerHTML");
        } catch (Exception e) {
            webDriverWait.until(ExpectedConditions.presenceOfElementLocated(By.className("torrent_list")));
            originalString = driver.findElementByClassName("torrent_list").getAttribute("outerHTML");
        }

        String searchString = originalString;
        String regexString = "id=[0-9]*.*[fF]ree";
        Pattern datePattern = Pattern.compile(regexString);
        Matcher dateMatcher = datePattern.matcher(searchString);
        int beEndIndex = 0;

        double size = 0;
        if (this.max == -1 || this.max == 0) this.max = 65535;
        String id = "";
        String subString = "";
        while(dateMatcher.find()) {

            subString = dateMatcher.group();

            Pattern idPattern = Pattern.compile("id=[0-9]*");
            Matcher idMatcher = idPattern.matcher(subString);
            if  (idMatcher.find()){
                id  = idMatcher.group().substring(3);
                if ("".equals(id)){
                    System.out.println("Cannot find torrent id.");
                    System.exit(106);
                }
            }

            Pattern sizeAndUnitPattern = Pattern.compile("id=" + id + ".*[GM][B]");
            Matcher sizeAndUnitMatcher = sizeAndUnitPattern.matcher(searchString.substring(searchString.indexOf(id)));
            if  (sizeAndUnitMatcher.find()){
                String sizeAndUnitString = sizeAndUnitMatcher.group().substring(sizeAndUnitMatcher.group().lastIndexOf("class"));
                Pattern sizePattern = Pattern.compile("[1-9]\\d*\\.\\d*|0\\.\\d*[1-9]\\d*|[1-9]\\d*");
                Matcher sizeMatcher = sizePattern.matcher(sizeAndUnitString);
                if  (sizeMatcher.find()) {
                    size = Double.valueOf(sizeMatcher.group());
                }
                if ("MB".equals(sizeAndUnitString.substring(sizeAndUnitString.length() - 2))){
                    size /= 1024;
                }
            }
            if (!this.urls.contains("https://" + domain + "/download.php?id=" + id + "&passkey=" + this.passkey)) {
                if (size >= this.min && size <= this.max) {
                    System.out.println("Get torrent from " + domain + ", id: " + id + ", size: " + new DecimalFormat("#0.00").format(size)  + " GB");
                    this.urls.add("https://" + domain + "/download.php?id=" + id + "&passkey=" + this.passkey);
                    this.newUrls.add("https://" + domain + "/download.php?id=" + id + "&passkey=" + this.passkey);
                }
            } else {
                System.out.println("Already added this torrent, id: " + id + "... Skip");
            }
            int subIndex = searchString.indexOf(subString);
            int subLength = subString.length();
            beEndIndex = subIndex + subLength + beEndIndex;
            searchString = originalString.substring(beEndIndex);
            dateMatcher = datePattern.matcher(searchString);
        }
    }

    @Override
    public void run() {
        this.getFreeIDs();
        qBittorrent.setUrls(this.newUrls);
        qBittorrent.addTorrents();
        this.newUrls = new ArrayList<String>();
    }
}
