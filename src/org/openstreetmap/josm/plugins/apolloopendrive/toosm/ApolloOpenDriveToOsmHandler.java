package org.openstreetmap.josm.plugins.apolloopendrive.toosm;

import org.openstreetmap.josm.plugins.apolloopendrive.xml.OpenDRIVE;
import org.xml.sax.helpers.DefaultHandler;

public class ApolloOpenDriveToOsmHandler extends DefaultHandler {
	
	private OpenDRIVE openDriveData;
	
	public OpenDRIVE getOpenDriveData() {
		return openDriveData;
	}
}
