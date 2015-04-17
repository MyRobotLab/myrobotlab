package com.thalmic.myo.example;

import java.util.Arrays;

import com.thalmic.myo.AbstractDeviceListener;
import com.thalmic.myo.FirmwareVersion;
import com.thalmic.myo.Myo;

public class EmgDataCollector extends AbstractDeviceListener {
	private byte[] emgSamples;

	public EmgDataCollector() {
	}

	@Override
	public void onPair(Myo myo, long timestamp, FirmwareVersion firmwareVersion) {
		if (emgSamples != null) {
			for (int i = 0; i < emgSamples.length; i++) {
				emgSamples[i] = 0;
			}
		}
	}

	@Override
	public void onEmgData(Myo myo, long timestamp, byte[] emg) {
		this.emgSamples = emg;
	}

	@Override
	public String toString() {
		return Arrays.toString(emgSamples);
	}
}
