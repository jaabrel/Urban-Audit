package pt.ipt.dam.urbanaudit.models

data class LoginResponse (
    val token: String,
    val userId: Int
)