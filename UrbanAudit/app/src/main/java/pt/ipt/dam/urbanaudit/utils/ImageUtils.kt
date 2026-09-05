package pt.ipt.dam.urbanaudit.utils

import android.graphics.Bitmap
import android.util.Base64
import java.io.ByteArrayOutputStream

object ImageUtils {

    fun converterBitmapParaBase64(bitmap: Bitmap, qualidade: Int = 50): String {
        val outputStream = ByteArrayOutputStream()

        bitmap.compress(Bitmap.CompressFormat.JPEG, qualidade, outputStream)

        val byteArray = outputStream.toByteArray()

        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }
}