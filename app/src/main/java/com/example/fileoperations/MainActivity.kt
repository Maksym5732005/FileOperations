package com.example.fileoperations

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.os.storage.StorageManager
import android.os.storage.StorageManager.ACTION_MANAGE_STORAGE
import android.provider.DocumentsContract
import android.provider.DocumentsContract.Document.FLAG_SUPPORTS_DELETE
import android.provider.OpenableColumns
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.*
import java.util.*


class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "TestOpenFile"
        private const val CREATE_FILE = 1
        private const val DELETE_REQUEST_CODE = 41
        private const val READ_REQUEST_CODE = 42
        private const val WRITE_REQUEST_CODE = 43
        private const val READ_CONTENT_CODE = 44
        private const val WRITE_CONTENT_CODE = 45
        private const val DEFAULT_PATH = "TESTPROJECT"

        // App needs 10 MB within internal storage.
        private const val NUM_BYTES_NEEDED_FOR_MY_APP = 1024 * 1024 * 10L;
    }

    private var count = 0
    private var displayName: String = ""
    private var mUri: Uri? = null
    private var bytes: ByteArray? = ByteArray(0)

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        btnChooseFile.setOnClickListener {
            performFileSearch()
        }
        btnCreateFileOnCloud.setOnClickListener {
            createFileOnCloud("text/plain", "appFile.txt");
        }
        btnDeleteFile.setOnClickListener {
            performFileDelete()
        }
        btnReadFileContent.setOnClickListener {
            performFileReadContent()
        }
        btnWriteFileToContent.setOnClickListener {
            performFileWriteContent()
            count++
        }
        btnWriteIntStorage.setOnClickListener {
            writeFileToInternalStorage()
        }
        btnReadIntStorage.setOnClickListener {
            readFileFromInternalStorage()
        }
        btnCreateNestedDirectory.setOnClickListener {
            createDirectoryAndFile()
        }
        btnCreateCache.setOnClickListener {
            cacheDirectoryInternal()
        }
        btnDeleteCache.setOnClickListener {
            deleteCacheFileInernal()
        }
        btnIsStorageRW.setOnClickListener {
            Log.d(
                TAG, "${this.javaClass.simpleName} isExternalStorageWritableAndReadable " +
                        "${isExternalStorageWritableAndReadable()}"
            )
        }
        btnIsStorageR.setOnClickListener {
            Log.d(
                TAG, "${this.javaClass.simpleName} isExternalStorageReadable" +
                        " ${isExternalStorageReadable()}"
            )
        }
        btnStorageAvailable.setOnClickListener {
            getStorageAvailable()
        }
        btnAccessFile.setOnClickListener {
            accessPersistentFiles()
        }
        btnCreateCacheFile.setOnClickListener {
            createCacheFile()
        }
        btnDeleteCacheFile.setOnClickListener {
            deleteCacheFileExternal()
        }
        btnQueryFreeSpace.setOnClickListener {
            queryFreeSpace()
        }
        btnWriteFileExternalStorage.setOnClickListener {
            GlobalScope.launch(Dispatchers.IO) {
                writeFileAPI29(mUri!!)
            }

        }
        btnGetRoot.setOnClickListener {
            getRoot()
        }
        btnReadBytesFromUri.setOnClickListener {
            GlobalScope.launch {
                readFileFromUri(mUri!!)
            }
        }
        btnWriteToPublicDownload.setOnClickListener {
            createFileExternalStorage(mUri!!)
        }
        btnDeleteDocument.setOnClickListener {
            deleteDocument(Uri.parse("content://com.android.providers.downloads.documents/document/183"))
        }
    }


    /**
     *choose file //my
     */
    private fun performFileSearch() {
        /**
         * ACTION_OPEN_DOCUMENT is the intent to choose a file via the system's file
         * browser.
         */


        /**
         * use ACTION_OPEN_DOCUMENT if your application needs long-term, permanent access
         * to documents owned by a document provider. An example is a photo editor that allows
         * users to manipulate images stored in a document provider.
         */
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)

        /**
         * use ACTION_GET_CONTENT if your application just needs to read or import data.
         * With this approach, the application imports a copy of the data, such as an image file.
         */

        //val intent = Intent(Intent.ACTION_GET_CONTENT)

        /**
         * Filter to only show results that can be "opened", such as a
         * file (as opposed to a list of contacts or timezones)
         */
        intent.addCategory(Intent.CATEGORY_OPENABLE)

        /**
         * Filter to show only images, using the image MIME data type.
         * If one wanted to search for ogg vorbis files, the type would be "audio/ogg".
         * To search for all documents available via installed storage providers,
         *  */
        //it would be "*/*"

        intent.type = "*/*"
        startActivityForResult(intent, READ_REQUEST_CODE)
    }

    private fun performFileDelete() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            type = "*/*"
        }
        startActivityForResult(intent, DELETE_REQUEST_CODE)
    }

    private fun performFileReadContent() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            type = "*/*"
        }
        startActivityForResult(intent, READ_CONTENT_CODE)
    }

    private fun performFileWriteContent() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            type = "*/*"
        }
        startActivityForResult(intent, WRITE_CONTENT_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)
        Log.d(TAG, "${this.javaClass.simpleName} onActivityResult requestCode $requestCode")
        /**
         * The ACTION_OPEN_DOCUMENT intent was sent with the request code
         * READ_REQUEST_CODE. If the request code seen here doesn't match, it's the
         * response to some other intent, and the code below shouldn't run at all.
         *
         */

        if (resultCode == Activity.RESULT_OK) {

            /**
             * The document selected by the user won't be returned in the intent.
             * Instead, a URI to that document will be contained in the return intent
             * provided to this method as a parameter.
             * Pull that URI using resultData.getData().
             */

            intent?.let {
                mUri = it.data
                Log.d(TAG, "${this.javaClass.simpleName} onActivityResult Uri: $mUri")
                mUri?.let { data ->
                    Log.d(TAG, "${this.javaClass.simpleName} onActivityResult mimeType: ${getMimeType(data)}")
                    addPermission(data)
                    when (requestCode) {
                        READ_REQUEST_CODE -> {
                            if (getMimeType(data)!!.contains("image")) {
                                ivImage.setImageBitmap(getBitmapFromUri(data))
                            }
                            dumpImageMetaData(data)
                            // readTextFromUri(data)
                        }
                        DELETE_REQUEST_CODE -> {
                            deleteFile(data)
                        }
                        READ_CONTENT_CODE -> {
                            tvFileContent.text = readTextFromUri(data)
                        }
                        WRITE_CONTENT_CODE -> {
                            writeTextFromFile(data)
                        }
                        CREATE_FILE -> {
                            GlobalScope.launch {
                                writeFromUri(data)
                            }
                        }
                        else -> {
                            Log.d(
                                TAG,
                                "${this.javaClass.simpleName} onActivityResult other request code"
                            )
                        }
                    }
                }
            }
        }
    }

    /**
     * add and save permissions
     */
    private fun addPermission(data: Uri) {
        val takeFlags = (intent.flags
                and (Intent.FLAG_GRANT_READ_URI_PERMISSION
                or Intent.FLAG_GRANT_WRITE_URI_PERMISSION))
        // Check for the freshest data.
        contentResolver.takePersistableUriPermission(data, takeFlags)
    }

    /**
     * get name and size file from Uri
     */
    @SuppressLint("Recycle")
    private fun dumpImageMetaData(uri: Uri) {
        /**
         * The query, since it only applies to a single document, will only return
         * one row. There's no need to filter, sort, or select fields, since we want
         * all fields for one document.
         */
        val cursor = contentResolver.query(uri, null, null, null, null)

        try {
            /**
             * moveToFirst() returns false if the cursor has 0 rows.  Very handy
             * for "if there's anything to look at, look at it" conditionals.
             */

            cursor?.let {
                if (it.moveToFirst()) {
                    /**
                     * Note it's called "Display Name".  This is
                     * provider-specific, and might not necessarily be the file name.
                     */

                    displayName = it.getString(
                        it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    )
                    Log.d(TAG, "${this.javaClass.simpleName} Display Name: $displayName")
                    val sizeIndex = it.getColumnIndex(OpenableColumns.SIZE)

                    /**
                     * If the size is unknown, the value stored is null.  But since an
                     * int can't be null in Java, the behavior is implementation-specific,
                     * which is just a fancy term for "unpredictable".  So as
                     * a rule, check if it's null before assigning to an int.  This will
                     * happen often:  The storage API allows for remote files, whose
                     * size might not be locally known.
                     */
                    val size = if (!it.isNull(sizeIndex)) {
                        /**
                         * Technically the column stores an int, but cursor.getString()
                         * will do the conversion automatically.
                         */
                        it.getString(sizeIndex)
                    } else {
                        "Unknown"
                    }
                    Log.d(TAG, "${this.javaClass.simpleName} Size: $size")
                }
            }

        } finally {
            cursor!!.close()
        }
    }

    /**
     * open file and set in imageView
     * It must be done in the background
     */
    private fun getBitmapFromUri(uri: Uri): Bitmap? {
        val parcelFileDescriptor =
            contentResolver.openFileDescriptor(uri, "r")
        val fileDescriptor = parcelFileDescriptor?.fileDescriptor
        val image = BitmapFactory.decodeFileDescriptor(fileDescriptor)
        parcelFileDescriptor?.close()
        return image
    }

    /**
     * read text from file
     */
    private fun readTextFromUri(uri: Uri): String? {
        Log.d(TAG, "${this.javaClass.simpleName} readTextFromUri()")
        val inputStream = contentResolver.openInputStream(uri)
        val stringBuilder = StringBuilder()
        var line: String?
        val map = mutableMapOf<String, String>()
        inputStream.use {ipSt ->
            val reader = BufferedReader(InputStreamReader(ipSt!!))
            while ((reader.readLine().also { line = it }) != null) {
                val result = line?.split("=")
                map[result?.get(0).toString()] = result?.get(1).toString()
                stringBuilder.append(line)
            }
        }

        Log.d(TAG, "${this.javaClass.simpleName} readTextFromUri() $stringBuilder")
        return stringBuilder.toString()
    }

    /**
     * write text in file
     */
    private fun writeTextFromFile(uri: Uri) {
        Log.d(TAG, "${this.javaClass.simpleName} writeTextFromFile() uri: $uri")
        //my example
        /* var writer: BufferedWriter? = null
         try {
             writer = BufferedWriter(OutputStreamWriter(contentResolver.openOutputStream(uri)!!))
             writer.write("new text")
         }catch (e: Exception){

         }finally {
             writer?.close()
         }*/

        //google example
        try {
            val pfd: ParcelFileDescriptor? = contentResolver.openFileDescriptor(uri, "rwt")
            val fileOutputStream = FileOutputStream(pfd!!.fileDescriptor)
            val stringBuilder = StringBuilder()
            stringBuilder.append("Overwritten by MyCloud at ${System.currentTimeMillis()} \n")
            stringBuilder.append(count)

            fileOutputStream.write(stringBuilder.toString().toByteArray())
            // Let the document provider know you're done by closing the stream.
            fileOutputStream.close()
            pfd.close()
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    /**
     * Here are some examples of how you might call this method.
     * The first parameter is the MIME type, and the second parameter is the name
     * of the file you are creating:
     * createFile("text/plain", "foobar.txt");
     * createFile("image/png", "mypicture.png");
     */

    private fun createFileOnCloud(mimeType: String, fileName: String) {
        Log.d(TAG, "${this.javaClass.simpleName} createFile() fileName: $fileName")
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)

        /**
         * Filter to only show results that can be "opened", such as
         * a file (as opposed to a list of contacts or timezones).
         */
        intent.addCategory(Intent.CATEGORY_OPENABLE)

        /**
         * Create a file with the requested MIME type.
         */

        intent.type = mimeType
        intent.putExtra(Intent.EXTRA_TITLE, fileName)
        startActivityForResult(intent, WRITE_REQUEST_CODE)
    }

    /**
     * delete file
     */
    private fun deleteFile(uri: Uri) {
        Log.d(TAG, "${this.javaClass.simpleName} deleteFile() uri: $uri")
        DocumentsContract.deleteDocument(contentResolver, uri);
    }

    /**
     * get file type from Uri
     */
    private fun getMimeType(uri: Uri): String? {
        /**
         * return extension file(.png)get file type from Uri
         */
        //val mimeType = MimeTypeMap.getSingleton()
        //return mimeType.getExtensionFromMimeType(contentResolver.getType(uri))
        return contentResolver.getType(uri)
    }

    /**
     * write file(if don't exist, it create new file) and write content in filesDir (internal storage)
     * this method cam appending data to file
     * this method write file only in path /data/user/0/com.example.app/files
     * I could not change
     */
    private fun writeFileToInternalStorage() {
        Log.d(TAG, "${this.javaClass.simpleName} writeFileToInternalStorage() ${filesDir.path}")
        val fileName = "myFile.txt"
        val fileContents = "Overwritten by MyCloud at ${System.currentTimeMillis()} \n"
        openFileOutput(fileName, MODE_APPEND).use {
            it.write(fileContents.toByteArray())
        }


    }

    /**
     * read content from file from filesDir
     */
    private fun readFileFromInternalStorage() {
        Log.d(TAG, "${this.javaClass.simpleName} readFileFromInternalStorage() ")
        val stringBuilder = StringBuilder()
        openFileInput("myFile").bufferedReader().useLines { lines ->
            lines.fold("") { some, text ->
                Log.d(TAG, "${this.javaClass.simpleName} text $text")
                stringBuilder.append(text)
                stringBuilder.append("\n")
                tvFileContent.text = stringBuilder
                "$some\n$text"
            }
        }
    }

    /**
     * get list files
     */
    private fun getListFiles() {
        var files: Array<String> = fileList()
    }

    private fun createNestedDirectory() {
        getDir("dirName", MODE_PRIVATE)
    }

    /**
     * this method create folder and nested file
     */
    private fun createDirectoryAndFile() {
        val fileName = "myFile.txt"
        val fileContents = "Overwritten by MyCloud at ${System.currentTimeMillis()} \n"
        val folder = File(filesDir.path.replaceAfterLast("/", ""), "newFolder")

        folder.mkdirs()
        val fileOutputStream = FileOutputStream(File(folder, fileName), true)
        fileOutputStream.use {
            it.write(fileContents.toByteArray())
        }
    }

    private fun cacheDirectoryInternal() {
        //val cacheFile = File(cacheDir, "cacheFile")
        File.createTempFile("cacheFile", null, cacheDir)
    }

    private fun deleteCacheFileInernal() {
        Log.d(TAG, "${this.javaClass.simpleName} deleteCacheFile() ")
        cacheDir.deleteRecursively()
        //deleteFile("cacheFile")
    }

    /**
     *  Checks if a volume containing external storage is availablefor read and write.
     */
    private fun isExternalStorageWritableAndReadable(): Boolean {
        return Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED
    }

    /**
     * Checks if a volume containing external storage is available to at least read.
     */
    private fun isExternalStorageReadable(): Boolean {
        return Environment.getExternalStorageState() in
                setOf(Environment.MEDIA_MOUNTED, Environment.MEDIA_MOUNTED_READ_ONLY)
    }

    /**
     * get list available external storage
     */
    private fun getStorageAvailable() {
        val externalStorageVolumes: Array<out File> =
            ContextCompat.getExternalFilesDirs(applicationContext, null)
        val primaryExternalStorage = externalStorageVolumes[0]
        Log.d(
            TAG,
            "${this.javaClass.simpleName} getStorageAvailable() ${externalStorageVolumes.size}"
        )
        Log.d(TAG, "${this.javaClass.simpleName} getStorageAvailable() $primaryExternalStorage")
    }

    /**
     * Access persistent files
     * don't work need to figure it out
     */
    private fun accessPersistentFiles() {
        val appSpecificExternalDir = File(
            getExternalFilesDir(Environment.DIRECTORY_PICTURES),
            "newFile"
        )
        Log.d(
            TAG,
            "${this.javaClass.simpleName} accessPersistentFiles() ${appSpecificExternalDir.exists()}"
        )
    }

    /**
     * create cache file to external storage
     */
    private fun createCacheFile() {
        val externalCacheFile = File(externalCacheDir, "newFile")
    }

    /**
     * delete cache file to external storage
     */
    private fun deleteCacheFileExternal() {
        externalCacheDir?.delete()
    }


    /**
     * query free space in devices
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun queryFreeSpace() {
        val storageManager = getSystemService(Context.STORAGE_SERVICE) as StorageManager
        val appSpecificInternalDirUuid: UUID = storageManager.getUuidForPath(filesDir)
        val availableBytes: Long = storageManager.getAllocatableBytes(appSpecificInternalDirUuid)
        if (availableBytes >= NUM_BYTES_NEEDED_FOR_MY_APP) {
            storageManager.allocateBytes(
                appSpecificInternalDirUuid, NUM_BYTES_NEEDED_FOR_MY_APP
            )
        } else {
            val storageIntent = Intent().apply {
                // To request that the user remove all app cache files instead, set
                // "action" to ACTION_CLEAR_APP_CACHE.
                action = ACTION_MANAGE_STORAGE
            }
        }
        Log.d(
            TAG,
            "${this.javaClass.simpleName} queryFreeSpace() ${availableBytes / 1024 / 1024} MB"
        )
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createFileExternalStorage(pickerInitialUri: Uri) {
        Log.d(TAG, "${this.javaClass.simpleName} createFileExternalStorage() pickerInitialUri $pickerInitialUri")
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/pdf"
            //type = "*/*"
            putExtra(Intent.EXTRA_TITLE, displayName)

            // Optionally, specify a URI for the directory that should be opened in
            // the system file picker before your app creates the document.
            putExtra(DocumentsContract.EXTRA_INITIAL_URI, pickerInitialUri)
        }
        startActivityForResult(intent, CREATE_FILE)
    }

    private fun getRoot() {
        val root = getExternalFilesDir(null)
        Log.d(TAG, "${this.javaClass.simpleName} getRoot() ${root?.canonicalPath}")
    }

    /**
     * writing bytes from Uri
     * needed to write a file to the specified folder(API 29 and higher)
     * @param uri file to which we write bytes
     */
    private fun writeFromUri(uri: Uri){
        val outputStream = contentResolver.openOutputStream(uri)
        outputStream.use {
            it?.write(bytes!!)
        }
    }

    /**
     * reading bytes from uri
     * needed to read from  specified file(API 29 and higher)
     * @param uri file from which we read bytes
     */

    private fun readFileFromUri(uri: Uri){
        val inputStream = contentResolver.openInputStream(uri)
        inputStream.use {
            bytes = it?.readBytes()
        }
    }


    /**
     * write data to /storage/emulated/0/Android/data/com.example.app/files/Download
     * (the directory can be changed with Environment.DIRECTORY_DOCUMENTS)
     * this method work for API 29 and higher
     */
    private  fun writeFileAPI29(uri: Uri){
        Log.d(TAG, "${this.javaClass.simpleName} writeFileAPI29() uri $uri ")

        val file = File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)?.path, displayName)
        val inputStream = contentResolver.openInputStream(uri)
        val outputStream = FileOutputStream(file)
        val buf = ByteArray(1024)
        var len: Int

        inputStream.use { inputStream ->
            while (inputStream!!.read(buf).also { len = it } > 0) {
                Log.d(TAG, "${this.javaClass.simpleName} writeFile() ${inputStream.available()}")
                outputStream.write(buf, 0, len)
            }
        }

        /*var inputStream: InputStream? = null
        var outputStream: OutputStream? = null
        val file = File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)?.path, displayName)
        //val file = File("/storage/emulated/0/Download", displayName)
        try {
            inputStream = contentResolver.openInputStream(uri)
            outputStream = FileOutputStream(file)
            val buf = ByteArray(1024)
            var len: Int
            while (inputStream!!.read(buf).also { len = it } > 0) {
                Log.d(TAG, "${this.javaClass.simpleName} writeFile() ${inputStream.available()}")
                outputStream.write(buf, 0, len)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            try {
                inputStream?.close()
                outputStream?.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }*/
    }

    /**
     * write data to /storage/emulated/0/Android/data/com.example.app/files/Download
     * (the directory can be changed with Environment.DIRECTORY_DOCUMENTS)
     * this method work for API 29 and higher
     * also need add tag android:requestLegacyExternalStorage="true" in AndroidManifest.xml
     */
    private  fun writeFileBeforeAPI29(uri: Uri){
        val file = File("/storage/emulated/0/Download", displayName)
        val inputStream = contentResolver.openInputStream(uri)
        val outputStream = FileOutputStream(file)
        val buf = ByteArray(1024)
        var len: Int

        inputStream.use { ins ->
            while (ins!!.read(buf).also { len = it } > 0) {
                Log.d(TAG, "${this.javaClass.simpleName} writeFile() ${ins.available()}")
                outputStream.write(buf, 0, len)
            }
        }

    }

    /**
     * delete file from Uri
     */
    private fun deleteDocument(uri: Uri) {
        Log.d(TAG, "${this.javaClass.simpleName} deleteDocument()")
        DocumentsContract.deleteDocument(contentResolver, uri)
    }


}