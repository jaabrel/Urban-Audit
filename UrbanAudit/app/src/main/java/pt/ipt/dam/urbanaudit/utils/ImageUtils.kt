package pt.ipt.dam.urbanaudit.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * Utilitários de manipulação de imagem para a aplicação Urban Audit.
 * Trata da rotação EXIF, compressão e armazenamento seguro de fotografias capturadas.
 */
object ImageUtils {

    /**
     * Cria um ficheiro temporário para receber a captura da câmara fotográfica.
     */
    fun criarFicheiroImagem(context: Context): File {
        val storageDir = context.getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "UA_${System.currentTimeMillis()}_",
            ".jpg",
            storageDir
        )
    }

    /**
     * Carrega um Bitmap de um ficheiro ajustando as dimensões para evitar problemas de memória (OOM).
     */
    fun carregarBitmapRedimensionado(caminhoFicheiro: String, larguraDesejada: Int = 1024, alturaDesejada: Int = 1024): Bitmap? {
        val ficheiro = File(caminhoFicheiro)
        if (!ficheiro.exists()) return null

        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeFile(caminhoFicheiro, options)

        var scale = 1
        while (options.outWidth / scale / 2 >= larguraDesejada && options.outHeight / scale / 2 >= alturaDesejada) {
            scale *= 2
        }

        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = scale
        }
        val bitmap = BitmapFactory.decodeFile(caminhoFicheiro, decodeOptions) ?: return null

        // Corrige a orientação da fotografia caso tenha sido tirada em modo retrato/paisagem
        return corrigirOrientacaoExif(caminhoFicheiro, bitmap)
    }

    /**
     * Corrige a orientação do Bitmap com base nos metadados EXIF da câmara.
     */
    private fun corrigirOrientacaoExif(caminhoFicheiro: String, bitmap: Bitmap): Bitmap {
        return try {
            val exif = ExifInterface(caminhoFicheiro)
            val orientacao = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            val angulo = when (orientacao) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
            if (angulo != 0f) {
                val matrix = Matrix().apply { postRotate(angulo) }
                Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            } else {
                bitmap
            }
        } catch (e: Exception) {
            bitmap
        }
    }

    /**
     * Converte um Bitmap numa String Base64 (útil para interoperabilidade com futuras APIs REST).
     */
    fun converterBitmapParaBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 75, outputStream)
        val bytes = outputStream.toByteArray()
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    /**
     * Descodifica uma string Base64 para um Bitmap.
     * Suporta formato bruto ou com prefixo data URL ("data:image/...;base64,").
     */
    fun converterBase64ParaBitmap(base64Str: String?): Bitmap? {
        if (base64Str.isNullOrBlank()) return null
        return try {
            val limpa = if (base64Str.contains(",")) {
                base64Str.substringAfter(",")
            } else {
                base64Str
            }
            val bytes = Base64.decode(limpa.trim(), Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Elimina o ficheiro da fotografia do armazenamento interno.
     */
    fun eliminarFicheiro(caminhoFicheiro: String) {
        try {
            val file = File(caminhoFicheiro)
            if (file.exists()) {
                file.delete()
            }
        } catch (e: Exception) {
            // Ignora falha de eliminação se o ficheiro já não existir
        }
    }
}
