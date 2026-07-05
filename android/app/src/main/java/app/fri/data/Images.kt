package app.fri.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.ByteArrayOutputStream

/**
 * Downscales a picked photo to [maxEdge] px on the long side and re-encodes
 * as JPEG — nobody on mobile data gets a 4MB hero image, and neither does
 * the git repo.
 */
fun resizeToJpeg(context: Context, uri: Uri, maxEdge: Int = 1600, quality: Int = 80): ByteArray {
    val resolver = context.contentResolver

    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    resolver.openInputStream(uri)!!.use { BitmapFactory.decodeStream(it, null, bounds) }

    var sample = 1
    while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxEdge) sample *= 2
    val opts = BitmapFactory.Options().apply { inSampleSize = sample }
    var bitmap = resolver.openInputStream(uri)!!.use {
        BitmapFactory.decodeStream(it, null, opts)
    } ?: error("could not decode image")

    val longEdge = maxOf(bitmap.width, bitmap.height)
    if (longEdge > maxEdge) {
        val scale = maxEdge.toFloat() / longEdge
        bitmap = Bitmap.createScaledBitmap(
            bitmap,
            (bitmap.width * scale).toInt(),
            (bitmap.height * scale).toInt(),
            true,
        )
    }

    val rotation = resolver.openInputStream(uri)!!.use { stream ->
        when (
            ExifInterface(stream).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL,
            )
        ) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
    }
    if (rotation != 0f) {
        val matrix = Matrix().apply { postRotate(rotation) }
        bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    val out = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
    return out.toByteArray()
}
