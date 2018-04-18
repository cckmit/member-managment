package com.mmiholdings.member.money.service.clients;

import com.mmiholdings.shared.library.rest.client.ClientBuilderHelper;
import lombok.Getter;
import lombok.Setter;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;

public class AbstractClient {

    private ClientBuilder clientBuilder;
    private Client client;

    protected static final int ONE_MINUTE = 60000;

    @Getter
    @Setter
    private String url;

    protected AbstractClient(String url) {
        this.url = url;
        clientBuilder = ClientBuilderHelper.createClientBuilder();
    }

    protected Client getClient() {
        if (client == null) {
            client = clientBuilder.build();
            client.property("javax.xml.ws.client.connectionTimeout", ONE_MINUTE);
            client.property("javax.xml.ws.client.receiveTimeout", ONE_MINUTE);
        }

        return client;
    }

    protected WebTarget getResource(String url, MediaType mediaType, String... path) {
        WebTarget webTarget =  getClient().target(url);
        for (String p : path) {
            webTarget = webTarget.path(p);
        }
        if(mediaType != null){
            webTarget.request(mediaType);
        }else{
            webTarget.request(MediaType.APPLICATION_JSON);
        }
        return webTarget;
    }
}
