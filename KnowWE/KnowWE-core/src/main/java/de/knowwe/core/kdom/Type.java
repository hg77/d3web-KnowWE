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

package de.knowwe.core.kdom;

import java.util.Collection;
import java.util.List;
import java.util.TreeMap;

import de.knowwe.core.compile.Priority;
import de.knowwe.core.kdom.parsing.Parser;
import de.knowwe.core.kdom.parsing.Section;
import de.knowwe.core.kdom.rendering.KnowWEDomRenderer;
import de.knowwe.core.kdom.subtreeHandler.SubtreeHandler;
import de.knowwe.core.report.MessageRenderer;

/**
 * @author Jochen
 * 
 *         This interface is the foundation of the KnowWE2 Knowledge-DOM
 *         type-system. To every node in this dom tree exactly one Type is
 *         associated.
 * 
 *         A type defines itself by its SectionFinder, which allocates text
 *         parts to this type.
 * @see getSectioner
 * 
 *      Further it defines what subtypes it allows.
 * @see getAllowedChildrenTypes
 * 
 *      For user presentation it provides a renderer.
 * @see getRenderer
 * 
 */
public interface Type {

	public boolean isType(Class<? extends Type> clazz);

	public boolean isAssignableFromType(Class<? extends Type> clazz);

	/**
	 * When KnowWE renders the article this renderer is used to render this
	 * node. In most cases rendering should be delegated to children types.
	 * 
	 * @return
	 */
	public KnowWEDomRenderer getRenderer();

	/**
	 * Returns the parser that can be used to parse the textual markup of this
	 * type into a section of this type. The parser is responsible to build the
	 * full kdom tree for the specified text.
	 * 
	 * @created 12.03.2011
	 * @return the parser to be used to parse textual markup of this type
	 */
	public abstract Parser getParser();

	/**
	 * @return name of this type
	 */
	public abstract String getName();

	/**
	 * A (priority-ordered) list of the types, which are allowed as children of
	 * nodes of this type
	 * 
	 * @return
	 */
	public abstract List<Type> getAllowedChildrenTypes();

	/**
	 * This method offers the possibility for a type to revise its subtree when
	 * its completed. Not necessary in most cases.
	 * 
	 * @param section
	 * @param kbm
	 */
	// public void reviseSubtree(KnowWEArticle article, Section<? extends
	// Type> section, Priority p);

	// public <T extends Type> void reviseSubtree(KnowWEArticle
	// article, Section<T> section, SubtreeHandler<T> h);

	public Collection<Section> getAllSectionsOfType();

	public abstract void findTypeInstances(Class clazz,
			List<Type> instances);

	public void deactivateType();

	public void activateType();

	public boolean getActivationStatus();

	public MessageRenderer getErrorRenderer();

	public MessageRenderer getNoticeRenderer();

	public MessageRenderer getWarningRenderer();

	public boolean isLeafType();

	public boolean isNotRecyclable();

	public boolean allowesGlobalTypes();

	public void setNotRecyclable(boolean notRecyclable);

	public boolean isOrderSensitive();

	public boolean isIgnoringPackageCompile();

	public void setIgnorePackageCompile(boolean ignorePackageCompile);

	public void setOrderSensitive(boolean orderSensitive);

	public TreeMap<Priority, List<SubtreeHandler<? extends Type>>> getSubtreeHandlers();

	public List<SubtreeHandler<? extends Type>> getSubtreeHandlers(Priority p);

}