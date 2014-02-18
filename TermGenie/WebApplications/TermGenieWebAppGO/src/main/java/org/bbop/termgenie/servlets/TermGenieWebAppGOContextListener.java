package org.bbop.termgenie.servlets;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.bbop.termgenie.core.ioc.IOCModule;
import org.bbop.termgenie.mail.MailHandler;
import org.bbop.termgenie.mail.SimpleMailHandler;
import org.bbop.termgenie.mail.review.DefaultReviewMailHandlerModule;
import org.bbop.termgenie.ontology.AdvancedPersistenceModule;
import org.bbop.termgenie.ontology.impl.SvnAwareXMLReloadingOntologyModule;
import org.bbop.termgenie.ontology.svn.CommitSvnUserKeyFileModule;
import org.bbop.termgenie.presistence.PersistenceBasicModule;
import org.bbop.termgenie.rules.XMLDynamicRulesModule;
import org.bbop.termgenie.services.DefaultTermCommitServiceImpl;
import org.bbop.termgenie.services.TermCommitService;
import org.bbop.termgenie.services.TermGenieServiceModule;
import org.bbop.termgenie.services.freeform.FreeFormTermServiceModule;
import org.bbop.termgenie.services.permissions.UserPermissionsModule;
import org.bbop.termgenie.services.review.OboTermCommitReviewServiceImpl;
import org.bbop.termgenie.services.review.TermCommitReviewService;
import org.bbop.termgenie.services.review.TermCommitReviewServiceModule;
import org.bbop.termgenie.user.go.GeneOntologyJsonUserDataModule;
import org.semanticweb.owlapi.model.IRI;

public class TermGenieWebAppGOContextListener extends AbstractTermGenieContextListener {

	private static final Logger logger = Logger.getLogger(TermGenieWebAppGOContextListener.class);
	
	public TermGenieWebAppGOContextListener() {
		super("TermGenieWebAppGOConfigFile");
	}
	
	@Override
	protected IOCModule getUserPermissionModule() {
		return new UserPermissionsModule("termgenie-go", applicationProperties);
	}
	
	@Override
	protected TermGenieServiceModule getServiceModule() {
		return new TermGenieServiceModule(applicationProperties) {

			@Override
			protected void bindTermCommitService() {
				bind(TermCommitService.class, DefaultTermCommitServiceImpl.class);
			}
			
			@Override
			public String getModuleName() {
				return "TermGenieGO-TermGenieServiceModule";
			}
		};
	}

	@Override
	protected IOCModule getOntologyModule() {
		String configFile = "ontology-configuration_go.xml";
		String repositoryURL = "svn+ssh://ext.geneontology.org/share/go/svn/trunk/ontology";
		String workFolder = null;	// no default value
		String svnUserName = null;	// no default value
		String keyFile = null;		// no default value
		boolean loadExternal = true;
		boolean usePassphrase = false;
		
		Map<IRI, String> mappedIRIs = new HashMap<IRI, String>();
		
		// http://purl.obolibrary.org/obo/go.owl ->  editors/gene_ontology_write.obo
		mappedIRIs.put(IRI.create("http://purl.obolibrary.org/obo/go.owl"), "editors/gene_ontology_write.obo");
			
		String catalogXML = "extensions/catalog-v001.xml";
		
		List<String> ignoreIRIs = Arrays.asList(
				"http://purl.obolibrary.org/obo/go/extensions/x-chemical.owl",
                "http://purl.obolibrary.org/obo/TEMP");
		
		return SvnAwareXMLReloadingOntologyModule.createSshKeySvnModule(configFile, applicationProperties, repositoryURL, mappedIRIs, catalogXML, workFolder, svnUserName, keyFile, loadExternal, usePassphrase, ignoreIRIs);
	}

	@Override
	protected IOCModule getRulesModule() {
		boolean useIsInferred = false;
		boolean assertInferences = true;
		return new XMLDynamicRulesModule("termgenie_rules_go.xml", useIsInferred, assertInferences, applicationProperties);
	}

	@Override
	protected IOCModule getCommitModule() {
		String repositoryURL = "svn+ssh://ext.geneontology.org/share/go/svn/trunk/ontology";
		String remoteTargetFile = "editors/gene_ontology_write.obo";
		String svnUserName = null;	// no default value
		File keyFile = null;		// no default value
		boolean loadExternal = true;
		boolean usePassphrase = false;
		
		return CommitSvnUserKeyFileModule.createOboModule(repositoryURL, remoteTargetFile, svnUserName, keyFile , applicationProperties, loadExternal, usePassphrase);
	}
	
	

	@Override
	protected TermCommitReviewServiceModule getCommitReviewWebModule() {
		return new TermCommitReviewServiceModule(true, applicationProperties) {

			@Override
			public String getModuleName() {
				return "TermGenieGO-TermCommitReviewServiceModule";
			}
			
			@Override
			protected void bindEnabled() {
				bind(TermCommitReviewService.class, OboTermCommitReviewServiceImpl.class);
			}
		};
	}

	@Override
	protected Collection<IOCModule> getAdditionalModules() {
		List<IOCModule> modules = new ArrayList<IOCModule>();
		try {
			// basic persistence
			String dbFolderString = IOCModule.getProperty("TermGenieWebappGODatabaseFolder", applicationProperties);
			File dbFolder;
			if (dbFolderString != null && !dbFolderString.isEmpty()) {
				dbFolder = new File(dbFolderString);
			}
			else {
				dbFolder = new File(FileUtils.getUserDirectory(), "termgenie-go-db");
			}
			logger.info("Using db folder: "+dbFolder);
			FileUtils.forceMkdir(dbFolder);
			modules.add(new PersistenceBasicModule(dbFolder, applicationProperties));
		} catch (IOException exception) {
			throw new RuntimeException(exception);
		}
		// commit history and ontology id store
		modules.add(new AdvancedPersistenceModule("GO-ID-Manager-Primary", 
				"go-id-manager-primary.conf",
				"GO-ID-Manager-Secondary", 
				"go-id-manager-secondary.conf", 
				applicationProperties));
		return modules;
	}

	@Override
	protected IOCModule getUserDataModule() {
		String gocjson = "GO.user_data.json";
		List<String> additionalXrefResources = Collections.emptyList();
		return new GeneOntologyJsonUserDataModule(applicationProperties, gocjson, additionalXrefResources);
	}
	
	@Override
	protected IOCModule getReviewMailHandlerModule() {
		
		return new DefaultReviewMailHandlerModule(applicationProperties, "help@go.termgenie.org", "GeneOntology TermGenie") {
			
			@Override
			protected MailHandler provideMailHandler() {
				return new SimpleMailHandler("smtp.lbl.gov");
			}
		};
	}
	
	@Override
	protected IOCModule getFreeFormTermModule() {
		List<String> oboNamespaces = new ArrayList<String>();
		oboNamespaces.add("biological_process");
		oboNamespaces.add("molecular_function");
		oboNamespaces.add("cellular_component");
		String defaultOntology = "default_go";
		return new FreeFormTermServiceModule(applicationProperties, true, defaultOntology, oboNamespaces, "termgenie_unvetted");
	}
}
