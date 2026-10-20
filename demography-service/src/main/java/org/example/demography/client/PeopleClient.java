package org.example.demography.client;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.Response;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@ApplicationScoped
public class PeopleClient {
    private static final String PEOPLE_URL = System.getProperty(
            "people.service.url",
            "https://localhost:8443/person-service/api/v1/people"
    );
    private static final int PAGE_SIZE = 20;

    public double hairColorPercentage(String color) {
        List<JsonObject> people = getAllPeople();
        if (people.isEmpty()) {
            return 0.0;
        }
        long matching = people.stream()
                .filter(person -> color.equals(person.getString("hairColor", "")))
                .count();
        return matching * 100.0 / people.size();
    }

    public double eyeColorPercentage(String color) {
        List<JsonObject> people = getAllPeople();
        if (people.isEmpty()) {
            return 0.0;
        }
        long matching = people.stream()
                .filter(person -> color.equals(person.getString("eyeColor", "")))
                .count();
        return matching * 100.0 / people.size();
    }

    private List<JsonObject> getAllPeople() {
        List<JsonObject> people = new ArrayList<>();
        Client client = ClientBuilder.newBuilder()
                .connectTimeout(3, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .sslContext(buildSslContext())
                .build();
        try (client) {
            int page = 0;
            while (true) {
                try (Response response = client.target(PEOPLE_URL)
                        .queryParam("page", page)
                        .queryParam("pageSize", PAGE_SIZE)
                        .request()
                        .get()) {
                    if (response.getStatus() != Response.Status.OK.getStatusCode()) {
                        throw new WebApplicationException(Response.status(502).build());
                    }
                    JsonArray result = response.readEntity(JsonArray.class);
                    for (int index = 0; index < result.size(); index++) {
                        people.add(result.getJsonObject(index));
                    }
                    if (result.size() < PAGE_SIZE) {
                        return people;
                    }
                    page++;
                }
            }
        }
    }

    private SSLContext buildSslContext() {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("people-service-truststore.p12")) {
            if (input == null) {
                throw new ProcessingException("People service truststore is missing");
            }
            KeyStore trustStore = KeyStore.getInstance("PKCS12");
            trustStore.load(input, "changeit".toCharArray());
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(trustStore);
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustManagerFactory.getTrustManagers(), null);
            return sslContext;
        } catch (IOException | GeneralSecurityException e) {
            throw new ProcessingException("Could not load people service truststore", e);
        }
    }
}
