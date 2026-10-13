package com.github.carlosliszt.plantsiot

import com.github.carlosliszt.plantsiot.domain.ReadingParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ReadingParserTest {
    private val parser = ReadingParser { 123L }

    @Test
    fun parsesTopicRgbAndCalculatesHealth() {
        val reading = parser.parse("plants/demo/rgb", "20,180,30")
        assertNotNull(reading)
        assertEquals(20, reading!!.red)
        assertEquals("Saudável", reading.healthStatus)
    }

    @Test
    fun parsesTopicNumericPayload() {
        val reading = parser.parse("plants/demo/soil/moisture", "42,5")
        assertNotNull(reading)
        assertEquals(42.5, reading!!.soilMoisture, 0.01)
    }

    @Test
    fun ignoresUnknownPayloads() {
        assertNull(parser.parse("plants/demo/unknown", "x"))
    }
}
