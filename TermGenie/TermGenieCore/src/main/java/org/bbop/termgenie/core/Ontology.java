package org.bbop.termgenie.core;

import java.util.List;

/**
 * Wrapper of an ontology, provides additional methods for identifying the
 * ontology.
 */
public final class Ontology {

	private String name;
	private List<String> roots;
	private String dlQuery;
	private String source;
	private List<String> additionals;
	private List<OntologySubset> subsets;

	/**
	 * Default constructor. Used in parsing.
	 */
	public Ontology() {
		super();
	}

	/**
	 * @param name
	 * @param source
	 * @param roots
	 * @param dlQuery
	 * @param additionals
	 * @param subsets
	 */
	public Ontology(String name,
			String source,
			List<String> roots,
			String dlQuery,
			List<String> additionals,
			List<OntologySubset> subsets)
	{
		super();
		this.name = name;
		this.source = source;
		this.roots = roots;
		this.dlQuery = dlQuery;
		this.additionals = additionals;
		this.subsets = subsets;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @return the roots
	 */
	public List<String> getRoots() {
		return roots;
	}
	
	/**
	 * @param roots the roots to set
	 */
	public void setRoots(List<String> roots) {
		this.roots = roots;
	}
	
	/**
	 * @return the dlQuery
	 */
	public String getDlQuery() {
		return dlQuery;
	}
	
	/**
	 * @param dlQuery the dlQuery to set
	 */
	public void setDlQuery(String dlQuery) {
		this.dlQuery = dlQuery;
	}

	/**
	 * @return the source
	 */
	public String getSource() {
		return source;
	}
	
	/**
	 * @param source the source to set
	 */
	public void setSource(String source) {
		this.source = source;
	}
	
	/**
	 * @return the additionals
	 */
	public List<String> getAdditionals() {
		return additionals;
	}
	
	/**
	 * @param additionals the additionals to set
	 */
	public void setAdditionals(List<String> additionals) {
		this.additionals = additionals;
	}
	
	/**
	 * @return the subsets
	 */
	public List<OntologySubset> getSubsets() {
		return subsets;
	}
	
	/**
	 * @param subsets the subsets to set
	 */
	public void setSubsets(List<OntologySubset> subsets) {
		this.subsets = subsets;
	}


	public static class OntologySubset {
		
		private String name;
		private List<String> roots;
		private String dlQuery;
		
		/**
		 * @param name
		 * @param roots
		 * @param dlQuery
		 */
		public OntologySubset(String name, List<String> roots, String dlQuery) {
			super();
			this.name = name;
			this.roots = roots;
			this.dlQuery = dlQuery;
		}

		
		/**
		 * @return the name
		 */
		public String getName() {
			return name;
		}

		
		/**
		 * @return the roots
		 */
		public List<String> getRoots() {
			return roots;
		}
		
		/**
		 * @return the dlQuery
		 */
		public String getDlQuery() {
			return dlQuery;
		}

		/**
		 * @param name the name to set
		 */
		public void setName(String name) {
			this.name = name;
		}
		
		/**
		 * @param roots the roots to set
		 */
		public void setRoots(List<String> roots) {
			this.roots = roots;
		}
		
		/**
		 * @param dlQuery the dlQuery to set
		 */
		public void setDlQuery(String dlQuery) {
			this.dlQuery = dlQuery;
		}
	}
}
