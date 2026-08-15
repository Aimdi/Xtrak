package com.github.andreyasadchy.xtra.model.ui

import android.os.Parcelable
import com.github.andreyasadchy.xtra.util.C
import com.github.andreyasadchy.xtra.util.KickApiHelper
import com.github.andreyasadchy.xtra.util.TwitchApiHelper
import kotlinx.parcelize.Parcelize

@Parcelize
class Stream(
    var id: String? = null,
    val channelId: String? = null,
    val channelLogin: String? = null,
    val channelName: String? = null,
    var channelImageURL: String? = null,
    var gameId: String? = null,
    var gameSlug: String? = null,
    var gameName: String? = null,
    var title: String? = null,
    val thumbnailURL: String? = null,
    var createdAt: String? = null,
    var viewerCount: Int? = null,
    val tags: List<String>? = null,
    val source: String? = C.TWITCH,
) : Parcelable {

    val isKick: Boolean
        get() = KickApiHelper.isKickSource(source, id)

    val channelImage: String?
        get() = if (isKick || KickApiHelper.isKickCdnUrl(channelImageURL)) {
            channelImageURL
        } else {
            TwitchApiHelper.getProfileImage(channelImageURL)
        }
    val thumbnail: String?
        get() = if (isKick || KickApiHelper.isKickCdnUrl(thumbnailURL)) {
            thumbnailURL
        } else {
            TwitchApiHelper.getStreamThumbnail(thumbnailURL)
        }
}
