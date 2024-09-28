package com.anurupjaiswal.ajmediapicker.basic.customgallery

import android.net.Uri



/**
 * Data class representing a folder item in the gallery.
 * @param folderName The name of the folder.
 * @param folderId A unique identifier for the folder.
 * @param folderPath The file path of the folder.
 * @param folderUri The URI of the folder's thumbnail image or representative media item.
 * @param mediaCount The number of media items within the folder. Defaults to 0.
 */

data class FolderItem(
    val folderName: String,
    val folderId: String,
    val folderPath: String,
    val folderUri: Uri,
    var mediaCount: Int = 0
)