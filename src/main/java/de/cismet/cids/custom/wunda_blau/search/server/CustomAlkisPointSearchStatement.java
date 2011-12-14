/***************************************************
*
* cismet GmbH, Saarbruecken, Germany
*
*              ... and it just works.
*
****************************************************/
package de.cismet.cids.custom.wunda_blau.search.server;

import Sirius.server.middleware.interfaces.domainserver.MetaService;
import Sirius.server.middleware.types.MetaClass;
import Sirius.server.middleware.types.MetaObjectNode;
import Sirius.server.search.CidsServerSearch;

import java.rmi.RemoteException;

import java.util.ArrayList;
import java.util.Collection;

/**
 * DOCUMENT ME!
 *
 * @author   jweintraut
 * @version  $Revision$, $Date$
 */
public class CustomAlkisPointSearchStatement extends CidsServerSearch {

    //~ Static fields/initializers ---------------------------------------------

    private static final String DOMAIN = "WUNDA_BLAU";
    private static final String CIDSCLASS = "alkis_point";
    private static final String SQL =
        "SELECT id, pointcode FROM alkis_point WHERE pointcode like '%<searchString>%' ORDER BY pointcode DESC";
    private static final String SQL_GEOM = "SELECT ap.id, ap.pointcode FROM alkis_point ap, geom g"
                + " WHERE ap.geom = g.id"
                + " AND ap.pointcode like '%<searchString>%'"
                + " AND intersects(g.geo_field, envelope('<geometry>'::geometry))"
                + " ORDER BY ap.pointcode DESC";

    //~ Instance fields --------------------------------------------------------

    private final String searchString;
    private final String geometry;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new CustomAlkisPointSearchStatement object.
     *
     * @param  searchString  DOCUMENT ME!
     * @param  geometry      DOCUMENT ME!
     */
    public CustomAlkisPointSearchStatement(final String searchString, final String geometry) {
        this.searchString = searchString;
        this.geometry = geometry;
    }

    //~ Methods ----------------------------------------------------------------

    @Override
    public Collection performServerSearch() {
        final String statement;
        if ((geometry != null) && (geometry.trim().length() > 0)) {
            statement = SQL_GEOM;
            getLog().info("Starting search for '" + searchString + "' and geometry '" + geometry + "'.");
        } else {
            statement = SQL;
            getLog().info("Starting search for '" + searchString + "'.");
        }

        final ArrayList result = new ArrayList();

        final MetaService metaService = (MetaService)getActiveLocalServers().get(DOMAIN);
        if (metaService == null) {
            getLog().error("Could not retrieve MetaService '" + DOMAIN + "'.");
            return result;
        }

        final MetaClass metaClass;
        try {
            metaClass = metaService.getClassByTableName(getUser(), CIDSCLASS);
        } catch (RemoteException ex) {
            getLog().error("Could not retrieve MetaClass '" + CIDSCLASS + "'.", ex);
            return result;
        }

        if (metaClass == null) {
            getLog().error("Could not retrieve MetaClass '" + CIDSCLASS + "'.");
            return result;
        }

        final String sql = statement.replace("<searchString>", searchString).replace("<geometry>", geometry);
        ;
        final ArrayList<ArrayList> resultset;
        try {
            resultset = metaService.performCustomSearch(sql);
        } catch (RemoteException ex) {
            getLog().error("Error occurred while executing SQL statement '" + sql + "'.", ex);
            return result;
        }

        for (final ArrayList alkisPoint : resultset) {
            final int id = (Integer)alkisPoint.get(0);
            final MetaObjectNode node = new MetaObjectNode(metaClass.getDomain(), id, metaClass.getId());

            result.add(node);
        }

        return result;
    }
}
