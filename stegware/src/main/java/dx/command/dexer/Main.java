/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dx.command.dexer;

import dx.Version;
import dx.cf.code.SimException;
import dx.cf.direct.ClassPathOpener;
import dx.cf.iface.ParseException;
import dx.command.DxConsole;
import dx.command.UsageException;
import dx.dex.DexFormat;
import dx.dex.DexOptions;
import dx.dex.cf.CfOptions;
import dx.dex.cf.CfTranslator;
import dx.dex.cf.CodeStatistics;
import dx.dex.code.PositionList;
import dx.dex.file.ClassDefItem;
import dx.dex.file.DexFile;
import dx.dex.file.EncodedMethod;
import dx.io.DexBuffer;
import dx.merge.CollisionPolicy;
import dx.merge.DexMerger;
import dx.rop.annotation.Annotation;
import dx.rop.annotation.Annotations;
import dx.rop.annotation.AnnotationsList;
import dx.rop.cst.CstNat;
import dx.rop.cst.CstString;
import dx.util.FileUtils;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

/**
 * Main class for the class file translator.
 */
public class Main {
    /**
     * {@code non-null;} the lengthy message that tries to discourage
     * people from defining core classes in applications
     */
    private static final String IN_RE_CORE_CLASSES =
        "Ill-advised or mistaken usage of a core class (java.* or javax.*)\n" +
        "when not building a core library.\n\n" +
        "This is often due to inadvertently including a core library file\n" +
        "in your application's project, when using an IDE (such as\n" +
        "Eclipse). If you are sure you're not intentionally defining a\n" +
        "core class, then this is the most likely explanation of what's\n" +
        "going on.\n\n" +
        "However, you might actually be trying to define a class in a core\n" +
        "namespace, the source of which you may have taken, for example,\n" +
        "from a non-Android virtual machine project. This will most\n" +
        "assuredly not work. At a minimum, it jeopardizes the\n" +
        "compatibility of your app with future versions of the platform.\n" +
        "It is also often of questionable legality.\n\n" +
        "If you really intend to build a core library -- which is only\n" +
        "appropriate as part of creating a full virtual machine\n" +
        "distribution, as opposed to compiling an application -- then use\n" +
        "the \"--core-library\" option to suppress this error message.\n\n" +
        "If you go ahead and use \"--core-library\" but are in fact\n" +
        "building an application, then be forewarned that your application\n" +
        "will still fail to build or run, at some point. Please be\n" +
        "prepared for angry customers who find, for example, that your\n" +
        "application ceases to function once they upgrade their operating\n" +
        "system. You will be to blame for this problem.\n\n" +
        "If you are legitimately using some code that happens to be in a\n" +
        "core package, then the easiest safe alternative you have is to\n" +
        "repackage that code. That is, move the classes in question into\n" +
        "your own package namespace. This means that they will never be in\n" +
        "conflict with core system classes. JarJar is a tool that may help\n" +
        "you in this endeavor. If you find that you cannot do this, then\n" +
        "that is an indication that the path you are on will ultimately\n" +
        "lead to pain, suffering, grief, and lamentation.\n";

    /**
     * {@code non-null;} name of the standard manifest file in {@code .jar}
     * files
     */
    private static final String MANIFEST_NAME = "META-INF/MANIFEST.MF";

    /**
     * {@code non-null;} attribute name for the (quasi-standard?)
     * {@code Created-By} attribute
     */
    private static final Attributes.Name CREATED_BY =
        new Attributes.Name("Created-By");

    /**
     * {@code non-null;} list of {@code javax} subpackages that are considered
     * to be "core". <b>Note:</b>: This list must be sorted, since it
     * is binary-searched.
     */
    private static final String[] JAVAX_CORE = {
        "accessibility", "crypto", "imageio", "management", "naming", "net",
        "print", "rmi", "security", "sip", "sound", "sql", "swing",
        "transaction", "xml"
    };

    /** number of warnings during processing */
    private static int warnings = 0;

    /** number of errors during processing */
    private static int errors = 0;

    /** {@code non-null;} parsed command-line arguments */
    private static Arguments args;

    /** {@code non-null;} output file in-progress */
    private static DexFile outputDex;

    /**
     * {@code null-ok;} map of resources to include in the output, or
     * {@code null} if resources are being ignored
     */
    private static TreeMap<String, byte[]> outputResources;

    /** Library .dex files to merge into the output .dex. */
    private static final List<byte[]> libraryDexBuffers = new ArrayList<byte[]>();

    /** thread pool object used for multi-threaded file processing */
    private static ExecutorService threadPool;

    /** true if any files are successfully processed */
    private static boolean anyFilesProcessed;

    /** class files older than this must be defined in the target dex file. */
    private static long minimumFileAge = 0;

    /**
     * This class is uninstantiable.
     */
    private Main() {
        // This space intentionally left blank.
    }

    /**
     * Run and exit if something unexpected happened.
     * @param argArray the command line arguments
     */
    public static void main(String[] argArray) throws IOException {
        Arguments arguments = new Arguments();
        arguments.parse(argArray);

        int result = run(arguments);
        if (result != 0) {
            System.exit(result);
        }
    }

    /**
     * Run and return a result code.
     * @param arguments the data + parameters for the conversion
     * @return 0 if success > 0 otherwise.
     */
    public static int run(Arguments arguments) throws IOException {
        // Reset the error/warning count to start fresh.
        warnings = 0;
        errors = 0;
        // empty the list, so that  tools that load dx and keep it around
        // for multiple runs don't reuse older buffers.
        libraryDexBuffers.clear();

        args = arguments;
        args.makeOptionsObjects();

        File incrementalOutFile = null;
        if (args.incremental) {
            if (args.outName == null) {
                System.err.println(
                        "error: no incremental output name specified");
                return -1;
            }
            incrementalOutFile = new File(args.outName);
            if (incrementalOutFile.exists()) {
                minimumFileAge = incrementalOutFile.lastModified();
            }
        }

        if (!processAllFiles()) {
            return 1;
        }

        if (args.incremental && !anyFilesProcessed) {
            return 0; // this was a no-op incremental build
        }

        // this array is null if no classes were defined
        byte[] outArray = null;

        if (!outputDex.isEmpty()) {
            outArray = writeDex();

            if (outArray == null) {
                return 2;
            }
        }

        if (args.incremental) {
            outArray = mergeIncremental(outArray, incrementalOutFile);
        }

        outArray = mergeLibraryDexBuffers(outArray);

        if (args.jarOutput) {
            // Effectively free up the (often massive) DexFile memory.
            outputDex = null;

            if (!createJar(args.outName, outArray)) {
                return 3;
            }
        } else if (outArray != null && args.outName != null) {
            OutputStream out = openOutput(args.outName);
            out.write(outArray);
            closeOutput(out);
        }

        return 0;
    }

    /**
     * Merges the dex files {@code update} and {@code base}, preferring
     * {@code update}'s definition for types defined in both dex files.
     *
     * @param base a file to find the previous dex file. May be a .dex file, a
     *     jar file possibly containing a .dex file, or null.
     * @return the bytes of the merged dex file, or null if both the update
     *     and the base dex do not exist.
     */
    private static byte[] mergeIncremental(byte[] update, File base) throws IOException {
        DexBuffer dexA = null;
        DexBuffer dexB = null;

        if (update != null) {
            dexA = new DexBuffer(update);
        }

        if (base.exists()) {
            dexB = new DexBuffer(base);
        }

        DexBuffer result;
        if (dexA == null && dexB == null) {
            return null;
        } else if (dexA == null) {
            result = dexB;
        } else if (dexB == null) {
            result = dexA;
        } else {
            result = new DexMerger(dexA, dexB, CollisionPolicy.KEEP_FIRST).merge();
        }

        ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
        result.writeTo(bytesOut);
        return bytesOut.toByteArray();
    }

    /**
     * Merges the dex files in library jars. If multiple dex files define the
     * same type, this fails with an exception.
     */
    private static byte[] mergeLibraryDexBuffers(byte[] outArray) throws IOException {
        for (byte[] libraryDexBuffer : libraryDexBuffers) {
            if (outArray == null) {
                outArray = libraryDexBuffer;
                continue;
            }

            DexBuffer a = new DexBuffer(outArray);
            DexBuffer b = new DexBuffer(libraryDexBuffer);
            DexBuffer ab = new DexMerger(a, b, CollisionPolicy.FAIL).merge();
            outArray = ab.getBytes();
        }

        return outArray;
    }

    /**
     * Constructs the output {@link DexFile}, fill it in with all the
     * specified classes, and populate the resources map if required.
     *
     * @return whether processing was successful
     */
    private static boolean processAllFiles() {
        outputDex = new DexFile(args.dexOptions);

        if (args.jarOutput) {
            outputResources = new TreeMap<String, byte[]>();
        }

        if (args.dumpWidth != 0) {
            outputDex.setDumpWidth(args.dumpWidth);
        }

        anyFilesProcessed = false;
        String[] fileNames = args.fileNames;

        if (args.numThreads > 1) {
            threadPool = Executors.newFixedThreadPool(args.numThreads);
        }

        try {
            for (int i = 0; i < fileNames.length; i++) {
                if (processOne(fileNames[i])) {
                    anyFilesProcessed = true;
                }
            }
        } catch (StopProcessing ex) {
            /*
             * Ignore it and just let the warning/error reporting do
             * their things.
             */
        }

        if (args.numThreads > 1) {
            try {
                threadPool.shutdown();
                threadPool.awaitTermination(600L, TimeUnit.SECONDS);
            } catch (InterruptedException ex) {
                throw new RuntimeException("Timed out waiting for threads.");
            }
        }

        if (warnings != 0) {
            DxConsole.err.println(warnings + " warning" +
                               ((warnings == 1) ? "" : "s"));
        }

        if (errors != 0) {
            DxConsole.err.println(errors + " error" +
                    ((errors == 1) ? "" : "s") + "; aborting");
            return false;
        }

        if (args.incremental && !anyFilesProcessed) {
            return true;
        }

        if (!(anyFilesProcessed || args.emptyOk)) {
            DxConsole.err.println("no classfiles specified");
            return false;
        }

        if (args.optimize && args.statistics) {
            CodeStatistics.dumpStatistics(DxConsole.out);
        }

        return true;
    }

    /**
     * Processes one pathname element.
     *
     * @param pathname {@code non-null;} the pathname to process. May
     * be the path of a class file, a jar file, or a directory
     * containing class files.
     * @return whether any processing actually happened
     */
    private static boolean processOne(String pathname) {
        ClassPathOpener opener;

        opener = new ClassPathOpener(pathname, false,
                new ClassPathOpener.Consumer() {
            public boolean processFileBytes(String name, long lastModified, byte[] bytes) {
                if (args.numThreads > 1) {
                    threadPool.execute(new ParallelProcessor(name, lastModified, bytes));
                    return false;
                } else {
                    return Main.processFileBytes(name, lastModified, bytes);
                }
            }
            public void onException(Exception ex) {
                if (ex instanceof StopProcessing) {
                    throw (StopProcessing) ex;
                } else if (ex instanceof SimException) {
                    DxConsole.err.println("\nEXCEPTION FROM SIMULATION:");
                    DxConsole.err.println(ex.getMessage() + "\n");
                    DxConsole.err.println(((SimException) ex).getContext());
                } else {
                    DxConsole.err.println("\nUNEXPECTED TOP-LEVEL EXCEPTION:");
                    ex.printStackTrace(DxConsole.err);
                }
                errors++;
            }
            public void onProcessArchiveStart(File file) {
                if (args.verbose) {
                    DxConsole.out.println("processing archive " + file +
                            "...");
                }
            }
        });

        return opener.process();
    }

    /**
     * Processes one file, which may be either a class or a resource.
     *
     * @param name {@code non-null;} name of the file
     * @param bytes {@code non-null;} contents of the file
     * @return whether processing was successful
     */
    private static boolean processFileBytes(String name, long lastModified, byte[] bytes) {
        boolean isClass = name.endsWith(".class");
        boolean isClassesDex = name.equals(DexFormat.DEX_IN_JAR_NAME);
        boolean keepResources = (outputResources != null);

        if (!isClass && !isClassesDex && !keepResources) {
            if (args.verbose) {
                DxConsole.out.println("ignored resource " + name);
            }
            return false;
        }

        if (args.verbose) {
            DxConsole.out.println("processing " + name + "...");
        }

        String fixedName = fixPath(name);

        if (isClass) {
            if (keepResources && args.keepClassesInJar) {
                synchronized (outputResources) {
                    outputResources.put(fixedName, bytes);
                }
            }
            if (lastModified < minimumFileAge) {
                return true;
            }
            return processClass(fixedName, bytes);
        } else if (isClassesDex) {
            synchronized (libraryDexBuffers) {
                libraryDexBuffers.add(bytes);
            }
            return true;
        } else {
            synchronized (outputResources) {
                outputResources.put(fixedName, bytes);
            }
            return true;
        }
    }

    /**
     * Processes one classfile.
     *
     * @param name {@code non-null;} name of the file, clipped such that it
     * <i>should</i> correspond to the name of the class it contains
     * @param bytes {@code non-null;} contents of the file
     * @return whether processing was successful
     */
    private static boolean processClass(String name, byte[] bytes) {
        if (! args.coreLibrary) {
            checkClassName(name);
        }

        try {
            ClassDefItem clazz =
                CfTranslator.translate(name, bytes, args.cfOptions, args.dexOptions);
            synchronized (outputDex) {
                outputDex.add(clazz);
            }
            return true;
        } catch (ParseException ex) {
            DxConsole.err.println("\ntrouble processing:");
            if (args.debug) {
                ex.printStackTrace(DxConsole.err);
            } else {
                ex.printContext(DxConsole.err);
            }
        }

        warnings++;
        return false;
    }

    /**
     * Check the class name to make sure it's not a "core library"
     * class. If there is a problem, this updates the error count and
     * throws an exception to stop processing.
     *
     * @param name {@code non-null;} the fully-qualified internal-form
     * class name
     */
    private static void checkClassName(String name) {
        boolean bogus = false;

        if (name.startsWith("java/")) {
            bogus = true;
        } else if (name.startsWith("javax/")) {
            int slashAt = name.indexOf('/', 6);
            if (slashAt == -1) {
                // Top-level javax classes are verboten.
                bogus = true;
            } else {
                String pkg = name.substring(6, slashAt);
                bogus = (Arrays.binarySearch(JAVAX_CORE, pkg) >= 0);
            }
        }

        if (! bogus) {
            return;
        }

        /*
         * The user is probably trying to include an entire desktop
         * core library in a misguided attempt to get their application
         * working. Try to help them understand what's happening.
         */

        DxConsole.err.println("\ntrouble processing \"" + name + "\":\n\n" +
                IN_RE_CORE_CLASSES);
        errors++;
        throw new StopProcessing();
    }

    /**
     * Converts {@link #outputDex} into a {@code byte[]} and do whatever
     * human-oriented dumping is required.
     *
     * @return {@code null-ok;} the converted {@code byte[]} or {@code null}
     * if there was a problem
     */
    private static byte[] writeDex() {
        byte[] outArray = null;

        try {
            OutputStream humanOutRaw = null;
            OutputStreamWriter humanOut = null;
            try {
                if (args.humanOutName != null) {
                    humanOutRaw = openOutput(args.humanOutName);
                    humanOut = new OutputStreamWriter(humanOutRaw);
                }

                if (args.methodToDump != null) {
                    /*
                     * Simply dump the requested method. Note: The call
                     * to toDex() is required just to get the underlying
                     * structures ready.
                     */
                    outputDex.toDex(null, false);
                    dumpMethod(outputDex, args.methodToDump, humanOut);
                } else {
                    /*
                     * This is the usual case: Create an output .dex file,
                     * and write it, dump it, etc.
                     */
                    outArray = outputDex.toDex(humanOut, args.verboseDump);
                }

                if (args.statistics) {
                    DxConsole.out.println(outputDex.getStatistics().toHuman());
                }
            } finally {
                if (humanOut != null) {
                    humanOut.flush();
                }
                closeOutput(humanOutRaw);
            }
        } catch (Exception ex) {
            if (args.debug) {
                DxConsole.err.println("\ntrouble writing output:");
                ex.printStackTrace(DxConsole.err);
            } else {
                DxConsole.err.println("\ntrouble writing output: " +
                                   ex.getMessage());
            }
            return null;
        }

        return outArray;
    }

    /**
     * Creates a jar file from the resources and given dex file array.
     *
     * @param fileName {@code non-null;} name of the file
     * @param dexArray array containing the dex file to include, or null if the
     *     output contains no class defs.
     * @return whether the creation was successful
     */
    private static boolean createJar(String fileName, byte[] dexArray) {
        /*
         * Make or modify the manifest (as appropriate), put the dex
         * array into the resources map, and then process the entire
         * resources map in a uniform manner.
         */

        try {
            Manifest manifest = makeManifest();
            OutputStream out = openOutput(fileName);
            JarOutputStream jarOut = new JarOutputStream(out, manifest);

            if (dexArray != null) {
                outputResources.put(DexFormat.DEX_IN_JAR_NAME, dexArray);
            }

            try {
                for (Map.Entry<String, byte[]> e :
                         outputResources.entrySet()) {
                    String name = e.getKey();
                    byte[] contents = e.getValue();
                    JarEntry entry = new JarEntry(name);

                    if (args.verbose) {
                        DxConsole.out.println("writing " + name + "; size " +
                                           contents.length + "...");
                    }

                    entry.setSize(contents.length);
                    jarOut.putNextEntry(entry);
                    jarOut.write(contents);
                    jarOut.closeEntry();
                }
            } finally {
                jarOut.finish();
                jarOut.flush();
                closeOutput(out);
            }
        } catch (Exception ex) {
            if (args.debug) {
                DxConsole.err.println("\ntrouble writing output:");
                ex.printStackTrace(DxConsole.err);
            } else {
                DxConsole.err.println("\ntrouble writing output: " +
                                   ex.getMessage());
            }
            return false;
        }

        return true;
    }

    /**
     * Creates and returns the manifest to use for the output. This may
     * modify {@link #outputResources} (removing the pre-existing manifest).
     *
     * @return {@code non-null;} the manifest
     */
    private static Manifest makeManifest() throws IOException {
        byte[] manifestBytes = outputResources.get(MANIFEST_NAME);
        Manifest manifest;
        Attributes attribs;

        if (manifestBytes == null) {
            // We need to construct an entirely new manifest.
            manifest = new Manifest();
            attribs = manifest.getMainAttributes();
            attribs.put(Attributes.Name.MANIFEST_VERSION, "1.0");
        } else {
            manifest = new Manifest(new ByteArrayInputStream(manifestBytes));
            attribs = manifest.getMainAttributes();
            outputResources.remove(MANIFEST_NAME);
        }

        String createdBy = attribs.getValue(CREATED_BY);
        if (createdBy == null) {
            createdBy = "";
        } else {
            createdBy += " + ";
        }
        createdBy += "dx " + Version.VERSION;

        attribs.put(CREATED_BY, createdBy);
        attribs.putValue("Dex-Location", DexFormat.DEX_IN_JAR_NAME);

        return manifest;
    }

    /**
     * Opens and returns the named file for writing, treating "-" specially.
     *
     * @param name {@code non-null;} the file name
     * @return {@code non-null;} the opened file
     */
    private static OutputStream openOutput(String name) throws IOException {
        if (name.equals("-") ||
                name.startsWith("-.")) {
            return System.out;
        }

        return new FileOutputStream(name);
    }

    /**
     * Flushes and closes the given output stream, except if it happens to be
     * {@link System#out} in which case this method does the flush but not
     * the close. This method will also silently do nothing if given a
     * {@code null} argument.
     *
     * @param stream {@code null-ok;} what to close
     */
    private static void closeOutput(OutputStream stream) throws IOException {
        if (stream == null) {
            return;
        }

        stream.flush();

        if (stream != System.out) {
            stream.close();
        }
    }

    /**
     * Returns the "fixed" version of a given file path, suitable for
     * use as a path within a {@code .jar} file and for checking
     * against a classfile-internal "this class" name. This looks for
     * the last instance of the substring {@code "/./"} within
     * the path, and if it finds it, it takes the portion after to be
     * the fixed path. If that isn't found but the path starts with
     * {@code "./"}, then that prefix is removed and the rest is
     * return. If neither of these is the case, this method returns
     * its argument.
     *
     * @param path {@code non-null;} the path to "fix"
     * @return {@code non-null;} the fixed version (which might be the same as
     * the given {@code path})
     */
    private static String fixPath(String path) {
        /*
         * If the path separator is \ (like on windows), we convert the
         * path to a standard '/' separated path.
         */
        if (File.separatorChar == '\\') {
            path = path.replace('\\', '/');
        }

        int index = path.lastIndexOf("/./");

        if (index != -1) {
            return path.substring(index + 3);
        }

        if (path.startsWith("./")) {
            return path.substring(2);
        }

        return path;
    }

    /**
     * Dumps any method with the given name in the given file.
     *
     * @param dex {@code non-null;} the dex file
     * @param fqName {@code non-null;} the fully-qualified name of the
     * method(s)
     * @param out {@code non-null;} where to dump to
     */
    private static void dumpMethod(DexFile dex, String fqName,
            OutputStreamWriter out) {
        boolean wildcard = fqName.endsWith("*");
        int lastDot = fqName.lastIndexOf('.');

        if ((lastDot <= 0) || (lastDot == (fqName.length() - 1))) {
            DxConsole.err.println("bogus fully-qualified method name: " +
                               fqName);
            return;
        }

        String className = fqName.substring(0, lastDot).replace('.', '/');
        String methodName = fqName.substring(lastDot + 1);
        ClassDefItem clazz = dex.getClassOrNull(className);

        if (clazz == null) {
            DxConsole.err.println("no such class: " + className);
            return;
        }

        if (wildcard) {
            methodName = methodName.substring(0, methodName.length() - 1);
        }

        ArrayList<EncodedMethod> allMeths = clazz.getMethods();
        TreeMap<CstNat, EncodedMethod> meths =
            new TreeMap<CstNat, EncodedMethod>();

        /*
         * Figure out which methods to include in the output, and get them
         * all sorted, so that the printout code is robust with respect to
         * changes in the underlying order.
         */
        for (EncodedMethod meth : allMeths) {
            String methName = meth.getName().getString();
            if ((wildcard && methName.startsWith(methodName)) ||
                (!wildcard && methName.equals(methodName))) {
                meths.put(meth.getRef().getNat(), meth);
            }
        }

        if (meths.size() == 0) {
            DxConsole.err.println("no such method: " + fqName);
            return;
        }

        PrintWriter pw = new PrintWriter(out);

        for (EncodedMethod meth : meths.values()) {
            // TODO: Better stuff goes here, perhaps.
            meth.debugPrint(pw, args.verboseDump);

            /*
             * The (default) source file is an attribute of the class, but
             * it's useful to see it in method dumps.
             */
            CstString sourceFile = clazz.getSourceFile();
            if (sourceFile != null) {
                pw.println("  source file: " + sourceFile.toQuoted());
            }

            Annotations methodAnnotations =
                clazz.getMethodAnnotations(meth.getRef());
            AnnotationsList parameterAnnotations =
                clazz.getParameterAnnotations(meth.getRef());

            if (methodAnnotations != null) {
                pw.println("  method annotations:");
                for (Annotation a : methodAnnotations.getAnnotations()) {
                    pw.println("    " + a);
                }
            }

            if (parameterAnnotations != null) {
                pw.println("  parameter annotations:");
                int sz = parameterAnnotations.size();
                for (int i = 0; i < sz; i++) {
                    pw.println("    parameter " + i);
                    Annotations annotations = parameterAnnotations.get(i);
                    for (Annotation a : annotations.getAnnotations()) {
                        pw.println("      " + a);
                    }
                }
            }
        }

        pw.flush();
    }

    /**
     * Exception class used to halt processing prematurely.
     */
    private static class StopProcessing extends RuntimeException {
        // This space intentionally left blank.
    }

    /**
     * Command-line argument parser and access.
     */
    public static class Arguments {
        /** whether to run in debug mode */
        public boolean debug = false;

        /** whether to emit high-level verbose human-oriented output */
        public boolean verbose = false;

        /** whether to emit verbose human-oriented output in the dump file */
        public boolean verboseDump = false;

        /** whether we are constructing a core library */
        public boolean coreLibrary = false;

        /** {@code null-ok;} particular method to dump */
        public String methodToDump = null;

        /** max width for columnar output */
        public int dumpWidth = 0;

        /** {@code null-ok;} output file name for binary file */
        public String outName = null;

        /** {@code null-ok;} output file name for human-oriented dump */
        public String humanOutName = null;

        /** whether strict file-name-vs-class-name checking should be done */
        public boolean strictNameCheck = true;

        /**
         * whether it is okay for there to be no {@code .class} files
         * to process
         */
        public boolean emptyOk = false;

        /**
         * whether the binary output is to be a {@code .jar} file
         * instead of a plain {@code .dex}
         */
        public boolean jarOutput = false;

        /**
         * when writing a {@code .jar} file, whether to still
         * keep the {@code .class} files
         */
        public boolean keepClassesInJar = false;

        /** what API level to target */
        public int targetApiLevel = DexFormat.API_NO_EXTENDED_OPCODES;

        /** how much source position info to preserve */
        public int positionInfo = PositionList.LINES;

        /** whether to keep local variable information */
        public boolean localInfo = true;

        /** whether to merge with the output dex file if it exists. */
        public boolean incremental = false;

        /** {@code non-null} after {@link #parse}; file name arguments */
        public String[] fileNames;

        /** whether to do SSA/register optimization */
        public boolean optimize = true;

        /** Filename containg list of methods to optimize */
        public String optimizeListFile = null;

        /** Filename containing list of methods to NOT optimize */
        public String dontOptimizeListFile = null;

        /** Whether to print statistics to stdout at end of compile cycle */
        public boolean statistics;

        /** Options for class file transformation */
        public CfOptions cfOptions;

        /** Options for dex file output */
        public DexOptions dexOptions;

        /** number of threads to run with */
        public int numThreads = 1;

        private static class ArgumentsParser {

            /** The arguments to process. */
            private final String[] arguments;
            /** The index of the next argument to process. */
            private int index;
            /** The current argument being processed after a {@link #getNext()} call. */
            private String current;
            /** The last value of an argument processed by {@link #isArg(String)}. */
            private String lastValue;

            public ArgumentsParser(String[] arguments) {
                this.arguments = arguments;
                index = 0;
            }

            public String getCurrent() {
                return current;
            }

            public String getLastValue() {
                return lastValue;
            }

            /**
             * Moves on to the next argument.
             * Returns false when we ran out of arguments that start with --.
             */
            public boolean getNext() {
                if (index >= arguments.length) {
                    return false;
                }
                current = arguments[index];
                if (current.equals("--") || !current.startsWith("--")) {
                    return false;
                }
                index++;
                return true;
            }

            /**
             * Similar to {@link #getNext()}, this moves on the to next argument.
             * It does not check however whether the argument starts with --
             * and thus can be used to retrieve values.
             */
            private boolean getNextValue() {
                if (index >= arguments.length) {
                    return false;
                }
                current = arguments[index];
                index++;
                return true;
            }

            /**
             * Returns all the arguments that have not been processed yet.
             */
            public String[] getRemaining() {
                int n = arguments.length - index;
                String[] remaining = new String[n];
                if (n > 0) {
                    System.arraycopy(arguments, index, remaining, 0, n);
                }
                return remaining;
            }

            /**
             * Checks the current argument against the given prefix.
             * If prefix is in the form '--name=', an extra value is expected.
             * The argument can then be in the form '--name=value' or as a 2-argument
             * form '--name value'.
             */
            public boolean isArg(String prefix) {
                int n = prefix.length();
                if (n > 0 && prefix.charAt(n-1) == '=') {
                    // Argument accepts a value. Capture it.
                    if (current.startsWith(prefix)) {
                        // Argument is in the form --name=value, split the value out
                        lastValue = current.substring(n);
                        return true;
                    } else {
                        // Check whether we have "--name value" as 2 arguments
                        prefix = prefix.substring(0, n-1);
                        if (current.equals(prefix)) {
                            if (getNextValue()) {
                                lastValue = current;
                                return true;
                            } else {
                                System.err.println("Missing value after parameter " + prefix);
                                throw new UsageException();
                            }
                        }
                        return false;
                    }
                } else {
                    // Argument does not accept a value.
                    return current.equals(prefix);
                }
            }
        }

        /**
         * Parses the given command-line arguments.
         *
         * @param args {@code non-null;} the arguments
         */
        public void parse(String[] args) {
            ArgumentsParser parser = new ArgumentsParser(args);

            while(parser.getNext()) {
                if (parser.isArg("--debug")) {
                    debug = true;
                } else if (parser.isArg("--verbose")) {
                    verbose = true;
                } else if (parser.isArg("--verbose-dump")) {
                    verboseDump = true;
                } else if (parser.isArg("--no-files")) {
                    emptyOk = true;
                } else if (parser.isArg("--no-optimize")) {
                    optimize = false;
                } else if (parser.isArg("--no-strict")) {
                    strictNameCheck = false;
                } else if (parser.isArg("--core-library")) {
                    coreLibrary = true;
                } else if (parser.isArg("--statistics")) {
                    statistics = true;
                } else if (parser.isArg("--optimize-list=")) {
                    if (dontOptimizeListFile != null) {
                        System.err.println("--optimize-list and "
                                + "--no-optimize-list are incompatible.");
                        throw new UsageException();
                    }
                    optimize = true;
                    optimizeListFile = parser.getLastValue();
                } else if (parser.isArg("--no-optimize-list=")) {
                    if (dontOptimizeListFile != null) {
                        System.err.println("--optimize-list and "
                                + "--no-optimize-list are incompatible.");
                        throw new UsageException();
                    }
                    optimize = true;
                    dontOptimizeListFile = parser.getLastValue();
                } else if (parser.isArg("--keep-classes")) {
                    keepClassesInJar = true;
                } else if (parser.isArg("--output=")) {
                    outName = parser.getLastValue();
                    if (FileUtils.hasArchiveSuffix(outName)) {
                        jarOutput = true;
                    } else if (outName.endsWith(".dex") ||
                               outName.equals("-")) {
                        jarOutput = false;
                    } else {
                        System.err.println("unknown output extension: " +
                                           outName);
                        throw new UsageException();
                    }
                } else if (parser.isArg("--dump-to=")) {
                    humanOutName = parser.getLastValue();
                } else if (parser.isArg("--dump-width=")) {
                    dumpWidth = Integer.parseInt(parser.getLastValue());
                } else if (parser.isArg("--dump-method=")) {
                    methodToDump = parser.getLastValue();
                    jarOutput = false;
                } else if (parser.isArg("--positions=")) {
                    String pstr = parser.getLastValue().intern();
                    if (pstr == "none") {
                        positionInfo = PositionList.NONE;
                    } else if (pstr == "important") {
                        positionInfo = PositionList.IMPORTANT;
                    } else if (pstr == "lines") {
                        positionInfo = PositionList.LINES;
                    } else {
                        System.err.println("unknown positions option: " +
                                           pstr);
                        throw new UsageException();
                    }
                } else if (parser.isArg("--no-locals")) {
                    localInfo = false;
                } else if (parser.isArg("--num-threads=")) {
                    numThreads = Integer.parseInt(parser.getLastValue());
                } else if (parser.isArg("--incremental")) {
                    incremental = true;
                } else {
                    System.err.println("unknown option: " + parser.getCurrent());
                    throw new UsageException();
                }
            }

            fileNames = parser.getRemaining();
            if (fileNames.length == 0) {
                if (!emptyOk) {
                    System.err.println("no input files specified");
                    throw new UsageException();
                }
            } else if (emptyOk) {
                System.out.println("ignoring input files");
            }

            if ((humanOutName == null) && (methodToDump != null)) {
                humanOutName = "-";
            }

            makeOptionsObjects();
        }

        /**
         * Copies relevent arguments over into CfOptions and
         * DexOptions instances.
         */
        private void makeOptionsObjects() {
            cfOptions = new CfOptions();
            cfOptions.positionInfo = positionInfo;
            cfOptions.localInfo = localInfo;
            cfOptions.strictNameCheck = strictNameCheck;
            cfOptions.optimize = optimize;
            cfOptions.optimizeListFile = optimizeListFile;
            cfOptions.dontOptimizeListFile = dontOptimizeListFile;
            cfOptions.statistics = statistics;
            cfOptions.warn = DxConsole.err;

            dexOptions = new DexOptions();
            dexOptions.targetApiLevel = targetApiLevel;
        }
    }

    /** Runnable helper class to process files in multiple threads */
    private static class ParallelProcessor implements Runnable {

        String path;
        long lastModified;
        byte[] bytes;

        /**
         * Constructs an instance.
         *
         * @param path {@code non-null;} filename of element. May not be a valid
         * filesystem path.
         * @param bytes {@code non-null;} file data
         */
        private ParallelProcessor(String path, long lastModified, byte bytes[]) {
            this.path = path;
            this.lastModified = lastModified;
            this.bytes = bytes;
        }

        /**
         * Task run by each thread in the thread pool. Runs processFileBytes
         * with the given path and bytes.
         */
        public void run() {
            if (Main.processFileBytes(path, lastModified, bytes)) {
                anyFilesProcessed = true;
            }
        }
    }
}
