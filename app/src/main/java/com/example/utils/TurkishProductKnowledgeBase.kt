package com.example.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.Locale
import java.util.concurrent.TimeUnit

data class ScannedProductInfo(
  val name: String,
  val category: String,
  val whatItDoes: String,
  val expiryDate: String? = null,
  val batchNumber: String? = null,
  val serialNumber: String? = null,
  val gtin: String? = null,
  val suggestedPaoMonths: Int? = null
)

object TurkishProductKnowledgeBase {

  private val httpClient by lazy {
    OkHttpClient.Builder()
      .connectTimeout(7, TimeUnit.SECONDS)
      .readTimeout(7, TimeUnit.SECONDS)
      .followRedirects(true)
      .build()
  }

  private data class CatalogItem(
    val gtin13: String,
    val name: String,
    val category: String,
    val whatItDoes: String,
    val paoMonths: Int? = null
  )

  private val catalog = listOf(
    CatalogItem(
      gtin13 = "8699508090014",
      name = "Parol 500 mg 20 Tablet",
      category = "İlaç",
      whatItDoes = "Ağrı kesici (analjezik) ve ateş düşürücü (antipiretik). Baş, diş, kas ağrıları ve soğuk algınlığı ateşinde kullanılır."
    ),
    CatalogItem(
      gtin13 = "8699525090010",
      name = "Augmentin BID 1000 mg 14 Film Tablet",
      category = "İlaç",
      whatItDoes = "Geniş spektrumlu antibiyotik (Amoksisilin + Klavulanik Asit). Üst ve alt solunum yolu, idrar yolu ve cilt enfeksiyonlarında kullanılır."
    ),
    CatalogItem(
      gtin13 = "8699514090015",
      name = "Majezik 100 mg 15 Film Tablet",
      category = "İlaç",
      whatItDoes = "Non-steroid antiinflamatuar (Flurbiprofen) ağrı kesici ve iltihap giderici. Eklem, kas, diş ve adet ağrılarında kullanılır."
    ),
    CatalogItem(
      gtin13 = "8699828090011",
      name = "Arveles 25 mg 20 Film Tablet",
      category = "İlaç",
      whatItDoes = "Hızlı etkili ağrı kesici (Deksketoprofen). Kas-iskelet sistemi ağrıları, diş ağrısı ve dismenore tedavisinde kullanılır."
    ),
    CatalogItem(
      gtin13 = "8699546010011",
      name = "Nurofen Cold & Flu 200 mg/30 mg 24 Tablet",
      category = "İlaç",
      whatItDoes = "Grip ve soğuk algınlığı ilacı (İbuprofen + Psödoefedrin). Burun tıkanıklığı, baş ağrısı, ateş ve vücut kırgınlığını giderir."
    ),
    CatalogItem(
      gtin13 = "8699578570016",
      name = "Calpol 6 Plus 250 mg/5 ml Süspansiyon",
      category = "Şurup",
      whatItDoes = "Çocuklar için ağrı kesici ve ateş düşürücü şurup (Parasetamol). Açıldıktan sonra 6 ay içinde tüketilmelidir.",
      paoMonths = 6
    ),
    CatalogItem(
      gtin13 = "8699546570010",
      name = "Dolven Pediatrik Şurup 100 mg/5 ml",
      category = "Şurup",
      whatItDoes = "Çocuklarda ateş düşürücü ve ağrı/iltihap giderici şurup (İbuprofen). Açıldıktan sonra 6 ay geçerlidir.",
      paoMonths = 6
    ),
    CatalogItem(
      gtin13 = "8699546350018",
      name = "Bepanthol Cilt Bakım Kremi 100 g",
      category = "Cilt Bakımı",
      whatItDoes = "Dekspantenol (Provitamin B5) içeren günlük nemlendirici ve cilt bariyeri onarıcı bakım kremi.",
      paoMonths = 12
    ),
    CatalogItem(
      gtin13 = "8699516120017",
      name = "Nexium 40 mg 28 Enterik Kaplı Pellet Tablet",
      category = "İlaç",
      whatItDoes = "Mide koruyucu (Esomeprazol - Proton Pompa İnhibitörü). Reflü, gastrit ve mide ülseri tedavisinde mide asidini azaltır."
    ),
    CatalogItem(
      gtin13 = "8699502010018",
      name = "Aerius 5 mg 20 Film Tablet",
      category = "İlaç",
      whatItDoes = "Antihistaminik alerji ilacı (Desloratadin). Alerjik rinit (saman nezlesi), hapşırma, göz kaşıntısı ve ürtikerde kullanılır."
    ),
    CatalogItem(
      gtin13 = "8699546700011",
      name = "Redoxon Üçlü Etki (C + D Vitamini + Çinko)",
      category = "Vitamin",
      whatItDoes = "Bağışıklık sistemini destekleyen C Vitamini, D Vitamini ve Çinko takviye edici efervesan tablet."
    ),
    CatalogItem(
      gtin13 = "8699536090115",
      name = "Apranax Fort 550 mg 10 Film Tablet",
      category = "İlaç",
      whatItDoes = "Naproksen sodyum içeren güçlü ağrı kesici ve iltihap giderici. Migren, diş, eklem ve kas ağrılarında kullanılır."
    ),
    CatalogItem(
      gtin13 = "8699504090018",
      name = "Klamoks BID 1000 mg 10 Film Tablet",
      category = "İlaç",
      whatItDoes = "Amoksisilin ve Klavulanik asit içeren antibiyotik. Bakteriyel solunum yolu, kulak, sinüzit ve idrar yolu enfeksiyonlarında kullanılır."
    ),
    CatalogItem(
      gtin13 = "8699522090013",
      name = "Voltaren SR 75 mg 10 Tablet",
      category = "İlaç",
      whatItDoes = "Diklofenak sodyum içeren ağrı ve iltihap giderici. Romatizmal eklem ağrıları, bel tutulması ve burkulmalarda kullanılır."
    ),
    CatalogItem(
      gtin13 = "8699540010015",
      name = "Aferin Forte 650 mg/4 mg 30 Film Tablet",
      category = "İlaç",
      whatItDoes = "Soğuk algınlığı ve grip ilacı (Parasetamol + Klorfeniramin). Ateş, baş ağrısı, burun akıntısı ve hapşırmayı giderir."
    )
  )

  /**
   * Resolves a barcode / ITS DataMatrix and optional OCR box text by querying both
   * the local catalog AND live internet search so the product name and "what it does"
   * are automatically populated.
   */
  suspend fun resolveBarcodeOrITS(
    rawCode: String,
    companionOcrText: String = ""
  ): ScannedProductInfo = withContext(Dispatchers.IO) {
    val trimmed = rawCode.trim()
    val itsResult = ITSBarcodeParser.parseITSBarcode(trimmed)
    val rawGtin = itsResult.gtin ?: trimmed.filter { it.isDigit() }
    val normalizedGtin13 = when {
      rawGtin.length == 14 && rawGtin.startsWith("0") -> rawGtin.substring(1)
      rawGtin.length == 13 -> rawGtin
      else -> rawGtin
    }

    // 1. Check built-in Turkish catalog
    val localMatch = catalog.firstOrNull {
      it.gtin13 == normalizedGtin13 || (rawGtin.isNotBlank() && rawGtin.endsWith(it.gtin13))
    }

    if (localMatch != null) {
      val lotSuffix = buildLotSuffix(itsResult.batchNumber, itsResult.serialNumber)
      return@withContext ScannedProductInfo(
        name = localMatch.name,
        category = localMatch.category,
        whatItDoes = "${localMatch.whatItDoes}$lotSuffix",
        expiryDate = itsResult.expiryDate ?: OCRParser.parseExpiryFromOCR(companionOcrText),
        batchNumber = itsResult.batchNumber,
        serialNumber = itsResult.serialNumber,
        gtin = normalizedGtin13.ifBlank { rawGtin },
        suggestedPaoMonths = localMatch.paoMonths
      )
    }

    // 2. Query OpenBeautyFacts / OpenFoodFacts / OpenProductsFacts REST APIs
    if (normalizedGtin13.length >= 8) {
      val openFactsInfo = queryOpenFactsDatabases(normalizedGtin13)
      if (openFactsInfo != null && openFactsInfo.name.isNotBlank()) {
        val lotSuffix = buildLotSuffix(itsResult.batchNumber, itsResult.serialNumber)
        return@withContext openFactsInfo.copy(
          whatItDoes = "${openFactsInfo.whatItDoes}$lotSuffix",
          expiryDate = itsResult.expiryDate
            ?: openFactsInfo.expiryDate
            ?: OCRParser.parseExpiryFromOCR(companionOcrText),
          batchNumber = itsResult.batchNumber,
          serialNumber = itsResult.serialNumber,
          gtin = normalizedGtin13
        )
      }
    }

    // 3. Query Live Internet Search (DuckDuckGo HTML search) by Barcode or Box OCR Title
    val ocrCandidate = if (companionOcrText.isNotBlank()) {
      analyzeBoxOcrText(companionOcrText)
    } else null

    val searchQuery = when {
      normalizedGtin13.length >= 8 && ocrCandidate != null && ocrCandidate.name.isNotBlank() ->
        "${ocrCandidate.name} nedir ne işe yarar ilaç"
      normalizedGtin13.length >= 8 ->
        "$normalizedGtin13 barkod ilaç ürün nedir ne işe yarar"
      ocrCandidate != null && ocrCandidate.name.isNotBlank() ->
        "${ocrCandidate.name} nedir ne işe yarar"
      else -> ""
    }

    if (searchQuery.isNotBlank()) {
      val webInfo = searchProductOnInternet(searchQuery, normalizedGtin13)
      if (webInfo != null && webInfo.name.isNotBlank()) {
        val lotSuffix = buildLotSuffix(itsResult.batchNumber, itsResult.serialNumber)
        return@withContext webInfo.copy(
          whatItDoes = "${webInfo.whatItDoes}$lotSuffix",
          expiryDate = itsResult.expiryDate
            ?: ocrCandidate?.expiryDate
            ?: OCRParser.parseExpiryFromOCR(companionOcrText),
          batchNumber = itsResult.batchNumber,
          serialNumber = itsResult.serialNumber,
          gtin = normalizedGtin13.ifBlank { null },
          suggestedPaoMonths = webInfo.suggestedPaoMonths ?: ocrCandidate?.suggestedPaoMonths
        )
      }
    }

    // 4. Fallback to OCR candidate if available
    if (ocrCandidate != null && ocrCandidate.name.isNotBlank()) {
      val lotSuffix = buildLotSuffix(itsResult.batchNumber, itsResult.serialNumber)
      return@withContext ocrCandidate.copy(
        whatItDoes = "${ocrCandidate.whatItDoes}$lotSuffix",
        expiryDate = itsResult.expiryDate ?: ocrCandidate.expiryDate,
        batchNumber = itsResult.batchNumber,
        serialNumber = itsResult.serialNumber,
        gtin = normalizedGtin13.ifBlank { null }
      )
    }

    // 5. Final fallback when barcode is unknown and no OCR text yet
    val isTurkishPharma = normalizedGtin13.startsWith("8699")
    val defaultCategory = if (isTurkishPharma) "İlaç" else "Diğer"
    val lotSuffix = buildLotSuffix(itsResult.batchNumber, itsResult.serialNumber)

    ScannedProductInfo(
      name = if (normalizedGtin13.isNotBlank()) "Barkodlu Ürün ($normalizedGtin13)" else "",
      category = defaultCategory,
      whatItDoes = "İTS/Barkod ($normalizedGtin13) okundu.$lotSuffix Kutu ön yüzündeki ürün ismini okutarak detaylı açıklamayı internetten çekebilirsiniz.",
      expiryDate = itsResult.expiryDate,
      batchNumber = itsResult.batchNumber,
      serialNumber = itsResult.serialNumber,
      gtin = normalizedGtin13.ifBlank { null }
    )
  }

  /**
   * Analyzes box OCR text AND looks up the detected product name on the internet
   * to fetch a rich "what it does" description.
   */
  suspend fun analyzeBoxOcrTextWithInternet(rawText: String): ScannedProductInfo = withContext(Dispatchers.IO) {
    val localAnalysis = analyzeBoxOcrText(rawText)
    if (localAnalysis.name.isNotBlank()) {
      val webEnriched = searchProductOnInternet("${localAnalysis.name} nedir ne işe yarar", "")
      if (webEnriched != null && webEnriched.whatItDoes.isNotBlank()) {
        return@withContext localAnalysis.copy(
          name = if (webEnriched.name.length in 3..55) webEnriched.name else localAnalysis.name,
          category = webEnriched.category.ifBlank { localAnalysis.category },
          whatItDoes = webEnriched.whatItDoes
        )
      }
    }
    localAnalysis
  }

  private fun buildLotSuffix(batch: String?, serial: String?): String {
    if (batch.isNullOrBlank() && serial.isNullOrBlank()) return ""
    return buildString {
      append(" [")
      if (!batch.isNullOrBlank()) append("Parti/Lot: $batch")
      if (!serial.isNullOrBlank()) {
        if (!batch.isNullOrBlank()) append(", ")
        append("Seri: $serial")
      }
      append("]")
    }
  }

  private fun queryOpenFactsDatabases(barcode: String): ScannedProductInfo? {
    val urls = listOf(
      "https://world.openbeautyfacts.org/api/v2/product/$barcode.json",
      "https://world.openfoodfacts.org/api/v2/product/$barcode.json",
      "https://world.openproductsfacts.org/api/v2/product/$barcode.json"
    )
    for (url in urls) {
      try {
        val req = Request.Builder()
          .url(url)
          .header("User-Agent", "EczaTakipAndroid/2.0")
          .get()
          .build()
        httpClient.newCall(req).execute().use { resp ->
          if (!resp.isSuccessful) return@use
          val bodyStr = resp.body?.string() ?: return@use
          val json = JSONObject(bodyStr)
          if (json.optInt("status", 0) == 1) {
            val product = json.optJSONObject("product") ?: return@use
            val productName = product.optString("product_name_tr")
              .ifBlank { product.optString("product_name") }
              .trim()
            val brands = product.optString("brands").trim()
            val genericName = product.optString("generic_name_tr")
              .ifBlank { product.optString("generic_name") }
              .ifBlank { product.optString("categories") }
              .trim()

            if (productName.isNotBlank()) {
              val fullName = if (brands.isNotBlank() && !productName.contains(brands, ignoreCase = true)) {
                "$brands $productName"
              } else {
                productName
              }
              val inferred = analyzeBoxOcrText("$fullName $genericName")
              return ScannedProductInfo(
                name = fullName,
                category = inferred.category,
                whatItDoes = if (genericName.isNotBlank()) {
                  "$genericName. ${inferred.whatItDoes}"
                } else {
                  inferred.whatItDoes
                },
                gtin = barcode,
                suggestedPaoMonths = inferred.suggestedPaoMonths
              )
            }
          }
        }
      } catch (_: Exception) {
      }
    }
    return null
  }

  /**
   * Performs a live web lookup to discover the product name and what it is used for.
   */
  private fun searchProductOnInternet(query: String, barcode: String): ScannedProductInfo? {
    try {
      val encoded = URLEncoder.encode(query, "UTF-8")
      val url = "https://html.duckduckgo.com/html/?q=$encoded"
      val request = Request.Builder()
        .url(url)
        .header(
          "User-Agent",
          "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
        )
        .header("Accept-Language", "tr-TR,tr;q=0.9")
        .get()
        .build()

      httpClient.newCall(request).execute().use { response ->
        if (!response.isSuccessful) return null
        val html = response.body?.string() ?: return null

        val titleRegex = Regex("""class="result__a"[^>]*>(.*?)</a>""", RegexOption.DOT_MATCHES_ALL)
        val snippetRegex = Regex("""class="result__snippet"[^>]*>(.*?)</(?:a|div|span)>""", RegexOption.DOT_MATCHES_ALL)

        val rawTitle = titleRegex.find(html)?.groupValues?.get(1)?.let { stripHtml(it) } ?: ""
        val rawSnippet = snippetRegex.find(html)?.groupValues?.get(1)?.let { stripHtml(it) } ?: ""

        if (rawTitle.isBlank() && rawSnippet.isBlank()) return null

        val cleanedTitle = rawTitle
          .replace(Regex("""(?i)\s*[-|–:]\s*.*$"""), "")
          .replace(Regex("""(?i)\bnedir\b.*$"""), "")
          .replace(Regex("""(?i)\bne işe yarar\b.*$"""), "")
          .replace(Regex("""(?i)\bkullanımı\b.*$"""), "")
          .replace(Regex("""(?i)\bfiyatı\b.*$"""), "")
          .trim()

        val inferred = analyzeBoxOcrText("$cleanedTitle $rawSnippet")
        val finalName = if (cleanedTitle.length in 3..60 && !cleanedTitle.contains("DuckDuckGo", ignoreCase = true)) {
          cleanedTitle
        } else {
          inferred.name
        }

        val finalDescription = if (rawSnippet.length >= 15) {
          rawSnippet.take(260).trim()
        } else {
          inferred.whatItDoes
        }

        if (finalName.isNotBlank()) {
          return ScannedProductInfo(
            name = finalName,
            category = inferred.category,
            whatItDoes = finalDescription,
            gtin = barcode.ifBlank { null },
            suggestedPaoMonths = inferred.suggestedPaoMonths
          )
        }
      }
    } catch (_: Exception) {
    }
    return null
  }

  private fun stripHtml(input: String): String {
    return input
      .replace(Regex("<[^>]*>"), " ")
      .replace("&amp;", "&")
      .replace("&quot;", "\"")
      .replace("&#x27;", "'")
      .replace("&#39;", "'")
      .replace("&nbsp;", " ")
      .replace(Regex("\\s+"), " ")
      .trim()
  }

  fun analyzeBoxOcrText(rawText: String): ScannedProductInfo {
    val cleanText = rawText.trim()
    val upper = cleanText.uppercase(Locale("tr", "TR"))

    val parsedExpiry = OCRParser.parseExpiryFromOCR(cleanText)

    val paoMatch = Regex("""\b(3|6|9|12|18|24)\s*(?:M|AY)\b""").find(upper)
    var paoMonths = paoMatch?.groupValues?.get(1)?.toIntOrNull()

    for (item in catalog) {
      val firstWord = item.name.substringBefore(" ").uppercase(Locale("tr", "TR"))
      if (firstWord.length >= 4 && upper.contains(firstWord)) {
        return ScannedProductInfo(
          name = item.name,
          category = item.category,
          whatItDoes = item.whatItDoes,
          expiryDate = parsedExpiry,
          suggestedPaoMonths = paoMonths ?: item.paoMonths
        )
      }
    }

    val category: String
    val usageSummary: String

    when {
      upper.contains("SPF") || upper.contains("GÜNEŞ") || upper.contains("SUN ") || upper.contains("SUNSCREEN") || upper.contains("ANTHELIOS") -> {
        category = "Güneş Kremi"
        usageSummary = "UVA/UVB ışınlarına karşı cildi koruyan güneş koruyucu bakım ürünü."
        if (paoMonths == null) paoMonths = 12
      }
      upper.contains("ŞURUP") || upper.contains("SÜSPANSİYON") || upper.contains("SUSPANSİYON") || upper.contains("PEDİATRİK") || upper.contains("PEDIATRIK") -> {
        category = "Şurup"
        usageSummary = inferMedicineUsage(upper, defaultText = "Oral kullanım için sıvı şurup/süspansiyon. Açıldıktan sonra PAO süresine dikkat edilmelidir.")
        if (paoMonths == null) paoMonths = 6
      }
      upper.contains("VİTAMİN") || upper.contains("VITAMIN") || upper.contains("OMEGA") || upper.contains("MAGNEZYUM") || upper.contains("TAKVİYE") || upper.contains("MULTİVİTAMİN") || upper.contains("B12") || upper.contains("D3") -> {
        category = "Vitamin"
        usageSummary = "Bağışıklık ve günlük metabolizma desteği sağlayan vitamin/mineral takviye edici ürün."
      }
      upper.contains("NEMLENDİRİCİ") || upper.contains("SERUM") || upper.contains("TEMİZLEME JELİ") || upper.contains("CİLT") || upper.contains("LOSYON") || upper.contains("LOTION") || upper.contains("CERAVE") || upper.contains("LA ROCHE") || upper.contains("BIODERMA") || upper.contains("AVENE") -> {
        category = "Cilt Bakımı"
        usageSummary = "Cilt bariyerini destekleyen, nemlendirici ve onarıcı dermokozmetik cilt bakım ürünü."
        if (paoMonths == null) paoMonths = 12
      }
      else -> {
        category = "İlaç"
        usageSummary = inferMedicineUsage(upper, defaultText = "Kutu üzerinden tarandı. Tedavi ve kullanım dozu için prospektüs bilgisini takip ediniz.")
      }
    }

    val candidateLines = cleanText.lines()
      .map { it.trim() }
      .filter { line ->
        val u = line.uppercase(Locale("tr", "TR"))
        line.length in 3..55 &&
          !u.startsWith("SKT") &&
          !u.startsWith("S.K.T") &&
          !u.startsWith("SON KUL") &&
          !u.startsWith("EXP") &&
          !u.startsWith("PARTİ") &&
          !u.startsWith("SERİ") &&
          !u.startsWith("BARKOD") &&
          !u.contains("REÇETE İLE") &&
          !u.contains("ÇOCUKLARIN GÖREMEYECEĞİ") &&
          line.any { it.isLetter() }
      }

    val dosageLine = candidateLines.firstOrNull { line ->
      Regex("""\b(\d+\s*(?:mg|ml|mcg|g|iu|tablet|kapsül|spf))\b""", RegexOption.IGNORE_CASE).containsMatchIn(line)
    }
    val firstTitleLine = candidateLines.firstOrNull() ?: ""
    val extractedName = when {
      dosageLine != null && firstTitleLine.isNotBlank() && dosageLine != firstTitleLine && !dosageLine.contains(firstTitleLine, ignoreCase = true) ->
        "$firstTitleLine $dosageLine".trim()
      dosageLine != null -> dosageLine
      else -> firstTitleLine
    }

    return ScannedProductInfo(
      name = extractedName,
      category = category,
      whatItDoes = usageSummary,
      expiryDate = parsedExpiry,
      suggestedPaoMonths = paoMonths
    )
  }

  private fun inferMedicineUsage(upper: String, defaultText: String): String {
    return when {
      upper.contains("PARASETAMOL") || upper.contains("PAROL") || upper.contains("TYLOL") || upper.contains("MINOSET") ->
        "Ağrı kesici ve ateş düşürücü (Parasetamol). Baş, diş, kas ağrıları ve ateşli durumlarda kullanılır."
      upper.contains("İBUPROFEN") || upper.contains("IBUPROFEN") || upper.contains("NUROFEN") || upper.contains("DOLVEN") || upper.contains("ADVIL") ->
        "Ağrı kesici, ateş düşürücü ve iltihap giderici (İbuprofen). Soğuk algınlığı, baş, diş ve kas ağrılarında kullanılır."
      upper.contains("AMOKSİSİLİN") || upper.contains("AMOKSISILIN") || upper.contains("AUGMENTIN") || upper.contains("KLAMOKS") || upper.contains("LARGOPEN") ->
        "Bakteriyel enfeksiyonların tedavisinde kullanılan antibiyotik (Amoksisilin). Hekim önerisiyle tam kür kullanılır."
      upper.contains("DEKSKETOPROFEN") || upper.contains("ARVELES") || upper.contains("DEXDAY") || upper.contains("FLURBİPROFEN") || upper.contains("MAJEZİK") || upper.contains("DİKLOFENAK") || upper.contains("VOLTAREN") || upper.contains("NAPROKSEN") || upper.contains("APRANAX") ->
        "Kas-iskelet sistemi, eklem, diş ve adet ağrılarında kullanılan güçlü ağrı kesici ve antiinflamatuar ilaç."
      upper.contains("PANTOPRAZOL") || upper.contains("PULCET") || upper.contains("PANTPAS") || upper.contains("LANSOPRAZOL") || upper.contains("LANSOR") || upper.contains("ESOMEPRAZOL") || upper.contains("NEXIUM") || upper.contains("RENNIE") || upper.contains("GAVISCON") ->
        "Mide koruyucu ve asit düzenleyici. Reflü, gastrit ve mide yanması şikayetlerinde kullanılır."
      upper.contains("SETİRİZİN") || upper.contains("ZYRTEC") || upper.contains("ALLERSET") || upper.contains("DESLORATADİN") || upper.contains("AERIUS") || upper.contains("LORATADİN") || upper.contains("CLARITINE") || upper.contains("BİLASTİN") ->
        "Antihistaminik alerji ilacı. Mevsimsel alerjik rinit, hapşırma, burun akıntısı ve kaşıntı tedavisinde kullanılır."
      upper.contains("PSÖDOEFEDRİN") || upper.contains("AFERİN") || upper.contains("TYLOLHOT") || upper.contains("KATARİN") || upper.contains("THFLU") || upper.contains("THERAFLU") ->
        "Grip ve soğuk algınlığı semptomlarını (burun tıkanıklığı, ateş, kırgınlık) hafifletmek için kullanılır."
      upper.contains("OKSOLAMİN") || upper.contains("LEVODROPROPİZİN") || upper.contains("ASETİLSİSTEİN") || upper.contains("ASİST") || upper.contains("MUKOLİTİK") || upper.contains("ÖKSÜRÜK") ->
        "Öksürük kesici veya balgam söktürücü solunum yolu ilacı."
      upper.contains("DEKSPANTENOL") || upper.contains("BEPANTHOL") || upper.contains("MADECASSOL") || upper.contains("FİTO") || upper.contains("HAMETAN") || upper.contains("SILVERDIN") ->
        "Cilt yenileyici, yara/tahriş onarıcı ve nemlendirici topikal bakım kremi."
      else -> defaultText
    }
  }
}
