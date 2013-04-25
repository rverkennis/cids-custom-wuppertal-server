/***************************************************
*
* cismet GmbH, Saarbruecken, Germany
*
*              ... and it just works.
*
****************************************************/
package de.cismet.cids.custom.utils.nas;

import com.vividsolutions.jts.geom.Geometry;

import de.aed_sicad.namespaces.svr.AMAuftragServer;
import de.aed_sicad.namespaces.svr.AuftragsManager;
import de.aed_sicad.namespaces.svr.AuftragsManagerSoap;

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

import java.net.MalformedURLException;
import java.net.URL;

import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import de.cismet.cids.custom.wunda_blau.search.actions.NasDataQueryAction;

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

    //~ Instance fields --------------------------------------------------------

    private final transient org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(this.getClass());
    private AuftragsManagerSoap manager;
    private final String SERVICE_URL;
    private final String USER;
    private final String PW;
    private final String OUTPUT_DIR;
    private HashSet<String> openOrders = new HashSet<String>();
    private HashSet<String> undeliveredOrders = new HashSet<String>();

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new NASProductGenerator object.
     *
     * @throws  RuntimeException  DOCUMENT ME!
     */
    public NASProductGenerator() {
        final Properties serviceProperties = new Properties();
        try {
            serviceProperties.load(NASProductGenerator.class.getResourceAsStream("nasServer_conf.properties"));
            SERVICE_URL = serviceProperties.getProperty("service");
            USER = serviceProperties.getProperty("user");
            PW = serviceProperties.getProperty("password");
            OUTPUT_DIR = serviceProperties.getProperty("outputDir");
        } catch (Exception ex) {
            log.fatal("NAS Datenabgabe initialisation Error!", ex);
            throw new RuntimeException(ex);
        }
    }

    //~ Methods ----------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @param   geom          DOCUMENT ME!
     * @param   templateFile  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private InputStream generateQeury(final Geometry geom, final InputStream templateFile) {
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
                final Node importedNode = doc.importNode(newPolygonNode, true);
                intersectNodes.item(i).removeChild(oldPolygonNode);
                intersectNodes.item(i).appendChild(importedNode);
            }
            // Use a Transformer for output
            final TransformerFactory tFactory = TransformerFactory.newInstance();
            final Transformer transformer = tFactory.newTransformer();

            final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            final DOMSource source = new DOMSource(doc);
            final StreamResult result = new StreamResult(outputStream);
            transformer.transform(source, result);
            if (log.isDebugEnabled()) {
                log.debug(outputStream.toString());
            }
            return new ByteArrayInputStream(outputStream.toByteArray());
        } catch (ParserConfigurationException ex) {
            log.error("Parser Configuration Error", ex);
        } catch (SAXException ex) {
            log.error("Error during parsing document", ex);
        } catch (IOException ex) {
            log.error("Error while openeing nas template file", ex);
        } catch (TransformerConfigurationException ex) {
            log.error("Error writing adopted nas template file", ex);
        } catch (TransformerException ex) {
            log.error("Error writing adopted nas template file", ex);
        }
        return null;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   template  DOCUMENT ME!
     * @param   geom      DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public String executeAsynchQuery(final NasDataQueryAction.PRODUCT_TEMPLATE template, final Geometry geom) {
        InputStream templateFile = null;

        try {
            if (template == NasDataQueryAction.PRODUCT_TEMPLATE.KOMPLETT) {
                templateFile = NASProductGenerator.class.getResourceAsStream(
                        "A_komplett.xml");
            } else if (template == NasDataQueryAction.PRODUCT_TEMPLATE.OHNE_EIGENTUEMER) {
                templateFile = NASProductGenerator.class.getResourceAsStream(
                        "A_o_eigentuemer.xml");
            } else {
                templateFile = NASProductGenerator.class.getResourceAsStream(
                        "A_points.xml");
            }
        } catch (Exception ex) {
            log.fatal("ka", ex);
        }
        if (geom == null) {
            log.error("geometry is null, cannot execute nas query");
            return null;
        }

        final AuftragsManager am;
        try {
            am = new AuftragsManager(new URL(SERVICE_URL));
        } catch (MalformedURLException ex) {
            log.error("error creating 3AServer interface", ex);
            return null;
        }
        manager = am.getAuftragsManagerSoap();
        final InputStream preparedQuery = generateQeury(geom, templateFile);
        final int sessionID = manager.login(USER, PW);
        final String orderId = manager.registerGZip(sessionID, gZipFile(preparedQuery));
        openOrders.add(orderId);
        undeliveredOrders.add(orderId);

        final Runnable r = new Runnable() {

                @Override
                public void run() {
                    AMAuftragServer amServer = manager.listAuftrag(sessionID, orderId);
                    while (amServer.getWannBeendet() == null) {
                        amServer = manager.listAuftrag(sessionID, orderId);
                    }

                    logProtocol(manager.getProtocolGZip(sessionID, orderId));
                    unzipAndSaveFile(orderId, manager.getResultGZip(sessionID, orderId));
                    openOrders.remove(orderId);
                }
            };

        final Thread workerThread = new Thread(r);
        workerThread.start();

        return orderId;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   orderId  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public byte[] getResultForOrder(final String orderId) {
        if (openOrders.contains(orderId)) {
            if (log.isDebugEnabled()) {
                log.debug("requesting an order that isnt not done");
            }
            return null;
        }
        undeliveredOrders.remove(orderId);
        return loadFile(orderId);
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public Set<String> getUndeliveredOrders() {
        return undeliveredOrders;
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
        final byte[] unzippedProtocol = gunzipFile(protocol);
//        ByteArrayOutputStream bos = new ByteArrayOutputStream();
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
    private byte[] gunzipFile(final byte[] data) {
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
        }
        return null;
    }

    /**
     * DOCUMENT ME!
     *
     * @param  orderId  DOCUMENT ME!
     * @param  data     DOCUMENT ME!
     */
    private void unzipAndSaveFile(final String orderId, final byte[] data) {
        final File file = new File(determineFileName(orderId));
//        file.get
        file.getParentFile().mkdirs();
        InputStream is = null;
        OutputStream os = null;
        try {
            is = new GZIPInputStream(new ByteArrayInputStream(data));
            os = new FileOutputStream(file);
            final byte[] buffer = new byte[8192];
            int length = is.read(buffer, 0, 8192);
            while (length != -1) {
                os.write(buffer, 0, length);
                length = is.read(buffer, 0, 8192);
            }
        } catch (IOException ex) {
            log.error("error during gunzip of nas response files", ex);
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param   orderId  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private byte[] loadFile(final String orderId) {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try {
            final InputStream is = new FileInputStream(determineFileName(orderId));
            final byte[] buffer = new byte[8192];
            int length = is.read(buffer, 0, 8192);
            while (length != -1) {
                bos.write(buffer, 0, length);
                length = is.read(buffer, 0, 8192);
            }
            return bos.toByteArray();
        } catch (FileNotFoundException ex) {
            log.error("could not find result file for order id " + orderId);
        } catch (IOException ex) {
            log.error("error during loading result file for order id " + orderId);
        }
        return null;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   orderId  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private String determineFileName(final String orderId) {
        final StringBuilder fileNameBuilder = new StringBuilder(OUTPUT_DIR);
        fileNameBuilder.append(System.getProperty("file.separator"));
        fileNameBuilder.append(orderId);
        fileNameBuilder.append(FILE_APPENDIX);
        return fileNameBuilder.toString();
    }
}
