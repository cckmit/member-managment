package com.mmiholdings.client.money.member;

import com.mmiholdings.member.money.api.MemberManagementException;
import com.mmiholdings.member.money.api.dto.CashbackDeposit;
import com.mmiholdings.service.money.commerce.member.CustomerNotFound;
import com.mmiholdings.service.money.commerce.payment.CashDepositException;
import com.mmiholdings.shared.library.rest.client.ClientBuilderHelper;
import lombok.extern.java.Log;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.logging.Level;

@Log
public class MemberServiceClient {

    private static final String MONEY = "money";
    private static final String CASHBACK = "cashback";
    private static final String CREDIT = "credit";


    private ClientBuilder clientBuilder = ClientBuilderHelper.createClientBuilder();
    private Client client;
    private static final int ONE_MINUTE = 60000;
    private final String url;

    public String depositCashback(CashbackDeposit cashbackDeposit) throws MemberManagementException,
            CashDepositException, CustomerNotFound {
        WebTarget resource = getResource(url,null,MONEY,CASHBACK,CREDIT,cashbackDeposit.getMemberReference(),
                String.valueOf(cashbackDeposit.getAmount()));
        log.log(Level.INFO, "Calling Cashback Deposit Service... {0}", resource.getUri());
        resource = getWebTarget(cashbackDeposit,resource);
        Response response = resource.request(MediaType.APPLICATION_JSON_TYPE).post(null);
        return parseResponse(response);
    }

    public MemberServiceClient(String url) {
        this.url = url;
    }

    public MemberServiceClient() {
        this("http://localhost:9080//member-management-api/rest");
    }

    private String parseResponse(Response response) {
        if (response.getStatus() != Response.Status.OK.getStatusCode()) {
            throw new RuntimeException(String.format("Unexpected status code from %s: %d, %s",
                    getClass().getSimpleName(),
                    response.getStatus(),
                    response.getHeaderString("reason")));
        }
        return response.readEntity(String.class);
    }

    private WebTarget getResource(String url, MediaType mediaType, String... path) {
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

    private WebTarget getWebTarget(CashbackDeposit deposit, WebTarget webResource) {
        if (deposit.getTransactionId() != null) {
            webResource = webResource.queryParam("transactionId",deposit.getTransactionId());
        }
        if (deposit.getDescription() != null) {
            webResource = webResource.queryParam("description", deposit.getDescription());
        }
        if (deposit.getUserId() != null) {
            webResource = webResource.queryParam("userId", deposit.getUserId());
        }
        return webResource;
    }

    private Client getClient() {
        if (client == null) {
            client = clientBuilder.build();
            client.property("javax.xml.ws.client.connectionTimeout", ONE_MINUTE);
            client.property("javax.xml.ws.client.receiveTimeout", ONE_MINUTE);
        }

        return client;
    }
}
