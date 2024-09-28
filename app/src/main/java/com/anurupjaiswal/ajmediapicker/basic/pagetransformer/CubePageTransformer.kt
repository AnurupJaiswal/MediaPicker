package com.anurupjaiswal.ajmediapicker.basic.pagetransformer

import android.view.View
import androidx.viewpager2.widget.ViewPager2

class CubePageTransformer : ViewPager2.PageTransformer {
    override fun transformPage(page: View, position: Float) {
        when {
            position < -1 -> { // This page is way off-screen to the left
                page.alpha = 0f
            }
            position <= 1 -> { // [-1,1]
                val scaleFactor = 0.75f + (1 - Math.abs(position)) * 0.25f
                page.scaleX = scaleFactor
                page.scaleY = scaleFactor
                page.alpha = 1 - Math.abs(position)

                val rotationY = 90f * position
                page.rotationY = rotationY
            }
            else -> { // This page is way off-screen to the right
                page.alpha = 0f
            }
        }
    }
}
