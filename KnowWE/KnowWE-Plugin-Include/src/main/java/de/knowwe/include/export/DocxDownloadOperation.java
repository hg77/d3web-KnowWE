/*
 * Copyright (C) 2014 denkbares GmbH
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
package de.knowwe.include.export;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import de.d3web.core.io.progress.ProgressListener;
import de.knowwe.core.action.UserActionContext;
import de.knowwe.core.kdom.Article;
import de.knowwe.core.kdom.parsing.Section;
import de.knowwe.core.report.Message;
import de.knowwe.core.report.Messages;
import de.knowwe.core.utils.KnowWEUtils;
import de.knowwe.core.utils.progress.FileDownloadOperation;
import de.knowwe.include.IncludeMarkup;
import de.knowwe.kdom.defaultMarkup.AnnotationContentType;
import de.knowwe.kdom.defaultMarkup.DefaultMarkupType;
import de.knowwe.tools.DefaultTool;
import de.knowwe.tools.Tool;

/**
 * 
 * @author Volker Belli (denkbares GmbH)
 * @created 07.02.2014
 */
public class DocxDownloadOperation extends FileDownloadOperation {

	private final Section<?> section;
	private ExportManager export = null;

	public DocxDownloadOperation(Section<?> section) {
		super(section.getArticle(), section.getTitle() + ".docx");
		this.section = section;
	}

	@Override
	public void before(UserActionContext user) throws IOException {
		super.before(user);
		this.export = new ExportManager(section);

		// check read access for all articles
		for (Article article : export.getIncludedArticles()) {
			if (!KnowWEUtils.canView(article, user)) {
				addMessage(Messages.error(
						"User is not allowed to view article '" +
								article.getTitle() + "'"));
			}
		}

		// increase version if available and required
		// export.incVersionIfRequired(user);
		Article article = user.getArticle();
		if (KnowWEUtils.getLastModified(article).before(export.getLastModified())) {
			addMessage(Messages.warning("The version number appears to be out-dated. " +
					"Please update and download the word file again."));
		}
	}

	@Override
	public List<Tool> getActions(UserActionContext context) {
		List<Tool> actions = super.getActions(context);
		if (export == null) return actions;
		if (hasError()) return actions;
		if (!export.isNewVersionRequired()) return actions;

		// check if next version is available
		String nextVersion = getNextVersion();
		if (nextVersion == null) return actions;

		// create update tool action
		String versionSectionID = DefaultMarkupType.getAnnotationContentSection(section,
				IncludeMarkup.ANNOTATION_VERSION).getID();
		String jsAction = "KNOWWE.plugin.include.updateVersion(" +
				"'" + versionSectionID + "', " + nextVersion + ");";

		// make a copy and add update tool
		actions = new LinkedList<Tool>(actions);
		actions.add(new DefaultTool(
				"KnowWEExtension/images/pencil.png",
				"Update Version to " + nextVersion,
				"Increments the version number of the document to be downloaded.",
				jsAction));
		return actions;
	}

	@Override
	public String getFileIcon() {
		return "KnowWEExtension/icons/word.png";
	}

	/**
	 * Creates the increased version number. The method return null if no
	 * updated number can be provided.
	 * 
	 * @created 16.02.2014
	 * @return the next version number
	 */
	private String getNextVersion() {
		// if we passed the previous check, we know there is a version available
		Section<? extends AnnotationContentType> versionSection =
				DefaultMarkupType.getAnnotationContentSection(section,
						IncludeMarkup.ANNOTATION_VERSION);
		if (versionSection == null) return null;

		// if update is required inc version and write back to wiki
		try {
			int version = Integer.parseInt(versionSection.getText());
			return String.valueOf(version + 1);
		}
		catch (NumberFormatException e) {
			return null;
		}
	}

	@Override
	public void execute(File resultFile, ProgressListener listener) throws IOException, InterruptedException {
		if (hasError()) return;
		FileOutputStream stream = new FileOutputStream(resultFile);
		try {
			ExportModel model = export.createExport();
			for (Message message : model.getMessages()) {
				addMessage(message);
			}
			model.getDocument().write(stream);
		}
		catch (ExportException e) {
			addMessage(Messages.error(e.getMessage()));
		}
		finally {
			stream.close();
		}
	}

}