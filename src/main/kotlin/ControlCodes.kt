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
     * Sets or resets the colour of the following text, using hexadecimal. NOT CURRENTLY SUPPORTED.
     *
     * Takes colours in the same manner as [Colour].
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

sealed class Style {
    object BoldStyle : Style()
    object ItalicStyle : Style()
    object MonospaceStyle : Style()
    object UnderlineStyle : Style()
    object StrikethroughStyle : Style()
    data class CustomStyle(val style: String) : Style()
    data class ColourStyle(val foreground: Int, val background: Int?) : Style()
}

data class StyledSpan(val content: String, val styles: Set<Style>)

fun String.convertControlCodes() = sequence {
    val styles = mutableSetOf<Style>()
    val buffer = StringBuilder()
    var nextColour = -1
    val colours = arrayOf(StringBuilder(), StringBuilder())

    fun emitThen(block: () -> Unit) = sequence {
        var needComma = false
        if (nextColour > -1) {
            if (nextColour == 1 && colours[1].isEmpty()) {
                // We consumed a comma that wasn't used; re-add it
                needComma = true
            }

            if (colours[0].isEmpty()) {
                styles.removeIf { it is Style.ColourStyle }
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

        if (needComma) buffer.append(',')

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
            ControlCode.Colour -> yieldAll(emitThen { nextColour = 0 })
            else -> when {
                nextColour == -1 -> buffer.append(it)
                nextColour == 0 && it == ',' -> nextColour = 1
                nextColour >= 0 && it.isDigit() && colours[nextColour].length < 2 -> colours[nextColour].append(it)
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

private fun Array<StringBuilder>.toStyle() = Style.ColourStyle(
    this[0].toString().toInt(),
    if (this[1].isEmpty()) null else this[1].toString().toInt()
)
