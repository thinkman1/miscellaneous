package com.jpmc.dart.util.staxutils.predicate;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.namespace.QName;
import javax.xml.stream.events.StartElement;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;

import com.jpmc.dart.util.staxutils.CurrentDocumentState;
import com.jpmc.dart.util.staxutils.StreamingXmlReader;

/**
 * Parses an xpath and can indicate if the current state of a
 * StreamingXmlVisitor matches that path. This class stores no state and should
 * be created once and re-used.
 * 
 * The following expressions are supported: - simple selects /foo/bar - selects
 * with exists predicates: /foo[@prop] - selects with value predicates:
 * /foo[@prop='value'] - selects with and/or predicates: /foo[@foo and
 * @bar='baz']/dart /foo[@foo or @bar='baz']/dart - wildcard selects /foo//bar
 * //bar foo/bar foo//bar - property selects //@baz /foo/bar/@baz - numeric
 * predicate selects /foo[1]/bar[2]/baz[5]
 * 
 * @author E001668
 * 
 */
public class StreamingXmlPath {
	private Pattern predicatePattern = Pattern.compile("and|or|AND|OR|\\(|\\)");

	public List<String> tokenizePredicate(String predicate) {
		Matcher matcher = predicatePattern.matcher(predicate);

		List<String> tokens = new ArrayList<String>();
		int lastEnd = 0;
		while (matcher.find()) {
			tokens.add(predicate.substring(lastEnd, matcher.start()));
			tokens.add(predicate.substring(matcher.start(), matcher.end()));
			lastEnd = matcher.end();
		}
		tokens.add(predicate.substring(lastEnd));

		ListIterator<String> iterator = tokens.listIterator();
		while (iterator.hasNext()) {
			String trim = StringUtils.trimToEmpty(iterator.next());
			if (StringUtils.EMPTY.equals(trim)) {
				iterator.remove();
			}
			iterator.set(trim);
		}

		return tokens;
	}

	private String pathText;
	private List<XmlPathPart> pathParts = new ArrayList<XmlPathPart>();
	private Pattern matchPath;
	private boolean selectProperty = false;
	private QName selectPropertyName;

	public List<XmlPathPart> getPathParts() {
		return pathParts;
	}

	public StreamingXmlPath(String path) throws Exception {
		this.pathText = path;
		XPath xp = XPathFactory.newInstance().newXPath();
		xp.compile(path);
		parsePath();
	}

	private QName getNameFromString(String name) {
		int col = name.indexOf(":");
		if (col > -1) {
			String prefix = name.substring(0, col);
			return new QName(null, name.substring(col + 1), prefix);
		}

		return new QName(name);
	}

	private void getNameForRegex(QName name, StringBuilder builder) {
		if (builder.length() == 0) {
			builder.append("/");
		} else if (builder.charAt(builder.length() - 1) != '/') {
			builder.append("/");
		}

		if (!StringUtils.isEmpty(name.getPrefix())) {
			builder.append(name.getPrefix());
			builder.append(":");
		}
		builder.append(name.getLocalPart());
	}

	private void parsePath() {
		// split into elements.
		String parts[] = this.pathText.split("/");
		StringBuilder matchPathExpression = new StringBuilder();

		if (!this.pathText.startsWith("/")) {
			XmlPathPart startWild = new XmlPathPart();
			startWild.any = true;
			this.pathParts.add(startWild);
			matchPathExpression.append("/");
			matchPathExpression.append(XpathPredicate.ANY);
		}

		for (int i = 0; i < parts.length; i++) {
			String part = parts[i];
			XmlPathPart xpart = new XmlPathPart();
			boolean add = true;

			if (StringUtils.isEmpty(part)) {
				if ((i != 0)) {
					xpart.any = true;
					matchPathExpression.append("(/");
					matchPathExpression.append(XpathPredicate.ANY);
					matchPathExpression.append(")?");
				} else {
					add = false;
				}
			} else if (part.contains("[")) {
				String name = part.substring(0, part.indexOf("["));
				String predicate = part.substring(part.indexOf("[", 0) + 1, part.lastIndexOf("]"));

				xpart.tagName = getNameFromString(name);

				if (NumberUtils.isDigits(predicate)) {
					xpart.count = Integer.parseInt(predicate);
				} else {
					// now the predicate
					xpart.predicateCondition = new XpathPredicate(tokenizePredicate(predicate));
				}
				getNameForRegex(xpart.tagName, matchPathExpression);
			} else if (part.startsWith("@")) {
				if (i == parts.length - 1) {
					// this is only a property select if this is the last item
					this.selectProperty = true;
					xpart.selectProperty = true;

					this.selectPropertyName = new QName(part.substring(1));
					add = false;
				} else {
					xpart.any = true;
					// now the predicate
					xpart.predicateCondition = new XpathPredicate(tokenizePredicate(part));
				}
			} else {
				xpart.tagName = getNameFromString(part);
				matchPathExpression.append("/");
				getNameForRegex(xpart.tagName, matchPathExpression);
			}

			// if this selects a property, don't add a null xpart.
			if (add) {
				this.pathParts.add(xpart);
			}
		}
		matchPathExpression.append("$");

		matchPath = Pattern.compile(matchPathExpression.toString());
	}

	public String getXpath() {
		return pathText;
	}

	public boolean isSelectProperty() {
		return selectProperty;
	}

	public QName getSelectPropertyName() {
		return selectPropertyName;
	}

	/**
	 * return true if the current stack of start elements matches this path.
	 * 
	 * @param startElements
	 * @return
	 */
	public boolean matches(CurrentDocumentState state) {

		if ((this.pathText.contentEquals(state.getCurrentPath()))
				|| (matchPath.matcher(state.getCurrentPath()).matches())) {
			// since we screen the incoming stuff with a regex (with will make
			// sure the tags are in order) we
			// just have to make sure all the name/propery rules are hit.
			List<XmlPathPart> availableParts = new ArrayList<XmlPathPart>(getPathParts());

			StringBuilder partialPathPart = new StringBuilder();

			// start from the back, move to the front
			for (int i = state.getStartElements().size() - 1; i >= 0; i--) {
				StartElement ele = state.getStartElements().get(i);
				if (availableParts.isEmpty()) {
					break;
				}
				XmlPathPart lastPart = availableParts.get(availableParts.size() - 1);

				if (lastPart.equals(ele)) {
					if (lastPart.count > -1) {
						// build the path that reaches this element
						StreamingXmlReader.buildXpath(partialPathPart, state.getStartElements().subList(0, i + 1));

						// now see how many times the tag has been hit
						int count = state.getCountForPath(partialPathPart);
						if (count == lastPart.count) {
							availableParts.remove(availableParts.size() - 1);
						}
					} else {
						availableParts.remove(availableParts.size() - 1);
					}
				} else if (i == state.getStartElements().size() - 1) {
					// if this is the first element and not equal, we fail.
					return false;
				}
			}

			// if we have a //foo type of select you may have an extra left over
			// wildcard for the path /foo
			// go ahead an pop it.
			if (!availableParts.isEmpty()) {
				if (availableParts.size() == 1) {
					if (availableParts.get(0).any) {
						availableParts.clear();
					}
				}
			}

			if (availableParts.isEmpty()) {
				if (selectProperty) {
					StartElement last = state.getStartElements().get(state.getStartElements().size() - 1);
					if (last.getAttributeByName(selectPropertyName) == null) {
						return false;
					}
				}

				return true;
			}
		}
		return false;
	}
}