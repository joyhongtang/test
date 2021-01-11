package com.joyhong.test;

public class MessageEventTest {

    public static final String HUMAN_SENSOR_OFF = "humanSensor.off";
    public static final String HUMAN_SENSOR_ON = "humanSensor.on";

    public final String message;

    public MessageEventTest(String message) {
        this.message = message;
    }
}
