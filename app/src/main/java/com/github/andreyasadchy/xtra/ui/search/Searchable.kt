package com.github.andreyasadchy.xtra.ui.search

import com.github.andreyasadchy.xtra.ui.common.StreamSourceAware

interface Searchable : StreamSourceAware {
    fun search(query: String)
    override fun setSource(source: String) {}
}