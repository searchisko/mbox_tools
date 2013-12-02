package org.searchisko.preprocessor;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.safety.Whitelist;
import org.jsoup.select.NodeTraversor;
import org.jsoup.select.NodeVisitor;

import java.util.Map;

/**
 * Strips HTML from <code>sys_content</code> field and populate result into <code>sys_content_plaintext</code> field.
 *
 * @author Lukáš Vlček
 */
public class HTMLStripUtil {

	public static String SOURCE_FIELD = "sys_content";
	public static String TARGET_FIELD = "sys_content_plaintext";

	/**
	 * Strip HTML from <code>sys_content</code> field and store it (replace) to <code>sys_content_plaintext</code> field
	 * in incoming map. (i.e. it modify input object)
	 * @param input
	 */
	public static void stripHTML(Map<String, Object> input) {
		if (input != null && input.containsKey(SOURCE_FIELD)) {
			String sys_content = (String) input.get(SOURCE_FIELD);
			if (sys_content != null) {
				Document doc = Jsoup.parse(Jsoup.clean(sys_content, Whitelist.relaxed()));
				input.put(TARGET_FIELD, convertNodeToText(doc.body()));
			}
		}
	}

	private static String convertNodeToText(Element element)
	{
		final StringBuilder buffer = new StringBuilder();

		new NodeTraversor(new NodeVisitor() {

			@Override
			public void head(Node node, int depth) {
				if (node instanceof TextNode) {
					TextNode textNode = (TextNode) node;
					String text = textNode.text().replace('\u00A0', ' ').trim();
					if(!text.isEmpty())
					{
						buffer.append(text);
						if (!text.endsWith(" ")) {
							buffer.append(" ");
						}
					}
				}
			}

			@Override
			public void tail(Node node, int depth) {
			}
		}).traverse(element);

		return buffer.toString();
	}
}
