package com.anurupjaiswal.ajmediapicker.basic.pagetransformer

import android.view.View
import androidx.viewpager2.widget.ViewPager2

class WavePageTransformer : ViewPager2.PageTransformer {
    override fun transformPage(page: View, position: Float) {
        val absPos = Math.abs(position)

        page.alpha = if (absPos >= 1) {
            0f // Not visible
        } else {
            1f // Fully visible
        }

        page.translationY = if (absPos < 1) {
            (page.height * 0.25f * Math.sin(Math.toRadians((absPos * 180).toDouble()))).toFloat()
        } else {
            0f
        }
    }
}
