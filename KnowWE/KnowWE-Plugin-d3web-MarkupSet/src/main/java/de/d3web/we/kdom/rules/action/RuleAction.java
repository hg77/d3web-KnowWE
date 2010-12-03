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

package de.d3web.we.kdom.rules.action;

import java.util.logging.Level;
import java.util.logging.Logger;

import de.d3web.we.kdom.DefaultAbstractKnowWEObjectType;
import de.d3web.we.kdom.KnowWEObjectType;
import de.d3web.we.kdom.basic.EndLineComment;
import de.d3web.we.kdom.sectionFinder.AllTextSectionFinder;
import de.knowwe.core.renderer.CommentRenderer;

public class RuleAction extends DefaultAbstractKnowWEObjectType {

	@Override
	protected void init() {
		sectionFinder = new AllTextSectionFinder();
		try {
			// TODO remove this evil workaround
			// when updating KnowWE architecture
			this.childrenTypes.add((KnowWEObjectType) Class.forName(
					"cc.knowwe.tdb.EvalAssignActionType").newInstance());
		}
		catch (Throwable e) {
			Logger.getLogger("KnowWE").log(Level.INFO,
					"cc.knowwe.tdb.EvalAssignActionType is not attached");
		}
		EndLineComment comment = new EndLineComment();
		comment.setCustomRenderer(new CommentRenderer());
		this.childrenTypes.add(comment);
		this.childrenTypes.add(new SolutionValueAssignment());
		this.childrenTypes.add(new SetQNumFormulaAction());
		this.childrenTypes.add(new SetQuestionValue());
		this.childrenTypes.add(new ContraIndicationAction());
		this.childrenTypes.add(new InstantIndication());
		this.childrenTypes.add(new RepeatedIndication());
		this.childrenTypes.add(new QASetIndicationAction());

	}
}
