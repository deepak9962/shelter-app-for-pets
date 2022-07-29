/*
 * Copyright (C) 2016 The Android Open Source Project
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
package com.example.android.pets;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;

import com.example.android.pets.data.PetContract.PetEntry;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

/**
 * Displays list of pets that were entered and stored in the app.
 */
public class CatalogActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    private PetCursorAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_catalog);

        // Setup FAB to open EditorActivity
        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(view -> {
            Intent intent = new Intent(CatalogActivity.this, EditorActivity.class);
            startActivity(intent);
        });

        ListView petListView = findViewById(R.id.pets_recycler_view);

        View emptyView = findViewById(R.id.empty_view);
        petListView.setEmptyView(emptyView);

        mAdapter = new PetCursorAdapter(this, null);
        petListView.setAdapter(mAdapter);
        petListView.setOnItemClickListener((adapterView, view, position, id) -> {
            Intent editorIntent = new Intent(CatalogActivity.this, EditorActivity.class);
            Uri currentPetUri = ContentUris.withAppendedId(PetEntry.CONTENT_URI, id);
            editorIntent.setData(currentPetUri);
            startActivity(editorIntent);
        });

        LoaderManager.getInstance(this).initLoader(0, null, this);
    }

    private void insertData() {
        ContentValues values = new ContentValues();
        values.put(PetEntry.COLUMN_PET_NAME, "Toto");
        values.put(PetEntry.COLUMN_PET_BREED, "Terrier");
        values.put(PetEntry.COLUMN_PET_GENDER, PetEntry.GENDER_MALE);
        values.put(PetEntry.COLUMN_PET_WEIGHT, 7);

        Uri newRowId = getContentResolver().insert(PetEntry.CONTENT_URI, values);
        Toast.makeText(this, "Dummy pet saved", Toast.LENGTH_SHORT).show();
        Log.v(CatalogActivity.class.getName(), "New Row ID: " + ContentUris.parseId(newRowId));
    }

    private void deleteAllPets() {
        int getRowId = getContentResolver().delete(PetEntry.CONTENT_URI, null, null);

        if (getRowId > 0) {
            Toast.makeText(this, R.string.catalog_delete_all_pets_successful, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, R.string.catalog_delete_all_pet_failed, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu options from the res/menu/menu_catalog.xml file.
        // This adds menu items to the app bar.
        getMenuInflater().inflate(R.menu.menu_catalog, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // User clicked on a menu option in the app bar overflow menu
        if (item.getItemId() == R.id.action_insert_dummy_data) {
            // Respond to a click on the "Insert dummy data" menu option
            insertData();
            return true;
        } else if (item.getItemId() == R.id.action_delete_all_entries) {
            DialogInterface.OnClickListener deleteButtonClickListener =
                    (dialogInterface, i) -> deleteAllPets();

            showDeleteConformationDialog(deleteButtonClickListener);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showDeleteConformationDialog(DialogInterface.OnClickListener discardButtonClickListener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.delete_all_confirmation_dialog_msg);
        builder.setPositiveButton(R.string.action_delete, discardButtonClickListener);
        builder.setNegativeButton(R.string.cancel, (dialogInterface, i) -> {
            if (dialogInterface != null) {
                dialogInterface.dismiss();
            }
        });

        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int id, @Nullable Bundle args) {
        String[] projection = {
                PetEntry._ID,
                PetEntry.COLUMN_PET_NAME,
                PetEntry.COLUMN_PET_BREED,
        };

        return new CursorLoader(
                this,
                PetEntry.CONTENT_URI,
                projection,
                null,
                null,
                null
        );
    }

    @Override
    public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor data) {
        mAdapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(@NonNull Loader<Cursor> loader) {
        mAdapter.swapCursor(null);
    }
}
