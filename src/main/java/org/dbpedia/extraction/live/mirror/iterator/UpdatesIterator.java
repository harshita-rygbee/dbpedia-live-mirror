package org.dbpedia.extraction.live.mirror.iterator;

import org.dbpedia.extraction.live.mirror.helper.DownloadTimeCounter;
import org.dbpedia.extraction.live.mirror.helper.Global;
import org.dbpedia.extraction.live.mirror.helper.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.util.Calendar;
import java.util.Iterator;

/**
 * Created by IntelliJ IDEA.
 * User: Mohamed Morsey
 * Date: 5/26/11
 * Time: 9:21 PM
 * To change this template use File | Settings | File Templates.
 */
public class UpdatesIterator implements Iterator<DownloadTimeCounter> {

    private static final Logger logger = LoggerFactory.getLogger(UpdatesIterator.class);
    private final int delay;
    private final int maximumNumberOfSuccessiveFailedTrials; //Integer.parseInt(Global.getOptions().get("maximumNumberOfSuccessiveFailedTrials"))

    private final DownloadTimeCounter counter;

    /**
     * Initializes UpdatesIterator object
     *  @param startingCounter Counter containing the starting point
     * @param delayInterval   Interval to wait in case there is no new items available (in seconds)
     * @param maximumNumberOfSuccessiveFailedTrials
     */
    public UpdatesIterator(DownloadTimeCounter startingCounter, int delayInterval, int maximumNumberOfSuccessiveFailedTrials) {
        this.maximumNumberOfSuccessiveFailedTrials = maximumNumberOfSuccessiveFailedTrials;

        counter = new DownloadTimeCounter(startingCounter);

        delay = delayInterval * 1000;
    }

    public boolean hasNext() {

        Calendar cal = Calendar.getInstance();
        //cal.setTime(new Date(111,5,1));

        while (true) {
            String lastPublishFile = Global.getOptions().get("UpdateServerAddress") + Global.getOptions().get("lastPublishedFilename");
            Utils.downloadFile(lastPublishFile, Global.getOptions().get("UpdatesDownloadFolder"));


            String strLastPublishDate = "";
            FileInputStream fsLastResponseDateFile = null;

            try {
                fsLastResponseDateFile = new FileInputStream(Global.getOptions().get("UpdatesDownloadFolder") +
                        Global.getOptions().get("lastPublishedFilename"));

                int ch;
                strLastPublishDate = "";
                while ((ch = fsLastResponseDateFile.read()) != -1) {
                    strLastPublishDate += (char) ch;
                }

                DownloadTimeCounter lastPublishCounter = new DownloadTimeCounter(strLastPublishDate, maximumNumberOfSuccessiveFailedTrials);

                if (counter.compareTo(lastPublishCounter) < 0) {
                    //fsLastResponseDateFile.close();
                    return true;
                } else {

                    cal.add(Calendar.HOUR, 1);

                    logger.info("No new updates available waiting for " + delay + " mSec");
                    Thread.sleep(delay);
                    fsLastResponseDateFile.close();
                }

            } catch (Exception exp) {
                logger.warn("Last publish date file cannot be read due to " + exp.getMessage(), exp);
                exp.printStackTrace();
            } finally {
                try {
                    if (fsLastResponseDateFile != null)
                        fsLastResponseDateFile.close();

                } catch (Exception exp) {
                    logger.warn("File " + lastPublishFile + " cannot be closed due to " + exp.getMessage(), exp);
                }

            }
        }

    }

    public DownloadTimeCounter next() {
//		if (!firstRun) {
//			try {
//				logger.info("Waiting " + delay + "ms");
//				Thread.sleep(delay, 0);
//			}
//			catch (Exception e) {
//				logger.warn(ExceptionUtil.toString(e));
//			}
//		}
//		firstRun = false;
//
//		T result = iterator.next();
//
//		return result;

        //If no new updates then we should return null;
        /*if(!this.hasNext())
            return null;*/


        counter.advancePatch();
        return counter;
        //return null;
    }

    public void remove() {
        throw new UnsupportedOperationException("Remove is not supported for that iterator");
    }

}
