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

package de.d3web.we.ci4ke.handling;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.d3web.testing.ArgsCheckResult;
import de.d3web.testing.ExecutableTest;
import de.d3web.testing.Test;
import de.d3web.testing.TestManager;
import de.knowwe.core.Environment;
import de.knowwe.core.kdom.Article;
import de.knowwe.core.kdom.parsing.Section;
import de.knowwe.core.kdom.parsing.Sections;
import de.knowwe.core.kdom.subtreeHandler.SubtreeHandler;
import de.knowwe.core.report.Message;
import de.knowwe.core.report.Messages;
import de.knowwe.core.utils.KnowWEUtils;
import de.knowwe.kdom.defaultMarkup.AnnotationContentType;
import de.knowwe.kdom.defaultMarkup.DefaultMarkup;
import de.knowwe.kdom.defaultMarkup.DefaultMarkupType;

public class CIDashboardType extends DefaultMarkupType {

	public static final String NAME_KEY = "name";
	public static final String TEST_KEY = "test";
	public static final String TRIGGER_KEY = "trigger";

	public static enum CIBuildTriggers {
		onDemand, onSave
	}

	private static final DefaultMarkup MARKUP;

	static {
		MARKUP = new DefaultMarkup("CIDashboard");
		MARKUP.addAnnotation(NAME_KEY, true);
		MARKUP.addAnnotation(TEST_KEY, true);
		MARKUP.addAnnotation(TRIGGER_KEY, true);
	}

	public CIDashboardType() {
		super(MARKUP);
		this.addSubtreeHandler(new DashboardSubtreeHandler());
		this.setIgnorePackageCompile(true);
		// this.setCustomRenderer(new DashboardRenderer());
		this.setRenderer(new CIDashboardRenderer());
	}

	public static String getDashboardName(Section<CIDashboardType> section) {
		return DefaultMarkupType.getAnnotation(section, NAME_KEY);
	}

	private class DashboardSubtreeHandler extends SubtreeHandler<CIDashboardType> {

		@Override
		public Collection<Message> create(Article article, Section<CIDashboardType> s) {

			List<Message> msgs = new ArrayList<Message>();

			String dashboardName = DefaultMarkupType.getAnnotation(s, NAME_KEY);

			if (dashboardName == null) return msgs;

			String triggerString = DefaultMarkupType.getAnnotation(s, TRIGGER_KEY);

			if (triggerString == null) return msgs;

			CIBuildTriggers trigger = null;

			Pattern pattern = Pattern.compile("(?:\\w+|\".+?\")");

			Set<String> monitoredArticles = new HashSet<String>();
			Matcher matcher = pattern.matcher(triggerString);
			if (matcher.find()) {
				// get the name of the test
				try {
					trigger = CIBuildTriggers.valueOf(matcher.group());
					// get the monitoredArticles if onSave
					if (trigger.equals(CIDashboardType.CIBuildTriggers.onSave)) {
						while (matcher.find()) {
							String parameter = matcher.group();
							if (parameter.startsWith("\"") && parameter.endsWith("\"")) {
								parameter = parameter.substring(1, parameter.length() - 1);
							}
							monitoredArticles.add(parameter);
						}
					}
				}
				catch (IllegalArgumentException e) {
					msgs.add(Messages.error("Invalid trigger specified: " + triggerString));
					return msgs;
				}
			}

			if (trigger.equals(CIBuildTriggers.onSave) && monitoredArticles.isEmpty()) {
				msgs.add(Messages.error("Invalid trigger: " + CIBuildTriggers.onSave
						+ " requires attached articles to monitor."));
				return msgs;
			}

			// This map is used for storing tests and their parameter-list
			// Map<String, List<String>> tests = new HashMap<String,
			// List<String>>();
			List<ExecutableTest> tests = new ArrayList<ExecutableTest>();

			List<Section<? extends AnnotationContentType>> annotationSections =
					DefaultMarkupType.getAnnotationContentSections(s, TEST_KEY);

			// iterate over all @test-Annotations
			for (Section<?> annoSection : annotationSections) {
				String annotationText = annoSection.getText();
				matcher = pattern.matcher(annotationText);
				if (matcher.find()) {
					// get the name of the test
					String testName = matcher.group();
					// get the parameters of the test
					List<String> testParamters = new ArrayList<String>();
					while (matcher.find()) {
						String parameter = matcher.group();
						if (parameter.startsWith("\"") && parameter.endsWith("\"")) {
							parameter = parameter.substring(1, parameter.length() - 1);
						}
						testParamters.add(parameter);
					}
					Test<?> test = TestManager.findTest(testName);
					if (test != null) {
						String[] args = testParamters.toArray(new String[] {});
						tests.add(new ExecutableTest(test, args));

						// check arguments and create error messages if
						// necessary
						testArguments(msgs, testName, test, args);
					}
					else {
						msgs.add(new Message(Message.Type.ERROR, "Class not found for test name: "
								+ testName));
					}
				}
			}

			CIConfig config = new CIConfig(article.getWeb(), s.getArticle().getTitle(),
					dashboardName, tests, trigger);

			// Parse the trigger-parameter and (eventually) register
			// a CIHook
			if (trigger.equals(CIBuildTriggers.onSave)) {
				// Hook registrieren
				CIHook ciHook = new CIHook(article.getWeb(), article.getTitle(), dashboardName,
						monitoredArticles);
				CIHookManager.getInstance().registerHook(ciHook);
				// Store to be able to unregister in destroy method
				KnowWEUtils.storeObject(article, s,
						CIHook.CIHOOK_STORE_KEY, ciHook);
			}

			// Alright, everything seems to be ok. Let's store the CIConfig in
			// the store

			KnowWEUtils.storeObject(article, s, CIConfig.CICONFIG_STORE_KEY, config);

			return msgs;
		}

		private void testArguments(List<Message> msgs, String testName, Test<?> test, String[] args) {
			args = Arrays.copyOfRange(args, 1, args.length);
			ArgsCheckResult argsCheckResult = test.checkArgs(args);
			if (argsCheckResult.hasError() || argsCheckResult.hasWarning()) {
				String[] arguments = argsCheckResult.getArguments();
				for (int i = 0; i < arguments.length; i++) {
					if (argsCheckResult.hasError(i)) {
						msgs.add(new Message(Message.Type.ERROR, testName + ": "
								+ renderMessage(
										args, argsCheckResult,
										i)));
					}
					if (argsCheckResult.hasWarning(i)) {
						msgs.add(new Message(Message.Type.WARNING, testName + ": "
								+ renderMessage(
										args, argsCheckResult,
										i)));
					}
				}

				// error on zero arguments
				if (arguments.length == 0 && argsCheckResult.hasError(0)) {
					msgs.add(new Message(Message.Type.ERROR, testName + ": "
							+ renderMessage(
									args, argsCheckResult, 0)));
				}
			}
		}

		private String renderMessage(final String[] args, ArgsCheckResult argsCheckResult, int i) {
			if (argsCheckResult.getMessage(i) != null) {
				String arg = "none";
				if (i < args.length) {
					arg = args[i];
				}
				String message = argsCheckResult.getMessage(i);
				return "Invalid argument: " + arg + " (" + message + ")";
			}
			return null;
		}

		@Override
		public void destroy(Article article, Section<CIDashboardType> s) {
			CIHook ciHook = (CIHook) s.getSectionStore().getObject(article,
					CIHook.CIHOOK_STORE_KEY);
			if (ciHook != null) {
				CIHookManager.getInstance().unregisterHook(ciHook);
			}
		}
	}

	/**
	 * Checks if the name of the given CIDashboard-Section is not taken by any
	 * other CIDashboard-Section in the wiki.
	 * 
	 * @created 12.11.2010
	 * @param section the name of this CIDashboard-section is checked for
	 *        uniqueness
	 * @return true if the name of the section is unique in the wiki
	 */
	public static boolean dashboardNameIsUnique(Section<CIDashboardType> section) {

		String thisDashboardName = CIDashboardType.getAnnotation(section, NAME_KEY);

		List<Section<CIDashboardType>> sectionList = new ArrayList<Section<CIDashboardType>>();
		for (Article article : Environment.getInstance().
				getArticleManager(section.getWeb()).getArticles()) {
			Sections.findSuccessorsOfType(article.getRootSection(), CIDashboardType.class,
					sectionList);
		}

		for (Section<CIDashboardType> s : sectionList) {
			if (s.getID() != section.getID()) {
				String otherDashboardName = DefaultMarkupType.getAnnotation(section, NAME_KEY);
				if (otherDashboardName.equals(thisDashboardName)) {
					return false;
				}
			}
		}
		return true;
	}

}
