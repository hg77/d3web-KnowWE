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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.logging.Logger;

import javax.servlet.ServletContext;

import de.d3web.plugin.JPFPluginManager;
import de.d3web.plugin.Plugin;
import de.d3web.plugin.PluginManager;
import de.d3web.plugin.Resource;
import de.knowwe.core.append.PageAppendHandler;
import de.knowwe.core.compile.packaging.PackageManager;
import de.knowwe.core.compile.terminology.TerminologyManager;
import de.knowwe.core.event.EventManager;
import de.knowwe.core.kdom.Article;
import de.knowwe.core.kdom.RootType;
import de.knowwe.core.kdom.Type;
import de.knowwe.core.kdom.parsing.Sectionizer;
import de.knowwe.core.kdom.parsing.SectionizerModule;
import de.knowwe.core.taghandler.TagHandler;
import de.knowwe.core.utils.KnowWEUtils;
import de.knowwe.core.wikiConnector.WikiConnector;
import de.knowwe.event.InitEvent;
import de.knowwe.knowRep.KnowledgeRepresentationHandler;
import de.knowwe.knowRep.KnowledgeRepresentationManager;
import de.knowwe.plugin.Instantiation;
import de.knowwe.plugin.Plugins;

/**
 * This is the core class of KnowWE. It manages the {@link ArticleManager} and
 * provides methods to access {@link Article}s and other Managers. Further it is
 * connected to the used Wiki-engine, holding an instance of
 * {@link WikiConnector} and allows page saves.
 * 
 * @author Jochen
 */

public class Environment {

	/**
	 * An article manager for each web. In case of JSPWiki there is only on web
	 * ('default_web')
	 */
	private final Map<String, ArticleManager> articleManagers = new HashMap<String, ArticleManager>();

	/**
	 * A knowledge manager for each web. In case of JSPWiki there is only on web
	 * ('default_web')
	 */
	private final Map<String, KnowledgeRepresentationManager> knowledgeManagers = new HashMap<String, KnowledgeRepresentationManager>();

	/**
	 * A package manager for each web. In case of JSPWiki there is only on web
	 * ('default_web')
	 */
	private final Map<String, PackageManager> packageManagers = new HashMap<String, PackageManager>();

	/**
	 * A terminology handler for each web and article. In case of JSPWiki there
	 * is only on web ('default_web')
	 */
	private final Map<String, Map<String, TerminologyManager>> terminologyHandlers = new HashMap<String, Map<String, TerminologyManager>>();

	/**
	 * This is the link to the connected Wiki-engine. Allows saving pages etc.
	 */
	private WikiConnector wikiConnector = null;

	/**
	 * Holding the default tag handlers of KnowWE
	 */
	private final HashMap<String, TagHandler> tagHandlers = new HashMap<String, TagHandler>();

	/**
	 * The {@link CompilationMode} of KnowWE:
	 */
	private CompilationMode currentCompilationMode = CompilationMode.DEFAULT;

	/**
	 * Hard coded name of the default web
	 */
	public static final String DEFAULT_WEB = "default_web";

	/**
	 * This is used to mask HTML-syntax (and Markup, that collides with the
	 * wiki-core-markup) Necessary to enable HTML-generation in the
	 * pretranslate-hook, because the wiki engine (with HTML-diabled) escapes
	 * HTML So HTML needs to be masked with these replacements and are unmasked
	 * in the posttranslate-hook.
	 */
	public static final String HTML_DOUBLEQUOTE = "KNOWWEHTML_DOUBLEQUOTE";
	public static final String HTML_GT = "KNOWWEHTML_GREATERTHAN";
	public static final String HTML_ST = "KNOWWEHTML_SMALLERTHAN";
	public static final String HTML_QUOTE = "KNOWWEHTML_QUOTE";
	public static final String HTML_BRACKET_OPEN = "KNOWWE_BRACKET_OPEN";
	public static final String HTML_BRACKET_CLOSE = "KNOWWE_BRACKET_CLOSE";
	public static final String HTML_PLUGIN_BRACKETS_OPEN = "KNOWEPLUGIN_BRACKETS_OPEN";
	public static final String HTML_PLUGIN_BRACKETS_CLOSE = "KNOWEPLUGIN_BRACKETS_CLOSE";
	public static final String HTML_CURLY_BRACKET_OPEN = "KNOWWE_CURLY_BRACKET_OPEN";
	public static final String HTML_CURLY_BRACKET_CLOSE = "KNOWWE_CURLY_BRACKET_CLOSE";
	public static final String NEWLINE = "KNOWWE_NEWLINE";

	public enum CompilationMode {
		INCREMENTAL, DEFAULT
	}

	/**
	 * Singleton instance
	 */
	private static Environment instance;

	/**
	 * Singleton lazy factory
	 */
	public static synchronized Environment getInstance() {
		if (instance == null) {
			Logger.getLogger(Environment.class.getName()).severe(
					"Environment was not instantiated!");
		}
		return instance;
	}

	public static boolean isInitialized() {
		return instance != null;
	}

	public static void initInstance(WikiConnector wiki) {
		Logger.getLogger(Environment.class.getName()).info(
				"STARTING TO INITIALIZE KNOWWE ENVIRONMENT");

		instance = new Environment(wiki);
		instance.init();
		EventManager.getInstance().fireEvent(InitEvent.getInstance());

		Logger.getLogger(Environment.class.getName()).info(
				"INITIALIZED KNOWWE ENVIRONMENT");
	}

	/**
	 * private contructor
	 * 
	 * @see getInstance()
	 * 
	 * @param wiki Connector to the used core wiki engine
	 */
	private Environment(WikiConnector wiki) {
		this.wikiConnector = wiki;
	}

	private void init() throws InstantiationError {
		ResourceBundle config = KnowWEUtils.getConfigBundle();
		if (config != null && config.getString("compilation.mode").contains("incremental")) {
			this.setCompilationMode(CompilationMode.INCREMENTAL);
		}

		initPlugins();

		initTagHandler();

		initSectionizerModules();

		decorateTypeTree(RootType.getInstance());

		initInstantiations();

		initKnowledgeRepresentationHandler();

		Plugins.initJS();
		Plugins.initCSS();
	}

	private void initInstantiations() {
		for (Instantiation inst : Plugins.getInstantiations()) {
			inst.init();
		}
	}

	private void initTagHandler() {
		for (TagHandler tagHandler : Plugins.getTagHandlers()) {
			initTagHandler(tagHandler);
		}
	}

	private void initSectionizerModules() {
		for (SectionizerModule sm : Plugins.getSectionizerModules()) {
			Sectionizer.registerSectionizerModule(sm);
		}
	}

	private void initKnowledgeRepresentationHandler() {
		KnowledgeRepresentationManager manager = this.getKnowledgeRepresentationManager(DEFAULT_WEB);
		for (KnowledgeRepresentationHandler handler : Plugins.getKnowledgeRepresentationHandlers()) {
			handler.setWeb(DEFAULT_WEB);
			manager.registerHandler(handler);
		}
	}

	private void initPlugins() throws InstantiationError {
		File libDir = new File(KnowWEUtils.getApplicationRootPath() + "/WEB-INF/lib");
		// when testing, libDir doesn't exist, but the plugin framework is
		// initialized in junittest, so there is no problem
		// if libDir is doesn't exist in runtime, nothing will work, so this
		// code won't be reached ;-)
		if (libDir.exists()) {
			List<File> pluginFiles = getPluginFiles(libDir);
			JPFPluginManager.init(pluginFiles.toArray(new File[pluginFiles.size()]));
			extractPluginResources();
		}
	}

	private List<File> getPluginFiles(File libDir) {
		List<File> pluginFiles = new ArrayList<File>();
		for (File file : libDir.listFiles()) {
			if (file.getName().contains("KnowWE-Plugin-")
					|| file.getName().contains("d3web-Plugin-")) {
				pluginFiles.add(file);
			}
		}
		return pluginFiles;
	}

	private void extractPluginResources() throws InstantiationError {
		Plugin[] plugins = PluginManager.getInstance().getPlugins();

		for (Plugin plugin : plugins) {
			Resource[] resources = plugin.getResources();
			for (Resource resource : resources) {
				String pathName = resource.getPathName();
				if (!pathName.endsWith("/") && pathName.startsWith("webapp/")) {
					pathName = pathName.substring("webapp/".length());
					try {
						File file = new File(KnowWEUtils.getApplicationRootPath() + "/" + pathName);
						File parent = file.getParentFile();
						if (!parent.isDirectory()) {
							parent.mkdirs();
						}
						FileOutputStream out = new FileOutputStream(file);
						InputStream in = resource.getInputStream();
						try {
							byte[] buf = new byte[1024];
							int len;
							while ((len = in.read(buf)) != -1) {
								out.write(buf, 0, len);
							}
						}
						finally {
							in.close();
							out.close();
						}
					}
					catch (IOException e) {
						String msg = "Cannot instantiate plugin "
								+ plugin
								+ ", the following error occured while extracting its resources: "
								+ e.getMessage();
						throw new InstantiationError(msg);
					}
				}
			}
		}
	}

	private void decorateTypeTree(Type type) {

		Plugins.addChildrenTypesToType(type);
		Plugins.addSubtreeHandlersToType(type);
		Plugins.addRendererToType(type);

		if (type.getChildrenTypesInit() == null) return;

		for (Type childType : type.getChildrenTypesInit()) {
			if (childType.getPathToRoot() != null) continue;
			Type[] pathToRoot = type.getPathToRoot();
			Type[] childPath = new Type[pathToRoot.length + 1];
			System.arraycopy(pathToRoot, 0, childPath, 0, pathToRoot.length);
			childPath[childPath.length - 1] = childType;
			childType.setPathToRoot(childPath);
			decorateTypeTree(childType);
		}

	}

	private void initTagHandler(TagHandler tagHandler) {
		String tagName = tagHandler.getTagName();
		String key = tagName.toLowerCase();

		if (tagHandlers.containsKey(key)) {
			Logger.getLogger(this.getClass().getName()).warning(
					"TagHandler for tag '" + tagName
							+ "' had already been added.");
		}
		else {
			this.tagHandlers.put(key, tagHandler);
		}
	}

	public WikiConnector getWikiConnector() {
		return this.wikiConnector;
	}

	/**
	 * Builds an {@link Article} and registers it in the {@link ArticleManager}.
	 */
	public Article buildAndRegisterArticle(String content,
			String title, String web) {
		return buildAndRegisterArticle(content, title, web, false);
	}

	/**
	 * Builds an {@link Article} and registers it in the {@link ArticleManager}.
	 */
	public Article buildAndRegisterArticle(String content,
			String title, String web, boolean fullParse) {

		if (Article.isArticleCurrentlyBuilding(web, title)) {
			return Article.getCurrentlyBuildingArticle(web, title);
		}

		// create article with the new content
		Article article = Article.createArticle(content, title, web);

		this.getArticleManager(web).registerArticle(article);

		return article;
	}

	/**
	 * Returns the {@link Article} object for a given web and title
	 * 
	 * @param web the web of the {@link Article}
	 * @param title the title of the {@link Article}
	 */
	public Article getArticle(String web, String title) {
		return getArticleManager(web).getArticle(title);
	}

	/**
	 * Returns the {@link ArticleManager} for a given web.
	 * 
	 * @param web the web of the {@link ArticleManager}
	 */
	public ArticleManager getArticleManager(String web) {
		ArticleManager mgr = this.articleManagers.get(web);
		if (mgr == null) {
			mgr = new ArticleManager(this, web);
			articleManagers.put(web, mgr);
		}
		return mgr;
	}

	public KnowledgeRepresentationManager getKnowledgeRepresentationManager(String web) {
		KnowledgeRepresentationManager mgr = this.knowledgeManagers.get(web);
		if (mgr == null) {
			mgr = new KnowledgeRepresentationManager(web);
			knowledgeManagers.put(web, mgr);
		}
		return mgr;
	}

	/**
	 * Returns the {@link PackageManager} for a given web.
	 * 
	 * @param web the web of the {@link PackageManager}
	 */
	public PackageManager getPackageManager(String web) {
		PackageManager mgr = this.packageManagers.get(web);
		if (mgr == null) {
			mgr = new PackageManager(web);
			packageManagers.put(web, mgr);
		}
		return mgr;
	}

	/**
	 * returns the TerminologyHandler for a given web
	 * 
	 * @param web
	 * @return
	 */
	public TerminologyManager getTerminologyManager(String web, String title) {
		Map<String, TerminologyManager> handlersOfWeb = this.terminologyHandlers.get(web);
		if (handlersOfWeb == null) {
			handlersOfWeb = new HashMap<String, TerminologyManager>();
			this.terminologyHandlers.put(web, handlersOfWeb);
		}
		TerminologyManager mgr = handlersOfWeb.get(title);
		if (mgr == null) {
			mgr = new TerminologyManager(web, title);
			handlersOfWeb.put(title, mgr);
		}
		return mgr;
	}

	public ServletContext getContext() {
		return wikiConnector.getServletContext();
	}

	/**
	 * grants access on the default tag handlers of KnowWE
	 * 
	 * @return HashMap holding the default tag handlers of KnowWE
	 */
	public HashMap<String, TagHandler> getDefaultTagHandlers() {
		return tagHandlers;
	}

	public List<PageAppendHandler> getAppendHandlers() {
		return Plugins.getPageAppendHandlers();
	}

	public void setCompilationMode(CompilationMode mode) {
		currentCompilationMode = mode;
	}

	public CompilationMode getCompilationMode() {
		return currentCompilationMode;
	}

	/**
	 * Cloning is not allowed for the Environment of KnowWE.
	 */
	@Override
	public Object clone() throws CloneNotSupportedException {
		throw new CloneNotSupportedException();
	}

}
