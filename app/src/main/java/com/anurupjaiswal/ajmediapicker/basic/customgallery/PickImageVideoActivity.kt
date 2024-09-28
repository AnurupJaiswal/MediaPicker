package com.anurupjaiswal.ajmediapicker.basic.customgallery


/**
 * PickImageVideoActivity.kt
 *
 * Created by: Anurup Jaiswal
 * Created on: 28th August 2024
 * Purpose: This activity handles the functionality for picking images and videos
 * Last modified: 28th September 2024
 */

import android.Manifest
import android.app.Activity
import android.content.ContentResolver
import android.content.ContentValues.TAG
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.anurupjaiswal.ajmediapicker.R
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.anurupjaiswal.ajmediapicker.databinding.ActivityPickImageVideoBinding
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PickImageVideoActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPickImageVideoBinding
    private lateinit var mediaAdapter: MediaAdapter
    private lateinit var folderAdapter: FolderAdapter
    private var folderList = mutableListOf<FolderItem>()
    private var isMultiSelectMode = false
    private var maxSelection: Int = 1 // Default to single selection
    private var selectedFolderId: String? = "all_media"
    private var selectedFolderName: String = "All Media"


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Inflate the layout using View Binding
        binding = ActivityPickImageVideoBinding.inflate(layoutInflater)
        setContentView(binding.root)


        // Initialize the adapter and set click listeners
        binding.mediaRecyclerView.layoutManager = GridLayoutManager(this, 3)
        mediaAdapter = MediaAdapter(this, emptyList(), { mediaItem ->
            // Handle media item click
        }, { selectedCount ->
            // Update the done button and selected count when the selection changes
            updateDoneButton(selectedCount)
        })
        binding.mediaRecyclerView.adapter = mediaAdapter

// Initially set the mode to multi-select
        isMultiSelectMode = true
        updateSelectionMode()

// Toggle between single and multi-select mode
        binding.mcvSelectMultipleSingle.setOnClickListener {
            isMultiSelectMode = !isMultiSelectMode
            updateSelectionMode()
        }


        val receivedItems: ArrayList<MediaItem> =
            intent.getParcelableArrayListExtra("SELECTED_ITEMS") ?: arrayListOf()

// Log or handle the received data
        Log.e("Data: ", "$receivedItems")

// Set the received items in your adapter or use them as required
        mediaAdapter.setSelectedItems(receivedItems)



        binding.mcvDone.setOnClickListener {
            // Get selected items from the adapter
            val selectedItems = mediaAdapter.getSelectedItems()
            val selectedItemsArrayList = ArrayList(selectedItems)

            // Log the selected items for debugging
            Log.e(TAG, "onCreate: $selectedItemsArrayList")

            // Create an intent and put the selected items
            val resultIntent = Intent().apply {
                putParcelableArrayListExtra("SELECTED_ITEMS", selectedItemsArrayList)
            }

            // Set the result and finish the activity
            setResult(Activity.RESULT_OK, resultIntent)
            finish()
        }

        if (isReadStoragePermissionGranted()) {
            GlobalScope.launch(Dispatchers.Main) {
                folderList = loadFolders().toMutableList()
                loadMedia("all_media")
            }
        } else {
            requestAppropriatePermissions()
        }

        binding.selectedFolderName.setOnClickListener {
            showFolderSelectionDialog()
        }


        binding.selectedFolderName.text = selectedFolderName



        binding.mediaRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                // Get the first visible item position
                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()


                if (firstVisibleItemPosition != RecyclerView.NO_POSITION) {
                    // Get the media item from the adapter based on the position
                    val mediaItem = mediaAdapter.getMediaItemAt(firstVisibleItemPosition)

                    // Update the date TextView based on the media item
                    mediaItem?.let {
                        it.uri?.let { uri ->
                            // Pass the contentResolver along with the uri
                            updateDateTextView(contentResolver, uri)
                        }
                    }
                }

            }
        })


    }

    /**
     * Loads a list of folders from the device's media store.
     *
     * This function queries the media store for folders and media items. It first retrieves and creates special folders
     * for "AllMedia" and "AllVideos" with corresponding media counts and first media thumbnails. Then, it queries the media
     * store to get individual folders based on bucket IDs and names, and collects their media counts.
     *
     * @return A list of FolderItem objects representing the folders and their media counts.
     */
    private suspend fun loadFolders(): List<FolderItem> = withContext(Dispatchers.IO) {
        val folderList = mutableListOf<FolderItem>()
        val allMediaUri = getFirstMediaUriForAllMedia()
        val allVideosUri = getFirstMediaUriForAllVideos()
        val allMediaFolder = allMediaUri?.let {
            FolderItem(
                folderName = "AllMedia",
                folderId = "all_media",
                folderPath = "",
                folderUri = it,
                mediaCount = getAllMediaCount()
            )
        }
        if (allMediaFolder != null) {
            folderList.add(allMediaFolder)
        }
        val allVideosFolder = allVideosUri?.let {
            FolderItem(
                folderName = "AllVideos",
                folderId = "all_videos",
                folderPath = "",
                folderUri = it,
                mediaCount = getAllVideosCount()
            )
        }
        if (allVideosFolder != null) {
            folderList.add(allVideosFolder)
        }
        // Query for individual folders
        val projection = arrayOf(
            MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME,
            MediaStore.Files.FileColumns.BUCKET_ID,
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.MEDIA_TYPE
        )
        val selection =
            "${MediaStore.Files.FileColumns.MEDIA_TYPE}=? OR ${MediaStore.Files.FileColumns.MEDIA_TYPE}=?"
        val selectionArgs = arrayOf(
            MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString(),
            MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString()
        )
        val sortOrder =
            "${MediaStore.Files.FileColumns.BUCKET_ID} ASC, ${MediaStore.Files.FileColumns.DATE_ADDED} DESC"
        val queryUri = MediaStore.Files.getContentUri("external")

        val cursor =
            contentResolver.query(queryUri, projection, selection, selectionArgs, sortOrder)

        cursor?.use {
            val bucketIdColumn = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.BUCKET_ID)
            val bucketNameColumn =
                it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.BUCKET_DISPLAY_NAME)
            val idColumn = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
            val mediaTypeColumn = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MEDIA_TYPE)

            val folderMap = mutableMapOf<String, FolderItem>()

            while (it.moveToNext()) {
                val bucketId = it.getString(bucketIdColumn)
                val bucketName = it.getString(bucketNameColumn)
                val id = it.getLong(idColumn)
                val mediaType = it.getInt(mediaTypeColumn)

                if (!folderMap.containsKey(bucketId)) {
                    val contentUri = when (mediaType) {
                        MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE ->
                            Uri.withAppendedPath(
                                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                                id.toString()
                            )

                        MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO ->
                            Uri.withAppendedPath(
                                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                                id.toString()
                            )

                        else -> Uri.parse("")
                    }
                    folderMap[bucketId] = FolderItem(bucketName, bucketId, bucketId, contentUri)
                }
            }

            folderList.addAll(folderMap.values)

            for (folderItem in folderList) {
                if (folderItem.folderId != "all_media" && folderItem.folderId != "all_videos") {
                    folderItem.mediaCount = getMediaCount(folderItem.folderId)
                }
            }
        }

        return@withContext folderList
    }


    /**
     * Displays a bottom sheet dialog allowing users to select a folder from a list.
     *
     * This function initializes a bottom sheet dialog that displays a list of folders. It sets up a RecyclerView
     * with a folder adapter to show the list of available folders. When a folder is selected, the dialog is dismissed,
     * and the selected folder's ID and name are updated in the UI. It also triggers a coroutine to load media items
     * from the selected folder.
     */

    private fun showFolderSelectionDialog() {
        val dialog = BottomSheetDialog(this)
        val view = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_folders, null)
        dialog.setContentView(view)

        val folderRecyclerView: RecyclerView = view.findViewById(R.id.folder_recycler_view)
        folderRecyclerView.layoutManager = LinearLayoutManager(this)

        folderAdapter = FolderAdapter(folderList, selectedFolderId) { folderItem ->
            dialog.dismiss()
            selectedFolderId = folderItem.folderId
            selectedFolderName = folderItem.folderName

            binding.selectedFolderName.text = selectedFolderName // Update UI

            GlobalScope.launch(Dispatchers.Main) {
                loadMedia(folderItem.folderId)  // Load media from the selected folder
            }
        }
        folderRecyclerView.adapter = folderAdapter

        dialog.show()
    }


    /**
     * Loads media files for a specified folder and updates the media adapter with the loaded items.
     *
     * This function first clears any existing media items from the adapter. It then loads media items based on
     * the provided folder ID. If the folder ID is "all_media", it loads all media (images and videos). If the folder ID
     * is "all_videos", it loads only videos. For any other folder ID, it loads media items specific to that folder.
     * Finally, it updates the media adapter with the newly loaded media items and logs the count of the loaded items.
     *
     * @param folderId The ID of the folder for which media items are to be loaded. Special IDs are:
     *                 - "all_media" for all media (images and videos)
     *                 - "all_videos" for all videos
     *                 - Any other ID to load media items specific to that folder.
     */


    private suspend fun loadMedia(folderId: String) {
        // Clear existing media items in the adapter to prepare for new data
        mediaAdapter.clearMediaItems()

        // Load media items based on the provided folder ID
        val mediaList = when (folderId) {
            "all_media" -> loadAllMedia() // Load all media (images and videos) for "AllMedia" folder
            "all_videos" -> loadVideos() // Load only videos for "AllVideos" folder
            else -> loadMediaFiles(folderId) // Load media specific to the given folder ID
        }

        // Log the number of loaded media items for debugging purposes
        Log.d("MediaLoading", "Loaded media items: ${mediaList.size}")

        // Update the media adapter with the newly loaded media items
        mediaAdapter.updateMediaItems(mediaList)
    }


    /**
     * Loads all media files (images and videos) from the device's media store.
     *
     * This function queries the media store to retrieve all media files, including images and videos.
     * It constructs `MediaItem` objects for each media file found and returns a list of these objects.
     *
     * @return A list of `MediaItem` objects representing all media files (images and videos) on the device.
     */

    private suspend fun loadAllMedia(): List<MediaItem> = withContext(Dispatchers.IO) {
        // Create a mutable list to store media items
        val mediaList = mutableListOf<MediaItem>()

        // Define the projection to retrieve the necessary columns
        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.MEDIA_TYPE,
            MediaStore.Files.FileColumns.MIME_TYPE
        )

        // Define the selection criteria to filter by media types (images or videos)
        val selection =
            "${MediaStore.Files.FileColumns.MEDIA_TYPE}=? OR ${MediaStore.Files.FileColumns.MEDIA_TYPE}=?"
        val selectionArgs = arrayOf(
            MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString(),
            MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString()
        )

        // Define the sort order to sort by the date added in descending order
        val sortOrder = "${MediaStore.Files.FileColumns.DATE_ADDED} DESC"

        // Define the URI for querying the external media content
        val queryUri = MediaStore.Files.getContentUri("external")

        // Perform the query to get the media files
        val cursor =
            contentResolver.query(queryUri, projection, selection, selectionArgs, sortOrder)

        cursor?.use {
            // Get the column indices for ID and MIME type
            val idColumn = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
            val mimeTypeColumn = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE)

            // Iterate through the cursor to retrieve media items
            while (it.moveToNext()) {
                val id = it.getLong(idColumn)
                val mimeType = it.getString(mimeTypeColumn)

                // Construct the content URI based on MIME type
                val contentUri = when {
                    mimeType.startsWith("image") -> Uri.withAppendedPath(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        id.toString()
                    )

                    mimeType.startsWith("video") -> Uri.withAppendedPath(
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                        id.toString()
                    )

                    else -> continue
                }

                // Determine the media type
                val mediaType =
                    if (mimeType.startsWith("image")) MediaType.IMAGE else MediaType.VIDEO

                // Add the media item to the list
                mediaList.add(MediaItem(contentUri, mediaType))
            }
        }

        // Return the list of media items
        return@withContext mediaList
    }


    /**
     * Loads media files (images and videos) from a specific folder in the device's media store.
     *
     * This function queries the media store to retrieve media files that belong to the specified folder.
     * It filters by the folder ID and media type (image or video), and then constructs `MediaItem` objects
     * for each media file found.
     *
     * @param folderId The ID of the folder from which to load media files.
     * @return A list of `MediaItem` objects representing the media files in the specified folder.
     */

    private suspend fun loadMediaFiles(folderId: String): List<MediaItem> =
        withContext(Dispatchers.IO) {
            val mediaList = mutableListOf<MediaItem>()

            // Define the projection to retrieve the necessary columns
            val projection = arrayOf(
                MediaStore.Files.FileColumns._ID,
                MediaStore.Files.FileColumns.MEDIA_TYPE,
                MediaStore.Files.FileColumns.MIME_TYPE
            )

            // Define the selection criteria to filter by the folder ID and media types (images or videos)
            val selection =
                "${MediaStore.Files.FileColumns.BUCKET_ID}=? AND (${MediaStore.Files.FileColumns.MEDIA_TYPE}=? OR ${MediaStore.Files.FileColumns.MEDIA_TYPE}=?)"
            val selectionArgs = arrayOf(
                folderId,
                MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString(),
                MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString()
            )

            // Define the sort order to sort by the date added in descending order
            val sortOrder = "${MediaStore.Files.FileColumns.DATE_ADDED} DESC"

            // Define the URI for querying the external media content
            val queryUri = MediaStore.Files.getContentUri("external")

            // Perform the query to get the media files
            val cursor =
                contentResolver.query(queryUri, projection, selection, selectionArgs, sortOrder)

            cursor?.use {
                // Get the column indices for ID and MIME type
                val idColumn = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
                val mimeTypeColumn =
                    it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE)

                // Iterate through the cursor to retrieve media items
                while (it.moveToNext()) {
                    val id = it.getLong(idColumn)
                    val mimeType = it.getString(mimeTypeColumn)

                    // Construct the content URI based on MIME type
                    val contentUri = when {
                        mimeType.startsWith("image") -> Uri.withAppendedPath(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                            id.toString()
                        )

                        mimeType.startsWith("video") -> Uri.withAppendedPath(
                            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                            id.toString()
                        )

                        else -> continue
                    }

                    // Determine the media type
                    val mediaType =
                        if (mimeType.startsWith("image")) MediaType.IMAGE else MediaType.VIDEO

                    // Add the media item to the list
                    mediaList.add(MediaItem(contentUri, mediaType))
                }
            }

            // Return the list of media items
            return@withContext mediaList
        }


    /**
     *Show Permission Denied Dialog When  the  User Denied  For the Permission  of  Gallery
     */
    private fun showPermissionDeniedDialog() {

        CustomGalleryUtils.openAppSettings(this)

    }


    /**
     * Retrieves the count of media files (images and videos) within a specific folder from the device's media store.
     *
     * This function performs a query to count the number of media files in a given folder identified by its folder ID.
     * The query is executed on the content resolver to get the count of media items filtered by the specified folder ID.
     *
     * @param folderId The ID of the folder for which to count the media files.
     * @return The count of media files within the specified folder. Returns 0 if no media is found.
     */

    private fun getMediaCount(folderId: String): Int {
        // Define the projection to only retrieve the count of media items
        val projection = arrayOf("COUNT(${MediaStore.Files.FileColumns._ID})")

        // Define the selection criteria to filter by the specified folder ID
        val selection = "${MediaStore.Files.FileColumns.BUCKET_ID}=?"
        val selectionArgs = arrayOf(folderId)

        // Define the query URI for the external content
        val queryUri = MediaStore.Files.getContentUri("external")

        // Perform the query to get the count of media files in the specified folder
        val cursor = contentResolver.query(queryUri, projection, selection, selectionArgs, null)

        // Extract the count from the cursor
        val count = cursor?.use {
            if (it.moveToFirst()) it.getInt(0) else 0
        }

        // Return the count, defaulting to 0 if no count is found
        return count ?: 0
    }

    /**
     * Retrieves the total count of all media files (images and videos) from the device's media store.
     *
     * This function performs a query to count the number of media files (both images and videos) stored on the device.
     * The query is executed on the content resolver to get the count of media items, filtered by media type.
     *
     * @return The count of media files (images and videos) available in the media store. Returns 0 if no media is found.
     */

    private fun getAllMediaCount(): Int {
        // Define the projection to only retrieve the count of media items
        val projection = arrayOf("COUNT(${MediaStore.Files.FileColumns._ID})")

        // Define the selection criteria to filter for image and video files
        val selection =
            "${MediaStore.Files.FileColumns.MEDIA_TYPE}=? OR ${MediaStore.Files.FileColumns.MEDIA_TYPE}=?"
        val selectionArgs = arrayOf(
            MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString(),
            MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString()
        )

        // Define the query URI for the external content
        val queryUri = MediaStore.Files.getContentUri("external")

        // Perform the query to get the count of media files (images and videos)
        val cursor = contentResolver.query(queryUri, projection, selection, selectionArgs, null)

        // Extract the count from the cursor
        val count = cursor?.use {
            if (it.moveToFirst()) it.getInt(0) else 0
        }

        // Return the count, defaulting to 0 if no count is found
        return count ?: 0
    }


    /**
     * Retrieves the total count of video files from the device's media store.
     *
     * This function performs a query to count the number of video files stored on the device.
     * The query is executed on the content resolver to get the count of video media items.
     *
     * @return The count of video files available in the media store. Returns 0 if no videos are found.
     */
    private fun getAllVideosCount(): Int {
        // Define the selection criteria to filter for video files
        val projection = arrayOf("COUNT(${MediaStore.Files.FileColumns._ID})")


        val selection = "${MediaStore.Files.FileColumns.MEDIA_TYPE}=?"
        val selectionArgs = arrayOf(MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString())
        // Define the query URI for the external content
        val queryUri = MediaStore.Files.getContentUri("external")

        // Perform the query to get the count of video files
        val cursor = contentResolver.query(queryUri, projection, selection, selectionArgs, null)

        // Extract the count from the cursor
        val count = cursor?.use {
            if (it.moveToFirst()) it.getInt(0) else 0
        }
        return count ?: 0
    }


    /**
     * Loads a list of video media items from the device's media store.
     *
     * This function queries the media store for video files, processes the query results,
     * and returns a list of `MediaItem` objects representing the videos.
     * The query is performed on a background thread to avoid blocking the main thread.
     *
     * @return A list of `MediaItem` objects representing the video files found.
     */

    private suspend fun loadVideos(): List<MediaItem> = withContext(Dispatchers.IO) {
        val mediaList = mutableListOf<MediaItem>()

        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.MEDIA_TYPE,
            MediaStore.Files.FileColumns.MIME_TYPE
        )

        val selection = "${MediaStore.Files.FileColumns.MEDIA_TYPE}=?"
        val selectionArgs = arrayOf(MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString())
        val sortOrder = "${MediaStore.Files.FileColumns.DATE_ADDED} DESC"
        val queryUri = MediaStore.Files.getContentUri("external")

        val cursor =
            contentResolver.query(queryUri, projection, selection, selectionArgs, sortOrder)

        cursor?.use {
            val idColumn = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
            val mimeTypeColumn = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE)

            while (it.moveToNext()) {
                val id = it.getLong(idColumn)
                val mimeType = it.getString(mimeTypeColumn)

                val contentUri = if (mimeType.startsWith("video")) {
                    Uri.withAppendedPath(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id.toString())
                } else {
                    continue
                }

                mediaList.add(MediaItem(contentUri, MediaType.VIDEO))
            }
        }

        return@withContext mediaList
    }

    /**
     * Retrieves the URI of the first media file (image or video) found in the media store.
     *
     * This function queries the media store to get the URI of the most recently added media file.
     * The query is performed on a background thread to avoid blocking the main thread.
     *
     * @return The URI of the first media file (image or video), or null if no media files are found.
     */

    private suspend fun getFirstMediaUriForAllMedia(): Uri? = withContext(Dispatchers.IO) {
        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.MEDIA_TYPE,
            MediaStore.Files.FileColumns.MIME_TYPE
        )
        val selection =
            "${MediaStore.Files.FileColumns.MEDIA_TYPE}=? OR ${MediaStore.Files.FileColumns.MEDIA_TYPE}=?"
        val selectionArgs = arrayOf(
            MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString(),
            MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString()
        )
        val sortOrder = "${MediaStore.Files.FileColumns.DATE_ADDED} DESC"
        val queryUri = MediaStore.Files.getContentUri("external")

        val cursor =
            contentResolver.query(queryUri, projection, selection, selectionArgs, sortOrder)
        cursor?.use {
            if (it.moveToFirst()) {
                val idColumn = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
                val mimeTypeColumn =
                    it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE)

                val id = it.getLong(idColumn)
                val mimeType = it.getString(mimeTypeColumn)

                return@withContext when {
                    mimeType.startsWith("image") -> Uri.withAppendedPath(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        id.toString()
                    )

                    mimeType.startsWith("video") -> Uri.withAppendedPath(
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                        id.toString()
                    )

                    else -> null
                }
            }
        }
        return@withContext null
    }

    /**
     * Retrieves the URI of the first video file found in the media store.
     *
     * This function queries the media store to get the URI of the most recently added video.
     * The query is performed on a background thread to avoid blocking the main thread.
     *
     * @return The URI of the first video file, or null if no video files are found.
     */

    private suspend fun getFirstMediaUriForAllVideos(): Uri? = withContext(Dispatchers.IO) {
        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.MIME_TYPE
        )
        val selection = "${MediaStore.Files.FileColumns.MEDIA_TYPE}=?"
        val selectionArgs = arrayOf(MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString())
        val sortOrder = "${MediaStore.Files.FileColumns.DATE_ADDED} DESC"
        val queryUri = MediaStore.Files.getContentUri("external")

        val cursor =
            contentResolver.query(queryUri, projection, selection, selectionArgs, sortOrder)
        cursor?.use {
            if (it.moveToFirst()) {
                val idColumn = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
                val id = it.getLong(idColumn)

                return@withContext Uri.withAppendedPath(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    id.toString()
                )
            }
        }
        return@withContext null
    }

    /**
     * Updates the TextView displaying the date of a media file based on its URI.
     *
     * This function retrieves the formatted date of the media file asynchronously and updates
     * the TextView on the main thread.
     *
     * @param uri The URI of the media file whose date is to be retrieved and displayed.
     */

    fun updateDateTextView(contentResolver: ContentResolver, uri: Uri) {
        GlobalScope.launch(Dispatchers.IO) {
            val date = CustomGalleryUtils.getMediaDate(contentResolver, uri)
            withContext(Dispatchers.Main) {
                binding.tvDate.text = date
            }
        }
    }


    private fun updateDoneButton(selectedCount: Int) {
        if (selectedCount == 0) {
            binding.mcvDone.isClickable = false
            binding.tvSelctedCount.text = "Post"
        } else {
            // Enable the button, change its color back to default, and show selected count
            binding.mcvDone.isClickable = true

            binding.tvSelctedCount.text =
                "Post ($selectedCount/$maxSelection)" // Text with selected count
        }
    }

    /**
     * Updates the selection mode for the media items.
     * This function toggles between single and multi-selection modes.
     * It updates the UI to reflect the current mode and adjusts the maximum
     * number of items that can be selected accordingly. It also updates
     * the displayed selected item count and informs the adapter about the
     * change in selection mode.
     */
    private fun updateSelectionMode() {
        if (isMultiSelectMode) {
            binding.tvselectSignlemulti.text = "Select Single"
            maxSelection = 10 // Maximum selection for multi-select mode
        } else {
            binding.tvselectSignlemulti.text = "Select Multiple"
            maxSelection = 1 // Maximum selection for single-select mode
        }

        // Get the current selected count and update the done button
        val selectedCount = mediaAdapter.getSelectedItems().size
        updateDoneButton(selectedCount)
        mediaAdapter.setSelectionMode(isMultiSelectMode, maxSelection)
    }


    private fun requestAppropriatePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13 and above: Request specific media permissions
            requestMediaPermissionsLauncher.launch(
                arrayOf(
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_VIDEO,
                    Manifest.permission.READ_MEDIA_AUDIO
                )
            )
        } else {
            requestStoragePermission()
        }
    }

    private fun isReadStoragePermissionGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_MEDIA_IMAGES
            ) == PackageManager.PERMISSION_GRANTED
                    && ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_MEDIA_VIDEO
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            // For Android 12 and below, check general storage permission
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }


    private fun requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13 and above: Request specific media permissions
            requestMediaPermissionsLauncher.launch(
                arrayOf(
                    Manifest.permission.READ_MEDIA_IMAGES,// For Read the  Images From Storage
                    Manifest.permission.READ_MEDIA_VIDEO, // For Read the  Video From Storage
                    Manifest.permission.READ_MEDIA_AUDIO //  For Read the  Audio From Storage
                )
            )
        } else {
            // Android 12 and below: Request general storage permission
            requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }


    @OptIn(DelicateCoroutinesApi::class)
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                GlobalScope.launch(Dispatchers.Main) {
                    folderList = loadFolders().toMutableList()
                    loadMedia("all_media") // Load all media initially
                }
            } else {

                showPermissionDeniedDialog()
            }
        }

    // Launcher for requesting multiple media permissions
    private val requestMediaPermissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val allGranted = permissions.values.all { it }
            if (allGranted) {
                GlobalScope.launch(Dispatchers.Main) {
                    folderList = loadFolders().toMutableList()
                    loadMedia(folderList.firstOrNull()?.folderId ?: "")
                }

            } else {
                showPermissionDeniedDialog()
            }
        }
}
