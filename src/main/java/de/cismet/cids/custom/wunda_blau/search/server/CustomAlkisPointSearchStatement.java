/*
 * Copyright (C) 2011 cismet GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package de.cismet.cids.custom.wunda_blau.search.server;

import Sirius.server.middleware.interfaces.domainserver.MetaService;
import Sirius.server.middleware.types.MetaClass;
import Sirius.server.middleware.types.MetaObjectNode;
import Sirius.server.search.CidsServerSearch;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collection;

/**
 *
 * @author jweintraut
 */
public class CustomAlkisPointSearchStatement extends CidsServerSearch {
    private final static String DOMAIN = "WUNDA_BLAU";
    private final static String CIDSCLASS = "alkis_point";
    private final static String SQL = "SELECT id, pointcode FROM alkis_point WHERE pointcode like '%<searchString>%' ORDER BY pointcode DESC";
    
    
    private final String searchString;
    
    public CustomAlkisPointSearchStatement(final String searchString) {
        this.searchString = searchString;
    }
    
    @Override
    public Collection performServerSearch() {
        getLog().info("Starting search for '" + searchString + "'.");
        
        ArrayList result = new ArrayList();
        
        final MetaService metaService = (MetaService)getActiveLoaclServers().get(DOMAIN);
        if(metaService == null) {
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
        
        if(metaClass == null) {
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
            final int id = (Integer) alkisPoint.get(0);
            final MetaObjectNode node = new MetaObjectNode(metaClass.getDomain(), id, metaClass.getId());

            result.add(node);
        }
        
        return result;
    }
}
