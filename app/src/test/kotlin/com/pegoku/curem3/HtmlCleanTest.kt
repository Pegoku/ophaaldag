package com.pegoku.curem3

import com.pegoku.curem3.ui.components.cleanHtml
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HtmlCleanTest {
    @Test
    fun listsGetBulletsAndHeadingsBecomeBoldParagraphs() {
        val out = cleanHtml("<h4 class=\"x\">Title</h4>\r\n<ul>\r\n<li>One</li>\r\n<li>Two</li>\r\n</ul>")
        assertEquals("<p><b>Title</b></p><p>&#8226;&nbsp;&nbsp;One</p><p>&#8226;&nbsp;&nbsp;Two</p>", out.replace(Regex("\\s+"), ""))
    }

    @Test
    fun emptyParagraphsImagesAndDoubleBreaksAreRemoved() {
        val out = cleanHtml("<p>&nbsp;</p><p>A<br /><br />B<br /></p><img src=\"x.png\" /><p><br/>C</p>")
        assertEquals("<p>A<br>B</p><p>C</p>", out)
    }

    @Test
    fun tablesDegradeToLines() {
        val out = cleanHtml("<table><tr><td>Ma</td><td>8-17</td></tr></table>")
        assertTrue(out.contains("Ma - 8-17"))
        assertFalse(out.contains("<td"))
    }
}
