package org.dbpedia.extraction.live.mirror;

import org.dbpedia.extraction.live.mirror.changesets.Changeset;
import org.dbpedia.extraction.live.mirror.changesets.ChangesetExecutor;
import org.dbpedia.extraction.live.mirror.download.DownloadTimeCounter;
import org.dbpedia.extraction.live.mirror.download.LastDownloadDateManager;
import org.dbpedia.extraction.live.mirror.helper.*;
import org.dbpedia.extraction.live.mirror.download.UpdatesIterator;
import org.dbpedia.extraction.live.mirror.sparul.JDBCPoolConnection;
import org.dbpedia.extraction.live.mirror.sparul.SPARULGenerator;
import org.dbpedia.extraction.live.mirror.sparul.SPARULVosExecutor;
import org.slf4j.Logger;

import java.util.Arrays;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: Mohamed Morsey
 * Date: 5/24/11
 * Time: 4:26 PM
 * This class is originally created from class defined in http://www.devdaily.com/java/edu/pj/pj010011
 * which is created by http://www.DevDaily.com
 */
public final class LiveSync {


    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(LiveSync.class);

    private static final String LASTDOWNLOAD = "lastDownloadDate.dat";

    private LiveSync(){}

    public static void main(String[] args) {

        ChangesetExecutor changesetExecutor = new ChangesetExecutor(new SPARULVosExecutor(), new SPARULGenerator(Global.getOptions().get("LiveGraphURI")));

        String updateServerAddress = Global.getOptions().get("UpdateServerAddress");
        String UpdatesDownloadFolder = Global.getOptions().get("UpdatesDownloadFolder");
        String addedTriplesFileExtension = Global.getOptions().get("addedTriplesFileExtension");
        String removedTriplesFileExtension = Global.getOptions().get("removedTriplesFileExtension");

        DownloadTimeCounter lastDownload = LastDownloadDateManager.getLastDownloadDate(LASTDOWNLOAD);
        UpdatesIterator iterator = new UpdatesIterator(lastDownload, 3,  Integer.parseInt(Global.getOptions().get("MaximumNumberOfSuccessiveFailedTrials")));

        while (iterator.hasNext()) {
            DownloadTimeCounter cntr = iterator.next();

            //Since next returns null, if now new updates are available, then we should not go any further,
            //and just wait for mor updates
            if (cntr == null)
                continue;

            //u = new URL("http://dbpedia.aksw.org/updates.live.dbpedia.org/2011/05/22/00/000001.added.nt.gz");
//            String fullFilenameToBeDownloaded = Global.options.get("UpdateServerAddress") + lastDownload.getFormattedFilePath() + ".added.nt.gz";
            //String compressedDownloadedFile = FileDownloader.downloadFile(options.get("UpdateServerAddress") + "2011/05/22/00/000001.added.nt.gz",
            //         options.get("UpdatesDownloadFolder"));

            String addedTriplesFilename, deletedTriplesFilename;

            addedTriplesFilename = updateServerAddress + cntr.getFormattedFilePath() + addedTriplesFileExtension;

            deletedTriplesFilename = updateServerAddress + cntr.getFormattedFilePath() + removedTriplesFileExtension;

            // changesets default to empty
            List<String> triplesToDelete = Arrays.asList();
            List<String> triplesToAdd = Arrays.asList();

            //Download and decompress the file of deleted triples
            String deletedCompressedDownloadedFile = Utils.downloadFile(deletedTriplesFilename, UpdatesDownloadFolder);

            if (deletedCompressedDownloadedFile.compareTo("") != 0) {

                String decompressedDeletedNTriplesFile = Utils.decompressGZipFile(deletedCompressedDownloadedFile);
                triplesToDelete = Utils.getTriplesFromFile(decompressedDeletedNTriplesFile);

                Utils.deleteFile(decompressedDeletedNTriplesFile);

                //Reset the number of failed trails, since the file is found and downloaded successfully
                Global.setNumberOfSuccessiveFailedTrails(0);
            }

            //Download and decompress the file of added triples
            String addedCompressedDownloadedFile = Utils.downloadFile(addedTriplesFilename, UpdatesDownloadFolder);

            if (addedCompressedDownloadedFile.compareTo("") != 0) {
                String decompressedAddedNTriplesFile = Utils.decompressGZipFile(addedCompressedDownloadedFile);
                triplesToAdd = Utils.getTriplesFromFile(decompressedAddedNTriplesFile);

                Utils.deleteFile(decompressedAddedNTriplesFile);

                //Reset the number of failed trails, since the file is found and downloaded successfully
                Global.setNumberOfSuccessiveFailedTrails(0);
            }

            Changeset changeset = new Changeset(cntr.toString(), triplesToAdd, triplesToDelete);
            changesetExecutor.applyChangeset(changeset);


            //No files with that sequence so that indicates a failed trail, so we increment the counter of unsuccessful queries
            if ((addedCompressedDownloadedFile.compareTo("") == 0) && (deletedCompressedDownloadedFile.compareTo("") == 0)) {
                Global.setNumberOfSuccessiveFailedTrails(Global.getNumberOfSuccessiveFailedTrails() + 1);
            }
            LastDownloadDateManager.writeLastDownloadDate(LASTDOWNLOAD, cntr.toString());

        }

        JDBCPoolConnection.shutdown();

    }  // end of main

}