package com.joyhong.test;

public class TestEntity {
    int row;
    int colume;
    String tag;

    public TestResultEnum getTestResultEnum() {
        return testResultEnum;
    }

    public void setTestResultEnum(TestResultEnum testResultEnum) {
        this.testResultEnum = testResultEnum;
    }

    TestResultEnum testResultEnum;

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    String content;
    TestEntity(int row, int colume, String tag,String content, TestResultEnum testResultEnum) {
        this.row = row;
        this.colume = colume;
        this.tag = tag;
        this.content = content;
        this.testResultEnum = testResultEnum;
    }

    public int getRow() {
        return row;
    }

    public void setRow(int row) {
        this.row = row;
    }

    public int getColume() {
        return colume;
    }

    public void setColume(int colume) {
        this.colume = colume;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }


}
