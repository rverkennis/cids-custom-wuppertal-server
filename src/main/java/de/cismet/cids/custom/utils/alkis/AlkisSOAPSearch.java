/***************************************************
*
* cismet GmbH, Saarbruecken, Germany
*
*              ... and it just works.
*
****************************************************/
/*
 *  Copyright (C) 2011 stefan
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.cismet.cids.custom.utils.alkis;

import de.aedsicad.aaaweb.service.alkis.catalog.ALKISCatalogServices;
import de.aedsicad.aaaweb.service.alkis.info.ALKISInfoServices;
import de.aedsicad.aaaweb.service.alkis.search.ALKISSearchServices;
import de.aedsicad.aaaweb.service.util.Buchungsblatt;
import de.aedsicad.aaaweb.service.util.LandParcel;
import de.aedsicad.aaaweb.service.util.Owner;

import java.rmi.RemoteException;

/**
 * DOCUMENT ME!
 *
 * @author   stefan
 * @version  $Revision$, $Date$
 */
public class AlkisSOAPSearch {

    //~ Methods ----------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @param   args  DOCUMENT ME!
     *
     * @throws  RemoteException  DOCUMENT ME!
     */
    public static void main(final String[] args) throws RemoteException {
        final SOAPAccessProvider access = new SOAPAccessProvider();
        final ALKISSearchServices search = access.getAlkisSearchService();
        final ALKISCatalogServices catalog = access.getAlkisCatalogServices();
        final ALKISInfoServices info = access.getAlkisInfoService();
        // aIdentityCard,aConfiguration,aSalutation,aForeName,aSurName,aBirthName,
        // aDateOfBirth,aResidence,aAdministrativeUnitId,aMaxSearchTime
        // searchService.searchOwnersWithAttributes(accessProvider.getIdentityCard(), accessProvider.getService(),
        // salutation, vorname, name, geburtsname, geburtstag, null, null, TIMEOUT);
        final long l = System.currentTimeMillis();
        final String[] ownersIds = search.searchOwnersWithAttributes(access.getIdentityCard(),
                access.getService(),
                null,
                null,
                "DRH Deutsche Reihenhaus AG",
                null,
                null,
                null,
                null,
                100000); // "23.08.1971"

        // String[] ownersIds = search.searchOwnersWithAttributes(access.getIdentityCard(), access.getService(), null,
        // null, "meier", null, null, null, null, 100000);//"23.08.1971" String[] ownersIds =
        // search.searchOwnersWithAttributes(access.getIdentityCard(), access.getService(), null, null, "Engemann",
        // null, null, null, null, 10000);

        if ((ownersIds == null) || (ownersIds.length == 0)) {
            System.out.println("kein treffer");
        } else {
            System.out.println(ownersIds.length);
            System.out.println(ownersIds[0]);
            System.out.println(System.currentTimeMillis() - l);
//            Owner[] owners = info.getOwners(access.getIdentityCard(), access.getService(), ownersIds);
//            for (Owner o : owners) {
//                System.out.println(o.getOwnerId() + " " + o.getForeName() + " " + o.getSurName() + " " + o.getDateOfBirth() + " " + o.getNameOfBirth() + " " + o.getSalutationCode());
//            }
//            String[] fstckIds = search.searchParcelsWithOwner(access.getIdentityCard(), access.getService(), ownersIds, null);
//            //forceFullInfo = true
//            LandParcel[] result = info.getLandParcels(access.getIdentityCard(), access.getService(), fstckIds, true);
//            for (LandParcel lp : result) {
//                System.out.println(lp.getLocation().getLandParcelCode());
//            }
        }

//        Owner[] os=info.getOwners(access.getIdentityCard(), access.getService(),ownersIds);
//
//        System.out.println("BBC over Owner:"+os[0].getBuchungsblattCode());

//        Buchungsblatt b=access.getAlkisInfoService().getBuchungsblatt(access.getIdentityCard(), access.getService(),"053135-047669 ");
//        System.out.println(b.getBuchungsblattCode());

    }
}
