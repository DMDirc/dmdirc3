
import com.dmdirc.Style
import com.dmdirc.StyledSpan
import com.dmdirc.convertControlCodes
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.Arguments.arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

internal class ControlCodesTest {

    companion object {
        @JvmStatic
        @Suppress("unused")
        fun convertControlCodesArgumentsProvider(): Stream<Arguments> = Stream.of(
            arguments("", emptyList<StyledSpan>()),

            arguments("Crash and burn", listOf(
                StyledSpan("Crash and burn", emptySet())
            )),

            arguments("Crash and \u0002burn", listOf(
                StyledSpan("Crash and ", emptySet()),
                StyledSpan("burn", setOf(Style.BoldStyle))
            )),

            arguments("\u001fCrash \u001dand \u0002burn", listOf(
                StyledSpan("Crash ", setOf(Style.UnderlineStyle)),
                StyledSpan("and ", setOf(Style.UnderlineStyle, Style.ItalicStyle)),
                StyledSpan("burn", setOf(Style.UnderlineStyle, Style.ItalicStyle, Style.BoldStyle))
            )),

            arguments("\u001f\u001fCrash \u001e\u0011and \u000fburn", listOf(
                StyledSpan("Crash ", emptySet()),
                StyledSpan("and ", setOf(Style.MonospaceStyle, Style.StrikethroughStyle)),
                StyledSpan("burn", emptySet())
            )),

            arguments("\u00034Crash and burn", listOf(
                StyledSpan("Crash and burn", setOf(Style.ColourStyle(4, null)))
            )),

            arguments("\u000304Crash and burn", listOf(
                StyledSpan("Crash and burn", setOf(Style.ColourStyle(4, null)))
            )),

            arguments("\u00034,5Crash and \u000fburn", listOf(
                StyledSpan("Crash and ", setOf(Style.ColourStyle(4, 5))),
                StyledSpan("burn", emptySet())
            )),

            arguments("\u00034,Crash and \u0003burn", listOf(
                StyledSpan(",Crash and ", setOf(Style.ColourStyle(4, null))),
                StyledSpan("burn", emptySet())
            )),

            arguments("\u00034,Crash and \u0004burn", listOf(
                StyledSpan(",Crash and ", setOf(Style.ColourStyle(4, null))),
                StyledSpan("burn", emptySet())
            )),

            arguments("\u0004FFFFFFCrash and burn", listOf(
                StyledSpan("Crash and burn", setOf(Style.HexColourStyle("FFFFFF", null)))
            )),

            arguments("\u000412cdefCrash and burn", listOf(
                StyledSpan("Crash and burn", setOf(Style.HexColourStyle("12cdef", null)))
            )),

            arguments("\u0004FF0000,000000Crash and \u000fburn", listOf(
                StyledSpan("Crash and ", setOf(Style.HexColourStyle("FF0000", "000000"))),
                StyledSpan("burn", emptySet())
            )),

            arguments("\u0004aabbcc,Crash and \u0004burn", listOf(
                StyledSpan(",Crash and ", setOf(Style.HexColourStyle("aabbcc", null))),
                StyledSpan("burn", emptySet())
            )),

            arguments("\u0004Crash and burn", listOf(
                StyledSpan("Crash and burn", emptySet())
            )),

            arguments("\u0017Crash and \u0017burn", listOf(
                StyledSpan("Crash and ", setOf(Style.Link("Crash and "))),
                StyledSpan("burn", emptySet())
            )),

            arguments("\u0019Crash\u0019 and burn", listOf(
                StyledSpan("Crash", setOf(Style.CustomStyle("irc-nickname"))),
                StyledSpan(" and burn", emptySet())
            ))
        )
    }

    @ParameterizedTest
    @MethodSource("convertControlCodesArgumentsProvider")
    fun `converts control codes`(input: String, spans: List<StyledSpan>) {
        assertEquals(spans, input.convertControlCodes().toList())
    }

}