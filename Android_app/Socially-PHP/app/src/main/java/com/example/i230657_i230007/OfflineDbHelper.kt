package com.example.i230657_i230007

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class OfflineDBHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "offline_chat.db"
        const val DATABASE_VERSION = 1

        const val TABLE_MESSAGES = "offline_messages"

        const val COLUMN_ID = "id"
        const val COLUMN_TEXT = "message_text"
        const val COLUMN_IMAGE = "image_path"
        const val COLUMN_RECEIVER = "receiver_id"
        const val COLUMN_SENDER = "sender_id"
        const val COLUMN_TIMESTAMP = "timestamp"
        const val COLUMN_SYNC = "sync_status"      // 0 = pending, 1 = synced
        const val COLUMN_TYPE = "type"             // text / image / both
        const val COLUMN_CHAT_ID = "chat_id"
        const val COLUMN_IS_VANISH = "is_vanish"
    }

    override fun onCreate(db: SQLiteDatabase?) {
        val createMessagesTable = """
            CREATE TABLE $TABLE_MESSAGES (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_TEXT TEXT,
                $COLUMN_IMAGE TEXT,
                $COLUMN_TYPE TEXT,
                $COLUMN_RECEIVER TEXT,
                $COLUMN_SENDER TEXT,
                $COLUMN_CHAT_ID INTEGER,
                $COLUMN_TIMESTAMP LONG,
                $COLUMN_SYNC INTEGER DEFAULT 0,
                $COLUMN_IS_VANISH INTEGER DEFAULT 0
            );
        """
        db?.execSQL(createMessagesTable)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_MESSAGES")
        onCreate(db)
    }

    // -------------------------------------------------------------
    // INSERT OFFLINE MESSAGE
    // -------------------------------------------------------------
    fun insertOfflineMessage(
        senderId: String,
        receiverId: String,
        chatId: Int,
        messageText: String?,
        imagePath: String?,
        type: String,
        timestamp: Long,
        isVanish: Boolean
    ): Long {

        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_TEXT, messageText)
            put(COLUMN_IMAGE, imagePath)
            put(COLUMN_RECEIVER, receiverId)
            put(COLUMN_SENDER, senderId)
            put(COLUMN_CHAT_ID, chatId)
            put(COLUMN_TIMESTAMP, timestamp)
            put(COLUMN_TYPE, type)
            put(COLUMN_SYNC, 0)
            put(COLUMN_IS_VANISH, if (isVanish) 1 else 0)
        }

        return db.insert(TABLE_MESSAGES, null, values)
    }

    // -------------------------------------------------------------
    // GET ALL UNSYNCED MESSAGES
    // -------------------------------------------------------------
    fun getAllUnSyncedMessages(): Cursor {
        val db = readableDatabase
        return db.query(
            TABLE_MESSAGES,
            null,
            "$COLUMN_SYNC = ?",
            arrayOf("0"),
            null,
            null,
            "$COLUMN_TIMESTAMP ASC"
        )
    }

    // -------------------------------------------------------------
    // MARK MESSAGE AS SYNCED
    // -------------------------------------------------------------
    fun updateMessageAsSynced(id: Int): Int {
        val db = writableDatabase
        val values = ContentValues()
        values.put(COLUMN_SYNC, 1)

        return db.update(
            TABLE_MESSAGES,
            values,
            "$COLUMN_ID = ?",
            arrayOf(id.toString())
        )
    }

    // -------------------------------------------------------------
    // DELETE MESSAGE (OPTIONAL)
    // -------------------------------------------------------------
    fun deleteMessage(id: Int): Int {
        val db = writableDatabase
        return db.delete(
            TABLE_MESSAGES,
            "$COLUMN_ID = ?",
            arrayOf(id.toString())
        )
    }

    // -------------------------------------------------------------
    // CLEAR SYNCED MESSAGES (OPTIONAL)
    // -------------------------------------------------------------
    fun clearSyncedMessages() {
        val db = writableDatabase
        db.delete(TABLE_MESSAGES, "$COLUMN_SYNC = 1", null)
    }
}
