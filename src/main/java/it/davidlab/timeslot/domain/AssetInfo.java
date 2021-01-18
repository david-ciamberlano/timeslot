package it.davidlab.timeslot.domain;


import it.davidlab.timeslot.domain.AssetType;
import it.davidlab.timeslot.domain.TsLocation;

import java.util.concurrent.TimeUnit;

public class AssetInfo {

    private long assetId;
    private String unitName;
    private String  assetName;
    private String url;
    private long amount;
    private long startValidity;
    private long endValidity;
    private long duration;
    private TimeUnit timeUnit;
    private String description;
    private TsLocation tsLocation;
    private long price;
    private AssetType type;


    public AssetInfo(long assetId, String unitName, String assetName, String url, long amount, long startValidity,
                     long endValidity, long duration, TimeUnit timeUnit, String description, TsLocation tsLocation,
                     long price, AssetType type) {
        this.assetId = assetId;
        this.unitName = unitName;
        this.assetName = assetName;
        this.url = url;
        this.amount = amount;
        this.startValidity = startValidity;
        this.endValidity = endValidity;
        this.duration = duration;
        this.timeUnit = timeUnit;
        this.description = description;
        this.tsLocation = tsLocation;
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

    public TimeUnit getTimeUnit() {
        return timeUnit;
    }

    public String getDescription() {
        return description;
    }

    public long getPrice() {
        return price;
    }

    public AssetType getType() {
        return type;
    }

    public TsLocation getTsLocation() {
        return tsLocation;
    }

}
