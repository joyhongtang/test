package com.joyhong.test.interfacedevice;

public class HeadSetDevice extends InterfaceDevice{
    @Override
    public void initData() {
        setJustTestHeadSet(true);
        super.initData();
    }
}
