package de.d3web.we.kdom.rules.action;

import de.d3web.we.kdom.action.SingleAction;
import de.d3web.we.kdom.rules.RuleContainerFinder;
import de.d3web.we.kdom.rules.RuleTokenType;
import de.d3web.we.kdom.rules.RuleType;
import de.knowwe.core.kdom.AbstractType;
import de.knowwe.core.kdom.TypePriorityList;

/**
 * Created by Albrecht Striffler (denkbares GmbH) on 02.03.14.
 */
public class ActionContainer extends AbstractType {

	public ActionContainer(String... tokens) {
		setSectionFinder(new RuleContainerFinder(tokens, RuleType.INNER_TOKENS));
		addChildType(TypePriorityList.DEFAULT_PRIORITY - 1, new RuleTokenType(tokens));
		addChildType(new SingleAction(new RuleAction()));
	}
}