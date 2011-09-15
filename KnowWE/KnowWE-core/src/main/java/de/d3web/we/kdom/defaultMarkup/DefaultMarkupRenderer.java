/*
 * Copyright (C) 2010 Chair of Artificial Intelligence and Applied Informatics
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
package de.d3web.we.kdom.defaultMarkup;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import de.d3web.we.core.packaging.PackageRenderUtils;
import de.d3web.we.kdom.KnowWEArticle;
import de.d3web.we.kdom.Section;
import de.d3web.we.kdom.basic.PlainText;
import de.d3web.we.kdom.rendering.KnowWEDomRenderer;
import de.d3web.we.kdom.report.KDOMError;
import de.d3web.we.kdom.report.KDOMNotice;
import de.d3web.we.kdom.report.KDOMReportMessage;
import de.d3web.we.kdom.report.KDOMWarning;
import de.d3web.we.tools.Tool;
import de.d3web.we.tools.ToolUtils;
import de.d3web.we.user.UserContext;
import de.d3web.we.utils.KnowWEUtils;

public class DefaultMarkupRenderer<T extends DefaultMarkupType> extends KnowWEDomRenderer<T> {

	private final String iconPath;

	private final boolean renderCompileWarnings;

	public DefaultMarkupRenderer() {
		this(null, true);
	}

	public DefaultMarkupRenderer(boolean renderCompileWarnings) {
		this(null, renderCompileWarnings);
	}

	public DefaultMarkupRenderer(String iconPath) {
		this(iconPath, true);
	}

	public DefaultMarkupRenderer(String iconPath, boolean renderCompileWarnings) {
		this.iconPath = iconPath;
		this.renderCompileWarnings = renderCompileWarnings;
	}

	public static String renderToolbar(Tool[] tools) {
		StringBuilder toolbarHtml = new StringBuilder(" | <div class='markupTools'> ");
		for (Tool tool : tools) {
			toolbarHtml.append(
					" <a href=\"javascript:" + tool.getJSAction() + ";undefined;\">" +
							"<img " +
							"title=\"" + tool.getDescription() + "\" " +
							"src=\"" + tool.getIconPath() + "\"></img>" +
							"</a>");

		}
		toolbarHtml.append("</div>");
		return toolbarHtml.toString();
	}

	public static String renderMenu(Map<String, Map<String, List<Tool>>> tools, String id) {
		StringBuffer menuHtml = new StringBuffer("<div id='menu_" + id + "' class='markupMenu'>");

		List<String> levelOneCategories = new ArrayList<String>(tools.keySet());
		Collections.sort(levelOneCategories);

		for (String category : levelOneCategories) {
			Map<String, List<Tool>> levelTwoTools = tools.get(category);

			List<String> levelTwoCategories = new ArrayList<String>(levelTwoTools.keySet());
			Collections.sort(levelTwoCategories);

			for (String subcategory : levelTwoCategories) {
				for (Tool t : tools.get(category).get(subcategory)) {
					menuHtml.append(renderToolAsMenuItem(t));
				}
			}

			if (!category.equals(levelOneCategories.get(levelOneCategories.size() - 1))) {
				menuHtml.append("<span class=\"markupMenuDivider\">&nbsp;</span>");
			}
		}

		return menuHtml.append("</div>").toString();
	}

	private static String renderToolAsMenuItem(Tool tool) {
		String icon = tool.getIconPath();
		String jsAction = tool.getJSAction();
		boolean hasIcon = icon != null && !icon.trim().isEmpty();

		return "<div class=\"" + tool.getClass().getSimpleName() + "\" >"
				+ "<div class=\"markupMenuItem\">"
				+ "<"
				+ (jsAction == null ? "span" : "a")
				+ " class=\"markupMenuItem\""
				+ (jsAction != null
						? " href=\"javascript:" + tool.getJSAction() + ";undefined;\""
						: "") +
				" title=\"" + tool.getDescription() + "\">" +
				(hasIcon ? ("<img src=\"" + icon + "\"></img>") : "") +
				" <span>" + tool.getTitle() + "</span>" +
				"</" + (jsAction == null ? "span" : "a") + ">" +
				"</div></div>";
	}

	public static String renderMenuAnimation(String id) {
		return "<script>\n" +
				"var makeMenuFx = function() {\n" +
				"var a=$('header_" + id + "'),c=$('menu_" + id + "');\n" +
				"if(!a||!c){}\n" +
				"var b=c.effect(\"opacity\",{wait:false}).set(0);\n" +
				"a.adopt(c).set({href:\"#\",events:{" +
				"mouseout:function(){b.start(0);}," +
				"click:function(){b.start(0.9);}," +
				"mouseover:function(){b.start(0.9);}}});" +
				"};" +
				"makeMenuFx();" +
				"</script>\n";
	}

	public static String renderHeaderAnimation(String id) {
		return "<script>\n" +
				"var makeHeaderFx = function() {\n" +
				"var a=$('header_" + id + "');\n" +
				"var b=a.effect(\"opacity\",{wait:false,duration:200}).set(0.3);\n" +
				"var d=a.effect(\"max-width\",{wait:false}).set(35);\n" +
				"a.set({href:\"#\",events:{" +
				"mouseout:function(){b.start(0.3);d.start(35);a.style['z-index']=1000;}," +
				"click:function(){b.start(1);d.start(250);a.style['z-index']=1500;}," +
				"mouseover:function(){b.start(1);d.start(250);a.style['z-index']=1500;}}});" +
				"};" +
				"makeHeaderFx();" +
				"</script>\n";
	}

	public static String renderHeader(String iconPath, String title) {
		String result = "";
		// render icon
		if (iconPath != null) {
			result = "<img class='markupIcon' src='" + iconPath + "'></img> ";
		}

		// render heading
		result += "<span>" + title + "</span>";
		return KnowWEUtils.maskHTML(result);
	}

	public static void renderDefaultMarkupStyled(String header, String content, String sectionID, Tool[] tools, StringBuilder string) {
		renderDefaultMarkupStyled(header, content, sectionID, null, tools, string);
	}

	public static void renderDefaultMarkupStyled(String header, String content, String sectionID, String cssClassName, Tool[] tools, StringBuilder string) {

		String cssClass = "defaultMarkupFrame";
		if (cssClassName != null) cssClass += " " + cssClassName;
		string.append(KnowWEUtils.maskHTML("<div id=\"" + sectionID
				+ "\" class='" + cssClass + "'>\n"));

		boolean hasTools = tools != null && tools.length > 0;
		boolean hasMenu = hasTools;
		boolean hasToolbar = false;

		String toolbarHtml = "";
		if (hasToolbar) {
			toolbarHtml = renderToolbar(tools);
		}

		string.append(KnowWEUtils.maskHTML(
				"<div id='header_" + sectionID + "' " +
						"class='markupHeaderFrame'>"));
		string.append(KnowWEUtils.maskHTML("<div class='markupHeader'>"));
		string.append(header);
		if (hasMenu) {
			string.append(KnowWEUtils.maskHTML("<span class='markupMenuIndicator' />"));
		}
		string.append(KnowWEUtils.maskHTML(toolbarHtml));
		// string.append("\n");
		string.append(KnowWEUtils.maskHTML("</div>")); // class=markupHeader

		if (hasMenu) {
			String menuHtml = renderMenu(ToolUtils.groupTools(tools), sectionID);
			string.append(KnowWEUtils.maskHTML(menuHtml));
		}

		string.append(KnowWEUtils.maskHTML("</div>")); // class=markupHeaderFrame

		// render pre-formatted box
		string.append(KnowWEUtils.maskHTML("<div id=\"box_" + sectionID
				+ "\" class='defaultMarkup'>"));
		string.append(KnowWEUtils.maskHTML("<div id=\"content_" + sectionID
				+ "\" class='markupText'>"));

		// render content
		// Returns are replaced to avoid JSPWiki to render <p> </p>, do not edit
		// the following line!
		string.append(content.replaceAll("(\r?\n){2}",
				KnowWEUtils.maskHTML("<span>\n</span><span>\n</span>")));

		// and close the box(es)
		string.append(KnowWEUtils.maskHTML("</div>")); // class=markupText
		string.append(KnowWEUtils.maskHTML("</div>")); // class=defaultMarkup
		string.append(KnowWEUtils.maskHTML("</div>"));

		if (hasMenu) {
			string.append(KnowWEUtils.maskHTML(renderMenuAnimation(sectionID)));
		}
		string.append(KnowWEUtils.maskHTML(renderHeaderAnimation(sectionID)));
	}

	@Override
	public void render(KnowWEArticle article, Section<T> section, UserContext user, StringBuilder buffer) {
		String id = section.getID();
		Tool[] tools = ToolUtils.getTools(article, section, user);

		// render Header
		StringBuilder header = new StringBuilder();
		renderHeader(article, section, user, header);

		// create content
		StringBuilder content = new StringBuilder();
		if (this.renderCompileWarnings) {
			article = PackageRenderUtils.checkArticlesCompiling(article, section, content);
		}

		// add an anchor to enable direct link to the section
		String anchorName = KnowWEUtils.getAnchor(section);
		content.append(KnowWEUtils.maskHTML("<a name='" + anchorName + "'></a>"));

		// render messages and content
		renderMessages(article, section, content);
		renderContents(article, section, user, content);

		String cssClassName = "type_" + section.get().getName();

		renderDefaultMarkupStyled(
				header.toString(), content.toString(),
				id, cssClassName, tools, buffer);
	}

	protected void renderHeader(KnowWEArticle article, Section<T> section, UserContext user, StringBuilder string) {
		String icon = getHeaderIcon(article, section, user);
		String title = getHeaderName(article, section, user);
		string.append(renderHeader(icon, title));
	}

	protected String getHeaderName(KnowWEArticle article, Section<T> section, UserContext user) {
		return section.get().getName();
	}

	protected String getHeaderIcon(KnowWEArticle article, Section<T> section, UserContext user) {
		return this.iconPath;
	}

	protected void renderContents(KnowWEArticle article, Section<T> section, UserContext user, StringBuilder string) {
		List<Section<?>> subsecs = section.getChildren();
		Section<?> first = subsecs.get(0);
		Section<?> last = subsecs.get(subsecs.size() - 1);
		for (Section<?> subsec : subsecs) {
			if (subsec == first && subsec.get() instanceof PlainText) {
				continue;
			}
			if (subsec == last && subsec.get() instanceof PlainText) {
				continue;
			}
			subsec.get().getRenderer().render(article, subsec, user, string);
		}
	}

	protected void renderMessages(KnowWEArticle article, Section<? extends DefaultMarkupType> section, UserContext user, StringBuilder string) {
		renderMessages(article, section, string);
	}

	public static void renderMessages(KnowWEArticle article, Section<? extends DefaultMarkupType> section, StringBuilder string) {
		renderKDOMReportMessageBlock(KnowWEUtils.getMessagesFromSubtree(article, section,
				KDOMError.class), string);
		renderKDOMReportMessageBlock(KnowWEUtils.getMessagesFromSubtree(article, section,
				KDOMWarning.class), string);
	}

	private static void renderKDOMReportMessageBlock(Collection<? extends KDOMReportMessage> messages, StringBuilder string) {
		if (messages == null) return;
		if (messages.size() == 0) return;

		Class<? extends KDOMReportMessage> type = messages.iterator().next().getClass();
		String className = "";
		if (KDOMNotice.class.isAssignableFrom(type)) {
			className = "information";
		}
		else if (KDOMWarning.class.isAssignableFrom(type)) {
			className = "warning";
		}
		else if (KDOMError.class.isAssignableFrom(type)) {
			className = "error";
		}

		string.append(KnowWEUtils.maskHTML("<span class='" + className + "'>"));
		for (KDOMReportMessage error : messages) {
			string.append(error.getVerbalization());
			string.append("\n");
		}
		string.append(KnowWEUtils.maskHTML("</span>"));
	}
}