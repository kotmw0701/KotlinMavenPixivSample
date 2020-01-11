package jp.kotmw.parsed.illust

data class Illusts(
    val illusts: List<Illust>,
    val next_url: String
)

data class Illust(
    val caption: String,
    val create_date: String,
    val height: Int,
    val id: Int,
    val image_urls: ImageUrls,
    val is_bookmarked: Boolean,
    val is_muted: Boolean,
    val meta_pages: List<MetaPage>,
    val meta_single_page: MetaSinglePage,
    val page_count: Int,
    val restrict: Int,
    val sanity_level: Int,
    val series: Series?, //無しになるパターン有
    val tags: List<Tag>,
    val title: String,
    val tools: List<String>,
    val total_bookmarks: Int,
    val total_view: Int,
    val type: String,
    val user: User,
    val visible: Boolean,
    val width: Int,
    val x_restrict: Int
)

data class ImageUrls(
    val large: String,
    val medium: String,
    val original: String?, //無しになるパターン有
    val square_medium: String
)

data class MetaPage(
    val image_urls: ImageUrls
)

data class MetaSinglePage(
    val original_image_url: String? //無しになるパターン有
)

data class Series(
    val id: Int,
    val title: String
)

data class Tag(
    val name: String,
    val translated_name: Any? //無しになるパターン有
)

data class User(
    val account: String,
    val id: Int,
    val is_followed: Boolean,
    val name: String,
    val profile_image_urls: ProfileImageUrls
)

data class ProfileImageUrls(
    val medium: String
)
