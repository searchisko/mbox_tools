package org.searchisko.preprocessor;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static junit.framework.Assert.assertEquals;

/**
 * @author Lukáš Vlček
 */
@RunWith(JUnit4.class)
public class HTMLStripUtilTest {

	@Test
	public void shouldWorkForNullString() {
		assertEquals(null, HTMLStripUtil.stripHTML(null));
	}

	@Test
	public void shouldWorkForEmptyString() {
		assertEquals("", HTMLStripUtil.stripHTML(""));
	}

	@Test
	public void shouldStripHTML() {
		String html = "<p><h1>Title</h1>Dummy<br><i>text</i></p><p><ul><li>A<li>B<li>C</ul</p>";
		assertEquals("Title Dummy text A B C", HTMLStripUtil.stripHTML(html));
	}

	@Test
	public void shouldIgnoreScript() {
		String html = "<div>There is<script>var x = {};</script> no script</div>";
		assertEquals("There is no script", HTMLStripUtil.stripHTML(html));
	}
}
