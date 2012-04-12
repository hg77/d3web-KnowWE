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

package utils;

import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.knowwe.core.Attributes;
import de.knowwe.core.Environment;
import de.knowwe.core.action.ActionContext;
import de.knowwe.core.action.UserActionContext;
import de.knowwe.core.kdom.Article;

public class TestUtils {

	public static String readBytes(Reader r) {
		int zeichen = 0;
		LinkedList<Integer> ints = new LinkedList<Integer>();
		while (true) {

			try {
				zeichen = r.read();
			}
			catch (IOException e) {
				// TODO Auto-generated catch block
				break;
			}
			catch (OutOfMemoryError e1) {
				break;
			}

			// Ende des Stream erreicht
			if (zeichen == -1) {
				break;
			}

			ints.add(zeichen);
		}

		StringBuilder buffi = new StringBuilder(5000000);
		for (Integer i : ints) {

			if ((i.intValue() == 128) || (i.intValue() == 228)
					|| (i.intValue() == 252) || (i.intValue() == 246)
					|| (i.intValue() == 214) || (i.intValue() == 196)
					|| (i.intValue() == 220) || (i.intValue() == 223)) {
				if (i.intValue() == 128) {
					buffi.append('');
				}
				if (i.intValue() == 228) {
					buffi.append('ä');
				}
				if (i.intValue() == 252) {
					buffi.append('ü');
				}
				if (i.intValue() == 246) {
					buffi.append('ö');
				}
				if (i.intValue() == 214) {
					buffi.append('ü');
				}
				if (i.intValue() == 196) {
					buffi.append('Ö');
				}
				if (i.intValue() == 220) {
					buffi.append('Ü');
				}
				if (i.intValue() == 223) {
					buffi.append('ß');
				}
			}
			else {
				buffi.append(((char) i.intValue()));
			}
		}
		return buffi.toString();
	}

	public static String ReaderToString(Reader r) {
		return readBytes(r).replace('@', '%');
	}

	/**
	 * Creates an @link{UserActionContext} for test purposes. Both parameters
	 * are optional (in case you don't need the Action...)
	 * 
	 * @created Apr 28, 2011
	 * @param actionName <strong>optional:</strong> The name of the desired
	 *        Action
	 * @param path <strong>optional:</strong> special path (very unlikely that
	 *        you need this)
	 * @return
	 */
	public static UserActionContext createTestActionContext(String actionName, String path) {
		Map<String, String> map = new HashMap<String, String>();
		map.put(Attributes.WEB, Environment.DEFAULT_WEB);
		map.put(Attributes.USER, "Test User");
		return new ActionContext(actionName != null ? actionName : "", path != null ? path : "",
				map, null, null, null, null);
	}

	public static int countMatches(String text, String regex) {
		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(text);
		int count = 0;
		while (matcher.find()) {
			count++;
		}
		return count;
	}

	/**
	 * Called by the Core-Junit-Tests
	 */
	public static void processAndUpdateArticleJunit(String username, String content,
			String topic, String web) {
		Environment.getInstance().getArticleManager(web).registerArticle(
				Article.createArticle(content, topic, web));
	}
}
