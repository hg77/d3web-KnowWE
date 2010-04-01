package de.d3web.we.kdom.questionTreeNew;

import de.d3web.core.inference.Rule;
import de.d3web.core.inference.condition.Condition;
import de.d3web.core.knowledge.terminology.QContainer;
import de.d3web.core.manage.KnowledgeBaseManagement;
import de.d3web.core.manage.RuleFactory;
import de.d3web.we.d3webModule.D3webModule;
import de.d3web.we.kdom.DefaultAbstractKnowWEObjectType;
import de.d3web.we.kdom.KnowWEArticle;
import de.d3web.we.kdom.ReviseSubTreeHandler;
import de.d3web.we.kdom.Section;
import de.d3web.we.kdom.dashTree.DashTreeElement;
import de.d3web.we.kdom.report.KDOMError;
import de.d3web.we.kdom.report.NoSuchObjectError;
import de.d3web.we.kdom.objects.QuestionnaireDef;
import de.d3web.we.kdom.renderer.FontColorRenderer;
import de.d3web.we.kdom.report.CreateRelationFailed;
import de.d3web.we.kdom.report.KDOMReportMessage;
import de.d3web.we.kdom.report.ObjectCreatedMessage;
import de.d3web.we.kdom.sectionFinder.AllTextFinderTrimmed;
import de.d3web.we.utils.KnowWEObjectTypeUtils;

public class IndicationLine extends DefaultAbstractKnowWEObjectType {

	@Override
	protected void init() {
		this.sectionFinder = AllTextFinderTrimmed.getInstance();

		QuestionnaireDef qc = new QuestionnaireDef();
		qc.setCustomRenderer(new FontColorRenderer(FontColorRenderer.COLOR7));
		qc.setSectionFinder(AllTextFinderTrimmed.getInstance());
		qc.addReviseSubtreeHandler(new CreateIndication());
		this.childrenTypes.add(qc);
	}

	static class CreateIndication implements ReviseSubTreeHandler {

		@Override
		public KDOMReportMessage reviseSubtree(KnowWEArticle article, Section s) {
			KnowledgeBaseManagement mgn = D3webModule
					.getKnowledgeRepresentationHandler(article.getWeb())
					.getKBM(article, s);

			Section<QuestionnaireDef> indicationSec = (s);

			// current DashTreeElement
			Section<DashTreeElement> element = KnowWEObjectTypeUtils
					.getAncestorOfType(s, new DashTreeElement());

			String name = indicationSec.get().getID(indicationSec);

			QContainer qc = mgn.findQContainer(name);

			if (qc != null) {
				String newRuleID = mgn.createRuleID();
				Condition cond = Utils.createCondition(DashTreeElement.getDashTreeAncestors(element));
				if (cond != null) {
					Rule r = RuleFactory.createIndicationRule(newRuleID, qc,
							cond);
					if (r != null) {
						return new ObjectCreatedMessage(r.getClass() + " : "
								+ r.getId());
					}
					
				}
				return new CreateRelationFailed(Rule.class
						.getSimpleName());
			} else {

				KDOMError.storeError(s, new NoSuchObjectError(name));
			}
			return null;
		}

	}
}
