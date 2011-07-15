package org.bbop.termgenie.index;

import static org.bbop.termgenie.index.AutoCompletionTools.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.PerFieldAnalyzerWrapper;
import org.apache.lucene.analysis.WhitespaceAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Index;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.FieldSelector;
import org.apache.lucene.document.FieldSelectorResult;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;
import org.bbop.termgenie.core.rules.ReasonerFactory;
import org.bbop.termgenie.core.rules.ReasonerTaskManager;
import org.bbop.termgenie.ontology.DefaultOntologyConfiguration;
import org.bbop.termgenie.ontology.DefaultOntologyConfiguration.ConfiguredOntology;
import org.bbop.termgenie.ontology.DefaultOntologyLoader;
import org.bbop.termgenie.ontology.OntologyTaskManager;
import org.bbop.termgenie.ontology.OntologyTaskManager.OntologyTask;
import org.bbop.termgenie.tools.Pair;
import org.semanticweb.owlapi.model.OWLObject;

import owltools.graph.OWLGraphWrapper;

/**
 * Basic auto-completion using an in-memory lucene index.
 */
public class LuceneMemoryOntologyIndex {

	private static final Logger logger = Logger.getLogger(LuceneMemoryOntologyIndex.class);
	
	private static final Version version = Version.LUCENE_31;
	private static final String DEFAULT_FIELD = "label";
	private static final String BRANCH_FIELD = "branch";
	private static final String ID_FIELD = "id";
	private static final FieldSelector FIELD_SELECTOR = new FieldSelector() {
		
		private static final long serialVersionUID = 139300915748750525L;

		@Override
		public FieldSelectorResult accept(String fieldName) {
			// only load the id field
			if (ID_FIELD.equals(fieldName)) {
				return FieldSelectorResult.LOAD;
			}
			return FieldSelectorResult.NO_LOAD;
		}
	};
	
	private final AutoCompletionTools<SearchResult> tools = new AutoCompletionTools<SearchResult>() {
		
		@Override
		protected String getLabel(SearchResult t) {
			return ontology.getLabel(t.hit);
		}
		
		@Override
		protected String escape(String string) {
			return QueryParser.escape(string);
		}
	};
	
	private final OWLGraphWrapper ontology;
	private final Analyzer analyzer;
	private final IndexSearcher searcher;
	
	/**
	 * @param ontology
	 */
	public LuceneMemoryOntologyIndex(OWLGraphWrapper ontology, String root, List<Pair<String, String>> branches) throws IOException {
		super();
		if (logger.isInfoEnabled()) {
			StringBuilder message = new StringBuilder();
			message.append("Start creating lucene memory index for: ");
			message.append(ontology.getOntologyId());
			if (root != null) {
				message.append(" Root: ");
				message.append(root);
			}
			if (branches != null && !branches.isEmpty()) {
				message.append(" Branches: ");
				for(Pair<String, String> branch : branches) {
					message.append(" (");
					message.append(branch.getOne());
					message.append(",");
					message.append(branch.getTwo());
					message.append(") ");
				}
			}
			logger.info(message.toString());	
		}
		this.ontology = ontology;
		if (branches == null || branches.isEmpty()) {
			analyzer = new StandardAnalyzer(version);
		} else {
			Map<String, Analyzer> alternatives =  Collections.<String, Analyzer>singletonMap(BRANCH_FIELD, new WhitespaceAnalyzer(version));
			analyzer = new PerFieldAnalyzerWrapper(new StandardAnalyzer(version), alternatives);
		}
		
		RAMDirectory directory = new RAMDirectory();
		IndexWriterConfig conf = new IndexWriterConfig(version, analyzer);
		IndexWriter writer = new IndexWriter(directory, conf);
		Set<OWLObject> allOWLObjects;
		if (root == null) {
			allOWLObjects = this.ontology.getAllOWLObjects();
		}
		else {
			OWLObject x = this.ontology.getOWLObjectByIdentifier(root);
			if (x == null) {
				throw new RuntimeException("Error: could not find root term with id: "+root);
			}
			allOWLObjects = this.ontology.getDescendantsReflexive(x);
		}
		
		Map<OWLObject, Set<String>> branchInfo = new HashMap<OWLObject, Set<String>>();
		ReasonerTaskManager taskManager = ReasonerFactory.getDefaultTaskManager(ontology);
		for(Pair<String, String> branch : branches) {
			String name = branch.getOne();
			String id = branch.getTwo();
			OWLObject owlObject = this.ontology.getOWLObjectByIdentifier(id);
			if (owlObject == null) {
				throw new RuntimeException("Cannot create branch "+branch+" for unknown id: "+id);
			}
			add(owlObject, name, branchInfo);
			Collection<OWLObject> descendants = taskManager.getDescendants(owlObject, this.ontology);
			if (logger.isInfoEnabled()) {
				logger.info("Adding branch (" + name + "," + id + ") with " + descendants.size()
						+ " descendants");
			}
			for (OWLObject descendant : descendants) {
				add(descendant, name, branchInfo);
			}
		}
		
		int npeCounter = 0;
		
		for (OWLObject owlObject : allOWLObjects) {
			String value = this.ontology.getLabel(owlObject);
			if (value != null) {
				Document doc  = new Document();
				doc.add(new Field(DEFAULT_FIELD, value, Store.NO, Index.ANALYZED));
				try {
					String identifier = this.ontology.getIdentifier(owlObject);
					doc.add(new Field(ID_FIELD, identifier, Store.YES, Index.NOT_ANALYZED));
					Set<String> branchSet = branchInfo.get(owlObject);
					if (branchSet != null) {
						for (String branch : branchSet) {
							doc.add(new Field(BRANCH_FIELD, branch, Store.NO, Index.NOT_ANALYZED));						
						}
					}
					writer.addDocument(doc);
				} catch (NullPointerException exception) {
					// this happens for relationships
					// TODO Try to ignore relationships, as silently ignoring exceptions is an anti-pattern
					npeCounter += 1;
					logger.error("NPE for getting an ID for: "+owlObject);
				}
			}
						
		}
		
		writer.optimize();
		writer.close();
		
		searcher = new IndexSearcher(directory);
		if (logger.isInfoEnabled()) {
			logger.info("Finished creating index for: "+ontology.getOntologyId());
			if (npeCounter > 0) {
				logger.info("During the index creation there were "+npeCounter+" NPEs");
			}
		}
	}
	
	private void add(OWLObject x, String branch, Map<OWLObject, Set<String>> branchInfo) {
		Set<String> branches = branchInfo.get(x);
		if (branches == null) {
			branchInfo.put(x, Collections.singleton(branch));
		}
		else {
			if (!branches.contains(branch)) {
				// logger.info("Term "+ontology.getIdentifier(x)+" with multiple branches: "+branch+" "+branches);
				if (branches.size() == 1) {
					branches = new HashSet<String>(branches);
					branchInfo.put(x, branches);
				}
				branches.add(branch);
			}
		}
	}
	
	public Collection<SearchResult> search(String queryString, int maxCount, String branch) {
		try {
			if (queryString == null || queryString.isEmpty() || maxCount < 1) {
				// no empty string search
				// max count minimum value: 1
				return Collections.emptyList();
			}
			queryString = queryString.trim();
			if (queryString.length() <= 1) {
				// do not search for strings with only one char
				return Collections.emptyList();
			}
			queryString = tools.preprocessQuery(queryString);
			if (queryString == null) {
				// do not search for strings with no tokens
				return Collections.emptyList();
			}
			
			QueryParser p = new QueryParser(version, DEFAULT_FIELD, analyzer);
			if (branch != null) {
				StringBuilder sb = new StringBuilder();
				sb.append(BRANCH_FIELD);
				sb.append(":(");
				sb.append(branch);
				sb.append(") AND (");
				sb.append(queryString);
				sb.append(")");
				queryString = sb.toString();
			}
			Query query = p.parse(queryString);
			TopDocs topDocs = searcher.search(query, maxCount);
			if (topDocs.totalHits == 0) {
				return Collections.emptyList();
			}
			
			boolean rerank = false;
			ScoreDoc[] scoreDocs = topDocs.scoreDocs;
			float maxScore = topDocs.getMaxScore();
			if (maxCount < 100 && scoreDocs.length == maxCount) {
				float lastScore = scoreDocs[scoreDocs.length - 1].score;
				if (fEquals(maxScore, lastScore)) {
					rerank = true;
					maxScore = topDocs.getMaxScore();
					topDocs = searcher.search(query, 100);
					scoreDocs = topDocs.scoreDocs;
				}
			}
			
			List<SearchResult> results = new ArrayList<SearchResult>(scoreDocs.length);
			
			for (ScoreDoc scoreDoc : scoreDocs) {
				Document doc = searcher.doc(scoreDoc.doc, FIELD_SELECTOR);
				String id = doc.get("id");
				OWLObject owlObject = ontology.getOWLObjectByIdentifier(id);
				if (owlObject != null) {
					if (!rerank || fEquals(maxScore, scoreDoc.score) ) {
						results.add(new SearchResult(owlObject, scoreDoc.score));
					}
				}
			}
			if (rerank) {
				tools.sortbyLabelLength(results);
			}
			if (results.size() > maxCount) {
				results = results.subList(0, maxCount);
			}
			return results;
		} catch (Exception exception) {
			logger.warn("Could not execute search", exception);
		}
		return Collections.emptyList();
	}
	
	public static class SearchResult {
		
		public final OWLObject hit;
		public final float score;
		
		/**
		 * @param hit
		 * @param score
		 */
		SearchResult(OWLObject hit, float score) {
			super();
			this.hit = hit;
			this.score = score;
		}
	}

	public static void main(String[] args) {
		
		ConfiguredOntology go = DefaultOntologyConfiguration.getOntologies().get("GeneOntology");
		
		OntologyTaskManager ontology = DefaultOntologyLoader.getOntology(go);

		ontology.runManagedTask(new OntologyTask(){

			@Override
			public void run(OWLGraphWrapper managed) {
				try {
					LuceneMemoryOntologyIndex index = new LuceneMemoryOntologyIndex(managed, null, null);
					Collection<SearchResult> results = index.search(" me  pigmentation ", 5, null);
					for (SearchResult searchResult : results) {
						String id = managed.getIdentifier(searchResult.hit);
						String label = managed.getLabel(searchResult.hit);
						System.out.println(id+"  "+searchResult.score+"  "+label);	
					}
				} catch (IOException exception) {
					throw new RuntimeException(exception);
				}				
			}
			
		});
	}
}
