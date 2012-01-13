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
package de.d3web.we.object;

import java.util.Collection;

import de.d3web.core.knowledge.KnowledgeBase;
import de.d3web.core.knowledge.terminology.NamedObject;
import de.d3web.core.knowledge.terminology.QASet;
import de.d3web.core.knowledge.terminology.QContainer;
import de.d3web.we.reviseHandler.D3webSubtreeHandler;
import de.d3web.we.utils.D3webUtils;
import de.knowwe.core.compile.Priority;
import de.knowwe.core.compile.TerminologyHandler;
import de.knowwe.core.kdom.KnowWEArticle;
import de.knowwe.core.kdom.parsing.Section;
import de.knowwe.core.report.Message;
import de.knowwe.core.report.Messages;
import de.knowwe.core.utils.KnowWEUtils;
import de.knowwe.kdom.renderer.StyleRenderer;

/**
 * 
 * Abstract Type for the definition of questionnaires
 * 
 * @author Jochen/Albrecht
 * @created 26.07.2010
 */
public abstract class QuestionnaireDefinition extends QASetDefinition<QContainer> {

	public QuestionnaireDefinition() {
		super(QContainer.class);
		addSubtreeHandler(Priority.HIGHEST, new CreateQuestionnaireHandler());
		setCustomRenderer(StyleRenderer.Questionaire);
		setOrderSensitive(true);
	}

	public abstract int getPosition(Section<QuestionnaireDefinition> s);

	static class CreateQuestionnaireHandler
			extends D3webSubtreeHandler<QuestionnaireDefinition> {

		@Override
		public Collection<Message> create(KnowWEArticle article,
				Section<QuestionnaireDefinition> s) {

			String name = s.get().getTermIdentifier(s);

			TerminologyHandler terminologyHandler = KnowWEUtils.getTerminologyHandler(article.getWeb());
			terminologyHandler.registerTermDefinition(article, s);
			if (terminologyHandler.getTermDefiningSection(article, s) != s) {
				return s.get().handleRedundantDefinition(article, s);
			}

			KnowledgeBase kb = getKB(article);

			NamedObject o = kb.getManager().searchQContainer(name);

			if (o != null) {
				if (!(o instanceof QContainer)) {
					return Messages.asList(Messages.occupiedTermError(name, o.getClass()));
				}
				return Messages.asList();
			}
			QASet parent = kb.getRootQASet();
			QContainer qc = new QContainer(parent, name);

			s.get().storeTermObject(article, s, qc);
			return Messages.asList(Messages.objectCreatedNotice(
						qc.getClass().getSimpleName()
								+ " " + qc.getName()));
		}

		@Override
		public void destroy(KnowWEArticle article,
				Section<QuestionnaireDefinition> s) {

			QContainer q = s.get().getTermObject(article, s);
			if (q != null) {
				D3webUtils.removeRecursively(q);
				KnowWEUtils.getTerminologyHandler(article.getWeb()).unregisterTermDefinition(
						article, s);
			}
		}

	}

}
