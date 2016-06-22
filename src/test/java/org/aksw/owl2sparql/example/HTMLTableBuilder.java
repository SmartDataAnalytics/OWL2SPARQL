/*
 * #%L
 * owl2sparql-core
 * %%
 * Copyright (C) 2015 - 2016 AKSW
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package org.aksw.owl2sparql.example;

/**
 * Uses:
 * HTMLTableBuilder htmlBuilder = new HTMLTableBuilder(null, true, 2, 3);
 * htmlBuilder.addTableHeader("1H", "2H", "3H");
 * htmlBuilder.addRowValues("1", "2", "3");
 * htmlBuilder.addRowValues("4", "5", "6");
 * htmlBuilder.addRowValues("9", "8", "7");
 * String table = htmlBuilder.build();
 * System.out.println(table.toString());
 */

public class HTMLTableBuilder {

	private static String HTML_HEAD =
			"<head>\n" +
					"<link rel=\"stylesheet\" href=\"https://rawgit.com/twbs/bootstrap/master/dist/css/bootstrap.min.css\">\n" +
					"<link rel=\"stylesheet\" href=\"https://rawgit.com/wenzhixin/bootstrap-table/1.8.0/dist/bootstrap-table.css\">\n" +
					"<style type=\"text/css\">\n" +
					"   pre {\n" +
					"	border: 0; \n" +
					"	background-color: transparent\n"
					+ "font-family: monospace;" +
					"	}\n"
					+ "table {\n" +
					"    border-collapse: separate;\n" +
					"    border-spacing: 0 5px;\n" +
					"}\n" +
					"\n" +
					"thead th {\n" +
					"    background-color: #006DCC;\n" +
					"    color: white;\n" +
					"}\n" +
					"\n" +
					"tbody td {\n" +
					"    background-color: #EEEEEE;\n" +
					"}\n" +
					"\n" +
					"tr td:first-child,\n" +
					"tr th:first-child {\n" +
					"    border-top-left-radius: 6px;\n" +
					"    border-bottom-left-radius: 6px;\n" +
					"}\n" +
					"\n" +
					"tr td:last-child,\n" +
					"tr th:last-child {\n" +
					"    border-top-right-radius: 6px;\n" +
					"    border-bottom-right-radius: 6px;\n" +
					"}\n" +
					".fixed-table-container tbody td {\n" +
					"    border: none;\n" +
					"}\n" +
					".fixed-table-container thead th {\n" +
					"    border: none;\n" +
					"}\n" +
					"\n" +
					".bootstrap-table .table {\n" +
					"	border-collapse: inherit !important;\n" +
					"}" +
					"</style>\n" +
					"<script src=\"http://code.jquery.com/jquery-1.11.3.min.js\"></script>\n" +
					"<script src=\"https://rawgit.com/twbs/bootstrap/master/dist/js/bootstrap.min.js\"></script>\n" +
					"<script src=\"https://rawgit.com/wenzhixin/bootstrap-table/1.8.0/dist/bootstrap-table-all.min.js\"></script>\n" +
					"</head>\n";

	public static String HTML_START = "<html>";
	public static String HTML_END = "\n</html>";
	public static String TABLE_START_BORDER = "\n<table style='border:1; border-collapse: separate; border-spacing: 0 1em;'>";
	public static String TABLE_START = "\n<table style='border:1; border-collapse: separate; border-spacing: 0 1em;'>";
	public static String TABLE_END = "\n</table>";
	public static String HEADER_START = "<th>";
	public static String HEADER_END = "</th>\n";
	public static String ROW_START = "\n<tr>";
	public static String ROW_END = "</tr>";
	public static String COLUMN_START = "<td>";
	public static String COLUMN_END = "</td>";
	private final StringBuilder table = new StringBuilder();
	private int columns;


	/**
	 * @param header
	 * @param border
	 * @param rows
	 * @param columns
	 */
	public HTMLTableBuilder(String header, boolean border, int rows, int columns) {
		this.columns = columns;
		if (header != null) {
			table.append("<b>");
			table.append(header);
			table.append("</b>");
		}
		table.append(HTML_START);
		table.append(HTML_HEAD);
		table.append(border ? TABLE_START_BORDER : TABLE_START);
		table.append(TABLE_END);
		table.append(HTML_END);
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		HTMLTableBuilder htmlBuilder = new HTMLTableBuilder(null, true, 2, 3);
		htmlBuilder.addTableHeader("1H", "2H", "3H");
		htmlBuilder.addRowValues("1", "2", "3");
		htmlBuilder.addRowValues("4", "5", "6");
		htmlBuilder.addRowValues("9", "8", "7");
		String table = htmlBuilder.build();
		System.out.println(table.toString());
	}

	/**
	 * @param values
	 */
	public void addTableHeader(String... values) {
		if (values.length != columns) {
			System.out.println("Error column length");
		} else {
			int lastIndex = table.lastIndexOf(TABLE_END);
			if (lastIndex > 0) {
				StringBuilder sb = new StringBuilder();
				sb.append(ROW_START);
				for (String value : values) {
					sb.append(HEADER_START);
					sb.append(value);
					sb.append(HEADER_END);
				}
				sb.append(ROW_END);
				table.insert(lastIndex, sb.toString());
			}
		}
	}

	/**
	 * @param values
	 */
	public void addRowValues(String... values) {
		if (values.length != columns) {
			System.out.println("Error column length");
		} else {
			int lastIndex = table.lastIndexOf(ROW_END);
			if (lastIndex > 0) {
				int index = lastIndex + ROW_END.length();
				StringBuilder sb = new StringBuilder();
				sb.append(ROW_START);
				for (String value : values) {
					sb.append(COLUMN_START);
					sb.append(value);
					sb.append(COLUMN_END);
				}
				sb.append(ROW_END);
				table.insert(index, sb.toString());
			}
		}
	}

	/**
	 * @return
	 */
	public String build() {
		return table.toString();
	}


}