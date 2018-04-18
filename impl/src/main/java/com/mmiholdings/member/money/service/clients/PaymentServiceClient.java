package com.mmiholdings.member.money.service.clients;

import com.mmiholdings.member.money.service.clients.dto.DepositRequest;
import com.mmiholdings.service.money.commerce.payment.CashDepositException;
import lombok.NoArgsConstructor;
import lombok.extern.java.Log;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.logging.Level;

@Log
@NoArgsConstructor
public class PaymentServiceClient extends CommerceClient {

    public PaymentServiceClient(String url) {
        super(url);
    }

    public String cashDeposit(DepositRequest depositRequest) throws CashDepositException {
        WebTarget resource = getResource(getUrl(), null, "account", "credit",
                depositRequest.getAccountReference(),
                String.valueOf(depositRequest.getAmount()));

        log.log(Level.INFO, "Calling Cashback Deposit Service... {0}", resource.getUri());
        resource = getWebTarget(depositRequest, resource);
        Response response = resource.request(MediaType.APPLICATION_JSON).post(null);
        return parseResponse(response);
    }

    private String parseResponse(Response response) throws CashDepositException {
        if (response.getStatus() == Response.Status.OK.getStatusCode()) {
            return response.readEntity(String.class);
        }
        String reason = response.getHeaderString("Reason");
        if(reason == null) {
            reason = response.getStatusInfo().getReasonPhrase();
        }
        throw new CashDepositException(response.getStatus(), reason);
    }

    private WebTarget getWebTarget(DepositRequest deposit, WebTarget webResource) {
        if (deposit.getTransactionId() != null) {
            webResource = webResource.queryParam("transactionId", deposit.getTransactionId());
        }
        if (deposit.getDescription() != null) {
            webResource = webResource.queryParam("description", deposit.getDescription());
        }
        if (deposit.getUserId() != null) {
            webResource = webResource.queryParam("userId", deposit.getUserId());
        }
        return webResource;
    }

    public String getUrl() {
        return super.getUrl();
    }

    public void setUrl(String url) {
        super.setUrl(url);
    }
}
