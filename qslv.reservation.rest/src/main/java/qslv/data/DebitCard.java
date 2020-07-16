package qslv.data;

public class DebitCard {

	private String debitCardNumber;
	private String debitCardLifeCycleStatus;
	private Account account;
	
	public String getDebitCardNumber() {
		return debitCardNumber;
	}
	public void setDebitCardNumber(String debitCardNumber) {
		this.debitCardNumber = debitCardNumber;
	}
	public String getDebitCardLifeCycleStatus() {
		return debitCardLifeCycleStatus;
	}
	public void setDebitCardLifeCycleStatus(String debitCardLifeCycleStatus) {
		this.debitCardLifeCycleStatus = debitCardLifeCycleStatus;
	}
	public Account getAccount() {
		return account;
	}
	public void setAccount(Account account) {
		this.account = account;
	}
	
}
