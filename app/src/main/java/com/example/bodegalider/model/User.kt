package com.example.bodegalider.model

enum class UserRole(val label: String) {
    USER("Reponedor"),
    ADMIN("Supervisor de sector"), // Restringido a sus comunas asignadas
    SUPER_ADMIN("Administrador general") // Control total
}

data class User(
    val id: String,
    val name: String,
    val role: UserRole,
    val storeCode: String = "046", // Código de local
    val commune: String = "Maipú", // Comuna del trabajador
    val isOnline: Boolean = true,
    val shift: String = "Mañana", // Mañana / Tarde / Noche
    val contractType: String = "Full Time", // Full Time / Part Time / Backup
    val managedCommunes: List<String> = emptyList(), // Comunas bajo cargo exclusivo del Admin
    val username: String = "" // Usuario con el que inicia sesión
) {
    val scheduleText: String
        get() = "Turno $shift • $contractType"

    val initials: String
        get() = name.split(" ")
            .filter { it.isNotBlank() }
            .take(2)
            .joinToString("") { it.first().uppercase() }
            .ifEmpty { "?" }

    val isSupervisor: Boolean
        get() = role != UserRole.USER
}
