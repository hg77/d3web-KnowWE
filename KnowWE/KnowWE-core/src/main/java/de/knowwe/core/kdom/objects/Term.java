/*
 * Copyright (C) 2012 University Wuerzburg, Computer Science VI
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
package de.knowwe.core.kdom.objects;

import de.knowwe.core.compile.terminology.TermIdentifier;
import de.knowwe.core.compile.terminology.TerminologyManager;
import de.knowwe.core.kdom.Type;
import de.knowwe.core.kdom.parsing.Section;

/**
 * Basic {@link Term} which can easily be handled for registering in a
 * {@link TerminologyManager}.
 * 
 * @author Albrecht Striffler (denkbares GmbH)
 * @created 07.02.2012
 */
public interface Term extends Type {

	/**
	 * Defines the class for which the {@link Term} should be registered.
	 * 
	 * @created 28.03.2013
	 * @param section the {@link Section} with this interface
	 * @return the class of this {@link Term}
	 */
	public Class<?> getTermObjectClass(Section<? extends Term> section);

	/**
	 * Defines the {@link TermIdentifier} which can be used to register this
	 * {@link Term}. {@link TermIdentifier} need to be unique inside the
	 * {@link TerminologyManager}, the term name does not have this restriction.
	 * 
	 * @created 28.03.2013
	 * @param section the {@link Section} with this interface
	 * @return the TermIdentifier for this {@link Section}
	 */
	public TermIdentifier getTermIdentifier(Section<? extends Term> section);

	/**
	 * Defines the name of this {@link Term}. It can be used for example to
	 * render this Term, but is often also at least part of the
	 * {@link TermIdentifier}.
	 * 
	 * @created 28.03.2013
	 * @param section the {@link Section} with this {@link InternalError}
	 * @return the name of this term
	 */
	public String getTermName(Section<? extends Term> section);

}
