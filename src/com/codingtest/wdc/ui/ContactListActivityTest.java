/*
 * Copyright (c) 2011, salesforce.com, inc.
 * All rights reserved.
 * Redistribution and use of this software in source and binary forms, with or
 * without modification, are permitted provided that the following conditions
 * are met:
 * - Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * - Neither the name of salesforce.com, inc. nor the names of its contributors
 * may be used to endorse or promote products derived from this software without
 * specific prior written permission of salesforce.com, inc.
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package com.codingtest.wdc.ui;

import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.ProtocolVersion;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;

import android.app.Activity;
import android.app.Instrumentation.ActivityMonitor;
import android.content.Context;
import android.content.IntentFilter;
import android.widget.ListView;
import android.widget.TextView;

import com.codingtest.wdc.R;
import com.codingtest.wdc.rest.RestUtil;
import com.salesforce.androidsdk.app.SalesforceSDKManager;
import com.salesforce.androidsdk.auth.HttpAccess;
import com.salesforce.androidsdk.rest.ClientManager;
import com.salesforce.androidsdk.ui.LoginActivity;
import com.salesforce.androidsdk.util.BaseActivityInstrumentationTestCase;

/**
 * Tests for ContactListActivity using a MockHTTPAccessor for mocking the
 * server calls
 */
public class ContactListActivityTest extends BaseActivityInstrumentationTestCase<ContactListActivity> {

	private static final String TEST_ORG_ID = "test_org_id";
    private static final String TEST_USER_ID = "test_user_id";
    private static final String TEST_CLIENT_ID = "test_client_d";
    private static final String TEST_LOGIN_URL = "https://test.salesforce.com";
    private static final String TEST_INSTANCE_URL = "https://tapp0.salesforce.com";
    private static final String TEST_IDENTITY_URL = "https://test.salesforce.com";
    private static final String TEST_ACCESS_TOKEN = "test_access_token";
    private static final String TEST_REFRESH_TOKEN = "test_refresh_token";
    private static final String TEST_USERNAME = "test_username";
    private static final String TEST_ACCOUNT_NAME = "test_account_name";

    private static final String JSON_CONTACT_OBJECT = "{ \"FirstName\": \"John\", \"LastName\": \"Blazing\", \"Id\": \"1\", \"Account\": { \"Name\": \"Account Name\" } ," +
    													" \"Title\": \"Sr Sales Rep\", \"Email\": \"foo@bar.com\", \"Phone\": \"3124347252\"," +
    													" \"Question_1__c\": \"\", \"Question_2__c\": \"\", \"Question_3__c\": \"\" }";
    private static final String JSON_CONTACT_RECORDS = "{ \"records\": [{\"FirstName\": \"John\", \"LastName\": \"Blazing\", \"Id\": \"1\" }, " +
														"{\"FirstName\": \"Paul\", \"LastName\": \"Ginger\", \"Id\": \"2\" }]}";

    public ContactListActivityTest() {
        super("com.codingtest.wdc.ui", ContactListActivity.class);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        
		setActivityInitialTouchMode(false);
		
		Context targetContext = getInstrumentation().getTargetContext();
		ClientManager clientManager = new ClientManager(targetContext, targetContext.getString(R.string.account_type), null, SalesforceSDKManager.getInstance().shouldLogoutWhenTokenRevoked());
        clientManager.createNewAccount(TEST_ACCOUNT_NAME, TEST_USERNAME, TEST_REFRESH_TOKEN, TEST_ACCESS_TOKEN, TEST_INSTANCE_URL, 
        								TEST_LOGIN_URL, TEST_IDENTITY_URL, TEST_CLIENT_ID, TEST_ORG_ID, TEST_USER_ID, null);
        SalesforceSDKManager.getInstance().getPasscodeManager().setTimeoutMs(0 /* disabled */);
    }

    /**
     * Test the basic app flow on the phone
     *  - Contacts are loaded when app is launched
     *  - Details are populated when a contact is clicked
     *  - Details are updated when the submit is clicked
     * @throws Throwable 
     */
    public void testBasicAppFlow() throws Throwable {
        // Plug our mock access
    	String expectedResponse = JSON_CONTACT_RECORDS;
		MockHttpAccess mockHttpAccessor = new MockHttpAccess(SalesforceSDKManager.getInstance().getAppContext(), expectedResponse);
		RestUtil.HTTP_ACCESOR = mockHttpAccessor;
		
		// invoke the activity load
    	getActivity();
        waitForABit();
        waitForRender();
        
        // check if the contacts are loaded
        ContactListFragment contactListFragment = (ContactListFragment) getActivity().getSupportFragmentManager().findFragmentById(R.id.contact_list);
        assertEquals("The adapter should one contact", 2, contactListFragment.getListAdapter().getCount());

        // Plug our mock access for detail
    	expectedResponse = JSON_CONTACT_OBJECT;
		mockHttpAccessor = new MockHttpAccess(SalesforceSDKManager.getInstance().getAppContext(), expectedResponse);
		RestUtil.HTTP_ACCESOR = mockHttpAccessor;
		
        // Setup activity monitor
        ActivityMonitor monitor = getInstrumentation().addMonitor(new IntentFilter(LoginActivity.class.getName()), null, false);

		// click on the first list item
        final ListView contactListView = contactListFragment.getListView();
		runTestOnUiThread(new Runnable() {
		    @Override
		    public void run() {
		    	contactListView.performItemClick(contactListView, 0, contactListView.getItemIdAtPosition(0));
		    }
		});
		
		// if running on phone then check 
		// for detail activity
        Activity detailActivity = null;
		if (!getActivity().isTwoPane()) {
	        // Wait for detail screen
			detailActivity = monitor.waitForActivityWithTimeout(10000);
	        assertTrue(detailActivity instanceof ContactDetailActivity);
		}
        
		// need this to get reference to detail fields
		final Activity detailReference = getActivity().isTwoPane() ? getActivity() : detailActivity;

		// Wait for load to complete
        waitForABit();
        
        assertEquals("Account Name", ((TextView) detailReference.findViewById(R.id.account_detail_label)).getText());
        assertEquals("Sr Sales Rep", ((TextView) detailReference.findViewById(R.id.title_detail_label)).getText());
        
        // fill in the Question fields
		runTestOnUiThread(new Runnable() {
		    @Override
		    public void run() {
		        ((TextView) detailReference.findViewById(R.id.question_1_hint)).setText("Answer to question 1");
		        ((TextView) detailReference.findViewById(R.id.question_2_hint)).setText("Answer to question 2");
		        ((TextView) detailReference.findViewById(R.id.question_3_hint)).setText("Answer to question 3");
		    }
		});
		
        waitForABit();
        
        // Plug our mock access for detail
    	expectedResponse = "";
		mockHttpAccessor = new MockHttpAccess(SalesforceSDKManager.getInstance().getAppContext(), expectedResponse);
		RestUtil.HTTP_ACCESOR = mockHttpAccessor;
        
        // hit the submit button
        clickView(detailReference.findViewById(R.id.submit_button));
        
        waitForABit();
        
        // XXX: Unfortunately looks like there is no way to check for a toast
        // we could do a dialog instead but doesn't make sense just for the sake of
        // testing. Or may be use the EventObserver with a new event type.        
    }

	private void waitForABit() {
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			fail("Error while waiting for response");
		}
	}

    /**
     * Mock http access
     */
    private static class MockHttpAccess extends HttpAccess {
    	String response;
    	
        protected MockHttpAccess(Context app, String response) {
            super(app, null);
            this.response = response;
        }
        
        protected Execution execute(HttpRequestBase req) throws ClientProtocolException, IOException {
            HttpResponse res = new BasicHttpResponse(new BasicStatusLine(new ProtocolVersion("http", 1, 1), HttpStatus.SC_OK, null), null, null);
            res.setEntity(new StringEntity(response));
            return new Execution(req, res);
        }
    }
}
