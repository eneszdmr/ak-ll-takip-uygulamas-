package com.example.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerField(
  value: String,
  onValueChange: (String) -> Unit,
  label: String,
  modifier: Modifier = Modifier,
  inputTestTag: String? = null
) {
  var showDialog by remember { mutableStateOf(false) }

  OutlinedTextField(
    value = value,
    onValueChange = onValueChange,
    label = { Text(label) },
    modifier = modifier
      .fillMaxWidth()
      .let { if (inputTestTag != null) it.testTag(inputTestTag) else it },
    singleLine = true,
    shape = RoundedCornerShape(14.dp),
    trailingIcon = {
      IconButton(onClick = { showDialog = true }) {
        Icon(
          imageVector = Icons.Default.CalendarMonth,
          contentDescription = "Takvimi Aç",
          tint = MaterialTheme.colorScheme.primary
        )
      }
    }
  )

  if (showDialog) {
    val initialMillis = remember(value) {
      try {
        LocalDate.parse(value.trim())
          .atStartOfDay(ZoneOffset.UTC)
          .toInstant()
          .toEpochMilli()
      } catch (_: Exception) {
        LocalDate.now()
          .atStartOfDay(ZoneOffset.UTC)
          .toInstant()
          .toEpochMilli()
      }
    }

    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)

    DatePickerDialog(
      onDismissRequest = { showDialog = false },
      confirmButton = {
        TextButton(
          onClick = {
            datePickerState.selectedDateMillis?.let { millis ->
              val selectedDate = Instant.ofEpochMilli(millis)
                .atZone(ZoneOffset.UTC)
                .toLocalDate()
                .toString()
              onValueChange(selectedDate)
            }
            showDialog = false
          }
        ) {
          Text("Tamam")
        }
      },
      dismissButton = {
        TextButton(onClick = { showDialog = false }) {
          Text("İptal")
        }
      }
    ) {
      DatePicker(state = datePickerState)
    }
  }
}
