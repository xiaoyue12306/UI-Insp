package com.xiaoyue.uiinspector
import com.xiaoyue.uiinspector.color.*
import org.junit.Assert.*
import org.junit.Test

class ColorAnalyzerTest {
    @Test fun clusterRepresentativeIsRealModeNotAverageOrBucketEdge() {
        val result = ColorAnalyzer(ColorConfig(horizontalInset=0.0, verticalInset=0.0)).analyze(10,10,5,5) { x, _ ->
            if (x < 7) 0xffc7c6ca.toInt() else 0xffc1c2c9.toInt()
        }
        assertEquals("#C7C6CA", result.dominantHex); assertEquals(1.0, result.topColors.first().fraction, 0.0)
    }
    @Test fun centeredTextDoesNotReplaceMainSurface() {
        val result = ColorAnalyzer().analyze(200,96,100,48) { x,y ->
            if (x in 90..110 && y in 40..56) 0xff202124.toInt() else 0xffc7c6ca.toInt()
        }
        assertEquals("#202124", result.centerHex); assertEquals("#C7C6CA", result.dominantHex)
    }
    @Test fun translucentAntialiasingAndTransparentEdgeAreHandled() {
        val result = ColorAnalyzer(ColorConfig(horizontalInset=0.0, verticalInset=0.0)).analyze(10,10,5,5) { x,_ ->
            if (x < 2) 0x00123456 else 0xffc7c6ca.toInt()
        }
        assertEquals("#C7C6CA", result.dominantHex)
    }
}
