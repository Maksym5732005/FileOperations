package com.example.fileoperations

import android.annotation.SuppressLint
import android.content.Context
import android.provider.MediaStore
import android.util.Log
import java.text.SimpleDateFormat
import java.util.concurrent.TimeUnit


class MediaFiles(private val context: Context) {
    private val CLASS_NAME = this.javaClass.simpleName

    companion object {
        private const val TAG = "TestOpenFile"

    }

    fun queryImage() {
        /**
         * A key concept when working with Android [ContentProvider]s is something called
         * "projections". A projection is the list of columns to request from the provider,
         * and can be thought of (quite accurately) as the "SELECT ..." clause of a SQL
         * statement.
         *
         * It's not _required_ to provide a projection. In this case, one could pass `null`
         * in place of `projection` in the call to [ContentResolver.query], but requesting
         * more data than is required has a performance impact.
         *
         * For this sample, we only use a few columns of data, and so we'll request just a
         * subset of columns.
         */

        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATE_ADDED,
        )

        /**
         * The `selection` is the "WHERE ..." clause of a SQL statement. It's also possible
         * to omit this by passing `null` in its place, and then all rows will be returned.
         * In this case we're using a selection based on the date the image was taken.
         *
         * Note that we've included a `?` in our selection. This stands in for a variable
         * which will be provided by the next variable.
         */

        val selection = "${MediaStore.Images.Media.DATE_ADDED} >= ?"


        /**
         * The `selectionArgs` is a list of values that will be filled in for each `?`
         * in the `selection`.
         */

        val selectionArgs = arrayOf(
            // Release day of the G1. :)
            dateToTimestamp(day = 17, month = 11, year = 2020).toString())

        /**
         * Sort order to use. This can also be null, which will use the default sort
         * order. For [MediaStore.Images], the default sort order is ascending by date taken.
         */
        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} DESC"

        context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            sortOrder
        )?.use { cursor ->

            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val dateModifiedColumn =
                cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
            val displayNameColumn =
                cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)


           /* Log.d(TAG, "$CLASS_NAME Found idColumn $idColumn")
            Log.d(TAG, "$CLASS_NAME Found dateModifiedColumn $dateModifiedColumn")
            Log.d(TAG, "$CLASS_NAME Found displayNameColumn $displayNameColumn")
            Log.d(TAG, "$CLASS_NAME Found ${cursor.count} images")
*/
            while (cursor.moveToNext()) {
                // Use an ID column from the projection to get
                // a URI representing the media item itself.
            }
        }
    }

    @Suppress("SameParameterValue")
    @SuppressLint("SimpleDateFormat")
    fun dateToTimestamp(day: Int, month: Int, year: Int): Long =
        SimpleDateFormat("dd.MM.yyyy").let { formatter ->
            TimeUnit.MICROSECONDS.toSeconds(formatter.parse("$day.$month.$year")?.time ?: 0)
        }
}