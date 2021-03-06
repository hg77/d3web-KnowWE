/*
 * Copyright (C) 2015 denkbares GmbH, Germany
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

package de.knowwe.ontology.tools;

import org.openrdf.model.URI;

import de.knowwe.core.compile.Compilers;
import de.knowwe.core.kdom.parsing.Section;
import de.knowwe.core.kdom.parsing.Sections;
import de.knowwe.core.user.UserContext;
import de.knowwe.ontology.compile.OntologyCompiler;
import de.knowwe.ontology.kdom.resource.AbbreviatedResourceReference;
import de.knowwe.ontology.kdom.resource.ResourceReference;
import de.knowwe.tools.DefaultTool;
import de.knowwe.tools.Tool;
import de.knowwe.tools.ToolProvider;
import de.knowwe.util.Icon;

/**
 * @author Jochen Reutelshoefer (denkbares GmbH)
 * @created 03.02.15.
 */
public class DisplayFullURITool implements ToolProvider {
    @Override
    public Tool[] getTools(Section<?> section, UserContext userContext) {
        if (section.get() instanceof ResourceReference) {
            final Section<? extends AbbreviatedResourceReference> abbRef = Sections.ancestor(section, AbbreviatedResourceReference.class);
            final OntologyCompiler compiler = Compilers.getCompiler(abbRef, OntologyCompiler.class);
            if (compiler != null) {
                final URI resourceURI = abbRef.get().getResourceURI(compiler.getRdf2GoCore(), abbRef);
                if (resourceURI != null) {
                    final String fullURIText = resourceURI.toString();
                    return new Tool[]{new DefaultTool(Icon.INFO, fullURIText, fullURIText, "alert('"+fullURIText+"');")};
                }
            }
        }
        return new Tool[0];
    }

    @Override
    public boolean hasTools(Section<?> section, UserContext userContext) {
        return true;
    }
}
