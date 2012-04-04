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
package de.cismet.cids.custom.utils.alkis;

import de.cismet.tools.PropertyReader;

/**
 * DOCUMENT ME!
 *
 * @author   srichter
 * @version  $Revision$, $Date$
 */
public final class AlkisConstants {

    //~ Static fields/initializers ---------------------------------------------

    public static final AlkisConstants COMMONS = new AlkisConstants();
    public static final String MLESSNUMBER =
        "nmless=5061756C612030352E31322E32303035204A75737475732032352E30372E323030382054616E6A612030362E31302E31393734";

    //
    private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(AlkisConstants.class);
    public static final String NEWLINE = "<br>";
    public static final String LINK_SEPARATOR_TOKEN = "::";

    //~ Instance fields --------------------------------------------------------

    public final String USER;
    public final String PASSWORD;
    public final String SERVICE;
    public final String SERVER;
    public final String CATALOG_SERVICE;
    public final String INFO_SERVICE;
    public final String SEARCH_SERVICE;
    public final String SRS_GEOM;
    public final String SRS_SERVICE;
    public final String MAP_CALL_STRING;
    public final double GEO_BUFFER;
    public final double GEO_BUFFER_MULTIPLIER;
    public final String EINZEL_NACHWEIS_SERVICE;
    public final String LISTEN_NACHWEIS_SERVICE;
    public final String LIEGENSCHAFTSKARTE_SERVICE;
    public final String NIVP_HOST;
    public final String NIVP_PREFIX;
    public final String APMAPS_HOST;
    public final String APMAPS_PREFIX;
    public final String VERMESSUNG_HOST;
    public final String VERMESSUNG_PREFIX_BILD;
    public final String VERMESSUNG_PREFIX_GRENZNIEDERSCHRIFT;

    public final String DEMOSERVICEURL;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new AlkisConstants object.
     *
     * @throws  RuntimeException  DOCUMENT ME!
     */
    private AlkisConstants() {
        try {
            final PropertyReader serviceProperties = new PropertyReader(
                    "/de/cismet/cids/custom/wunda_blau/res/alkis/alkis_conf.properties");

            SERVER = serviceProperties.getProperty("SERVER");
            SERVICE = serviceProperties.getProperty("SERVICE");
            USER = serviceProperties.getProperty("USER");
            PASSWORD = serviceProperties.getProperty("PASSWORD");

            DEMOSERVICEURL = serviceProperties.getProperty("DEMOSERVICEURL");
            //
            CATALOG_SERVICE = serviceProperties.getProperty("CATALOG_SERVICE");
            INFO_SERVICE = serviceProperties.getProperty("INFO_SERVICE");
            SEARCH_SERVICE = serviceProperties.getProperty("SEARCH_SERVICE");
            //
            EINZEL_NACHWEIS_SERVICE = SERVER + serviceProperties.getProperty("BUCH_NACHWEIS_SERVICE");
            LISTEN_NACHWEIS_SERVICE = SERVER + serviceProperties.getProperty("LISTEN_NACHWEIS_SERVICE");
            LIEGENSCHAFTSKARTE_SERVICE = SERVER + serviceProperties.getProperty("LIEGENSCHAFTSKARTE_SERVICE");
            //
            SRS_GEOM = serviceProperties.getProperty("SRS_GEOM");
            SRS_SERVICE = serviceProperties.getProperty("SRS_SERVICE");
            MAP_CALL_STRING = serviceProperties.getProperty("MAP_CALL_STRING") + SRS_SERVICE;
            GEO_BUFFER = Double.parseDouble(serviceProperties.getProperty("GEO_BUFFER"));
            GEO_BUFFER_MULTIPLIER = Double.parseDouble(serviceProperties.getProperty("GEO_BUFFER_MULTIPLIER"));

            NIVP_HOST = serviceProperties.getProperty("NIVP_HOST");
            NIVP_PREFIX = serviceProperties.getProperty("NIVP_PREFIX");

            APMAPS_HOST = serviceProperties.getProperty("APMAPS_HOST");
            APMAPS_PREFIX = serviceProperties.getProperty("APMAPS_PREFIX");

            VERMESSUNG_HOST = serviceProperties.getProperty("VERMESSUNG_HOST");
            VERMESSUNG_PREFIX_BILD = serviceProperties.getProperty("VERMESSUNG_PREFIX_BILD");
            VERMESSUNG_PREFIX_GRENZNIEDERSCHRIFT = serviceProperties.getProperty(
                    "VERMESSUNG_PREFIX_GRENZNIEDERSCHRIFT");

//            SERVER = "http://s102x283:8080";
//            SERVICE = "Wuppertal";
//            USER = "3awup";
//            PASSWORD = "3awup";
//            //
//            CATALOG_SERVICE = "/AAAWebService/services/ALKISCatalogServices";
//            INFO_SERVICE ="/AAAWebService/services/ALKISInfoServices";
//            SEARCH_SERVICE = "/AAAWebService/services/ALKISSearchServices";
//            //
//            EINZEL_NACHWEIS_SERVICE = SERVER + "/ASWeb/ASA_AAAWeb/ALKISBuchNachweis";
//            LISTEN_NACHWEIS_SERVICE = SERVER + "/ASWeb/ASA_AAAWeb/ALKISListenNachweis";
//            LIEGENSCHAFTSKARTE_SERVICE = SERVER + "/ASWeb/ASA_AAAWeb/ALKISLiegenschaftskarte";
//            //
//            SRS_GEOM = "EPSG:25832";
//            SRS_SERVICE = "EPSG:25832";
//            MAP_CALL_STRING = "http://S102x284:8399/arcgis/services/3AWeb_Express_AGS1/MapServer/WMSServer?&VERSION=1.1.1&REQUEST=GetMap&BBOX=<cismap:boundingBox>&WIDTH=<cismap:width>&HEIGHT=<cismap:height>&FORMAT=image/png&TRANSPARENT=TRUE&BGCOLOR=0xF0F0F0&EXCEPTIONS=application/vnd.ogc.se_xml&LAYERS=34,35,36,37,38,39,40,41,42,43,44,45,46,47,48,49,50,51,52&STYLES=default,default,default,default,default,default,default,default,default,default,default,default,default,default,default,default,default,default,default&SRS=" + SRS_GEOM;
//            GEO_BUFFER = 5.0;
//            GEO_BUFFER_MULTIPLIER = 0.5;
//
//            //

        } catch (Exception ex) {
            log.fatal("AlkisCommons Error!", ex);
            throw new RuntimeException(ex);
        }
    }
}
