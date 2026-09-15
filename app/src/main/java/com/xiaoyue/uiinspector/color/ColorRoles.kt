package com.xiaoyue.uiinspector.color

import kotlin.math.pow

/** Screenshot estimates, not View background/textColor properties. */
data class ColorRoles(val background: Int?, val text: Int?) {
    companion object {
        fun estimate(colors: ColorResult, hasText: Boolean): ColorRoles {
            val dominant = colors.dominantColor ?: return ColorRoles(null,null)
            val share = colors.topColors.firstOrNull { it.color==dominant }?.fraction ?: 0.0
            if(share < .70) return ColorRoles(null,null)
            // A center pixel often hits antialiased glyph edges. Prefer a repeated cluster.
            val text = if(!hasText) null else colors.topColors
                .filter { it.color!=dominant && it.fraction in .001.. .25 && contrast(it.color,dominant)>=3.0 }
                .maxByOrNull { it.fraction }?.color
            return ColorRoles(dominant,text)
        }
        private fun luminance(color: Int): Double {
            fun channel(shift: Int): Double {
                val c=((color ushr shift) and 255)/255.0
                return if(c<=.04045) c/12.92 else ((c+.055)/1.055).pow(2.4)
            }
            return .2126*channel(16)+.7152*channel(8)+.0722*channel(0)
        }
        private fun contrast(a: Int,b: Int): Double {
            val x=luminance(a); val y=luminance(b)
            return (maxOf(x,y)+.05)/(minOf(x,y)+.05)
        }
    }
}
