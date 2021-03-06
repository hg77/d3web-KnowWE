package de.knowwe.core.preview;

import java.util.Collection;

import de.knowwe.core.kdom.parsing.Section;
import de.knowwe.core.kdom.rendering.RenderResult;
import de.knowwe.core.kdom.rendering.Renderer;
import de.knowwe.core.user.UserContext;

/**
 * Render interface for preview rendering. In addition to the "normal"
 * {@link Renderer} the preview renderer gets also a number of relevant
 * sub-sections that are the things the user requested to be shown. These
 * sub-sections shall be definitely included while rendering, maybe highlighted.
 * 
 * @author Volker Belli (denkbares GmbH)
 * @created 16.08.2013
 */
public interface PreviewRenderer {

	/**
	 * Renders the specified section as a preview. Additionally to the section,
	 * the relevant sub-sections are specified, that are the sections we to
	 * actually want to preview and the reason why this preview renderer has
	 * been selected.
	 * 
	 * @created 21.02.2014
	 * @param section the section to be rendered as a preview (matching the
	 *        scope)
	 * @param relevantSubSections the sub-sections that are actually requested
	 *        to be previewed
	 * @param user the user context to render for
	 * @param result the result to render into
	 */
	void render(Section<?> section, Collection<Section<?>> relevantSubSections, UserContext user, RenderResult result);

	/**
	 * Returns if this preview renderer matches the specified section and
	 * therefore can be used to render the section. If false is returned, this
	 * preview renderer will never by used to preview the specified section.
	 * Therefore these sections are never specified as the "section" attribute
	 * and/or "relevantSubSection" attribute in
	 * {@link #render(Section, Collection, UserContext, RenderResult)}.
	 * <p>
	 * Usually this method can always return true, because the section is
	 * previously been checked to be in the scope that is specified in the
	 * plugin declaration for this extension. But in some rare cases it might be
	 * useful, e.g. if the neighbor sections shall been considered or if the
	 * renderer shall only render a very common section (e.g. top-level article
	 * section, thus having a very global scope), but not been used to render
	 * very specific sub-sections.
	 * 
	 * @created 21.02.2014
	 * @param subSection the (sub-)section that shall been previewed
	 * @return if this renderer is capable to preview the section
	 */
	boolean matches(Section<?> subSection);
}