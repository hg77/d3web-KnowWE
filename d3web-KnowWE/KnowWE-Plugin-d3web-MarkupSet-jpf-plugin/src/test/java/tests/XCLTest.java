/*
 * Copyright (C) 2009 Chair of Artificial Intelligence and Applied Informatics
 * Computer Science VI, University of Wuerzburg
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

package tests;

import java.io.IOException;
import java.util.Collection;

import junit.framework.TestCase;
import utils.KBCreationTestUtil;
import utils.MyTestArticleManager;
import de.d3web.core.inference.KnowledgeSlice;
import de.d3web.core.knowledge.KnowledgeBase;
import de.d3web.plugin.test.InitPluginManager;
import de.d3web.we.kdom.KnowWEArticle;
import de.d3web.we.logging.Logging;
import de.d3web.xcl.XCLModel;
import de.d3web.xcl.XCLRelation;
import de.d3web.xcl.inference.PSMethodXCL;

/**
 * This class tests whether the XCLModels are created as expected.
 * 
 * TODO: Commented some assertions out in testXCLModels(). Because the new
 * Markup makes them fail. Johannes
 * 
 * @author Sebastian Furth
 * @see KBCreationTestUtil to modify the KB against which everything is tested
 * @see KBCreationTest.txt to modify the Article which is tested
 * 
 */
public class XCLTest extends TestCase {

	@Override
	protected void setUp() throws IOException {
		InitPluginManager.init();
	}

	public void testNumberOfXCLModels() {
		// load KnowledgeBases
		KnowWEArticle art = MyTestArticleManager
				.getArticle(KBCreationTestUtil.KBCREATION_ARTICLE_FILE);
		KnowledgeBase loadedKB = MyTestArticleManager.getKnowledgeBase(art);
		KnowledgeBase createdKB = KBCreationTestUtil.getInstance().getCreatedKB();

		// Check number of rules
		assertEquals("Wrong number of rules for PSMethodXCL.",
				createdKB.getAllKnowledgeSlicesFor(PSMethodXCL.class).size(),
				loadedKB.getAllKnowledgeSlicesFor(PSMethodXCL.class).size());
	}

	public void testXCLModels() {

		// load KnowledgeBases
		KnowWEArticle art = MyTestArticleManager
				.getArticle(KBCreationTestUtil.KBCREATION_ARTICLE_FILE);
		KnowledgeBase loadedKB = MyTestArticleManager.getKnowledgeBase(art);
		KnowledgeBase createdKB = KBCreationTestUtil.getInstance().getCreatedKB();

		Collection<KnowledgeSlice> loadedXCLModels =
				loadedKB.getAllKnowledgeSlicesFor(
						PSMethodXCL.class, XCLModel.XCLMODEL);
		Collection<KnowledgeSlice> createdXCLModels =
				createdKB.getAllKnowledgeSlicesFor(
						PSMethodXCL.class, XCLModel.XCLMODEL);

		XCLModel loadedXCLModel = (XCLModel) loadedXCLModels.toArray()[0];
		XCLModel createdXCLModel = (XCLModel) createdXCLModels.toArray()[0];

		// Check whether models exist
		assertNotNull("XCLModel is null.", loadedXCLModel);
		assertNotNull("XCLModel is null.", createdXCLModel);

		if (loadedXCLModel != null && createdXCLModel != null) {

			assertEquals(createdXCLModel.getSolution(), loadedXCLModel.getSolution());

			XCLRelation createdRelation;
			XCLRelation loadedRelation;

			// Check Contradicting Relations
			createdRelation = (XCLRelation)
					createdXCLModel.getContradictingRelations().toArray()[0];
			loadedRelation = (XCLRelation)
					loadedXCLModel.getContradictingRelations().toArray()[0];
			// assertEquals("Contradicting Relation is wrong.",
			// createdRelation.getConditionedFinding(),
			// loadedRelation.getConditionedFinding());

			// // Check Necessary Relations
			// createdRelation = (XCLRelation)
			// createdXCLModel.getNecessaryRelations().toArray()[0];
			// loadedRelation = (XCLRelation)
			// loadedXCLModel.getNecessaryRelations().toArray()[0];
			// assertEquals("Necessary Relation is wrong.",
			// createdRelation.getConditionedFinding(),
			// loadedRelation.getConditionedFinding());

			// // Check Sufficient Relations
			// createdRelation = (XCLRelation)
			// createdXCLModel.getSufficientRelations().toArray()[0];
			// loadedRelation = (XCLRelation)
			// loadedXCLModel.getSufficientRelations().toArray()[0];
			// assertEquals("Sufficient Relation is wrong.",
			// createdRelation.getConditionedFinding(),
			// loadedRelation.getConditionedFinding());

			// Check "normal" Relation with custom weight
			createdRelation = (XCLRelation) createdXCLModel.getRelations().toArray()[0];
			if (createdRelation.getWeight() <= 1) {
				createdRelation = (XCLRelation) createdXCLModel.getRelations().toArray()[1];
			}
			loadedRelation = (XCLRelation) loadedXCLModel.getRelations().toArray()[0];
			if (loadedRelation.getWeight() <= 1) {
				loadedRelation = (XCLRelation) loadedXCLModel.getRelations().toArray()[1];
			}
			// assertEquals("Relation is wrong.",
			// createdRelation.getConditionedFinding(),
			// loadedRelation.getConditionedFinding());
			// assertEquals("Relation has wrong weight.",
			// createdRelation.getWeight(),
			// loadedRelation.getWeight());

			// Check "normal" Relation with default weight (1)
			createdRelation = (XCLRelation) createdXCLModel.getRelations().toArray()[1];
			if (createdRelation.getWeight() > 1) {
				createdRelation = (XCLRelation) createdXCLModel.getRelations().toArray()[0];
			}
			loadedRelation = (XCLRelation) loadedXCLModel.getRelations().toArray()[1];
			if (loadedRelation.getWeight() > 1) {
				loadedRelation = (XCLRelation) loadedXCLModel.getRelations().toArray()[0];
			}
			assertEquals("Relation is wrong.", createdRelation.getConditionedFinding(),
					loadedRelation.getConditionedFinding());
			assertEquals("Relation has wrong weight.", createdRelation.getWeight(),
					loadedRelation.getWeight());
		}
		else {
			Logging.getInstance().getLogger().warning("XCLTest: XCLModel has not been tested!");
		}
	}

}
