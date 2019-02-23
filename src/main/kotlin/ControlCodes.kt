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
     * Sets or resets the colour of the following text. NOT CURRENTLY SUPPORTED.
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
    data class ColourStyle(val foreground: Int?, val background: Int?) : Style()
}

data class StyledSpan(val content: String, val styles: Set<Style>)

fun String.convertControlCodes() = sequence {
    val styles = mutableSetOf<Style>()
    val buffer = StringBuilder()

    fun emitThen(block: () -> Unit) = sequence {
        if (buffer.isNotEmpty()) {
            yield(StyledSpan(buffer.toString(), HashSet(styles)))
            buffer.clear()
        }
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
            else -> buffer.append(it)
        }
    }

    yieldAll(emitThen {})
}

private fun MutableSet<Style>.toggle(style: Style) = when {
    contains(style) -> remove(style)
    else -> add(style)
}