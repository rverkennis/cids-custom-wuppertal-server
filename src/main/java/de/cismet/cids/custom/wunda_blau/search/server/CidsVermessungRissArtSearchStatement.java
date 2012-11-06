/***************************************************
*
* cismet GmbH, Saarbruecken, Germany
*
*              ... and it just works.
*
****************************************************/
package de.cismet.cids.custom.wunda_blau.search.server;

import Sirius.server.middleware.interfaces.domainserver.MetaService;
import Sirius.server.newuser.User;

import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;

import de.cismet.cids.server.search.AbstractCidsServerSearch;

/**
 * DOCUMENT ME!
 *
 * @author   jweintraut
 * @version  $Revision$, $Date$
 */
public class CidsVermessungRissArtSearchStatement extends AbstractCidsServerSearch {

    //~ Static fields/initializers ---------------------------------------------

    /** LOGGER. */
    private static final transient Logger LOG = Logger.getLogger(CidsVermessungRissArtSearchStatement.class);

    private static final String DOMAIN = "WUNDA_BLAU";
    private static final String CIDSCLASS = "vermessung_art";

    private static final String SQL = "SELECT"
                + " DISTINCT (SELECT c.id FROM cs_class c WHERE table_name ilike '" + CIDSCLASS + "') as class_id,"
                + " id,"
                + " code||' - '||name as name"
                + " FROM "
                + CIDSCLASS
                + " ORDER BY name";

    //~ Instance fields --------------------------------------------------------

    private final User user;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new CidsVermessungRissArtSearchStatement object.
     *
     * @param  user  DOCUMENT ME!
     */
    public CidsVermessungRissArtSearchStatement(final User user) {
        this.user = user;
    }

    //~ Methods ----------------------------------------------------------------

    @Override
    public Collection performServerSearch() {
        try {
            final Collection result = new LinkedList();

            if (LOG.isDebugEnabled()) {
                LOG.debug("Search for all geometry states started.");
            }

            final MetaService metaService = (MetaService)getActiveLocalServers().get(DOMAIN);
            if (metaService == null) {
                LOG.error("Could not retrieve MetaService '" + DOMAIN + "'.");
                return result;
            }

            final ArrayList<ArrayList> resultset;
            if (LOG.isDebugEnabled()) {
                LOG.debug("Executing SQL statement '" + SQL + "'.");
            }

            resultset = metaService.performCustomSearch(SQL);

            for (final ArrayList veraenderungsart : resultset) {
                final int classID = (Integer)veraenderungsart.get(0);
                final int objectID = (Integer)veraenderungsart.get(1);

                try {
                    result.add(metaService.getMetaObject(user, objectID, classID));
                } catch (final Exception ex) {
                    LOG.warn("Couldn't get CidsBean for class '" + classID + "', object '" + objectID + "', user '"
                                + user + "'.",
                        ex);
                }
            }

            return result;
        } catch (final Exception e) {
            LOG.error("Problem", e);
            throw new RuntimeException(e);
        }
    }
}
