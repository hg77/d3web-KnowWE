/*
 * Copyright (C) 2012 denkbares GmbH
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
package de.knowwe.wikicontentdownload;

import de.knowwe.core.Attributes;
import de.knowwe.core.kdom.parsing.Section;
import de.knowwe.core.user.UserContext;
import de.knowwe.tools.DefaultTool;
import de.knowwe.tools.Tool;
import de.knowwe.tools.ToolProvider;

/**
 * 
 * @author Johanna Latt
 * @created 16.04.2012
 */
public class WikiZIPDownloadProvider implements ToolProvider {

	public static final String PARAM_FILENAME = "filename";

	@Override
	public Tool[] getTools(Section<?> section, UserContext userContext) {
		// and provide both downloads as tools
		if (!userContext.userIsAdmin()) {
			return new Tool[0];
		}
		return new Tool[] {
				getDownloadTool(section, userContext, false),
				getDownloadTool(section, userContext, true)
		};
	}

	protected Tool getDownloadTool(Section<?> section, UserContext userContext, boolean fingerprint) {
		// tool to provide download capability
		String jsAction = "window.location='action/DownloadWikiZIP" +
				"?" + Attributes.TOPIC + "=" + section.getTitle() +
				"&amp;" + Attributes.WEB + "=" + section.getWeb() +
				"&amp;" + DownloadWikiZIP.PARAM_FINGERPRINT + "=" + fingerprint +
				"&amp;" + DownloadWikiZIP.PARAM_VERSIONS + "=" + !fingerprint +
				"'";
		return new DefaultTool(
				"KnowWEExtension/images/zip.jpg",
				fingerprint
						? "Download Finger-Print"
						: "Download Wiki Zip",
				fingerprint
						? "Download the entire Wiki as a Zip-File, including a finger-print for debug purposes, but no version history."
						: "Download the entire Wiki as a Zip-File.",
				jsAction);
	}

}
