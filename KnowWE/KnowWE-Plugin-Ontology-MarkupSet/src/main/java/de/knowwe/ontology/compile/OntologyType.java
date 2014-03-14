/*
 * Copyright (C) 2013 University Wuerzburg, Computer Science VI
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
package de.knowwe.ontology.compile;

import de.knowwe.core.compile.PackageCompiler;
import de.knowwe.core.compile.PackageRegistrationCompiler;
import de.knowwe.core.compile.PackageRegistrationCompiler.PackageRegistrationScript;
import de.knowwe.core.compile.Priority;
import de.knowwe.core.compile.packaging.DefaultMarkupPackageCompileType;
import de.knowwe.core.compile.packaging.DefaultMarkupPackageCompileTypeRenderer;
import de.knowwe.core.compile.packaging.PackageCompileType;
import de.knowwe.core.compile.packaging.PackageManager;
import de.knowwe.core.compile.packaging.PackageTerm;
import de.knowwe.core.kdom.parsing.Section;
import de.knowwe.core.kdom.parsing.Sections;
import de.knowwe.core.report.CompilerMessage;
import de.knowwe.kdom.defaultMarkup.DefaultMarkup;
import de.knowwe.kdom.defaultMarkup.DefaultMarkupPackageTermReferenceRegistrationHandler;
import de.knowwe.kdom.defaultMarkup.DefaultMarkupType;
import de.knowwe.ontology.kdom.InitTerminologyHandler;
import de.knowwe.rdf2go.RuleSet;

/**
 * Compiles and provides ontology from the Ontology-MarkupSet.
 * 
 * @author Albrecht Striffler (denkbares GmbH)
 * @created 17.12.2013
 */
public class OntologyType extends DefaultMarkupType {

	public static final String ANNOTATION_COMPILE = "uses";
	public static final String ANNOTATION_RULESET = "ruleset";

	private static final DefaultMarkup MARKUP;

	static {
		MARKUP = new DefaultMarkup("Ontology");
		MARKUP.addAnnotation(ANNOTATION_COMPILE, false);
		MARKUP.addAnnotation(ANNOTATION_RULESET, false, RuleSet.values());
		DefaultMarkupPackageCompileType compileType = new DefaultMarkupPackageCompileType();
		compileType.addCompileScript(Priority.HIGHEST, new InitTerminologyHandler());
		compileType.addCompileScript(new PackageRegistrationScript<PackageCompileType>() {

			@Override
			public void compile(de.knowwe.core.compile.PackageRegistrationCompiler compiler, Section<PackageCompileType> section) throws CompilerMessage {
				Section<DefaultMarkupType> ontologyType = Sections.findAncestorOfType(section, DefaultMarkupType.class);
				compiler.getPackageManager().registerPackageCompileSection(section);
				String ruleSetValue = DefaultMarkupType.getAnnotation(ontologyType, ANNOTATION_RULESET);
				RuleSet ruleSet = getRuleSet(ruleSetValue);
				OntologyCompiler ontologyCompiler = new OntologyCompiler(
						compiler.getPackageManager(), section, ruleSet);
				compiler.getCompilerManager().addCompiler(5, ontologyCompiler);
				if (ruleSetValue != null && ruleSet == null) {
					throw CompilerMessage.warning("The rule set \"" + ruleSetValue + "\" does not exist.");
				}
			}

			private RuleSet getRuleSet(String ruleSetValue) {
				if (ruleSetValue != null) {
					return RuleSet.valueOf(ruleSetValue);
				}
				return null;
			}

			@Override
			public void destroy(de.knowwe.core.compile.PackageRegistrationCompiler compiler, Section<PackageCompileType> section) {
				// we just remove the no longer used compiler... we do not need to destroy the s
				compiler.getPackageManager().unregisterPackageCompileSection(section);
				for (PackageCompiler packageCompiler : section.get().getPackageCompilers(section)) {
					if (packageCompiler instanceof OntologyCompiler) {
						compiler.getCompilerManager().removeCompiler(packageCompiler);
					}
				}
			}

		});
		MARKUP.addContentType(compileType);

		MARKUP.addAnnotationContentType(PackageManager.COMPILE_ATTRIBUTE_NAME,
				new PackageTerm());
	}

	public OntologyType() {
		super(MARKUP);

		this.removeCompileScript(PackageRegistrationCompiler.class,
				DefaultMarkupPackageTermReferenceRegistrationHandler.class);
		this.setRenderer(new DefaultMarkupPackageCompileTypeRenderer());

	}
}