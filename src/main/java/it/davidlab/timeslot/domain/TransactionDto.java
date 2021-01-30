package it.davidlab.timeslot.domain;

public class TransactionDto {

    private String txId;
    private long timeslotId;
    private long amount;
    private String senderUser;
    private String receiverUser;
    private long timestamp;

    private String note;

    public TransactionDto() {
    }

    public TransactionDto(String txId, long timeslotId, long amount, String senderUser,
                          String receiverUser, long timestamp, String note) {
        this.txId = txId;
        this.timeslotId = timeslotId;
        this.amount = amount;
        this.senderUser = senderUser;
        this.receiverUser = receiverUser;
        this.timestamp = timestamp;
        this.note = note;
    }

    public String getTxId() {
        return txId;
    }

    public long getTimeslotId() {
        return timeslotId;
    }

    public void setTimeslotId(long timeslotId) {
        this.timeslotId = timeslotId;
    }

    public long getAmount() {
        return amount;
    }

    public String getSenderUser() {
        return senderUser;
    }

    public String getReceiverUser() {
        return receiverUser;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getNote() {
        return note;
    }
}
