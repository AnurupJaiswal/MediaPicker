package com.anurupjaiswal.ajmediapicker.basic.pagetransformer

import android.view.View
import androidx.viewpager2.widget.ViewPager2

class ZoomOutPageTransformer : ViewPager2.PageTransformer {
    override fun transformPage(page: View, position: Float) {
        when {
            position < -1 -> { // [-Infinity,-1)
                page.alpha = 0f
            }
            position <= 1 -> { // [-1,1]
                val scaleFactor = Math.max(0.85f, 1 - Math.abs(position))
                val vertMargin = page.height * (1 - scaleFactor) / 2
                val horzMargin = page.width * (1 - scaleFactor) / 2

                page.translationX = if (position < 0) {
                    horzMargin - vertMargin / 2
                } else {
                    horzMargin + vertMargin / 2
                }

                page.scaleY = scaleFactor
                page.alpha = scaleFactor
            }
            else -> { // (1,+Infinity]
                page.alpha = 0f
            }
        }
    }
}
