package com.example.ui.components

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

/**
 * Masks a 10-digit Turkish phone number (without leading 0) as:
 * (507) 123 12 12
 *
 * Raw text length: 0..10 digits
 * Formatted text indices:
 * raw 0 -> '(' at 0, digit 0 at 1
 * raw 1 -> digit 1 at 2
 * raw 2 -> digit 2 at 3, ') ' at 4..5
 * raw 3 -> digit 3 at 6
 * raw 4 -> digit 4 at 7
 * raw 5 -> digit 5 at 8, ' ' at 9
 * raw 6 -> digit 6 at 10
 * raw 7 -> digit 7 at 11, ' ' at 12
 * raw 8 -> digit 8 at 13
 * raw 9 -> digit 9 at 14
 */
class TurkishPhoneVisualTransformation : VisualTransformation {
  override fun filter(text: AnnotatedString): TransformedText {
    val trimmed = text.text.take(10)
    val out = StringBuilder()

    for (i in trimmed.indices) {
      if (i == 0) out.append("(")
      out.append(trimmed[i])
      if (i == 2) out.append(") ")
      if (i == 5 || i == 7) out.append(" ")
    }

    val offsetMapping = object : OffsetMapping {
      override fun originalToTransformed(offset: Int): Int {
        val clamped = offset.coerceIn(0, trimmed.length)
        if (clamped == 0) return 0
        return when {
          clamped <= 2 -> clamped + 1 // after '('
          clamped <= 5 -> clamped + 3 // after '(' and ') '
          clamped <= 7 -> clamped + 4 // after '(', ') ', and first ' '
          else -> clamped + 5         // after second ' '
        }.coerceAtMost(out.length)
      }

      override fun transformedToOriginal(offset: Int): Int {
        val clamped = offset.coerceIn(0, out.length)
        return when {
          clamped <= 1 -> 0
          clamped <= 4 -> clamped - 1
          clamped <= 5 -> 3
          clamped <= 9 -> clamped - 3
          clamped <= 12 -> clamped - 4
          else -> clamped - 5
        }.coerceIn(0, trimmed.length)
      }
    }

    return TransformedText(AnnotatedString(out.toString()), offsetMapping)
  }
}
