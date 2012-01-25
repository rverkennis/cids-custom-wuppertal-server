/***************************************************
*
* cismet GmbH, Saarbruecken, Germany
*
*              ... and it just works.
*
****************************************************/
package de.cismet.cids.custom.wunda_blau.search.server;

import Sirius.server.middleware.interfaces.domainserver.MetaService;
import Sirius.server.middleware.types.MetaObjectNode;
import Sirius.server.search.CidsServerSearch;

import org.apache.commons.lang.StringEscapeUtils;

import java.rmi.RemoteException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

/**
 * DOCUMENT ME!
 *
 * @author   jweintraut
 * @version  $Revision$, $Date$
 */
public class CidsAlkisPointSearchStatement extends CidsServerSearch {

    //~ Static fields/initializers ---------------------------------------------

    private static final String DOMAIN = "WUNDA_BLAU";
    private static final String CIDSCLASS = "alkis_point";
    private static final String SQL = "SELECT DISTINCT (SELECT c.id FROM cs_class c WHERE table_name ilike '"
                + CIDSCLASS
                + "') as class_id, ap.id, ap.pointcode FROM <fromClause> <whereClause> ORDER BY ap.pointcode DESC";

    //~ Enums ------------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    public enum Pointtype {

        //~ Enum constants -----------------------------------------------------

        AUFNAHMEPUNKTE(4), SONSTIGE_VERMESSUNGSPUNKTE(5), GRENZPUNKTE(1), BESONDERE_GEBAEUDEPUNKTE(2),
        BESONDERE_BAUWERKSPUNKTE(3);

        //~ Instance fields ----------------------------------------------------

        private int id;

        //~ Constructors -------------------------------------------------------

        /**
         * Creates a new Pointtype object.
         *
         * @param  id  DOCUMENT ME!
         */
        Pointtype(final int id) {
            this.id = id;
        }

        //~ Methods ------------------------------------------------------------

        /**
         * DOCUMENT ME!
         *
         * @return  DOCUMENT ME!
         */
        public int getId() {
            return id;
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    public enum GST {

        //~ Enum constants -----------------------------------------------------

        LE2(new int[] { 1000, 1100, 1200, 2000 }), LE3(new int[] { 1000, 1100, 1200, 2000, 2100 }),
        LE6(new int[] { 1000, 1100, 1200, 2000, 2100, 2200 }),
        LE10(new int[] { 1000, 1100, 1200, 2000, 2100, 2200, 2300 });

        //~ Instance fields ----------------------------------------------------

        private int[] condition;

        //~ Constructors -------------------------------------------------------

        /**
         * Creates a new GST object.
         *
         * @param  condition  DOCUMENT ME!
         */
        GST(final int[] condition) {
            this.condition = condition;
        }

        //~ Methods ------------------------------------------------------------

        /**
         * DOCUMENT ME!
         *
         * @return  DOCUMENT ME!
         */
        public int[] getCondition() {
            return condition;
        }
    }

    //~ Instance fields --------------------------------------------------------

    private final String pointcode;
    private Collection<Pointtype> pointtypes;
    private GST gst;
    private String geometry;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new CustomAlkisPointSearchStatement object.
     *
     * @param  pointcode   DOCUMENT ME!
     * @param  pointtypes  DOCUMENT ME!
     * @param  gst         DOCUMENT ME!
     * @param  geometry    DOCUMENT ME!
     */
    public CidsAlkisPointSearchStatement(final String pointcode,
            final Collection<Pointtype> pointtypes,
            final GST gst,
            final String geometry) {
        this.pointcode = StringEscapeUtils.escapeSql(pointcode);
        this.pointtypes = pointtypes;
        this.gst = gst;
        this.geometry = geometry;
    }

    //~ Methods ----------------------------------------------------------------

    @Override
    public Collection performServerSearch() {
        getLog().info("Starting search for ALKIS Point. Pointcode: '" + pointcode + "', pointtypes: '" + pointtypes
                    + "', GST: '" + gst + "', geometry: '" + geometry + "'.");

        final ArrayList result = new ArrayList();

        final MetaService metaService = (MetaService)getActiveLoaclServers().get(DOMAIN);
        if (metaService == null) {
            getLog().error("Could not retrieve MetaService '" + DOMAIN + "'.");
            return result;
        }

        final String sql = SQL.replace("<fromClause>", generateFromClause())
                    .replace("<whereClause>", generateWhereClause());

        final ArrayList<ArrayList> resultset;
        try {
            if (getLog().isDebugEnabled()) {
                getLog().debug("Executing SQL statement '" + sql + "'.");
            }
            resultset = metaService.performCustomSearch(sql);
        } catch (RemoteException ex) {
            getLog().error("Error occurred while executing SQL statement '" + sql + "'.", ex);
            return result;
        }

        for (final ArrayList alkisPoint : resultset) {
            final int classID = (Integer)alkisPoint.get(0);
            final int objectID = (Integer)alkisPoint.get(1);
            final String name = (String)alkisPoint.get(2);

            final MetaObjectNode node = new MetaObjectNode(DOMAIN, objectID, classID, name);

            result.add(node);
        }

        return result;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    protected String generateFromClause() {
        String fromClause = null;
        if (geometry != null) {
            fromClause = CIDSCLASS.concat(" ap, geom g");
        } else {
            fromClause = CIDSCLASS.concat(" ap");
        }
        return fromClause;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    protected String generateWhereClause() {
        final StringBuilder whereClauseBuilder = new StringBuilder();
        String conjunction = "WHERE ";
        if ((pointcode != null) && (pointcode.trim().length() > 0)) {
            whereClauseBuilder.append(conjunction);

            whereClauseBuilder.append("ap.pointcode like '");
            whereClauseBuilder.append(pointcode);
            whereClauseBuilder.append('\'');

            conjunction = " AND ";
        }
        if ((pointtypes != null) && !pointtypes.isEmpty()) {
            whereClauseBuilder.append(conjunction);
            whereClauseBuilder.append('(');

            final Iterator<Pointtype> pointtypesIter = pointtypes.iterator();
            while (pointtypesIter.hasNext()) {
                final Pointtype pointtype = pointtypesIter.next();

                whereClauseBuilder.append("ap.pointtype=");
                whereClauseBuilder.append(pointtype.getId());

                if (pointtypesIter.hasNext()) {
                    whereClauseBuilder.append(" OR ");
                }
            }
            whereClauseBuilder.append(')');

            conjunction = " AND ";
        }
        if (gst != null) {
            whereClauseBuilder.append(conjunction);
            whereClauseBuilder.append('(');

            final int[] condition = gst.getCondition();
            for (int i = 0; i < condition.length; i++) {
                whereClauseBuilder.append("ap.gst=");
                whereClauseBuilder.append(condition[i]);

                if (i < (condition.length - 1)) {
                    whereClauseBuilder.append(" OR ");
                }
            }
            whereClauseBuilder.append(')');

            conjunction = " AND ";
        }
        if (geometry != null) {
            whereClauseBuilder.append(conjunction);
            conjunction = " AND ";

            whereClauseBuilder.append("ap.geom = g.id");

            whereClauseBuilder.append(conjunction);

            whereClauseBuilder.append("g.geo_field && GeometryFromText('");
            whereClauseBuilder.append(geometry);
            whereClauseBuilder.append("')");

            whereClauseBuilder.append(conjunction);

            whereClauseBuilder.append("intersects(g.geo_field, GeometryFromText('");
            whereClauseBuilder.append(geometry);
            whereClauseBuilder.append("'))");
        }
        return whereClauseBuilder.toString();
    }
}
