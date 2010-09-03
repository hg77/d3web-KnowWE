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

package de.d3web.we.action;

import java.io.IOException;
import java.net.URLDecoder;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.ResourceBundle;

import common.Logger;

import de.d3web.core.knowledge.terminology.Choice;
import de.d3web.core.knowledge.terminology.Question;
import de.d3web.core.knowledge.terminology.QuestionChoice;
import de.d3web.core.knowledge.terminology.QuestionMC;
import de.d3web.core.manage.KnowledgeBaseManagement;
import de.d3web.core.session.Session;
import de.d3web.core.session.Value;
import de.d3web.core.session.blackboard.Blackboard;
import de.d3web.core.session.blackboard.DefaultFact;
import de.d3web.core.session.blackboard.Fact;
import de.d3web.core.session.values.ChoiceValue;
import de.d3web.core.session.values.DateValue;
import de.d3web.core.session.values.MultipleChoiceValue;
import de.d3web.core.session.values.NumValue;
import de.d3web.core.session.values.Unknown;
import de.d3web.indication.inference.PSMethodUserSelected;
import de.d3web.we.basic.IdentifiableInstance;
import de.d3web.we.basic.Information;
import de.d3web.we.basic.InformationType;
import de.d3web.we.basic.TerminologyType;
import de.d3web.we.core.DPSEnvironment;
import de.d3web.we.core.KnowWEAttributes;
import de.d3web.we.core.KnowWEFacade;
import de.d3web.we.core.KnowWEParameterMap;
import de.d3web.we.core.broker.Broker;
import de.d3web.we.core.knowledgeService.D3webKnowledgeServiceSession;
import de.d3web.we.core.knowledgeService.KnowledgeServiceSession;
import de.d3web.we.d3webModule.D3webModule;
import de.d3web.we.d3webModule.DPSEnvironmentManager;
import de.d3web.we.event.EventManager;
import de.d3web.we.event.FindingSetEvent;
import de.d3web.we.terminology.term.Term;
import de.d3web.we.utils.D3webUtils;

public class SetSingleFindingAction extends DeprecatedAbstractKnowWEAction {

	@SuppressWarnings( {
			"unchecked", "deprecation" })
	@Override
	public String perform(KnowWEParameterMap parameterMap) {
		String namespace = java.net.URLDecoder.decode(parameterMap
				.get(KnowWEAttributes.SEMANO_NAMESPACE));

		String objectid = parameterMap.get(KnowWEAttributes.SEMANO_OBJECT_ID);
		String termName = URLDecoder.decode(parameterMap
				.get(KnowWEAttributes.SEMANO_TERM_NAME));
		String valueid = parameterMap.get(KnowWEAttributes.SEMANO_VALUE_ID);
		String valuenum = parameterMap.get(KnowWEAttributes.SEMANO_VALUE_NUM);
		String valuedate = parameterMap.get(KnowWEAttributes.SEMANO_VALUE_DATE);
		String topic = parameterMap.getTopic();
		String user = parameterMap.get(KnowWEAttributes.USER);
		String web = parameterMap.get(KnowWEAttributes.WEB);

		if (namespace == null || objectid == null) {
			return "null";
		}

		// if DPS is inactive
		if (!ResourceBundle.getBundle("KnowWE_config").getString("dps.active").contains("true")) {

			KnowledgeBaseManagement kbm = D3webModule.getKnowledgeRepresentationHandler(web).getKBM(
					topic);
			Session session = D3webUtils.getSession(topic, user, web);
			Blackboard blackboard = session.getBlackboard();

			// Necessary for FindingSetEvent
			Question question = kbm.findQuestion(objectid);
			if (question != null) {

				// update: resetting the value is unnecessary --rh@100903
				// reset choices in case the selection changed
				// (user removed choices)
				// blackboard.addValueFact(new DefaultFact(question,
				// Unknown.getInstance(), PSMethodUserSelected.getInstance(),
				// PSMethodUserSelected.getInstance()));
				//

				Value value = null;
				if (valueid != null) {
					value = kbm.findValue(question, valueid);
				}
				else if (valuenum != null) {
					value = new NumValue(Double.parseDouble(valuenum));

					// TODO set valuedate in Attributes
				}
				else if (valuedate != null) {
					final DateFormat format = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
					try {
						Date date = format.parse(valuedate);
						value = new DateValue(date);
						System.out.println("date: " + value);
					}
					catch (ParseException e) {
						e.printStackTrace();
					}
				}

				if (value != null) {
					if (question instanceof QuestionMC && !value.equals(Unknown.getInstance())) {
						Fact mcFact = blackboard.getValueFact(question);
						if (mcFact != null && !mcFact.getValue().equals(Unknown.getInstance())) {
							MultipleChoiceValue mcv = ((MultipleChoiceValue) mcFact.getValue());
							Collection<ChoiceValue> thisMcv = (Collection<ChoiceValue>) ((MultipleChoiceValue) value).getValue();
							for (ChoiceValue cv : (Collection<ChoiceValue>) mcv.getValue()) {
								if (!thisMcv.contains(cv)) {
									thisMcv.add(cv);
								}
							}
						}
					}

					blackboard.addValueFact(new DefaultFact(question,
							value, PSMethodUserSelected.getInstance(),
									PSMethodUserSelected.getInstance()));

					EventManager.getInstance().fireEvent(
								new FindingSetEvent(question, value, namespace, web, user));
				}
			}
		}
		// with active DPS
		else {
			DPSEnvironment env = DPSEnvironmentManager.getInstance()
					.getEnvironments(web);
			Broker broker = env.getBroker(user);

			// Object value = null;
			// List<Object> values = new ArrayList<Object>();
			// if(valueid != null) {
			// value = valueid;
			// } else if(valuenum != null) {
			// value = Double.valueOf(valuenum);
			// }
			// if(value != null) {
			// values.add(value);
			// }
			// if(valueids != null) {
			// String[] ids = valueids.split("\\,");
			// for (String string : ids) {
			// values.add(string.trim());
			// }
			// }

			Term term = null;
			term = SemanticAnnotationAction.getTerm(env, termName);

			// workaround TODO refactor
			if (term == null) {
				KnowledgeServiceSession kss = broker.getSession()
						.getServiceSession(namespace);
				if (kss instanceof D3webKnowledgeServiceSession) {
					Question q = ((D3webKnowledgeServiceSession) kss).getBaseManagement().getKnowledgeBase().searchQuestion(
							objectid);
					if (q != null) {
						termName = q.getName();
						term = SemanticAnnotationAction.getTerm(env, termName);
					}
				}
			}

			IdentifiableInstance ii = null;
			if (term != null) {
				ii = SemanticAnnotationAction.getII(env, namespace, term);
			}
			if (ii == null) {
				return "Question not found in KB: " + termName;
			}

			List<String> answers = new ArrayList<String>();

			String qid = null;

			KnowledgeServiceSession kss = broker.getSession().getServiceSession(
					namespace);

			List<Information> userInfos = broker.getSession().getBlackboard()
					.getOriginalUserInformation();
			for (Information information : userInfos) {
				IdentifiableInstance iio = information
						.getIdentifiableObjectInstance();
				if (!iio.equals(ii)) continue;
				qid = iio.getObjectId();
				Collection<IdentifiableInstance> iivs = information
						.getIdentifiableValueInstances();
				if (iivs.isEmpty()) break;
				Iterator<IdentifiableInstance> iter = iivs.iterator();

				while (iter.hasNext()) {
					IdentifiableInstance iiv = iter.next();
					Object val = iiv.getValue();
					if (val instanceof String) {
						answers.add((String) val);
					}
				}
			}

			// HOTFIX for answer not set in mc-question.
			// Occurred just once, dont know why exactly.
			if (qid == null) qid = objectid;

			// Necessary for FindingSetEvent
			Question question = D3webUtils.getQuestion(kss, qid);
			Value value = null;

			// We need the Answer (Choice) Object for the FindingSetEvent
			if (question instanceof QuestionChoice) {
				for (Choice choice : ((QuestionChoice) question).getAllAlternatives()) {
					if (choice.getId().equals(valueid)) {
						value = new ChoiceValue(choice);
						break;
					}
				}
			}

			boolean contains = false;
			boolean mc = (question instanceof QuestionMC);
			for (String a : answers) {
				if (a.equals(valueid)) {
					contains = true;
				}
			}
			List<Object> valuesAfterClick = new ArrayList<Object>();
			if (mc) {
				if (!contains) {
					for (String a : answers) {
						valuesAfterClick.add(a);
					}
					valuesAfterClick.add(valueid.trim());
				}
				else {
					for (String a : answers) {
						if (!a.equals(valueid)) valuesAfterClick.add(a);
					}
				}

			}
			else {
				if (valuenum != null) {
					try {
						Double doubleValue = Double.valueOf(valuenum);
						valuesAfterClick.add(doubleValue);
						// Necessary for FindingSetEvent
						value = new NumValue(doubleValue);

					}
					catch (NumberFormatException e) {
					}

				}
				else {
					valuesAfterClick.add(valueid.trim());
				}
			}

			if (value == null && valueid.equals(Unknown.getInstance().getId())) {
				value = Unknown.getInstance();
			}

			EventManager.getInstance().fireEvent(
					new FindingSetEvent(question, value, namespace, web, user));

			Information info = new Information(namespace, objectid,
					valuesAfterClick, TerminologyType.symptom,
					InformationType.OriginalUserInformation);
			kss.inform(info);
			broker.update(info);
		}

		try {
			// TODO WATCH!
			// if anyone still needs to work with the old interview you need
			// to activate the commented out line below.
			// KnowWEFacade.getInstance().performAction("RefreshHTMLDialogAction",
			// parameterMap);

			if (namespace.contains("OneQuestionDialog")) {
				KnowWEFacade.getInstance().performAction("OneQuestionDialogAction", parameterMap);
			}
			else {
				KnowWEFacade.getInstance().performAction("QuickInterviewAction", parameterMap);
			}

		}
		catch (IOException e) {
			Logger.getLogger(this.getClass()).error(
					"Error while performing QuickInterviewAction" + e.getMessage());
		}

		return null;
	}

}
