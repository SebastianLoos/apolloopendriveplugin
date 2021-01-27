package org.openstreetmap.josm.plugins.apolloopendrive;

import org.openstreetmap.josm.plugins.apolloopendrive.xml.OpenDRIVE;
import org.xml.sax.helpers.DefaultHandler;

public class ApolloOpenDriveHandler extends DefaultHandler {

	private OpenDRIVE openDriveData;
	
	public OpenDRIVE getOpenDriveData() {
		return openDriveData;
	}
	
	
}
