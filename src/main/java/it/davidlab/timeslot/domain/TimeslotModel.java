package it.davidlab.timeslot.domain;


public class TimeslotModel {

    private String creatorAddress;
    private boolean defaultFrozen;
    private String unitName;
    private String  assetName;
    private long assetTotal;
    private int assetDecimals;
    private String url;
    private TimeslotProperties timeslotProperties;

    public TimeslotModel() {
    }

    public TimeslotModel(String creatorAddress, boolean defaultFrozen, String unitName,
                         String assetName, long assetTotal, int assetDecimals,
                         String url, TimeslotProperties timeslotProperties) {
        this.creatorAddress = creatorAddress;
        this.defaultFrozen = defaultFrozen;
        this.unitName = unitName;
        this.assetName = assetName;
        this.assetTotal = assetTotal;
        this.assetDecimals = assetDecimals;
        this.url = url;
        this.timeslotProperties = timeslotProperties;
    }

    public String getCreatorAddress() {
        return this.creatorAddress;
    }

    public boolean isDefaultFrozen() {
        return this.defaultFrozen;
    }

    public String getUnitName() {
        return this.unitName;
    }

    public String getAssetName() {
        return this.assetName;
    }

    public long getAssetTotal() {
        return this.assetTotal;
    }

    public int getAssetDecimals() {
        return this.assetDecimals;
    }

    public String getUrl() {
        return this.url;
    }

    public TimeslotProperties getTimeslotProperties() {
        return timeslotProperties;
    }
}
