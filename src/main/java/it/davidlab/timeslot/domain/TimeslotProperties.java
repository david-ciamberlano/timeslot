package it.davidlab.timeslot.domain;


import javax.validation.constraints.Min;

public class TimeslotProperties {

    @Min(0)
    private long startValidity;
    @Min(0)
    private long endValidity;
    @Min(0)
    private long duration;
    private TimeslotUnit timeslotUnit;
    private String description;
    @Min(0)
    private long price;
    private TimeslotType timeslotType;
    private TimeslotLocation timeslotLocation;


    public TimeslotProperties() {
    }

    public TimeslotProperties(long startValidity, long endValidity, long duration, TimeslotUnit timeslotUnit,
                              long price, TimeslotLocation timeslotLocation,
                              TimeslotType timeslotType, String description) {
        this.startValidity = startValidity;
        this.endValidity = endValidity;
        this.duration = duration;
        this.timeslotUnit = timeslotUnit;
        this.description = description;
        this.price = price;
        this.timeslotType = timeslotType;
        this.timeslotLocation = timeslotLocation;
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

    public TimeslotUnit getTimeslotUnit() {
        return timeslotUnit;
    }

    public TimeslotType getTimeslotType() {
        return timeslotType;
    }

    public TimeslotLocation getTimeslotLocation() {
        return timeslotLocation;
    }
}
