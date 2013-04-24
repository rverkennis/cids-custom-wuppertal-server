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
package de.cismet.cids.custom.wunda_blau.search.actions;

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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;

import java.net.URL;

import java.util.Properties;
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

import de.cismet.cids.custom.utils.nas.GML3Writer;
import de.cismet.cids.custom.utils.nas.NASProductGenerator;

import de.cismet.cids.server.actions.ServerAction;
import de.cismet.cids.server.actions.ServerActionParameter;

/**
 * DOCUMENT ME!
 *
 * @author   daniel
 * @version  $Revision$, $Date$
 */
@org.openide.util.lookup.ServiceProvider(service = ServerAction.class)
public class NasDataQueryAction implements ServerAction {

    //~ Static fields/initializers ---------------------------------------------

    private static final transient org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(
            NasDataQueryAction.class);

    //~ Enums ------------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    public enum PRODUCT_TEMPLATE {

        //~ Enum constants -----------------------------------------------------

        KOMPLETT, OHNE_EIGENTUEMER, POINTS
    }

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    public enum PARAMETER_TYPE {

        //~ Enum constants -----------------------------------------------------

        TEMPLATE, GEOMETRY, ADD, GET, GET_ALL,
    }

    //~ Instance fields --------------------------------------------------------

    private AuftragsManagerSoap manager;

    //~ Methods ----------------------------------------------------------------

    @Override
    public Object execute(final Object body, final ServerActionParameter... params) {
        PRODUCT_TEMPLATE template = null;
        Geometry geom = null;
        for (final ServerActionParameter sap : params) {
            if (sap.getKey().equals(PARAMETER_TYPE.TEMPLATE.toString())) {
                template = (PRODUCT_TEMPLATE)sap.getValue();
            } else if (sap.getKey().equals(PARAMETER_TYPE.GEOMETRY.toString())) {
                geom = (Geometry)sap.getValue();
            }
        }
        final NASProductGenerator nasPg = new NASProductGenerator();

        return nasPg.executeAsynchQuery(template, geom);
    }

    @Override
    public String getTaskName() {
        return "nasDataQuery";
    }
}
