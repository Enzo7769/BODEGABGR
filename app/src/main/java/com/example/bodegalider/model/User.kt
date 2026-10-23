package com.example.bodegalider.model

enum class UserRole {
    USER,       // Reponedor
    ADMIN,      // Supervisor de Sector (Restringido a sus comunas asignadas)
    SUPER_ADMIN // Control Total
}

data class User(
    val id: String,
    val name: String,
    val role: UserRole,
    val storeCode: String = "046", // Código de local
    val commune: String = "Maipú", // Comuna del trabajador
    val isOnline: Boolean = true,
    val shift: String = "Mañana",          // Mañana / Tarde / Noche
    val contractType: String = "Full Time", // Full Time / Part Time / Backup
    val managedCommunes: List<String> = listOf("Maipú", "Pudahuel") // Comunas bajo cargo exclusivo del Admin
) {
    val scheduleText: String
        get() = "Turno $shift • $contractType"
}
