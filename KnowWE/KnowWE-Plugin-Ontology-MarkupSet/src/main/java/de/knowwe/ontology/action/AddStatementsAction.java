/*
 * Copyright (C) 2013 denkbares GmbH, Germany
 */
package de.knowwe.ontology.action;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.ontoware.rdf2go.model.Statement;
import org.ontoware.rdf2go.model.impl.StatementImpl;

import de.d3web.strings.Strings;
import de.knowwe.core.ArticleManager;
import de.knowwe.core.Attributes;
import de.knowwe.core.Environment;
import de.knowwe.core.action.AbstractAction;
import de.knowwe.core.action.UserActionContext;
import de.knowwe.core.kdom.Article;
import de.knowwe.ontology.kdom.OntologyUtils;
import de.knowwe.rdf2go.Rdf2GoCore;
import de.knowwe.tools.Tool;

/**
 * Action to add a set of statements to a specified wiki page. The action
 * requires its arguments as a json expression. The arguments consists of a page
 * name where the statements shall be inserted and an array of statements. Each
 * statement is a javascript object with a 'subject', 'predicate' and 'object'
 * property. Additionally the following optional properties are available:
 * <ul>
 * <li><b>compact</b>:<br>
 * A boolean to determine if the created markup be kept compact (if
 * <code>true</code>) or more structured using line breaks and intends (if
 * <code>false</code>). Default value is false.
 * </ul>
 * 
 * Here some example json:
 * 
 * <pre>
 * {
 *   article: "&lt;article-title&gt;",
 *   statements: [
 *     { subject: "&lt;subject1&gt;", predicate: "&lt;predicate1&gt;", object: "&lt;object1&gt;" },
 *     { subject: "&lt;subject2&gt;", predicate: "&lt;predicate2&gt;", object: "&lt;object2&gt;" },
 *     ...
 *   ],
 *   compactMode: &lt;boolean&gt;
 * }
 * </pre>
 * 
 * @author Volker Belli (denkbares GmbH)
 * @created 25.11.2013
 */
public class AddStatementsAction extends AbstractAction {

	public static final String PARAM_DATA = "data";

	@Override
	public void execute(UserActionContext context) throws IOException {

		String web = context.getParameter(Attributes.WEB);
		String jsonText = context.getParameter(PARAM_DATA);
		if (Strings.isBlank(jsonText)) {
			context.sendError(HttpServletResponse.SC_BAD_REQUEST, "no data to be inserted");
			return;
		}

		Rdf2GoCore core = Rdf2GoCore.getInstance();

		try {
			JSONObject json = new JSONObject(jsonText);
			String articleName = (String) json.get("article");
			boolean compactMode = json.optBoolean("compact");
			Statement[] statementsToAdd = toStatements(core, json.getJSONArray("add"));
			Statement[] statementsToRemove = toStatements(core, json.getJSONArray("remove"));

			ArticleManager manager = Environment.getInstance().getArticleManager(web);
			Article article = manager.getArticle(articleName);
			String newText = OntologyUtils.modifyTurtle(
					article, compactMode, statementsToAdd, statementsToRemove);

			context.setContentType("text/plain; charset=UTF-8");
			context.getWriter().append(newText);
		}
		catch (JSONException e) {
			context.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
		}
	}

	private static Statement[] toStatements(Rdf2GoCore core, JSONArray array) throws JSONException {
		Statement[] statements = new Statement[array.length()];
		for (int i = 0; i < array.length(); i++) {
			JSONObject stmt = array.getJSONObject(i);
			statements[i] = new StatementImpl(
					null,
					core.createResource(stmt.getString("subject")),
					core.createURI(stmt.getString("predicate")),
					core.createNode(stmt.getString("object")));
		}
		return statements;
	}

	/**
	 * Creates a JavaScript action that can be used as a tool action which will
	 * add the specified statements to the specified article if the action will
	 * be executed.
	 * 
	 * @created 25.11.2013
	 * @param source the article to place the JavaScript action
	 * @param targetArticle the article to add the statements to
	 * @param statements the statements to be added
	 * @return the JavaScript action to be included in a {@link Tool}
	 */
	public static String getJSAction(Article source, String targetArticle, Statement... statements) {
		return getJSAction(source, targetArticle, true, statements);
	}

	/**
	 * Creates a JavaScript action that can be used as a tool action which will
	 * add the specified statements to the specified article if the action will
	 * be executed.
	 * 
	 * @created 25.11.2013
	 * @param source the article to place the JavaScript action
	 * @param targetArticle the article to add the statements to
	 * @param compactMode Shall the created markup be kept compact or more
	 *        structured using line breaks and intends.
	 * @param statement the statements to be added
	 * @return the JavaScript action to be included in a {@link Tool}
	 */
	public static String getJSAction(Article source, String targetArticle, boolean compactMode, Statement... statement) {
		return getJSAction(source, targetArticle, compactMode,
				Arrays.asList(statement), Collections.<Statement> emptyList());
	}

	/**
	 * Creates a JavaScript action that can be used as a tool action which will
	 * add the specified statements to the specified article if the action will
	 * be executed.
	 * 
	 * @created 25.11.2013
	 * @param source the article to place the JavaScript action
	 * @param targetArticle the article to add the statements to
	 * @param compactMode Shall the created markup be kept compact or more
	 *        structured using line breaks and intends.
	 * @param statementToAdd the statements to be added
	 * @param statementToRemove the statements to be removed
	 * @return the JavaScript action to be included in a {@link Tool}
	 */
	public static String getJSAction(Article source, String targetArticle, boolean compactMode, List<Statement> statementToAdd, List<Statement> statementToRemove) {
		try {
			JSONObject json = new JSONObject();
			json.accumulate("article", targetArticle);
			for (Statement statement : statementToAdd) {
				json.append("add", toJSON(statement));
			}
			for (Statement statement : statementToRemove) {
				json.append("remove", toJSON(statement));
			}
			json.accumulate("compact", compactMode);

			return "window.location.href = 'action/AddStatementsAction?"
					+ Attributes.WEB + "=" + source.getWeb() + "&"
					+ PARAM_DATA + "=" + Strings.encodeURL(json.toString())
					+ "'";
		}
		catch (JSONException e) {
			Logger.getLogger(AddStatementsAction.class.getName()).log(Level.WARNING,
					"cannot create js action for statement insert", e);
			return null;
		}
	}

	private static JSONObject toJSON(Statement statement) throws JSONException {
		JSONObject stmt = new JSONObject();
		stmt.accumulate("subject", statement.getSubject().toString());
		stmt.accumulate("predicate", statement.getPredicate().toString());
		stmt.accumulate("object", statement.getObject().toString());
		return stmt;
	}
}