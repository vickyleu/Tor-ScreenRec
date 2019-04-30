/*
 * Copyright (C) 2006 The Android Open Source Project
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

package dev.nick.tiles.tile

import android.graphics.Bitmap
import android.graphics.Bitmap.CompressFormat
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import org.xmlpull.v1.XmlSerializer
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.ProtocolException
import java.util.*

/**
 * {@hide}
 */
object XmlUtils {

    @Throws(XmlPullParserException::class, IOException::class)
    fun skipCurrentTag(parser: XmlPullParser) {
        val outerDepth = parser.depth
        var type = 0
        while (({ type = parser.next();type }()) != XmlPullParser.END_DOCUMENT && (type != XmlPullParser.END_TAG || parser.depth > outerDepth)) {
        }
    }

    fun convertValueToList(value: CharSequence?, options: Array<String>, defaultValue: Int): Int {
        if (null != value) {
            for (i in options.indices) {
                if (value == options[i])
                    return i
            }
        }

        return defaultValue
    }

    fun convertValueToBoolean(value: CharSequence?, defaultValue: Boolean): Boolean {
        var result = false

        if (null == value)
            return defaultValue

        if (value == "1"
                || value == "true"
                || value == "TRUE")
            result = true

        return result
    }

    fun convertValueToInt(charSeq: CharSequence?, defaultValue: Int): Int {
        if (null == charSeq)
            return defaultValue

        val nm = charSeq.toString()

        // XXX This code is copied from Integer.decode() so we don't
        // have to instantiate an Integer!

        val value: Int
        var sign = 1
        var index = 0
        val len = nm.length
        var base = 10

        if ('-' == nm[0]) {
            sign = -1
            index++
        }

        if ('0' == nm[index]) {
            //  Quick check for a zero by itself
            if (index == len - 1)
                return 0

            val c = nm[index + 1]

            if ('x' == c || 'X' == c) {
                index += 2
                base = 16
            } else {
                index++
                base = 8
            }
        } else if ('#' == nm[index]) {
            index++
            base = 16
        }

        return Integer.parseInt(nm.substring(index), base) * sign
    }

    fun convertValueToUnsignedInt(value: String?, defaultValue: Int): Int {
        return value?.let { parseUnsignedIntAttribute(it) } ?: defaultValue

    }

    fun parseUnsignedIntAttribute(charSeq: CharSequence): Int {
        val value = charSeq.toString()

        val bits: Long
        var index = 0
        val len = value.length
        var base = 10

        if ('0' == value[index]) {
            //  Quick check for zero by itself
            if (index == len - 1)
                return 0

            val c = value[index + 1]

            if ('x' == c || 'X' == c) {     //  check for hex
                index += 2
                base = 16
            } else {                        //  check for octal
                index++
                base = 8
            }
        } else if ('#' == value[index]) {
            index++
            base = 16
        }

        return java.lang.Long.parseLong(value.substring(index), base).toInt()
    }

    /**
     * Flatten a Map into an output stream as XML.  The map can later be
     * read back with readMapXml().
     *
     * @param val The map to be flattened.
     * @param out Where to write the XML data.
     * @see .writeMapXml
     * @see .writeListXml
     *
     * @see .writeValueXml
     *
     * @see .readMapXml
     */
    @Throws(XmlPullParserException::class, IOException::class)
    fun writeMapXml(`val`: Map<*, *>, out: OutputStream) {
        val serializer = FastXmlSerializer()
        serializer.setOutput(out, "utf-8")
        serializer.startDocument(null, true)
        serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true)
        writeMapXml(`val`, null, serializer)
        serializer.endDocument()
    }

    /**
     * Flatten a List into an output stream as XML.  The list can later be
     * read back with readListXml().
     *
     * @param val The list to be flattened.
     * @param out Where to write the XML data.
     * @see .writeListXml
     * @see .writeMapXml
     *
     * @see .writeValueXml
     *
     * @see .readListXml
     */
    @Throws(XmlPullParserException::class, IOException::class)
    fun writeListXml(`val`: List<*>, out: OutputStream) {
        val serializer = Xml.newSerializer()
        serializer.setOutput(out, "utf-8")
        serializer.startDocument(null, true)
        serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true)
        writeListXml(`val`, null, serializer)
        serializer.endDocument()
    }

    /**
     * Flatten a Map into an XmlSerializer.  The map can later be read back
     * with readThisMapXml().
     *
     * @param val      The map to be flattened.
     * @param name     Name attribute to include with this list's tag, or null for
     * none.
     * @param out      XmlSerializer to write the map into.
     * @param callback Method to call when an Object type is not recognized.
     * @hide
     * @see .writeMapXml
     * @see .writeListXml
     *
     * @see .writeValueXml
     *
     * @see .readMapXml
     */
    @Throws(XmlPullParserException::class, IOException::class)
    @JvmOverloads
    fun writeMapXml(`val`: Map<*, *>?, name: String?, out: XmlSerializer,
                    callback: WriteMapCallback? = null) {

        if (`val` == null) {
            out.startTag(null, "null")
            out.endTag(null, "null")
            return
        }

        out.startTag(null, "map")
        if (name != null) {
            out.attribute(null, "name", name)
        }

        writeMapXml(`val`, out, callback)

        out.endTag(null, "map")
    }

    /**
     * Flatten a Map into an XmlSerializer.  The map can later be read back
     * with readThisMapXml(). This method presumes that the start tag and
     * name attribute have already been written and does not write an end tag.
     *
     * @param val The map to be flattened.
     * @param out XmlSerializer to write the map into.
     * @hide
     * @see .writeMapXml
     * @see .writeListXml
     *
     * @see .writeValueXml
     *
     * @see .readMapXml
     */
    @Throws(XmlPullParserException::class, IOException::class)
    fun writeMapXml(`val`: Map<*, *>?, out: XmlSerializer,
                    callback: WriteMapCallback?) {
        if (`val` == null) {
            return
        }

        val s = `val`.entries
        val i = s.iterator()

        while (i.hasNext()) {
            val e = i.next() as Map.Entry<*, *>
            writeValueXml(e.value, e.key as String, out, callback)
        }
    }

    /**
     * Flatten a List into an XmlSerializer.  The list can later be read back
     * with readThisListXml().
     *
     * @param val  The list to be flattened.
     * @param name Name attribute to include with this list's tag, or null for
     * none.
     * @param out  XmlSerializer to write the list into.
     * @see .writeListXml
     * @see .writeMapXml
     *
     * @see .writeValueXml
     *
     * @see .readListXml
     */
    @Throws(XmlPullParserException::class, IOException::class)
    fun writeListXml(`val`: List<*>?, name: String?, out: XmlSerializer) {
        if (`val` == null) {
            out.startTag(null, "null")
            out.endTag(null, "null")
            return
        }

        out.startTag(null, "list")
        if (name != null) {
            out.attribute(null, "name", name)
        }

        val N = `val`.size
        var i = 0
        while (i < N) {
            val value = `val`[i]
            if (value != null)
                writeValueXml(value, null, out)
            i++
        }

        out.endTag(null, "list")
    }

    @Throws(XmlPullParserException::class, IOException::class)
    fun writeSetXml(`val`: Set<*>?, name: String?, out: XmlSerializer) {
        if (`val` == null) {
            out.startTag(null, "null")
            out.endTag(null, "null")
            return
        }

        out.startTag(null, "set")
        if (name != null) {
            out.attribute(null, "name", name)
        }

        for (v in `val`) {
            if (v != null)
                writeValueXml(v, null, out)
        }

        out.endTag(null, "set")
    }

    /**
     * Flatten a byte[] into an XmlSerializer.  The list can later be read back
     * with readThisByteArrayXml().
     *
     * @param val  The byte array to be flattened.
     * @param name Name attribute to include with this array's tag, or null for
     * none.
     * @param out  XmlSerializer to write the array into.
     * @see .writeMapXml
     *
     * @see .writeValueXml
     */
    @Throws(XmlPullParserException::class, IOException::class)
    fun writeByteArrayXml(`val`: ByteArray?, name: String?,
                          out: XmlSerializer) {

        if (`val` == null) {
            out.startTag(null, "null")
            out.endTag(null, "null")
            return
        }

        out.startTag(null, "byte-array")
        if (name != null) {
            out.attribute(null, "name", name)
        }

        val N = `val`.size
        out.attribute(null, "num", Integer.toString(N))

        val sb = StringBuilder(`val`.size * 2)
        for (i in 0 until N) {
            val b = `val`[i].toInt()
            var h = b shr 4
            sb.append(if (h >= 10) 'a'.toInt() + h - 10 else '0'.toInt() + h)
            h = b and 0xff
            sb.append(if (h >= 10) 'a'.toInt() + h - 10 else '0'.toInt() + h)
        }

        out.text(sb.toString())

        out.endTag(null, "byte-array")
    }

    /**
     * Flatten an int[] into an XmlSerializer.  The list can later be read back
     * with readThisIntArrayXml().
     *
     * @param val  The int array to be flattened.
     * @param name Name attribute to include with this array's tag, or null for
     * none.
     * @param out  XmlSerializer to write the array into.
     * @see .writeMapXml
     *
     * @see .writeValueXml
     *
     * @see .readThisIntArrayXml
     */
    @Throws(XmlPullParserException::class, IOException::class)
    fun writeIntArrayXml(`val`: IntArray?, name: String?,
                         out: XmlSerializer) {

        if (`val` == null) {
            out.startTag(null, "null")
            out.endTag(null, "null")
            return
        }

        out.startTag(null, "int-array")
        if (name != null) {
            out.attribute(null, "name", name)
        }

        val N = `val`.size
        out.attribute(null, "num", Integer.toString(N))

        for (i in 0 until N) {
            out.startTag(null, "item")
            out.attribute(null, "value", Integer.toString(`val`[i]))
            out.endTag(null, "item")
        }

        out.endTag(null, "int-array")
    }

    /**
     * Flatten a long[] into an XmlSerializer.  The list can later be read back
     * with readThisLongArrayXml().
     *
     * @param val  The long array to be flattened.
     * @param name Name attribute to include with this array's tag, or null for
     * none.
     * @param out  XmlSerializer to write the array into.
     * @see .writeMapXml
     *
     * @see .writeValueXml
     *
     * @see .readThisIntArrayXml
     */
    @Throws(XmlPullParserException::class, IOException::class)
    fun writeLongArrayXml(`val`: LongArray?, name: String?, out: XmlSerializer) {

        if (`val` == null) {
            out.startTag(null, "null")
            out.endTag(null, "null")
            return
        }

        out.startTag(null, "long-array")
        if (name != null) {
            out.attribute(null, "name", name)
        }

        val N = `val`.size
        out.attribute(null, "num", Integer.toString(N))

        for (i in 0 until N) {
            out.startTag(null, "item")
            out.attribute(null, "value", java.lang.Long.toString(`val`[i]))
            out.endTag(null, "item")
        }

        out.endTag(null, "long-array")
    }

    /**
     * Flatten a double[] into an XmlSerializer.  The list can later be read back
     * with readThisDoubleArrayXml().
     *
     * @param val  The double array to be flattened.
     * @param name Name attribute to include with this array's tag, or null for
     * none.
     * @param out  XmlSerializer to write the array into.
     * @see .writeMapXml
     *
     * @see .writeValueXml
     *
     * @see .readThisIntArrayXml
     */
    @Throws(XmlPullParserException::class, IOException::class)
    fun writeDoubleArrayXml(`val`: DoubleArray?, name: String?, out: XmlSerializer) {

        if (`val` == null) {
            out.startTag(null, "null")
            out.endTag(null, "null")
            return
        }

        out.startTag(null, "double-array")
        if (name != null) {
            out.attribute(null, "name", name)
        }

        val N = `val`.size
        out.attribute(null, "num", Integer.toString(N))

        for (i in 0 until N) {
            out.startTag(null, "item")
            out.attribute(null, "value", java.lang.Double.toString(`val`[i]))
            out.endTag(null, "item")
        }

        out.endTag(null, "double-array")
    }

    /**
     * Flatten a String[] into an XmlSerializer.  The list can later be read back
     * with readThisStringArrayXml().
     *
     * @param val  The String array to be flattened.
     * @param name Name attribute to include with this array's tag, or null for
     * none.
     * @param out  XmlSerializer to write the array into.
     * @see .writeMapXml
     *
     * @see .writeValueXml
     *
     * @see .readThisIntArrayXml
     */
    @Throws(XmlPullParserException::class, IOException::class)
    fun writeStringArrayXml(`val`: Array<String>?, name: String?, out: XmlSerializer) {

        if (`val` == null) {
            out.startTag(null, "null")
            out.endTag(null, "null")
            return
        }

        out.startTag(null, "string-array")
        if (name != null) {
            out.attribute(null, "name", name)
        }

        val N = `val`.size
        out.attribute(null, "num", Integer.toString(N))

        for (i in 0 until N) {
            out.startTag(null, "item")
            out.attribute(null, "value", `val`[i])
            out.endTag(null, "item")
        }

        out.endTag(null, "string-array")
    }

    /**
     * Flatten a boolean[] into an XmlSerializer.  The list can later be read back
     * with readThisBooleanArrayXml().
     *
     * @param val  The boolean array to be flattened.
     * @param name Name attribute to include with this array's tag, or null for
     * none.
     * @param out  XmlSerializer to write the array into.
     * @see .writeMapXml
     *
     * @see .writeValueXml
     *
     * @see .readThisIntArrayXml
     */
    @Throws(XmlPullParserException::class, IOException::class)
    fun writeBooleanArrayXml(`val`: BooleanArray?, name: String?, out: XmlSerializer) {

        if (`val` == null) {
            out.startTag(null, "null")
            out.endTag(null, "null")
            return
        }

        out.startTag(null, "boolean-array")
        if (name != null) {
            out.attribute(null, "name", name)
        }

        val N = `val`.size
        out.attribute(null, "num", Integer.toString(N))

        for (i in 0 until N) {
            out.startTag(null, "item")
            out.attribute(null, "value", java.lang.Boolean.toString(`val`[i]))
            out.endTag(null, "item")
        }

        out.endTag(null, "boolean-array")
    }

    /**
     * Flatten an object's value into an XmlSerializer.  The value can later
     * be read back with readThisValueXml().
     *
     *
     * Currently supported value types are: null, String, Integer, Long,
     * Float, Double Boolean, Map, List.
     *
     * @param v    The object to be flattened.
     * @param name Name attribute to include with this value's tag, or null
     * for none.
     * @param out  XmlSerializer to write the object into.
     * @see .writeMapXml
     *
     * @see .writeListXml
     *
     * @see .readValueXml
     */
    @Throws(XmlPullParserException::class, IOException::class)
    fun writeValueXml(v: Any, name: String?, out: XmlSerializer) {
        writeValueXml(v, name, out, null)
    }

    /**
     * Flatten an object's value into an XmlSerializer.  The value can later
     * be read back with readThisValueXml().
     *
     *
     * Currently supported value types are: null, String, Integer, Long,
     * Float, Double Boolean, Map, List.
     *
     * @param v        The object to be flattened.
     * @param name     Name attribute to include with this value's tag, or null
     * for none.
     * @param out      XmlSerializer to write the object into.
     * @param callback Handler for Object types not recognized.
     * @see .writeMapXml
     *
     * @see .writeListXml
     *
     * @see .readValueXml
     */
    @Throws(XmlPullParserException::class, IOException::class)
    private fun writeValueXml(v: Any?, name: String?, out: XmlSerializer,
                              callback: WriteMapCallback?) {
        val typeStr: String
        if (v == null) {
            out.startTag(null, "null")
            if (name != null) {
                out.attribute(null, "name", name)
            }
            out.endTag(null, "null")
            return
        } else if (v is String) {
            out.startTag(null, "string")
            if (name != null) {
                out.attribute(null, "name", name)
            }
            out.text(v.toString())
            out.endTag(null, "string")
            return
        } else if (v is Int) {
            typeStr = "int"
        } else if (v is Long) {
            typeStr = "long"
        } else if (v is Float) {
            typeStr = "float"
        } else if (v is Double) {
            typeStr = "double"
        } else if (v is Boolean) {
            typeStr = "boolean"
        } else if (v is ByteArray) {
            writeByteArrayXml(v as ByteArray?, name, out)
            return
        } else if (v is IntArray) {
            writeIntArrayXml(v as IntArray?, name, out)
            return
        } else if (v is LongArray) {
            writeLongArrayXml(v as LongArray?, name, out)
            return
        } else if (v is DoubleArray) {
            writeDoubleArrayXml(v as DoubleArray?, name, out)
            return
        } else if (v is Array<*>) {
            writeStringArrayXml(v as Array<String>?, name, out)
            return
        } else if (v is BooleanArray) {
            writeBooleanArrayXml(v as BooleanArray?, name, out)
            return
        } else if (v is Map<*, *>) {
            writeMapXml(v as Map<*, *>?, name, out)
            return
        } else if (v is List<*>) {
            writeListXml(v as List<*>?, name, out)
            return
        } else if (v is Set<*>) {
            writeSetXml(v as Set<*>?, name, out)
            return
        } else if (v is CharSequence) {
            // XXX This is to allow us to at least write something if
            // we encounter styled text...  but it means we will drop all
            // of the styling information. :(
            out.startTag(null, "string")
            if (name != null) {
                out.attribute(null, "name", name)
            }
            out.text(v.toString())
            out.endTag(null, "string")
            return
        } else if (callback != null) {
            callback.writeUnknownObject(v, name, out)
            return
        } else {
            throw RuntimeException("writeValueXml: unable to write value $v")
        }

        out.startTag(null, typeStr)
        if (name != null) {
            out.attribute(null, "name", name)
        }
        out.attribute(null, "value", v.toString())
        out.endTag(null, typeStr)
    }

    /**
     * Read a HashMap from an InputStream containing XML.  The stream can
     * previously have been written by writeMapXml().
     *
     * @param in The InputStream from which to read.
     * @return HashMap The resulting map.
     * @see .readListXml
     *
     * @see .readValueXml
     *
     * @see .readThisMapXml
     * .see .writeMapXml
     */
    @Throws(XmlPullParserException::class, IOException::class)
    fun readMapXml(`in`: InputStream): HashMap<String, *>? {
        val parser = Xml.newPullParser()
        parser.setInput(`in`, null)
        return readValueXml(parser, arrayOf("")) as? HashMap<String, *>
    }

    /**
     * Read an ArrayList from an InputStream containing XML.  The stream can
     * previously have been written by writeListXml().
     *
     * @param in The InputStream from which to read.
     * @return ArrayList The resulting list.
     * @see .readMapXml
     *
     * @see .readValueXml
     *
     * @see .readThisListXml
     *
     * @see .writeListXml
     */
    @Throws(XmlPullParserException::class, IOException::class)
    fun readListXml(`in`: InputStream): ArrayList<*>? {
        val parser = Xml.newPullParser()
        parser.setInput(`in`, null)
        return readValueXml(parser, arrayOf("")) as? ArrayList<*>
    }


    /**
     * Read a HashSet from an InputStream containing XML. The stream can
     * previously have been written by writeSetXml().
     *
     * @param in The InputStream from which to read.
     * @return HashSet The resulting set.
     * @throws XmlPullParserException
     * @throws IOException
     * @see .readValueXml
     *
     * @see .readThisSetXml
     *
     * @see .writeSetXml
     */
    @Throws(XmlPullParserException::class, IOException::class)
    fun readSetXml(`in`: InputStream): HashSet<*>? {
        val parser = Xml.newPullParser()
        parser.setInput(`in`, null)
        return readValueXml(parser, arrayOf("")) as? HashSet<*>
    }

    /**
     * Read a HashMap object from an XmlPullParser.  The XML data could
     * previously have been generated by writeMapXml().  The XmlPullParser
     * must be positioned *after* the tag that begins the map.
     *
     * @param parser The XmlPullParser from which to read the map data.
     * @param endTag Name of the tag that will end the map, usually "map".
     * @param name   An array of one string, used to return the name attribute
     * of the map's tag.
     * @return HashMap The newly generated map.
     * @hide
     * @see .readMapXml
     */
    @Throws(XmlPullParserException::class, IOException::class)
    @JvmOverloads
    fun readThisMapXml(parser: XmlPullParser, endTag: String,
                       name: Array<String>, callback: ReadMapCallback? = null): HashMap<String, *> {
        val map = HashMap<String, Any?>()

        var eventType = parser.eventType
        do {
            if (eventType == XmlPullParser.START_TAG) {
                val `val` = readThisValueXml(parser, name, callback)
                map[name[0]] = `val`
            } else if (eventType == XmlPullParser.END_TAG) {
                if (parser.name == endTag) {
                    return map
                }
                throw XmlPullParserException(
                        "Expected " + endTag + " end tag at: " + parser.name)
            }
            eventType = parser.next()
        } while (eventType != XmlPullParser.END_DOCUMENT)

        throw XmlPullParserException(
                "Document ended before $endTag end tag")
    }

    /**
     * Read an ArrayList object from an XmlPullParser.  The XML data could
     * previously have been generated by writeListXml().  The XmlPullParser
     * must be positioned *after* the tag that begins the list.
     *
     * @param parser The XmlPullParser from which to read the list data.
     * @param endTag Name of the tag that will end the list, usually "list".
     * @param name   An array of one string, used to return the name attribute
     * of the list's tag.
     * @return HashMap The newly generated list.
     * @see .readListXml
     */
    @Throws(XmlPullParserException::class, IOException::class)
    fun readThisListXml(parser: XmlPullParser, endTag: String,
                        name: Array<String>): ArrayList<*> {
        return readThisListXml(parser, endTag, name, null)
    }

    /**
     * Read an ArrayList object from an XmlPullParser.  The XML data could
     * previously have been generated by writeListXml().  The XmlPullParser
     * must be positioned *after* the tag that begins the list.
     *
     * @param parser The XmlPullParser from which to read the list data.
     * @param endTag Name of the tag that will end the list, usually "list".
     * @param name   An array of one string, used to return the name attribute
     * of the list's tag.
     * @return HashMap The newly generated list.
     * @see .readListXml
     */
    @Throws(XmlPullParserException::class, IOException::class)
    private fun readThisListXml(parser: XmlPullParser, endTag: String,
                                name: Array<String>, callback: ReadMapCallback?): ArrayList<*> {
        val list = ArrayList<Any?>()

        var eventType = parser.eventType
        do {
            if (eventType == XmlPullParser.START_TAG) {
                val `val` = readThisValueXml(parser, name, callback)
                list.add(`val`)
                //System.out.println("Adding to list: " + val);
            } else if (eventType == XmlPullParser.END_TAG) {
                if (parser.name == endTag) {
                    return list
                }
                throw XmlPullParserException(
                        "Expected " + endTag + " end tag at: " + parser.name)
            }
            eventType = parser.next()
        } while (eventType != XmlPullParser.END_DOCUMENT)

        throw XmlPullParserException(
                "Document ended before $endTag end tag")
    }

    /**
     * Read a HashSet object from an XmlPullParser. The XML data could previously
     * have been generated by writeSetXml(). The XmlPullParser must be positioned
     * *after* the tag that begins the set.
     *
     * @param parser The XmlPullParser from which to read the set data.
     * @param endTag Name of the tag that will end the set, usually "set".
     * @param name   An array of one string, used to return the name attribute
     * of the set's tag.
     * @return HashSet The newly generated set.
     * @throws XmlPullParserException
     * @throws IOException
     * @see .readSetXml
     */
    @Throws(XmlPullParserException::class, IOException::class)
    fun readThisSetXml(parser: XmlPullParser, endTag: String, name: Array<String>): HashSet<*> {
        return readThisSetXml(parser, endTag, name, null)
    }

    /**
     * Read a HashSet object from an XmlPullParser. The XML data could previously
     * have been generated by writeSetXml(). The XmlPullParser must be positioned
     * *after* the tag that begins the set.
     *
     * @param parser The XmlPullParser from which to read the set data.
     * @param endTag Name of the tag that will end the set, usually "set".
     * @param name   An array of one string, used to return the name attribute
     * of the set's tag.
     * @return HashSet The newly generated set.
     * @throws XmlPullParserException
     * @throws IOException
     * @hide
     * @see .readSetXml
     */
    @Throws(XmlPullParserException::class, IOException::class)
    private fun readThisSetXml(parser: XmlPullParser, endTag: String, name: Array<String>,
                               callback: ReadMapCallback?): HashSet<*> {
        val set = HashSet<Any?>()

        var eventType = parser.eventType
        do {
            if (eventType == XmlPullParser.START_TAG) {
                val `val` = readThisValueXml(parser, name, callback)
                set.add(`val`)
                //System.out.println("Adding to set: " + val);
            } else if (eventType == XmlPullParser.END_TAG) {
                if (parser.name == endTag) {
                    return set
                }
                throw XmlPullParserException(
                        "Expected " + endTag + " end tag at: " + parser.name)
            }
            eventType = parser.next()
        } while (eventType != XmlPullParser.END_DOCUMENT)

        throw XmlPullParserException(
                "Document ended before $endTag end tag")
    }

    /**
     * Read an int[] object from an XmlPullParser.  The XML data could
     * previously have been generated by writeIntArrayXml().  The XmlPullParser
     * must be positioned *after* the tag that begins the list.
     *
     * @param parser The XmlPullParser from which to read the list data.
     * @param endTag Name of the tag that will end the list, usually "list".
     * @param name   An array of one string, used to return the name attribute
     * of the list's tag.
     * @return Returns a newly generated int[].
     * @see .readListXml
     */
    @Throws(XmlPullParserException::class, IOException::class)
    fun readThisIntArrayXml(parser: XmlPullParser,
                            endTag: String, name: Array<String>): IntArray {

        val num: Int
        try {
            num = Integer.parseInt(parser.getAttributeValue(null, "num"))
        } catch (e: NullPointerException) {
            throw XmlPullParserException(
                    "Need num attribute in byte-array")
        } catch (e: NumberFormatException) {
            throw XmlPullParserException(
                    "Not a number in num attribute in byte-array")
        }

        parser.next()

        val array = IntArray(num)
        var i = 0

        var eventType = parser.eventType
        do {
            if (eventType == XmlPullParser.START_TAG) {
                if (parser.name == "item") {
                    try {
                        array[i] = Integer.parseInt(
                                parser.getAttributeValue(null, "value"))
                    } catch (e: NullPointerException) {
                        throw XmlPullParserException(
                                "Need value attribute in item")
                    } catch (e: NumberFormatException) {
                        throw XmlPullParserException(
                                "Not a number in value attribute in item")
                    }

                } else {
                    throw XmlPullParserException(
                            "Expected item tag at: " + parser.name)
                }
            } else if (eventType == XmlPullParser.END_TAG) {
                if (parser.name == endTag) {
                    return array
                } else if (parser.name == "item") {
                    i++
                } else {
                    throw XmlPullParserException(
                            "Expected " + endTag + " end tag at: "
                                    + parser.name)
                }
            }
            eventType = parser.next()
        } while (eventType != XmlPullParser.END_DOCUMENT)

        throw XmlPullParserException(
                "Document ended before $endTag end tag")
    }

    /**
     * Read a long[] object from an XmlPullParser.  The XML data could
     * previously have been generated by writeLongArrayXml().  The XmlPullParser
     * must be positioned *after* the tag that begins the list.
     *
     * @param parser The XmlPullParser from which to read the list data.
     * @param endTag Name of the tag that will end the list, usually "list".
     * @param name   An array of one string, used to return the name attribute
     * of the list's tag.
     * @return Returns a newly generated long[].
     * @see .readListXml
     */
    @Throws(XmlPullParserException::class, IOException::class)
    fun readThisLongArrayXml(parser: XmlPullParser,
                             endTag: String, name: Array<String>): LongArray {

        val num: Int
        try {
            num = Integer.parseInt(parser.getAttributeValue(null, "num"))
        } catch (e: NullPointerException) {
            throw XmlPullParserException("Need num attribute in long-array")
        } catch (e: NumberFormatException) {
            throw XmlPullParserException("Not a number in num attribute in long-array")
        }

        parser.next()

        val array = LongArray(num)
        var i = 0

        var eventType = parser.eventType
        do {
            if (eventType == XmlPullParser.START_TAG) {
                if (parser.name == "item") {
                    try {
                        array[i] = java.lang.Long.parseLong(parser.getAttributeValue(null, "value"))
                    } catch (e: NullPointerException) {
                        throw XmlPullParserException("Need value attribute in item")
                    } catch (e: NumberFormatException) {
                        throw XmlPullParserException("Not a number in value attribute in item")
                    }

                } else {
                    throw XmlPullParserException("Expected item tag at: " + parser.name)
                }
            } else if (eventType == XmlPullParser.END_TAG) {
                if (parser.name == endTag) {
                    return array
                } else if (parser.name == "item") {
                    i++
                } else {
                    throw XmlPullParserException("Expected " + endTag + " end tag at: " +
                            parser.name)
                }
            }
            eventType = parser.next()
        } while (eventType != XmlPullParser.END_DOCUMENT)

        throw XmlPullParserException("Document ended before $endTag end tag")
    }

    /**
     * Read a double[] object from an XmlPullParser.  The XML data could
     * previously have been generated by writeDoubleArrayXml().  The XmlPullParser
     * must be positioned *after* the tag that begins the list.
     *
     * @param parser The XmlPullParser from which to read the list data.
     * @param endTag Name of the tag that will end the list, usually "double-array".
     * @param name   An array of one string, used to return the name attribute
     * of the list's tag.
     * @return Returns a newly generated double[].
     * @see .readListXml
     */
    @Throws(XmlPullParserException::class, IOException::class)
    fun readThisDoubleArrayXml(parser: XmlPullParser, endTag: String,
                               name: Array<String>): DoubleArray {

        val num: Int
        try {
            num = Integer.parseInt(parser.getAttributeValue(null, "num"))
        } catch (e: NullPointerException) {
            throw XmlPullParserException("Need num attribute in double-array")
        } catch (e: NumberFormatException) {
            throw XmlPullParserException("Not a number in num attribute in double-array")
        }

        parser.next()

        val array = DoubleArray(num)
        var i = 0

        var eventType = parser.eventType
        do {
            if (eventType == XmlPullParser.START_TAG) {
                if (parser.name == "item") {
                    try {
                        array[i] = java.lang.Double.parseDouble(parser.getAttributeValue(null, "value"))
                    } catch (e: NullPointerException) {
                        throw XmlPullParserException("Need value attribute in item")
                    } catch (e: NumberFormatException) {
                        throw XmlPullParserException("Not a number in value attribute in item")
                    }

                } else {
                    throw XmlPullParserException("Expected item tag at: " + parser.name)
                }
            } else if (eventType == XmlPullParser.END_TAG) {
                if (parser.name == endTag) {
                    return array
                } else if (parser.name == "item") {
                    i++
                } else {
                    throw XmlPullParserException("Expected " + endTag + " end tag at: " +
                            parser.name)
                }
            }
            eventType = parser.next()
        } while (eventType != XmlPullParser.END_DOCUMENT)

        throw XmlPullParserException("Document ended before $endTag end tag")
    }

    /**
     * Read a String[] object from an XmlPullParser.  The XML data could
     * previously have been generated by writeStringArrayXml().  The XmlPullParser
     * must be positioned *after* the tag that begins the list.
     *
     * @param parser The XmlPullParser from which to read the list data.
     * @param endTag Name of the tag that will end the list, usually "string-array".
     * @param name   An array of one string, used to return the name attribute
     * of the list's tag.
     * @return Returns a newly generated String[].
     * @see .readListXml
     */
    @Throws(XmlPullParserException::class, IOException::class)
    fun readThisStringArrayXml(parser: XmlPullParser, endTag: String,
                               name: Array<String>): Array<String> {

        val num: Int
        try {
            num = Integer.parseInt(parser.getAttributeValue(null, "num"))
        } catch (e: NullPointerException) {
            throw XmlPullParserException("Need num attribute in string-array")
        } catch (e: NumberFormatException) {
            throw XmlPullParserException("Not a number in num attribute in string-array")
        }

        parser.next()

        val array = arrayOfNulls<String>(num)
        var i = 0

        var eventType = parser.eventType
        do {
            if (eventType == XmlPullParser.START_TAG) {
                if (parser.name == "item") {
                    try {
                        array[i] = parser.getAttributeValue(null, "value")
                    } catch (e: NullPointerException) {
                        throw XmlPullParserException("Need value attribute in item")
                    } catch (e: NumberFormatException) {
                        throw XmlPullParserException("Not a number in value attribute in item")
                    }

                } else {
                    throw XmlPullParserException("Expected item tag at: " + parser.name)
                }
            } else if (eventType == XmlPullParser.END_TAG) {
                if (parser.name == endTag) {
                    return array.filterNotNull().toTypedArray()
                } else if (parser.name == "item") {
                    i++
                } else {
                    throw XmlPullParserException("Expected " + endTag + " end tag at: " +
                            parser.name)
                }
            }
            eventType = parser.next()
        } while (eventType != XmlPullParser.END_DOCUMENT)

        throw XmlPullParserException("Document ended before $endTag end tag")
    }

    /**
     * Read a boolean[] object from an XmlPullParser.  The XML data could
     * previously have been generated by writeBooleanArrayXml().  The XmlPullParser
     * must be positioned *after* the tag that begins the list.
     *
     * @param parser The XmlPullParser from which to read the list data.
     * @param endTag Name of the tag that will end the list, usually "string-array".
     * @param name   An array of one string, used to return the name attribute
     * of the list's tag.
     * @return Returns a newly generated boolean[].
     * @see .readListXml
     */
    @Throws(XmlPullParserException::class, IOException::class)
    fun readThisBooleanArrayXml(parser: XmlPullParser, endTag: String,
                                name: Array<String>): BooleanArray {

        val num: Int
        try {
            num = Integer.parseInt(parser.getAttributeValue(null, "num"))
        } catch (e: NullPointerException) {
            throw XmlPullParserException("Need num attribute in string-array")
        } catch (e: NumberFormatException) {
            throw XmlPullParserException("Not a number in num attribute in string-array")
        }

        parser.next()

        val array = BooleanArray(num)
        var i = 0

        var eventType = parser.eventType
        do {
            if (eventType == XmlPullParser.START_TAG) {
                if (parser.name == "item") {
                    try {
                        array[i] = java.lang.Boolean.valueOf(parser.getAttributeValue(null, "value"))
                    } catch (e: NullPointerException) {
                        throw XmlPullParserException("Need value attribute in item")
                    } catch (e: NumberFormatException) {
                        throw XmlPullParserException("Not a number in value attribute in item")
                    }

                } else {
                    throw XmlPullParserException("Expected item tag at: " + parser.name)
                }
            } else if (eventType == XmlPullParser.END_TAG) {
                if (parser.name == endTag) {
                    return array
                } else if (parser.name == "item") {
                    i++
                } else {
                    throw XmlPullParserException("Expected " + endTag + " end tag at: " +
                            parser.name)
                }
            }
            eventType = parser.next()
        } while (eventType != XmlPullParser.END_DOCUMENT)

        throw XmlPullParserException("Document ended before $endTag end tag")
    }

    /**
     * Read a flattened object from an XmlPullParser.  The XML data could
     * previously have been written with writeMapXml(), writeListXml(), or
     * writeValueXml().  The XmlPullParser must be positioned *at* the
     * tag that defines the value.
     *
     * @param parser The XmlPullParser from which to read the object.
     * @param name   An array of one string, used to return the name attribute
     * of the value's tag.
     * @return Object The newly generated value object.
     * @see .readMapXml
     *
     * @see .readListXml
     *
     * @see .writeValueXml
     */
    @Throws(XmlPullParserException::class, IOException::class)
    fun readValueXml(parser: XmlPullParser, name: Array<String>): Any? {
        var eventType = parser.eventType
        do {
            if (eventType == XmlPullParser.START_TAG) {
                return readThisValueXml(parser, name, null)
            } else if (eventType == XmlPullParser.END_TAG) {
                throw XmlPullParserException(
                        "Unexpected end tag at: " + parser.name)
            } else if (eventType == XmlPullParser.TEXT) {
                throw XmlPullParserException(
                        "Unexpected text: " + parser.text)
            }
            eventType = parser.next()
        } while (eventType != XmlPullParser.END_DOCUMENT)

        throw XmlPullParserException(
                "Unexpected end of document")
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun readThisValueXml(parser: XmlPullParser, name: Array<String>,
                                 callback: ReadMapCallback?): Any? {
        val valueName = parser.getAttributeValue(null, "name")
        val tagName = parser.name

        //System.out.println("Reading this value tag: " + tagName + ", name=" + valueName);

        var res: Any? = null

        if (tagName == "null") {
            res = null
        } else if (tagName == "string") {
            var value = ""
            var eventType = 0
            while (({ eventType = parser.next();eventType }()) != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.END_TAG) {
                    if (parser.name == "string") {
                        name[0] = valueName
                        //System.out.println("Returning value for " + valueName + ": " + value);
                        return value
                    }
                    throw XmlPullParserException(
                            "Unexpected end tag in <string>: " + parser.name)
                } else if (eventType == XmlPullParser.TEXT) {
                    value += parser.text
                } else if (eventType == XmlPullParser.START_TAG) {
                    throw XmlPullParserException(
                            "Unexpected start tag in <string>: " + parser.name)
                }
            }
            throw XmlPullParserException(
                    "Unexpected end of document in <string>")
        } else if (({ res = readThisPrimitiveValueXml(parser, tagName);res }()) != null) {
            // all work already done by readThisPrimitiveValueXml
        } else if (tagName == "int-array") {
            res = readThisIntArrayXml(parser, "int-array", name)
            name[0] = valueName
            //System.out.println("Returning value for " + valueName + ": " + res);
            return res
        } else if (tagName == "long-array") {
            res = readThisLongArrayXml(parser, "long-array", name)
            name[0] = valueName
            //System.out.println("Returning value for " + valueName + ": " + res);
            return res
        } else if (tagName == "double-array") {
            res = readThisDoubleArrayXml(parser, "double-array", name)
            name[0] = valueName
            //System.out.println("Returning value for " + valueName + ": " + res);
            return res
        } else if (tagName == "string-array") {
            res = readThisStringArrayXml(parser, "string-array", name)
            name[0] = valueName
            //System.out.println("Returning value for " + valueName + ": " + res);
            return res
        } else if (tagName == "boolean-array") {
            res = readThisBooleanArrayXml(parser, "boolean-array", name)
            name[0] = valueName
            //System.out.println("Returning value for " + valueName + ": " + res);
            return res
        } else if (tagName == "map") {
            parser.next()
            res = readThisMapXml(parser, "map", name)
            name[0] = valueName
            //System.out.println("Returning value for " + valueName + ": " + res);
            return res
        } else if (tagName == "list") {
            parser.next()
            res = readThisListXml(parser, "list", name)
            name[0] = valueName
            //System.out.println("Returning value for " + valueName + ": " + res);
            return res
        } else if (tagName == "set") {
            parser.next()
            res = readThisSetXml(parser, "set", name)
            name[0] = valueName
            //System.out.println("Returning value for " + valueName + ": " + res);
            return res
        } else if (callback != null) {
            res = callback.readThisUnknownObjectXml(parser, tagName)
            name[0] = valueName
            return res
        } else {
            throw XmlPullParserException("Unknown tag: $tagName")
        }

        // Skip through to end tag.
        var eventType = 0
        while (({ eventType = parser.next();eventType }()) != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.END_TAG) {
                if (parser.name == tagName) {
                    name[0] = valueName
                    //System.out.println("Returning value for " + valueName + ": " + res);
                    return res
                }
                throw XmlPullParserException(
                        "Unexpected end tag in <" + tagName + ">: " + parser.name)
            } else if (eventType == XmlPullParser.TEXT) {
                throw XmlPullParserException(
                        "Unexpected text in <" + tagName + ">: " + parser.name)
            } else if (eventType == XmlPullParser.START_TAG) {
                throw XmlPullParserException(
                        "Unexpected start tag in <" + tagName + ">: " + parser.name)
            }
        }
        throw XmlPullParserException(
                "Unexpected end of document in <$tagName>")
    }

    @Throws(XmlPullParserException::class, IOException::class)
    private fun readThisPrimitiveValueXml(parser: XmlPullParser, tagName: String): Any? {
        try {
            return if (tagName == "int") {
                parser.getAttributeValue(null, "value").toInt()
            } else if (tagName == "long") {
                parser.getAttributeValue(null, "value").toLong()
            } else if (tagName == "float") {
                parser.getAttributeValue(null, "value").toFloat()
            } else if (tagName == "double") {
                parser.getAttributeValue(null, "value").toDouble()
            } else if (tagName == "boolean") {
                parser.getAttributeValue(null, "value")?.toBoolean()
            } else {
                null
            }
        } catch (e: NullPointerException) {
            throw XmlPullParserException("Need value attribute in <$tagName>")
        } catch (e: NumberFormatException) {
            throw XmlPullParserException(
                    "Not a number in value attribute in <$tagName>")
        }

    }

    @Throws(XmlPullParserException::class, IOException::class)
    fun beginDocument(parser: XmlPullParser, firstElementName: String) {
        var type: Int = 0
        while (({ type = parser.next();type }()) != XmlPullParser.START_TAG && type != XmlPullParser.END_DOCUMENT) {
        }

        if (type != XmlPullParser.START_TAG) {
            throw XmlPullParserException("No start tag found")
        }

        if (parser.name != firstElementName) {
            throw XmlPullParserException("Unexpected start tag: found " + parser.name +
                    ", expected " + firstElementName)
        }
    }

    @Throws(XmlPullParserException::class, IOException::class)
    fun nextElement(parser: XmlPullParser) {
        var type = 0
        while (({ type = parser.next();type }()) != XmlPullParser.START_TAG && type != XmlPullParser.END_DOCUMENT) {
        }
    }

    @Throws(IOException::class, XmlPullParserException::class)
    fun nextElementWithin(parser: XmlPullParser, outerDepth: Int): Boolean {
        while (true) {
            val type = parser.next()
            if (type == XmlPullParser.END_DOCUMENT || type == XmlPullParser.END_TAG && parser.depth == outerDepth) {
                return false
            }
            if (type == XmlPullParser.START_TAG && parser.depth == outerDepth + 1) {
                return true
            }
        }
    }

    fun readIntAttribute(`in`: XmlPullParser, name: String, defaultValue: Int): Int {
        val value = `in`.getAttributeValue(null, name)
        try {
            return Integer.parseInt(value)
        } catch (e: NumberFormatException) {
            return defaultValue
        }

    }

    @Throws(IOException::class)
    fun readIntAttribute(`in`: XmlPullParser, name: String): Int {
        val value = `in`.getAttributeValue(null, name)
        try {
            return Integer.parseInt(value)
        } catch (e: NumberFormatException) {
            throw ProtocolException("problem parsing $name=$value as int")
        }

    }

    @Throws(IOException::class)
    fun writeIntAttribute(out: XmlSerializer, name: String, value: Int) {
        out.attribute(null, name, Integer.toString(value))
    }

    fun readLongAttribute(`in`: XmlPullParser, name: String, defaultValue: Long): Long {
        val value = `in`.getAttributeValue(null, name)
        try {
            return java.lang.Long.parseLong(value)
        } catch (e: NumberFormatException) {
            return defaultValue
        }

    }

    @Throws(IOException::class)
    fun readLongAttribute(`in`: XmlPullParser, name: String): Long {
        val value = `in`.getAttributeValue(null, name)
        try {
            return java.lang.Long.parseLong(value)
        } catch (e: NumberFormatException) {
            throw ProtocolException("problem parsing $name=$value as long")
        }

    }

    @Throws(IOException::class)
    fun writeLongAttribute(out: XmlSerializer, name: String, value: Long) {
        out.attribute(null, name, java.lang.Long.toString(value))
    }

    @Throws(IOException::class)
    fun readFloatAttribute(`in`: XmlPullParser, name: String): Float {
        val value = `in`.getAttributeValue(null, name)
        try {
            return java.lang.Float.parseFloat(value)
        } catch (e: NumberFormatException) {
            throw ProtocolException("problem parsing $name=$value as long")
        }

    }

    @Throws(IOException::class)
    fun writeFloatAttribute(out: XmlSerializer, name: String, value: Float) {
        out.attribute(null, name, java.lang.Float.toString(value))
    }

    fun readBooleanAttribute(`in`: XmlPullParser, name: String): Boolean {
        val value = `in`.getAttributeValue(null, name)
        return java.lang.Boolean.parseBoolean(value)
    }

    fun readBooleanAttribute(`in`: XmlPullParser, name: String,
                             defaultValue: Boolean): Boolean {
        val value = `in`.getAttributeValue(null, name)
        return if (value == null) {
            defaultValue
        } else {
            java.lang.Boolean.parseBoolean(value)
        }
    }

    @Throws(IOException::class)
    fun writeBooleanAttribute(out: XmlSerializer, name: String, value: Boolean) {
        out.attribute(null, name, java.lang.Boolean.toString(value))
    }

    fun readUriAttribute(`in`: XmlPullParser, name: String): Uri? {
        val value = `in`.getAttributeValue(null, name)
        return if (value != null) Uri.parse(value) else null
    }

    @Throws(IOException::class)
    fun writeUriAttribute(out: XmlSerializer, name: String, value: Uri?) {
        if (value != null) {
            out.attribute(null, name, value.toString())
        }
    }

    fun readStringAttribute(`in`: XmlPullParser, name: String): String {
        return `in`.getAttributeValue(null, name)
    }

    @Throws(IOException::class)
    fun writeStringAttribute(out: XmlSerializer, name: String, value: String?) {
        if (value != null) {
            out.attribute(null, name, value)
        }
    }

    fun readByteArrayAttribute(`in`: XmlPullParser, name: String): ByteArray? {
        val value = `in`.getAttributeValue(null, name)
        return if (value != null) {
            Base64.decode(value, Base64.DEFAULT)
        } else {
            null
        }
    }

    @Throws(IOException::class)
    fun writeByteArrayAttribute(out: XmlSerializer, name: String, value: ByteArray?) {
        if (value != null) {
            out.attribute(null, name, Base64.encodeToString(value, Base64.DEFAULT))
        }
    }

    fun readBitmapAttribute(`in`: XmlPullParser, name: String): Bitmap? {
        val value = readByteArrayAttribute(`in`, name)
        return if (value != null) {
            BitmapFactory.decodeByteArray(value, 0, value.size)
        } else {
            null
        }
    }

    @Deprecated("")
    @Throws(IOException::class)
    fun writeBitmapAttribute(out: XmlSerializer, name: String, value: Bitmap?) {
        if (value != null) {
            val os = ByteArrayOutputStream()
            value.compress(CompressFormat.PNG, 90, os)
            writeByteArrayAttribute(out, name, os.toByteArray())
        }
    }

    /**
     * @hide
     */
    interface WriteMapCallback {
        /**
         * Called from writeMapXml when an Object type is not recognized. The implementer
         * must write out the entire element including start and end tags.
         *
         * @param v    The object to be written out
         * @param name The mapping key for v. Must be written into the "name" attribute of the
         * start tag.
         * @param out  The XML output stream.
         * @throws XmlPullParserException on unrecognized Object type.
         * @throws IOException            on XmlSerializer serialization errors.
         * @hide
         */
        @Throws(XmlPullParserException::class, IOException::class)
        fun writeUnknownObject(v: Any, name: String?, out: XmlSerializer)
    }

    /**
     * @hide
     */
    interface ReadMapCallback {
        /**
         * Called from readThisMapXml when a START_TAG is not recognized. The input stream
         * is positioned within the start tag so that attributes can be read using in.getAttribute.
         *
         * @param in  the XML input stream
         * @param tag the START_TAG that was not recognized.
         * @return the Object parsed from the stream which will be put into the map.
         * @throws XmlPullParserException if the START_TAG is not recognized.
         * @throws IOException            on XmlPullParser serialization errors.
         * @hide
         */
        @Throws(XmlPullParserException::class, IOException::class)
        fun readThisUnknownObjectXml(`in`: XmlPullParser, tag: String): Any
    }
}
/**
 * Flatten a Map into an XmlSerializer.  The map can later be read back
 * with readThisMapXml().
 *
 * @param val  The map to be flattened.
 * @param name Name attribute to include with this list's tag, or null for
 * none.
 * @param out  XmlSerializer to write the map into.
 * @see .writeMapXml
 * @see .writeListXml
 *
 * @see .writeValueXml
 *
 * @see .readMapXml
 */
/**
 * Read a HashMap object from an XmlPullParser.  The XML data could
 * previously have been generated by writeMapXml().  The XmlPullParser
 * must be positioned *after* the tag that begins the map.
 *
 * @param parser The XmlPullParser from which to read the map data.
 * @param endTag Name of the tag that will end the map, usually "map".
 * @param name   An array of one string, used to return the name attribute
 * of the map's tag.
 * @return HashMap The newly generated map.
 * @see .readMapXml
 */
