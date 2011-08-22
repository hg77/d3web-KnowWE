/*
 * Copyright (C) 2011 denkbares GmbH, Germany
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
package utils;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import de.d3web.we.core.KnowWEAttributes;
import de.d3web.we.kdom.KnowWEArticle;
import de.d3web.we.user.UserContext;

/**
 * 
 * @author volker_belli
 * @created 04.08.2011
 */
public class TestUserContext implements UserContext {

	private static int counter = 0;

	private final boolean isAdmin;
	private final boolean isAsserted;
	private final String userName = "Testuser #" + (counter++);
	private final KnowWEArticle article;
	private final Map<String, String> parameterMap = new HashMap<String, String>();

	public TestUserContext(KnowWEArticle article) {
		this(article, false, false);
	}

	public TestUserContext(KnowWEArticle article, boolean isAdmin, boolean isAsserted) {
		this.article = article;
		this.isAdmin = isAdmin;
		this.isAsserted = isAsserted;
		addParameter(KnowWEAttributes.WEB, getWeb());
		addParameter(KnowWEAttributes.TOPIC, getTopic());
		addParameter(KnowWEAttributes.USER, getUserName());
	}

	@Override
	public boolean userIsAdmin() {
		return isAdmin;
	}

	@Override
	public boolean userIsAsserted() {
		return isAsserted;
	}

	@Override
	public String getUserName() {
		return userName;
	}

	@Override
	public String getTopic() {
		return article.getTitle();
	}

	@Override
	public String getWeb() {
		return article.getWeb();
	}

	@Override
	public Map<String, String> getParameters() {
		return Collections.unmodifiableMap(this.parameterMap);
	}

	@Override
	public String getParameter(String key) {
		return this.getParameters().get(key);
	}

	@Override
	public String getParameter(String key, String defaultValue) {
		return this.getParameters().get(key) != null
				? this.getParameters().get(key)
				: defaultValue;
	}

	public void addParameter(String key, String value) {
		this.parameterMap.put(key, value);
	}

	@Override
	public HttpServletRequest getRequest() {
		throw new UnsupportedOperationException("not implemented yet");
	}

	@Override
	public HttpSession getSession() {
		throw new UnsupportedOperationException("not implemented yet");
	}

	@Override
	public ServletContext getServletContext() {
		throw new UnsupportedOperationException("not implemented yet");
	}

}