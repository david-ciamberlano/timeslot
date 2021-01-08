package it.davidlab.timeslot.dto;

public class TxInfo {

    private String txId;
    private long ticketId;
    private long amount;
    private String senderAddr;
    private String receiverAddr;
    private long timestamp;
    private String note;

    public TxInfo(String txId, long ticketId, long amount, String senderAddr,
                  String receiverAddr, long timestamp, String note) {
        this.txId = txId;
        this.ticketId = ticketId;
        this.amount = amount;
        this.senderAddr = senderAddr;
        this.receiverAddr = receiverAddr;
        this.timestamp = timestamp;
        this.note = note;
    }

    public String getTxId() {
        return txId;
    }

    public long getTicketId() {
        return ticketId;
    }

    public long getAmount() {
        return amount;
    }

    public String getSenderAddr() {
        return senderAddr;
    }

    public String getReceiverAddr() {
        return receiverAddr;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getNote() {
        return note;
    }
}
