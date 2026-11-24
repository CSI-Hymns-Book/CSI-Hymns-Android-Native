package com.reyzie.hymns.carols.ui.detail

import org.junit.Assert.assertEquals
import org.junit.Test

class CarolPresenterTapTest {

    @Test
    fun `a tap on the left half goes to the previous verse`() {
        assertEquals(-1, presenterStepFor(tapX = 10f, width = 400f))
        assertEquals(-1, presenterStepFor(tapX = 199.9f, width = 400f))
    }

    @Test
    fun `a tap on the right half or the middle goes to the next verse`() {
        assertEquals(1, presenterStepFor(tapX = 200f, width = 400f))
        assertEquals(1, presenterStepFor(tapX = 390f, width = 400f))
    }
}
