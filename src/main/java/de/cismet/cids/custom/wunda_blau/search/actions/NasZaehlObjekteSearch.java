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
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;

import org.apache.log4j.Logger;

import org.openide.util.Exceptions;

import java.io.IOException;


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
    private static Connection fmeConn = null;

    //~ Enums ------------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    public enum NasSearchType {

        //~ Enum constants -----------------------------------------------------

        FLURSTUECKE, GEBAEUDE
    }

    //~ Instance fields --------------------------------------------------------

    final Geometry geometry;
    final NasSearchType searchType;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new NasZaehlObjekteSearch object.
     *
     * @param  g     DOCUMENT ME!
     * @param  type  useCids DOCUMENT ME!
     */
    public NasZaehlObjekteSearch(final Geometry g, final NasSearchType type) {
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
        try {
            initConnection();
            final Statement st = fmeConn.createStatement();
            final StringBuilder sb = new StringBuilder();
            final String geostring = PostGisGeometryFactory.getPostGisCompliantDbString(geometry);
            if ((geometry instanceof Polygon) || (geometry instanceof MultiPolygon)) { // with buffer for geostring
                sb.append(FLURSTUECK_STMT.replace(
                        "<geom>",
                        "GeometryFromText('"
                                + geostring
                                + "')"));
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
    private int getGebaeudeObjectsCount() throws SearchException {
        try {
            initConnection();
            final Statement st = fmeConn.createStatement();
            final StringBuilder sb = new StringBuilder();
            final String geostring = PostGisGeometryFactory.getPostGisCompliantDbString(geometry);
            if ((geometry instanceof Polygon) || (geometry instanceof MultiPolygon)) { // with buffer for geostring
                sb.append(GEAEUDE_STMT.replace(
                        "<geom>",
                        "GeometryFromText('"
                                + geostring
                                + "')"));
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
        if (searchType == NasSearchType.FLURSTUECKE) {
            resultList.add(getFlurstueckObjectsCount());
        } else if (searchType == NasSearchType.GEBAEUDE) {
            resultList.add(getGebaeudeObjectsCount());
        }
        return resultList;
    }

    /**
     * DOCUMENT ME!
     *
     * @param  args  DOCUMENT ME!
     */
    public static void main(final String[] args) {
        final NasZaehlObjekteSearch search = new NasZaehlObjekteSearch(null, NasSearchType.GEBAEUDE);
        try {
            search.initConnection();
        } catch (SearchException ex) {
            Exceptions.printStackTrace(ex);
        }
    }
}
