package com.joyhong.test.control;

public class TestRecord {
    int pos;
    boolean success;
    public TestRecord(int pos){
        this.pos = pos;
    }
    public int getPos() {
        return pos;
    }

    public void setPos(int pos) {
        this.pos = pos;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }
}
