package de.d3web.we.kdom.objects;

import de.d3web.we.kdom.KnowWEObjectType;
import de.d3web.we.kdom.Section;

/**
 * Interface for type containing/wrapping term names
 * 
 * @author Jochen
 * 
 * @param <TermObject>
 */
public interface TermReference<TermObject> extends KnowWEObjectType {

	public String getTermName(Section<? extends TermReference<TermObject>> s);
}
