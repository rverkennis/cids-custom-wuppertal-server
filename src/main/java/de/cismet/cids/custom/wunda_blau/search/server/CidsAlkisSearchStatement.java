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
import Sirius.server.middleware.types.Node;
import Sirius.server.search.CidsServerSearch;

import com.vividsolutions.jts.geom.Geometry;

import de.aedsicad.aaaweb.service.alkis.search.ALKISSearchServices;

import org.apache.commons.lang.StringEscapeUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import de.cismet.cids.custom.utils.alkis.SOAPAccessProvider;

import de.cismet.cismap.commons.jtsgeometryfactories.PostGisGeometryFactory;

/**
 * DOCUMENT ME!
 *
 * @author   stefan
 * @version  $Revision$, $Date$
 */
public class CidsAlkisSearchStatement extends CidsServerSearch {

    //~ Static fields/initializers ---------------------------------------------

    public static String WILDCARD = "%";
    private static final int TIMEOUT = 100000;

    //~ Enums ------------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    public enum Resulttyp {

        //~ Enum constants -----------------------------------------------------

        FLURSTUECK, BUCHUNGSBLATT
    }

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    public enum SucheUeber {

        //~ Enum constants -----------------------------------------------------

        FLURSTUECKSNUMMER, BUCHUNGSBLATTNUMMER, EIGENTUEMER
    }

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    public enum Personentyp {

        //~ Enum constants -----------------------------------------------------

        MANN, FRAU, FIRMA
    }

    //~ Instance fields --------------------------------------------------------

    private Resulttyp resulttyp = Resulttyp.FLURSTUECK;
    private String name;
    private String vorname;
    private String geburtsname;
    private String geburtstag;
    private Personentyp ptyp = null;
    private String flurstuecksnummer = null;
    private String buchungsblattnummer = null;
    private SucheUeber ueber = null;
    private Geometry geom = null;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new CidsAlkisSearchStatement object.
     *
     * @param  resulttyp                               DOCUMENT ME!
     * @param  ueber                                   DOCUMENT ME!
     * @param  flurstuecksnummerOrBuchungsblattnummer  DOCUMENT ME!
     * @param  g                                       DOCUMENT ME!
     */
    public CidsAlkisSearchStatement(final Resulttyp resulttyp,
            final SucheUeber ueber,
            final String flurstuecksnummerOrBuchungsblattnummer,
            final Geometry g) {
        this.resulttyp = resulttyp;
        this.ueber = ueber;
        if (ueber == SucheUeber.FLURSTUECKSNUMMER) {
            flurstuecksnummer = flurstuecksnummerOrBuchungsblattnummer;
        } else if (ueber == SucheUeber.BUCHUNGSBLATTNUMMER) {
            buchungsblattnummer = flurstuecksnummerOrBuchungsblattnummer;
        }
        geom = g;
    }

    /**
     * Creates a new CidsBaulastSearchStatement object.
     *
     * @param  resulttyp    searchInfo DOCUMENT ME!
     * @param  name         DOCUMENT ME!
     * @param  vorname      DOCUMENT ME!
     * @param  geburtsname  DOCUMENT ME!
     * @param  geburtstag   DOCUMENT ME!
     * @param  ptyp         DOCUMENT ME!
     * @param  g            DOCUMENT ME!
     */
    public CidsAlkisSearchStatement(final Resulttyp resulttyp,
            final String name,
            final String vorname,
            final String geburtsname,
            final String geburtstag,
            final Personentyp ptyp,
            final Geometry g) {
        this.resulttyp = resulttyp;
        this.ueber = SucheUeber.EIGENTUEMER;
        String lengthTest = name;
        this.name = (lengthTest.length() > 0) ? lengthTest : null;
        lengthTest = vorname;
        this.vorname = (lengthTest.length() > 0) ? lengthTest : null;
        lengthTest = geburtsname;
        this.geburtsname = (lengthTest.length() > 0) ? lengthTest : null;
        lengthTest = geburtstag;
        this.geburtstag = (lengthTest.length() > 0) ? lengthTest : null;
        this.ptyp = ptyp;
        geom = g;
    }

    //~ Methods ----------------------------------------------------------------

    @Override
    public Collection performServerSearch() {
        final List<Node> result = new ArrayList<Node>();
        try {
            final SOAPAccessProvider accessProvider = new SOAPAccessProvider();
            final ALKISSearchServices searchService = accessProvider.getAlkisSearchService();

            String query = null;

            switch (ueber) {
                case EIGENTUEMER: {
                    String salutation = null;
                    if (ptyp == Personentyp.MANN) {
                        salutation = "2000"; // NOI18N
                    } else if (ptyp == Personentyp.FRAU) {
                        salutation = "1000"; // NOI18N
                    } else if (ptyp == Personentyp.FIRMA) {
                        salutation = "3000"; // NOI18N
                    }
                    final String[] ownersIds = searchService.searchOwnersWithAttributes(accessProvider
                                    .getIdentityCard(),
                            accessProvider.getService(),
                            salutation,
                            vorname,
                            name,
                            geburtsname,
                            geburtstag,
                            null,
                            null,
                            TIMEOUT);

                    if (ownersIds != null) {
                        final StringBuilder whereClauseBuilder = new StringBuilder(ownersIds.length * 20);
                        for (final String oid : ownersIds) {
                            if (whereClauseBuilder.length() > 0) {
                                whereClauseBuilder.append(',');
                            }
                            whereClauseBuilder.append('\'').append(StringEscapeUtils.escapeSql(oid)).append('\'');
                        }
                        if (resulttyp == Resulttyp.FLURSTUECK) {
                            query =
                                "select distinct (select id from cs_class where table_name ilike 'alkis_landparcel') as class_id, lp.id as object_id, lp.alkis_id from alkis_landparcel lp,alkis_flurstueck_to_buchungsblaetter jt,alkis_buchungsblatt bb,ownerofbb ,geom where geom.id = lp.geometrie and lp.buchungsblaetter=jt.flurstueck_reference and jt.buchungsblatt=bb.id and bb.buchungsblattcode = ownerofbb.bb and ownerofbb.ownerid in ("
                                        + whereClauseBuilder
                                        + ")";
                        } else {
                            query =
                                "select distinct (select id from cs_class where table_name ilike 'alkis_buchungsblatt') as class_id, jt.buchungsblatt as object_id,bb.buchungsblattcode from alkis_landparcel lp,alkis_flurstueck_to_buchungsblaetter jt,alkis_buchungsblatt bb,ownerofbb ,geom where geom.id = lp.geometrie and lp.buchungsblaetter=jt.flurstueck_reference and jt.buchungsblatt=bb.id and bb.buchungsblattcode = ownerofbb.bb and ownerofbb.ownerid in ("
                                        + whereClauseBuilder
                                        + ")";
                        }
                        break;
                    }
                    break;
                }
                case BUCHUNGSBLATTNUMMER: {
                    if (resulttyp == Resulttyp.FLURSTUECK) {
                        query =
                            "select distinct (select id from cs_class where table_name ilike 'alkis_landparcel') as class_id, lp.id as object_id, lp.alkis_id from alkis_landparcel lp,alkis_flurstueck_to_buchungsblaetter jt,alkis_buchungsblatt bb ,geom where geom.id = lp.geometrie and lp.buchungsblaetter=jt.flurstueck_reference and jt.buchungsblatt=bb.id and bb.buchungsblattcode ilike '"
                                    + buchungsblattnummer
                                    + WILDCARD
                                    + "'";
                    } else {
                        query =
                            "select distinct (select id from cs_class where table_name ilike 'alkis_buchungsblatt') as class_id, jt.buchungsblatt as object_id,bb.buchungsblattcode from alkis_landparcel lp,alkis_flurstueck_to_buchungsblaetter jt,alkis_buchungsblatt bb ,geom where geom.id = lp.geometrie and lp.buchungsblaetter=jt.flurstueck_reference and jt.buchungsblatt=bb.id and bb.buchungsblattcode ilike '"
                                    + buchungsblattnummer
                                    + WILDCARD
                                    + "'";
                    }
                    break;
                }

                case FLURSTUECKSNUMMER: {
                    if (resulttyp == Resulttyp.FLURSTUECK) {
                        query =
                            "select distinct (select id from cs_class where table_name ilike 'alkis_landparcel') as class_id, lp.id as object_id, lp.alkis_id from alkis_landparcel lp ,geom where geom.id = lp.geometrie and lp.alkis_id ilike '"
                                    + flurstuecksnummer
                                    + "'";
                    } else {
                        query =
                            "select distinct (select id from cs_class where table_name ilike 'alkis_buchungsblatt') as class_id, jt.buchungsblatt as object_id,bb.buchungsblattcode from  alkis_landparcel lp,alkis_flurstueck_to_buchungsblaetter jt,alkis_buchungsblatt bb,geom where geom.id = lp.geometrie and lp.buchungsblaetter=jt.flurstueck_reference and jt.buchungsblatt=bb.id and lp.alkis_id ilike '"
                                    + flurstuecksnummer
                                    + "'";
                    }
                    break;
                }
            }
            if (geom != null) {
                final String geostring = PostGisGeometryFactory.getPostGisCompliantDbString(geom);
                query += " and intersects(st_buffer(geo_field, 0.00000001),GeometryFromText('"
                            + geostring
                            + "'))";
            }

            if (getLog().isInfoEnabled()) {
                getLog().info("Search:\n" + query);
            }

            if (query != null) {
                final MetaService ms = (MetaService)getActiveLoaclServers().get("WUNDA_BLAU");

                final List<ArrayList> resultList = ms.performCustomSearch(query);
                for (final ArrayList al : resultList) {
                    final int cid = (Integer)al.get(0);
                    final int oid = (Integer)al.get(1);
                    final String nodename = (String)al.get(2);
                    final MetaObjectNode mon = new MetaObjectNode("WUNDA_BLAU", oid, cid, nodename);
                    result.add(mon);
                }
            }
        } catch (final Exception e) {
            getLog().error("Problem", e);
            throw new RuntimeException(e);
        }
        return result;
    }
}
