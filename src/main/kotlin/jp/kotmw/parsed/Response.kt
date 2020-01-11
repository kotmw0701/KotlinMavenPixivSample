package jp.kotmw.parsed

data class Response(
    val access_token: String,
    val device_token: String,
    val expires_in: Int,
    val refresh_token: String,
    val scope: String,
    val token_type: String,
    val user: User
)
