package com.dmdirc

import javafx.scene.Node
import org.fxmisc.richtext.StyledTextArea
import org.fxmisc.richtext.model.SimpleEditableStyledDocument

sealed class Style {
    object BoldStyle : Style()
    object ItalicStyle : Style()
    object MonospaceStyle : Style()
    object UnderlineStyle : Style()
    object StrikethroughStyle : Style()
    data class CustomStyle(val style: String) : Style()
    data class ColourStyle(val foreground: Int, val background: Int?) : Style()
}

class IrcTextArea : StyledTextArea<Collection<Style>, Collection<Style>>(
    emptySet(),
    ::applyStyles,
    emptySet(),
    ::applyStyles,
    SimpleEditableStyledDocument(emptyList(), emptyList()),
    true
)

private fun applyStyles(node: Node, styles: Collection<Style>) {
    styles.forEach {
        when (it) {
            is Style.BoldStyle -> node.styleClass.add("irc-bold")
            is Style.ItalicStyle -> node.styleClass.add("irc-italic")
            is Style.UnderlineStyle -> node.styleClass.add("irc-underline")
            is Style.StrikethroughStyle -> node.styleClass.add("irc-strikethrough")
            is Style.MonospaceStyle -> node.styleClass.add("irc-monospace")
            is Style.ColourStyle -> {
                node.styleClass.add("irc-colour-fg-${it.foreground}")
                it.background?.let { bg -> node.styleClass.add("irc-colour-bg-$bg") }
            }
            is Style.CustomStyle -> node.styleClass.add(it.style)
        }
    }
}

