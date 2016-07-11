/*
 * Copyright (C) 2014 denkbares GmbH, Germany
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

package de.knowwe.event;

import de.knowwe.core.ArticleManager;
import com.denkbares.events.Event;

/**
 * @author Albrecht Striffler (denkbares GmbH)
 * @created 16.06.2014
 */
public class ArticleManagerEvent implements Event {

	protected final ArticleManager articleManager;

	public ArticleManagerEvent(ArticleManager articleManager) {
		this.articleManager = articleManager;
	}

	public ArticleManager getArticleManager() {
		return this.articleManager;
	}
}
