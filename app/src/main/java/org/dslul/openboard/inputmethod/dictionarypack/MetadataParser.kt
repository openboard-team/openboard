package org.dslul.openboard.inputmethod.dictionarypack

import android.text.TextUtils
import android.util.JsonReader
import java.io.IOException
import java.io.InputStreamReader
import java.util.*

/**
 * Helper class containing functions to parse the dictionary metadata.
 */
object MetadataParser {
    // Name of the fields in the JSON-formatted file.
    private val ID_FIELD_NAME: String = MetadataDbHelper.Companion.WORDLISTID_COLUMN
    private const val LOCALE_FIELD_NAME = "locale"
    private val DESCRIPTION_FIELD_NAME: String = MetadataDbHelper.Companion.DESCRIPTION_COLUMN
    private const val UPDATE_FIELD_NAME = "update"
    private val FILESIZE_FIELD_NAME: String = MetadataDbHelper.Companion.FILESIZE_COLUMN
    private val RAW_CHECKSUM_FIELD_NAME: String = MetadataDbHelper.Companion.RAW_CHECKSUM_COLUMN
    private val CHECKSUM_FIELD_NAME: String = MetadataDbHelper.Companion.CHECKSUM_COLUMN
    private val REMOTE_FILENAME_FIELD_NAME: String = MetadataDbHelper.Companion.REMOTE_FILENAME_COLUMN
    private val VERSION_FIELD_NAME: String = MetadataDbHelper.Companion.VERSION_COLUMN
    private val FORMATVERSION_FIELD_NAME: String = MetadataDbHelper.Companion.FORMATVERSION_COLUMN
    /**
     * Parse one JSON-formatted word list metadata.
     * @param reader the reader containing the data.
     * @return a WordListMetadata object from the parsed data.
     * @throws IOException if the underlying reader throws IOException during reading.
     */
    @Throws(IOException::class, BadFormatException::class)
    private fun parseOneWordList(reader: JsonReader): WordListMetadata {
        val arguments = TreeMap<String, String>()
        reader.beginObject()
        while (reader.hasNext()) {
            val name = reader.nextName()
            if (!TextUtils.isEmpty(name)) {
                arguments[name] = reader.nextString()
            }
        }
        reader.endObject()
        if (TextUtils.isEmpty(arguments[ID_FIELD_NAME])
                || TextUtils.isEmpty(arguments[LOCALE_FIELD_NAME])
                || TextUtils.isEmpty(arguments[DESCRIPTION_FIELD_NAME])
                || TextUtils.isEmpty(arguments[UPDATE_FIELD_NAME])
                || TextUtils.isEmpty(arguments[FILESIZE_FIELD_NAME])
                || TextUtils.isEmpty(arguments[CHECKSUM_FIELD_NAME])
                || TextUtils.isEmpty(arguments[REMOTE_FILENAME_FIELD_NAME])
                || TextUtils.isEmpty(arguments[VERSION_FIELD_NAME])
                || TextUtils.isEmpty(arguments[FORMATVERSION_FIELD_NAME])) {
            throw BadFormatException(arguments.toString())
        }
        // TODO: need to find out whether it's bulk or update
// The null argument is the local file name, which is not known at this time and will
// be decided later.
        return WordListMetadata(
                arguments[ID_FIELD_NAME],
                MetadataDbHelper.Companion.TYPE_BULK,
                arguments[DESCRIPTION_FIELD_NAME], arguments[UPDATE_FIELD_NAME]!!.toLong(), arguments[FILESIZE_FIELD_NAME]!!.toLong(),
                arguments[RAW_CHECKSUM_FIELD_NAME],
                arguments[CHECKSUM_FIELD_NAME],
                MetadataDbHelper.Companion.DICTIONARY_RETRY_THRESHOLD /* retryCount */,
                null,
                arguments[REMOTE_FILENAME_FIELD_NAME], arguments[VERSION_FIELD_NAME]!!.toInt(), arguments[FORMATVERSION_FIELD_NAME]!!.toInt(),
                0, arguments[LOCALE_FIELD_NAME])
    }

    /**
     * Parses metadata in the JSON format.
     * @param input a stream reader expected to contain JSON formatted metadata.
     * @return dictionary metadata, as an array of WordListMetadata objects.
     * @throws IOException if the underlying reader throws IOException during reading.
     * @throws BadFormatException if the data was not in the expected format.
     */
    @Throws(IOException::class, BadFormatException::class)
    fun parseMetadata(input: InputStreamReader?): List<WordListMetadata> {
        val reader = JsonReader(input)
        val readInfo = ArrayList<WordListMetadata>()
        reader.beginArray()
        while (reader.hasNext()) {
            val thisMetadata = parseOneWordList(reader)
            if (!TextUtils.isEmpty(thisMetadata.mLocale)) readInfo.add(thisMetadata)
        }
        return Collections.unmodifiableList(readInfo)
    }
}