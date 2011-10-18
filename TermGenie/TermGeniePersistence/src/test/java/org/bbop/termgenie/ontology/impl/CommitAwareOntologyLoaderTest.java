package org.bbop.termgenie.ontology.impl;

import static org.junit.Assert.*;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.bbop.termgenie.core.ioc.IOCModule;
import org.bbop.termgenie.core.ioc.TermGenieGuice;
import org.bbop.termgenie.ontology.CommitHistoryStore;
import org.bbop.termgenie.ontology.CommitHistoryStoreImpl;
import org.bbop.termgenie.ontology.IRIMapper;
import org.bbop.termgenie.ontology.OntologyCleaner;
import org.bbop.termgenie.ontology.OntologyConfiguration;
import org.bbop.termgenie.ontology.OntologyTaskManager;
import org.bbop.termgenie.ontology.OntologyTaskManager.OntologyTask;
import org.bbop.termgenie.ontology.entities.CommitHistoryItem;
import org.bbop.termgenie.ontology.entities.CommitedOntologyTerm;
import org.bbop.termgenie.presistence.PersistenceBasicModule;
import org.bbop.termgenie.tools.TempTestFolderTools;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.semanticweb.owlapi.model.OWLObject;

import owltools.graph.OWLGraphWrapper;

import com.google.inject.Injector;

public class CommitAwareOntologyLoaderTest {

	private static File testFolder;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		testFolder = TempTestFolderTools.createTestFolder(CommitAwareOntologyLoaderTest.class);
		FileUtils.cleanDirectory(testFolder);
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
//		TempTestFolderTools.deleteTestFolder(testFolder);
	}

	@Test
	public void testPostProcessOBOOntology() throws Exception {
		// general setup
		Injector injector = TermGenieGuice.createInjector(new XMLReloadingOntologyModule("persistence-tests-ontology-configuration.xml"),
				new PersistenceBasicModule(testFolder),
				new IOCModule() {
					
					@Override
					protected void configure() {
						bind(CommitHistoryStore.class).to(CommitHistoryStoreImpl.class);
					}
				});
		OntologyConfiguration configuration = injector.getInstance(OntologyConfiguration.class);
		IRIMapper iriMapper = injector.getInstance(IRIMapper.class);
		OntologyCleaner cleaner = injector.getInstance(OntologyCleaner.class);
		CommitHistoryStore commitHistoryStore = injector.getInstance(CommitHistoryStore.class);

		String ontology = "CL";
		final String testId = "CL:2000001";
		final String testLabel = "Test term label";

		// add mods to the history
		CommitHistoryItem item = new CommitHistoryItem();
		item.setDate(new Date());
		item.setUser("test");
		List<CommitedOntologyTerm> terms = new ArrayList<CommitedOntologyTerm>();
		CommitedOntologyTerm term = new CommitedOntologyTerm();
		term.setId(testId);
		term.setLabel("Test term label");
		terms.add(term);
		item.setTerms(terms);
		commitHistoryStore.add(item, ontology);

		// setup ontology loader
		CommitAwareOntologyLoader loader = new CommitAwareOntologyLoader(configuration, iriMapper, cleaner, Collections.<String> emptySet(), 1L, TimeUnit.DAYS, commitHistoryStore);
		assertNotNull(loader);

		// load ontology
		List<OntologyTaskManager> ontologies = loader.getOntologies();
		OntologyTaskManager taskManager = ontologies.get(0);

		// check content for modification from history
		taskManager.runManagedTask(new OntologyTask() {

			@Override
			protected void runCatching(OWLGraphWrapper managed) throws TaskException, Exception {
				OWLObject owlObject = managed.getOWLObjectByIdentifier(testId);
				assertEquals(testLabel, managed.getLabel(owlObject));
			}
		});
	}

}