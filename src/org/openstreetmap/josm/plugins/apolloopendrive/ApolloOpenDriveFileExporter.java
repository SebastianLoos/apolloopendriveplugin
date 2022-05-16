package org.openstreetmap.josm.plugins.apolloopendrive;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.io.File;
import java.util.Arrays;

import javax.swing.JOptionPane;

import org.openstreetmap.josm.actions.ExtensionFileFilter;
import org.openstreetmap.josm.gui.io.importexport.FileExporter;
import org.openstreetmap.josm.gui.layer.Layer;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.util.GuiHelper;

import de.apollomasterbeuth.apolloconverter.Converter;
import de.apollomasterbeuth.apolloconverter.structure.Environment;

public class ApolloOpenDriveFileExporter extends FileExporter 
{
	private static final ExtensionFileFilter FILE_FILTER = new ExtensionFileFilter(
	        "xodr,xml", "xodr", tr("Apollo OpenDrive file") + " (*.xodr, *.xml)");
	
	
	public ApolloOpenDriveFileExporter() {
		super(FILE_FILTER);
		this.setEnabled(true);
	}
	
	@Override
	public void exportData(File file, Layer layer) {
		if (layer.getClass() == OsmDataLayer.class) {
			Environment env = ApolloOpenDriveEnvironmentCreator.createEnvironment((OsmDataLayer)layer);
			Converter conv = new Converter(file.getPath());
			conv.convertOSM(env);
		} else {
			GuiHelper.runInEDT(() -> JOptionPane.showMessageDialog(
	                null, tr("The selected layer is not an OSM layer!"), tr("Error"), JOptionPane.WARNING_MESSAGE));
		}
	}
	
}
