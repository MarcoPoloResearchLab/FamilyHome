package com.mprlab.portal;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

public final class ShareProvider extends ContentProvider {
    static Uri uriFor(Context context, File file) {
        return new Uri.Builder().scheme("content").authority(context.getPackageName() + ".files")
                .appendPath(file.getName()).build();
    }

    @Override public boolean onCreate() { return true; }

    @Override public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        if (!"r".equals(mode)) throw new FileNotFoundException("read-only provider");
        File file = resolve(uri);
        return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
    }

    private File resolve(Uri uri) throws FileNotFoundException {
        if (getContext() == null || uri.getLastPathSegment() == null) throw new FileNotFoundException();
        File directory = new File(getContext().getCacheDir(), "shared_drawings");
        File file = new File(directory, uri.getLastPathSegment());
        try {
            if (!file.getCanonicalPath().startsWith(directory.getCanonicalPath() + File.separator) || !file.isFile()) throw new FileNotFoundException();
        } catch (IOException error) { throw new FileNotFoundException(error.getMessage()); }
        return file;
    }

    @Override public String getType(Uri uri) { return "image/png"; }

    @Override public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        try {
            File file = resolve(uri);
            String[] columns = projection == null ? new String[]{OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE} : projection;
            MatrixCursor cursor = new MatrixCursor(columns);
            Object[] values = new Object[columns.length];
            for (int index = 0; index < columns.length; index++) {
                if (OpenableColumns.DISPLAY_NAME.equals(columns[index])) values[index] = file.getName();
                else if (OpenableColumns.SIZE.equals(columns[index])) values[index] = file.length();
            }
            cursor.addRow(values);
            return cursor;
        } catch (FileNotFoundException error) { return null; }
    }

    @Override public Uri insert(Uri uri, ContentValues values) { throw new UnsupportedOperationException(); }
    @Override public int delete(Uri uri, String selection, String[] selectionArgs) { return 0; }
    @Override public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) { return 0; }
}
