/*
 * Copyright (C) 2013 denkbares GmbH
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
package de.knowwe.ontology.turtle;

import org.openrdf.model.Resource;
import org.openrdf.model.Value;

import de.knowwe.core.kdom.AbstractType;
import de.knowwe.core.kdom.Type;
import de.knowwe.core.kdom.parsing.Section;
import de.knowwe.core.kdom.parsing.Sections;
import de.knowwe.core.kdom.rendering.DelegateRenderer;
import de.knowwe.kdom.renderer.CompositeRenderer;
import de.knowwe.ontology.edit.DropTargetRenderer;
import de.knowwe.ontology.turtle.compile.NodeProvider;
import de.knowwe.ontology.turtle.compile.ResourceProvider;
import de.knowwe.ontology.turtle.lazyRef.LazyURIReference;
import de.knowwe.rdf2go.Rdf2GoCompiler;

public class Subject extends AbstractType implements ResourceProvider<Subject> {

	public Subject(Type subjectDefinitionType) {
		this.addChildType(new BlankNode());
		this.addChildType(new BlankNodeID());
		this.addChildType(new EncodedTurtleURI());
		this.addChildType(subjectDefinitionType);
		this.addChildType(new LazyURIReference());
		setSectionFinder(new FirstWordFinder());

		this.setRenderer(new CompositeRenderer(DelegateRenderer.getInstance(),
				new DropTargetRenderer()));

	}


	@Override
	@SuppressWarnings({
			"rawtypes", "unchecked" })
	public Value getNode(Section<Subject> section, Rdf2GoCompiler compiler) {
		// there should be exactly one NodeProvider child (while potentially
		// many successors)
		Section<NodeProvider> nodeProviderChild = Sections.child(section,
				NodeProvider.class);
		if (nodeProviderChild != null) {
			return nodeProviderChild.get().getNode(nodeProviderChild, compiler);
		}
		return null;
	}

	@Override
	public Resource getResource(Section<Subject> section, Rdf2GoCompiler core) {
		return (Resource) getNode(section, core);

	}

}