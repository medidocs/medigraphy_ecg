package com.medigraphy.ecg;

import java.io.File;
import java.io.FilenameFilter;
import java.io.RandomAccessFile;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.comparator.LastModifiedFileComparator;


public class Filechooser extends Activity {

    // Stores names of traversed directories
    ArrayList<String> str = new ArrayList<String>();
    // Check if the first level of the directory structure is the one showing
    private Item[] fileList;
    private File path = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS) + "/ECG", "ECG-Recorder Reports");
    private String chosenFile;
    private static final int DIALOG_LOAD_FILE = 1000;
    public static File sel;

    int iBackPress = 0;
    public boolean BACK = false;
    private String[] mFileList;
    private String[] myFileListPdf;
    boolean diriscreated = false;
    boolean isFirst = false;
    ArrayList<String> listItems;
    ArrayAdapter<String> arrayAdapter;
    Dialog dialogItem;
    Dialog dialog;

    @SuppressWarnings("deprecation")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_filechooser);
        int actionBarTitleId = Resources.getSystem().getIdentifier("action_bar_title", "id", "android");
        if (actionBarTitleId > 0) {
            TextView title = (TextView) findViewById(actionBarTitleId);
            if (title != null)
                title.setTextColor(Color.WHITE);
        }

        ActionBar ab = getActionBar();
        ab.setBackgroundDrawable(getResources().getDrawable(R.drawable.action_bar_background_color));
        ab.setDisplayUseLogoEnabled(false);
        ab.setDisplayShowHomeEnabled(false);
        ab.show();
        setTitleColor(Color.WHITE);
        setTitle("ECG..");
        //setTitle("Patient Info...");
        loadFileList();

        showDialog(DIALOG_LOAD_FILE);

        listItems = new ArrayList<>(Arrays.asList(mFileList));

        dialog = new Dialog(this);
        dialog.setTitle("Choose file to view report.");
        dialog.setContentView(R.layout.dialog_listview);

        dialogItem = new Dialog(Filechooser.this);
        dialogItem.setTitle("Choose file to view report.");
        dialogItem.setContentView(R.layout.dialog_listview);

        final ListView listViewPdf = (ListView) dialogItem.findViewById(R.id.listview);
        EditText textpdf = dialogItem.findViewById(R.id.search);
        textpdf.setVisibility(View.GONE);

        final ListView listView = (ListView) dialog.findViewById(R.id.listview);
        EditText text = dialog.findViewById(R.id.search);
        arrayAdapter = new ArrayAdapter<String>(this, R.layout.list_item, R.id.tv, listItems);
        listView.setAdapter(arrayAdapter);

        dialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
            @Override
            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                if(keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_UP)
                    finish();
                return false;
            }
        });

        //Search in Dialog File
        text.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                if (charSequence.toString().equals("")) {

                    listItems = new ArrayList<>(Arrays.asList(mFileList));
                    arrayAdapter = new ArrayAdapter<String>(Filechooser.this, R.layout.list_item, R.id.tv, listItems);
                    listView.setAdapter(arrayAdapter);

                } else {
                    //Filter based on Items in Dialog
                    searchItems(charSequence.toString().toLowerCase());
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

                isFirst = true;
                chosenFile = listItems.get(i);
                sel = new File(path + "/" + chosenFile);

                if (sel.isDirectory()) {
                    // Adds chosen directory to list
                    str.add(chosenFile);
                    fileList = null;
                    path = new File(sel + "");
                    loadFileList();
                    dialog.dismiss();

                    arrayAdapter = new ArrayAdapter(Filechooser.this, R.layout.list_item, R.id.tv, myFileListPdf);
                    listViewPdf.setAdapter(arrayAdapter);

                    listViewPdf.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

                            chosenFile = myFileListPdf[i];
                            sel = new File(path + "/" + chosenFile);

                            if (sel.getName().endsWith(".dat") == false) {
                                Toast.makeText(getApplicationContext(), "Select .dat file", Toast.LENGTH_SHORT).show();
                                onBackPressed();

                            } else {
                                // Perform action with file picked
                                iBackPress = 1;
                                read_data(sel);
                                dialogItem.dismiss();

                            }
                        }
                    });

                    dialogItem.show();
                } else {

                }
            }
        });

        // Delete File Code
        //When User Press Long Press on Particular File is going to Delete
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {

                dialogItem.dismiss();

                String name = (String) adapterView.getItemAtPosition(i);

                final AlertDialog.Builder adb = new AlertDialog.Builder(Filechooser.this);
                adb.setTitle("Delete?");
                adb.setMessage("Are you sure you want to delete " + name);
                final int positionToRemove = i;
                final File deleteFile = new File(path + "/" + mFileList[i]);

                adb.setPositiveButton("Ok", new AlertDialog.OnClickListener() {

                    public void onClick(DialogInterface dialogI, int which) {

                        if (deleteFile.isDirectory()) {

                            String[] children = deleteFile.list();
                            for (int i = 0; i < children.length; i++) {
                                new File(deleteFile, children[i]).delete();
                            }
                            deleteFile.delete();
                            dialog.show();
                        }

                        listItems.remove(positionToRemove);
                        arrayAdapter.notifyDataSetChanged();
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
                return false;
            }
        });
        dialog.show();
    }

    //search item Function
    private void searchItems(String toString) {
        for (String item : mFileList) {
            if (!item.toLowerCase().contains(toString)) {
                listItems.remove(item);
            }
        }
        arrayAdapter.notifyDataSetChanged();
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

                File[] files = path.listFiles();
                Arrays.sort(files, LastModifiedFileComparator.LASTMODIFIED_REVERSE);
                displayFiles(files);

                if (isFirst) {

                    myFileListPdf = mFileList;

                    //Shorting Based on Date and Time
                    Collections.sort(Arrays.asList(mFileList), new Comparator<String>() {

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

                fileList = new Item[counter];

                for (int j = 0; j < mFileList.length; j++) {
                    if (mFileList[j].endsWith(".dat") == true) {
                        fileList[counter] = new Item(mFileList[j]);
                        counter++;
                    }
                }
            } else {

                Toast.makeText(getApplicationContext(), "path does not exist..\nplease create new file first..", Toast.LENGTH_SHORT).show();
                mFileList = new String[0];
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    ArrayList<String> stringArrayList = new ArrayList<String>();

    public void displayFiles(File[] files) {

        stringArrayList.clear();

        for (File file : files) {
            if ((file.isFile() && file.getName().endsWith(".dat")) || file.isDirectory()) {
                stringArrayList.add(FilenameUtils.getName(file.getName()));
            }
        }
        mFileList = stringArrayList.toArray(new String[stringArrayList.size()]);
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

/*	protected Dialog onCreateDialog(int id) {
	    Dialog dialog = null;
	    AlertDialog.Builder builder = new Builder(this);

		switch(id)
	    {
	        case DIALOG_LOAD_FILE:
	            builder.setTitle("Choose file to load/view data.");

		    	if(mFileList == null)
	            {	             
	                dialog = builder.create();
	                return dialog;
	            }
	            builder.setItems(mFileList, new DialogInterface.OnClickListener() {
	                @SuppressWarnings("deprecation")
					public void onClick(DialogInterface dialog, int which) 
	                {
	                	isFirst=true;
	                	chosenFile = mFileList[which];
	                	sel = new File(path + "/" + chosenFile);
						if (sel.isDirectory())
						{
							// Adds chosen directory to list
							str.add(chosenFile);
							fileList = null;
							path = new File(sel + "");							
							loadFileList();							
							removeDialog(DIALOG_LOAD_FILE);
							showDialog(DIALOG_LOAD_FILE);						
						}
						else 
						{
							if(sel.getName().endsWith(".dat")==false)
							{
								Toast.makeText(getApplicationContext(), "Select .dat file", Toast.LENGTH_SHORT).show();
								onBackPressed();

							}
							else
							{	
								// Perform action with file picked
									iBackPress=1;
									read_data(sel);


							}
						}
	                }
	            });
	            break;
	    }
	    dialog = builder.show();
	    return dialog;
	}*/

    private void read_data(File sel) {
        try {
            RandomAccessFile raf = new RandomAccessFile(sel, "rw");
            raf.readUTF();//title
            MainActivity.sdf1 = raf.readUTF();//date
            MainActivity.schno = raf.readUTF();//chssno
            MainActivity.sname = raf.readUTF();//name
            MainActivity.sdob = raf.readUTF();//dob//
            MainActivity.sage = Integer.parseInt(raf.readUTF());//age//
            MainActivity.sgen = raf.readUTF();//gende
            MainActivity.sht = Integer.parseInt(raf.readUTF());//height
            MainActivity.swt = Integer.parseInt(raf.readUTF());//weight
            MainActivity.smedi = raf.readUTF();//medi
            MainActivity.sBP = raf.readUTF();//bp
            MainActivity.iGain = raf.readInt();
            ;//gain
            paintView.filter_state = raf.readInt();//filter
            raf.seek(1023);

            for (int j = 0; j < 12; j++) {
                for (int k = 0; k < paintView.total_samples; k++) {
                    paintView.raw_data[j][k] = (short) raf.readInt();
                    paintView.Gen_report[j][k] = paintView.raw_data[j][k];
                }
            }
            raf.close();//SNB: CONFIRM
            paintView.START_GUI = true;
            MainActivity.refresh_data = true;
            Filechooser.this.finish();

            //calls patient info class if any changes in the patient details to be done
//            Intent i = new Intent(getApplicationContext(), Patient_Info.class);
//            startActivity(i);

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Error in Noo of text lines: " + e.getMessage());
        }
    }

    //exit the application on pressing of back button
    @Override
    public void onBackPressed() {
        dialog.dismiss();
        finish();
    }
}

