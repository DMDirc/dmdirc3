package com.dmdirc

import javafx.collections.ListChangeListener
import kotlin.math.max

/**
 * Starts observing all newly added lines for image links, and automatically
 * inserts inlined images when they are detected.
 */
fun WindowModel.addImageHandler(config: ClientConfig) {
    // Keep track of the last line processed, as adding lines will cause another
    // change event to fire with the original lines plus our new lines.
    var lastLine = 0
    lines.addListener(ListChangeListener<Array<StyledSpan>> {
        while (it.next()) {
            if (it.wasAdded() && config[ClientSpec.Display.embedImages] && it.to > lastLine) {
                val start = max(lastLine, it.from)
                lastLine = it.to
                // Iterate backwards so that our indices don't change as we're inserting
                for (i in it.to - 1 downTo start) {
                    val images = lines[i].imageSpans()
                    if (images.isNotEmpty()) {
                        runLater {
                            lastLine++
                            lines.add(it.to - i, images)
                        }
                    }
                }
            }
        }
    })
}

private fun Array<StyledSpan>.imageSpans() =
    this.flatMap { it.styles }.filterIsInstance(Style.Link::class.java).map { it.url }.filter {
            it.matches(
                Regex(
                    ".*\\.(png|jpg|jpeg)$",
                    RegexOption.IGNORE_CASE
                )
            )
        }.map { StyledSpan("${ControlCode.InternalImages}$it", emptySet()) }.toTypedArray()
