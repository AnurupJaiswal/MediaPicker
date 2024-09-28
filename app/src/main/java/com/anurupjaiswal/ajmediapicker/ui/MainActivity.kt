package com.anurupjaiswal.ajmediapicker.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.anurupjaiswal.ajmediapicker.basic.customgallery.MediaItem
import com.anurupjaiswal.ajmediapicker.basic.customgallery.MediaPagerAdapter
import com.anurupjaiswal.ajmediapicker.basic.customgallery.PickImageVideoActivity
import com.anurupjaiswal.ajmediapicker.databinding.ActivityMainBinding
import com.anurupjaiswal.ajmediapicker.basic.dotindicotor.InstaDotView


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewPager2: ViewPager2
    private lateinit var adapter: MediaPagerAdapter
    private val selectedMediaItems = ArrayList<MediaItem>()
    private lateinit var dotIndicator: InstaDotView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViewPager()
        setupDotIndicator()
        setupSelectionButton()
    }

    private fun setupViewPager() {
        viewPager2 = binding.viewPager2
        adapter = MediaPagerAdapter(selectedMediaItems, supportFragmentManager, lifecycle)
        viewPager2.adapter = adapter



           //viewPager2.setPageTransformer(CubePageTransformer()) // or CarouselPageTransformer(), etc.

        // Register a callback to update the dot indicator when the page changes
        viewPager2.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                if (selectedMediaItems.size > 1) {
                    dotIndicator.onPageChange(position)
                }
            }
        })
    }

    private fun setupDotIndicator() {
        dotIndicator = binding.dotIndicator // Layout for dots
        if (selectedMediaItems.size > 1) {
            dotIndicator.setNoOfPages(adapter.itemCount)
        }
    }

    private fun setupSelectionButton() {
        binding.mcvSelectionItem.setOnClickListener {
            val intent = Intent(this, PickImageVideoActivity::class.java).apply {
                putParcelableArrayListExtra("SELECTED_ITEMS", selectedMediaItems)
            }
            launcher.launch(intent)
        }
    }

    private val launcher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val updatedSelectedItems: ArrayList<MediaItem>? = result.data?.getParcelableArrayListExtra("SELECTED_ITEMS")
            updatedSelectedItems?.let { items ->
                updateSelectedMediaItems(items)
            }
        }
    }

    private fun updateSelectedMediaItems(items: ArrayList<MediaItem>) {
        selectedMediaItems.clear()
        selectedMediaItems.addAll(items)

        Log.e("MainActivity", "Updated Selected Items: $selectedMediaItems")

        // Update the adapter with the new selected media items
        adapter = MediaPagerAdapter(selectedMediaItems, supportFragmentManager, lifecycle)
        viewPager2.adapter = adapter

        if (selectedMediaItems.size > 1) {
            dotIndicator.setNoOfPages(selectedMediaItems.size)
        }else{
             binding.dotIndicator.visibility = View.GONE
        }

        // Reset current item to the first page if desired
        viewPager2.currentItem = 0
    }

}
