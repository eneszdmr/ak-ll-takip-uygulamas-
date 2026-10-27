package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.remote.MariaDBRepository
import com.example.utils.NetworkMonitor
import kotlinx.coroutines.launch

@Composable
fun DatabaseConnectionTester(
  repository: MariaDBRepository = remember { MariaDBRepository() },
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val hasInternet by NetworkMonitor.observeConnectivity(context).collectAsState(
    initial = NetworkMonitor.isInternetAvailable(context)
  )
  var connectionStatus by remember { mutableStateOf(ConnectionState.Testing) }
  val scope = rememberCoroutineScope()

  fun runTest() {
    if (!NetworkMonitor.isInternetAvailable(context)) {
      connectionStatus = ConnectionState.Failed
      return
    }
    connectionStatus = ConnectionState.Testing
    scope.launch {
      val (success, _) = repository.testDbQueryDetailed()
      connectionStatus = if (success) ConnectionState.Connected else ConnectionState.Failed
    }
  }

  LaunchedEffect(hasInternet) {
    if (hasInternet) {
      runTest()
    } else {
      connectionStatus = ConnectionState.Failed
    }
  }

  if (!hasInternet) {
    // Prominent warning when user's internet connection is disabled
    Card(
      modifier = modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 6.dp)
        .testTag("offline_warning_banner"),
      shape = RoundedCornerShape(12.dp),
      colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2))
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Icon(
          imageVector = Icons.Default.WifiOff,
          contentDescription = "İnternet Yok",
          tint = Color(0xFFDC2626),
          modifier = Modifier.size(22.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "İnternet Bağlantınız Kapalı",
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            color = Color(0xFF991B1B)
          )
          Text(
            text = "İnternet bağlantısı olmadan bilgileriniz sunucudan gelemez ve kaydedilemez.",
            fontSize = 11.sp,
            color = Color(0xFFB91C1C)
          )
        }
        IconButton(
          onClick = { runTest() },
          modifier = Modifier.size(36.dp)
        ) {
          Icon(
            imageVector = Icons.Default.Refresh,
            contentDescription = "Yeniden Dene",
            tint = Color(0xFF991B1B),
            modifier = Modifier.size(18.dp)
          )
        }
      }
    }
  } else {
    // Subtle, compact pill badge without database name or IP address
    val bgColor = when (connectionStatus) {
      ConnectionState.Connected -> Color(0xFFECFDF5)
      ConnectionState.Failed -> Color(0xFFFEF2F2)
      ConnectionState.Testing -> Color(0xFFFFFBEB)
    }
    val borderColor = when (connectionStatus) {
      ConnectionState.Connected -> Color(0xFFA7F3D0)
      ConnectionState.Failed -> Color(0xFFFECACA)
      ConnectionState.Testing -> Color(0xFFFDE68A)
    }
    val textColor = when (connectionStatus) {
      ConnectionState.Connected -> Color(0xFF065F46)
      ConnectionState.Failed -> Color(0xFF991B1B)
      ConnectionState.Testing -> Color(0xFF92400E)
    }

    Row(
      modifier = modifier
        .clip(CircleShape)
        .background(bgColor)
        .border(1.dp, borderColor, CircleShape)
        .clickable { runTest() }
        .padding(horizontal = 10.dp, vertical = 4.dp)
        .testTag("db_connection_tester"),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
      when (connectionStatus) {
        ConnectionState.Connected -> Icon(
          imageVector = Icons.Default.CheckCircle,
          contentDescription = "Bağlantı Başarılı",
          tint = Color(0xFF059669),
          modifier = Modifier.size(14.dp)
        )
        ConnectionState.Failed -> Icon(
          imageVector = Icons.Default.ErrorOutline,
          contentDescription = "Bağlantı Hatası",
          tint = Color(0xFFDC2626),
          modifier = Modifier.size(14.dp)
        )
        ConnectionState.Testing -> CircularProgressIndicator(
          modifier = Modifier.size(12.dp),
          strokeWidth = 1.5.dp,
          color = Color(0xFFD97706)
        )
      }

      Text(
        text = when (connectionStatus) {
          ConnectionState.Connected -> "Veritabanı bağlantısı başarılı"
          ConnectionState.Failed -> "Veritabanı bağlantısı yok (Yenile)"
          ConnectionState.Testing -> "Bağlantı kontrol ediliyor..."
        },
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        color = textColor
      )
    }
  }
}

enum class ConnectionState {
  Testing, Connected, Failed
}
