package com.example.theeradach.layout_cardview;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.provider.ContactsContract;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity implements CompoundButton.OnCheckedChangeListener {

    private Switch swVoice , swBattery;
    private TextView txtVoice ,txtName , txtNumber;
    private int RequestPermissionCode = 1;
    public String TempNameHolder, TempNumberHolder, TempContactID, IDresult = "" ;
    private ImageButton imgBtnContract;

    // Navigator part
    public static double cur_lat;
    public static double cur_long;
    public static double des_lat;
    public static double des_long;
    GPSTracker gps;
    private static final int REQUEST_CODE_PERMISSION = 2;
    String mPermission = Manifest.permission.ACCESS_FINE_LOCATION;
    // Navigator part

    //SMS part
    private static final int MY_PERMISSIONS_REQUEST_SEND_SMS =0 ;
    // SMS part


    SharedPreferences sharedpreferences;

    // speech recognizerManager class
    private SpeechRecognizerManager mSpeechManager;

    // set variables for sharePreferences !
    public static final String MyPREFERENCES = "Emergency" ;
    public static final String Name = "nameKey";
    public static final String Phone = "phoneKey";
    // End set variables for sharePreferences !

    // Battery level
    String[] batteryLevel = { "90", "80","70" , "60" , "50" , "40" ,"30" , "20" , };
    public static int level;
    public static int adapterLevel;
    private static int switchState = 0;
    private Spinner spinnerBattery;
    // Battery level

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        swVoice = (Switch) findViewById(R.id.swVoice);
        swBattery = (Switch) findViewById(R.id.swBattery);
        txtVoice = (TextView) findViewById(R.id.txtVoice);
        imgBtnContract = (ImageButton) findViewById(R.id.imgBtnContract);
        txtName = (TextView) findViewById(R.id.txtName);
        txtNumber = (TextView) findViewById(R.id.txtNumber);
        spinnerBattery = (Spinner) findViewById(R.id.spinnerBattery);
        swBattery = (Switch) findViewById(R.id.swBattery);

        registerBatteryLevelReceiver();

        spinnerBattery.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                Toast.makeText(getApplicationContext(), "Battery Level: " + batteryLevel[position], Toast.LENGTH_LONG).show();
                adapterLevel = Integer.parseInt(batteryLevel[position]);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        // adapter for battery spinner
        ArrayAdapter aa = new ArrayAdapter(this, android.R.layout.simple_spinner_item, batteryLevel);
        aa.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        //Setting the ArrayAdapter data on the Spinner
        spinnerBattery.setAdapter(aa);



        swBattery.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                if (isChecked){
                    Toast.makeText(MainActivity.this, "SWITCH ON", Toast.LENGTH_SHORT).show();
                    switchState = 1;
                }else{
                    Toast.makeText(MainActivity.this, "SWITCH OFF", Toast.LENGTH_SHORT).show();
                    switchState = 0;
                }
            }
        });


        swVoice.setOnCheckedChangeListener(this);

        // set share preferences
        sharedpreferences = getApplicationContext().getSharedPreferences(MyPREFERENCES, Context.MODE_PRIVATE);

        // image contract button is clicked
        imgBtnContract.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
                startActivityForResult(intent, 7);

            }
        });
        // end image contract button is clicked


        // Emergency SMS text
        if (sharedpreferences.contains(Name)) {
            txtName.setText(sharedpreferences.getString(Name, ""));
        }
        if (sharedpreferences.contains(Phone)) {
            txtNumber.setText(sharedpreferences.getString(Phone, ""));

        }else {
            txtName.setText("");
            txtNumber.setText("");
        }
        // End Emergency SMS text
    }

    // broadcast Receiver for battery
    private BroadcastReceiver battery_receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            boolean isPresent = intent.getBooleanExtra("present", false);
            int scale = intent.getIntExtra("scale", -1);
            int rawlevel = intent.getIntExtra("level", -1);
            level = 0;

//            Bundle bundle = intent.getExtras();
//            Log.i("BatteryLevel", bundle.toString());

            if (isPresent) {
                if (rawlevel >= 0 && scale > 0) {
                    level = (rawlevel * 100) / scale;
                }
                //String info = "Battery Level: " + level;
            }

            batterySwitch(adapterLevel, level);

        }

    };
    // End broadcast Receiver for battery

    public void batterySwitch(int adapterLevel, int level) {
        if(switchState == 1) {
            if (adapterLevel >= level) {
                Toast.makeText(this, "Low Battery", Toast.LENGTH_SHORT).show();

//                MyTTS.getInstance(getApplicationContext())
//                        .setEngine("com.google.android.tts")
//                        .speak("Battery level is lower than " + adapterLevel + "percents");
            } else {
                Toast.makeText(this, "Battery enough", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void registerBatteryLevelReceiver() {
        IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        registerReceiver(battery_receiver, filter);
    }


    // end Battery zone

    // control switch on/off speech recognizer
    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {

        if(PermissionHandler.checkPermission(this,PermissionHandler.RECORD_AUDIO)) {

            if (isChecked) {
                if (mSpeechManager == null) {
                    SetSpeechListener();
                } else if (!mSpeechManager.ismIsListening()) {
                    mSpeechManager.destroy();
                    SetSpeechListener();
                }
                txtVoice.setText(getString(R.string.you_may_speak));
            } else {
                if (mSpeechManager != null) {
                    txtVoice.setText(getString(R.string.destroied));
                    mSpeechManager.destroy();
                    mSpeechManager = null;
                }
            }
        }else {
            PermissionHandler.askForPermission(PermissionHandler.RECORD_AUDIO,this);
        }
    }


    // control switch on/off battery notification




    // set speech listener to get text
    private void SetSpeechListener()
    {
        mSpeechManager=new SpeechRecognizerManager(this, new SpeechRecognizerManager.onResultsReady() {
            @Override
            public void onResults(ArrayList<String> results) {

                if(results!=null && results.size()>0)
                {
                    txtVoice.setText(results.get(0));      // get result from speech here
                    commandVoice(results.get(0));           // *** Voice Commands ***
                }
                else
                    txtVoice.setText(getString(R.string.no_results_found));
            }
        });
    }

    // request record audio Permission
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch(requestCode)
        {
            case PermissionHandler.RECORD_AUDIO:
                if(grantResults.length>0) {
                    if(grantResults[0]== PackageManager.PERMISSION_GRANTED) {
                        //start_listen_btn.performClick();
                        swVoice.performClick();
                    }
                }
                break;

        }
    }

    // function get phone number and name from contract
    @Override
    public void onActivityResult(int RequestCode, int ResultCode, Intent ResultIntent) {

        super.onActivityResult(RequestCode, ResultCode, ResultIntent);

        switch (RequestCode) {

            case (7):
                if (ResultCode == Activity.RESULT_OK) {

                    Uri uri;
                    Cursor cursor1, cursor2;

                    int IDresultHolder ;

                    uri = ResultIntent.getData();

                    cursor1 = getContentResolver().query(uri, null, null, null, null);

                    if (cursor1.moveToFirst()) {

                        TempNameHolder = cursor1.getString(cursor1.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));

                        TempContactID = cursor1.getString(cursor1.getColumnIndex(ContactsContract.Contacts._ID));

                        IDresult = cursor1.getString(cursor1.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER));

                        IDresultHolder = Integer.valueOf(IDresult) ;

                        if (IDresultHolder == 1) {

                            cursor2 = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = " + TempContactID, null, null);

                            while (cursor2.moveToNext()) {

                                TempNumberHolder = cursor2.getString(cursor2.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));

                                txtName.setText(" "+ TempNameHolder);
                                txtNumber.setText(" "+ TempNumberHolder);

                                SharedPreferences.Editor editor = sharedpreferences.edit();
                                editor.putString(Name, "" + TempNameHolder);
                                editor.putString(Phone, "" + TempNumberHolder);
                                editor.commit();
                            }
                        }

                    }
                }
                break;
        }
    }
    // function get phone number and name from contract


    // permission for read contact
    public void EnableRuntimePermission(){

        if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                Manifest.permission.READ_CONTACTS))
        {

            Toast.makeText(MainActivity.this,"CONTACTS permission allows us to Access CONTACTS app", Toast.LENGTH_LONG).show();

        } else {

            ActivityCompat.requestPermissions(MainActivity.this,new String[]{
                    Manifest.permission.READ_CONTACTS}, RequestPermissionCode);

        }
    }


    // Voice command function !!
    private void commandVoice(String s) {
        if(s.contains("โทรหา")){   // call from name

            // get name from calling
            String getContactName;
            int index , length ;
            index = s.indexOf("า");
            length = s.length();

            // get name
            getContactName = s.substring(index+1 , length);
            //Toast.makeText(this, "index :" + index +"\n"+ "length : "+ length, Toast.LENGTH_SHORT).show();
            Toast.makeText(this, "ชื่อ : "+ getContactName , Toast.LENGTH_SHORT).show();

            // get number from name and call
            NumNameGet getContact = new NumNameGet();
            String temp = getContact.get_Number(getContactName, getApplicationContext());
            Toast.makeText(this, "เบอร์โทรศัพท์ : "+ temp , Toast.LENGTH_SHORT).show();

            Intent callIntent = new Intent(Intent.ACTION_CALL);
            callIntent.setData(Uri.parse("tel:" + temp ));
//            callIntent.setData(Uri.parse("tel:0992980430" ));
            startActivity(callIntent);

//            SetSpeechListener();  // keep speech recognition carries on after phone call

        }if (s.contains("นำทางไป")){   // Navigator

            // get name from calling
            String getLocation;
            int index , length ;
            index = s.indexOf("ป");
            length = s.length();

            // get location name
            getLocation = s.substring(index+1 , length);
            Toast.makeText(this, "สถานที่ : "+ getLocation , Toast.LENGTH_SHORT).show();

            gps = new GPSTracker(MainActivity.this);
            cur_lat = gps.getLatitude();
            cur_long = gps.getLongitude();

            convertAddress(getLocation);

            String direction = "https://www.google.com/maps/dir/" + des_lat + ",+" + des_long + "/" + cur_lat + ",+" + cur_long + "/";
            //String direction = cur_lat  + "  " +  cur_long;

            Context context = getApplicationContext();
            Toast.makeText(context, "direction :" + direction , Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(android.content.Intent.ACTION_VIEW, Uri.parse(direction));
            startActivity(intent);


        }if (s.contains("โทรหาเบอร์ฉุกเฉิน") || s.contains("ฉุกเฉิน") || s.contains("emergency")){

            Toast.makeText(this, "ได้ค่ะจะโทรหาเบอร์ ฉุกเฉินทันที", Toast.LENGTH_SHORT).show();


        }if (s.contains("สวัสดี")){

            Toast.makeText(this, "สวัสดีครับ มีอะไรให้รับใช้หรอค่ะ", Toast.LENGTH_SHORT).show();
            // call Text To Speech from class
            MyTTS.getInstance(getApplicationContext())
                    .setEngine("com.google.android.tts")
                    .setLocale(new Locale("th"))
                    .speak("สวัสดีค่ะ");

        }if (s.contains("เป็นอย่างไรบ้าง") || s.contains("เป็นไงบ้าง")){

            Toast.makeText(this, "ดิฉันสบายดีค่ะ", Toast.LENGTH_SHORT).show();
            // call Text To Speech from class
            MyTTS.getInstance(getApplicationContext())
                    .setEngine("com.google.android.tts")
                    .setLocale(new Locale("th"))
                    .speak("สบายดีค่ะ");

        }if (s.contains("ยินดีที่ได้รู้จัก")){

            Toast.makeText(this, "ยินดีที่ได้รู้จักเช่นกันค่ะ", Toast.LENGTH_SHORT).show();
            // call Text To Speech from class
            MyTTS.getInstance(getApplicationContext())
                    .setEngine("com.google.android.tts")
                    .setLocale(new Locale("th"))
                    .speak("ยินดีที่ได้รู้จักเช่นกันค่ะ");
        }if (s.contains("รับสาย") || s.contains("answer")){  // Answer incoming call
//            // Answer Call from incoming number
//            Intent i = new Intent(Intent.ACTION_MEDIA_BUTTON);
//            i.putExtra(Intent.EXTRA_KEY_EVENT, new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_HEADSETHOOK));
//            getApplicationContext().sendOrderedBroadcast(i, null);
//            // End Answer Call from incoming number

        }if (s.contains("วางสาย") || s.contains("reject")){ // Reject incoming call
//            disconnectCall();

        }if (s.contains("หยุดการทำงาน") || s.contains("stop working")){
            MyTTS.getInstance(getApplicationContext())
                    .setEngine("com.google.android.tts")
//                    .setLocale(new Locale("th"))
                    .speak("เป็นเกียรติที่ได้รับใช้ค่ะ");
            mSpeechManager.destroy();
            mSpeechManager=null;
        }
    }

    // End - Voice command function !!



    // Class ConvertAddress to Longtitude and Latitude
    public void convertAddress(String address) {
        if (address != null && !address.isEmpty()) {
            try {
                Context context = getApplicationContext();
                //Locale locale = Locale.ENGLISH;
                Locale locale = new Locale("th_TH");  // support Thai language
                Geocoder geoCoder = new Geocoder(context, locale);
                List<Address> addressList = geoCoder.getFromLocationName(address, 1);
                if (addressList != null && addressList.size() > 0) {
                    des_lat = addressList.get(0).getLatitude();
                    des_long = addressList.get(0).getLongitude();
                    //Toast.makeText(context, "Lat :" + des_lat + "\nLong :" + des_long, Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                e.printStackTrace();
            } // end catch
        } // end if
    } // end convertAddress



    // on application is pause
    @Override
    protected void onPause() {
        if(mSpeechManager!=null) {
            mSpeechManager.destroy();
            mSpeechManager=null;
        }
        super.onPause();
    }
}
