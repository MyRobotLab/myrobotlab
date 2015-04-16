package com.thalmic.myo.example;

import com.thalmic.myo.DeviceListener;
import com.thalmic.myo.Hub;

public class MultipleMyos {
	public static void main(String... args) {
		try {
			Hub hub = new Hub("com.example.multiple-myos");

			DeviceListener printer = new PrintMyoEvents();
			hub.addListener(printer);

			while (true) {
				hub.run(10);
			}
		} catch (Exception e) {
			System.err.println("Error: ");
			e.printStackTrace();
			System.exit(1);
		}
	}
}