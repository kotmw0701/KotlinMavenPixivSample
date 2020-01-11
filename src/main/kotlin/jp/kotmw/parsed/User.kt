package jp.kotmw.parsed

data class User(
    val account: String,
    val id: String,
    val is_mail_authorized: Boolean,
    val is_premium: Boolean,
    val mail_address: String,
    val name: String,
    val profile_image_urls: ProfileImageUrls,
    val require_policy_agreement: Boolean,
    val x_restrict: Int
)
