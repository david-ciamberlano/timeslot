package it.davidlab.timeslot.domain;


import java.util.concurrent.TimeUnit;

public class TimeslotDto {

    private long assetId;
    private String unitName;
    private String  assetName;
    private String url;
    private long amount;
    private long startValidity;
    private long endValidity;
    private long duration;
    private TimeslotUnit timeslotUnit;
    private String description;
    private TimeslotLocation timeslotLocation;
    private long price;
    private TimeslotType type;


    public TimeslotDto(long assetId, String unitName, String assetName, String url, long amount, long startValidity,
                       long endValidity, long duration, TimeslotUnit timeUnit, String description, TimeslotLocation timeslotLocation,
                       long price, TimeslotType type) {
        this.assetId = assetId;
        this.unitName = unitName;
        this.assetName = assetName;
        this.url = url;
        this.amount = amount;
        this.startValidity = startValidity;
        this.endValidity = endValidity;
        this.duration = duration;
        this.timeslotUnit = timeUnit;
        this.description = description;
        this.timeslotLocation = timeslotLocation;
        this.price = price;
        this.type = type;
    }

    public long getAssetId() {
        return assetId;
    }

    public String getUnitName() {
        return unitName;
    }

    public String getAssetName() {
        return assetName;
    }

    public String getUrl() {
        return url;
    }

    public long getAmount() {
        return amount;
    }

    public void setAmount(long amount) {
        this.amount = amount;
    }

    public long getStartValidity() {
        return startValidity;
    }

    public long getEndValidity() {
        return endValidity;
    }

    public long getDuration() {
        return duration;
    }

    public TimeslotUnit getTimeslotUnit() {
        return timeslotUnit;
    }

    public String getDescription() {
        return description;
    }

    public long getPrice() {
        return price;
    }

    public TimeslotType getType() {
        return type;
    }

    public TimeslotLocation getTsLocation() {
        return timeslotLocation;
    }

}
