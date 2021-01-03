package it.davidlab.timeslot.domain;


public class AssetModel {

    private String creatorAddress;
    private boolean defaultFrozen;
    private String unitName;
    private String  assetName;
    private long assetTotal;
    private int assetDecimals;
    private String url;
    private TimeslotProps timeslotProps;

    public AssetModel() {
    }

    public AssetModel(final String creatorAddress, final boolean defaultFrozen, final String unitName,
                      final String assetName, final long assetTotal, final int assetDecimals,
                      final String url, TimeslotProps timeslotProps) {
        this.creatorAddress = creatorAddress;
        this.defaultFrozen = defaultFrozen;
        this.unitName = unitName;
        this.assetName = assetName;
        this.assetTotal = assetTotal;
        this.assetDecimals = assetDecimals;
        this.url = url;
        this.timeslotProps = timeslotProps;
    }

    public String getCreatorAddress() {
        return this.creatorAddress;
    }

    public void setCreatorAddress(String creatorAddress) {
        this.creatorAddress = creatorAddress;
    }

    public boolean isDefaultFrozen() {
        return this.defaultFrozen;
    }

    public void setDefaultFrozen(final boolean defaultFrozen) {
        this.defaultFrozen = defaultFrozen;
    }

    public String getUnitName() {
        return this.unitName;
    }

    public void setUnitName(final String unitName) {
        this.unitName = unitName;
    }

    public String getAssetName() {
        return this.assetName;
    }

    public void setAssetName(final String assetName) {
        this.assetName = assetName;
    }

    public long getAssetTotal() {
        return this.assetTotal;
    }

    public void setAssetTotal(final long assetTotal) {
        this.assetTotal = assetTotal;
    }

    public int getAssetDecimals() {
        return this.assetDecimals;
    }

    public void setAssetDecimals(final int assetDecimals) {
        this.assetDecimals = assetDecimals;
    }

    public String getUrl() {
        return this.url;
    }

    public void setUrl(final String url) {
        this.url = url;
    }

    public TimeslotProps getAssetParams() {
        return timeslotProps;
    }

    public void setAssetParams(TimeslotProps timeslotProps) {
        this.timeslotProps = timeslotProps;
    }
}
