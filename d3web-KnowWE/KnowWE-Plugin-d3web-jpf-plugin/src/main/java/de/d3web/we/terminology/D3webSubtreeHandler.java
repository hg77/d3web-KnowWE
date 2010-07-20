/*
 * Copyright (C) 2009 Chair of Artificial Intelligence and Applied Informatics
 *                    Computer Science VI, University of Wuerzburg
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 3 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package de.d3web.we.terminology;

import de.d3web.core.manage.KnowledgeBaseManagement;
import de.d3web.we.d3webModule.D3webModule;
import de.d3web.we.kdom.KnowWEArticle;
import de.d3web.we.kdom.KnowWEObjectType;
import de.d3web.we.kdom.Section;
import de.d3web.we.kdom.objects.KnowWETerm;
import de.d3web.we.kdom.subtreeHandler.SubtreeHandler;
import de.d3web.we.utils.KnowWEUtils;

public abstract class D3webSubtreeHandler<T extends KnowWEObjectType> extends SubtreeHandler<T> {

	/**
	 * @param article is the article you need the KBM from
	 * @returns the KBM for the given article
	 */
	protected KnowledgeBaseManagement getKBM(KnowWEArticle article) {
		return D3webModule.getKnowledgeRepresentationHandler(article.getWeb()).getKBM(
				article.getTitle());
	}

	/*
	 * Checking for a Section with an KnowWEObjectType extending KnowWETerm is
	 * necessary for the compatibility with KnowWEObjectTypes that do not use
	 * KnowWETerms. So if you use them in the SubtreeHandler for your
	 * KnowWEObjectType, be sure to extend KnowWETerm.
	 */
	@Override
	public boolean needsToCreate(KnowWEArticle article, Section<T> s) {
		return super.needsToCreate(article, s)
				|| (!(s.get() instanceof KnowWETerm<?>)
						&& KnowWEUtils.getTerminologyHandler(article.getWeb())
								.areTermDefinitionsModifiedFor(article));
	}

	/*
	 * Checking for a Section with an KnowWEObjectType extending KnowWETerm is
	 * necessary for the compatibility with KnowWEObjectTypes that do not use
	 * KnowWETerms. So if you use them in the SubtreeHandler for your
	 * KnowWEObjectType, be sure to extend KnowWETerm.
	 */
	@Override
	public boolean needsToDestroy(KnowWEArticle article, Section<T> s) {
		return super.needsToDestroy(article, s)
				|| (!(s.get() instanceof KnowWETerm<?>)
						&& KnowWEUtils.getTerminologyHandler(article.getWeb())
								.areTermDefinitionsModifiedFor(article));
	}

	@Override
	public void destroy(KnowWEArticle article, Section<T> s) {
		article.setFullParse(true, this);
		return;
	}

}
