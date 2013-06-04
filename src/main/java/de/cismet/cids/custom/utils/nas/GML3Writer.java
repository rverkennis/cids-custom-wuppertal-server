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
package de.cismet.cids.custom.utils.nas;

import com.sun.org.apache.xml.internal.serialize.OutputFormat;
import com.sun.org.apache.xml.internal.serialize.XMLSerializer;

import com.vividsolutions.jts.geom.GeometryCollection;

import org.openide.util.Exceptions;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringWriter;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

/**
 * DOCUMENT ME!
 *
 * @author   daniel
 * @version  $Revision$, $Date$
 */
public class GML3Writer {

    //~ Methods ----------------------------------------------------------------

// static final String SCHEMA_LOCATION_ATTRIBUTE =
// "http://www.opengis.net/gml http://schemas.opengis.net/gml/3.2.1/base/gml.xsd";
    /**
     * DOCUMENT ME!
     *
     * @param   geometries   DOCUMENT ME!
     * @param   crs          DOCUMENT ME!
     * @param   srsNameProp  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
// private static String convertToGMLWithDeegree(final com.vividsolutions.jts.geom.Geometry geometry,
// final String crs,
// final String srsNameProp) {
// // transform to fix CRS ETRS89-UTM32
// CrsTransformer.transformToGivenCrs(geometry, crs);
// final StringBuilder res = new StringBuilder();
// try {
// final StringWriter sw = new StringWriter();
// final XMLOutputFactory xmlOutputFactory = XMLOutputFactory.newFactory();
// final XMLStreamWriter xmlStreamWriter = xmlOutputFactory.createXMLStreamWriter(sw);
// final GMLStreamWriter w = GMLOutputFactory.createGMLStreamWriter(GMLVersion.GML_32, xmlStreamWriter);
// final GML3GeometryWriter gmlWriter = new GML3GeometryWriter(w);
// final WKTWriter wktWriter = new WKTWriter();
// final String wktGeom = wktWriter.write(geometry);
// // use a static ICRS of deegree, since it does not affect
// final WKTReader reader = new WKTReader(GeographicCRS.WGS84_YX);
// gmlWriter.export(reader.read(wktGeom));
// xmlStreamWriter.flush();
// xmlStreamWriter.close();
// String foo = sw.toString();
// foo = foo.replaceAll("xmlns:gml=\"http://www.opengis.net/gml/3.2\"", "");
// foo = foo.replaceAll("srsName=\"EPSG:4326\"", "srsName=\"" + srsNameProp + "\"");
// res.append(foo);
// } catch (Exception ex) {
// Exceptions.printStackTrace(ex);
// }
// return res.toString();
// }
    /**
     * DOCUMENT ME!
     *
     * @param   geometries   DOCUMENT ME!
     * @param   crs          DOCUMENT ME!
     * @param   srsNameProp  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public static String convertToGML(final GeometryCollection geometries, final String crs, final String srsNameProp) {
        String rawXML = "";
        try {
//            CrsTransformer.transformToGivenCrs(geometry, crs);
            final JAXBContext ffo = JAXBContext.newInstance("org.jvnet.ogc.gml.v_3_1_1.jts");
            final StringWriter sw = new StringWriter();
            ffo.createMarshaller().marshal(geometries, sw);
            rawXML = sw.getBuffer().toString();

            // replace ns1 prefix through gml
            rawXML = rawXML.replaceAll("ns1:", "gml:");

            /*
             * the 3AServer just likes Polygons with one gml:posList element instead several gml:Pos elements so take
             * the coordinates from the gml:Pos elems and create a gml:posList element
             */
            final ByteArrayInputStream is = new ByteArrayInputStream(rawXML.getBytes());
            final DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            final DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            final Document doc = dBuilder.parse(is);

            final NodeList linearRingNodes = doc.getElementsByTagName("gml:LinearRing");
            for (int i = 0; i < linearRingNodes.getLength(); i++) {
                final StringBuilder poslistCoords = new StringBuilder();
                Node child = linearRingNodes.item(i).getFirstChild();
                while (child != null) {
                    if (child.getNodeName().equals("gml:pos")) {
                        poslistCoords.append(child.getTextContent());
                        poslistCoords.append(" ");
                    }
                    final Node nextSibling = child.getNextSibling();
                    linearRingNodes.item(i).removeChild(child);
                    child = nextSibling;
                }
                final Node newNode = (Node)doc.createElement("gml:posList");
                newNode.setTextContent(poslistCoords.toString());
                linearRingNodes.item(i).appendChild(newNode);
            }

            /*
             * the 3A Server just likes MultiSurfaces so we have to convert the geom into an MultiSurface
             */

            final NodeList geometryMemberNodes = doc.getElementsByTagName("gml:geometryMember");
            for (int i = 0; i < geometryMemberNodes.getLength(); i++) {
                final Node geometryMember = geometryMemberNodes.item(i);
                final Node oldPolygonNode = geometryMember.getFirstChild();
                geometryMember.removeChild(oldPolygonNode);
                final Element surface = doc.createElement("gml:Surface");
                surface.setAttribute("srsName", srsNameProp);
                final Element patches = doc.createElement("gml:patches");
                surface.appendChild(patches);
                patches.appendChild(oldPolygonNode);
                geometryMember.appendChild(surface);
            }

            final OutputFormat format = new OutputFormat(doc);
            // as a String
            final StringWriter stringOut = new StringWriter();
            final XMLSerializer serial = new XMLSerializer(stringOut,
                    format);
            serial.serialize(doc);
//            rawXML = os.toString();
            rawXML = stringOut.toString();
        } catch (JAXBException ex) {
            Exceptions.printStackTrace(ex);
        } catch (ParserConfigurationException ex) {
            Exceptions.printStackTrace(ex);
        } catch (SAXException ex) {
            Exceptions.printStackTrace(ex);
        } catch (IOException ex) {
            Exceptions.printStackTrace(ex);
        } catch (Exception ex) {
            Exceptions.printStackTrace(ex);
        }
        // replcae MultiGeom with MultiSurface since 3A Server just likes MultiSurfaces
        rawXML = rawXML.replaceAll("MultiGeometry", "MultiSurface");
        rawXML = rawXML.replaceAll("geometryMember", "surfaceMember");
        rawXML = rawXML.replaceAll("Polygon", "PolygonPatch");
        // remove the xml doc elem
        rawXML = rawXML.replace("<?xml version=\"1.0\" encoding=\"UTF-8\"?>", "");
        // remove the namespaces that are inserted by jvnet parser
        rawXML = rawXML.replace("xmlns:ns1=\"http://www.opengis.net/gml\"", "");
        rawXML = rawXML.replace("xmlns:ns2=\"http://www.w3.org/1999/xlink\"", "");
        rawXML = rawXML.replace("xmlns:ns3=\"http://www.w3.org/2001/SMIL20/\"", "");
        rawXML = rawXML.replace("xmlns:ns4=\"http://www.w3.org/2001/SMIL20/Language\"", "");
        rawXML = rawXML.replace("    ", "");
        // replace the srsName element
        rawXML = rawXML.replaceFirst("srsName=\"urn:ogc:def:crs:EPSG::25832\"", "srsName=\"" + srsNameProp + "\"");
        rawXML = rawXML.replaceAll(" srsName=\"urn:ogc:def:crs:EPSG::25832\"", "");
        return rawXML;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   geometries  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public static String writeGML3_2WithETRS89(final GeometryCollection geometries) {
//        return convertToGML(geometry, "EPSG:25832", "urn:adv:crs:ETRS89_UTM32");
        final String confluence = convertToGML(geometries, "EPSG:25832", "urn:adv:crs:ETRS89_UTM32");
//        final String deegree = convertToGMLWithDeegree(geometry, "EPSG:25832", "urn:adv:crs:ETRS89_UTM32");
        return confluence;
    }
}
