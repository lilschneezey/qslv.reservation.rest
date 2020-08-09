package qslv.reservefunds.rest;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashMap;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import qslv.common.TraceableRequest;
import qslv.data.Account;
import qslv.data.DebitCard;
import qslv.reservefunds.request.ReserveFundsRequest;
import qslv.reservefunds.response.ReserveFundsResponse;
import qslv.transaction.request.ReservationRequest;
import qslv.transaction.resource.TransactionResource;
import qslv.transaction.response.ReservationResponse;


@ExtendWith(MockitoExtension.class)
class Unit_ReservationServiceTest_reserveFunds {
	@Mock
	public TransactionDao dao;
	@Mock 
	JdbcDao debitDao;

	ReserveFundsService service = new ReserveFundsService();

	@BeforeEach
	public void setup() {
		service.setDao(debitDao);
		service.setTrDao(dao);
	}

	@Test
	void testPostReserveAccountFunds_success() {
		
		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(TraceableRequest.AIT_ID, "12345");
		headers.put(TraceableRequest.BUSINESS_TAXONOMY_ID, "7483495");
		headers.put(TraceableRequest.CORRELATION_ID, "273849273498273498");

		ReserveFundsRequest request = new ReserveFundsRequest();
		request.setRequestUUID(UUID.randomUUID());
		request.setAccountNumber("1234HHHH1234");
		request.setTransactionAmount(27384);
		request.setTransactionMetaDataJSON("{}");
		request.setProtectAgainstOverdraft(false);

		Account acct = new Account();
		acct.setAccountNumber("1234HGJT78934");
		acct.setAccountLifeCycleStatus("EF");
		when(debitDao.getAccount(anyString())).thenReturn(acct);

		TransactionResource transaction = new TransactionResource();
		transaction.setTransactionUuid(UUID.randomUUID());
		transaction.setAccountNumber("123781923123");
		transaction.setDebitCardNumber(null);
		transaction.setInsertTimestamp(new Timestamp(Instant.now().toEpochMilli()));
		transaction.setReservationUuid(UUID.randomUUID());
		transaction.setRequestUuid(UUID.randomUUID());
		transaction.setRunningBalanceAmount(99999L);
		transaction.setTransactionAmount(-2323L);
		transaction.setTransactionMetaDataJson("{etc, etc}");
		transaction.setTransactionTypeCode(TransactionResource.RESERVATION);
		ReservationResponse setupResponse = new ReservationResponse(ReservationResponse.SUCCESS, transaction);
		doReturn(setupResponse).when(dao).recordReservation(any(), any());
		
		ReserveFundsResponse response = service.reserveFunds(headers, request);

		verify(dao).recordReservation(any(), any());
		verify(debitDao).getAccount(anyString());

		assert (response.getStatus() == ReserveFundsResponse.SUCCESS);
		TransactionResource rr = response.getTransactions().get(0);
		assert (rr.getTransactionUuid().equals(setupResponse.getResource().getTransactionUuid()));
		assert (rr.getAccountNumber().equals(setupResponse.getResource().getAccountNumber()));
		assert (rr.getDebitCardNumber() == null);
		assert (rr.getInsertTimestamp() == setupResponse.getResource().getInsertTimestamp());
		assert (rr.getReservationUuid().equals(setupResponse.getResource().getReservationUuid()));
		assert (rr.getRequestUuid().equals(setupResponse.getResource().getRequestUuid()));
		assert (rr.getRunningBalanceAmount() == setupResponse.getResource().getRunningBalanceAmount());
		assert (rr.getTransactionAmount() == setupResponse.getResource().getTransactionAmount());
		assert (rr.getTransactionMetaDataJson()
				.equals(setupResponse.getResource().getTransactionMetaDataJson()));
		assert (rr.getTransactionTypeCode() == setupResponse.getResource().getTransactionTypeCode());
	}

	@Test
	void testPostReserveDebitFunds_success() {
		
		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(TraceableRequest.AIT_ID, "12345");
		headers.put(TraceableRequest.BUSINESS_TAXONOMY_ID, "7483495");
		headers.put(TraceableRequest.CORRELATION_ID, "273849273498273498");

		ReserveFundsRequest request = new ReserveFundsRequest();
		request.setRequestUUID(UUID.randomUUID());
		request.setDebitCardNumber("1234HDFG7834JKTR");
		request.setAccountNumber(null);
		request.setTransactionAmount(27384);
		request.setTransactionMetaDataJSON("{}");
		request.setProtectAgainstOverdraft(false);

		DebitCard debitResource = new DebitCard();
		debitResource.setDebitCardLifeCycleStatus("EF");
		debitResource.setAccount(new Account());
		debitResource.getAccount().setAccountNumber("1234HGJT78934");
		debitResource.getAccount().setAccountLifeCycleStatus("EF");
		when(debitDao.getDebitCardAndAccount(anyString())).thenReturn(debitResource);

		TransactionResource transaction = new TransactionResource();
		transaction.setTransactionUuid(UUID.randomUUID());
		transaction.setAccountNumber("123781923123");
		transaction.setDebitCardNumber(null);
		transaction.setInsertTimestamp(new Timestamp(Instant.now().toEpochMilli()));
		transaction.setReservationUuid(UUID.randomUUID());
		transaction.setRequestUuid(UUID.randomUUID());
		transaction.setRunningBalanceAmount(99999L);
		transaction.setTransactionAmount(-2323L);
		transaction.setTransactionMetaDataJson("{etc, etc}");
		transaction.setTransactionTypeCode(TransactionResource.RESERVATION);
		ReservationResponse setupResponse = new ReservationResponse(ReservationResponse.SUCCESS, transaction);
		when(dao.recordReservation(any(), any(ReservationRequest.class))).thenReturn(setupResponse);
		
		ReserveFundsResponse response = service.reserveFunds(headers, request);

		verify(dao).recordReservation(any(), any(ReservationRequest.class));
		verify(debitDao).getDebitCardAndAccount(anyString());

		assert (response.getStatus() == ReserveFundsResponse.SUCCESS);
		TransactionResource rr = response.getTransactions().get(0);
		assert (rr.getTransactionUuid().equals(setupResponse.getResource().getTransactionUuid()));
		assert (rr.getAccountNumber().equals(setupResponse.getResource().getAccountNumber()));
		assert (rr.getDebitCardNumber() == null);
		assert (rr.getInsertTimestamp() == setupResponse.getResource().getInsertTimestamp());
		assert (rr.getReservationUuid().equals(setupResponse.getResource().getReservationUuid()));
		assert (rr.getRequestUuid().equals(setupResponse.getResource().getRequestUuid()));
		assert (rr.getRunningBalanceAmount() == setupResponse.getResource().getRunningBalanceAmount());
		assert (rr.getTransactionAmount() == setupResponse.getResource().getTransactionAmount());
		assert (rr.getTransactionMetaDataJson()
				.equals(setupResponse.getResource().getTransactionMetaDataJson()));
		assert (rr.getTransactionTypeCode() == setupResponse.getResource().getTransactionTypeCode());
	}
	
	@Test
	void testPostReserveAccountFunds_accountBad() {		
		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(TraceableRequest.AIT_ID, "12345");
		headers.put(TraceableRequest.BUSINESS_TAXONOMY_ID, "7483495");
		headers.put(TraceableRequest.CORRELATION_ID, "273849273498273498");

		ReserveFundsRequest request = new ReserveFundsRequest();
		request.setRequestUUID(UUID.randomUUID());
		request.setDebitCardNumber(null);
		request.setAccountNumber("1234HHHH1234");
		request.setTransactionAmount(27384);
		request.setTransactionMetaDataJSON("{}");
		request.setProtectAgainstOverdraft(false);

		Account acct = new Account();
		acct.setAccountNumber("1234HGJT78934");
		acct.setAccountLifeCycleStatus("CL");
		when(debitDao.getAccount(anyString())).thenReturn(acct);

		ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
			service.reserveFunds(headers, request);
		});

		verify(debitDao).getAccount(anyString());
		assert(ex.getStatus() == HttpStatus.UNPROCESSABLE_ENTITY);
	}
	
	@Test
	void testPostReserveDebitFunds_accountBad() {		
		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(TraceableRequest.AIT_ID, "12345");
		headers.put(TraceableRequest.BUSINESS_TAXONOMY_ID, "7483495");
		headers.put(TraceableRequest.CORRELATION_ID, "273849273498273498");

		ReserveFundsRequest request = new ReserveFundsRequest();
		request.setRequestUUID(UUID.randomUUID());
		request.setDebitCardNumber("1234HDFG7834JKTR");
		request.setAccountNumber(null);
		request.setTransactionAmount(27384);
		request.setTransactionMetaDataJSON("{}");
		request.setProtectAgainstOverdraft(false);

		DebitCard debitResource = new DebitCard();
		debitResource.setDebitCardLifeCycleStatus("EF");
		debitResource.setAccount(new Account());
		debitResource.getAccount().setAccountNumber("1234HGJT78934");
		debitResource.getAccount().setAccountLifeCycleStatus("CL");
		when(debitDao.getDebitCardAndAccount(anyString())).thenReturn(debitResource);

		ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
			service.reserveFunds(headers, request);
		});

		verify(debitDao).getDebitCardAndAccount(anyString());
		assert(ex.getStatus() == HttpStatus.UNPROCESSABLE_ENTITY);
	}

	@Test
	void testPostReserveDebitFunds_debitCardBad() {		
		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(TraceableRequest.AIT_ID, "12345");
		headers.put(TraceableRequest.BUSINESS_TAXONOMY_ID, "7483495");
		headers.put(TraceableRequest.CORRELATION_ID, "273849273498273498");

		ReserveFundsRequest request = new ReserveFundsRequest();
		request.setRequestUUID(UUID.randomUUID());
		request.setAccountNumber(null);
		request.setDebitCardNumber("67234HFDK78234HJ");
		request.setTransactionAmount(27384);
		request.setTransactionMetaDataJSON("{}");
		request.setProtectAgainstOverdraft(false);

		DebitCard debitResource = new DebitCard();
		debitResource.setDebitCardLifeCycleStatus("CL");
		debitResource.setAccount(new Account());
		debitResource.getAccount().setAccountNumber("1234HGJT78934");
		debitResource.getAccount().setAccountLifeCycleStatus("EF");
		when(debitDao.getDebitCardAndAccount(anyString())).thenReturn(debitResource);

		ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
			service.reserveFunds(headers, request);
		});

		verify(debitDao).getDebitCardAndAccount(anyString());
		assert(ex.getStatus() == HttpStatus.UNPROCESSABLE_ENTITY);
	}
	
	@Test
	void testPostReserveDebitFunds_insufficientFunds() {
		
		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(TraceableRequest.AIT_ID, "12345");
		headers.put(TraceableRequest.BUSINESS_TAXONOMY_ID, "7483495");
		headers.put(TraceableRequest.CORRELATION_ID, "273849273498273498");

		ReserveFundsRequest request = new ReserveFundsRequest();
		request.setRequestUUID(UUID.randomUUID());
		request.setAccountNumber(null);
		request.setDebitCardNumber("1234HJDF89097HKJT");
		request.setTransactionAmount(27384);
		request.setTransactionMetaDataJSON("{}");
		request.setProtectAgainstOverdraft(false);

		DebitCard debitResource = new DebitCard();
		debitResource.setDebitCardLifeCycleStatus("EF");
		debitResource.setAccount(new Account());
		debitResource.getAccount().setAccountNumber("1234HGJT78934");
		debitResource.getAccount().setAccountLifeCycleStatus("EF");
		when(debitDao.getDebitCardAndAccount(anyString())).thenReturn(debitResource);

		TransactionResource transaction = new TransactionResource();
		transaction.setTransactionUuid(UUID.randomUUID());
		transaction.setAccountNumber("123781923123");
		transaction.setDebitCardNumber("23847923784982734892");
		transaction.setInsertTimestamp(new Timestamp(Instant.now().toEpochMilli()));
		transaction.setReservationUuid(UUID.randomUUID());
		transaction.setRequestUuid(UUID.randomUUID());
		transaction.setRunningBalanceAmount(99999L);
		transaction.setTransactionAmount(-2323L);
		transaction.setTransactionMetaDataJson("{etc, etc}");
		transaction.setTransactionTypeCode(TransactionResource.REJECTED_TRANSACTION);
		ReservationResponse setupResponse = new ReservationResponse(ReservationResponse.INSUFFICIENT_FUNDS, transaction);
		when(dao.recordReservation(any(), any())).thenReturn(setupResponse);
		
		ReserveFundsResponse response = service.reserveFunds(headers, request);

		verify(dao).recordReservation(any(), any());
		verify(debitDao).getDebitCardAndAccount(anyString());

		assert (response.getStatus() == ReserveFundsResponse.INSUFFICIENT_FUNDS);
		TransactionResource rr = response.getTransactions().get(0);
		assert (rr.getTransactionUuid().equals(setupResponse.getResource().getTransactionUuid()));
		assert (rr.getAccountNumber().equals(setupResponse.getResource().getAccountNumber()));
		assert (rr.getDebitCardNumber().equals(setupResponse.getResource().getDebitCardNumber()));
		assert (rr.getInsertTimestamp() == setupResponse.getResource().getInsertTimestamp());
		assert (rr.getReservationUuid().equals(setupResponse.getResource().getReservationUuid()));
		assert (rr.getRequestUuid().equals(setupResponse.getResource().getRequestUuid()));
		assert (rr.getRunningBalanceAmount() == setupResponse.getResource().getRunningBalanceAmount());
		assert (rr.getTransactionAmount() == setupResponse.getResource().getTransactionAmount());
		assert (rr.getTransactionMetaDataJson()
				.equals(setupResponse.getResource().getTransactionMetaDataJson()));
		assert (rr.getTransactionTypeCode() == setupResponse.getResource().getTransactionTypeCode());
	}
	
	@Test
	void testPostReserveDebitFunds_alreadyPresent() {
		
		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(TraceableRequest.AIT_ID, "12345");
		headers.put(TraceableRequest.BUSINESS_TAXONOMY_ID, "7483495");
		headers.put(TraceableRequest.CORRELATION_ID, "273849273498273498");

		ReserveFundsRequest request = new ReserveFundsRequest();
		request.setRequestUUID(UUID.randomUUID());
		request.setDebitCardNumber("273842934782");
		request.setAccountNumber(null);
		request.setTransactionAmount(27384);
		request.setTransactionMetaDataJSON("{}");
		request.setProtectAgainstOverdraft(false);

		DebitCard debitResource = new DebitCard();
		debitResource.setDebitCardLifeCycleStatus("EF");
		debitResource.setAccount(new Account());
		debitResource.getAccount().setAccountNumber("1234HGJT78934");
		debitResource.getAccount().setAccountLifeCycleStatus("EF");
		when(debitDao.getDebitCardAndAccount(anyString())).thenReturn(debitResource);

		TransactionResource transaction = new TransactionResource();
		transaction.setTransactionUuid(UUID.randomUUID());
		transaction.setAccountNumber("123781923123");
		transaction.setDebitCardNumber("2374827349872983472983");
		transaction.setInsertTimestamp(new Timestamp(Instant.now().toEpochMilli()));
		transaction.setReservationUuid(UUID.randomUUID());
		transaction.setRequestUuid(UUID.randomUUID());
		transaction.setRunningBalanceAmount(99999L);
		transaction.setTransactionAmount(-2323L);
		transaction.setTransactionMetaDataJson("{etc, etc}");
		transaction.setTransactionTypeCode(TransactionResource.RESERVATION);
		ReservationResponse setupResponse = new ReservationResponse(ReservationResponse.SUCCESS, transaction);
		
		when(dao.recordReservation(any(), any())).thenReturn(setupResponse);
		
		ReserveFundsResponse response = service.reserveFunds(headers, request);

		verify(dao).recordReservation(any(), any());
		verify(debitDao).getDebitCardAndAccount(anyString());

		assert (response.getStatus() == ReserveFundsResponse.SUCCESS);
		TransactionResource rr = response.getTransactions().get(0);
		assert (rr.getTransactionUuid().equals(setupResponse.getResource().getTransactionUuid()));
		assert (rr.getAccountNumber().equals(setupResponse.getResource().getAccountNumber()));
		assert (rr.getDebitCardNumber().equals(setupResponse.getResource().getDebitCardNumber()));
		assert (rr.getInsertTimestamp() == setupResponse.getResource().getInsertTimestamp());
		assert (rr.getReservationUuid().equals(setupResponse.getResource().getReservationUuid()));
		assert (rr.getRequestUuid().equals(setupResponse.getResource().getRequestUuid()));
		assert (rr.getRunningBalanceAmount() == setupResponse.getResource().getRunningBalanceAmount());
		assert (rr.getTransactionAmount() == setupResponse.getResource().getTransactionAmount());
		assert (rr.getTransactionMetaDataJson()
				.compareTo(setupResponse.getResource().getTransactionMetaDataJson()) == 0);
		assert (rr.getTransactionTypeCode() == setupResponse.getResource().getTransactionTypeCode());
	}
	
	@Test
	void testPostReserveDebitFunds_throws() {
		
		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(TraceableRequest.AIT_ID, "12345");
		headers.put(TraceableRequest.BUSINESS_TAXONOMY_ID, "7483495");
		headers.put(TraceableRequest.CORRELATION_ID, "273849273498273498");

		ReserveFundsRequest request = new ReserveFundsRequest();
		request.setRequestUUID(UUID.randomUUID());
		request.setAccountNumber(null);
		request.setDebitCardNumber("1234HGJKT7845HDRT");
		request.setTransactionAmount(27384);
		request.setTransactionMetaDataJSON("{}");
		request.setProtectAgainstOverdraft(false);

		DebitCard debitResource = new DebitCard();
		debitResource.setDebitCardLifeCycleStatus("EF");
		debitResource.setAccount(new Account());
		debitResource.getAccount().setAccountNumber("1234HGJT78934");
		debitResource.getAccount().setAccountLifeCycleStatus("EF");
		when(debitDao.getDebitCardAndAccount(anyString())).thenReturn(debitResource);

		when(dao.recordReservation(any(), any()))
			.thenThrow(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "myError"));
		
		ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
			service.reserveFunds(headers, request);
		});

		verify(debitDao).getDebitCardAndAccount(anyString());

		assert (ex.getStatus() == HttpStatus.INTERNAL_SERVER_ERROR );
	}

}
