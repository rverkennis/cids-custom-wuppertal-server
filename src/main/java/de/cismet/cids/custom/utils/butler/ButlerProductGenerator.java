/***************************************************
*
* cismet GmbH, Saarbruecken, Germany
*
*              ... and it just works.
*
****************************************************/
/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.cismet.cids.custom.utils.butler;

import Sirius.server.newuser.User;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import org.apache.log4j.Logger;

import org.openide.util.Exceptions;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;

import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * DOCUMENT ME!
 *
 * @author   daniel
 * @version  $Revision$, $Date$
 */
public class ButlerProductGenerator {

    //~ Static fields/initializers ---------------------------------------------

    private static final ButlerProductGenerator instance = new ButlerProductGenerator();
    private static final Logger LOG = Logger.getLogger(ButlerProductGenerator.class);
    private static final String pdfResultDir = "pdf";
    private static final String dxfResultDir = "dxf";
    private static final String tifResultDir = "tif";
    private static final String shapeResultDir = "shape";
    private static final String FILE_APPENDIX = ".but";
    private static final String SEPERATOR = ";";

    //~ Instance fields --------------------------------------------------------

    final File openOrdersLogFile;
    // Map that lists all open Orders to a user id
    private HashMap<Integer, HashMap<String, ButlerRequestInfo>> openOrderMap =
        new HashMap<Integer, HashMap<String, ButlerRequestInfo>>();
    private final String requestFolder;
    private final String resultBaseFolder;
    private final String butlerBasePath;
    private boolean initError = false;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new ButlerProductGenerator object.
     *
     * @throws  RuntimeException  DOCUMENT ME!
     */
    private ButlerProductGenerator() {
        try {
            final Properties butlerProperties = new Properties();
            butlerProperties.load(ButlerProductGenerator.class.getResourceAsStream("butler.properties"));
            butlerBasePath = butlerProperties.getProperty("butlerBasePath");
            requestFolder = butlerBasePath + System.getProperty("file.separator")
                        + butlerProperties.getProperty("butler1RequestPath");
            resultBaseFolder = butlerBasePath + System.getProperty("file.separator")
                        + butlerProperties.getProperty("butler1ResultPath");
            final StringBuilder fileNameBuilder = new StringBuilder(butlerBasePath);
            fileNameBuilder.append(System.getProperty("file.separator"));
            openOrdersLogFile = new File(fileNameBuilder.toString() + "openOrders.json");
            if (!openOrdersLogFile.exists()) {
                openOrdersLogFile.createNewFile();
                // serialiaze en empty map to the file to avoid parsing exception
                updateJsonLogFiles();
            }
            if (!(openOrdersLogFile.isFile() && openOrdersLogFile.canWrite())) {
                LOG.error("can not write to open order log file");
            }
            loadOpenOrdersFromJsonFile();
        } catch (IOException ex) {
            LOG.error("Could not load butler properties", ex);
            throw new RuntimeException(ex);
        }
        checkFolders();
    }

    //~ Methods ----------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public static ButlerProductGenerator getInstance() {
        return instance;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   orderNumber  DOCUMENT ME!
     * @param   user         that sends the request
     * @param   product      of the product that shall be generated
     * @param   minX         lower x coordinate of the rectangle the product is generated for
     * @param   minY         lower y coordinate of the rectangle the product is generated for
     * @param   maxX         upper x coordinate of the rectangle the product is generated for
     * @param   maxY         lower y coordinate of the rectangle the product is generated for
     * @param   isGeoTiff    default no, set only to true if <code>format.equals("tif")</code> and the output file
     *                       should end with *.geotiff instead of *.tif
     *
     * @return  the requestId necessary to retrive results with
     *          {@link #getResultForRequest(java.lang.String, java.lang.String)} method
     */
    public String createButlerRequest(final String orderNumber,
            final User user,
            final ButlerProduct product,
            final double minX,
            final double minY,
            final double maxX,
            final double maxY,
            final boolean isGeoTiff) {
        if (!initError) {
            File reqeustFile = null;
            FileWriter fw = null;
            final String filename = determineRequestFileName(user, orderNumber);
            addToOpenOrderMap(user, filename, orderNumber, product);
            try {
                reqeustFile = new File(requestFolder + System.getProperty("file.separator") + filename
                                + FILE_APPENDIX);
                if (reqeustFile.exists()) {
                    // should not happen;
                    LOG.error("butler 1 request file already exists");
                    return null;
                }
                fw = new FileWriter(reqeustFile);
                final BufferedWriter bw = new BufferedWriter(fw);
                bw.write(getRequestLine(
                        product.getKey(),
                        minX,
                        minY,
                        maxX,
                        maxY,
                        product.getColorDepth(),
                        product.getResolution().getKey(),
                        isGeoTiff,
                        product.getFormat().getKey()));
                bw.close();
                return filename;
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            } finally {
                try {
                    fw.close();
                } catch (IOException ex) {
                    Exceptions.printStackTrace(ex);
                }
            }
        }
        return null;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   user       DOCUMENT ME!
     * @param   requestId  represents the request
     * @param   format     must be one of the following "dxf", "shp","tif"
     *
     * @return  A list of bytes representing the result files <code>null</code> if there are no result files
     */
    public ArrayList<byte[]> getResultForRequest(final User user, final String requestId, final String format) {
        File resultDir;
        if (format.equals("dxf")) {
            resultDir = new File(resultBaseFolder + System.getProperty("file.separator") + dxfResultDir);
        } else if (format.equals("shp")) {
            resultDir = new File(resultBaseFolder + System.getProperty("file.separator") + shapeResultDir);
        } else if (format.equals("tif")) {
            resultDir = new File(resultBaseFolder + System.getProperty("file.separator") + tifResultDir);
        } else if (format.equals("geotif")) {
            resultDir = new File(resultBaseFolder + System.getProperty("file.separator") + tifResultDir);
        } else {
            // this must be true here: format.equals("pdf")
            resultDir = new File(resultBaseFolder + System.getProperty("file.separator") + pdfResultDir);
        }

        // get a list of files with the respective fileName and read them all
        final String regex = (requestId + ".*").replaceAll("\\+", "\\\\\\+");
        final File[] resultFiles = resultDir.listFiles(new FileFilter() {

                    @Override
                    public boolean accept(final File file) {
                        return file.getName().matches(regex);
                    }
                });
        // if there the list is emtpy the butler service hasnt finished the request
        if ((resultFiles == null) || (resultFiles.length <= 0)) {
            LOG.info("could not find the result file for butler order " + requestId
                        + ". Maybe the server side processing isn't finished");
            return null;
        }

        removeFromOpenOrders(user, requestId);
        final ArrayList<byte[]> result = new ArrayList<byte[]>();
        for (int i = 0; i < resultFiles.length; i++) {
            result.add(loadFile(resultFiles[i]));
        }

        return result;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   user  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public HashMap<String, ButlerRequestInfo> getAllOpenUserRequests(final User user) {
        if (openOrderMap.keySet().contains(user.getId())) {
            final HashMap<String, ButlerRequestInfo> result = new HashMap<String, ButlerRequestInfo>();
            result.putAll(openOrderMap.get(user.getId()));
            return result;
        }
        return null;
    }

    /**
     * DOCUMENT ME!
     *
     * @throws  RuntimeException  DOCUMENT ME!
     */
    private void checkFolders() {
        final File requestDir = new File(requestFolder);
        if (!requestDir.exists()) {
            requestDir.mkdirs();
        }
        if (!requestDir.isDirectory() || !requestDir.canWrite()) {
            LOG.error("could not write to the given butler request directory " + requestDir);
            throw new RuntimeException("could not write to the given butler request directory " + requestDir);
        }

        final File resultBaseDir = new File(resultBaseFolder);
        if (!resultBaseDir.exists()) {
            resultBaseDir.mkdirs();
        }
        final File pdfDir = new File(resultBaseDir + System.getProperty("file.separator") + pdfResultDir);
        final File dxfDir = new File(resultBaseDir + System.getProperty("file.separator") + dxfResultDir);
        final File tifDir = new File(resultBaseDir + System.getProperty("file.separator") + tifResultDir);
        final File shapeDir = new File(resultBaseDir + System.getProperty("file.separator") + shapeResultDir);

        if (!(pdfDir.exists() && dxfDir.exists() && tifDir.exists() && shapeDir.exists())) {
            LOG.fatal("one ore all of butler result directories does not exists");
            initError = true;
            return;
        }

        if (!(pdfDir.isDirectory() && pdfDir.canExecute() && dxfDir.isDirectory() && dxfDir.canExecute()
                        && tifDir.isDirectory() && tifDir.canExecute()
                        && shapeDir.isDirectory() && shapeDir.canExecute())) {
            LOG.fatal("can not write to one ore all of butler result directories");
            initError = true;
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param   user       DOCUMENT ME!
     * @param   requestId  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private String determineRequestFileName(final User user, final String requestId) {
        final GregorianCalendar cal = new GregorianCalendar();
        cal.setTime(new Date());

        return user.getName() + "_" + requestId; // + "_" + cal.get(GregorianCalendar.HOUR_OF_DAY)
//                    + "+" + cal.get(GregorianCalendar.MINUTE)
//                    + "+" + cal.get(GregorianCalendar.SECOND);
    }

    /**
     * DOCUMENT ME!
     *
     * @param   productId   DOCUMENT ME!
     * @param   minX        DOCUMENT ME!
     * @param   minY        DOCUMENT ME!
     * @param   maxX        DOCUMENT ME!
     * @param   maxY        DOCUMENT ME!
     * @param   colorDepth  DOCUMENT ME!
     * @param   resolution  DOCUMENT ME!
     * @param   geoTiff     DOCUMENT ME!
     * @param   format      DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private String getRequestLine(final String productId,
            final double minX,
            final double minY,
            final double maxX,
            final double maxY,
            final int colorDepth,
            final String resolution,
            final boolean geoTiff,
            final String format) {
        //
        final StringBuffer buffer = new StringBuffer();
        // product id
        buffer.append(productId);
        buffer.append(SEPERATOR);

        // coordinates
        buffer.append(minX);
        buffer.append(SEPERATOR);
        buffer.append(minY);
        buffer.append(SEPERATOR);
        buffer.append(maxX);
        buffer.append(SEPERATOR);
        buffer.append(maxY);
        buffer.append(SEPERATOR);

        // colordepth
        buffer.append(colorDepth);
        buffer.append(SEPERATOR);

        // resolution
        buffer.append(resolution);
        buffer.append(SEPERATOR);

        // geotif
        if (geoTiff) {
            buffer.append("yes");
        } else {
            buffer.append("no");
        }
        buffer.append(SEPERATOR);

        // format
        buffer.append(format);
        return buffer.toString();
    }

    /**
     * DOCUMENT ME!
     *
     * @param   f  userKey DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private byte[] loadFile(final File f) {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        InputStream is = null;
        try {
            is = new FileInputStream(f);
            final byte[] buffer = new byte[8192];
            int length = is.read(buffer, 0, 8192);
            while (length != -1) {
                bos.write(buffer, 0, length);
                length = is.read(buffer, 0, 8192);
            }
            return bos.toByteArray();
        } catch (FileNotFoundException ex) {
            LOG.error("could not find result file " + f.getName());
        } catch (IOException ex) {
            LOG.error("error during loading file " + f.getName());
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
                bos.close();
            } catch (IOException ex) {
            }
        }
        return null;
    }

    /**
     * DOCUMENT ME!
     *
     * @param  args  DOCUMENT ME!
     */
    public static void main(final String[] args) {
        final ButlerProductGenerator generator = ButlerProductGenerator.getInstance();
        final User user = new User(100, "admin", "wunda_blau");
        final String productId = "0404";
        final double minX = 370000d;
        final double minY = 5680000d;
        final double maxX = 370300d;
        final double maxY = 5680300d;
        final int colorDepth = 8;
        final ButlerResolution resolution = new ButlerResolution();
        resolution.setKey("0.50");
        final boolean useGeoTif = true;
        final ButlerFormat format = new ButlerFormat("tif");
        System.out.println("Sending request to butler");
        final ButlerProduct product = new ButlerProduct();
        product.setKey(productId);
        product.setColorDepth(colorDepth);
        product.setResolution(resolution);
        product.setFormat(format);
        final String requestId = generator.createButlerRequest(
                "cismet_test",
                user,
                product,
                minX,
                minY,
                maxX,
                maxY,
                useGeoTif);
        System.out.println("Sent request, Received request id " + requestId + " for result polling");
        System.out.println("Polling result for request id " + requestId);
        ArrayList<byte[]> files = generator.getResultForRequest(new User(1, "admin", "wunda_blau"),
                requestId,
                format.getKey());
        while (files == null) {
            System.out.println("Requesting results for " + requestId + " is not finished, try later again");
            try {
                Thread.sleep(2000);
            } catch (InterruptedException ex) {
                Exceptions.printStackTrace(ex);
            }
            files = generator.getResultForRequest(new User(1, "admin", "wunda_blau"), requestId, format.getKey());
        }

        System.out.println("Received " + files.size() + " Files from butler server");
    }

    /**
     * DOCUMENT ME!
     *
     * @param  user         DOCUMENT ME!
     * @param  requestId    filename DOCUMENT ME!
     * @param  userOrderId  DOCUMENT ME!
     * @param  product      DOCUMENT ME!
     */
    private void addToOpenOrderMap(final User user,
            final String requestId,
            final String userOrderId,
            final ButlerProduct product) {
        HashMap<String, ButlerRequestInfo> openUserOrders = (HashMap<String, ButlerRequestInfo>)openOrderMap.get(
                user.getId());
        if (openUserOrders == null) {
            openUserOrders = new HashMap<String, ButlerRequestInfo>();
            openOrderMap.put(user.getId(), openUserOrders);
        }
        openUserOrders.put(requestId, new ButlerRequestInfo(userOrderId, product));
        updateJsonLogFiles();
    }

    /**
     * DOCUMENT ME!
     *
     * @param  user       DOCUMENT ME!
     * @param  requestId  DOCUMENT ME!
     */
    private void removeFromOpenOrders(final User user, final String requestId) {
        final HashMap<String, ButlerRequestInfo> openUserOrders = (HashMap<String, ButlerRequestInfo>)openOrderMap.get(
                user.getId());
        if (openUserOrders != null) {
            openUserOrders.remove(requestId);
        }
    }

    /**
     * DOCUMENT ME!
     */
    private void updateJsonLogFiles() {
        final ObjectMapper mapper = new ObjectMapper();
        final ObjectWriter writer = mapper.writerWithDefaultPrettyPrinter();

        try {
            final HashMap<Integer, OpenOrderMapWrapper> mapToSerialize = new HashMap<Integer, OpenOrderMapWrapper>();
            for (final Integer i : openOrderMap.keySet()) {
                final OpenOrderMapWrapper openuserOders = new OpenOrderMapWrapper(openOrderMap.get(i));
                mapToSerialize.put(i, openuserOders);
            }
            writer.writeValue(openOrdersLogFile, mapToSerialize);
        } catch (IOException ex) {
            LOG.error("error during writing open butler orders to log file", ex);
        }
    }

    /**
     * DOCUMENT ME!
     */
    private void loadOpenOrdersFromJsonFile() {
        try {
            final ObjectMapper mapper = new ObjectMapper();
            final HashMap<Integer, OpenOrderMapWrapper> wrapperMap = mapper.readValue(
                    openOrdersLogFile,
                    new TypeReference<HashMap<Integer, OpenOrderMapWrapper>>() {
                    });

            for (final Integer i : wrapperMap.keySet()) {
                openOrderMap.put(i, wrapperMap.get(i).getMap());
            }
        } catch (JsonParseException ex) {
            LOG.error("Could not parse nas order log files", ex);
        } catch (JsonMappingException ex) {
            LOG.error("error while json mapping/unmarshalling of nas order log file", ex);
        } catch (IOException ex) {
            LOG.error("error while loading nas order log file", ex);
        }
    }

    //~ Inner Classes ----------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    private static final class OpenOrderMapWrapper {

        //~ Instance fields ----------------------------------------------------

        private HashMap<String, ButlerRequestInfo> map;

        //~ Constructors -------------------------------------------------------

        /**
         * Creates a new OpenOrderMapWrapper object.
         */
        public OpenOrderMapWrapper() {
        }

        /**
         * Creates a new OpenOrderMapWrapper object.
         *
         * @param  map  DOCUMENT ME!
         */
        public OpenOrderMapWrapper(final HashMap<String, ButlerRequestInfo> map) {
            this.map = map;
        }

        //~ Methods ------------------------------------------------------------

        /**
         * DOCUMENT ME!
         *
         * @return  DOCUMENT ME!
         */
        public HashMap<String, ButlerRequestInfo> getMap() {
            return map;
        }

        /**
         * DOCUMENT ME!
         *
         * @param  map  DOCUMENT ME!
         */
        public void setMap(final HashMap<String, ButlerRequestInfo> map) {
            this.map = map;
        }
    }
}
