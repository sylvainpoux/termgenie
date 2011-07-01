package org.bbop.termgenie.rules;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.log4j.helpers.ISO8601DateFormat;
import org.bbop.termgenie.core.OntologyAware.OntologyTerm.DefaultOntologyTerm;
import org.bbop.termgenie.core.OntologyAware.Relation;
import org.bbop.termgenie.core.rules.TermGenerationEngine.TermGenerationInput;
import org.bbop.termgenie.core.rules.TermGenerationEngine.TermGenerationOutput;
import org.semanticweb.owlapi.model.OWLObject;

import owltools.graph.OWLGraphWrapper;

class BasicRules extends BasicTools {
	
	protected Set<String> synonyms(String prefix, OWLObject x, OWLGraphWrapper ontology, String suffix, String label) {
		String[] synonymStrings = getSynonyms(x, ontology);
		if (synonymStrings == null || synonymStrings.length == 0) {
			return null;
		}
		Set<String> synonyms = new HashSet<String>();
		for (String synonym : synonymStrings) {
			if (prefix != null) {
				synonym = prefix + synonym;
			}
			if (suffix != null) {
				synonym = synonym + suffix;
			}
			synonyms.add(synonym);
		}
		synonyms.remove(label);
		return synonyms;
	}
	
	protected Set<String> synonyms(String prefix, OWLObject x1, OWLGraphWrapper ontology1, String middle, OWLObject x2, OWLGraphWrapper ontology2, String suffix, String label) {
		String[] synonymStrings1 = getSynonyms(x1, ontology1);
		String[] synonymStrings2 = getSynonyms(x2, ontology2);
		boolean empty1 = synonymStrings1 == null || synonymStrings1.length == 0;
		boolean empty2 = synonymStrings2 == null || synonymStrings2.length == 0;
		if (empty1 && empty2) {
			// both do not have any synonyms
			return null;
		}
		
		List<String> synonyms1 = createSynList(x1, ontology1, synonymStrings1, empty1);
		List<String> synonyms2 = createSynList(x2, ontology2, synonymStrings2, empty2);
		
		Set<String> synonyms = new HashSet<String>();
		for (String synonym1 : synonyms1) {
			for (String synonym2 : synonyms2) {
				StringBuilder sb = new StringBuilder();
				if (prefix != null) {
					sb.append(prefix);
				}
				sb.append(synonym1);
				if (middle != null) {
					sb.append(middle);
				}
				sb.append(synonym2);
				if (suffix != null) {
					sb.append(suffix);
				}
				synonyms.add(sb.toString());
			}
		}
		synonyms.remove(label);
		return synonyms;
	}

	private List<String> createSynList(OWLObject x, OWLGraphWrapper ontology,
			String[] synonymStrings, boolean empty) {
		List<String> synonyms;
		
		String label = ontology.getLabel(x);
		
		if (empty) {
			// use label for synonym generation
			synonyms = Collections.singletonList(label);
		}
		else {
			synonyms = new ArrayList<String>(synonymStrings.length + 1);
			synonyms.addAll(Arrays.asList(synonymStrings));
			synonyms.add(label);
		}
		return synonyms;
	}
	
	@SuppressWarnings("deprecation")
	private String[] getSynonyms(OWLObject id, OWLGraphWrapper ontology) {
		if (ontology != null) {
			return ontology.getSynonymStrings(id);
		}
		return null;
	}
	
	protected String createCDef(String prefix, OWLObject x, OWLGraphWrapper ontology, String infix, String suffix) {
		return createCDef(prefix, Collections.singletonList(x), ontology, infix, null, suffix);
	}
	
	protected String createCDef(String prefix, List<OWLObject> list, OWLGraphWrapper ontology, String type, String separator, String suffix) {
		StringBuilder sb = new StringBuilder();
		if (prefix != null) {
			sb.append(prefix);
		}
		for (int i = 0; i < list.size(); i++) {
			OWLObject x = list.get(i);
			if (i > 0) {
				sb.append(separator);
			}
			if (type != null) {
				sb.append(type);
			}
			sb.append(ontology.getIdentifier(x));
		}
		if (suffix != null) {
			sb.append(suffix);
		}
		return sb.toString();
	}
	
	protected String createDefinition(String prefix, List<OWLObject> list, OWLGraphWrapper ontology, String infix, String suffix, TermGenerationInput input) {
		StringBuilder sb = new StringBuilder();
		if (prefix != null) {
			sb.append(prefix);
		}
		for (int i = 0; i < list.size(); i++) {
			OWLObject x = list.get(i);
			if (i > 0 && infix != null) {
				sb.append(infix);
			}
			sb.append(refname(x, ontology));
		}
		
		if (suffix != null) {
			sb.append(suffix);
		}
		return createDefinition(sb.toString(), input);
	}
	
	protected String createDefinition(String definition, TermGenerationInput input) {
		String inputDefinition = getFieldSingleString(input, "Definition");
		if (inputDefinition != null) {
			inputDefinition = inputDefinition.trim(); 
			if (inputDefinition.length() > 1) {
				return inputDefinition;
			}
		}
		return definition;
	}
	
	protected String createName(String name, TermGenerationInput input) {
		String inputName = getFieldSingleString(input, "Name");
		if (inputName != null) {
			inputName = inputName.trim(); 
			if (inputName.length() > 1) {
				return inputName;
			}
		}
		return name;
	}
	
	protected List<TermGenerationOutput> createTermList(String label, String definition, Set<String> synonyms, String logicalDefinition, List<Relation> relations, TermGenerationInput input, OWLGraphWrapper ontology) {
		List<TermGenerationOutput> output = new ArrayList<TermGenerationOutput>(1);
		createTermList(label, definition, synonyms, logicalDefinition, relations, input, ontology, output);
		return output;
	}
	
	private static final Pattern def_xref_Pattern = Pattern.compile("\\S+:\\S+");
	
	protected void createTermList(String label, String definition, Set<String> synonyms, String logicalDefinition, List<Relation> relations, TermGenerationInput input, OWLGraphWrapper ontology, List<TermGenerationOutput> output) {
		List<String> defxrefs = getDefXref(input);
		String comment = getComment(input);
		// Fact Checking
		// check label
		OWLObject sameName = ontology.getOWLObjectByLabel(label);
		if (sameName != null) {
			output.add(singleError("The term "+ontology.getIdentifier(sameName)+" with the same label already exists", input));
			return;
		}
		if (defxrefs != null) {
			// check xref conformity
			boolean hasXRef = false;
			for (String defxref : defxrefs) {
				// check if the termgenie def_xref is already in the list
				hasXRef = hasXRef || defxref.equals("GOC:TermGenie");
				
				// simple defxref check, TODO use a centralized qc check.
				if (defxref.length() < 3) {
					output.add(singleError("The Def_Xref "+defxref+" is too short. A Def_Xref consists of a prefix and suffix with a colon (:) as separator", input));
					continue;
				}
				if(!def_xref_Pattern.matcher(defxref).matches()) {
					output.add(singleError("The Def_Xref "+defxref+" does not conform to the expected pattern. A Def_Xref consists of a prefix and suffix with a colon (:) as separator and contains no whitespaces.", input));
				}
			}
			if (!hasXRef) {
				// add the termgenie def_xref
				defxrefs.add("GOC:TermGenie");
			}
		}
		else {
			defxrefs = Collections.singletonList("GOC:TermGenie");
		}
		DefaultOntologyTerm term = new DefaultOntologyTerm(null, label, definition, synonyms, logicalDefinition, defxrefs, comment, relations);
		Map<String, String> metaData = term.getMetaData();
		metaData.put("creation_date", getDate());
		metaData.put("created_by", "TermGenie");
		metaData.put("resource", ontology.getOntologyId());
		output.add(success(term, input));
	}
	
	// TODO use proper date time format as defined in OBO 1.4 standard
	private final static DateFormat df = ISO8601DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.FULL);
	
	private String getDate() {
		return df.format(new Date());
	}
	
}