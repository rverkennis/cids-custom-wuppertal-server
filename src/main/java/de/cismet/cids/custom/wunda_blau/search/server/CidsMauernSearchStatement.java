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

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;

import org.apache.log4j.Logger;

import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import de.cismet.cids.server.search.AbstractCidsServerSearch;
import de.cismet.cids.server.search.MetaObjectNodeServerSearch;
import de.cismet.cids.server.search.SearchException;

import de.cismet.cismap.commons.jtsgeometryfactories.PostGisGeometryFactory;

/**
 * DOCUMENT ME!
 *
 * @author   daniel
 * @version  $Revision$, $Date$
 */
public class CidsMauernSearchStatement extends AbstractCidsServerSearch implements MetaObjectNodeServerSearch {

    //~ Static fields/initializers ---------------------------------------------

    private static final Logger LOG = Logger.getLogger(CidsMauernSearchStatement.class);
    private static final String CIDSCLASS = "mauer";
    private static final String SQL_STMT = "SELECT DISTINCT (SELECT c.id FROM cs_class c WHERE table_name ilike '"
                + CIDSCLASS + "') as class_id, m.id,m.lagebezeichnung as name FROM <fromClause> <whereClause>";
    private static final String FROM = CIDSCLASS + " m";
    private static final String JOIN_GEOM = " LEFT OUTER JOIN geom g ON m.georeferenz = g.id";
    private static final String JOIN_LASTKLASSE = " LEFT OUTER JOIN mauer_lastklasse l ON l.id=m.lastklasse";
    private static final String JOIN_EIGENTUEMER = " LEFT OUTER JOIN mauer_eigentuemer e ON e.id=m.eigentuemer";
    private static final String DOMAIN = "WUNDA_BLAU";

    //~ Enums ------------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    public enum SearchMode {

        //~ Enum constants -----------------------------------------------------

        AND_SEARCH, OR_SEARCH;
    }

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    public enum PropertyKeys {

        //~ Enum constants -----------------------------------------------------

        HOEHE_VON, HOEHE_BIS, GELAENDER_VON, GELAENDER_BIS, ANSICHT_VON, ANSICHT_BIS, WANDKOPF_VON, WANDKOPF_BIS,
        GRUENDUNG_VON, GRUENDUNG_BIS, VERFORMUNG_VON, VERFORMUNG_BIS, GELAENDE_VON, GELAENDE_BIS, BAUSUBSTANZ_VON,
        BAUSUBSTANZ_BIS, SANIERUNG_VON, SANIERUNG_BIS;
    }

    //~ Instance fields --------------------------------------------------------

    private boolean leadingConjucjtionNeeded = false;
    private boolean whereNeeded = true;
    private boolean lastBraceNeeded = false;
    private boolean hoeheHandled = false;
    private boolean gelaenderHandled = false;
    private boolean kopfHandled = false;
    private boolean ansichtHandled = false;
    private boolean gruendungHandled = false;
    private boolean verformungHandled = false;
    private boolean gelaendeHandled = false;
    private boolean bausubstanzHandled = false;
    private boolean sanierungHandled = false;
    private String CONJUNCTION;
    private Geometry geom;
    private List<Integer> eigentuemer;
    private List<Integer> lastKlasseIds;
    private Date pruefungFrom;
    private Date pruefungTil;
    private HashMap<PropertyKeys, Double> filter;
    private final StringBuilder fromBuilder = new StringBuilder(FROM);
    private final StringBuilder whereBuilder = new StringBuilder();

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new CidsMauernSearchStatement object.
     *
     * @param  eigentuemerIds  DOCUMENT ME!
     * @param  pruefFrom       DOCUMENT ME!
     * @param  pruefTil        DOCUMENT ME!
     * @param  lastKlasseIds   DOCUMENT ME!
     * @param  geom            DOCUMENT ME!
     * @param  searchMode      DOCUMENT ME!
     * @param  filterProps     DOCUMENT ME!
     */
    public CidsMauernSearchStatement(final List<Integer> eigentuemerIds,
            final Date pruefFrom,
            final Date pruefTil,
            final List<Integer> lastKlasseIds,
            final Geometry geom,
            final SearchMode searchMode,
            final HashMap<PropertyKeys, Double> filterProps) {
        this.geom = geom;
        this.eigentuemer = eigentuemerIds;
        this.lastKlasseIds = lastKlasseIds;
        this.pruefungFrom = pruefFrom;
        this.pruefungTil = pruefTil;
        this.filter = filterProps;
        CONJUNCTION = (searchMode == SearchMode.AND_SEARCH) ? " AND " : " OR ";
    }

    //~ Methods ----------------------------------------------------------------

    @Override
    public Collection<MetaObjectNode> performServerSearch() throws SearchException {
        try {
            final ArrayList result = new ArrayList();
            final StringBuilder sqlBuilder = new StringBuilder();
            final MetaService metaService = (MetaService)getActiveLocalServers().get(DOMAIN);
            if (metaService == null) {
                LOG.error("Could not retrieve MetaService '" + DOMAIN + "'.");
                return result;
            }

            if ((geom == null) && ((eigentuemer == null) || eigentuemer.isEmpty())
                        && ((lastKlasseIds == null) || lastKlasseIds.isEmpty())
                        && (pruefungFrom == null)
                        && (pruefungTil == null)
                        && ((filter == null) || filter.isEmpty())) {
                LOG.warn("No filters provided. Cancel search.");
                return result;
            }

            sqlBuilder.append(SQL_STMT.replace("<fromClause>", generateFromClause()).replace(
                    "<whereClause>",
                    generateWhereClause()));

            final ArrayList<ArrayList> resultset;
            if (LOG.isDebugEnabled()) {
                LOG.debug("Executing SQL statement '" + sqlBuilder.toString() + "'.");
            }
            resultset = metaService.performCustomSearch(sqlBuilder.toString());

            for (final ArrayList mauer : resultset) {
                final int classID = (Integer)mauer.get(0);
                final int objectID = (Integer)mauer.get(1);
                final String name = (String)mauer.get(2);

                final MetaObjectNode node = new MetaObjectNode(DOMAIN, objectID, classID, name);

                result.add(node);
            }
            return result;
        } catch (final Exception e) {
            LOG.error("Problem", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private String generateFromClause() {
        if ((geom != null)) {
            fromBuilder.append(JOIN_GEOM);
        }

        if ((eigentuemer != null) && !eigentuemer.isEmpty()) {
            fromBuilder.append(JOIN_EIGENTUEMER);
            if (whereNeeded) {
                whereBuilder.append("WHERE (");
                lastBraceNeeded = true;
                whereNeeded = false;
            }
            if (leadingConjucjtionNeeded) {
                whereBuilder.append(CONJUNCTION);
            } else {
                leadingConjucjtionNeeded = true;
            }
            whereBuilder.append(" m.eigentuemer in (");
            for (final Integer eigentuemerID : eigentuemer) {
                if (eigentuemer.indexOf(eigentuemerID) == (eigentuemer.size() - 1)) {
                    whereBuilder.append(eigentuemerID);
                } else {
                    whereBuilder.append(eigentuemerID + ",");
                }
            }
            whereBuilder.append(") ");
        }

        if ((lastKlasseIds != null) && !lastKlasseIds.isEmpty()) {
            fromBuilder.append(JOIN_LASTKLASSE);
            if (whereNeeded) {
                whereBuilder.append("WHERE (");
                whereNeeded = false;
                lastBraceNeeded = true;
            }
            if (leadingConjucjtionNeeded) {
                whereBuilder.append(CONJUNCTION);
            } else {
                leadingConjucjtionNeeded = true;
            }
            whereBuilder.append(" m.lastklasse in (");
            for (final Integer lastklasseId : lastKlasseIds) {
                if (lastKlasseIds.indexOf(lastklasseId) == (lastKlasseIds.size() - 1)) {
                    whereBuilder.append(lastklasseId);
                } else {
                    whereBuilder.append(lastklasseId + ",");
                }
            }
            whereBuilder.append(") ");
        }

        return fromBuilder.toString();
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private String generateWhereClause() {
        if ((pruefungFrom != null) || (pruefungTil != null)) {
            final SimpleDateFormat df = new SimpleDateFormat("yyy-MM-dd");
            if (whereNeeded) {
                whereBuilder.append("WHERE (");
                lastBraceNeeded = true;
                whereNeeded = false;
            }
            whereBuilder.append(CONJUNCTION);
            whereBuilder.append(" (");
            boolean flag = false;
            if (pruefungFrom != null) {
                whereBuilder.append("m.datum_naechste_pruefung >='" + df.format(pruefungFrom) + "'");
                flag = true;
            }
            if (pruefungTil != null) {
                if (flag) {
                    whereBuilder.append(" and ");
                }
                whereBuilder.append(" m.datum_naechste_pruefung <='" + df.format(pruefungTil) + "'");
            }
            whereBuilder.append(") ");
        }

        for (final PropertyKeys pKey : filter.keySet()) {
            generateWhereConditionForProperty(pKey);
        }

        if (geom != null) {
            if (whereNeeded) {
                whereBuilder.append("WHERE");
                whereNeeded = false;
            } else {
                whereBuilder.append(") AND ");
                lastBraceNeeded = false;
            }
            final String geostring = PostGisGeometryFactory.getPostGisCompliantDbString(geom);
//            whereBuilder.append(CONJUNCTION);

            whereBuilder.append(" g.geo_field && GeometryFromText('").append(geostring).append("')");

            whereBuilder.append("AND");

            if ((geom instanceof Polygon) || (geom instanceof MultiPolygon)) { // with buffer for geostring
                whereBuilder.append(" intersects("
                            + "st_buffer(geo_field, 0.000001),"
                            + "st_buffer(GeometryFromText('"
                            + geostring
                            + "'), 0.000001))");
            } else {                                                           // without buffer for
                // geostring
                whereBuilder.append(" and intersects("
                            + "st_buffer(geo_field, 0.000001),"
                            + "GeometryFromText('"
                            + geostring
                            + "'))");
            }
        }
        if (lastBraceNeeded) {
            whereBuilder.append(")");
        }
        return whereBuilder.toString();
    }

    /**
     * DOCUMENT ME!
     *
     * @param  pKey  DOCUMENT ME!
     */
    private void generateWhereConditionForProperty(final PropertyKeys pKey) {
        if ((!hoeheHandled) && ((pKey == PropertyKeys.HOEHE_VON) || (pKey == PropertyKeys.HOEHE_BIS))) {
            hoeheHandled = true;
            final Double hoeheVon = filter.get(PropertyKeys.HOEHE_VON);
            final Double hoeheBis = filter.get(PropertyKeys.HOEHE_BIS);
            if ((hoeheVon != null) || (hoeheBis != null)) {
                if (whereNeeded) {
                    whereBuilder.append("WHERE (");
                    lastBraceNeeded = true;
                    whereNeeded = false;
                }
                if (leadingConjucjtionNeeded) {
                    whereBuilder.append(CONJUNCTION);
                } else {
                    leadingConjucjtionNeeded = true;
                }
                whereBuilder.append(" (");
                boolean flag = false;
                if (hoeheVon != null) {
                    whereBuilder.append("m.hoehe_min >=").append(hoeheVon);
                    flag = true;
                }
                if (hoeheBis != null) {
                    if (flag) {
                        whereBuilder.append(" and ");
                    }
                    whereBuilder.append(" m.hoehe_max <=").append(hoeheBis);
                }
                whereBuilder.append(") ");
            }
        } else if ((!ansichtHandled) && ((pKey == PropertyKeys.ANSICHT_VON) || (pKey == PropertyKeys.ANSICHT_BIS))) {
            ansichtHandled = true;
            final Double vonValue = filter.get(PropertyKeys.ANSICHT_VON);
            final Double bisValue = filter.get(PropertyKeys.ANSICHT_BIS);
            if ((vonValue != null) || (bisValue != null)) {
                if (whereNeeded) {
                    whereBuilder.append("WHERE (");
                    lastBraceNeeded = true;
                    whereNeeded = false;
                }
                if (leadingConjucjtionNeeded) {
                    whereBuilder.append(CONJUNCTION);
                } else {
                    leadingConjucjtionNeeded = true;
                }
                whereBuilder.append(" (");
                boolean flag = false;
                if (vonValue != null) {
                    whereBuilder.append("m.zustand_ansicht >=").append(vonValue);
                    flag = true;
                }
                if (bisValue != null) {
                    if (flag) {
                        whereBuilder.append(" and ");
                    }
                    whereBuilder.append(" m.zustand_ansicht <=").append(bisValue);
                }
                whereBuilder.append(") ");
            }
        } else if ((!gelaenderHandled)
                    && ((pKey == PropertyKeys.GELAENDER_VON)
                        || (pKey == PropertyKeys.GELAENDER_BIS))) {
            gelaenderHandled = true;
            final Double vonValue = filter.get(PropertyKeys.GELAENDER_VON);
            final Double bisValue = filter.get(PropertyKeys.GELAENDER_BIS);
            if ((vonValue != null) || (bisValue != null)) {
                if (whereNeeded) {
                    whereBuilder.append("WHERE (");
                    lastBraceNeeded = true;
                    whereNeeded = false;
                }
                if (leadingConjucjtionNeeded) {
                    whereBuilder.append(CONJUNCTION);
                } else {
                    leadingConjucjtionNeeded = true;
                }
                whereBuilder.append(" (");
                boolean flag = false;
                if (vonValue != null) {
                    whereBuilder.append("m.zustand_gelaender >=").append(vonValue);
                    flag = true;
                }
                if (bisValue != null) {
                    if (flag) {
                        whereBuilder.append(" and ");
                    }
                    whereBuilder.append(" m.zustand_gelaender <=").append(bisValue);
                }
                whereBuilder.append(") ");
            }
        } else if ((!kopfHandled) && ((pKey == PropertyKeys.WANDKOPF_VON) || (pKey == PropertyKeys.WANDKOPF_BIS))) {
            kopfHandled = true;
            final Double vonValue = filter.get(PropertyKeys.WANDKOPF_VON);
            final Double bisValue = filter.get(PropertyKeys.WANDKOPF_BIS);
            if ((vonValue != null) || (bisValue != null)) {
                if (whereNeeded) {
                    whereBuilder.append("WHERE (");
                    lastBraceNeeded = true;
                    whereNeeded = false;
                }
                if (leadingConjucjtionNeeded) {
                    whereBuilder.append(CONJUNCTION);
                } else {
                    leadingConjucjtionNeeded = true;
                }
                whereBuilder.append(" (");
                boolean flag = false;
                if (vonValue != null) {
                    whereBuilder.append("m.zustand_kopf >=").append(vonValue);
                    flag = true;
                }
                if (bisValue != null) {
                    if (flag) {
                        whereBuilder.append(" and ");
                    }
                    whereBuilder.append(" m.zustand_kopf <=").append(bisValue);
                }
                whereBuilder.append(") ");
            }
        } else if ((!gruendungHandled)
                    && ((pKey == PropertyKeys.GRUENDUNG_VON)
                        || (pKey == PropertyKeys.GRUENDUNG_BIS))) {
            gruendungHandled = true;
            final Double vonValue = filter.get(PropertyKeys.GRUENDUNG_VON);
            final Double bisValue = filter.get(PropertyKeys.GRUENDUNG_BIS);
            if ((vonValue != null) || (bisValue != null)) {
                if (whereNeeded) {
                    whereBuilder.append("WHERE (");
                    lastBraceNeeded = true;
                    whereNeeded = false;
                }
                if (leadingConjucjtionNeeded) {
                    whereBuilder.append(CONJUNCTION);
                } else {
                    leadingConjucjtionNeeded = true;
                }
                whereBuilder.append(" (");
                boolean flag = false;
                if (vonValue != null) {
                    whereBuilder.append("m.zustand_gruendung >=").append(vonValue);
                    flag = true;
                }
                if (bisValue != null) {
                    if (flag) {
                        whereBuilder.append(" and ");
                    }
                    whereBuilder.append(" m.zustand_gruendung <=").append(bisValue);
                }
                whereBuilder.append(") ");
            }
        } else if (!verformungHandled
                    && ((pKey == PropertyKeys.VERFORMUNG_VON)
                        || (pKey == PropertyKeys.VERFORMUNG_BIS))) {
            verformungHandled = true;
            final Double vonValue = filter.get(PropertyKeys.VERFORMUNG_VON);
            final Double bisValue = filter.get(PropertyKeys.VERFORMUNG_BIS);
            if ((vonValue != null) || (bisValue != null)) {
                if (whereNeeded) {
                    whereBuilder.append("WHERE (");
                    lastBraceNeeded = true;
                    whereNeeded = false;
                }
                if (leadingConjucjtionNeeded) {
                    whereBuilder.append(CONJUNCTION);
                } else {
                    leadingConjucjtionNeeded = true;
                }
                whereBuilder.append(" (");
                boolean flag = false;
                if (vonValue != null) {
                    whereBuilder.append("m.zustand_verformung >=").append(vonValue);
                    flag = true;
                }
                if (bisValue != null) {
                    if (flag) {
                        whereBuilder.append(" and ");
                    }
                    whereBuilder.append(" m.zustand_verformung <=").append(bisValue);
                }
                whereBuilder.append(") ");
            }
        } else if (!gelaendeHandled && ((pKey == PropertyKeys.GELAENDE_VON) || (pKey == PropertyKeys.GELAENDE_BIS))) {
            gelaendeHandled = true;
            final Double vonValue = filter.get(PropertyKeys.GELAENDE_VON);
            final Double bisValue = filter.get(PropertyKeys.GELAENDE_BIS);
            if ((vonValue != null) || (bisValue != null)) {
                if (whereNeeded) {
                    whereBuilder.append("WHERE (");
                    lastBraceNeeded = true;
                    whereNeeded = false;
                }
                if (leadingConjucjtionNeeded) {
                    whereBuilder.append(CONJUNCTION);
                } else {
                    leadingConjucjtionNeeded = true;
                }
                whereBuilder.append(" (");
                boolean flag = false;
                if (vonValue != null) {
                    whereBuilder.append("m.zustand_gelaende >=").append(vonValue);
                    flag = true;
                }
                if (bisValue != null) {
                    if (flag) {
                        whereBuilder.append(" and ");
                    }
                    whereBuilder.append(" m.zustand_gelaende <=").append(bisValue);
                }
                whereBuilder.append(") ");
            }
        } else if (((pKey == PropertyKeys.BAUSUBSTANZ_VON)
                        || (pKey == PropertyKeys.BAUSUBSTANZ_BIS)) && !bausubstanzHandled) {
            bausubstanzHandled = true;
            final Double vonValue = filter.get(PropertyKeys.BAUSUBSTANZ_VON);
            final Double bisValue = filter.get(PropertyKeys.BAUSUBSTANZ_BIS);
            if ((vonValue != null) || (bisValue != null)) {
                if (whereNeeded) {
                    whereBuilder.append("WHERE (");
                    lastBraceNeeded = true;
                    whereNeeded = false;
                }
                if (leadingConjucjtionNeeded) {
                    whereBuilder.append(CONJUNCTION);
                } else {
                    leadingConjucjtionNeeded = true;
                }
                whereBuilder.append(" (");
                boolean flag = false;
                if (vonValue != null) {
                    whereBuilder.append(
                            "(zustand_kopf+ zustand_gelaender+zustand_ansicht+zustand_gruendung+ zustand_verformung+zustand_gelaende)/6 >=")
                            .append(vonValue);
                    flag = true;
                }
                if (bisValue != null) {
                    if (flag) {
                        whereBuilder.append(" and ");
                    }
                    whereBuilder.append(
                            "(zustand_kopf+ zustand_gelaender+zustand_ansicht+zustand_gruendung+ zustand_verformung+zustand_gelaende)/6 <=")
                            .append(bisValue);
                }
                whereBuilder.append(") ");
            }
        } else if (((pKey == PropertyKeys.SANIERUNG_VON)
                        || (pKey == PropertyKeys.SANIERUNG_BIS)) && !sanierungHandled) {
            sanierungHandled = true;
            final Double vonValue = filter.get(PropertyKeys.SANIERUNG_VON);
            final Double bisValue = filter.get(PropertyKeys.GELAENDE_BIS);
            if ((vonValue != null) || (bisValue != null)) {
                if (whereNeeded) {
                    whereBuilder.append("WHERE (");
                    lastBraceNeeded = true;
                    whereNeeded = false;
                }
                if (leadingConjucjtionNeeded) {
                    whereBuilder.append(CONJUNCTION);
                } else {
                    leadingConjucjtionNeeded = true;
                }
                whereBuilder.append(" (");
                boolean flag = false;
                if (vonValue != null) {
                    whereBuilder.append("m.sanierung >=").append(vonValue);
                    flag = true;
                }
                if (bisValue != null) {
                    if (flag) {
                        whereBuilder.append(" and ");
                    }
                    whereBuilder.append(" m.sanierung <=").append(bisValue);
                }
                whereBuilder.append(") ");
            }
        }
    }
}
