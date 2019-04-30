package com.stericson.rootshell.containers

import java.io.BufferedReader
import java.io.File
import java.io.FileFilter
import java.io.FileNotFoundException
import java.io.FileReader
import java.io.FilenameFilter
import java.io.IOException
import java.lang.reflect.Constructor
import java.lang.reflect.InvocationTargetException
import java.util.ArrayList
import java.util.regex.Matcher
import java.util.regex.Pattern

/* #ANNOTATIONS @SupportedAnnotationTypes("com.stericson.RootShell.containers.RootClass.Candidate") */
/* #ANNOTATIONS @SupportedSourceVersion(SourceVersion.RELEASE_6) */
class RootClass /* #ANNOTATIONS extends AbstractProcessor */ @Throws(ClassNotFoundException::class, NoSuchMethodException::class, IllegalAccessException::class, InvocationTargetException::class, InstantiationException::class)
constructor(args: Array<String>) {

    internal enum class READ_STATE {
        STARTING, FOUND_ANNOTATION
    }

    init {

        // Note: rather than calling System.load("/system/lib/libandroid_runtime.so");
        // which would leave a bunch of unresolved JNI references,
        // we are using the 'withFramework' class as a preloader.
        // So, yeah, russian dolls: withFramework > RootClass > actual method

        val className = args[0]
        val actualArgs = RootArgs()
        actualArgs.args = arrayOfNulls(args.size - 1)
        System.arraycopy(args, 1, actualArgs.args, 0, args.size - 1)
        val classHandler = Class.forName(className)
        val classConstructor = classHandler.getConstructor(RootArgs::class.java)
        classConstructor.newInstance(actualArgs)
    }

    annotation class Candidate

    inner class RootArgs {

        var args: Array<String?>? = null
    }

    // I reckon it would be better to investigate classes using getAttribute()
    // however this method allows the developer to simply select "Run" on RootClass
    // and immediately re-generate the necessary jar file.
    class AnnotationsFinder @Throws(IOException::class)
    constructor() {

        private val AVOIDDIRPATH = "stericson" + File.separator + "RootShell" + File.separator

        private val classFiles: MutableList<File>

        protected val pathToDx: String
            @Throws(IOException::class)
            get() {
                val androidHome = System.getenv("ANDROID_HOME")
                        ?: throw IOException("Error: you need to set \$ANDROID_HOME globally")
                var dxPath: String? = null
                val files = File(androidHome + File.separator + "build-tools").listFiles()
                var recentSdkVersion = 0
                for (file in files) {

                    var fileName: String? = null
                    if (file.name.contains("-")) {
                        val splitFileName = file.name.split("-".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                        if (splitFileName[1].contains("W")) {
                            val fileNameChars = splitFileName[1].toCharArray()
                            fileName = fileNameChars[0].toString()
                        } else {
                            fileName = splitFileName[1]
                        }
                    } else {
                        fileName = file.name
                    }

                    var sdkVersion: Int

                    val sdkVersionBits = fileName!!.split("[.]".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                    sdkVersion = Integer.parseInt(sdkVersionBits[0]) * 10000
                    if (sdkVersionBits.size > 1) {
                        sdkVersion += Integer.parseInt(sdkVersionBits[1]) * 100
                        if (sdkVersionBits.size > 2) {
                            sdkVersion += Integer.parseInt(sdkVersionBits[2])
                        }
                    }
                    if (sdkVersion > recentSdkVersion) {
                        val tentativePath = file.absolutePath + File.separator + "dx"
                        if (File(tentativePath).exists()) {
                            recentSdkVersion = sdkVersion
                            dxPath = tentativePath
                        }
                    }
                }
                if (dxPath == null) {
                    throw IOException("Error: unable to find dx binary in \$ANDROID_HOME")
                }
                return dxPath
            }

        protected// IntelliJ
        // Eclipse IDE
        val builtPath: File?
            get() {
                var foundPath: File? = null

                val ideaPath = File("out" + File.separator + "production")
                if (ideaPath.isDirectory) {
                    val children = ideaPath.listFiles { pathname -> pathname.isDirectory }
                    if (children.size > 0) {
                        foundPath = File(ideaPath.absolutePath + File.separator + children[0].name)
                    }
                }
                if (null == foundPath) {
                    val eclipsePath = File("bin" + File.separator + "classes")
                    if (eclipsePath.isDirectory) {
                        foundPath = eclipsePath
                    }
                }

                return foundPath
            }

        init {
            println("Discovering root class annotations...")
            classFiles = ArrayList()
            lookup(File("src"), classFiles)
            println("Done discovering annotations. Building jar file.")
            val builtPath = builtPath
            if (null != builtPath) {
                // Android! Y U no have com.google.common.base.Joiner class?
                val rc1 = ("com" + File.separator
                        + "stericson" + File.separator
                        + "RootShell" + File.separator
                        + "containers" + File.separator
                        + "RootClass.class")
                val rc2 = ("com" + File.separator
                        + "stericson" + File.separator
                        + "RootShell" + File.separator
                        + "containers" + File.separator
                        + "RootClass\$RootArgs.class")
                val rc3 = ("com" + File.separator
                        + "stericson" + File.separator
                        + "RootShell" + File.separator
                        + "containers" + File.separator
                        + "RootClass\$AnnotationsFinder.class")
                val rc4 = ("com" + File.separator
                        + "stericson" + File.separator
                        + "RootShell" + File.separator
                        + "containers" + File.separator
                        + "RootClass\$AnnotationsFinder$1.class")
                val rc5 = ("com" + File.separator
                        + "stericson" + File.separator
                        + "RootShell" + File.separator
                        + "containers" + File.separator
                        + "RootClass\$AnnotationsFinder$2.class")
                var cmd: Array<String>
                val onWindows = -1 != System.getProperty("os.name")!!.toLowerCase().indexOf("win")
                if (onWindows) {
                    val sb = StringBuilder(
                            " $rc1 $rc2 $rc3 $rc4 $rc5"
                    )
                    for (file in classFiles) {
                        sb.append(" " + file.path)
                    }
                    cmd = arrayOf("cmd", "/C", "jar cvf" +
                            " anbuild.jar" +
                            sb.toString())
                } else {
                    val al = ArrayList<String>()
                    al.add("jar")
                    al.add("cf")
                    al.add("anbuild.jar")
                    al.add(rc1)
                    al.add(rc2)
                    al.add(rc3)
                    al.add(rc4)
                    al.add(rc5)
                    for (file in classFiles) {
                        al.add(file.path)
                    }
                    cmd = al.toTypedArray()
                }
                val jarBuilder = ProcessBuilder(*cmd)
                jarBuilder.directory(builtPath)
                try {
                    jarBuilder.start().waitFor()
                } catch (e: IOException) {
                } catch (e: InterruptedException) {
                }

                val rawFolder = File("res" + File.separator + "raw")
                if (!rawFolder.exists()) {
                    rawFolder.mkdirs()
                }

                println("Done building jar file. Creating dex file.")
                if (onWindows) {
                    cmd = arrayOf("cmd", "/C", "dx --dex --output=res" + File.separator + "raw" + File.separator + "anbuild.dex "
                            + builtPath + File.separator + "anbuild.jar")
                } else {
                    cmd = arrayOf(pathToDx, "--dex", "--output=res" + File.separator + "raw" + File.separator + "anbuild.dex", builtPath.toString() + File.separator + "anbuild.jar")
                }
                val dexBuilder = ProcessBuilder(*cmd)
                try {
                    dexBuilder.start().waitFor()
                } catch (e: IOException) {
                } catch (e: InterruptedException) {
                }

            }
            println("All done. ::: anbuild.dex should now be in your project's res" + File.separator + "raw" + File.separator + " folder :::")
        }

        protected fun lookup(path: File, fileList: MutableList<File>) {
            val desourcedPath = path.toString().replace("src" + File.separator, "")
            val files = path.listFiles()
            for (file in files) {
                if (file.isDirectory) {
                    if (-1 == file.absolutePath.indexOf(AVOIDDIRPATH)) {
                        lookup(file, fileList)
                    }
                } else {
                    if (file.name.endsWith(".java")) {
                        if (hasClassAnnotation(file)) {
                            val fileNamePrefix = file.name.replace(".java", "")
                            val compiledPath = File(builtPath!!.toString() + File.separator + desourcedPath)
                            val classAndInnerClassFiles = compiledPath.listFiles { dir, filename -> filename.startsWith(fileNamePrefix) }
                            for (matchingFile in classAndInnerClassFiles) {
                                fileList.add(File(desourcedPath + File.separator + matchingFile.name))
                            }

                        }
                    }
                }
            }
        }

        protected fun hasClassAnnotation(file: File): Boolean {
            var readState = READ_STATE.STARTING
            val p = Pattern.compile(" class ([A-Za-z0-9_]+)")
            try {
                val reader = BufferedReader(FileReader(file))
                var line: String?=null
                while (({line = reader.readLine();line}())!=null) {
                    when (readState) {
                        READ_STATE.STARTING -> if (-1 < line!!.indexOf("@RootClass.Candidate")) {
                            readState = READ_STATE.FOUND_ANNOTATION
                        }
                        READ_STATE.FOUND_ANNOTATION -> {
                            val m = p.matcher(line)
                            if (m.find()) {
                                println(" Found annotated class: " + m.group(0))
                                return true
                            } else {
                                System.err.println("Error: unmatched annotation in " + file.absolutePath)
                                readState = READ_STATE.STARTING
                            }
                        }
                    }
                }
            } catch (e: FileNotFoundException) {
                e.printStackTrace()
            } catch (e: IOException) {
                e.printStackTrace()
            }

            return false
        }


    }

    companion object {

        /* #ANNOTATIONS
    @Override
    public boolean process(Set<? extends TypeElement> typeElements, RoundEnvironment roundEnvironment) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "I was invoked!!!");

        return false;
    }
    */

        internal var PATH_TO_DX = "/Users/Chris/Projects/android-sdk-macosx/build-tools/18.0.1/dx"

        internal fun displayError(e: Exception) {
            // Not using system.err to make it easier to capture from
            // calling library.
            println("##ERR##" + e.message + "##")
            e.printStackTrace()
        }

        @JvmStatic
        fun main(args: Array<String>) {
            try {
                if (args.size == 0) {
                    AnnotationsFinder()
                } else {
                    RootClass(args)
                }
            } catch (e: Exception) {
                displayError(e)
            }

        }
    }
}
