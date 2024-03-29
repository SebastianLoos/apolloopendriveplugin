package org.openstreetmap.josm.plugins.apolloopendrive.toosm;

import static org.openstreetmap.josm.tools.I18n.tr;

import java.util.Optional;
import java.util.concurrent.Future;

import org.openstreetmap.josm.actions.downloadtasks.DownloadOsmTask;
import org.openstreetmap.josm.actions.downloadtasks.DownloadParams;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.gui.MainApplication;
import org.openstreetmap.josm.gui.layer.OsmDataLayer;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;
import org.openstreetmap.josm.tools.Utils;

public class ApolloOpenDriveToOsmDownloadTask extends DownloadOsmTask{
	private static final String PATTERN_COMPRESS = "https?://.*/(.*\\.(xodr|xml)(\\.(gz|xz|bz2?|zip))?)";

    @Override
    public String[] getPatterns() {
        return new String[]{PATTERN_COMPRESS};
    }

    @Override
    public String getTitle() {
        return tr("Download Apollo OpenDRIVE");
    }

    @Override
    public Future<?> download(DownloadParams settings, Bounds downloadArea, ProgressMonitor progressMonitor) {
        return null;
    }

    @Override
    public Future<?> loadUrl(DownloadParams settings, String url, ProgressMonitor progressMonitor) {
        downloadTask = new InternalDownloadTask(settings, url, progressMonitor);
        return MainApplication.worker.submit(downloadTask);
    }

    class InternalDownloadTask extends DownloadTask {

        private final String url;

        InternalDownloadTask(DownloadParams settings, String url, ProgressMonitor progressMonitor) {
            super(settings, new ApolloOpenDriveToOsmServerReader(url), progressMonitor);
            this.url = url;
        }

        @Override
        protected String generateLayerName() {
            return Optional.of(url.substring(url.lastIndexOf('/')+1))
                .filter(it -> !Utils.isStripEmpty(it))
                .orElse(super.generateLayerName());
        }

        @Override
        protected OsmDataLayer createNewLayer(final DataSet dataSet, final Optional<String> layerName) {
            if (layerName.filter(Utils::isStripEmpty).isPresent()) {
                throw new IllegalArgumentException("Blank layer name!");
            }
            return new OsmDataLayer(dataSet, layerName.orElseGet(this::generateLayerName), null);
        }
    }
}
