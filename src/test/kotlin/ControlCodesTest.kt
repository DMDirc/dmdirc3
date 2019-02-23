
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
            ))
        )
    }

    @ParameterizedTest
    @MethodSource("convertControlCodesArgumentsProvider")
    fun `converts control codes`(input: String, spans: List<StyledSpan>) {
        assertEquals(spans, input.convertControlCodes().toList())
    }

}