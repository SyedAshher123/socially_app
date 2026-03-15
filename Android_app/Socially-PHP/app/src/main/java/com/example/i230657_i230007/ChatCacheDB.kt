package com.example.i230657_i230007

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class ChatCacheDB(context: Context) :
    SQLiteOpenHelper(context, "chat_cache.db", null, 1) {

    companion object {
        const val TABLE = "cached_messages"

        const val COL_ID = "message_id"
        const val COL_CHAT_ID = "chat_id"
        const val COL_SENDER = "sender_id"
        const val COL_TEXT = "message_text"
        const val COL_IMAGE = "image_base64"
        const val COL_TYPE = "type"
        const val COL_TIMESTAMP = "timestamp"
        const val COL_VANISH = "is_vanish"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val query = """
            CREATE TABLE $TABLE (
                $COL_ID TEXT PRIMARY KEY,
                $COL_CHAT_ID INTEGER,
                $COL_SENDER TEXT,
                $COL_TEXT TEXT,
                $COL_IMAGE TEXT,
                $COL_TYPE TEXT,
                $COL_TIMESTAMP LONG,
                $COL_VANISH INTEGER
            );
        """
        db.execSQL(query)
    }

    override fun onUpgrade(db: SQLiteDatabase, old: Int, new: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE")
        onCreate(db)
    }

    // Save message
    fun saveMessage(
        msgId: String,
        chatId: Int,
        sender: String,
        text: String?,
        img: String?,
        type: String,
        timestamp: Long,
        vanish: Int
    ) {
        val db = writableDatabase
        val cv = ContentValues()

        cv.put(COL_ID, msgId)
        cv.put(COL_CHAT_ID, chatId)
        cv.put(COL_SENDER, sender)
        cv.put(COL_TEXT, text)
        cv.put(COL_IMAGE, img)
        cv.put(COL_TYPE, type)
        cv.put(COL_TIMESTAMP, timestamp)
        cv.put(COL_VANISH, vanish)

        db.insertWithOnConflict(TABLE, null, cv, SQLiteDatabase.CONFLICT_REPLACE)
    }

    // Read messages
    fun getChatMessages(chatId: Int): Cursor {
        val db = readableDatabase
        return db.query(
            TABLE,
            null,
            "$COL_CHAT_ID = ?",
            arrayOf(chatId.toString()),
            null,
            null,
            "$COL_TIMESTAMP ASC"
        )
    }
}
