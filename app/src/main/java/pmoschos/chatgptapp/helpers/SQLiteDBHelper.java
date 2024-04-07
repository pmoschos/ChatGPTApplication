package pmoschos.chatgptapp.helpers;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

public class SQLiteDBHelper extends SQLiteOpenHelper {

    public static final String DATABASE_NAME = "chat_gpt_app.db";
    public static final int DATABASE_VERSION = 3;
    public static final String TABLE_NAME = "posts";
    public static final String POST_ID = "postId";
    public static final String POST_QUESTION = "question";
    public static final String POST_RESPONSE = "response";

    private static final String SQL_CREATE_ENTITIES =
            "CREATE TABLE " + TABLE_NAME + "(" +
                    POST_ID + " INTEGER PRIMARY KEY," +
                    POST_QUESTION + " TEXT," +
                    POST_RESPONSE + " TEXT)" ;

    public SQLiteDBHelper(@Nullable Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL(SQL_CREATE_ENTITIES);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(sqLiteDatabase);
    }

    public Cursor getAllData() {
        SQLiteDatabase sqLiteDatabase = this.getReadableDatabase();
        String[] projection = { POST_ID, POST_QUESTION, POST_RESPONSE };

        return sqLiteDatabase.query(
                TABLE_NAME,
                projection,
                null,
                null,
                null,
                null,
                null);
    }

    public void deleteFullDatabase() {
        this.getWritableDatabase().delete(TABLE_NAME, null, null);
    }

    public int deletePost(int postId) {
        SQLiteDatabase db = this.getWritableDatabase();
        String whereClause = POST_ID + " = ?";
        String[] whereArgs = {String.valueOf(postId)};
        int postDeleted = db.delete(TABLE_NAME, whereClause, whereArgs);

        return postDeleted;
    }

}
