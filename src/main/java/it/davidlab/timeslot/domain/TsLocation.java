package it.davidlab.timeslot.domain;


public class TsLocation {

    private String address;
    private float latitude;
    private float longitude;
    private boolean hasCoordinates;

    public TsLocation() {
    }

    public TsLocation(String address) {
        this.address = address;
        this.latitude = 0;
        this.longitude = 0;
        this.hasCoordinates = false;
    }

    public TsLocation(String address, float latitude, float longitude) {
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.hasCoordinates = true;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TsLocation)) return false;
        TsLocation tsLocation = (TsLocation) o;
        return Float.compare(tsLocation.latitude, latitude) == 0 && Float.compare(tsLocation.longitude, longitude) == 0 && hasCoordinates == tsLocation.hasCoordinates && address.equals(tsLocation.address);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(address, latitude, longitude, hasCoordinates);
    }
}
