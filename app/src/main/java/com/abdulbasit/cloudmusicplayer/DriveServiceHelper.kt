package com.abdulbasit.cloudmusicplayer

import com.google.api.services.drive.Drive
import com.google.api.services.drive.model.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

class DriveServiceHelper(private val driveService: Drive) {

    /**
     * Yeh function user ke Google Drive se saari MP3 files dhoondh kar layega
     */
    suspend fun queryMp3Files(): List<Song> = withContext(Dispatchers.IO) {
        val songList = mutableListOf<Song>()
        try {
            // Broad Query: MimeType ke sath '.mp3' extension bhi check karega
            val query = "(mimeType = 'audio/mpeg' or name contains '.mp3') and trashed = false"

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