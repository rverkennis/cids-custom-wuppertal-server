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
public class ButlerProduct {

    //~ Instance fields --------------------------------------------------------

    String key;
    String name;
    int colorDepth;
    ButlerResolution resolution;
    ButlerFormat format;

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

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public String getName() {
        return name;
    }

    /**
     * DOCUMENT ME!
     *
     * @param  name  DOCUMENT ME!
     */
    public void setName(final String name) {
        this.name = name;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public int getColorDepth() {
        return colorDepth;
    }

    /**
     * DOCUMENT ME!
     *
     * @param  colorDepth  DOCUMENT ME!
     */
    public void setColorDepth(final int colorDepth) {
        this.colorDepth = colorDepth;
    }

    @Override
    public String toString() {
        return name;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public ButlerResolution getResolution() {
        return resolution;
    }

    /**
     * DOCUMENT ME!
     *
     * @param  resolution  DOCUMENT ME!
     */
    public void setResolution(final ButlerResolution resolution) {
        this.resolution = resolution;
    }

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public ButlerFormat getFormat() {
        return format;
    }

    /**
     * DOCUMENT ME!
     *
     * @param  outputFormat  DOCUMENT ME!
     */
    public void setFormat(final ButlerFormat outputFormat) {
        this.format = outputFormat;
    }
}
