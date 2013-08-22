package de.d3web.we.kdom.xcl.list;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import de.d3web.we.kdom.xcl.list.CoveringList.CoveringRelation;
import de.knowwe.core.kdom.parsing.Section;
import de.knowwe.core.kdom.parsing.Sections;
import de.knowwe.core.kdom.rendering.DelegateRenderer;
import de.knowwe.core.kdom.rendering.RenderResult;
import de.knowwe.core.preview.PreviewRenderer;
import de.knowwe.core.user.UserContext;

public class CoveringListPreviewRenderer implements PreviewRenderer {

	@Override
	public void render(Section<?> section, Collection<Section<?>> relevantSubSections, UserContext user, RenderResult result) {

		Section<ListSolutionType> self = Sections.findChildOfType(section, ListSolutionType.class);
		DelegateRenderer.getRenderer(self, user).render(self, user, result);

		boolean skipped = false;
		for (Section<CoveringRelation> relation : Sections.findChildrenOfType(section,
				CoveringRelation.class)) {
			List<Section<?>> all = Sections.getSubtreePreOrder(relation);
			if (Collections.disjoint(all, relevantSubSections)) {
				skipped = true;
			}
			else {
				if (skipped) {
					renderEllipse(result);
					skipped = false;
				}
				result.appendHtml("<div class='relation'>");
				DelegateRenderer.getRenderer(relation, user).render(relation, user, result);
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