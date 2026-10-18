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

    private val minTouchTarget = 44.dp

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
                if (results.any { it.hasVisualOverflow }) issues += "text cut off: $label"
            }
            if (!inScroll && bounds.width > 0 && (bounds.right > width + 1f || bounds.left < -1f)) {
                issues += "past the screen edge (${bounds.left.toInt()}..${bounds.right.toInt()} of ${width.toInt()}): $label"
            }
            if (node.config.getOrNull(SemanticsActions.OnClick) != null) {
                if (label.isBlank()) issues += "control without a label at $bounds"
                if (bounds.width < minTarget - 1f || bounds.height < minTarget - 1f) {
                    val w = with(density) { bounds.width.toDp().value.toInt() }
                    val h = with(density) { bounds.height.toDp().value.toInt() }
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
