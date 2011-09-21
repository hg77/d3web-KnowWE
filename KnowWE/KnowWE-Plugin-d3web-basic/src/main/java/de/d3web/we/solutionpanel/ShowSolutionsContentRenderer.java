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
package de.d3web.we.solutionpanel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import de.d3web.core.knowledge.TerminologyObject;
import de.d3web.core.knowledge.terminology.Question;
import de.d3web.core.knowledge.terminology.Rating;
import de.d3web.core.knowledge.terminology.Rating.State;
import de.d3web.core.knowledge.terminology.Solution;
import de.d3web.core.knowledge.terminology.info.BasicProperties;
import de.d3web.core.knowledge.terminology.info.MMInfo;
import de.d3web.core.session.Session;
import de.d3web.core.session.Value;
import de.d3web.core.session.values.MultipleChoiceValue;
import de.d3web.core.session.values.NumValue;
import de.d3web.core.session.values.UndefinedValue;
import de.d3web.core.session.values.Unknown;
import de.d3web.we.kdom.KnowWEArticle;
import de.d3web.we.kdom.Section;
import de.d3web.we.kdom.Sections;
import de.d3web.we.kdom.defaultMarkup.ContentType;
import de.d3web.we.kdom.rendering.KnowWEDomRenderer;
import de.d3web.we.user.UserContext;
import de.d3web.we.utils.D3webUtils;
import de.d3web.we.utils.KnowWEUtils;

/**
 * Displays a configurable pane presenting derived solutions and abstractions.
 * The following options are available:
 * <ul>
 * <li>@show_established: true/false
 * <li>@show_suggested: true/false
 * <li>@show_excluded: true/false
 * <li>@show_abstractions: true/false
 * <li>@only_derivations: questionnaire name
 * <li>@show_digits: 0..NUMBER of fractional digits to be shown
 * <li>@master: Name of the article with the knowledge base
 * </ul>
 * 
 * @author Joachim Baumeister (denkbares GmbH)
 * @created 15.10.2010
 */
public class ShowSolutionsContentRenderer extends KnowWEDomRenderer<ContentType> {

	public ShowSolutionsContentRenderer() {
	}

	@Override
	public void render(KnowWEArticle article, Section<ContentType> section, UserContext user, StringBuilder string) {
		string.append(KnowWEUtils.maskHTML("<span id='" + section.getID() + "'>"));
		String text = section.getOriginalText();
		if (!text.isEmpty()) {
			string.append(text + "\n");
		}

		String masterArticleName = ShowSolutionsType.getMaster(getShowSolutionsSection(section));
		Session session = getSessionFor(masterArticleName, article.getWeb(), user);
		if (session == null) {
			string.append("No knowledge base for: " + masterArticleName + "\n");
		}
		else {
			string.append(renderSolutions(section, session));
			string.append(renderAbstractions(section, session));
		}
		string.append(KnowWEUtils.maskHTML("</span>"));
	}

	private Section<ShowSolutionsType> getShowSolutionsSection(Section<ContentType> section) {
		return Sections.findAncestorOfType(section, ShowSolutionsType.class);
	}

	/**
	 * Renders the derived abstractions when panel opted for it.
	 */
	private StringBuffer renderAbstractions(Section<ContentType> section, Session session) {
		// Check, if the shown abstractions are limited to a number of
		// questionnaires
		Section<ShowSolutionsType> parentSection = getShowSolutionsSection(section);
		String[] allowedQuestionnaires = ShowSolutionsType.getShownAbstraction(parentSection);

		StringBuffer buffer = new StringBuffer();
		if (ShowSolutionsType.shouldShowAbstractions(parentSection)) {
			List<Question> abstractions = new ArrayList<Question>();
			for (Question question : session.getBlackboard().getAnsweredQuestions()) {
				Boolean isAbstract = question.getInfoStore().getValue(
						BasicProperties.ABSTRACTION_QUESTION);
				if (isAbstract != null && isAbstract) {
					if (allowedQuestionnaires.length == 0 || // no restriction,
																// so
																// always insert
							contains(allowedQuestionnaires, question)) {
						abstractions.add(question);
					}
				}
			}
			Collections.sort(abstractions, new Comparator<Question>() {

				@Override
				public int compare(Question o1, Question o2) {
					return o1.getName().compareTo(o2.getName());
				}
			});
			for (Question question : abstractions) {
				addListItem(buffer, question, session, section);
			}
		}
		return buffer;
	}

	private boolean contains(String[] allowedParents, TerminologyObject object) {
		if (object.getParents() == null) {
			return false;
		}
		else {
			for (TerminologyObject parent : object.getParents()) {
				if (arrayIgnoreCaseContains(allowedParents, parent.getName())) {
					return true;
				}
				else {
					boolean tester = contains(allowedParents, parent);
					if (tester == true) return true;
				}
			}
		}
		return false;
	}

	private boolean arrayIgnoreCaseContains(String[] allowedParents, String name) {
		for (String string : allowedParents) {
			if (name.equalsIgnoreCase(string)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Renders the derived solutions when panel opted for it.
	 */
	private StringBuffer renderSolutions(Section<ContentType> section, final Session session) {
		StringBuffer content = new StringBuffer();
		List<Solution> allSolutions = new ArrayList<Solution>();
		Section<ShowSolutionsType> parentSection = getShowSolutionsSection(section);

		// collect the solutions to be presented
		// --- established solutions are presented by default and have to be
		// --- opted out
		if (ShowSolutionsType.shouldShowEstablished(parentSection)) {
			allSolutions.addAll(session.getBlackboard().getSolutions(State.ESTABLISHED));
		}
		if (ShowSolutionsType.shouldShowSuggested(parentSection)) {
			allSolutions.addAll(session.getBlackboard().getSolutions(State.SUGGESTED));
		}
		if (ShowSolutionsType.shouldShowExcluded(parentSection)) {
			allSolutions.addAll(session.getBlackboard().getSolutions(State.EXCLUDED));
		}

		// sort the solutions to be presented
		Collections.sort(allSolutions, new Comparator<Solution>() {

			@Override
			public int compare(Solution o1, Solution o2) {
				Rating rating1 = session.getBlackboard().getRating(o1);
				Rating rating2 = session.getBlackboard().getRating(o2);
				int comparison = rating2.compareTo(rating1);
				if (comparison == 0) return o1.getName().compareTo(o2.getName());
				return comparison;
			}
		});

		// format the solutions
		for (Solution solution : allSolutions) {
			addListItem(content, solution, session);
		}

		return content;
	}

	private void addListItem(StringBuffer content, Solution solution, Session session) {
		// TODO: look for internationalization and only print getName,
		// when no intlz is available
		// content.append("* ");

		Rating solutionRating = session.getBlackboard().getRating(solution);
		String stateName = solutionRating.toString();

		if (solutionRating.hasState(State.ESTABLISHED)) {
			content.append(renderImage("KnowWEExtension/images/fsp_established.gif", "Established"));
		}
		else if (solutionRating.hasState(State.SUGGESTED)) {
			content.append(renderImage("KnowWEExtension/images/fsp_suggested.gif", "Suggested"));
		}
		else if (solutionRating.hasState(State.EXCLUDED)) {
			content.append(renderImage("KnowWEExtension/images/fsp_excluded.gif", "Excluded"));
		}
		// render span for better testability
		content.append(mask("<span class=\"SOLUTION-" + stateName + "\">"));
		content.append(solution.getName());
		content.append(mask("</span>"));

		content.append(br() + "\n");
	}

	private String renderImage(String filename, String altText) {
		return mask(" <img src='" + filename
						+ "' id='sstate-update' class='pointer'"
							+ " align='top' alt='" + altText + "'"
							+ " title='" + altText + "' "
						+ "/> ");
	}

	private String mask(String string) {
		return KnowWEUtils.maskHTML(string);
	}

	private void addListItem(StringBuffer buffer, Question question, Session session, Section<ContentType> section) {
		// TODO: look for internationalization and only print getName,
		// when no intlz is available
		// buffer.append("* ");
		buffer.append(renderImage("KnowWEExtension/images/fsp_abstraction.gif", "Abstraction"));
		buffer.append(mask("<span class=\"ABSTRACTION\">"));
		// render the abstraction question with value
		buffer.append(question.getName() + " = "
				+ formatValue(session.getBlackboard().getValue(question), section));

		// add the unit name for num question, if available
		String unit = question.getInfoStore().getValue(MMInfo.UNIT);
		if (unit != null) {
			buffer.append(" " + unit);
		}

		buffer.append(mask("</span>") + br() + "\n");
	}

	private String br() {
		return "";
	}

	/**
	 * Renders the string representation of the specified value. For a
	 * {@link NumValue} the float is truncated to its integer value, when
	 * possible.
	 * 
	 * @created 19.10.2010
	 * @param value the specified value
	 * @return A string representation of the specified value.
	 */
	private String formatValue(Value value, Section<ContentType> section) {

		if (value instanceof NumValue) {
			Double numValue = (Double) value.getValue();
			// check, if we need to round the value
			int digits = ShowSolutionsType.numberOfShownDigits(getShowSolutionsSection(section));
			if (digits >= 0) {
				double d = Math.pow(10, digits);
				numValue = (Math.round(numValue * d) / d);
			}
			// cut an ending .0 when appropriate
			if (Math.abs(numValue - Math.round(numValue)) > 0) {
				return numValue.toString();
			}
			else {
				return "" + Math.round(numValue);
			}
		}
		else if (value instanceof MultipleChoiceValue) {
			String mcText = value.toString();
			// remove the brackets
			return mcText.substring(1, mcText.length() - 1);
		}
		else if (value instanceof Unknown || value instanceof UndefinedValue) return "-";
		else return value.toString();
	}

	private Session getSessionFor(String articleName, String webName, UserContext user) {
		return D3webUtils.getSession(articleName, user, webName);
	}

}