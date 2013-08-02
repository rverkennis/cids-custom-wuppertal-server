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

import Sirius.server.middleware.interfaces.domainserver.MetaService;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;

import org.apache.log4j.Logger;

import org.openide.util.Exceptions;

import java.io.IOException;

import java.rmi.RemoteException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Properties;

import de.cismet.cids.custom.wunda_blau.search.server.CidsMauernSearchStatement;

import de.cismet.cids.server.search.AbstractCidsServerSearch;
import de.cismet.cids.server.search.SearchException;

import de.cismet.cismap.commons.jtsgeometryfactories.PostGisGeometryFactory;

/**
 * DOCUMENT ME!
 *
 * @author   daniel
 * @version  $Revision$, $Date$
 */
public class NasZaehlObjekteSearch extends AbstractCidsServerSearch {

    //~ Static fields/initializers ---------------------------------------------

    private static final Logger LOG = Logger.getLogger(CidsMauernSearchStatement.class);
    private static final String FLURSTUECK_STMT =
        "select count(*) as Anzahl from ax_flurstueck where st_intersects(wkb_geometry,<geom>)";
    private static final String GEAEUDE_STMT =
        "select count(*) as Anzahl from ax_gebaeude where st_intersects(wkb_geometry,<geom>)";
    private static final String DACH_PKT_STMT =
        "SELECT count (*) FROM sic_regen_dachpg where st_intersects(wkb_geometry,<geom>)";
    private static final String BODEN_PKT_STMT =
        "SELECT count (*) FROM sic_regen_bodenpg where st_intersects(wkb_geometry,<geom>)";
    private static final String ADRESE_STMT = "SELECT DISTINCT i.class_id , i.object_id, s.stringrep"
                + " FROM geom g, cs_attr_object_derived i LEFT OUTER JOIN cs_stringrepcache s ON ( s.class_id =i.class_id AND s.object_id=i.object_id )"
                + " WHERE i.attr_class_id = ( SELECT cs_class.id FROM cs_class WHERE cs_class.table_name::text = 'GEOM'::text )"
                + " AND i.attr_object_id = g.id"
                + " AND i.class_id IN (6)"
                + " AND geo_field && GeometryFromText('<geom>')"
                + " AND intersects(st_buffer(geo_field, 0.000001),st_buffer(GeometryFromText('<geom>'), 0.000001)) ORDER BY 1,2,3";
    private static Connection fmeConn = null;

    //~ Enums ------------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    public enum NasSearchType {

        //~ Enum constants -----------------------------------------------------

        FLURSTUECKE, GEBAEUDE, ADRESSE, DACHPUNKTE, BODENPUNKTE
    }

    //~ Instance fields --------------------------------------------------------

    final Geometry geometry;
    final NasZaehlObjekteSearch.NasSearchType searchType;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new NasZaehlObjekteSearch object.
     *
     * @param  g     DOCUMENT ME!
     * @param  type  useCids DOCUMENT ME!
     */
    public NasZaehlObjekteSearch(final Geometry g, final NasZaehlObjekteSearch.NasSearchType type) {
        geometry = g;
        this.searchType = type;
    }

    //~ Methods ----------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  SearchException  DOCUMENT ME!
     */
    private int getFlurstueckObjectsCount() throws SearchException {
        return getObjectsCount(FLURSTUECK_STMT);
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  SearchException  DOCUMENT ME!
     */
    private int getGebaeudeObjectsCount() throws SearchException {
        return getObjectsCount(GEAEUDE_STMT);
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  SearchException  DOCUMENT ME!
     */
    private int getBodenpunkteObjectsCount() throws SearchException {
        return getObjectsCount(BODEN_PKT_STMT);
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  SearchException  DOCUMENT ME!
     */
    private int getDachpunkteObjectsCount() throws SearchException {
        return getObjectsCount(DACH_PKT_STMT);
    }

    /**
     * DOCUMENT ME!
     *
     * @param   statement  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  SearchException  DOCUMENT ME!
     */
    private int getObjectsCount(final String statement) throws SearchException {
        try {
            initConnection();
            final Statement st = fmeConn.createStatement();
            final StringBuilder sb = new StringBuilder();
            final String geostring = PostGisGeometryFactory.getPostGisCompliantDbString(geometry);
            if ((geometry instanceof Polygon) || (geometry instanceof MultiPolygon)) { // with buffer for geostring
                sb.append(statement.replace(
                        "<geom>",
                        "st_buffer(GeometryFromText('"
                                + geostring
                                + "'), 0.000001)"));
            }
            st.execute(sb.toString());
            final ResultSet rs = st.getResultSet();
            rs.next();
            return rs.getInt(1);
        } catch (SQLException ex) {
            LOG.error("Error during NasZaehlobjekteSearch", ex);
            throw new SearchException("Error during NasZaehlobjekteSearch");
        } finally {
            try {
                fmeConn.close();
            } catch (SQLException ex) {
                Exceptions.printStackTrace(ex);
            }
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     *
     * @throws  SearchException  DOCUMENT ME!
     */
    private int getAddressenCount() throws SearchException {
        final MetaService ms = (MetaService)getActiveLocalServers().get("WUNDA_BLAU");

        if (ms != null) {
            try {
                final StringBuilder sb = new StringBuilder();
                final String geostring = PostGisGeometryFactory.getPostGisCompliantDbString(geometry);
                if ((geometry instanceof Polygon) || (geometry instanceof MultiPolygon)) { // with buffer for geostring
                    sb.append(ADRESE_STMT.replace(
                            "<geom>",
                            geostring));
                }
                if (LOG.isDebugEnabled()) {
                    LOG.debug("query: " + sb.toString());                                  // NOI18N
                }
                final ArrayList<ArrayList> lists = ms.performCustomSearch(sb.toString());

                return lists.size();
            } catch (RemoteException ex) {
                LOG.error(ex.getMessage(), ex);
            }
        } else {
            LOG.error("active local server not found"); // NOI18N
        }
        return 0;
    }

    /**
     * DOCUMENT ME!
     *
     * @throws  SearchException  DOCUMENT ME!
     */
    private void initConnection() throws SearchException {
        try {
            final Properties serviceProperties = new Properties();
            serviceProperties.load(NasZaehlObjekteSearch.class.getResourceAsStream("fme_db_conn.properties"));
            final String url = serviceProperties.getProperty("connection_url");
            final String user = serviceProperties.getProperty("connection_username");
            final String pw = serviceProperties.getProperty("connection_pw");
            fmeConn = DriverManager.getConnection(url,
                    user, pw);
        } catch (IOException ex) {
            LOG.error("error during reading properties for fme_db connection", ex);
            throw new SearchException("Error during NasZaehlObjekte search");
        } catch (SQLException ex) {
            LOG.error("Could not create db connection to fme_import database", ex);
            throw new SearchException("Error during NasZaehlObjekte search");
        }
    }

    @Override
    public Collection performServerSearch() throws SearchException {
        final ArrayList<Integer> resultList = new ArrayList<Integer>();
        if (searchType == NasZaehlObjekteSearch.NasSearchType.FLURSTUECKE) {
            resultList.add(getFlurstueckObjectsCount());
        } else if (searchType == NasZaehlObjekteSearch.NasSearchType.GEBAEUDE) {
            resultList.add(getGebaeudeObjectsCount());
        } else if (searchType == NasZaehlObjekteSearch.NasSearchType.ADRESSE) {
            resultList.add(getAddressenCount());
        } else if (searchType == NasZaehlObjekteSearch.NasSearchType.BODENPUNKTE) {
            resultList.add(getBodenpunkteObjectsCount());
        } else if (searchType == NasZaehlObjekteSearch.NasSearchType.DACHPUNKTE) {
            resultList.add(getDachpunkteObjectsCount());
        }
        return resultList;
    }

    /**
     * DOCUMENT ME!
     *
     * @param  args  DOCUMENT ME!
     */
    public static void main(final String[] args) {
        final NasZaehlObjekteSearch search = new NasZaehlObjekteSearch(
                null,
                NasZaehlObjekteSearch.NasSearchType.GEBAEUDE);
        try {
            search.initConnection();
        } catch (SearchException ex) {
            Exceptions.printStackTrace(ex);
        }
    }
}
