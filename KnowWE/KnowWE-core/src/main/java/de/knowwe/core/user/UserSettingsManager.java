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

package de.knowwe.core.user;

import java.util.HashMap;
import java.util.Map;

import de.knowwe.core.kdom.rendering.RenderingMode;

public class UserSettingsManager {

	private static UserSettingsManager instance = null;

	public static UserSettingsManager getInstance() {
		if (instance == null) {
			instance = new UserSettingsManager();

		}

		return instance;
	}

	public RenderingMode getRenderingType(String user, String topic) {
		// TODO elaborate VIEW is just default behaviour
		return RenderingMode.VIEW;
	}

	private final Map<String, UserSetting> settings = new HashMap<String, UserSetting>();

	public boolean hasQuickEditFlagSet(String nodeID, String user, String topic) {
		if (settings.get(user) == null) return false;

		return settings.get(user).hasQuickEditFlagSet(nodeID, topic);
	}

	public boolean quickEditIsInPre(String nodeID, String user, String topic) {
		if (settings.get(user) == null) return false;

		return settings.get(user).quickEditIsInPre(nodeID, topic);
	}

	public void setQuickEditFlag(String nodeID, String user, String topic, String inPre) {
		if (settings.get(user) == null) settings.put(user, new UserSetting());

		settings.get(user).setQuickEditFlag(nodeID, topic, inPre);

	}

}