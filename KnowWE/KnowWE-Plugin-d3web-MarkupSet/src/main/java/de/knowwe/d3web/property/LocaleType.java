/*
 * Copyright (C) 2010 denkbares GmbH
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
package de.knowwe.d3web.property;

import java.util.Locale;

import de.d3web.we.kdom.AbstractType;
import de.d3web.we.kdom.Section;
import de.d3web.we.kdom.rendering.StyleRenderer;
import de.d3web.we.kdom.sectionFinder.RegexSectionFinder;

/**
 * Represents a java {@link Locale}. getLocale(...) returns a Locale
 * representing the text content of the Section.
 * 
 * @author Albrecht Striffler (denkbares GmbH)
 * @created 02.08.2011
 */
public class LocaleType extends AbstractType {

	public LocaleType() {
		this.setSectionFinder(new RegexSectionFinder("^\\s*\\.\\s*(\\w{2}(?:\\.\\w{2})?)\\s*", 1));
		this.setCustomRenderer(StyleRenderer.LOCALE);
	}

	public Locale getLocale(Section<LocaleType> s) {
		String text = s.getText();
		if (text.contains(".")) {
			String[] split = text.split("\\.");
			return new Locale(split[0], split[1]);
		}
		else {
			return new Locale(text);
		}
	}

}