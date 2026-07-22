package vn.loi.learning.desktop.ui.search

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight

@Composable
fun HighlightedSearchText(
    text: String,
    query: String,
    modifier: Modifier = Modifier,
    style: TextStyle = TextStyle.Default,
    fontWeight: FontWeight? = null
) {
    val presentation = presentSearchMatches(text, query)
    Text(
        text = presentation.toAnnotatedString(),
        modifier = modifier.semantics {
            contentDescription = presentation.contentDescription
        },
        style = style,
        fontWeight = fontWeight
    )
}

@Composable
private fun SearchMatchPresentation.toAnnotatedString(): AnnotatedString {
    if (ranges.isEmpty()) return AnnotatedString(text)
    val highlightStyle = SpanStyle(
        color = MaterialTheme.colorScheme.onPrimaryContainer,
        background = MaterialTheme.colorScheme.primaryContainer,
        fontWeight = FontWeight.Bold
    )
    return buildAnnotatedString {
        append(text)
        ranges.forEach { range ->
            addStyle(highlightStyle, range.start, range.endExclusive)
        }
    }
}
