package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import com.example.ui.screens.ReelMediaTypeOption
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import java.util.UUID

sealed class PickedMediaResult {
    data class Success(
        val localMediaUri: String,
        val localThumbnailUri: String,
        val mediaType: ReelMediaTypeOption,
        val displayName: String,
        val fileSizeMb: Double,
        val durationSeconds: Int
    ) : PickedMediaResult()

    data class Error(val message: String) : PickedMediaResult()
}

object MediaUploadHelper {

    private const val MAX_FILE_SIZE_BYTES = 150L * 1024L * 1024L // 150 MB
    private val VIDEO_EXTENSIONS = setOf("mp4", "mov", "mkv", "webm", "3gp", "m4v", "avi")
    private val IMAGE_EXTENSIONS = setOf("jpg", "jpeg", "png", "webp", "heic", "heif")

    /**
     * Copies a device-picked image or video URI (from Photo Picker or SAF OpenDocument)
     * into app-private permanent storage so ExoPlayer & Room never lose URI read permissions,
     * auto-detects video vs image reliably, and extracts a real video frame poster + duration.
     */
    suspend fun persistAndPreparePickedMedia(
        context: Context,
        sourceUri: Uri,
        preferredType: ReelMediaTypeOption? = null
    ): PickedMediaResult = withContext(Dispatchers.IO) {
        try {
            val contentResolver = context.contentResolver
            val rawMime = contentResolver.getType(sourceUri)?.lowercase(Locale.ROOT)
            val (displayName, reportedSize) = queryDisplayNameAndSize(context, sourceUri)
            val extension = resolveExtension(displayName, rawMime, sourceUri)

            val detectedType = detectMediaType(
                context = context,
                sourceUri = sourceUri,
                mimeType = rawMime,
                extension = extension,
                preferredType = preferredType
            )

            if (reportedSize > MAX_FILE_SIZE_BYTES) {
                return@withContext PickedMediaResult.Error(
                    "Selected file exceeds the 150 MB upload limit (${"%.1f".format(reportedSize / (1024.0 * 1024.0))} MB)."
                )
            }

            val uploadDir = File(context.filesDir, "uploaded_reels").apply {
                if (!exists()) mkdirs()
            }

            val finalExt = when {
                extension.isNotBlank() -> extension
                detectedType == ReelMediaTypeOption.VIDEO -> "mp4"
                else -> "jpg"
            }

            val uniqueId = UUID.randomUUID().toString().take(12)
            val destFile = File(uploadDir, "reel_${uniqueId}.$finalExt")

            val copiedBytes = contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    val buffer = ByteArray(16 * 1024)
                    var total = 0L
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        total += read
                        if (total > MAX_FILE_SIZE_BYTES) {
                            destFile.delete()
                            return@withContext PickedMediaResult.Error(
                                "Selected file exceeds the 150 MB upload limit."
                            )
                        }
                        output.write(buffer, 0, read)
                    }
                    total
                }
            } ?: return@withContext PickedMediaResult.Error(
                "Could not read the selected file from device storage. Please try Browse Files."
            )

            if (copiedBytes <= 0L || !destFile.exists() || destFile.length() <= 0L) {
                destFile.delete()
                return@withContext PickedMediaResult.Error(
                    "The selected file appears to be empty (0 bytes)."
                )
            }

            val finalType = if (detectedType == ReelMediaTypeOption.VIDEO) {
                ReelMediaTypeOption.VIDEO
            } else {
                // Double-check if an ambiguous file is actually a playable video
                if (probeHasVideoTrack(destFile.absolutePath)) {
                    ReelMediaTypeOption.VIDEO
                } else {
                    ReelMediaTypeOption.IMAGE
                }
            }

            val localMediaUri = Uri.fromFile(destFile).toString()
            val fileSizeMb = (destFile.length() / (1024.0 * 1024.0)).coerceAtLeast(0.1)

            if (finalType == ReelMediaTypeOption.VIDEO) {
                val (durationSec, thumbFile) = extractVideoMetadataAndPoster(
                    videoFile = destFile,
                    outputDir = uploadDir,
                    uniqueId = uniqueId
                )
                val localThumbUri = if (thumbFile != null && thumbFile.exists()) {
                    Uri.fromFile(thumbFile).toString()
                } else {
                    ""
                }
                PickedMediaResult.Success(
                    localMediaUri = localMediaUri,
                    localThumbnailUri = localThumbUri,
                    mediaType = ReelMediaTypeOption.VIDEO,
                    displayName = displayName.ifBlank { destFile.name },
                    fileSizeMb = fileSizeMb,
                    durationSeconds = durationSec
                )
            } else {
                PickedMediaResult.Success(
                    localMediaUri = localMediaUri,
                    localThumbnailUri = localMediaUri,
                    mediaType = ReelMediaTypeOption.IMAGE,
                    displayName = displayName.ifBlank { destFile.name },
                    fileSizeMb = fileSizeMb,
                    durationSeconds = 0
                )
            }
        } catch (e: SecurityException) {
            PickedMediaResult.Error(
                "Permission denied reading this media URI. Try selecting it via Browse Files."
            )
        } catch (e: Exception) {
            PickedMediaResult.Error(
                e.localizedMessage ?: "Failed to prepare selected media file."
            )
        }
    }

    internal fun detectMediaTypeFromHints(
        mimeType: String?,
        extension: String?,
        preferredType: ReelMediaTypeOption? = null
    ): ReelMediaTypeOption {
        val normMime = mimeType?.trim()?.lowercase(Locale.ROOT).orEmpty()
        val normExt = extension?.trim()?.lowercase(Locale.ROOT)?.removePrefix(".").orEmpty()

        return when {
            normMime.startsWith("video/") -> ReelMediaTypeOption.VIDEO
            normMime.startsWith("image/") -> ReelMediaTypeOption.IMAGE
            normExt in VIDEO_EXTENSIONS -> ReelMediaTypeOption.VIDEO
            normExt in IMAGE_EXTENSIONS -> ReelMediaTypeOption.IMAGE
            preferredType != null -> preferredType
            else -> ReelMediaTypeOption.IMAGE
        }
    }

    private fun detectMediaType(
        context: Context,
        sourceUri: Uri,
        mimeType: String?,
        extension: String,
        preferredType: ReelMediaTypeOption?
    ): ReelMediaTypeOption {
        val normMime = mimeType?.trim()?.lowercase(Locale.ROOT).orEmpty()
        if (normMime.startsWith("video/")) return ReelMediaTypeOption.VIDEO
        if (normMime.startsWith("image/")) return ReelMediaTypeOption.IMAGE

        val normExt = extension.lowercase(Locale.ROOT).removePrefix(".")
        if (normExt in VIDEO_EXTENSIONS) return ReelMediaTypeOption.VIDEO
        if (normExt in IMAGE_EXTENSIONS) return ReelMediaTypeOption.IMAGE

        // Fallback: ask MediaMetadataRetriever whether this URI has a video track
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, sourceUri)
            val hasVideo = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_VIDEO)
            if (hasVideo.equals("yes", ignoreCase = true) || hasVideo == "1") {
                return ReelMediaTypeOption.VIDEO
            }
        } catch (_: Exception) {
            // Not a video / unreadable via MMR
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {
            }
        }

        return preferredType ?: ReelMediaTypeOption.IMAGE
    }

    private fun probeHasVideoTrack(filePath: String): Boolean {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(filePath)
            val hasVideo = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_VIDEO)
            hasVideo.equals("yes", ignoreCase = true) || hasVideo == "1"
        } catch (_: Exception) {
            false
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {
            }
        }
    }

    private fun extractVideoMetadataAndPoster(
        videoFile: File,
        outputDir: File,
        uniqueId: String
    ): Pair<Int, File?> {
        val retriever = MediaMetadataRetriever()
        var durationSeconds = 15
        var posterFile: File? = null
        try {
            retriever.setDataSource(videoFile.absolutePath)
            val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            if (durationMs > 0L) {
                durationSeconds = ((durationMs + 500L) / 1000L).toInt().coerceAtLeast(1)
            }

            val frameTimeUs = if (durationMs > 1000L) 500_000L else 0L
            val bitmap = retriever.getFrameAtTime(frameTimeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                ?: retriever.getFrameAtTime(0L)
            if (bitmap != null) {
                val scaled = scaleBitmapIfNeeded(bitmap, maxDim = 720)
                val out = File(outputDir, "thumb_${uniqueId}.jpg")
                FileOutputStream(out).use { fos ->
                    scaled.compress(Bitmap.CompressFormat.JPEG, 85, fos)
                }
                if (scaled !== bitmap) scaled.recycle()
                bitmap.recycle()
                posterFile = out
            }
        } catch (_: Exception) {
            // Leave fallback defaults if metadata extraction isn't supported
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {
            }
        }
        return durationSeconds to posterFile
    }

    private fun scaleBitmapIfNeeded(source: Bitmap, maxDim: Int): Bitmap {
        val w = source.width
        val h = source.height
        if (w <= maxDim && h <= maxDim) return source
        val ratio = maxDim.toFloat() / maxOf(w, h).toFloat()
        val targetW = (w * ratio).toInt().coerceAtLeast(1)
        val targetH = (h * ratio).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(source, targetW, targetH, true)
    }

    private fun queryDisplayNameAndSize(context: Context, uri: Uri): Pair<String, Long> {
        var name = uri.lastPathSegment?.substringAfterLast('/') ?: ""
        var size = -1L
        try {
            context.contentResolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (nameIdx >= 0 && !cursor.isNull(nameIdx)) {
                        name = cursor.getString(nameIdx)
                    }
                    if (sizeIdx >= 0 && !cursor.isNull(sizeIdx)) {
                        size = cursor.getLong(sizeIdx)
                    }
                }
            }
        } catch (_: Exception) {
            // Some URI providers don't support OpenableColumns queries
        }
        return name to size
    }

    private fun resolveExtension(displayName: String, mimeType: String?, uri: Uri): String {
        val fromName = displayName.substringAfterLast('.', "").lowercase(Locale.ROOT)
        if (fromName.length in 2..5 && fromName.all { it.isLetterOrDigit() }) {
            return fromName
        }
        val fromMime = if (!mimeType.isNullOrBlank()) {
            MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)?.lowercase(Locale.ROOT)
        } else null
        if (!fromMime.isNullOrBlank()) {
            return fromMime
        }
        val fromPath = uri.lastPathSegment?.substringAfterLast('.', "")?.lowercase(Locale.ROOT).orEmpty()
        return if (fromPath.length in 2..5 && fromPath.all { it.isLetterOrDigit() }) fromPath else ""
    }
}
