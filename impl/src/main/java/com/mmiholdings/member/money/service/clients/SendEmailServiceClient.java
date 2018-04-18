package com.mmiholdings.member.money.service.clients;

import com.mmiholdings.multiply.service.communications.email.SendEmailException;
import com.mmiholdings.multiply.service.communications.email.SendEmailRequest;
import com.mmiholdings.multiply.service.communications.email.SendEmailResponse;
import lombok.extern.java.Log;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Log
public class SendEmailServiceClient extends AbstractClient {

    public SendEmailServiceClient(String url) {
        super(url);
    }

    public SendEmailServiceClient() {
        this("http://localhost:9080/multiply-communications-impl/rest");
    }

    public SendEmailResponse sendEmail(SendEmailRequest sendEmailRequest) throws SendEmailException {
        WebTarget resource = getResource(getUrl(), MediaType.APPLICATION_JSON_TYPE, "email", "send");

        Response response = resource.request().accept(MediaType.APPLICATION_JSON).post(Entity.json(sendEmailRequest));

        validateUnexpectedResponse(response);
        return response.readEntity(SendEmailResponse.class);
    }

    private void validateUnexpectedResponse(Response response) {
        if (response.getStatus() != Response.Status.OK.getStatusCode()) {
            throw new RuntimeException("Unexpected status code from SendEmailService: " + response.getStatus());
        }
    }
}
