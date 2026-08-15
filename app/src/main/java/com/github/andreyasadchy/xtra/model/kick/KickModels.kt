package com.github.andreyasadchy.xtra.model.kick

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
class KickLivestreamsResponse(
    @SerialName("current_page")
    val currentPage: Int? = null,
    @SerialName("next_page_url")
    val nextPageUrl: String? = null,
    val data: List<KickLivestream> = emptyList(),
)

@Serializable
class KickLivestream(
    val id: Long? = null,
    @SerialName("channel_id")
    val channelId: Long? = null,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("session_title")
    val title: String? = null,
    @SerialName("viewer_count")
    val viewerCount: Int? = null,
    val thumbnail: KickThumbnail? = null,
    val tags: List<String>? = null,
    val channel: KickChannelSummary? = null,
    val categories: List<KickCategory>? = null,
)

@Serializable
class KickSubcategoriesResponse(
    @SerialName("current_page")
    val currentPage: Int? = null,
    @SerialName("next_page_url")
    val nextPageUrl: String? = null,
    val data: List<KickSubcategory> = emptyList(),
)

@Serializable
class KickSubcategory(
    val id: Long? = null,
    @SerialName("category_id")
    val categoryId: Long? = null,
    val name: String? = null,
    val slug: String? = null,
    val viewers: Int? = null,
    val banner: KickBanner? = null,
)

@Serializable
class KickChannelResponse(
    val id: Long? = null,
    @SerialName("user_id")
    val userId: Long? = null,
    val slug: String? = null,
    @SerialName("playback_url")
    val playbackUrl: String? = null,
    @SerialName("followers_count")
    val followersCount: Int? = null,
    @SerialName("banner_image")
    val bannerImage: KickBanner? = null,
    val chatroom: KickChatroom? = null,
    val livestream: KickChannelLivestream? = null,
    val user: KickUser? = null,
)

@Serializable
class KickChannelSummary(
    val id: Long? = null,
    val slug: String? = null,
    @SerialName("playback_url")
    val playbackUrl: String? = null,
    val user: KickUser? = null,
)

@Serializable
class KickChannelLivestream(
    val id: Long? = null,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("session_title")
    val title: String? = null,
    @SerialName("viewer_count")
    val viewerCount: Int? = null,
    @SerialName("playback_url")
    val playbackUrl: String? = null,
    val thumbnail: KickThumbnail? = null,
    val category: KickCategory? = null,
)

@Serializable
class KickChannelLivestreamResponse(
    val data: KickChannelLivestream? = null,
)

@Serializable
class KickCategory(
    val id: Long? = null,
    val name: String? = null,
    val slug: String? = null,
)

@Serializable
class KickThumbnail(
    val src: String? = null,
    @SerialName("url")
    val url: String? = null,
    val srcset: String? = null,
    val responsive: String? = null,
) {
    val imageUrl: String?
        get() {
            val candidates = buildList {
                listOf(url, src).forEach { value ->
                    value?.trim()?.takeIf { it.isNotBlank() }?.let(::add)
                }
                addAll(extractImageSetUrls(srcset))
                addAll(extractImageSetUrls(responsive))
            }
            return candidates.firstOrNull { !it.contains("://stream.kick.com/", ignoreCase = true) }
                ?: candidates.firstOrNull()
        }

    private fun extractImageSetUrls(value: String?): List<String> {
        return value
            ?.split(',')
            ?.asSequence()
            ?.map { candidate -> candidate.trim().substringBefore(' ').trim() }
            ?.filter { candidate -> candidate.isNotBlank() }
            ?.toList()
            .orEmpty()
    }
}

@Serializable
class KickBanner(
    val url: String? = null,
    val src: String? = null,
) {
    val imageUrl: String?
        get() = listOf(url, src)
            .mapNotNull { it?.trim()?.takeIf { value -> value.isNotBlank() } }
            .firstOrNull()
}

@Serializable
class KickWebsiteSearchResponse(
    val channels: List<KickSearchChannel> = emptyList(),
    val categories: List<KickSubcategory> = emptyList(),
    val livestreams: KickWebsiteSearchLivestreams = KickWebsiteSearchLivestreams(),
)

@Serializable
class KickWebsiteSearchLivestreams(
    val tags: List<KickLivestream> = emptyList(),
)

@Serializable
class KickSearchChannel(
    val id: Long? = null,
    @SerialName("user_id")
    val userId: Long? = null,
    val slug: String? = null,
    @SerialName("playback_url")
    val playbackUrl: String? = null,
    @SerialName("followers_count")
    val followersCount: Int? = null,
    @SerialName("is_live")
    val isLive: Boolean? = null,
    val user: KickUser? = null,
)

@Serializable
class KickChatroom(
    val id: Long? = null,
)

@Serializable
class KickUser(
    val id: Long? = null,
    val username: String? = null,
    val bio: String? = null,
    @SerialName("profilepic")
    val profilePic: String? = null,
    @SerialName("profile_picture")
    val profilePicture: String? = null,
) {
    val profileImage: String?
        get() = profilePic ?: profilePicture
}

@Serializable
class KickChatMessageEvent(
    val id: String? = null,
    val content: String? = null,
    val message: String? = null,
    @SerialName("created_at")
    val createdAt: String? = null,
    val sender: KickMessageSender? = null,
    val metadata: JsonElement? = null,
)

@Serializable
class KickMessageSender(
    val id: Long? = null,
    val slug: String? = null,
    val username: String? = null,
    val identity: KickMessageIdentity? = null,
)

@Serializable
class KickMessageIdentity(
    val color: String? = null,
    val badges: List<KickMessageBadge>? = null,
)

@Serializable
class KickMessageBadge(
    val type: String? = null,
    val text: String? = null,
    val count: Int? = null,
)

@Serializable
class KickOfficialListResponse<T>(
    val data: List<T> = emptyList(),
    val message: String? = null,
)

@Serializable
class KickOfficialLivestream(
    @SerialName("broadcaster_user_id")
    val broadcasterUserId: Long? = null,
    val category: KickOfficialCategory? = null,
    @SerialName("channel_id")
    val channelId: Long? = null,
    val language: String? = null,
    @SerialName("profile_picture")
    val profilePicture: String? = null,
    val slug: String? = null,
    @SerialName("started_at")
    val startedAt: String? = null,
    @SerialName("stream_title")
    val streamTitle: String? = null,
    val thumbnail: String? = null,
    @SerialName("viewer_count")
    val viewerCount: Int? = null,
    @SerialName("custom_tags")
    val customTags: List<String> = emptyList(),
)

@Serializable
class KickOfficialCategory(
    val id: Long? = null,
    val name: String? = null,
    val thumbnail: String? = null,
    @SerialName("viewer_count")
    val viewerCount: Int? = null,
)

@Serializable
class KickOfficialChannel(
    @SerialName("broadcaster_user_id")
    val broadcasterUserId: Long? = null,
    val slug: String? = null,
    val stream: KickOfficialChannelStream? = null,
    @SerialName("stream_title")
    val streamTitle: String? = null,
    val thumbnail: String? = null,
    val category: KickOfficialCategory? = null,
)

@Serializable
class KickOfficialChannelStream(
    @SerialName("is_live")
    val isLive: Boolean? = null,
    val thumbnail: String? = null,
    val url: String? = null,
    @SerialName("viewer_count")
    val viewerCount: Int? = null,
    @SerialName("start_time")
    val startTime: String? = null,
)

@Serializable
class KickAppTokenResponse(
    @SerialName("access_token")
    val accessToken: String? = null,
    @SerialName("expires_in")
    val expiresIn: Long? = null,
    @SerialName("token_type")
    val tokenType: String? = null,
)

@Serializable
class KickFeaturedLivestreamsResponse(
    val data: List<KickLivestream> = emptyList(),
    val featured: List<KickLivestream> = emptyList(),
    val livestreams: List<KickLivestream> = emptyList(),
) {
    val items: List<KickLivestream>
        get() = data.ifEmpty { featured.ifEmpty { livestreams } }
}

@Serializable
class KickCategoriesTopResponse(
    val data: List<KickSubcategory> = emptyList(),
    val categories: List<KickSubcategory> = emptyList(),
) {
    val items: List<KickSubcategory>
        get() = data.ifEmpty { categories }
}
