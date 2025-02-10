package net.winedownwednesday.web

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

fun formatPhoneNumber(phoneNumber: String): String {
    val trimmed = phoneNumber.replace(Regex("[^0-9]"), "").take(10)
    val builder = StringBuilder()
    if (trimmed.isNotEmpty()) {
        builder.append("(")
    }
    for (i in trimmed.indices) {
        if (i == 3) builder.append(") ")
        if (i == 6) builder.append("-")
        builder.append(trimmed[i])
    }
    return builder.toString()
}

class NanpVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val formatted = formatPhoneNumber(text.text)
        return TransformedText(AnnotatedString(formatted), phoneNumberOffsetTranslator)
    }

    private val phoneNumberOffsetTranslator = object : OffsetMapping {
        override fun originalToTransformed(offset: Int): Int {
            return when {
                offset <= 0 -> 0
                offset <= 3 -> offset + 1
                offset <= 6 -> offset + 3
                offset <= 10 -> offset + 4
                else -> 14
            }
        }

        override fun transformedToOriginal(offset: Int): Int =
            when {
                offset <= 0 -> 0
                offset <= 5 -> offset - 1
                offset <= 10 -> offset - 3
                offset <= 14 -> offset - 4
                else -> 10
            }
    }
}