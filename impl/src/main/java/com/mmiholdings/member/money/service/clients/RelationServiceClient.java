package com.mmiholdings.member.money.service.clients;

import com.mmiholdings.member.money.service.clients.dto.MemberUpdateRequest;
import com.mmiholdings.service.money.commerce.member.CommerceException;
import com.mmiholdings.service.money.commerce.member.Customer;
import com.mmiholdings.service.money.commerce.member.CustomerNotFound;
import com.mmiholdings.service.money.commerce.member.IdentificationDocument;
import com.mmiholdings.service.money.commerce.relation.CommerceResponse;
import lombok.NoArgsConstructor;
import lombok.extern.java.Log;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.logging.Level;

import static com.mmiholdings.service.money.commerce.relation.CommerceResponse.Response.FAILURE;
import static javax.ws.rs.client.Entity.entity;

@Log
@NoArgsConstructor
public class RelationServiceClient extends CommerceClient {

    public RelationServiceClient(String url) {
        super(url);
    }

    /**
     * Creates or update customer in Traderoot
     * @param updateRequest
     * @return member reference
     * @throws CommerceException
     */
    public String createOrUpdateMember(MemberUpdateRequest updateRequest) throws CommerceException {
        WebTarget resource = getResource(getUrl(),null, "relation", "createOrUpdateMember");

        log.log(Level.INFO, "Calling Create / Update Member Service... {0}", resource.getUri());
        resource = getWebTarget(updateRequest, resource);

        Response response = resource.request(MediaType.APPLICATION_JSON)
                .post(entity(updateRequest.getCustomer(), MediaType.APPLICATION_JSON));
        return parseResponse(response);
    }

    /**
     * Finds customer using Traderoot Member Reference number (i.e. Money Management Number)
     *
     * @param memberReference
     * @return customer
     */
    public Customer findMemberUsingMemberReference(String memberReference) throws CustomerNotFound {
        WebTarget resource = getResource(getUrl(), null,
                "member", "customerReference", memberReference);

        log.log(Level.INFO, "Calling Find Member By Member Number Service... {0}", resource.getUri());
        Response response = resource.request().accept(MediaType.APPLICATION_JSON).get();
        checkStatusCodes(response);

        if(response.getStatus() == Response.Status.NO_CONTENT.getStatusCode()) {
            throw new CustomerNotFound(memberReference);
        }
        return response.readEntity(Customer.class);
    }

    /**
     * Finds customer using Traderoot Member Reference number (i.e. Money Management Number)
     *
     * @param idNumber
     * @return customer
     */
    public Customer findMemberUsingIdNumber(String idNumber, IdentificationDocument.Type idType) throws CustomerNotFound {
        WebTarget resource = getResource(getUrl(), null,
                "relation", "find", "idNumber", idType.name(), idNumber);

        log.log(Level.INFO, "Calling Find Member By Member Number Service... {0}", resource.getUri());
        Response response = resource.request().accept(MediaType.APPLICATION_JSON).get();
        checkStatusCodes(response);

        if(response.getStatus() == Response.Status.NO_CONTENT.getStatusCode()) {
            throw new CustomerNotFound(IdentificationDocument.getIdentificationDocument(idNumber, idType));
        }
        return response.readEntity(Customer.class);
    }

    private String parseResponse(Response response) throws CommerceException {
        checkStatusCodes(response);
        CommerceResponse commerceResponse = response.readEntity(CommerceResponse.class);

        if(commerceResponse.getResponse() == FAILURE) {
            throw new CommerceException(commerceResponse.getErrors());
        }
        return commerceResponse.getReference();
    }

    private void checkStatusCodes(Response response) {
        if (!(response.getStatus() == Response.Status.OK.getStatusCode()
                || response.getStatus() == Response.Status.NO_CONTENT.getStatusCode())) {
            throw new RuntimeException(String.format("Unexpected status code from %s: %d",
                    getClass().getSimpleName(),
                    response.getStatus()));
        }
    }

    private WebTarget getWebTarget(MemberUpdateRequest updateRequest, WebTarget webResource) {
        if (updateRequest.getUserId() != null) {
            webResource = webResource.queryParam("agentId", updateRequest.getUserId());
        }
        return webResource;
    }
}
