package qslv.reservefunds.rest;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.web.server.ResponseStatusException;

import qslv.data.Account;
import qslv.data.DebitCard;
import qslv.data.OverdraftInstruction;
import qslv.util.ExternalResourceSLI;

@Repository
public class JdbcDao {
	private static final Logger log = LoggerFactory.getLogger(JdbcDao.class);

	@Autowired
	private JdbcTemplate jdbcTemplate;

	public void setJdbcTemplate(JdbcTemplate template) {
		this.jdbcTemplate = template;
	}

	public final static String getDebitCardData_sql = 
			"SELECT d.account_no, d.lifecycle_status_cd AS debit_lifecycle_status_cd, a.lifecycle_status_cd AS account_lifecycle_status_cd"
			+ " FROM debit_card d, account a "
			+ " WHERE a.account_no = d.account_no AND debit_card_no = ?; ";

	@ExternalResourceSLI(value="jdbc::AccountDB", ait = "#{@configProperties.aitid}", remoteFailures= {DataAccessException.class})
	public DebitCard getDebitCardAndAccount(final String debitCardNumber) {
		log.debug("getDebitCardAccount ENTRY {}", debitCardNumber);

		List<DebitCard> resources = jdbcTemplate.query(getDebitCardData_sql,
				new RowMapper<DebitCard>() {
					public DebitCard mapRow(ResultSet rs, int rowNum) throws SQLException {
						DebitCard res = new DebitCard();
						res.setAccount( new Account() );
						
						res.setDebitCardNumber(debitCardNumber);
						res.setDebitCardLifeCycleStatus(rs.getString(2));
						res.getAccount().setAccountLifeCycleStatus(rs.getString(3));
						res.getAccount().setAccountNumber(rs.getString(1));
						return res;
					}
				}, debitCardNumber);
		
		if ( resources == null || resources.size() != 1 ) {
			log.error("setupAccount, ERROR=%d rows returned, SQL=%s", resources.size(), getDebitCardData_sql);
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
					String.format("%d (!= 1) account - debit_card rows returned.", resources.size()));
		}
		
		log.debug("getDebitCardAccount DebitCard {} {}", resources.get(0).getDebitCardNumber(), resources.get(0).getDebitCardLifeCycleStatus(),
				resources.get(0).getAccount().getAccountNumber(), resources.get(0).getAccount().getAccountLifeCycleStatus());
		return resources.get(0);
	}

	public final static String getAccount_sql = "SELECT account_no, lifecycle_status_cd FROM account WHERE account_no = ?; ";

	@ExternalResourceSLI(value="jdbc::AccountDB", ait = "#{@configProperties.aitid}", remoteFailures= {DataAccessException.class})
	public Account getAccount(final String accountNumber) {
		log.debug("getAccount ENTRY {}", accountNumber);

		List<Account> resources = jdbcTemplate.query(getAccount_sql,
				new RowMapper<Account>() {
					public Account mapRow(ResultSet rs, int rowNum) throws SQLException {
						Account res = new Account();
						
						res.setAccountNumber(rs.getString(1));
						res.setAccountLifeCycleStatus(rs.getString(2));
						return res;
					}
				}, accountNumber);
		
		if ( resources == null || resources.size() != 1 ) {
			log.error("setupAccount, ERROR=%d rows returned, SQL=%s", resources.size(), getAccount_sql);
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
					String.format("%d (!= 1) account_balance rows returned.", resources.size()));
		}
		
		log.debug("getAccount EXIT {} {}", resources.get(0).getAccountNumber(), resources.get(0).getAccountLifeCycleStatus());
		return resources.get(0);
	}
	
	public final static String getOverdraftInstructions_sql = 
			"SELECT o.overdraft_account_no, oda.lifecycle_status_cd as od_lifecycle_status, o.lifecycle_status_cd, o.effective_start_dt, o.effective_end_dt"
			+ " FROM overdraft_instruction o, account oda"
			+ " WHERE o.account_no = ?"
			+ " AND o.overdraft_account_no = oda.account_no"
			+ " ORDER BY o.sequence asc;";
	
	public List<OverdraftInstruction> getOverdraftInstructions(final String accountNumber) {
		log.debug("getOverdraftInstructions ENTRY {}", accountNumber);

		List<OverdraftInstruction> resources = jdbcTemplate.query(getOverdraftInstructions_sql,
				new RowMapper<OverdraftInstruction>() {
					public OverdraftInstruction mapRow(ResultSet rs, int rowNum) throws SQLException {
						OverdraftInstruction res = new OverdraftInstruction();
						res.setOverdraftAccount(new Account());
						
						res.getOverdraftAccount().setAccountNumber(rs.getString(1));
						res.getOverdraftAccount().setAccountLifeCycleStatus(rs.getString(2));
						res.setInstructionLifecycleStatus(rs.getString(3));
						res.setEffectiveStart(rs.getDate(4).toLocalDate().atStartOfDay() );
						res.setEffectiveEnd(rs.getDate(5) == null ? null :rs.getDate(5).toLocalDate().atStartOfDay());
						return res;
					}
				}, accountNumber);
		
		if ( resources == null || resources.size() == 0 ) {
			log.error("setupAccount, ERROR=%d rows returned, SQL=%s", resources.size(), getOverdraftInstructions_sql);
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
					String.format("%d (!= 1) account_balance rows returned.", resources.size()));
		}

		log.debug("getOverdraftInstructions size {}", resources.size());
		return resources;
	}
}