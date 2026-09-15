package com.xiaoyue.uiinspector
import com.xiaoyue.uiinspector.color.*
import org.junit.Assert.*
import org.junit.Test

class ColorRolesTest {
    private val bg=0xffc7c6ca.toInt()
    private val ink=0xff202124.toInt()
    private fun sample(fraction: Double=.98)=ColorResult(0xff8b8a8e.toInt(),bg,listOf(ColorShare(bg,fraction),ColorShare(ink,.006)))
    @Test fun flatTextButtonUsesRepeatedInkInsteadOfCenterPixel() {
        assertEquals(ColorRoles(bg,ink),ColorRoles.estimate(sample(),true))
    }
    @Test fun iconWithoutTextDoesNotInventTextColor() {
        assertEquals(ColorRoles(bg,null),ColorRoles.estimate(sample(),false))
    }
    @Test fun mixedBackgroundDoesNotAssignRoles() {
        assertEquals(ColorRoles(null,null),ColorRoles.estimate(sample(.4),true))
    }
    @Test fun lowContrastAndRareNoiseAreNotTextCandidates() {
        assertNull(ColorRoles.estimate(ColorResult(bg,bg,listOf(ColorShare(bg,.98),ColorShare(0xffbfc0c1.toInt(),.02))),true).text)
        assertNull(ColorRoles.estimate(ColorResult(bg,bg,listOf(ColorShare(bg,.9999),ColorShare(ink,.0001))),true).text)
    }
}
