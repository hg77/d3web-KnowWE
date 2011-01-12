/*
 * Copyright (C) 2010 denkbares GmbH
 * 
 * This is free software; you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option) any
 * later version.
 * 
 * This software is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this software; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA, or see the FSF
 * site: http://www.fsf.org.
 */
package de.knowwe.d3web.property;

import java.util.regex.Pattern;

import de.d3web.core.knowledge.terminology.Choice;
import de.d3web.core.knowledge.terminology.IDObject;
import de.d3web.core.knowledge.terminology.QContainer;
import de.d3web.core.knowledge.terminology.Question;
import de.d3web.core.knowledge.terminology.Solution;
import de.d3web.we.kdom.DefaultAbstractKnowWEObjectType;
import de.d3web.we.kdom.IncrementalConstraints;
import de.d3web.we.kdom.KnowWEArticle;
import de.d3web.we.kdom.Priority;
import de.d3web.we.kdom.Section;
import de.d3web.we.kdom.basic.PlainText;
import de.d3web.we.kdom.objects.KnowWETermMarker;
import de.d3web.we.kdom.rendering.KnowWEDomRenderer;
import de.d3web.we.kdom.rendering.StyleRenderer;
import de.d3web.we.kdom.sectionFinder.AllTextFinderTrimmed;
import de.d3web.we.kdom.sectionFinder.RegexSectionFinder;
import de.d3web.we.object.ContentDefinition;
import de.d3web.we.object.IDObjectReference;
import de.d3web.we.object.LocaleDefinition;
import de.d3web.we.object.PropertyReference;
import de.d3web.we.utils.KnowWEUtils;
import de.d3web.we.utils.Patterns;
import de.d3web.we.wikiConnector.KnowWEUserContext;

/**
 * Adds the PropertyReviseSubtreeHandler to the Property line
 * 
 * @author Markus Friedrich (denkbares GmbH)
 * @created 10.11.2010
 */
public class PropertyType extends DefaultAbstractKnowWEObjectType implements KnowWETermMarker, IncrementalConstraints {

	/**
	 * 
	 * @author volker_belli
	 * @created 15.12.2010
	 */
	private static final class PropertyIDObbjetReferenceRenderer extends KnowWEDomRenderer<IDObjectReference> {

		@Override
		public void render(KnowWEArticle article, Section<IDObjectReference> sec, KnowWEUserContext user, StringBuilder string) {
			IDObject object = sec.get().getTermObject(article, sec);
			KnowWEDomRenderer renderer;
			if (object instanceof Question) {
				renderer = StyleRenderer.Question;
			}
			else if (object instanceof QContainer) {
				renderer = StyleRenderer.Questionaire;
			}
			else if (object instanceof Solution) {
				renderer = StyleRenderer.SOLUTION;
			}
			else if (object instanceof Choice) {
				renderer = StyleRenderer.CHOICE;
			}
			else {
				renderer = PlainText.getInstance().getRenderer();
			}
			renderer.render(article, sec, user, string);
		}
	}

	public PropertyType() {
		setSectionFinder(new AllTextFinderTrimmed());
		String quoted = Patterns.quoted;
		// no " . # and = allowed
		String unquotedName = "(?:[^\".=#])+";
		String name = "(?:" + quoted + "|" + unquotedName + ")";
		String language = "(\\.\\w{2}(?:\\.\\w{2})?)?";
		String idObject = "(" + name + "(?:#" + name + ")?" + ")";
		String leftSide = idObject + "\\.(" + name + ")" + language;
		// no " and = allowed, dots are allowed
		String unquotedContent = "(?:[^\"=])+";
		String content = "(" + quoted + "|" + unquotedContent + ")";
		String pattern = leftSide + "\\s*=" + content;
		Pattern p = Pattern.compile(pattern);

		// Locale
		LocaleDefinition ld = new LocaleDefinition();
		ld.setSectionFinder(new RegexSectionFinder(p, 3));
		this.childrenTypes.add(ld);

		// Content
		ContentDefinition cd = new ContentDefinition();
		cd.setSectionFinder(new RegexSectionFinder(Pattern.compile("=\\s*" + content), 1));
		this.childrenTypes.add(cd);

		// Property
		PropertyReference pr = new PropertyReference();
		pr.setSectionFinder(new RegexSectionFinder(Pattern.compile("\\.(" + name + ")"), 1));
		this.childrenTypes.add(pr);

		// IDObject
		IDObjectReference idor = new IDObjectReference();
		idor.setSectionFinder(new RegexSectionFinder(Pattern.compile(idObject + "\\."), 1));
		idor.setCustomRenderer(new PropertyIDObbjetReferenceRenderer());
		this.childrenTypes.add(idor);

		addSubtreeHandler(Priority.LOW, new PropertyReviseSubtreeHandler());
	}

	@Override
	public boolean hasViolatedConstraints(KnowWEArticle article, Section<?> s) {
		return KnowWEUtils.getTerminologyHandler(
				article.getWeb()).areTermDefinitionsModifiedFor(article);
	}

}
