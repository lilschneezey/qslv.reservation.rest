package qslv.reservefunds.rest;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import qslv.common.TimedResponse;
import qslv.common.TraceableRequest;
import qslv.reservefunds.request.ReserveFundsRequest;
import qslv.reservefunds.response.ReserveFundsResponse;
import qslv.util.LogRequestTracingData;
import qslv.util.ServiceElapsedTimeSLI;

/**
 * Debit Card Reservation REST Service. Performs basic validation
 * Validation - Debit Card is present and in good condition
 * Action - gets Account mapped to Debit Card
 * Validation - mapped Account is present and in good condition
 * Action - reserve money in Account
 * If success - return
 * If fails
 * Action - gets OverDraft instructions
 * For Each Overdraft - Action - reserve money in Account
 * When one succeeds return
 * If all fail return failure
 * 
 */

@RestController
public class ReserveFundsController {
	private static final Logger log = LoggerFactory.getLogger(ReserveFundsController.class);

	@Autowired
	public ConfigProperties config;
	@Autowired
	private ReserveFundsService service;

	public void setConfig(ConfigProperties config) {
		this.config = config;
	}
	public void setService(ReserveFundsService service) {
		this.service = service;
	}
	
	@PostMapping("/ReserveFunds")
	@ResponseStatus(HttpStatus.OK)
	@LogRequestTracingData(value="POST/ReserveFunds", ait = "#{@configProperties.aitid}")
	@ServiceElapsedTimeSLI(value="POST/ReserveFunds", injectResponse = true, ait = "#{@configProperties.aitid}")
	public TimedResponse<ReserveFundsResponse> postReserveFunds(final @RequestHeader Map<String, String> headers,
			final @RequestBody ReserveFundsRequest request) {
		validateHeaders(headers);
		validateReserveFundsRequest(request);
		if ( false == headers.get(TraceableRequest.ACCEPT_VERSION).equals(ReserveFundsRequest.version1_0) ) {
			log.error("controller.postReserveFunds, Malformed Request. Invalid version {}", headers.get(TraceableRequest.ACCEPT_VERSION));
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid version "+ headers.get(TraceableRequest.ACCEPT_VERSION));			
		}
		ReserveFundsResponse answer = service.reserveFunds(headers, request);

		TimedResponse<ReserveFundsResponse> response = new TimedResponse<ReserveFundsResponse>();
		response.setPayload(answer);
		return response;
	}

	private void validateReserveFundsRequest(ReserveFundsRequest request) {
		log.trace("validateReserveFundsRequest ENTRY");
		if (request.getRequestUUID() == null) {
			log.error("controller.validateTransactionRequest, Malformed Request. Missing request_uuid");
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing request_uuid");
		}

		boolean accountPresent = request.getAccountNumber() != null && request.getAccountNumber().length() > 0;
		boolean debitCardPresent = request.getDebitCardNumber() != null && request.getDebitCardNumber().length() > 0;
		
		if  (accountPresent && debitCardPresent ){
			log.error("controller.validateTransactionRequest Malformed Request. Specify only one: account_id or debit_card");
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Specify only one: account_id or debit_card");
		}
		if  (accountPresent== false && debitCardPresent==false ){
			log.error("controller.validateTransactionRequest Malformed Request. Specify at least one: account_id or debit_card");
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Specify at least one: account_id or debit_card");
		}

		if (request.getTransactionMetaDataJSON() == null || request.getTransactionMetaDataJSON().length() <= 1) {
			log.error("controller.validateTransactionRequest Malformed Request. Missing transactionMetaData_json");
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing transactionMetaData_json");
		}
		
		if (request.getTransactionAmount() == 0) {
			log.error("controller.validateTransactionRequest Malformed Request. Transaction Amount must not be zero(0).");
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Transaction Amount must not be zero(0).");
		}
	}

	private void validateHeaders(Map<String, String> headerMap) {
		log.trace("validateHeaders ENTRY");

		if (headerMap.get(TraceableRequest.AIT_ID) == null) {
			log.error("controller.validateHeaders, Malformed Request. Missing header variable {}", TraceableRequest.AIT_ID);
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing header variable "+TraceableRequest.AIT_ID);
		}
		if (headerMap.get(TraceableRequest.BUSINESS_TAXONOMY_ID) == null) {
			log.error("controller.validateHeaders, Malformed Request. Missing header variable {}", TraceableRequest.BUSINESS_TAXONOMY_ID);
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing header variable "+TraceableRequest.BUSINESS_TAXONOMY_ID);
		}
		if (headerMap.get(TraceableRequest.CORRELATION_ID) == null) {
			log.error("controller.validateHeaders, Malformed Request. Missing header variable {}", TraceableRequest.CORRELATION_ID);
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing header variable " + TraceableRequest.CORRELATION_ID);
		}
		if (headerMap.get(TraceableRequest.ACCEPT_VERSION) == null) {
			log.error("controller.validateHeaders, Malformed Request. Missing header variable {}", TraceableRequest.ACCEPT_VERSION);
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing header variable "+ TraceableRequest.ACCEPT_VERSION);
		}	}

}
