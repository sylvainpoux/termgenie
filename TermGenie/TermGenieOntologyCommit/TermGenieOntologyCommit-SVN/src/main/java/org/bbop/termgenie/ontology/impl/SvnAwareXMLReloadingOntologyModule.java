package org.bbop.termgenie.ontology.impl;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;

import org.apache.commons.io.FileUtils;
import org.bbop.termgenie.ontology.IRIMapper;
import org.bbop.termgenie.svn.SvnTool;
import org.semanticweb.owlapi.model.IRI;

import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;


public abstract class SvnAwareXMLReloadingOntologyModule extends XMLReloadingOntologyModule {

	public static SvnAwareXMLReloadingOntologyModule createAnonymousSvnModule(String configFile,
			Properties applicationProperties,
			String repositoryURL,
			Map<IRI, String> mappedIRIs,
			String catalogXML,
			String workFolder,
			String fileCachingFolder,
			boolean loadExternal)
	{
		return new AnonymousSvnAwareXMLReloadingOntologyModule(configFile, applicationProperties, repositoryURL, mappedIRIs, catalogXML, workFolder, fileCachingFolder, loadExternal);
	}
	
	public static SvnAwareXMLReloadingOntologyModule createUsernamePasswordSvnModule(String configFile,
			Properties applicationProperties,
			String repositoryURL,
			Map<IRI, String> mappedIRIs,
			String catalogXML,
			String workFolder,
			String svnUsername,
			boolean loadExternal)
	{
		return new PasswordSvnAwareXMLReloadingOntologyModule(configFile, applicationProperties, repositoryURL, mappedIRIs, catalogXML, workFolder, svnUsername, loadExternal);
	}
	

	private final String repositoryURLDefault;
	private final Map<IRI, String> mappedIRIs;
	private final String catalogXML;
	private final String workFolderDefault;
	private final String fileCachingFolder;
	private final boolean loadExternal;
	
	private SvnAwareXMLReloadingOntologyModule(String configFile,
			Properties applicationProperties,
			String repositoryURL,
			Map<IRI, String> mappedIRIs,
			String catalogXML,
			String workFolder,
			String fileCachingFolder,
			boolean loadExternal)
	{
		super(configFile, applicationProperties);
		this.repositoryURLDefault = repositoryURL;
		this.mappedIRIs = mappedIRIs;
		this.catalogXML = catalogXML;
		this.workFolderDefault = workFolder;
		if(fileCachingFolder == null) {
			this.fileCachingFolder = new File(FileUtils.getTempDirectory(), "termgenie-download-cache").getAbsolutePath();
		}
		else {
			this.fileCachingFolder = fileCachingFolder;
		}
		this.loadExternal = loadExternal;

	}

	@Override
	protected void bindIRIMapper() {
		bind(IRIMapper.class, "FallbackIRIMapper", FileCachingIRIMapper.class);
		bind("FileCachingIRIMapperLocalCache", fileCachingFolder);
		bind("FileCachingIRIMapperPeriod", new Long(6L));
		bind("FileCachingIRIMapperTimeUnit", TimeUnit.HOURS);

		bind("SVNAwareIRIMapperRepositoryURL", repositoryURLDefault);
		bind("SVNAwareIRIMapperWorkFolder", workFolderDefault);
		bindIRIMap("SVNAwareIRIMapperMappedIRIs", mappedIRIs);
		bind("SVNAwareIRIMapperCatalogXML", catalogXML, true);
		bind("SVNAwareIRIMapperSVNConfigDir", SvnTool.getDefaultSvnConfigDir());
		bind("SVNAwareIRIMapperSVNLoadExternal", loadExternal);
		bindAdditional();
	}
	
	protected void bindAdditional() {
		// default empty;
	}

	
	static class AnonymousSvnAwareXMLReloadingOntologyModule extends SvnAwareXMLReloadingOntologyModule {

		AnonymousSvnAwareXMLReloadingOntologyModule(String configFile,
				Properties applicationProperties,
				String repositoryURL,
				Map<IRI, String> mappedIRIs,
				String catalogXML,
				String workFolder,
				String fileCachingFolder,
				boolean svnLoadExternal)
		{
			super(configFile, applicationProperties, repositoryURL, mappedIRIs, catalogXML, workFolder, fileCachingFolder, svnLoadExternal);
			
		}
		

		@Provides
		@Singleton
		protected IRIMapper getIRIMapper(@Named("FallbackIRIMapper") IRIMapper fallbackIRIMapper,
				@Named("SVNAwareIRIMapperRepositoryURL") String repositoryURL,
				@Named("SVNAwareIRIMapperCatalogXML") String catalogXML,
				@Named("SVNAwareIRIMapperMappedIRIs") Map<IRI, String> mappedIRIs,
				@Named("SVNAwareIRIMapperWorkFolder") @Nullable String workFolder,
				@Named("SVNAwareIRIMapperSVNConfigDir") File svnConfigDir,
				@Named("SVNAwareIRIMapperSVNLoadExternal") Boolean svnLoadExternal)
		{
			final File workFolderFile = new File(workFolder);
			final SvnTool svn = SvnTool.createAnonymousSVN(workFolderFile, repositoryURL, svnConfigDir, svnLoadExternal);
			List<String> checkout = new ArrayList<String>(mappedIRIs.values());
			return new SvnIRIMapper(fallbackIRIMapper, svn, checkout, mappedIRIs, catalogXML);
		}
	}
	
	static class PasswordSvnAwareXMLReloadingOntologyModule extends SvnAwareXMLReloadingOntologyModule {

		private final String svnUsername;

		PasswordSvnAwareXMLReloadingOntologyModule(String configFile,
				Properties applicationProperties,
				String repositoryURL,
				Map<IRI, String> mappedIRIs,
				String catalogXML,
				String workFolder,
				String svnUsername,
				boolean svnLoadExternal)
		{
			super(configFile, applicationProperties, repositoryURL, mappedIRIs, catalogXML, workFolder, null, svnLoadExternal);
			this.svnUsername = svnUsername;
		}
		

		@Override
		protected void bindAdditional() {
			bind("SVNAwareIRIMapperUsername", svnUsername);
			bindSecret("SVNAwareIRIMapperPassword");
		}


		@Provides
		@Singleton
		protected IRIMapper getIRIMapper(@Named("FallbackIRIMapper") IRIMapper fallbackIRIMapper,
				@Named("SVNAwareIRIMapperRepositoryURL") String repositoryURL,
				@Named("SVNAwareIRIMapperCatalogXML") String catalogXML,
				@Named("SVNAwareIRIMapperMappedIRIs") Map<IRI, String> mappedIRIs,
				@Named("SVNAwareIRIMapperWorkFolder") @Nullable String workFolder,
				@Named("SVNAwareIRIMapperUsername") String svnUsername,
				@Named("SVNAwareIRIMapperPassword") String svnPassword,
				@Named("SVNAwareIRIMapperSVNConfigDir") File svnConfigDir,
				@Named("SVNAwareIRIMapperSVNLoadExternal") Boolean svnLoadExternal)
		{
			final File workFolderFile = new File(workFolder);
			final SvnTool svn = SvnTool.createUsernamePasswordSVN(workFolderFile, repositoryURL, svnUsername, svnPassword, svnConfigDir, svnLoadExternal);
			List<String> checkout = new ArrayList<String>(mappedIRIs.values());
			return new SvnIRIMapper(fallbackIRIMapper, svn, checkout, mappedIRIs, catalogXML);
		}
	}
}
