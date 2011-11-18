package org.bbop.termgenie.ontology.go.svn;

import java.util.Properties;

import org.bbop.termgenie.ontology.obo.OboScmHelper;

public class GOCommitSVNAnonymousModule extends AbstractGoCommitSvnModule {

	public GOCommitSVNAnonymousModule(String svnRepository,
			String svnOntologyFileName,
			Properties applicationProperties) {
		super(svnRepository, svnOntologyFileName, applicationProperties);
	}

	@Override
	protected void bindOBOSCMHelper() {
		bind(OboScmHelper.class).to(GoSvnHelper.GoCvsHelperAnonymous.class);
		
	}

}
