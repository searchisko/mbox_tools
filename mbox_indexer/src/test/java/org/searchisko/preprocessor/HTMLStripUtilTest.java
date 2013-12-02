package org.searchisko.preprocessor;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.HashMap;
import java.util.Map;

import static junit.framework.Assert.assertEquals;

/**
 * @author Lukáš Vlček
 */
@RunWith(JUnit4.class)
public class HTMLStripUtilTest {

	@Test
	public void shouldWorkForNullString() {
		Map<String, Object> map = new HashMap<>();
		map.put(HTMLStripUtil.SOURCE_FIELD, null);
		HTMLStripUtil.stripHTML(map);
		assertEquals(null, map.get(HTMLStripUtil.TARGET_FIELD));
	}

	@Test
	public void shouldWorkForEmptyString() {
		Map<String, Object> map = new HashMap<>();
		map.put(HTMLStripUtil.SOURCE_FIELD, "");
		HTMLStripUtil.stripHTML(map);
		assertEquals("", map.get(HTMLStripUtil.TARGET_FIELD));
	}

	@Test
	public void shouldStripHTML() {
		Map<String, Object> map = new HashMap<>();
		map.put(HTMLStripUtil.SOURCE_FIELD, "<p><h1>Title</h1>Dummy<br><i>text</i></p><p><ul><li>A<li>B<li>C</ul</p>");
		HTMLStripUtil.stripHTML(map);
		assertEquals("Title Dummy text A B C ", map.get(HTMLStripUtil.TARGET_FIELD));
	}

	@Test
	public void shouldIgnoreScript() {
		Map<String, Object> map = new HashMap<>();
		map.put(HTMLStripUtil.SOURCE_FIELD, "<div>There is<script>var x = {};</script> no script</div>");
		HTMLStripUtil.stripHTML(map);
		assertEquals("There is no script ", map.get(HTMLStripUtil.TARGET_FIELD));
	}
}
