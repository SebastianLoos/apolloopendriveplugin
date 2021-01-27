package org.openstreetmap.josm.plugins.apolloopendrive;

import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.io.OsmServerReader;
import org.openstreetmap.josm.io.OsmTransferException;

import static org.openstreetmap.josm.tools.I18n.tr;

public class ApolloOpenDriveServerReader extends OsmServerReader {

	private final String url;
	
	public ApolloOpenDriveServerReader(String url){
		this.url = url;
	}
	
	@Override
	public DataSet parseOsm(ProgressMonitor progressMonitor) throws OsmTransferException {
		try {
            progressMonitor.beginTask(tr("Contacting Server…"), 10);
            return new ApolloOpenDriveFileImporter().parseDataSet(url);
        } catch (Exception e) {
            throw new OsmTransferException(e);
        } finally {
            progressMonitor.finishTask();
        }        
	}

}
