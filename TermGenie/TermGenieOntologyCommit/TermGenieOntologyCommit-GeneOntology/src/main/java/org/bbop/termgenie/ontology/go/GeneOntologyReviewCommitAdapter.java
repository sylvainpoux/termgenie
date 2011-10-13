package org.bbop.termgenie.ontology.go;

import java.io.BufferedWriter;
import java.io.File;
import java.io.StringWriter;
import java.util.List;

import org.bbop.termgenie.cvs.CVSTools;
import org.bbop.termgenie.ontology.CommitException;
import org.bbop.termgenie.ontology.CommitHistoryStore;
import org.bbop.termgenie.ontology.Committer;
import org.bbop.termgenie.ontology.OntologyCommitReviewPipelineStages;
import org.bbop.termgenie.ontology.CommitInfo.CommitMode;
import org.bbop.termgenie.ontology.OntologyCommitReviewPipeline;
import org.bbop.termgenie.ontology.OntologyTaskManager;
import org.bbop.termgenie.ontology.OntologyTaskManager.OntologyTask;
import org.bbop.termgenie.ontology.entities.CommitHistoryItem;
import org.bbop.termgenie.ontology.entities.CommitedOntologyTerm;
import org.bbop.termgenie.ontology.entities.CommitedOntologyTermRelation;
import org.bbop.termgenie.ontology.go.GoCvsHelper.OboCommitData;
import org.obolibrary.obo2owl.Owl2Obo;
import org.obolibrary.oboformat.model.Frame;
import org.obolibrary.oboformat.model.OBODoc;
import org.obolibrary.oboformat.writer.OBOFormatWriter;

import owltools.graph.OWLGraphWrapper;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

@Singleton
public class GeneOntologyReviewCommitAdapter extends OntologyCommitReviewPipeline<CVSTools, OboCommitData, OBODoc> implements
		OntologyCommitReviewPipelineStages
{

	private final GoCvsHelper helper;

	@Inject
	GeneOntologyReviewCommitAdapter(@Named("GeneOntologyTaskManager") OntologyTaskManager source,
			CommitHistoryStore store,
			boolean supportAnonymus,
			GoCvsHelper helper)
	{
		super(source, store, supportAnonymus);
		this.helper = helper;
	}

	@Override
	protected String createDiff(CommitHistoryItem historyItem, OntologyTaskManager source)
			throws CommitException
	{

		CreateDiffTask task = new CreateDiffTask(historyItem);
		source.runManagedTask(task);
		if (task.exception != null) {
			throw error("Could not create diff", task.exception);
		}
		if (task.diff == null) {
			throw error("Could not create diff: empty result");
		}
		return task.diff;
	}

	private class CreateDiffTask implements OntologyTask {

		private final CommitHistoryItem historyItem;

		private Throwable exception = null;
		private String diff = null;

		public CreateDiffTask(CommitHistoryItem historyItem) {
			this.historyItem = historyItem;
		}

		@Override
		public Modified run(OWLGraphWrapper managed) {
			try {
				Owl2Obo owl2Obo = new Owl2Obo();
				OBODoc oboDoc = owl2Obo.convert(managed.getSourceOntology());

				List<CommitedOntologyTerm> terms = historyItem.getTerms();
				boolean succcess = applyChanges(terms, historyItem.getRelations(), oboDoc);
				if (succcess) {
					OBOFormatWriter oboWriter = new OBOFormatWriter();
					StringWriter stringWriter = new StringWriter();
					BufferedWriter writer = new BufferedWriter(stringWriter);
					for (CommitedOntologyTerm term : terms) {
						Frame termFrame = oboDoc.getTermFrame(term.getId());
						oboWriter.write(termFrame, writer, oboDoc);
						writer.append('\n');
					}
					writer.close();
					diff = stringWriter.getBuffer().toString();
				}
			} catch (Exception exception) {
				this.exception = exception;
			}
			return Modified.no;
		}

	}

	@Override
	protected OboCommitData prepareWorkflow(File workFolder) throws CommitException {
		return helper.prepareWorkflow(workFolder);
	}

	@Override
	protected CVSTools prepareSCM(CommitMode mode,
			String username,
			String password,
			OboCommitData data) throws CommitException
	{
		return helper.createCVS(mode, username, password, data.cvsFolder);
	}

	@Override
	protected void updateSCM(CVSTools scm, OBODoc targetOntology, OboCommitData data)
			throws CommitException
	{
		// TODO Auto-generated method stub

	}

	@Override
	protected OBODoc retrieveTargetOntology(CVSTools scm, OboCommitData data)
			throws CommitException
	{
		return helper.retrieveTargetOntology(scm, data);
	}

	@Override
	protected void checkTargetOntology(OboCommitData data, OBODoc targetOntology)
			throws CommitException
	{
		helper.checkTargetOntology(data, targetOntology);
	}

	@Override
	protected boolean applyChanges(List<CommitedOntologyTerm> terms,
			List<CommitedOntologyTermRelation> relations,
			OBODoc ontology) throws CommitException
	{
		return helper.applyHistoryChanges(terms, relations, ontology);
	}

	@Override
	protected void createModifiedTargetFile(OboCommitData data, OBODoc ontology)
			throws CommitException
	{
		helper.createModifiedTargetFile(data, ontology);
	}

	@Override
	protected void commitToRepository(String username, CVSTools scm, OboCommitData data, String diff)
			throws CommitException
	{
		helper.commitToRepository(username, scm, data, diff);
	}

	@Override
	public Committer getReviewCommitter() {
		return this;
	}

	@Override
	public BeforeReview getBeforeReview() {
		return this;
	}

	@Override
	public AfterReview getAfterReview() {
		return this;
	}

}