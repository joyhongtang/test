package com.joyhong.test;

public class MessageEventTest {

    public static final String HUMAN_SENSOR_OFF = "humanSensor.off";
    public static final String HUMAN_SENSOR_ON = "humanSensor.on";
    public static final String HUMAN_SENSOR_DETECT_SUCCESS = "humanSensor.success";
    public static final String HUMAN_SENSOR_DETECT = "humanSensor.decect";
    public final String message;

    public MessageEventTest(String message) {
        this.message = message;
    }
}
