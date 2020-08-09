package qslv.reservefunds.rest;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
import org.springframework.web.server.ResponseStatusException;

import qslv.data.Account;
import qslv.data.DebitCard;
import qslv.data.OverdraftInstruction;

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
	public void getAccount_noRows() {		
		String accountNumber = "DDDD3456HKWER7890";

		doReturn(new ArrayList<Account>())
			.when(jdbcTemplate)
			.query(anyString(), ArgumentMatchers.<RowMapper<Account>>any(), anyString());
	
		assertThrows(ResponseStatusException.class, ()-> { jdbcDao.getAccount(accountNumber); } );
		verify(jdbcTemplate).query(anyString(), ArgumentMatchers.<RowMapper<Account>>any(), anyString());	

	}
	
	@Test
	public void getAccount_throws() {		
		String accountNumber = "DDDD3456HKWER7890";

		doThrow(new QueryTimeoutException("message"))
			.when(jdbcTemplate).query(anyString(), ArgumentMatchers.<RowMapper<Account>>any(), anyString());
		
		assertThrows(DataAccessException.class, ()-> { jdbcDao.getAccount(accountNumber); } );
		verify(jdbcTemplate).query(anyString(), ArgumentMatchers.<RowMapper<Account>>any(), anyString());	

	}
	
	@Test
	public void test_getDebitCardAndAccount_success() {
		
		String accountNumber = "DDDD3456HKWER7890";

		// ---- Setup ----
		DebitCard setupDebit = new DebitCard();
		setupDebit.setDebitCardLifeCycleStatus("EF");
		setupDebit.setDebitCardNumber("2734827349");
		Account setupAccount = new Account();
		setupAccount.setAccountLifeCycleStatus("EF");
		setupAccount.setAccountNumber(accountNumber);
		setupDebit.setAccount(setupAccount);
		
		// ---- Prepare ----
		doReturn(Collections.singletonList(setupDebit))
			.when(jdbcTemplate).query(anyString(), ArgumentMatchers.<RowMapper<Account>>any(), anyString());
		
		// ---- Execute ----
		DebitCard debitCard = jdbcDao.getDebitCardAndAccount(accountNumber);
		
		// ---- Verify ----
		verify(jdbcTemplate).query(anyString(), ArgumentMatchers.<RowMapper<Account>>any(), anyString());		
		assertSame(debitCard, setupDebit);
	}

	@Test
	public void test_getDebitCardAndAccount_noRows() {
		
		String accountNumber = "DDDD3456HKWER7890";

		
		// ---- Prepare ----
		doReturn(new ArrayList<DebitCard>())
			.when(jdbcTemplate)
			.query(anyString(), ArgumentMatchers.<RowMapper<Account>>any(), anyString());
		
		// ---- Execute ----
		assertThrows(ResponseStatusException.class, ()-> { jdbcDao.getDebitCardAndAccount(accountNumber); } );
		
		// ---- Verify ----
		verify(jdbcTemplate).query(anyString(), ArgumentMatchers.<RowMapper<Account>>any(), anyString());
	}
	
	@Test
	public void test_getOverdraftInstructions_success() {
		
		String accountNumber = "DDDD3456HKWER7890";

		// ---- Setup ----
		ArrayList<OverdraftInstruction> setuplist = new ArrayList<>();
		OverdraftInstruction instruction = new OverdraftInstruction();
		instruction.setOverdraftAccount(new Account());
		setuplist.add(instruction);
		 instruction = new OverdraftInstruction();
		instruction.setOverdraftAccount(new Account());
		setuplist.add(instruction);

		
		// ---- Prepare ----
		doReturn(setuplist)
			.when(jdbcTemplate)
			.query(anyString(), ArgumentMatchers.<RowMapper<Account>>any(), anyString());
		
		// ---- Execute ----
		List<OverdraftInstruction> ods = jdbcDao.getOverdraftInstructions(accountNumber);
		
		// ---- Verify ----
		verify(jdbcTemplate).query(anyString(), ArgumentMatchers.<RowMapper<Account>>any(), anyString());		
		assertSame(setuplist, ods);
	}

	@Test
	public void test_getOverdraftInstructions_noRows() {
		
		String accountNumber = "DDDD3456HKWER7890";

		// ---- Prepare ----
		doReturn(new ArrayList<OverdraftInstruction>())
			.when(jdbcTemplate)
			.query(anyString(), ArgumentMatchers.<RowMapper<Account>>any(), anyString());
		
		// ---- Execute ----
		assertThrows(ResponseStatusException.class, ()-> { jdbcDao.getOverdraftInstructions(accountNumber); } );
		
		// ---- Verify ----
		verify(jdbcTemplate).query(anyString(), ArgumentMatchers.<RowMapper<Account>>any(), anyString());
	}
}
