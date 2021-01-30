package it.davidlab.timeslot.domain;

public class TimeslotLocation {

    private String name;
    private String address;
    private float latitude;
    private float longitude;
    private boolean hasCoordinates;

    public TimeslotLocation() {
    }

    public TimeslotLocation(String name, String address) {
        this.name = name;
        this.address = address;
        this.latitude = 0;
        this.longitude = 0;
        this.hasCoordinates = false;
    }

    public TimeslotLocation(String name, String address, float latitude, float longitude) {
        this.name = name;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.hasCoordinates = true;
    }

    public String getName() {
        return name;
    }

    public String getAddress() {
        return address;
    }

    public float getLatitude() {
        return latitude;
    }

    public float getLongitude() {
        return longitude;
    }

    public boolean isHasCoordinates() {
        return hasCoordinates;
    }



}
