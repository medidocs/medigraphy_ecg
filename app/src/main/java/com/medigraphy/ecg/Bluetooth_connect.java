package com.medigraphy.ecg;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import android.Manifest;
import android.app.ActionBar;
import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;

@SuppressLint("HandlerLeak")
public class Bluetooth_connect extends MainActivity implements OnClickListener, OnItemClickListener {
    AlertDialog.Builder builder;
    private static final UUID MY_UUID_SECURE = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    ListView list;
    Button btnScan;
    BluetoothAdapter bAdapter;
    BroadcastReceiver receiver;
    IntentFilter filter;
    ProgressDialog ScanDialog;
    ProgressDialog DataDialog;
    private ArrayAdapter<String> listAdapter;
    ArrayList<BluetoothDevice> devices;
    public static String device_address, deviceName;
    public static BluetoothDevice selectedDevice;
    private static final int REQUEST_ENABLE_BT = 0;
    protected static final int SUCCESS_CONNECT = 0;
    protected static final int SEND_INSTREAM = 1;
    protected static final int SEND_OUTSTREAM = 2;
    protected static final int CONNECTION_ERROR_GENERAL = 3;
    protected static final int MESSAGE_COMMUNICATION = 4;
    protected static final int CONNECTION_ERROR_DEVICE = 6;
    private static int DEVICE_FOUND = 0;
    //private String sDeviceAddress;

    int BLUETOOTH_ADD_LENGTH = 17;
    InputStream instm;
    OutputStream outstm;
    private boolean REGISTER = false;
    boolean deviceselected = false;
    BluetoothDevice device1;
    boolean bFileRead = false;
    SharedPreferences hospitalPreference;
    SharedPreferences.Editor hospitalEditor;
    SharedPreferences reportPreference;
    SharedPreferences.Editor reportEditor;
    JsonArray reportDetailsList;
    boolean isChecked = false;

    SharedPreferences preferences;
    SharedPreferences.Editor editor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_connect);
        Constant_Class.context = Bluetooth_connect.this;

        hospitalPreference = this.getSharedPreferences("HOSPITAL_DETAILS", MODE_PRIVATE);
        hospitalEditor = hospitalPreference.edit();

        reportPreference = this.getSharedPreferences("REPORT_DETAILS", MODE_PRIVATE);
        reportEditor = reportPreference.edit();

        preferences = getSharedPreferences("LOGIN_PREF", MODE_PRIVATE);
        editor = preferences.edit();

  /*      reportDetailsList = new JsonArray();
        JsonParser jsonParser = new JsonParser();

        if (!reportPreference.getString("Report", "").equals("")) {
            reportDetailsList = (JsonArray) jsonParser.parse(reportPreference.getString("Report", ""));
        }

        if (reportPreference.getString("Report", "").equals("")) {

            JsonObject reportSimultaneous = new JsonObject();
            JsonObject reportSequential = new JsonObject();
            JsonObject report62Report = new JsonObject();
            JsonObject report61Report = new JsonObject();
            reportJsonArray= new JsonArray();

            reportSimultaneous.addProperty("reportName","Simultaneous");
            reportSimultaneous.addProperty("position","0");
            reportSimultaneous.addProperty("isChecked","true");

            reportSequential.addProperty("reportName","Sequential");
            reportSequential.addProperty("position","1");
            reportSequential.addProperty("isChecked","true");

            report62Report.addProperty("reportName","62Report");
            report62Report.addProperty("position","2");
            report62Report.addProperty("isChecked","true");

            report61Report.addProperty("reportName","61Report");
            report61Report.addProperty("position","3");
            report61Report.addProperty("isChecked","true");

            reportJsonArray.add(reportSimultaneous);
            reportJsonArray.add(reportSequential);
            reportJsonArray.add(report62Report);
            reportJsonArray.add(report61Report);

            reportEditor.putString("Report",reportJsonArray.toString());
            reportEditor.apply();

        }*/
  /*      if (isFirst) {
            selectedReport.add(new ReportName("Simultaneous", "0"));
            selectedReport.add(new ReportName("Sequential", "1"));
            selectedReport.add(new ReportName("62Report", "2"));
            selectedReport.add(new ReportName("61Report", "3"));
        }*/

        int actionBarTitleId = Resources.getSystem().getIdentifier("action_bar_title", "id", "android");
        if (actionBarTitleId > 0) {
            TextView title = (TextView) findViewById(actionBarTitleId);
            if (title != null)
                title.setTextColor(Color.WHITE);

        }
        Log.i(TAG, "entering OnCreate of Bluetooth \\n");
        //initialization of GUI

        builder = new AlertDialog.Builder(this);
        list = (ListView) findViewById(R.id.list);
        btnScan = (Button) findViewById(R.id.btnscan);
        listAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, 0);
        devices = new ArrayList<BluetoothDevice>();
        list.setAdapter(listAdapter);
        list.setOnItemClickListener(this);
        btnScan.setOnClickListener(this);
        bAdapter = BluetoothAdapter.getDefaultAdapter();
        setTitle("ECG Device Connect");

        ActionBar ab = getActionBar();
        ab.setDisplayUseLogoEnabled(false);
        ab.setDisplayShowHomeEnabled(false);
        ab.setBackgroundDrawable(getResources().getDrawable(R.drawable.action_bar_background_color));

        ab.show();

        ScanDialog = new ProgressDialog(this); // this = YourActivity
        ScanDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        ScanDialog.setTitle("Searching");
        ScanDialog.setMessage("Searching for nearby ECG devices...");
        ScanDialog.setIndeterminate(true);
        ScanDialog.setCanceledOnTouchOutside(false);

        DataDialog = new ProgressDialog(this); // this = YourActivity
        DataDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        DataDialog.setTitle("Connecting");
        DataDialog.setMessage("Please wait for device to connect...");
        DataDialog.setIndeterminate(true);
        DataDialog.setCanceledOnTouchOutside(false);

        checkBluetoothStatus();

        if (bFileRead == false)
            bFileRead = read_bluetooth_address();    //this will read the file and save address in "device_address" else will be blank.
        Log.i(TAG, "Done with OnCreate of Bluetooth \\n");
    }

    //function to check initial Bluetooth status
    public void checkBluetoothStatus() {
        try {
            if (bAdapter == null)
                Toast.makeText(getApplicationContext(), "NO BLUETOOTH DEVICE DETECTED!", Toast.LENGTH_SHORT).show();

            if (bAdapter.isEnabled()) {
                System.out.println("checkBluetoothStatus");
                doDiscovery();
                init();
            } else {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        } catch (Exception e) {
            System.out.println("bt status>" + e.getMessage());
        }
    }

    private void init() {
        if (Build.VERSION.RELEASE.equals("12")) {
            if (ActivityCompat.checkSelfPermission(Bluetooth_connect.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(Bluetooth_connect.this, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 401);
                return;
            }
        }
        try {
            filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            receiver = new BroadcastReceiver() {
                @SuppressWarnings("static-access")
                @Override
                public void onReceive(Context arg0, Intent intent) {
                    String action = intent.getAction();
                    String deviceNameRecieved;
                    //	read_bluetooth_address();//Moved to onCreate
                    if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                        System.out.println("actio>>>n" + action);
                        setProgressBarVisibility(false);

                        device1 = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                        System.out.println(device1);


                        deviceNameRecieved = device1.getName();

                        if (listAdapter.isEmpty()) {
                            System.out.println("empty");
                            //Populate only those devices which are ECG Recorders
                            try {
                                if (deviceNameRecieved != null && deviceNameRecieved.startsWith("ECGREC-") || deviceNameRecieved.startsWith("ECGDEC-")) {
                                    //if device is found then flag DEVICE_FOUND is set 1
                                    System.out.println("deviceNameRecieved" + deviceNameRecieved);
                                    DEVICE_FOUND = 1;
                                    deviceName = deviceNameRecieved;

                                    String device = device1.getAddress();

                                    editor.putString("Connected_Device", deviceName);
                                    editor.apply();

                                    Log.i("device_address", device_address);
//									if(device_address.equals("") || device_address.equals(null))// there is no entry in the bluetooth file, so add the device to list.
//									{
//										System.out.println("deviceName"+deviceName);
//										listAdapter.add(deviceName);
//									}
                                    if (device1.getAddress().toString().equals(device_address)) {
                                        bAdapter.cancelDiscovery();
                                        ScanDialog.hide();

                                        String demo = "ECGREC-DEMO201020";

                                        if (deviceNameRecieved.startsWith("ECGREC-DEMO") || deviceNameRecieved.startsWith("ECGDEC-DEMO")) {

                                            String inputDate = deviceNameRecieved.replaceAll("[^0-9]", "");

                                            SimpleDateFormat inputdate = new SimpleDateFormat("yyMMdd");
                                            SimpleDateFormat outputFormat = new SimpleDateFormat("dd/MM/yyyy");
                                            Date outDate = inputdate.parse(inputDate);
                                            String outputText = outputFormat.format(outDate);
                                            String currentDate = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date());

                                            Date current = outputFormat.parse(currentDate);
                                            Date outPut = outputFormat.parse(outputText);

                                            if (outPut.equals(current) || outPut.compareTo(current) < 0) {

                                                builder.setMessage("Please return the Demo piece to the company").setCancelable(false);
                                                builder.setTitle("Alert!");
                                                builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                                    @Override
                                                    public void onClick(DialogInterface dialogInterface, int i) {
                                                        dialogInterface.dismiss();
                                                    }
                                                });

                                                AlertDialog alert = builder.create();
                                                alert.show();

                                            } else if (current.compareTo(outDate) < 0) {

                                                long remainingTime = outDate.getTime() - current.getTime();
                                                long days = remainingTime / (24 * 60 * 60 * 1000);

                                                builder.setMessage("You have " + days + " days left for demo ").setCancelable(false);
                                                builder.setTitle("Alert!");

                                                builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                                    @Override
                                                    public void onClick(DialogInterface dialogInterface, int i) {
                                                        ConnectThread connect = new ConnectThread(device1);
                                                        connect.start();
                                                    }
                                                });
                                                AlertDialog alert = builder.create();
                                                alert.show();
                                            }
                                        } else {
                                            ConnectThread connect = new ConnectThread(device1);
                                            connect.start();
                                        }
                                    }

                                    devices.add(device1);
                                    if (deviceName.startsWith("ECGDEC-")) {
                                        List strDeviceName = Arrays.asList(deviceName.split("-"));
                                        String name = strDeviceName.get(1).toString();
                                        listAdapter.add(name);
                                        ScanDialog.hide();
                                    } else {
                                        listAdapter.add(deviceName);
                                        ScanDialog.hide();
                                    }

                                } else {
                                    //do nothing.. continue with search
                                }
                            } catch (Exception e) {
                                System.out.println("error in bluetooth init" + e.getMessage());

                            }

                        } else {
                            Log.e("list_count", String.valueOf(list.getCount()));
                            for (int a = 0; a < list.getCount(); a++) {
                                //if not exist in list then add in list
                                if (deviceNameRecieved != null) {
                                    if (deviceNameRecieved.startsWith("ECGREC-") || deviceNameRecieved.startsWith("ECGDEC-") && !devices.contains(deviceNameRecieved)) //Make sure only one ECG machine is in at a time. If two devices are present their name has to be different.
                                    {
                                        deviceName = deviceNameRecieved;

                                        editor.putString("Connected_Device", deviceName);
                                        editor.apply();

                                        if (device1.getAddress().toString().equals(device_address) && a == 0)    //a=0 in list is the last element added... List is Last IN First Out
                                        {
                                            bAdapter.cancelDiscovery();
                                            ScanDialog.hide();

                                            String demo = "ECGREC-DEMO201020";

                                            if (deviceNameRecieved.startsWith("ECGREC-DEMO") || deviceNameRecieved.startsWith("ECGDEC-DEMO")) {

                                                String inputDate = deviceNameRecieved.replaceAll("[^0-9]", "");

                                                SimpleDateFormat inputdate = new SimpleDateFormat("yyMMdd");
                                                SimpleDateFormat outputFormat = new SimpleDateFormat("dd/MM/yyyy");
                                                Date outDate = null;
                                                try {
                                                    outDate = inputdate.parse(inputDate);
                                                } catch (ParseException e) {
                                                    e.printStackTrace();
                                                }
                                                String outputText = outputFormat.format(outDate);
                                                String currentDate = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date());

                                                Date current = null;
                                                Date outPut = null;
                                                try {
                                                    current = outputFormat.parse(currentDate);
                                                    outPut = outputFormat.parse(outputText);
                                                } catch (ParseException e) {
                                                    e.printStackTrace();
                                                }

                                                if (outPut.equals(current) || outPut.compareTo(current) < 0) {

                                                    builder.setMessage("Please return the Demo piece to the company").setCancelable(false);
                                                    builder.setTitle("Alert!");
                                                    builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                                        @Override
                                                        public void onClick(DialogInterface dialogInterface, int i) {
                                                            dialogInterface.dismiss();
                                                        }
                                                    });

                                                    AlertDialog alert = builder.create();
                                                    alert.show();

                                                } else if (current.compareTo(outDate) < 0) {

                                                    long remainingTime = outDate.getTime() - current.getTime();
                                                    long days = remainingTime / (24 * 60 * 60 * 1000);

                                                    builder.setMessage("You have " + days + " days left for demo ").setCancelable(false);
                                                    builder.setTitle("Alert!");

                                                    builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                                        @Override
                                                        public void onClick(DialogInterface dialogInterface, int i) {
                                                            ConnectThread connect = new ConnectThread(device1);
                                                            connect.start();
                                                        }
                                                    });
                                                    AlertDialog alert = builder.create();

                                                    alert.show();
                                                }
                                            } else {

                                                ConnectThread connect = new ConnectThread(device1);
                                                connect.start();

                                            }

                                        } else {

                                            if (deviceName.startsWith("ECGDEC-")) {
                                                List strDeviceName = Arrays.asList(deviceName.split("-"));
                                                String name = strDeviceName.get(1).toString();
                                                listAdapter.add(name);
                                                ScanDialog.hide();
                                            } else {
                                                listAdapter.add(deviceName);
                                                ScanDialog.hide();
                                            }
                                        }
                                        devices.add(device1);
                                        System.out.println("add device");
                                        break;
                                    }
                                }
                            }
                        }
                        // devices.add(device1);
                        //setTitleColor(Color.BLACK);
                        //setTitle("Select ECGRec Device...");

                    } else if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                        ScanDialog.show();
                    } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                        ScanDialog.hide();

                        if (DEVICE_FOUND == 0) {

                            Toast.makeText(getApplicationContext(), "No ECG Recorders Found. Check If the Device is Off.", Toast.LENGTH_LONG).show();
                        }
                    } else if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                        ScanDialog.hide();
                        if (bAdapter.getState() == bAdapter.STATE_OFF) {
                            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                            doDiscovery();
                            init();
                        }
                    }
                }
            };

            registerReceiver(receiver, filter);

            filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
            registerReceiver(receiver, filter);

            filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
            registerReceiver(receiver, filter);

            filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
            registerReceiver(receiver, filter);

            REGISTER = true;

        } catch (Exception e) {
            System.out.println("error in bluetooth init" + e.getMessage());
            Toast.makeText(getApplicationContext(), "Bluetooth init error!", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (REGISTER) {
            unregisterReceiver(receiver);
            REGISTER = false;
        }
    }

    private boolean read_bluetooth_address() {
        File f = new File(settingsFolder, "bluetooth_address.bin");
        InputStream fin = null;
        char[] array = new char[100];
        int i = 0;

        try {
            Log.i(TAG, "Entering read_bluetooth_address() \\n");
            device_address = "";
            if (!f.exists()) {
                System.out.println("No previous Bluetooth connection to ECGRec device found!");
                return true;
            } else {
                fin = new FileInputStream(f);
                while (fin.available() > 0) {
                    array[i] = (char) fin.read();
                    i++;
                }
                if (i > 0) {
                    device_address = String.valueOf(array, 0, BLUETOOTH_ADD_LENGTH);
                    System.out.println("device_address read=" + device_address);
                }
                fin.close();
            }
            Log.i(TAG, "device_address found: " + device_address + "\\n");
        } catch (IOException ex) {
            System.out.println("func read_bluetooth_address =" + ex.getMessage());
        }
        return true;
    }


    private void doDiscovery() {
        ScanDialog.show();
        // If we're already discovering, stop it
        if (bAdapter.isDiscovering()) {
            bAdapter.cancelDiscovery();
        }
        System.out.println("doDiscovery");
        // Request discover from BluetoothAdapter
        bAdapter.startDiscovery();
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    // Bluetooth is now enabled, so set device discovery
                    doDiscovery();
                    init();
                    System.out.println("REQUEST_ENABLE_BT");
                } else {
                    // User did not enable Bluetooth or an error occurred
                    System.out.println("bt should be on");
                    Toast.makeText(this, "BLUETOOTH SHOULD BE ENABLED",
                            Toast.LENGTH_LONG).show();
                    System.exit(0);
                }
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
        // load_menu = menu.getItem(9);
        //new_patient = menu.getItem(10);
        // prev_lead = menu.getItem(11);
        //next_lead = menu.getItem(12);
        // repeat_acq = menu.getItem(13);

        battery.setVisible(true);
        battery.setTitle("Battery: " + MainActivity.battery_status);
        battery.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

        filter_menu.setVisible(true);
        mode_menu.setVisible(true);
        gain_menu.setVisible(true);
        // load_menu.setVisible(true);
        prev_menu.setVisible(false);
        next_menu.setVisible(false);
        //  new_patient.setVisible(false);
        // next_lead.setVisible(false);
        // prev_lead.setVisible(false);
        //  repeat_acq.setVisible(false);

        //hides the menu during connectivity
        if (mState == "HIDE_MENU") {
            for (int j = 0; j < menu.size(); j++)
                menu.getItem(j).setVisible(false);

        }
        if (mState == "UNHINDE_MENU") {
            //demo_menu.setVisible(false);
        }

        if (no_of_lead_to_display == 12)
            acq_mode.setVisible(false);

        MainActivity.read_filter_gain();
        return true;
    }

    @Override
    public void onItemClick(AdapterView<?> arg0, View v, int arg2, long arg3) {
        System.out.println("on item click");
        connect_device(arg2);

    }

    //this function is used for pairing with the device
    public void connect_device(int arg2) {
        deviceselected = true;
        if (bAdapter.isDiscovering())
            bAdapter.cancelDiscovery();

        try {
            selectedDevice = devices.get(arg2);
            if (!BluetoothAdapter.checkBluetoothAddress(devices.get(arg2).getAddress())) {
                System.out.println("BT not valid");
                Toast.makeText(getApplicationContext(), "Not a valid Bluetooth Address..Select another bluetooth device OR Configure again!!", Toast.LENGTH_SHORT).show();
            } else {
                System.out.println("selectedDevice.getName()" + selectedDevice.getName());
                System.out.println("bAdapter.getBondedDevices()" + bAdapter.getBondedDevices());
                if (bAdapter.getBondedDevices().contains(selectedDevice.getName()))//device_address))//device is already paired, directly start the thread.
                {
                    System.out.println("paired device available, directly connect to device");
                    String demo = selectedDevice.getName();

                    if (demo.startsWith("ECGREC-DEMO") || demo.startsWith("ECGDEC-DEMO")) {

                        String inputDate = demo.replaceAll("[^0-9]", "");

                        SimpleDateFormat inputdate = new SimpleDateFormat("yyMMdd");
                        SimpleDateFormat outputFormat = new SimpleDateFormat("dd/MM/yyyy");
                        Date outDate = null;
                        try {
                            outDate = inputdate.parse(inputDate);
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }

                        String outputText = outputFormat.format(outDate);
                        String currentDate = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date());

                        Date current = null;
                        Date outPut = null;
                        try {
                            current = outputFormat.parse(currentDate);
                            outPut = outputFormat.parse(outputText);
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }

                        if (outPut.equals(current) || outPut.compareTo(current) < 0) {

                            builder.setMessage("Please return the Demo piece to the company").setCancelable(false);
                            builder.setTitle("Alert!");
                            builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    dialogInterface.dismiss();
                                }
                            });

                            AlertDialog alert = builder.create();
                            alert.show();

                        } else if (current.compareTo(outDate) < 0) {

                            long remainingTime = outDate.getTime() - current.getTime();
                            long days = remainingTime / (24 * 60 * 60 * 1000);

                            builder.setMessage("You have " + days + " days left for demo ").setCancelable(false);
                            builder.setTitle("Alert!");

                            builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    ConnectThread connect = new ConnectThread(selectedDevice);
                                    connect.start();
                                }
                            });
                            AlertDialog alert = builder.create();
                            alert.show();
                        }
                    } else {
                        ConnectThread connect = new ConnectThread(selectedDevice);
                        connect.start();
                    }

                } else {
                    System.out.println("call pair device");
                    pairDevice(selectedDevice);
                }
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    public final Handler Bt_mHandler = new Handler() {
        public void handleMessage(Message msg) {
            battery.setTitle("Battery: " + MainActivity.battery_status);

            switch (msg.what) {

                case SUCCESS_CONNECT:
                    try {
                        System.out.println("Device Address to Store: " + device_address);
                        store_bluetooth_address(device_address);
                        DataDialog.hide();
                        ScanDialog.dismiss();

                        if (deviceName.startsWith("ECGDEC-")) {
                            List strDeviceName = Arrays.asList(deviceName.split("-"));
                            String name = strDeviceName.get(1).toString();
                            Toast.makeText(getApplicationContext(), "Successfully Connected to " + name, Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(getApplicationContext(), "Successfully Connected to " + deviceName, Toast.LENGTH_LONG).show();
                        }

                        System.out.println("SUCCESS_CONNECT");
                        ConnectedThread connectedThread = new ConnectedThread((BluetoothSocket) msg.obj);
                        connectedThread.start();
                    } catch (Exception e1) {
                        Toast.makeText(getApplicationContext(), "Error after connecting to Device through bluetooth", Toast.LENGTH_SHORT).show();
                        System.out.println("Error161=" + e1.getMessage());
                    }
                    battery.setTitle("Battery: " + MainActivity.battery_status);

                    break;
                case SEND_INSTREAM:
                    instrm = (InputStream) msg.obj;
                    System.out.println("SEND_INSTREAM=" + instrm.toString());
                    battery.setTitle("Battery: " + MainActivity.battery_status);

                    break;
                case SEND_OUTSTREAM:
                    outstrm = (OutputStream) msg.obj;
                    System.out.println("SEND_OUTSTREAM=" + outstrm.toString());
                    mState = "UNHINDE_MENU";
                    //supportInvalidateOptionsMenu();//AK: for APK 8 and below

                    invalidateOptionsMenu(); //currently set to 11
                    initialization_paintView();


                    findViewById(R.id.gen_start).setVisibility(View.GONE);
                    findViewById(R.id.start_button).setVisibility(View.VISIBLE);
                    findViewById(R.id.generate_button).setVisibility(View.GONE);

                    bt_start = (Button) findViewById(R.id.start_button);
                    bt_start.setOnClickListener(Bluetooth_connect.this);
                    bt_start_2 = (Button) findViewById(R.id.start_2);
                    bt_start_2.setOnClickListener(Bluetooth_connect.this);

                    bt_gen = (Button) findViewById(R.id.generate_2);
                    bt_gen.setOnClickListener(Bluetooth_connect.this);
                    battery.setTitle("Battery: " + MainActivity.battery_status);

                    //call the method from main activity for creating canvas
                    break;
                case CONNECTION_ERROR_DEVICE:
                    //Hide this Error Message when Device Getting Error in Connection
//                    Toast.makeText(getApplicationContext(), "Bluetooth connection to ECG Recorder failed.", Toast.LENGTH_SHORT).show();
                    break;
                case CONNECTION_ERROR_GENERAL:
                    Toast.makeText(getApplicationContext(), "Selected Device is not a ECG Recorder.", Toast.LENGTH_SHORT).show();
                    break;

                case MESSAGE_COMMUNICATION:
                    AlertDialog.Builder build2 = new AlertDialog.Builder(Bluetooth_connect.this);
                    build2.setTitle("Communication Error");
                    build2.setMessage("Communication Error! \n Try Again!");
                    build2.setCancelable(false);
                    build2.setPositiveButton("OK",
                            new DialogInterface.OnClickListener() {

                                @Override
                                public void onClick(DialogInterface dialog, int arg1) {
                                    System.exit(1);
                                }
                            });
                    AlertDialog alert2 = build2.create();
                    alert2.show();
                    battery.setTitle("Battery: " + MainActivity.battery_status);
                    break;
            }
        }
    };

    //displays the pairing window for connectivity
    public void pairDevice(final BluetoothDevice device) {
        try {
            bAdapter.cancelDiscovery();
            Method m = device.getClass().getMethod("createBond", (Class[]) null);
            m.invoke(device, (Object[]) null);
            device_address = device.getAddress().toString();
            System.out.println("pairing device");

            String demo = device.getName();
            Log.e("===>>DEMO", "pairDevice: " + demo);

            if (demo.startsWith("ECGREC-DEMO") || demo.startsWith("ECGDEC-DEMO")) {

                String inputDate = demo.replaceAll("[^0-9]", "");

                SimpleDateFormat inputdate = new SimpleDateFormat("yyMMdd");
                SimpleDateFormat outputFormat = new SimpleDateFormat("dd/MM/yyyy");
                Date outDate = null;
                try {
                    outDate = inputdate.parse(inputDate);
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                String outputText = outputFormat.format(outDate);
                String currentDate = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(new Date());

                Date current = null;
                Date outPut = null;
                try {
                    current = outputFormat.parse(currentDate);
                    outPut = outputFormat.parse(outputText);
                } catch (ParseException e) {
                    e.printStackTrace();
                }

                if (outPut.equals(current) || outPut.compareTo(current) < 0) {

                    builder.setMessage("Please return the Demo piece to the company").setCancelable(false);
                    builder.setTitle("Alert!");
                    builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.dismiss();
                        }
                    });

                    AlertDialog alert = builder.create();
                    alert.show();

                } else if (current.compareTo(outDate) < 0) {

                    long remainingTime = outDate.getTime() - current.getTime();
                    long days = remainingTime / (24 * 60 * 60 * 1000);

                    builder.setMessage("You have " + days + " days left for demo ").setCancelable(false);
                    builder.setTitle("Alert!");

                    builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            ConnectThread connect = new ConnectThread(device);
                            connect.start();
                        }
                    });

                    AlertDialog alert = builder.create();
                    alert.show();

                }
            } else {
                ConnectThread connect = new ConnectThread(device);
                connect.start();
            }

        } catch (Exception e) {
            System.out.println("ERROR PAIR DEVICE " + e.getMessage());
        }
    }

    //store the mac address of the device on pairing for first time and then connects automatically when found for next time
    private void store_bluetooth_address(String bt_address) {
        File f = new File(settingsFolder, "bluetooth_address.bin");
        OutputStream osw = null;
        try {
            if (!f.exists()) {
                f.createNewFile();
            }
            osw = new FileOutputStream(f);
            osw.write(bt_address.getBytes());
            System.out.println("Bytes Written:" + bt_address.getBytes().length);
            osw.close();
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
    }

    // unpair the device
    public void unpairDevice(BluetoothDevice device) {
        try {
            Method m = device.getClass().getMethod("removeBond", (Class[]) null);
            m.invoke(device, (Object[]) null);
        } catch (Exception e) {
            System.out.println("Error6>" + e.getMessage());
        }
    }

/*
    public void onCheckboxClicked(View view) {

        boolean checked = ((CheckBox) view).isChecked();

        switch (view.getId()) {

            case R.id.chkSimultaneous:

                if (checked) {
                    selectedReport.add(new ReportName("Simultaneous", "0"));
                } else {

                    for (int i = 0; i < selectedReport.size(); i++) {
                        if (selectedReport.get(i).getReportName().equals("Simultaneous")) {
                            selectedReport.remove(selectedReport.get(i));
                        }
                    }
                }

                break;

            case R.id.chkSequential:

                if (checked) {
                    selectedReport.add(new ReportName("Sequential", "1"));

                } else {
                    for (int i = 0; i < selectedReport.size(); i++) {
                        if (selectedReport.get(i).getReportName().equals("Sequential")) {
                            selectedReport.remove(selectedReport.get(i));
                        }
                    }
                }

                break;

            case R.id.chk62Report:

                if (checked) {
                    selectedReport.add(new ReportName("62Report", "2"));

                } else {
                    for (int i = 0; i < selectedReport.size(); i++) {
                        if (selectedReport.get(i).getReportName().equals("62Report")) {
                            selectedReport.remove(selectedReport.get(i));
                        }
                    }

                }
                break;

            case R.id.chk61Report:

                if (checked) {
                    selectedReport.add(new ReportName("61Report", "3"));
                } else {
                    for (int i = 0; i < selectedReport.size(); i++) {
                        if (selectedReport.get(i).getReportName().equals("61Report")) {
                            selectedReport.remove(selectedReport.get(i));
                        }
                    }
                }

                break;

        }
    }*/

    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;

        public ConnectThread(BluetoothDevice device) {
            // Use a temporary object that is later assigned to mmSocket, because mmSocket is final
            BluetoothSocket tmp = null;
            try {
                // MY_UUID is the app's UUID string, also used by the server code
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID_SECURE);
                String deviceName = device.getName();
                System.out.println("ConnectThread");
            } catch (Exception e) {
                Toast.makeText(getApplicationContext(), "Socket error: Constructor error", Toast.LENGTH_SHORT).show();
                System.out.println("ERR!=" + e.getMessage());
            }
            mmSocket = tmp;
        }

        public void run() {
            // Cancel discovery because it will slow down the connection
            System.out.println("entered run thread, before cancelDiscovery");
            bAdapter.cancelDiscovery();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    ScanDialog.dismiss();
                    DataDialog.show();
                }
            });

            try {
                // Connect the device through the socket. This will block
                // until it succeeds or throws an exception

                System.out.println("run ConnectThread");
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {

                    e.printStackTrace();
                }
                mmSocket.connect();

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        DataDialog.hide();
                    }
                });

            } catch (IOException connectException) {

                runOnUiThread(new Runnable() {

                    @Override
                    public void run() {
                        DataDialog.hide();
                    }
                });

                //Toast.makeText(getApplicationContext(), "Socket error: run() IOException", Toast.LENGTH_SHORT).show();
                System.out.println("IO Exception in run()");
                // Unable to connect; close the socket and get out
                Log.e("Dhaval APP", "I got an error", connectException);

                Log.e("DHAVAL APP", "STACKTRACE");
                Log.e("DHAVAL APP", Log.getStackTraceString(connectException));
                try {
                    mmSocket.close();
                    this.cancel();    //Thread is canceled/closed along with the socket... RKJ
                    System.out.println("Not a ECG Recorder run()");
                    Message msg1 = Bt_mHandler.obtainMessage(CONNECTION_ERROR_DEVICE);
                    Bt_mHandler.sendMessage(msg1);
                } catch (Exception closeException) {
                    Toast.makeText(getApplicationContext(), "Socket error: run() socket close", Toast.LENGTH_SHORT).show();
                    System.out.println("Socket close error in run ERR3=" + closeException.getMessage());
                    Message msg2 = Bt_mHandler.obtainMessage(CONNECTION_ERROR_GENERAL);
                    Bt_mHandler.sendMessage(msg2);
                }

                return;
            }
            // Do work to manage the connection (in a separate thread)
            Message msg = Bt_mHandler.obtainMessage(SUCCESS_CONNECT, mmSocket);
            Bt_mHandler.sendMessage(msg);
        }

        /**
         * Will cancel an in-progress connection, and close the socket
         */

        public void cancel() {
            try {
                mmSocket.close();
                // Stop this Line because it is look like going to infinite loop
//                this.cancel();        //Thread is canceled/closed along with the socket... RKJ
            } catch (IOException e) {
            }
        }
    }

    //used for scanning the devices on button click(scan button)
    //@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onClick(View v) {

        battery.setTitle("Battery: " + MainActivity.battery_status);
        myVib.vibrate(50);

        if (v.getId() == R.id.generate_2) {
            JsonParser jsonParser = new JsonParser();
            reportDetailsList = (JsonArray) jsonParser.parse(reportPreference.getString("Report", ""));

            try {
                for (int k = 0; k < reportDetailsList.size(); k++) {
                    if (reportDetailsList.get(k).getAsJsonObject().get("isChecked").getAsString().equals("true")) {
                        isChecked = true;
                    }
                }
            } catch (Exception e) {
                isChecked = true;
            }

            if (isChecked) {
                MainActivity.isFormSub = false;
                Create_report_alert();
            } else {
                Toast.makeText(Bluetooth_connect.this, "PLease select Type Of Report From Setting", Toast.LENGTH_LONG).show();
            }

        } else {
            if (v.getId() == R.id.start_button || v.getId() == R.id.start_2) {
                myVib.vibrate(50);
                if (started) {
                    stop();
                } else {
                    start();
                }
            } else {
                try {
                    listAdapter.clear();
                    devices.clear();
                    Thread.sleep(2000); // Dhaval1
                    doDiscovery();

                } catch (Exception e) {
                    System.out.println("error in on click" + e.getMessage());
                }
            }
        }
    }

    protected void onDestroy() {
        pv.isshowM = false;
        // Make sure we're not doing discovery anymore
        if (bAdapter != null) {
            bAdapter.cancelDiscovery();
            // Unregister broadcast listeners
            if (REGISTER)
                this.unregisterReceiver(receiver);
        }
        Toast.makeText(this, "Quitting App", Toast.LENGTH_LONG).show();
        super.onDestroy();
        System.exit(2);
    }

    private class ConnectedThread extends Thread {
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams, using temp objects because member streams are final
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
                System.out.println("ConnectedThread");
            } catch (Exception e) {
                System.out.println("Exception in ConnectedThread: + " + e.getMessage());
            }
            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            // Keep listening to the InputStream until an exception occurs
            try {
                Message msg1 = Bt_mHandler.obtainMessage(SEND_INSTREAM, mmInStream);
                Bt_mHandler.sendMessage(msg1);
                System.out.println("run ConnectedThread");
                Message msg2 = Bt_mHandler.obtainMessage(SEND_OUTSTREAM, mmOutStream);
                Bt_mHandler.sendMessage(msg2);
                battery.setTitle("Battery: " + MainActivity.battery_status);

            } catch (Exception e) {
                Message m1 = Bt_mHandler.obtainMessage(MESSAGE_COMMUNICATION);
                Bt_mHandler.sendMessage(m1);
                System.out.println("Error in connected thread=" + e.getMessage());
            }
        }
    }
}
