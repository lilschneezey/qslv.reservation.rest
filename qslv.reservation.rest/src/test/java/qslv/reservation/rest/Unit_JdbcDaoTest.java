package qslv.reservation.rest;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import qslv.data.Account;

@ExtendWith(MockitoExtension.class)
public class Unit_JdbcDaoTest {
	@Mock 
	JdbcTemplate jdbcTemplate; 
	
	JdbcDao jdbcDao = new JdbcDao();
	
	@BeforeEach
	public void init() {
		jdbcDao.setJdbcTemplate(jdbcTemplate);		
	}

	@Test
	public void test_getAccount_success() {
		
		String accountNumber = "DDDD3456HKWER7890";

		// ---- Setup ----
		Account setupAccount = new Account();
		setupAccount.setAccountLifeCycleStatus("EF");
		setupAccount.setAccountNumber(accountNumber);
		
		// ---- Prepare ----
		doReturn(Collections.singletonList(setupAccount))
			.when(jdbcTemplate).query(anyString(), ArgumentMatchers.<RowMapper<Account>>any(), anyString());
		
		// ---- Execute ----
		Account account = jdbcDao.getAccount(accountNumber);
		
		// ---- Verify ----
		verify(jdbcTemplate).query(anyString(), ArgumentMatchers.<RowMapper<Account>>any(), anyString());		
		assertSame(account, setupAccount);
	}

	@Test
	public void getAccount_throws() {		
		String accountNumber = "DDDD3456HKWER7890";


		doThrow(new QueryTimeoutException("message"))
			.when(jdbcTemplate).query(anyString(), ArgumentMatchers.<RowMapper<Account>>any(), anyString());
		
		assertThrows(DataAccessException.class, ()-> { jdbcDao.getAccount(accountNumber); } );
		verify(jdbcTemplate).query(anyString(), ArgumentMatchers.<RowMapper<Account>>any(), anyString());	

	}

}
