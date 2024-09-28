package com.anurupjaiswal.ajmediapicker.basic.customgallery

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter

/**
 * Adapter for managing and displaying media fragments in a ViewPager2.
 *
 * @param mediaItems The list of media items to be displayed.
 * @param fragmentManager The FragmentManager used to manage fragments.
 * @param lifecycle The Lifecycle of the parent component.
 */
class MediaPagerAdapter(
    private val mediaItems: List<MediaItem>,
    fragmentManager: FragmentManager,
    lifecycle: Lifecycle
) : FragmentStateAdapter(fragmentManager, lifecycle) {

    private val selectedItems = mutableSetOf<MediaItem>()

    /**
     * Returns the total number of items in the adapter.
     *
     * @return The number of media items.
     */
    override fun getItemCount(): Int = mediaItems.size

    /**
     * Creates a new fragment for the specified position.
     *
     * @param position The position of the item within the adapter's data set.
     * @return A Fragment corresponding to the item at the specified position.
     */
    override fun createFragment(position: Int): Fragment {
        return MediaFragment(mediaItems[position])
    }


}
