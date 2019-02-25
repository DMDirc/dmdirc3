package com.dmdirc

import javafx.scene.Node
import org.fxmisc.richtext.GenericStyledArea
import org.fxmisc.richtext.TextExt
import org.fxmisc.richtext.model.SegmentOpsBase
import org.fxmisc.richtext.model.StyledSegment
import org.fxmisc.richtext.model.TextOps
import java.util.*

sealed class Style {
    object BoldStyle : Style()
    object ItalicStyle : Style()
    object MonospaceStyle : Style()
    object UnderlineStyle : Style()
    object StrikethroughStyle : Style()
    data class CustomStyle(val style: String) : Style()
    data class ColourStyle(val foreground: Int, val background: Int?) : Style()
    data class Link(var url: String) : Style()
}

sealed class Segment {
    data class Image(val url: String) : Segment()
    data class Text(val content: String) : Segment()
    object Empty : Segment()
}

class IrcSegmentOps : SegmentOpsBase<Segment, Collection<Style>>(Segment.Empty), TextOps<Segment, Collection<Style>> {

    override fun realGetText(seg: Segment?) = when (seg) {
        is Segment.Text -> seg.content
        else -> ""
    }

    override fun realCharAt(seg: Segment?, index: Int) = when (seg) {
        is Segment.Text -> seg.content[index]
        else -> '?'
    }

    override fun realSubSequence(seg: Segment?, start: Int, end: Int) = when (seg) {
        is Segment.Text -> Segment.Text(seg.content.substring(start, end))
        else -> seg
    }

    override fun length(seg: Segment?) = when (seg) {
        is Segment.Text -> seg.content.length
        is Segment.Image -> 1
        is Segment.Empty -> 0
        null -> 0
    }

    override fun joinSeg(currentSeg: Segment?, nextSeg: Segment?): Optional<Segment> =
        if (currentSeg is Segment.Text && nextSeg is Segment.Text) {
            Optional.of(Segment.Text(currentSeg.content + nextSeg.content))
        } else {
            Optional.empty()
        }

    override fun create(text: String?) = text?.let {
        Segment.Text(it)
    } ?: Segment.Empty

}

class IrcTextArea(linkClickHandler: (String) -> Unit) :
    GenericStyledArea<Collection<Style>, Segment, Collection<Style>>(
        emptySet(),
        { _, _ -> Unit },
        emptySet(),
        IrcSegmentOps(),
        true,
        { ss: StyledSegment<Segment, Collection<Style>> ->
            ss.segment.let { seg ->
                when (seg) {
                    is Segment.Text -> TextExt(seg.content).also { te ->
                        te.styleClass.add("text")
                        applyStyles(te, ss.style, linkClickHandler)
                    }
                    is Segment.Image -> javafx.scene.image.ImageView(seg.url)
                    is Segment.Empty -> TextExt("There's nothing here :(").also { te -> te.styleClass.add("text") }
                }
            }
        }
    )

private fun applyStyles(node: Node, styles: Collection<Style>, linkClickHandler: (String) -> Unit) {
    styles.forEach { style ->
        when (style) {
            is Style.BoldStyle -> node.styleClass.add("irc-bold")
            is Style.ItalicStyle -> node.styleClass.add("irc-italic")
            is Style.UnderlineStyle -> node.styleClass.add("irc-underline")
            is Style.StrikethroughStyle -> node.styleClass.add("irc-strikethrough")
            is Style.MonospaceStyle -> node.styleClass.add("irc-monospace")
            is Style.ColourStyle -> {
                node.styleClass.add("irc-colour-fg-${style.foreground}")
                style.background?.let { bg -> node.styleClass.add("irc-colour-bg-$bg") }
            }
            is Style.CustomStyle -> node.styleClass.add(style.style)
            is Style.Link -> {
                node.styleClass.add("irc-link")
                node.setOnMouseClicked { linkClickHandler(style.url) }
            }
        }
    }
}
