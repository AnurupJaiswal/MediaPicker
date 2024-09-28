package com.anurupjaiswal.ajmediapicker.basic.customgallery

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.anurupjaiswal.ajmediapicker.databinding.ItemMediaBinding

/**
 * RecyclerView adapter for displaying and handling media items (images and videos).
 * @param context The context.
 * @param mediaList The list of media items to display.
 * @param onItemSelected Callback when an item is selected.
 * @param onSelectionChanged Callback when the selection count changes.
 */


class MediaAdapter(
    private val context: Context,
    private var mediaList: List<MediaItem>,
    private val onItemSelected: (MediaItem) -> Unit,
    private val onSelectionChanged: (Int) -> Unit
) : RecyclerView.Adapter<MediaAdapter.MediaViewHolder>() {

    private val selectedItems = mutableSetOf<Uri>()
    private val thumbnailCache = mutableMapOf<Uri, Bitmap?>()
    private val videoDurationCache = mutableMapOf<Uri, String>()
    private var isMultiSelectMode = false
    private var maxSelection = 1

    /**
     * Creates and returns a ViewHolder for media items.
     * @param parent The parent ViewGroup.
     * @param viewType The type of view.
     */

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MediaViewHolder {
        val binding = ItemMediaBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MediaViewHolder(binding)
    }


    /**
     * Binds the media item to the ViewHolder.
     * @param holder The ViewHolder for the media item.
     * @param position The position of the media item in the list.
     */

    override fun onBindViewHolder(holder: MediaViewHolder, position: Int) {
        val mediaItem = mediaList[position]
        holder.bind(mediaItem)

        val isItemSelectable = selectedItems.size < 10 || selectedItems.contains(mediaItem.uri)

        holder.itemView.isClickable = isItemSelectable
        holder.itemView.isEnabled = isItemSelectable

        // Optionally, adjust the appearance of non-selectable items.
        holder.itemView.alpha = if (isItemSelectable) 1.0f else 0.5f
    }

    /**
     * Returns the total number of media items in the list.
     * @return The size of the media list.
     */

    override fun getItemCount() = mediaList.size


    /**
     * Retrieves the media item at the given position.
     * @param position The position of the media item.
     * @return The media item, or null if the position is invalid.
     */

    fun getMediaItemAt(position: Int): MediaItem? {
        return if (position in mediaList.indices) mediaList[position] else null
    }

    /**
     * Clears the list of media items and notifies the adapter of data changes.
     */

    fun clearMediaItems() {
        mediaList = emptyList()
        notifyDataSetChanged()
    }

    /**
     * Updates the media list with new items and notifies the adapter.
     * @param newMediaList The new list of media items.
     */
    fun updateMediaItems(newMediaList: List<MediaItem>) {
        mediaList = newMediaList
        notifyDataSetChanged()
    }


    /**
     * ViewHolder class for media items.
     * @param binding The view binding for the media item layout.
     */

    inner class MediaViewHolder(private val binding: ItemMediaBinding) :
        RecyclerView.ViewHolder(binding.root) {

        /**
         * Binds a media item to the ViewHolder and sets up click listeners.
         * @param mediaItem The media item to bind.
         */

/*
        fun bind(mediaItem: MediaItem) {
            if (mediaItem.type == MediaType.VIDEO) {
                binding.videoIcon.visibility = View.VISIBLE
                binding.videoDuration.visibility = View.VISIBLE
                mediaItem.uri?.let { loadVideoThumbnail(it) }
                mediaItem.uri?.let { loadVideoDuration(it) }
            } else {
                Glide.with(context)
                    .load(mediaItem.uri)
                    .override(300, 300)
                    .into(binding.mediaImage)
                binding.videoIcon.visibility = View.GONE
                binding.videoDuration.visibility = View.GONE
            }

            // Determine if the item should be selectable
            val isItemSelectable =
                isMultiSelectMode || selectedItems.isEmpty() || selectedItems.contains(mediaItem.uri)

            binding.checkIcon.isClickable = isItemSelectable
            binding.checkIcon.isEnabled = isItemSelectable
            binding.checkIcon.alpha = if (isItemSelectable) 1.0f else 0.5f

            // Update the checkbox state based on the current selection
            updateSelectionState(mediaItem.uri)

            // Handle checkbox click
            binding.checkIcon.setOnClickListener {
                if (isItemSelectable) {
                    mediaItem.uri?.let { it1 -> toggleSelection(it1) }
                }
            }

            binding.root.setOnClickListener {
                if (isItemSelectable) {
                    mediaItem.uri?.let { it1 -> toggleSelection(it1) }
                    onItemSelected(mediaItem)
                }
            }
        }
*/



        fun bind(mediaItem: MediaItem) {
            if (mediaItem.type == MediaType.VIDEO) {
                binding.videoIcon.visibility = View.VISIBLE
                binding.videoDuration.visibility = View.VISIBLE
                mediaItem.uri?.let { loadVideoThumbnail(it) }
                mediaItem.uri?.let { loadVideoDuration(it) }
            } else {
                Glide.with(context)
                    .load(mediaItem.uri)
                    .override(300, 300)
                    .into(binding.mediaImage)
                binding.videoIcon.visibility = View.GONE
                binding.videoDuration.visibility = View.GONE
            }

            // Determine if the item should be selectable
            val isItemSelectable = isMultiSelectMode || selectedItems.isEmpty() || selectedItems.contains(mediaItem.uri)

            // Update the checkbox state based on the current selection and max selection limit
            binding.checkIcon.isEnabled = selectedItems.size < maxSelection || selectedItems.contains(mediaItem.uri)
            binding.checkIcon.alpha = if (binding.checkIcon.isEnabled) 1.0f else 0.5f // Visually disable the checkbox if not enabled

            // Update the checkbox state based on the current selection
            updateSelectionState(mediaItem.uri)

            // Handle checkbox click
            binding.checkIcon.setOnClickListener {
                if (binding.checkIcon.isEnabled) {
                    mediaItem.uri?.let { it1 -> toggleSelection(it1) }
                } else {
                    Toast.makeText(context, "You can only select up to $maxSelection items.", Toast.LENGTH_SHORT).show()
                }
            }

            binding.root.setOnClickListener {
                if (binding.checkIcon.isEnabled) {
                    mediaItem.uri?.let { it1 -> toggleSelection(it1) }
                    onItemSelected(mediaItem)
                } else {
                    Toast.makeText(context, "You can only select up to $maxSelection items.", Toast.LENGTH_SHORT).show()
                }
            }
        }



        /**
         * Updates the UI based on the selection state of the item.
         * @param uri The URI of the media item.
         */

        private fun updateSelectionState(uri: Uri?) {
            if (uri == null) return

            val isSelected = selectedItems.contains(uri)
            binding.mediaOverlay.visibility = if (isSelected) View.VISIBLE else View.GONE
            binding.checkIcon.isChecked = isSelected
        }


        /**
         * Loads a video thumbnail asynchronously and caches it.
         * @param uri The URI of the video.
         */

        private fun loadVideoThumbnail(uri: Uri) {
            if (thumbnailCache.containsKey(uri)) {
                binding.mediaImage.setImageBitmap(thumbnailCache[uri])
            } else {
                CoroutineScope(Dispatchers.IO).launch {
                    val bitmap = CustomGalleryUtils.createVideoThumbnail(context, uri)
                    thumbnailCache[uri] = bitmap

                    withContext(Dispatchers.Main) {
                        binding.mediaImage.setImageBitmap(bitmap)
                    }
                }
            }
        }

        /**
         * Loads and displays the duration of a video.
         * @param uri The URI of the video.
         */

        private fun loadVideoDuration(uri: Uri) {
            if (videoDurationCache.containsKey(uri)) {
                binding.videoDuration.text = videoDurationCache[uri]
            } else {
                getVideoDurationAsync(uri) { duration ->
                    videoDurationCache[uri] = duration
                    binding.videoDuration.text = duration
                }
            }
        }

        /**
         * Retrieves the duration of a video asynchronously.
         * @param uri The URI of the video.
         * @param callback The callback to return the duration.
         */

        private fun getVideoDurationAsync(uri: Uri, callback: (String) -> Unit) {
            CoroutineScope(Dispatchers.IO).launch {
                val duration = CustomGalleryUtils.getVideoDuration(context, uri)
                withContext(Dispatchers.Main) {
                    callback(duration)
                }
            }
        }

        /**
         * Toggles the selection state of a media item.
         * @param uri The URI of the media item.
         */

        private fun toggleSelection(uri: Uri) {
            if (isMultiSelectMode) {
                // Multi-selection mode logic
                if (selectedItems.contains(uri)) {
                    // Remove the item if it's already selected
                    selectedItems.remove(uri)
                } else {
                    // Add the item if the number of selected items is less than the max selection limit
                    if (selectedItems.size < maxSelection) {
                        selectedItems.add(uri)
                    } else {

                           Toast.makeText(context,"You can only select up to $maxSelection items.",Toast.LENGTH_SHORT).show()


                        return
                    }
                }
            } else {
                // Single-selection mode logic
                if (selectedItems.contains(uri)) {
                    // Deselect the item if it's already selected
                    selectedItems.remove(uri)
                } else {
                    // Check if there's already a selected item
                    if (selectedItems.isNotEmpty()) {


                        Toast.makeText(context,"Only $maxSelection item can be selected at a time.",Toast.LENGTH_SHORT).show()



                        return
                    }
                    // Clear previous selections and select the new item
                    selectedItems.clear()
                    selectedItems.add(uri)
                }
            }
            onSelectionChanged(selectedItems.size)
            notifyDataSetChanged()
        }
    }


    /**
     * Retrieves the currently selected media items.
     * @return A list of selected media items.
     */


    fun getSelectedItems(): List<MediaItem> {
        return mediaList.filter { selectedItems.contains(it.uri) }
    }


    /**
     * Sets the selection mode (single or multi-select) and the max selection limit.
     * @param isMultiSelectMode Whether multi-select mode is enabled.
     * @param maxSelection The maximum number of selectable items.
     */

    fun setSelectionMode(isMultiSelectMode: Boolean, maxSelection: Int) {
        if (this.isMultiSelectMode != isMultiSelectMode) {
            selectedItems.clear()
            onSelectionChanged(0)
        }
        this.isMultiSelectMode = isMultiSelectMode
        this.maxSelection = maxSelection
        notifyDataSetChanged()
    }

    /**
     * Sets theSelectedItems.
     * clear the existing  list and add the new list.
     */

    fun setSelectedItems(items: List<MediaItem>) {
        selectedItems.clear() // Clear existing selections
        // Add the selected items URIs to the selected items set
        selectedItems.addAll(items.mapNotNull { it.uri })
        // Notify the adapter to refresh the views
        notifyDataSetChanged()
        // Notify about the selection count change
        onSelectionChanged(selectedItems.size)
    }
}