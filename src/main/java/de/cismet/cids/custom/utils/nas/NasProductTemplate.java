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

/**
 * Enumeration that defines the different NAS Templates.
 *
 * @author   daniel
 * @version  $Revision$, $Date$
 */
public enum NasProductTemplate {

    //~ Enum constants ---------------------------------------------------------

    KOMPLETT("Komplett", "naskom"), OHNE_EIGENTUEMER("ohne Eigentuemer", "nasoeig"), POINTS("nur Punkte", "pktlsttxt");

    //~ Instance fields --------------------------------------------------------

    private final String displayText;
    private final String billingKey;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new ProductTemplate object.
     *
     * @param  s           DOCUMENT ME!
     * @param  billingKey  DOCUMENT ME!
     */
    private NasProductTemplate(final String s, final String billingKey) {
        displayText = s;
        this.billingKey = billingKey;
    }

    //~ Methods ----------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public String getDisplayText() {
        return displayText;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public String getBillingKey() {
        return billingKey;
    }

    @Override
    public String toString() {
        return displayText;
    }
}
