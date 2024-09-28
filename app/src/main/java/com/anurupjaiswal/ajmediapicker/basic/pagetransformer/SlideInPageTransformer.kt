package com.anurupjaiswal.ajmediapicker.basic.pagetransformer

import android.view.View
import androidx.viewpager2.widget.ViewPager2

class SlideInPageTransformer : ViewPager2.PageTransformer {
    override fun transformPage(page: View, position: Float) {
        when {
            position < -1 -> { // This page is way off-screen to the left
                page.alpha = 0f
            }
            position <= 1 -> { // [-1,1]
                page.alpha = 1 - Math.abs(position)
                page.translationX = page.width * -position
            }
            else -> { // This page is way off-screen to the right
                page.alpha = 0f
            }
        }
    }
}
