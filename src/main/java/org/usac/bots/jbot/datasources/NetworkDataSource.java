package org.usac.bots.jbot.datasources;

import org.apache.http.HttpEntity;
import org.apache.http.StatusLine;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.fluent.Request;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.springframework.boot.configurationprocessor.json.JSONObject;
import org.springframework.stereotype.Repository;
import org.w3c.dom.Document;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Repository
public class NetworkDataSource extends LogGrootDataSource {

    public NetworkDataSource() {
        InetAddress test = resolve("google.com");
        if (test == null) {
            setEnabled(false);
            log.error("Cannot resolve google.com. Network or DNS down?");
        }
    }

    public InetAddress resolve(String hostname) {
        InetAddress result = null;
        try {
            result = InetAddress.getByName(hostname);
        } catch (UnknownHostException e) {
            log.error(e.getMessage());
        }

        return result;
    }

    public Map<String, Object> wget(String url) {
        final Map<String, Object> result = new HashMap<>();
        try {

            Request.Get(url)
                    .connectTimeout(1000)
                    .socketTimeout(1000)
                    .execute()
                    .handleResponse(
                    (ResponseHandler<Document>) response -> {
                        StatusLine statusLine = response.getStatusLine();
                        HttpEntity entity = response.getEntity();

                        result.put("status", statusLine.getStatusCode());
                        result.put("contentLength", entity.getContentLength());
                        result.put("content", EntityUtils.toString(entity));

                        return null;
                    }
            );


        } catch (Exception e) {
            log.error(e.getMessage());
        }

        return (result.size() == 0) ? null : result;
    }

    public JSONObject wgetJSONBasicAuth(String url, String user, String token) {
        JSONObject object = null;
        try {

            String response = Request.Get(url)
                    .connectTimeout(1000)
                    .socketTimeout(1000)
                    .setHeader("Authorization", "Basic " + Base64.getEncoder().encodeToString((user + ":" + token).getBytes()))
                    .setHeader("Content-Type", "application/json")
                    .execute()
                    .returnContent().asString();
            object = new JSONObject(response);



        } catch (Exception e) {
            log.error(e.getMessage());
        }

        return object;
    }

    public JSONObject postJSONBasicAuth(String url, String user, String token, JSONObject json) {
        JSONObject object = null;
        try {
            String response = Request.Post(url)
                    .connectTimeout(1000)
                    .socketTimeout(1000)
                    .setHeader("Authorization", "Basic " + Base64.getEncoder().encodeToString((user + ":" + token).getBytes()))
                    .setHeader("Content-Type", "application/json")
                    .body(new StringEntity(json.toString()))
                    .execute()
                    .returnContent().asString();
            object = new JSONObject(response);
        } catch (Exception e) {
            log.error(e.getMessage());
        }

        return object;
    }

}
