package com.thalmic.myo.example;

import java.util.ArrayList;
import java.util.List;

import com.thalmic.myo.AbstractDeviceListener;
import com.thalmic.myo.FirmwareVersion;
import com.thalmic.myo.Myo;
import com.thalmic.myo.Pose;

public class PrintMyoEvents extends AbstractDeviceListener {
    private final List<Myo> knownMyos = new ArrayList<>();

    @Override
    public void onPair(Myo myo, long timestamp, FirmwareVersion firmwareVersion) {
	knownMyos.add(myo);
    }

    @Override
    public void onPose(Myo myo, long timestamp, Pose pose) {
	System.out.println(String.format("Myo %s switched to pose %s.", identifyMyo(myo), pose.toString()));
    }

    @Override
    public void onConnect(Myo myo, long timestamp, FirmwareVersion firmwareVersion) {
	System.out.println(String.format("Myo %s has connected.", identifyMyo(myo)));
    }

    @Override
    public void onDisconnect(Myo myo, long timestamp) {
	System.out.println(String.format("Myo %s has disconnected.", identifyMyo(myo)));
    }

    private int identifyMyo(Myo myo) {
	return knownMyos.indexOf(myo);
    }
}