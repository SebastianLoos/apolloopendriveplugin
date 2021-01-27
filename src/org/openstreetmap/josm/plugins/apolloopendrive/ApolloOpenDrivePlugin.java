package org.openstreetmap.josm.plugins.apolloopendrive;

import org.openstreetmap.josm.plugins.Plugin;
import org.openstreetmap.josm.plugins.PluginInformation;

import org.openstreetmap.josm.actions.ExtensionFileFilter;
import org.openstreetmap.josm.gui.MainApplication;

public class ApolloOpenDrivePlugin extends Plugin{
	private final ApolloOpenDriveFileImporter apolloOpenDriveFileImporter;
	
	public ApolloOpenDrivePlugin(PluginInformation info)
	{
		super(info);
		System.out.println("ffs");
        this.apolloOpenDriveFileImporter = new ApolloOpenDriveFileImporter();
        ExtensionFileFilter.addImporter(this.apolloOpenDriveFileImporter);
        ExtensionFileFilter.updateAllFormatsImporter();
        MainApplication.getMenu().openLocation.addDownloadTaskClass(ApolloOpenDriveDownloadTask.class);
	}
}