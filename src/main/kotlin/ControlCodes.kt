package com.dmdirc

/**
 * Control codes that may be used in messages to change their formatting.
 */
object ControlCode {

    /**
     * Toggles the bold property of text.
     */
    const val Bold = '\u0002'

    /**
     * Sets or resets the colour of the following text.
     *
     * If followed by one or two numbers separated by a comma, sets the foreground and optionally background of the
     * text. Otherwise, clears all colours.
     */
    const val Colour = '\u0003'

    /**
     * Sets or resets the colour of the following text, using hexadecimal.
     *
     * Takes 6-character hexadecimal colours in the same manner as [Colour].
     */
    const val HexColour = '\u0004'

    /**
     * Resets all styles.
     */
    const val Reset = '\u000f'

    /**
     * Toggles the monospace property of text.
     */
    const val Monospace = '\u0011'

    /**
     * Toggles the 'reverse' (fg and bg colours swapped) property of text. NOT CURRENTLY SUPPORTED.
     */
    const val Reverse = '\u0016'

    /**
     * Identifies links for special processing within the client.
     */
    const val InternalLinks = '\u0017'

    /**
     * Identifies embedded images for special processing within the client.
     */
    const val InternalImages = '\u0018'

    /**
     * Toggles the italic property of text.
     */
    const val Italic = '\u001d'

    /**
     * Toggles the strike through property of text.
     */
    const val Strikethrough = '\u001e'

    /**
     * Toggles the underline property of text.
     */
    const val Underline = '\u001f'

}

data class StyledSpan(val content: String, val styles: Set<Style>)

private val linkRegex = Regex("(?i)(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]", RegexOption.IGNORE_CASE)

fun String.detectLinks() = linkRegex.replace(this, "${ControlCode.InternalLinks}$0${ControlCode.InternalLinks}")

fun String.convertControlCodes() = sequence {
    val styles = mutableSetOf<Style>()
    val buffer = StringBuilder()
    var nextColour = -1
    var nextColourLength = 0 to 0
    var nextColourFilter: (Char) -> Boolean =  Char::isDigit
    val colours = arrayOf(StringBuilder(), StringBuilder())

    fun emitThen(block: () -> Unit) = sequence {
        var extraContent = ""
        if (nextColour > -1) {
            if (colours[nextColour].length < nextColourLength.first) {
                // We got a bit of a colour, but not enough.
                extraContent = String(colours[nextColour])
                colours[nextColour].clear()
            }

            if (nextColour == 1 && colours[1].isEmpty()) {
                // We consumed a comma that wasn't used; re-add it
                extraContent = ",$extraContent"
            }

            if (colours[0].isEmpty()) {
                styles.removeIf { it is Style.ColourStyle || it is Style.HexColourStyle }
            } else {
                styles.add(colours.toStyle())
            }

            nextColour = -1
            colours.forEach { it.clear() }
        }

        if (buffer.isNotEmpty()) {
            yield(StyledSpan(buffer.toString(), HashSet(styles)))
            buffer.clear()
        }

        buffer.append(extraContent)

        block.invoke()
    }

    forEach {
        when (it) {
            ControlCode.Bold -> yieldAll(emitThen { styles.toggle(Style.BoldStyle) })
            ControlCode.Italic -> yieldAll(emitThen { styles.toggle(Style.ItalicStyle) })
            ControlCode.Monospace -> yieldAll(emitThen { styles.toggle(Style.MonospaceStyle) })
            ControlCode.Underline -> yieldAll(emitThen { styles.toggle(Style.UnderlineStyle) })
            ControlCode.Strikethrough -> yieldAll(emitThen { styles.toggle(Style.StrikethroughStyle) })
            ControlCode.Reset -> yieldAll(emitThen { styles.clear() })
            ControlCode.Colour -> yieldAll(emitThen {
                nextColour = 0
                nextColourLength = 1 to 2
                nextColourFilter = Char::isDigit
            })
            ControlCode.HexColour -> yieldAll(emitThen {
                nextColour = 0
                nextColourLength = 6 to 6
                nextColourFilter = { c: Char -> c.isDigit() || c.toLowerCase() in 'a'..'f' }
            })
            ControlCode.InternalLinks -> {
                styles.firstOrNull { s -> s is Style.Link }?.let { link ->
                    (link as Style.Link).url = buffer.toString()
                    yieldAll(emitThen { styles.removeAll { l -> l is Style.Link } })
                } ?: yieldAll(emitThen { styles.add(Style.Link("")) })
            }
            else -> when {
                // We're not expecting a colour argument:
                nextColour == -1 -> buffer.append(it)
                // We've expecting the first colour, have got enough chars, and hit a comma:
                nextColour == 0 && colours[nextColour].length >= nextColourLength.first && it == ',' -> nextColour = 1
                // We're expecting a colour, hit a valid char for our colour, and don't yet have enough chars:
                nextColour >= 0 && nextColourFilter(it) && colours[nextColour].length < nextColourLength.second -> colours[nextColour].append(it)
                // We're expecting a colour but got something else:
                nextColour >= 0 -> yieldAll(emitThen { buffer.append(it) })
            }
        }
    }

    yieldAll(emitThen {})
}

private fun MutableSet<Style>.toggle(style: Style) = when {
    contains(style) -> remove(style)
    else -> add(style)
}

private fun Array<StringBuilder>.toStyle(): Style {
    val foreground = this[0].toString()
    val background = if (this[1].isEmpty()) null else this[1].toString()
    return if (foreground.length <= 2) {
        Style.ColourStyle(foreground.toInt(), background?.toInt())
    } else {
        Style.HexColourStyle(foreground, background)
    }
}
