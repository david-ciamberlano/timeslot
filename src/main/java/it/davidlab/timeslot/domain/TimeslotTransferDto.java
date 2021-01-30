package it.davidlab.timeslot.domain;

public class TimeslotTransferDto {

    private String username;
    private long amount;
    private String transferNote;

    public TimeslotTransferDto(String username, long amount, String transferNote) {
        this.username = username;
        this.amount = amount;
        this.transferNote = transferNote;
    }

    public String getUsername() {
        return username;
    }

    public long getAmount() {
        return amount;
    }

    public String getTransferNote() {
        return transferNote;
    }
}
