package jp.kotmw

data class Configuration(
    val clientId: String,
    val clientSecret: String,
    val hashSecret: String,
    val refreshToken: String
)
