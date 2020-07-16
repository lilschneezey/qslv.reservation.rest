package qslv.reservation.rest;

import java.util.Collections;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import qslv.common.TimedResponse;
import qslv.common.TraceableRequest;
import qslv.transaction.request.ReservationRequest;
import qslv.transaction.response.ReservationResponse;
import qslv.util.RestClientElapsedTimeSLILogger;

@Repository
public class TransactionDao {
	private static final Logger log = LoggerFactory.getLogger(TransactionDao.class);
	private ParameterizedTypeReference<TimedResponse<ReservationResponse>> typeReference 
		= new ParameterizedTypeReference<TimedResponse<ReservationResponse>>() {};

	@Autowired
	private ConfigProperties config;
	@Autowired
	RestClientElapsedTimeSLILogger restTimer;
	@Autowired
	private RestTemplate restTemplate;
	@Autowired
	private RetryTemplate retryTemplate;

	public void setConfig(ConfigProperties config) {
		this.config = config;
	}
	public void setTemplate(RestTemplate template) {
		this.restTemplate = template;
	}
	public void setRestTimer( RestClientElapsedTimeSLILogger restTimer) {
		this.restTimer=restTimer;
	}
	
	public ReservationResponse recordReservation(final Map<String, String> callingHeaders,
			final ReservationRequest request) {
		log.trace("recordReservation ENTRY {}", request.toString());

		ResponseEntity<TimedResponse<ReservationResponse>> response;
		try {
			response = retryTemplate.execute(new RetryCallback<ResponseEntity<TimedResponse<ReservationResponse>>, ResourceAccessException>() {
					public ResponseEntity<TimedResponse<ReservationResponse>> doWithRetry( RetryContext context) throws ResourceAccessException {
						return restTimer.logElapsedTime(() -> {
							return restTemplate.exchange(config.getTransactionUrl(), HttpMethod.POST, 
									new HttpEntity<ReservationRequest>(request, buildHeaders(callingHeaders)), typeReference);
						});
				} });
		} 
		catch (ResourceAccessException ex ) {
			String msg = String.format("HTTP POST to URL %s with %d retries failed.", config.getTransactionUrl(), config.getRestAttempts());
			log.warn("recordTransaction EXIT {}", msg);
			throw ex;
		}
		catch (Exception ex) {
			log.debug("recordTransaction EXIT {}", ex.getLocalizedMessage());
			throw ex;
		}
		
		//TODO compare remote time vs. local time
		log.trace("recordReservation EXIT");
		return response.getBody().getPayload();
	}
	
	private HttpHeaders buildHeaders(final Map<String, String> callingHeaders) {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON) );
		headers.add(TraceableRequest.AIT_ID, config.getAitid());
		headers.add(TraceableRequest.BUSINESS_TAXONOMY_ID, callingHeaders.get(TraceableRequest.BUSINESS_TAXONOMY_ID));
		headers.add(TraceableRequest.CORRELATION_ID, callingHeaders.get(TraceableRequest.CORRELATION_ID));
		return headers;
	}
}