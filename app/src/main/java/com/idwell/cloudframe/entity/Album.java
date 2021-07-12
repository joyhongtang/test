package com.idwell.cloudframe.entity;

/**
 * author : chason
 * mailbox : 156874547@qq.com
 * time : 2018/4/16 18:03
 * version : 1.0
 * describe :
 */

public class Album {
    private String bucket_id;
    private String bucket_display_name;
    private String filePath;
    private String count;
    private boolean selected;

    public Album(String bucket_id, String bucket_display_name, String filePath, String count, boolean selected) {
        this.bucket_id = bucket_id;
        this.bucket_display_name = bucket_display_name;
        this.filePath = filePath;
        this.count = count;
        this.selected = selected;
    }

    public String getBucket_id() {
        return bucket_id;
    }

    public void setBucket_id(String bucket_id) {
        this.bucket_id = bucket_id;
    }

    public String getBucket_display_name() {
        return bucket_display_name;
    }

    public void setBucket_display_name(String bucket_display_name) {
        this.bucket_display_name = bucket_display_name;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getCount() {
        return count;
    }

    public void setCount(String count) {
        this.count = count;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Album album = (Album) o;

        if (bucket_id != null ? !bucket_id.equals(album.bucket_id) : album.bucket_id != null)
            return false;
        return bucket_display_name != null ? bucket_display_name.equals(album.bucket_display_name) : album.bucket_display_name == null;
    }

    @Override
    public int hashCode() {
        int result = bucket_id != null ? bucket_id.hashCode() : 0;
        result = 31 * result + (bucket_display_name != null ? bucket_display_name.hashCode() : 0);
        return result;
    }
}
