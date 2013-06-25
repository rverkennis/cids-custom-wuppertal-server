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
package de.cismet.cids.custom.utils.nas;

import java.io.Serializable;

/**
 * DOCUMENT ME!
 *
 * @author   daniel
 * @version  $Revision$, $Date$
 */
public class NasProductInfo implements Serializable {

    //~ Instance fields --------------------------------------------------------

    private boolean isSplittet;
    private String requestName;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new NasProductInfo object.
     *
     * @param  isSplittet   DOCUMENT ME!
     * @param  requestName  DOCUMENT ME!
     */
    public NasProductInfo(final boolean isSplittet, final String requestName) {
        this.isSplittet = isSplittet;
        this.requestName = requestName;
    }

    //~ Methods ----------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public boolean isIsSplittet() {
        return isSplittet;
    }

    /**
     * DOCUMENT ME!
     *
     * @param  isSplittet  DOCUMENT ME!
     */
    public void setIsSplittet(final boolean isSplittet) {
        this.isSplittet = isSplittet;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public String getRequestName() {
        return requestName;
    }

    /**
     * DOCUMENT ME!
     *
     * @param  requestName  DOCUMENT ME!
     */
    public void setRequestName(final String requestName) {
        this.requestName = requestName;
    }
}
