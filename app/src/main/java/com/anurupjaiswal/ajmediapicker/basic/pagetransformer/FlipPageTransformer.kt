package com.anurupjaiswal.ajmediapicker.basic.pagetransformer

import android.view.View
import androidx.viewpager2.widget.ViewPager2

class FlipPageTransformer : ViewPager2.PageTransformer {
    override fun transformPage(page: View, position: Float) {
        val absPos = Math.abs(position)

        // Rotate the page based on its position
        if (absPos >= 1) {
            page.alpha = 0f // Not visible
        } else {
            page.alpha = 1f // Fully visible
            page.rotationY = position * -30 // Rotate the page
            page.translationX = if (position < 0) {
                page.width * -absPos // Move to the left
            } else {
                page.width * absPos // Move to the right
            }
        }
    }
}
