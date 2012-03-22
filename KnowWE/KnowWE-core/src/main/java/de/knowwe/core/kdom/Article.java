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

package de.knowwe.core.kdom;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import de.knowwe.core.Environment;
import de.knowwe.core.Environment.CompilationMode;
import de.knowwe.core.compile.Priority;
import de.knowwe.core.compile.ReviseIterator;
import de.knowwe.core.compile.ReviseIterator.SectionPriorityTuple;
import de.knowwe.core.event.EventManager;
import de.knowwe.core.kdom.parsing.Section;
import de.knowwe.core.kdom.parsing.Sections;
import de.knowwe.event.ArticleCreatedEvent;
import de.knowwe.event.FullParseEvent;
import de.knowwe.event.KDOMCreatedEvent;
import de.knowwe.event.PreCompileFinishedEvent;

/**
 * 
 * This class is the representation of one wiki article in KnowWE. It is a Type
 * that always forms the root node and only the root node of each KDOM
 * document-parse-tree.
 * 
 * @author Jochen
 */
public class Article extends AbstractType {

	/**
	 * Name of this article (topic-name)
	 */
	private final String title;

	private final String web;

	/**
	 * The section representing the root-node of the KDOM-tree
	 */
	private Section<Article> sec;

	private Article lastVersion;

	private final long startTimeOverall;

	private long currentStartTime;

	private boolean fullParse;

	private final boolean reParse;

	private boolean postDestroy;

	private boolean postPreDestroy;

	private boolean postDestroyFullParse;

	private boolean postPreDestroyFullParse;

	private boolean secondBuild;

	private ReviseIterator reviseIterator;

	private final Set<String> classesCausingFullParse = new HashSet<String>();

	private static Map<String, Article> currentlyBuildingArticles = Collections.synchronizedMap(new HashMap<String, Article>());

	private static String getArticleKey(String web, String title) {
		return web + title;
	}

	public static boolean isArticleCurrentlyBuilding(String web, String title) {
		return currentlyBuildingArticles.containsKey(getArticleKey(web, title));
	}

	public static Article getCurrentlyBuildingArticle(String web, String title) {
		return currentlyBuildingArticles.get(getArticleKey(web, title));
	}

	public static Article createArticle(String text, String title, RootType rootType,
			String web) {
		return createArticle(text, title, rootType, web, false);
	}

	public static Article createArticle(String text, String title, RootType rootType,
			String web, boolean fullParse) {

		if (isArticleCurrentlyBuilding(web, title)) {
			Logger.getLogger(Article.class.getName()).severe(
					"The article '"
							+ title
							+ "' is build more than once at the same time, "
							+ "this should not be done! Developer please check with "
							+ "Article#isArticleCurrentlyBuilding(String) first!");
		}
		Article article = null;
		currentlyBuildingArticles.put(getArticleKey(web, title), null);
		try {
			article = new Article(text, title, rootType,
					web, fullParse);
			EventManager.getInstance().fireEvent(new ArticleCreatedEvent(article));
		}
		catch (Throwable e) {
			e.printStackTrace();
		}
		finally {
			currentlyBuildingArticles.remove(getArticleKey(web, title));
		}

		return article;
	}

	/**
	 * Constructor: starts recursive parsing by creating new Section object
	 * 
	 * @param text
	 * @param title
	 * @param allowedObjects
	 */
	private Article(String text, String title, RootType rootType,
			String web, boolean fullParse) {

		Logger.getLogger(this.getClass().getName()).log(Level.INFO,
				"====>> Starting to build article '" + title + "' ====>>");

		currentlyBuildingArticles.put(getArticleKey(web, title), this);
		this.startTimeOverall = System.currentTimeMillis();
		this.currentStartTime = this.startTimeOverall;
		this.title = title;
		this.web = web;
		this.childrenTypes.add(rootType);
		this.lastVersion = Environment.getInstance().getArticle(web, title);

		boolean unchangedContent = lastVersion != null
				&& lastVersion.getSection().getText().equals(text);

		this.reParse = unchangedContent && fullParse;

		boolean defFullParse = fullParse
				|| lastVersion == null
				|| Environment.getInstance().getCompilationMode() == CompilationMode.DEFAULT;

		this.fullParse = defFullParse;

		build(text);

		if (this.postDestroyFullParse) {
			this.secondBuild = true;
			build(text);
		}

		// if for example a SubtreeHandlers uses
		// Article#setFullParse(Class) he prevents incremental updating
		if (!defFullParse && !classesCausingFullParse.isEmpty()) {
			Logger.getLogger(this.getClass().getName()).log(
					Level.INFO, "The following classes " +
							"caused a full parse:\n" +
							classesCausingFullParse.toString());
		}

		// important! prevents memory leak
		lastVersion = null;
	}

	private void build(String text) {

		this.postPreDestroy = false;
		this.postDestroy = false;

		sectionizeArticle(text);

		Logger.getLogger(this.getClass().getName()).log(
				Level.FINE,
				"<- Built KDOM in "
						+ (System.currentTimeMillis() - currentStartTime) + "ms <-");
		currentStartTime = System.currentTimeMillis();

		preCompile();

		compile();

		postCompile();

		Logger.getLogger(this.getClass().getName()).log(
				Level.FINE,
				"<- Built Knowledge in "
						+ (System.currentTimeMillis() - currentStartTime) + "ms <-");
		currentStartTime = System.currentTimeMillis();
	}

	private void sectionizeArticle(String text) {

		// create Sections recursively
		sec = Section.createSection(text, this, null);
		sec.setArticle(this);
		getRootType().getParser().parse(text, sec);

		sec.setAbsolutePositionStartInArticle(0);
		sec.clearReusedSuccessorRecursively();

		if (lastVersion != null) {
			lastVersion.getSection().clearReusedOfOldSectionsRecursively(this);
			unregisterSectionIDRecursively(lastVersion.getSection());
		}

		EventManager.getInstance().fireEvent(new KDOMCreatedEvent(this));
	}

	private void preCompile() {
		if (this.fullParse) {
			EventManager.getInstance().fireEvent(new FullParseEvent(this));
		}

		// destroy
		if (!this.fullParse && this.lastVersion != null) {
			lastVersion.reviseIterator.reset();
		}
		destroy(Priority.PRECOMPILE_LOW);
		this.postPreDestroy = true;

		// create
		reviseIterator = new ReviseIterator();
		reviseIterator.addRootSectionToRevise(sec);
		create(Priority.PRECOMPILE_LOW);
		EventManager.getInstance().fireEvent(new PreCompileFinishedEvent(this));
	}

	private void compile() {

		// destroy
		destroy(Priority.LOWEST);
		this.postDestroy = true;

		Environment.getInstance().getKnowledgeRepresentationManager(web)
				.initArticle(this);

		// create
		if (this.postPreDestroyFullParse && !this.secondBuild) {
			reviseIterator = new ReviseIterator();
			reviseIterator.addRootSectionToRevise(sec);
		}
		create(Priority.LOWEST);
	}

	private void postCompile() {
		for (Section<?> node : reviseIterator.getAllSections()) {
			node.setReusedBy(title, true);
		}
	}

	private void destroy(Priority p) {
		if (!this.fullParse && this.lastVersion != null) {
			lastVersion.reviseIterator.setIteratorStop(p);
			while (lastVersion.reviseIterator.hasNext()) {
				SectionPriorityTuple tuple = lastVersion.reviseIterator.next();
				tuple.getSection().letSubtreeHandlersDestroy(this, tuple.getPriority());
			}

		}
	}

	private void create(Priority p) {
		reviseIterator.setIteratorStop(p);
		// compile the handlers with main priorities
		while (reviseIterator.hasNext()) {
			SectionPriorityTuple tuple = reviseIterator.next();
			tuple.getSection().letSubtreeHandlersCreate(this, tuple.getPriority());
		}
	}

	private void unregisterSectionIDRecursively(Section<?> section) {
		Sections.unregisterSectionID(section);
		for (Section<?> childSection : section.getChildren()) {
			unregisterSectionIDRecursively(childSection);
		}
	}

	/**
	 * Returns the title of this Article.
	 */
	public String getTitle() {
		return title;
	}

	public String getWeb() {
		return web;
	}

	/**
	 * The last version is only available during the initialization of the
	 * article
	 */
	public Article getLastVersionOfArticle() {
		return lastVersion;
	}

	public long getStartTime() {
		return this.startTimeOverall;
	}

	/**
	 * Returns the simple name of this class, NOT THE NAME (Title) OF THIS
	 * ARTICLE! For the articles title, use getTitle() instead!
	 */
	@Override
	public String getName() {
		return this.getClass().getSimpleName();
	}

	public Section<Article> getSection() {
		return sec;
	}

	public Section<? extends Type> findSmallestNodeContaining(int start, int end) {
		return Sections.findSmallestNodeContaining(sec, start, end);
	}

	private final Map<String, Map<String, List<Section<?>>>> knownResults =
			new HashMap<String, Map<String, List<Section<?>>>>();

	/**
	 * Finds all children with the same path of Types in the KDOM. The
	 * <tt>path</tt> has to start with the type Article and end with the Type of
	 * the Sections you are looking for.
	 * 
	 * @return Map of Sections, using their originalText as key.
	 */
	public Map<String, List<Section<?>>> findSectionsWithTypePathAsMap(List<Class<? extends Type>> path) {
		String stringPath = path.toString();
		Map<String, List<Section<? extends Type>>> foundChildren = knownResults.get(stringPath);
		if (foundChildren == null) {
			foundChildren = new HashMap<String, List<Section<? extends Type>>>();
			Sections.findSuccessorsWithTypePath(sec, path, 0, foundChildren);
			knownResults.put(stringPath, foundChildren);
		}
		return foundChildren;
	}

	/**
	 * Finds all children with the same path of Types in the KDOM. The
	 * <tt>path</tt> has to start with the Article and end with the ObjectType
	 * of the Sections you are looking for.
	 * 
	 * @return List of Sections
	 */
	public List<Section<? extends Type>> findSectionsWithTypePathAsList(
			LinkedList<Class<? extends Type>> path) {
		List<Section<? extends Type>> foundChildren = new ArrayList<Section<? extends Type>>();
		Sections.findSuccessorsWithTypePath(sec, path, 0, foundChildren);
		return foundChildren;
	}

	public String collectTextsFromLeaves() {
		StringBuilder buffi = new StringBuilder();
		this.sec.collectTextsFromLeaves(buffi);
		return buffi.toString();
	}

	public List<Section<? extends Type>> getAllNodesPreOrder() {
		List<Section<? extends Type>> nodes = new ArrayList<Section<? extends Type>>();
		Sections.getAllNodesPreOrder(sec, nodes);
		return nodes;
	}

	public List<Section<? extends Type>> getAllNodesPostOrder() {
		List<Section<? extends Type>> nodes = new LinkedList<Section<? extends Type>>();
		Sections.getAllNodesPostOrder(sec, nodes);
		return nodes;
	}

	// public List<Section<? extends Type>>
	// getAllNodesToDestroyPostOrder() {
	// List<Section<? extends Type>> nodes = new
	// LinkedList<Section<? extends Type>>();
	// if (lastVersion != null)
	// lastVersion.sec.getAllNodesToDestroyPostOrder(this, nodes);
	// return nodes;
	// }

	@Override
	public String toString() {
		return sec.getText();
	}

	// public Set<Section<Include>> getActiveIncludes() {
	// return this.activeIncludes;
	// }

	// private void reviseLastArticleToDestroy() {
	//
	// List<Section<?>> nodes = getAllNodesToDestroyPostOrder();
	// // Collections.reverse(nodes);
	// TreeMap<Priority, List<Section<? extends Type>>> prioMap =
	// Priority.createPrioritySortedList(nodes);
	//
	// for (Priority priority : prioMap.descendingKeySet()) {
	// List<Section<? extends Type>> prioList =
	// prioMap.get(priority);
	// for (Section<? extends Type> section : prioList) {
	// section.letSubtreeHandlersDestroy(this, priority);
	// }
	//
	// }
	// }
	//
	// private void reviseCurrentArticleToCreate() {
	// TreeMap<Priority, List<Section<? extends Type>>> prioMap =
	// Priority.createPrioritySortedList(getAllNodesPostOrder());
	//
	// for (Priority priority : prioMap.descendingKeySet()) {
	// List<Section<? extends Type>> prioList =
	// prioMap.get(priority);
	// for (Section<? extends Type> section : prioList) {
	// section.letSubtreeHandlersCreate(this, priority);
	// }
	// }
	// sec.setReusedStateRecursively(title, true);
	// }

	// // This method is needed for the case that Sections get reused and are
	// flagged
	// // false from previous revising.
	// private List<Section<? extends Type>>
	// setAllHandlersToNotYetRevised
	// (List<Section<? extends Type>> sectionList) {
	// for (Section<? extends Type> section:sectionList) {
	// for (SubtreeHandler<? extends Type> handler
	// :section.get().getSubtreeHandlers()) {
	// handler.setNotYetRevisedBy(title, true);
	// }
	// }
	// return sectionList;
	// }

	public boolean isFullParse() {
		return this.fullParse;
	}

	public boolean isReParse() {
		return this.reParse;
	}

	public boolean isPostDestroyFullParse() {
		return this.postDestroyFullParse;
	}

	public boolean isSecondBuild() {
		return this.secondBuild;
	}

	public Type getRootType() {
		return getChildrenTypes().get(0);
	}

	public ReviseIterator getReviseIterator() {
		return this.reviseIterator;
	}

	/**
	 * Causes an full parse for this article.
	 * 
	 * @created 09.10.2010
	 * @param source is just for tracking...
	 */
	public void setFullParse(Class<?> source) {
		if (!this.fullParse) {
			if (this.postPreDestroy) this.postPreDestroyFullParse = true;
			if (this.postDestroy) this.postDestroyFullParse = true;
			sec.setNotCompiledByRecursively(title);
			EventManager.getInstance().fireEvent(new FullParseEvent(this));
		}
		classesCausingFullParse.add(source.isAnonymousClass()
				? source.getName().substring(
						source.getName().lastIndexOf(".") + 1)
				: source.getSimpleName());

		this.fullParse = true;
	}

	public Set<String> getClassesCausingFullParse() {
		return this.classesCausingFullParse;
	}

}