package pt.ipt.dam.urbanaudit.data.model

data class LoginRequest(val email: String, val password: String)
data class RegisterRequest(val email: String, val password: String)
data class LoginResponse(val access_token: String, val token_type: String, val userId: Int, val role: String)
data class UserResponse(val id: Int, val email: String)

data class Ocorrencia(
    val id: Int,
    val titulo: String,
    val descricao: String,
    val latitude: Double,
    val longitude: Double,
    val fotoBase64: String,
    val estado: String,
    val owner_id: Int
)

data class OcorrenciaCreate(
    val titulo: String, val descricao: String,
    val latitude: Double, val longitude: Double, val fotoBase64: String
)
data class OcorrenciaUpdate(
    val titulo: String?, val descricao: String?, val estado: String?
)