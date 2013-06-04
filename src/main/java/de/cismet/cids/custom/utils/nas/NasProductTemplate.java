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

    KOMPLETT("Komplett"), OHNE_EIGENTUEMER("ohne Eigentuemer"), POINTS("nur Punkte");

    //~ Instance fields --------------------------------------------------------

    private final String displayText;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new ProductTemplate object.
     *
     * @param  s  DOCUMENT ME!
     */
    private NasProductTemplate(final String s) {
        displayText = s;
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

    @Override
    public String toString() {
        return displayText;
    }
}
