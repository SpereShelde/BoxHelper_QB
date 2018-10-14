import com.gargoylesoftware.htmlunit.BrowserVersion;
import models.NexusPHP;
import models.QBChecker;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import tools.ConvertJson;
import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.sun.org.apache.xalan.internal.lib.ExsltDatetime.time;
import static java.lang.Thread.sleep;

/**
 * Created by SpereShelde on 2018/6/6.
 */
public class BoxHelper {

    private Map configures = new HashMap();
    private Map cookies = new HashMap();
    private Map drivers = new HashMap();

    private void getConfigures() {// Get configures from file.

        try {
            this.configures = ConvertJson.convertConfigure("config.json");
        } catch (IOException e) {
            e.printStackTrace();
        }

        ArrayList<Path> jsonFiles = new ArrayList<>();

        try {
            DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get("cookies"));
            for(Path path : stream){
                if (path.getFileName().toString().endsWith(".json")) {
                    jsonFiles.add(path);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("Loading cookies ...");
        for (Path path: jsonFiles) {
            try {
                String domainName = path.getFileName().toString();
                cookies.put(domainName.substring(0, domainName.lastIndexOf(".")), ConvertJson.convertCookie(path.toString()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        ArrayList<String> urls = (ArrayList<String>) this.configures.get("urls");
        urls.forEach(url -> {
            HtmlUnitDriver driver = new HtmlUnitDriver(BrowserVersion.FIREFOX_45, false);
            String domain = url.substring(url.indexOf("//") + 2, url.indexOf("/", url.indexOf("//") + 2));
            driver.get("http://" + domain);
            ArrayList<Cookie> cookiesT = (ArrayList) cookies.get(domain);
            cookiesT.forEach(cookie -> driver.manage().addCookie(cookie));
            drivers.put(url, driver);
        });

        System.out.println("Initialization done.");
    }

    public static void main(String[] args) {

        Logger logger = Logger.getLogger("");
        logger.setLevel(Level.OFF);
        BoxHelper boxHelper = new BoxHelper();
        boxHelper.getConfigures();
        int cpuThreads = Runtime.getRuntime().availableProcessors();
        int count  = 1;

        ArrayList<NexusPHP> nexusPHPS = new ArrayList<>();
        boxHelper.drivers.forEach((url, driver) -> {
            String[] urlAndLimit = boxHelper.configures.get(url).toString().split("/");
            Map qbconfig = new HashMap();
            qbconfig.put("webUI", boxHelper.configures.get("webUI").toString());
            qbconfig.put("sessionID", boxHelper.configures.get("sessionID").toString());
            nexusPHPS.add(new NexusPHP(url.toString(), Double.parseDouble(urlAndLimit[0]), Double.parseDouble(urlAndLimit[1]), Double.parseDouble(urlAndLimit[2]),Double.parseDouble(urlAndLimit[3]), (HtmlUnitDriver)driver, qbconfig));
        });

        while (true){
            ExecutorService executorService = Executors.newFixedThreadPool(cpuThreads);
            System.out.println("\nBoxHelper " + count + " begins at " + time());
            Object[] actions = (Object[]) boxHelper.configures.get("action");
            executorService.submit(new QBChecker(boxHelper.configures.get("webUI").toString(), boxHelper.configures.get("sessionID").toString(), new Long(boxHelper.configures.get("diskLimit").toString()).longValue() * 1073741824, new String[]{actions[0].toString(), actions[1].toString()}));
            nexusPHPS.forEach(nexusPHP -> {
                executorService.submit(nexusPHP);
            });
            executorService.shutdown();
            try {
                sleep((long) (1000*Double.valueOf(boxHelper.configures.get("runningCycleInSec").toString())));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            count++;
        }
    }
}
