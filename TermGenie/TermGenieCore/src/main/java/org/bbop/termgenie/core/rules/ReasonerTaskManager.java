package org.bbop.termgenie.core.rules;

import java.util.Collection;
import java.util.HashSet;

import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.reasoner.NodeSet;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import owltools.graph.OWLGraphWrapper;

public abstract class ReasonerTaskManager extends GenericTaskManager<OWLReasoner> {

	/**
	 * A task which requires a reasoner. 
	 */
	public static interface ReasonerTask extends ManagedTask<OWLReasoner>{}
	
	// implement some common reasoner tasks.
	
	public Collection<OWLObject> getDescendants(final OWLObject x, final OWLGraphWrapper wrapper) {
		final Collection<OWLObject> result = new HashSet<OWLObject>();
		ReasonerTask task = new ReasonerTask() {
			
			@Override
			public void run(OWLReasoner managed) {
				NodeSet<OWLClass> subClasses = managed.getSubClasses(wrapper.getOWLClass(x), false);
				result.addAll(subClasses.getFlattened());
			}
		};
		runManagedTask(task);
		return result;
	}
	
	public Collection<OWLObject> getAncestors(final OWLObject x, final OWLGraphWrapper wrapper) {
		final Collection<OWLObject> result = new HashSet<OWLObject>();
		ReasonerTask task = new ReasonerTask() {
			
			@Override
			public void run(OWLReasoner managed) {
				NodeSet<OWLClass> subClasses = managed.getSuperClasses(wrapper.getOWLClass(x), false);
				result.addAll(subClasses.getFlattened());
			}
		};
		runManagedTask(task);
		return result;
	}
}