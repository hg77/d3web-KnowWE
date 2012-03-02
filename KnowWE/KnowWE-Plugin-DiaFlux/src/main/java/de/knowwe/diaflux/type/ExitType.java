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

package de.knowwe.diaflux.type;

import de.knowwe.core.compile.terminology.TermRegistrationScope;
import de.knowwe.core.kdom.objects.SimpleDefinition;
import de.knowwe.core.kdom.objects.SimpleTerm;
import de.knowwe.core.kdom.parsing.Section;
import de.knowwe.core.kdom.parsing.Sections;
import de.knowwe.core.kdom.sectionFinder.AllTextSectionFinder;
import de.knowwe.kdom.xml.AbstractXMLType;

/**
 * 
 * 
 * @author Reinhard Hatko
 * @created on: 09.10.2009
 */
public class ExitType extends AbstractXMLType {

	private static ExitType instance;

	private ExitType() {
		super("exit");
		addChildType(new ExitNodeDef());
	}

	public static ExitType getInstance() {
		if (instance == null) instance = new ExitType();

		return instance;
	}

	static class ExitNodeDef extends SimpleDefinition {

		public ExitNodeDef() {
			super(TermRegistrationScope.LOCAL, String.class);
			setSectionFinder(new AllTextSectionFinder());
		}

		@Override
		public String getTermIdentifier(Section<? extends SimpleTerm> s) {
			String nodeName = s.getText();
			Section<FlowchartType> flowchart = Sections.findAncestorOfType(s, FlowchartType.class);
			String flowchartName = FlowchartType.getFlowchartName(flowchart);

			return flowchartName + "(" + nodeName + ")";
		}

	}

}