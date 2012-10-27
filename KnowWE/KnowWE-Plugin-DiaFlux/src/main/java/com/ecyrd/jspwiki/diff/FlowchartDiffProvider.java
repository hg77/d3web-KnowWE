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

package com.ecyrd.jspwiki.diff;

import java.io.IOException;
import java.text.ChoiceFormat;
import java.text.Format;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.ResourceBundle;

import org.apache.commons.jrcs.diff.AddDelta;
import org.apache.commons.jrcs.diff.ChangeDelta;
import org.apache.commons.jrcs.diff.Chunk;
import org.apache.commons.jrcs.diff.DeleteDelta;
import org.apache.commons.jrcs.diff.Diff;
import org.apache.commons.jrcs.diff.DifferentiationFailedException;
import org.apache.commons.jrcs.diff.Revision;
import org.apache.commons.jrcs.diff.RevisionVisitor;
import org.apache.commons.jrcs.diff.myers.MyersDiff;

import com.ecyrd.jspwiki.NoRequiredPropertyException;
import com.ecyrd.jspwiki.TextUtil;
import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.i18n.InternationalizationManager;

import de.d3web.core.utilities.Pair;
import de.knowwe.core.kdom.RootType;
import de.knowwe.core.kdom.parsing.Section;
import de.knowwe.core.kdom.parsing.Sections;
import de.knowwe.core.user.UserContext;
import de.knowwe.core.user.UserContextUtil;
import de.knowwe.diaflux.DiaFluxDiffDisplay;
import de.knowwe.diaflux.FlowchartUtils;
import de.knowwe.diaflux.LoadFlowchartAction;
import de.knowwe.diaflux.type.DiaFluxType;
import de.knowwe.diaflux.type.FlowchartType;
import de.knowwe.jspwiki.JSPWikiUserContext;

/**
 * 
 * 
 * @author Reinhard Hatko
 * @created 26.10.2012
 */
public class FlowchartDiffProvider implements DiffProvider {

	private static final String CSS_DIFF_ADDED = "<tr><td class=\"diffadd\">";
	private static final String CSS_DIFF_REMOVED = "<tr><td class=\"diffrem\">";
	private static final String CSS_DIFF_UNCHANGED = "<tr><td class=\"diff\">";
	private static final String CSS_DIFF_CLOSE = "</td></tr>" + Diff.NL;


	public String getProviderInfo() {
		return "FlowchartDiffProvider";
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see com.ecyrd.jspwiki.WikiProvider#initialize(com.ecyrd.jspwiki.WikiEngine,
	 *      java.util.Properties)
	 */
	public void initialize(WikiEngine engine, Properties properties)
			throws NoRequiredPropertyException, IOException {
	}

	/**
	 * Makes a diff using the BMSI utility package. We use our own diff printer,
	 * which makes things easier.
	 * 
	 * @param ctx The WikiContext in which the diff should be made.
	 * @param p1 The first string
	 * @param p2 The second string.
	 * 
	 * @return Full HTML diff.
	 */
	public String makeDiffHtml(WikiContext ctx, String p1, String p2) {


		// first version is empty page. Diff is called when article is created
		if (p1.equals("")) {
			return "";
		}
		UserContext user = new JSPWikiUserContext(ctx,
				UserContextUtil.getParameters(ctx.getHttpRequest()));


		StringBuffer buffy = new StringBuffer();

		List<Section<FlowchartType>> leftFlows = getFlowsFrom(p1);
		List<Section<FlowchartType>> rightFlows = getFlowsFrom(p2);

		Collection<Pair<Section<FlowchartType>, Section<FlowchartType>>> alignedFlows = alignFlows(
				leftFlows, rightFlows);

		// TODO instead of removing the flows and creating the textdiff over the
		// remainings at the end, the textdiff should be inserted at the right
		// positionss
		StringBuilder leftBob = new StringBuilder(p1);
		StringBuilder rightBob = new StringBuilder(p2);

		boolean insert = true;
		for (Pair<Section<FlowchartType>, Section<FlowchartType>> pair : alignedFlows) {
			Section<FlowchartType> left = pair.getA();
			Section<FlowchartType> right = pair.getB();

			if (left != null && right != null) {
				if (!compareFlows(left, right)) {
					renderChangedFlow(user, pair, buffy, insert);
					remove(left, leftBob);
					remove(right, rightBob);
				}

			}
			else if (left != null) {
				renderRemovedFlow(user, left, buffy, insert);
				remove(left, leftBob);
			}
			else {
				renderAddedFlow(user, right, buffy, insert);
				remove(right, rightBob);
			}

			insert = false;

		}


		try {
			createTextDiff(ctx, leftBob.toString(), rightBob.toString(), buffy);
		}
		catch (DifferentiationFailedException e) {
			buffy.append("Error in Diff:" + e.getMessage());
			e.printStackTrace();
		}

		return buffy.toString();

	}

	/**
	 * removes the source of the flow from the article
	 */
	private void remove(Section<FlowchartType> flow, StringBuilder bob) {
		Section<DiaFluxType> diaFlux = Sections.findAncestorOfType(flow, DiaFluxType.class);
		int start = diaFlux.getAbsolutePositionStartInArticle();
		int end = start + diaFlux.getText().length();
		bob.replace(start, end, "");

	}

	/**
	 * 
	 * @created 26.10.2012
	 * @param user
	 * @param b
	 * @param buffy
	 * @param insert
	 */
	private void renderAddedFlow(UserContext user, Section<FlowchartType> flow, StringBuffer buffy, boolean insert) {
		renderFlow(user, flow, buffy, insert, "flowAdded");
	}

	private void renderRemovedFlow(UserContext user, Section<FlowchartType> flow, StringBuffer buffy, boolean insert) {
		renderFlow(user, flow, buffy, insert, "flowRemoved");
	}

	private void renderFlow(UserContext user, Section<FlowchartType> flow, StringBuffer buffy, boolean insert, String cssClass) {
		String parentID = FlowchartUtils.getParentID(flow);
		buffy.append("<div class=\"" + cssClass + "\">");
		insertFlowRenderer(user, flow, buffy, insert, parentID);
		buffy.append("</div>");

	}

	private void renderChangedFlow(UserContext user, Pair<Section<FlowchartType>, Section<FlowchartType>> pair, StringBuffer buffy, boolean insert) {
		String parentID = FlowchartUtils.getParentID(pair.getA());
		buffy.append("<div class=\"flowChanged\" id=\"diffParent" + parentID + "\">");

		insertFlowRenderer(user, pair.getA(), buffy, insert, parentID + "-LEFT");
		insertFlowRenderer(user, pair.getB(), buffy, false, parentID + "-RGHT");
		buffy.append("</div>");
	}

	/**
	 * 
	 * @created 25.10.2012
	 * @param articleSource
	 * @return
	 */
	public List<Section<FlowchartType>> getFlowsFrom(String articleSource) {
		Section<RootType> root = LoadFlowchartAction.sectionizeArticle(articleSource);
		List<Section<FlowchartType>> flows = Sections.findSuccessorsOfType(root,
				FlowchartType.class);
		return flows;
	}


	private static Collection<Pair<Section<FlowchartType>, Section<FlowchartType>>> alignFlows(List<Section<FlowchartType>> leftFlows, List<Section<FlowchartType>> rightFlows) {
		List<Pair<Section<FlowchartType>, Section<FlowchartType>>> alignments = new LinkedList<Pair<Section<FlowchartType>, Section<FlowchartType>>>();

		for (Section<FlowchartType> leftFlow : leftFlows) {
			Section<FlowchartType> rightFlow = LoadFlowchartAction.findFlowInDifferentVersion(
					leftFlow, rightFlows);

			if (rightFlow != null) {
				alignments.add(createPair(leftFlow, rightFlow));

			}
			else {
				alignments.add(createPair(leftFlow, null));
			}
		}

		nextFlow:
		for (int i = 0, size = rightFlows.size(); i < size; i++) {
			Section<FlowchartType> rightFlow = rightFlows.get(i);

			Section<FlowchartType> leftFlow = LoadFlowchartAction.findFlowInDifferentVersion(
					rightFlow, leftFlows);

			// check if this flow was added (has no corresponding one in other
			// version)
			if (leftFlow == null) {
				// now find the correct position
				int index = 0;
				for (int j = 0; j < alignments.size(); j++) {
					Pair<Section<FlowchartType>, Section<FlowchartType>> pair = alignments.get(j);

					//
					if (pair.getB() != null) index++;

					if (index == i) {
						alignments.add(index, createPair(null, rightFlow));
						continue nextFlow;
					}

				}
				// we reached the end of the alignments, so just add it
				alignments.add(createPair(null, rightFlow));

			}
			// other this flow must have been found before, so do nothing

		}

		return alignments;
	}

	private static Pair<Section<FlowchartType>, Section<FlowchartType>> createPair(Section<FlowchartType> leftFlow, Section<FlowchartType> rightFlow) {
		return new Pair<Section<FlowchartType>, Section<FlowchartType>>(leftFlow,
				rightFlow);
	}

	private static boolean compareFlows(Section<FlowchartType> left, Section<FlowchartType> right) {
		return left.getText().equals(right.getText());
	}

	public void insertFlowRenderer(UserContext user, Section<FlowchartType> flow, StringBuffer ret, boolean insert, String parentId) {
		ret.append(FlowchartUtils.createFlowchartRenderer(flow, user, parentId,
				DiaFluxDiffDisplay.SCOPE, insert));
	}

	private String createTextDiff(WikiContext ctx, String p1, String p2, StringBuffer buffy) throws DifferentiationFailedException {
		String[] first = Diff.stringToArray(TextUtil.replaceEntities(p1));
		String[] second = Diff.stringToArray(TextUtil.replaceEntities(p2));
		Revision rev = Diff.diff(first, second, new MyersDiff());

		if (rev == null || rev.size() == 0) {
			// No difference

			return "";
		}

		// first version is empty page. Diff is called when article is created
		if (p1.equals("")) {
			return "";
		}

		// how big
		// it will
		// become...

		buffy.append("<table class=\"diff\" border=\"0\" cellspacing=\"0\" cellpadding=\"0\">\n");
		rev.accept(new RevisionPrint(ctx, buffy));
		buffy.append("</table>\n");

		return buffy.toString();
	}



	private static final class RevisionPrint
			implements RevisionVisitor {

		private StringBuffer m_result = null;
		private final WikiContext m_context;
		private final ResourceBundle m_rb;

		private RevisionPrint(WikiContext ctx, StringBuffer sb) {
			m_result = sb;
			m_context = ctx;
			m_rb = ctx.getBundle(InternationalizationManager.CORE_BUNDLE);
		}

		public void visit(Revision rev) {
			// GNDN (Goes nowhere, does nothing)
		}

		public void visit(AddDelta delta) {
			Chunk changed = delta.getRevised();
			print(changed, m_rb.getString("diff.traditional.added"));
			changed.toString(m_result, CSS_DIFF_ADDED, CSS_DIFF_CLOSE);
		}

		public void visit(ChangeDelta delta) {
			Chunk changed = delta.getOriginal();
			print(changed, m_rb.getString("diff.traditional.changed"));
			changed.toString(m_result, CSS_DIFF_REMOVED, CSS_DIFF_CLOSE);
			delta.getRevised().toString(m_result, CSS_DIFF_ADDED, CSS_DIFF_CLOSE);
		}

		public void visit(DeleteDelta delta) {
			Chunk changed = delta.getOriginal();
			print(changed, m_rb.getString("diff.traditional.removed"));
			changed.toString(m_result, CSS_DIFF_REMOVED, CSS_DIFF_CLOSE);
		}

		private void print(Chunk changed, String type) {
			m_result.append(CSS_DIFF_UNCHANGED);

			String[] choiceString =
					{
							m_rb.getString("diff.traditional.oneline"),
							m_rb.getString("diff.traditional.lines")
					};
			double[] choiceLimits = {
					1, 2 };

			MessageFormat fmt = new MessageFormat("");
			fmt.setLocale(WikiContext.getLocale(m_context));
			ChoiceFormat cfmt = new ChoiceFormat(choiceLimits, choiceString);
			fmt.applyPattern(type);
			Format[] formats = {
					NumberFormat.getInstance(), cfmt, NumberFormat.getInstance() };
			fmt.setFormats(formats);

			Object[] params = {
					changed.first() + 1,
					changed.size(),
					changed.size() };
			m_result.append(fmt.format(params));
			m_result.append(CSS_DIFF_CLOSE);
		}
	}

}