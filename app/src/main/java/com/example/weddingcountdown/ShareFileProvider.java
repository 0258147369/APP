package com.example.weddingcountdown;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;

import java.io.File;
import java.io.FileNotFoundException;

/**
 * Shares the generated invitation/countdown card with other apps.
 * The URI is granted read-only access only to the receiving share target.
 */
public class ShareFileProvider extends ContentProvider {
    private static final int CARD = 1;
    private static final UriMatcher MATCHER = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        MATCHER.addURI("com.example.weddingcountdown.v2.share", "card", CARD);
    }

    @Override public boolean onCreate() { return true; }

    private File cardFile(Uri uri) throws FileNotFoundException {
        if (MATCHER.match(uri) != CARD) throw new FileNotFoundException("Unknown shared file");
        File file = new File(getContext().getCacheDir(), "wedding-share.png");
        if (!file.exists() || !file.isFile()) throw new FileNotFoundException("Share card not found");
        return file;
    }

    @Override public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        if (!"r".equals(mode)) throw new FileNotFoundException("Read-only sharing");
        return ParcelFileDescriptor.open(cardFile(uri), ParcelFileDescriptor.MODE_READ_ONLY);
    }

    @Override public String getType(Uri uri) {
        return MATCHER.match(uri) == CARD ? "image/png" : null;
    }

    @Override public Cursor query(Uri uri, String[] projection, String selection,
                                 String[] selectionArgs, String sortOrder) {
        try {
            File file = cardFile(uri);
            MatrixCursor cursor = new MatrixCursor(new String[]{
                    OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE
            });
            cursor.addRow(new Object[]{"wedding-countdown.png", file.length()});
            return cursor;
        } catch (FileNotFoundException e) {
            return null;
        }
    }

    @Override public Uri insert(Uri uri, ContentValues values) { return null; }
    @Override public int delete(Uri uri, String selection, String[] selectionArgs) { return 0; }
    @Override public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) { return 0; }
}
