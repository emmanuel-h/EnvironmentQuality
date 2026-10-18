package fr.mandarine.environmentquality.ui

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp

/**
 * Looks for the UI bugs a screenshot would show: text cut off, content pushed
 * past the screen edge, controls too small to tap and controls with no label.
 */
object LayoutAudit {

    /**
     * Compose already widens the touch area of smaller controls to 48 dp when
     * nothing overlaps them, so only controls far below that are reported.
     */
    private val minTouchTarget = 32.dp

    fun findIssues(rule: ComposeTestRule): List<String> {
        val root = rule.onRoot(useUnmergedTree = true).fetchSemanticsNode()
        val density = root.layoutInfo.density
        val width = root.boundsInRoot.width
        val minTarget = with(density) { minTouchTarget.toPx() }
        val issues = mutableListOf<String>()
        visit(root, insideHorizontalScroll = false) { node, inScroll ->
            val label = describe(node)
            val bounds = node.boundsInRoot
            node.config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action?.let { getLayout ->
                val results = mutableListOf<TextLayoutResult>()
                getLayout(results)
                // Ellipsis on the last line is what a reader sees as cut-off text.
                if (results.any { it.lineCount > 0 && it.isLineEllipsized(it.lineCount - 1) }) issues += "text cut off: $label"
            }
            if (!inScroll && bounds.width > 0 && (bounds.right > width + 1f || bounds.left < -1f)) {
                issues += "past the screen edge (${bounds.left.toInt()}..${bounds.right.toInt()} of ${width.toInt()}): $label"
            }
            if (node.config.getOrNull(SemanticsActions.OnClick) != null) {
                if (label.isBlank()) issues += "control without a label at $bounds"
                // Measured size, not the visible bounds: a chip half scrolled out of a row is clipped, not small.
                val size = node.size
                if (size.width < minTarget - 1f || size.height < minTarget - 1f) {
                    val w = with(density) { size.width.toDp().value.toInt() }
                    val h = with(density) { size.height.toDp().value.toInt() }
                    issues += "small touch target ${w}x${h} dp: $label"
                }
            }
        }
        return issues.distinct()
    }

    private fun visit(node: SemanticsNode, insideHorizontalScroll: Boolean, block: (SemanticsNode, Boolean) -> Unit) {
        block(node, insideHorizontalScroll)
        val scrolls = insideHorizontalScroll || node.config.getOrNull(SemanticsProperties.HorizontalScrollAxisRange) != null
        node.children.forEach { visit(it, scrolls, block) }
    }

    /** Own text or description, else the merged text of its children. */
    private fun describe(node: SemanticsNode): String {
        val own = (node.config.getOrNull(SemanticsProperties.Text).orEmpty().map { it.text } +
            node.config.getOrNull(SemanticsProperties.ContentDescription).orEmpty()).joinToString(" ")
        if (own.isNotBlank()) return own
        return node.children.joinToString(" ") { describe(it) }.trim()
    }
}
