package com.github.carlosliszt.plantsiot.controller

import com.github.carlosliszt.plantsiot.mqtt.MqttManager

class ReadingController(private val mqtt: MqttManager) {
    fun connect(topic: String) = mqtt.connectAndSubscribe(topic)
    fun disconnect() = mqtt.disconnect()
}
