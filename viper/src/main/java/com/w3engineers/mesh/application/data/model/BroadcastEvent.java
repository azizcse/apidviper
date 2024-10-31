package com.w3engineers.mesh.application.data.model;

public class BroadcastEvent extends Event {
    private String broadcastId, metaData, contentPath, expiryTime;
    private double latitude, longitude, range;

    public String getBroadcastId() {
        return broadcastId;
    }

    public BroadcastEvent setBroadcastId(String broadcastId) {
        this.broadcastId = broadcastId;
        return this;
    }

    public String getMetaData() {
        return metaData;
    }

    public BroadcastEvent setMetaData(String metaData) {
        this.metaData = metaData;
        return this;
    }

    public String getContentPath() {
        return contentPath;
    }

    public BroadcastEvent setContentPath(String contentPath) {
        this.contentPath = contentPath;
        return this;
    }

    public double getLatitude() {
        return latitude;
    }

    public BroadcastEvent setLatitude(double latitude) {
        this.latitude = latitude;
        return this;
    }

    public double getLongitude() {
        return longitude;
    }

    public BroadcastEvent setLongitude(double longitude) {
        this.longitude = longitude;
        return this;
    }

    public double getRange() {
        return range;
    }

    public BroadcastEvent setRange(double range) {
        this.range = range;
        return this;
    }

    public String getExpiryTime() {
        return expiryTime;
    }

    public BroadcastEvent setExpiryTime(String expiryTime) {
        this.expiryTime = expiryTime;
        return this;
    }
}
