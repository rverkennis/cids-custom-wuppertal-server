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
package de.cismet.cids.custom.utils.butler;

/**
 * DOCUMENT ME!
 *
 * @author   daniel
 * @version  $Revision$, $Date$
 */
class ButlerRequestInfo {

    //~ Instance fields --------------------------------------------------------

    private String userOrderId;
    private ButlerProduct product;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new ButlerRequestInfo object.
     *
     * @param  userOrderId  DOCUMENT ME!
     * @param  product      DOCUMENT ME!
     */
    public ButlerRequestInfo(final String userOrderId, final ButlerProduct product) {
        this.userOrderId = userOrderId;
        this.product = product;
    }

    //~ Methods ----------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public String getUserOrderId() {
        return userOrderId;
    }

    /**
     * DOCUMENT ME!
     *
     * @param  userOrderId  DOCUMENT ME!
     */
    public void setUserOrderId(final String userOrderId) {
        this.userOrderId = userOrderId;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public ButlerProduct getProduct() {
        return product;
    }

    /**
     * DOCUMENT ME!
     *
     * @param  product  DOCUMENT ME!
     */
    public void setProduct(final ButlerProduct product) {
        this.product = product;
    }
}
