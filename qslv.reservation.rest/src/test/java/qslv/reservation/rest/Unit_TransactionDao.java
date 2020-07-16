package qslv.reservation.rest;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import qslv.common.TimedResponse;
import qslv.common.TraceableRequest;
import qslv.transaction.request.ReservationRequest;
import qslv.transaction.resource.TransactionResource;
import qslv.transaction.response.ReservationResponse;

@ExtendWith(MockitoExtension.class)
@SpringBootTest
class Unit_TransactionDao {

	@Mock
	RestTemplate template;
	
	@Autowired
	TransactionDao transactionDao;

	@BeforeEach
	public void init() {
		transactionDao.setTemplate(template);
	}
	
	@Test
	void test_recordReservation_success() {
		
		// ------------------
		Map<String, String> headers = new HashMap<String, String>();
		headers.put(TraceableRequest.BUSINESS_TAXONOMY_ID, "78237492834");
		headers.put(TraceableRequest.CORRELATION_ID, "234234234234234234");
		
		//------------------
		ReservationRequest request = new ReservationRequest();
		request.setAccountNumber("237489237492");
		request.setDebitCardNumber("1234HHHH1234JJJJ");
		request.setRequestUuid(UUID.randomUUID());
		request.setTransactionAmount(-27384);
		request.setTransactionMetaDataJson("{}");
		
		//------------------
		TimedResponse<ReservationResponse> rr = new TimedResponse<ReservationResponse>();
		rr.setPayload(new ReservationResponse(ReservationResponse.SUCCESS, new TransactionResource()));
		ResponseEntity<TimedResponse<ReservationResponse>> response = 
			new ResponseEntity<TimedResponse<ReservationResponse>>(rr, HttpStatus.OK);
		
		//-----------------
		when(template.exchange(anyString(), eq(HttpMethod.POST), 
				ArgumentMatchers.<HttpEntity<ReservationRequest>>any(), 
				ArgumentMatchers.<ParameterizedTypeReference<TimedResponse<ReservationResponse>>>any()))
			.thenReturn(response);
		
		ReservationResponse callresult = transactionDao.recordReservation(headers, request);
		assert(callresult.getStatus() == ReservationResponse.SUCCESS);
	}

	@Test
	void test_recordReservation_failsOnce() {
		
		// ------------------
		Map<String, String> headers = new HashMap<String, String>();
		headers.put(TraceableRequest.BUSINESS_TAXONOMY_ID, "78237492834");
		headers.put(TraceableRequest.CORRELATION_ID, "234234234234234234");
		
		//------------------
		ReservationRequest request = new ReservationRequest();
		request.setAccountNumber("237489237492");
		request.setDebitCardNumber("1234HHHH1234JJJJ");
		request.setRequestUuid(UUID.randomUUID());
		request.setTransactionAmount(-27384);
		request.setTransactionMetaDataJson("{}");
		
		//------------------
		TimedResponse<ReservationResponse> rr = new TimedResponse<ReservationResponse>();
		rr.setServiceTimeElapsed(345890L);
		rr.setPayload(new ReservationResponse(ReservationResponse.SUCCESS, new TransactionResource()));
		ResponseEntity<TimedResponse<ReservationResponse>> response = 
			new ResponseEntity<TimedResponse<ReservationResponse>>(rr, HttpStatus.OK);
		//-----------------
		when(template.exchange(anyString(), eq(HttpMethod.POST), 
				ArgumentMatchers.<HttpEntity<ReservationRequest>>any(), 
				ArgumentMatchers.<ParameterizedTypeReference<TimedResponse<ReservationResponse>>>any()))
			.thenThrow(new ResourceAccessException("message", new SocketTimeoutException()) )
			.thenReturn(response);
		
		ReservationResponse callresult = transactionDao.recordReservation(headers, request);
		assert(callresult.getStatus() == ReservationResponse.SUCCESS);
	}

	@Test
	void test_recordReservation_failsTwice() {
		
		// ------------------
		Map<String, String> headers = new HashMap<String, String>();
		headers.put(TraceableRequest.BUSINESS_TAXONOMY_ID, "78237492834");
		headers.put(TraceableRequest.CORRELATION_ID, "234234234234234234");
		
		//------------------
		ReservationRequest request = new ReservationRequest();
		request.setAccountNumber("237489237492");
		request.setDebitCardNumber("1234HHHH1234JJJJ");
		request.setRequestUuid(UUID.randomUUID());
		request.setTransactionAmount(-27384);
		request.setTransactionMetaDataJson("{}");
		
		//------------------
		TimedResponse<ReservationResponse> rr = new TimedResponse<ReservationResponse>();
		rr.setServiceTimeElapsed(377890L);
		rr.setPayload(new ReservationResponse(ReservationResponse.SUCCESS, new TransactionResource()));
		ResponseEntity<TimedResponse<ReservationResponse>> response = 
			new ResponseEntity<TimedResponse<ReservationResponse>>(rr, HttpStatus.OK);
		
		//-----------------
		when(template.exchange(anyString(), eq(HttpMethod.POST), 
				ArgumentMatchers.<HttpEntity<ReservationRequest>>any(), 
				ArgumentMatchers.<ParameterizedTypeReference<TimedResponse<ReservationResponse>>>any()))
			.thenThrow(new ResourceAccessException("message", new SocketTimeoutException()) )
			.thenThrow(new ResourceAccessException("message", new SocketTimeoutException()) )
			.thenReturn(response);
		
		ReservationResponse callresult = transactionDao.recordReservation(headers, request);
		assert(callresult.getStatus() == ReservationResponse.SUCCESS);
	}
	
	@Test
	void test_recordReservation_failsThrice() {
		
		// ------------------
		Map<String, String> headers = new HashMap<String, String>();
		headers.put(TraceableRequest.BUSINESS_TAXONOMY_ID, "78237492834");
		headers.put(TraceableRequest.CORRELATION_ID, "234234234234234234");
		
		//------------------
		ReservationRequest request = new ReservationRequest();
		request.setAccountNumber("237489237492");
		request.setDebitCardNumber("1234HHHH1234JJJJ");
		request.setRequestUuid(UUID.randomUUID());
		request.setTransactionAmount(-27384);
		request.setTransactionMetaDataJson("{}");
		
		//-----------------
		when(template.exchange(anyString(), eq(HttpMethod.POST), 
				ArgumentMatchers.<HttpEntity<ReservationRequest>>any(), 
				ArgumentMatchers.<ParameterizedTypeReference<TimedResponse<ReservationResponse>>>any()))
			.thenThrow(new ResourceAccessException("message", new SocketTimeoutException()) )
			.thenThrow(new ResourceAccessException("message", new SocketTimeoutException()) )
			.thenThrow(new ResourceAccessException("message", new SocketTimeoutException()) );
		
		assertThrows(ResourceAccessException.class, () -> {
			transactionDao.recordReservation(headers, request);
		});

	}
}
