/*
 * Copyright (C) 2009 Chair of Artificial Intelligence and Applied Informatics
 *                    Computer Science VI, University of Wuerzburg
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 3 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package de.d3web.we.kdom.rulesNew.ruleAction;

import de.d3web.abstraction.ActionAddValue;
import de.d3web.core.inference.PSAction;
import de.d3web.core.knowledge.terminology.Choice;
import de.d3web.core.knowledge.terminology.Question;
import de.d3web.we.kdom.DefaultAbstractKnowWEObjectType;
import de.d3web.we.kdom.KnowWEArticle;
import de.d3web.we.kdom.Section;
import de.d3web.we.kdom.basic.AnonymousType;
import de.d3web.we.kdom.objects.AnswerReference;
import de.d3web.we.kdom.objects.AnswerReferenceImpl;
import de.d3web.we.kdom.objects.QuestionReference;
import de.d3web.we.kdom.sectionFinder.AllBeforeTypeSectionFinder;
import de.d3web.we.kdom.sectionFinder.AllTextFinderTrimmed;
import de.d3web.we.kdom.sectionFinder.ConditionalAllTextFinder;
import de.d3web.we.kdom.sectionFinder.StringSectionFinderUnquoted;
import de.d3web.we.utils.SplitUtility;

public class AddValueAction extends DefaultAbstractKnowWEObjectType {

	public static final String ADD_VALUE_SIGN = "+=";

	public AddValueAction() {
		this.addChildType(new NumericalAddValueAction());
		this.addChildType(new ChoiceAddValueAction());
		this.setSectionFinder(new ConditionalAllTextFinder() {
			@Override
			protected boolean condition(String text, Section father) {
				return SplitUtility.containsUnquoted(text, ADD_VALUE_SIGN);
			}
		});
	}


	class NumericalAddValueAction extends D3webRuleAction<NumericalAddValueAction> {

		public NumericalAddValueAction() {
			AnonymousType equals = new AnonymousType("plus-equal");
			equals.setSectionFinder(new StringSectionFinderUnquoted(ADD_VALUE_SIGN));

			QuestionReference qr = new QuestionReference();
			qr.setSectionFinder(new AllBeforeTypeSectionFinder(equals));
			this.childrenTypes.add(equals);
			this.childrenTypes.add(qr);

			de.d3web.we.kdom.rulesNew.terminalCondition.Number a = new de.d3web.we.kdom.rulesNew.terminalCondition.Number();
			a.setSectionFinder(new AllTextFinderTrimmed());
			this.childrenTypes.add(a);

			this.setSectionFinder(new ConditionalAllTextFinder() {
				@Override
				protected boolean condition(String text, Section father) {
					int index = SplitUtility.indexOfUnquoted(text, ADD_VALUE_SIGN);

					String value = text.substring(index + 2).trim();
					try {
						Double d = Double.parseDouble(value);
						return true;
					}
					catch (Exception e) {
						return false;
					}
				}
			});
		}

		@Override
		public PSAction getAction(KnowWEArticle article, Section<NumericalAddValueAction> s) {
			Section<QuestionReference> qref = s.findSuccessor(QuestionReference.class);
			Question q = qref.get().getTermObject(article, qref);
			Section<de.d3web.we.kdom.rulesNew.terminalCondition.Number> aref = s.findSuccessor(de.d3web.we.kdom.rulesNew.terminalCondition.Number.class);

			Double d = aref.get().getNumber(aref);
			if (q != null && d != null) {
				ActionAddValue a = new ActionAddValue();
				a.setQuestion(q);
				a.setValue(d);
				return a;
			}
			return null;
		}

	}

	class ChoiceAddValueAction extends D3webRuleAction<NumericalAddValueAction> {
		public ChoiceAddValueAction() {
			AnonymousType equals = new AnonymousType("plus-equal");
			equals.setSectionFinder(new StringSectionFinderUnquoted(ADD_VALUE_SIGN));

			QuestionReference qr = new QuestionReference();
			qr.setSectionFinder(new AllBeforeTypeSectionFinder(equals));
			this.childrenTypes.add(equals);
			this.childrenTypes.add(qr);

			AnswerReference a = new AnswerReferenceImpl();
			a.setSectionFinder(new AllTextFinderTrimmed());
			this.childrenTypes.add(a);
			this.sectionFinder = new AllTextFinderTrimmed();
		}

		@Override
		public PSAction getAction(KnowWEArticle article, Section<NumericalAddValueAction> s) {
			Section<QuestionReference> qref = s.findSuccessor(QuestionReference.class);
			Question q = qref.get().getTermObject(article, qref);
			Section<AnswerReference> aref = s.findSuccessor(AnswerReference.class);
			Choice c = aref.get().getTermObject(article, aref);

			if (q != null && c != null) {
				ActionAddValue a = new ActionAddValue();
				a.setQuestion(q);
				a.setValue(c);
				return a;
			}
			return null;
		}
	}
}
