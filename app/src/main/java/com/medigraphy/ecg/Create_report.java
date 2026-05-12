package com.medigraphy.ecg;

import java.io.BufferedOutputStream;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Random;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import com.itextpdf.text.BadElementException;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.ColumnText;
import com.itextpdf.text.pdf.GrayColor;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.Image;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.ContentUris;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Paint.Style;
import android.graphics.PorterDuff.Mode;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.comparator.LastModifiedFileComparator;

import pl.droidsonroids.gif.GifImageView;

import static android.content.Context.MODE_PRIVATE;

@SuppressLint("ViewConstructor")
public class Create_report extends View {
    paintView pv;
    Paint paint = new Paint();
    int width = 5 * 1080;//5400
    int height = 5 * 770;//3850
    int sample_data[] = new int[200];
    Bitmap bitmap;

    Canvas can;
    String lead_name[] = {"I", "II", "III", "aVR", "aVL", "aVF", "V1", "V2", "V3", "V4", "V5", "V6"};
    String lead_name6X1for2ndPage[] = {"V1", "V2", "V3", "V4", "V5", "V6", "I", "II", "III", "aVR", "aVL", "aVF",}; // Medigraphy for 6x1 part 2 (page 2)
    SharedPreferences hospitalPreference;
    SharedPreferences.Editor hospitalEditor;
    SharedPreferences hospitalDetailsPreference;
    SharedPreferences.Editor hospitalDetailsEditor;
    SharedPreferences reportPreference;
    SharedPreferences.Editor reportEditor;//int lead_arrange[]	={1,9,7,4,2,10,3,6,8,11,5,0};
    JsonArray reportDetailsList;
    int lead_arrange[] = {1, 2, 8, 9, 10, 11, 7, 3, 5, 4, 6, 0};
    int lead_arrange6X1for2ndPage[] = {7, 3, 5, 4, 6, 0, 1, 2, 8, 9, 10, 11}; // Medigraphy for 6x1 part 2 (page 2)
    public String comment;

    String report_name;
    public static String sFileTitle_png, sFileTitle_data;
    String OWNER_PASS = "ecg";
    static String USER_PASS;
    Context p_context;
    public static String sdf;
    public static boolean outofmemory_error = false, png_created = false;
    PdfContentByte cnvs;
    String Report_title;
    ProgressDialog progressDialog;
    private File path = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).getAbsolutePath() + "/ECG", "ECG-Recorder Reports");
    ArrayList<String> stringArrayList = new ArrayList<String>();
    private String strPass_On_age = strsge(String.valueOf(paintView.Pass_On_age));
    //File root;
    int REPORTTYPE = 0;

    Phrase p_age, p_gen, p_weight, p_height, p_DOB, p_date, p_bp, p_medi, p_chno, p_docName, p_hospitalName, p_comments, p_remark,
            gender, weight, DOB, date, BP, medication, age, pheight, chno, docName, hospitalName, comments, remark;

    String RandomDigit = "";
    int Count = 0;

    ArrayList<String> arrGain = new ArrayList<>();

    // This Function is For there is Any Folder with Same name if Yes then get Count of that Files and Append it in New File Name
    private void loadFileList() {
        try {
            if (path.exists()) {
                File[] files = path.listFiles();
                for (int i = 0; i < files.length; i++) {
                    if (files[i].getName().contains(paintView.Pass_On_name)) {
                        Count = Count + 1;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public static String strsge(String strPass_On_age) {
        if (strPass_On_age.equals("0") || strPass_On_age.equals("00")) {
            return "infant";
        } else {
            return strPass_On_age;
        }
    }

    @SuppressLint("SimpleDateFormat")
    public Create_report(Context context) {
        super(context);
        p_context = context;
        this.comment = "";
        USER_PASS = "";

        // Get File Count of Same Name
        loadFileList();

        if (Count != 0) {
            RandomDigit = " " + Count;
        }
        //setRoot();
        //used when ACQ of same patient but at different time
        if (MainActivity.acq_repeat == true) {
            Date now = new Date();
            sdf = new SimpleDateFormat("dd MMMM yyyy, HH:mm a").format(now);
            paintView.Pass_On_date = sdf;
        }

        if (MainActivity.iScreenWidth > paintView.XAXIS_WD) {
            this.setDrawingCacheEnabled(true);
            bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            can = new Canvas(bitmap);
            draw_grid();
        } else {
            //for small mobile only pdf is generated
            if (MainActivity.create_pdf == true || HelpActivity.create_pdf == true) {
                create_pdf();
                create_binary();
            }
        }
    }

    //draws the grid lines required to plot data
    public void draw_grid() {
        // int end_pt_vertical=1070,end_pt_hrznt=650,dirtn_v=1,dirtn_h=2;
        paint.setColor(Color.WHITE);
        can.drawRect(0, 0, bitmap.getWidth(), bitmap.getHeight(), paint);

        Draw_Lines(5 * 1070, 1);//vertical
        Draw_Lines(5 * 650, 2);//horizontal
        Draw_text();
        paint.setStyle(Style.FILL);
        plot_data();

    }

    private void Draw_Lines(int end_pt, int dirtn) {
        try {
            int x1 = 0, x2 = 0, y1 = 0, y2 = 0;

            if (dirtn == 1) {
                y1 = 5 * 126;
                y2 = 5 * ((650 + 124) - 8);
            } else {
                x1 = 5 * 5;
                x2 = 5 * 1065;
            }

            for (int i = 25, j = 0, k = 0; i <= end_pt - 5 * 4; i += 5 * 4, j++) //4 is the pixel on canvas
            {
                if (dirtn == 1)//vertical
                {
                    if (j % 5 == 0)//dark pink line
                    {
                        paint.setColor(Color.rgb(255, 51, 153));
                        paint.setStrokeWidth(2f);

                    } else {
                        paint.setColor(Color.rgb(255, 185, 220));
                        paint.setStrokeWidth(2f);
                    }

                    can.drawLine(i, y1, i, y2, paint);

                    //plots blue lines
                    if (j % 5 == 0) {
                        if ((k - 1) % 13 == 0 || k == 0) {
                            paint.setColor(Color.BLUE);
                            paint.setStrokeWidth(5f);
                            if ((k) == 14 || k == 27 || k == 40)
                                can.drawLine(i, y1, i, 5 * 605, paint);
                            else
                                can.drawLine(i, y1, i, y2, paint);
                        }
                        k++;
                    }

                } else//horizontal
                {
                    if (j % 5 == 0)//dark line
                    {
                        paint.setColor(Color.rgb(255, 51, 153));
                        paint.setStrokeWidth(2f);

                        if ((k) % 24 == 0 || k % 32 == 0)//draws horztal blue lines
                        {
                            paint.setColor(Color.BLUE);
                            paint.setStrokeWidth(5f);
                        }
                        k++;
                    } else {
                        paint.setColor(Color.rgb(255, 185, 220));
                        paint.setStrokeWidth(2f);
                    }
                    can.drawLine(x1, (i + 5 * 120), x2, (i + 5 * 120), paint);
                }

            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //writes the text of patient details on the ECG Report
    private void Draw_text() {
        int p = 0;
        int x, y;
        x = 5 * 30;

        reportDetailsList = new JsonArray();
        JsonParser jsonParser = new JsonParser();
        ArrayList<String> selectedReport = new ArrayList<>();

        if (!reportPreference.getString("Report", "").equals("")) {
            reportDetailsList = (JsonArray) jsonParser.parse(reportPreference.getString("Report", ""));

            for (int i = 0; i < reportDetailsList.size(); i++) {
                if (reportDetailsList.get(i).getAsJsonObject().get("isChecked").getAsString().equals("true")) {
                    selectedReport.add(reportDetailsList.get(i).getAsJsonObject().get("reportName").getAsString());
                }
            }
        }

        arrGain.clear();
        arrGain.add("2.5mm/mV");
        arrGain.add("5mm/mv");
        arrGain.add("7.5mm/mv");
        arrGain.add("10mm/mv");
        arrGain.add("15mm/mv");
        arrGain.add("20mm/mv");
        arrGain.add("30mm/mv");

        paint.setStyle(Style.STROKE);
        paint.setColor(Color.BLUE);
        can.drawRect(25, 25, 5 * 1065, 5 * 120, paint);
        paint.setColor(Color.BLACK);
        paint.setStyle(Style.FILL);
        paint.setTextSize(50f);
        paint.setStrokeWidth(1f);

        //prints the labels on report
        can.drawText("Patient  ", 5 * 10, 5 * 20, paint);
        can.drawText("Patient form No ", 5 * 10, 5 * 35, paint);
        can.drawText("Age  ", 5 * 10, 5 * 50, paint);
        can.drawText("Sex", 5 * 10, 5 * 65, paint);
        can.drawText("Medications", 5 * 10, 5 * 80, paint);
        can.drawText("Blood Pressure", 5 * 10, 5 * 95, paint);
        String Gain = MainActivity.selectedGain;

        can.drawText("X=25mm/sec Y=" + Gain + paintView.txt_filter, 5 * 10, 5 * 115, paint);
        //if value of report_sequential is 1  then it plots sequential report else plots simultaneous report
        Report_title = "(3X4 ";
        if (MainActivity.report_sequential == 1) {
            Report_title += "Sequential ECG Report)";
            report_name = "Sequential";
        } else if (MainActivity.report_sequential == 0) {
            Report_title += "Simultaneous ECG Report)";
            report_name = "Simultaneous";
        } else if (MainActivity.report_sequential == 2) {
            Report_title += "6x2 Report)";
            report_name = "6x2 Report";
        } else if (MainActivity.report_sequential == 3) {
            Report_title += "6x1 Report)";
            report_name = "6x1 Report";
        }

        can.drawText(Report_title, 5 * 410, 5 * 115, paint);

        if (!paintView.Pass_On_date.equals("")) {
            can.drawText("Date & Time: " + paintView.Pass_On_date, 5 * 300, 5 * 20, paint);
        } else if (!paintView.Pass_On_dob.equals("")) {
            can.drawText("Date of birth:" + paintView.Pass_On_dob, 5 * 300, 5 * 35, paint);
        } else if (!String.valueOf(paintView.Pass_On_ht).equals("")) {
            can.drawText("Height:" + paintView.Pass_On_ht, 5 * 300, 5 * 50, paint);
        } else if (!String.valueOf(paintView.Pass_On_wt).equals("")) {
            can.drawText("Weight:" + paintView.Pass_On_wt, 5 * 300, 5 * 65, paint);
        }
        can.drawText("Comment: ", 5 * 610, 5 * 20, paint);
        //if the length of comment is 15 then it will directly display.
        String copyComment = "";
        int length = (int) (paint.measureText(comment));
        int iRoundOff = (int) Math.round((length / 2003.0) + 0.5);//2003 no of characters set in one line in pixels in given space
        int istart = 0, iend = 84, y_co_ordinate = 99;
        copyComment = comment;

        if (!copyComment.equals("")) {
            if (length <= 2003) {
                //24 is the pixel to display characters
                can.drawText(copyComment.substring(istart, ((length / 24))), 3294, y_co_ordinate, paint);
                can.drawText(copyComment.substring(istart), 3294, (y_co_ordinate), paint);
            } else {
                for (int i = 0; i <= (iRoundOff); i++) {
                    if (i != (iRoundOff - 1))
                        can.drawText(copyComment.substring(istart, iend), 3294, y_co_ordinate, paint);
                    else
                        can.drawText(copyComment.substring(istart), 3294, (y_co_ordinate), paint);

                    istart = iend;

                    if (((length) - (iend * 24)) < 2003 && i != (iRoundOff)) {
                        iend = (length / 24);
                    } else if (istart == iend) {
                        iend += 2 * 47;
                    }
                    y_co_ordinate += 74;

                }//for(int i=0;i<=(iRoundOff);i++)
            }//else of if(length<=376)
        }

        //prints data on report
        can.drawText(": " + paintView.Pass_On_name, 5 * 120, 5 * 20, paint);
        can.drawText(": " + paintView.Pass_On_chno, 5 * 120, 5 * 35, paint);
        can.drawText(": " + paintView.Pass_On_age, 5 * 120, 5 * 50, paint);
        can.drawText(": " + paintView.Pass_On_gen, 5 * 120, 5 * 65, paint);
        can.drawText(": " + paintView.Pass_On_medi, 5 * 120, 5 * 80, paint);
        can.drawText(": " + paintView.Pass_On_BP, 5 * 120, 5 * 95, paint);

        paint.setStrokeWidth(0f);
        paint.setColor(Color.BLUE);
        paint.setStyle(Style.FILL);
        for (int i = 0; i < 4; i++) {
            y = 5 * 280;
            for (int j = 0; j < 3; j++) {

                for (int k = 0; k < selectedReport.size(); i++) {

                    if (selectedReport.get(i).equals("Simultaneous")) {
                        can.drawText(lead_name[p], x, y, paint);
                        p++;
                        y = y + 5 * 160;
                    }
                    if (selectedReport.get(i).equals("Sequential")) {
                        can.drawText(lead_name[p], x, y, paint);
                        p++;
                        y = y + 5 * 160;
                    }

                    if (selectedReport.get(i).equals("62Report")) {
                        can.drawText(lead_name[p], x, y, paint);
                        p++;
                        y = y + 5 * 160;
                    }

                    if (selectedReport.get(i).equals("61Report")) {

                        can.drawText(lead_name6X1for2ndPage[p], x, y, paint);
                        p++;
                        y = y + 5 * 160;

                    }
                    if (selectedReport.get(i).equals("61Report2ndPage")) {

                        can.drawText(lead_name[p], x, y, paint);
                        p++;
                        y = y + 5 * 160;

                    }
                }
            }
            x = x + 5 * 260;
        }
        can.drawText("II", 5 * 30, 5 * 760, paint);
    }

    /*
    //drawing dark lines on graph
     private void draw_Dark_lines(int endpt,int k1)
     {
         paint.setColor(Color.rgb(255,51,153));
         int limit=0;
         for (int g =5, count=0; g <=endpt-4; g +=4,count++)
         {
             if( count%5==0)
             { //vertical dark lines
                 if(k1==1)
                 {








                     //paint.setColor(Color.BLACK);
                    if(count/5==0||count/5==1|| count /5==14|| count /5==27 ||  count /5==40|| count/5==53  )
                    {
                         paint.setColor(Color.BLUE);
                         paint.setStrokeWidth(0);
                         paint.setStyle(Style.FILL);
                     }else
                     {
                         paint.setColor(Color.rgb(255,51,153));
                         paint.setStrokeWidth(0);
                     }
                    if(count /5==14|| count /5==27 ||  count /5==40)
                        limit=605;
                    else
                        limit=(650+124)-8;

                    can.drawLine(g, 126, g,limit,paint);

                    paint.setColor(Color.rgb(255,51,153));
                    paint.setStrokeWidth(0);
                    can.drawLine(285, 605, 285,(650+124)-8,paint);
                    can.drawLine(545, 605, 545,(650+124)-8,paint);
                    can.drawLine(805, 605, 805,(650+124)-8,paint);

                 }
                 else if(k1==2)
                 {
                     //horizontal dark lines
                     if(count/5!=32)
                     {
                      paint.setColor(Color.rgb(255,51,153));
                     }
                     if((count/5)%24==0 || (count/5)%32==0)
                     {
                    paint.setColor(Color.BLUE);
                    paint.setStyle(Style.FILL);
                     }
                  can.drawLine(5, g+120, 1065, g+120,paint);
                 }
             }
         }
     }*/
    //plots the ECG Data on the graph
    public void plot_data() {
        float prev_x1, xbase1, current_x1, current_y = 0, ybase, prev_y1;
        int x_offset[] = {5 * 25, 5 * 285, 5 * 545, 5 * 805};
        int y_offset[] = {5 * 205, 5 * 365, 5 * 525};

        //plot calibration on report
        paint.setColor(Color.BLACK);
        paint.setStrokeWidth(3f);
        plot_CAL();

        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 4; c++) {
                prev_x1 = x_offset[c];
                prev_y1 = y_offset[r];
                for (int d = 0; d < paintView.XAXIS_WD; d++) {
                    //current_y=y_offset[r]-Adjust_y_pixel(paintView.disp_report[lead_arrange[r+c*3]][d]);
                    switch (c) {
                        case 0:
                            current_y = y_offset[r] - Adjust_y_pixel(paintView.disp_report[lead_arrange[r + c * 3]][d]);
                            break;
                        case 1:
                            current_y = y_offset[r] - Adjust_y_pixel(paintView.disp_report[lead_arrange[r + c * 3]][d + (1300 * MainActivity.report_sequential)]);
                            break;
                        case 2:
                            current_y = y_offset[r] - Adjust_y_pixel(paintView.disp_report[lead_arrange[r + c * 3]][d + (2600 * MainActivity.report_sequential)]);

                            break;
                        case 3:
                            current_y = y_offset[r] - Adjust_y_pixel(paintView.disp_report[lead_arrange[r + c * 3]][d + (3900 * MainActivity.report_sequential)]);

                            break;
                        default:
                            break;
                    }
                    can.drawLine(prev_x1, prev_y1, x_offset[c] + d, current_y, paint);
                    prev_x1 = x_offset[c] + d;
                    prev_y1 = current_y;
                }
            }
        }

        //plot full lead II data
        prev_x1 = xbase1 = 5 * 25;
        prev_y1 = ybase = 5 * 685;
        paint.setColor(Color.BLACK);
        paint.setStrokeWidth(3f);
        for (int k = 0; k < paintView.total_samples; k++)//XAXIS_WD=1300
        {
            current_x1 = (float) (xbase1 + (k));
            //System.out.println("data=="+paintView.disp_report[2][k]);

            current_y = (float) (ybase - Adjust_y_pixel(paintView.disp_report[2][k]));
            can.drawLine(prev_x1, prev_y1, current_x1, current_y, paint);
            prev_x1 = current_x1;
            prev_y1 = current_y;
        }

        create_binary();//creates binary file
        if (MainActivity.create_image == true || HelpActivity.create_image == true)
            create_png();//creates PDF of ECG Report
        if (MainActivity.create_pdf == true || HelpActivity.create_pdf == true/*&& png_created==true*/)
            create_pdf();//creates PDF of ECG Report

        if (MainActivity.create_text == true || HelpActivity.create_text == true)
            create_ascii_file();
    }

    //plot calibration on report
    private void plot_CAL() {

        int inc = 5 * 205;
        can.drawLine(25, inc, 45, inc, paint);

        reportPreference = getContext().getSharedPreferences("REPORT_DETAILS", MODE_PRIVATE);
        reportEditor = reportPreference.edit();
        reportDetailsList = new JsonArray();
        JsonParser jsonParser = new JsonParser();
        ArrayList<String> selectedReport = new ArrayList<>();

        if (!reportPreference.getString("Report", "").equals("")) {
            reportDetailsList = (JsonArray) jsonParser.parse(reportPreference.getString("Report", ""));

            for (int k = 0; k < reportDetailsList.size(); k++) {
                if (reportDetailsList.get(k).getAsJsonObject().get("isChecked").getAsString().equals("true")) {
                    selectedReport.add(reportDetailsList.get(k).getAsJsonObject().get("reportName").getAsString());
                }
            }
        }

        for (int j = 0; j < selectedReport.size(); j++) {

            if (selectedReport.get(j).equals("Simultaneous")) {

                for (int i = 0; i < 4; i++) {

                    if (MainActivity.iGain == 1 || HelpActivity.iGain == 1) {
                        can.drawLine(45, inc, 45, inc - 50, paint);
                        can.drawLine(45, inc - 50, 105, inc - 50, paint);
                        can.drawLine(105, inc - 50, 105, inc, paint);
                    } else if (MainActivity.iGain == 2 || HelpActivity.iGain == 2) {
                        can.drawLine(45, inc, 45, inc - 100, paint);
                        can.drawLine(45, inc - 100, 105, inc - 100, paint);
                        can.drawLine(105, inc - 100, 105, inc, paint);
                    } else if (MainActivity.iGain == 3 || HelpActivity.iGain == 3) {

                        can.drawLine(45, inc, 45, inc - 150, paint);
                        can.drawLine(45, inc - 150, 105, inc - 150, paint);
                        can.drawLine(105, inc - 150, 105, inc, paint);
                    } else if (MainActivity.iGain == 4 || HelpActivity.iGain == 4) {

                        can.drawLine(45, inc, 45, inc - 200, paint);
                        can.drawLine(45, inc - 200, 105, inc - 200, paint);
                        can.drawLine(105, inc - 200, 105, inc, paint);
                    } else if (MainActivity.iGain == 6 || HelpActivity.iGain == 6) {

                        can.drawLine(45, inc, 45, inc - 300, paint);
                        can.drawLine(45, inc - 300, 105, inc - 300, paint);
                        can.drawLine(105, inc - 300, 105, inc, paint);
                    } else if (MainActivity.iGain == 8 || HelpActivity.iGain == 8) {

                        can.drawLine(45, inc, 45, inc - 400, paint);
                        can.drawLine(45, inc - 400, 105, inc - 400, paint);
                        can.drawLine(105, inc - 400, 105, inc, paint);
                    } else if (MainActivity.iGain == 12 || HelpActivity.iGain == 12) {

                        can.drawLine(45, inc, 45, inc - 600, paint);
                        can.drawLine(45, inc - 600, 105, inc - 600, paint);
                        can.drawLine(105, inc - 600, 105, inc, paint);
                    }

                    can.drawLine(105, inc, 125, inc, paint);
                    inc += 5 * 160;

                }

            } else if (selectedReport.get(j).equals("Sequential")) {
                for (int i = 0; i < 4; i++) {

                    if (MainActivity.iGain == 1 || HelpActivity.iGain == 1) {
                        can.drawLine(45, inc, 45, inc - 50, paint);
                        can.drawLine(45, inc - 50, 105, inc - 50, paint);
                        can.drawLine(105, inc - 50, 105, inc, paint);
                    } else if (MainActivity.iGain == 2 || HelpActivity.iGain == 2) {
                        can.drawLine(45, inc, 45, inc - 100, paint);
                        can.drawLine(45, inc - 100, 105, inc - 100, paint);
                        can.drawLine(105, inc - 100, 105, inc, paint);
                    } else if (MainActivity.iGain == 3 || HelpActivity.iGain == 3) {

                        can.drawLine(45, inc, 45, inc - 150, paint);
                        can.drawLine(45, inc - 150, 105, inc - 150, paint);
                        can.drawLine(105, inc - 150, 105, inc, paint);
                    } else if (MainActivity.iGain == 4 || HelpActivity.iGain == 4) {

                        can.drawLine(45, inc, 45, inc - 200, paint);
                        can.drawLine(45, inc - 200, 105, inc - 200, paint);
                        can.drawLine(105, inc - 200, 105, inc, paint);
                    } else if (MainActivity.iGain == 6 || HelpActivity.iGain == 6) {

                        can.drawLine(45, inc, 45, inc - 300, paint);
                        can.drawLine(45, inc - 300, 105, inc - 300, paint);
                        can.drawLine(105, inc - 300, 105, inc, paint);
                    } else if (MainActivity.iGain == 8 || HelpActivity.iGain == 8) {

                        can.drawLine(45, inc, 45, inc - 400, paint);
                        can.drawLine(45, inc - 400, 105, inc - 400, paint);
                        can.drawLine(105, inc - 400, 105, inc, paint);
                    } else if (MainActivity.iGain == 12 || HelpActivity.iGain == 12) {

                        can.drawLine(45, inc, 45, inc - 600, paint);
                        can.drawLine(45, inc - 600, 105, inc - 600, paint);
                        can.drawLine(105, inc - 600, 105, inc, paint);
                    }

                    can.drawLine(105, inc, 125, inc, paint);
                    inc += 5 * 160;

                }
            } else if (selectedReport.get(j).equals("62Report")) {

                for (int i = 0; i < 6; i++) {

                    if (MainActivity.iGain == 1 || HelpActivity.iGain == 1) {
                        can.drawLine(45, inc, 45, inc - 50, paint);
                        can.drawLine(45, inc - 50, 105, inc - 50, paint);
                        can.drawLine(105, inc - 50, 105, inc, paint);
                    } else if (MainActivity.iGain == 2 || HelpActivity.iGain == 2) {
                        can.drawLine(45, inc, 45, inc - 100, paint);
                        can.drawLine(45, inc - 100, 105, inc - 100, paint);
                        can.drawLine(105, inc - 100, 105, inc, paint);
                    } else if (MainActivity.iGain == 3 || HelpActivity.iGain == 3) {
                        can.drawLine(45, inc, 45, inc - 150, paint);
                        can.drawLine(45, inc - 150, 105, inc - 150, paint);
                        can.drawLine(105, inc - 150, 105, inc, paint);
                    } else if (MainActivity.iGain == 4 || HelpActivity.iGain == 4) {
                        can.drawLine(45, inc, 45, inc - 200, paint);
                        can.drawLine(45, inc - 200, 105, inc - 200, paint);
                        can.drawLine(105, inc - 200, 105, inc, paint);
                    } else if (MainActivity.iGain == 6 || HelpActivity.iGain == 6) {
                        can.drawLine(45, inc, 45, inc - 300, paint);
                        can.drawLine(45, inc - 300, 105, inc - 300, paint);
                        can.drawLine(105, inc - 300, 105, inc, paint);
                    } else if (MainActivity.iGain == 8 || HelpActivity.iGain == 8) {
                        can.drawLine(45, inc, 45, inc - 400, paint);
                        can.drawLine(45, inc - 400, 105, inc - 400, paint);
                        can.drawLine(105, inc - 400, 105, inc, paint);
                    } else if (MainActivity.iGain == 12 || HelpActivity.iGain == 12) {
                        can.drawLine(45, inc, 45, inc - 600, paint);
                        can.drawLine(45, inc - 600, 105, inc - 600, paint);
                        can.drawLine(105, inc - 600, 105, inc, paint);
                    }

                    can.drawLine(105, inc, 125, inc, paint);
                    inc += 5 * 100;
                }
            } else if (selectedReport.get(j).equals("61Report")) {
                for (int i = 0; i < 6; i++) {

                    if (MainActivity.iGain == 1 || HelpActivity.iGain == 1) {
                        can.drawLine(45, inc, 45, inc - 50, paint);
                        can.drawLine(45, inc - 50, 105, inc - 50, paint);
                        can.drawLine(105, inc - 50, 105, inc, paint);
                    } else if (MainActivity.iGain == 2 || HelpActivity.iGain == 2) {
                        can.drawLine(45, inc, 45, inc - 100, paint);
                        can.drawLine(45, inc - 100, 105, inc - 100, paint);
                        can.drawLine(105, inc - 100, 105, inc, paint);
                    } else if (MainActivity.iGain == 3 || HelpActivity.iGain == 3) {
                        can.drawLine(45, inc, 45, inc - 150, paint);
                        can.drawLine(45, inc - 150, 105, inc - 150, paint);
                        can.drawLine(105, inc - 150, 105, inc, paint);
                    } else if (MainActivity.iGain == 4 || HelpActivity.iGain == 4) {
                        can.drawLine(45, inc, 45, inc - 200, paint);
                        can.drawLine(45, inc - 200, 105, inc - 200, paint);
                        can.drawLine(105, inc - 200, 105, inc, paint);
                    } else if (MainActivity.iGain == 6 || HelpActivity.iGain == 6) {
                        can.drawLine(45, inc, 45, inc - 300, paint);
                        can.drawLine(45, inc - 300, 105, inc - 300, paint);
                        can.drawLine(105, inc - 300, 105, inc, paint);
                    } else if (MainActivity.iGain == 8 || HelpActivity.iGain == 8) {
                        can.drawLine(45, inc, 45, inc - 400, paint);
                        can.drawLine(45, inc - 400, 105, inc - 400, paint);
                        can.drawLine(105, inc - 400, 105, inc, paint);
                    } else if (MainActivity.iGain == 12 || HelpActivity.iGain == 12) {
                        can.drawLine(45, inc, 45, inc - 600, paint);
                        can.drawLine(45, inc - 600, 105, inc - 600, paint);
                        can.drawLine(105, inc - 600, 105, inc, paint);
                    }

                    can.drawLine(105, inc, 125, inc, paint);
                    inc += 5 * 100;
                }
            }
        }
    }

    //calculation for plotting data
    private float Adjust_y_pixel(float disp_report) {
        //40pixel correspondsto 2 boxes, which corresonds to 1mV means 27
        //return (float) ((200 / 27.0) * disp_report);//disp_report is the raw data value
        return (float) ((50 / 27.0) * disp_report);//disp_report is the raw data value
    }

    //function for creating binary file
    private void create_binary() {

        if (paintView.Pass_On_date == null) {
            Date now = new Date();
            sdf = new SimpleDateFormat("yyyy_MM_dd HH-mm").format(now);
            paintView.Pass_On_date = sdf;
        }

        try {
            sFileTitle_data = paintView.Pass_On_name.replaceAll(" ", "_") + "_" + paintView.Pass_On_date.replaceAll(" ", "_");

            // Here Random Digit is Count of File having same Name
            File pathOfReport = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).getAbsolutePath() + "/ECG/ECG-Recorder Reports", paintView.Pass_On_name + RandomDigit);
            if (!pathOfReport.exists()) {
                pathOfReport.mkdirs();
            }

            String mPath = paintView.Pass_On_name + RandomDigit + ".dat";
            File ECG_FILE_NAME = new File(pathOfReport, mPath);
            RandomAccessFile raf = new RandomAccessFile(ECG_FILE_NAME, "rw");

            raf.writeUTF("12ECG_2014\n");
            raf.writeUTF(paintView.Pass_On_date + "\n");
            raf.writeUTF(paintView.Pass_On_chno + "\n");
            raf.writeUTF(paintView.Pass_On_name + "\n");
            raf.writeUTF(paintView.Pass_On_dob + "\n");
            raf.writeUTF(strPass_On_age + "\n");
            raf.writeUTF(paintView.Pass_On_gen + "\n");
            raf.writeUTF(paintView.Pass_On_ht + "\n");
            raf.writeUTF(paintView.Pass_On_wt + "\n");
            raf.writeUTF(paintView.Pass_On_medi + "\n");
            raf.writeUTF(paintView.Pass_On_BP + "\n");
            raf.writeInt(MainActivity.iGain);
            raf.writeInt(HelpActivity.iGain);
            raf.writeInt(paintView.filter_state);
            raf.seek(1023);
            //writing raw data to binary file
            for (int j = 0; j < 12; j++) {
                for (int k = 0; k < paintView.total_samples; k++) {
                    raf.writeInt(paintView.Gen_report[j][k]);
                }
            }

            raf.close();

            Toast.makeText(getContext(), "Data Saved.", Toast.LENGTH_SHORT).show();
            MainActivity.report_gen = true;
            HelpActivity.report_gen = true;
            //MainActivity.view_report.setVisible(true);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("File not found exception: " + e.getMessage());
        }
    }

    private void create_png() {

        try {
            if (paintView.Pass_On_date == null) {
                Date now = new Date();
                sdf = new SimpleDateFormat("dd MMMM yyyy, HH:mm a").format(now);
                paintView.Pass_On_date = sdf;
            }
            sFileTitle_png = paintView.Pass_On_date.replaceAll(" ", "_") + "_" + paintView.Pass_On_name.replaceAll(" ", "_") + "_" + report_name;
            // Here Random Digit is Count of File having same Name
            File root = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).getAbsolutePath() + "/ECG/ECG-Recorder Reports", paintView.Pass_On_name + RandomDigit);
            if (!root.isDirectory())
                root.mkdirs();
            //   setRoot();

            //for .png file
            File file1 = new File(root, sFileTitle_png + ".PNG");
            file1.createNewFile();
            FileOutputStream fos1;
            fos1 = new FileOutputStream(file1);
            bitmap.compress(CompressFormat.PNG, 95, fos1);
            fos1.flush();
            fos1.close();
            Toast.makeText(getContext(), "Report generated successfully..", Toast.LENGTH_SHORT).show();
            //clear bitmap and canvas for further use...
            bitmap.recycle();
            can.drawColor(Color.WHITE, Mode.CLEAR);
            png_created = true;
        } catch (Exception e) {
            e.printStackTrace();
        } catch (OutOfMemoryError ex) {
            System.out.println(ex.getMessage());
        }
    }

    //function for creating PDF file
    private void create_pdf() {

        hospitalPreference = getContext().getSharedPreferences("HOSPITAL_DETAILS", MODE_PRIVATE);
        hospitalEditor = hospitalPreference.edit();

        hospitalDetailsPreference = getContext().getSharedPreferences("HOSPITAL_DETAILS_ADD", MODE_PRIVATE);
        hospitalDetailsEditor = hospitalDetailsPreference.edit();

        reportPreference = getContext().getSharedPreferences("REPORT_DETAILS", MODE_PRIVATE);
        reportEditor = reportPreference.edit();
        reportDetailsList = new JsonArray();
        JsonParser jsonParser = new JsonParser();
        ArrayList<String> selectedReport = new ArrayList<>();

        if (!reportPreference.getString("Report", "").equals("")) {
            reportDetailsList = (JsonArray) jsonParser.parse(reportPreference.getString("Report", ""));
            for (int i = 0; i < reportDetailsList.size(); i++) {
                if (reportDetailsList.get(i).getAsJsonObject().get("isChecked").getAsString().equals("true")) {
                    selectedReport.add(reportDetailsList.get(i).getAsJsonObject().get("reportName").getAsString());
                }
            }
        }

        try {
            if (paintView.Pass_On_date == null) {
                Date now = new Date();
                sdf = new SimpleDateFormat("dd MMMM yyyy HH:mm a").format(now);
                paintView.Pass_On_date = sdf;
            }
            /*       Report_title = "(3X4 ";
            if (MainActivity.report_sequential == 1) {
                Report_title += "Sequential ECG Report)";
                report_name = "Sequential";
            } else if (MainActivity.report_sequential == 0) {
                Report_title += "Simultaneous ECG Report)";
                report_name = "Simultaneous";
            } else if (MainActivity.report_sequential == 2) {
                Report_title += "6x2 Report)";
                report_name = "6x2 Report";
            } else if (MainActivity.report_sequential == 3) {
                Report_title += "6x1 Report)";
                report_name = "6x1 Report";
            }*/

            sFileTitle_png = paintView.Pass_On_name.replaceAll(" ", "_") + "_" + paintView.Pass_On_date.replaceAll(" ", "_");
            Log.e("===>>>>FILENAME", "create_pdf: " + sFileTitle_png);
            System.out.println("sFileTitle_png=" + sFileTitle_png);

            //setRoot();
            //Here Random Digit is Count of File having same Name
            File pathOfReport = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).getAbsolutePath() + "/ECG/ECG-Recorder Reports", paintView.Pass_On_name + RandomDigit);
            if (!pathOfReport.exists()) {
                pathOfReport.mkdirs();
            }

            String mPath = paintView.Pass_On_name + RandomDigit + ".pdf";
            File pdfFile = new File(pathOfReport, mPath);

            Document document = new Document(new Rectangle(width, height));
            PdfWriter writer = null;

            try {
                writer = PdfWriter.getInstance(document, new FileOutputStream(pdfFile));
//				writer.setEncryption(USER_PASS.getBytes(), OWNER_PASS.getBytes(),
//				PdfWriter.ALLOW_PRINTING, PdfWriter.ENCRYPTION_AES_128);
                document.setMargins(0, 0, 0, 0);
                document.open();
            } catch (DocumentException e) {
                e.printStackTrace();
            }
            Image imgAppLogo = null;
            try {
                Drawable d = getResources().getDrawable(R.drawable.company_logo);
                BitmapDrawable bitDw = ((BitmapDrawable) d);
                Bitmap bmp = bitDw.getBitmap();
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                bmp.compress(Bitmap.CompressFormat.PNG, 100, stream);
                imgAppLogo = Image.getInstance(stream.toByteArray());
                imgAppLogo.scaleAbsolute(170, 150);
                imgAppLogo.setAbsolutePosition(5400 - 250, 3750 - 100);

            } catch (Exception e) {
                e.printStackTrace();
            }

            for (int i = 0; i < selectedReport.size(); i++) {

//                if (selectedReport.get(i).equals("Simultaneous")) {
//                    Report_title = "(3X4 ";
//                    Report_title += "Simultaneous ECG Report)";
//                    report_name = "Simultaneous";
//                    REPORTTYPE =0;
//                    PdfContentByte canvas = writer.getDirectContent();
//
//                    canvas.setColorStroke(new BaseColor(21, 64, 234));//blue
//                    Rectangle data_rect = new Rectangle(36, 36, width - 63, 3235);
//                    data_rect.setBorder(Rectangle.BOX);
//                    data_rect.setBorderWidth(2);
//                    canvas.rectangle(data_rect);
//                    canvas.stroke();
//                    print_details_pdf(canvas);//prints patient details on report
//                    draw_grid_pdf(canvas, 1, width - 63);//5350 vertical
//                    draw_grid_pdf(canvas, 2, 3250);//3250 horizontal
//                    plot_CAL_pdf(canvas);//plot calibration on report
//                    plot_data_for_pdf(canvas);//plot data
//                    document.newPage();
//
//                }
//
//                if (selectedReport.get(i).equals("Sequential")) {
//
//                    Report_title = "(3X4 ";
//                    Report_title += "Sequential ECG Report)";
//                    report_name = "Sequential";
//
//                    REPORTTYPE=1;
//                    PdfContentByte canvas = writer.getDirectContent();
//
//                    canvas.setColorStroke(new BaseColor(21, 64, 234));//blue
//                    Rectangle data_rect = new Rectangle(36, 36, width - 63, 3235);
//                    data_rect.setBorder(Rectangle.BOX);
//                    data_rect.setBorderWidth(2);
//                    canvas.rectangle(data_rect);
//                    canvas.stroke();
//                    print_details_pdf(canvas);//prints patient details on report
//                    draw_grid_pdf(canvas, 1, width - 63);//5350 vertical
//                    draw_grid_pdf(canvas, 2, 3250);//3250 horizontal
//                    plot_CAL_pdf(canvas);//plot calibration on report
//                    plot_data_for_pdf(canvas);//plot data
//                    document.newPage();
//                }
//                if (selectedReport.get(i).equals("62Report")) {
//                    Report_title = "6x2 ECG report";
//                    report_name = "6x2 ECG report";
//                    PdfContentByte canvas2 = writer.getDirectContent();
//
//                    canvas2.setColorStroke(new BaseColor(21, 64, 234));//blue
//                    Rectangle data_rect = new Rectangle(36, 36, width - 63, 3235);
//                    data_rect.setBorder(Rectangle.BOX);
//                    data_rect.setBorderWidth(2);
//                    canvas2.rectangle(data_rect);
//                    canvas2.stroke();
//                    print_details_pdf_6X2(canvas2);//prints patient details on report
//                    draw_grid_pdf_6X2(canvas2, 1, width - 63);//5350 vertical
//                    draw_grid_pdf_6X2(canvas2, 2, 3250);//3250 horizontal
//                    plot_CAL_pdf_6X2_6X1(canvas2);//plot calibration on report
//                    plot_data_for_pdf_6X2(canvas2);
//                    document.newPage();
//                }

                if (selectedReport.get(i).equals("61Report")) {

                    Report_title = "12 x 1 report - 1st page ( 10 secs )";
                    report_name = "12 x 1 report - 1st page ( 10 secs )";

                    PdfContentByte canvas = writer.getDirectContent();
                    document.add(imgAppLogo);
                    try {
                        if (!hospitalDetailsPreference.getString("HOSPITAL_LOGO", "").equals("")) {

                            byte[] imageAsBytes = Base64.decode(hospitalDetailsPreference.getString("HOSPITAL_LOGO", "").getBytes(), Base64.DEFAULT);
                            Image hospitalImage = Image.getInstance(imageAsBytes);
                            Log.e("==>>>IMG", "create_pdf: " + hospitalImage);

                            hospitalImage.scaleAbsolute(250, 250);
                            hospitalImage.setAbsolutePosition(5400 - 5250, 3750 - 200);
                            document.add(hospitalImage);
                        }

                        Image image = Image.getInstance(getPathFromUri(getContext(), paintView.Pass_On_Image));
                        image.scaleAbsolute(250, 250);
                        image.setAbsolutePosition(5400 - 5250, 3750 - 470);
                        document.add(image);

                    } catch (Exception e) {
                        Log.e("Pdf", e.toString());
                    }

                    canvas.setColorStroke(new BaseColor(21, 64, 234));//blue
                    Rectangle data_rect = new Rectangle(36, 36, width - 63, 3235);
                    data_rect.setBorder(Rectangle.BOX);
                    data_rect.setBorderWidth(2);
                    canvas.rectangle(data_rect);
                    canvas.stroke();
                    print_details_pdf_6X1_1st_page(canvas);//prints patient details on report
                    draw_grid_pdf_6X1(canvas, 1, width - 63);//5350 vertical
                    draw_grid_pdf_6X1(canvas, 2, 3250);//3250 horizontal
                    plot_CAL_pdf_6X2_6X1(canvas);//plot calibration on report
                    plot_data_for_pdf_6X1_for_1st_page(canvas);
                    document.newPage();
                }

                if (selectedReport.get(i).equals("61Report2")) {

                    Report_title = "12 x 1 report - 2nd page (10 secs)";
                    report_name = "12 x 1 report - 2nd page (10 secs)";

                    PdfContentByte canvas2 = writer.getDirectContent();
                    canvas2.setColorStroke(new BaseColor(21, 64, 234));//blue
                    Rectangle data_rect2 = new Rectangle(36, 36, width - 63, 3235);
                    data_rect2.setBorder(Rectangle.BOX);
                    data_rect2.setBorderWidth(2);
                    canvas2.rectangle(data_rect2);
                    canvas2.stroke();
                    print_details_pdf_6X1_2nd_page(canvas2);//prints patient details on report
                    draw_grid_pdf_6X1(canvas2, 1, width - 63);//5350 vertical
                    draw_grid_pdf_6X1(canvas2, 2, 3250);//3250 horizontal
                    plot_CAL_pdf_6X2_6X1(canvas2);//plot calibration on report
                    plot_data_for_pdf_6X1_for_2nd_page(canvas2);
                    document.newPage();

                }

            }

            if (MainActivity.report_gen == true) {
                MainActivity.report_gen = false;
            } else if (HelpActivity.report_gen == true) {
                HelpActivity.report_gen = false;
            }
            document.close();
        } catch (IOException e) {
            progressDialog.dismiss();
            e.printStackTrace();
        } catch (DocumentException e) {
            e.printStackTrace();
        }
    }

//    private void setRoot() {
//        root = new File(Environment.getExternalStorageDirectory() + "/ECG/ECG-Recorder Reports", paintView.Pass_On_name);
//        isCreate = true;
//        while (isCreate) {
//
//            if (!root.isDirectory()) {
//
//                root.mkdirs();
//                isCreate = false;
//                count = 1;
//
//            } else {
//
//                root = new File(Environment.getExternalStorageDirectory() + "/ECG/ECG-Recorder Reports", paintView.Pass_On_name + "(" + count + ")");
//                count++;
//                isCreate = true;
//
//            }
//        }
//    }

    private void displayFiles(File[] files) {
        stringArrayList.clear();
        for (File file : files) {
            if ((file.isFile() && file.getName().endsWith(".pdf")) || file.isDirectory()) {
                stringArrayList.add(FilenameUtils.getName(file.getName()));
            }
        }
    }

    private void plot_CAL_pdf(PdfContentByte canvas) {

        try {

            int inc = (5 * 47) + 200;//1025

            canvas.setColorStroke(new GrayColor(0.2f));
            canvas.setColorFill(new BaseColor(0, 0, 0));
/*
            for (int i = 0; i < 4; i++) {
                canvas.moveTo(36, inc);
                canvas.lineTo(55, inc);
                canvas.moveTo(55, inc);
                canvas.lineTo(55, inc + 200);
                canvas.moveTo(55, inc + 200);
                canvas.lineTo(115, inc + 200);
                canvas.moveTo(115, inc + 200);
                canvas.lineTo(115, inc);
                canvas.moveTo(115, inc);
                canvas.lineTo(135, inc);
                canvas.fillStroke();
                inc += (5 * 160);
            }

*/
            //  int inc = 5 * 205;
/*
            can.drawLine(25, inc, 45, inc, paint);

            reportPreference = getContext().getSharedPreferences("REPORT_DETAILS", MODE_PRIVATE);
            reportEditor = reportPreference.edit();
            reportDetailsList = new JsonArray();
            JsonParser jsonParser = new JsonParser();
            ArrayList<String> selectedReport = new ArrayList<>();

            if (!reportPreference.getString("Report", "").equals("")) {
                reportDetailsList = (JsonArray) jsonParser.parse(reportPreference.getString("Report", ""));

                for (int k = 0; k < reportDetailsList.size(); k++) {
                    if (reportDetailsList.get(k).getAsJsonObject().get("isChecked").getAsString().equals("true")) {
                        selectedReport.add(reportDetailsList.get(k).getAsJsonObject().get("reportName").getAsString());
                    }
                }
            }
*/

            // for (int j = 0; j < selectedReport.size(); j++) {

            //if (selectedReport.get(j).equals("Simultaneous")) {

            for (int i = 0; i < 4; i++) {

                canvas.moveTo(36, inc);
                canvas.lineTo(55, inc);
                canvas.moveTo(55, inc);

                if (MainActivity.iGain == 1 || HelpActivity.iGain == 1) {
                    canvas.lineTo(55, inc + 50); // For iGAIN = 1
                    canvas.moveTo(55, inc + 50); // For iGAIN = 1
                    canvas.lineTo(115, inc + 50); // For iGAIN = 1
                    canvas.moveTo(115, inc + 50); // For iGAIN = 1
                } else if (MainActivity.iGain == 2 || HelpActivity.iGain == 2) {
                    canvas.lineTo(55, inc + 100); // For iGAIN = 2
                    canvas.moveTo(55, inc + 100); // For iGAIN = 2
                    canvas.lineTo(115, inc + 100); // For iGAIN = 2
                    canvas.moveTo(115, inc + 100); // For iGAIN = 2
                } else if (MainActivity.iGain == 3 || HelpActivity.iGain == 3) {

                    canvas.lineTo(55, inc + 150); // For iGAIN = 3
                    canvas.moveTo(55, inc + 150); // For iGAIN = 3
                    canvas.lineTo(115, inc + 150); // For iGAIN = 3
                    canvas.moveTo(115, inc + 150); // For iGAIN = 3
                } else if (MainActivity.iGain == 4 || HelpActivity.iGain == 4) {

                    canvas.lineTo(55, inc + 200); // For iGAIN = 4
                    canvas.moveTo(55, inc + 200); // For iGAIN = 4
                    canvas.lineTo(115, inc + 200); // For iGAIN = 4
                    canvas.moveTo(115, inc + 200); // For iGAIN = 4
                } else if (MainActivity.iGain == 6 || HelpActivity.iGain == 6) {

                    canvas.lineTo(55, inc + 300); // For iGAIN = 6
                    canvas.moveTo(55, inc + 300); // For iGAIN = 6
                    canvas.lineTo(115, inc + 300); // For iGAIN = 6
                    canvas.moveTo(115, inc + 300); // For iGAIN = 6
                } else if (MainActivity.iGain == 8 || HelpActivity.iGain == 8) {

                    canvas.lineTo(55, inc + 400); // For iGAIN = 8
                    canvas.moveTo(55, inc + 400); // For iGAIN = 8
                    canvas.lineTo(115, inc + 400); // For iGAIN = 8
                    canvas.moveTo(115, inc + 400); // For iGAIN = 8
                } else if (MainActivity.iGain == 12 || HelpActivity.iGain == 12) {

                    canvas.lineTo(55, inc + 600); // For iGAIN = 12
                    canvas.moveTo(55, inc + 600); // For iGAIN = 12
                    canvas.lineTo(115, inc + 600); // For iGAIN = 12
                    canvas.moveTo(115, inc + 600); // For iGAIN = 12
                }

                canvas.lineTo(115, inc);
                canvas.moveTo(115, inc);
                canvas.lineTo(135, inc);
                canvas.fillStroke();
                inc += (5 * 160);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void print_details_pdf(PdfContentByte canvas) {

        hospitalPreference = getContext().getSharedPreferences("HOSPITAL_DETAILS", MODE_PRIVATE);
        hospitalEditor = hospitalPreference.edit();

        hospitalDetailsPreference = getContext().getSharedPreferences("HOSPITAL_DETAILS_ADD", MODE_PRIVATE);
        hospitalDetailsEditor = hospitalDetailsPreference.edit();

        try {
            int h = (height - (5 * 740));
            int p = 0;
            int x, y;
            int istart = 0, iend = 80, y_co_ordinate = height - 99;
            int NO_OF_CHAR = 80, SIZE_PER_CHARACTER = 24;

            //draws the rectangle to print details
            Rectangle patient_detail_rect = new Rectangle(36, 3250, width - 63, height - 36);
            //patient_detail_rect.setBorder(Rectangle.BOX);
            //patient_detail_rect.setBorderWidth(2);
            //canvas.rectangle(patient_detail_rect);
            //draws the rectangle to print details

            if (!hospitalDetailsPreference.getString("DOC_NAME", "").equals("")
                    || !hospitalDetailsPreference.getString("HOSPITAL_NAME", "").equals("")
                    || !hospitalDetailsPreference.getString("HOSPITAL_LOGO", "").equals("")) {

                Rectangle patient_detail_rect2 = new Rectangle(36, 3850 - 300, width - 63, height - 36);
                patient_detail_rect2.setBorder(Rectangle.BOX);
                patient_detail_rect2.setBorderWidth(2);
                canvas.rectangle(patient_detail_rect2);

            } else {
                patient_detail_rect.setBorder(Rectangle.BOX);
                patient_detail_rect.setBorderWidth(2);
                canvas.rectangle(patient_detail_rect);
            }

            //calculating font size
            String text = "Test";//string used for calculating the text size for rectangle
            // try to get max font size that fit in rectangle
            BaseFont base_font = null;
            try {
                base_font = BaseFont.createFont();
            } catch (DocumentException e) {
                e.printStackTrace();
            }
            int textHeightInGlyphSpace = base_font.getAscent(text) - base_font.getDescent(text);
            float fontSize = 75f * patient_detail_rect.getHeight() / textHeightInGlyphSpace;
            float hospitalNamFont = 160f * patient_detail_rect.getHeight() / textHeightInGlyphSpace;
            float DoctNameFont = 110f * patient_detail_rect.getHeight() / textHeightInGlyphSpace;

            //phrase used to set the fontsize for the given text
            Phrase name = new Phrase("Patient Name :", new Font(base_font, fontSize));

            Phrase filter = new Phrase("X=25mm/sec Y=" + MainActivity.selectedGain + "  " + paintView.txt_filter, new Font(base_font, fontSize));
            Phrase title = new Phrase(Report_title, new Font(base_font, fontSize));
            //Phrase txt_comment = new Phrase("Comments :  ", new Font(base_font, fontSize));

            Phrase p_name = new Phrase(paintView.Pass_On_name, new Font(base_font, fontSize));
            BP = new Phrase("Bp  :", new Font(base_font, fontSize));
            p_bp = new Phrase("188/90", new Font(base_font, fontSize));

            comments = new Phrase("Comments :", new Font(base_font, fontSize));
            // Append Years After Age in PDF file

            if (!String.valueOf(strPass_On_age).equals("") && !String.valueOf(strPass_On_age).equals("0")) {
                age = new Phrase("Age :", new Font(base_font, fontSize));
                if (strPass_On_age.equals("infant")) {
                    p_age = new Phrase(strPass_On_age, new Font(base_font, fontSize));
                } else {
                    p_age = new Phrase(strPass_On_age + " Years", new Font(base_font, fontSize));
                }
            }


            if (!paintView.Pass_On_chno.equals("")) {
                chno = new Phrase("Patient form No :", new Font(base_font, fontSize));
                p_chno = new Phrase(paintView.Pass_On_chno, new Font(base_font, fontSize));
            }

            if (!paintView.Pass_On_gen.equals("")) {
                gender = new Phrase("Sex :", new Font(base_font, fontSize));
                p_gen = new Phrase(paintView.Pass_On_gen, new Font(base_font, fontSize));
            }

            if (!paintView.Pass_On_medi.equals("")) {
                medication = new Phrase("Medications :", new Font(base_font, fontSize));
                p_medi = new Phrase(paintView.Pass_On_medi, new Font(base_font, fontSize));
            }

            if (!paintView.Pass_On_BP.equals("")) {
                remark = new Phrase("Remark :", new Font(base_font, fontSize));
                p_remark = new Phrase(paintView.Pass_On_BP, new Font(base_font, fontSize));
            }

            if (!paintView.Pass_On_date.equals("")) {
                date = new Phrase("Date & Time :", new Font(base_font, fontSize));
                p_date = new Phrase(paintView.Pass_On_date, new Font(base_font, fontSize));
            }

            if (!paintView.Pass_On_dob.equals("")) {
                DOB = new Phrase("Date of birth :", new Font(base_font, fontSize));
                p_DOB = new Phrase(paintView.Pass_On_dob, new Font(base_font, fontSize));
            }

            if (!String.valueOf(paintView.Pass_On_ht).equals("") && !String.valueOf(paintView.Pass_On_ht).equals("0")) {
                pheight = new Phrase("Height :", new Font(base_font, fontSize));
                p_height = new Phrase(paintView.Pass_On_ht + " cm", new Font(base_font, fontSize));
            }

            if (!String.valueOf(paintView.Pass_On_wt).equals("") && !String.valueOf(paintView.Pass_On_wt).equals("0")) {
                weight = new Phrase("Weight :", new Font(base_font, fontSize));
                p_weight = new Phrase(paintView.Pass_On_wt + " kg", new Font(base_font, fontSize));
            }

            if (!hospitalDetailsPreference.getString("HOSPITAL_NAME", "").equals("")) {
                hospitalName = new Phrase("Hospital Name :", new Font(base_font, hospitalNamFont));
                p_hospitalName = new Phrase(hospitalDetailsPreference.getString("HOSPITAL_NAME", ""), new Font(base_font, hospitalNamFont));
            }

            if (!hospitalDetailsPreference.getString("DOC_NAME", "").equals("")) {
                docName = new Phrase("Doctor Name :", new Font(base_font, DoctNameFont));
                p_docName = new Phrase(hospitalDetailsPreference.getString("DOC_NAME", ""), new Font(base_font, DoctNameFont));
            }

            //print the patient details

            ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT, name, 500, 3750 - 320, 0);
            ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT, p_name, 900, 3750 - 320, 0);

            // ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT, comments, 500, 3750 - 325, 0);

            ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT, DOB, 1700, 3750 - 320, 0);
            ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT, p_DOB, 2050, 3750 - 320, 0);

            ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT, weight, 1700, 3750 - 395, 0);
            ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT, p_weight, 1930, 3750 - 395, 0);

            ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT, age, 2700, 3750 - 320, 0);
            ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT, p_age, 2850, 3750 - 320, 0);

            ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT, pheight, 2700, 3750 - 395, 0);
            ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT, p_height, 2900, 3750 - 395, 0);

            ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT, chno, 3200, 3750 - 320, 0);
            ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT, p_chno, 3650, 3750 - 320, 0);

            ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT, gender, 3200, 3750 - 395, 0);
            ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT, p_gen, 3350, 3750 - 395, 0);

            ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT, filter, 4000, 3750 - 320, 0);
            ColumnText.showTextAligned(canvas, Element.ALIGN_CENTER, title, 4400, 3750 - 395, 0);

            ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT, date, 500, 3750 - 395, 0);
            ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT, p_date, 880, 3750 - 395, 0);

            //  ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT, BP, 4650, 3750 - 255, 0);
            //  ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT, p_bp, 4800, 3750 - 255, 0);

            ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT, medication, 500, 3750 - 465, 0);
            ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT, p_medi, 850, 3750 - 465, 0);

            ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT, remark, 3000, 3750 - 465, 0);
            ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT, p_remark, 3250, 3750 - 465, 0);

            //ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT, docName, 1500, 3460, 0);
            ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT, p_docName, 500, 3750 - 140, 0);

            //ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT, hospitalName, 1500, 3390, 0);
            ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT, p_hospitalName, 500, 3750 - 50, 0);

            //ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT,txt_comment,2990,3750,0);

            //print lead names
            x = 5 * 40;
            canvas.setColorFill(BaseColor.BLUE);//sets font color

            for (int i = 0; i < 4; i++) {
                y = ((2600));//5*280,
                for (int j = 0; j < 3; j++) {
                    Phrase leadnames = new Phrase(lead_name[p], new Font(base_font, fontSize));
                    ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT, leadnames, x, y, 0);
                    p++;
                    y = y - (5 * 160);//800
                }
                x = x + 5 * 260; //1350
            }

            Phrase lead2 = new Phrase("II", new Font(base_font, fontSize));
            ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT, lead2, 5 * 40, h, 0);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void plot_data_for_pdf(PdfContentByte canvas) {
        try {
            float prev_x1, xbase1, current_x1, current_y = 0, ybase, prev_y1;
            int x_offset[] = {135, 1435, 2735, 4035};
            int y_offset[] = {height - 2837, height - 2037, height - 1234};
            canvas.setColorStroke(new GrayColor(0.2f));

            for (int r = 0; r < 3; r++) {
                for (int c = 0; c < 4; c++) {
                    prev_x1 = x_offset[c];
                    prev_y1 = y_offset[r];
                    for (int d = 0; d < paintView.XAXIS_WD; d++) {
                        switch (c) {
                            case 0:
                                current_y = y_offset[r] - Adjust_y_pixel(paintView.disp_report[lead_arrange[r + c * 3]][d]);
                                current_y = height - current_y;
                                break;
                            case 1:
                                current_y = y_offset[r] - Adjust_y_pixel(paintView.disp_report[lead_arrange[r + c * 3]][d + (1300 * REPORTTYPE)]);
                                current_y = height - current_y;
                                break;
                            case 2:
                                current_y = y_offset[r] - Adjust_y_pixel(paintView.disp_report[lead_arrange[r + c * 3]][d + (2600 * REPORTTYPE)]);
                                current_y = height - current_y;
                                break;
                            case 3:
                                current_y = y_offset[r] - Adjust_y_pixel(paintView.disp_report[lead_arrange[r + c * 3]][d + (3900 * REPORTTYPE)]);
                                current_y = height - current_y;
                                break;
                            default:
                                break;
                        }
                        if (d == 0) {
                            canvas.saveState();
                            canvas.setColorStroke(new GrayColor(0.2f));
                            canvas.setColorFill(new BaseColor(0, 0, 0));
                            canvas.circle(prev_x1, current_y, 1);
                            canvas.fillStroke();
                            canvas.restoreState();
                        } else {
                            canvas.saveState();
                            canvas.setColorStroke(new GrayColor(0.2f));
                            canvas.setColorFill(new BaseColor(0, 0, 0));
                            canvas.moveTo(prev_x1, prev_y1);
                            canvas.lineTo(x_offset[c] + d, current_y);
                            canvas.fillStroke();
                            canvas.restoreState();
                        }

                        prev_x1 = x_offset[c] + d;
                        prev_y1 = current_y;
                    }
                }
            }

            //plot full lead II data
            prev_x1 = xbase1 = 135;//5*25;
            prev_y1 = ybase = (3420);//3425,5*685
            paint.setColor(Color.BLACK);
            paint.setStrokeWidth(3f);
            for (int k = 0; k < paintView.total_samples - 1300; k++)//XAXIS_WD=1300
            {
                current_x1 = (float) (xbase1 + (k));
                current_y = (float) (ybase - Adjust_y_pixel(paintView.disp_report[2][k]));
                current_y = height - current_y;

                if (k == 0) {
                    canvas.saveState();
                    canvas.setColorStroke(new GrayColor(0.2f));
                    canvas.setColorFill(new BaseColor(0, 0, 0));
                    canvas.circle(prev_x1, current_y, 1);
                    canvas.fillStroke();
                    canvas.restoreState();
                } else {
                    canvas.saveState();
                    canvas.setColorStroke(new GrayColor(0.2f));
                    canvas.setColorFill(new BaseColor(0, 0, 0));
                    canvas.moveTo(prev_x1, prev_y1);
                    canvas.lineTo(current_x1, current_y);
                    canvas.fillStroke();
                    canvas.restoreState();
                }
                prev_x1 = current_x1;
                prev_y1 = current_y;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void draw_grid_pdf(PdfContentByte canvas, int dirtn, int end_pt) {
        try {
            int x1 = 0, x2 = 0, y1 = 0, y2 = 0;
            if (dirtn == 1)// vertical lines
            {
                y1 = 36;
                y2 = 3235; // y2=3035;3250
            } else ///horizontal lines
            {
                x1 = 36;
                x2 = width - 63;
            }

            for (int i = 36, j = 0, k = 0; i <= end_pt - 5 * 4; i += 5 * 4, j++) //4 is the pixel on canvas
            {
                if (dirtn == 1)//vertical
                {
                    if (j % 5 == 0)//dark pink line
                    {
                        canvas.setColorStroke(new BaseColor(255, 51, 153));
                    } else {
                        canvas.setColorStroke(new BaseColor(255, 185, 220));
                    }
                    canvas.saveState();
                    canvas.moveTo(i, y1);
                    canvas.lineTo(i, y2);
                    canvas.fillStroke();
                    canvas.restoreState();

                    //plots blue lines
                    if (j % 5 == 0) {
                        if ((k - 1) % 13 == 0 || k == 0) {
                            canvas.saveState();
                            canvas.setColorStroke(new BaseColor(21, 64, 234));//blue
                            if ((k) == 14 || k == 27 || k == 40) {
                                canvas.moveTo(i, 840);
                                canvas.lineTo(i, y2);//5*605,3025
                                canvas.fillStroke();
                                canvas.restoreState();
                            } else {
                                canvas.moveTo(i, y1);
                                canvas.lineTo(i, y2);
                                canvas.fillStroke();
                                canvas.restoreState();
                            }
                        }
                        k++;
                    }
                } else//horizontal
                {
                    if (j % 5 == 0)//dark line
                    {
                        canvas.setColorStroke(new BaseColor(255, 51, 153));
                        if ((k) == 8 || k == 0)//draws horztal blue lines
                        {
                            canvas.setColorStroke(new BaseColor(21, 64, 234));
                        }
                        k++;
                    } else {
                        canvas.setColorStroke(new BaseColor(255, 185, 220));
                    }
                    canvas.saveState();
                    canvas.moveTo(x1, i);
                    canvas.lineTo(x2, i);
                    canvas.fillStroke();
                    canvas.restoreState();
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void create_ascii_file() {

        try {
            sFileTitle_data = paintView.Pass_On_date.replaceAll(" ", "_") + "_" + paintView.Pass_On_name.replaceAll(" ", "_");

            // Here Random Digit is Count of File having same Name
            File root = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).getAbsolutePath() + "/ECG/ECG-Recorder Reports", paintView.Pass_On_name + RandomDigit);
            if (!root.isDirectory())
                root.mkdirs();

            File ECG_FILE_NAME = new File(root, sFileTitle_data + ".txt");

            FileOutputStream fstream = new FileOutputStream(ECG_FILE_NAME);
            BufferedOutputStream bstream = new BufferedOutputStream(fstream);
            DataOutputStream dstream = new DataOutputStream(bstream);

            dstream.writeBytes("12ECG_2014\n");
            dstream.writeBytes("Patient Details:\n");
            dstream.writeBytes("Date:" + paintView.Pass_On_date + "\n");
            dstream.writeBytes("ChSS No:" + paintView.Pass_On_chno + "\n");
            dstream.writeBytes("Name:" + paintView.Pass_On_name + "\n");
            dstream.writeBytes("Date of birth:" + paintView.Pass_On_dob + "\n");
            dstream.writeBytes("Age:" + strPass_On_age + "\n");
            dstream.writeBytes("Gender:" + paintView.Pass_On_gen + "\n");
            dstream.writeBytes("Height:" + paintView.Pass_On_ht + "\n");
            dstream.writeBytes("Weight:" + paintView.Pass_On_wt + "\n");
            dstream.writeBytes("Medications:" + paintView.Pass_On_medi + "\n");
            dstream.writeBytes("Blood pressure:" + paintView.Pass_On_BP + "\n");
            dstream.writeBytes("Gain:\n");

            dstream.writeBytes(String.format("%20s %20s %20s %20s %20s %20s %20s %20s %20s %20s %20s %20s", "lead I", "lead II ", "lead III", "lead aVR", "lead aVL", "lead aVF", "lead V1", "lead V2", "lead V3", "lead V4", "lead V5", "lead V6\r\n"));

            for (int i = 0; i < paintView.total_samples; i++) {
                dstream.writeBytes(String.format("%20s %20s %20s %20s %20s %20s %20s %20s %20s %20s %20s %20s", paintView.raw_data[paintView.Lead_I][i], paintView.raw_data[paintView.Lead_II][i], paintView.raw_data[paintView.Lead_III][i], paintView.raw_data[paintView.aVR][i], paintView.raw_data[paintView.aVL][i], paintView.raw_data[paintView.aVF][i], paintView.raw_data[paintView.V1][i], paintView.raw_data[paintView.V2][i], paintView.raw_data[paintView.V3][i], paintView.raw_data[paintView.V4][i], paintView.raw_data[paintView.V5][i], paintView.raw_data[paintView.V6][i] + "\r\n"));
            }
            dstream.close();
            bstream.close();
            fstream.close();
            Toast.makeText(getContext(), "text file created", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String getPathFromUri(final Context context, final Uri uri) {

        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).getAbsolutePath() + "/" + split[1];
                }

                // TODO handle non-primary volumes
            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {

                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));
                return getDataColumn(context, contentUri, null, null);
            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[]{
                        split[1]
                };
                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {
            // Return the remote address
            if (isGooglePhotosUri(uri))
                return uri.getLastPathSegment();
            return getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }
        return null;
    }

    public static String getDataColumn(Context context, Uri uri, String selection, String[] selectionArgs) {

        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {
                column
        };

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                final int index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is Google Photos.
     */
    public static boolean isGooglePhotosUri(Uri uri) {
        return "com.google.android.apps.photos.content".equals(uri.getAuthority());
    }

    private void draw_grid_pdf_6X2(PdfContentByte canvas, int dirtn, int end_pt) {
        try {
            int x1 = 0, x2 = 0, y1 = 0, y2 = 0;
            if (dirtn == 1)// vertical lines
            {
                y1 = 36;
                y2 = 3235; // y2=3035;3250
            } else ///horizontal lines
            {
                x1 = 36;
                x2 = width - 63;
            }

            for (int i = 36, j = 0, k = 0; i <= end_pt - 5 * 4; i += 5 * 4, j++) //4 is the pixel on canvas
            {
                if (dirtn == 1)//vertical
                {
                    if (j % 5 == 0)//dark pink line
                    {
                        canvas.setColorStroke(new BaseColor(255, 51, 153));
                    } else {
                        canvas.setColorStroke(new BaseColor(255, 185, 220));
                    }
                    canvas.saveState();
                    canvas.moveTo(i, y1);
                    canvas.lineTo(i, y2);
                    canvas.fillStroke();
                    canvas.restoreState();

                    //plots blue lines
                    if (j % 5 == 0) {
                        if (k == 27 || k == 0 || k == 1) {
                            canvas.saveState();
                            canvas.setColorStroke(new BaseColor(21, 64, 234));//blue
                            if (k == 27) {
                                canvas.moveTo(i, y1);
                                canvas.lineTo(i, y2);//5*605,3025
                                canvas.fillStroke();
                                canvas.restoreState();
                            } else {
                                canvas.moveTo(i, y1);
                                canvas.lineTo(i, y2);
                                canvas.fillStroke();
                                canvas.restoreState();
                            }
                        }
                        k++;
                    }
                } else//horizontal
                {
                    if (j % 5 == 0)//dark line
                    {
                        canvas.setColorStroke(new BaseColor(255, 51, 153));
                        if (k == 0)//draws horztal blue lines
                        {
                            canvas.setColorStroke(new BaseColor(21, 64, 234));
                        }
                        k++;
                    } else {
                        canvas.setColorStroke(new BaseColor(255, 185, 220));
                    }
                    canvas.saveState();
                    canvas.moveTo(x1, i);
                    canvas.lineTo(x2, i);
                    canvas.fillStroke();
                    canvas.restoreState();
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void draw_grid_pdf_6X1(PdfContentByte canvas, int dirtn, int end_pt) {
        try {
            int x1 = 0, x2 = 0, y1 = 0, y2 = 0;
            if (dirtn == 1)// vertical lines
            {
                y1 = 36;
                y2 = 3235; // y2=3035;3250
            } else ///horizontal lines
            {
                x1 = 36;
                x2 = width - 63;
            }

            for (int i = 36, j = 0, k = 0; i <= end_pt - 5 * 4; i += 5 * 4, j++) //4 is the pixel on canvas
            {
                if (dirtn == 1)//vertical
                {
                    if (j % 5 == 0)//dark pink line
                    {
                        canvas.setColorStroke(new BaseColor(255, 51, 153));
                    } else {
                        canvas.setColorStroke(new BaseColor(255, 185, 220));
                    }
                    canvas.saveState();
                    canvas.moveTo(i, y1);
                    canvas.lineTo(i, y2);
                    canvas.fillStroke();
                    canvas.restoreState();

                    //plots blue lines
                    if (j % 5 == 0) {
                        if (k == 0 || k == 1) {
                            canvas.saveState();
                            canvas.setColorStroke(new BaseColor(21, 64, 234));//blue
                            if (k == 27) {
                                canvas.moveTo(i, y1);
                                canvas.lineTo(i, y2);//5*605,3025
                                canvas.fillStroke();
                                canvas.restoreState();
                            } else {
                                canvas.moveTo(i, y1);
                                canvas.lineTo(i, y2);
                                canvas.fillStroke();
                                canvas.restoreState();
                            }
                        }
                        k++;
                    }
                } else//horizontal
                {
                    if (j % 5 == 0)//dark line
                    {
                        canvas.setColorStroke(new BaseColor(255, 51, 153));
                        if (k == 0)//draws horztal blue lines
                        {
                            canvas.setColorStroke(new BaseColor(21, 64, 234));
                        }
                        k++;
                    } else {
                        canvas.setColorStroke(new BaseColor(255, 185, 220));
                    }
                    canvas.saveState();
                    canvas.moveTo(x1, i);
                    canvas.lineTo(x2, i);
                    canvas.fillStroke();
                    canvas.restoreState();
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void plot_CAL_pdf_6X2_6X1(PdfContentByte canvas) {

        try {
            int inc = (5 * 47) + 200;//1025
            canvas.setColorStroke(new GrayColor(0.2f));
            canvas.setColorFill(new BaseColor(0, 0, 0));

            for (int i = 0; i < 6; i++) {

                canvas.moveTo(36, inc);
                canvas.lineTo(55, inc);
                canvas.moveTo(55, inc);

                if (MainActivity.iGain == 1 || HelpActivity.iGain == 1) {
                    canvas.lineTo(55, inc + 50); // For iGAIN = 1
                    canvas.moveTo(55, inc + 50); // For iGAIN = 1
                    canvas.lineTo(115, inc + 50); // For iGAIN = 1
                    canvas.moveTo(115, inc + 50); // For iGAIN = 1
                } else if (MainActivity.iGain == 2 || HelpActivity.iGain == 2) {
                    canvas.lineTo(55, inc + 100); // For iGAIN = 2
                    canvas.moveTo(55, inc + 100); // For iGAIN = 2
                    canvas.lineTo(115, inc + 100); // For iGAIN = 2
                    canvas.moveTo(115, inc + 100); // For iGAIN = 2
                } else if (MainActivity.iGain == 3 || HelpActivity.iGain == 3) {
                    canvas.lineTo(55, inc + 150); // For iGAIN = 3
                    canvas.moveTo(55, inc + 150); // For iGAIN = 3
                    canvas.lineTo(115, inc + 150); // For iGAIN = 3
                    canvas.moveTo(115, inc + 150); // For iGAIN = 3
                } else if (MainActivity.iGain == 4 || HelpActivity.iGain == 4) {
                    canvas.lineTo(55, inc + 200); // For iGAIN = 4
                    canvas.moveTo(55, inc + 200); // For iGAIN = 4
                    canvas.lineTo(115, inc + 200); // For iGAIN = 4
                    canvas.moveTo(115, inc + 200); // For iGAIN = 4

                } else if (MainActivity.iGain == 6 || HelpActivity.iGain == 6) {
                    canvas.lineTo(55, inc + 300); // For iGAIN = 6
                    canvas.moveTo(55, inc + 300); // For iGAIN = 6
                    canvas.lineTo(115, inc + 300); // For iGAIN = 6
                    canvas.moveTo(115, inc + 300); // For iGAIN = 6
                } else if (MainActivity.iGain == 8 || HelpActivity.iGain == 8) {
                    canvas.lineTo(55, inc + 400); // For iGAIN = 8
                    canvas.moveTo(55, inc + 400); // For iGAIN = 8
                    canvas.lineTo(115, inc + 400); // For iGAIN = 8
                    canvas.moveTo(115, inc + 400); // For iGAIN = 8
                } else if (MainActivity.iGain == 12 || HelpActivity.iGain == 12) {
                    canvas.lineTo(55, inc + 600); // For iGAIN = 12
                    canvas.moveTo(55, inc + 600); // For iGAIN = 12
                    canvas.lineTo(115, inc + 600); // For iGAIN = 12
                    canvas.moveTo(115, inc + 600); // For iGAIN = 12
                }

                canvas.lineTo(115, inc);
                canvas.moveTo(115, inc);
                canvas.lineTo(135, inc);
                canvas.fillStroke();
                inc += (5 * 100);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Medigraphy for 6x2
    private void plot_data_for_pdf_6X2(PdfContentByte canvas) {
        try {
            float prev_x1, xbase1, current_x1, current_y = 0, ybase, prev_y1;
            int x_offset[] = {135, 2735};
            int y_offset[] = {height - 2937, height - 2437, height - 1937, height - 1437, height - 937, 3420};
            canvas.setColorStroke(new GrayColor(0.2f));

            for (int r = 0; r < 6; r++) {
                for (int c = 0; c < 2; c++) {
                    prev_x1 = x_offset[c];
                    prev_y1 = y_offset[r];
                    for (int d = 0; d < 2600; d++) {
                        switch (c) {
                            case 0:
                                current_y = y_offset[r] - Adjust_y_pixel(paintView.disp_report[lead_arrange[r + c * 6]][d]);
                                current_y = height - current_y;
                                break;
                            case 1:
                                current_y = y_offset[r] - Adjust_y_pixel(paintView.disp_report[lead_arrange[r + c * 6]][d + (2600 * MainActivity.report_sequential)]);
                                current_y = height - current_y;
                                break;
                            default:
                                break;
                        }
                        if (d == 0) {
                            canvas.saveState();
                            canvas.setColorStroke(new GrayColor(0.2f));
                            canvas.setColorFill(new BaseColor(0, 0, 0));
                            canvas.circle(prev_x1, current_y, 1);
                            canvas.fillStroke();
                            canvas.restoreState();
                        } else {
                            canvas.saveState();
                            canvas.setColorStroke(new GrayColor(0.2f));
                            canvas.setColorFill(new BaseColor(0, 0, 0));
                            canvas.moveTo(prev_x1, prev_y1);
                            canvas.lineTo(x_offset[c] + d, current_y);
                            canvas.fillStroke();
                            canvas.restoreState();
                        }

                        prev_x1 = x_offset[c] + d;
                        prev_y1 = current_y;
                    }
                }
            }


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Medigraphy for 6x1
    private void plot_data_for_pdf_6X1_for_2nd_page(PdfContentByte canvas) {
        try {
            float prev_x1, xbase1, current_x1, current_y = 0, ybase, prev_y1;
            int x_offset[] = {135};
            int y_offset[] = {height - 2937, height - 2437, height - 1937, height - 1437, height - 937, 3420};
            canvas.setColorStroke(new GrayColor(0.2f));

            for (int r = 0; r < 6; r++) {
                for (int c = 0; c < 1; c++) {
                    prev_x1 = x_offset[c];
                    prev_y1 = y_offset[r];
                    for (int d = 0; d < paintView.total_samples - 1300; d++) { //For full waveform like Lead II
                        switch (c) {
                            case 0:
                                current_y = y_offset[r] - Adjust_y_pixel(paintView.disp_report[lead_arrange6X1for2ndPage[r + c * 3]][d]);
                                current_y = height - current_y;
                                break;

                            default:
                                break;
                        }
                        if (d == 0) {
                            canvas.saveState();
                            canvas.setColorStroke(new GrayColor(0.2f));
                            canvas.setColorFill(new BaseColor(0, 0, 0));
                            canvas.circle(prev_x1, current_y, 1);
                            canvas.fillStroke();
                            canvas.restoreState();
                        } else {
                            canvas.saveState();
                            canvas.setColorStroke(new GrayColor(0.2f));
                            canvas.setColorFill(new BaseColor(0, 0, 0));
                            canvas.moveTo(prev_x1, prev_y1);
                            canvas.lineTo(x_offset[c] + d, current_y);
                            canvas.fillStroke();
                            canvas.restoreState();
                        }

                        /*
                        // Medigraphy for 6x2
                        private void plot_data_for_pdf(PdfContentByte canvas) {
                            try {
                                float prev_x1, xbase1, current_x1, current_y = 0, ybase, prev_y1;
                                int x_offset[] = {135, 2735};
                                int y_offset[] = {height - 2937, height - 2437, height - 1937, height - 1437, height - 937, 3420};
                                canvas.setColorStroke(new GrayColor(0.2f));

                                for (int r = 0; r < 6; r++) {
                                    for (int c = 0; c < 2; c++) {
                                        prev_x1 = x_offset[c];
                                        prev_y1 = y_offset[r];
                                        for (int d = 0; d < 2600; d++) {
                                            switch (c) {
                                                case 0:
                                                    current_y = y_offset[r] - Adjust_y_pixel(paintView.disp_report[lead_arrange[r + c * 3]][d]);
                                                    current_y = height - current_y;
                                                    break;
                                                case 1:
                                                    current_y = y_offset[r] - Adjust_y_pixel(paintView.disp_report[lead_arrange[r + c * 3]][d + (3900 * MainActivity.report_sequential)]);
                                                    current_y = height - current_y;
                                                    break;
                                                default:
                                                    break;
                                            }
                                            if (d == 0) {
                                                canvas.saveState();
                                                canvas.setColorStroke(new GrayColor(0.2f));
                                                canvas.setColorFill(new BaseColor(0, 0, 0));
                                                canvas.circle(prev_x1, current_y, 1);
                                                canvas.fillStroke();
                                                canvas.restoreState();
                                            } else {
                                                canvas.saveState();
                                                canvas.setColorStroke(new GrayColor(0.2f));
                                                canvas.setColorFill(new BaseColor(0, 0, 0));
                                                canvas.moveTo(prev_x1, prev_y1);
                                                canvas.lineTo(x_offset[c] + d, current_y);
                                                canvas.fillStroke();
                                                canvas.restoreState();
                                            }
                                            */

                        prev_x1 = x_offset[c] + d;
                        prev_y1 = current_y;
                    }
                }
            }

            /*
            //plot full lead II data
            prev_x1 = xbase1 = 135;//5*25;
            prev_y1 = ybase = (3420);//3425,5*685
            paint.setColor(Color.BLACK);
            paint.setStrokeWidth(3f);
            for (int k = 0; k < paintView.total_samples - 1300; k++)//XAXIS_WD=1300
            {
                current_x1 = (float) (xbase1 + (k));
                current_y = (float) (ybase - Adjust_y_pixel(paintView.disp_report[2][k]));
                current_y = height - current_y;

                if (k == 0) {
                    canvas.saveState();
                    canvas.setColorStroke(new GrayColor(0.2f));
                    canvas.setColorFill(new BaseColor(0, 0, 0));
                    canvas.circle(prev_x1, current_y, 1);
                    canvas.fillStroke();
                    canvas.restoreState();
                } else {
                    canvas.saveState();
                    canvas.setColorStroke(new GrayColor(0.2f));
                    canvas.setColorFill(new BaseColor(0, 0, 0));
                    canvas.moveTo(prev_x1, prev_y1);
                    canvas.lineTo(current_x1, current_y);
                    canvas.fillStroke();
                    canvas.restoreState();
                }
                prev_x1 = current_x1;
                prev_y1 = current_y;
            }
*/

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void plot_data_for_pdf_6X1_for_1st_page(PdfContentByte canvas) {
        try {
            float prev_x1, xbase1, current_x1, current_y = 0, ybase, prev_y1;
            int x_offset[] = {135};
            int y_offset[] = {height - 2937, height - 2437, height - 1937, height - 1437, height - 937, 3420};
            canvas.setColorStroke(new GrayColor(0.2f));

            for (int r = 0; r < 6; r++) {
                for (int c = 0; c < 1; c++) {
                    prev_x1 = x_offset[c];
                    prev_y1 = y_offset[r];
                    for (int d = 0; d < paintView.total_samples - 1300; d++) { //For full waveform like Lead II
                        switch (c) {
                            case 0:
                                current_y = y_offset[r] - Adjust_y_pixel(paintView.disp_report[lead_arrange[r + c * 3]][d]);
                                current_y = height - current_y;
                                break;

                            default:
                                break;
                        }
                        if (d == 0) {
                            canvas.saveState();
                            canvas.setColorStroke(new GrayColor(0.2f));
                            canvas.setColorFill(new BaseColor(0, 0, 0));
                            canvas.circle(prev_x1, current_y, 1);
                            canvas.fillStroke();
                            canvas.restoreState();
                        } else {
                            canvas.saveState();
                            canvas.setColorStroke(new GrayColor(0.2f));
                            canvas.setColorFill(new BaseColor(0, 0, 0));
                            canvas.moveTo(prev_x1, prev_y1);
                            canvas.lineTo(x_offset[c] + d, current_y);
                            canvas.fillStroke();
                            canvas.restoreState();
                        }

                        /*
                        // Medigraphy for 6x2
                        private void plot_data_for_pdf(PdfContentByte canvas) {
                            try {
                                float prev_x1, xbase1, current_x1, current_y = 0, ybase, prev_y1;
                                int x_offset[] = {135, 2735};
                                int y_offset[] = {height - 2937, height - 2437, height - 1937, height - 1437, height - 937, 3420};
                                canvas.setColorStroke(new GrayColor(0.2f));

                                for (int r = 0; r < 6; r++) {
                                    for (int c = 0; c < 2; c++) {
                                        prev_x1 = x_offset[c];
                                        prev_y1 = y_offset[r];
                                        for (int d = 0; d < 2600; d++) {
                                            switch (c) {
                                                case 0:
                                                    current_y = y_offset[r] - Adjust_y_pixel(paintView.disp_report[lead_arrange[r + c * 3]][d]);
                                                    current_y = height - current_y;
                                                    break;
                                                case 1:
                                                    current_y = y_offset[r] - Adjust_y_pixel(paintView.disp_report[lead_arrange[r + c * 3]][d + (3900 * MainActivity.report_sequential)]);
                                                    current_y = height - current_y;
                                                    break;
                                                default:
                                                    break;
                                            }
                                            if (d == 0) {
                                                canvas.saveState();
                                                canvas.setColorStroke(new GrayColor(0.2f));
                                                canvas.setColorFill(new BaseColor(0, 0, 0));
                                                canvas.circle(prev_x1, current_y, 1);
                                                canvas.fillStroke();
                                                canvas.restoreState();
                                            } else {
                                                canvas.saveState();
                                                canvas.setColorStroke(new GrayColor(0.2f));
                                                canvas.setColorFill(new BaseColor(0, 0, 0));
                                                canvas.moveTo(prev_x1, prev_y1);
                                                canvas.lineTo(x_offset[c] + d, current_y);
                                                canvas.fillStroke();
                                                canvas.restoreState();
                                            }
                                            */

                        prev_x1 = x_offset[c] + d;
                        prev_y1 = current_y;
                    }
                }
            }

            /*
            //plot full lead II data
            prev_x1 = xbase1 = 135;//5*25;
            prev_y1 = ybase = (3420);//3425,5*685
            paint.setColor(Color.BLACK);
            paint.setStrokeWidth(3f);
            for (int k = 0; k < paintView.total_samples - 1300; k++)//XAXIS_WD=1300
            {
                current_x1 = (float) (xbase1 + (k));
                current_y = (float) (ybase - Adjust_y_pixel(paintView.disp_report[2][k]));
                current_y = height - current_y;

                if (k == 0) {
                    canvas.saveState();
                    canvas.setColorStroke(new GrayColor(0.2f));
                    canvas.setColorFill(new BaseColor(0, 0, 0));
                    canvas.circle(prev_x1, current_y, 1);
                    canvas.fillStroke();
                    canvas.restoreState();
                } else {
                    canvas.saveState();
                    canvas.setColorStroke(new GrayColor(0.2f));
                    canvas.setColorFill(new BaseColor(0, 0, 0));
                    canvas.moveTo(prev_x1, prev_y1);
                    canvas.lineTo(current_x1, current_y);
                    canvas.fillStroke();
                    canvas.restoreState();
                }
                prev_x1 = current_x1;
                prev_y1 = current_y;
            }
*/

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void print_details_pdf_6X1_1st_page(PdfContentByte canvas) {

        hospitalPreference = getContext().getSharedPreferences("HOSPITAL_DETAILS", MODE_PRIVATE);
        hospitalEditor = hospitalPreference.edit();

        hospitalDetailsPreference = getContext().getSharedPreferences("HOSPITAL_DETAILS_ADD", MODE_PRIVATE);
        hospitalDetailsEditor = hospitalDetailsPreference.edit();

        try {
            int h = (height - (5 * 740));
            int p = 0;
            int x, y;
            int istart = 0, iend = 80, y_co_ordinate = height - 99;
            int NO_OF_CHAR = 80, SIZE_PER_CHARACTER = 24;

            //draws the rectangle to print details
            Rectangle patient_detail_rect = new Rectangle(36, 3250, width - 63, height - 36);
            //patient_detail_rect.setBorder(Rectangle.BOX);
            //patient_detail_rect.setBorderWidth(2);
            //canvas.rectangle(patient_detail_rect);
            //draws the rectangle to print details

            if (!hospitalDetailsPreference.getString("DOC_NAME", "").equals("")
                    || !hospitalDetailsPreference.getString("HOSPITAL_NAME", "").equals("")
                    || !hospitalDetailsPreference.getString("HOSPITAL_LOGO", "").equals("")) {

                Rectangle patient_detail_rect2 = new Rectangle(36, 3850 - 300, width - 63, height - 36);
                patient_detail_rect2.setBorder(Rectangle.BOX);
                patient_detail_rect2.setBorderWidth(2);
                canvas.rectangle(patient_detail_rect2);

            } else {
                patient_detail_rect.setBorder(Rectangle.BOX);
                patient_detail_rect.setBorderWidth(2);
                canvas.rectangle(patient_detail_rect);
            }


            //calculating font size
            String text = "Test";//string used for calculating the text size for rectangle
            // try to get max font size that fit in rectangle
            BaseFont base_font = null;
            try {
                base_font = BaseFont.createFont();
            } catch (DocumentException e) {
                e.printStackTrace();
            }
            int textHeightInGlyphSpace = base_font.getAscent(text) - base_font.getDescent(text);
            float fontSize = 75f * patient_detail_rect.getHeight() / textHeightInGlyphSpace;
            float hospitalNamFont = 160f * patient_detail_rect.getHeight() / textHeightInGlyphSpace;
            float DoctNameFont = 110f * patient_detail_rect.getHeight() / textHeightInGlyphSpace;


            //phrase used to set the fontsize for the given text
            Phrase name = new Phrase("Patient Name :", new Font(base_font, fontSize));

            Phrase filter = new Phrase("X=25mm/sec Y=" + MainActivity.selectedGain + "  " + paintView.txt_filter, new Font(base_font, fontSize));
            Phrase title = new Phrase(Report_title, new Font(base_font, fontSize));
            //Phrase txt_comment = new Phrase("Comments :  ", new Font(base_font, fontSize));

            Phrase p_name = new Phrase(paintView.Pass_On_name, new Font(base_font, fontSize));
            BP = new Phrase("Bp  :", new Font(base_font, fontSize));
            p_bp = new Phrase("188/90", new Font(base_font, fontSize));

            comments = new Phrase("Comments :", new Font(base_font, fontSize));
            // Append Years After Age in PDF file

            if (!String.valueOf(strPass_On_age).equals("") && !String.valueOf(strPass_On_age).equals("0")) {
                age = new Phrase("Age :", new Font(base_font, fontSize));
                if (strPass_On_age.equals("infant")) {
                    p_age = new Phrase(strPass_On_age, new Font(base_font, fontSize));
                } else {
                    p_age = new Phrase(strPass_On_age + " Years", new Font(base_font, fontSize));
                }
            }

            if (!paintView.Pass_On_chno.equals("")) {
                chno = new Phrase("Patient form No :", new Font(base_font, fontSize));
                p_chno = new Phrase(paintView.Pass_On_chno, new Font(base_font, fontSize));
            }

            if (!paintView.Pass_On_gen.equals("")) {
                gender = new Phrase("Sex :", new Font(base_font, fontSize));
                p_gen = new Phrase(paintView.Pass_On_gen, new Font(base_font, fontSize));
            }

            if (!paintView.Pass_On_medi.equals("")) {
                medication = new Phrase("Medications :", new Font(base_font, fontSize));
                p_medi = new Phrase(paintView.Pass_On_medi, new Font(base_font, fontSize));
            }

            if (!paintView.Pass_On_BP.equals("")) {
                remark = new Phrase("Remark :", new Font(base_font, fontSize));
                p_remark = new Phrase(paintView.Pass_On_BP, new Font(base_font, fontSize));
            }

            if (!paintView.Pass_On_date.equals("")) {
                date = new Phrase("Date & Time :", new Font(base_font, fontSize));
                p_date = new Phrase(paintView.Pass_On_date, new Font(base_font, fontSize));
            }

            if (!paintView.Pass_On_dob.equals("")) {
                DOB = new Phrase("Date of birth :", new Font(base_font, fontSize));
                p_DOB = new Phrase(paintView.Pass_On_dob, new Font(base_font, fontSize));
            }

            if (!String.valueOf(paintView.Pass_On_ht).equals("") && !String.valueOf(paintView.Pass_On_ht).equals("0")) {
                pheight = new Phrase("Height :", new Font(base_font, fontSize));
                p_height = new Phrase(paintView.Pass_On_ht + " cm", new Font(base_font, fontSize));
            }

            if (!String.valueOf(paintView.Pass_On_wt).equals("") && !String.valueOf(paintView.Pass_On_wt).equals("0")) {
                weight = new Phrase("Weight :", new Font(base_font, fontSize));
                p_weight = new Phrase(paintView.Pass_On_wt + " kg", new Font(base_font, fontSize));
            }

            if (!hospitalDetailsPreference.getString("HOSPITAL_NAME", "").equals("")) {
                hospitalName = new Phrase("Hospital Name :", new Font(base_font, hospitalNamFont));
                p_hospitalName = new Phrase(hospitalDetailsPreference.getString("HOSPITAL_NAME", ""), new Font(base_font, hospitalNamFont));
            }

            if (!hospitalDetailsPreference.getString("DOC_NAME", "").equals("")) {
                docName = new Phrase("Doctor Name :", new Font(base_font, DoctNameFont));
                p_docName = new Phrase(hospitalDetailsPreference.getString("DOC_NAME", ""), new Font(base_font, DoctNameFont));
            }

            //print the patient details

            ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT, name, 500, 3750 - 320, 0);
            ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT, p_name, 900, 3750 - 320, 0);

            // ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT, comments, 500, 3750 - 325, 0);

            ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT, DOB, 1700, 3750 - 320, 0);
            ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT, p_DOB, 2050, 3750 - 320, 0);

            ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT, weight, 1700, 3750 - 395, 0);
            ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT, p_weight, 1930, 3750 - 395, 0);

            ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT, age, 2700, 3750 - 320, 0);
            ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT, p_age, 2850, 3750 - 320, 0);

            ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT, pheight, 2700, 3750 - 395, 0);
            ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT, p_height, 2900, 3750 - 395, 0);

            ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT, chno, 3200, 3750 - 320, 0);
            ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT, p_chno, 3650, 3750 - 320, 0);

            ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT, gender, 3200, 3750 - 395, 0);
            ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT, p_gen, 3350, 3750 - 395, 0);

            ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT, filter, 4000, 3750 - 320, 0);
            ColumnText.showTextAligned(canvas, Element.ALIGN_CENTER, title, 4400, 3750 - 395, 0);

            ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT, date, 500, 3750 - 395, 0);
            ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT, p_date, 880, 3750 - 395, 0);

            //  ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT, BP, 4650, 3750 - 255, 0);
            //  ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT, p_bp, 4800, 3750 - 255, 0);

            ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT, medication, 500, 3750 - 465, 0);
            ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT, p_medi, 850, 3750 - 465, 0);

            ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT, remark, 3000, 3750 - 465, 0);
            ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT, p_remark, 3250, 3750 - 465, 0);

            //ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT, docName, 1500, 3460, 0);
            ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT, p_docName, 500, 3750 - 140, 0);

            //ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT, hospitalName, 1500, 3390, 0);
            ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT, p_hospitalName, 500, 3750 - 50, 0);

            //ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT,txt_comment,2990,3750,0);
            // Medigraphy for 6x1
            x = 5 * 40;
            canvas.setColorFill(BaseColor.BLUE);//sets font color

            for (int i = 0; i < 1; i++) {
                y = ((2800));//5*280,
                for (int j = 0; j < 6; j++) {
                    Phrase leadnames = new Phrase(lead_name[p], new Font(base_font, fontSize));
                    ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT, leadnames, x, y, 0);
                    p++;
                    y = y - (5 * 100);//800/1.5 = 600
                }
                // x = x + 5 * 520; //1350
            }

            /*
            //print lead names
            x = 5 * 40;
            canvas.setColorFill(BaseColor.BLUE);//sets font color

            for (int i = 0; i < 4; i++) {
                y = ((2600));//5*280,
                for (int j = 0; j < 3; j++) {
                    Phrase leadnames = new Phrase(lead_name[p], new Font(base_font, fontSize));
                    ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT, leadnames, x, y, 0);
                    p++;
                    y = y - (5 * 160);//800
                }
                x = x + 5 * 260; //1350
            }

            Phrase lead2 = new Phrase("II", new Font(base_font, fontSize));
            ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT, lead2, 5 * 40, h, 0);

             */

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void print_details_pdf_6X1_2nd_page(PdfContentByte canvas) {

        hospitalPreference = getContext().getSharedPreferences("HOSPITAL_DETAILS", MODE_PRIVATE);
        hospitalEditor = hospitalPreference.edit();

        hospitalDetailsPreference = getContext().getSharedPreferences("HOSPITAL_DETAILS_ADD", MODE_PRIVATE);
        hospitalDetailsEditor = hospitalDetailsPreference.edit();

        try {
            int h = (height - (5 * 740));
            int p = 0;
            int x, y;
            int istart = 0, iend = 80, y_co_ordinate = height - 99;
            int NO_OF_CHAR = 80, SIZE_PER_CHARACTER = 24;

            //draws the rectangle to print details
            Rectangle patient_detail_rect = new Rectangle(36, 3250, width - 63, height - 36);
            //patient_detail_rect.setBorder(Rectangle.BOX);
            //patient_detail_rect.setBorderWidth(2);
            //canvas.rectangle(patient_detail_rect);
            //draws the rectangle to print details

            if (!hospitalDetailsPreference.getString("DOC_NAME", "").equals("")
                    || !hospitalDetailsPreference.getString("HOSPITAL_NAME", "").equals("")
                    || !hospitalDetailsPreference.getString("HOSPITAL_LOGO", "").equals("")) {

                Rectangle patient_detail_rect2 = new Rectangle(36, 3850 - 300, width - 63, height - 36);
                patient_detail_rect2.setBorder(Rectangle.BOX);
                patient_detail_rect2.setBorderWidth(2);
                canvas.rectangle(patient_detail_rect2);

            } else {
                patient_detail_rect.setBorder(Rectangle.BOX);
                patient_detail_rect.setBorderWidth(2);
                canvas.rectangle(patient_detail_rect);
            }


            //calculating font size
            String text = "Test";//string used for calculating the text size for rectangle
            // try to get max font size that fit in rectangle
            BaseFont base_font = null;
            try {
                base_font = BaseFont.createFont();
            } catch (DocumentException e) {
                e.printStackTrace();
            }
            int textHeightInGlyphSpace = base_font.getAscent(text) - base_font.getDescent(text);
            float fontSize = 75f * patient_detail_rect.getHeight() / textHeightInGlyphSpace;
            float hospitalNamFont = 160f * patient_detail_rect.getHeight() / textHeightInGlyphSpace;
            float DoctNameFont = 110f * patient_detail_rect.getHeight() / textHeightInGlyphSpace;


            //phrase used to set the fontsize for the given text
            Phrase name = new Phrase("Patient Name :", new Font(base_font, fontSize));

            Phrase filter = new Phrase("X=25mm/sec Y=" + MainActivity.selectedGain + "  " + paintView.txt_filter, new Font(base_font, fontSize));
            Phrase title = new Phrase(Report_title, new Font(base_font, fontSize));
            //Phrase txt_comment = new Phrase("Comments :  ", new Font(base_font, fontSize));

            Phrase p_name = new Phrase(paintView.Pass_On_name, new Font(base_font, fontSize));
            BP = new Phrase("Bp  :", new Font(base_font, fontSize));
            p_bp = new Phrase("188/90", new Font(base_font, fontSize));

            comments = new Phrase("Comments :", new Font(base_font, fontSize));
            // Append Years After Age in PDF file

            if (!String.valueOf(strPass_On_age).equals("") && !String.valueOf(strPass_On_age).equals("0")) {
                age = new Phrase("Age :", new Font(base_font, fontSize));
                if (strPass_On_age.equals("infant")) {
                    p_age = new Phrase(strPass_On_age, new Font(base_font, fontSize));
                } else {
                    p_age = new Phrase(strPass_On_age + " Years", new Font(base_font, fontSize));
                }
            }

            if (!paintView.Pass_On_chno.equals("")) {
                chno = new Phrase("Patient form No :", new Font(base_font, fontSize));
                p_chno = new Phrase(paintView.Pass_On_chno, new Font(base_font, fontSize));
            }

            if (!paintView.Pass_On_gen.equals("")) {
                gender = new Phrase("Sex :", new Font(base_font, fontSize));
                p_gen = new Phrase(paintView.Pass_On_gen, new Font(base_font, fontSize));
            }

            if (!paintView.Pass_On_medi.equals("")) {
                medication = new Phrase("Medications :", new Font(base_font, fontSize));
                p_medi = new Phrase(paintView.Pass_On_medi, new Font(base_font, fontSize));
            }

            if (!paintView.Pass_On_BP.equals("")) {
                remark = new Phrase("Remark :", new Font(base_font, fontSize));
                p_remark = new Phrase(paintView.Pass_On_BP, new Font(base_font, fontSize));
            }

            if (!paintView.Pass_On_date.equals("")) {
                date = new Phrase("Date & Time :", new Font(base_font, fontSize));
                p_date = new Phrase(paintView.Pass_On_date, new Font(base_font, fontSize));
            }

            if (!paintView.Pass_On_dob.equals("")) {
                DOB = new Phrase("Date of birth :", new Font(base_font, fontSize));
                p_DOB = new Phrase(paintView.Pass_On_dob, new Font(base_font, fontSize));
            }

            if (!String.valueOf(paintView.Pass_On_ht).equals("") && !String.valueOf(paintView.Pass_On_ht).equals("0")) {
                pheight = new Phrase("Height :", new Font(base_font, fontSize));
                p_height = new Phrase(paintView.Pass_On_ht + " cm", new Font(base_font, fontSize));
            }

            if (!String.valueOf(paintView.Pass_On_wt).equals("") && !String.valueOf(paintView.Pass_On_wt).equals("0")) {
                weight = new Phrase("Weight :", new Font(base_font, fontSize));
                p_weight = new Phrase(paintView.Pass_On_wt + " kg", new Font(base_font, fontSize));
            }

            if (!hospitalDetailsPreference.getString("HOSPITAL_NAME", "").equals("")) {
                hospitalName = new Phrase("Hospital Name :", new Font(base_font, hospitalNamFont));
                p_hospitalName = new Phrase(hospitalDetailsPreference.getString("HOSPITAL_NAME", ""), new Font(base_font, hospitalNamFont));
            }

            if (!hospitalDetailsPreference.getString("DOC_NAME", "").equals("")) {
                docName = new Phrase("Doctor Name :", new Font(base_font, DoctNameFont));
                p_docName = new Phrase(hospitalDetailsPreference.getString("DOC_NAME", ""), new Font(base_font, DoctNameFont));
            }

            //print the patient details

            ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT, name, 500, 3750 - 320, 0);
            ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT, p_name, 900, 3750 - 320, 0);

            // ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT, comments, 500, 3750 - 325, 0);

            ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT, DOB, 1700, 3750 - 320, 0);
            ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT, p_DOB, 2050, 3750 - 320, 0);

            ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT, weight, 1700, 3750 - 395, 0);
            ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT, p_weight, 1930, 3750 - 395, 0);

            ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT, age, 2700, 3750 - 320, 0);
            ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT, p_age, 2850, 3750 - 320, 0);

            ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT, pheight, 2700, 3750 - 395, 0);
            ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT, p_height, 2900, 3750 - 395, 0);

            ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT, chno, 3200, 3750 - 320, 0);
            ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT, p_chno, 3650, 3750 - 320, 0);

            ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT, gender, 3200, 3750 - 395, 0);
            ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT, p_gen, 3350, 3750 - 395, 0);

            ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT, filter, 4000, 3750 - 320, 0);
            ColumnText.showTextAligned(canvas, Element.ALIGN_CENTER, title, 4400, 3750 - 395, 0);

            ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT, date, 500, 3750 - 395, 0);
            ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT, p_date, 880, 3750 - 395, 0);

            //  ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT, BP, 4650, 3750 - 255, 0);
            //  ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT, p_bp, 4800, 3750 - 255, 0);

            ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT, medication, 500, 3750 - 465, 0);
            ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT, p_medi, 850, 3750 - 465, 0);

            ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT, remark, 3000, 3750 - 465, 0);
            ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT, p_remark, 3250, 3750 - 465, 0);

            //ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT, docName, 1500, 3460, 0);
            ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT, p_docName, 500, 3750 - 140, 0);

            //ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT, hospitalName, 1500, 3390, 0);
            ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT, p_hospitalName, 500, 3750 - 50, 0);

            //ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT,txt_comment,2990,3750,0);
            // Medigraphy for 6x1
            x = 5 * 40;
            canvas.setColorFill(BaseColor.BLUE);//sets font color

            for (int i = 0; i < 1; i++) {
                y = ((2800));//5*280,
                for (int j = 0; j < 6; j++) {
                    Phrase leadnames = new Phrase(lead_name6X1for2ndPage[p], new Font(base_font, fontSize));
                    ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT, leadnames, x, y, 0);
                    p++;
                    y = y - (5 * 100);//800/1.5 = 600
                }
                // x = x + 5 * 520; //1350
            }

            /*
            //print lead names
            x = 5 * 40;
            canvas.setColorFill(BaseColor.BLUE);//sets font color

            for (int i = 0; i < 4; i++) {
                y = ((2600));//5*280,
                for (int j = 0; j < 3; j++) {
                    Phrase leadnames = new Phrase(lead_name[p], new Font(base_font, fontSize));
                    ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT, leadnames, x, y, 0);
                    p++;
                    y = y - (5 * 160);//800
                }
                x = x + 5 * 260; //1350
            }

            Phrase lead2 = new Phrase("II", new Font(base_font, fontSize));
            ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT, lead2, 5 * 40, h, 0);

             */

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void print_details_pdf_6X2(PdfContentByte canvas) {
        hospitalPreference = getContext().getSharedPreferences("HOSPITAL_DETAILS", MODE_PRIVATE);
        hospitalEditor = hospitalPreference.edit();

        hospitalDetailsPreference = getContext().getSharedPreferences("HOSPITAL_DETAILS_ADD", MODE_PRIVATE);
        hospitalDetailsEditor = hospitalDetailsPreference.edit();

        try {
            int h = (height - (5 * 740));
            int p = 0;
            int x, y;
            int istart = 0, iend = 80, y_co_ordinate = height - 99;
            int NO_OF_CHAR = 80, SIZE_PER_CHARACTER = 24;

            //draws the rectangle to print details
            Rectangle patient_detail_rect = new Rectangle(36, 3250, width - 63, height - 36);
            //patient_detail_rect.setBorder(Rectangle.BOX);
            //patient_detail_rect.setBorderWidth(2);
            //canvas.rectangle(patient_detail_rect);
            //draws the rectangle to print details

            if (!hospitalDetailsPreference.getString("DOC_NAME", "").equals("")
                    || !hospitalDetailsPreference.getString("HOSPITAL_NAME", "").equals("")
                    || !hospitalDetailsPreference.getString("HOSPITAL_LOGO", "").equals("")) {

                Rectangle patient_detail_rect2 = new Rectangle(36, 3850 - 300, width - 63, height - 36);
                patient_detail_rect2.setBorder(Rectangle.BOX);
                patient_detail_rect2.setBorderWidth(2);
                canvas.rectangle(patient_detail_rect2);

            } else {
                patient_detail_rect.setBorder(Rectangle.BOX);
                patient_detail_rect.setBorderWidth(2);
                canvas.rectangle(patient_detail_rect);
            }


            //calculating font size
            String text = "Test";//string used for calculating the text size for rectangle
            // try to get max font size that fit in rectangle
            BaseFont base_font = null;
            try {
                base_font = BaseFont.createFont();
            } catch (DocumentException e) {
                e.printStackTrace();
            }
            int textHeightInGlyphSpace = base_font.getAscent(text) - base_font.getDescent(text);
            float fontSize = 75f * patient_detail_rect.getHeight() / textHeightInGlyphSpace;
            float hospitalNamFont = 160f * patient_detail_rect.getHeight() / textHeightInGlyphSpace;
            float DoctNameFont = 110f * patient_detail_rect.getHeight() / textHeightInGlyphSpace;


            //phrase used to set the fontsize for the given text
            Phrase name = new Phrase("Patient Name :", new Font(base_font, fontSize));

            Phrase filter = new Phrase("X=25mm/sec Y=" + MainActivity.selectedGain + "  " + paintView.txt_filter, new Font(base_font, fontSize));
            Phrase title = new Phrase(Report_title, new Font(base_font, fontSize));
            //Phrase txt_comment = new Phrase("Comments :  ", new Font(base_font, fontSize));

            Phrase p_name = new Phrase(paintView.Pass_On_name, new Font(base_font, fontSize));
            BP = new Phrase("Bp  :", new Font(base_font, fontSize));
            p_bp = new Phrase("188/90", new Font(base_font, fontSize));

            comments = new Phrase("Comments :", new Font(base_font, fontSize));
            // Append Years After Age in PDF file

            if (!String.valueOf(strPass_On_age).equals("") && !String.valueOf(strPass_On_age).equals("0")) {
                age = new Phrase("Age :", new Font(base_font, fontSize));
                if (strPass_On_age.equals("infant")) {
                    p_age = new Phrase(strPass_On_age, new Font(base_font, fontSize));
                } else {
                    p_age = new Phrase(strPass_On_age + " Years", new Font(base_font, fontSize));
                }
            }

            if (!paintView.Pass_On_chno.equals("")) {
                chno = new Phrase("Patient form No :", new Font(base_font, fontSize));
                p_chno = new Phrase(paintView.Pass_On_chno, new Font(base_font, fontSize));
            }

            if (!paintView.Pass_On_gen.equals("")) {
                gender = new Phrase("Sex :", new Font(base_font, fontSize));
                p_gen = new Phrase(paintView.Pass_On_gen, new Font(base_font, fontSize));
            }

            if (!paintView.Pass_On_medi.equals("")) {
                medication = new Phrase("Medications :", new Font(base_font, fontSize));
                p_medi = new Phrase(paintView.Pass_On_medi, new Font(base_font, fontSize));
            }

            if (!paintView.Pass_On_BP.equals("")) {
                remark = new Phrase("Remark :", new Font(base_font, fontSize));
                p_remark = new Phrase(paintView.Pass_On_BP, new Font(base_font, fontSize));
            }

            if (!paintView.Pass_On_date.equals("")) {
                date = new Phrase("Date & Time :", new Font(base_font, fontSize));
                p_date = new Phrase(paintView.Pass_On_date, new Font(base_font, fontSize));
            }

            if (!paintView.Pass_On_dob.equals("")) {
                DOB = new Phrase("Date of birth :", new Font(base_font, fontSize));
                p_DOB = new Phrase(paintView.Pass_On_dob, new Font(base_font, fontSize));
            }

            if (!String.valueOf(paintView.Pass_On_ht).equals("") && !String.valueOf(paintView.Pass_On_ht).equals("0")) {
                pheight = new Phrase("Height :", new Font(base_font, fontSize));
                p_height = new Phrase(paintView.Pass_On_ht + " cm", new Font(base_font, fontSize));
            }

            if (!String.valueOf(paintView.Pass_On_wt).equals("") && !String.valueOf(paintView.Pass_On_wt).equals("0")) {
                weight = new Phrase("Weight :", new Font(base_font, fontSize));
                p_weight = new Phrase(paintView.Pass_On_wt + " kg", new Font(base_font, fontSize));
            }

            if (!hospitalDetailsPreference.getString("HOSPITAL_NAME", "").equals("")) {
                hospitalName = new Phrase("Hospital Name :", new Font(base_font, hospitalNamFont));
                p_hospitalName = new Phrase(hospitalDetailsPreference.getString("HOSPITAL_NAME", ""), new Font(base_font, hospitalNamFont));
            }

            if (!hospitalDetailsPreference.getString("DOC_NAME", "").equals("")) {
                docName = new Phrase("Doctor Name :", new Font(base_font, DoctNameFont));
                p_docName = new Phrase(hospitalDetailsPreference.getString("DOC_NAME", ""), new Font(base_font, DoctNameFont));
            }

            //print the patient details

            ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT, name, 500, 3750 - 320, 0);
            ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT, p_name, 900, 3750 - 320, 0);

            // ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT, comments, 500, 3750 - 325, 0);

            ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT, DOB, 1700, 3750 - 320, 0);
            ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT, p_DOB, 2050, 3750 - 320, 0);

            ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT, weight, 1700, 3750 - 395, 0);
            ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT, p_weight, 1930, 3750 - 395, 0);

            ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT, age, 2700, 3750 - 320, 0);
            ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT, p_age, 2850, 3750 - 320, 0);

            ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT, pheight, 2700, 3750 - 395, 0);
            ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT, p_height, 2900, 3750 - 395, 0);

            ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT, chno, 3200, 3750 - 320, 0);
            ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT, p_chno, 3650, 3750 - 320, 0);

            ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT, gender, 3200, 3750 - 395, 0);
            ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT, p_gen, 3350, 3750 - 395, 0);

            ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT, filter, 4000, 3750 - 320, 0);
            ColumnText.showTextAligned(canvas, Element.ALIGN_CENTER, title, 4400, 3750 - 395, 0);

            ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT, date, 500, 3750 - 395, 0);
            ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT, p_date, 880, 3750 - 395, 0);

            //  ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT, BP, 4650, 3750 - 255, 0);
            //  ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT, p_bp, 4800, 3750 - 255, 0);

            ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT, medication, 500, 3750 - 465, 0);
            ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT, p_medi, 850, 3750 - 465, 0);

            ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT, remark, 3000, 3750 - 465, 0);
            ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT, p_remark, 3250, 3750 - 465, 0);

            //ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT, docName, 1500, 3460, 0);
            ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT, p_docName, 500, 3750 - 140, 0);

            //ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT, hospitalName, 1500, 3390, 0);
            ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT, p_hospitalName, 500, 3750 - 50, 0);

            //ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT,txt_comment,2990,3750,0);

            //print lead names = Change by Dhaval M

            // Medigraphy for 6x2

            x = 5 * 40;
            canvas.setColorFill(BaseColor.BLUE);//sets font color
            for (int i = 0; i < 2; i++) {
                y = ((2800));//5*280,
                for (int j = 0; j < 6; j++) {
                    Phrase leadnames = new Phrase(lead_name[p], new Font(base_font, fontSize));
                    ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT, leadnames, x, y, 0);
                    p++;
                    y = y - (5 * 100);//800/1.5 = 600
                }
                x = x + 5 * 520; //1350
            }

            // Medigraphy for 6x1
/*            x = 5 * 40;
            canvas.setColorFill(BaseColor.BLUE);//sets font color

            for (int i = 0; i < 1; i++) {
                y = ((2800));//5*280,
                for (int j = 0; j < 6; j++) {
                    Phrase leadnames = new Phrase(lead_name[p], new Font(base_font, fontSize));
                    ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT, leadnames, x, y, 0);
                    p++;
                    y = y - (5 * 100);//800/1.5 = 600
                }
                // x = x + 5 * 520; //1350
            }*/
            /*
            //print lead names
            x = 5 * 40;
            canvas.setColorFill(BaseColor.BLUE);//sets font color

            for (int i = 0; i < 4; i++) {
                y = ((2600));//5*280,
                for (int j = 0; j < 3; j++) {
                    Phrase leadnames = new Phrase(lead_name[p], new Font(base_font, fontSize));
                    ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT, leadnames, x, y, 0);
                    p++;
                    y = y - (5 * 160);//800
                }
                x = x + 5 * 260; //1350
            }

            Phrase lead2 = new Phrase("II", new Font(base_font, fontSize));
            ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT, lead2, 5 * 40, h, 0);

             */

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
