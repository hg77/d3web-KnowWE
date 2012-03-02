/*
 * Copyright (C) 2009 Chair of Artificial Intelligence and Applied Informatics
 * Computer Science VI, University of Wuerzburg
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

package de.knowwe.diaflux.kbinfo;

import java.io.IOException;
import java.util.List;

import de.d3web.core.knowledge.KnowledgeBase;
import de.d3web.core.knowledge.terminology.Question;
import de.d3web.we.utils.D3webUtils;
import de.knowwe.core.KnowWEArticleManager;
import de.knowwe.core.KnowWEAttributes;
import de.knowwe.core.KnowWEEnvironment;
import de.knowwe.core.action.AbstractAction;
import de.knowwe.core.action.UserActionContext;
import de.knowwe.core.kdom.KnowWEArticle;
import de.knowwe.core.kdom.parsing.Section;

/**
 * @author Florian Ziegler
 */
public class UpdateQuestions extends AbstractAction {

	private static final String LINE_SEPARATOR = System.getProperty("line.separator");

	@Override
	public void execute(UserActionContext context) throws IOException {
		// get everything important from the parameter map
		String web = context.getParameter(KnowWEAttributes.WEB);
		String questionText = context.getParameter("text");
		String questionType = context.getParameter("type");
		String pageName = context.getParameter("pageName");
		String answersToLine = context.getParameter("answers");
		String[] answers = answersToLine.split("::");

		// get everything to update the article
		KnowWEArticleManager artManager = KnowWEEnvironment.getInstance().getArticleManager(web);
		KnowWEArticle article = artManager.getArticle(pageName);
		KnowWEEnvironment instance = KnowWEEnvironment.getInstance();
		Section<KnowWEArticle> sec = article.getSection();
		context.getParameters().put(KnowWEAttributes.WEB, sec.getWeb());
		String oldText = article.getSection().getText();

		// get everything fot the new Questions-section
		String[] surroundings = getRightInsertPosition(oldText, "Question");
		String firstPart = surroundings[0];
		String lastPart = surroundings[1];
		String currentQuestionSection = getCurrentSectionContent(oldText, "Question");
		String newQuestionsSection = addQuestion(currentQuestionSection, questionText,
				questionType, answers);

		// save the new article
		String newText = firstPart + newQuestionsSection + lastPart;
		instance.getWikiConnector().writeArticleToWikiEnginePersistence(sec.getTitle(),
				newText, context);

		// remove leading and ending quotes
		questionText = removeLeadingAndClosingQuotes(questionText);

		// get the right id for the nodemodel
		KnowledgeBase kb = D3webUtils.getKnowledgeRepresentationHandler(article.getWeb()).getKB(
				article.getTitle());
		List<Question> questions = kb.getManager().getQuestions();
		String questionID = null;

		for (Question question : questions) {
			if (question.getName().equals(revertSpecialCharacterEscape(questionText))) {
				questionID = question.getName();
				break;
			}
		}

		if (questionID != null) {

			StringBuilder bob = new StringBuilder();
			bob.append("<kbinfo>");
			// TODO hotfix
			String test = pageName + ".." + pageName + "_KB/" + questionID;

			GetInfoObjects.appendInfoObject(web, test, bob);
			bob.append("</kbinfo>");
			context.setContentType("text/xml; charset=UTF-8");
			context.getWriter().write(bob.toString());

		}
		else {
			context.setContentType("text/plain; charset=UTF-8");
			context.getWriter().write("Could not find id for: " + questionText);
		}

	}

	@Override
	public boolean isAdminAction() {
		return false;
	}

	/**
	 * some special characters cause bugs, so the have to be escaped in
	 * javascript. this method turns them back into normal special characters.
	 *
	 * @param text
	 * @return
	 */
	public static String revertSpecialCharacterEscape(String text) {
		text = text.replace("[FLOWCHART_PC]", "%");
		text = text.replace("[FLOWCHART_ST]", "<");
		text = text.replace("[FLOWCHART_AND]", "&");
		text = text.replace("[FLOWCHART_PLUS]", "+");
		text = text.replace("[FLOWCHART_CAP]", "^");
		text = text.replace("[FLOWCHART_BACKSLASH]", "\\");
		text = text.replace("[FLOWCHART_AG]", "`");
		text = text.replace("[FLOWCHART_HASH]", "#");
		text = text.replace("[FLOWCHART_oe]", "ö");
		text = text.replace("[FLOWCHART_OE]", "Ö");
		text = text.replace("[FLOWCHART_ae]", "ä");
		text = text.replace("[FLOWCHART_AE]", "Ä");
		text = text.replace("[FLOWCHART_ue]", "ü");
		text = text.replace("[FLOWCHART_UE]", "Ü");
		text = text.replace("[FLOWCHART_SS]", "ß");
		System.out.println(text);
		return text;
	}

	/**
	 * finds the right place for Solutions or Questions-section
	 *
	 * @param the article as string
	 * @param sectionName Questions or Solutions
	 * @return [0] everything before the new questions/solutions [1] everything
	 *         after the new questions/solutions
	 */
	public static String[] getRightInsertPosition(String article, String sectionName) {

		if (!(sectionName.equals("Question") || sectionName.equals("Solution"))) {
			throw new IllegalArgumentException("section must be either Question or Solution");
		}

		String[] surrounding = new String[2];
		String startTag = "%%" + sectionName;
		String endTag = "%";

		// if the arcticle already contains the section
		if (article.contains(startTag)
				&& article.contains(endTag)) {
			surrounding[0] = article.substring(0, article
					.indexOf(startTag) + startTag.length());
			surrounding[1] = article.substring(article
					.indexOf(endTag, article.indexOf(startTag) + 2));
		}
		else {
			surrounding[0] = article + LINE_SEPARATOR + startTag;
			surrounding[1] = endTag;

		}
		return surrounding;

	}

	/**
	 * returns the content of a questions or solutions-section
	 *
	 * @param article as string
	 * @param sectionName
	 */
	public static String getCurrentSectionContent(String article, String sectionName) {
		if (!(sectionName.equals("Question") || sectionName.equals("Solution"))) {
			return "";
		}
		String startTag = "%%" + sectionName;
		String endTag = "%";
		String questionSection = "";

		if (article.contains(startTag)
				&& article.contains(endTag)) {
			questionSection = article.substring(article.indexOf(startTag) + startTag.length());
			questionSection = questionSection.substring(0, questionSection.indexOf(endTag, 2));
			return questionSection;
		}
		return "";
	}

	/**
	 * removes leading and closing quotes
	 */
	public static String removeLeadingAndClosingQuotes(String questionText) {
		while (questionText.startsWith("\"")) {
			questionText = questionText.substring(1);
		}

		while (questionText.endsWith("\"")) {
			questionText = questionText.substring(0, questionText.length() - 1);
		}
		return questionText;
	}

	private String addQuestion(String questionsSection, String questionText, String questionType, String[] answers) {
		String newQuestionsSection = questionsSection;
		StringBuffer buffy = new StringBuffer();

		if (!questionsSection.contains("added Questions")) {
			buffy.append(LINE_SEPARATOR + "added Questions" + LINE_SEPARATOR);
			buffy.append("- " + revertSpecialCharacterEscape(questionText) + " [" + questionType
					+ "]" + LINE_SEPARATOR);
			if (!questionType.equals("num")) {
				for (String s : answers) {
					buffy.append("-- " + revertSpecialCharacterEscape(s) + LINE_SEPARATOR);
				}
			}

		}
		else {
			buffy.append("- " + revertSpecialCharacterEscape(questionText) + " [" + questionType
					+ "]" + LINE_SEPARATOR);
			if (!questionType.equals("num")) {
				for (String s : answers) {
					buffy.append("-- " + revertSpecialCharacterEscape(s) + LINE_SEPARATOR);
				}
			}
		}
		newQuestionsSection += buffy.toString();
		return newQuestionsSection;
	}

}