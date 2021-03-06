package de.d3web.we.kdom.xcl.list;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import de.d3web.we.kdom.xcl.list.CoveringList.CoveringRelation;
import de.knowwe.core.kdom.parsing.Section;
import de.knowwe.core.kdom.parsing.Sections;
import de.knowwe.core.kdom.rendering.RenderResult;
import de.knowwe.core.preview.AbstractPreviewRenderer;
import de.knowwe.core.user.UserContext;

public class CoveringListPreviewRenderer extends AbstractPreviewRenderer {

	@Override
	public void render(Section<?> section, Collection<Section<?>> relevantSubSections, UserContext user, RenderResult result) {

		Section<ListSolutionType> self = Sections.child(section, ListSolutionType.class);
		result.append(self, user);

		boolean skipped = false;
		for (Section<CoveringRelation> relation : Sections.children(section,
				CoveringRelation.class)) {
			List<Section<?>> all = Sections.successors(relation);
			if (Collections.disjoint(all, relevantSubSections)) {
				skipped = true;
			}
			else {
				if (skipped) {
					renderEllipse(result);
					skipped = false;
				}
				result.appendHtml("<div class='relation'>");
				result.append(relation, user);
				result.appendHtml("</div>");
			}
		}
		if (skipped) {
			renderEllipse(result);
		}
		result.append("}");
	}

	private void renderEllipse(RenderResult result) {
		result.appendHtml("<div class='ellipse'>")
				.appendJSPWikiMarkup("[...]")
				.appendHtml("</div>");
	}
}
