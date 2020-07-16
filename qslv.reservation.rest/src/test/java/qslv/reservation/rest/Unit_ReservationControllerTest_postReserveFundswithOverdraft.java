package qslv.reservation.rest;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import qslv.common.TimedResponse;
import qslv.common.TraceableRequest;
import qslv.reservation.request.ReserveFundsRequest;
import qslv.reservation.response.ReserveFundsResponse;
import qslv.transaction.resource.TransactionResource;


@ExtendWith(MockitoExtension.class)
class Unit_ReservationControllerTest_postReserveFundswithOverdraft {
	@Mock
	public ReservationService service;
	@Mock
	public ConfigProperties config;

	ReservationController controller = new ReservationController();

	@BeforeEach
	public void setup() {
		controller.setService(service);
		controller.setConfig(config);
	}

	@Test
	void testPostReserveFundsWithOverdraft_success() {
		
		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(TraceableRequest.AIT_ID, "12345");
		headers.put(TraceableRequest.BUSINESS_TAXONOMY_ID, "7483495");
		headers.put(TraceableRequest.CORRELATION_ID, "273849273498273498");
		headers.put(TraceableRequest.ACCEPT_VERSION, ReserveFundsRequest.version1_0);

		ReserveFundsRequest request = new ReserveFundsRequest();
		request.setRequestUUID(UUID.randomUUID());
		request.setAccountNumber("1234HHHH1234");
		request.setTransactionAmount(27384);
		request.setTransactionMetaDataJSON("{}");

		ReserveFundsResponse setupResponse = new ReserveFundsResponse();

		setupResponse.setStatus(ReserveFundsResponse.SUCCESS);
		TransactionResource tr = new TransactionResource();
		setupResponse.setTransactions(Collections.singletonList(tr));
		tr.setTransactionUuid(UUID.randomUUID());
		tr.setAccountNumber("123781923123");
		tr.setDebitCardNumber("126743812673981623");
		tr.setInsertTimestamp(new Timestamp(Instant.now().toEpochMilli()));
		tr.setReservationUuid(UUID.randomUUID());
		tr.setRequestUuid(UUID.randomUUID());
		tr.setRunningBalanceAmount(99999L);
		tr.setTransactionAmount(-2323L);
		tr.setTransactionMetaDataJson("{etc, etc}");
		tr.setTransactionTypeCode(TransactionResource.RESERVATION);

		when(service.reserveFunds(any(), any(), anyBoolean())).thenReturn(setupResponse);
		
		TimedResponse<ReserveFundsResponse> response = controller.postReserveFundsWithOverdraft(headers, request);

		verify(service).reserveFunds(any(), any(), anyBoolean());
		assert (response.getPayload().getStatus() == ReserveFundsResponse.SUCCESS);

		TransactionResource rr = response.getPayload().getTransactions().get(0);
		assertTrue (rr.getTransactionUuid().equals(tr.getTransactionUuid()));
		assertTrue (rr.getAccountNumber().equals(tr.getAccountNumber()));
		assertTrue (rr.getDebitCardNumber().equals(tr.getDebitCardNumber()));
		assertTrue (rr.getInsertTimestamp() == tr.getInsertTimestamp());
		assertTrue (rr.getReservationUuid().equals(tr.getReservationUuid()));
		assertTrue (rr.getRequestUuid().equals(tr.getRequestUuid()));
		assertTrue (rr.getRunningBalanceAmount() == tr.getRunningBalanceAmount());
		assertTrue (rr.getTransactionAmount() == tr.getTransactionAmount());
		assertTrue (rr.getTransactionMetaDataJson().equals(tr.getTransactionMetaDataJson()));
		assertTrue (rr.getTransactionTypeCode() == tr.getTransactionTypeCode());

	}

	@Test
	void testPostReserveFundsWithOverdraft_failure() {
		
		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(TraceableRequest.AIT_ID, "12345");
		headers.put(TraceableRequest.BUSINESS_TAXONOMY_ID, "7483495");
		headers.put(TraceableRequest.CORRELATION_ID, "273849273498273498");
		headers.put(TraceableRequest.ACCEPT_VERSION, ReserveFundsRequest.version1_0);

		ReserveFundsRequest request = new ReserveFundsRequest();
		request.setRequestUUID(UUID.randomUUID());
		request.setAccountNumber("1234HHHH1234");
		request.setTransactionAmount(27384);
		request.setTransactionMetaDataJSON("{}");
		
		when(service.reserveFunds(any(), any(), anyBoolean()))
			.thenThrow(new ResponseStatusException(HttpStatus.NOT_ACCEPTABLE, "garbage"));

		ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
			controller.postReserveFundsWithOverdraft(headers, request);
		});
		assertTrue (ex.getStatus() == HttpStatus.NOT_ACCEPTABLE);

	}

	@Test
	void testPostReserveFundsWithOverdraft_validateInput() {

		HashMap<String, String> headers = new HashMap<String, String>();
		ReserveFundsRequest request = new ReserveFundsRequest();

		// --- No headers, no data
		ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
			controller.postReserveFundsWithOverdraft(headers, request);
		});
		assertTrue(ex.getStatus() == HttpStatus.BAD_REQUEST);
		assertTrue(ex.getLocalizedMessage().contains("ait-id"));

		// --- add AIT_ID
		headers.put(TraceableRequest.AIT_ID, "12345");
		ex = assertThrows(ResponseStatusException.class, () -> {
			controller.postReserveFundsWithOverdraft(headers, request);
		});
		assertTrue (ex.getStatus() == HttpStatus.BAD_REQUEST);
		assertTrue(ex.getLocalizedMessage().contains("business-taxonomy-id"));

		// --- add BUSINESS_TAXONOMY_ID
		headers.put(TraceableRequest.BUSINESS_TAXONOMY_ID, "7483495");
		ex = assertThrows(ResponseStatusException.class, () -> {
			controller.postReserveFundsWithOverdraft(headers, request);
		});
		assertTrue (ex.getStatus() == HttpStatus.BAD_REQUEST);
		assertTrue(ex.getLocalizedMessage().contains("correlation-id"));
		
		// --- add CORRELATION_ID
		headers.put(TraceableRequest.CORRELATION_ID, "273849273498273498");
		ex = assertThrows(ResponseStatusException.class, () -> {
			controller.postReserveFundsWithOverdraft(headers, request);
		});
		assertTrue (ex.getStatus() == HttpStatus.BAD_REQUEST);
		assertTrue(ex.getLocalizedMessage().contains("accept-version"));

		// --- add VERSION
		headers.put(TraceableRequest.ACCEPT_VERSION, "273849273498273498");
		ex = assertThrows(ResponseStatusException.class, () -> {
			controller.postReserveFundsWithOverdraft(headers, request);
		});
		assertTrue (ex.getStatus() == HttpStatus.BAD_REQUEST);
		assertTrue(ex.getLocalizedMessage().contains("request_uuid"));

		// --- add Request UUID Number
		request.setRequestUUID(UUID.randomUUID());
		ex = assertThrows(ResponseStatusException.class, () -> {
			controller.postReserveFundsWithOverdraft(headers, request);
		});
		assertTrue (ex.getStatus() == HttpStatus.BAD_REQUEST);
		assertTrue (ex.getLocalizedMessage().contains("Specify at least one"));

		// --- add Account Card Number
		request.setAccountNumber("178239123");
		ex = assertThrows(ResponseStatusException.class, () -> {
			controller.postReserveFundsWithOverdraft(headers, request);
		});
		assertTrue (ex.getStatus() == HttpStatus.BAD_REQUEST);
		assertTrue (ex.getLocalizedMessage().contains("transactionMetaData_json"));

		// --- add Debit Card Number
		request.setDebitCardNumber("178239123");
		ex = assertThrows(ResponseStatusException.class, () -> {
			controller.postReserveFundsWithOverdraft(headers, request);
		});
		assertTrue (ex.getStatus() == HttpStatus.BAD_REQUEST);
		assertTrue (ex.getLocalizedMessage().contains("Specify only one"));

		// --- remove Debit Card Number
		// --- add JSON
		request.setDebitCardNumber(null);
		request.setTransactionMetaDataJSON("178239123");
		ex = assertThrows(ResponseStatusException.class, () -> {
			controller.postReserveFundsWithOverdraft(headers, request);
		});
		assertTrue (ex.getStatus() == HttpStatus.BAD_REQUEST);
		assertTrue (ex.getLocalizedMessage().contains("Transaction Amount"));

		// --- add Amount
		request.setTransactionAmount(7890);
	
		// --- all clear
		ReserveFundsResponse setupResponse = new ReserveFundsResponse();
		setupResponse.setStatus(ReserveFundsResponse.SUCCESS);
		when(service.reserveFunds(any(), any(), anyBoolean())).thenReturn(setupResponse);

		TimedResponse<ReserveFundsResponse> response = controller.postReserveFundsWithOverdraft(headers, request);
		assertNotNull( response.getPayload() );
	}

}
