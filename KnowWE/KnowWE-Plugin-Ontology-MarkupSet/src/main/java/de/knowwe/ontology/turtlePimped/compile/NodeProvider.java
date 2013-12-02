package de.knowwe.ontology.turtlePimped.compile;

import org.ontoware.rdf2go.model.node.Node;

import de.knowwe.core.kdom.Type;
import de.knowwe.core.kdom.parsing.Section;
import de.knowwe.rdf2go.Rdf2GoCore;


public interface NodeProvider<T extends Type> extends Type {

	Node getNode(Section<T> section, Rdf2GoCore core);

}
