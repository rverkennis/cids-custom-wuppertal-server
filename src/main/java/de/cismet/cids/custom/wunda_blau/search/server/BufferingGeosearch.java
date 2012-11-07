/***************************************************
*
* cismet GmbH, Saarbruecken, Germany
*
*              ... and it just works.
*
****************************************************/
package de.cismet.cids.custom.wunda_blau.search.server;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;

import org.apache.log4j.Logger;

import org.openide.util.lookup.ServiceProvider;

import de.cismet.cids.server.search.builtin.DefaultGeoSearch;
import de.cismet.cids.server.search.builtin.GeoSearch;

/**
 * DOCUMENT ME!
 *
 * @author   martin.scholl@cismet.de
 * @version  $Revision$, $Date$
 */
@ServiceProvider(
    service = GeoSearch.class,
    position = 1000
)
public final class BufferingGeosearch extends DefaultGeoSearch {

    //~ Static fields/initializers ---------------------------------------------

    /** LOGGER. */
    private static final transient Logger LOG = Logger.getLogger(BufferingGeosearch.class);

    //~ Methods ----------------------------------------------------------------

    @Override
    public String getSearchSql(final String domainKey) {
        final String sql = ""                                                                                       // NOI18N
                    + "SELECT DISTINCT i.class_id , "                                                               // NOI18N
                    + "                i.object_id, "                                                               // NOI18N
                    + "                s.stringrep "                                                                // NOI18N
                    + "FROM            geom g, "                                                                    // NOI18N
                    + "                cs_attr_object_derived i "                                                   // NOI18N
                    + "                LEFT OUTER JOIN cs_stringrepcache s "                                        // NOI18N
                    + "                ON              ( "                                                          // NOI18N
                    + "                                                s.class_id =i.class_id "                     // NOI18N
                    + "                                AND             s.object_id=i.object_id "                    // NOI18N
                    + "                                ) "                                                          // NOI18N
                    + "WHERE           i.attr_class_id = "                                                          // NOI18N
                    + "                ( SELECT cs_class.id "                                                       // NOI18N
                    + "                FROM    cs_class "                                                           // NOI18N
                    + "                WHERE   cs_class.table_name::text = 'GEOM'::text "                           // NOI18N
                    + "                ) "                                                                          // NOI18N
                    + "AND             i.attr_object_id = g.id "                                                    // NOI18N
                    + "AND i.class_id IN <cidsClassesInStatement> "                                                 // NOI18N
                    + "AND geo_field && GeometryFromText('SRID=<cidsSearchGeometrySRID>;<cidsSearchGeometryWKT>') " // NOI18N
                    + "AND <intersectsStatement> "                                                                  // NOI18N
                    + "ORDER BY        1,2,3";                                                                      // NOI18N

        final Geometry searchGeometry = getGeometry();
        final String intersectsStatement;
        if (searchGeometry.getSRID() == 4326) {
            intersectsStatement =
                "intersects(geo_field,GeometryFromText('SRID=<cidsSearchGeometrySRID>;<cidsSearchGeometryWKT>'))";                                               // NOI18N
        } else {
            if ((searchGeometry instanceof Polygon) || (searchGeometry instanceof MultiPolygon)) {                                                               // with buffer for searchGeometry
                intersectsStatement =
                    "intersects(st_buffer(geo_field, 0.000001),st_buffer(GeometryFromText('SRID=<cidsSearchGeometrySRID>;<cidsSearchGeometryWKT>'), 0.000001))"; // NOI18N
            } else {                                                                                                                                             // without buffer for searchGeometry
                intersectsStatement =
                    "intersects(st_buffer(geo_field, 0.000001),GeometryFromText('SRID=<cidsSearchGeometrySRID>;<cidsSearchGeometryWKT>'))";                      // NOI18N
            }
        }
        final String cidsSearchGeometryWKT = searchGeometry.toText();
        final String sridString = Integer.toString(searchGeometry.getSRID());
        final String classesInStatement = getClassesInSnippetsPerDomain().get(domainKey);
        if ((cidsSearchGeometryWKT == null) || (cidsSearchGeometryWKT.trim().length() == 0)
                    || (sridString == null)
                    || (sridString.trim().length() == 0)) {
            // TODO: Notify user?
            LOG.error(
                "Search geometry or srid is not given. Can't perform a search without those information."); // NOI18N

            return null;
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("cidsClassesInStatement=" + classesInStatement);   // NOI18N
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("cidsSearchGeometryWKT=" + cidsSearchGeometryWKT); // NOI18N
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("cidsSearchGeometrySRID=" + sridString);           // NOI18N
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug("intersectsStatement=" + intersectsStatement);     // NOI18N
        }

        if ((classesInStatement == null) || (classesInStatement.trim().length() == 0)) {
            LOG.warn("There are no search classes defined for domain '" + domainKey // NOI18N
                        + "'. This domain will be skipped."); // NOI18N

            return null;
        }

        return
            sql.replaceAll("<intersectsStatement>", intersectsStatement)  // NOI18N
            .replaceAll("<cidsClassesInStatement>", classesInStatement)   // NOI18N
            .replaceAll("<cidsSearchGeometryWKT>", cidsSearchGeometryWKT) // NOI18N
            .replaceAll("<cidsSearchGeometrySRID>", sridString);          // NOI18N
    }
}
