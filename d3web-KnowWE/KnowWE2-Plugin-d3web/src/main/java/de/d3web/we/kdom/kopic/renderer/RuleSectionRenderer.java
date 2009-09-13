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

package de.d3web.we.kdom.kopic.renderer;

import java.util.ArrayList;
import java.util.List;

import de.d3web.KnOfficeParser.util.MessageKnOfficeGenerator;
import de.d3web.report.Message;
import de.d3web.we.core.KnowWEEnvironment;
import de.d3web.we.d3webModule.D3webModule;
import de.d3web.we.kdom.Section;
import de.d3web.we.kdom.TextLine;
import de.d3web.we.kdom.kopic.AbstractKopicSection;
import de.d3web.we.kdom.rendering.DelegateRenderer;
import de.d3web.we.kdom.rules.Rule;
import de.d3web.we.wikiConnector.KnowWEUserContext;

public class RuleSectionRenderer extends KopicSectionRenderer {
		
		@Override
		public void render(Section sec, KnowWEUserContext user, StringBuilder result) {
			List<Section> lines = new ArrayList<Section>(); 
			sec.findSuccessorsOfType(TextLine.class, lines);
			
			StringBuilder messagesBuilder = new StringBuilder();
			
			if (sec.getObjectType() instanceof AbstractKopicSection) {
				
				List<Message> sectionMessages = new ArrayList<Message>();
				List<Section> rules = new ArrayList<Section>();
				sec.findSuccessorsOfType(Rule.class, rules);
				
				int parsedCount = 0;
				List<String> errors = new ArrayList<String>();
				List<String> warnings = new ArrayList<String>();
				
				String rulesParsed = MessageKnOfficeGenerator.getResourceBundle().getString("rule");
				int rnum = 1;
				for (Section r:rules) {
					List<Message> rmsgs = ((Rule) r.getObjectType()).getMessages(r);
					boolean foundWarning = false;
					boolean foundError = false;
					for (Message msg:rmsgs) {
						if (msg.getMessageText().equals(rulesParsed)) {
							parsedCount += msg.getCount();
						} else if (msg.getMessageType() == Message.WARNING && foundWarning == false) {
							warnings.add(KnowWEEnvironment.maskHTML("<a onclick='highlightRule(\"" + r.getId() + "\",\"" 
									+ r.getTitle() + "\",\"0\",\"0\" );'/>" + rnum + "</a>"));
							foundWarning = true;
						} else if (msg.getMessageType() == Message.ERROR && foundError == false) {
							errors.add(KnowWEEnvironment.maskHTML("<a onclick='highlightRule(\"" + r.getId() + "\",\"" 
									+ r.getTitle() + "\",\"0\",\"0\" );'/>" + rnum + "</a>"));
							foundError = true;
						}
					}
					rnum++;
				}
				
				sectionMessages.add(0, MessageKnOfficeGenerator.createNoteMSGWithCount("rule", "", 0, "", parsedCount));
				if (sectionMessages != null && !sectionMessages.isEmpty()) {
					messagesBuilder.append("{{{");
					messagesBuilder.append(rulesParsed + " " + parsedCount + KnowWEEnvironment.maskHTML("<br>"));
					if (warnings.size() > 0) {
						messagesBuilder.append(D3webModule.getKwikiBundle_d3web()
							.getString("KnowWE.KopicRenderer.RulesSection.warnings")  + " "+ warnings.toString());
					}
					if (errors.size() > 0) {
						messagesBuilder.append(D3webModule.getKwikiBundle_d3web()
							.getString("KnowWE.KopicRenderer.RulesSection.errors")  + " "+ errors.toString());
					}
					messagesBuilder.append("}}}");
				}
				
				result.append("%%collapsebox-closed \n");
				
				result.append(generateTitle(sec, user, errors.size() > 0));
				
				result.append(messagesBuilder);
			}
			
			// a div containing
			// the class SetCoveringList and the nodes id
			result.append(KnowWEEnvironment.maskHTML("<div class=\"ReRenderSectionMarker\" id=\"" 
						  + sec.getId() + "\">"));
			
			// Rendering children
			StringBuilder b = new StringBuilder();
			DelegateRenderer.getInstance().render(sec,user, b);
			result.append(b.toString());
			
			// close the div
			result.append(KnowWEEnvironment.maskHTML("</div>"));
			
			result.append("/%\n");
			
			//return result.toString();
		}
	}

