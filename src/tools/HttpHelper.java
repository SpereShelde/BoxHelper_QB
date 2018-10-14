package tools;

import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.util.*;

/**
 * Created by SpereShelde on 2018/7/3.
 */
public class HttpHelper {

    public static String doGet(String destination, String userAgent, String cookieValue, String host) throws IOException {

        String responseString = "";
        CloseableHttpClient httpclient = HttpClients.createDefault();

        // 创建httpget
        HttpGet httpget = new HttpGet(destination);
        httpget.setHeader("User-Agent", userAgent);
        httpget.setHeader("Cookie", "SID=" + cookieValue);
        httpget.setHeader("Host", host);

        // 执行get请求.
        CloseableHttpResponse response = httpclient.execute(httpget);
        try {
            // 获取响应实体
            HttpEntity entity = response.getEntity();
            // 打印响应状态
            if (entity != null && response.getStatusLine().toString().contains("200")) {
                // 打印响应内容
                responseString = EntityUtils.toString(entity) ;
            }
        } finally {
            response.close();
            httpclient.close();
        }
        return responseString;
    }

    public static boolean doPostFileForm(String destination, String userAgent, String cookieValue, String host, Map<String, String> contents) throws IOException {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpPost httpPost = new HttpPost(destination);
        //创建参数队列
        httpPost.addHeader("User-Agent", userAgent);
        httpPost.addHeader("Cookie", "SID=" + cookieValue);
        httpPost.addHeader("Host", host);
        MultipartEntityBuilder multipartEntityBuilder = MultipartEntityBuilder.create();
        contents.forEach((k,v) -> multipartEntityBuilder.addTextBody(k, v));
        httpPost.setEntity(multipartEntityBuilder.build());
        CloseableHttpResponse response = httpClient.execute(httpPost);
        HttpEntity entity = null;
        try {
            // 获取响应实体
            entity = response.getEntity();
        } finally {
            response.close();
            httpClient.close();
        }
        return entity != null && response.getStatusLine().toString().contains("200");//Not precise
    }

    public static boolean doPostNormalForm(String destination, String userAgent, String cookieValue, String host, Map<String, String> contents) throws IOException {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpPost httpPost = new HttpPost(destination);
        //创建参数队列
        httpPost.addHeader("User-Agent", userAgent);
        httpPost.addHeader("Cookie", "SID=" + cookieValue);
        httpPost.addHeader("Content-Type", "application/x-www-form-urlencoded");
        httpPost.addHeader("Host", host);

        List <NameValuePair> nvps = new ArrayList <NameValuePair>();
        contents.forEach((k,v) -> nvps.add(new BasicNameValuePair(k, v)));
        httpPost.setEntity(new UrlEncodedFormEntity(nvps));
        CloseableHttpResponse response = httpClient.execute(httpPost);
        HttpEntity entity = null;
        try {
            // 获取响应实体
            entity = response.getEntity();
        } finally {
            response.close();
            httpClient.close();
        }
        return entity != null && response.getStatusLine().toString().contains("200");//Not precise
    }

}
