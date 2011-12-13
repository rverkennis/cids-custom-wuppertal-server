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

    //~ Instance fields --------------------------------------------------------

    private final String searchString;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new CustomAlkisPointSearchStatement object.
     *
     * @param  searchString  DOCUMENT ME!
     */
    public CustomAlkisPointSearchStatement(final String searchString) {
        this.searchString = searchString;
    }

    //~ Methods ----------------------------------------------------------------

    @Override
    public Collection performServerSearch() {
        getLog().info("Starting search for '" + searchString + "'.");

        final ArrayList result = new ArrayList();

        final MetaService metaService = (MetaService)getActiveLoaclServers().get(DOMAIN);
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

        final String sql = SQL.replace("<searchString>", searchString);
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
