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
package de.knowwe.jspwiki;

import de.knowwe.core.kdom.parsing.Section;
import de.knowwe.core.kdom.rendering.RenderResult;
import de.knowwe.core.user.UserContext;
import de.knowwe.kdom.defaultMarkup.DefaultMarkup;
import de.knowwe.kdom.defaultMarkup.DefaultMarkupRenderer;
import de.knowwe.kdom.defaultMarkup.DefaultMarkupType;

/**
 * Markup to easily activate (and deactivate) a read-only mode to the wiki. This
 * is only an administrative tool, not a security tool. If you know what you are
 * doing, you can easily circumvent the read-only mode.
 * 
 * @author Albrecht Striffler (denkbares GmbH)
 * @created 01.08.2013
 */
public class ReadOnlyMarkup extends DefaultMarkupType {

	private static final DefaultMarkup MARKUP;

	static {
		MARKUP = new DefaultMarkup("ReadOnly");
	}

	public ReadOnlyMarkup() {
		super(MARKUP);
		this.setRenderer(new ReadOnlyMarkupRenderer());
	}

	private static class ReadOnlyMarkupRenderer extends DefaultMarkupRenderer {

		@Override
		protected void renderContents(Section<?> section, UserContext user, RenderResult string) {
			if (!user.userIsAdmin()) {
				string.append("This feature is only visible and usable for administrators.");
				return;
			}
			string.append("__ReadOnly Mode__");
			String checked = ReadOnlyManager.isReadOnly() ? " checked" : "";
			string.appendHtml("<div class='onoffswitch'>"
					+ "<input type='checkbox' name='onoffswitch' class='onoffswitch-checkbox' id='myonoffswitch'"
					+ checked
					+ " onchange='javascript:KNOWWE.plugin.jspwikiConnector.setReadOnly(this)'>"
					+ "<label class='onoffswitch-label' for='myonoffswitch'>"
					+ "<div class='onoffswitch-inner'></div>"
					+ "<div class='onoffswitch-switch'></div>"
					+ "</label>"
					+ "</div>");
			string.append("Disclaimer: This is a purely administrative feature and should not be used for security purposes, because it is not secure.");
		}
	}

}
