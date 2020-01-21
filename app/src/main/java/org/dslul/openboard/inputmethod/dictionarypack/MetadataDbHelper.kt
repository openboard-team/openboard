package org.dslul.openboard.inputmethod.dictionarypack

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteOpenHelper
import android.text.TextUtils
import android.util.Log
import org.dslul.openboard.inputmethod.latin.R
import org.dslul.openboard.inputmethod.latin.utils.DebugLogUtils
import java.io.File
import java.util.*

/**
 * Various helper functions for the state database
 */
class MetadataDbHelper private constructor(private val mContext: Context?, private val mClientId: String) : SQLiteOpenHelper(mContext,
        METADATA_DATABASE_NAME_STEM + if (TextUtils.isEmpty(mClientId)) "" else ".$mClientId",
        null, CURRENT_METADATA_DATABASE_VERSION) {
    private fun createClientTable(db: SQLiteDatabase) { // The clients table only exists in the primary db, the one that has an empty client id
        if (!TextUtils.isEmpty(mClientId)) return
        db.execSQL(METADATA_CREATE_CLIENT_TABLE)
        val defaultMetadataUri = mContext!!.getString(R.string.default_metadata_uri)
        if (!TextUtils.isEmpty(defaultMetadataUri)) {
            val defaultMetadataValues = ContentValues()
            defaultMetadataValues.put(CLIENT_CLIENT_ID_COLUMN, "")
            defaultMetadataValues.put(CLIENT_METADATA_URI_COLUMN, defaultMetadataUri)
            db.insert(CLIENT_TABLE_NAME, null, defaultMetadataValues)
        }
    }

    /**
     * Create the table and populate it with the resources found inside the apk.
     *
     * @see SQLiteOpenHelper.onCreate
     * @param db the database to create and populate.
     */
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(METADATA_TABLE_CREATE)
        createClientTable(db)
    }

    /**
     * Upgrade the database. Upgrade from version 3 is supported.
     * Version 3 has a DB named METADATA_DATABASE_NAME_STEM containing a table METADATA_TABLE_NAME.
     * Version 6 and above has a DB named METADATA_DATABASE_NAME_STEM containing a
     * table CLIENT_TABLE_NAME, and for each client a table called METADATA_TABLE_STEM + "." + the
     * name of the client and contains a table METADATA_TABLE_NAME.
     * For schemas, see the above create statements. The schemas have never changed so far.
     *
     * This method is called by the framework. See [SQLiteOpenHelper.onUpgrade]
     * @param db The database we are upgrading
     * @param oldVersion The old database version (the one on the disk)
     * @param newVersion The new database version as supplied to the constructor of SQLiteOpenHelper
     */
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (METADATA_DATABASE_INITIAL_VERSION == oldVersion && METADATA_DATABASE_VERSION_WITH_CLIENTID <= newVersion && CURRENT_METADATA_DATABASE_VERSION >= newVersion) { // Upgrade from version METADATA_DATABASE_INITIAL_VERSION to version
// METADATA_DATABASE_VERSION_WITH_CLIENT_ID
// Only the default database should contain the client table, so we test for mClientId.
            if (TextUtils.isEmpty(mClientId)) { // Anyway in version 3 only the default table existed so the emptiness
// test should always be true, but better check to be sure.
                createClientTable(db)
            }
        } else if (METADATA_DATABASE_VERSION_WITH_CLIENTID < newVersion
                && CURRENT_METADATA_DATABASE_VERSION >= newVersion) { // Here we drop the client table, so that all clients send us their information again.
// The client table contains the URL to hit to update the available dictionaries list,
// but the info about the dictionaries themselves is stored in the table called
// METADATA_TABLE_NAME and we want to keep it, so we only drop the client table.
            db.execSQL("DROP TABLE IF EXISTS $CLIENT_TABLE_NAME")
            // Only the default database should contain the client table, so we test for mClientId.
            if (TextUtils.isEmpty(mClientId)) {
                createClientTable(db)
            }
        } else { // If we're not in the above case, either we are upgrading from an earlier versionCode
// and we should wipe the database, or we are handling a version we never heard about
// (can only be a bug) so it's safer to wipe the database.
            db.execSQL("DROP TABLE IF EXISTS $METADATA_TABLE_NAME")
            db.execSQL("DROP TABLE IF EXISTS $CLIENT_TABLE_NAME")
            onCreate(db)
        }
        // A rawChecksum column that did not exist in the previous versions was added that
// corresponds to the md5 checksum of the file after decompression/decryption. This is to
// strengthen the system against corrupted dictionary files.
// The most secure way to upgrade a database is to just test for the column presence, and
// add it if it's not there.
        addRawChecksumColumnUnlessPresent(db)
        // A retry count column that did not exist in the previous versions was added that
// corresponds to the number of download & installation attempts that have been made
// in order to strengthen the system recovery from corrupted dictionary files.
// The most secure way to upgrade a database is to just test for the column presence, and
// add it if it's not there.
        addRetryCountColumnUnlessPresent(db)
    }

    /**
     * Downgrade the database. This drops and recreates the table in all cases.
     */
    override fun onDowngrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) { // No matter what the numerical values of oldVersion and newVersion are, we know this
// is a downgrade (newVersion < oldVersion). There is no way to know what the future
// databases will look like, but we know it's extremely likely that it's okay to just
// drop the tables and start from scratch. Hence, we ignore the versions and just wipe
// everything we want to use.
        if (oldVersion <= newVersion) {
            Log.e(TAG, "onDowngrade database but new version is higher? " + oldVersion + " <= "
                    + newVersion)
        }
        db.execSQL("DROP TABLE IF EXISTS $METADATA_TABLE_NAME")
        db.execSQL("DROP TABLE IF EXISTS $CLIENT_TABLE_NAME")
        onCreate(db)
    }

    companion object {
        private val TAG = MetadataDbHelper::class.java.simpleName
        // This was the initial release version of the database. It should never be
// changed going forward.
        private const val METADATA_DATABASE_INITIAL_VERSION = 3
        // This is the first released version of the database that implements CLIENTID. It is
// used to identify the versions for upgrades. This should never change going forward.
        private const val METADATA_DATABASE_VERSION_WITH_CLIENTID = 6
        // The current database version.
// This MUST be increased every time the dictionary pack metadata URL changes.
        private const val CURRENT_METADATA_DATABASE_VERSION = 16
        private const val NOT_A_DOWNLOAD_ID: Long = -1
        // The number of retries allowed when attempting to download a broken dictionary.
        const val DICTIONARY_RETRY_THRESHOLD = 2
        const val METADATA_TABLE_NAME = "pendingUpdates"
        const val CLIENT_TABLE_NAME = "clients"
        const val PENDINGID_COLUMN = "pendingid" // Download Manager ID
        const val TYPE_COLUMN = "type"
        const val STATUS_COLUMN = "status"
        const val LOCALE_COLUMN = "locale"
        const val WORDLISTID_COLUMN = "id"
        const val DESCRIPTION_COLUMN = "description"
        const val LOCAL_FILENAME_COLUMN = "filename"
        const val REMOTE_FILENAME_COLUMN = "url"
        const val DATE_COLUMN = "date"
        const val CHECKSUM_COLUMN = "checksum"
        const val FILESIZE_COLUMN = "filesize"
        const val VERSION_COLUMN = "version"
        const val FORMATVERSION_COLUMN = "formatversion"
        const val FLAGS_COLUMN = "flags"
        const val RAW_CHECKSUM_COLUMN = "rawChecksum"
        const val RETRY_COUNT_COLUMN = "remainingRetries"
        const val COLUMN_COUNT = 15
        private const val CLIENT_CLIENT_ID_COLUMN = "clientid"
        private const val CLIENT_METADATA_URI_COLUMN = "uri"
        private const val CLIENT_METADATA_ADDITIONAL_ID_COLUMN = "additionalid"
        private const val CLIENT_LAST_UPDATE_DATE_COLUMN = "lastupdate"
        private const val CLIENT_PENDINGID_COLUMN = "pendingid" // Download Manager ID
        const val METADATA_DATABASE_NAME_STEM = "pendingUpdates"
        const val METADATA_UPDATE_DESCRIPTION = "metadata"
        const val DICTIONARIES_ASSETS_PATH = "dictionaries"
        // Statuses, for storing in the STATUS_COLUMN
// IMPORTANT: The following are used as index arrays in ../WordListPreference
// Do not change their values without updating the matched code.
// Unknown status: this should never happen.
        const val STATUS_UNKNOWN = 0
        // Available: this word list is available, but it is not downloaded (not downloading), because
// it is set not to be used.
        const val STATUS_AVAILABLE = 1
        // Downloading: this word list is being downloaded.
        const val STATUS_DOWNLOADING = 2
        // Installed: this word list is installed and usable.
        const val STATUS_INSTALLED = 3
        // Disabled: this word list is installed, but has been disabled by the user.
        const val STATUS_DISABLED = 4
        // Deleting: the user marked this word list to be deleted, but it has not been yet because
// Latin IME is not up yet.
        const val STATUS_DELETING = 5
        // Retry: dictionary got corrupted, so an attempt must be done to download & install it again.
        const val STATUS_RETRYING = 6
        // Types, for storing in the TYPE_COLUMN
// This is metadata about what is available.
        const val TYPE_METADATA = 1
        // This is a bulk file. It should replace older files.
        const val TYPE_BULK = 2
        // This is an incremental update, expected to be small, and meaningless on its own.
        const val TYPE_UPDATE = 3
        private const val METADATA_TABLE_CREATE = ("CREATE TABLE " + METADATA_TABLE_NAME + " ("
                + PENDINGID_COLUMN + " INTEGER, "
                + TYPE_COLUMN + " INTEGER, "
                + STATUS_COLUMN + " INTEGER, "
                + WORDLISTID_COLUMN + " TEXT, "
                + LOCALE_COLUMN + " TEXT, "
                + DESCRIPTION_COLUMN + " TEXT, "
                + LOCAL_FILENAME_COLUMN + " TEXT, "
                + REMOTE_FILENAME_COLUMN + " TEXT, "
                + DATE_COLUMN + " INTEGER, "
                + CHECKSUM_COLUMN + " TEXT, "
                + FILESIZE_COLUMN + " INTEGER, "
                + VERSION_COLUMN + " INTEGER,"
                + FORMATVERSION_COLUMN + " INTEGER, "
                + FLAGS_COLUMN + " INTEGER, "
                + RAW_CHECKSUM_COLUMN + " TEXT,"
                + RETRY_COUNT_COLUMN + " INTEGER, "
                + "PRIMARY KEY (" + WORDLISTID_COLUMN + "," + VERSION_COLUMN + "));")
        private const val METADATA_CREATE_CLIENT_TABLE = ("CREATE TABLE IF NOT EXISTS " + CLIENT_TABLE_NAME + " ("
                + CLIENT_CLIENT_ID_COLUMN + " TEXT, "
                + CLIENT_METADATA_URI_COLUMN + " TEXT, "
                + CLIENT_METADATA_ADDITIONAL_ID_COLUMN + " TEXT, "
                + CLIENT_LAST_UPDATE_DATE_COLUMN + " INTEGER NOT NULL DEFAULT 0, "
                + CLIENT_PENDINGID_COLUMN + " INTEGER, "
                + FLAGS_COLUMN + " INTEGER, "
                + "PRIMARY KEY (" + CLIENT_CLIENT_ID_COLUMN + "));")
        // List of all metadata table columns.
        val METADATA_TABLE_COLUMNS = arrayOf(PENDINGID_COLUMN, TYPE_COLUMN,
                STATUS_COLUMN, WORDLISTID_COLUMN, LOCALE_COLUMN, DESCRIPTION_COLUMN,
                LOCAL_FILENAME_COLUMN, REMOTE_FILENAME_COLUMN, DATE_COLUMN, CHECKSUM_COLUMN,
                FILESIZE_COLUMN, VERSION_COLUMN, FORMATVERSION_COLUMN, FLAGS_COLUMN,
                RAW_CHECKSUM_COLUMN, RETRY_COUNT_COLUMN)
        // List of all client table columns.
        val CLIENT_TABLE_COLUMNS = arrayOf(CLIENT_CLIENT_ID_COLUMN,
                CLIENT_METADATA_URI_COLUMN, CLIENT_PENDINGID_COLUMN, FLAGS_COLUMN)
        // List of public columns returned to clients. Everything that is not in this list is
// private and implementation-dependent.
        val DICTIONARIES_LIST_PUBLIC_COLUMNS = arrayOf(STATUS_COLUMN, WORDLISTID_COLUMN,
                LOCALE_COLUMN, DESCRIPTION_COLUMN, DATE_COLUMN, FILESIZE_COLUMN, VERSION_COLUMN)
        // This class exhibits a singleton-like behavior by client ID, so it is getInstance'd
// and has a private c'tor.
        private var sInstanceMap: TreeMap<String, MetadataDbHelper>? = null

        @Synchronized
        fun getInstance(context: Context?,
                        clientIdOrNull: String?): MetadataDbHelper { // As a backward compatibility feature, null can be passed here to retrieve the "default"
// database. Before multi-client support, the dictionary packed used only one database
// and would not be able to handle several dictionary sets. Passing null here retrieves
// this legacy database. New clients should make sure to always pass a client ID so as
// to avoid conflicts.
            val clientId = clientIdOrNull ?: ""
            if (null == sInstanceMap) sInstanceMap = TreeMap()
            var helper = sInstanceMap!![clientId]
            if (null == helper) {
                helper = MetadataDbHelper(context, clientId)
                sInstanceMap!![clientId] = helper
            }
            return helper
        }

        /**
         * Get the database itself. This always returns the same object for any client ID. If the
         * client ID is null, a default database is returned for backward compatibility. Don't
         * pass null for new calls.
         *
         * @param context the context to create the database from. This is ignored after the first call.
         * @param clientId the client id to retrieve the database of. null for default (deprecated)
         * @return the database.
         */
        fun getDb(context: Context?, clientId: String?): SQLiteDatabase {
            return getInstance(context, clientId).writableDatabase
        }

        private fun addRawChecksumColumnUnlessPresent(db: SQLiteDatabase) {
            try {
                db.execSQL("SELECT " + RAW_CHECKSUM_COLUMN + " FROM "
                        + METADATA_TABLE_NAME + " LIMIT 0;")
            } catch (e: SQLiteException) {
                Log.i(TAG, "No $RAW_CHECKSUM_COLUMN column : creating it")
                db.execSQL("ALTER TABLE " + METADATA_TABLE_NAME + " ADD COLUMN "
                        + RAW_CHECKSUM_COLUMN + " TEXT;")
            }
        }

        private fun addRetryCountColumnUnlessPresent(db: SQLiteDatabase) {
            try {
                db.execSQL("SELECT " + RETRY_COUNT_COLUMN + " FROM "
                        + METADATA_TABLE_NAME + " LIMIT 0;")
            } catch (e: SQLiteException) {
                Log.i(TAG, "No $RETRY_COUNT_COLUMN column : creating it")
                db.execSQL("ALTER TABLE " + METADATA_TABLE_NAME + " ADD COLUMN "
                        + RETRY_COUNT_COLUMN + " INTEGER DEFAULT " + DICTIONARY_RETRY_THRESHOLD + ";")
            }
        }

        /**
         * Given a client ID, returns whether this client exists.
         *
         * @param context a context to open the database
         * @param clientId the client ID to check
         * @return true if the client is known, false otherwise
         */
        fun isClientKnown(context: Context?, clientId: String?): Boolean { // If the client is known, they'll have a non-null metadata URI. An empty string is
// allowed as a metadata URI, if the client doesn't want any updates to happen.
            return null != getMetadataUriAsString(context, clientId)
        }

        /**
         * Returns the metadata URI as a string.
         *
         * If the client is not known, this will return null. If it is known, it will return
         * the URI as a string. Note that the empty string is a valid value.
         *
         * @param context a context instance to open the database on
         * @param clientId the ID of the client we want the metadata URI of
         * @return the string representation of the URI
         */
        fun getMetadataUriAsString(context: Context?, clientId: String?): String? {
            val defaultDb = getDb(context, null)
            val cursor = defaultDb.query(CLIENT_TABLE_NAME, arrayOf(CLIENT_METADATA_URI_COLUMN),
                    "$CLIENT_CLIENT_ID_COLUMN = ?", arrayOf(clientId),
                    null, null, null, null)
            return try {
                if (!cursor.moveToFirst()) null else MetadataUriGetter.getUri(context, cursor.getString(0))
            } finally {
                cursor.close()
            }
        }

        /**
         * Update the last metadata update time for all clients using a particular URI.
         *
         * This method searches for all clients using a particular URI and updates the last
         * update time for this client.
         * The current time is used as the latest update time. This saved date will be what
         * is returned henceforth by [.getLastUpdateDateForClient],
         * until this method is called again.
         *
         * @param context a context instance to open the database on
         * @param uri the metadata URI we just downloaded
         */
        fun saveLastUpdateTimeOfUri(context: Context?, uri: String) {
            PrivateLog.log("Save last update time of URI : " + uri + " " + System.currentTimeMillis())
            val values = ContentValues()
            values.put(CLIENT_LAST_UPDATE_DATE_COLUMN, System.currentTimeMillis())
            val defaultDb = getDb(context, null)
            val cursor = queryClientIds(context)
            try {
                if (!cursor.moveToFirst()) return
                do {
                    val clientId = cursor.getString(0)
                    val metadataUri = getMetadataUriAsString(context, clientId)
                    if (metadataUri == uri) {
                        defaultDb.update(CLIENT_TABLE_NAME, values,
                                "$CLIENT_CLIENT_ID_COLUMN = ?", arrayOf(clientId))
                    }
                } while (cursor.moveToNext())
            } finally {
                cursor.close()
            }
        }

        /**
         * Retrieves the last date at which we updated the metadata for this client.
         *
         * The returned date is in milliseconds from the EPOCH; this is the same unit as
         * returned by [System.currentTimeMillis].
         *
         * @param context a context instance to open the database on
         * @param clientId the client ID to get the latest update date of
         * @return the last date at which this client was updated, as a long.
         */
        fun getLastUpdateDateForClient(context: Context?, clientId: String?): Long {
            val defaultDb = getDb(context, null)
            val cursor = defaultDb.query(CLIENT_TABLE_NAME, arrayOf(CLIENT_LAST_UPDATE_DATE_COLUMN),
                    "$CLIENT_CLIENT_ID_COLUMN = ?", arrayOf(clientId ?: ""),
                    null, null, null, null)
            return try {
                if (!cursor.moveToFirst()) 0 else cursor.getLong(0)
                // Only one column, return it
            } finally {
                cursor.close()
            }
        }

        fun getOldestUpdateTime(context: Context?): Long {
            val defaultDb = getDb(context, null)
            val cursor = defaultDb.query(CLIENT_TABLE_NAME, arrayOf(CLIENT_LAST_UPDATE_DATE_COLUMN),
                    null, null, null, null, null)
            return try {
                if (!cursor.moveToFirst()) return 0
                val columnIndex = 0 // Only one column queried
                // Initialize the earliestTime to the largest possible value.
                var earliestTime = Long.MAX_VALUE // Almost 300 million years in the future
                do {
                    val thisTime = cursor.getLong(columnIndex)
                    earliestTime = Math.min(thisTime, earliestTime)
                } while (cursor.moveToNext())
                earliestTime
            } finally {
                cursor.close()
            }
        }

        /**
         * Helper method to make content values to write into the database.
         * @return content values with all the arguments put with the right column names.
         */
        fun makeContentValues(pendingId: Int, type: Int,
                              status: Int, wordlistId: String?, locale: String?,
                              description: String?, filename: String?, url: String?, date: Long,
                              rawChecksum: String?, checksum: String?, retryCount: Int,
                              filesize: Long, version: Int, formatVersion: Int): ContentValues {
            val result = ContentValues(COLUMN_COUNT)
            result.put(PENDINGID_COLUMN, pendingId)
            result.put(TYPE_COLUMN, type)
            result.put(WORDLISTID_COLUMN, wordlistId)
            result.put(STATUS_COLUMN, status)
            result.put(LOCALE_COLUMN, locale)
            result.put(DESCRIPTION_COLUMN, description)
            result.put(LOCAL_FILENAME_COLUMN, filename)
            result.put(REMOTE_FILENAME_COLUMN, url)
            result.put(DATE_COLUMN, date)
            result.put(RAW_CHECKSUM_COLUMN, rawChecksum)
            result.put(RETRY_COUNT_COLUMN, retryCount)
            result.put(CHECKSUM_COLUMN, checksum)
            result.put(FILESIZE_COLUMN, filesize)
            result.put(VERSION_COLUMN, version)
            result.put(FORMATVERSION_COLUMN, formatVersion)
            result.put(FLAGS_COLUMN, 0)
            return result
        }

        /**
         * Helper method to fill in an incomplete ContentValues with default values.
         * A wordlist ID and a locale are required, otherwise BadFormatException is thrown.
         * @return the same object that was passed in, completed with default values.
         */
        @Throws(BadFormatException::class)
        fun completeWithDefaultValues(result: ContentValues): ContentValues {
            if (null == result[WORDLISTID_COLUMN] || null == result[LOCALE_COLUMN]) {
                throw BadFormatException()
            }
            // 0 for the pending id, because there is none
            if (null == result[PENDINGID_COLUMN]) result.put(PENDINGID_COLUMN, 0)
            // This is a binary blob of a dictionary
            if (null == result[TYPE_COLUMN]) result.put(TYPE_COLUMN, TYPE_BULK)
            // This word list is unknown, but it's present, else we wouldn't be here, so INSTALLED
            if (null == result[STATUS_COLUMN]) result.put(STATUS_COLUMN, STATUS_INSTALLED)
            // No description unless specified, because we can't guess it
            if (null == result[DESCRIPTION_COLUMN]) result.put(DESCRIPTION_COLUMN, "")
            // File name - this is an asset, so it works as an already deleted file.
//     hence, we need to supply a non-existent file name. Anything will
//     do as long as it returns false when tested with File#exist(), and
//     the empty string does not, so it's set to "_".
            if (null == result[LOCAL_FILENAME_COLUMN]) result.put(LOCAL_FILENAME_COLUMN, "_")
            // No remote file name : this can't be downloaded. Unless specified.
            if (null == result[REMOTE_FILENAME_COLUMN]) result.put(REMOTE_FILENAME_COLUMN, "")
            // 0 for the update date : 1970/1/1. Unless specified.
            if (null == result[DATE_COLUMN]) result.put(DATE_COLUMN, 0)
            // Raw checksum unknown unless specified
            if (null == result[RAW_CHECKSUM_COLUMN]) result.put(RAW_CHECKSUM_COLUMN, "")
            // Retry column 0 unless specified
            if (null == result[RETRY_COUNT_COLUMN]) result.put(RETRY_COUNT_COLUMN,
                    DICTIONARY_RETRY_THRESHOLD)
            // Checksum unknown unless specified
            if (null == result[CHECKSUM_COLUMN]) result.put(CHECKSUM_COLUMN, "")
            // No filesize unless specified
            if (null == result[FILESIZE_COLUMN]) result.put(FILESIZE_COLUMN, 0)
            // Smallest possible version unless specified
            if (null == result[VERSION_COLUMN]) result.put(VERSION_COLUMN, 1)
            // No flags unless specified
            if (null == result[FLAGS_COLUMN]) result.put(FLAGS_COLUMN, 0)
            return result
        }

        /**
         * Reads a column in a Cursor as a String and stores it in a ContentValues object.
         * @param result the ContentValues object to store the result in.
         * @param cursor the Cursor to read the column from.
         * @param columnId the column ID to read.
         */
        private fun putStringResult(result: ContentValues, cursor: Cursor, columnId: String) {
            result.put(columnId, cursor.getString(cursor.getColumnIndex(columnId)))
        }

        /**
         * Reads a column in a Cursor as an int and stores it in a ContentValues object.
         * @param result the ContentValues object to store the result in.
         * @param cursor the Cursor to read the column from.
         * @param columnId the column ID to read.
         */
        private fun putIntResult(result: ContentValues, cursor: Cursor, columnId: String) {
            result.put(columnId, cursor.getInt(cursor.getColumnIndex(columnId)))
        }

        private fun getFirstLineAsContentValues(cursor: Cursor): ContentValues? {
            val result: ContentValues?
            if (cursor.moveToFirst()) {
                result = ContentValues(COLUMN_COUNT)
                putIntResult(result, cursor, PENDINGID_COLUMN)
                putIntResult(result, cursor, TYPE_COLUMN)
                putIntResult(result, cursor, STATUS_COLUMN)
                putStringResult(result, cursor, WORDLISTID_COLUMN)
                putStringResult(result, cursor, LOCALE_COLUMN)
                putStringResult(result, cursor, DESCRIPTION_COLUMN)
                putStringResult(result, cursor, LOCAL_FILENAME_COLUMN)
                putStringResult(result, cursor, REMOTE_FILENAME_COLUMN)
                putIntResult(result, cursor, DATE_COLUMN)
                putStringResult(result, cursor, RAW_CHECKSUM_COLUMN)
                putStringResult(result, cursor, CHECKSUM_COLUMN)
                putIntResult(result, cursor, RETRY_COUNT_COLUMN)
                putIntResult(result, cursor, FILESIZE_COLUMN)
                putIntResult(result, cursor, VERSION_COLUMN)
                putIntResult(result, cursor, FORMATVERSION_COLUMN)
                putIntResult(result, cursor, FLAGS_COLUMN)
                if (cursor.moveToNext()) { // TODO: print the second level of the stack to the log so that we know
// in which code path the error happened
                    Log.e(TAG, "Several SQL results when we expected only one!")
                }
            } else {
                result = null
            }
            return result
        }

        /**
         * Gets the info about as specific download, indexed by its DownloadManager ID.
         * @param db the database to get the information from.
         * @param id the DownloadManager id.
         * @return metadata about this download. This returns all columns in the database.
         */
        fun getContentValuesByPendingId(db: SQLiteDatabase,
                                        id: Long): ContentValues? {
            val cursor = db.query(METADATA_TABLE_NAME,
                    METADATA_TABLE_COLUMNS,
                    "$PENDINGID_COLUMN= ?", arrayOf(java.lang.Long.toString(id)),
                    null, null, null)
                    ?: return null
            return try { // There should never be more than one result. If because of some bug there are,
// returning only one result is the right thing to do, because we couldn't handle
// several anyway and we should still handle one.
                getFirstLineAsContentValues(cursor)
            } finally {
                cursor.close()
            }
        }

        /**
         * Gets the info about an installed OR deleting word list with a specified id.
         *
         * Basically, this is the word list that we want to return to Android Keyboard when
         * it asks for a specific id.
         *
         * @param db the database to get the information from.
         * @param id the word list ID.
         * @return the metadata about this word list.
         */
        fun getInstalledOrDeletingWordListContentValuesByWordListId(
                db: SQLiteDatabase, id: String?): ContentValues? {
            val cursor = db.query(METADATA_TABLE_NAME,
                    METADATA_TABLE_COLUMNS,
                    "$WORDLISTID_COLUMN=? AND ($STATUS_COLUMN=? OR $STATUS_COLUMN=?)", arrayOf(id, Integer.toString(STATUS_INSTALLED),
                    Integer.toString(STATUS_DELETING)),
                    null, null, null)
                    ?: return null
            return try { // There should only be one result, but if there are several, we can't tell which
// is the best, so we just return the first one.
                getFirstLineAsContentValues(cursor)
            } finally {
                cursor.close()
            }
        }

        /**
         * Gets the info about a specific word list.
         *
         * @param db the database to get the information from.
         * @param id the word list ID.
         * @param version the word list version.
         * @return the metadata about this word list.
         */
        fun getContentValuesByWordListId(db: SQLiteDatabase,
                                         id: String?, version: Int): ContentValues? {
            val cursor = db.query(METADATA_TABLE_NAME,
                    METADATA_TABLE_COLUMNS,
                    WORDLISTID_COLUMN + "= ? AND " + VERSION_COLUMN + "= ? AND "
                            + FORMATVERSION_COLUMN + "<= ?", arrayOf(id,
                    Integer.toString(version),
                    Integer.toString(version)
            ),
                    null /* groupBy */,
                    null /* having */,
                    "$FORMATVERSION_COLUMN DESC" /* orderBy */)
                    ?: return null
            return try { // This is a lookup by primary key, so there can't be more than one result.
                getFirstLineAsContentValues(cursor)
            } finally {
                cursor.close()
            }
        }

        /**
         * Gets the info about the latest word list with an id.
         *
         * @param db the database to get the information from.
         * @param id the word list ID.
         * @return the metadata about the word list with this id and the latest version number.
         */
        fun getContentValuesOfLatestAvailableWordlistById(
                db: SQLiteDatabase, id: String): ContentValues? {
            val cursor = db.query(METADATA_TABLE_NAME,
                    METADATA_TABLE_COLUMNS,
                    "$WORDLISTID_COLUMN= ?", arrayOf(id), null, null, "$VERSION_COLUMN DESC", "1")
                    ?: return null
            return try { // Return the first result from the list of results.
                getFirstLineAsContentValues(cursor)
            } finally {
                cursor.close()
            }
        }

        /**
         * Gets the current metadata about INSTALLED, AVAILABLE or DELETING dictionaries.
         *
         * This odd method is tailored to the needs of
         * DictionaryProvider#getDictionaryWordListsForContentUri, which needs the word list if
         * it is:
         * - INSTALLED: this should be returned to LatinIME if the file is still inside the dictionary
         * pack, so that it can be copied. If the file is not there, it's been copied already and should
         * not be returned, so getDictionaryWordListsForContentUri takes care of this.
         * - DELETING: this should be returned to LatinIME so that it can actually delete the file.
         * - AVAILABLE: this should not be returned, but should be checked for auto-installation.
         *
         * @param context the context for getting the database.
         * @param clientId the client id for retrieving the database. null for default (deprecated)
         * @return a cursor with metadata about usable dictionaries.
         */
        fun queryInstalledOrDeletingOrAvailableDictionaryMetadata(
                context: Context?, clientId: String?): Cursor { // If clientId is null, we get the defaut DB (see #getInstance() for more about this)
            return getDb(context, clientId).query(METADATA_TABLE_NAME,
                    METADATA_TABLE_COLUMNS,
                    "$STATUS_COLUMN = ? OR $STATUS_COLUMN = ? OR $STATUS_COLUMN = ?", arrayOf(Integer.toString(STATUS_INSTALLED),
                    Integer.toString(STATUS_DELETING),
                    Integer.toString(STATUS_AVAILABLE)),
                    null, null, LOCALE_COLUMN)
        }

        /**
         * Gets the current metadata about all dictionaries.
         *
         * This will retrieve the metadata about all dictionaries, including
         * older files, or files not yet downloaded.
         *
         * @param context the context for getting the database.
         * @param clientId the client id for retrieving the database. null for default (deprecated)
         * @return a cursor with metadata about usable dictionaries.
         */
        fun queryCurrentMetadata(context: Context?, clientId: String?): Cursor { // If clientId is null, we get the defaut DB (see #getInstance() for more about this)
            return getDb(context, clientId).query(METADATA_TABLE_NAME,
                    METADATA_TABLE_COLUMNS, null, null, null, null, LOCALE_COLUMN)
        }

        /**
         * Gets the list of all dictionaries known to the dictionary provider, with only public columns.
         *
         * This will retrieve information about all known dictionaries, and their status. As such,
         * it will also return information about dictionaries on the server that have not been
         * downloaded yet, but may be requested.
         * This only returns public columns. It does not populate internal columns in the returned
         * cursor.
         * The value returned by this method is intended to be good to be returned directly for a
         * request of the list of dictionaries by a client.
         *
         * @param context the context to read the database from.
         * @param clientId the client id for retrieving the database. null for default (deprecated)
         * @return a cursor that lists all available dictionaries and their metadata.
         */
        fun queryDictionaries(context: Context?, clientId: String?): Cursor { // If clientId is null, we get the defaut DB (see #getInstance() for more about this)
            return getDb(context, clientId).query(METADATA_TABLE_NAME,
                    DICTIONARIES_LIST_PUBLIC_COLUMNS,  // Filter out empty locales so as not to return auxiliary data, like a
// data line for downloading metadata:
                    "$LOCALE_COLUMN != ?", arrayOf(""),  // TODO: Reinstate the following code for bulk, then implement partial updates
/*                MetadataDbHelper.TYPE_COLUMN + " = ?",
                new String[] { Integer.toString(MetadataDbHelper.TYPE_BULK) }, */
                    null, null, LOCALE_COLUMN)
        }

        /**
         * Deletes all data associated with a client.
         *
         * @param context the context for opening the database
         * @param clientId the ID of the client to delete.
         * @return true if the client was successfully deleted, false otherwise.
         */
        fun deleteClient(context: Context?, clientId: String?): Boolean { // Remove all metadata associated with this client
            val db = getDb(context, clientId)
            db.execSQL("DROP TABLE IF EXISTS $METADATA_TABLE_NAME")
            db.execSQL(METADATA_TABLE_CREATE)
            // Remove this client's entry in the clients table
            val defaultDb = getDb(context, "")
            return 0 != defaultDb.delete(CLIENT_TABLE_NAME,
                    "$CLIENT_CLIENT_ID_COLUMN = ?", arrayOf(clientId))
        }

        /**
         * Updates information relative to a specific client.
         *
         * Updatable information includes the metadata URI and the additional ID column. It may be
         * expanded in the future.
         * The passed values must include a client ID in the key CLIENT_CLIENT_ID_COLUMN, and it must
         * be equal to the string passed as an argument for clientId. It may not be empty.
         * The passed values must also include a non-null metadata URI in the
         * CLIENT_METADATA_URI_COLUMN column, as well as a non-null additional ID in the
         * CLIENT_METADATA_ADDITIONAL_ID_COLUMN. Both these strings may be empty.
         * If any of the above is not complied with, this function returns without updating data.
         *
         * @param context the context, to open the database
         * @param clientId the ID of the client to update
         * @param values the values to update. Must conform to the protocol (see above)
         */
        fun updateClientInfo(context: Context?, clientId: String?,
                             values: ContentValues) { // Sanity check the content values
            val valuesClientId = values.getAsString(CLIENT_CLIENT_ID_COLUMN)
            val valuesMetadataUri = values.getAsString(CLIENT_METADATA_URI_COLUMN)
            val valuesMetadataAdditionalId = values.getAsString(CLIENT_METADATA_ADDITIONAL_ID_COLUMN)
            // Empty string is a valid client ID, but external apps may not configure it, so disallow
// both null and empty string.
// Empty string is a valid metadata URI if the client does not want updates, so allow
// empty string but disallow null.
// Empty string is a valid additional ID so allow empty string but disallow null.
            if (TextUtils.isEmpty(valuesClientId) || null == valuesMetadataUri || null == valuesMetadataAdditionalId) { // We need all these columns to be filled in
                DebugLogUtils.l("Missing parameter for updateClientInfo")
                return
            }
            if (clientId != valuesClientId) { // Mismatch! The client violates the protocol.
                DebugLogUtils.l("Received an updateClientInfo request for ", clientId,
                        " but the values " + "contain a different ID : ", valuesClientId)
                return
            }
            // Default value for a pending ID is NOT_AN_ID
            val defaultDb = getDb(context, "")
            if (-1L == defaultDb.insert(CLIENT_TABLE_NAME, null, values)) {
                defaultDb.update(CLIENT_TABLE_NAME, values,
                        "$CLIENT_CLIENT_ID_COLUMN = ?", arrayOf(clientId))
            }
        }

        /**
         * Retrieves the list of existing client IDs.
         * @param context the context to open the database
         * @return a cursor containing only one column, and one client ID per line.
         */
        fun queryClientIds(context: Context?): Cursor {
            return getDb(context, null).query(CLIENT_TABLE_NAME, arrayOf(CLIENT_CLIENT_ID_COLUMN), null, null, null, null, null)
        }

        /**
         * Marks a downloading entry as having successfully downloaded and being installed.
         *
         * The metadata database contains information about ongoing processes, typically ongoing
         * downloads. This marks such an entry as having finished and having installed successfully,
         * so it becomes INSTALLED.
         *
         * @param db the metadata database.
         * @param r content values about the entry to mark as processed.
         */
        fun markEntryAsFinishedDownloadingAndInstalled(db: SQLiteDatabase,
                                                       r: ContentValues) {
            when (r.getAsInteger(TYPE_COLUMN)) {
                TYPE_BULK -> {
                    DebugLogUtils.l("Ended processing a wordlist")
                    // Updating a bulk word list is a three-step operation:
// - Add the new entry to the table
// - Remove the old entry from the table
// - Erase the old file
// We start by gathering the names of the files we should delete.
                    val filenames: MutableList<String> = LinkedList()
                    val c = db.query(METADATA_TABLE_NAME, arrayOf(LOCAL_FILENAME_COLUMN),
                            LOCALE_COLUMN + " = ? AND " +
                                    WORDLISTID_COLUMN + " = ? AND " + STATUS_COLUMN + " = ?", arrayOf(r.getAsString(LOCALE_COLUMN),
                            r.getAsString(WORDLISTID_COLUMN),
                            Integer.toString(STATUS_INSTALLED)),
                            null, null, null)
                    try {
                        if (c.moveToFirst()) { // There should never be more than one file, but if there are, it's a bug
// and we should remove them all. I think it might happen if the power of
// the phone is suddenly cut during an update.
                            val filenameIndex = c.getColumnIndex(LOCAL_FILENAME_COLUMN)
                            do {
                                DebugLogUtils.l("Setting for removal", c.getString(filenameIndex))
                                filenames.add(c.getString(filenameIndex))
                            } while (c.moveToNext())
                        }
                    } finally {
                        c.close()
                    }
                    r.put(STATUS_COLUMN, STATUS_INSTALLED)
                    db.beginTransactionNonExclusive()
                    // Delete all old entries. There should never be any stalled entries, but if
// there are, this deletes them.
                    db.delete(METADATA_TABLE_NAME,
                            "$WORDLISTID_COLUMN = ?", arrayOf(r.getAsString(WORDLISTID_COLUMN)))
                    db.insert(METADATA_TABLE_NAME, null, r)
                    db.setTransactionSuccessful()
                    db.endTransaction()
                    for (filename in filenames) {
                        try {
                            val f = File(filename)
                            f.delete()
                        } catch (e: SecurityException) { // No permissions to delete. Um. Can't do anything.
                        } // I don't think anything else can be thrown
                    }
                }
                else -> {
                }
            }
        }

        /**
         * Removes a downloading entry from the database.
         *
         * This is invoked when a download fails. Either we tried to download, but
         * we received a permanent failure and we should remove it, or we got manually
         * cancelled and we should leave it at that.
         *
         * @param db the metadata database.
         * @param id the DownloadManager id of the file.
         */
        fun deleteDownloadingEntry(db: SQLiteDatabase, id: Long) {
            db.delete(METADATA_TABLE_NAME, "$PENDINGID_COLUMN = ? AND $STATUS_COLUMN = ?", arrayOf(java.lang.Long.toString(id), Integer.toString(STATUS_DOWNLOADING)))
        }

        /**
         * Forcefully removes an entry from the database.
         *
         * This is invoked when a file is broken. The file has been downloaded, but Android
         * Keyboard is telling us it could not open it.
         *
         * @param db the metadata database.
         * @param id the id of the word list.
         * @param version the version of the word list.
         */
        fun deleteEntry(db: SQLiteDatabase, id: String, version: Int) {
            db.delete(METADATA_TABLE_NAME, "$WORDLISTID_COLUMN = ? AND $VERSION_COLUMN = ?", arrayOf(id, Integer.toString(version)))
        }

        /**
         * Internal method that sets the current status of an entry of the database.
         *
         * @param db the metadata database.
         * @param id the id of the word list.
         * @param version the version of the word list.
         * @param status the status to set the word list to.
         * @param downloadId an optional download id to write, or NOT_A_DOWNLOAD_ID
         */
        private fun markEntryAs(db: SQLiteDatabase, id: String?,
                                version: Int, status: Int, downloadId: Long) {
            val values = getContentValuesByWordListId(db, id, version)
            values!!.put(STATUS_COLUMN, status)
            if (NOT_A_DOWNLOAD_ID != downloadId) {
                values.put(PENDINGID_COLUMN, downloadId)
            }
            db.update(METADATA_TABLE_NAME, values,
                    "$WORDLISTID_COLUMN = ? AND $VERSION_COLUMN = ?", arrayOf(id, Integer.toString(version)))
        }

        /**
         * Writes the status column for the wordlist with this id as enabled. Typically this
         * means the word list is currently disabled and we want to set its status to INSTALLED.
         *
         * @param db the metadata database.
         * @param id the id of the word list.
         * @param version the version of the word list.
         */
        fun markEntryAsEnabled(db: SQLiteDatabase, id: String?,
                               version: Int) {
            markEntryAs(db, id, version, STATUS_INSTALLED, NOT_A_DOWNLOAD_ID)
        }

        /**
         * Writes the status column for the wordlist with this id as disabled. Typically this
         * means the word list is currently installed and we want to set its status to DISABLED.
         *
         * @param db the metadata database.
         * @param id the id of the word list.
         * @param version the version of the word list.
         */
        fun markEntryAsDisabled(db: SQLiteDatabase, id: String?,
                                version: Int) {
            markEntryAs(db, id, version, STATUS_DISABLED, NOT_A_DOWNLOAD_ID)
        }

        /**
         * Writes the status column for the wordlist with this id as available. This happens for
         * example when a word list has been deleted but can be downloaded again.
         *
         * @param db the metadata database.
         * @param id the id of the word list.
         * @param version the version of the word list.
         */
        fun markEntryAsAvailable(db: SQLiteDatabase, id: String?,
                                 version: Int) {
            markEntryAs(db, id, version, STATUS_AVAILABLE, NOT_A_DOWNLOAD_ID)
        }

        /**
         * Writes the designated word list as downloadable, alongside with its download id.
         *
         * @param db the metadata database.
         * @param id the id of the word list.
         * @param version the version of the word list.
         * @param downloadId the download id.
         */
        fun markEntryAsDownloading(db: SQLiteDatabase, id: String?,
                                   version: Int, downloadId: Long) {
            markEntryAs(db, id, version, STATUS_DOWNLOADING, downloadId)
        }

        /**
         * Writes the designated word list as deleting.
         *
         * @param db the metadata database.
         * @param id the id of the word list.
         * @param version the version of the word list.
         */
        fun markEntryAsDeleting(db: SQLiteDatabase, id: String?,
                                version: Int) {
            markEntryAs(db, id, version, STATUS_DELETING, NOT_A_DOWNLOAD_ID)
        }

        /**
         * Checks retry counts and marks the word list as retrying if retry is possible.
         *
         * @param db the metadata database.
         * @param id the id of the word list.
         * @param version the version of the word list.
         * @return `true` if the retry is possible.
         */
        fun maybeMarkEntryAsRetrying(db: SQLiteDatabase, id: String?,
                                     version: Int): Boolean {
            val values = getContentValuesByWordListId(db, id, version)
            val retryCount = values!!.getAsInteger(RETRY_COUNT_COLUMN)
            if (retryCount > 1) {
                values.put(STATUS_COLUMN, STATUS_RETRYING)
                values.put(RETRY_COUNT_COLUMN, retryCount - 1)
                db.update(METADATA_TABLE_NAME, values,
                        "$WORDLISTID_COLUMN = ? AND $VERSION_COLUMN = ?", arrayOf(id, Integer.toString(version)))
                return true
            }
            return false
        }
    }

}