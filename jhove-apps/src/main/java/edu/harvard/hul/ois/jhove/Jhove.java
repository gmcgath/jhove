/**********************************************************************
 * Jhove - JSTOR/Harvard Object Validation Environment
 * Copyright 2004-2007 by the President and Fellows of Harvard College
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or (at
 * your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 * USA
 **********************************************************************/
package edu.harvard.hul.ois.jhove;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Jhove {
    /** Application name. */
    private static final String NAME = "Jhove";
    /** Logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(Jhove.class.getCanonicalName());

    private static final String C_CONFIG_OPTION = "-c";
    private static final String X_CONFIG_OPTION = "-x";
    private static final String NOT_FOUND = "not found";
    private static final String HANDLER = "Handler '";
    private static final String MODULE = "Module";

    private Jhove() {
        throw new AssertionError("Should never enter private constructor");
    }

    /**
     * MAIN ENTRY POINT.
     */
    public static void main(String[] args) {
        // Make sure we have a satisfactory version of Java.
        String version = System.getProperty("java.vm.version");
        if (version.compareTo("1.8.0") < 0) {
            LOGGER.log(Level.SEVERE, CoreMessageConstants.EXC_JAVA_VER_INCMPT);
            System.exit(ExitCode.INCOMPATIBLE_VM.getReturnCode());
        }

        try {

            // Initialize the application state object.
            App app = App.newAppWithName(NAME);

            // Retrieve configuration.
            String configFile = JhoveBase.getConfigFileFromProperties();
            String saxClass = JhoveBase.getSaxClassFromProperties();

            /*
             * Pre-parse the command line for -c and -x config options.
             * With Windows, we have to deal with quote marks on our own.
             * With Unix, the shell takes care of quotes for us.
             */
            boolean quoted = false;
            for (int i = 0; i < args.length; i++) {
                if (quoted) {
                    int len = args[i].length();
                    if (args[i].charAt(len - 1) == '"') {
                        quoted = false;
                    }
                } else {
                    if (C_CONFIG_OPTION.equals(args[i])) {
                        if (i < args.length - 1) {
                            configFile = args[++i];
                        }
                    } else if (X_CONFIG_OPTION.equals(args[i])) {
                        if (i < args.length - 1) {
                            saxClass = args[++i];
                        }
                    } else if (args[i].charAt(0) == '"') {
                        quoted = true;
                    }
                }
            }

            // Initialize the JHOVE engine.
            String encoding = null;
            String tempDir = null;
            int bufferSize = -1;
            String moduleName = null;
            String handlerName = null;
            String aboutHandler = null;
            String logLevel = null;
            String outputFile = null;
            boolean checksum = false;
            boolean showRaw = false;
            boolean signature = false;
            List<String> list = new ArrayList<>();

            /*
             * Parse command line arguments:
             * -m module Module name
             * -h handler Output handler
             * -e encoding Output encoding
             * -H handler About handler
             * -o output Output file pathname
             * -t tempdir Directory for temp files
             * -b bufsize Buffer size for buffered I/O
             * -k Calculate checksums
             * -r Display raw numeric flags
             * -s Check internal signatures only
             * dirFileOrUri Directories, file pathnames, or URIs
             *
             * The following arguments were defined in previous
             * versions, but are now obsolete
             * -p param OBSOLETE
             * -P param OBSOLETE
             */

            quoted = false;
            StringBuilder filename = null;

            for (int i = 0; i < args.length; i++) {
                if (quoted) {
                    filename.append(" ");
                    int len = args[i].length();
                    if (args[i].charAt(len - 1) == '"') {
                        filename.append(args[i].substring(0, len - 1));
                        list.add(filename.toString());
                        quoted = false;
                    } else {
                        filename.append(args[i]);
                    }
                } else {
                    if (C_CONFIG_OPTION.equals(args[i])) {
                        i++;
                    } else if ("-m".equals(args[i])) {
                        if (i < args.length - 1) {
                            moduleName = args[++i];
                        }
                    } else if ("-p".equals(args[i])) {
                        // Obsolete -- but eat the next arg for compatibility
                        if (i < args.length - 1) {
                            @SuppressWarnings("unused")
                            String moduleParam = args[++i];
                        }
                    } else if ("-h".equals(args[i])) {
                        if (i < args.length - 1) {
                            handlerName = args[++i];
                        }
                    } else if ("-P".equals(args[i])) {
                        // Obsolete -- but eat the next arg for compatibility
                        if (i < args.length - 1) {
                            @SuppressWarnings("unused")
                            String handlerParam = args[++i];
                        }
                    } else if ("-e".equals(args[i])) {
                        if (i < args.length - 1) {
                            encoding = args[++i];
                        }
                    } else if ("-H".equals(args[i])) {
                        if (i < args.length - 1) {
                            aboutHandler = args[++i];
                        }
                    } else if ("-l".equals(args[i])) {
                        if (i < args.length - 1) {
                            logLevel = args[++i];
                        }
                    } else if ("-o".equals(args[i])) {
                        if (i < args.length - 1) {
                            outputFile = args[++i];
                        }
                    } else if (X_CONFIG_OPTION.equals(args[i])) {
                        i++;
                    } else if ("-t".equals(args[i])) {
                        if (i < args.length - 1) {
                            tempDir = args[++i];
                        }
                    } else if ("-b".equals(args[i])) {
                        if (i < args.length - 1) {
                            try {
                                bufferSize = Integer.parseInt(args[++i]);
                            } catch (NumberFormatException nfe) {
                                LOGGER.log(Level.WARNING, "Invalid buffer size, using default.");
                            }
                        }
                    } else if ("-k".equals(args[i])) {
                        checksum = true;
                    } else if ("-r".equals(args[i])) {
                        showRaw = true;
                    } else if ("-s".equals(args[i])) {
                        signature = true;
                    } else if (args[i].charAt(0) != '-') {
                        if (args[i].charAt(0) == '"') {
                            filename = new StringBuilder();
                            filename.append(args[i].substring(1));
                            quoted = true;
                        } else {
                            list.add(args[i]);
                        }
                    }
                }
            }
            if (quoted) {
                list.add(filename.toString());
            }

            JhoveBase je = new JhoveBase();
            // Only set the log level if a param value was assigned
            if (logLevel != null) {
                je.setLogLevel(logLevel);
            }
            je.init(configFile, saxClass);
            if (encoding == null) {
                encoding = je.getEncoding();
            }
            if (tempDir == null) {
                tempDir = je.getTempDirectory();
            }
            if (bufferSize < 0) {
                bufferSize = je.getBufferSize();
            }
            Module module = je.getModule(moduleName);
            if (module == null && moduleName != null) {
                LOGGER.log(Level.SEVERE, notFoundMessage(MODULE, moduleName));
                System.exit(ExitCode.ERROR.getReturnCode());
            }
            OutputHandler about = je.getHandler(aboutHandler);
            if (about == null && aboutHandler != null) {
                LOGGER.log(Level.SEVERE, notFoundMessage(HANDLER, aboutHandler));
                System.exit(ExitCode.ERROR.getReturnCode());
            }
            OutputHandler handler = je.getHandler(handlerName);
            if (handler == null && handlerName != null) {
                LOGGER.log(Level.SEVERE, notFoundMessage(HANDLER, handlerName));
                System.exit(ExitCode.ERROR.getReturnCode());
            }
            String[] dirFileOrUri = null;
            int len = list.size();
            if (len > 0) {
                dirFileOrUri = new String[len];
                for (int i = 0; i < len; i++) {
                    dirFileOrUri[i] = list.get(i);
                }
            }

            // Invoke the JHOVE engine.
            je.setEncoding(encoding);
            je.setTempDirectory(tempDir);
            je.setBufferSize(bufferSize);
            je.setChecksumFlag(checksum);
            je.setShowRawFlag(showRaw);
            je.setSignatureFlag(signature);
            je.dispatch(app, module, about, handler, outputFile, dirFileOrUri);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
            e.printStackTrace(System.err);
            System.exit(ExitCode.ERROR.getReturnCode());
        }
    }

    private static final String notFoundMessage(final String notFoundType, final String notFoundName) {
        return String.format("%s '%s' %s", notFoundType, notFoundName, NOT_FOUND);
    }
}
