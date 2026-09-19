/*
 * Cure M3 - a Material 3 client for the Cure Afvalbeheer waste calendar.
 * Copyright (C) 2026 Pere Gomila
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
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
