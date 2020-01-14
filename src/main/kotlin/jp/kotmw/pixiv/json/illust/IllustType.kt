package jp.kotmw.pixiv.json.illust

import com.fasterxml.jackson.annotation.JsonProperty

enum class IllustType {
    @JsonProperty("illust") Illust,
    @JsonProperty("ugoira") Ugoira,
    @JsonProperty("manga") Manga

}
