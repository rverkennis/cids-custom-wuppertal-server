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
public class ButlerFormat {

    //~ Instance fields --------------------------------------------------------

    String key;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new ButlerFormat object.
     */
    public ButlerFormat() {
    }

    /**
     * Creates a new ButlerFormat object.
     *
     * @param  key  DOCUMENT ME!
     */
    public ButlerFormat(final String key) {
        this.key = key;
    }

    //~ Methods ----------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public String getKey() {
        return key;
    }

    /**
     * DOCUMENT ME!
     *
     * @param  key  DOCUMENT ME!
     */
    public void setKey(final String key) {
        this.key = key;
    }

    @Override
    public String toString() {
        return super.toString(); // To change body of generated methods, choose Tools | Templates.
    }
}
