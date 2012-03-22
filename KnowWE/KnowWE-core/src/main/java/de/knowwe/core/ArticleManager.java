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

package de.knowwe.core;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import de.knowwe.core.event.Event;
import de.knowwe.core.event.EventListener;
import de.knowwe.core.event.EventManager;
import de.knowwe.core.kdom.Article;
import de.knowwe.core.utils.KnowWEUtils;
import de.knowwe.event.ArticleRegisteredEvent;
import de.knowwe.event.ArticleUpdatesFinishedEvent;
import de.knowwe.event.InitializedAllArticlesEvent;
import de.knowwe.event.UpdatingDependenciesEvent;
import dummies.TestWikiConnector;

/**
 * Manages all the articles of one web in a HashMap
 * 
 * @author Jochen
 */
public class ArticleManager implements EventListener {

	/**
	 * Stores Articles for article-names
	 */
	private HashMap<String, Article> articleMap = new HashMap<String, Article>();

	private final TreeSet<String> currentRefreshQueue = new TreeSet<String>();

	/**
	 * List that keeps track of all articles that are updating their
	 * dependencies at the moment.
	 */
	private final Set<String> updatingArticles = new HashSet<String>();

	/**
	 * List that keeps track of all articles, that are already queued for
	 * updating and don't need to be queued again.
	 */
	private final HashSet<String> globalRefreshQueue = new HashSet<String>();

	private boolean initializedArticles = false;

	private final String web;

	public String jarsPath;
	public String reportPath;

	public String getReportPath() {
		return reportPath;
	}

	private static ResourceBundle rb = ResourceBundle
			.getBundle("KnowWE_config");

	public ArticleManager(Environment env, String webname) {
		this.web = webname;
		if (!(env.getWikiConnector() instanceof TestWikiConnector)) {
			jarsPath = KnowWEUtils.getRealPath(env.getWikiConnector()
					.getServletContext(), rb.getString("path_to_jars"));
			reportPath = KnowWEUtils.getRealPath(env.getWikiConnector()
					.getServletContext(), rb.getString("path_to_reports"));

		}
		else {
			jarsPath = System.getProperty("java.io.tmpdir")
					+ File.separatorChar + "jars";
			reportPath = System.getProperty("java.io.tmpdir")
					+ File.separatorChar + "reports";

		}
		EventManager.getInstance().registerListener(this);
	}

	public String getWebname() {
		return web;
	}

	/**
	 * Servs the Article for a given article name
	 * 
	 * @param title
	 * @return
	 */
	public Article getArticle(String title) {
		return articleMap.get(title);
	}

	public Iterator<Article> getArticleIterator() {
		return articleMap.values().iterator();
	}

	public Collection<Article> getArticles() {
		return Collections.unmodifiableCollection(articleMap.values());
	}

	public Set<String> getTitles() {
		return Collections.unmodifiableSet(articleMap.keySet());
	}

	/**
	 * Registers an changed article in the manager and also updates depending
	 * articles.
	 * 
	 * @created 14.12.2010
	 * @param article is the changed article to register
	 */
	public void registerArticle(Article article) {

		// store new article
		String title = article.getTitle();
		articleMap.put(title, article);

		long startTime = System.currentTimeMillis();

		Logger.getLogger(this.getClass().getName()).log(
				Level.FINE,
				"-> Starting to update dependencies to article '" + title
						+ "' ->");
		updatingArticles.add(title);

		EventManager.getInstance().fireEvent(new UpdatingDependenciesEvent(article));

		if (areArticlesInitialized()) updateQueuedArticles();

		updatingArticles.remove(title);
		Logger.getLogger(this.getClass().getName()).log(
				Level.FINE,
				"<- Finished updating dependencies to article '" + title
						+ "' in " + (System.currentTimeMillis() - startTime)
						+ "ms <-");

		Logger.getLogger(this.getClass().getName()).log(
				Level.INFO,
				"<<==== Finished building article '" + title + "' in "
						+ web + " in "
						+ (System.currentTimeMillis() - article.getStartTime())
						+ "ms <<====");
		EventManager.getInstance().fireEvent(new ArticleRegisteredEvent(article));
	}

	public void updateQueuedArticles() {

		List<String> localQueue = new ArrayList<String>();
		while (!currentRefreshQueue.isEmpty()) {
			String title = currentRefreshQueue.pollFirst();
			if (!globalRefreshQueue.contains(title)) {
				// Since this method is called recursively, we need a global
				// queue to keep track of which articles are already queued.
				// Don't queue (or update) articles in this call, if they are
				// already queued further up in the call stack.
				localQueue.add(title);
				globalRefreshQueue.add(title);
			}
		}

		for (String title : localQueue) {
			if (!updatingArticles.contains(title)) {
				if (Environment.getInstance().getWikiConnector().doesPageExist(title)) {
					Article newArt = Article.createArticle(
							articleMap.get(title).getSection().getText(), title,
							Environment.getInstance().getRootType(), web, false);
					registerArticle(newArt);
				}
				else {
					deleteArticle(getArticle(title));
				}

			}
			globalRefreshQueue.remove(title);
		}

		if (globalRefreshQueue.isEmpty()) {
			EventManager.getInstance().fireEvent(new ArticleUpdatesFinishedEvent());
		}
	}

	public void clearArticleMap() {
		this.articleMap = new java.util.HashMap<String, Article>();
	}

	/**
	 * Deletes the given article from the article map and invalidates all
	 * knowledge content that was in the article.
	 * 
	 * @param article The article to delete
	 */
	public void deleteArticle(Article article) {
		Environment.getInstance().buildAndRegisterArticle("",
				article.getTitle(), web, true);

		articleMap.remove(article.getTitle());

		Logger.getLogger(this.getClass().getName()).log(Level.INFO,
				"-> Deleted article '" + article.getTitle() + "'" + " from " + web);
	}

	public Set<String> getUpdatingArticles() {
		return Collections.unmodifiableSet(this.updatingArticles);
	}

	public void addArticleToUpdate(String title) {
		this.currentRefreshQueue.add(title);
	}

	public void addAllArticlesToUpdate(Collection<String> titles) {
		this.currentRefreshQueue.addAll(titles);
	}

	public boolean areArticlesInitialized() {
		return initializedArticles;
	}

	public void setArticlesInitialized(boolean b) {
		initializedArticles = true;
	}

	@Override
	public Collection<Class<? extends Event>> getEvents() {
		List<Class<? extends Event>> events = new ArrayList<Class<? extends Event>>(1);
		events.add(InitializedAllArticlesEvent.class);
		return events;
	}

	@Override
	public void notify(Event event) {
		if (event instanceof InitializedAllArticlesEvent) {
			updateQueuedArticles();
		}
	}

}