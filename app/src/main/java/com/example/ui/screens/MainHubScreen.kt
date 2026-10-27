package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.DatabaseConnectionTester
import com.example.ui.viewmodel.ProductStats

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainHubScreen(
  userPhone: String,
  productStats: ProductStats,
  totalWarranties: Int,
  expiringWarranties: Int,
  totalSubscriptions: Int = 0,
  totalMonthlyCost: Double = 0.0,
  totalVehicles: Int = 0,
  expiringVehicles: Int = 0,
  activeRemindersCount: Int = 0,
  onOpenEczaTakip: () -> Unit,
  onOpenGarantiTakip: () -> Unit,
  onOpenAbonelikTakip: () -> Unit = {},
  onOpenAracTakip: () -> Unit = {},
  onOpenNotifications: () -> Unit = {},
  onLogout: () -> Unit
) {
  val iosBackground = Color(0xFFF2F2F7)
  val iosLabelPrimary = Color(0xFF1C1C1E)
  val iosLabelSecondary = Color(0xFF8E8E93)

  Scaffold(
    containerColor = iosBackground,
    topBar = {
      TopAppBar(
        title = {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(Color(0xFF007AFF).copy(alpha = 0.14f)),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                Icons.Default.Person,
                contentDescription = null,
                tint = Color(0xFF007AFF),
                modifier = Modifier.size(18.dp)
              )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Text(
                text = "Akıllı Takip Merkezi",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = iosLabelPrimary
              )
              Text(
                text = userPhone,
                fontSize = 11.sp,
                color = iosLabelSecondary
              )
            }
          }
        },
        actions = {
          IconButton(
            onClick = onOpenNotifications,
            modifier = Modifier.testTag("top_notifications_button")
          ) {
            BadgedBox(
              badge = {
                if (activeRemindersCount > 0) {
                  Badge(containerColor = Color(0xFFFF2D55)) {
                    Text(activeRemindersCount.toString(), color = Color.White, fontSize = 10.sp)
                  }
                }
              }
            ) {
              Icon(
                imageVector = Icons.Default.NotificationsActive,
                contentDescription = "Bildirimler",
                tint = Color(0xFFFF2D55)
              )
            }
          }

          TextButton(
            onClick = onLogout,
            modifier = Modifier.testTag("logout_button")
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.Logout,
              contentDescription = "Çıkış Yap",
              modifier = Modifier.size(16.dp),
              tint = Color(0xFFFF3B30)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = "Çıkış Yap",
              color = Color(0xFFFF3B30),
              fontSize = 13.sp,
              fontWeight = FontWeight.SemiBold
            )
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = iosBackground)
      )
    }
  ) { padding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding)
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 18.dp, vertical = 6.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      // Apple iOS Health-style Large Title & Compact DB Status Pill
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "Özet",
          fontSize = 32.sp,
          fontWeight = FontWeight.ExtraBold,
          color = iosLabelPrimary
        )
        DatabaseConnectionTester()
      }

      // Apple-style Notification & Alarm Studio Hero Card
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(22.dp))
          .clickable(onClick = onOpenNotifications)
          .testTag("module_notifications_center"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
      ) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .background(
              Brush.linearGradient(
                colors = listOf(Color(0xFF1C1C1E), Color(0xFF2C2C2E))
              )
            )
            .padding(18.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.weight(1f)
            ) {
              Box(
                modifier = Modifier
                  .size(46.dp)
                  .clip(RoundedCornerShape(14.dp))
                  .background(Color(0xFFFF2D55)),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = Icons.Default.NotificationsActive,
                  contentDescription = null,
                  tint = Color.White,
                  modifier = Modifier.size(24.dp)
                )
              }
              Spacer(modifier = Modifier.width(14.dp))
              Column {
                Text(
                  text = "Bildirim & Alarm Merkezi",
                  color = Color.White,
                  fontWeight = FontWeight.Bold,
                  fontSize = 17.sp
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                  text = "Her ürüne özel 1 Gün, 1 Hafta, 2 Hafta, 1 Ay önceden müzikli ve saatli bildirim kurun",
                  color = Color(0xFFD1D1D6),
                  fontSize = 12.sp,
                  lineHeight = 16.sp
                )
              }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
              contentDescription = "Aç",
              tint = Color(0xFF8E8E93),
              modifier = Modifier.size(16.dp)
            )
          }
        }
      }

      Text(
        text = "Aktif Takip Modülleri",
        fontSize = 19.sp,
        fontWeight = FontWeight.Bold,
        color = iosLabelPrimary
      )

      // Module 1: Ecza & Kozmetik Takip (Apple Teal #00C7BE)
      AppleModuleCard(
        title = "Ecza & Kozmetik Takip",
        subtitle = "İlaç, şurup, güneş kremi ve bakım ürünlerinin İTS karekod, SKT ve PAO takibi",
        icon = Icons.Default.MedicalServices,
        squircleColor = Color(0xFF00C7BE),
        badge1Text = "${productStats.total} Ürün",
        badge2Text = if (productStats.critical > 0) "${productStats.critical} Kritik SKT" else "${productStats.safe} Güvenli",
        badge2Critical = productStats.critical > 0,
        testTag = "module_ecza_takip",
        onClick = onOpenEczaTakip
      )

      // Module 2: Garanti & Fatura Takip (Apple Blue #007AFF)
      AppleModuleCard(
        title = "Garanti & Fatura Takip",
        subtitle = "Elektronik, beyaz eşya ve ev cihazlarının garanti bitiş ve fatura takibi",
        icon = Icons.Default.VerifiedUser,
        squircleColor = Color(0xFF007AFF),
        badge1Text = "$totalWarranties Cihaz",
        badge2Text = if (expiringWarranties > 0) "$expiringWarranties Yaklaşan" else "Tümü Aktif",
        badge2Critical = expiringWarranties > 0,
        testTag = "module_garanti_takip",
        onClick = onOpenGarantiTakip
      )

      // Module 3: Abonelik & Fatura Takip (Apple Indigo #5856D6)
      AppleModuleCard(
        title = "Abonelik & Fatura Takip",
        subtitle = "Dijital platformlar, internet ve aylık/yıllık düzenli ödemelerin takibi",
        icon = Icons.Default.Autorenew,
        squircleColor = Color(0xFF5856D6),
        badge1Text = "$totalSubscriptions Abonelik",
        badge2Text = "₺${"%.0f".format(totalMonthlyCost)} / Ay",
        badge2Critical = false,
        testTag = "module_abonelik_takip",
        onClick = onOpenAbonelikTakip
      )

      // Module 4: Araç & Sigorta Takip (Apple Orange #FF9500)
      AppleModuleCard(
        title = "Araç & Sigorta Takip",
        subtitle = "Trafik sigortası, kasko, TÜVTÜRK muayene ve periyodik bakım takibi",
        icon = Icons.Default.DirectionsCar,
        squircleColor = Color(0xFFFF9500),
        badge1Text = "$totalVehicles Kayıt",
        badge2Text = if (expiringVehicles > 0) "$expiringVehicles Yaklaşan" else "Tümü Güncel",
        badge2Critical = expiringVehicles > 0,
        testTag = "module_arac_takip",
        onClick = onOpenAracTakip
      )

      Spacer(modifier = Modifier.height(16.dp))
    }
  }
}

@Composable
private fun AppleModuleCard(
  title: String,
  subtitle: String,
  icon: ImageVector,
  squircleColor: Color,
  badge1Text: String,
  badge2Text: String,
  badge2Critical: Boolean,
  testTag: String,
  onClick: () -> Unit
) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(22.dp))
      .clickable(onClick = onClick)
      .testTag(testTag),
    shape = RoundedCornerShape(22.dp),
    colors = CardDefaults.cardColors(containerColor = Color.White),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
  ) {
    Column(modifier = Modifier.padding(18.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.weight(1f)
        ) {
          // Apple iOS Squircle Icon Badge
          Box(
            modifier = Modifier
              .size(48.dp)
              .clip(RoundedCornerShape(14.dp))
              .background(squircleColor),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = icon,
              contentDescription = null,
              tint = Color.White,
              modifier = Modifier.size(25.dp)
            )
          }
          Spacer(modifier = Modifier.width(14.dp))
          Column {
            Text(
              text = title,
              fontSize = 17.sp,
              fontWeight = FontWeight.Bold,
              color = Color(0xFF1C1C1E)
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
              text = subtitle,
              fontSize = 12.sp,
              color = Color(0xFF8E8E93),
              lineHeight = 16.sp
            )
          }
        }
        Spacer(modifier = Modifier.width(8.dp))
        Icon(
          imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
          contentDescription = "Aç",
          tint = Color(0xFFC7C7CC),
          modifier = Modifier.size(16.dp)
        )
      }

      Spacer(modifier = Modifier.height(14.dp))
      HorizontalDivider(color = Color(0xFFF2F2F7), thickness = 1.dp)
      Spacer(modifier = Modifier.height(10.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Surface(
          color = squircleColor.copy(alpha = 0.12f),
          shape = RoundedCornerShape(10.dp)
        ) {
          Text(
            text = badge1Text,
            color = squircleColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
          )
        }

        Surface(
          color = if (badge2Critical) Color(0xFFFFE5E5) else Color(0xFFE8F8EE),
          shape = RoundedCornerShape(10.dp)
        ) {
          Text(
            text = badge2Text,
            color = if (badge2Critical) Color(0xFFFF3B30) else Color(0xFF28CD41),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
          )
        }
      }
    }
  }
}
