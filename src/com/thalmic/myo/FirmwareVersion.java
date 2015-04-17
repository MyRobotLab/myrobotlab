package com.thalmic.myo;

public class FirmwareVersion {
    private final int firmwareVersionMajor;
    private final int firmwareVersionMinor;
    private final int firmwareVersionPath;
    private final int firmwareVersionHardwareRev;

    public FirmwareVersion(int firmwareVersionMajor, int firmwareVersionMinor, int firmwareVersionPath, int firmwareVersionHardwareRev) {
	super();
	this.firmwareVersionMajor = firmwareVersionMajor;
	this.firmwareVersionMinor = firmwareVersionMinor;
	this.firmwareVersionPath = firmwareVersionPath;
	this.firmwareVersionHardwareRev = firmwareVersionHardwareRev;
    }

    public int getFirmwareVersionMajor() {
	return firmwareVersionMajor;
    }

    public int getFirmwareVersionMinor() {
	return firmwareVersionMinor;
    }

    public int getFirmwareVersionPath() {
	return firmwareVersionPath;
    }

    public int getFirmwareVersionHardwareRev() {
	return firmwareVersionHardwareRev;
    }

    @Override
    public int hashCode() {
	final int prime = 31;
	int result = 1;
	result = prime * result + firmwareVersionHardwareRev;
	result = prime * result + firmwareVersionMajor;
	result = prime * result + firmwareVersionMinor;
	result = prime * result + firmwareVersionPath;
	return result;
    }

    @Override
    public boolean equals(Object obj) {
	if (this == obj) {
	    return true;
	}
	if (obj == null) {
	    return false;
	}
	if (getClass() != obj.getClass()) {
	    return false;
	}
	FirmwareVersion other = (FirmwareVersion) obj;
	if (firmwareVersionHardwareRev != other.firmwareVersionHardwareRev) {
	    return false;
	}
	if (firmwareVersionMajor != other.firmwareVersionMajor) {
	    return false;
	}
	if (firmwareVersionMinor != other.firmwareVersionMinor) {
	    return false;
	}
	if (firmwareVersionPath != other.firmwareVersionPath) {
	    return false;
	}
	return true;
    }

    @Override
    public String toString() {
	return "FirmwareVersion [firmwareVersionMajor=" + firmwareVersionMajor + ", firmwareVersionMinor=" + firmwareVersionMinor + ", firmwareVersionPath=" + firmwareVersionPath + ", firmwareVersionHardwareRev=" + firmwareVersionHardwareRev + "]";
    }
}