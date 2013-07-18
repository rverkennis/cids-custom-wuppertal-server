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

import Sirius.server.newuser.User;

import de.cismet.cids.custom.utils.butler.ButlerProduct;
import de.cismet.cids.custom.utils.butler.ButlerProductGenerator;

import de.cismet.cids.server.actions.ServerAction;
import de.cismet.cids.server.actions.ServerActionParameter;
import de.cismet.cids.server.actions.UserAwareServerAction;

/**
 * DOCUMENT ME!
 *
 * @author   daniel
 * @version  $Revision$, $Date$
 */
@org.openide.util.lookup.ServiceProvider(service = ServerAction.class)
public class ButlerQueryAction implements UserAwareServerAction {

    //~ Enums ------------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    public enum METHOD_TYPE {

        //~ Enum constants -----------------------------------------------------

        ADD, GET, GET_ALL, CANCEL
    }

    /**
     * DOCUMENT ME!
     *
     * @version  $Revision$, $Date$
     */
    public enum PARAMETER_TYPE {

        //~ Enum constants -----------------------------------------------------

        IS_WMPS, REQUEST_ID, ORDER_ID, BUTLER_PRODUCT, MIN_X, MIN_Y, MAX_X, MAX_Y, METHOD, BOX_SIZE
    }

    //~ Instance fields --------------------------------------------------------

    private User user;

    //~ Methods ----------------------------------------------------------------

    @Override
    public Object execute(final Object body, final ServerActionParameter... params) {
        METHOD_TYPE method = null;
        String orderId = null;
        String requestId = null;
        ButlerProduct product = null;
        double minX = 0;
        double minY = 0;
        double maxX = 0;
        double maxY = 0;
        String box = null;

        boolean useWmps = false;
        for (final ServerActionParameter sap : params) {
            final String sapKey = sap.getKey();
            if (sapKey.equals(PARAMETER_TYPE.ORDER_ID.toString())) {
                orderId = (String)sap.getValue();
            } else if (sapKey.equals(PARAMETER_TYPE.BUTLER_PRODUCT.toString())) {
                product = (ButlerProduct)sap.getValue();
            } else if (sapKey.equals(PARAMETER_TYPE.MIN_X.toString())) {
                minX = (Double)sap.getValue();
            } else if (sapKey.equals(PARAMETER_TYPE.MIN_Y.toString())) {
                minY = (Double)sap.getValue();
            } else if (sapKey.equals(PARAMETER_TYPE.MAX_X.toString())) {
                maxX = (Double)sap.getValue();
            } else if (sapKey.equals(PARAMETER_TYPE.MAX_Y.toString())) {
                maxY = (Double)sap.getValue();
            } else if (sapKey.equals(PARAMETER_TYPE.METHOD.toString())) {
                method = (METHOD_TYPE)sap.getValue();
            } else if (sapKey.equals(PARAMETER_TYPE.REQUEST_ID.toString())) {
                requestId = (String)sap.getValue();
            } else if (sapKey.equals(PARAMETER_TYPE.IS_WMPS.toString())) {
                useWmps = ((Boolean)sap.getValue()).booleanValue();
            } else if (sapKey.equals(PARAMETER_TYPE.BOX_SIZE.toString())) {
                box = (String)sap.getValue();
            }
        }

        if (method == METHOD_TYPE.ADD) {
            if ((product != null) && (product.getKey() != null)) {
                if (!useWmps) {
                    return ButlerProductGenerator.getInstance()
                                .createButlerRequest(
                                    orderId,
                                    user,
                                    product,
                                    minX,
                                    minY,
                                    maxX,
                                    maxY,
                                    true);
                } else {
                    return ButlerProductGenerator.getInstance()
                                .createButler2Request(orderId, user, product, box, minX, minY);
                }
            }
        } else if (method == METHOD_TYPE.GET) {
            if ((product != null) && (product.getFormat() != null)) {
                return ButlerProductGenerator.getInstance()
                            .getResultForRequest(user, requestId, product.getFormat().getKey());
            }
        } else if (method == METHOD_TYPE.GET_ALL) {
            return ButlerProductGenerator.getInstance().getAllOpenUserRequests(user);
        } else if (method == METHOD_TYPE.CANCEL) {
            ButlerProductGenerator.getInstance().removeOrder(user, requestId);
        }

        return null;
    }

    @Override
    public String getTaskName() {
        return "butler1Query";
    }

    @Override
    public User getUser() {
        return user;
    }

    @Override
    public void setUser(final User user) {
        this.user = user;
    }
}
