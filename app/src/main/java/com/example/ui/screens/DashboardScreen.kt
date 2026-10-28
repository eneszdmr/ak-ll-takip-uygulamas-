package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ProductEntity
import com.example.ui.components.*
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
  isInitialLoading: Boolean = false,
  isSyncing: Boolean = false,
  onRefresh: () -> Unit = {},
  onBackToHub: (() -> Unit)? = null,
  onLogout: (() -> Unit)? = null
) {
  var productPendingDelete by remember { mutableStateOf<ProductEntity?>(null) }

  val categories = listOf("Tümü", "İlaç", "Şurup", "Güneş Kremi", "Cilt Bakımı", "Vitamin", "Diğer")
  val familyMembers = listOf("Tümü", "Anne", "Baba", "Çocuk 1", "Ortak")

  if (productPendingDelete != null) {
    DeleteConfirmationDialog(
      itemName = productPendingDelete!!.name,
      onConfirm = {
        onDelete(productPendingDelete!!)
        productPendingDelete = null
      },
      onDismiss = { productPendingDelete = null }
    )
  }

  Scaffold(
    containerColor = Color(0xFFF2F2F7),
    topBar = {
      TopAppBar(
        navigationIcon = {
          if (onBackToHub != null) {
            IconButton(
              onClick = onBackToHub,
              modifier = Modifier.semantics { contentDescription = "Ana Menüye Dön" }
            ) {
              Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Ana Menü")
            }
          }
        },
        title = {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF00C7BE).copy(alpha = 0.16f)),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                Icons.Default.MedicalServices,
                contentDescription = null,
                tint = Color(0xFF0D9488),
                modifier = Modifier.size(20.dp)
              )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
              "Ecza & Kozmetik Takip",
              fontWeight = FontWeight.Bold,
              fontSize = 18.sp,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis
            )
          }
        },
        actions = {
          IconButton(
            onClick = onRefresh,
            modifier = Modifier
              .pressClickEffect()
              .semantics { contentDescription = "Ürünleri Yenile" }
          ) {
            if (isSyncing) {
              CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = Color(0xFF0D9488)
              )
            } else {
              Icon(
                imageVector = Icons.Default.Sync,
                contentDescription = "Ürünleri Yenile",
                tint = Color(0xFF0D9488)
              )
            }
          }
          if (onLogout != null) {
            IconButton(
              onClick = onLogout,
              modifier = Modifier.semantics { contentDescription = "Çıkış Yap" }
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.Logout,
                contentDescription = "Çıkış Yap",
                tint = MaterialTheme.colorScheme.error
              )
            }
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFF2F2F7))
      )
    },
    floatingActionButton = {
      ExtendedFloatingActionButton(
        onClick = onAddProductClicked,
        icon = { Icon(Icons.Default.Add, contentDescription = null) },
        text = { Text("Yeni Ürün Ekle", fontWeight = FontWeight.Bold) },
        containerColor = Color(0xFF0D9488),
        contentColor = Color.White,
        modifier = Modifier
          .pressClickEffect()
          .testTag("fab_add_product")
          .semantics { contentDescription = "Yeni Ürün Ekle" }
      )
    }
  ) { padding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(padding),
      contentAlignment = Alignment.TopCenter
    ) {
      LazyColumn(
        modifier = Modifier
          .widthIn(max = 680.dp)
          .fillMaxSize(),
        contentPadding = PaddingValues(bottom = 92.dp)
      ) {
        // Compact Database Connection Status Pill
        item {
          Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
            DatabaseConnectionTester()
          }
        }

        // Full-height, non-clipped Search Bar in Body
        item {
          Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            AppleSearchBar(
              query = searchQuery,
              onQueryChange = onSearchQueryChanged,
              placeholder = "Ürün adı, kategori, konum veya not ara...",
              testTag = "search_input"
            )
          }
        }

        // Summary Statistics Cards
        item {
          StatsOverviewSection(stats)
        }

        // Category Chips Filter
        item {
          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = "Kategoriler",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = Color(0xFF1C1C1E),
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
            color = Color(0xFF1C1C1E),
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
          Spacer(modifier = Modifier.height(6.dp))
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
              fontSize = 16.sp,
              color = Color(0xFF1C1C1E)
            )
          }
        }

        // Skeleton Loading, Empty State, or Product Cards
        if (isInitialLoading && products.isEmpty()) {
          item {
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
              SkeletonLoadingList(count = 3)
            }
          }
        } else if (products.isEmpty()) {
          item {
            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
              val hasActiveFilter = searchQuery.isNotBlank() ||
                selectedCategory != "Tümü" ||
                selectedFamilyMember != "Tümü"
              EmptyStateCard(
                icon = Icons.Default.MedicalServices,
                title = if (hasActiveFilter) "Aramanızla Eşleşen Ürün Bulunamadı" else "Henüz Kayıtlı Ürün Yok",
                subtitle = if (hasActiveFilter) {
                  "Arama terimini veya seçili kategori filtrelerini temizleyerek tüm ürünleri görebilirsiniz."
                } else {
                  "İlaç, şurup, güneş kremi veya bakım ürünlerinizi İTS karekod ve SKT tarayıcı ile hemen ekleyin."
                },
                accentColor = Color(0xFF0D9488),
                actionLabel = if (hasActiveFilter) "Filtreleri Temizle" else "İlk Ürünü Ekle",
                onActionClick = {
                  if (hasActiveFilter) {
                    onSearchQueryChanged("")
                    onCategorySelected("Tümü")
                    onFamilyMemberSelected("Tümü")
                  } else {
                    onAddProductClicked()
                  }
                }
              )
            }
          }
        } else {
          items(products, key = { it.id }) { product ->
            ProductCard(
              product = product,
              onOpenToday = onOpenToday,
              onEdit = onEdit,
              onDelete = { productPendingDelete = it }
            )
          }
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
      .padding(horizontal = 16.dp, vertical = 6.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    StatCard(
      title = "Toplam",
      count = stats.total.toString(),
      color = Color(0xFF0D9488),
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
fun StatCard(
  title: String,
  count: String,
  color: Color,
  modifier: Modifier = Modifier
) {
  Card(
    modifier = modifier.semantics { contentDescription = "$title: $count ürün" },
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = Color.White),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 12.dp, horizontal = 6.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Text(
        text = count,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 20.sp,
        color = color,
        maxLines = 1
      )
      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text = title,
        fontSize = 11.sp,
        color = Color(0xFF6B7280),
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
    }
  }
}
