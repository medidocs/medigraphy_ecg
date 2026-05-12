package com.medigraphy.ecg;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Rect;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Display;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;

import com.github.barteksc.pdfviewer.PDFView;
import com.github.barteksc.pdfviewer.listener.OnLoadCompleteListener;
import com.github.barteksc.pdfviewer.listener.OnPageChangeListener;
import com.github.barteksc.pdfviewer.scroll.DefaultScrollHandle;
import com.shockwave.pdfium.PdfDocument;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.comparator.LastModifiedFileComparator;
import org.jsoup.Jsoup;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import pl.droidsonroids.gif.GifImageView;

public class HelpActivity extends Activity implements View.OnClickListener, OnPageChangeListener, OnLoadCompleteListener {

    public static boolean  refresh_data = false,create_image = false, create_pdf = false,  create_text = false, lead_pressed = false, loggerCreated = false;
    public static boolean report_gen = false,
            filter_change = false,
            gain_change = false,
            load_existing_file = false,

            acq_mode_change = false;
    public static int f_size, no_of_lead_to_display, total_pages,
            lead_page = 0, step, selectedPosition = 12, item_id_acq_mode,
            item_id_gain, item_id_filter, mode;
    public static MenuItem acq_mode,device_detail_menu, gain_menu, filter_menu, add_patient_info, load_menu, next_menu, demo_menu, prev_menu, view_report,
            mode_menu, battery ;
    private File path = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).getAbsolutePath() + "/ECG", "ECG-Recorder Reports");

    public static String mStateHelp = "HIDE_MENU";

    PDFView pdfView;
    paintView pv;
    OutputStream outstrm;
    Integer pageNumber = 0;
    protected static final int GENERATE_REPORT = 3;
    Create_report cr;
    public static String comment = "";
    Display d;
    public static int iScreenWidth, iScreenHeight;
    public boolean next_and_prev_clicked = false, menu_clicked = false;
    public static String selectedGain = "10mm/mv";
    public static int report_sequential, iGain;
    public static String battery_status = "N/A", v1_status = "Not Connected", v6_status = "Not Connected", v2_status = "Not Connected", v3_status = "Not Connected", v4_status = "Not Connected", v5_status = "Not Connected", ll_status = "Not Connected", la_status = "Not Connected", ra_status = "Not Connected";
    public static String settingsFolder = null;

    GifImageView animatedView;

    @SuppressWarnings("deprecation")

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help);


        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        getWindow().setStatusBarColor(getResources().getColor(R.color.color_status_bar));
        settingsFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS) + "/ECG/Settings";



        d = getWindowManager().getDefaultDisplay();
        iScreenWidth = d.getWidth();
        iScreenHeight = d.getHeight();


        read_lead_number();
        Filter_Gain_preference();

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == 101) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                callPhoneNumber();
            }
        }
    }

    public void callPhoneNumber() {
        try {
            if (Build.VERSION.SDK_INT > 22) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(HelpActivity.this, new String[]{Manifest.permission.CALL_PHONE}, 101);
                   return;
                }

                Intent callIntent = new Intent(Intent.ACTION_CALL);
                callIntent.setData(Uri.parse("tel:" + "+91-9321189117"));
                startActivity(callIntent);

            } else {
                Intent callIntent = new Intent(Intent.ACTION_CALL);
                callIntent.setData(Uri.parse("tel:" + "+91-9321189117"));
                startActivity(callIntent);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }



    ArrayList<String> stringArrayList = new ArrayList<String>();

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.generate_button) {
            init_patient_info();
            Create_report_alert();
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


    @Override
    public void onPageChanged(int page, int pageCount) {
        pageNumber = page;
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
//		MenuInflater inf=getMenuInflater();
//		inf.inflate(R.menu.main, menu);
        new MenuInflater(getApplication()).inflate(R.menu.main, menu);
        //get the positions of menu item
        battery = menu.getItem(0);

        prev_menu = menu.getItem(1);
        device_detail_menu = menu.getItem(3);
        next_menu = menu.getItem(2);
        mode_menu = menu.getItem(4);
        filter_menu = menu.getItem(5);
        gain_menu = menu.getItem(6);
        acq_mode = menu.getItem(7);
        add_patient_info = menu.getItem(8);
        //  load_menu = menu.getItem(9);
        //new_patient = menu.getItem(10);
        // prev_lead = menu.getItem(11);
        //next_lead = menu.getItem(12);
        // repeat_acq = menu.getItem(13);

        battery.setVisible(true);
        battery.setTitle("Battery: " + battery_status);
        battery.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

        filter_menu.setVisible(true);
        mode_menu.setVisible(true);
        gain_menu.setVisible(true);
        //  load_menu.setVisible(true);
        prev_menu.setVisible(false);
        next_menu.setVisible(false);
        // new_patient.setVisible(false);
        // next_lead.setVisible(false);
        // prev_lead.setVisible(false);
        //  repeat_acq.setVisible(false);
        //hides the menu during connectivity
        if (mStateHelp == "HIDE_MENU") {
            for (int j = 0; j < menu.size(); j++)
                menu.getItem(j).setVisible(false);

        }
        if (mStateHelp == "UNHINDE_MENU") {
            //demo_menu.setVisible(false);
        }

        if (no_of_lead_to_display == 12)
            acq_mode.setVisible(false);

        read_filter_gain();
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


    @Override
    public void loadComplete(int nbPages) {
        PdfDocument.Meta meta = pdfView.getDocumentMeta();
        printBookmarksTree(pdfView.getTableOfContents(), "-");
    }

    public void printBookmarksTree(List<PdfDocument.Bookmark> tree, String sep) {
        for (PdfDocument.Bookmark b : tree) {
            if (b.hasChildren()) {
                printBookmarksTree(b.getChildren(), sep + "-");
            }
        }
    }

    //ask user for type of report to be generated
    public void Create_report_alert() {
        try {
            create_pdf = true;
            if (MainActivity.data_entered == false && load_existing_file == false) {
                onOptionsItemSelected(add_patient_info);
            }
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Generate Report");
            builder.setMessage("Do you want to generate a pdf report?");
            builder.setCancelable(false);
            builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    animatedView.setVisibility(View.VISIBLE);
                    Message report = mHandler.obtainMessage(GENERATE_REPORT);
                    mHandler.sendMessage(report);
                }
            });

            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    dialog.cancel();
                }
            });

            AlertDialog alert = builder.create();
            alert.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case GENERATE_REPORT:
                    try {
                        report_gen = true;
                        comment = "";
                        cr = new Create_report(HelpActivity.this);
                        Open_view_report();
                    } catch (Exception e) {
                        System.out.println("error in GENERATE_REPORT" + e.getMessage());
                    }
                    break;
            }
        }
    };

    protected void Open_view_report() {
        try {
            animatedView.setVisibility(View.GONE);
            final AlertDialog.Builder build = new AlertDialog.Builder(HelpActivity.this);
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

    public void pdf() {
        Intent i = new Intent(this, ViewFileChooser.class);
        startActivity(i);
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
            paintView.filter_state = filter_arr[item_id_filter];

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
            e.printStackTrace();
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
        /*    case R.id.item_next_lead://used only for 6,3,1 lead data
                handle_item_next_lead();
                break;*/
     /*       case R.id.item_prev_lead://used only for 6,3,1 lead data
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

    public void status() {
        new AlertDialog.Builder(this)
                .setTitle("Device Status")
                .setMessage("Battery: " + battery_status + "\n V1 Status: " + v1_status + "\n V2 Status: " + v2_status + "\n V3 Status: " + v3_status + "\n V4 Status: " + v4_status + "\n V5 Status: " + v5_status + "\n V6 Status: " + v6_status + "\n LL Status: " + ll_status + "\n LA Status: " + la_status + "\n RA Status: " + ra_status)
                .setPositiveButton(android.R.string.ok, null)
                .show();
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

    private void handle_patient_detail() {
        try {
            load_existing_file = false;
            paintView.START_GUI = false;
//            Intent getData = new Intent(getApplicationContext(), Patient_Info.class);
//            startActivity(getData);
            set_title(mode, paintView.Auto);
            pv.invalidate();
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
                item_id_gain = 4;
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

            //  }
        } catch (Exception e) {
            e.printStackTrace();
        }
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

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}