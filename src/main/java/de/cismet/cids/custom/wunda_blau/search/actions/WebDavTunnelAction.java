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
            final ServerActionParameter param = params[0];
            final String path = (String)param.getValue();

            final Proxy proxy = (Proxy)params[1].getValue();
            final String username = (String)params[2].getValue();
            final String password = (String)params[3].getValue();
            final Boolean useNTAuth = (Boolean)params[4].getValue();

            final WebDavClient webdavclient = new WebDavClient(proxy, username, password, useNTAuth);

            if (param.getKey().equals(PARAMETER_TYPE.GET.toString())) {
                final InputStream is = webdavclient.getInputStream(path);
                return IOUtils.toByteArray(is);
            } else if (param.getKey().equals(PARAMETER_TYPE.PUT.toString())) {
                final InputStream data = new ByteArrayInputStream((byte[])body);
                webdavclient.put(path, data);
            } else if (param.getKey().equals(PARAMETER_TYPE.DELETE.toString())) {
                webdavclient.delete(path);
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
