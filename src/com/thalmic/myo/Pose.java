package com.thalmic.myo;

import com.thalmic.myo.enums.PoseType;

public final class Pose {
    private final PoseType type;

    public Pose() {
	this(PoseType.REST);
    }

    public Pose(PoseType type) {
	this.type = type;
    }

    public PoseType getType() {
	return type;
    }

    @Override
    public int hashCode() {
	final int prime = 31;
	int result = 1;
	result = prime * result + ((type == null) ? 0 : type.hashCode());
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
	Pose other = (Pose) obj;
	if (type != other.type) {
	    return false;
	}
	return true;
    }

    @Override
    public String toString() {
	return "Pose [type=" + type + "]";
    }

}