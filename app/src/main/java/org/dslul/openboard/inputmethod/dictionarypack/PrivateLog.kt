package org.dslul.openboard.inputmethod.dictionarypack

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.text.SimpleDateFormat
import java.util.*

/**
 * Class to keep long-term log. This is inactive in production, and is only for debug purposes.
 */
object PrivateLog {
    const val DEBUG = false
    private const val LOG_DATABASE_NAME = "log"
    private const val LOG_TABLE_NAME = "log"
    private const val LOG_DATABASE_VERSION = 1
    private const val COLUMN_DATE = "date"
    private const val COLUMN_EVENT = "event"
    private const val LOG_TABLE_CREATE = ("CREATE TABLE " + LOG_TABLE_NAME + " ("
            + COLUMN_DATE + " TEXT,"
            + COLUMN_EVENT + " TEXT);")
    val sDateFormat = SimpleDateFormat(
            "yyyy/MM/dd HH:mm:ss", Locale.ROOT)
    private val sInstance: PrivateLog = PrivateLog
    private var sDebugHelper: DebugHelper? = null
    @Synchronized
    fun getInstance(context: Context?): PrivateLog {
        if (!DEBUG) return sInstance
        synchronized(PrivateLog::class.java) {
            if (sDebugHelper == null) {
                sDebugHelper = DebugHelper(context)
            }
            return sInstance
        }
    }

    fun log(event: String?) {
        if (!DEBUG) return
        val l = sDebugHelper!!.writableDatabase
        DebugHelper.insert(l, event)
    }

    internal class DebugHelper(context: Context?) : SQLiteOpenHelper(context, LOG_DATABASE_NAME, null, LOG_DATABASE_VERSION) {
        override fun onCreate(db: SQLiteDatabase) {
            if (!DEBUG) return
            db.execSQL(LOG_TABLE_CREATE)
            insert(db, "Created table")
        }

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            if (!DEBUG) return
            // Remove all data.
            db.execSQL("DROP TABLE IF EXISTS $LOG_TABLE_NAME")
            onCreate(db)
            insert(db, "Upgrade finished")
        }

        companion object {
            fun insert(db: SQLiteDatabase, event: String?) {
                if (!DEBUG) return
                val c = ContentValues(2)
                c.put(COLUMN_DATE, sDateFormat.format(Date(System.currentTimeMillis())))
                c.put(COLUMN_EVENT, event)
                db.insert(LOG_TABLE_NAME, null, c)
            }
        }
    }
}