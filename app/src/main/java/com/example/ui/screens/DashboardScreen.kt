package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ProductEntity
import com.example.ui.components.DatabaseConnectionTester
import com.example.ui.components.ProductCard
import com.example.ui.theme.*
import com.example.ui.viewmodel.ProductStats

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
  products: List<ProductEntity>,
  stats: ProductStats,
  searchQuery: String,
  onSearchQueryChanged: (String) -> Unit,
  selectedCategory: String,
  onCategorySelected: (String) -> Unit,
  selectedFamilyMember: String,
  onFamilyMemberSelected: (String) -> Unit,
  onOpenToday: (ProductEntity) -> Unit,
  onEdit: (ProductEntity) -> Unit,
  onDelete: (ProductEntity) -> Unit,
  onAddProductClicked: () -> Unit,
  onBackToHub: (() -> Unit)? = null,
  onLogout: (() -> Unit)? = null
) {
  var isSearchActive by remember { mutableStateOf(false) }

  val categories = listOf("Tümü", "İlaç", "Şurup", "Güneş Kremi", "Cilt Bakımı", "Vitamin", "Diğer")
  val familyMembers = listOf("Tümü", "Anne", "Baba", "Çocuk 1", "Ortak")

  Scaffold(
    topBar = {
      TopAppBar(
        navigationIcon = {
          if (onBackToHub != null && !isSearchActive) {
            IconButton(onClick = onBackToHub) {
              Icon(Icons.Default.ArrowBack, contentDescription = "Ana Menü")
            }
          }
        },
        title = {
          if (isSearchActive) {
            OutlinedTextField(
              value = searchQuery,
              onValueChange = onSearchQueryChanged,
              placeholder = { Text("Ürün veya semptom ara...") },
              modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("search_input"),
              singleLine = true,
              colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
              )
            )
          } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Box(
                modifier = Modifier
                  .size(36.dp)
                  .clip(CircleShape)
                  .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
              ) {
                Icon(Icons.Default.MedicalServices, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
              }
              Spacer(modifier = Modifier.width(10.dp))
              Text("EczaTakip", fontWeight = FontWeight.Bold, fontSize = 20.sp)
            }
          }
        },
        actions = {
          IconButton(onClick = {
            isSearchActive = !isSearchActive
            if (!isSearchActive) onSearchQueryChanged("")
          }) {
            Icon(
              imageVector = if (isSearchActive) Icons.Default.Close else Icons.Default.Search,
              contentDescription = "Arama"
            )
          }
          if (onLogout != null && !isSearchActive) {
            IconButton(onClick = onLogout) {
              Icon(
                imageVector = Icons.Default.Logout,
                contentDescription = "Çıkış Yap",
                tint = MaterialTheme.colorScheme.error
              )
            }
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
      )
    },
    floatingActionButton = {
      FloatingActionButton(
        onClick = onAddProductClicked,
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        modifier = Modifier.testTag("fab_add_product")
      ) {
        Icon(Icons.Default.Add, contentDescription = "Yeni Ürün Ekle")
      }
    }
  ) { padding ->
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding)
        .background(MaterialTheme.colorScheme.background),
      contentPadding = PaddingValues(bottom = 80.dp)
    ) {
      // Compact Database Connection Status Pill
      item {
        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
          DatabaseConnectionTester()
        }
      }

      // Summary Statistics Cards
      item {
        StatsOverviewSection(stats)
      }

      // Category Chips Filter
      item {
        Spacer(modifier = Modifier.height(12.dp))
        Text(
          text = "Kategoriler",
          fontWeight = FontWeight.Bold,
          fontSize = 14.sp,
          modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(6.dp))
        LazyRow(
          contentPadding = PaddingValues(horizontal = 16.dp),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          items(categories) { category ->
            FilterChip(
              selected = selectedCategory == category,
              onClick = { onCategorySelected(category) },
              label = { Text(category) }
            )
          }
        }
      }

      // Family Member Chips Filter
      item {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
          text = "Aile Üyesi",
          fontWeight = FontWeight.Bold,
          fontSize = 14.sp,
          modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(6.dp))
        LazyRow(
          contentPadding = PaddingValues(horizontal = 16.dp),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          items(familyMembers) { member ->
            FilterChip(
              selected = selectedFamilyMember == member,
              onClick = { onFamilyMemberSelected(member) },
              label = { Text(member) }
            )
          }
        }
        Spacer(modifier = Modifier.height(8.dp))
      }

      // Products Header
      item {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "Takip Edilen Ürünler (${products.size})",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
          )
        }
      }

      // Products List or Empty State
      if (products.isEmpty()) {
        item {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .padding(40.dp),
            contentAlignment = Alignment.Center
          ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Icon(
                Icons.Default.Inbox,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.outline
              )
              Spacer(modifier = Modifier.height(12.dp))
              Text(
                text = "Kayıtlı ürün bulunamadı",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
              )
            }
          }
        }
      } else {
        items(products, key = { it.id }) { product ->
          ProductCard(
            product = product,
            onOpenToday = onOpenToday,
            onEdit = onEdit,
            onDelete = onDelete
          )
        }
      }
    }
  }
}

@Composable
fun StatsOverviewSection(stats: ProductStats) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 8.dp),
    horizontalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    StatCard(
      title = "Toplam",
      count = stats.total.toString(),
      color = MaterialTheme.colorScheme.primary,
      modifier = Modifier.weight(1f)
    )
    StatCard(
      title = "Kritik",
      count = stats.critical.toString(),
      color = DangerRedText,
      modifier = Modifier.weight(1f)
    )
    StatCard(
      title = "Yaklaşan",
      count = stats.warning.toString(),
      color = WarningAmberText,
      modifier = Modifier.weight(1f)
    )
    StatCard(
      title = "Güvenli",
      count = stats.safe.toString(),
      color = SafeGreenText,
      modifier = Modifier.weight(1f)
    )
  }
}

@Composable
fun StatCard(title: String, count: String, color: androidx.compose.ui.graphics.Color, modifier: Modifier = Modifier) {
  Card(
    modifier = modifier,
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 12.dp, horizontal = 8.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Text(
        text = count,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        color = color
      )
      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text = title,
        fontSize = 11.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.Medium
      )
    }
  }
}
