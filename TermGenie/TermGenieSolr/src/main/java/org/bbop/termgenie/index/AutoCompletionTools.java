package org.bbop.termgenie.index;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Tools for auto-completion.
 */
public abstract class AutoCompletionTools<T> {

	/**
	 * Split the string into tokens using white spaces. 
	 * Ignore multiple white spaces (begin, end, and in-between)
	 * 
	 * @param s
	 * @return list of non-white space sub strings
	 */
	public static List<String> split(String s) {
		if (s.isEmpty()) {
			return Collections.emptyList();
		}
		
		int start = -1;
		for (int i = 0; i < s.length(); i++) {
			if (!Character.isWhitespace(s.charAt(i))) {
				start = i;
				break;
			}
		}
		if (start < 0) {
			return Collections.emptyList();
		}
		List<String> tokens = new ArrayList<String>();
		for (int i = start; i < s.length(); i++) {
			if (Character.isWhitespace(s.charAt(i))) {
				if (start >= 0) {
					tokens.add(s.substring(start, i));
					start = -1;
				}
			}
			else {
				if (start < 0) {
					start = i;
				}
			}
		}
		if (start >= 0) {
			tokens.add(s.substring(start, s.length()));
		}
		return tokens;
	}
	
	public String preprocessQuery(String queryString) {
		queryString = escape(queryString);
		StringBuilder subquery1 = new StringBuilder();
		StringBuilder subquery2 = new StringBuilder();
		List<String> list = AutoCompletionTools.split(queryString);
		for (String string : list) {
			if (subquery1.length() == 0) {
				subquery1.append('(');
				subquery2.append("(\"");
			}
			else{
				subquery1.append(" AND ");
				subquery2.append(' ');
			}
			subquery1.append(string);
			subquery2.append(string);
			subquery1.append('*');
		}
		subquery1.append(')');
		subquery2.append("\"^2)");
		
		StringBuilder sb = new StringBuilder(subquery1);
		sb.append(" OR ");
		sb.append(subquery2);
		return sb.toString();
	}
	
	protected abstract String escape(String string);
	
	public void sortbyLabelLength(List<T> documents) {
		Collections.sort(documents, new Comparator<T>() {

			@Override
			public int compare(T o1, T o2) {
				final String label1 = getLabel(o1);
				final String label2 = getLabel(o2);
				int l1 = label1.length();
				int l2 = label2.length();
				return (l1 < l2 ? -1 : (l1 == l2 ? 0 : 1));
			}
		});
	}
	
	public static boolean fEquals(float f1, float f2) {
		return Math.abs(f1 - f2) < 0.0001f;
	}
	
	protected abstract String getLabel(T t);
}
