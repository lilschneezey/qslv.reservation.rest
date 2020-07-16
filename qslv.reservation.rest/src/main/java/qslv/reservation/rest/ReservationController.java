package qslv.reservation.rest;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import qslv.common.TimedResponse;
import qslv.common.TraceableRequest;
import qslv.reservation.request.ReserveFundsRequest;
import qslv.reservation.response.ReserveFundsResponse;
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
public class ReservationController {
	private static final Logger log = LoggerFactory.getLogger(ReservationController.class);

	@Autowired
	public ConfigProperties config;
	@Autowired
	private ReservationService service;

	public void setConfig(ConfigProperties config) {
		this.config = config;
	}
	public void setService(ReservationService service) {
		this.service = service;
	}
	
	@PostMapping("/ReserveFunds")
	@ResponseStatus(HttpStatus.OK)
	@LogRequestTracingData(value="POST/ReserveFunds", ait = "33333")
	@ServiceElapsedTimeSLI(value="POST/ReserveFunds", injectResponse = true, ait = "44444")
	public TimedResponse<ReserveFundsResponse> postReserveFunds(final @RequestHeader Map<String, String> headers,
			final @RequestBody ReserveFundsRequest request) {
		validateHeaders(headers);
		validateReserveFundsRequest(request);
		ReserveFundsResponse answer = service.reserveFunds(headers, request, false);

		TimedResponse<ReserveFundsResponse> response = new TimedResponse<ReserveFundsResponse>();
		response.setPayload(answer);
		return response;
	}
	
	@PostMapping("/ReserveFundsWithOverdraft")
	@ResponseStatus(HttpStatus.OK)
	@LogRequestTracingData(value="POST/ReserveFundsWithOverdraft", ait = "33333")
	@ServiceElapsedTimeSLI(value="POST/ReserveFundsWithOverdraft", injectResponse = true, ait = "44444")
	public TimedResponse<ReserveFundsResponse> postReserveFundsWithOverdraft(final @RequestHeader Map<String, String> headers,
			final @RequestBody ReserveFundsRequest request) {
		validateHeaders(headers);
		validateReserveFundsRequest(request);
		ReserveFundsResponse answer = service.reserveFunds(headers, request, true);

		TimedResponse<ReserveFundsResponse> response = new TimedResponse<ReserveFundsResponse>();
		response.setPayload(answer);
		return response;
	}

	private void validateReserveFundsRequest(ReserveFundsRequest request) {
		log.debug("validateReserveFundsRequest ENTRY");
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
		log.debug("validateHeaders ENTRY");

		if (headerMap.get(TraceableRequest.AIT_ID) == null) {
			log.error("controller.validateHeaders, Malformed Request. Missing header variable ait-id");
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing header variable ait-id");
		}
		if (headerMap.get(TraceableRequest.BUSINESS_TAXONOMY_ID) == null) {
			log.error("controller.validateHeaders, Malformed Request. Missing header variable business-taxonomy-id");
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing header variable business-taxonomy-id");
		}
		if (headerMap.get(TraceableRequest.CORRELATION_ID) == null) {
			log.error("controller.validateHeaders, Malformed Request. Missing header variable correlation-id");
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing header variable correlation-id");
		}
		if (headerMap.get(TraceableRequest.ACCEPT_VERSION) == null) {
			log.error("controller.validateHeaders, Malformed Request. Missing header variable accept-version");
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing header variable accept-version");
		}	}

}
