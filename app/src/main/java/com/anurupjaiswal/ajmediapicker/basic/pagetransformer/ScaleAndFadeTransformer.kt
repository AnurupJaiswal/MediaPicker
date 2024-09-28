package com.anurupjaiswal.ajmediapicker.basic.pagetransformer

import android.view.View
import androidx.viewpager2.widget.ViewPager2

class ScaleAndFadeTransformer : ViewPager2.PageTransformer {
    override fun transformPage(page: View, position: Float) {
        // Scale the page down (between 0.85f and 1.0f)
        val scaleFactor = 0.85f + (1 - Math.abs(position)) * 0.15f
        page.scaleX = scaleFactor
        page.scaleY = scaleFactor

        // Fade the page relative to its size
        page.alpha = 0.5f + (1 - Math.abs(position)) * 0.5f
    }
}