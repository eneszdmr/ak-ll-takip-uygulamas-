package com.example.data.model

data class VehicleInsuranceEntity(
  val id: Long = 0,
  val plateAndVehicle: String, // örn: 34 ABC 123 - Volkswagen Golf
  val policyType: String, // Trafik Sigortası, Kasko, TÜVTÜRK Muayene, Periyodik Bakım, Egzoz Muayenesi, Diğer
  val company: String = "", // örn: Allianz, Anadolu Sigorta, TÜVTÜRK
  val startDate: String, // YYYY-MM-DD
  val expiryDate: String, // YYYY-MM-DD
  val notes: String = ""
)
