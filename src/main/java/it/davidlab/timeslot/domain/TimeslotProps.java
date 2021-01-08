package it.davidlab.timeslot.domain;


import java.util.concurrent.TimeUnit;

public class TimeslotProps {

    private long startValidity;
    private long endValidity;
    private long duration;
    private TimeUnit timeUnit;
    private String description;
    private long price;
    private AssetType type;
    private TsLocation tsLocation;


    public TimeslotProps() {
    }

    public TimeslotProps(long startValidity, long endValidity, long duration, TimeUnit timeUnit, String description, long price, AssetType type) {
        this.startValidity = startValidity;
        this.endValidity = endValidity;
        this.duration = duration;
        this.timeUnit = timeUnit;
        this.description = description;
        this.price = price;
        this.type = type;
    }

    public long getStartValidity() {
        return startValidity;
    }

    public long getEndValidity() {
        return endValidity;
    }

    public String getDescription() {
        return description;
    }

    public long getPrice() {
        return price;
    }

    public long getDuration() {
        return duration;
    }

    public TimeUnit getTimeUnit() {
        return timeUnit;
    }

    public AssetType getType() {
        return type;
    }
}
