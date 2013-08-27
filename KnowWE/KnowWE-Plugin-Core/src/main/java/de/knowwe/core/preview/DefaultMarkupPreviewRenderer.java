package de.knowwe.core.preview;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import de.knowwe.core.kdom.RootType;
import de.knowwe.core.kdom.Type;
import de.knowwe.core.kdom.parsing.Section;
import de.knowwe.core.kdom.parsing.Sections;
import de.knowwe.core.kdom.rendering.RenderResult;
import de.knowwe.core.user.UserContext;
import de.knowwe.core.utils.Scope;
import de.knowwe.kdom.defaultMarkup.DefaultMarkupRenderer;
import de.knowwe.kdom.defaultMarkup.DefaultMarkupType;

/**
 * Default implementation of a {@link PreviewRenderer} that can be applied to
 * any ancestor-section of a default markup type (as scope of the preview
 * rendering extension), as well as to the whole default markup type. if it is
 * applied to the whole default markup type. the default markup frame is not
 * rendered, but the whole content and all annotations sections.
 * <p>
 * If it is only scoped to certain sub-sections only these sections are
 * rendered. In this case you can additionally specify a set of (relative)
 * scopes that will be additionally rendered.
 * <p>
 * Please not that this preview renderer is for selective rendering only. If you
 * wand to render a complete default markup section without its frame, you may
 * use the {@link DefaultPreviewRenderer} instead.
 * 
 * @author Volker Belli (denkbares GmbH)
 * @created 26.08.2013
 */
public class DefaultMarkupPreviewRenderer implements PreviewRenderer {

	public static enum Select {
		/**
		 * Shows all successors matching the scope
		 */
		all,

		/**
		 * Shows successors that contains relevant items
		 */
		relevant,

		/**
		 * Shows successors that contains relevant items, but all if there are
		 * no successors with relevant items
		 */
		relevantOrAll,

		/**
		 * Includes the sections matching the scope if the sibling after (!) the
		 * matched section has been included by a scope selection that is prior
		 * to this scope selection
		 */
		beforeSelected,

		/**
		 * Includes the sections matching the scope if the sibling before (!)
		 * the matched section has been included by a scope selection that is
		 * prior to this scope selection
		 */
		afterSelected
	}

	public final Map<Scope, Select> previewItems = new LinkedHashMap<Scope, Select>();

	public DefaultMarkupPreviewRenderer() {
	}

	/**
	 * Adds a new scope of sub-items to be shown. When rendering the preview all
	 * sub-sections will be visible that matches the scopes specified through
	 * this method. If forceVisible is true. all the matching sub-section are
	 * shown. If not, only the matching sub-sections are shown that contains
	 * relevant items.
	 * 
	 * @created 26.08.2013
	 * @param scope the sub-scope to be shown
	 * @param forceVisible if the are always shown or only if the contain
	 *        relevant items
	 */
	public void addPreviewItem(Scope scope, Select selector) {
		previewItems.put(scope, selector);
	}

	/**
	 * Adds the text of plain text section ins between annotation sections,
	 * containing the line breaks and white-spaces between the single
	 * annotations.
	 * 
	 * @created 27.08.2013
	 */
	public void addTextBeforeAnnotations() {
		addPreviewItem(Scope.getScope("DefaultMarkupType/PlainText"), Select.beforeSelected);
	}

	@Override
	public void render(Section<?> section, Collection<Section<?>> relevantSubSections, UserContext user, RenderResult result) {
		Section<?> parent = getParentSection(section);
		List<Section<?>> previews = new LinkedList<Section<?>>();
		// collect all relevant scoped sections plus the matched section
		for (Entry<Scope, Select> entry : previewItems.entrySet()) {
			Scope scope = entry.getKey();
			Select preview = entry.getValue();
			List<Section<?>> matches = scope.getMatchingSuccessors(parent);
			switch (preview) {

			case all:
				// add all matches if requested
				previews.addAll(matches);
				break;

			case relevant:
			case relevantOrAll:
				// add only relevant matches if requested
				boolean added = false;
				for (Section<?> match : matches) {
					if (hasSuccessor(match, relevantSubSections)) {
						previews.add(match);
						added = true;
					}
				}
				// add all if no one has shown to be relevant
				// if this has been requested
				if (preview.equals(Select.relevantOrAll) && !added) {
					previews.addAll(matches);
				}
				break;

			case afterSelected:
			case beforeSelected:
				for (Section<?> match : matches) {
					List<Section<? extends Type>> siblings = match.getFather().getChildren();
					int index = siblings.indexOf(match);
					if (index == -1) continue;
					// move index to the sibling that has to be included
					int selectedIndex = Select.afterSelected.equals(preview)
							? index - 1 : index + 1;
					if (selectedIndex < 0) continue;
					if (selectedIndex >= siblings.size()) continue;
					Section<?> sibling = siblings.get(selectedIndex);
					// and add this match if the desired sibling is available
					if (previews.contains(sibling)) previews.add(match);
				}
				break;
			}
		}
		renderSections(previews, user, result);
	}

	static void renderSections(List<Section<?>> previews, UserContext user, RenderResult result) {
		Collections.sort(previews);
		DefaultMarkupRenderer.renderContentSections(previews, user, result);
	}

	private Section<?> getParentSection(Section<?> section) {
		if (section.get() instanceof DefaultMarkupType) {
			return section;
		}
		Section<?> parent = Sections.findAncestorOfType(section, DefaultMarkupType.class);
		if (parent != null) {
			return parent;
		}
		parent = section;
		while (!(parent.getFather().get() instanceof RootType)) {
			parent = parent.getFather();
		}
		return parent;
	}

	/**
	 * Returns true if the specified parent section hat at least one of the
	 * specified successor sections as an successor.
	 * 
	 * @created 26.08.2013
	 * @param parent the parent section to start the search from
	 * @param successors the list of potential successors
	 * @return
	 */
	private static boolean hasSuccessor(Section<?> parent, Collection<Section<?>> successors) {
		if (successors.contains(parent)) return true;
		for (Section<?> child : parent.getChildren()) {
			if (hasSuccessor(child, successors)) return true;
		}
		return false;
	}

}