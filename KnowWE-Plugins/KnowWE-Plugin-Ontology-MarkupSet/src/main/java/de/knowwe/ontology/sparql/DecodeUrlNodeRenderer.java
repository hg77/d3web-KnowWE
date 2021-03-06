package de.knowwe.ontology.sparql;

import java.net.URLDecoder;

import org.openrdf.model.Value;

import de.knowwe.core.user.UserContext;
import de.knowwe.rdf2go.Rdf2GoCore;

public class DecodeUrlNodeRenderer implements SparqlResultNodeRenderer {

	@Override
	public String renderNode(Value node, String text, String variable, UserContext user, Rdf2GoCore core, RenderMode mode) {
		try {
			return URLDecoder.decode(text, "UTF-8");

		}
		catch (Exception e) {
			return text;
		}
	}

	@Override
	public boolean allowFollowUpRenderer() {
		return true;
	}

}
