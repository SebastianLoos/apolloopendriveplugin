package org.openstreetmap.josm.plugins.apolloopendrive.toosm;

import static org.openstreetmap.josm.tools.I18n.tr;

import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.io.OsmServerReader;
import org.openstreetmap.josm.io.OsmTransferException;

public class ApolloOpenDriveToOsmServerReader extends OsmServerReader {
	
	private final String url;
	
	public ApolloOpenDriveToOsmServerReader(String url){
		this.url = url;
	}
	
	@Override
	public DataSet parseOsm(ProgressMonitor progressMonitor) throws OsmTransferException {
		try {
            progressMonitor.beginTask(tr("Contacting Server…"), 10);
            return new ApolloOpenDriveToOsmFileImporter().parseDataSet(url);
        } catch (Exception e) {
            throw new OsmTransferException(e);
        } finally {
            progressMonitor.finishTask();
        }        
	}
}
