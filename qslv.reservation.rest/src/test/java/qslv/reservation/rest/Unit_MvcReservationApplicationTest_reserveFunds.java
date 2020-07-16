package qslv.reservation.rest;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.net.SocketTimeoutException;
import java.nio.charset.Charset;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collections;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import qslv.common.TimedResponse;
import qslv.common.TraceableRequest;
import qslv.data.Account;
import qslv.data.DebitCard;
import qslv.reservation.request.ReserveFundsRequest;
import qslv.reservation.response.ReserveFundsResponse;
import qslv.transaction.request.ReservationRequest;
import qslv.transaction.resource.TransactionResource;
import qslv.transaction.response.ReservationResponse;
import qslv.util.EnableQuickSilver;

@SpringBootTest
@AutoConfigureMockMvc
@EnableQuickSilver
@ComponentScan("qslv.util")
class Unit_MvcReservationApplicationTest_reserveFunds {

	public static final MediaType APPLICATION_JSON_UTF8 = new MediaType(MediaType.APPLICATION_JSON.getType(),
			MediaType.APPLICATION_JSON.getSubtype(), Charset.forName("utf8"));
	
	public TypeReference<TimedResponse<ReserveFundsResponse>> responseReference = 
			new TypeReference<TimedResponse<ReserveFundsResponse>>() {};
			
	@Autowired
	private MockMvc mockMvc;
	@Autowired
	TransactionDao dao;
	@Autowired
	JdbcDao jdbcDao;
	@Autowired
	ConfigProperties config;
	
	@Mock
	RestTemplate restTemplate;
	@Mock
	JdbcTemplate jdbcTemplate;

	@BeforeEach
	void setup() {
		dao.setTemplate(restTemplate);
		jdbcDao.setJdbcTemplate(jdbcTemplate);
	}
	
	@Test
	void testPostTransaction_debit_success() throws Exception {
		// ---------------------
		ReserveFundsRequest request = new ReserveFundsRequest();
		request.setAccountNumber(null);
		request.setDebitCardNumber("8398345345");
		request.setRequestUUID(UUID.randomUUID());
		request.setTransactionAmount(-2323L);
		request.setTransactionMetaDataJSON("{\"intvalue\":829342}");
		
		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(SerializationFeature.WRAP_ROOT_VALUE, false);
		String requestJson = mapper.writer().withDefaultPrettyPrinter().writeValueAsString(request);

		//--Jdbc Debit Card and Account
		DebitCard debitdata = new DebitCard();
		debitdata.setAccount(new Account());
		debitdata.getAccount().setAccountLifeCycleStatus("EF");
		debitdata.getAccount().setAccountNumber("234234234234");
		debitdata.setDebitCardLifeCycleStatus("EF");
		debitdata.setDebitCardNumber(request.getDebitCardNumber());
		when(jdbcTemplate.query( eq(JdbcDao.getDebitCardData_sql), ArgumentMatchers.<RowMapper<DebitCard>>any(), 
				eq(request.getDebitCardNumber()) ) ).thenReturn(Collections.singletonList(debitdata));
	
		//--Rest Transaction DAO ----------------
		TransactionResource transaction = new TransactionResource();
		transaction.setTransactionUuid(UUID.randomUUID());
		transaction.setAccountNumber(debitdata.getAccount().getAccountNumber());
		transaction.setDebitCardNumber(request.getDebitCardNumber());
		transaction.setInsertTimestamp(new Timestamp(Instant.now().toEpochMilli()));
		transaction.setReservationUuid(UUID.randomUUID());
		transaction.setRequestUuid(request.getRequestUUID());
		transaction.setRunningBalanceAmount(99999L);
		transaction.setTransactionAmount(request.getTransactionAmount());
		transaction.setTransactionMetaDataJson(request.getTransactionMetaDataJSON());
		transaction.setTransactionTypeCode(TransactionResource.RESERVATION);
		TimedResponse<ReservationResponse> rr = new TimedResponse<ReservationResponse>();
		rr.setPayload(new ReservationResponse(ReservationResponse.SUCCESS, transaction));
		rr.setServiceTimeElapsed(456123L);
		ResponseEntity<TimedResponse<ReservationResponse>> trr = 
			new ResponseEntity<TimedResponse<ReservationResponse>>(rr, HttpStatus.OK);
		
		when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), 
				ArgumentMatchers.<HttpEntity<ReservationRequest>>any(), 
				ArgumentMatchers.<ParameterizedTypeReference<TimedResponse<ReservationResponse>>>any()))
			.thenReturn(trr);
	
		// post transaction
		String stringResult = this.mockMvc.perform(post("/ReserveFunds")
				.contentType(APPLICATION_JSON_UTF8)
				.content(requestJson)
				.header(TraceableRequest.AIT_ID, "2345")
				.header(TraceableRequest.BUSINESS_TAXONOMY_ID, "2342")
				.header(TraceableRequest.CORRELATION_ID, "234234")
				.header(TraceableRequest.ACCEPT_VERSION, "v1") )
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();
		
		verify(jdbcTemplate).query( eq(JdbcDao.getDebitCardData_sql), ArgumentMatchers.<RowMapper<DebitCard>>any(), 
				eq(request.getDebitCardNumber()) );
		verify(restTemplate).exchange(anyString(), eq(HttpMethod.POST), 
				ArgumentMatchers.<HttpEntity<ReservationRequest>>any(), 
				ArgumentMatchers.<ParameterizedTypeReference<TimedResponse<TransactionResource>>>any());

		TimedResponse<ReserveFundsResponse> response = mapper.readValue(stringResult, responseReference);
		
		assertTrue(response.getPayload().getStatus() == ReservationResponse.SUCCESS);
		assertNotNull(response.getPayload());
		assertTrue( response.getServiceTimeElapsed() > 0);

		TransactionResource tr = response.getPayload().getTransactions().get(0);
		assertTrue(tr.getAccountNumber().equals(debitdata.getAccount().getAccountNumber()));
		assertTrue(tr.getDebitCardNumber().equals(request.getDebitCardNumber() ));
		assertTrue(tr.getRequestUuid().equals(request.getRequestUUID()));
		assertTrue(tr.getTransactionAmount() == request.getTransactionAmount());
		assertTrue(tr.getTransactionMetaDataJson().equals(request.getTransactionMetaDataJSON()));
		assertTrue(tr.getTransactionTypeCode().equals( TransactionResource.RESERVATION) );
		assertTrue(tr.getTransactionUuid().equals(rr.getPayload().getResource().getTransactionUuid()));
		assertTrue(tr.getRunningBalanceAmount() == rr.getPayload().getResource().getRunningBalanceAmount() );
		assertTrue(tr.getTransactionAmount() == request.getTransactionAmount());
	}
	
	@Test
	void testPostTransaction_debit_twoTimeouts() throws Exception {
		// ---------------------
		ReserveFundsRequest request = new ReserveFundsRequest();
		request.setAccountNumber(null);
		request.setDebitCardNumber("8398345345");
		request.setRequestUUID(UUID.randomUUID());
		request.setTransactionAmount(-2323L);
		request.setTransactionMetaDataJSON("{\"intvalue\":829342}");
		
		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(SerializationFeature.WRAP_ROOT_VALUE, false);
		String requestJson = mapper.writer().withDefaultPrettyPrinter().writeValueAsString(request);

		//--Jdbc Debit Card and Account
		DebitCard debitdata = new DebitCard();
		debitdata.setAccount(new Account());
		debitdata.getAccount().setAccountLifeCycleStatus("EF");
		debitdata.getAccount().setAccountNumber("234234234234");
		debitdata.setDebitCardLifeCycleStatus("EF");
		debitdata.setDebitCardNumber(request.getDebitCardNumber());
		when(jdbcTemplate.query( eq(JdbcDao.getDebitCardData_sql), ArgumentMatchers.<RowMapper<DebitCard>>any(), 
				eq(request.getDebitCardNumber()) ) ).thenReturn(Collections.singletonList(debitdata));
	

		//--Rest Transaction DAO ----------------
		TransactionResource transaction = new TransactionResource();
		transaction.setTransactionUuid(UUID.randomUUID());
		transaction.setAccountNumber(debitdata.getAccount().getAccountNumber());
		transaction.setDebitCardNumber(request.getDebitCardNumber());
		transaction.setInsertTimestamp(new Timestamp(Instant.now().toEpochMilli()));
		transaction.setReservationUuid(UUID.randomUUID());
		transaction.setRequestUuid(request.getRequestUUID());
		transaction.setRunningBalanceAmount(99999L);
		transaction.setTransactionAmount(request.getTransactionAmount());
		transaction.setTransactionMetaDataJson(request.getTransactionMetaDataJSON());
		transaction.setTransactionTypeCode(TransactionResource.RESERVATION);
		TimedResponse<ReservationResponse> rr = new TimedResponse<ReservationResponse>();
		rr.setPayload(new ReservationResponse(ReservationResponse.SUCCESS, transaction));
		rr.setServiceTimeElapsed(789123L);
		ResponseEntity<TimedResponse<ReservationResponse>> trr = 
			new ResponseEntity<TimedResponse<ReservationResponse>>(rr, HttpStatus.OK);
		
		when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), 
				ArgumentMatchers.<HttpEntity<ReservationRequest>>any(), 
				ArgumentMatchers.<ParameterizedTypeReference<TimedResponse<ReservationResponse>>>any()))
			.thenThrow(new ResourceAccessException("message", new SocketTimeoutException() ) )
			.thenThrow(new ResourceAccessException("message", new SocketTimeoutException() ) )
			.thenReturn(trr);
		
		//-------------
		config.setRestAttempts(3);
		
		// post transaction
		String stringResult = this.mockMvc.perform(post("/ReserveFunds")
				.contentType(APPLICATION_JSON_UTF8)
				.content(requestJson)
				.header(TraceableRequest.AIT_ID, "2345")
				.header(TraceableRequest.BUSINESS_TAXONOMY_ID, "2342")
				.header(TraceableRequest.CORRELATION_ID, "234234")
				.header(TraceableRequest.ACCEPT_VERSION, "v1") )
				.andExpect(status().isOk())
				.andReturn()
				.getResponse()
				.getContentAsString();
		
		verify(jdbcTemplate).query( eq(JdbcDao.getDebitCardData_sql), ArgumentMatchers.<RowMapper<DebitCard>>any(), 
				eq(request.getDebitCardNumber()) );
		verify(restTemplate, times(3)).exchange(anyString(), eq(HttpMethod.POST), 
				ArgumentMatchers.<HttpEntity<ReservationRequest>>any(), 
				ArgumentMatchers.<ParameterizedTypeReference<TimedResponse<TransactionResource>>>any());

		TimedResponse<ReserveFundsResponse> response = mapper.readValue(stringResult, responseReference);
		
		assertTrue(response.getPayload().getStatus() == ReservationResponse.SUCCESS);
		assertNotNull(response.getPayload());
		assertTrue( response.getServiceTimeElapsed() > 0);

		TransactionResource tr = response.getPayload().getTransactions().get(0);
		assertTrue(tr.getAccountNumber().equals(debitdata.getAccount().getAccountNumber()));
		assertTrue(tr.getDebitCardNumber().equals(request.getDebitCardNumber() ));
		assertTrue(tr.getRequestUuid().equals(request.getRequestUUID()));
		assertTrue(tr.getTransactionAmount() == request.getTransactionAmount());
		assertTrue(tr.getTransactionMetaDataJson().equals(request.getTransactionMetaDataJSON()));
		assertTrue(tr.getTransactionTypeCode().equals( TransactionResource.RESERVATION) );
		assertTrue(tr.getTransactionUuid().equals(rr.getPayload().getResource().getTransactionUuid()));
		assertTrue(tr.getRunningBalanceAmount() == rr.getPayload().getResource().getRunningBalanceAmount() );
		assertTrue(tr.getTransactionAmount() == request.getTransactionAmount());
	}
}
