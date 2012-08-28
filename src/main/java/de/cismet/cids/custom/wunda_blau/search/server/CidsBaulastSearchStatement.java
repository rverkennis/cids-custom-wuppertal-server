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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

//select distinct 666 as class_id, l.id, l.blattnummer, l.laufende_nummer as object_id from alb_baulast l
//, alb_baulast_baulastarten la, alb_baulast_art a
//, (
//select * from alb_baulast_flurstuecke_beguenstigt
//UNION
//select * from alb_baulast_flurstuecke_belastet) as fsj
//, alb_flurstueck_kicker k, flurstueck f
//, geom g
//where
//l.blattnummer ~ '^[0]*1234$'
//and loeschungsdatum is null
//and loeschungsdatum is not null
//and (
//l.id = fsj.baulast_reference and fsj.flurstueck = k.id
//and k.fs_referenz = f.id and f.umschreibendes_rechteck = g.id and g.geo_field && GeometryFromText('') and intersects(g.geo_field,GeometryFromText(''))
//and k.gemarkung = '123' and k.flur = '123' and k.zaehler = '123' and k.nenner = '123')
//and l.id = la.baulast_reference and la.baulast_art = a.id and a.baulast_art in ('art1', 'art2')
//order by blattnummer, laufende_nummer
/**
 * DOCUMENT ME!
 *
 * @author   stefan
 * @version  $Revision$, $Date$
 */
public class CidsBaulastSearchStatement extends CidsServerSearch {

    //~ Enums ------------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    public enum Result {

        //~ Enum constants -----------------------------------------------------

        BAULAST, BAULASTBLATT
    }

    //~ Instance fields --------------------------------------------------------

    //
    private String blattnummer;
    //
    private Result result;
    //
    private boolean gueltig;
    private boolean ungueltig;
    //
    private boolean belastet;
    private boolean beguenstigt;
    //
    private String bounds;
    //
    private List<FlurstueckInfo> flurstuecke;
    //
    private String art;
    private final int baulastClassID;
    private final int baulastblattClassID;
    private String blattnummerquerypart = "";
    private String gueltigquerypart = "";
    private String ungueltigquerypart = "";
    private String geoquerypart = "";
    private String fsquerypart = "";
    private String artquerypart = "";

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new CidsBaulastSearchStatement object.
     *
     * @param  searchInfo           DOCUMENT ME!
     * @param  baulastClassID       DOCUMENT ME!
     * @param  baulastblattClassID  DOCUMENT ME!
     */
    public CidsBaulastSearchStatement(final BaulastSearchInfo searchInfo,
            final int baulastClassID,
            final int baulastblattClassID) {
        this.baulastClassID = baulastClassID;
        this.baulastblattClassID = baulastblattClassID;
        this.result = searchInfo.getResult();
        this.blattnummer = searchInfo.getBlattnummer();
        if (blattnummer != null) {
            blattnummer = StringEscapeUtils.escapeSql(blattnummer);
        }
        this.gueltig = searchInfo.isGueltig();
        this.ungueltig = searchInfo.isUngueltig();
        this.beguenstigt = searchInfo.isBeguenstigt();
        this.belastet = searchInfo.isBelastet();
        this.art = searchInfo.getArt();
        if (art != null) {
            art = StringEscapeUtils.escapeSql(art);
        }
        this.bounds = searchInfo.getBounds();
        if (bounds != null) {
            bounds = StringEscapeUtils.escapeSql(bounds);
        }
        this.flurstuecke = searchInfo.getFlurstuecke();

        if ((blattnummer != null) && (blattnummer.length() > 0)) {
            // ^             beginning of line
            // [0]*          preceded by any amount of 0s
            // [[:alpha:]]?  possibly followed by one letter (issue 2156)
            // $             end of line
            blattnummerquerypart = " and l.blattnummer ~* '^[0]*" + blattnummer + "[[:alpha:]]?$'";
        }

        if (!(gueltig && ungueltig)) {
            if (!gueltig && !ungueltig) {
                gueltigquerypart = " and false";
            } else {
                if (gueltig) {
                    gueltigquerypart = " and loeschungsdatum is null and geschlossen_am is null";
                } else if (ungueltig) {
                    ungueltigquerypart = " and (loeschungsdatum is not null or geschlossen_am is not null)";
                }
            }
        }

        if (bounds != null) {
            geoquerypart = " and g.geo_field && GeometryFromText('" + bounds
                        + "',25832) and intersects(g.geo_field,GeometryFromText('" + bounds + "',25832))";
        }

        if ((art != null) && (art.length() > 0)) {
            artquerypart = " and l.id = la.baulast_reference and la.baulast_art = a.id and a.baulast_art = '" + art
                        + "'";
        }

        fsquerypart = getSqlByFlurstuecksInfo(flurstuecke);
    }

    //~ Methods ----------------------------------------------------------------

    @Override
    public Collection performServerSearch() {
        try {
            final String primary = getPrimaryQuery();
            final String secondary = getSecondaryQuery();
            final MetaService ms = (MetaService)getActiveLoaclServers().get("WUNDA_BLAU");
            final List<ArrayList> primaryResultList = ms.performCustomSearch(primary);

            final List<MetaObjectNode> aln = new ArrayList<MetaObjectNode>();
            for (final ArrayList al : primaryResultList) {
                final int cid = (Integer)al.get(0);
                final int oid = (Integer)al.get(1);
//                final String name = "<html><p><!--sorter:000 -->" + (String)al.get(2) + "</p></html>";
                final MetaObjectNode mon = new MetaObjectNode("WUNDA_BLAU", oid, cid, (String)al.get(2));
//                mon.setIconString("/res/16/bewoelkt.png");
                aln.add(mon);
            }

            if ((flurstuecke != null) && (flurstuecke.size() > 0)) {
                final List<ArrayList> secondaryResultList = ms.performCustomSearch(secondary);
                for (final ArrayList al : secondaryResultList) {
                    final int cid = (Integer)al.get(0);
                    final int oid = (Integer)al.get(1);
//                    final String name = "<html><p><!--sorter:001 -->" + (String)al.get(2) + " (indirekt)"
//                                + "</p></html>";
                    final MetaObjectNode mon = new MetaObjectNode(
                            "WUNDA_BLAU",
                            oid,
                            cid,
                            "indirekt: "
                                    + (String)al.get(2));
//                    mon.setIconString("/res/16/bewoelkt.png");
                    aln.add(mon);
                }
            }
            return aln;
        } catch (Exception e) {
            getLog().error("Problem der Baulastensuche", e);
            throw new RuntimeException("Problem der Baulastensuche", e);
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param   fis  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private String getSqlByFlurstuecksInfo(final Collection<FlurstueckInfo> fis) {
        assert (fis != null);
        String queryPart = "";
        if (fis.size() > 0) {
            queryPart += " AND ( ";
            for (final FlurstueckInfo fi : fis) {
                queryPart += getSqlByFlurstuecksInfo(fi);
                queryPart += " or ";
            }
            queryPart = queryPart.substring(0, queryPart.length() - 4); // letztes " or " wieder entfernen
            queryPart += " ) ";
        }
        return queryPart;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   fi  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private String getSqlByFlurstuecksInfo(final FlurstueckInfo fi) {
        return " ( k.gemarkung = '" + fi.gemarkung + "' and k.flur = '"
                    + StringEscapeUtils.escapeSql(fi.flur) + "' and k.zaehler = '"
                    + StringEscapeUtils.escapeSql(fi.zaehler) + "' and k.nenner = '"
                    + StringEscapeUtils.escapeSql(fi.nenner) + "' ) ";
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private String getBelastetBeguenstigtSubselect() {
        if (belastet || beguenstigt) {
            String subselect = "";
            subselect += " (";
            if (beguenstigt) {
                subselect += " select * from alb_baulast_flurstuecke_beguenstigt";
                if (belastet) {
                    subselect += " UNION";
                }
            }
            if (belastet) {
                subselect += " select * from alb_baulast_flurstuecke_belastet";
            }
            subselect += " ) as fsj";
            return subselect;
        } else {
            return " (SELECT * FROM   alb_baulast_flurstuecke_beguenstigt where true=false) AS fsj";
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private String getPrimaryQuery() {
        String query = "";

        final String queryBlattPrefix = ""
                    + "SELECT " + baulastblattClassID + "  AS class_id, "
                    + "\n       b.id AS object_id, "
                    + "\n       b.blattnummer "
                    + "\nFROM   alb_baulastblatt b "
                    + "\nWHERE  b.blattnummer IN (SELECT blattnummer "
                    + "\n                         FROM "
                    + "\n       (";
        final String queryMid = ""
                    + "\nSELECT " + baulastClassID + "  AS class_id, "
                    + "\n               l.id AS object_id, "
                    + "\n               l.blattnummer|| '/' || case when l.laufende_nummer is not null then l.laufende_nummer else 'keine laufende Nummer' end, "
                    + "\n               l.blattnummer , "
                    + "\n               l.laufende_nummer "
                    + "\n        FROM   alb_baulast l "
                    + "\n               left outer join alb_baulast_baulastarten la on (l.id = la.baulast_reference) "
                    + "\n               left outer join alb_baulast_art a on (la.baulast_art = a.id),"
                    + "\n" + getBelastetBeguenstigtSubselect()
                    + "\n               , "
                    + "\n               alb_flurstueck_kicker k, "
                    + "\n               flurstueck f, "
                    + "\n               geom g "
                    + "\n        WHERE  1 = 1 "
                    + "\n               AND l.id = fsj.baulast_reference "
                    + "\n               AND fsj.flurstueck = k.id "
                    + "\n               AND k.fs_referenz = f.id "
                    + "\n               AND f.umschreibendes_rechteck = g.id "
                    + "\n               "
                    + blattnummerquerypart               // --  AND l.blattnummer LIKE '^[0]*4711[[:alpha:]]?$'  "
                    + "\n               " + geoquerypart
                    + "\n               " + artquerypart // -- AND a.baulast_art = 'Wertsteigerungsverzicht' "
                    + "\n               " + ungueltigquerypart
                    + "\n               " + gueltigquerypart
                    + "\n               " + fsquerypart;
        final String queryBlattPostfix = ""
                    + "\n) AS x "
                    + "\n                        ) "
                    + "\nGROUP  BY b.blattnummer, "
                    + "\n          class_id, "
                    + "\n          object_id "
                    + "\nORDER  BY b.blattnummer ";
        final String queryBaulastPostfix = ""
                    + "\n group by blattnummer, laufende_nummer, class_id, object_id"
                    + "\n order by blattnummer, laufende_nummer";

        if (result == Result.BAULASTBLATT) {
            query = queryBlattPrefix + queryMid + queryBlattPostfix;
        } else {
            query = queryMid + queryBaulastPostfix;
        }
        return query;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private String getSecondaryQuery() {
        String query = "";
        final String queryBlattPrefix = ""
                    + "SELECT " + baulastblattClassID + "        AS class_id, "
                    + "\n       b.id       AS object_id, "
                    + "\n       b.blattnummer "
                    + "\nFROM   alb_baulastblatt b "
                    + "\nWHERE  b.blattnummer IN (SELECT blattnummer "
                    + "\n                         FROM (";
        final String queryMid = ""
                    + "\n       SELECT " + baulastClassID + "  AS class_id, "
                    + "\n               l.id AS object_id, "
                    + "\n               l.blattnummer|| '/' || case when l.laufende_nummer is not null then l.laufende_nummer else 'keine laufende Nummer' end, "
                    + "\n               l.blattnummer, "
                    + "\n               l.laufende_nummer "
                    + "\n        FROM   alb_baulast l "
                    + "\n               left outer join alb_baulast_baulastarten la on (l.id = la.baulast_reference) "
                    + "\n               left outer join alb_baulast_art a on (la.baulast_art = a.id),"
                    + "\n               alb_flurstueck_kicker k, "
                    + "\n               flurstueck f, "
                    + "\n" + getBelastetBeguenstigtSubselect()
                    + "\n               , "
                    + "\n              (SELECT f.gemarkungs_nr gemarkung, "
                    + "\n                      f.flur          flur, "
                    + "\n                      f.fstnr_z       zaehler, "
                    + "\n                      f.fstnr_n       nenner "
                    + "\n               FROM   alb_flurstueck_kicker k, "
                    + "\n                      flurstueck f, "
                    + "\n                      geom g, "
                    + "\n                      (SELECT f.id fid, "
                    + "\n                              k.id kid, "
                    + "\n                              geo_field "
                    + "\n                       FROM   alb_flurstueck_kicker k "
                    + "\n                              LEFT OUTER JOIN (SELECT "
                    + "\n                              flurstueck "
                    + "\n                                               FROM "
                    + "\n                              alb_baulast_flurstuecke_beguenstigt "
                    + "\n                                               UNION "
                    + "\n                                               SELECT flurstueck "
                    + "\n                                               FROM "
                    + "\n                              alb_baulast_flurstuecke_belastet "
                    + "\n                                              ) AS x "
                    + "\n                                ON ( x.flurstueck = k.id ), "
                    + "\n                              flurstueck f, "
                    + "\n                              geom g "
                    + "\n                       WHERE  x.flurstueck IS NULL "
                    + "\n                              AND k.fs_referenz = f.id "
                    + "\n                              AND f.umschreibendes_rechteck = g.id "
                    + "\n                              " + fsquerypart
                    + "\n                                                     ) AS y "
                    + "\n               WHERE  k.fs_referenz = f.id "
                    + "\n                      AND f.umschreibendes_rechteck = g.id "
                    + "\n                      AND y.geo_field && g.geo_field "
                    + "\n                      AND Intersects(y.geo_field, g.geo_field) "
                    + "\n                      AND NOT y.fid = f.id "
                    + "\n                      AND Intersects(Buffer(y.geo_field, -0.05), g.geo_field)"
                    + "\n                      " + geoquerypart
                    + "\n                ) "
                    + "\n              AS indirekt "
                    + "\n                                 WHERE  1 = 1 "
                    + "\n                                        AND l.id = fsj.baulast_reference "
                    + "\n                                        AND fsj.flurstueck = k.id "
                    + "\n                                        AND k.fs_referenz = f.id "
                    + "\n                                        AND f.gemarkungs_nr = indirekt.gemarkung "
                    + "\n                                        AND f.flur = indirekt.flur "
                    + "\n                                        AND f.fstnr_z = indirekt.zaehler "
                    + "\n                                        AND f.fstnr_n = indirekt.nenner "
                    + "\n                                      "
                    + blattnummerquerypart // --  AND l.blattnummer LIKE '^[0]*4711[[:alpha:]]?$'  "
                    + "\n                                      "
                    + artquerypart         // -- AND a.baulast_art = 'Wertsteigerungsverzicht' "
                    + "\n                                      " + ungueltigquerypart
                    + "\n                                      " + gueltigquerypart;
        final String queryBlattPostfix = ""
                    + "\n)AS x) "
                    + "\nGROUP  BY b.blattnummer, "
                    + "\n          class_id, "
                    + "\n          object_id "
                    + "\nORDER  BY b.blattnummer";
        final String queryBaulastPostfix = ""
                    + "\n group by blattnummer, laufende_nummer, class_id, object_id"
                    + "\n order by blattnummer, laufende_nummer";

        if (result == Result.BAULASTBLATT) {
            query = queryBlattPrefix + queryMid + queryBlattPostfix;
        } else {
            query = queryMid + queryBaulastPostfix;
        }
        return query;
    }

    /**
     * DOCUMENT ME!
     *
     * @param  args  DOCUMENT ME!
     */
    public static void main(final String[] args) {
        final BaulastSearchInfo bsi = new BaulastSearchInfo();
        bsi.setResult(Result.BAULASTBLATT);
//        bsi.getFlurstuecke().add(new FlurstueckInfo(3135, "252", "576", "0"));
        // bsi.getFlurstuecke().add(new FlurstueckInfo(3279, "012", "1975", "402"));
        bsi.setBlattnummer("9724");

        final CidsBaulastSearchStatement css = new CidsBaulastSearchStatement(bsi, 177, 182);
        System.out.println(css.getPrimaryQuery());
    }
}
