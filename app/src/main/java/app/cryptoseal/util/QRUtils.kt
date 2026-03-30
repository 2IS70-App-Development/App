package app.cryptoseal.util

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter

/**
 * Utility object for generating QR codes.
 */
object QRUtils {
    /**
     * Generates a square QR code bitmap for the given content string.
     *
     * @param content The text to be encoded in the QR code.
     * @param size The width and height of the resulting bitmap in pixels.
     * @return A [Bitmap] representing the QR code.
     */
    fun generateQrBitmap(content: String, size: Int = 512): Bitmap {
        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size)
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)

        // Map the ZXing bit matrix to a Bitmap.
        for (x in 0 until size) {
            for (y in 0 until size) {
                bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
            }
        }
        return bitmap
    }
}
