package net.winedownwednesday.web


import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import org.jetbrains.skia.ImageInfo
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

object ImageBitmapSerializer : KSerializer<Bitmap> {

    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("Bitmap", PrimitiveKind.STRING)

    @OptIn(ExperimentalEncodingApi::class)
    override fun serialize(encoder: Encoder, value: Bitmap) {
        val image = Image.makeFromBitmap(value)

        val encodedBytes = image.encodeToData(EncodedImageFormat.PNG, 80)?.bytes ?: byteArrayOf()

        val base64String = Base64.encode(encodedBytes)

        encoder.encodeString(base64String)
    }

    @OptIn(ExperimentalEncodingApi::class)
    override fun deserialize(decoder: Decoder): Bitmap {

        val base64String = decoder.decodeString()

        val decodedBytes = Base64.decode(base64String)

        val image = Image.makeFromEncoded(decodedBytes)

        val bitmap = Bitmap()
        val imageInfo = ImageInfo.makeN32Premul(image.width, image.height)
        bitmap.allocPixels(imageInfo)

        image.readPixels(bitmap, 0, 0)

        return bitmap
    }
}