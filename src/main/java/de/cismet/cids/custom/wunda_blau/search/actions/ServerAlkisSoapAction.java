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
package de.cismet.cids.custom.wunda_blau.search.actions;

import de.aedsicad.aaaweb.service.alkis.info.ALKISInfoServices;
import de.aedsicad.aaaweb.service.util.Buchungsblatt;
import de.aedsicad.aaaweb.service.util.Point;

import javassist.bytecode.CodeAttribute;

import java.rmi.RemoteException;

import de.cismet.cids.custom.utils.alkis.SOAPAccessProvider;

import de.cismet.cids.server.actions.ServerAction;
import de.cismet.cids.server.actions.ServerActionParameter;

/**
 * DOCUMENT ME!
 *
 * @author   thorsten
 * @version  $Revision$, $Date$
 */
@org.openide.util.lookup.ServiceProvider(service = ServerAction.class)
public class ServerAlkisSoapAction implements ServerAction {

    //~ Static fields/initializers ---------------------------------------------

    private static final transient org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(
            ServerAlkisSoapAction.class);
    private static SOAPAccessProvider soapProvider = new SOAPAccessProvider();
    private static ALKISInfoServices infoService = soapProvider.getAlkisInfoService();

    //~ Enums ------------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    public enum RETURN_VALUE {

        //~ Enum constants -----------------------------------------------------

        POINT, BUCHUNGSBLATT
    }

    //~ Methods ----------------------------------------------------------------

    @Override
    public Object execute(final Object body, final ServerActionParameter... params) {
        LOG.fatal("ALKIS SOAP CSA");

//        final SOAPAccessProvider soapProvider = new SOAPAccessProvider();
//        final ALKISInfoServices infoService = soapProvider.getAlkisInfoService();

        if (!(body instanceof RETURN_VALUE)) {
            throw new IllegalArgumentException("Body has to be either POINT or BUCHUNGSBLATT");
        }

        if (body.toString().equals(RETURN_VALUE.POINT.toString())) {
            // POINT
            try {
                final String pointCode = params[0].getValue().toString();
                final Point point = infoService.getPoint(soapProvider.getIdentityCard(),
                        soapProvider.getService(),
                        pointCode);
                return point;
            } catch (RemoteException remoteException) {
                LOG.error("Error in ServerAlkisSoapAction", remoteException);
                throw new RuntimeException("Error in ServerAlkisSoapAction", remoteException);
            }
        } else {
            // BUCHUNGSBLATT
            try {
                final String buchungsblattCode = params[0].getValue().toString();
                final Buchungsblatt buchungsblatt = infoService.getBuchungsblatt(soapProvider.getIdentityCard(),
                        soapProvider.getService(),
                        buchungsblattCode);
                return buchungsblatt;
            } catch (RemoteException remoteException) {
                LOG.error("Error in ServerAlkisSoapAction", remoteException);
                throw new RuntimeException("Error in ServerAlkisSoapAction", remoteException);
            }
        }
    }

    @Override
    public String getTaskName() {
        return "alkisSoapTunnelAction";
    }
}
