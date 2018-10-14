package tools;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import models.Torrent;
import org.openqa.selenium.Cookie;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;

import static java.nio.file.Paths.get;

/**
 * Created by SpereShelde on 2018/6/11.
 */
public class ConvertJson {

    public static Map convertConfigure(String jsonFileName) throws IOException {

        ArrayList<String> action = new ArrayList<>();
        Map configures = new HashMap();
        JsonParser jsonParser = new JsonParser();
        String content = null;

        if (new File(jsonFileName).exists()){
            content = new String(Files.readAllBytes(get(jsonFileName)));
        }

        JsonObject object = (JsonObject) jsonParser.parse(content);

        String webUI = object.get("webUI").getAsString();
        if (webUI.endsWith("/")) webUI = webUI.substring(0, webUI.length() - 1);
        configures.put("webUI", webUI);
        configures.put("sessionID", object.get("sessionID").getAsString());
        configures.put("diskLimit", object.get("diskLimit").getAsInt());
        JsonArray arrayAction = object.get("actionAfterLimit").getAsJsonArray();
        for (JsonElement a: arrayAction) {
            if (a != null || !"".equals(a.getAsString())) action.add(a.getAsString());
        }
        configures.put("action", action.toArray());
        configures.put("runningCycleInSec", object.get("runningCycleInSec").getAsInt());
        JsonArray arrayURL = object.get("url&speedLimit").getAsJsonArray();
        ArrayList<String> urls = new ArrayList<>();
        for (JsonElement a: arrayURL) {
            JsonArray url = a.getAsJsonArray();
            String min = url.get(1).getAsString();
            String max = url.get(2).getAsString();
            String down = url.get(3).getAsString();
            String up = url.get(4).getAsString();
            if ("".equals(min)) min = "-1";
            if ("".equals(max)) max = "-1";
            if ("".equals(down)) down = "-1";
            if ("".equals(up)) up = "-1";
            if (!"".equals(url.get(0).getAsString())) {
                configures.put(url.get(0).getAsString(), min + "/" + max + "/" + down + "/" + up);
                urls.add(url.get(0).getAsString());
            }
        }
        configures.put("urls", urls);
        if (object.has("email")) {
            configures.put("email", object.get("email").getAsString());
        }
        if (object.has("sendgridKey")) {
            configures.put("sendgridKey", object.get("sendgridKey").getAsString());
        }
        return configures;
    }

    public static ArrayList<Cookie> convertCookie(String jsonFileName) throws IOException {

        String domain = "", name = "", value = "", path = "";
        Long expirationDate = Long.valueOf(0);
        boolean hostOnly = false, secure = false;
        ArrayList<Cookie> cookies = new ArrayList<>();

        JsonParser jsonParser = new JsonParser();
        String content = "";

        if (new File(jsonFileName).exists()){
            content = new String(Files.readAllBytes(get(jsonFileName)));
        }

        JsonArray jsonArray = (JsonArray) jsonParser.parse(content);

        Long timestamp = null;
        for (JsonElement array: jsonArray) {
            JsonObject object = array.getAsJsonObject();
            if (object.has("domain")) {
                domain = object.get("domain").getAsString();
            }
            if (object.has("expirationDate")) {
                expirationDate = Math.floorDiv(object.get("expirationDate").getAsLong() * 1000, 1);
            }
            if (object.has("secure")) {
                hostOnly = object.get("secure").getAsBoolean();
            }
            if (object.has("httpOnly")) {
                secure = object.get("httpOnly").getAsBoolean();
            }
            if (object.has("name")) {
                name = object.get("name").getAsString();
            }
            if (object.has("path")) {
                path = object.get("path").getAsString();
            }
            if (object.has("value")) {
                value = object.get("value").getAsString();
            }
            cookies.add(new Cookie(name, value, domain, path, new Date(expirationDate)));
        }
        return cookies;
    }

    public static ArrayList<Torrent> convertTorrents(String content){
        ArrayList<Torrent> torrents = new ArrayList<>();
        JsonParser jsonParser = new JsonParser();
        String name, hash;
        long added_on, completion_on, last_activity, size;
        int dl_limit, up_limit, num_incomplete;
        double ratio;

        JsonArray jsonArray = (JsonArray) jsonParser.parse(content);
        for (JsonElement array: jsonArray) {
            JsonObject object = array.getAsJsonObject();
            added_on = object.get("added_on").getAsLong();
            completion_on = object.get("completion_on").getAsLong();
            dl_limit = object.get("dl_limit").getAsInt();
            hash = object.get("hash").getAsString();
            last_activity = object.get("last_activity").getAsLong();
            name = object.get("name").getAsString();
            num_incomplete = object.get("num_incomplete").getAsInt();
            ratio = object.get("ratio").getAsDouble();
            size = object.get("size").getAsLong();
            up_limit = object.get("up_limit").getAsInt();
            torrents.add(new Torrent(name, hash, added_on, completion_on, last_activity, size, dl_limit, up_limit, num_incomplete, ratio));
        }
        return torrents;
    }

}


