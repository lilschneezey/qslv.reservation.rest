package qslv.reservefunds.rest;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import qslv.common.TraceableRequest;
import qslv.data.Account;
import qslv.data.OverdraftInstruction;
import qslv.reservefunds.request.ReserveFundsRequest;
import qslv.reservefunds.response.ReserveFundsResponse;
import qslv.transaction.request.ReservationRequest;
import qslv.transaction.resource.TransactionResource;
import qslv.transaction.response.ReservationResponse;


@ExtendWith(MockitoExtension.class)
class Unit_ReservationServiceTest_overdraft {
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
	void testPostReserveAccountFunds_OD_success() {
		
		// -----------------
		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(TraceableRequest.AIT_ID, "12345");
		headers.put(TraceableRequest.BUSINESS_TAXONOMY_ID, "7483495");
		headers.put(TraceableRequest.CORRELATION_ID, "273849273498273498");

		// ------------------
		ReserveFundsRequest request = new ReserveFundsRequest();
		request.setRequestUUID(UUID.randomUUID());
		request.setAccountNumber("1234HHHH1234");
		request.setTransactionAmount(27384);
		request.setTransactionMetaDataJSON("{}");
		request.setProtectAgainstOverdraft(true);

		// -------------------
		Account acct = new Account();
		acct.setAccountNumber("1234HGJT78934");
		acct.setAccountLifeCycleStatus("EF");
		when(debitDao.getAccount(anyString())).thenReturn(acct);
		
		// ------------------
		LinkedList<OverdraftInstruction> odlist = new LinkedList<OverdraftInstruction>();
		OverdraftInstruction od = new OverdraftInstruction();
		od.setOverdraftAccount(new Account());
		od.getOverdraftAccount().setAccountLifeCycleStatus("EF");
		od.getOverdraftAccount().setAccountNumber("1234hjkf8943");
		od.setInstructionLifecycleStatus("EF");
		od.setEffectiveStart(LocalDateTime.now().minusYears(1));
		od.setEffectiveEnd(LocalDateTime.now().plusYears(1));
		odlist.add(od);
		
		od = new OverdraftInstruction();
		od.setOverdraftAccount(new Account());
		od.getOverdraftAccount().setAccountLifeCycleStatus("EF");
		od.getOverdraftAccount().setAccountNumber("782342634234");
		od.setInstructionLifecycleStatus("EF");
		od.setEffectiveStart(LocalDateTime.now().minusYears(1));
		od.setEffectiveEnd(LocalDateTime.now().plusYears(1));
		odlist.add(od);
		
		when(debitDao.getOverdraftInstructions(anyString())).thenReturn(odlist);

		// -------------------
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
		transaction.setTransactionTypeCode(TransactionResource.REJECTED_TRANSACTION);
		ReservationResponse a1response = new ReservationResponse(ReservationResponse.INSUFFICIENT_FUNDS, transaction);
		
		transaction = new TransactionResource();
		transaction.setTransactionUuid(UUID.randomUUID());
		transaction.setAccountNumber("378922983742938");
		transaction.setDebitCardNumber(null);
		transaction.setInsertTimestamp(new Timestamp(Instant.now().toEpochMilli()));
		transaction.setReservationUuid(UUID.randomUUID());
		transaction.setRequestUuid(UUID.randomUUID());
		transaction.setRunningBalanceAmount(99999L);
		transaction.setTransactionAmount(-2323L);
		transaction.setTransactionMetaDataJson("{etc, etc}");
		transaction.setTransactionTypeCode(TransactionResource.RESERVATION);
		ReservationResponse a2response = new ReservationResponse(ReservationResponse.SUCCESS, transaction);
		
		when(dao.recordReservation(any(), any(ReservationRequest.class)))
			.thenReturn(a1response)
			.thenReturn(a2response);
	
		// -------------
		ReserveFundsResponse response = service.reserveFunds(headers, request);

		// -------------
		verify(dao, times(2) ).recordReservation(any(), any(ReservationRequest.class));
		verify(debitDao).getOverdraftInstructions(anyString());
		verify(debitDao).getAccount(anyString());

		// -------------
		assertTrue (response.getStatus() == ReserveFundsResponse.SUCCESS_OVERDRAFT);
		
		TransactionResource rr = response.getTransactions().get(0);
		assertTrue (rr.getTransactionUuid().equals(a1response.getResource().getTransactionUuid()));
		assertTrue (rr.getAccountNumber().equals(a1response.getResource().getAccountNumber()));
		assertTrue (rr.getDebitCardNumber() == null);
		assertTrue (rr.getInsertTimestamp() == a1response.getResource().getInsertTimestamp());
		assertTrue (rr.getReservationUuid().equals(a1response.getResource().getReservationUuid()));
		assertTrue (rr.getRequestUuid().equals(a1response.getResource().getRequestUuid()));
		assertTrue (rr.getRunningBalanceAmount() == a1response.getResource().getRunningBalanceAmount());
		assertTrue (rr.getTransactionAmount() == a1response.getResource().getTransactionAmount());
		assertTrue (rr.getTransactionMetaDataJson() == a1response.getResource().getTransactionMetaDataJson());
		assertTrue (rr.getTransactionTypeCode() == a1response.getResource().getTransactionTypeCode());
		
		rr = response.getTransactions().get(1);
		assertTrue (rr.getTransactionUuid().equals(a2response.getResource().getTransactionUuid()));
		assertTrue (rr.getAccountNumber().equals(a2response.getResource().getAccountNumber()));
		assertTrue (rr.getDebitCardNumber() == null);
		assertTrue (rr.getInsertTimestamp() == a2response.getResource().getInsertTimestamp());
		assertTrue (rr.getReservationUuid().equals(a2response.getResource().getReservationUuid()));
		assertTrue (rr.getRequestUuid().equals(a2response.getResource().getRequestUuid()));
		assertTrue (rr.getRunningBalanceAmount() == a2response.getResource().getRunningBalanceAmount());
		assertTrue (rr.getTransactionAmount() == a2response.getResource().getTransactionAmount());
		assertTrue (rr.getTransactionMetaDataJson() == a2response.getResource().getTransactionMetaDataJson());
		assertTrue (rr.getTransactionTypeCode() == a2response.getResource().getTransactionTypeCode());
	}

	@Test
	void testPostReserveAccountFunds_ODI_expire() {
		
		// -----------------
		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(TraceableRequest.AIT_ID, "12345");
		headers.put(TraceableRequest.BUSINESS_TAXONOMY_ID, "7483495");
		headers.put(TraceableRequest.CORRELATION_ID, "273849273498273498");

		// ------------------
		ReserveFundsRequest request = new ReserveFundsRequest();
		request.setRequestUUID(UUID.randomUUID());
		request.setAccountNumber("1234HHHH1234");
		request.setTransactionAmount(27384);
		request.setTransactionMetaDataJSON("{}");
		request.setProtectAgainstOverdraft(true);

		// -------------------
		Account acct = new Account();
		acct.setAccountNumber("1234HGJT78934");
		acct.setAccountLifeCycleStatus("EF");
		when(debitDao.getAccount(anyString())).thenReturn(acct);
		
		// ------------------
		LinkedList<OverdraftInstruction> odlist = new LinkedList<OverdraftInstruction>();
		OverdraftInstruction od = new OverdraftInstruction();
		od.setOverdraftAccount(new Account());
		od.getOverdraftAccount().setAccountLifeCycleStatus("EF");
		od.getOverdraftAccount().setAccountNumber("1234hjkf8943");
		od.setInstructionLifecycleStatus("EF");
		od.setEffectiveStart(LocalDateTime.now().minusYears(1));
		od.setEffectiveEnd(LocalDateTime.now());
		odlist.add(od);
		
		when(debitDao.getOverdraftInstructions(anyString())).thenReturn(odlist);

		// -------------------
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
		transaction.setTransactionTypeCode(TransactionResource.REJECTED_TRANSACTION);
		ReservationResponse a1response = new ReservationResponse(ReservationResponse.INSUFFICIENT_FUNDS, transaction);
		
		when(dao.recordReservation(any(), any(ReservationRequest.class)))
			.thenReturn(a1response);
	
		// -------------
		ReserveFundsResponse response = service.reserveFunds(headers, request);

		// -------------
		verify(dao).recordReservation(any(), any(ReservationRequest.class));
		verify(debitDao).getOverdraftInstructions(anyString());
		verify(debitDao).getAccount(anyString());

		// -------------
		assertTrue (response.getStatus() == ReserveFundsResponse.INSUFFICIENT_FUNDS);
		
		TransactionResource rr = response.getTransactions().get(0);
		assertTrue (rr.getTransactionUuid().equals(a1response.getResource().getTransactionUuid()));
		assertTrue (rr.getAccountNumber().equals(a1response.getResource().getAccountNumber()));
		assertTrue (rr.getDebitCardNumber() == null);
		assertTrue (rr.getInsertTimestamp() == a1response.getResource().getInsertTimestamp());
		assertTrue (rr.getReservationUuid().equals(a1response.getResource().getReservationUuid()));
		assertTrue (rr.getRequestUuid().equals(a1response.getResource().getRequestUuid()));
		assertTrue (rr.getRunningBalanceAmount() == a1response.getResource().getRunningBalanceAmount());
		assertTrue (rr.getTransactionAmount() == a1response.getResource().getTransactionAmount());
		assertTrue (rr.getTransactionMetaDataJson() == a1response.getResource().getTransactionMetaDataJson());
		assertTrue (rr.getTransactionTypeCode() == a1response.getResource().getTransactionTypeCode());
	}
	
	@Test
	void testPostReserveAccountFunds_ODI_notEffective() {
		
		// -----------------
		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(TraceableRequest.AIT_ID, "12345");
		headers.put(TraceableRequest.BUSINESS_TAXONOMY_ID, "7483495");
		headers.put(TraceableRequest.CORRELATION_ID, "273849273498273498");

		// ------------------
		ReserveFundsRequest request = new ReserveFundsRequest();
		request.setRequestUUID(UUID.randomUUID());
		request.setAccountNumber("1234HHHH1234");
		request.setTransactionAmount(27384);
		request.setTransactionMetaDataJSON("{}");
		request.setProtectAgainstOverdraft(true);

		// -------------------
		Account acct = new Account();
		acct.setAccountNumber("1234HGJT78934");
		acct.setAccountLifeCycleStatus("EF");
		when(debitDao.getAccount(anyString())).thenReturn(acct);
		
		// ------------------
		LinkedList<OverdraftInstruction> odlist = new LinkedList<OverdraftInstruction>();
		OverdraftInstruction od = new OverdraftInstruction();
		od.setOverdraftAccount(new Account());
		od.getOverdraftAccount().setAccountLifeCycleStatus("EF");
		od.getOverdraftAccount().setAccountNumber("1234hjkf8943");
		od.setInstructionLifecycleStatus("CL");
		od.setEffectiveStart(LocalDateTime.now().minusYears(1));
		od.setEffectiveEnd(LocalDateTime.now().plusYears(1));
		odlist.add(od);
		
		when(debitDao.getOverdraftInstructions(anyString())).thenReturn(odlist);

		// -------------------
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
		transaction.setTransactionTypeCode(TransactionResource.REJECTED_TRANSACTION);
		ReservationResponse a1response = new ReservationResponse(ReservationResponse.INSUFFICIENT_FUNDS, transaction);
		
		when(dao.recordReservation(any(), any(ReservationRequest.class)))
			.thenReturn(a1response);
	
		// -------------
		ReserveFundsResponse response = service.reserveFunds(headers, request);

		// -------------
		verify(dao).recordReservation(any(), any(ReservationRequest.class));
		verify(debitDao).getOverdraftInstructions(anyString());
		verify(debitDao).getAccount(anyString());

		// -------------
		assertTrue (response.getStatus() == ReserveFundsResponse.INSUFFICIENT_FUNDS);
		
		TransactionResource rr = response.getTransactions().get(0);
		assertTrue (rr.getTransactionUuid().equals(a1response.getResource().getTransactionUuid()));
		assertTrue (rr.getAccountNumber().equals(a1response.getResource().getAccountNumber()));
		assertTrue (rr.getDebitCardNumber() == null);
		assertTrue (rr.getInsertTimestamp() == a1response.getResource().getInsertTimestamp());
		assertTrue (rr.getReservationUuid().equals(a1response.getResource().getReservationUuid()));
		assertTrue (rr.getRequestUuid().equals(a1response.getResource().getRequestUuid()));
		assertTrue (rr.getRunningBalanceAmount() == a1response.getResource().getRunningBalanceAmount());
		assertTrue (rr.getTransactionAmount() == a1response.getResource().getTransactionAmount());
		assertTrue (rr.getTransactionMetaDataJson() == a1response.getResource().getTransactionMetaDataJson());
		assertTrue (rr.getTransactionTypeCode() == a1response.getResource().getTransactionTypeCode());
	}
	@Test
	void testPostReserveAccountFunds_OD_twoNSF() {
		
		// -----------------
		HashMap<String, String> headers = new HashMap<String, String>();
		headers.put(TraceableRequest.AIT_ID, "12345");
		headers.put(TraceableRequest.BUSINESS_TAXONOMY_ID, "7483495");
		headers.put(TraceableRequest.CORRELATION_ID, "273849273498273498");

		// ------------------
		ReserveFundsRequest request = new ReserveFundsRequest();
		request.setRequestUUID(UUID.randomUUID());
		request.setAccountNumber("1234HHHH1234");
		request.setTransactionAmount(27384);
		request.setTransactionMetaDataJSON("{}");
		request.setProtectAgainstOverdraft(true);

		// -------------------
		Account acct = new Account();
		acct.setAccountNumber("1234HGJT78934");
		acct.setAccountLifeCycleStatus("EF");
		when(debitDao.getAccount(anyString())).thenReturn(acct);
		
		// ------------------
		LinkedList<OverdraftInstruction> odlist = new LinkedList<OverdraftInstruction>();
		OverdraftInstruction od = new OverdraftInstruction();
		od.setOverdraftAccount(new Account());
		od.getOverdraftAccount().setAccountLifeCycleStatus("EF");
		od.getOverdraftAccount().setAccountNumber("1234hjkf8943");
		od.setInstructionLifecycleStatus("EF");
		od.setEffectiveStart(LocalDateTime.now().minusYears(1));
		od.setEffectiveEnd(LocalDateTime.now().plusYears(1));
		odlist.add(od);
		
		od = new OverdraftInstruction();
		od.setOverdraftAccount(new Account());
		od.getOverdraftAccount().setAccountLifeCycleStatus("EF");
		od.getOverdraftAccount().setAccountNumber("782342634234");
		od.setInstructionLifecycleStatus("EF");
		od.setEffectiveStart(LocalDateTime.now().minusYears(1));
		od.setEffectiveEnd(LocalDateTime.now().plusYears(1));
		odlist.add(od);
		
		when(debitDao.getOverdraftInstructions(anyString())).thenReturn(odlist);

		// -------------------
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
		transaction.setTransactionTypeCode(TransactionResource.REJECTED_TRANSACTION);
		ReservationResponse a1response = new ReservationResponse(ReservationResponse.INSUFFICIENT_FUNDS, transaction);

		transaction = new TransactionResource();
		transaction.setTransactionUuid(UUID.randomUUID());
		transaction.setAccountNumber("123781923123");
		transaction.setDebitCardNumber(null);
		transaction.setInsertTimestamp(new Timestamp(Instant.now().toEpochMilli()));
		transaction.setReservationUuid(UUID.randomUUID());
		transaction.setRequestUuid(UUID.randomUUID());
		transaction.setRunningBalanceAmount(99999L);
		transaction.setTransactionAmount(-2323L);
		transaction.setTransactionMetaDataJson("{etc, etc}");
		transaction.setTransactionTypeCode(TransactionResource.REJECTED_TRANSACTION);
		ReservationResponse a2response = new ReservationResponse(ReservationResponse.INSUFFICIENT_FUNDS, transaction);
		
		transaction = new TransactionResource();
		transaction.setTransactionUuid(UUID.randomUUID());
		transaction.setAccountNumber("378922983742938");
		transaction.setDebitCardNumber(null);
		transaction.setInsertTimestamp(new Timestamp(Instant.now().toEpochMilli()));
		transaction.setReservationUuid(UUID.randomUUID());
		transaction.setRequestUuid(UUID.randomUUID());
		transaction.setRunningBalanceAmount(99999L);
		transaction.setTransactionAmount(-2323L);
		transaction.setTransactionMetaDataJson("{etc, etc}");
		transaction.setTransactionTypeCode(TransactionResource.RESERVATION);
		ReservationResponse a3response = new ReservationResponse(ReservationResponse.SUCCESS, transaction);
		
		when(dao.recordReservation(any(), any(ReservationRequest.class)))
			.thenReturn(a1response)
			.thenReturn(a2response)
			.thenReturn(a3response);
	
		// -------------
		ReserveFundsResponse response = service.reserveFunds(headers, request);

		// -------------
		verify(dao, times(3) ).recordReservation(any(), any(ReservationRequest.class));
		verify(debitDao).getOverdraftInstructions(anyString());
		verify(debitDao).getAccount(anyString());

		// -------------
		assertTrue (response.getStatus() == ReserveFundsResponse.SUCCESS_OVERDRAFT);
		
		TransactionResource rr = response.getTransactions().get(0);
		assertTrue (rr.getTransactionUuid().equals(a1response.getResource().getTransactionUuid()));
		assertTrue (rr.getAccountNumber().equals(a1response.getResource().getAccountNumber()));
		assertNull (rr.getDebitCardNumber());
		assertTrue (rr.getInsertTimestamp() == a1response.getResource().getInsertTimestamp());
		assertTrue (rr.getReservationUuid().equals(a1response.getResource().getReservationUuid()));
		assertTrue (rr.getRequestUuid().equals(a1response.getResource().getRequestUuid()));
		assertTrue (rr.getRunningBalanceAmount() == a1response.getResource().getRunningBalanceAmount());
		assertTrue (rr.getTransactionAmount() == a1response.getResource().getTransactionAmount());
		assertTrue (rr.getTransactionMetaDataJson() == a1response.getResource().getTransactionMetaDataJson());
		assertTrue (rr.getTransactionTypeCode() == a1response.getResource().getTransactionTypeCode());

		rr = response.getTransactions().get(1);
		assertTrue (rr.getTransactionUuid().equals(a2response.getResource().getTransactionUuid()));
		assertTrue (rr.getAccountNumber().equals(a2response.getResource().getAccountNumber()));
		assertNull (rr.getDebitCardNumber());
		assertTrue (rr.getInsertTimestamp() == a2response.getResource().getInsertTimestamp());
		assertTrue (rr.getReservationUuid().equals(a2response.getResource().getReservationUuid()));
		assertTrue (rr.getRequestUuid().equals(a2response.getResource().getRequestUuid()));
		assertTrue (rr.getRunningBalanceAmount() == a2response.getResource().getRunningBalanceAmount());
		assertTrue (rr.getTransactionAmount() == a2response.getResource().getTransactionAmount());
		assertTrue (rr.getTransactionMetaDataJson() == a2response.getResource().getTransactionMetaDataJson());
		assertTrue (rr.getTransactionTypeCode() == a2response.getResource().getTransactionTypeCode());
		
		rr = response.getTransactions().get(2);
		assertTrue (rr.getTransactionUuid().equals(a3response.getResource().getTransactionUuid()));
		assertTrue (rr.getAccountNumber().equals(a3response.getResource().getAccountNumber()));
		assertNull (rr.getDebitCardNumber());
		assertTrue (rr.getInsertTimestamp() == a3response.getResource().getInsertTimestamp());
		assertTrue (rr.getReservationUuid().equals(a3response.getResource().getReservationUuid()));
		assertTrue (rr.getRequestUuid().equals(a3response.getResource().getRequestUuid()));
		assertTrue (rr.getRunningBalanceAmount() == a3response.getResource().getRunningBalanceAmount());
		assertTrue (rr.getTransactionAmount() == a3response.getResource().getTransactionAmount());
		assertTrue (rr.getTransactionMetaDataJson() == a3response.getResource().getTransactionMetaDataJson());
		assertTrue (rr.getTransactionTypeCode() == a3response.getResource().getTransactionTypeCode());
	}
}
