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

package de.knowwe.kdom.dashtree;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import de.knowwe.core.kdom.KnowWEArticle;
import de.knowwe.core.kdom.Type;
import de.knowwe.core.kdom.objects.TermDefinition;
import de.knowwe.core.kdom.parsing.Section;
import de.knowwe.core.kdom.parsing.Sections;
import de.knowwe.core.utils.KnowWEUtils;

public class DashTreeUtils {

	private static DashTreeUtils instance = null;

	public static DashTreeUtils getInstance() {
		if (instance == null) {
			instance = new DashTreeUtils();
		}
		return instance;
	}

	protected DashTreeUtils() {
	}
	
	public static List<Section<DashTreeElement>> findSuccessorDashtreeElements(Section<? extends DashTreeElement> element) {
		List<Section<DashTreeElement>> found = new ArrayList<Section<DashTreeElement>>();
		Sections.findSuccessorsOfType(element.getFather(),DashTreeElement.class,found);
		found.remove(element); //remove self
		return found;
	}
	
	public static List<Section<DashTreeElement>> findChildrenDashtreeElements(Section<? extends DashTreeElement> element) {
		List<Section<DashTreeElement>> found = new ArrayList<Section<DashTreeElement>>();
		Sections.findSuccessorsOfType(element.getFather(),DashTreeElement.class,2,found);
		found.remove(element); //remove self
		return found;
	}

	public static Section<? extends DashTreeElement> getFatherDashTreeElement(Section<?> s) {
		Section<? extends DashSubtree> dashSubtree = getFatherDashSubtree(s);
		if (dashSubtree != null) {
			return Sections.findSuccessor(dashSubtree, DashTreeElement.class);
		}
		return null;
	}

	public static Section<? extends DashTreeElement> getAncestorDashTreeElement(Section<?> s, int dashLevel) {
		Section<? extends DashSubtree> dashSubtree = getAncestorDashSubtree(s, dashLevel);
		if (dashSubtree != null) {
			return Sections.findChildOfType(dashSubtree, DashTreeElement.class);
		}
		return null;
	}

	public static List<Section<? extends DashTreeElement>> getAncestorDashTreeElements(Section<?> s) {
		List<Section<? extends DashTreeElement>> ancestors = new ArrayList<Section<? extends DashTreeElement>>();
		List<Section<?>> ancestorSubTrees = new ArrayList<Section<?>>();
		Section<?> ancestorSubtree = Sections.findAncestorOfType(s, DashSubtree.class).getFather();
		while (ancestorSubtree != null && ancestorSubtree.get() instanceof DashSubtree) {
			ancestorSubTrees.add(ancestorSubtree);
			ancestorSubtree = ancestorSubtree.getFather();
		}
		for (Section<?> subTree : ancestorSubTrees) {
			ancestors.add(Sections.findChildOfType(subTree, DashTreeElement.class));
		}
		return ancestors;
	}

	/**
	 * Delegates the getDashTreeFather-operation to DashTreeElement
	 * 
	 * @param s
	 * @return
	 */
	public static Section<? extends DashTreeElementContent> getFatherDashTreeElementContent(Section<?> s) {
		Section<? extends DashTreeElement> dashTreeFatherElement = getFatherDashTreeElement(s);
		if (dashTreeFatherElement != null) {
			return Sections.findChildOfType(dashTreeFatherElement,
					DashTreeElementContent.class);
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	public static Section<? extends DashSubtree> getFatherDashSubtree(Section<?> s) {
		Section<? extends DashSubtree> dashSubtree = Sections.findAncestorOfType(s,
				DashSubtree.class);
		if (dashSubtree != null) {
			if (dashSubtree.getFather().get() instanceof DashSubtree) {
				return (Section<? extends DashSubtree>) dashSubtree.getFather();
			}
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	public static Section<DashSubtree> getAncestorDashSubtree(Section<?> s, int dashLevel) {
		if (dashLevel < 0) return null;
		Section<?> dashSubtree = Sections.findAncestorOfType(s, DashSubtree.class);
		if (dashSubtree != null) {
			int fLevel = getDashLevel(dashSubtree);
			if (fLevel < dashLevel) {
				return null;
			}
			for (int i = fLevel; i > dashLevel; i--) {
				dashSubtree = dashSubtree.getFather();
			}
			return (Section<DashSubtree>) dashSubtree;
		}
		return null;
	}

	/**
	 * Delivers the (dash-)level of the element by counting leading '-'
	 * 
	 * @param s
	 *            Only works for DashSubtree oder DashTreeElement sections
	 * @return
	 */
	public static int getDashLevel(Section<?> s) {

		if (s == null) return -1;

		String text = s.getOriginalText().trim();

		int index = 0;
		while (index < text.length() && text.charAt(index) == '-') {
			index++;
		}
		return index;
	}

	public static int getPositionInFatherDashSubtree(Section<?> s) {

		Section<DashSubtree> subTreeRoot = Sections.findAncestorOfType(s,
				DashSubtree.class);

		if (subTreeRoot != null) {

			Section<?> fatherSubTree = subTreeRoot.getFather();
			if (fatherSubTree.get() instanceof DashSubtree
					|| fatherSubTree.get() instanceof DashTree) {
				int pos = 0;
				for (Section<?> sec : fatherSubTree.getChildren()) {
					if (sec.get() instanceof DashSubtree) {
						if (sec == subTreeRoot) {
							return pos;
						}
						pos++;
					}
				}
			}
		}
		return 0;
	}

	/**
	 * Checks in the Subtree with the given dash level, if there are changed
	 * Sections. Ignores TermReferences!
	 */
	public static boolean isChangeInAncestorSubtree(KnowWEArticle article,
			Section<?> s, int dashLevel) {

		Section<DashSubtree> subtreeAncestor =
				DashTreeUtils.getAncestorDashSubtree(s, dashLevel);

		if (subtreeAncestor != null) {
			return isChangeInSubtree(article, subtreeAncestor, null);

		}
		return false;
	}

	public static boolean isChangeInAncestorSubtree(KnowWEArticle article, Section<?> s, int dashLevel, List<Class<? extends Type>> filteredTypes) {

		Section<DashSubtree> subtreeAncestor = DashTreeUtils.getAncestorDashSubtree(s,
				dashLevel);

		if (subtreeAncestor != null) {
			return isChangeInSubtree(article, subtreeAncestor, filteredTypes);

		}
		return false;
	}

	public static boolean isChangeInSubtree(KnowWEArticle article, Section<? extends DashSubtree> s, List<Class<? extends Type>> filteredTypes) {
		HashSet<Section<? extends DashSubtree>> visited = new HashSet<Section<? extends DashSubtree>>();
		visited.add(s);
		boolean change = isChangeInSubtree(article, s, filteredTypes, visited);
		return change;
	}

	@SuppressWarnings("unchecked")
	private static boolean isChangeInSubtree(KnowWEArticle article, Section<? extends DashSubtree> s, List<Class<? extends Type>> filteredTypes, HashSet<Section<? extends DashSubtree>> visited) {
		List<Section<?>> nodes = new LinkedList<Section<?>>();
		Sections.getAllNodesPostOrder(s, nodes);
		for (Section<?> node : nodes) {
			if (node.get() instanceof TermDefinition) {
				Section<TermDefinition> tdef = (Section<TermDefinition>) node;
				Collection<Section<? extends TermDefinition>> termDefs = new ArrayList<Section<? extends TermDefinition>>();
				termDefs.addAll(KnowWEUtils.getTerminologyHandler(article.getWeb()).getRedundantTermDefiningSections(
								article, tdef.get().getTermIdentifier(tdef),
						tdef.get().getTermScope()));
				termDefs.add(KnowWEUtils.getTerminologyHandler(
						article.getWeb()).getTermDefiningSection(article,
						tdef.get().getTermIdentifier(tdef), tdef.get().getTermScope()));
				for (Section<?> tDef : termDefs) {
					if (tDef != null && tDef != node) {
						Section<DashSubtree> dashSubtree = getAncestorDashSubtree(tDef,
								getDashLevel(s));
						if (dashSubtree != null && !visited.contains(dashSubtree)) {
							visited.add(dashSubtree);
							if (isChangeInSubtree(article, dashSubtree, filteredTypes,
									visited)) {
								return true;
							}
						}
					}
				}
			}
			if (node.isChanged(article.getTitle(), filteredTypes)) return true;
		}
		return false;
	}

}