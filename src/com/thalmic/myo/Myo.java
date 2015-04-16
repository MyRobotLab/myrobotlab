package com.thalmic.myo;

import com.thalmic.myo.enums.StreamEmgType;
import com.thalmic.myo.enums.UnlockType;
import com.thalmic.myo.enums.VibrationType;

public final class Myo {
	private long nativeHandle;

	private Myo() {
	}

	public void vibrate(VibrationType type) {
		vibrate(type.ordinal());
	}

	private native void vibrate(int type);

	public native void requestRssi();

	public void unlock(UnlockType unlockType) {
		unlock(unlockType.ordinal());
	}

	private native void unlock(int unlockType);

	public native void lock();

	public native void notifyUserAction();

	public void setStreamEmg(StreamEmgType streamEmgType) {
		setStreamEmg(streamEmgType.ordinal());
	}

	private native void setStreamEmg(int streamEmgType);
}