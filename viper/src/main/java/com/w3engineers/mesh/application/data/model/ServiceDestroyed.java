package com.w3engineers.mesh.application.data.model;

import android.os.Parcel;
import android.os.Parcelable;

public class ServiceDestroyed extends Event implements Parcelable {
    public boolean isFullDestroyed;

    public ServiceDestroyed(){}

    protected ServiceDestroyed(Parcel in) {
        isFullDestroyed = in.readByte() == 1;
    }

    public static final Creator<ServiceDestroyed> CREATOR = new Creator<ServiceDestroyed>() {
        @Override
        public ServiceDestroyed createFromParcel(Parcel in) {
            return new ServiceDestroyed(in);
        }

        @Override
        public ServiceDestroyed[] newArray(int size) {
            return new ServiceDestroyed[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeByte((byte) (isFullDestroyed ? 1 : 0));
    }
}
