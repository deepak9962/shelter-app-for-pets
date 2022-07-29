package com.example.android.pets.data;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.android.pets.R;

import java.util.Objects;

public class PetProvider extends ContentProvider {

    public static final String LOG_TAG = PetProvider.class.getSimpleName();

    private static final int PETS = 100;
    private static final int PET_ID = 101;

    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        sUriMatcher.addURI(PetContract.CONTENT_AUTHORITY, PetContract.PATH_PETS, PETS);
        sUriMatcher.addURI(PetContract.CONTENT_AUTHORITY, PetContract.PATH_PETS + "/#", PET_ID);
    }

    private PetDbHelper mDbHelper;

    @Override
    public boolean onCreate() {
        mDbHelper = new PetDbHelper(getContext());
        return true;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection, @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        SQLiteDatabase database = mDbHelper.getReadableDatabase();

        Cursor cursor;

        int match = sUriMatcher.match(uri);
        switch (match) {
            case PETS:
                cursor = database.query(
                        PetContract.PetEntry.TABLE_NAME,
                        projection,
                        null,
                        null,
                        null,
                        null,
                        sortOrder
                );
                break;
            case PET_ID:
                selection = PetContract.PetEntry._ID + "=?";
                selectionArgs = new String[] {
                        String.valueOf(ContentUris.parseId(uri))
                };
                cursor = database.query(
                        PetContract.PetEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder
                );
                break;
            default:
                throw new IllegalArgumentException("Cannot query unknown uri " + uri);
        }

        cursor.setNotificationUri(getContext().getContentResolver(), uri);
        return cursor;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case PETS:
                return PetContract.PetEntry.CONTENT_LIST_TYPE;
            case PET_ID:
                return PetContract.PetEntry.CONTENT_ITEM_TYPE;
            default:
                throw new IllegalArgumentException("Unknown URI " + uri + " with match " + match);
        }
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues contentValues) {
        int match = sUriMatcher.match(uri);
        if (match == PETS) {
            if (contentValues != null) {
                return insertPet(uri, contentValues);
            }
        }
        throw new IllegalArgumentException("Insertion is not supported for this " + uri);
    }

    private Uri insertPet(Uri uri, ContentValues contentValues) {
        String petName = contentValues.getAsString(PetContract.PetEntry.COLUMN_PET_NAME);
        Integer petGender = contentValues.getAsInteger(PetContract.PetEntry.COLUMN_PET_GENDER);
        Integer petWeight = contentValues.getAsInteger(PetContract.PetEntry.COLUMN_PET_WEIGHT);

        if (petName.isEmpty()) {
            throw new IllegalArgumentException("Pet name cannot be empty");
        }

        if (petGender == null || !PetContract.PetEntry.isValidGender(petGender)) {
            throw new IllegalArgumentException("Pet requires a valid gender");
        }

        if (petWeight != null && petWeight < 0) {
            throw new IllegalArgumentException("Pet weight cannot be empty or less than 0");
        }

        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        long id = db.insert(PetContract.PetEntry.TABLE_NAME, null, contentValues);

        if (id == -1) {
            Log.e(LOG_TAG, "Failed to insert row for " + uri);
            return null;
        }

        if (!(id <= 0)){
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return ContentUris.withAppendedId(uri, id);
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        int delCount;

        final int match = sUriMatcher.match(uri);
        switch (match) {
            case PETS:
                delCount = db.delete(PetContract.PetEntry.TABLE_NAME, selection, selectionArgs);
                break;
            case PET_ID:
                selection = PetContract.PetEntry._ID + "=?";
                selectionArgs = new String[] {String.valueOf(ContentUris.parseId(uri))};
                delCount = db.delete(PetContract.PetEntry.TABLE_NAME, selection, selectionArgs);
                break;
            default:
                throw new IllegalArgumentException("Deletion for not supported for " + uri);
        }

        if (delCount > 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return delCount;
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues contentValues, @Nullable String selection, @Nullable String[] selectionArgs) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case PETS:
                return updatePet(uri, Objects.requireNonNull(contentValues), selection, selectionArgs);
            case PET_ID:
                selection = PetContract.PetEntry._ID + "=?";
                selectionArgs = new String[] {String.valueOf(ContentUris.parseId(uri))};
                return updatePet(uri, Objects.requireNonNull(contentValues), selection, selectionArgs);
            default:
                throw new IllegalArgumentException("Update is not supported for " + uri);
        }
    }

    private int updatePet(Uri uri, ContentValues contentValues, String selection, String[] selectionArgs) {
        SQLiteDatabase db = mDbHelper.getWritableDatabase();
        int updateCount;

        if (contentValues.containsKey(PetContract.PetEntry.COLUMN_PET_NAME)) {
            String petName = contentValues.getAsString(PetContract.PetEntry.COLUMN_PET_NAME);
            if (petName.isEmpty()) {
                throw new IllegalArgumentException("Pet name cannot be empty");
            }
        }

        if (contentValues.containsKey(PetContract.PetEntry.COLUMN_PET_GENDER)) {
            Integer petGender = contentValues.getAsInteger(PetContract.PetEntry.COLUMN_PET_GENDER);
            if (petGender == null || !PetContract.PetEntry.isValidGender(petGender)) {
                throw new IllegalArgumentException("Pet requires a valid gender");
            }
        }

        if (contentValues.containsKey(PetContract.PetEntry.COLUMN_PET_WEIGHT)) {
            Integer petWeight = contentValues.getAsInteger(PetContract.PetEntry.COLUMN_PET_WEIGHT);
            if (petWeight != null && petWeight < 0) {
                throw new IllegalArgumentException("Pet weight cannot be empty or less than 0");
            }
        }

        if (contentValues.size() == 0) {
            return 0;
        }

        updateCount = db.update(PetContract.PetEntry.TABLE_NAME, contentValues, selection, selectionArgs);

        if (updateCount > 0) {
            getContext().getContentResolver().notifyChange(uri, null);
        }
        return updateCount;
    }
}
