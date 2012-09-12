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
package de.cismet.cids.custom.wunda_blau.search.server;

import Sirius.server.middleware.interfaces.domainserver.MetaService;
import Sirius.server.middleware.types.MetaObjectNode;
import Sirius.server.newuser.User;
import Sirius.server.search.CidsServerSearch;

import org.openide.util.Exceptions;

import java.rmi.RemoteException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;

/**
 * DOCUMENT ME!
 *
 * @author   jweintraut
 * @version  $Revision$, $Date$
 */
public class CidsVermessungRissArtSearchStatement extends CidsServerSearch {

    //~ Static fields/initializers ---------------------------------------------

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
        
        final Collection result = new LinkedList();

        if (getLog().isDebugEnabled()) {
            getLog().debug("Search for all geometry states started.");
        }

        final MetaService metaService = (MetaService)getActiveLoaclServers().get(DOMAIN);
        if (metaService == null) {
            getLog().error("Could not retrieve MetaService '" + DOMAIN + "'.");
            return result;
        }

        final ArrayList<ArrayList> resultset;
        try {
            if (getLog().isDebugEnabled()) {
                getLog().debug("Executing SQL statement '" + SQL + "'.");
            }

            resultset = metaService.performCustomSearch(SQL);
        } catch (RemoteException ex) {
            getLog().error("Error occurred while executing SQL statement '" + SQL + "'.", ex);
            return result;
        }

        for (final ArrayList veraenderungsart : resultset) {
            final int classID = (Integer)veraenderungsart.get(0);
            final int objectID = (Integer)veraenderungsart.get(1);

            try {
                result.add(metaService.getMetaObject(user, objectID, classID));
            } catch (final Exception ex) {
                getLog().warn("Couldn't get CidsBean for class '" + classID + "', object '" + objectID + "', user '"
                            + user + "'.",
                    ex);
            }
        }

        return result;
    }
}
