package com.w3engineers.models;

import android.os.Parcel;
import android.os.Parcelable;

public class ContentMetaInfo implements Parcelable {
    private String i, mi;
    private byte t;
    private int mt;
    private boolean ic;

    public ContentMetaInfo() {

    }

    protected ContentMetaInfo(Parcel in) {
        i = in.readString();
        t = in.readByte();
        mt = in.readInt();
        ic = in.readByte() != 0;
        mi = in.readString();
    }

    public static final Creator<ContentMetaInfo> CREATOR = new Creator<ContentMetaInfo>() {
        @Override
        public ContentMetaInfo createFromParcel(Parcel in) {
            return new ContentMetaInfo(in);
        }

        @Override
        public ContentMetaInfo[] newArray(int size) {
            return new ContentMetaInfo[size];
        }
    };

    public String getMessageId() {
        return i;
    }

    public ContentMetaInfo setMessageId(String i) {
        this.i = i;
        return this;
    }

    public byte getContentType() {
        return t;
    }

    public ContentMetaInfo setContentType(byte t) {
        this.t = t;
        return this;
    }

    public int getMessageType() {
        return mt;
    }

    public ContentMetaInfo setMessageType(int mt) {
        this.mt = mt;
        return this;
    }

    public boolean getIsContent() {
        return ic;
    }

    public ContentMetaInfo setIsContent(boolean isContent) {
        this.ic = isContent;
        return this;
    }

    public String getMetaInfo() {
        return mi;
    }

    public ContentMetaInfo setMetaInfo(String metaInfo) {
        this.mi = metaInfo;
        return this;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(i);
        dest.writeByte(t);
        dest.writeInt(mt);
        dest.writeByte((byte) (ic ? 1 : 0));
        dest.writeString(mi);
    }
}
