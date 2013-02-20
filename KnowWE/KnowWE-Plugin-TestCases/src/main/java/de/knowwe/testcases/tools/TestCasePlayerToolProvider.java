package de.knowwe.testcases.tools;

import de.knowwe.core.kdom.parsing.Section;
import de.knowwe.core.kdom.parsing.Sections;
import de.knowwe.core.user.UserContext;
import de.knowwe.kdom.defaultMarkup.ContentType;
import de.knowwe.tools.DefaultTool;
import de.knowwe.tools.Tool;
import de.knowwe.tools.ToolProvider;

public class TestCasePlayerToolProvider implements ToolProvider {

	@Override
	public Tool[] getTools(Section<?> section, UserContext userContext) {
		return new Tool[] {
				getGoToTools(section, userContext), getDownalodTool(section, userContext),
				getExpandTool(section, userContext), getCollapseTool(section, userContext) };
	}

	public Tool getCollapseTool(Section<?> section, UserContext userContext) {
		String jsAction = "TestCasePlayer.toggleFindings('" + section.getID() + "', 'collapse');";
		Tool collapseTool = new DefaultTool("KnowWEExtension/images/collapseall_16x16.png",
				"Collapse findings", "Collapses all finding columns of the current test case",
				jsAction);
		return collapseTool;
	}

	public Tool getExpandTool(Section<?> section, UserContext userContext) {
		String jsAction = "TestCasePlayer.toggleFindings('" + section.getID() + "', 'expand');";
		Tool expandTool = new DefaultTool("KnowWEExtension/images/expandall_16x16.png",
				"Expand findings", "Expands all finding columns of the current test case",
				jsAction);
		return expandTool;
	}

	public Tool getGoToTools(Section<?> section, UserContext userContext) {

		String jsAction = "var goToLink = jq$('#"
				+ section.getID()
				+ "').find('select').find('[selected=&quot;selected&quot;]').attr('caselink');window.location=goToLink;";
		Tool goToTool = new DefaultTool("KnowWEExtension/testcaseplayer/icon/testcaselink.png",
				"Go to test case", "Go to the currently selected test case", jsAction);
		return goToTool;
	}

	public Tool getDownalodTool(Section<?> section, UserContext userContext) {
		Section<ContentType> content = Sections.findChildOfType(section, ContentType.class);
		String jsAction = "window.location='action/DownloadCaseAction?playerid="
				+ content.getID()
				+ "&KWikiWeb=default_web';";
		Tool downloadTool = new DefaultTool("KnowWEExtension/d3web/icon/download16.gif",
				"Download test case", "Downloads the currently selected test case", jsAction);
		return downloadTool;
	}

}