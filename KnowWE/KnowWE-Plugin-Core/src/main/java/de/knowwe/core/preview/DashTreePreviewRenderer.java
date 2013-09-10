package de.knowwe.core.preview;

import java.util.Collection;

import de.knowwe.core.kdom.parsing.Section;
import de.knowwe.core.kdom.parsing.Sections;
import de.knowwe.core.kdom.rendering.DelegateRenderer;
import de.knowwe.core.kdom.rendering.RenderResult;
import de.knowwe.core.user.UserContext;
import de.knowwe.kdom.dashtree.DashSubtree;
import de.knowwe.kdom.dashtree.DashTreeElement;

/**
 * Renders a preview of a dashtree element. The preview shows the element itself
 * and the direct children elements only (skipping all other items)
 * 
 * @author Volker Belli (denkbares GmbH)
 * @created 16.08.2013
 */
public class DashTreePreviewRenderer implements PreviewRenderer {

	private static final String ELLIPSE = " [...]";
	private static final int maxChildren = 3;

	public DashTreePreviewRenderer() {
	}

	@Override
	public void render(Section<?> section, Collection<Section<?>> relevantSubSections, UserContext user, RenderResult result) {
		Section<DashTreeElement> self = Sections.findChildOfType(section, DashTreeElement.class);
		renderDashTreeElement(self, user, result);
		int count = 0;
		for (Section<DashSubtree> child : Sections.findChildrenOfType(section, DashSubtree.class)) {
			if (++count > maxChildren) {
				result.appendJSPWikiMarkup(ELLIPSE);
				break;
			}
			Section<DashTreeElement> item = Sections.findChildOfType(child, DashTreeElement.class);
			renderDashTreeElement(item, user, result);
		}
	}

	private void renderDashTreeElement(Section<DashTreeElement> self, UserContext user, RenderResult string) {
		DelegateRenderer.getRenderer(self, user).render(self, user, string);
	}
}