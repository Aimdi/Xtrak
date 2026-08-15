package com.github.andreyasadchy.xtra.util

import android.content.SharedPreferences
import android.view.View
import androidx.core.content.edit
import com.github.andreyasadchy.xtra.databinding.SourceSwitchBinding

object StreamSource {
    fun current(prefs: SharedPreferences): String {
        return prefs.getString(C.UI_STREAM_SOURCE, C.TWITCH)
            ?.takeIf { it.equals(C.KICK, ignoreCase = true) || it.equals(C.TWITCH, ignoreCase = true) }
            ?.lowercase()
            ?: C.TWITCH
    }

    fun includeTwitch(source: String?): Boolean {
        return !source.equals(C.KICK, ignoreCase = true)
    }

    fun includeKick(source: String?): Boolean {
        return source.equals(C.KICK, ignoreCase = true)
    }

    fun bind(binding: SourceSwitchBinding, prefs: SharedPreferences, onChanged: (String) -> Unit) {
        val group = binding.sourceToggle
        val selected = current(prefs)
        group.check(if (selected == C.KICK) binding.sourceKick.id else binding.sourceTwitch.id)
        group.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            val source = if (checkedId == binding.sourceKick.id) C.KICK else C.TWITCH
            if (current(prefs) == source) return@addOnButtonCheckedListener
            prefs.edit { putString(C.UI_STREAM_SOURCE, source) }
            onChanged(source)
        }
        binding.root.visibility = View.VISIBLE
    }
}
