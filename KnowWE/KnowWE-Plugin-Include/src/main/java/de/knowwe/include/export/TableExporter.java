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

import java.util.HashMap;
import java.util.Map;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTShd;

import de.d3web.utils.Pair;
import de.knowwe.core.kdom.parsing.Section;
import de.knowwe.core.kdom.parsing.Sections;
import de.knowwe.instantedit.table.WikiTable;
import de.knowwe.jspwiki.types.ParagraphTypeForLists;
import de.knowwe.jspwiki.types.TableCell;
import de.knowwe.jspwiki.types.TableRow;

/**
 * Exports a wiki table to word document
 * 
 * @author Volker Belli (denkbares GmbH)
 * @created 07.02.2014
 */
public class TableExporter implements Exporter<WikiTable> {

	@Override
	public boolean canExport(Section<WikiTable> section) {
		return true;
	}

	@Override
	public Class<WikiTable> getSectionType() {
		return WikiTable.class;
	}

	@Override
	public void export(Section<WikiTable> section, DocumentBuilder manager) throws ExportException {
		// initialize table for easier access
		Matrix<Section<TableCell>> matrix = toMatrix(section);

		// create table with correct dimension
		XWPFDocument doc = manager.getDocument();
		XWPFTable table = doc.createTable(matrix.getRowSize(), matrix.getColSize());

		for (int row = 0; row < matrix.getRowSize(); row++) {
			for (int col = 0; col < matrix.getColSize(); col++) {
				Section<TableCell> cell = matrix.get(row, col);
				boolean isHeader = cell.get().isHeader(cell);
				boolean isZebra = row % 2 == 0;

				// prepare cell shading
				XWPFTableCell tableCell = table.getRow(row).getCell(col);
				CTShd shade = tableCell.getCTTc().addNewTcPr().addNewShd();
				if (isHeader) {
					shade.setFill("D0D0D0");
				}
				else if (isZebra) {
					shade.setFill("F2F2F2");
				}

				// fill cell contents
				DocumentBuilder cellBuilder = new CellBuilder(manager, tableCell, isHeader);
				Section<?> content = Sections.successor(cell, ParagraphTypeForLists.class);
				cellBuilder.export(content);
			}
		}

		// append empty line after each table
		manager.closeParagraph();
		manager.append("\n\r");
		manager.closeParagraph();
	}

	private Matrix<Section<TableCell>> toMatrix(Section<WikiTable> section) {
		Matrix<Section<TableCell>> matrix = new Matrix<Section<TableCell>>();
		int row = 0;
		for (Section<TableRow> tableRow : Sections.successors(section, TableRow.class)) {
			int col = 0;
			for (Section<TableCell> cell : Sections.successors(tableRow, TableCell.class)) {
				matrix.set(row, col, cell);
				col++;
			}
			row++;
		}
		return matrix;
	}

	public static class Matrix<E> {

		private final Map<Pair<Integer, Integer>, E> elements =
				new HashMap<Pair<Integer, Integer>, E>();
		private int rows = 0;
		private int cols = 0;

		/**
		 * Replaces the element at the specified position in this matrix with
		 * the specified element (optional operation).
		 * 
		 * @param row row of the element to replace
		 * @param col col of the element to replace
		 * @param element element to be stored at the specified position
		 * @return the element previously at the specified position
		 */
		public E set(int row, int col, E element) {
			rows = Math.max(rows, row + 1);
			cols = Math.max(cols, col + 1);
			return elements.put(getKey(row, col), element);
		}

		public E get(int row, int col) {
			return elements.get(getKey(row, col));
		}

		public int getRowSize() {
			return rows;
		}

		public int getColSize() {
			return cols;
		}

		private Pair<Integer, Integer> getKey(int row, int col) {
			if (row < 0) throw new IndexOutOfBoundsException("row must not be negative");
			if (col < 0) throw new IndexOutOfBoundsException("col must not be negative");
			return new Pair<Integer, Integer>(row, col);
		}
	}
}