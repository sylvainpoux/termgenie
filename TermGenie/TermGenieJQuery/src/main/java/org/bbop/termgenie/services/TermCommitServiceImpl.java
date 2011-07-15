package org.bbop.termgenie.services;

import org.bbop.termgenie.data.JsonOntologyTerm;
import org.bbop.termgenie.data.JsonOntologyTerm.JsonSynonym;
import org.bbop.termgenie.ontology.OntologyTaskManager;
import org.bbop.termgenie.ontology.OntologyTaskManager.OntologyTask;
import org.bbop.termgenie.tools.ImplementationFactory;
import org.bbop.termgenie.tools.OntologyTools;

import owltools.graph.OWLGraphWrapper;

public class TermCommitServiceImpl implements TermCommitService {

	private static final OntologyTools ontologyTools = ImplementationFactory.getOntologyTools();
	
//	@Override
//	public boolean isValidUser(String username, String password, String ontology) {
//		return ImplementationFactory.getUserCredentialValidator().validate(username, password, ontology);
//	}

	@Override
	public JsonExportResult exportTerms(String sessionId, JsonOntologyTerm[] terms, String ontologyName) {
		JsonExportResult result = new JsonExportResult();
		
		OntologyTaskManager manager = ontologyTools.getManager(ontologyName);
		if (manager == null) {
			result.setSuccess(false);
			result.setMessage("Unknown ontology: "+ontologyName);
			return result;
		}
		
		// TODO use a proper obo export tool here!
		final StringBuilder sb = new StringBuilder();
		sb.append("Preliminary export results:<br/>");
		sb.append("<pre>\n");
		for (JsonOntologyTerm term : terms) {
			sb.append("[Term]\n");
			manager.runManagedTask(new OntologyTask(){

				@Override
				public void run(OWLGraphWrapper managed) {
					String id = managed.getOntologyId();
					sb.append("id: ");
					sb.append(id);
					sb.append(":-------\n");
				}
			});
			sb.append("name: ");
			sb.append(term.getLabel());
			sb.append('\n');
			sb.append("def: \"");
			sb.append(term.getDefinition());
			sb.append("\"");
			String[] defxRefs = term.getDefxRef();
			if (defxRefs != null && defxRefs.length > 0) {
				sb.append(" [");
				for (int i = 0; i < defxRefs.length; i++) {
					if (i > 0) {
						sb.append(", ");
					}
					sb.append(defxRefs[i]);
				}
				sb.append("]\n");
			}
			else {
				sb.append('\n');
			}
			final String comment = term.getMetaData().getComment();
			if (comment != null && !comment.isEmpty()) {
				sb.append("comment: \"");
				sb.append(comment);
				sb.append("\"\n");
			}
			JsonSynonym[] synonyms = term.getSynonyms();
			if (synonyms != null && synonyms.length > 0) {
				for (JsonSynonym synonym : synonyms) {
					sb.append("synonym: \"");
					sb.append(synonym.getLabel());
					sb.append('"');
					String scope = synonym.getScope();
					if (scope != null && !scope.isEmpty()) {
						sb.append(' ');
						sb.append(scope);
					}
					String category = synonym.getCategory();
					if (category != null && !category.isEmpty()) {
						sb.append(' ');
						sb.append(category);
					}
					String[] xrefs = synonym.getXrefs();
					if (xrefs != null && xrefs.length > 0) {
						for (int i = 0; i < xrefs.length; i++) {
							if (i == 0) {
								sb.append(" [");
							}
							else {
								sb.append(',');
							}
							sb.append(xrefs[i]);
						}
						sb.append(']');
					}
					
					sb.append("\n");
				}
			}
			
			/*
	private String logDef;
	private JsonTermRelation[] relations;
			 */
//			is_a
//			intersection_of
//			union_of
//			disjoint_from
//			relationship
			sb.append("created_by: ");
			sb.append(term.getMetaData().getCreated_by());
			sb.append('\n');
			sb.append("creation_date: ");
			sb.append(term.getMetaData().getCreation_date());
			sb.append('\n');
			sb.append('\n');
		}
		sb.append("\n</pre>");
		result.setSuccess(true);
		result.setFormats(new String[]{"OBO"});
		result.setContents(new String[]{sb.toString()});
		// TODO add OWL export support
		return result;
	}
	
	@Override
	public JsonCommitResult commitTerms(String sessionId, JsonOntologyTerm[] terms, String ontology) {
		JsonCommitResult result = new JsonCommitResult();
		result.setSuccess(false);
		result.setMessage("The commit operation is not yet implemented.");
		return result;
	}

}
