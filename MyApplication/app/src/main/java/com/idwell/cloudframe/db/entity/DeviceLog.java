package com.idwell.cloudframe.db.entity;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class DeviceLog implements Parcelable {

    @NonNull
    @PrimaryKey
    private String filePath;

    @NonNull
    private String fileType;

    //上传次数
    private int uploadTimes = 0;

    public DeviceLog(@NonNull String filePath, @NonNull String fileType) {
        this.filePath = filePath;
        this.fileType = fileType;
    }

    @NonNull
    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(@NonNull String filePath) {
        this.filePath = filePath;
    }

    @NonNull
    public String getFileType() {
        return fileType;
    }

    public void setFileType(@NonNull String fileType) {
        this.fileType = fileType;
    }

    public int getUploadTimes() {
        return uploadTimes;
    }

    public void setUploadTimes(int uploadTimes) {
        this.uploadTimes = uploadTimes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DeviceLog deviceLog = (DeviceLog) o;

        if (!filePath.equals(deviceLog.filePath)) return false;
        return fileType.equals(deviceLog.fileType);
    }

    @Override
    public int hashCode() {
        int result = filePath.hashCode();
        result = 31 * result + fileType.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "DeviceLog{" +
                "filePath='" + filePath + '\'' +
                ", fileType='" + fileType + '\'' +
                ", uploadTimes=" + uploadTimes +
                '}';
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.filePath);
        dest.writeString(this.fileType);
        dest.writeInt(this.uploadTimes);
    }

    protected DeviceLog(Parcel in) {
        this.filePath = in.readString();
        this.fileType = in.readString();
        this.uploadTimes = in.readInt();
    }

    public static final Parcelable.Creator<DeviceLog> CREATOR = new Parcelable.Creator<DeviceLog>() {
        @Override
        public DeviceLog createFromParcel(Parcel source) {
            return new DeviceLog(source);
        }

        @Override
        public DeviceLog[] newArray(int size) {
            return new DeviceLog[size];
        }
    };
}
