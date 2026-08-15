package com.github.andreyasadchy.xtra.repository

import com.github.andreyasadchy.xtra.model.kick.KickChannelLivestream
import com.github.andreyasadchy.xtra.model.kick.KickChannelResponse
import com.github.andreyasadchy.xtra.model.kick.KickLivestream
import com.github.andreyasadchy.xtra.model.kick.KickSearchChannel
import com.github.andreyasadchy.xtra.model.kick.KickSubcategory
import com.github.andreyasadchy.xtra.model.ui.Game
import com.github.andreyasadchy.xtra.model.ui.Stream
import com.github.andreyasadchy.xtra.model.ui.User
import com.github.andreyasadchy.xtra.util.C
import com.github.andreyasadchy.xtra.util.KickApiHelper
import java.util.Locale

internal object KickWebsiteSearchMapper {

    fun toUser(item: KickSearchChannel): User {
        return User(
            id = item.id?.toString(),
            login = item.slug,
            name = item.user?.username,
            profileImageURL = item.user?.profileImage,
            followerCount = item.followersCount,
            isLive = item.isLive == true,
            source = C.KICK,
        )
    }

    fun toUser(channel: KickChannelResponse): User {
        return User(
            id = channel.id?.toString(),
            login = channel.slug,
            name = channel.user?.username,
            profileImageURL = channel.user?.profileImage,
            followerCount = channel.followersCount,
            bannerImageURL = channel.bannerImage?.imageUrl,
            isLive = channel.livestream != null,
            source = C.KICK,
        )
    }

    fun toGame(item: KickSubcategory): Game {
        return Game(
            id = item.id?.toString(),
            slug = item.slug,
            name = item.name,
            boxArtURL = item.banner?.imageUrl,
            viewerCount = item.viewers,
            source = C.KICK,
        )
    }

    fun toStream(item: KickLivestream): Stream {
        val category = item.categories?.firstOrNull()
        val channelLogin = item.channel?.slug ?: item.channel?.user?.username?.lowercase(Locale.ROOT)
        return Stream(
            id = KickApiHelper.kickStreamId(item.id?.toString() ?: channelLogin),
            channelId = item.channel?.id?.toString() ?: item.channelId?.toString(),
            channelLogin = channelLogin,
            channelName = item.channel?.user?.username,
            channelImageURL = item.channel?.user?.profileImage,
            gameId = category?.id?.toString(),
            gameSlug = category?.slug,
            gameName = category?.name,
            title = item.title,
            thumbnailURL = item.thumbnail?.imageUrl,
            createdAt = normalizeDate(item.createdAt),
            viewerCount = item.viewerCount,
            tags = item.tags,
            source = C.KICK,
        )
    }

    fun toStream(channel: KickSearchChannel, livestream: KickChannelLivestream): Stream {
        val channelLogin = channel.slug ?: channel.user?.username?.lowercase(Locale.ROOT)
        return Stream(
            id = KickApiHelper.kickStreamId(livestream.id?.toString() ?: channelLogin),
            channelId = channel.id?.toString() ?: channel.userId?.toString(),
            channelLogin = channelLogin,
            channelName = channel.user?.username,
            channelImageURL = channel.user?.profileImage,
            gameId = livestream.category?.id?.toString(),
            gameSlug = livestream.category?.slug,
            gameName = livestream.category?.name,
            title = livestream.title,
            thumbnailURL = livestream.thumbnail?.imageUrl,
            createdAt = normalizeDate(livestream.createdAt),
            viewerCount = livestream.viewerCount,
            source = C.KICK,
        )
    }

    fun toStream(channel: KickChannelResponse, livestreamOverride: KickChannelLivestream? = null): Stream {
        val livestream = livestreamOverride ?: channel.livestream
        return Stream(
            id = KickApiHelper.kickStreamId(livestream?.id?.toString() ?: channel.slug),
            channelId = channel.id?.toString(),
            channelLogin = channel.slug,
            channelName = channel.user?.username,
            channelImageURL = channel.user?.profileImage,
            gameId = livestream?.category?.id?.toString(),
            gameSlug = livestream?.category?.slug,
            gameName = livestream?.category?.name,
            title = livestream?.title,
            thumbnailURL = livestream?.thumbnail?.imageUrl,
            createdAt = normalizeDate(livestream?.createdAt),
            viewerCount = livestream?.viewerCount,
            source = C.KICK,
        )
    }

    private fun normalizeDate(input: String?): String? {
        if (input.isNullOrBlank()) return null
        return when {
            input.contains('T') -> if (input.endsWith("Z") || input.contains("+")) input else "${input}Z"
            else -> input.replace(' ', 'T') + "Z"
        }
    }
}
