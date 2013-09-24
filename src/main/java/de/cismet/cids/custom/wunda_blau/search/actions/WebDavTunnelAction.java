/***************************************************
*
* cismet GmbH, Saarbruecken, Germany
*
*              ... and it just works.
*
****************************************************/
package de.cismet.cids.custom.wunda_blau.search.actions;

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

import org.apache.commons.io.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import de.cismet.cids.server.actions.ServerAction;
import de.cismet.cids.server.actions.ServerActionParameter;

import de.cismet.netutil.Proxy;

import de.cismet.security.WebDavClient;

/**
 * DOCUMENT ME!
 *
 * @author   thorsten
 * @version  $Revision$, $Date$
 */
@org.openide.util.lookup.ServiceProvider(service = ServerAction.class)
public class WebDavTunnelAction implements ServerAction {

    //~ Static fields/initializers ---------------------------------------------

    private static final transient org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(
            WebDavTunnelAction.class);

    //~ Enums ------------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    public enum PARAMETER_TYPE {

        //~ Enum constants -----------------------------------------------------

        GET, PUT, DELETE, PROXY, USERNAME, PASSWORD, NTAUTH
    }

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new WebDavTunnelAction object.
     */
    public WebDavTunnelAction() {
    }

    //~ Methods ----------------------------------------------------------------

    @Override
    public Object execute(final Object body, final ServerActionParameter... params) {
        try {
            String path = null;
            boolean isGet = false;
            boolean isPut = false;
            boolean isDelete = false;
            Proxy proxy = null;
            String username = null;
            String password = null;
            boolean useNTAuth = false;

            for (final ServerActionParameter sap : params) {
                if (sap.getKey().equals(PARAMETER_TYPE.PROXY.toString())) {
                    proxy = (Proxy)sap.getValue();
                } else if (sap.getKey().equals(PARAMETER_TYPE.USERNAME.toString())) {
                    username = (String)sap.getValue();
                } else if (sap.getKey().equals(PARAMETER_TYPE.PASSWORD.toString())) {
                    password = (String)sap.getValue();
                } else if (sap.getKey().equals(PARAMETER_TYPE.NTAUTH.toString())) {
                    useNTAuth = (Boolean)sap.getValue();
                } else if (sap.getKey().equals(PARAMETER_TYPE.GET.toString())) {
                    path = (String)sap.getValue();
                    isGet = true;
                } else if (sap.getKey().equals(PARAMETER_TYPE.PUT.toString())) {
                    path = (String)sap.getValue();
                    isPut = true;
                } else if (sap.getKey().equals(PARAMETER_TYPE.DELETE.toString())) {
                    path = (String)sap.getValue();
                    isDelete = true;
                }
            }

            final WebDavClient webdavclient = new WebDavClient(proxy, username, password, useNTAuth);

            if (isGet) {
                final InputStream is = webdavclient.getInputStream(path);
                return IOUtils.toByteArray(is);
            } else if (isPut) {
                final InputStream data = new ByteArrayInputStream((byte[])body);
                webdavclient.put(path, data);
            } else if (isDelete) {
                webdavclient.delete(path);
            } else {
                throw new RuntimeException("Problem during WebDavTunnelAction - request have to be get, put or delete");
            }
            return null;
        } catch (Exception exception) {
            LOG.error("Problem during WebDavTunnelAction", exception);
            throw new RuntimeException("Problem during WebDavTunnelAction", exception);
        }
    }

    @Override
    public String getTaskName() {
        return "webDavTunnelAction";
    }
}
