package com.anurupjaiswal.ajmediapicker.basic.customgallery


/**
 * CustomGalleryUtils.kt
 *
 * Created by: Anurup Jaiswal
 * Created on: 27th August 2024
 * Purpose: This utility class provides helper methods for retrieving media dates
 * and video durations,getRealPathFromURI.
 */


import android.app.Activity
import android.app.Dialog
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaPlayer
import android.media.ThumbnailUtils
import android.net.Uri
import android.provider.MediaStore
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import android.widget.Toast
import com.anurupjaiswal.ajmediapicker.R
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CustomGalleryUtils {

    /**
     * Retrieves the real file path from a URI.
     * @param context The context.
     * @param uri The URI to query.
     * @return The file path as a string, or null if not found.
     */
    fun getRealPathFromURI(context: Context, uri: Uri): String? {
        var result: String? = null
        val cursor = context.contentResolver.query(
            uri,
            arrayOf(MediaStore.Images.ImageColumns.DATA),
            null,
            null,
            null
        )
        cursor?.use {
            if (it.moveToFirst()) {
                val idx = it.getColumnIndex(MediaStore.Images.ImageColumns.DATA)
                result = it.getString(idx)
            }
        }
        return result
    }

    /**
     * Creates a thumbnail for a video.
     * @param context The context.
     * @param uri The URI of the video.
     * @return A Bitmap of the thumbnail, or null if creation failed.
     */
    fun createVideoThumbnail(context: Context, uri: Uri): Bitmap? {
        val filePath = getRealPathFromURI(context, uri)
        return filePath?.let {
            ThumbnailUtils.createVideoThumbnail(it, MediaStore.Video.Thumbnails.MINI_KIND)
        }
    }

    /**
     * Retrieves the duration of a video as a formatted string (mm:ss).
     * @param context The context.
     * @param uri The URI of the video.
     * @return The formatted duration string.
     */
    fun getVideoDuration(context: Context, uri: Uri): String {
        var duration = "00:00"
        val mediaPlayer = MediaPlayer()

        try {
            mediaPlayer.setDataSource(context, uri)
            mediaPlayer.prepare()
            val durationMs = mediaPlayer.duration
            val minutes = (durationMs / (1000 * 60)) % 60
            val seconds = (durationMs / 1000) % 60
            duration = String.format("%02d:%02d", minutes, seconds)
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            mediaPlayer.release()
        }

        return duration
    }



    /**
     * Retrieves the media creation date from a URI.
     * @param contentResolver The content resolver to query the media.
     * @param uri The URI of the media file.
     * @return The formatted date string or 'Unknown' if not found.
     */
    suspend fun getMediaDate(contentResolver: ContentResolver, uri: Uri): String = withContext(
        Dispatchers.IO) {
        val today = Date()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val displayFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())

        var date = "Unknown"
        val projection = arrayOf(MediaStore.Files.FileColumns.DATE_ADDED)
        val cursor = contentResolver.query(uri, projection, null, null, null)

        cursor?.use {
            if (it.moveToFirst()) {
                val dateAdded = it.getLong(it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_ADDED))
                val mediaDate = Date(dateAdded * 1000) // Convert from seconds to milliseconds

                val mediaDateStr = dateFormat.format(mediaDate)
                val todayStr = dateFormat.format(today)

                date = when (mediaDateStr) {
                    todayStr -> "Today"
                    dateFormat.format(Date(today.time - 86400000)) -> "Yesterday" // 86400000ms = 1 day
                    else -> displayFormat.format(mediaDate)
                }
            }
        }
        date
    }



    fun openAppSettings(activity: Activity) {
        val dialog = Dialog(activity)
        val view = activity.layoutInflater.inflate(R.layout.dialog_box_item_settings, null)
        val setting = view.findViewById<MaterialCardView>(R.id.mcvSettings)
        val cancel = view.findViewById<MaterialCardView>(R.id.mcvCancel)

        cancel.setOnClickListener {
            dialog.dismiss()

        }
        setting.setOnClickListener {
            dialog.dismiss()
            activity.startActivity(
                Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", activity.packageName, null)
                )
            )
        }
        dialog.setCancelable(true)
        dialog.setContentView(view)
        dialog.show()
    }

}
