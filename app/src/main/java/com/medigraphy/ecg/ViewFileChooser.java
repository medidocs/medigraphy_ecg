package com.medigraphy.ecg;

import static android.view.View.GONE;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import com.github.barteksc.pdfviewer.PDFView;
import com.github.barteksc.pdfviewer.listener.OnLoadCompleteListener;
import com.github.barteksc.pdfviewer.listener.OnPageChangeListener;
import com.github.barteksc.pdfviewer.scroll.DefaultScrollHandle;
import com.medigraphy.ecg.Adapter.PdfListAdapter;
import com.shockwave.pdfium.PdfDocument;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.comparator.LastModifiedFileComparator;

import java.io.File;
import java.net.URLConnection;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ViewFileChooser extends Activity implements OnPageChangeListener, OnLoadCompleteListener, PdfListAdapter.OnDelete {

    // Stores names of traversed directories
    ArrayList<String> str = new ArrayList<String>();
    // Check if the first level of the directory structure is the one showing
    private Item[] vfileList;
    private File path = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).getAbsolutePath() + "/ECG", "ECG-Recorder Reports");
    private String chosenFile;
    private static final int DIALOG_LOAD_FILE = 1000;
    public static File sel;
    public boolean BACK = false;
    private String[] myFileList;
    private String[] duplicateListArray;
    private String[] myFileListPdf;
    ArrayList<String> listItems;
    ArrayList<String> listItemsPdf;
    // ArrayAdapter<String> arrayAdapter;
    boolean diriscreated = false;
    private static final String TAG = ViewFileChooser.class.getSimpleName();
    PDFView pdfView;
    Integer pageNumber = 0;
    String pdfFileName;
    boolean isFirst = false;
    Dialog dialog;
    String UserFname, UserLname;

    SharedPreferences loginPref;
    SharedPreferences.Editor loginEditor;
    boolean OpenPDF = false;
    ListView listView;

    ArrayList<String> arrDuplicatePdf = new ArrayList<>();
    PdfListAdapter pdfListAdapter;
    boolean isFirstTimeOpen = false;
    ArrayList<File> arrPDFLists = new ArrayList<>();
    boolean isMultiSelect = false, isDuplicateData = false;
    ArrayList<File> arrSelectedPdfs = new ArrayList<>();
    ImageView imgShare;

    @SuppressWarnings("deprecation")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_viewfilechooser);
        loginPref = getSharedPreferences("LOGIN_PREF", MODE_PRIVATE);
        loginEditor = loginPref.edit();
        int actionBarTitleId = Resources.getSystem().getIdentifier("action_bar_title", "id", "android");
        if (actionBarTitleId > 0) {
            TextView title = (TextView) findViewById(actionBarTitleId);
            if (title != null)
                title.setTextColor(Color.WHITE);
        }

        /*ActionBar ab = getActionBar();
        if(ab!=null){
            ab.setBackgroundDrawable(getResources().getDrawable(R.drawable.action_bar_background_color));
            ab.setDisplayUseLogoEnabled(false);
            ab.setDisplayShowHomeEnabled(false);
            ab.show();
        }*/

        setTitleColor(Color.WHITE);
        setTitle("Reports ...");
        //setTitle("Patient Info...");
        loadFileList();
        //showDialog(DIALOG_LOAD_FILE);

        getAllPdfs();

        //listItems = new ArrayList<>(Arrays.asList(myFileList));
        dialog = new Dialog(this);
        dialog.setTitle("Choose file to view report.");
        dialog.setContentView(R.layout.dialog_listview);
        dialog.setCanceledOnTouchOutside(false);
        listView = (ListView) dialog.findViewById(R.id.listview);
        EditText text = dialog.findViewById(R.id.search);
        imgShare = dialog.findViewById(R.id.imgShare);

        imgShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (arrSelectedPdfs.size() > 0) {
                    ArrayList<Uri> uris = new ArrayList<>();
                    for (File file : arrSelectedPdfs) {
                        uris.add(FileProvider.getUriForFile(
                                ViewFileChooser.this,
                                "com.medigraphy.ecg", //(use your app signature + ".provider" )
                                file));
                    }
                    Intent share = new Intent(Intent.ACTION_SEND_MULTIPLE);
                    share.setType("application/pdf");
                    share.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                    share.putExtra(Intent.EXTRA_STREAM, uris);
                    share.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(share);
                }
            }
        });

        // arrayAdapter = new ArrayAdapter<String>(this, R.layout.list_item, R.id.tv, listItems);
        // listView.setAdapter(arrayAdapter);

        pdfListAdapter = new PdfListAdapter(this, listItems, arrDuplicatePdf, this);
        listView.setAdapter(pdfListAdapter);

        //Dialog set to non Cancelable
        dialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
            @Override
            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP)
                    finish();
                return false;
            }
        });

        //Search Functionality in Dialog
        text.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                if (charSequence.toString().trim().isEmpty()) {
                    //  listItems = new ArrayList<>(Arrays.asList(myFileList));
                    pdfListAdapter = new PdfListAdapter(ViewFileChooser.this, listItems, arrDuplicatePdf, ViewFileChooser.this);
                    listView.setAdapter(pdfListAdapter);
                } else {
                    searchItems(charSequence.toString().toLowerCase());
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        // Click on Any Item Open Particular File
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                if (isMultiSelect) {
                    multi_select(i);
                } else {
                    if (!OpenPDF) {
                        OpenPDF = true;
                        isFirst = true;
                        chosenFile = listItems.get(i);
                        sel = new File(new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).getAbsolutePath() + "/ECG", "ECG-Recorder Reports") + "/" + chosenFile);

                        if (sel.isDirectory()) {
                            // Adds chosen directory to list
                            str.add(chosenFile);
                            vfileList = null;
                            path = new File(sel + "");

                            loadFileList();
                            dialog.dismiss();

                            listItemsPdf = new ArrayList<>(Arrays.asList(myFileListPdf));

                            EditText text = dialog.findViewById(R.id.search);

                            text.setVisibility(View.GONE);
                        /*    arrayAdapter = new ArrayAdapter(ViewFileChooser.this, R.layout.list_item, R.id.tv, listItemsPdf);
                            pdfListAdapter.deleteFunctionality(false);
                            listView.setAdapter(arrayAdapter);*/

                            pdfListAdapter = new PdfListAdapter(ViewFileChooser.this, listItemsPdf, arrDuplicatePdf, ViewFileChooser.this);
                            pdfListAdapter.deleteFunctionality(false);
                            listView.setAdapter(pdfListAdapter);
                            dialog.show();
                        } else {

                        }
                    } else {
                        OpenPDF = false;
                        chosenFile = myFileListPdf[i];
                        sel = new File(path + "/" + chosenFile);

                        if (sel.getName().endsWith(".pdf") == false) {
                            Toast.makeText(getApplicationContext(), "Select .pdf file", Toast.LENGTH_SHORT).show();
                            onBackPressed();
                        } else {
                            // Perform action with file picked
                            displayFromAsset(sel);
                            dialog.dismiss();
                        }
                    }
                }
            }
        });

        //if User Press Long click to Any Items then it is going to Delete
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
                if (!isMultiSelect) {
                    imgShare.setVisibility(View.VISIBLE);
                    isMultiSelect = true;
                    multi_select(i);
                }
                return true;
            }
        });
        dialog.show();
    }

    private void multi_select(int i) {
        if (arrSelectedPdfs.size() > 0) {
            if (arrSelectedPdfs.contains(arrPDFLists.get(i))) {
                arrSelectedPdfs.remove(arrPDFLists.get(i));
                arrDuplicatePdf.remove(listItems.get(i));
                if (arrDuplicatePdf.size() == 0) {
                    isMultiSelect = false;
                    imgShare.setVisibility(GONE);
                }
            } else {
                arrSelectedPdfs.add(arrPDFLists.get(i));
                arrDuplicatePdf.add(listItems.get(i));
            }
        } else {
            arrSelectedPdfs.add(arrPDFLists.get(i));
            arrDuplicatePdf.add(listItems.get(i));
        }
        refreshAdapter();
    }

    private void refreshAdapter() {
        pdfListAdapter.arrDuplicatePDFList = arrDuplicatePdf;
        pdfListAdapter.arrPdfList = listItems;
        pdfListAdapter.notifyDataSetChanged();
    }

    //Search Item Function
    private void searchItems(String toString) {

        for (String item : duplicateListArray) {
            if (!item.toLowerCase().contains(toString)) {
                listItems.remove(item);
            }
        }

        pdfListAdapter.notifyDataSetChanged();
    }

    private void getAllPdfs() {
        path = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS) + "/ECG", "ECG-Recorder Reports");
        loadFileList();
        isFirstTimeOpen = false;

        listItems = new ArrayList<>(Arrays.asList(myFileList));
        isFirst = true;
        for (int i = 0; i < listItems.size(); i++) {
            chosenFile = listItems.get(i);
            File sel = new File(new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).getAbsolutePath() + "/ECG", "ECG-Recorder Reports") + "/" + chosenFile);
            if (sel.isDirectory()) {
                // Adds chosen directory to list
                str.add(chosenFile);
                vfileList = null;
                path = new File(sel + "");
                loadFileList();
                listItemsPdf = new ArrayList<>(Arrays.asList(myFileListPdf));
                if (listItemsPdf.size() > 0) {
                    path = new File(sel + "/" + listItemsPdf.get(0));
                    if (path.exists()) {
                        arrPDFLists.add(path);
                        Log.e(TAG, "getAllPdfs: ");
                    } else {
                        Log.e(TAG, "getAllPdfs: ");
                    }
                }

               /* EditText text = dialog.findViewById(R.id.search);
                text.setVisibility(View.GONE);
                arrayAdapter = new ArrayAdapter<String>(MainActivity.this, R.layout.list_item, R.id.tv, listItemsPdf);
                listView.setAdapter(arrayAdapter);*/
            }
        }
    }


    private void loadFileList() {
        try {
            diriscreated = path.mkdirs();
        } catch (SecurityException e) {
            //e.printStackTrace();
        }

        try {
            int counter = 0;
            if (path.exists()) {
	/*		    FilenameFilter filter = new FilenameFilter()
			    {
			        @Override
			        public boolean accept(File dir, String filename)
			        {
			            File sel = new File(dir, filename);
			            return ((sel.isFile() && sel.getName().endsWith(".pdf")) || sel.isDirectory()) ;
			        }
			    };
			    myFileList = path.list(filter);*/
                File[] files = path.listFiles();
                Arrays.sort(files, LastModifiedFileComparator.LASTMODIFIED_REVERSE);

                displayFiles(files);

                isDuplicateData = true;

                if (isFirst) {

                    //Sorting of Files based on Date and time
                    myFileListPdf = myFileList;
                    Collections.sort(Arrays.asList(myFileList), new Comparator<String>() {

                        DateFormat f = new SimpleDateFormat("yyyy_MM_dd HH-mm");

                        @Override
                        public int compare(String lhs, String rhs) {

                            String[] parts = lhs.split("_");
                            String dateLHS = parts[0] + "_" + parts[1] + "_" + parts[2] + " " + parts[3];

                            String[] partsRHS = rhs.split("_");
                            String dateLHSRHS = partsRHS[0] + "_" + partsRHS[1] + "_" + partsRHS[2] + " " + partsRHS[3];

                            try {
                                return f.parse(dateLHSRHS).compareTo(f.parse(dateLHS));
                            } catch (ParseException e) {
                                throw new IllegalArgumentException(e);
                            }
                        }
                    });
                }

                vfileList = new Item[counter];
                for (int j = 0; j < myFileList.length; j++) {
                    if (myFileList[j].endsWith(".pdf")) {
                        vfileList[counter] = new Item(myFileList[j]);
                        counter++;
                    }
                }
            } else {

                Toast.makeText(getApplicationContext(), "path does not exist..\nplease create new file first..", Toast.LENGTH_SHORT).show();
                myFileList = new String[0];
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    ArrayList<String> stringArrayList = new ArrayList<String>();

    public void displayFiles(File[] files) {
        stringArrayList.clear();
        for (File file : files) {
            if ((file.isFile() && file.getName().endsWith(".pdf")) || file.isDirectory()) {
                stringArrayList.add(FilenameUtils.getName(file.getName()));
            }
        }

        myFileList = stringArrayList.toArray(new String[stringArrayList.size()]);

        if (!isDuplicateData) {
            duplicateListArray = stringArrayList.toArray(new String[stringArrayList.size()]);
        }


        Log.e(TAG, "displayFiles: ");
    }

    @Override
    public void onClickDelete(int position) {
        String name = (String) listItems.get(position);
        final AlertDialog.Builder adb = new AlertDialog.Builder(ViewFileChooser.this);
        adb.setTitle("Delete?");
        adb.setMessage("Are you sure you want to delete " + name);
        final int positionToRemove = position;
        final File deleteFile = new File(path + "/" + listItems.get(position));
        adb.setPositiveButton("Ok", new AlertDialog.OnClickListener() {
            public void onClick(DialogInterface dialogI, int which) {
                if (deleteFile.isDirectory()) {
                    String[] children = deleteFile.list();
                    for (int i = 0; i < children.length; i++) {
                        new File(deleteFile, children[i]).delete();
                    }
                    deleteFile.delete();
                }
                listItems.remove(positionToRemove);
                pdfListAdapter.notifyDataSetChanged();
                dialog.show();
            }
        });

        adb.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
                dialog.show();
            }
        });
        dialog.dismiss();
        adb.show();
    }

    private class Item {
        public String file;

        public Item(String file) {
            this.file = file;
        }

        @Override
        public String toString() {
            return file;
        }
    }

/*    protected Dialog onCreateDialog(int id) {

        Dialog dialog = null;
        Builder builder = new Builder(this);

        switch (id) {

            case DIALOG_LOAD_FILE:

                builder.setTitle("Choose file to view report.");

                if (myFileList == null) {
                    dialog = builder.create();
                    return dialog;
                }

          */

    /*      builder.setItems(myFileList, new DialogInterface.OnClickListener() {
                    @SuppressWarnings("deprecation")
                    public void onClick(DialogInterface dialog, int which) {
                        isFirst = true;
                        chosenFile = myFileList[which];
                        sel = new File(path + "/" + chosenFile);
                        if (sel.isDirectory()) {
                            // Adds chosen directory to list
                            str.add(chosenFile);
                            vfileList = null;
                            path = new File(sel + "");
                            loadFileList();
                            removeDialog(DIALOG_LOAD_FILE);
                            showDialog(DIALOG_LOAD_FILE);
                        } else {
                            if (sel.getName().endsWith(".pdf") == false) {
                                Toast.makeText(getApplicationContext(), "Select .pdf file", Toast.LENGTH_SHORT).show();
                                onBackPressed();

                            } else {
                                // Perform action with file picked

                                displayFromAsset(sel);


                            }
                        }
                    }
                });*/

    /*
                break;
        }
   //     dialog = builder.show();
        return dialog;
    }*/

    private void displayFromAsset(File pdfFileName) {
        ActionBar ab = getActionBar();
        ab.hide();

        if (pdfFileName.exists() && pdfFileName.canRead()) {
            pdfView = (PDFView) findViewById(R.id.pdfView);

            pdfView.fromFile(pdfFileName)
                    .defaultPage(pageNumber)
                    .enableSwipe(true)
                    .swipeHorizontal(false)
                    .onPageChange(this)
                    .enableAnnotationRendering(true)
                    .onLoad(this)
                    .scrollHandle(new DefaultScrollHandle(this))
                    .load();
        }
    }

    @Override
    public void onPageChanged(int page, int pageCount) {
        pageNumber = page;
        setTitle(String.format("%s %s / %s", pdfFileName, page + 1, pageCount));
    }

    @Override
    public void loadComplete(int nbPages) {
        PdfDocument.Meta meta = pdfView.getDocumentMeta();
        printBookmarksTree(pdfView.getTableOfContents(), "-");
    }


    public void printBookmarksTree(List<PdfDocument.Bookmark> tree, String sep) {

        for (PdfDocument.Bookmark b : tree) {

            Log.e(TAG, String.format("%s %s, p %d", sep, b.getTitle(), b.getPageIdx()));

            if (b.hasChildren()) {
                printBookmarksTree(b.getChildren(), sep + "-");
            }
        }
    }

    //exit the application on pressing of back button
    @Override
    public void onBackPressed() {
        dialog.dismiss();
        finish();
//        BACK = true;
//        Intent i = new Intent(this, MainActivity.class);
//        startActivity(i);
//        finish();
    }
}

