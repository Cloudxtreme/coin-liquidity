package com.coinliquidity.core.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;

public class HttpUtil {

    public static JsonNode get(final String url) {
        final Client client = ClientBuilder.newClient();
        final WebTarget target = client.target(url);
        final Response response = target.request().get();
        try {
            return new ObjectMapper().readTree(response.readEntity(InputStream.class));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}