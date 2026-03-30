package app.cryptoseal.tabs.creator

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build

/**
 * Loads a [Bitmap] from a given [Uri] while handling API version differences.
 * Decodes the image into software memory for compatibility with most image processing tools.
 */
fun loadBitmapFromUri(context: android.content.Context, uri: Uri): Bitmap? {
    return try {
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                android.graphics.ImageDecoder.decodeBitmap(
                    android.graphics.ImageDecoder.createSource(context.contentResolver, uri)
                ) { decoder, _, _ ->
                    decoder.allocator = android.graphics.ImageDecoder.ALLOCATOR_SOFTWARE
                    decoder.isMutableRequired = true
                }
            } else {
                BitmapFactory.decodeStream(inputStream)
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

/**
 * A custom [ActivityResultContract] to trigger the system document creation picker
 * with a pre-filled default filename.
 */
class CreateDocumentWithName(mimeType: String) :
    androidx.activity.result.contract.ActivityResultContract<String, android.net.Uri?>() {
    private val mimeType_ = mimeType
    override fun createIntent(context: android.content.Context, input: String) =
        android.content.Intent(android.content.Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(android.content.Intent.CATEGORY_OPENABLE)
            type = mimeType_
            putExtra(android.content.Intent.EXTRA_TITLE, input)
        }

    override fun parseResult(resultCode: Int, intent: android.content.Intent?) =
        if (resultCode == android.app.Activity.RESULT_OK) intent?.data else null
}
