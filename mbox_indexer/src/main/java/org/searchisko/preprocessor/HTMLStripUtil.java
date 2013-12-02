package org.searchisko.preprocessor;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.safety.Whitelist;
import org.jsoup.select.NodeTraversor;
import org.jsoup.select.NodeVisitor;

/**
 * Strips HTML utils.
 *
 * @author Lukáš Vlček
 */
public class HTMLStripUtil {

	/**
	 * Strip HTML out of input string.
	 * @param html
	 * @return
	 */
	public static String stripHTML(String html) {
		if (html != null) {
			Document doc = Jsoup.parse(Jsoup.clean(html, Whitelist.relaxed()));
			return convertNodeToText(doc.body());
		}
		return null;
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
