package de.knowwe.ontology.turtlePimped.compile;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import de.knowwe.core.kdom.Article;
import de.knowwe.core.kdom.parsing.Section;
import de.knowwe.core.kdom.parsing.Sections;
import de.knowwe.core.kdom.subtreeHandler.SubtreeHandler;
import de.knowwe.core.report.Message;
import de.knowwe.ontology.turtlePimped.TurtleContent;
import de.knowwe.rdf2go.Rdf2GoCore;

public class TurtleCompileHandler extends SubtreeHandler<TurtleContent> {

	@SuppressWarnings({
			"unchecked", "rawtypes" })
	@Override
	public Collection<Message> create(Article article, Section<TurtleContent> section) {

		List<Message> messages = new ArrayList<Message>();

		Rdf2GoCore core = Rdf2GoCore.getInstance(article);

		List<Section<StatementProvider>> statementProviders = Sections.findSuccessorsOfType(
				section, StatementProvider.class);
		for (Section<StatementProvider> statementSection : statementProviders) {

			StatementProviderResult providerResult = statementSection.get().getStatements(
					statementSection, core);
			if (providerResult != null) {
				core.addStatements(statementSection,
						providerResult.getStatments());
				messages.addAll(providerResult.getMessages());
			}

		}
		return messages;
	}

}
