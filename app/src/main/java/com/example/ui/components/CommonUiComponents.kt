package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Framer-Motion style spring scale press effect for buttons and interactive cards.
 */
fun Modifier.pressClickEffect(pressedScale: Float = 0.96f): Modifier = composed {
  var isPressed by remember { mutableStateOf(false) }
  val scale by animateFloatAsState(
    targetValue = if (isPressed) pressedScale else 1f,
    animationSpec = spring(
      dampingRatio = Spring.DampingRatioMediumBouncy,
      stiffness = Spring.StiffnessMediumLow
    ),
    label = "press_scale"
  )

  this
    .graphicsLayer {
      scaleX = scale
      scaleY = scale
    }
    .pointerInput(Unit) {
      while (true) {
        awaitPointerEventScope {
          awaitFirstDown(requireUnconsumed = false)
          isPressed = true
          waitForUpOrCancellation()
          isPressed = false
        }
      }
    }
}

/**
 * Delete confirmation modal dialog used across all modules before deleting any record.
 */
@Composable
fun DeleteConfirmationDialog(
  itemName: String,
  onConfirm: () -> Unit,
  onDismiss: () -> Unit
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    icon = {
      Box(
        modifier = Modifier
          .size(48.dp)
          .clip(CircleShape)
          .background(Color(0xFFFEE2E2)),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = Icons.Default.DeleteForever,
          contentDescription = "Silme Uyarısı",
          tint = Color(0xFFDC2626),
          modifier = Modifier.size(26.dp)
        )
      }
    },
    title = {
      Text(
        text = "Kaydı Silmek İstiyor musunuz?",
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        textAlign = TextAlign.Center
      )
    },
    text = {
      Text(
        text = "\"$itemName\" kaydı kalıcı olarak silinecektir. Bu işlem geri alınamaz.",
        fontSize = 14.sp,
        color = Color(0xFF4B5563),
        textAlign = TextAlign.Center
      )
    },
    confirmButton = {
      Button(
        onClick = onConfirm,
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.testTag("confirm_delete_button")
      ) {
        Text("Evet, Sil", color = Color.White, fontWeight = FontWeight.Bold)
      }
    },
    dismissButton = {
      OutlinedButton(
        onClick = onDismiss,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.testTag("cancel_delete_button")
      ) {
        Text("Vazgeç", fontWeight = FontWeight.SemiBold)
      }
    },
    shape = RoundedCornerShape(22.dp),
    containerColor = Color.White
  )
}

/**
 * Full-height, non-clipped search input box with clear button and accessibility labels.
 */
@Composable
fun AppleSearchBar(
  query: String,
  onQueryChange: (String) -> Unit,
  placeholder: String,
  modifier: Modifier = Modifier,
  testTag: String = "search_input"
) {
  val focusManager = LocalFocusManager.current
  OutlinedTextField(
    value = query,
    onValueChange = onQueryChange,
    placeholder = {
      Text(
        text = placeholder,
        fontSize = 14.sp,
        color = Color(0xFF8E8E93)
      )
    },
    leadingIcon = {
      Icon(
        imageVector = Icons.Default.Search,
        contentDescription = "Ara",
        tint = Color(0xFF8E8E93)
      )
    },
    trailingIcon = {
      if (query.isNotEmpty()) {
        IconButton(
          onClick = { onQueryChange("") },
          modifier = Modifier.semantics { contentDescription = "Aramayı Temizle" }
        ) {
          Icon(
            imageVector = Icons.Default.Close,
            contentDescription = "Aramayı Temizle",
            tint = Color(0xFF8E8E93)
          )
        }
      }
    },
    textStyle = TextStyle(
      fontSize = 15.sp,
      lineHeight = 20.sp,
      color = Color(0xFF1C1C1E),
      fontWeight = FontWeight.Normal
    ),
    singleLine = true,
    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
    keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
    shape = RoundedCornerShape(14.dp),
    colors = OutlinedTextFieldDefaults.colors(
      focusedContainerColor = Color.White,
      unfocusedContainerColor = Color.White,
      focusedBorderColor = Color(0xFF007AFF),
      unfocusedBorderColor = Color(0xFFE5E5EA),
      focusedTextColor = Color(0xFF1C1C1E),
      unfocusedTextColor = Color(0xFF1C1C1E)
    ),
    modifier = modifier
      .fillMaxWidth()
      .defaultMinSize(minHeight = 54.dp)
      .testTag(testTag)
      .semantics { contentDescription = placeholder }
  )
}

/**
 * Shimmering skeleton loading cards displayed while initial data is loading from MariaDB.
 */
@Composable
fun SkeletonLoadingList(
  count: Int = 3,
  modifier: Modifier = Modifier
) {
  val transition = rememberInfiniteTransition(label = "skeleton_shimmer")
  val alpha by transition.animateFloat(
    initialValue = 0.35f,
    targetValue = 0.85f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 850, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "skeleton_alpha"
  )

  Column(
    modifier = modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    repeat(count) {
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .semantics { contentDescription = "Yükleniyor" },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
          ) {
            Box(
              modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFE5E5EA).copy(alpha = alpha))
            )
            Column(modifier = Modifier.weight(1f)) {
              Box(
                modifier = Modifier
                  .fillMaxWidth(0.6f)
                  .height(16.dp)
                  .clip(RoundedCornerShape(6.dp))
                  .background(Color(0xFFE5E5EA).copy(alpha = alpha))
              )
              Spacer(modifier = Modifier.height(8.dp))
              Box(
                modifier = Modifier
                  .fillMaxWidth(0.4f)
                  .height(12.dp)
                  .clip(RoundedCornerShape(6.dp))
                  .background(Color(0xFFE5E5EA).copy(alpha = alpha))
              )
            }
            Box(
              modifier = Modifier
                .width(72.dp)
                .height(26.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFFE5E5EA).copy(alpha = alpha))
            )
          }
          Spacer(modifier = Modifier.height(14.dp))
          Box(
            modifier = Modifier
              .fillMaxWidth(0.85f)
              .height(12.dp)
              .clip(RoundedCornerShape(6.dp))
              .background(Color(0xFFE5E5EA).copy(alpha = alpha))
          )
        }
      }
    }
  }
}

/**
 * Consistent, accessible empty-state card with optional primary action button.
 */
@Composable
fun EmptyStateCard(
  icon: ImageVector,
  title: String,
  subtitle: String,
  accentColor: Color,
  actionLabel: String? = null,
  onActionClick: (() -> Unit)? = null
) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .semantics { contentDescription = "$title. $subtitle" },
    shape = RoundedCornerShape(22.dp),
    colors = CardDefaults.cardColors(containerColor = Color.White),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 24.dp, vertical = 32.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Box(
        modifier = Modifier
          .size(68.dp)
          .clip(RoundedCornerShape(20.dp))
          .background(accentColor.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = icon,
          contentDescription = title,
          tint = accentColor,
          modifier = Modifier.size(34.dp)
        )
      }
      Spacer(modifier = Modifier.height(14.dp))
      Text(
        text = title,
        fontWeight = FontWeight.Bold,
        fontSize = 17.sp,
        color = Color(0xFF1C1C1E),
        textAlign = TextAlign.Center
      )
      Spacer(modifier = Modifier.height(6.dp))
      Text(
        text = subtitle,
        fontSize = 13.sp,
        color = Color(0xFF8E8E93),
        textAlign = TextAlign.Center,
        lineHeight = 18.sp
      )
      if (actionLabel != null && onActionClick != null) {
        Spacer(modifier = Modifier.height(16.dp))
        Button(
          onClick = onActionClick,
          colors = ButtonDefaults.buttonColors(containerColor = accentColor),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.pressClickEffect()
        ) {
          Text(actionLabel, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
      }
    }
  }
}
