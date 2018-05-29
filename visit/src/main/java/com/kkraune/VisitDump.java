package com.kkraune;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Iterator;

public class VisitDump {

    public static void main(String[] args) {
        String baseURL = "http://localhost:8080/document/v1/iddoc/iddoc/docid/";;
        String visitEndpoint = baseURL;
        String continuation = "";
        try {
            do {
                CloseableHttpClient httpClient = HttpClients.createDefault();
                HttpGet getRequest = new HttpGet(visitEndpoint);
                CloseableHttpResponse response = httpClient.execute(getRequest);
                try {
                    if (response.getStatusLine().getStatusCode() == 200) {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
                        ObjectMapper mapper = new ObjectMapper();
                        JsonNode node = mapper.readTree(reader);
                        Iterator<JsonNode> documents = node.path("documents").iterator();
                        while (documents.hasNext()) {
                            JsonNode document = documents.next();
                            ObjectNode put = (ObjectNode) document;
                            put.set("put", new TextNode(document.get("id").asText()));
                            put.remove("id");
                            System.out.println(document.toString());
                        }
                        continuation = node.path("continuation").asText();
                        visitEndpoint = baseURL + "?continuation=" + continuation;
                    }
                } finally {
                    response.close();
                }
            } while (!continuation.isEmpty());
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
}
