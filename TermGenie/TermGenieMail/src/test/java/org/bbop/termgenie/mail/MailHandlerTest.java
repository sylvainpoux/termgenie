package org.bbop.termgenie.mail;

import org.junit.Ignore;
import org.junit.Test;


public class MailHandlerTest {

	@Test
	@Ignore
	public void test() throws Exception {
		MailHandler h = new SimpleMailHandler("smtp.lbl.gov");
		h.sendEmail("Test 6",
				"This is a test message send via java",
				"help@go.termgenie.org",
				"TermGenie",
				"hdietze@lbl.gov");
		
	}

}
