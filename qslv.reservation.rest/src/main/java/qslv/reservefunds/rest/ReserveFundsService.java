package qslv.reservefunds.rest;
import qslv.transaction.request.ReservationRequest;
import qslv.transaction.resource.TransactionResource;
import qslv.transaction.response.ReservationResponse;

import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import qslv.data.Account;
import qslv.data.DebitCard;
import qslv.data.OverdraftInstruction;
import qslv.reservefunds.request.ReserveFundsRequest;
import qslv.reservefunds.response.ReserveFundsResponse;

@Service
public class ReserveFundsService {
	private static final Logger log = LoggerFactory.getLogger(ReserveFundsService.class);

	@Autowired
	private JdbcDao jdbcDao;
	
	@Autowired
	private TransactionDao trDao;

	public void setDao(JdbcDao dao) {
		this.jdbcDao = dao;
	}
	public void setTrDao(TransactionDao trDao) {
		this.trDao = trDao;
	}

	public ReserveFundsResponse reserveFunds(Map<String, String> callingHeaders, ReserveFundsRequest request) {
		log.trace("service.reserveFunds ENTRY");
		
		if (request.getDebitCardNumber() == null) {
			Account acctResource = jdbcDao.getAccount(request.getAccountNumber());

			if (false == accountInGoodStanding(acctResource)) {
				throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
						String.format("Account is in an invalid state."));
			}
		} else {
			DebitCard debitResource = jdbcDao.getDebitCardAndAccount(request.getDebitCardNumber());

			if (false == debitCardInGoodStanding(debitResource)) {
				throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
						String.format("Invalid Debit Card state."));

			}
			if (false == accountInGoodStanding(debitResource.getAccount())) {
				throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
						String.format("Account associated with debit card is in an invalid state."));

			}
			request.setAccountNumber(debitResource.getAccount().getAccountNumber());
		}

		// ---------------
		ReservationRequest treq = new ReservationRequest();
		treq.setAccountNumber(request.getAccountNumber());
		treq.setDebitCardNumber(request.getDebitCardNumber());
		treq.setRequestUuid(request.getRequestUUID());
		treq.setTransactionAmount(request.getTransactionAmount());
		treq.setTransactionMetaDataJson(request.getTransactionMetaDataJSON());
		treq.setProtectAgainstOverdraft(true);
		
		// ---------------
		ReservationResponse reservationResponse = trDao.recordReservation(callingHeaders, treq);

		// ---------------
		ReserveFundsResponse response = new ReserveFundsResponse();
		response.setTransactions(new LinkedList<TransactionResource>());
		response.getTransactions().add(reservationResponse.getResource());
		
		if (reservationResponse.getStatus() == ReservationResponse.SUCCESS ) {	
			response.setStatus(ReserveFundsResponse.SUCCESS);
		} else {
			response.setStatus(ReserveFundsResponse.INSUFFICIENT_FUNDS);
			if ( request.isProtectAgainstOverdraft() ) {
				 processOverdraftInstructions( callingHeaders, request, response);
			}
		}
		
		return response;
	}
	
	private void processOverdraftInstructions(Map<String, String> callingHeaders, 
			ReserveFundsRequest request, ReserveFundsResponse response) {
		log.debug("service.processOverdraftAccount ENTRY");

		List<OverdraftInstruction> overdraftInstructions = jdbcDao
				.getOverdraftInstructions(request.getAccountNumber());

		ReservationRequest treq = new ReservationRequest();
		treq.setDebitCardNumber(request.getDebitCardNumber());
		treq.setRequestUuid(request.getRequestUUID());
		treq.setTransactionAmount(request.getTransactionAmount());
		treq.setTransactionMetaDataJson(request.getTransactionMetaDataJSON());
		treq.setProtectAgainstOverdraft(true);

		ListIterator<OverdraftInstruction> iter = overdraftInstructions.listIterator();
		while (iter.hasNext()) {
			OverdraftInstruction instruction = iter.next();
			if (false == instructionEffective(instruction) ||
				false ==accountInGoodStanding(instruction.getOverdraftAccount())) {
				log.debug("Overdraft Instruction not valid. {}", instruction.toString());
			} else {
				treq.setAccountNumber(instruction.getOverdraftAccount().getAccountNumber());
				ReservationResponse reservationResponse = trDao.recordReservation(callingHeaders, treq);
				response.getTransactions().add(reservationResponse.getResource());

				if (reservationResponse.getStatus() == ReservationResponse.SUCCESS) {
					response.setStatus(ReserveFundsResponse.SUCCESS_OVERDRAFT);
					log.debug("Overdraft Instruction success. {}", reservationResponse.toString());
					break;
				} else {
					log.debug("Overdraft Instruction failed. {}", instruction.toString());
				}
			}			
		}

		log.debug("service.processOverdraftAccount EXIT");
		return;
	}

	private boolean debitCardInGoodStanding(DebitCard debitResource) {
		return (debitResource.getDebitCardLifeCycleStatus().contentEquals("EF"));
	}

	private boolean instructionEffective(OverdraftInstruction instruction) {
		return ( instruction.getInstructionLifecycleStatus().contentEquals("EF") &&
				 java.time.LocalDateTime.now().compareTo(instruction.getEffectiveStart()) > 0 &&
				 ( instruction.getEffectiveEnd() == null ||
				 java.time.LocalDateTime.now().compareTo(instruction.getEffectiveEnd()) < 0) );
	}

	private boolean accountInGoodStanding(Account account) {
		return (account.getAccountLifeCycleStatus().contentEquals("EF"));
	}

}