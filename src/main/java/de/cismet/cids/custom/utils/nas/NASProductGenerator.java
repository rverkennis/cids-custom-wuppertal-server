/***************************************************
*
* cismet GmbH, Saarbruecken, Germany
*
*              ... and it just works.
*
****************************************************/
package de.cismet.cids.custom.utils.nas;

import Sirius.server.newuser.User;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import com.sun.org.apache.xml.internal.serialize.OutputFormat;
import com.sun.org.apache.xml.internal.serialize.XMLSerializer;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.GeometryCollection;

import de.aed_sicad.namespaces.svr.AMAuftragServer;
import de.aed_sicad.namespaces.svr.AuftragsManager;
import de.aed_sicad.namespaces.svr.AuftragsManagerSoap;

import org.openide.util.Exceptions;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.StringWriter;

import java.net.MalformedURLException;
import java.net.URL;

import java.rmi.RemoteException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 * DOCUMENT ME!
 *
 * @author   daniel
 * @version  $Revision$, $Date$
 */
public class NASProductGenerator {

    //~ Static fields/initializers ---------------------------------------------

    private static final String FILE_APPENDIX = ".xml";
    private static NASProductGenerator instance;
    private static final int REQUEST_PERIOD = 3000;
    private static final String REQUEST_PLACE_HOLDER = "REQUEST-ID";
    private static final String DATA_FORMAT_STD = "<datenformat>1000</datenformat>";
    private static final String DATA_FORMAT_500 = "<datenformat>NAS_500m</datenformat>";

    //~ Instance fields --------------------------------------------------------

    final File openOrdersLogFile;
    final File undeliveredOrdersLogFile;
    private final transient org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(this.getClass());
    private AuftragsManagerSoap manager;
    private final String SERVICE_URL;
    private final String USER;
    private final String PW;
    private final String OUTPUT_DIR;
    private HashMap<String, HashMap<String, Boolean>> openOrderMap = new HashMap<String, HashMap<String, Boolean>>();
    private HashMap<String, HashMap<String, Boolean>> undeliveredOrderMap =
        new HashMap<String, HashMap<String, Boolean>>();
    private HashMap<String, NasProductDownloader> downloaderMap = new HashMap<String, NasProductDownloader>();

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new NASProductGenerator object.
     *
     * @throws  RuntimeException  DOCUMENT ME!
     */
    private NASProductGenerator() {
        final Properties serviceProperties = new Properties();
        try {
            serviceProperties.load(NASProductGenerator.class.getResourceAsStream("nasServer_conf.properties"));
            SERVICE_URL = serviceProperties.getProperty("service");
            USER = serviceProperties.getProperty("user");
            PW = serviceProperties.getProperty("password");
            OUTPUT_DIR = serviceProperties.getProperty("outputDir");
            final File outputDir = new File(OUTPUT_DIR);
            if (!outputDir.exists()) {
                outputDir.mkdirs();
            }
            if (!outputDir.isDirectory() || !outputDir.canWrite()) {
                log.error("could not write to the given nas output directory " + outputDir);
                throw new RuntimeException("could not write to the given nas output directory " + outputDir);
            }
            final StringBuilder fileNameBuilder = new StringBuilder(OUTPUT_DIR);
            fileNameBuilder.append(System.getProperty("file.separator"));
            openOrdersLogFile = new File(fileNameBuilder.toString() + "openOrdersMap.json");
            undeliveredOrdersLogFile = new File(fileNameBuilder.toString() + "undeliveredOrdersMap.json");
            if (!openOrdersLogFile.exists()) {
                openOrdersLogFile.createNewFile();
            }
            if (!undeliveredOrdersLogFile.exists()) {
                undeliveredOrdersLogFile.createNewFile();
            }
            if (!(openOrdersLogFile.isFile() && openOrdersLogFile.canWrite())
                        || !(undeliveredOrdersLogFile.isFile() && undeliveredOrdersLogFile.canWrite())) {
                log.error("could not write to order log files");
            }
            initFromOrderLogFiles();
        } catch (Exception ex) {
            log.fatal("NAS Datenabgabe initialisation Error!", ex);
            throw new RuntimeException(ex);
        }
    }

    //~ Methods ----------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public static NASProductGenerator instance() {
        if (instance == null) {
            instance = new NASProductGenerator();
        }
        return instance;
    }

    /**
     * DOCUMENT ME!
     */
    private void initFromOrderLogFiles() {
        try {
            final ObjectMapper mapper = new ObjectMapper();
            openOrderMap = transformJsonMap(mapper.readValue(openOrdersLogFile, Map.class));
            undeliveredOrderMap = transformJsonMap(mapper.readValue(undeliveredOrdersLogFile, Map.class));
            // check of there are open orders that arent downloaded from the 3a server yet
            for (final String userId : openOrderMap.keySet()) {
                final HashMap<String, Boolean> openOrderIds = openOrderMap.get(userId);
                for (final String orderId : openOrderIds.keySet()) {
                    final NasProductDownloader downloader = new NasProductDownloader(userId, orderId);
                    downloaderMap.put(orderId, downloader);
                    final Thread workerThread = new Thread(downloader);
                    workerThread.start();
                }
            }
        } catch (JsonParseException ex) {
            log.error("Could not parse nas order log files", ex);
        } catch (JsonMappingException ex) {
            log.error("error while json mapping/unmarshalling of nas order log file", ex);
        } catch (IOException ex) {
            log.error("error while loading nas order log file", ex);
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param   loadedJsonObj  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private HashMap<String, HashMap<String, Boolean>> transformJsonMap(
            final Map<String, HashMap<String, Boolean>> loadedJsonObj) {
        final HashMap<String, HashMap<String, Boolean>> map = new HashMap<String, HashMap<String, Boolean>>();
        for (final String user : loadedJsonObj.keySet()) {
            final HashMap<String, Boolean> orderIdSet = new HashMap<String, Boolean>(loadedJsonObj.get(user));
            map.put(user, orderIdSet);
        }
        return map;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   geom          DOCUMENT ME!
     * @param   templateFile  DOCUMENT ME!
     * @param   requestName   DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private InputStream generateQeury(final GeometryCollection geom,
            final InputStream templateFile,
            final String requestName) {
        int gmlId = 0;
        try {
            final String xmlGeom = GML3Writer.writeGML3_2WithETRS89(geom);
            // parse the queryTemplate and insert the geom in it
            final DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            final DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            final Document doc = dBuilder.parse(templateFile);
            final NodeList intersectNodes = doc.getElementsByTagName("ogc:Intersects");
            final Document doc2 = dBuilder.parse(new InputSource(new StringReader(xmlGeom)));
            final Element newPolygonNode = doc2.getDocumentElement();
            for (int i = 0; i < intersectNodes.getLength(); i++) {
                Node oldPolygonNode = null;
                Node child = intersectNodes.item(i).getFirstChild();
                while (child != null) {
                    if (child.getNodeName().equals("gml:Polygon")) {
                        oldPolygonNode = child;
                        break;
                    }
                    child = child.getNextSibling();
                }
                if (oldPolygonNode == null) {
                    log.error("corrupt query template file, could not find a geometry node");
                }
                newPolygonNode.setAttribute("gml:id", "G" + gmlId);
                gmlId++;
                // set id for surface nodes
                final NodeList surfaceNodes = newPolygonNode.getElementsByTagName("gml:Surface");
                for (int j = 0; j < surfaceNodes.getLength(); j++) {
                    final Element surfaceNode = (Element)surfaceNodes.item(j);
                    surfaceNode.setAttribute("gml:id", "G" + gmlId);
                    gmlId++;
                }
                final Node importedNode = doc.importNode(newPolygonNode, true);
                intersectNodes.item(i).removeChild(oldPolygonNode);
                intersectNodes.item(i).appendChild(importedNode);
            }
            final OutputFormat format = new OutputFormat(doc);
            // as a String
            final StringWriter stringOut = new StringWriter();
            final XMLSerializer serial = new XMLSerializer(stringOut,
                    format);
            serial.serialize(doc);

            // set the request id that is shown in the 3A Auftagsmanagement Interface
            String request = stringOut.toString();
            request = request.replaceAll(REQUEST_PLACE_HOLDER, requestName);

            // check if this request needs to be portioned

            if (isOrderSplitted(geom)) {
                request = request.replaceAll(DATA_FORMAT_STD, DATA_FORMAT_500);
            }
            if (log.isDebugEnabled()) {
                log.debug(request);
            }
            return new ByteArrayInputStream(request.getBytes());
        } catch (ParserConfigurationException ex) {
            log.error("Parser Configuration Error", ex);
        } catch (SAXException ex) {
            log.error("Error during parsing document", ex);
        } catch (IOException ex) {
            log.error("Error while openeing nas template file", ex);
        }
        return null;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   template   DOCUMENT ME!
     * @param   geoms      DOCUMENT ME!
     * @param   user       DOCUMENT ME!
     * @param   requestId  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public String executeAsynchQuery(final NasProductTemplate template,
            final GeometryCollection geoms,
            final User user,
            final String requestId) {
//        try {
        InputStream templateFile = null;

        try {
            if (template == NasProductTemplate.KOMPLETT) {
                templateFile = NASProductGenerator.class.getResourceAsStream(
                        "A_komplett.xml");
            } else if (template == NasProductTemplate.OHNE_EIGENTUEMER) {
                templateFile = NASProductGenerator.class.getResourceAsStream(
                        "A_o_eigentuemer.xml");
            } else {
                templateFile = NASProductGenerator.class.getResourceAsStream(
                        "A_points.xml");
            }
        } catch (Exception ex) {
            log.fatal("ka", ex);
        }
        if (geoms == null) {
            log.error("geometry is null, cannot execute nas query");
            return null;
        }

        final String requestName = getRequestName(user, requestId);
        final InputStream preparedQuery = generateQeury(geoms, templateFile, requestName);
        initAmManager();
        final int sessionID = manager.login(USER, PW);
        final String orderId = manager.registerGZip(sessionID, gZipFile(preparedQuery));

        final boolean isSplitted = isOrderSplitted(geoms);
        addToOpenOrders(determineUserPrefix(user), orderId, isSplitted);
        addToUndeliveredOrders(determineUserPrefix(user), orderId, isSplitted);

        final NasProductDownloader downloader = new NasProductDownloader(determineUserPrefix(user), orderId);
        downloaderMap.put(orderId, downloader);
        final Thread workerThread = new Thread(downloader);
        workerThread.start();

        return orderId;
//        } catch (RemoteException ex) {
//            log.error("could not create conenction to 3A Server", ex);
//        }
//        return null;
    }

    /**
     * DOCUMENT ME!
     */
    private void initAmManager() {
//        try {
//            final AuftragsManagerLocator am = new AuftragsManagerLocator();
//            manager = am.getAuftragsManagerSoap(new URL(SERVICE_URL));
        final AuftragsManager am;
        try {
            am = new AuftragsManager(new URL(SERVICE_URL));
        } catch (Exception ex) {
            log.error("error creating 3AServer interface", ex);
            return;
        }
        manager = am.getAuftragsManagerSoap();
//        } catch (Exception ex) {
//            log.error("error creating 3AServer interface", ex);
//            Exceptions.printStackTrace(ex);
//        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param   orderId  DOCUMENT ME!
     * @param   user     DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public byte[] getResultForOrder(final String orderId, final User user) {
        final HashMap<String, Boolean> openUserOrders = openOrderMap.get(determineUserPrefix(user));
        if ((openUserOrders != null) && openUserOrders.keySet().contains(orderId)) {
//            if (log.isDebugEnabled()) {
//                log.debug("requesting an order that isnt not done");
//            }
            return new byte[0];
        }
        final HashMap<String, Boolean> undeliveredUserOrders = undeliveredOrderMap.get(determineUserPrefix(user));
        if ((undeliveredUserOrders == null) || undeliveredUserOrders.isEmpty()) {
            log.error("there are no undelivered nas orders for the user " + user.toString());
            return null;
        }
        if (!undeliveredUserOrders.keySet().contains(orderId)) {
            log.error("there is no order for user " + user.toString() + " with order id " + orderId);
            return null;
        }
        removeFromUndeliveredOrders(determineUserPrefix(user), orderId);
        return loadFile(determineUserPrefix(user), orderId);
    }

    /**
     * DOCUMENT ME!
     *
     * @param   user  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public Map<String, Boolean> getUndeliveredOrders(final User user) {
        final HashMap<String, Boolean> undeliveredOrders = new HashMap<String, Boolean>();
        for (final String undeliveredOrderId : undeliveredOrderMap.get(determineUserPrefix(user)).keySet()) {
            undeliveredOrders.put(undeliveredOrderId, Boolean.TRUE);
        }
        return undeliveredOrders;
    }

    /**
     * DOCUMENT ME!
     *
     * @param  orderId  DOCUMENT ME!
     * @param  user     DOCUMENT ME!
     */
    public void cancelOrder(final String orderId, final User user) {
        final String userKey = determineUserPrefix(user);
        final NasProductDownloader downloader = downloaderMap.get(orderId);
        if (downloader != null) {
            downloader.setInterrupted(true);
            downloaderMap.remove(orderId);
        }
        removeFromOpenOrders(userKey, orderId);
        removeFromUndeliveredOrders(userKey, orderId);
        deleteFileIfExists(orderId, user);
    }

    /**
     * DOCUMENT ME!
     *
     * @param   is  queryFile DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private byte[] gZipFile(final InputStream is) {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        OutputStream zipOut = null;
        try {
            zipOut = new GZIPOutputStream(bos);
            final byte[] buffer = new byte[8192];
            int length = is.read(buffer, 0, 8192);
            while (length != -1) {
                zipOut.write(buffer, 0, length);
                length = is.read(buffer, 0, 8192);
            }
            is.close();
            zipOut.close();
            return bos.toByteArray();
        } catch (FileNotFoundException ex) {
            log.error("error during gzip of gile", ex);
        } catch (IOException ex) {
            log.error("error during gzip of gile", ex);
        } finally {
        }
        return null;
    }

    /**
     * DOCUMENT ME!
     *
     * @param  protocol  DOCUMENT ME!
     */
    private void logProtocol(final byte[] protocol) {
        final byte[] unzippedProtocol = gunzip(protocol);
        if (log.isDebugEnabled()) {
            log.debug("Nas Protokoll " + new String(unzippedProtocol));
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param   data  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private byte[] gunzip(final byte[] data) {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        InputStream is = null;
        try {
            is = new GZIPInputStream(new ByteArrayInputStream(data));
            final byte[] buffer = new byte[8192];
            int length = is.read(buffer, 0, 8192);
            while (length != -1) {
                bos.write(buffer, 0, length);
                length = is.read(buffer, 0, 8192);
            }
            return bos.toByteArray();
        } catch (IOException ex) {
            log.error("error during gunzip of nas response files", ex);
        } finally {
            try {
                bos.close();
                if (is != null) {
                    is.close();
                }
            } catch (IOException ex) {
            }
        }
        return null;
    }

    /**
     * DOCUMENT ME!
     *
     * @param  userKey      DOCUMENT ME!
     * @param  orderId      DOCUMENT ME!
     * @param  zippedFiles  DOCUMENT ME!
     */
    private void saveZipFileOfUnzippedFileCollection(final String userKey,
            final String orderId,
            final ArrayList<byte[]> zippedFiles) {
        final ArrayList<byte[]> unzippedFileCollection = new ArrayList<byte[]>();
        for (final byte[] zipFile : zippedFiles) {
            unzippedFileCollection.add(gunzip(zipFile));
        }
        final String filename = determineFileName(userKey, orderId);
        final File file = new File(filename.replace(FILE_APPENDIX, ".zip"));
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        FileOutputStream fos = null;
        ZipOutputStream zos = null;
        try {
            fos = new FileOutputStream(file);
            zos = new ZipOutputStream(fos);
            for (int i = 0; i < unzippedFileCollection.size(); i++) {
                final byte[] unzippedFile = unzippedFileCollection.get(i);
                final String fileEntryName = orderId + "#" + i + FILE_APPENDIX;
                zos.putNextEntry(new ZipEntry(fileEntryName));
                zos.write(unzippedFile);
                zos.closeEntry();
            }
        } catch (IOException ex) {
            log.warn("error during creation of zip file");
        } finally {
            try {
                if (zos != null) {
                    zos.close();
                }
                if (fos != null) {
                    fos.close();
                }
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            }
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param  userKey  DOCUMENT ME!
     * @param  orderId  DOCUMENT ME!
     * @param  data     DOCUMENT ME!
     */
    private void unzipAndSaveFile(final String userKey, final String orderId, final byte[] data) {
        if (data == null) {
            log.error("result of nas order " + orderId + " is null");
            return;
        }
        final File file = new File(determineFileName(userKey, orderId));
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        InputStream is = null;
        OutputStream os = null;
        try {
            is = new GZIPInputStream(new ByteArrayInputStream(data));
//            is = new ByteArrayInputStream(data);
            os = new FileOutputStream(file);
            final byte[] buffer = new byte[8192];
            int length = is.read(buffer, 0, 8192);
            while (length != -1) {
                os.write(buffer, 0, length);
                length = is.read(buffer, 0, 8192);
            }
        } catch (IOException ex) {
            log.error("error during gunzip of nas response files", ex);
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
                if (os != null) {
                    os.close();
                }
            } catch (IOException ex) {
            }
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param   userKey  DOCUMENT ME!
     * @param   orderId  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private byte[] loadFile(final String userKey, final String orderId) {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        InputStream is = null;
        try {
            is = new FileInputStream(determineFileName(userKey, orderId));
            final byte[] buffer = new byte[8192];
            int length = is.read(buffer, 0, 8192);
            while (length != -1) {
                bos.write(buffer, 0, length);
                length = is.read(buffer, 0, 8192);
            }
            return bos.toByteArray();
        } catch (FileNotFoundException ex) {
            try {
                log.error("could not find result file for order id " + orderId);
                final String filename = determineFileName(userKey, orderId);
                is = new FileInputStream(filename.replace(FILE_APPENDIX, ".zip"));
                final byte[] buffer = new byte[8192];
                int length = is.read(buffer, 0, 8192);
                while (length != -1) {
                    bos.write(buffer, 0, length);
                    length = is.read(buffer, 0, 8192);
                }
                return bos.toByteArray();
            } catch (FileNotFoundException ex1) {
                log.error("could not find result file for order id " + orderId);
            } catch (IOException ex1) {
                log.error("could not find result file for order id " + orderId);
            }
        } catch (IOException ex) {
            log.error("error during loading result file for order id " + orderId);
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
     * @param   userKey  DOCUMENT ME!
     * @param   orderId  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private String determineFileName(final String userKey, final String orderId) {
        final StringBuilder fileNameBuilder = new StringBuilder(OUTPUT_DIR);
        fileNameBuilder.append(System.getProperty("file.separator"));
        fileNameBuilder.append(userKey);
        fileNameBuilder.append(System.getProperty("file.separator"));
        fileNameBuilder.append(orderId);
        fileNameBuilder.append(FILE_APPENDIX);
        return fileNameBuilder.toString();
    }

    /**
     * DOCUMENT ME!
     *
     * @param   user  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private String determineUserPrefix(final User user) {
        return user.getId() + "_" + user.getName();
    }

    /**
     * DOCUMENT ME!
     *
     * @param  userKey     userId DOCUMENT ME!
     * @param  orderId     DOCUMENT ME!
     * @param  isSplitted  DOCUMENT ME!
     */
    private void addToOpenOrders(final String userKey, final String orderId, final boolean isSplitted) {
        HashMap<String, Boolean> openUserOders = openOrderMap.get(userKey);
        if (openUserOders == null) {
            openUserOders = new HashMap<String, Boolean>();
            openOrderMap.put(userKey, openUserOders);
        }
        openUserOders.put(orderId, isSplitted);
        updateJsonLogFiles();
    }

    /**
     * DOCUMENT ME!
     *
     * @param  userKey  userId DOCUMENT ME!
     * @param  orderId  DOCUMENT ME!
     */
    private void removeFromOpenOrders(final String userKey, final String orderId) {
        final HashMap<String, Boolean> openUserOrders = openOrderMap.get(userKey);
        if (openUserOrders == null) {
            log.info("there are no undelivered nas orders for the user with id " + userKey);
            return;
        }
        openUserOrders.remove(orderId);
        if (openUserOrders.isEmpty()) {
            openOrderMap.remove(userKey);
        }
        updateJsonLogFiles();
    }

    /**
     * DOCUMENT ME!
     *
     * @param  userKey     userId DOCUMENT ME!
     * @param  orderId     DOCUMENT ME!
     * @param  isSplitted  DOCUMENT ME!
     */
    private void addToUndeliveredOrders(final String userKey, final String orderId, final boolean isSplitted) {
        HashMap<String, Boolean> undeliveredUserOders = undeliveredOrderMap.get(userKey);
        if (undeliveredUserOders == null) {
            undeliveredUserOders = new HashMap<String, Boolean>();
            undeliveredOrderMap.put(userKey, undeliveredUserOders);
        }
        undeliveredUserOders.put(orderId, isSplitted);
        updateJsonLogFiles();
    }

    /**
     * DOCUMENT ME!
     *
     * @param  userKey  userId DOCUMENT ME!
     * @param  orderId  DOCUMENT ME!
     */
    private void removeFromUndeliveredOrders(final String userKey, final String orderId) {
        final HashMap<String, Boolean> undeliveredUserOders = undeliveredOrderMap.get(userKey);
        if (undeliveredUserOders == null) {
            log.info("there are no undelivered nas orders for the user with id " + userKey);
            return;
        }
        undeliveredUserOders.remove(orderId);
        if (undeliveredUserOders.isEmpty()) {
            undeliveredOrderMap.remove(userKey);
        }
        updateJsonLogFiles();
    }

    /**
     * DOCUMENT ME!
     */
    private synchronized void updateJsonLogFiles() {
        final ObjectMapper mapper = new ObjectMapper();
        final ObjectWriter writer = mapper.writerWithDefaultPrettyPrinter();

        try {
            writer.writeValue(undeliveredOrdersLogFile, undeliveredOrderMap);
            writer.writeValue(openOrdersLogFile, openOrderMap);
        } catch (IOException ex) {
            log.error("error during writing open and undelivered order maps to file", ex);
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param  orderId  DOCUMENT ME!
     * @param  user     DOCUMENT ME!
     */
    private void deleteFileIfExists(final String orderId, final User user) {
        final String userKey = determineUserPrefix(user);
        final File file = new File(determineFileName(userKey, orderId));
        if (file.exists()) {
            if (!file.delete()) {
                log.warn("could not delete file " + file.toString());
            }
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
    private String getRequestName(final User user, final String requestId) {
        return user.getName() + "_" + requestId;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   geoms  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private boolean isOrderSplitted(final GeometryCollection geoms) {
        final Envelope env = geoms.getEnvelopeInternal();
        final double xSize = env.getMaxX() - env.getMinX();
        final double ySize = env.getMaxY() - env.getMinY();

        if ((xSize > 500) && (ySize > 500)) {
            return true;
        }
        return false;
    }

    //~ Inner Classes ----------------------------------------------------------

    /**
     * NasProductDownloader checks at a fixed rate if the nas order is completed in the 3A order management system.
     *
     * @version  $Revision$, $Date$
     */
    private class NasProductDownloader implements Runnable {

        //~ Instance fields ----------------------------------------------------

        private String orderId;
        private String userId;
        private boolean interrupted = false;

        //~ Constructors -------------------------------------------------------

        /**
         * Creates a new NasProductDownloader object.
         *
         * @param  userId   DOCUMENT ME!
         * @param  orderId  DOCUMENT ME!
         */
        public NasProductDownloader(final String userId, final String orderId) {
            this.orderId = orderId;
            this.userId = userId;
        }

        //~ Methods ------------------------------------------------------------

        @Override
        public void run() {
//            try {
            initAmManager();
            final int sessionId = manager.login(USER, PW);
            final Timer t = new Timer();
            t.scheduleAtFixedRate(new TimerTask() {

                    @Override
                    public void run() {
                        AMAuftragServer amServer = null;
//                            AM_AuftragServer amServer = null;
//                            try {
                        if (interrupted) {
                            log.info(
                                "interrupting the dowload of nas order "
                                        + orderId);
                            t.cancel();
                            return;
                        }
                        amServer = manager.listAuftrag(sessionId, orderId);
                        if (amServer.getWannBeendet() == null) {
                            return;
                        }
                        t.cancel();
                        logProtocol(manager.getProtocolGZip(sessionId, orderId));
                        if (!interrupted) {
                            final int resCount = manager.getResultCount(sessionId, orderId);
                            if (resCount > 1) {
                                // unzip and save all files, then zip them
                                final ArrayList<byte[]> resultFiles = new ArrayList<byte[]>();
                                for (int i = 0; i < resCount; i++) {
                                    resultFiles.add(manager.getNResultGZip(sessionId, orderId, i));
                                }
                                saveZipFileOfUnzippedFileCollection(userId, orderId, resultFiles);
                            } else {
                                unzipAndSaveFile(userId, orderId, manager.getResultGZip(sessionId, orderId));
                            }
                            for (int i = 0; i < resCount; i++) {
                            }
//                                    unzipAndSaveFile(userId, orderId, manager.getResultGZip(sessionId, orderId));
                            removeFromOpenOrders(userId, orderId);
                            downloaderMap.remove(orderId);
                        } else {
                            log.info(
                                "interrupting the download of nas order "
                                        + orderId);
                        }
//                            } catch (RemoteException ex) {
//                                Exceptions.printStackTrace(ex);
//                            }
                    }
                }, REQUEST_PERIOD, REQUEST_PERIOD);
//            } catch (RemoteException ex) {
//                log.error("Could not connect to 3A server", ex);
//            }
        }

        /**
         * DOCUMENT ME!
         *
         * @param  interrupted  DOCUMENT ME!
         */
        public void setInterrupted(final boolean interrupted) {
            this.interrupted = interrupted;
        }
    }
}
