package com.abdulbasit.cloudmusicplayer

import com.google.api.services.drive.Drive
import com.google.api.services.drive.model.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

class DriveServiceHelper(private val driveService: Drive) {

    /**
     * Step 1: Yeh function Google Drive se sirf Folders fetch karega (Main Screen ke liye)
     */
    suspend fun queryFolders(): List<Folder> = withContext(Dispatchers.IO) {
        val folderList = mutableListOf<Folder>()
        try {
            // Query: Sirf folders uthayega jo delete nahi huye hain (trashed = false)
            val query = "mimeType = 'application/vnd.google-apps.folder' and trashed = false"

            var pageToken: String? = null
            do {
                val result = driveService.files().list()
                    .setQ(query)
                    .setSpaces("drive")
                    .setFields("nextPageToken, files(id, name)")
                    .setPageToken(pageToken)
                    .execute()

                val files: List<File>? = result.files
                if (files != null) {
                    for (file in files) {
                        // Hum system/hidden folders ko filter karne ke liye filter laga sakte hain
                        // Filhal hum saare normal folders ko add kar rahe hain
                        folderList.add(
                            Folder(
                                id = file.id ?: "",
                                name = file.name ?: "Unknown Folder"
                            )
                        )
                    }
                }
                pageToken = result.nextPageToken
            } while (pageToken != null)

        } catch (e: IOException) {
            e.printStackTrace()
        }
        return@withContext folderList
    }

    /**
     * Step 2: Jab user folder par click karega, tab us specific folder ke andar ki MP3 files fetch hongi
     */
    suspend fun queryMp3FilesFromFolder(folderId: String): List<Song> = withContext(Dispatchers.IO) {
        val songList = mutableListOf<Song>()
        try {
            // Query: Is baar hum check kar rahe hain ki file MP3 ho AUR uska parent wahi folderId ho jise click kiya gaya hai
            val query = "'$folderId' in parents and (mimeType = 'audio/mpeg' or name contains '.mp3') and trashed = false"

            var pageToken: String? = null
            do {
                val result = driveService.files().list()
                    .setQ(query)
                    .setSpaces("drive")
                    .setFields("nextPageToken, files(id, name, size)")
                    .setPageToken(pageToken)
                    .execute()

                val files: List<File>? = result.files
                if (files != null) {
                    for (file in files) {
                        val streamUrl = "https://docs.google.com/uc?export=download&id=${file.id}"
                        songList.add(
                            Song(
                                id = file.id ?: "",
                                title = file.name ?: "Unknown Song",
                                url = streamUrl
                            )
                        )
                    }
                }
                pageToken = result.nextPageToken
            } while (pageToken != null)

        } catch (e: IOException) {
            e.printStackTrace()
        }
        return@withContext songList
    }
}