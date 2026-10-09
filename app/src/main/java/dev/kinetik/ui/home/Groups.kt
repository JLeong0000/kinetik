package dev.kinetik.ui.home

import dev.kinetik.model.Library
import dev.kinetik.model.groupOf
import dev.kinetik.model.upNext

/** When the app starts, every group is collapsed except the one holding the up-next workout. */
fun expandedOnStart(lib: Library): Set<String> =
    lib.upNext()?.let { lib.groupOf(it.id)?.id }?.let(::setOf).orEmpty()
