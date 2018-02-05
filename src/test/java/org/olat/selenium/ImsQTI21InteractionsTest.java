/**
 * <a href="http://www.openolat.org">
 * OpenOLAT - Online Learning and Training</a><br>
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); <br>
 * you may not use this file except in compliance with the License.<br>
 * You may obtain a copy of the License at the
 * <a href="http://www.apache.org/licenses/LICENSE-2.0">Apache homepage</a>
 * <p>
 * Unless required by applicable law or agreed to in writing,<br>
 * software distributed under the License is distributed on an "AS IS" BASIS, <br>
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. <br>
 * See the License for the specific language governing permissions and <br>
 * limitations under the License.
 * <p>
 * Initial code contributed and copyrighted by<br>
 * frentix GmbH, http://www.frentix.com
 * <p>
 */
package org.olat.selenium;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.UUID;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.drone.api.annotation.Drone;
import org.jboss.arquillian.graphene.page.InitialPage;
import org.jboss.arquillian.graphene.page.Page;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.olat.ims.qti21.QTI21AssessmentResultsOptions;
import org.olat.selenium.page.LoginPage;
import org.olat.selenium.page.NavigationPage;
import org.olat.selenium.page.qti.QTI21Page;
import org.olat.test.JunitTestHelper;
import org.olat.test.rest.UserRestClient;
import org.olat.user.restapi.UserVO;
import org.openqa.selenium.WebDriver;


/**
 * This suite tests the interactions in runtime and only in runtime:
 * <ul>
 *   <li>Hotspot
 *   <li>Associate
 *   <li>Graphic associate
 *   <li>Match
 * </ul>
 * 
 * Initial date: 26 janv. 2018<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
@RunWith(Arquillian.class)
public class ImsQTI21InteractionsTest extends Deployments {
	
	@Drone
	private WebDriver browser;
	@ArquillianResource
	private URL deploymentUrl;
	@Page
	private NavigationPage navBar;
	
	/**
	 * Check if the hotspot interaction send a "correct" feedback.
	 * 
	 * @param authorLoginPage
	 * @throws IOException
	 * @throws URISyntaxException
	 */
	@Test
	@RunAsClient
	public void qti21HotspotInteraction(@InitialPage LoginPage authorLoginPage)
	throws IOException, URISyntaxException {
		UserVO author = new UserRestClient(deploymentUrl).createAuthor();
		authorLoginPage.loginAs(author.getLogin(), author.getPassword());
		
		//upload a test
		String qtiTestTitle = "Simple QTI 2.1 " + UUID.randomUUID();
		URL qtiTestUrl = JunitTestHelper.class.getResource("file_resources/qti21/simple_QTI_21_hotspot.zip");
		File qtiTestFile = new File(qtiTestUrl.toURI());
		navBar
			.openAuthoringEnvironment()
			.uploadResource(qtiTestTitle, qtiTestFile)
			.clickToolbarRootCrumb();
		
		QTI21Page qtiPage = QTI21Page
				.getQTI12Page(browser);
		qtiPage
			.options()
			.showResults(Boolean.TRUE, QTI21AssessmentResultsOptions.allOptions())
			.save();
		// to the test and spot it
		qtiPage
			.clickToolbarBack()
			.assertOnAssessmentItem()
			.answerHotspot("circle")
			.saveAnswer()
			.assertFeedback("Correct!")
			.endTest();
		//check the results
		qtiPage
			.assertOnAssessmentResults()
			.assertOnAssessmentTestScore(1)
			.assertOnAssessmentTestMaxScore(1);
	}
	
	/**
	 * Check if the associate interaction return its 4 points.
	 * 
	 * @param authorLoginPage
	 * @throws IOException
	 * @throws URISyntaxException
	 */
	@Test
	@RunAsClient
	public void qti21AssociateInteraction(@InitialPage LoginPage authorLoginPage)
	throws IOException, URISyntaxException {
		UserVO author = new UserRestClient(deploymentUrl).createAuthor();
		authorLoginPage.loginAs(author.getLogin(), author.getPassword());
		
		//upload a test
		String qtiTestTitle = "Associate QTI 2.1 " + UUID.randomUUID();
		URL qtiTestUrl = JunitTestHelper.class.getResource("file_resources/qti21/simple_QTI_21_associate_ims.zip");
		File qtiTestFile = new File(qtiTestUrl.toURI());
		navBar
			.openAuthoringEnvironment()
			.uploadResource(qtiTestTitle, qtiTestFile)
			.clickToolbarRootCrumb();
		
		QTI21Page qtiPage = QTI21Page
				.getQTI12Page(browser);
		qtiPage
			.options()
			.showResults(Boolean.TRUE, QTI21AssessmentResultsOptions.allOptions())
			.save();
		// to the test and spot it
		qtiPage
			.clickToolbarBack()
			.assertOnAssessmentItem()
			.answerAssociate("Antonio", 1, true)
			.answerAssociate("Prospero", 1, false)
			.answerAssociate("Capulet", 2, true)
			.answerAssociate("Montague", 2, false)
			.answerAssociate("Demetrius", 3, true)
			.answerAssociate("Lysander", 3, false)
			.saveAnswer()
			.endTest()
			.closeTest();
		//check the results
		qtiPage
			.assertOnAssessmentResults()
			.assertOnAssessmentItemScore("Shakespearian Rivals", 4);
	}
	
	/**
	 * Check if the graphic associate interaction return its 2 points.
	 * 
	 * @param authorLoginPage
	 * @throws IOException
	 * @throws URISyntaxException
	 */
	@Test
	@RunAsClient
	public void qti21GraphicAssociateInteraction(@InitialPage LoginPage authorLoginPage)
	throws IOException, URISyntaxException {
		UserVO author = new UserRestClient(deploymentUrl).createAuthor();
		authorLoginPage.loginAs(author.getLogin(), author.getPassword());
		
		//upload a test
		String qtiTestTitle = "Associate QTI 2.1 " + UUID.randomUUID();
		URL qtiTestUrl = JunitTestHelper.class.getResource("file_resources/qti21/simple_QTI_21_graphic_associate_ims.zip");
		File qtiTestFile = new File(qtiTestUrl.toURI());
		navBar
			.openAuthoringEnvironment()
			.uploadResource(qtiTestTitle, qtiTestFile)
			.clickToolbarRootCrumb();
		
		QTI21Page qtiPage = QTI21Page
				.getQTI12Page(browser);
		qtiPage
			.options()
			.showResults(Boolean.TRUE, QTI21AssessmentResultsOptions.allOptions())
			.save();
		// to the test and spot it
		qtiPage
			.clickToolbarBack()
			.assertOnAssessmentItem()
			.answerGraphicAssociate("B")
			.answerGraphicAssociate("C")
			.answerGraphicAssociate("C")
			.answerGraphicAssociate("D")
			.saveAnswer()
			.endTest()
			.closeTest();
		//check the results
		qtiPage
			.assertOnAssessmentResults()
			.assertOnAssessmentItemScore("Low-cost Flying", 2);
	}
	
	/**
	 * Check if the classic match interaction return its 3 points.
	 * 
	 * @param authorLoginPage
	 * @throws IOException
	 * @throws URISyntaxException
	 */
	@Test
	@RunAsClient
	public void qti21MatchInteraction(@InitialPage LoginPage authorLoginPage)
	throws IOException, URISyntaxException {
		UserVO author = new UserRestClient(deploymentUrl).createAuthor();
		authorLoginPage.loginAs(author.getLogin(), author.getPassword());
		
		//upload a test
		String qtiTestTitle = "Match QTI 2.1 " + UUID.randomUUID();
		URL qtiTestUrl = JunitTestHelper.class.getResource("file_resources/qti21/simple_QTI_21_match_ims.zip");
		File qtiTestFile = new File(qtiTestUrl.toURI());
		navBar
			.openAuthoringEnvironment()
			.uploadResource(qtiTestTitle, qtiTestFile)
			.clickToolbarRootCrumb();
		
		QTI21Page qtiPage = QTI21Page
				.getQTI12Page(browser);
		qtiPage
			.options()
			.showResults(Boolean.TRUE, QTI21AssessmentResultsOptions.allOptions())
			.save();
		// to the test and spot it
		qtiPage
			.clickToolbarBack()
			.assertOnAssessmentItem()
			.answerMatch("Prospero", "Romeo and Juliet", true)
			.answerMatch("Capulet", "Romeo and Juliet", true)
			.answerMatch("Demetrius", "A Midsummer", true)
			.answerMatch("Lysander", "A Midsummer", true)
			//ooops
			.answerMatch("Prospero", "Romeo and Juliet", false)
			.answerMatch("Prospero", "The Tempest", true)
			.saveAnswer()
			.endTest();
		//check the results
		qtiPage
			.assertOnAssessmentResults()
			.assertOnAssessmentItemScore("Characters and Plays", 3);
	}
	
	/**
	 * Check if the order interaction return its 1 point.
	 * 
	 * @param authorLoginPage
	 * @throws IOException
	 * @throws URISyntaxException
	 */
	@Test
	@RunAsClient
	public void qti21OrderInteraction(@InitialPage LoginPage authorLoginPage)
	throws IOException, URISyntaxException {
		UserVO author = new UserRestClient(deploymentUrl).createAuthor();
		authorLoginPage.loginAs(author.getLogin(), author.getPassword());
		
		//upload a test
		String qtiTestTitle = "Order QTI 2.1 " + UUID.randomUUID();
		URL qtiTestUrl = JunitTestHelper.class.getResource("file_resources/qti21/simple_QTI_21_order_ims.zip");
		File qtiTestFile = new File(qtiTestUrl.toURI());
		navBar
			.openAuthoringEnvironment()
			.uploadResource(qtiTestTitle, qtiTestFile)
			.clickToolbarRootCrumb();
		
		QTI21Page qtiPage = QTI21Page
				.getQTI12Page(browser);
		qtiPage
			.options()
			.showResults(Boolean.TRUE, QTI21AssessmentResultsOptions.allOptions())
			.save();
		// to the test and spot it
		qtiPage
			.clickToolbarBack()
			.assertOnAssessmentItem()
			.answerOrderDropItem("Jenson")
			.answerOrderDropItem("Rubens")
			.answerOrderDropItem("Michael")
			.saveAnswer()
			.endTest()
			.closeTest();
		//check the results
		qtiPage
			.assertOnAssessmentResults()
			.assertOnAssessmentItemScore("Grand Prix of Bahrain", 1);
	}

}