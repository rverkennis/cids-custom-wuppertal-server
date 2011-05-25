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
import de.aedsicad.aaaweb.service.alkis.info.ALKISInfoServices;
import de.aedsicad.aaaweb.service.alkis.search.ALKISSearchServices;
import de.aedsicad.aaaweb.service.util.Buchungsblatt;
import de.aedsicad.aaaweb.service.util.Owner;
import de.cismet.cids.custom.utils.alkis.SOAPAccessProvider;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.apache.commons.lang.StringEscapeUtils;

/**
 * DOCUMENT ME!
 *
 * @author   stefan
 * @version  $Revision$, $Date$
 */
public class CidsAlkisSearchStatement extends CidsServerSearch {

    public static String WILDCARD = "%";

    public enum Resulttyp {

        FLURSTUECK, BUCHUNGSBLATT
    }

    public enum SucheUeber {

        FLURSTUECKSNUMMER, BUCHUNGSBLATTNUMMER, EIGENTUEMER
    }

    public enum Personentyp {

        MANN, FRAU, FIRMA
    }
    private Resulttyp resulttyp = Resulttyp.FLURSTUECK;
    private String name;
    private String vorname;
    private String geburtsname;
    private String geburtstag;
    private Personentyp ptyp = null;
    private String flurstuecksnummer = null;
    private String buchungsblattnummer = null;
    private SucheUeber ueber = null;
    private static final int TIMEOUT = 100000;

    //~ Constructors -----------------------------------------------------------
    /**
     * Creates a new CidsBaulastSearchStatement object.
     *
     * @param  searchInfo  DOCUMENT ME!
     */
    public CidsAlkisSearchStatement(Resulttyp resulttyp, String name, String vorname, String geburtsname, String geburtstag, Personentyp ptyp) {
        this.resulttyp = resulttyp;
        this.ueber = SucheUeber.EIGENTUEMER;
        String lengthTest = name;
        this.name = lengthTest.length() > 0 ? lengthTest : null;
        lengthTest = vorname;
        this.vorname = lengthTest.length() > 0 ? lengthTest : null;
        lengthTest = geburtsname;
        this.geburtsname = lengthTest.length() > 0 ? lengthTest : null;
        lengthTest = geburtstag;
        this.geburtstag = lengthTest.length() > 0 ? lengthTest : null;
        this.ptyp = ptyp;
    }

    public CidsAlkisSearchStatement(Resulttyp resulttyp, SucheUeber ueber, String flurstuecksnummerOrBuchungsblattnummer) {
        this.resulttyp = resulttyp;
        this.ueber = ueber;
        if (ueber == SucheUeber.FLURSTUECKSNUMMER) {
            flurstuecksnummer = flurstuecksnummerOrBuchungsblattnummer;
        } else if (ueber == SucheUeber.BUCHUNGSBLATTNUMMER) {
            buchungsblattnummer = flurstuecksnummerOrBuchungsblattnummer;
        }
    }

    //~ Methods ----------------------------------------------------------------
    @Override
    public Collection performServerSearch() {
        List<Node> result = new ArrayList<Node>();
        try {
            final SOAPAccessProvider accessProvider = new SOAPAccessProvider();
            final ALKISSearchServices searchService = accessProvider.getAlkisSearchService();
            final ALKISInfoServices infoService = accessProvider.getAlkisInfoService();

            String query = null;

            switch (ueber) {
                case EIGENTUEMER:
                    String salutation = null;
                    if (ptyp == Personentyp.MANN) {
                        salutation = "2000";
                    } else if (ptyp == Personentyp.FRAU) {
                        salutation = "1000";
                    } else if (ptyp == Personentyp.FIRMA) {
                        salutation = "3000";
                    }
                    final String[] ownersIds = searchService.searchOwnersWithAttributes(accessProvider.getIdentityCard(), accessProvider.getService(), salutation, vorname, name, geburtsname, geburtstag, null, null, TIMEOUT);
                    Owner[] owners = infoService.getOwners(accessProvider.getIdentityCard(), accessProvider.getService(), ownersIds);

                    if (ownersIds != null) {
                        final String[] fstckIds = searchService.searchParcelsWithOwner(accessProvider.getIdentityCard(), accessProvider.getService(), ownersIds, null);

                        if (fstckIds != null) {
                            StringBuilder whereClauseBuilder = new StringBuilder(fstckIds.length * 20);
                            for (String lpAId : fstckIds) {
                                if (whereClauseBuilder.length() > 0) {
                                    whereClauseBuilder.append(',');
                                }
                                final String escapedCurrentLandparcelCode = StringEscapeUtils.escapeSql(lpAId);
                                whereClauseBuilder.append('\'').append(escapedCurrentLandparcelCode).append('\'');
                            }
                            if (resulttyp == Resulttyp.FLURSTUECK) {
                                query = "select (select id from cs_class where table_name ilike 'alkis_landparcel') as class_id, lp.id as object_id from alkis_landparcel lp where lp.alkis_id in (" + whereClauseBuilder + ")";
                            } else {
                                HashSet<String> buchungsblaetterIds = new HashSet<String>();
                                for (String fstck : fstckIds) {
                                    String[] bbs = infoService.getBuchungsblaetterOfLandParcel(accessProvider.getIdentityCard(), accessProvider.getService(), fstck);
                                    buchungsblaetterIds.addAll(Arrays.asList(bbs));
                                }


                                whereClauseBuilder = new StringBuilder(buchungsblaetterIds.size() * 20);
                                for (String bbs : buchungsblaetterIds) {
                                    getLog().fatal("test " + bbs);
                                    final Buchungsblatt b = infoService.getBuchungsblatt(accessProvider.getIdentityCard(), accessProvider.getService(), bbs);

                                    final Collection<Owner> testbuchungsblattowners = Arrays.asList(b.getOwners());

                                    for (Owner test : testbuchungsblattowners) {
                                        for (Owner fOwner : Arrays.asList(owners)) {
                                            if (fOwner.getOwnerId().equals(test.getOwnerId())) {
                                                if (whereClauseBuilder.length() > 0) {
                                                    whereClauseBuilder.append(',');
                                                }
                                                final String escapedCurrentBuchungsblattCode = StringEscapeUtils.escapeSql(b.getBuchungsblattCode());
                                                whereClauseBuilder.append('\'').append(escapedCurrentBuchungsblattCode).append('\'');
                                                break;
                                            }
                                        }
                                    }
                                    
                                }


                                query = "select (select id from cs_class where table_name ilike 'alkis_buchungsblatt') as class_id, id as object_id from alkis_buchungsblatt bb where bb.buchungsblattcode in (" + whereClauseBuilder + ")";
                            }
                        }
                    }
                    break;
                case BUCHUNGSBLATTNUMMER:
                    if (resulttyp == Resulttyp.FLURSTUECK) {
                        query = "select (select id from cs_class where table_name ilike 'alkis_landparcel') as class_id, lp.id as object_id from alkis_landparcel lp,alkis_flurstueck_to_buchungsblaetter jt,alkis_buchungsblatt bb where lp.buchungsblaetter=jt.flurstueck_reference and jt.buchungsblatt=bb.id and bb.buchungsblattcode ilike '" + buchungsblattnummer + "'";
                    } else {
                        query = "select (select id from cs_class where table_name ilike 'alkis_buchungsblatt') as class_id, jt.buchungsblatt as object_id from alkis_landparcel lp,alkis_flurstueck_to_buchungsblaetter jt,alkis_buchungsblatt bb where lp.buchungsblaetter=jt.flurstueck_reference and jt.buchungsblatt=bb.id and bb.buchungsblattcode ilike '" + buchungsblattnummer + "'";
                    }
                    break;

                case FLURSTUECKSNUMMER:
                    if (resulttyp == Resulttyp.FLURSTUECK) {
                        query = "select (select id from cs_class where table_name ilike 'alkis_landparcel') as class_id, lp.id as object_id from alkis_landparcel lp where lp.alkis_id ilike '" + flurstuecksnummer + "'";
                    } else {
                        query = "select (select id from cs_class where table_name ilike 'alkis_buchungsblatt') as class_id, jt.buchungsblatt as object_id from alkis_landparcel lp,alkis_flurstueck_to_buchungsblaetter jt where lp.buchungsblaetter=jt.flurstueck_reference and lp.alkis_id ilike '" + flurstuecksnummer + "'";
                    }
                    break;

            }
            getLog().info("Search:\n" + query);
            if (query != null) {
                final MetaService ms = (MetaService) getActiveLoaclServers().get("WUNDA_BLAU");

                final List<ArrayList> resultList = ms.performCustomSearch(query);
                for (final ArrayList al : resultList) {
                    final int cid = (Integer) al.get(0);
                    final int oid = (Integer) al.get(1);
                    final MetaObjectNode mon = new MetaObjectNode("WUNDA_BLAU", oid, cid);
                    result.add(mon);
                }
            }

        } catch (Exception e) {
            getLog().error("Problem", e);
        }
        return result;
    }
}
