/*
 * Copyright (C) 2013 University Wuerzburg, Computer Science VI
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
package de.d3web.we.ci4ke.dashboard.rendering;

import com.denkbares.strings.Identifier;
import de.knowwe.core.kdom.rendering.RenderResult;
import de.knowwe.core.utils.KnowWEUtils;

/**
 * Renders a link to the definition of a Term or to the ObjectInfoPage of the
 * Term, if there are multiple definitions.
 * 
 * @author Reinhard Hatko
 * @created 27.03.2013
 */
public class TermIdentifierRenderer implements ObjectNameRenderer {

	@Override
	public void render(String web, String objectName, RenderResult result) {
		renderTermIdentifier(web, objectName, result);
	}

	public static void renderTermIdentifier(String web, String objectName, RenderResult result) {

		String url = KnowWEUtils.getURLLinkToObjectInfoPage(Identifier.fromExternalForm(objectName));
		result.appendHtml("<a href='" + url + "'>");
		result.append(objectName);
		result.appendHtml("</a>");
	}

}
