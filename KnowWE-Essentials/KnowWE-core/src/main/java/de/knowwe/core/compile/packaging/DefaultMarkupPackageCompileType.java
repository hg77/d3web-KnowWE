/*
 * Copyright (C) ${year} denkbares GmbH, Germany
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

package de.knowwe.core.compile.packaging;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

import com.denkbares.strings.Identifier;
import com.denkbares.strings.Strings;
import de.knowwe.core.compile.Compilers;
import de.knowwe.core.compile.PackageRegistrationCompiler;
import de.knowwe.core.compile.PackageRegistrationCompiler.PackageRegistrationScript;
import de.knowwe.core.compile.Priority;
import de.knowwe.core.compile.terminology.TerminologyManager;
import de.knowwe.core.kdom.parsing.Section;
import de.knowwe.core.kdom.parsing.Sections;
import de.knowwe.core.kdom.sectionFinder.RegexSectionFinder;
import de.knowwe.core.report.CompilerMessage;
import de.knowwe.core.report.Message;
import de.knowwe.core.report.Messages;
import de.knowwe.core.utils.KnowWEUtils;
import de.knowwe.kdom.defaultMarkup.AnnotationContentType;
import de.knowwe.kdom.defaultMarkup.DefaultMarkupType;

/**
 * Implementation of a compile type to handle the uses-annotations of a default markup. This Type does not add a
 * specific compiler, add a Compiler when you add this Type to your parent type with a new {@link
 * PackageRegistrationScript}.
 *
 * @author Albrecht Striffler, Volker Belli (denkbares GmbH)
 * @created 13.10.2010
 */
public class DefaultMarkupPackageCompileType extends PackageCompileType {

	public DefaultMarkupPackageCompileType() {
		this.setSectionFinder(new RegexSectionFinder(".*", Pattern.DOTALL));

		this.addCompileScript(Priority.HIGH, new PackageCompileSectionRegistrationScript());
		this.addCompileScript(Priority.DEFAULT, new CompileTypePackageTermDefinitionRegistrationScript());
		this.addCompileScript(Priority.LOW, new PackageWithoutSectionsWarningScript());
	}

	@Override
	public String[] getPackagesToCompile(Section<? extends PackageCompileType> section) {
		Section<DefaultMarkupType> markupSection = Sections.ancestor(section, DefaultMarkupType.class);
		List<Section<? extends AnnotationContentType>> usesSections = DefaultMarkupType.getAnnotationContentSections(markupSection, PackageManager.COMPILE_ATTRIBUTE_NAME);
		List<Section<PackageTerm>> termSections = new ArrayList<>();
		for (Section<? extends AnnotationContentType> usesSection : usesSections) {
			termSections.addAll(Sections.successors(usesSection, PackageTerm.class));
		}
		String[] uses = new String[termSections.size()];
		for (int i = 0; i < termSections.size(); i++) {
			uses[i] = termSections.get(i).getText();
		}
		if (uses.length == 0) {
			return KnowWEUtils.getPackageManager(section).getDefaultPackages(section.getArticle());
		}
		return uses;
	}

	/**
	 * Script to add the default markup section as the term definition of
	 * the compiled packages
	 */
	private static class CompileTypePackageTermDefinitionRegistrationScript extends PackageRegistrationScript<DefaultMarkupPackageCompileType> {

		private static final String PACKAGE_DEFINITIONS_KEY = "packageDefinitions";

		@Override
		public void compile(PackageRegistrationCompiler compiler, Section<DefaultMarkupPackageCompileType> section) {
			Section<DefaultMarkupType> markupSection = Sections.ancestor(section, DefaultMarkupType.class);
			String[] packageNames = DefaultMarkupType.getPackages(markupSection, PackageManager.COMPILE_ATTRIBUTE_NAME);
			// while destroying, the default packages will already
			// be removed, so we have to store artificially
			markupSection.storeObject(compiler, PACKAGE_DEFINITIONS_KEY, packageNames);
			TerminologyManager terminologyManager = compiler.getTerminologyManager();
			for (String packageName : packageNames) {
				Identifier termIdentifier = new Identifier(packageName);
				terminologyManager.registerTermDefinition(compiler, markupSection, Package.class, termIdentifier);
				Collection<Section<?>> termReferenceSections = terminologyManager.getTermReferenceSections(termIdentifier);
				Compilers.addSectionsToCompile(compiler, termReferenceSections, PackageNotCompiledWarningScript.class);
			}

		}

		@Override
		public void destroy(PackageRegistrationCompiler compiler, Section<DefaultMarkupPackageCompileType> section) {
			Section<DefaultMarkupType> markupSection = Sections.ancestor(section, DefaultMarkupType.class);
			String[] packageNames = (String[]) markupSection.getObject(compiler, PACKAGE_DEFINITIONS_KEY);
			TerminologyManager terminologyManager = compiler.getTerminologyManager();
			for (String packageName : packageNames) {
				Identifier termIdentifier = new Identifier(packageName);
				terminologyManager.unregisterTermDefinition(compiler, markupSection, Package.class, termIdentifier);
				Collection<Section<?>> termReferenceSections = terminologyManager.getTermReferenceSections(termIdentifier);
				Compilers.addSectionsToDestroyAndCompile(compiler, termReferenceSections, PackageNotCompiledWarningScript.class);
			}

		}

	}

	private static class PackageCompileSectionRegistrationScript extends PackageRegistrationScript<DefaultMarkupPackageCompileType> {

		@Override
		public void compile(PackageRegistrationCompiler compiler, Section<DefaultMarkupPackageCompileType> section) throws CompilerMessage {
			compiler.getPackageManager().registerPackageCompileSection(section);
		}

		@Override
		public void destroy(PackageRegistrationCompiler compiler, Section<DefaultMarkupPackageCompileType> section) {
			compiler.getPackageManager().unregisterPackageCompileSection(section);
		}
	}

	/**
	 * Script to check whether the compiled packages contain any packages
	 */
	private class PackageWithoutSectionsWarningScript extends PackageRegistrationScript<DefaultMarkupPackageCompileType> {

		@Override
		public void compile(PackageRegistrationCompiler compiler, Section<DefaultMarkupPackageCompileType> section) {
			String[] packagesToCompile = getPackagesToCompile(section);
			Collection<Section<?>> sectionsOfPackages = compiler.getPackageManager().getSectionsOfPackage(
					packagesToCompile);
			Section<DefaultMarkupType> markupSection = Sections.ancestor(
					section, DefaultMarkupType.class);
			boolean emptyPackages = sectionsOfPackages.isEmpty()
					// the parents markup section does not count
					|| (sectionsOfPackages.size() == 1
					&& markupSection != null
					&& sectionsOfPackages.contains(markupSection));
			if (emptyPackages) {
				String packagesString = Strings.concat(" ,", packagesToCompile);
				if (packagesToCompile.length > 1) {
					packagesString = "s '" + packagesString + "' do";
				}
				else {
					packagesString = " '" + packagesString + "' does";
				}
				Message warning = Messages.warning("The package" + packagesString + " not contain any sections.");
				Messages.storeMessage(compiler, section, getClass(), warning);
			}

		}

		@Override
		public void destroy(PackageRegistrationCompiler compiler, Section<DefaultMarkupPackageCompileType> section) {
			Messages.clearMessages(compiler, section, getClass());
		}
	}
}
