package de.d3web.knowledgeExporter.tests;

import java.io.StringReader;
import java.util.List;

import junit.framework.TestCase;
import de.d3web.KnOfficeParser.SingleKBMIDObjectManager;
import de.d3web.KnOfficeParser.dashtree.QuestionnaireBuilder;
import de.d3web.KnOfficeParser.dashtree.SolutionsBuilder;
import de.d3web.KnOfficeParser.decisiontree.D3DTBuilder;
import de.d3web.KnOfficeParser.rule.D3ruleBuilder;
import de.d3web.kernel.domainModel.KnowledgeBase;
import de.d3web.kernel.domainModel.KnowledgeBaseManagement;
import de.d3web.knowledgeExporter.KnowledgeManager;
import de.d3web.report.Message;

public abstract class KnowledgeExporterTest extends TestCase {
	
	protected KnowledgeBase kb;
	protected KnowledgeManager manager;
	
//	private InputStream getStream(String ressource) {
//		InputStream stream;
//		try {
//			stream = new ByteArrayInputStream(ressource.getBytes("UTF-8"));
//		} catch (UnsupportedEncodingException e1) {
//			e1.printStackTrace();
//			stream = null;
//		}
//
//		return stream;
//	}
	
	protected void setUpKB(String diagnosis, String initQuestion,
			String decisionTree, String rules, String xcl) {
		
		KnowledgeBaseManagement kbm = KnowledgeBaseManagement.createInstance();
		kb = kbm.getKnowledgeBase();
		
		if (initQuestion != null) {
			List<de.d3web.report.Message> messages = QuestionnaireBuilder
					.parse(new StringReader(initQuestion), new SingleKBMIDObjectManager(kbm));
		}
		
		if (diagnosis != null) {
			List<de.d3web.report.Message> messages = SolutionsBuilder
				.parse(new StringReader(diagnosis), kbm, new SingleKBMIDObjectManager(kbm));
		}
		
		if (decisionTree != null) {
			List<de.d3web.report.Message> messages = D3DTBuilder.parse(new StringReader(decisionTree), new SingleKBMIDObjectManager(kbm));
		}
		
		if (rules != null) {
			D3ruleBuilder builder = new D3ruleBuilder("", false,
					new SingleKBMIDObjectManager(kbm));
			
			List<Message> bm = builder.addKnowledge(new StringReader(rules),
					new SingleKBMIDObjectManager(kbm), null);
		}
		
		
		setUpWriter();

		
	}
	
	protected void setUpKB2(String[] diagnosis, String[] initQuestion,
			String[] decisionTreeFormatted) {
		// TODO Auto-generated method stub
		
	}
	
	protected abstract void setUpWriter();
	


}
