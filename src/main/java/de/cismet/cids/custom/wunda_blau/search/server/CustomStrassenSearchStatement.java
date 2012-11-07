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

import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Collection;

import de.cismet.cids.server.search.AbstractCidsServerSearch;
import de.cismet.cids.server.search.MetaObjectNodeServerSearch;

/**
 * DOCUMENT ME!
 *
 * @author   thorsten
 * @version  $Revision$, $Date$
 */
public class CustomStrassenSearchStatement extends AbstractCidsServerSearch implements MetaObjectNodeServerSearch {

    //~ Static fields/initializers ---------------------------------------------

    /** LOGGER. */
    private static final transient Logger LOG = Logger.getLogger(CustomStrassenSearchStatement.class);

    //~ Instance fields --------------------------------------------------------

    private String searchString;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new CustomStrassenSearchStatement object.
     *
     * @param  searchString  DOCUMENT ME!
     */
    public CustomStrassenSearchStatement(final String searchString) {
        this.searchString = searchString;
    }

    //~ Methods ----------------------------------------------------------------

    @Override
    public Collection<MetaObjectNode> performServerSearch() {
        try {
            LOG.fatal("search started");

            final MetaService ms = (MetaService)getActiveLocalServers().get("WUNDA_BLAU");

            final MetaClass c = ms.getClassByTableName(getUser(), "strasse");

            final String sql = "select strassenschluessel,name from strasse where name like '%" + searchString
                        + "%' order by name desc";

            final ArrayList<ArrayList> result = ms.performCustomSearch(sql);

            final ArrayList<MetaObjectNode> aln = new ArrayList<MetaObjectNode>();
            for (final ArrayList al : result) {
                final int id = (Integer)al.get(0);
                final MetaObjectNode mon = new MetaObjectNode(c.getDomain(), id, c.getId());

                aln.add(mon);
            }
            // Thread.sleep(5000);
            return aln;
        } catch (Exception e) {
            LOG.fatal("Problem", e);
            throw new RuntimeException(e);
        }
    }
}
