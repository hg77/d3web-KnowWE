/*
 * Copyright (C) 2010 University Wuerzburg, Computer Science VI
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
package de.knowwe.diaflux.type;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.d3web.core.inference.condition.Condition;
import de.d3web.diaFlux.inference.NodeActiveCondition;
import de.d3web.we.kdom.condition.D3webCondition;
import de.knowwe.core.kdom.KnowWEArticle;
import de.knowwe.core.kdom.parsing.Section;
import de.knowwe.core.kdom.sectionFinder.RegexSectionFinder;

/**
 * 
 * @author Reinhard Hatko
 * @created 15.11.2010
 */
public class NodeActiveConditionType extends D3webCondition<NodeActiveConditionType> {

	public static final int FLOWCHART_GROUP = 1;
	public static final int EXITNODE_GROUP = 2;
	public static final String REGEX = "IS_ACTIVE\\[([^\\]]*)\\(([^)]*)\\)\\]";
	public static final Pattern PATTERN = Pattern.compile(REGEX, Pattern.CASE_INSENSITIVE);

	public NodeActiveConditionType() {
		setSectionFinder(new RegexSectionFinder(PATTERN));

		FlowchartReference flowchartReference = new FlowchartReference();
		flowchartReference.setSectionFinder(new RegexSectionFinder(PATTERN, FLOWCHART_GROUP));
		addChildType(flowchartReference);

		ExitNodeReference exitNodeReference = new ExitNodeReference();
		exitNodeReference.setSectionFinder(new RegexSectionFinder(Pattern.compile("\\(([^)]*)\\)"),
				1));
		addChildType(exitNodeReference);
	}

	@Override
	protected Condition createCondition(KnowWEArticle article, Section<NodeActiveConditionType> section) {

		Matcher matcher = PATTERN.matcher(section.getText());

		if (!matcher.matches()) {
			return null;
		}
		else {
			String flowName = matcher.group(1);
			String nodeName = matcher.group(2);

			return new NodeActiveCondition(flowName, nodeName);
		}

	}

}