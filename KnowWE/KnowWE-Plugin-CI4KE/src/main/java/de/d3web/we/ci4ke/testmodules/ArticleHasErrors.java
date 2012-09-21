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
package de.d3web.we.ci4ke.testmodules;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;

import de.d3web.testing.AbstractTest;
import de.d3web.testing.Message;
import de.d3web.testing.Utils;
import de.knowwe.core.kdom.Article;
import de.knowwe.core.report.Message.Type;
import de.knowwe.core.report.Messages;

/**
 * This tests checks, if
 * 
 * @author Marc-Oliver Ochlast
 * @created 29.05.2010
 */
public class ArticleHasErrors extends AbstractTest<Article> {

	@Override
	public Message execute(Article moni, String[] args2, String[]... ignores) throws InterruptedException {

		boolean hasError = false;
		StringBuffer buffy = new StringBuffer();

		String monitoredArticleTitle = moni.getTitle();

		Collection<de.knowwe.core.report.Message> messages = new LinkedList<de.knowwe.core.report.Message>();
		Type[] typeArray = new de.knowwe.core.report.Message.Type[] { de.knowwe.core.report.Message.Type.ERROR };
		Map<String, Collection<de.knowwe.core.report.Message>> allMessagesMap =
				Messages.getMessagesFromSubtree(moni.getRootSection(), typeArray);

		for (String s : allMessagesMap.keySet()) {
			messages.addAll(allMessagesMap.get(s));
		}

		Utils.checkInterrupt();

		buffy.append("<a href=\"Wiki.jsp?page=" + monitoredArticleTitle + "\"> "
				+ monitoredArticleTitle + "</a>:\n");
		buffy.append("<ul>");
		for (de.knowwe.core.report.Message message : messages) {
			// This finds only messages, that are explicitly stored
			// as Message.ERROR, because the Type Message.UNKNOWN_ERROR
			// is not public!
			hasError = true;
			// buffy.append("Error on monitored article: ");
			buffy.append("<li> " + message.getVerbalization() + "</li>");
		}
		buffy.append("</ul>");
		// Utils.slowDowntest(this.getClass(), 10000, true);
		if (hasError) {
			return new Message(
					Message.Type.FAILURE, buffy.toString());
		}
		else {
			return new Message(
					Message.Type.SUCCESS, null);
		}
	}

	@Override
	public Class<Article> getTestObjectClass() {
		return Article.class;
	}

	@Override
	public String getDescription() {
		return "Checks, that the specified article reports no compile errors for knowledge definitions.";
	}
}