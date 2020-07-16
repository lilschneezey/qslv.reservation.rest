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
class Unit_ReservationControllerTest_postReserveFunds {
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
	void testPostReserveFunds_success() {
		
		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(TraceableRequest.AIT_ID, "12345");
		headers.put(TraceableRequest.BUSINESS_TAXONOMY_ID, "7483495");
		headers.put(TraceableRequest.CORRELATION_ID, "273849273498273498");
		headers.put(TraceableRequest.ACCEPT_VERSION, "v1");

		ReserveFundsRequest request = new ReserveFundsRequest();
		request.setRequestUUID(UUID.randomUUID());
		request.setAccountNumber("1234HHHH1234");
		request.setTransactionAmount(27384);
		request.setTransactionMetaDataJSON("{}");

		ReserveFundsResponse setupResponse = new ReserveFundsResponse();

		setupResponse.setStatus(ReserveFundsResponse.SUCCESS);
		TransactionResource tr = new TransactionResource();
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
		setupResponse.setTransactions(Collections.singletonList(tr));

		when(service.reserveFunds(any(), any(), anyBoolean())).thenReturn(setupResponse);
		
		TimedResponse<ReserveFundsResponse> response = controller.postReserveFunds(headers, request);

		verify(service).reserveFunds(any(), any(), anyBoolean());
		assert (response.getPayload().getStatus() == ReserveFundsResponse.SUCCESS);

		TransactionResource rr = response.getPayload().getTransactions().get(0);
		assert (rr.getTransactionUuid().equals(tr.getTransactionUuid()));
		assert (rr.getAccountNumber().equals(tr.getAccountNumber()));
		assert (rr.getDebitCardNumber().equals(tr.getDebitCardNumber()));
		assert (rr.getInsertTimestamp() == tr.getInsertTimestamp());
		assert (rr.getReservationUuid().equals(tr.getReservationUuid()));
		assert (rr.getRequestUuid().equals(tr.getRequestUuid()));
		assert (rr.getRunningBalanceAmount() == tr.getRunningBalanceAmount());
		assert (rr.getTransactionAmount() == tr.getTransactionAmount());
		assert (rr.getTransactionMetaDataJson() == tr.getTransactionMetaDataJson());
		assert (rr.getTransactionTypeCode() == tr.getTransactionTypeCode());

	}

	@Test
	void testPostReserveFunds_failure() {
		
		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(TraceableRequest.AIT_ID, "12345");
		headers.put(TraceableRequest.BUSINESS_TAXONOMY_ID, "7483495");
		headers.put(TraceableRequest.CORRELATION_ID, "273849273498273498");
		headers.put(TraceableRequest.ACCEPT_VERSION, "v1");

		ReserveFundsRequest request = new ReserveFundsRequest();
		request.setRequestUUID(UUID.randomUUID());
		request.setAccountNumber("1234HHHH1234");
		request.setTransactionAmount(27384);
		request.setTransactionMetaDataJSON("{}");
		
		when(service.reserveFunds(any(), any(), anyBoolean()))
			.thenThrow(new ResponseStatusException(HttpStatus.NOT_ACCEPTABLE, "garbage"));

		ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
			controller.postReserveFunds(headers, request);
		});
		assert (ex.getStatus() == HttpStatus.NOT_ACCEPTABLE);

	}

	@Test
	void testPostReserveFunds_validateInput() {

		HashMap<String, String> headers = new HashMap<String, String>();
		ReserveFundsRequest request = new ReserveFundsRequest();

		// --- No headers, no data
		ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
			controller.postReserveFunds(headers, request);
		});
		assert (ex.getStatus() == HttpStatus.BAD_REQUEST);

		// --- add AIT_ID
		headers.put(TraceableRequest.AIT_ID, "12345");
		ex = assertThrows(ResponseStatusException.class, () -> {
			controller.postReserveFunds(headers, request);
		});
		assert (ex.getStatus() == HttpStatus.BAD_REQUEST);

		// --- add BUSINESS_TAXONOMY_ID
		headers.put(TraceableRequest.BUSINESS_TAXONOMY_ID, "7483495");
		ex = assertThrows(ResponseStatusException.class, () -> {
			controller.postReserveFunds(headers, request);
		});
		assert (ex.getStatus() == HttpStatus.BAD_REQUEST);

		// --- add CORRELATION_ID
		headers.put(TraceableRequest.CORRELATION_ID, "273849273498273498");
		ex = assertThrows(ResponseStatusException.class, () -> {
			controller.postReserveFunds(headers, request);
		});
		assert (ex.getStatus() == HttpStatus.BAD_REQUEST);

		// --- add Version
		headers.put(TraceableRequest.ACCEPT_VERSION, "v1");
		ex = assertThrows(ResponseStatusException.class, () -> {
			controller.postReserveFunds(headers, request);
		});
		assert (ex.getStatus() == HttpStatus.BAD_REQUEST);
		assert (ex.getLocalizedMessage().contains("request_uuid"));

		// --- add Request UUID Number
		request.setRequestUUID(UUID.randomUUID());
		ex = assertThrows(ResponseStatusException.class, () -> {
			controller.postReserveFunds(headers, request);
		});
		assert (ex.getStatus() == HttpStatus.BAD_REQUEST);
		assert (ex.getLocalizedMessage().contains("Specify at least one"));

		// --- add Account Card Number
		request.setAccountNumber("178239123");
		ex = assertThrows(ResponseStatusException.class, () -> {
			controller.postReserveFunds(headers, request);
		});
		assert (ex.getStatus() == HttpStatus.BAD_REQUEST);
		assert (ex.getLocalizedMessage().contains("transactionMetaData_json"));

		// --- add Debit Card Number
		request.setDebitCardNumber("178239123");
		ex = assertThrows(ResponseStatusException.class, () -> {
			controller.postReserveFunds(headers, request);
		});
		assert (ex.getStatus() == HttpStatus.BAD_REQUEST);
		assert (ex.getLocalizedMessage().contains("Specify only one"));

		// --- remove Debit Card Number
		// --- add JSON
		request.setDebitCardNumber(null);
		request.setTransactionMetaDataJSON("178239123");
		ex = assertThrows(ResponseStatusException.class, () -> {
			controller.postReserveFunds(headers, request);
		});
		assert (ex.getStatus() == HttpStatus.BAD_REQUEST);
		assert (ex.getLocalizedMessage().contains("Transaction Amount"));

		// --- add Amount
		request.setTransactionAmount(7890);
	
		// --- all clear
		ReserveFundsResponse setupResponse = new ReserveFundsResponse();
		setupResponse.setStatus(ReserveFundsResponse.SUCCESS);
		when(service.reserveFunds(any(), any(), anyBoolean())).thenReturn(setupResponse);

		TimedResponse<ReserveFundsResponse> response = controller.postReserveFunds(headers, request);
		assertNotNull( response.getPayload() );
	}

}
