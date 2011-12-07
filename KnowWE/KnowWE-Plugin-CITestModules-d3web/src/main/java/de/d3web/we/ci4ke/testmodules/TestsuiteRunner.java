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

package de.d3web.we.ci4ke.testmodules;

import de.d3web.empiricaltesting.TestCase;
import de.d3web.empiricaltesting.caseAnalysis.functions.TestCaseAnalysis;
import de.d3web.empiricaltesting.caseAnalysis.functions.TestCaseAnalysisReport;
import de.d3web.we.ci4ke.testing.AbstractCITest;
import de.d3web.we.ci4ke.testing.CITestResult;
import de.d3web.we.ci4ke.testing.CITestResult.Type;
import de.d3web.we.testcase.TestCaseUtils;
import de.knowwe.core.KnowWEEnvironment;

public class TestsuiteRunner extends AbstractCITest {

	@Override
	public CITestResult call() {

		if (!checkIfParametersAreSufficient(1)) {
			return numberOfParametersNotSufficientError(1);
		}
		String monitoredArticleTitle = getParameter(0);
		String config = "article: " + monitoredArticleTitle;

		TestCase suite = TestCaseUtils.loadTestSuite(
				monitoredArticleTitle, KnowWEEnvironment.DEFAULT_WEB);

		if (suite == null) {
			return new CITestResult(Type.ERROR, "No Testsuite found in article '"
					+ monitoredArticleTitle + "'", config);
		}

		TestCaseAnalysis analysis = new TestCaseAnalysis();
		TestCaseAnalysisReport result = analysis.runAndAnalyze(suite);

		if (suite != null) {
			if (!suite.isConsistent()) {
				return new CITestResult(Type.FAILED, "Testsuite is not consistent!", config);
			}
			else if (result.recall() == 1.0 && result.precision() == 1.0) {
				return new CITestResult(Type.SUCCESSFUL, null, config);
			}
			else {
				return new CITestResult(Type.FAILED,
						"Testsuite failed! (Total Precision: " + result.precision() +
								", Total Recall: " + result.recall() + ")", config);
			}
		}
		else {
			return new CITestResult(Type.ERROR,
					"Error while retrieving Testsuite from Article '" +
							monitoredArticleTitle + "' !", config);
		}

	}

}
