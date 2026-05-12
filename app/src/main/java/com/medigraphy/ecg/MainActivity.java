package com.medigraphy.ecg;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.URLConnection;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Vibrator;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.FragmentActivity;

import android.provider.MediaStore;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Window;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.github.barteksc.pdfviewer.PDFView;
import com.github.barteksc.pdfviewer.listener.OnLoadCompleteListener;
import com.github.barteksc.pdfviewer.listener.OnPageChangeListener;
import com.github.barteksc.pdfviewer.scroll.DefaultScrollHandle;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.medigraphy.ecg.Adapter.PdfListAdapter;
import com.mikhaellopez.circularimageview.CircularImageView;
import com.shockwave.pdfium.PdfDocument;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.comparator.LastModifiedFileComparator;

import static android.view.View.GONE;

@SuppressLint("HandlerLeak")
public class MainActivity extends FragmentActivity implements OnClickListener, OnPageChangeListener, OnLoadCompleteListener, GoogleApiClient.OnConnectionFailedListener, PdfListAdapter.OnDelete {
    public static final String TAG = "ECGRec";
    public static int f_size, no_of_lead_to_display = 12, total_pages,
            lead_page = 0, step, selectedPosition = 12, item_id_acq_mode,
            item_id_gain, item_id_filter = 1, mode;
    public static String selectedGain = "10mm/mv";
    public static MenuItem menu_id;
    public static String mState = "HIDE_MENU";
    protected static final int START_DATA_ACQ = 1;
    protected static final int STOP_DATA = 2;
    protected static final int GENERATE_REPORT = 3;
    protected static final int LOAD_DATA = 5;
    protected static final int MESSAGE_COMMUNICATION = 6;
    protected static final int NEW_PATIENT = 8;
    final AnimatorSet mAnimationSet = new AnimatorSet();
    public static String settingsFolder = null;
    protected static AlertDialog alertDialog;
    public static boolean started = false;
    public static String battery_status = "N/A", v1_status = "Not Connected", v6_status = "Not Connected", v2_status = "Not Connected", v3_status = "Not Connected", v4_status = "Not Connected", v5_status = "Not Connected", ll_status = "Not Connected", la_status = "Not Connected", ra_status = "Not Connected";
    public boolean load;

    public static InputStream instrm;
    public static OutputStream outstrm;
    public static MenuItem device_detail_menu, gain_menu, filter_menu, add_patient_info, load_menu, next_menu, demo_menu, prev_menu, view_report,
            mode_menu, battery, manual_menu, new_patient, export_to_ascii, acq_mode, pg_one, pg_two, pg_three, pg_four, next_lead, prev_lead, repeat_acq;
    public boolean next_and_prev_clicked = false,
            menu_clicked = false;
    public static boolean report_gen = false,
            filter_change = false,
            gain_change = false,
            load_existing_file = false,
            send_report = false,
            acq_repeat = false,
            acq_mode_change = false;
    int strcounter = 0;
    double RAM_SIZE = 0;
    public static int report_sequential, iGain;
    paintView pv;
    Display d;
    Create_report cr;
    public static int iScreenWidth, iScreenHeight;
    Button bt_new;
    Button bt_load;
    Button bt_lead;
    Button bt_view;
    static Button bt_start;
    Button bt_start_2;
    Button bt_gen;
    String comment = "";
    static TextView title;
    public static boolean stop = false, demo_start = false, refresh_data = false, menu_press = false, create_image = false,
            create_pdf = false, create_data = false, create_text = false, lead_pressed = false, loggerCreated = false, isFormSub = false;
    public Vibrator myVib;
    ArrayList<String> str = new ArrayList<String>();
    private Item[] vfileList;
    private File path = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).getAbsolutePath() + "/ECG", "ECG-Recorder Reports");
    private String chosenFile;
    public static File sel;
    private String[] myFileList;
    private String[] myFileListPdf;
    ArrayList<String> listItems = new ArrayList<>();
    ArrayList<String> arrDuplicatePdf = new ArrayList<>();
    ArrayList<File> arrPDFLists = new ArrayList<>();
    ArrayList<File> arrSelectedPdfs = new ArrayList<>();
    ArrayList<String> listItemsPdf;
    PdfListAdapter pdfListAdapter;
    boolean diriscreated = false;
    PDFView pdfView;
    Integer pageNumber = 0;
    String pdfFileName;
    boolean isFirst = false;
    boolean isFirstTimeOpen = false;
    Dialog dialog;
    View viewFile;
    SharedPreferences reportPreference;
    SharedPreferences.Editor reportEditor;
    JsonArray reportJsonArray;
    SharedPreferences preferences;
    SharedPreferences.Editor editor;
    ProgressDialog Pdialog;
    boolean isLoadFile = false;
    ProgressDialog progressDialog;
    boolean isMultiSelect = false;
    ImageView imgShare;


    Uri imageUri;
    CircularImageView simage;
    public MainActivity activity;
    Button save, clear, cancel, upload;
    public static LinearLayout liviewforage;
    public static EditText name, age, chno, medi, BP, dob, ht, wt;
    public static RadioGroup gender;
    public static RadioButton selectedGender;
    public static TextView date;
    public static String sname, sgen, schno, smedi, sBP, sdob;
    public static int sage, sht, swt;
    public static String sdf, sdf1;
    public static boolean data_entered = false;
    final Calendar myCalendar = Calendar.getInstance();
    private static final int CAMERA_REQUEST = 1888;
    paintView paintV;


    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
    }

    @Override
    public void onClickDelete(int position) {
        String name = (String) listItems.get(position);

        final AlertDialog.Builder adb = new AlertDialog.Builder(MainActivity.this);
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

    @SuppressWarnings("deprecation")
    private void initialize_GUI() {
        try {
            //to run application full screen
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
            //to keep the screen on while application is running
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            //to change the text color of title
            int actionBarTitleId = Resources.getSystem().getIdentifier("action_bar_title", "id", "android");
            if (actionBarTitleId > 0) {
                title = (TextView) findViewById(actionBarTitleId);
                if (title != null)
                    title.setTextColor(Color.WHITE);
            }
            //set the color of titlebar
            ActionBar ab = getActionBar();
            ab.setDisplayUseLogoEnabled(false);

            ab.setBackgroundDrawable(getResources().getDrawable(R.drawable.action_bar_background_color));
            ab.hide();
            myVib = (Vibrator) this.getSystemService(VIBRATOR_SERVICE);

            //initialize GUI components
            bt_new = (Button) findViewById(R.id.new_main_activity_record_test_btn);
            bt_new.setOnClickListener(this);
            bt_load = (Button) findViewById(R.id.new_main_activity_view_report_btn);
            bt_load.setOnClickListener(this);
            bt_lead = (Button) findViewById(R.id.new_main_activity_select_lead_btn);
            bt_lead.setOnClickListener(this);
            bt_view = (Button) findViewById(R.id.view_button);
            bt_view.setOnClickListener(this);

            //getting the height and width of tablet/mobile
            d = getWindowManager().getDefaultDisplay();
            iScreenWidth = d.getWidth();
            iScreenHeight = d.getHeight();
            Log.i(TAG, "initialize_GUI() completed");
        } catch (Exception e) {
            Log.e(TAG, "Error in initialize_GUI()", e.getCause());
            e.printStackTrace();
        }
    }

    ListView listView;
    SharedPreferences loginPref;
    SharedPreferences.Editor loginEditor;

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_new);

        preferences = getSharedPreferences("LOGIN_PREF", MODE_PRIVATE);
        editor = preferences.edit();
//        Constant_Class.context = MainActivity.this;
        reportPreference = this.getSharedPreferences("REPORT_DETAILS", MODE_PRIVATE);
        reportEditor = reportPreference.edit();


        loginPref = getSharedPreferences("LOGIN_PREF", MODE_PRIVATE);
        loginEditor = loginPref.edit();
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Please wait...");

        if (canGetLocation()) {
            //DO SOMETHING USEFUL HERE. ALL GPS PROVIDERS ARE CURRENTLY ENABLED
        } else {
            //SHOW OUR SETTINGS ALERT, AND LET THE USE TURN ON ALL THE GPS PROVIDERS
            showSettingsAlert();
        }

   /*   ImageView imgShare = findViewById(R.id.imgShare);
        imgShare.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {

                if (arrPDFLists.size() > 0) {
                    ArrayList<Uri> uris = new ArrayList<>();
                    for (File file : arrPDFLists) {
                        uris.add(FileProvider.getUriForFile(
                                MainActivity.this,
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
        });*/

        if (reportPreference.getString("Report", "").equals("")) {

            JsonObject reportSimultaneous = new JsonObject();
            JsonObject reportSequential = new JsonObject();
            JsonObject report62Report = new JsonObject();
            JsonObject report61Report = new JsonObject();
            JsonObject report61Report2 = new JsonObject();
            reportJsonArray = new JsonArray();

            reportSimultaneous.addProperty("reportName", "Simultaneous");
            reportSimultaneous.addProperty("position", "0");
            reportSimultaneous.addProperty("isChecked", "false");

            reportSequential.addProperty("reportName", "Sequential");
            reportSequential.addProperty("position", "1");
            reportSequential.addProperty("isChecked", "false");

            report62Report.addProperty("reportName", "62Report");
            report62Report.addProperty("position", "2");
            report62Report.addProperty("isChecked", "false");

            report61Report.addProperty("reportName", "61Report");
            report61Report.addProperty("position", "3");
            report61Report.addProperty("isChecked", "true");

            report61Report2.addProperty("reportName", "61Report2");
            report61Report2.addProperty("position", "4");
            report61Report2.addProperty("isChecked", "true");

            reportJsonArray.add(reportSimultaneous);
            reportJsonArray.add(reportSequential);
            reportJsonArray.add(report62Report);
            reportJsonArray.add(report61Report);
            reportJsonArray.add(report61Report2);

            reportEditor.putString("Report", reportJsonArray.toString());
            reportEditor.apply();
        }

        checkPermission();
        String size;
        // set the directory to store the settings file.
        settingsFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS) + "/ECG/Settings";
        size = find_RAM_SIZE();     //Why to find out RAM size?    //RKJ //To calculate RAM_SIZE //SNB
        Log.i(TAG, "ram_size=" + size);
        initialize_GUI();// initialises the

        read_lead_number();//reads the selected lead value saved in file
        Filter_Gain_preference();//reads and sets the selected filter and gain value

        viewFile = findViewById(R.id.view_file);

        getAllPdfs();
        // File Dialog
        // Open Dialog in Main Screen of Load File and View Reports

        dialog = new Dialog(this);
        dialog.setTitle("Choose file to view report.");
        dialog.setContentView(R.layout.dialog_listview);
        dialog.setCanceledOnTouchOutside(false);

        listView = (ListView) dialog.findViewById(R.id.listview);
        EditText text = dialog.findViewById(R.id.search);
        imgShare = dialog.findViewById(R.id.imgShare);

        imgShare.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if (arrSelectedPdfs.size() > 0) {
                    ArrayList<Uri> uris = new ArrayList<>();
                    for (File file : arrSelectedPdfs) {
                        uris.add(FileProvider.getUriForFile(MainActivity.this, "com.medigraphy.ecg", file));
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

        pdfListAdapter = new PdfListAdapter(this, listItems, arrDuplicatePdf, this);
        listView.setAdapter(pdfListAdapter);

        //Search in Dialog
        text.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (charSequence.toString().trim().isEmpty()) {
                    listItems = new ArrayList<>(Arrays.asList(myFileList));
                    pdfListAdapter = new PdfListAdapter(MainActivity.this, listItems, arrDuplicatePdf, MainActivity.this);
                    listView.setAdapter(pdfListAdapter);
                } else {
                    searchItems(charSequence.toString().toLowerCase());
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        // When Click on Any Items its First Time Open PDF or .dat file based on Selection View Reports or Load File
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                if (isMultiSelect) {
                    multi_select(i);
                } else {
                    if (!isFirstTimeOpen) {

                        isFirst = true;
                        isFirstTimeOpen = true;
                        chosenFile = listItems.get(i);
                        sel = new File(path + "/" + chosenFile);

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

                            //arrayAdapter = new ArrayAdapter<String>(MainActivity.this, R.layout.list_item, R.id.tv, listItemsPdf);
                            pdfListAdapter = new PdfListAdapter(MainActivity.this, listItemsPdf, arrDuplicatePdf, MainActivity.this);
                            pdfListAdapter.deleteFunctionality(false);
                            listView.setAdapter(pdfListAdapter);
                            dialog.show();
                        }
                    } else {
                        isFirstTimeOpen = false;
                        chosenFile = myFileListPdf[i];
                        sel = new File(path + "/" + chosenFile);
                        // if it is From View Reports then Open PDFs else open .dat file
                        if (!isLoadFile) {
                            if (sel.getName().endsWith(".pdf") == false) {

                                Toast.makeText(getApplicationContext(), "Select .pdf file", Toast.LENGTH_SHORT).show();
                                dialog.dismiss();

                            } else {
                                // Perform action with file picked
                                displayFromAsset(sel);
                                dialog.dismiss();
                            }
                        } else {
                            if (sel.getName().endsWith(".dat") == false) {
                                Toast.makeText(getApplicationContext(), "Select .dat file", Toast.LENGTH_SHORT).show();
                                dialog.dismiss();
                            } else {
                                // Perform action with file picked
                                initialization_paintView();
                                pv.postInvalidate();
                                read_data(sel);
                                dialog.dismiss();
                            }
                        }
                    }
                }
            }
        });

        //SELECT MULTIPLE PDFS TO SHARE
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
                if (!isMultiSelect) {
                    imgShare.setVisibility(View.VISIBLE);
                    isMultiSelect = true;
                    multi_select(i);
                }
/*
                String name = (String) adapterView.getItemAtPosition(i);
                final AlertDialog.Builder adb = new AlertDialog.Builder(MainActivity.this);
                adb.setTitle("Delete?");
                adb.setMessage("Are you sure you want to delete " + name);
                final int positionToRemove = i;
                final File deleteFile = new File(path + "/" + listItems.get(i));
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
                        arrayAdapter.notifyDataSetChanged();
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
                adb.show();*/
                return true;
            }
        });
    }

    public boolean canGetLocation() {
        boolean result = true;
        LocationManager lm;
        boolean gpsEnabled = false;
        boolean networkEnabled = false;

        lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        // exceptions will be thrown if provider is not permitted.
        try {
            gpsEnabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch (Exception ex) {
        }

        try {
            networkEnabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch (Exception ex) {
        }

        return gpsEnabled && networkEnabled;
    }

    public void showSettingsAlert() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);

        // Setting Dialog Title
        alertDialog.setTitle("Enable Location!");

        // Setting Dialog Message
        alertDialog.setMessage("Please enable location ");

        // On pressing Settings button
        alertDialog.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(intent);
            }
        });
        alertDialog.show();
    }

    private void multi_select(int i) {
        if (arrSelectedPdfs.size() > 0) {
            if (arrPDFLists.size() > 0) {
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
            }
        } else {
            if (arrPDFLists.size() > 0) {
                arrSelectedPdfs.add(arrPDFLists.get(i));
                arrDuplicatePdf.add(listItems.get(i));
            }
        }

        refreshAdapter();
    }

    private void refreshAdapter() {
        pdfListAdapter.arrDuplicatePDFList = arrDuplicatePdf;
        pdfListAdapter.arrPdfList = listItems;
        pdfListAdapter.notifyDataSetChanged();
    }

    private void getAllPdfs() {
        try {
            path = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS) + "/ECG", "ECG-Recorder Reports");
            loadFileList();
            isFirstTimeOpen = false;
            listItems.clear();
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

                }
            }
        } catch (Exception e) {

        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CAMERA_REQUEST && resultCode == Activity.RESULT_OK) {
            Bitmap photo = (Bitmap) data.getExtras().get("data");
            this.imageUri = getImageUri(getBaseContext(), photo);
            Log.e(TAG, imageUri.toString());
            simage.setImageURI(imageUri);
        }

    }

    private boolean isNetworkConnected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        return cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().isConnected();
    }

    private void read_data(File sel) {
        try {
            RandomAccessFile raf = new RandomAccessFile(sel, "rw");
            raf.readUTF();//title

            String age = String.valueOf(sage);
            String hight = String.valueOf(sht);
            String weight = String.valueOf(swt);

            sdf1 = raf.readUTF();//date
            schno = raf.readUTF();//chssno
            sname = raf.readUTF();//name
            sdob = raf.readUTF();//dob//
            age = raf.readUTF();//age//
            sgen = raf.readUTF();//gende
            hight = raf.readUTF();//height
            weight = raf.readUTF();//weight
            smedi = raf.readUTF();//medi
            sBP = raf.readUTF();//bp
            MainActivity.iGain = raf.readInt();
            //gain
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

            //calls patient info class if any changes in the patient details to be done
            Patient_Info_Dialog();

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Error in Noo of text lines: " + e.getMessage());
        }
    }

    private void displayFromAsset(File pdfFileName) {

        ActionBar ab = getActionBar();
        ab.hide();

        if (pdfFileName.exists() && pdfFileName.canRead()) {
            viewFile.setVisibility(View.VISIBLE);
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

    private void loadFileList() {
        try {
            diriscreated = path.mkdirs();
        } catch (SecurityException e) {
            e.printStackTrace();
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
                if (!isLoadFile) {
                    displayFiles(files);
                } else {
                    displayDatFile(files);
                }

                if (isFirst) {

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

    // Get PDF Files
    public void displayFiles(File[] files) {

        stringArrayList.clear();

        for (File file : files) {
            if ((file.isFile() && file.getName().endsWith(".pdf")) || file.isDirectory()) {
                stringArrayList.add(FilenameUtils.getName(file.getName()));
            }
        }

        myFileList = stringArrayList.toArray(new String[stringArrayList.size()]);
        Log.e(TAG, "displayFiles: ");
    }

    //Get Load Files
    public void displayDatFile(File[] files) {
        stringArrayList.clear();

        for (File file : files) {
            if ((file.isFile() && file.getName().endsWith(".dat")) || file.isDirectory()) {
                stringArrayList.add(FilenameUtils.getName(file.getName()));
            }
        }
        myFileList = stringArrayList.toArray(new String[stringArrayList.size()]);
    }

    // Search in Dialog Based in Search KeyWords
    private void searchItems(String toString) {
        for (String item : myFileList) {
            if (!item.toLowerCase().contains(toString)) {
                listItems.remove(item);
            }
        }
        pdfListAdapter.notifyDataSetChanged();
    }

    //check Permission
    public boolean checkPermission() {
        if (Integer.parseInt(Build.VERSION.RELEASE) >= 12) {
            Log.e("================================>>>>>>>>>>><<<<<<<<<<<<<", "checkPermission: if");
            if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.CAMERA, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN}, 401);
                Toast.makeText(MainActivity.this, "Please agree with permission.", Toast.LENGTH_SHORT).show();
                return true;
            } else {
                return false;
            }
        } else {
            Log.e("================================>>>>>>>>>>><<<<<<<<<<<<<", "checkPermission: else");
            if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.CAMERA, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN}, 401);
                Toast.makeText(MainActivity.this, "Please agree with permission.", Toast.LENGTH_SHORT).show();
                return true;
            } else {
                return false;
            }
        }
    }

    private String find_RAM_SIZE() {
        RandomAccessFile reader = null;
        String load = null;
        DecimalFormat twoDecimalForm = new DecimalFormat("#.##");
        double totalRam = 0;
        String lastValue = "";
        try {
            reader = new RandomAccessFile("/proc/meminfo", "r");
            load = reader.readLine();

            // Get the Number value from the string
            Pattern p = Pattern.compile("(\\d+)");
            Matcher m = p.matcher(load);
            String value = "";
            while (m.find())
                value = m.group(1);
            reader.close();

            totalRam = Double.parseDouble(value);

            double mb = totalRam / 1024.0;
            double gb = totalRam / 1048576.0;
            double tb = totalRam / 1073741824.0;

            if (tb > 1) {
                lastValue = twoDecimalForm.format(tb).concat(" TB");
                RAM_SIZE = Double.parseDouble(twoDecimalForm.format(tb));
                Log.i(TAG, "RAM_SIZE tb=" + RAM_SIZE);
            } else if (gb > 1) {
                lastValue = twoDecimalForm.format(gb).concat(" GB");
                RAM_SIZE = Double.parseDouble(twoDecimalForm.format(gb));
                Log.i(TAG, "RAM_SIZE gb=" + RAM_SIZE);
            } else if (mb > 1) {
                lastValue = twoDecimalForm.format(mb).concat(" MB");
                RAM_SIZE = Double.parseDouble(twoDecimalForm.format(mb));
                Log.i(TAG, "RAM_SIZE mb=" + RAM_SIZE);
            } else {
                lastValue = twoDecimalForm.format(totalRam).concat(" KB");
                RAM_SIZE = Double.parseDouble(twoDecimalForm.format(totalRam));
                Log.i(TAG, "RAM_SIZE total RAM=" + RAM_SIZE);
            }
        } catch (IOException ex) {
            Log.e(TAG, "Error in find_RAM_SIZE(): ", ex.getCause());
            ex.printStackTrace();
        } finally {
            // Streams.close(reader);
        }
        return lastValue;
    }

    private void Filter_Gain_preference() {

        try {
            //store the value of selected Gain
            SharedPreferences gain_settings = getSharedPreferences("GAIN", 0);
            if (gain_change == true) {
                gain_settings.getInt("gain_val", item_id_gain);//user selected gain
//                selectedGain = item_id_gain;
            } else {
                item_id_gain = R.id.gain4;
//                item_id_gain = gain_settings.getInt("gain_val", 4);// default gain value
//                selectedGain = 4;
            }

            //store the value of selected filter
            SharedPreferences filter_settings = getSharedPreferences("FILTER", 0);
            if (filter_change == true)
                filter_settings.getInt("filter_val", item_id_filter);//User selected filter
            else
                item_id_filter = filter_settings.getInt("filter_val", 1);// default filter

            //store the value of selected acquisition mode
            SharedPreferences acq_mode_settings = getSharedPreferences("ACQ_MODE", 0);
            if (acq_mode_change == true)
                acq_mode_settings.getInt("acq_mode_val", item_id_acq_mode);// User selected mode
            else
                item_id_acq_mode = acq_mode_settings.getInt("acq_mode_val", 0);//default mode

            Log.i(TAG, "Gain set to: item_id_gain=" + item_id_gain);
            Log.i(TAG, "Filter set to: item_id_filter=" + item_id_filter);
            Log.i(TAG, "Acq Mode set to: item_id_acq_mode=" + item_id_acq_mode);
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "Error in Filter_Gain_preference(): ", e.getCause());
        }
    }

    //saves the variable value of the selected gain and filter
    @Override
    protected void onStop() {
        super.onStop();
        //saves selected gain in menu option
        SharedPreferences settings = getSharedPreferences("GAIN", 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt("gain_val", item_id_gain);
        editor.commit();

        //saves selected filter in menu option
        SharedPreferences filter_settings = getSharedPreferences("FILTER", 0);
        SharedPreferences.Editor edit_filter = filter_settings.edit();
        edit_filter.putInt("filter_val", item_id_filter);
        edit_filter.commit();

        //saves selected filter in menu option
        SharedPreferences acq_mode_settings = getSharedPreferences("ACQ_MODE", 0);
        SharedPreferences.Editor edit_acq_mode = acq_mode_settings.edit();
        edit_acq_mode.putInt("acq_mode_val", item_id_acq_mode);
        edit_acq_mode.commit();

        Log.i(TAG, "set item_id_gain=" + item_id_gain);
        Log.i(TAG, "set item_id_filter=" + item_id_filter);
        Log.i(TAG, "set item_id_acq_mode=" + item_id_acq_mode);
    }

    public static void read_filter_gain() {
        try {
            int gain_arr[] = {1, 2, 3, 4, 6, 8, 12};
            int filter_arr[] = {1, 2, 3, 4, 5, 0};
            int item_id_gain_arr[] = {R.id.gain1, R.id.gain2, R.id.gain3, R.id.gain4, R.id.gain6, R.id.gain8, R.id.gain12};

            //get the item position of gain in menu item
            for (int i = 0; i < 7; i++) {
                if (item_id_gain == item_id_gain_arr[i]) {
                    item_id_gain = i;
                    break;
                }
            }
            iGain = gain_arr[item_id_gain];
//            selectedGain = item_id_gain;
//            paintView.filter_state = filter_arr[item_id_filter];

            if (item_id_acq_mode == 0)//
            {
                paintView.Auto = true;
                next_menu.setVisible(false);
                prev_menu.setVisible(false);
            } else {
                paintView.Auto = false;
                next_menu.setVisible(true);
                prev_menu.setVisible(true);
            }

            if (no_of_lead_to_display == 12) {
                paintView.Auto = false;
                next_menu.setVisible(false);
                prev_menu.setVisible(false);
            }

            //set selected gain and filter value as checked in the menu option
            gain_menu.getSubMenu().getItem().getSubMenu().getItem(item_id_gain).setChecked(true);
            filter_menu.getSubMenu().getItem().getSubMenu().getItem(item_id_filter).setChecked(true);
            acq_mode.getSubMenu().getItem().getSubMenu().getItem(item_id_acq_mode).setChecked(true);
        } catch (Exception e) {
            Log.e(TAG, "Error in read_filter_gain(): ", e);
            e.printStackTrace();
        }
    }

    public static void Store_filter_gain() {
        try {
            //code to set gain id according to item id
            int item_id_gain_arr[] = {R.id.gain1, R.id.gain2, R.id.gain3, R.id.gain4, R.id.gain6, R.id.gain8, R.id.gain12};
            int item_id_filter_arr[] = {R.id.item_0_150, R.id.item_5_40, R.id.item_0_40, R.id.item_5_25, R.id.item_0_25, R.id.item_filter_off};
            int item_id_acq_mode_arr[] = {R.id.item_Auto, R.id.item_manual};

            //setting gain
            if (gain_change == true) {
                for (int i = 0; i < 7; i++) {
                    if (item_id_gain == item_id_gain_arr[i]) {
                        item_id_gain = i;
                        break;
                    }
                }
            } else {
                item_id_gain = 3;
            }

            //setting filter
            if (filter_change == true) {
                for (int i = 0; i < 6; i++) {
                    if (item_id_filter == item_id_filter_arr[i]) {
                        item_id_filter = i;
                        paintView.filter_state = (item_id_filter == 5) ? 0 : i + 1;
                        break;
                    }
                }
            } else {
                item_id_filter = 1;
            }

            //setting acquisition mode
            if (acq_mode_change == true) {
                for (int i = 0; i <= 1; i++) {
                    if (item_id_acq_mode == item_id_acq_mode_arr[i]) {
                        item_id_acq_mode = i;
                        break;
                    }
                }
            } else {
                item_id_acq_mode = 0;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
//		MenuInflater inf=getMenuInflater();
//		inf.inflate(R.menu.main, menu);
        new MenuInflater(getApplication()).inflate(R.menu.main, menu);


        //get the position of menu options
        battery = menu.getItem(0);
        prev_menu = menu.getItem(1);
        device_detail_menu = menu.getItem(3);
        next_menu = menu.getItem(2);
        mode_menu = menu.getItem(4);
        filter_menu = menu.getItem(5);
        gain_menu = menu.getItem(4);
        acq_mode = menu.getItem(7);
        add_patient_info = menu.getItem(8);
        //   load_menu = menu.getItem(9);
        // new_patient = menu.getItem(10);
        // prev_lead = menu.getItem(11);
        //  next_lead = menu.getItem(12);
        //repeat_acq = menu.getItem(13);

        battery.setVisible(false);
        filter_menu.setVisible(false);
        mode_menu.setVisible(false);
        device_detail_menu.setVisible(true);
        gain_menu.setVisible(false);
        // load_menu.setVisible(false);
        prev_menu.setVisible(false);
        next_menu.setVisible(false);
        // new_patient.setVisible(false);
        // next_lead.setVisible(false);
        // prev_lead.setVisible(false);
        //repeat_acq.setVisible(false);

        //hides the menu during connectivity
        if (mState == "HIDE_MENU") {
            for (int j = 0; j < menu.size(); j++) {
                menu.getItem(j).setVisible(false);
            }
        }
        return true;
    }

    //used for setting the title of the screen
    public void set_title(int mode, boolean auto) {
        String data_str;
        if (mode == 1)
            data_str = "Test";
        else
            data_str = "ECG";

        setTitle(data_str);
    }


    void set_acq_mode(MenuItem item, boolean mode) //AUTO:TRUE, MANUAL:FALSE
    {
        item_id_acq_mode = item.getItemId();
        acq_mode_change = true;
        item.setChecked(true);
        menu_clicked = true;
        paintView.Auto = mode;
        next_menu.setVisible(!mode);
        prev_menu.setVisible(!mode);
        paintView.page = 0;
        repaint();
    }

    private void handle_item_previous() {
        if (load_existing_file == true)//changes the data
        {
            if (paintView.page > 1)
                paintView.page--;
            else if (paintView.page == 1)
                paintView.page = 4;
            refresh_data = true;
            pv.postInvalidate();
        } else {
            if (paintView.page > 0)
                paintView.page--;
            else if (paintView.page == 0)
                paintView.page = (total_pages - 1);
            menu_clicked = true;
            next_and_prev_clicked = true;
            refresh_data = true;
            repaint();
        }
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.item_Auto:
                set_acq_mode(item, true);  //AUTO:TRUE, MANUAL :FALSE
                break;
            case R.id.item_manual:
                set_acq_mode(item, false); //AUTO:TRUE, MANUAL :FALSE
                break;
            case R.id.item_previous:   //back button
                handle_item_previous();
                break;
            case R.id.item_next:   //next button
                handle_item_next();
                break;
    /*        case R.id.item_next_lead://used only for 6,3,1 lead data
                handle_item_next_lead();
                break;*/
      /*      case R.id.item_prev_lead://used only for 6,3,1 lead data
                handle_item_prev_lead();
                break;*/
            case R.id.item_device_detail:
                status();
                break;
            case R.id.item_0_150://(-50Hz)
                paintView.filter_state = 1;
                handle_filter(item);
                break;
            case R.id.item_5_40:
                paintView.filter_state = 3;
                handle_filter(item);
                break;
            case R.id.item_0_40:
                paintView.filter_state = 2;
                handle_filter(item);
                break;
            case R.id.item_5_25:
                paintView.filter_state = 4;
                handle_filter(item);
                break;
            case R.id.item_0_25:
                paintView.filter_state = 5;
                handle_filter(item);
                break;
            case R.id.item_filter_off:
                paintView.filter_state = 0;
                handle_filter(item);
                break;
            case R.id.item_test:
                mode = 1;
                paintView.filter_state = 0;
                handle_data_mode(item, false);//test mode filter should be OFF...filter visible false
                break;
            case R.id.item_ecg:
                mode = 0;
                Store_filter_gain();
                read_filter_gain();
                handle_data_mode(item, true);//ecg mode filter should be ON...filter visible true
                break;
            case R.id.item_patient_detail:
                handle_patient_detail();
                break;
/*            case R.id.item_load_data:
                handle_load_data_new();
                break;*/
        /*    case R.id.item_new_patient:
                Store_filter_gain();
                read_filter_gain();
                handle_new_patient();
                break;*/

            case R.id.gain1:
                iGain = 1;
                handle_gain(item);
                break;
            case R.id.gain2:
                iGain = 2;
                handle_gain(item);
                break;
            case R.id.gain3:
                iGain = 3;
                handle_gain(item);
                break;
            case R.id.gain4:
                iGain = 4;
                handle_gain(item);
                break;
            case R.id.gain6:
                iGain = 6;
                handle_gain(item);
                break;
            case R.id.gain8:
                iGain = 8;
                handle_gain(item);
                break;
            case R.id.gain12:
                iGain = 12;
                handle_gain(item);
                break;

/*            case R.id.item_repeat_acq:
                acq_repeat = true;
                Store_filter_gain();
                read_filter_gain();
             //   onOptionsItemSelected(new_patient);
                break;*/
            default:
                break;
        }
        return true;
    }
/*
    private void handle_load_data_new() {
        try {
            refresh_data = true;
            load_existing_file = true;
            load = true;

            data_entered = false;
            lead_page = 0;

            prev_menu.setVisible(true);
            prev_menu.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

            next_menu.setVisible(true);
            next_menu.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

            if (no_of_lead_to_display != paintView.CH_NUM) {
                // next_lead.setVisible(true);
               // prev_lead.setVisible(true);
            }

            Intent i = new Intent(this, Filechooser.class);
            startActivity(i);
            initialization_paintView();
            pv.postInvalidate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }*/

    private void handle_gain(MenuItem item) {
        try {
            item_id_gain = item.getItemId();

            if (item_id_gain == R.id.gain12) {
                selectedGain = item.getTitle().toString().substring(2);
            } else if (item_id_gain == R.id.gain4) {
                selectedGain = "10mm/mv";
            } else {
                selectedGain = item.getTitle().toString().substring(1);
            }
            selectedGain = selectedGain.replace("(", "").replace(")", "");
//            selectedGain = item.getItemId();
            gain_change = true;
            item.setChecked(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handle_data_mode(MenuItem item, boolean data_mode) {
        try {
            item.setChecked(true);
            filter_menu.setVisible(data_mode);
            pv.init_done = false;
            paintView.ADS_samples_count = 0;
            paintView.check_data_count = 0;
            paintView.disp_count = 0;
            paintView.chk_disp_count = 0;
            paintView.batt_count = 0;
            menu_clicked = true;
            repaint();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handle_item_prev_lead() {
        try {
            if (lead_page <= (total_pages - 1)) {
                lead_page--;
                refresh_data = true;
                pv.postInvalidate();
            }
            if (lead_page < 0)
                lead_page = (total_pages - 1);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handle_item_next_lead() {
        try {
            if (lead_page >= 0) {
                lead_page++;
                refresh_data = true;
                pv.postInvalidate();
            }
            if (lead_page > (total_pages - 1))
                lead_page = 0;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handle_item_next() {
        try {
            if (load_existing_file == true) {
                if (paintView.page < 4)
                    paintView.page++;
                else if (paintView.page == 4)
                    paintView.page = 1;
                refresh_data = true;
                pv.postInvalidate();
            } else {
                if (paintView.page < (total_pages - 1))
                    paintView.page++;
                else if (paintView.page == (total_pages - 1))
                    paintView.page = 0;
                menu_clicked = true;
                next_and_prev_clicked = true;
                repaint();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handle_filter(MenuItem item) {
        try {
            item_id_filter = item.getItemId();
            filter_change = true;
            item.setChecked(true);
            menu_clicked = true;
            if ((paintView.filter_state == 0 || paintView.filter_state == 1) && paintView.skip_point < 2)
                paintView.skip_point = 2;
            else if (paintView.skip_point > 6)
                paintView.skip_point = 6;
            System.out.println("skip_point chnge=" + paintView.skip_point);
            repaint();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /*private void handle_new_patient() {
        try {
            create_data = false;
            create_image = false;
            create_pdf = false;
            create_text = false;
            report_gen = false;
            set_empty_arry();
            Message new_patient = mHandler.obtainMessage(NEW_PATIENT);
            mHandler.sendMessage(new_patient);
            invalidateOptionsMenu();//SNB REVISIT
            menu_clicked = true;
            repaint();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }*/

    private void handle_load_data() {
        try {
            refresh_data = true;
            load_existing_file = true;
            load = true;

            data_entered = false;
            lead_page = 0;

            prev_menu.setVisible(true);
            prev_menu.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

            next_menu.setVisible(true);
            next_menu.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

            if (no_of_lead_to_display != paintView.CH_NUM) {
                // next_lead.setVisible(true);
                //  prev_lead.setVisible(true);
            }

            //Open Dialog in Same Class File (Main Activity)
            isLoadFile = true;
            isFirstTimeOpen = false;
            path = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS) + "/ECG", "ECG-Recorder Reports");
            loadFileList();
            listItems.clear();
            listItems = new ArrayList<>(Arrays.asList(myFileList));
            // arrayAdapter = new ArrayAdapter<String>(this, R.layout.list_item, R.id.tv, listItems);
            pdfListAdapter = new PdfListAdapter(this, listItems, arrDuplicatePdf, MainActivity.this);
            listView.setAdapter(pdfListAdapter);
            dialog.show();

//
//            Intent i=new Intent(this,Filechooser.class);
//            startActivity(i);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handle_patient_detail() {//done

//        activity_generate_report_new


        try {
            load_existing_file = false;
            paintView.START_GUI = false;
            Patient_Info_Dialog();
            set_title(mode, paintView.Auto);
            pv.invalidate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void Demo_read() {
        try {
            //opens the demo file stored in the asset folder of application...file included in the application apk file
            AssetManager manager = getAssets();
            InputStream mInput = manager.open("Demo.dat");
            DataInputStream dinstream = new DataInputStream(mInput);

            //reads the other details
            dinstream.readUTF();
            dinstream.readUTF();
            dinstream.readUTF();
            dinstream.readUTF();
            dinstream.readUTF();
            dinstream.readUTF();
            dinstream.readUTF();
            dinstream.readUTF();
            dinstream.readUTF();
            dinstream.readUTF();
            dinstream.readUTF();
            dinstream.readUTF();
            dinstream.readUTF();
            dinstream.readUTF();
            for (int j = 0; j < 12; j++) {
                for (int k = 0; k < paintView.total_samples; k++) {
                    paintView.raw_data[j][k] = (short) dinstream.readInt();
                }
            }
            dinstream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected void Open_view_report() {
        try {
            //animatedView.setVisibility(GONE);
            progressDialog.dismiss();
            final AlertDialog.Builder build = new AlertDialog.Builder(MainActivity.this);
            build.setMessage("View Report?");
            build.setCancelable(false);
            build.setPositiveButton("YES", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface arg0, int arg1) {
                    //File file = new File(Environment.getExternalStorageDirectory()+"/TELE-ECG/TELE-ECG Reports",paintView.Pass_On_name);
                    pdf();
                }
            });

            build.setNegativeButton("NO", new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface arg0, int arg1) {
                    arg0.cancel();
                }
            });
            AlertDialog alert = build.create();
            alert.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //mobile gallery is opened to view report
    public void openFile(String minmeType) {

        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);

        // special intent for Samsung file manager
        Intent sIntent = new Intent("com.sec.android.app.myfiles.PICK_DATA");
        Intent chooserIntent;
        if (getPackageManager().resolveActivity(sIntent, 0) != null) {
            // it is device with samsung file manager
            chooserIntent = Intent.createChooser(sIntent, "Open file");
            chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[]{intent});
        } else {
            chooserIntent = Intent.createChooser(intent, "Open file");
        }
        chooserIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(chooserIntent);
        try {
            // startActivityForResult(chooserIntent, CHOOSE_FILE_REQUESTCODE);
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(getApplicationContext(), "No suitable File Manager was found.", Toast.LENGTH_SHORT).show();
        }
    }

    public void pdf() {
        Intent i = new Intent(this, ViewFileChooser.class);
        startActivity(i);
    }

    //ask user for type of report to be generated
    public void Create_report_alert() {
        try {
            Log.e("===========>>>>>>>>><<<<<", "Create_report_alert: " + MainActivity.isFormSub);

            create_pdf = true;
            if (data_entered == false && load_existing_file == false) {
                onOptionsItemSelected(add_patient_info);
            }


//            AlertDialog.Builder builder = new AlertDialog.Builder(this);
//            builder.setTitle("Generate Report");
//            builder.setMessage("Do you want to generate a pdf report?");
//            builder.setCancelable(false);
//            builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
//                public void onClick(DialogInterface dialog, int id) {
//                    runOnUiThread(new Runnable() {
//                        @Override
//                        public void run() {
//                            progressDialog.show();
//                        }
//                    });
//                    //animatedView.setVisibility(View.VISIBLE);
//                    Message report = mHandler.obtainMessage(GENERATE_REPORT);
//                    mHandler.sendMessage(report);
//                    dialog.dismiss();
//                }
//            });
//
//            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
//                public void onClick(DialogInterface dialog, int id) {
//                    dialog.cancel();
//                }
//            });
//
//            AlertDialog alert = builder.create();
//            alert.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //redraw the screen view
    public void repaint() {
        if (menu_clicked == true) {
            if (pv.bitmap != null)
                pv.cnvs.drawBitmap(pv.bitmap, 0, 0, pv.paint);

            pv.bitmap.eraseColor(Color.TRANSPARENT);
            pv.cnvs.drawRGB(0, 0, 0);
            pv.drawGridLines(pv.cnvs);
            pv.print_text_on_canvas(paintView.page);
            menu_clicked = false;
        }

        if (next_and_prev_clicked != true) {
            paintView.plot_on = false;
            pv.initialise_array();
        } else
            paintView.plot_on = true;

        //set_title(mode,false);
        set_title(mode, paintView.Auto);
        //pv.invalidate();
        paintView.disp_count = 0;
        paintView.chk_disp_count = 0;
        paintView.ADS_samples_count = 0;
        paintView.check_data_count = 0;
        paintView.batt_count = 0;
        next_and_prev_clicked = false;
    }

    //handler receives messages and carries out the specified action
    //@SuppressWarnings("deprecation")
    public final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case START_DATA_ACQ:
                    set_empty_arry();
                    if (strcounter == 0) {
                        try {
                            while (instrm.available() > 0) {
                                instrm.read();
                            }
                            switch_to_gain();
                            set_filter(paintView.filter_state);
                            if (pv.debug_mode == true)
                                outstrm.write('U');//for debug mode
                            if (mode == 1)        // Test Channels
                            {
                                outstrm.write('T');
                            } else {
                                outstrm.write('A');
                            }// Take ECG

                            Toast.makeText(getApplicationContext(), "Initialising please wait...", Toast.LENGTH_SHORT).show();
                            pv.init_done = false;
                            pv.array_full = false;
                            pv.report_count = 0;
                            pv.echo.start();
                            strcounter++;
                        } catch (IOException e) {
                            System.out.println("START_DATA error=" + e.getMessage());
                            e.printStackTrace();
                        }
                    } else {
                        try {
                            switch_to_gain();
                            set_filter(paintView.filter_state);
                            if (mode == 1)        // Test Channels
                            {
                                outstrm.write('T');
                                if (paintView.filter_state != 0)
                                    paintView.filter_state = 0;
                            } else               // Take ECG
                                outstrm.write('A');

                            Toast.makeText(getApplicationContext(), "Acquiring data please wait...", Toast.LENGTH_SHORT).show();
                            pv.init_done = false;
                            pv.plot_done = false;
                            paintView.roll_over = false;
                            paintView.disp_count = 0;
                            paintView.chk_disp_count = 0;
                            paintView.ADS_samples_count = 0;
                            paintView.check_data_count = 0;
                            paintView.fill_count = 0;
                            paintView.Max_Scaling_factor = 0;
                            paintView.Min_Scaling_factor = 0;
                            pv.report_count = 0;
                            pv.onResume();
                        } catch (Exception e) {
                            System.out.println("START_DATA error=" + e.getMessage());
                            e.printStackTrace();
                        }
                    }
                    break;
                case STOP_DATA:
                    try {
                        pv.onPause();
                        if (pv.report_count > 0) {
                            paintView.generate_report_opt = true;
                            pv.store_Array(paintView.ADS_samples_count);
                            Log.e("=>LLLL", "handleMessage: " + paintView.ADS_samples_count);

                        } else {

                            findViewById(R.id.generate_2).setVisibility(View.GONE);
                            findViewById(R.id.effect2).setVisibility(View.GONE);

                            final Handler handler = new Handler();
                            handler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(getApplicationContext(), "Insufficient data and report cannot be generated", Toast.LENGTH_SHORT).show();
                                    handler.postDelayed(this, 1000);
                                }
                            }, 1000);
                        }

                        if (paintView.generate_report_opt == true)
                            pv.last_position(paintView.disp_count);
                    } catch (Exception e) {
                        System.out.println("STOP_DATA error=" + e.getMessage());
                        e.printStackTrace();
                    }
                    break;
                case NEW_PATIENT:
                    try {

                        mode_menu.setVisible(true);
                        filter_menu.setVisible(true);
                        gain_menu.setVisible(true);
                        //   new_patient.setVisible(false);
                        pv.invalidate();
                    } catch (Exception e) {
                        e.printStackTrace();
                        System.out.println("error in NEW_PATIENT" + e.getMessage());
                    }
                    break;
                case GENERATE_REPORT:
                    try {
                        report_gen = true;
                        comment = "";
                        cr = new Create_report(MainActivity.this);
                        Open_view_report();
                    } catch (Exception e) {
                        System.out.println("error in GENERATE_REPORT" + e.getMessage());
                    }
                    break;
                case LOAD_DATA:
                    //   onOptionsItemSelected(load_menu);
                    break;
                case MESSAGE_COMMUNICATION:
                    AlertDialog.Builder build2 = new AlertDialog.Builder(MainActivity.this);
                    build2.setTitle("Communication Error");
                    build2.setMessage("Communication Error! \n Try Again!");
                    build2.setCancelable(false);
                    build2.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int arg1) {
                            System.exit(3);
                        }
                    });
                    AlertDialog alert2 = build2.create();
                    alert2.show();
                    break;
            }
        }
    };

    //sends the characters according to the selected gain
    private void switch_to_gain() {
        try {
            switch (iGain) {
                case 1:
                    outstrm.write('B'); //System.out.println("igain method="+iGain);
                    break;
                case 2:
                    outstrm.write('C'); //System.out.println("igain method="+iGain);
                    break;
                case 3:
                    outstrm.write('D'); //System.out.println("igain method="+iGain);
                    break;
                case 4:
                    outstrm.write('E');//System.out.println("igain method="+iGain);
                    break;
                case 6:
                    outstrm.write('F');//System.out.println("igain method="+iGain);
                    break;
                case 8:
                    outstrm.write('G');//System.out.println("igain method="+iGain);
                    break;
                case 12:
                    outstrm.write('H');//System.out.println("igain method="+iGain);
                    break;
            }// switch
        } catch (Exception e) {
            System.out.println("Exception in gain=>" + e.getMessage());
        }
    }

    protected void set_filter(int filter) {
        try {
            switch (filter) {
                case 0:       //System.out.println("0 FIR_B_150 raw");
                    break;    // 0 to 150 Hz (RAW)
                case 1:
                    for (int i = 0; i <= 500; i++) {
                        pv.FIR_B[i] = pv.FIR_B_150[i];
                    }
                    break;    // 0 to 150 Hz (-50Hz)
                case 2:
                    for (int i = 0; i <= 500; i++) {
                        pv.FIR_B[i] = pv.FIR_B_5_40[i];
                    }
                    break;    // 5 to 40 Hz
                case 3:
                    for (int i = 0; i <= 500; i++) {
                        pv.FIR_B[i] = pv.FIR_B_40[i];
                    }
                    break;    // 0 to 40 Hz
                case 4:
                    for (int i = 0; i <= 500; i++) {
                        pv.FIR_B[i] = pv.FIR_B_5_25[i];
                    }
                    break;    // 5 to 25 Hz
                case 5:
                    for (int i = 0; i <= 500; i++) {
                        pv.FIR_B[i] = pv.FIR_B_25[i];
                    }
                    break;    // 0 to 25 Hz
            }// switch
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //to change the context from main Activity to paint view class
    public void initialization_paintView() {
        try {
            setContentView(R.layout.activity_main_canvas);

            bt_start = (Button) findViewById(R.id.start_button);
            // animatedView = findViewById(R.id.animatedView);
            bt_start.setOnClickListener(this);

            if (load) {
                findViewById(R.id.gen_start).setVisibility(View.GONE);
                findViewById(R.id.start_button).setVisibility(View.GONE);
                findViewById(R.id.generate_button).setVisibility(View.VISIBLE);
                findViewById(R.id.generate_button).setOnClickListener(this);
                findViewById(R.id.effect).setVisibility(View.GONE);
            } else {
                findViewById(R.id.gen_start).setVisibility(View.GONE);
                findViewById(R.id.start_button).setVisibility(View.VISIBLE);
                findViewById(R.id.generate_button).setVisibility(View.GONE);
                RelativeLayout mylayout = (RelativeLayout) findViewById(R.id.effect);
                ObjectAnimator fadeOut = ObjectAnimator.ofFloat(mylayout, "alpha", .5f, .1f);
                fadeOut.setDuration(300);
                ObjectAnimator fadeIn = ObjectAnimator.ofFloat(mylayout, "alpha", .1f, .5f);
                fadeIn.setDuration(300);

                mAnimationSet.play(fadeIn).after(fadeOut);
                mAnimationSet.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        mAnimationSet.start();
                    }
                });
                mAnimationSet.start();
            }

            int status_bar_height = getStatusBarHeight();//
            int titlebar_height = (getTitleBarHeight() - status_bar_height);//titlebar_height is the total height of the screen including the titlebar

            //create paintview object......

            pv = new paintView(titlebar_height, status_bar_height, iScreenWidth, instrm, outstrm, MainActivity.this, false);
            pv.cnvs.drawRGB(0, 0, 0);
            setTitleColor(Color.BLACK);
            set_title(mode, paintView.Auto);
            //total pages to be displayed for different lead selected
            total_pages = paintView.CH_NUM / no_of_lead_to_display;
            pv.drawGridLines(pv.cnvs);
            pv.print_text_on_canvas(paintView.page);

            ActionBar ab = getActionBar();
            ab.setBackgroundDrawable(getResources().getDrawable(R.drawable.action_bar_background_color));
            ab.setDisplayUseLogoEnabled(false);
            ab.setDisplayShowHomeEnabled(false);
            ab.show();
            LinearLayout ll = (LinearLayout) findViewById(R.id.canvas);
            ll.addView(pv);
            //setContentView(pv);
            pv.requestFocus();
            System.out.println("");
            switch_to_gain();
            if (load_existing_file == false) {

                findViewById(R.id.gen_start).setVisibility(View.VISIBLE);
                findViewById(R.id.generate_button).setVisibility(View.GONE);
                findViewById(R.id.start_button).setVisibility(View.GONE);

                outstrm.write('S');//to stop data ACQ when connection was break
                outstrm.write('R');
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Error" + e.getMessage());
        }
    }

    @SuppressWarnings("static-access")
    protected void set_empty_arry() {
        pv.initialise_array();
        paintView.disp_count = 0;
        paintView.chk_disp_count = 0;
        paintView.ADS_samples_count = 0;
        paintView.check_data_count = 0;
        paintView.fill_count = 0;
        pv.batt_count = 0;
        paintView.generate_report_opt = false;
    }

    //returns the height of the title bar
    public int getTitleBarHeight() {
        int viewtop = getWindow().findViewById(Window.ID_ANDROID_CONTENT).getTop();
        f_size = getResources().getDimensionPixelSize(R.dimen.font_size);
        return (viewtop);
    }

    //this method finds the height of status bar
    public int getStatusBarHeight() {
        Rect r = new Rect();
        Window w = getWindow();
        w.getDecorView().getWindowVisibleDisplayFrame(r);
        return r.top;
    }

    public void start() {
        mAnimationSet.pause();
        findViewById(R.id.effect).setVisibility(View.GONE);
        new CountDownTimer(25000, 1000) {//netra
            public void onTick(long millisUntilFinished) {
                bt_start.setEnabled(false);
                bt_start.setText("Stop(" + millisUntilFinished / 1000 + ")");
                bt_start.setBackgroundColor(Color.BLACK);
            }

            public void onFinish() {
                bt_start.setEnabled(true);
                bt_start.setText("Stop");
                bt_start.setBackground(getResources().getDrawable(R.drawable.new_main_activity_button_background));
            }

        }.start();
        started = true;
        bt_start.setVisibility(View.VISIBLE);
        findViewById(R.id.gen_start).setVisibility(View.GONE);

        if (demo_start == true) {
            load_existing_file = true;
            repaint();
            Demo_read();
            initialization_paintView();
            pv.invalidate();
        } else {
            pv.first_pass = true;
            refresh_data = true;
            load_existing_file = false;
            stop = false;
            if (acq_repeat == true)
                data_entered = true;
            else
                data_entered = false;
            mode_menu.setVisible(false);
            gain_menu.setVisible(false);
            filter_menu.setVisible(false);
            // load_menu.setVisible(false);
            paintView.batt_count = 0;
            paintView.Battery_level = 0;
            //pass message to handler
            Message m7 = mHandler.obtainMessage(START_DATA_ACQ);
            mHandler.sendMessage(m7);
        }
    }

    public void stop() {
        started = false;
        bt_start.setVisibility(View.GONE);
        findViewById(R.id.gen_start).setVisibility(View.VISIBLE);
        RelativeLayout mylayout = (RelativeLayout) findViewById(R.id.effect2);

        ObjectAnimator fadeOut = ObjectAnimator.ofFloat(mylayout, "alpha", .5f, .1f);
        fadeOut.setDuration(300);
        ObjectAnimator fadeIn = ObjectAnimator.ofFloat(mylayout, "alpha", .1f, .5f);
        fadeIn.setDuration(300);

        mAnimationSet.play(fadeIn).after(fadeOut);

        mAnimationSet.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                mAnimationSet.start();
            }
        });

        mAnimationSet.start();

        try {
            outstrm.write('S');    //char 'S' sent to stop acquisition
        } catch (IOException e) {
            e.printStackTrace();
        }

        stop = true;
        refresh_data = false;
        mode_menu.setVisible(true);
        filter_menu.setVisible(true);
        gain_menu.setVisible(true);
        if (mode == 1)
            filter_menu.setVisible(false);
        //pass message to handler
        Message m = mHandler.obtainMessage(STOP_DATA);
        mHandler.sendMessage(m);
    }

    //used for scanning the devices on button click(scan button)
    @Override
    public void onClick(View v) {
        myVib.vibrate(50);
        if (v.getId() == R.id.start_button || v.getId() == R.id.start_2) {
            if (started) {
                stop();
            } else {
                start();
            }
        }

        if (v.getId() == R.id.generate_button) {
//            Create_report_alert();
        }

        if (v.getId() == R.id.new_main_activity_record_test_btn) {
            if (checkPermission()) return;

            if (canGetLocation()) {
                try {
                    if (no_of_lead_to_display == 12) {
                        paintView.Auto = false;
                        acq_mode.setVisible(false);
                        prev_menu.setVisible(false);
                        next_menu.setVisible(false);
                    }

                    create_data = false;
                    create_image = false;
                    create_pdf = false;
                    create_text = false;
                    load_existing_file = false;
                    //getActionBar().show();
                    init_patient_info();
                    Intent i = new Intent(getApplicationContext(), Bluetooth_connect.class);
                    startActivity(i);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                showSettingsAlert();
            }
        }

        if (v.getId() == R.id.new_main_activity_view_report_btn) {
            getActionBar().show();
            try {
                create_data = false;
                create_image = false;
                create_pdf = false;
                create_text = false;
                handle_load_data();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (v.getId() == R.id.view_button) {
//            getActionBar().show();
//            Intent i=new Intent(this,ViewFileChooser.class);
//            startActivity(i);

            path = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS) + "/ECG", "ECG-Recorder Reports");
            loadFileList();
            isFirstTimeOpen = false;
            listItems.clear();
            listItems = new ArrayList<>(Arrays.asList(myFileList));
            //  arrayAdapter = new ArrayAdapter<String>(this, R.layout.list_item, R.id.tv, listItems);
            pdfListAdapter = new PdfListAdapter(this, listItems, arrDuplicatePdf, MainActivity.this);
            listView.setAdapter(pdfListAdapter);
            isLoadFile = false;
            dialog.show();
        }

        if (v.getId() == R.id.new_main_activity_select_lead_btn) {

            try {
                getActionBar().hide();
                lead_pressed = true;
                Select_lead_display();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    private void init_patient_info() {
        try {
            paintView.Pass_On_date = "";
            paintView.Pass_On_name = "";
            paintView.Pass_On_chno = "";
            paintView.Pass_On_dob = "";
            paintView.Pass_On_age = Integer.parseInt("");
            paintView.Pass_On_gen = "";
            paintView.Pass_On_ht = Integer.parseInt("");
            paintView.Pass_On_wt = Integer.parseInt("");
            paintView.Pass_On_medi = "";
            paintView.Pass_On_BP = "";
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void Select_lead_display() {
        try {
            final CharSequence[] items = {"1 lead", "3 leads", "6 leads", "12 leads"};
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Select number of leads to display.");
            builder.setCancelable(false);
            builder.setSingleChoiceItems(items, selectedPosition, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface arg0, int item) {
                    int chan_menu_selection[] = {1, 3, 6, 12};
                    no_of_lead_to_display = chan_menu_selection[item];


                    JsonObject report61Report = new JsonObject();
                    JsonObject report61Report2 = new JsonObject();
                    reportJsonArray = new JsonArray();

                    if (no_of_lead_to_display == 6) {
                        report61Report.addProperty("reportName", "61Report");
                        report61Report.addProperty("position", "3");
                        report61Report.addProperty("isChecked", "true");

                        report61Report2.addProperty("reportName", "61Report2");
                        report61Report2.addProperty("position", "4");
                        report61Report2.addProperty("isChecked", "false");
                    } else if (no_of_lead_to_display == 12) {
                        report61Report.addProperty("reportName", "61Report");
                        report61Report.addProperty("position", "3");
                        report61Report.addProperty("isChecked", "true");

                        report61Report2.addProperty("reportName", "61Report2");
                        report61Report2.addProperty("position", "4");
                        report61Report2.addProperty("isChecked", "true");
                    } else {
                        report61Report.addProperty("reportName", "61Report");
                        report61Report.addProperty("position", "3");
                        report61Report.addProperty("isChecked", "true");

                        report61Report2.addProperty("reportName", "61Report2");
                        report61Report2.addProperty("position", "4");
                        report61Report2.addProperty("isChecked", "true");
                    }


                    reportJsonArray.add(report61Report);
                    reportJsonArray.add(report61Report2);

                    reportEditor.putString("Report", reportJsonArray.toString());
                    reportEditor.apply();

                }
            });
            builder.setPositiveButton("OK",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            selectedPosition = ((AlertDialog) dialog).getListView().getCheckedItemPosition();
                            store_lead_number();
                        }
                    });
            AlertDialog alert = builder.create();
            alert.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void read_lead_number() {
        try {
          /*  if (android.os.Build.VERSION.SDK_INT == Build.VERSION_CODES.R) {
                if (Environment.isExternalStorageManager()) {
                    int chan_menu_selection[] = {1, 3, 6, 12};

                    File settingPath = new File(settingsFolder);
                    if (!settingPath.exists()) {
                        settingPath.mkdirs();
                    }

                    String mPath = "selected_lead_number" + ".dat";
                    File pdfFile = new File(settingPath, mPath);

                    if (!pdfFile.exists()) {
                        store_lead_number();
                    }
                    RandomAccessFile raf = new RandomAccessFile(pdfFile, "rw");
                    selectedPosition = raf.readInt();
                    raf.close();

                    no_of_lead_to_display = chan_menu_selection[selectedPosition];
                    Log.i(TAG, "number of leads to display:" + no_of_lead_to_display);
                } else {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                    Uri uri = Uri.fromParts("package", getPackageName(), null);
                    intent.setData(uri);
                    startActivity(intent);
                }
            }else {*/
            int chan_menu_selection[] = {1, 3, 6, 12};

            File settingPath = new File(settingsFolder);
            if (!settingPath.exists()) {
                settingPath.mkdirs();
            }

            String mPath = "selected_lead_number" + ".dat";
            File pdfFile = new File(settingPath, mPath);

            if (!pdfFile.exists()) {
                store_lead_number();
            }
            RandomAccessFile raf = new RandomAccessFile(pdfFile, "rw");
            selectedPosition = raf.readInt();
            raf.close();

            no_of_lead_to_display = chan_menu_selection[selectedPosition];
            Log.i(TAG, "number of leads to display:" + no_of_lead_to_display);
            //  }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void status() {
        new AlertDialog.Builder(this)
                .setTitle("Device Status")
                .setMessage("Battery: " + battery_status + "\n V1 Status: " + v1_status + "\n V2 Status: " + v2_status + "\n V3 Status: " + v3_status + "\n V4 Status: " + v4_status + "\n V5 Status: " + v5_status + "\n V6 Status: " + v6_status + "\n LL Status: " + ll_status + "\n LA Status: " + la_status + "\n RA Status: " + ra_status)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    protected void store_lead_number() {
        try {

/*            if (android.os.Build.VERSION.SDK_INT == Build.VERSION_CODES.R) {
                if (Environment.isExternalStorageManager()) {
                    File settingPath = new File(settingsFolder);
                    if (!settingPath.exists()) {
                        settingPath.mkdirs();
                    }

                    String mPath = "selected_lead_number" + ".dat";
                    File pdfFile = new File(settingPath, mPath);
                    RandomAccessFile raf = new RandomAccessFile(pdfFile, "rw");

                    if (lead_pressed == true)
                        raf.writeInt(selectedPosition);
                    else
                        raf.writeInt(3);
                    raf.close();
                } else {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                    Uri uri = Uri.fromParts("package", getPackageName(), null);
                    intent.setData(uri);
                    startActivity(intent);
                }
            }else {*/
            File settingPath = new File(settingsFolder);
            if (!settingPath.exists()) {
                settingPath.mkdirs();
            }
            String mPath = "selected_lead_number" + ".dat";
            File pdfFile = new File(settingPath, mPath);
            RandomAccessFile raf = new RandomAccessFile(pdfFile, "rw");
            if (lead_pressed == true)
                raf.writeInt(selectedPosition);
            else
                raf.writeInt(3);
            raf.close();
            // }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //exit the application on pressing of back button
    @Override
    public void onBackPressed() {
        try {
            alertDialog = new AlertDialog.Builder(this).create();
            alertDialog.setMessage("What you want to do?");
            alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "HOME", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    MainActivity.this.finish();
                    Intent main = new Intent(getApplicationContext(), MainActivity.class);
                    startActivity(main);
                    overridePendingTransition(R.anim.stable, R.anim.stable);
                    finish();
                }
            });

            alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "CANCEL", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    dialog.cancel();
                }
            });


            alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "EXIT", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {

                    if (preferences.getBoolean("isRated", false)) {
                        MainActivity.this.finishAffinity();
                    } else {
                        AlertDialog.Builder alertDialog = new AlertDialog.Builder(MainActivity.this);
                        alertDialog.setMessage("please rate us");
                        alertDialog.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {

                                editor.putBoolean("isRated", true);
                                editor.apply();

                                try {
                                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + "com.medigraphy.ecg")));
                                } catch (android.content.ActivityNotFoundException anfe) {
                                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + "com.medigraphy.ecg")));
                                }

                                dialogInterface.dismiss();
                            }
                        }).setNegativeButton("Exit", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialogInterface.dismiss();
                                MainActivity.this.finishAffinity();
                            }
                        });
                        alertDialog.show();
                    }
                }
            });
            alertDialog.show();
        } catch (Exception e) {
            e.printStackTrace();
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

    private void updateLabel() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.US);

        dob.setText(sdf.format(myCalendar.getTime()));
    }

    private void getAge(int year, int month, int day) {
        Calendar dob = Calendar.getInstance();
        Calendar today = Calendar.getInstance();

        dob.set(year, month, day);

        int age = today.get(Calendar.YEAR) - dob.get(Calendar.YEAR);

        if (today.get(Calendar.DAY_OF_YEAR) < dob.get(Calendar.DAY_OF_YEAR)) {
            age--;
        }

        Integer ageInt = new Integer(age);
        String ageS = ageInt.toString();

        this.liviewforage.setVisibility(View.VISIBLE);

        this.age.setText(Create_report.strsge(ageS));
    }

    public Uri getImageUri(Context inContext, Bitmap inImage) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(inContext.getContentResolver(), inImage, "ECGUp", null);
        return Uri.parse(path);
    }


    //clear data and sets default data
    public void Clear_alert() {
        AlertDialog.Builder build = new AlertDialog.Builder(MainActivity.this);
        build.setMessage("Do you want to clear the entered details?");
        build.setPositiveButton("YES", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int arg1) {
                name.setText("");
                age.setText("");
                //gender.setText("");
                medi.setText("");
                BP.setText("");
                chno.setText("");
                dob.setText("");
                ht.setText("");
                wt.setText("");
                liviewforage.setVisibility(View.GONE);

            }
        });

        build.setNegativeButton("NO", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface arg0, int arg1) {
                arg0.cancel();
            }
        });
        AlertDialog alert = build.create();
        alert.show();
    }

    private void Patient_Info_Dialog() {
        Dialog dialog2 = new Dialog(this, android.R.style.Theme_Translucent_NoTitleBar);
        dialog2.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog2.setContentView(R.layout.activity_generate_report_new);
        Window window = dialog2.getWindow();
        WindowManager.LayoutParams wlp = window.getAttributes();

        int actionBarTitleId = Resources.getSystem().getIdentifier("action_bar_title", "id", "android");
        if (actionBarTitleId > 0) {

            TextView title = (TextView) findViewById(actionBarTitleId);
            if (title != null)
                title.setTextColor(Color.WHITE);
        }

        AndroidBug5497Workaround.assistActivity(this);

        setTitleColor(Color.WHITE);
        setTitle("Patient Details");
        //initialization of GUI of patient details
        save = (Button) dialog2.findViewById(R.id.save_btn_new);
        simage = (CircularImageView) dialog2.findViewById(R.id.imageView1);
        upload = (Button) dialog2.findViewById(R.id.upload_btn_new);
        clear = (Button) dialog2.findViewById(R.id.Clear_btn_new);
        cancel = (Button) dialog2.findViewById(R.id.Cancel_btn_new);
        chno = (EditText) dialog2.findViewById(R.id.CHSSNO_et_new);
        dob = (EditText) dialog2.findViewById(R.id.dob_et_new);
        age = (EditText) dialog2.findViewById(R.id.patient_age_et_new);
        name = (EditText) dialog2.findViewById(R.id.patient_name_et_new);
        medi = (EditText) dialog2.findViewById(R.id.medications_et_new);
        BP = (EditText) dialog2.findViewById(R.id.remark_et_new);
        gender = (RadioGroup) dialog2.findViewById(R.id.radioGroup_gender_new);
        ht = (EditText) dialog2.findViewById(R.id.height_et_new);
        wt = (EditText) dialog2.findViewById(R.id.weight_et_new);
        liviewforage = (LinearLayout) dialog2.findViewById(R.id.liviewforage);
        selectedGender = (RadioButton) dialog2.findViewById(gender.getCheckedRadioButtonId());

        age.setEnabled(false);
        liviewforage.setVisibility(View.GONE);

        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog2.dismiss();
            }
        });

        clear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Clear_alert();
            }
        });

        upload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(cameraIntent, CAMERA_REQUEST);
            }
        });

        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                myVib.vibrate(50);

                data_entered = true;
                sname = name.getText().toString().trim();
                schno = chno.getText().toString().trim();
                sdob = dob.getText().toString().trim();

                if (!age.getText().toString().trim().equals("")) {
                    sage = Integer.parseInt(age.getText().toString().trim().replace("infant", "0"));
                }

                if (!ht.getText().toString().trim().equals("")) {
                    sht = Integer.parseInt(ht.getText().toString().trim());
                }

                if (!wt.getText().toString().trim().equals("")) {
                    swt = Integer.parseInt(wt.getText().toString().trim());
                }


                if (selectedGender == null)
                    sgen = "";
                else
                    sgen = selectedGender.getText().toString().trim();

                smedi = medi.getText().toString().trim();
                sBP = BP.getText().toString().trim();

                if (sname.equals("")) {
                    Toast.makeText(getApplicationContext(), "Please enter your Name", Toast.LENGTH_SHORT).show();
                } else if (sdob.equals("")) {
                    Toast.makeText(getApplicationContext(), "Please enter Date of Birth", Toast.LENGTH_SHORT).show();
                } else if (sage > 120) {
                    Toast.makeText(getApplicationContext(), "Maximum Age is 120 Years", Toast.LENGTH_SHORT).show();
                } else if (sage < 0) {
                    Toast.makeText(getApplicationContext(), "Minimum Age is 1 Year", Toast.LENGTH_SHORT).show();
                } else if (sht > 250) {
                    Toast.makeText(getApplicationContext(), "Maximum Hight is 250 cm", Toast.LENGTH_SHORT).show();
                } else if (swt > 200) {
                    Toast.makeText(getApplicationContext(), "Maximum Weight is 200 kg", Toast.LENGTH_SHORT).show();
                } else {
                    paintV = new paintView(sdf, sname, schno, sdob, sage, sgen, sht, swt, smedi, sBP, imageUri, MainActivity.this);
                    isFormSub = true;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            progressDialog.show();
                        }
                    });
                    Message report = mHandler.obtainMessage(GENERATE_REPORT);
                    mHandler.sendMessage(report);
                    dialog.dismiss();
                    dialog2.dismiss();

                }

            }
        });


        final DatePickerDialog.OnDateSetListener date = new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                // TODO Auto-generated method stub
                myCalendar.set(Calendar.YEAR, year);
                myCalendar.set(Calendar.MONTH, monthOfYear);
                myCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                Calendar today = Calendar.getInstance();
                getAge(year, monthOfYear, dayOfMonth);
                updateLabel();

            }
        };

        dob.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.N)
            @Override
            public void onClick(View v) {

                DatePickerDialog datePickerDialog = new DatePickerDialog(MainActivity.this,
                        date,
                        myCalendar.get(Calendar.YEAR),
                        myCalendar.get(Calendar.MONTH),
                        myCalendar.get(Calendar.DAY_OF_MONTH));

                datePickerDialog.getDatePicker().setMaxDate(new Date().getTime() - 10000);
                datePickerDialog.show();

            }
        });

        //adding weight to dropdown list
        List<Number> list_wt = new ArrayList<Number>();
        for (int i = 1; i <= 255; i++)
            list_wt.add(i);


        if (MainActivity.load_existing_file == true || HelpActivity.load_existing_file == true) {
            paintV = new paintView(sdf, sname, schno, sdob, sage, sgen, sht, swt, smedi, sBP, this.imageUri, MainActivity.this);
            dialog2.dismiss();
        }


        wlp.gravity = Gravity.CENTER;
        wlp.flags &= ~WindowManager.LayoutParams.FLAG_BLUR_BEHIND;
        window.setAttributes(wlp);
        dialog2.getWindow().setLayout(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
        dialog2.show();
    }
}