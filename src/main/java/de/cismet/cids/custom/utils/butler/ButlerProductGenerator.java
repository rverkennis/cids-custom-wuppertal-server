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

import Sirius.server.newuser.User;

import org.apache.log4j.Logger;

import org.openide.util.Exceptions;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;

import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Properties;
import java.util.regex.Pattern;

/**
 * DOCUMENT ME!
 *
 * @author   daniel
 * @version  $Revision$, $Date$
 */
public class ButlerProductGenerator {

    //~ Static fields/initializers ---------------------------------------------

    private static final ButlerProductGenerator instance = new ButlerProductGenerator();
    private static final Logger LOG = Logger.getLogger(ButlerProductGenerator.class);
    private static final String pdfResultDir = "pdf";
    private static final String dxfResultDir = "dxf";
    private static final String tifResultDir = "tif";
    private static final String shapeResultDir = "shape";
    private static final String FILE_APPENDIX = ".but";
    private static final String SEPERATOR = ";";

    //~ Instance fields --------------------------------------------------------

    private final String requestFolder;
    private final String resultBaseFolder;
    private boolean initError = false;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new ButlerProductGenerator object.
     *
     * @throws  RuntimeException  DOCUMENT ME!
     */
    private ButlerProductGenerator() {
        try {
            final Properties butlerProperties = new Properties();
            butlerProperties.load(ButlerProductGenerator.class.getResourceAsStream("butler.properties"));
            requestFolder = butlerProperties.getProperty("butler1RequestPath");
            resultBaseFolder = butlerProperties.getProperty("butler1ResultPath");
        } catch (IOException ex) {
            LOG.error("Could not load butler properties", ex);
            throw new RuntimeException(ex);
        }
        checkFolders();
    }

    //~ Methods ----------------------------------------------------------------

    /**
     * DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    public static ButlerProductGenerator getInstance() {
        return instance;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   user        boundingBox DOCUMENT ME!
     * @param   productId   DOCUMENT ME!
     * @param   minX        DOCUMENT ME!
     * @param   minY        DOCUMENT ME!
     * @param   maxX        DOCUMENT ME!
     * @param   maxY        DOCUMENT ME!
     * @param   colorDepth  DOCUMENT ME!
     * @param   resolution  DOCUMENT ME!
     * @param   isGeoTiff   DOCUMENT ME!
     * @param   format      DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private ArrayList<byte[]> createButlerRequest(final User user,
            final String productId,
            final double minX,
            final double minY,
            final double maxX,
            final double maxY,
            final int colorDepth,
            final String resolution,
            final boolean isGeoTiff,
            final String format) {
        if (!initError) {
            File reqeustFile = null;
            FileWriter fw = null;
            final String filename = determineRequestFileName(user);
            try {
                reqeustFile = new File(requestFolder + System.getProperty("file.separator") + filename
                                + FILE_APPENDIX);
                if (reqeustFile.exists()) {
                    // should not happen;
                    LOG.error("butler 1 request file already exists");
                    return null;
                }
                fw = new FileWriter(reqeustFile);
                final BufferedWriter bw = new BufferedWriter(fw);
                bw.write(getRequestLine(productId, minX, minY, maxX, maxY, colorDepth, resolution, isGeoTiff, format));
                bw.close();
            } catch (IOException ex) {
                Exceptions.printStackTrace(ex);
            } finally {
                try {
                    fw.close();
                } catch (IOException ex) {
                    Exceptions.printStackTrace(ex);
                }
            }
            // check in a loop if the file still exists, if not look for the result...
            if (reqeustFile == null) {
                LOG.error("the butler request file can not be null - stoped any further computation");
                return null;
            }
            while (reqeustFile.exists()) {
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException ex) {
                    Exceptions.printStackTrace(ex);
                }
            }

            final ArrayList<byte[]> results = getResultFiles(filename, isGeoTiff, format);
            return results;
        }
        return null;
    }

    /**
     * DOCUMENT ME!
     *
     * @throws  RuntimeException  DOCUMENT ME!
     */
    private void checkFolders() {
        final File requestDir = new File(requestFolder);
        if (!requestDir.exists()) {
            requestDir.mkdirs();
        }
        if (!requestDir.isDirectory() || !requestDir.canWrite()) {
            LOG.error("could not write to the given butler request directory " + requestDir);
            throw new RuntimeException("could not write to the given butler request directory " + requestDir);
        }

        final File resultBaseDir = new File(resultBaseFolder);
        if (!resultBaseDir.exists()) {
            resultBaseDir.mkdirs();
        }
        final File pdfDir = new File(resultBaseDir + System.getProperty("file.separator") + pdfResultDir);
        final File dxfDir = new File(resultBaseDir + System.getProperty("file.separator") + dxfResultDir);
        final File tifDir = new File(resultBaseDir + System.getProperty("file.separator") + tifResultDir);
        final File shapeDir = new File(resultBaseDir + System.getProperty("file.separator") + shapeResultDir);

        if (!(pdfDir.exists() && dxfDir.exists() && tifDir.exists() && shapeDir.exists())) {
            LOG.fatal("one ore all of butler result directories does not exists");
            initError = true;
            return;
        }

        if (!(pdfDir.isDirectory() && pdfDir.canExecute() && dxfDir.isDirectory() && dxfDir.canExecute()
                        && tifDir.isDirectory() && tifDir.canExecute()
                        && shapeDir.isDirectory() && shapeDir.canExecute())) {
            LOG.fatal("can not write to one ore all of butler result directories");
            initError = true;
        }
    }

    /**
     * DOCUMENT ME!
     *
     * @param   user  DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private String determineRequestFileName(final User user) {
        final GregorianCalendar cal = new GregorianCalendar();
        cal.setTime(new Date());

        return user.getName() + "_" + cal.get(GregorianCalendar.HOUR_OF_DAY)
                    + "+" + cal.get(GregorianCalendar.MINUTE)
                    + "+" + cal.get(GregorianCalendar.SECOND);
    }

    /**
     * DOCUMENT ME!
     *
     * @param   productId   DOCUMENT ME!
     * @param   minX        DOCUMENT ME!
     * @param   minY        DOCUMENT ME!
     * @param   maxX        DOCUMENT ME!
     * @param   maxY        DOCUMENT ME!
     * @param   colorDepth  DOCUMENT ME!
     * @param   resolution  DOCUMENT ME!
     * @param   geoTiff     DOCUMENT ME!
     * @param   format      DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private String getRequestLine(final String productId,
            final double minX,
            final double minY,
            final double maxX,
            final double maxY,
            final int colorDepth,
            final String resolution,
            final boolean geoTiff,
            final String format) {
        //
        final StringBuffer buffer = new StringBuffer();
        // product id
        buffer.append(productId);
        buffer.append(SEPERATOR);
//        if (boundingBox instanceof Recta) {
//        }
        // coordinates
        buffer.append(minX);
        buffer.append(SEPERATOR);
        buffer.append(minY);
        buffer.append(SEPERATOR);
        buffer.append(maxX);
        buffer.append(SEPERATOR);
        buffer.append(maxY);
        buffer.append(SEPERATOR);

        // colordepth
        buffer.append(colorDepth);
        buffer.append(SEPERATOR);

        // resolution
        buffer.append(resolution);
        buffer.append(SEPERATOR);

        // geotif
        if (geoTiff) {
            buffer.append("yes");
        } else {
            buffer.append("no");
        }
        buffer.append(SEPERATOR);

        // format
        buffer.append(format);
        return buffer.toString();
    }

    /**
     * DOCUMENT ME!
     *
     * @param  args  DOCUMENT ME!
     */
    public static void main(final String[] args) {
        final ButlerProductGenerator generator = ButlerProductGenerator.getInstance();
        final User user = new User(100, "admin", "wunda_blau");
        final String productId = "0404";
        final double minX = 370000d;
        final double minY = 5680000d;
        final double maxX = 370300d;
        final double maxY = 5680300d;
        final int colorDepth = 8;
        final String resolution = "0.50";
        final boolean useGeoTif = true;
        final String format = "tif";
        final ArrayList<byte[]> result = generator.createButlerRequest(
            user,
            productId,
            minX,
            minY,
            maxX,
            maxY,
            colorDepth,
            resolution,
            useGeoTif,
            format);
        
    }

    /**
     * DOCUMENT ME!
     *
     * @param   fileName  DOCUMENT ME!
     * @param   geoTiff   DOCUMENT ME!
     * @param   format    DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private ArrayList<byte[]> getResultFiles(final String fileName, final boolean geoTiff, final String format) {
        final ArrayList<byte[]> result = new ArrayList<byte[]>();
        File resultDir;
        if (format.equals("dxf")) {
            resultDir = new File(resultBaseFolder + System.getProperty("file.separator") + dxfResultDir);
        } else if (format.equals("shp")) {
            resultDir = new File(resultBaseFolder + System.getProperty("file.separator") + shapeResultDir);
        } else if (format.equals("tif")) {
            resultDir = new File(resultBaseFolder + System.getProperty("file.separator") + tifResultDir);
        } else {
            // this must be true here: format.equals("pdf")
            resultDir = new File(resultBaseFolder + System.getProperty("file.separator") + pdfResultDir);
        }

        // get a list of files with the respective fileName and read them all
        final String regex = (fileName + ".*").replaceAll("\\+", "\\\\\\+");
//        final Pattern p = Pattern.compile(regex);
//        p.quote("+");
        final File[] resultFiles = resultDir.listFiles(new FileFilter() {

                    @Override
                    public boolean accept(final File file) {
                        return file.getName().matches(regex);
//                        retursn p.matcher(file.getName()).matches();
                    }
                });
        // we assume that there is at least one file
        if ((resultFiles == null) || (resultFiles.length <= 0)) {
            LOG.error("could not find the result file for butler order " + fileName);
        }
        for (int i = 0; i < resultFiles.length; i++) {
            result.add(loadFile(resultFiles[i]));
        }

        return result;
    }

    /**
     * DOCUMENT ME!
     *
     * @param   f  userKey DOCUMENT ME!
     *
     * @return  DOCUMENT ME!
     */
    private byte[] loadFile(final File f) {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        InputStream is = null;
        try {
            is = new FileInputStream(f);
            final byte[] buffer = new byte[8192];
            int length = is.read(buffer, 0, 8192);
            while (length != -1) {
                bos.write(buffer, 0, length);
                length = is.read(buffer, 0, 8192);
            }
            return bos.toByteArray();
        } catch (FileNotFoundException ex) {
            LOG.error("could not find result file " + f.getName());
        } catch (IOException ex) {
            LOG.error("error during loading file " + f.getName());
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
                bos.close();
            } catch (IOException ex) {
            }
        }
        return null;
    }
}
