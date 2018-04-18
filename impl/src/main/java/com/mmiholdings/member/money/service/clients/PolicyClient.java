package com.mmiholdings.member.money.service.clients;

import com.mmiholdings.multiply.service.policy.Policy;
import com.mmiholdings.multiply.service.policy.PolicyNumber;
import com.mmiholdings.shared.library.rest.client.ClientBuilderHelper;
import lombok.extern.java.Log;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.logging.Level;

import static java.lang.String.valueOf;

// TODO: Should we implement PolicyService ? That ties us to api.jar. Then we might as well use RMI ?
// TODO: > We need the Policy class in api.jar anyway as it is what we need to deserialize.
@Log
public class PolicyClient extends AbstractClient {

    private static final int ONE_MINUTE_IN_MS = 60000;

    private ClientBuilder clientBuilder = ClientBuilderHelper.createClientBuilder();

    private Client client;

    public PolicyClient(String url) {
        super(url);
    }

    public PolicyClient() {
        this("http://localhost:9080/multiply-policy-impl/rest");
    }

    public Policy getPolicy(PolicyNumber policyNumber) {
        // @Path("/{productCode}/{number}")
        WebTarget resource = getResource(getUrl(), null, POLICY, policyNumber.getProductCode(),
                valueOf(policyNumber.getNumber()));
        log.log(Level.FINE, "Calling PolicyService with URL {0}", resource.getUri().toString());
        Response response = resource.request().accept(MediaType.APPLICATION_JSON).get();

        log.log(Level.FINE, "PolicyService returned status {0}", response.getStatus());
        if (response.getStatus() == 404) {
            return null;
        }

        validateUnexpectedResponse(response);

        return returnObject(Policy.class, response);
    }

    public Policy getPolicy(Long clientNumber) {
        // @Path("/{clientNumber}")
        WebTarget resource = getResource(getUrl(), null, POLICY, valueOf(clientNumber));
        log.log(Level.FINE, "Calling PolicyService with URL {0}", resource.getUriBuilder().build());

        Response response = resource.request().accept(MediaType.APPLICATION_JSON).get();
        log.log(Level.FINE, "PolicyService returned status {0}", response.getStatus());

        if (response.getStatus() == 404) {
            return null;
        }

        validateUnexpectedResponse(response);

        return returnObject(Policy.class, response);
    }

    public Policy getPreferredPolicy(Long clientNumber) {
        //@Path("/preferred/{clientNumber}")
        WebTarget resource = getResource(getUrl(), null, POLICY, PREFERRED, valueOf(clientNumber));
        log.log(Level.FINE, "Calling PolicyService with URL {0}", resource.getUriBuilder().build());

        Response response = resource.request().accept(MediaType.APPLICATION_JSON).get();
        log.log(Level.FINE, "PolicyService returned status {0}", response.getStatus());

        validateUnexpectedResponse(response);

        return returnObject(Policy.class, response);
    }

    public Policy getPreferredPolicy(PolicyNumber policyNumber) {
        //@Path("/preferred/{productCode}/{number}")

        WebTarget resource = getResource(getUrl(), null, POLICY, PREFERRED,
                policyNumber.getProductCode(), valueOf(policyNumber.getNumber()));
        log.log(Level.FINE, "Calling PolicyService with URL {0}", resource.getUriBuilder().build());

        Response response = resource.request().accept(MediaType.APPLICATION_JSON).get();
        log.log(Level.FINE, "PolicyService returned status {0}", response.getStatus());

        validateUnexpectedResponse(response);

        return returnObject(Policy.class, response);
    }

    public <T> T returnObject(Class<T> type, Response response) {
        if (response.getStatus() == Response.Status.NO_CONTENT.getStatusCode()) {
            return null;
        }

        return response.readEntity(type);
    }

    private void validateUnexpectedResponse(Response response) {
        if (response.getStatus() != Response.Status.OK.getStatusCode() &&
                response.getStatus() != Response.Status.NO_CONTENT.getStatusCode())
            throw new RuntimeException("Unexpected status code from PolicyService: " + response.getStatus());
    }

    private static final String PREFERRED = "preferred";
    private static final String CANCEL_CODES = "cancelCodes";
    private static final String POLICY = "policy";
}
