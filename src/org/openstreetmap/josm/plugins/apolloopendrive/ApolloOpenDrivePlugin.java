package org.openstreetmap.josm.plugins.apolloopendrive;

import org.openstreetmap.josm.plugins.Plugin;
import org.openstreetmap.josm.plugins.PluginInformation;
import org.openstreetmap.josm.plugins.apolloopendrive.toosm.ApolloOpenDriveToOsmDownloadTask;
import org.openstreetmap.josm.plugins.apolloopendrive.toosm.ApolloOpenDriveToOsmFileImporter;
import org.openstreetmap.josm.actions.ExtensionFileFilter;
import org.openstreetmap.josm.gui.MainApplication;

public class ApolloOpenDrivePlugin extends Plugin{
	private final ApolloOpenDriveFileImporter apolloOpenDriveFileImporter;
	private final ApolloOpenDriveToOsmFileImporter apolloOpenDriveToOsmFileImporter;
	private final ApolloOpenDriveFileExporter apolloOpenDriveFileExporter;
	
	public ApolloOpenDrivePlugin(PluginInformation info)
	{
		super(info);
        this.apolloOpenDriveFileImporter = new ApolloOpenDriveFileImporter();
        this.apolloOpenDriveToOsmFileImporter = new ApolloOpenDriveToOsmFileImporter();
        this.apolloOpenDriveFileExporter = new ApolloOpenDriveFileExporter();
        ExtensionFileFilter.addImporter(this.apolloOpenDriveFileImporter);
        ExtensionFileFilter.addImporter(this.apolloOpenDriveToOsmFileImporter);
        ExtensionFileFilter.addExporter(this.apolloOpenDriveFileExporter);
        ExtensionFileFilter.updateAllFormatsImporter();
        MainApplication.getMenu().openLocation.addDownloadTaskClass(ApolloOpenDriveDownloadTask.class);
        MainApplication.getMenu().openLocation.addDownloadTaskClass(ApolloOpenDriveToOsmDownloadTask.class);
        ExtensionFileFilter.getExporters().forEach(x -> {
        	org.openstreetmap.josm.tools.Logging.info(x.filter.getDefaultExtension());
        });
	}
}