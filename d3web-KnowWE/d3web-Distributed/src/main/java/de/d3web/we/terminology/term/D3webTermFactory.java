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

package de.d3web.we.terminology.term;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import de.d3web.core.knowledge.TerminologyObject;
import de.d3web.core.knowledge.terminology.Solution;
import de.d3web.core.knowledge.terminology.NamedObject;
import de.d3web.core.knowledge.terminology.QContainer;
import de.d3web.core.knowledge.terminology.Question;
import de.d3web.core.knowledge.terminology.QuestionChoice;
import de.d3web.core.knowledge.terminology.QuestionNum;
import de.d3web.core.knowledge.terminology.info.Property;
import de.d3web.core.session.values.Choice;
import de.d3web.utilities.ISetMap;
import de.d3web.utilities.SetMap;
import de.d3web.we.alignment.AlignmentUtilRepository;
import de.d3web.we.alignment.D3webAlignUtils;
import de.d3web.we.alignment.GlobalAlignment;
import de.d3web.we.alignment.NumericalIdentity;
import de.d3web.we.alignment.aligner.GlobalAligner;
import de.d3web.we.basic.TerminologyType;
import de.d3web.we.terminology.global.GlobalTerminology;
import de.d3web.we.terminology.global.GlobalTerminologyHandler;
import de.d3web.we.terminology.local.LocalTerminologyAccess;

public class D3webTermFactory implements TermFactory<TerminologyObject, TerminologyObject> {
	
	public static final String DEFAULT_QUESTIONNAIRE_NAME ="Standardfragebogen";
	
	public Term getTerm(TerminologyObject object, TerminologyType type, GlobalTerminology gt) {
		Term term = gt.getTerm(object.getName(), null);
		if(term == null) {
			term = new Term(gt.getType());
			term.setInfo(TermInfoType.TERM_NAME, object.getName());
			gt.addTerm(term);
		} 
		return term;
	}

	public List<GlobalAlignment> getAlignableTerms(TerminologyObject no, String idString, GlobalTerminology gt) {
		List<GlobalAlignment> result = new ArrayList<GlobalAlignment>();
		GlobalTerminologyHandler handler = AlignmentUtilRepository.getInstance().getGlobalTerminogyHandler(gt);
		Collection<GlobalAligner> aligners = AlignmentUtilRepository.getInstance().getGlobalAligners(NamedObject.class);
		for (Term term : handler) {
			if(term.getInfo(TermInfoType.TERM_VALUE) != null) {
				continue;
			}
			for (GlobalAligner aligner : aligners) {
				result.addAll(aligner.align(term, no, idString));
			}
		}
		Collections.sort(result);
		return result;
	}
	
	public ISetMap<TerminologyObject, Term> addTerminology(TerminologyObject to, String idString, GlobalTerminology globalTerminology, Term oldParentTerm, Collection<NamedObject> alreadyDone) {
		//TODO: remove this, if getInfoStore is active
		NamedObject no = null;
		if (to instanceof NamedObject) {
			no = (NamedObject) to;
		} else {
			return null;
		}
		Boolean privat = (Boolean) no.getProperties().getProperty(Property.PRIVATE);
		if(privat != null && privat) return new SetMap<TerminologyObject, Term>();
		if(alreadyDone.contains(no)) {
			return new SetMap<TerminologyObject, Term>();
		}
		alreadyDone.add(no);
		if(!isIntegrable(no)) {
			return new SetMap<TerminologyObject, Term>();
		}
		
		List<GlobalAlignment> alignments = getAlignableTerms(no, idString, globalTerminology);
		Term parentTerm = null;
		if(alignments.isEmpty()) {
			if((!no.getName().equals("P000")) && (!no.getName().equals("Q000"))&& (!no.getName().equals(DEFAULT_QUESTIONNAIRE_NAME))) {
				if((no instanceof QContainer || no instanceof Solution)) {
					// new Root -> complete subtree
					parentTerm = getTerm(no, globalTerminology.getType(), globalTerminology);
					//parentTerm.addAlignments(getAlignments(no, idString, parentTerm));
					if(oldParentTerm == null) {
						globalTerminology.addRoot(parentTerm);
					} else {
						oldParentTerm.addChild(parentTerm);	
					}
					
				} else if(no instanceof Question) {
					getTerm(no, globalTerminology.getType(), globalTerminology);
					createAnswerTerms((Question)no, globalTerminology);
				}
			}
			for (TerminologyObject child : no.getChildren()) {
				addTerminology(child, idString, globalTerminology, parentTerm, alreadyDone);
			}
		} else {
			for (GlobalAlignment alignment : alignments) {
				//[TODO]: heterarchie??
				parentTerm = alignment.getTerm();
				if(no instanceof Question) {
					
					createAnswerTerms((Question)no, globalTerminology);
				}
				for (TerminologyObject child : no.getChildren()) {
					addTerminology(child, idString, globalTerminology, parentTerm, alreadyDone);
				}
			}
		}
		ISetMap<TerminologyObject, Term> result = new SetMap<TerminologyObject, Term>();
		return result;
	}

	private boolean isIntegrable(NamedObject no) {
		Boolean b = (Boolean) no.getProperties().getProperty(Property.FOREIGN);
		if(b == null) return true;
		return !b;
	}

	private void createAnswerTerms(Question question, GlobalTerminology globalTerminology) {
		Term valueTerm = null;
		valueTerm = new Term(globalTerminology.getType());
		valueTerm.setInfo(TermInfoType.TERM_NAME,  D3webAlignUtils.getText(question));
		valueTerm.setInfo(TermInfoType.TERM_VALUE, D3webAlignUtils.getText(question.getUnknownAlternative()));
		globalTerminology.addTerm(valueTerm);
		if(question instanceof QuestionChoice) {
			for (Choice each : ((QuestionChoice)question).getAllAlternatives()) {
				valueTerm = new Term(globalTerminology.getType());
				valueTerm.setInfo(TermInfoType.TERM_NAME,  D3webAlignUtils.getText(question));
				valueTerm.setInfo(TermInfoType.TERM_VALUE, D3webAlignUtils.getText(each));
				globalTerminology.addTerm(valueTerm);
			}
		} else if(question instanceof QuestionNum) {
			valueTerm = new Term(globalTerminology.getType());
			valueTerm.setInfo(TermInfoType.TERM_NAME,  D3webAlignUtils.getText(question));
			valueTerm.setInfo(TermInfoType.TERM_VALUE, new NumericalIdentity());
			globalTerminology.addTerm(valueTerm);
		}
	}

	public ISetMap<TerminologyObject, Term> addTerminology(LocalTerminologyAccess<TerminologyObject> localTerminology, String idString, GlobalTerminology globalTerminology) {
		TerminologyObject child = localTerminology.getHandler().iterator().next();
		return addTerminology(child, idString, globalTerminology, null, new HashSet<NamedObject>());
	}
	

}
