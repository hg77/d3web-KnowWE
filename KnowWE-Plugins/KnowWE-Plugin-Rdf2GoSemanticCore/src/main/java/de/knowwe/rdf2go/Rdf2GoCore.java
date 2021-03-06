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
package de.knowwe.rdf2go;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;
import java.util.logging.Level;

import info.aduna.iteration.Iterations;
import org.openrdf.model.BNode;
import org.openrdf.model.Namespace;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.LiteralImpl;
import org.openrdf.model.vocabulary.XMLSchema;
import org.openrdf.query.BindingSet;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.QueryInterruptedException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.RepositoryResult;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFParseException;

import com.denkbares.collections.MultiMap;
import com.denkbares.collections.MultiMaps;
import com.denkbares.collections.N2MMap;
import com.denkbares.events.EventManager;
import com.denkbares.semanticcore.CachedTupleQueryResult;
import com.denkbares.semanticcore.RepositoryConnection;
import com.denkbares.semanticcore.SemanticCore;
import com.denkbares.semanticcore.SesameEndpoint;
import com.denkbares.semanticcore.TupleQuery;
import com.denkbares.semanticcore.TupleQueryResult;
import com.denkbares.semanticcore.config.RdfConfig;
import com.denkbares.semanticcore.config.RepositoryConfig;
import com.denkbares.semanticcore.config.RepositoryConfigs;
import com.denkbares.semanticcore.sparql.SPARQLEndpoint;
import com.denkbares.strings.Identifier;
import com.denkbares.strings.Strings;
import com.denkbares.utils.Log;
import com.denkbares.utils.Stopwatch;
import de.d3web.core.inference.RuleSet;
import de.knowwe.core.Environment;
import de.knowwe.core.compile.CompilerManager;
import de.knowwe.core.compile.Compilers;
import de.knowwe.core.compile.PackageCompiler;
import de.knowwe.core.compile.packaging.PackageCompileType;
import de.knowwe.core.kdom.Article;
import de.knowwe.core.kdom.Type;
import de.knowwe.core.kdom.parsing.Section;
import de.knowwe.core.kdom.parsing.Sections;
import de.knowwe.core.utils.KnowWEUtils;
import de.knowwe.rdf2go.sparql.utils.SparqlQuery;
import de.knowwe.rdf2go.utils.Rdf2GoUtils;

public class Rdf2GoCore {

	public static final String LNS_ABBREVIATION = "lns";

	private enum SparqlType {
		SELECT, CONSTRUCT, ASK
	}

	private static final AtomicLong coreId = new AtomicLong(0);

	private static Rdf2GoCore globaleInstance;

	public static final String GLOBAL = "GLOBAL";

	private static final ThreadPoolExecutor sparqlThreadPool = createThreadPool(
			Math.max(Runtime.getRuntime().availableProcessors() - 1, 1), "KnowWE-Sparql-Thread");

	private static final ThreadPoolExecutor shutDownThreadPool = createThreadPool(
			Math.max(Runtime.getRuntime()
					.availableProcessors() - 1, 1), "KnowWE-SemanticCore-Shutdown-Thread");

	private static final ThreadPoolExecutor sparqlReaperPool = createThreadPool(
			sparqlThreadPool.getMaximumPoolSize(), "KnowWE-Sparql-Deamon");

	public static final int DEFAULT_TIMEOUT = 15000;

	private static final int DEFAULT_MAX_CACHE_SIZE = 1000000; // should be below 100 MB of cache (we count each cell)

	private final Map<String, SparqlTask> resultCache = new LinkedHashMap<>(16, 0.75f, true);

	private int resultCacheSize = 0;

	/**
	 * Some models have extreme slow downs if during a SPARQL query new statements are added or
	 * removed. Concurrent SPARQLs however are no problem. Therefore we use a lock that locks
	 * exclusively for writing but shared for reading.
	 */

	private final Object statementLock = new Object();

	public static Rdf2GoCore getInstance(Section<?> section) {
		Rdf2GoCompiler compiler = Compilers.getCompiler(section, Rdf2GoCompiler.class);
		return getInstance(compiler);
	}

	public static Rdf2GoCore getInstance(Rdf2GoCompiler compiler) {
		return compiler.getRdf2GoCore();
	}

	@Deprecated
	public static Rdf2GoCore getInstance(Article master) {
		List<Section<PackageCompileType>> compileSections = Sections.successors(
				master.getRootSection(), PackageCompileType.class);
		for (Section<PackageCompileType> section : compileSections) {
			Collection<PackageCompiler> packageCompilers = section.get().getPackageCompilers(
					section);
			for (PackageCompiler packageCompiler : packageCompilers) {
				if (packageCompiler instanceof Rdf2GoCompiler) {
					return ((Rdf2GoCompiler) packageCompiler).getRdf2GoCore();
				}
			}
		}
		return null;
	}

	private static ThreadPoolExecutor createThreadPool(int threadCount, final String threadName) {
		return (ThreadPoolExecutor) Executors.newFixedThreadPool(threadCount,
				runnable -> new Thread(runnable, threadName));
	}

	@Deprecated
	public static Rdf2GoCore getInstance(String web, String master) {
		//noinspection deprecation
		return getInstance(KnowWEUtils.getArticleManager(web).getArticle(master));
	}

	/**
	 * @return one global instance for all webs an compilers
	 * @created 14.12.2013
	 * @deprecated use {@link Rdf2GoCore#getInstance(Rdf2GoCompiler)} instead. Using the global
	 * instance will cause problems with different webs or different article managers
	 */
	@Deprecated
	public static Rdf2GoCore getInstance() {
		if (globaleInstance == null) {
			globaleInstance = new Rdf2GoCore();
		}
		return globaleInstance;
	}

	private final String bns;

	private final String lns;

	private RepositoryConfig ruleSet;

	private final MultiMap<StatementSource, Statement> statementCache =
			new N2MMap<>(
					MultiMaps.hashMinimizedFactory(),
					MultiMaps.hashMinimizedFactory());

	/**
	 * All namespaces known to KnowWE. Key is the namespace abbreviation, value
	 * is the full namespace, e.g. rdf and
	 * http://www.w3.org/1999/02/22-rdf-syntax-ns#
	 */
	private Map<String, String> namespaces = new HashMap<>();

	private final Object nsMutext = new Object();

	/**
	 * For optimization reasons, we hold a map of all namespacePrefixes as they are used e.g. in
	 * Turtle and SPARQL
	 */
	private volatile Map<String, String> namespacePrefixes = new HashMap<>();

	private final Object nsPrefixMutex = new Object();

	private Set<Statement> insertCache;
	private Set<Statement> removeCache;
	private long lastModified = System.currentTimeMillis();

	private SemanticCore semanticCore;

	/**
	 * Initializes the Rdf2GoCore with the default settings specified
	 * in the "owlim.ttl" file.
	 */
	public Rdf2GoCore() {
		this(null);
	}

	/**
	 * Initializes the Rdf2GoCore with the specified {@link RuleSet}. Please note
	 * that this only has an effect if OWLIM is used as underlying implementation.
	 *
	 * @param ruleSet specifies the reasoning profile.
	 */
	public Rdf2GoCore(RepositoryConfig ruleSet) {
		this(Environment.getInstance().getWikiConnector().getBaseUrl()
						+ "Wiki.jsp?page=", "http://ki.informatik.uni-wuerzburg.de/d3web/we/knowwe.owl#",
				ruleSet
		);
	}

	/**
	 * Initializes the Rdf2GoCore with the specified arguments. Please note
	 * that the RuleSet argument only has an effect if OWLIM is used as underlying
	 * implementation.
	 *
	 * @param lns the uri used as local namespace
	 * @param bns the uri used as base namespace
	 * @param reasoning the rule set (only relevant for OWLIM model)
	 */
	public Rdf2GoCore(String lns, String bns, RepositoryConfig reasoning) {
		if (reasoning == null) {
			reasoning = RepositoryConfigs.get(RdfConfig.class);
		}
		this.bns = bns;
		this.lns = lns;
		try {
			semanticCore = SemanticCore.getOrCreateInstance(String.valueOf(coreId.incrementAndGet()), reasoning);
			semanticCore.allocate(); // make sure the core does not shut down on its own...
			Log.info("Semantic core with reasoning '" + reasoning.getName() + "' initialized");
		}
		catch (IOException e) {
			Log.severe("Unable to create SemanticCore", e);
			return;
		}
		this.ruleSet = reasoning;

		insertCache = new HashSet<>();
		removeCache = new HashSet<>();

		// lock probably not necessary here, just to make sure...
		this.namespaces = getSemanticCoreNameSpaces();
		initDefaultNamespaces();
	}

	/**
	 * Make sure to close the connection after use!
	 */
	public RepositoryConnection getRepositoryConnection() throws RepositoryException {
		return semanticCore.getConnection();
	}

	public Date getLastModified() {
		return new Date(lastModified);
	}

	/**
	 * Add a namespace to the model.
	 *
	 * @param abbreviation the short version of the namespace
	 * @param namespace the namespace (URL)
	 */
	public void addNamespace(String abbreviation, String namespace) {
		synchronized (nsMutext) {
			try {
				try (RepositoryConnection connection = semanticCore.getConnection()) {
					connection.setNamespace(abbreviation, namespace);
				}
				namespaces = null; // clear caches namespaces, will be get created lazy if needed
				namespacePrefixes = null;
			}
			catch (RepositoryException e) {
				Log.severe("Exception while adding namespace", e);
			}
		}
	}

	/**
	 * De-resolves a specified uri to a short uri name. If there is no matching
	 * namespace, the full uri is returned.
	 *
	 * @param uri the uri to be de-resolved
	 * @return the short uri name
	 * @created 13.11.2013
	 */
	public URI toShortURI(java.net.URI uri) {
		return toShortURI(new org.openrdf.model.impl.URIImpl(uri.toString()));
	}

	/**
	 * De-resolves a specified uri to a short uri name. If there is no matching
	 * namespace, the full uri is returned.
	 *
	 * @param uri the uri to be de-resolved
	 * @return the short uri name
	 * @created 13.11.2013
	 */
	public URI toShortURI(URI uri) {
		String uriText = uri.toString();
		int length = 0;
		URI shortURI = uri;
		for (Entry<String, String> entry : namespaces.entrySet()) {
			String partURI = entry.getValue();
			int partLength = partURI.length();
			if (partLength > length && uriText.length() > partLength && uriText.startsWith(partURI)) {
				String shortText = entry.getKey() + ":" + uriText.substring(partLength);
				shortURI = new ShortURIImpl(shortText, uri);
				length = partLength;
			}
		}
		return shortURI;
	}

	/**
	 * Returns the terminology manager's identifier for the specified uri. The
	 * uri's identifier is usually based on the short version of the uri, if
	 * there is any.
	 *
	 * @param uri the uri to create the identifier for
	 * @return the identifier for the specified uri
	 * @created 13.11.2013
	 */
	public Identifier toIdentifier(URI uri) {
		return ShortURIImpl.toIdentifier(toShortURI(uri));
	}

	/**
	 * Returns the terminology manager's identifier for the specified uri. The
	 * uri's identifier is usually based on the short version of the uri, if
	 * there is any.
	 *
	 * @param uri the uri to create the identifier for
	 * @return the identifier for the specified uri
	 * @created 13.11.2013
	 */
	public Identifier toIdentifier(java.net.URI uri) {
		return ShortURIImpl.toIdentifier(toShortURI(uri));
	}

	/**
	 * Adds the given {@link Statement}s for the given {@link SectionSource} to the
	 * triple store.
	 * <p>
	 * You can remove the {@link Statement}s using the method
	 * {@link Rdf2GoCore#removeStatements(Section)}.
	 *
	 * @param source the {@link StatementSource} for which the {@link Statement}s are added and
	 * cached
	 * @param statements the {@link Statement}s to add
	 */
	public void addStatements(StatementSource source, Statement... statements) {
		addStatements(source, Arrays.asList(statements));
	}

	/**
	 * Adds the given {@link Statement}s for the given {@link SectionSource} to the
	 * triple store.
	 * <p>
	 * You can remove the {@link Statement}s using the method
	 * {@link Rdf2GoCore#removeStatements(Section)}.
	 *
	 * @param source the {@link StatementSource} for which the {@link Statement}s are added and
	 * cached
	 * @param statements the {@link Statement}s to add
	 */
	public void addStatements(StatementSource source, Collection<Statement> statements) {
		synchronized (statementLock) {
			for (Statement statement : statements) {
				if (!statementCache.containsValue(statement)) {
					insertCache.add(statement);
				}
				statementCache.put(source, statement);
			}
		}
	}

	/**
	 * Adds the given {@link Statement}s for the given {@link Section} to the
	 * triple store.
	 * <p>
	 * You can remove the {@link Statement}s using the method
	 * {@link Rdf2GoCore#removeStatements(Section)}.
	 *
	 * @param section the {@link Section} for which the {@link Statement}s are added and cached
	 * @param statements the {@link Statement}s to add
	 * @created 06.12.2010
	 */
	public void addStatements(Section<?> section, Statement... statements) {
		addStatements(new SectionSource(section), Arrays.asList(statements));
	}

	/**
	 * Adds the given {@link Statement}s for the given {@link Section} to the
	 * triple store.
	 * <p>
	 * You can remove the {@link Statement}s using the method
	 * {@link Rdf2GoCore#removeStatements(Section)}.
	 *
	 * @param section the {@link Section} for which the {@link Statement}s are added and cached
	 * @param statements the {@link Statement}s to add
	 * @created 06.12.2010
	 */
	public void addStatements(Section<?> section, Collection<Statement> statements) {
		addStatements(new SectionSource(section), statements);
	}

	/**
	 * Adds the given {@link Statement}s directly to the triple store.
	 * <p>
	 * <b>Attention</b>: The added {@link Statement}s are not cached in the
	 * {@link Rdf2GoCore}, so you are yourself responsible to remove the right
	 * {@link Statement}s in case they are not longer valid. You can remove
	 * these {@link Statement}s with the method
	 * {@link Rdf2GoCore#removeStatements(Collection)}.
	 *
	 * @param statements the statements you want to add to the triple store
	 * @created 13.06.2012
	 */
	public void addStatements(Statement... statements) {
		addStatements(Arrays.asList(statements));
	}

	/**
	 * Adds the given {@link Statement}s directly to the triple store.
	 * <p>
	 * <b>Attention</b>: The added {@link Statement}s are not cached in the
	 * {@link Rdf2GoCore}, so you are yourself responsible to remove the right
	 * {@link Statement}s in case they are not longer valid. You can remove
	 * these {@link Statement}s with the method
	 * {@link Rdf2GoCore#removeStatements(Collection)}.
	 *
	 * @param statements the statements you want to add to the triple store
	 * @created 13.06.2012
	 */
	public void addStatements(Collection<Statement> statements) {
		addStatements((StatementSource) null, statements);
	}

	/**
	 * Commit is automatically called every time an article has finished
	 * compiling. When commit is called, all {@link Statement}s that were cached
	 * to be removed from the triple store are removed and all {@link Statement}
	 * s that were cached to be added to the triple store are added.
	 *
	 * @created 12.06.2012
	 */
	public void commit() {

		try {
			int removeSize = removeCache.size();
			int insertSize = insertCache.size();
			boolean verboseLog = removeSize + insertSize < 50 && !Log.logger()
					.isLoggable(Level.FINE);

			if (removeSize > 0 || insertSize > 0) {
				synchronized (resultCache) {
					resultCache.clear();
					resultCacheSize = 0;
				}
			}

            /*
			Hazard Filter:
			Since removing statements is expansive, we do not remove statements
			that are inserted again anyway.
			Since inserting a statement is cheap and the fact that a statement in
			the remove cache has not necessarily been committed to the model
			before (e.g. compiling the same sections multiple times before the
			first commit), we do not remove statements from the insert cache.
			Duplicate statements are ignored by the model anyway.
			*/

			removeCache.removeAll(insertCache);


            /*
			Do actual changes on the model
             */
			long connectionStart = System.currentTimeMillis();
			try (RepositoryConnection connection = semanticCore.getConnection()) {
				connection.begin();

				connection.remove(removeCache);
				connection.add(insertCache);

				connection.commit();
			}

            /*
			Fire events
             */
			boolean removedStatements = false;
			if (!removeCache.isEmpty()) {
				EventManager.getInstance()
						.fireEvent(new RemoveStatementsEvent(Collections.unmodifiableCollection(removeCache), this));
				removedStatements = true;
			}
			boolean insertedStatements = false;
			if (!insertCache.isEmpty()) {
				EventManager.getInstance()
						.fireEvent(new InsertStatementsEvent(Collections.unmodifiableCollection(removeCache), Collections
								.unmodifiableCollection(insertCache), this));
				insertedStatements = true;
			}
			if (removedStatements || insertedStatements) {
				EventManager.getInstance().fireEvent(new ChangedStatementsEvent(this));
			}

            /*
			Logging
             */
			if (verboseLog) {
				logStatements(removeCache, connectionStart,
						"Removed statements:\n");
				logStatements(insertCache, connectionStart,
						"Inserted statements:\n");
			}
			else {
				Log.info("Removed " + removeSize + " statements from and added "
						+ insertSize
						+ " statements to " + Rdf2GoCore.class.getSimpleName() + " in "
						+ (System.currentTimeMillis() - connectionStart) + "ms.");
			}

			Log.info("Current number of statements: " + statementCache.size());

            /*
			Reset caches
             */
			removeCache = new HashSet<>();
			insertCache = new HashSet<>();
		}
		catch (RepositoryException e) {
			Log.severe("Exception while committing changes to repository", e);
		}
		finally {
			// outside of commit an auto committing connection seems to be ok
			lastModified = System.currentTimeMillis();
			EventManager.getInstance().fireEvent(new Rdf2GoCoreCommitFinishedEvent(this));
		}
	}

	public BNode createBlankNode() {
		return getValueFactory().createBNode();
	}

	public BNode createBlankNode(String internalID) {
		return getValueFactory().createBNode(internalID);
	}

	/**
	 * Creates a xsd:boolean datatype literal with the specified boolean value.
	 *
	 * @param boolValue the value of the literal
	 * @return a datatype literal for the specified value
	 */
	public org.openrdf.model.Literal createDatatypeLiteral(boolean boolValue) {
		return createDatatypeLiteral(String.valueOf(boolValue), XMLSchema.BOOLEAN);
	}

	/**
	 * Creates a xsd:integer datatype literal with the specified int value.
	 *
	 * @param intValue the value of the literal
	 * @return a datatype literal for the specified value
	 */
	public org.openrdf.model.Literal createDatatypeLiteral(int intValue) {
		return createDatatypeLiteral(String.valueOf(intValue), XMLSchema.INTEGER);
	}

	/**
	 * Creates a xsd:double datatype literal with the specified double value.
	 *
	 * @param doubleValue the value of the literal
	 * @return a datatype literal for the specified value
	 */
	public org.openrdf.model.Literal createDatatypeLiteral(double doubleValue) {
		return createDatatypeLiteral(String.valueOf(doubleValue), XMLSchema.DOUBLE);
	}

	public org.openrdf.model.Literal createDatatypeLiteral(String literal, String datatype) {
		return createDatatypeLiteral(literal, createURI(datatype));
	}

	public org.openrdf.model.Literal createDatatypeLiteral(String literal, URI datatype) {
		return getValueFactory().createLiteral(literal, datatype);
	}

	public org.openrdf.model.Literal createLanguageTaggedLiteral(String text) {
		return new LiteralImpl(text);
	}

	public org.openrdf.model.Literal createLanguageTaggedLiteral(String text, String tag) {
		return getValueFactory().createLiteral(text, tag);
	}

	public org.openrdf.model.Literal createLiteral(String text) {
		return getValueFactory().createLiteral(text);
	}

	public org.openrdf.model.Literal createLiteral(String literal, URI datatypeURI) {
		return getValueFactory().createLiteral(literal, datatypeURI);
	}

	public Value createNode(String uriOrLiteral) {
		int index = Strings.indexOfUnquoted(uriOrLiteral, "^^");
		if (index > 0) {
			String literal = unquoteTurtleLiteral(uriOrLiteral.substring(0, index));
			String datatype = uriOrLiteral.substring(index + 2);
			return getValueFactory().createLiteral(literal, getValueFactory().createURI(datatype));
		}
		index = Strings.indexOfUnquoted(uriOrLiteral, "@");
		if (index > 0) {
			String literal = unquoteTurtleLiteral(uriOrLiteral.substring(0, index));
			String langugeTag = uriOrLiteral.substring(index + 1);
			return getValueFactory().createLiteral(literal, langugeTag);
		}
		if (uriOrLiteral.startsWith("'") && uriOrLiteral.endsWith("'")) {
			return getValueFactory().createLiteral(unquoteTurtleLiteral(uriOrLiteral));
		}
		if (uriOrLiteral.startsWith("\"") && uriOrLiteral.endsWith("\"")) {
			return getValueFactory().createLiteral(unquoteTurtleLiteral(uriOrLiteral));
		}
		return createResource(uriOrLiteral);
	}

	public Resource createResource(String uri) {
		// create blank node or uri,
		// at the moment we only support uris
		return createURI(uri);
	}

	public Resource createResource(java.net.URI uri) {
		return createResource(uri.toString());
	}

	public static String unquoteTurtleLiteral(String turtle) {
		turtle = turtle.trim();
		int len = turtle.length();
		if (turtle.startsWith("'''") && turtle.endsWith("'''") && len >= 6) {
			return Strings.unquote(turtle.substring(2, len - 2), '\'');
		}
		if (turtle.startsWith("\"\"\"") && turtle.endsWith("\"\"\"") && len >= 6) {
			return Strings.unquote(turtle.substring(2, len - 2), '"');
		}
		if (turtle.startsWith("'") && turtle.endsWith("'")) {
			return Strings.unquote(turtle, '\'');
		}
		return Strings.unquote(turtle);
	}

	/**
	 * Creates a uri with the given relative uri for the local namespace "lns:".
	 *
	 * @param name the relative uri (or simple name) to create a lns-uri for
	 * @return an uri of the local namespace
	 */
	public URI createlocalURI(String name) {
		return createURI(lns, name);
	}

	public Statement createStatement(Resource subject, URI predicate, Value object) {
		return getValueFactory().createStatement(subject, predicate, object);
	}

	public URI createURI(String value) {
		return getValueFactory().createURI(Rdf2GoUtils.expandNamespace(this, value));
	}

	public URI createURI(java.net.URI uri) {
		return createURI(uri.toString());
	}

	private ValueFactory getValueFactory() {
		return semanticCore.getValueFactory();
	}

	public URI createURI(String ns, String value) {
		// in case ns is just the abbreviation
		String fullNs = getNamespaces().get(ns);

		return createURI((fullNs == null ? ns : fullNs) + Strings.encodeURL(value));
	}

	public String getBaseNamespace() {
		return bns;
	}

	public String getLocalNamespace() {
		return this.lns;
	}

	/**
	 * Returns a map of all namespaces mapped by their abbreviation.<br>
	 * <b>Example:</b> rdf -> http://www.w3.org/1999/02/22-rdf-syntax-ns#
	 */
	public Map<String, String> getNamespaces() {
		Map<String, String> namespaces = this.namespaces;
		if (namespaces == null) {
			synchronized (nsMutext) {
				if (this.namespaces == null) {
					this.namespaces = getSemanticCoreNameSpaces();
				}
				namespaces = this.namespaces;
			}
		}
		return namespaces;
	}

	private Map<String, String> getSemanticCoreNameSpaces() {
		Map<String, String> temp = new HashMap<>();
		try {
			try (RepositoryConnection connection = semanticCore.getConnection()) {
				RepositoryResult<Namespace> namespaces = connection.getNamespaces();
				for (Namespace namespace : Iterations.asList(namespaces)) {
					temp.put(namespace.getPrefix(), namespace.getName());
				}
			}
		}
		catch (RepositoryException e) {
			Log.severe("Exception while getting namespaces", e);
		}
		if (!temp.containsKey("")) temp.put("", getLocalNamespace());
		return temp;
	}

	/**
	 * Returns a map of all namespaces mapped by their prefixes as they are used e.g. in Turtle and
	 * SPARQL.<br>
	 * <b>Example:</b> rdf: -> http://www.w3.org/1999/02/22-rdf-syntax-ns#
	 * <p>
	 * Although this map seems trivial, it is helpful for optimization reasons.
	 */
	public Map<String, String> getNamespacePrefixes() {
		Map<String, String> namespacePrefixes = this.namespacePrefixes;
		// check before synchronizing...
		if (namespacePrefixes == null) {
			synchronized (nsPrefixMutex) {
				// inspection is wrong here, could no longer be null due to another thread initializing the prefixes
				//noinspection ConstantConditions
				if (this.namespacePrefixes == null) {
					namespacePrefixes = new HashMap<>();
					Map<String, String> namespaces = getSemanticCoreNameSpaces();
					for (Entry<String, String> entry : namespaces.entrySet()) {
						namespacePrefixes.put(Rdf2GoUtils.toNamespacePrefix(entry.getKey()), entry.getValue());
					}
					this.namespacePrefixes = namespacePrefixes;
				}
				else {
					namespacePrefixes = this.namespacePrefixes;
				}
			}
		}
		return namespacePrefixes;
	}

	/**
	 * @return all {@link Statement}s of the Rdf2GoCore.
	 * @created 15.07.2012
	 */
	public Set<Statement> getStatements() {

		Set<Statement> statements1 = null;
		try {
			RepositoryResult<Statement> statements = semanticCore.getConnection()
					.getStatements(null, null, null, true);
			statements1 = Iterations.asSet(statements);
		}
		catch (RepositoryException e) {
			Log.severe("Exception while getting statements", e);
		}
		return statements1;
	}

	/**
	 * Returns the set of statements that have been created from the given section during the
	 * compile process
	 */
	public Set<Statement> getStatementsFromCache(Section<?> source) {
		return statementCache.getValues(new SectionSource(source));
	}

	public long getSize() {
		return getStatements().size();
	}

	/**
	 * sets the default namespaces
	 */
	private void initDefaultNamespaces() {
		addNamespace("ns", bns);
		addNamespace(LNS_ABBREVIATION, lns);
		addNamespace("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
		addNamespace("owl", "http://www.w3.org/2002/07/owl#");
		addNamespace("rdfs", "http://www.w3.org/2000/01/rdf-schema#");
		addNamespace("xsd", "http://www.w3.org/2001/XMLSchema#");
		addNamespace("fn", "http://www.w3.org/2005/xpath-functions#");
		addNamespace("onto", "http://www.ontotext.com/");
	}

	private void logStatements(Set<Statement> statements, long start, String caption) {
		// check if we have something to log
		if (statements.isEmpty()) return;

		// sort statements at this point using tree map
		StringBuilder buffer = new StringBuilder();
		for (Statement statement : statements) {
			buffer.append("* ").append(verbalizeStatement(statement)).append("\n");
		}
		buffer.append("Done after ").append(System.currentTimeMillis() - start).append("ms");
		Log.fine(caption + ":\n" + buffer);
	}

	public void readFrom(InputStream in, RDFFormat syntax) throws RDFParseException, RepositoryException, IOException {
		semanticCore.addData(in, syntax);
		namespaces = null;
		namespacePrefixes = null;
	}

	public void readFrom(File in) throws RDFParseException, RepositoryException, IOException {
		semanticCore.addData(in);
		namespaces = null;
		namespacePrefixes = null;
	}

	public void removeAllCachedStatements() {
		// get all statements of this wiki and remove them from the model
		removeCache.addAll(statementCache.valueSet());
		statementCache.clear();
	}

	public void removeNamespace(String abbreviation) throws RepositoryException {
		semanticCore.getConnection().removeNamespace(abbreviation);
		namespaces = null;
		namespacePrefixes = null;
	}

	/**
	 * Removes the specified statements if they have been added to this core and if they are added
	 * without specifying a specific source like a compiler or a section.
	 *
	 * @param statements the statements to be removed
	 * @created 13.06.2012
	 */
	public void removeStatements(Collection<Statement> statements) {
		synchronized (statementLock) {
			removeStatements(null, statements);
		}
	}

	/**
	 * Removes all statements cached for the given {@link StatementSource}.
	 *
	 * @param source the {@link StatementSource} for which the statements should be removed
	 */
	public void removeStatements(StatementSource source) {
		Collection<Statement> statements = statementCache.getValues(source);
		synchronized (statementLock) {
			removeStatements(source, new ArrayList<>(statements));
		}
	}

	private void removeStatements(StatementSource source, Collection<Statement> statements) {
		for (Statement statement : statements) {
			statementCache.remove(source, statement);
			if (!statementCache.containsValue(statement)) {
				removeCache.add(statement);
			}
		}
	}

	/**
	 * Removes all {@link Statement}s that were added and cached for the given
	 * {@link Section}.
	 * <p>
	 * <b>Attention</b>: This method only removes {@link Statement}s that were
	 * added (and cached) in connection with a {@link Section} using methods
	 * like {@link Rdf2GoCore#addStatements(Section, Collection)}.
	 *
	 * @param section the {@link Section} for which the {@link Statement}s should be removed
	 * @created 06.12.2010
	 */
	public void removeStatements(Section<? extends Type> section) {
		removeStatements(new SectionSource(section));
	}

	/**
	 * Returns the articles the statement has been created on. The method may
	 * return an empty list if the statement has not been added by a markup and
	 * cannot be associated to an article.
	 *
	 * @param statement the statement to get the articles for
	 * @return the articles that defines that statement
	 * @created 13.12.2013
	 */
	public Set<Article> getSourceArticles(Statement statement) {
		Collection<StatementSource> list = statementCache.getKeys(statement);
		if (list.isEmpty()) return Collections.emptySet();
		Set<Article> result = new HashSet<>();
		for (StatementSource source : list) {
			result.add(source.getArticle());
		}
		return Collections.unmodifiableSet(result);
	}

	/**
	 * Returns the article the statement has been created on. The method may
	 * return null if the statement has not been added by a markup and cannot be
	 * associated to an article. If there are multiple articles defining that
	 * statement one of the articles are returned.
	 *
	 * @param statement the statement to get the article for
	 * @return the article that defines that statement
	 * @created 13.12.2013
	 */
	public Article getSourceArticle(Statement statement) {
		Collection<StatementSource> list = statementCache.getKeys(statement);
		if (list.isEmpty()) return null;
		return list.iterator().next().getArticle();
	}

	/**
	 * Clears any cached result for the given sparql query. This way, if the query is executed
	 * again, it has to be calculated anew.
	 *
	 * @param query the query for which the cached result should be removed
	 * @return true if a result was cached, false if not
	 */
	public boolean clearSparqlResult(String query) {
		String completeQuery = prependPrefixesToQuery(query);
		synchronized (resultCache) {
			SparqlTask removed = resultCache.remove(completeQuery);
			if (removed != null) {
				resultCacheSize -= removed.getSize();
				return true;
			}
			return false;
		}
	}

	public TupleQueryResult sparqlSelect(SparqlQuery query) {
		return sparqlSelect(query.toSparql(this));
	}

	/**
	 * Performs a cached SPARQL select query with the default timeout of 5 seconds.
	 *
	 * @param query the SPARQL query to perform
	 * @return the result of the query
	 */
	public CachedTupleQueryResult sparqlSelect(String query) {
		return (CachedTupleQueryResult) sparqlSelect(query, true, DEFAULT_TIMEOUT);
	}

	/**
	 * Performs a cached SPARQL select query with the specified timeout.
	 *
	 * @param query the SPARQL query to perform
	 * @param timeout the time to be used for timout
	 * @return the result of the query
	 */
	public CachedTupleQueryResult sparqlSelect(String query, long timeout) {
		return (CachedTupleQueryResult) sparqlSelect(query, true, timeout);
	}

	/**
	 * Performs a cached SPARQL ask query with the default timeout of 5 seconds.
	 *
	 * @param query the SPARQL query to perform
	 * @return the result of the query
	 */
	public boolean sparqlAsk(String query) {
		return sparqlAsk(query, true, DEFAULT_TIMEOUT);
	}

//	/**
//	 * Performs a cached SPARQL construct query with the default timeout of 5 seconds.
//	 *
//	 * @param query the SPARQL query to perform
//	 * @return the result of the query
//	 */
//	public ClosableIterable<Statement> sparqlConstruct(String query) {
//		return sparqlConstruct(query, true, DEFAULT_TIMEOUT);
//	}

	/**
	 * Performs a SPARQL select query with the given parameters. Be aware that, in case of an
	 * uncached query, the timeout only effects the process of creating the iterator. Retrieving
	 * elements from the iterator might again take a long time not covered by the timeout.
	 *
	 * @param query the SPARQL query to perform
	 * @param cached sets whether the SPARQL query is to be cached or not
	 * @param timeOutMillis the timeout of the query
	 * @return the result of the query
	 */
	public TupleQueryResult sparqlSelect(String query, boolean cached, long timeOutMillis) {
		return (TupleQueryResult) sparql(query, cached, timeOutMillis, SparqlType.SELECT);
	}

	/**
	 * Performs a SPARQL ask query with the given parameters. Be aware that, in case of an uncached
	 * query, the timeout only effects the process of creating the iterator. Retrieving elements
	 * from the iterator might again take a long time not covered by the timeout.
	 *
	 * @param query the SPARQL query to perform
	 * @param cached sets whether the SPARQL query is to be cached or not
	 * @param timeOutMillis the timeout of the query
	 * @return the result of the query
	 */
	public boolean sparqlAsk(String query, boolean cached, long timeOutMillis) {
		return (Boolean) sparql(query, cached, timeOutMillis, SparqlType.ASK);
	}

//	/**
//	 * Performs a SPARQL construct query with the given parameters. Be aware that, in case of an uncached query, the
//	 * timeout only effects the process of creating the iterator. Retrieving elements from the iterator might again
//	 * take a long time not covered by the timeout.
//	 *
//	 * @param query         the SPARQL query to perform
//	 * @param cached        sets whether the SPARQL query is to be cached or not
//	 * @param timeOutMillis the timeout of the query
//	 * @return the result of the query
//	 */
//	@SuppressWarnings("unchecked")
//	public ClosableIterable<Statement> sparqlConstruct(String query, boolean cached, long timeOutMillis) {
//		return (ClosableIterable<Statement>) sparql(query, cached, timeOutMillis, SparqlType.CONSTRUCT);
//	}

	private Object sparql(String query, boolean cached, long timeOutMillis, SparqlType type) {
		String completeQuery = prependPrefixesToQuery(query);

		// if the compile thread is calling here, we continue without all the timeout, cache, and lock
		// they are not needed in that context and do even cause problems and overhead
		if (CompilerManager.isCompileThread()) {
			try {
				return new SparqlCallable(completeQuery, type, 0, true).call();
			}
			catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		// normal query, e.g.  from a renderer... do all the cache and timeout stuff
		SparqlTask sparqlTask;
		if (cached) {
			synchronized (resultCache) {
				sparqlTask = resultCache.get(completeQuery);
				if (sparqlTask == null
						|| (sparqlTask.isCancelled() && sparqlTask.callable.timeOutMillis != timeOutMillis)) {
					sparqlTask = new SparqlTask(new SparqlCallable(completeQuery, type, timeOutMillis, true));
					SparqlTask previous = resultCache.put(completeQuery, sparqlTask);
					if (previous != null) {
						resultCacheSize -= previous.getSize();
					}
					sparqlThreadPool.execute(sparqlTask);
				}
			}
		}
		else {
			sparqlTask = new SparqlTask(new SparqlCallable(completeQuery, type, timeOutMillis, false));
			sparqlThreadPool.execute(sparqlTask);
		}
		String timeOutMessage = "SPARQL query timed out after " + Strings.getDurationVerbalization(timeOutMillis, true) + ".";
		try {
			// We set a generous time out to be sure to not be blocked indefinitely, even if stuff goes wrong with
			// stopping the thread cold. Using maxEvaluation timeout and the SparqlTaskReaper, we should return
			// way sooner in normal cases.
			return sparqlTask.get(timeOutMillis * 2, TimeUnit.MILLISECONDS);
		}
		catch (CancellationException | InterruptedException | TimeoutException e) {
//			Log.warning("SPARQL query failed due to an exception", e);
			throw new RuntimeException(timeOutMessage, e);
		}
		catch (Exception e) {
			Throwable cause = e.getCause();
			if (cause == null) cause = e;
			if (cause instanceof ThreadDeath || cause instanceof QueryInterruptedException) {
				throw new RuntimeException(timeOutMessage, cause);
			}
			else if (cause instanceof RuntimeException) {
				if (!(cause.getCause() instanceof QueryInterruptedException)) {
					Log.warning("SPARQL query failed due to an exception", cause);
				}
				throw (RuntimeException) cause;
			}
			else {
				Log.warning("SPARQL query failed due to an exception", cause);
				throw new RuntimeException(cause);
			}
		}
	}

	/**
	 * Future for SPARQL queries with some addition control to stop it and get info about state.
	 */
	private class SparqlTask extends FutureTask<Object> {

		private long startTime = Long.MIN_VALUE;
		private final SparqlCallable callable;
		private Thread thread = null;
		private int size = 1;

		SparqlTask(SparqlCallable callable) {
			super(callable);
			this.callable = callable;
		}

		long getTimeOutMillis() {
			return callable.timeOutMillis;
		}

		public synchronized void setSize(int size) {
			this.size = size;
		}

		public synchronized int getSize() {
			return size;
		}

		synchronized long getRunDuration() {
			return hasStarted() ? System.currentTimeMillis() - startTime : 0;
		}

		synchronized boolean hasStarted() {
			return startTime != Long.MIN_VALUE;
		}

		synchronized boolean isAlive() {
			return !hasStarted() || (thread != null && thread.isAlive());
		}

		@Override
		public boolean cancel(boolean mayInterruptIfRunning) {
			boolean canceled = super.cancel(mayInterruptIfRunning);
			if (canceled) {
				Log.warning("SPARQL query was canceled after "
						+ Strings.getDurationVerbalization(getRunDuration())
						+ ": " + getReadableQuery(callable.query, callable.type) + "...");
			}
			return canceled;
		}

		public synchronized void stop() {
			if (thread != null) {
				//noinspection deprecation
				this.thread.stop();
				LockSupport.unpark(this.thread);
				this.thread = null;
				Log.warning("SPARQL query was stopped after "
						+ Strings.getDurationVerbalization(getRunDuration())
						+ ": " + getReadableQuery(callable.query, callable.type) + "...");
			}
		}

		@Override
		public Object get() throws InterruptedException, ExecutionException {
			return super.get();
		}

		@Override
		public void run() {
			synchronized (this) {
				this.thread = Thread.currentThread();
				startTime = System.currentTimeMillis();
			}
			try {
				sparqlReaperPool.execute(new SparqlTaskReaper(this));
				super.run();
			}
			finally {
				synchronized (this) {
					thread = null;
				}
			}
			if (callable.cached) {
				handleCacheSize(this);
			}
		}

		@Override
		protected void set(Object o) {
			super.set(o);
			if (callable.cached) {
				setSize(getResultSize(o));
			}
			if (getRunDuration() > 1000) {
				Log.info("SPARQL query finished after "
						+ Strings.getDurationVerbalization(getRunDuration())
						+ ": " + getReadableQuery(callable.query, callable.type) + "...");
			}
		}
	}

	/**
	 * Observes the SPARQL task end cancels/stops it, if it takes to long. We normally use the
	 * build-in sesame timeout to terminate queries that are too slow. In some cases though, these
	 * timeouts do not work as desired (probably not well implemented by underlying repos) so we use
	 * this kill switch to make sure the query is terminated after 150% of the intended timeout.
	 */
	private static class SparqlTaskReaper implements Runnable {

		private final SparqlTask task;

		SparqlTaskReaper(SparqlTask task) {
			this.task = task;
		}

		@SuppressWarnings("ConstantConditions")
		@Override
		public void run() {
			try {
				task.get((long) (task.getTimeOutMillis() * 1.5), TimeUnit.MILLISECONDS);
			}
			catch (TimeoutException e) {

				// we cancel the task
				task.cancel(true);

				sleep(task.getTimeOutMillis());

				// if it has not died after the sleep, we kill it
				// (not all repositories will react to cancel)
				if (task.isAlive()) {
					task.stop();
				}
			}
			catch (Exception ignore) {
				// nothing to do
			}
		}

		private void sleep(long timeout) {
			try {
				Thread.sleep(Math.max(timeout, 1000));
			}
			catch (InterruptedException ie) {
				Log.warning(Thread.currentThread().getName() + " was interrupted", ie);
			}
		}
	}

	/**
	 * Does the work and retrieves the SPARQL result.
	 */
	private class SparqlCallable implements Callable<Object> {

		private final String query;
		private final SparqlType type;
		private final boolean cached;
		private final long timeOutMillis;

		private SparqlCallable(String query, SparqlType type, long timeOutMillis, boolean cached) {
			this.query = query;
			this.type = type;
			this.cached = cached;
			// timeouts shorter than 1 seconds are not possible with sesame
			this.timeOutMillis = Math.max(1000, timeOutMillis);
		}

		@Override
		public Object call() throws RepositoryException, MalformedQueryException, QueryEvaluationException {
			Object result;
			if (type == SparqlType.CONSTRUCT) {
				result = null; // TODO?
			}
			else if (type == SparqlType.SELECT) {

				int timeOutSeconds = (int) (timeOutMillis / 1000);
				try (RepositoryConnection connection = semanticCore.getConnection()) {
					TupleQuery tupleQuery = connection.prepareTupleQuery(QueryLanguage.SPARQL, this.query);
					tupleQuery.setMaxExecutionTime(timeOutSeconds);
					long start = System.currentTimeMillis();
					TupleQueryResult queryResult = tupleQuery.evaluate();
					if (cached) {
						long evaluationTime = System.currentTimeMillis() - start;
						if (evaluationTime > 1000) {
							Log.info("SPARQL query evaluation finished after "
									+ Strings.getDurationVerbalization(evaluationTime)
									+ ", retrieving results...: " + getReadableQuery(query, type) + "...");
						}
						result = queryResult.cachedAndClosed();
					}
					else {
						result = queryResult;
					}
				}
//				catch (RepositoryException | MalformedQueryException | QueryEvaluationException e) {
//					throw new RuntimeException(e);
//				}

			}
			else {
				try {
					result = semanticCore.ask(query);
				}
				catch (RepositoryException | MalformedQueryException | QueryEvaluationException e) {
					throw new RuntimeException(e);
				}
			}
			if (Thread.currentThread().isInterrupted()) {
				// not need to waste cache size (e.g. in case of half done results that were aborted)
				result = null;
			}
			return result;
		}
	}

	private String getReadableQuery(String query, SparqlType type) {
		query = query.replace("\n", " ").replaceAll("\t|\\s\\s+", " ");
		int start = -1;
		if (type == SparqlType.ASK) {
			start = query.toLowerCase().indexOf("ask");
		}
		else if (type == SparqlType.SELECT) {
			start = query.toLowerCase().indexOf("select");
		}
		else if (type == SparqlType.CONSTRUCT) {
			start = query.toLowerCase().indexOf("construct");
		}
		if (start == -1) start = 0;
		final int endIndex = query.length() - start > 75 ? start + 75 : query.length();
		return query.substring(start, endIndex) + "...";
	}

	public String prependPrefixesToQuery(String query) {
		String completeQuery;
		if (query.startsWith(Rdf2GoUtils.getSparqlNamespaceShorts(this))) {
			completeQuery = query;
		}
		else {
			completeQuery = Rdf2GoUtils.getSparqlNamespaceShorts(this) + query;
		}
		return completeQuery;
	}

	private void handleCacheSize(SparqlTask task) {
		resultCacheSize += task.getSize();
		if (resultCacheSize > DEFAULT_MAX_CACHE_SIZE) {
			synchronized (resultCache) {
				Iterator<Entry<String, SparqlTask>> iterator = resultCache.entrySet().iterator();
				while (iterator.hasNext() && resultCacheSize > DEFAULT_MAX_CACHE_SIZE) {
					Entry<String, SparqlTask> next = iterator.next();
					iterator.remove();
					try {
						resultCacheSize -= next.getValue().getSize();
					}
					catch (Exception e) {
						// nothing to do, cache size wasn't increase either
					}
				}
			}
		}
	}

	private int getResultSize(Object result) {
		if (result instanceof TupleQueryResult) {
			TupleQueryResult cacheResult = (TupleQueryResult) result;
			try {
				return cacheResult.getBindingNames().size() * cacheResult.getBindingSets().size();
			}
			catch (QueryEvaluationException e) {
				return 1;
			}
		}
		else {
			return 1;
		}
	}

	public Iterator<BindingSet> sparqlSelectIt(String query) {
		return sparqlSelect(query).getBindingSets().iterator();
	}

	public SPARQLEndpoint getSparqlEndpoint() throws RepositoryException {
		return new SesameEndpoint(semanticCore.getConnection());
	}

	private String verbalizeStatement(Statement statement) {
		String statementVerbalization = Rdf2GoUtils.reduceNamespace(this, statement.toString());
		try {
			statementVerbalization = URLDecoder.decode(statementVerbalization, "UTF-8");
		}
		catch (Exception e) {
			// may happen, just ignore...
		}
		return statementVerbalization;
	}

	/**
	 * Writes the current repository model to the given writer in RDF/XML
	 * format.
	 *
	 * @param out the target to write the model to
	 * @created 03.02.2012
	 */
	public void writeModel(Writer out) throws IOException {
		writeModel(out, RDFFormat.RDFXML);
	}

	/**
	 * Writes the current repository model to the given writer in the specified
	 * syntax.
	 *
	 * @param out the target to write the model to
	 * @param syntax the syntax of the target file
	 * @created 28.07.2014
	 */
	public void writeModel(Writer out, RDFFormat syntax) throws IOException {
		try {
			semanticCore.export(out, syntax);
		}
		catch (RepositoryException | RDFHandlerException e) {
			throw new IOException(e);
		}
	}

	/**
	 * Writes the current repository model to the given writer in the specified
	 * syntax.
	 *
	 * @param out the target to write the model to
	 * @param syntax the syntax of the target file
	 * @created 28.07.2014
	 */
	public void writeModel(OutputStream out, RDFFormat syntax) throws IOException {
		try {
			semanticCore.export(out, syntax);
		}
		catch (RepositoryException | RDFHandlerException e) {
			throw new IOException(e);
		}
	}

	/**
	 * Returns true if this instance is empty. An instance is empty, if the
	 * method commit hasn't been called yet.
	 *
	 * @return true if instance is empty, else false.
	 * @created 19.04.2013
	 */
	public boolean isEmpty() {
		return statementCache.isEmpty();
	}

	/**
	 * Destroys this Rdf2GoCore and its underlying model.
	 */
	public void destroy() {
		shutDownThreadPool.execute(() -> {
			Stopwatch stopwatch = new Stopwatch();
			EventManager.getInstance().fireEvent(new Rdf2GoCoreDestroyEvent(this));
			this.semanticCore.shutdown();
			Log.info("SemanticCore shutdown in " + stopwatch.getDisplay());
		});
	}

	public RepositoryConfig getRuleSet() {
		return ruleSet;
	}
}
