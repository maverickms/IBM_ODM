/*
* Licensed Materials - Property of IBM
* 5725-B69 5655-Y17 5655-Y31 5724-X98 5724-Y15 5655-V82 
* Copyright IBM Corp. 1987, 2018. All Rights Reserved.
*
* Note to U.S. Government Users Restricted Rights: 
* Use, duplication or disclosure restricted by GSA ADP Schedule 
* Contract with IBM Corp.
*/

package com.ibm.odm.samples;

import java.util.Locale;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Messages {
	private static final String BUNDLE_NAME = "com.ibm.odm.samples.messages"; //$NON-NLS-1$

	private Messages() {
	}
	
	public static String getString(String key) {
        String text = null;
        try {
            ResourceBundle bundle = ResourceBundle.getBundle(BUNDLE_NAME);
            text = bundle.getString(key);
        } catch (Exception e) {
        	e.printStackTrace();
        	Logger logger = Logger.getLogger(Messages.class.getName());
        	logger.log(Level.INFO, "Unable to retrieve key in bundle ' key='" + key  +"'"); 
            ResourceBundle bundle = ResourceBundle.getBundle(BUNDLE_NAME, Locale.US);
            text = bundle.getString(key);
        }
        if (text==null) text = key;
        return text;
    }
	

}
