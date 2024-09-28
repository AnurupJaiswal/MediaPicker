package com.anurupjaiswal.ajmediapicker.basic.pagetransformer

import android.view.View
import androidx.viewpager2.widget.ViewPager2

class CarouselPageTransformer : ViewPager2.PageTransformer {
    override fun transformPage(page: View, position: Float) {
        val absPos = Math.abs(position)
        page.scaleY = if (absPos < 1) {
            1 - absPos * 0.3f
        } else {
            0.8f
        }

        page.alpha = if (absPos < 1) {
            1 - absPos
        } else {
            0f
        }

        page.translationX = -position * page.width
    }
}
