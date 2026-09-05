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
    val owner_id: Int,
    val categoria: String? = null
) {
    /**
     * Extrai a categoria atribuída à ocorrência ou infere a partir do conteúdo.
     */
    fun obterCategoria(): String {
        if (!categoria.isNullOrBlank()) return categoria

        // Se a descrição contém a tag [Categoria]
        if (descricao.startsWith("[") && descricao.contains("]")) {
            val tag = descricao.substringAfter("[").substringBefore("]").trim()
            if (tag.isNotBlank()) return tag
        }

        // Inferência inteligente por palavras-chave
        val texto = "$titulo $descricao".lowercase()
        return when {
            texto.contains("pavimento") || texto.contains("buraco") || texto.contains("estrada") || texto.contains("passeio") || texto.contains("via") || texto.contains("alcatrão") -> "Vias e Pavimento"
            texto.contains("iluminação") || texto.contains("iluminacao") || texto.contains("candeeiro") || texto.contains("luz") || texto.contains("poste") || texto.contains("lâmpada") || texto.contains("lampada") -> "Iluminação Pública"
            texto.contains("resíduo") || texto.contains("residuo") || texto.contains("lixo") || texto.contains("contentor") || texto.contains("ecoponto") || texto.contains("limpeza") -> "Resíduos e Limpeza"
            texto.contains("verde") || texto.contains("jardim") || texto.contains("árvore") || texto.contains("arvore") || texto.contains("relva") || texto.contains("parque") -> "Espaços Verdes"
            texto.contains("sinal") || texto.contains("semáforo") || texto.contains("semaforo") || texto.contains("trânsito") || texto.contains("transito") || texto.contains("passadeira") -> "Sinalização e Trânsito"
            else -> "Outro"
        }
    }

    /**
     * Devolve a descrição limpa, sem a tag [Categoria] no início caso exista.
     */
    fun obterDescricaoLimpa(): String {
        return if (descricao.startsWith("[") && descricao.contains("]")) {
            descricao.substringAfter("]").trim()
        } else {
            descricao
        }
    }
}

data class OcorrenciaCreate(
    val titulo: String,
    val descricao: String,
    val latitude: Double,
    val longitude: Double,
    val fotoBase64: String,
    val categoria: String? = null
)
data class OcorrenciaUpdate(
    val titulo: String?, val descricao: String?, val estado: String?
)