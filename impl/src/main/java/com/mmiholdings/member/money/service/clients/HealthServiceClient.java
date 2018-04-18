package com.mmiholdings.member.money.service.clients;

import com.mmiholdings.multiply.service.health.HealthPolicy;
import com.mmiholdings.service.money.util.LoggingInterceptor;
import com.mmiholdings.shared.library.rest.client.ClientBuilderHelper;
import lombok.extern.java.Log;
import net.jodah.failsafe.Failsafe;
import net.jodah.failsafe.RetryPolicy;
import net.jodah.failsafe.SyncFailsafe;

import javax.annotation.PostConstruct;
import javax.ejb.Stateless;
import javax.interceptor.Interceptors;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import static com.mmiholdings.multiply.service.entity.client.CDIEntityClient.HEALTH;

@Log
@Stateless
@Interceptors(LoggingInterceptor.class)
public class HealthServiceClient {
	private static final String KEY_HEALTH_URL = "healthServiceUrl";
	private static final String POLICY = "policy";
	private static final int ONE_MINUTE = 60000;

	private ClientBuilder clientBuilder = ClientBuilderHelper.createClientBuilder();
	private RetryPolicy retryPolicy = new RetryPolicy();
	private Client client;
	private String url;

	@PostConstruct
	public void init() {
		url = System.getProperty(KEY_HEALTH_URL);
		this.retryPolicy.retryOn(ProcessingException.class)
				.withBackoff(5, 30, TimeUnit.SECONDS)
				.withJitter(0.5)
				.withMaxRetries(5);
	}

	public HealthPolicy getHealthMemberDetails(Long healthPolicyNumber) throws HealthPolicyNotFoundException {
		WebTarget resource = getResource(url, null, HEALTH, POLICY, String.valueOf(healthPolicyNumber));
		Response response = executeRequest(() -> {
			log.log(Level.INFO, "Calling HealthService with URL {0}", resource.toString());
			Response response1 = resource.request(MediaType.APPLICATION_JSON_TYPE).get();
			log.log(Level.INFO, "HealthService returned status {0}", response1.getStatus());
			return response1;
		});
		validateResponse(healthPolicyNumber, response);
		return response.readEntity(HealthPolicy.class);
	}

	private void validateResponse(Long healthPolicyNumber, Response response) throws HealthPolicyNotFoundException {
		int statusCode = response.getStatus();
		if (statusCode == Response.Status.NO_CONTENT.getStatusCode()
				|| statusCode == Response.Status.NOT_FOUND.getStatusCode()) {
			throw new HealthPolicyNotFoundException("Health policy with number [" + healthPolicyNumber + "] not found");
		}
		if (statusCode != Response.Status.OK.getStatusCode()) {
			throw new RuntimeException("Unexpected status code from HealthService: " + statusCode);
		}
	}

	private WebTarget getResource(String url, MediaType mediaType, String... path) {
		WebTarget webTarget = getClient().target(url);
		for (String p : path) {
			webTarget = webTarget.path(p);
		}
		if (mediaType != null) {
			webTarget.request(mediaType);
		} else {
			webTarget.request(MediaType.APPLICATION_JSON);
		}
		return webTarget;
	}

	private Client getClient() {
		if (client == null) {
			client = clientBuilder.build();
			client.property("javax.xml.ws.client.connectionTimeout", ONE_MINUTE);
			client.property("javax.xml.ws.client.receiveTimeout", ONE_MINUTE);
		}

		return client;
	}

	private <T> T executeRequest(Callable<T> callable) {
		SyncFailsafe<T> failsafe = Failsafe.with(retryPolicy);
		return failsafe.get(callable);
	}
}
