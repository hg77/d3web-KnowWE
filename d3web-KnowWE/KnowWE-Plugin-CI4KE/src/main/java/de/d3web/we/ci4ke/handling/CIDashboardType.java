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
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import de.d3web.we.ci4ke.build.CIBuilder.CIBuildTriggers;
import de.d3web.we.core.KnowWEEnvironment;
import de.d3web.we.kdom.AbstractKnowWEObjectType;
import de.d3web.we.kdom.KnowWEArticle;
import de.d3web.we.kdom.Section;
import de.d3web.we.kdom.defaultMarkup.DefaultMarkup;
import de.d3web.we.kdom.defaultMarkup.DefaultMarkupRenderer;
import de.d3web.we.kdom.defaultMarkup.DefaultMarkupType;
import de.d3web.we.kdom.rendering.KnowWEDomRenderer;
import de.d3web.we.kdom.report.KDOMReportMessage;
import de.d3web.we.kdom.report.message.ObjectCreationError;
import de.d3web.we.kdom.subtreeHandler.SubtreeHandler;
import de.d3web.we.utils.KnowWEUtils;
import de.d3web.we.wikiConnector.KnowWEUserContext;

public class CIDashboardType extends DefaultMarkupType {

	public static final String WEB_KEY = "web";
	public static final String MONITORED_ARTICLE_KEY = "monitoredArticle";
	public static final String DASHBOARD_ARTICLE_KEY = "dashboardArticle";
	public static final String TESTS_KEY = "tests";
	public static final String TRIGGER_KEY = "trigger";
	public static final String OVERRIDDEN_ID = "id";

	private static final DefaultMarkup MARKUP;

	static {
		MARKUP = new DefaultMarkup("CIDashboard");
		MARKUP.addAnnotation(MONITORED_ARTICLE_KEY, true);
		MARKUP.addAnnotation(TESTS_KEY, true);
		MARKUP.addAnnotation(TRIGGER_KEY, true, CIBuildTriggers.values());
		MARKUP.addAnnotation(OVERRIDDEN_ID, false);
	}

	public CIDashboardType() {
		super(MARKUP);
		this.addSubtreeHandler(new DashboardSubtreeHandler());
		this.setCustomRenderer(new DashboardRenderer());
	}

	private class DashboardSubtreeHandler implements SubtreeHandler<CIDashboardType> {

		@Override
		public Collection<KDOMReportMessage> reviseSubtree(KnowWEArticle article, Section<CIDashboardType> s) {

			AbstractKnowWEObjectType.cleanMessages(article, s, this.getClass());

			String monitoredArticle =
					DefaultMarkupType.getAnnotation(s, MONITORED_ARTICLE_KEY);
			String tests =
					DefaultMarkupType.getAnnotation(s, TESTS_KEY);
			CIBuildTriggers trigger =
					CIBuildTriggers.valueOf(DefaultMarkupType.getAnnotation(s, TRIGGER_KEY));

			// Check if monitored Article exists
			/*
			 * This is commented out, because the existence-check of the
			 * monitored article resulted in a lot of error messages on KnowWE
			 * Startup... When the startup procedure gets improved, this check
			 * can probably be activated again!
			 */
			// KnowWEArticle art = KnowWEEnvironment.getInstance().
			// getArticle(s.getWeb(), monitoredArticle);
			// if(art==null) {
			// Message message = new Message(Message.ERROR,
			// "Monitored article does not exist!", "", -1, "");
			// DefaultMarkupType.storeSingleMessage(article, s, this.getClass(),
			// message);
			// return null;
			// }

			if (!dashboardIDisUnique(s)) {
				// dashboardArticle+monitoredArticle combination does not
				// uniquely identify a dashboard.
				// check, if the ID has been overridden:
				String overriddenID = DefaultMarkupType.getAnnotation(s, OVERRIDDEN_ID);

				if (overriddenID == null || overriddenID.equals("")) {
					// the dashboard can't be uniquely identified
					// and it's ID was not overridden. Post a warning message!
					String message = "This dashboard can't be " +
							"uniquely identified! Please add the 'id' annotation!";
					// DefaultMarkupType.storeSingleMessage(article, s,
					// this.getClass(), message);

					return Arrays.asList((KDOMReportMessage) new ObjectCreationError(message,
							this.getClass()));
				}
				else {
					// the dashboard can't be uniquely identified, but a
					// override-id
					// annotation was set. now check if this ID itself is
					// unique!
					if (!dashboardIDisUnique(s, overriddenID)) {
						// the overridden ID was not unique! Post error message
						String message = "This dashboard can't be " +
								"uniquely identified! Please check the id-annotation " +
								"(must be unique in the whole wiki)";
						// DefaultMarkupType.storeSingleMessage(article, s,
						// this.getClass(), message);
						return Arrays.asList((KDOMReportMessage) new ObjectCreationError(message,
								this.getClass()));
					}
				}
			}

			String id = getDashboardID(s);

			// Parse the trigger-parameter and (eventually) register
			// or deregister a CIHook
			if (trigger.equals(CIBuildTriggers.onDemand)) {
				// the tests of this dashboard should (currently!) be build
				// only on Demand. Lets check, if there is a hook registered
				// for this dashboard, then deregister it.
				if (CIHookManager.getInstance().containedInAHook(id)) {
					// TODO: Hook entfernen!
				}
			}
			else if (trigger.equals(CIBuildTriggers.onSave)) {
				Logger.getLogger(this.getClass().getName()).log(
						Level.INFO, ">> CI >> Setting Hook on " + monitoredArticle);

				// Hook registrieren, TODO: Wenn er nicht schon registriert ist!
				CIHookManager.getInstance().registerHook(monitoredArticle,
						s.getArticle().getTitle(), id);
			}

			// Alright, everything seems to be ok. Let's store the CIConfig in
			// the store

			CIConfig config = new CIConfig(id, monitoredArticle,
					s.getArticle().getTitle(), tests, trigger);

			KnowWEUtils.storeSectionInfo(s, CIConfig.CICONFIG_STORE_KEY, config);

			return new ArrayList<KDOMReportMessage>(0);
		}
	}

	private class DashboardRenderer extends KnowWEDomRenderer<CIDashboardType> {

		@Override
		public void render(KnowWEArticle article, Section<CIDashboardType> sec,
				KnowWEUserContext user, StringBuilder string) {

			if (DefaultMarkupType.getMessages(article, sec).size() > 0) {
				// Render Error-Messages!
				DefaultMarkupRenderer.renderMessages(article, sec, string);
				return;
			}

			CIDashboard board = new CIDashboard(sec);
			string.append(KnowWEUtils.maskHTML(board.render()));

			// string.append(KnowWEUtils.maskHTML(sec.verbalize()+"<br>"));
			// string.append(KnowWEUtils.maskHTML(dashboardIDisUnique(sec)+"<br>"));

		}
	}

	/**
	 * This method generates/gets the ID of a Dashboard. It DOES NOT CHECK
	 * UNIQUENESS!
	 * 
	 * @param article
	 * @param s
	 * @return
	 */
	public static String getDashboardID(Section<CIDashboardType> section) {

		String overriddenID = DefaultMarkupType.
				getAnnotation(section, OVERRIDDEN_ID);

		if (overriddenID == null || overriddenID.equals("")) {
			String dashboardArticle = section.getTitle();
			String monitoredArticle = DefaultMarkupType.
					getAnnotation(section, MONITORED_ARTICLE_KEY);
			return dashboardArticle + ".monitores." + monitoredArticle;
		}
		else return overriddenID;
	}

	/**
	 * Checks, if the dashboard defined in the given section can be uniquely
	 * identified. A dashboard is uniquely identifiable, if it is the only
	 * dashboard on this article which monitores a specific article. If more
	 * than one dashboard on a article monitores one specific article, this
	 * method returns false. In this case, the ID has to be overridden with the
	 * OVERRIDDEN_ID - Annotation.
	 * 
	 * @param section
	 * @return
	 */
	public static boolean dashboardIDisUnique(Section<CIDashboardType> section) {

		String thisMonitoredArticle = DefaultMarkupType.
				getAnnotation(section, MONITORED_ARTICLE_KEY);
		int countEqualSections = 0;

		List<Section<CIDashboardType>> list = new ArrayList<Section<CIDashboardType>>();
		section.getArticle().getSection().findSuccessorsOfType(CIDashboardType.class, list);

		for (Section<CIDashboardType> iterateSection : list) {
			String idAnnotation = DefaultMarkupType.
					getAnnotation(iterateSection, OVERRIDDEN_ID);
			String monitoredArticle = DefaultMarkupType.
					getAnnotation(iterateSection, MONITORED_ARTICLE_KEY);

			// count this section only if it is not overriding its ID itself!
			if (idAnnotation == null || idAnnotation.isEmpty()) {
				if (thisMonitoredArticle.equals(monitoredArticle)) countEqualSections++;
			}
		}

		return countEqualSections <= 1;
	}

	/**
	 * Checks, if the overridden ID of a dashboard is unique
	 * 
	 * @param section
	 * @param overriddenID
	 * @return
	 */
	public static boolean dashboardIDisUnique(Section<CIDashboardType> section,
			String overriddenID) {

		List<Section<CIDashboardType>> list = new ArrayList<Section<CIDashboardType>>();

		for (KnowWEArticle article : KnowWEEnvironment.getInstance().
				getArticleManager(section.getWeb()).getArticles()) {
			article.getSection().findSuccessorsOfType(CIDashboardType.class, list);
		}

		for (Section<CIDashboardType> s : list) {
			if (s.getId() != section.getId()) {
				String dashboardID = DefaultMarkupType.
						getAnnotation(section, OVERRIDDEN_ID);
				if (dashboardID != null && !dashboardID.isEmpty()) {
					if (dashboardID.equals(overriddenID)) return false;
				}
			}
		}
		return true;
	}
}
