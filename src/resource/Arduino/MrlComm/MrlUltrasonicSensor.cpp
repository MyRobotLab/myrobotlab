#include "Msg.h"
#include "Device.h"
#include "MrlUltrasonicSensor.h"

MrlUltrasonicSensor::MrlUltrasonicSensor(int deviceId) :
		Device(deviceId, DEVICE_TYPE_ULTRASONICSENSOR) {
	msg->publishDebug("ctor NewPing " + String(deviceId));
	lastDistance = 0;
}

MrlUltrasonicSensor::~MrlUltrasonicSensor() {
	delete newping;
}

void MrlUltrasonicSensor::attach(byte trigPin, byte echoPin) {
	msg->publishDebug("Ultrasonic.attach " + String(trigPin) + " " + String(echoPin));
	newping = new NewPing(trigPin, echoPin, 500);
}

void MrlUltrasonicSensor::startRanging() {
	msg->publishDebug("Ultrasonic.startRanging");
	// this should be public in NewPing
	// newping->set_max_distance(maxDistanceCm);
	isRanging = true;
}

void MrlUltrasonicSensor::stopRanging() {
	msg->publishDebug(F("Ultrasonic.stopRanging"));
	isRanging = false;
}

void MrlUltrasonicSensor::update() {
	if (!isRanging) {
		return;
	}
	unsigned long distance = newping->ping_cm();

	if (lastDistance != distance){
		msg->publishUltrasonicSensorData(id, distance);
	}

	lastDistance = distance;
}

