package models;

import tools.ConvertJson;
import tools.HttpHelper;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by SpereShelde on 2018/7/7.
 */
public class QBChecker implements Runnable {

    private String sessionID;
    private String webUI;
    private long currentSize = 0;
    private long space;
    private String[] action;

    public QBChecker(String webUI, String sessionID, long space, String[] action) {
        this.sessionID = sessionID;
        this.webUI = webUI;
        this.space = space;
        this.action = action;
    }

    @Override
    public void run() {
        ArrayList<Torrent> torrents = null;
        int amount = Integer.parseInt(action[0]);
        try {
            String act = "";
            switch (action[1]){
                default:
                case "slow": act = "upspeed"; break;
                case "add": act = "added_on"; break;
                case "complete": act = "completion_on"; break;
                case "active": act = "last_activity"; break;
                case "small":
                case "large": act = "size"; break;
                case "ratio": act = "ratio"; break;
            }
            torrents = ConvertJson.convertTorrents(HttpHelper.doGet(webUI + "/query/torrents?filter=completed&category=BoxHelper&sort=" + act, "Fiddler", sessionID, "127.0.0.1"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        torrents.forEach(torrent -> {
            currentSize += torrent.getSize();
        });
        if (currentSize <= space){
            System.out.println("Current space use is " + new DecimalFormat("#0.00").format(currentSize / (double)1073741824) + " GB, under space limit.");
        }else {
            System.out.println("Current space use is " + new DecimalFormat("#0.00").format(currentSize / (double)1073741824) + " GB, beyond space limit.\nBegin delete torrents...");
            List<Torrent> torrentsToBeRemoved;
            switch (action[1]){
                default:
                case "slow":
                case "add":
                case "complete":
                case "active":
                case "small":
                case "ratio":
                    if (amount <= torrents.size()) {
                        torrentsToBeRemoved = torrents.subList(0, amount);
                    }else {
                        torrentsToBeRemoved = torrents;
                    }
                    break;
                case "large":
                    if (amount <= torrents.size()) {
                        torrentsToBeRemoved = torrents.subList(torrents.size() - amount - 1, torrents.size());
                    }else {
                        torrentsToBeRemoved = torrents;
                    }
                    break;
            }
            Map<String, String> contents = new HashMap();
            StringBuilder hashs = new StringBuilder();
            torrentsToBeRemoved.forEach(torrent -> {
                hashs.append(torrent.getHash() + "|");
            });
            hashs.deleteCharAt(hashs.length() - 1);
            contents.put("hashes", hashs.toString());
            try {
                HttpHelper.doPostNormalForm(webUI + "/command/deletePerm", "Fiddler", sessionID, "127.0.0.1", contents);
                System.out.println("End of deleting torrents.");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
