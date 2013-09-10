/*
 * Copyright (C) 2010 Chair of Artificial Intelligence and Applied Informatics
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

package de.knowwe.core.kdom.parsing;

import de.knowwe.core.kdom.Type;
import de.knowwe.core.kdom.sectionFinder.SectionFinderResult;


public class DefaultSectionizerModule implements SectionizerModule {

	@Override
	public Section<?> createSection(String text, Type type, Section<?> father, SectionFinderResult result) {
		Parser parser = type.getParser();
		// small hack, should be removed soon...
		if (result.getParameterMap() != null && parser instanceof Sectionizer) {
			((Sectionizer) parser).addParameterMap(result.getParameterMap());
		}
		Section<?> s = parser.parse(text, father);
		return s;
	}

}
