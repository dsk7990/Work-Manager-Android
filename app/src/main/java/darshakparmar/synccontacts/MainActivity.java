package darshakparmar.synccontacts;

import android.Manifest;
import android.arch.lifecycle.Observer;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.work.Constraints;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkStatus;

import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.DexterError;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.PermissionRequestErrorListener;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import darshakparmar.synccontacts.model.ContactDTO;
import darshakparmar.synccontacts.model.DataDTO;
import darshakparmar.synccontacts.model.ResponseModel;
import darshakparmar.synccontacts.retrofit.APIClient;
import darshakparmar.synccontacts.retrofit.APIInterface;
import darshakparmar.synccontacts.work.MyWorker;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;

public class MainActivity extends AppCompatActivity {
    private static final String TAG_ANDROID_CONTACTS = "ANDROID_CONTACTS";
    List<ContactDTO> list;
    boolean isSync = false;
    private TextView txt, txtPercentage;
    private ProgressBar progressBar;
    private WorkManager mWorkManager;
    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Get extra data included in the Intent
            String message = intent.getStringExtra("message");
            Log.d("receiver", "Got message: " + message);
            txt.setText(message);
        }
    };
    private BroadcastReceiver mProgressReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Get extra data included in the Intent
            String progress = intent.getStringExtra("progress");
            Log.d("receiver", "Got progress: " + progress);
            txtPercentage.setText(progress);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mWorkManager = WorkManager.getInstance();
        txt = (TextView) findViewById(R.id.txt);
        txtPercentage = (TextView) findViewById(R.id.txtPercentage);
        list = new ArrayList<>();
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        progressBar.setVisibility(View.GONE);
        SharedPreferences pref = getApplicationContext().getSharedPreferences("MyPref", 0); // 0 - for private mode
        isSync = pref.getBoolean("isSynced", false);
        Log.e("sync", isSync + "");
        txt.setText("");
        if (!isSync) {
//            Log.e("sync",isSync+"");
            SharedPreferences.Editor editor = pref.edit();
            editor.putBoolean("isSynced", false);
            editor.commit();

//            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//                Intent intent = new Intent();
//                String packageName = getPackageName();
//                PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
//                if (!pm.isIgnoringBatteryOptimizations(packageName)) {
//                    intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
//                    intent.setData(Uri.parse("package:" + packageName));
//                    startActivity(intent);
//                }
//            }
            Dexter.withActivity(MainActivity.this)
                    .withPermissions(
                            Manifest.permission.READ_CONTACTS
                    )
                    .withListener(new MultiplePermissionsListener() {
                        @Override
                        public void onPermissionsChecked(MultiplePermissionsReport report) {
                            // check if all permissions are granted
                            if (report.areAllPermissionsGranted()) {
//                            getAllContacts();
//                            new TestAsync().execute();
                                // start service
//                                startService(new Intent(MainActivity.this, SyncService.class));

//                                ServiceManager.runService(MainActivity.this, SyncService.class);

//                                FirebaseJobDispatcher dispatcher = new FirebaseJobDispatcher(new GooglePlayDriver(MainActivity.this));
//                                Job myJob = dispatcher.newJobBuilder()
//                                        .setService(MyJobService.class) // the JobService that will be called
//                                        .setTag("my-unique-tag")
//                                        .setLifetime(Lifetime.FOREVER)
//
//                                        .setReplaceCurrent(true)// uniquely identifies the job
//                                        .build();
//
//                                dispatcher.mustSchedule(myJob);

                                startWorkManager();
                            }

                            // check for permanent denial of any permission
                            if (report.isAnyPermissionPermanentlyDenied()) {
                                // show alert dialog navigating to Settings
                                showSettingsDialog();
                            }
                        }

                        @Override
                        public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions, PermissionToken token) {
                            token.continuePermissionRequest();
                        }
                    }).
                    withErrorListener(new PermissionRequestErrorListener() {
                        @Override
                        public void onError(DexterError error) {
                            Toast.makeText(MainActivity.this, "Error occurred!", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .onSameThread()
                    .check();
        }
        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,
                new IntentFilter("custom-event-name"));
        LocalBroadcastManager.getInstance(this).registerReceiver(mProgressReceiver,
                new IntentFilter("progress_event"));
    }

    private void startWorkManager() {
        OneTimeWorkRequest someWork = new OneTimeWorkRequest.Builder(MyWorker.class)
                .setInitialDelay(1, TimeUnit.SECONDS)

                .build();
        OneTimeWorkRequest oneTimeWorkRequest = someWork;

        mWorkManager.enqueue(oneTimeWorkRequest);
        WorkManager.getInstance().getStatusById(someWork.getId())
                .observe(this, new Observer<WorkStatus>() {
                    @Override
                    public void onChanged(@Nullable WorkStatus workStatus) {
                        StringBuilder str = new StringBuilder();
                        if (workStatus != null) {
                            str.append("SimpleWorkRequest: " + workStatus.getState().name() + "\n");
                            Log.e("dssk", str + "");
                        }

                        if (workStatus != null && workStatus.getState().isFinished()) {

                            str.append("SimpleWorkRequest (Data): isFinished");
                            Log.e("dssk", str + "");
                        }
                    }
                });
//        WorkStatus workStatus = mWorkManager.getInstance().getStatusById(someWork.getId()).getValue();
//        if(workStatus.getState().)

    }

    private Constraints constraints() {
        Constraints constraints = new Constraints.Builder()
                .setRequiresBatteryNotLow(true) // set as per ur need
                .build();
        return constraints;
    }

    @Override
    protected void onDestroy() {
        // Unregister since the activity is about to be closed.
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mProgressReceiver);
        super.onDestroy();
    }

    public void showSettingsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("Need Permissions");
        builder.setMessage("This app needs permission to use this feature. You can grant them in app settings.");
        builder.setPositiveButton("GOTO SETTINGS", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
                openSettings();
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        builder.show();

    }

    // navigating user to app settings
    public void openSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", getPackageName(), null);
        intent.setData(uri);
        startActivityForResult(intent, 101);
    }

    private List<ContactDTO> getAllContacts() {
        List<ContactDTO> ret = new ArrayList<ContactDTO>();

        // Get all raw contacts id list.
        List<Integer> rawContactsIdList = getRawContactsIdList();

        int contactListSize = rawContactsIdList.size();

        ContentResolver contentResolver = getContentResolver();

        // Loop in the raw contacts list.
        for (int i = 0; i < contactListSize; i++) {
            // Get the raw contact id.
            Integer rawContactId = rawContactsIdList.get(i);

            Log.d(TAG_ANDROID_CONTACTS, "raw contact id : " + rawContactId.intValue());

            // Data content uri (access data table. )
            Uri dataContentUri = ContactsContract.Data.CONTENT_URI;

            // Build query columns name array.
            List<String> queryColumnList = new ArrayList<String>();

            // ContactsContract.Data.CONTACT_ID = "contact_id";
            queryColumnList.add(ContactsContract.Data.CONTACT_ID);

            // ContactsContract.Data.MIMETYPE = "mimetype";
            queryColumnList.add(ContactsContract.Data.MIMETYPE);

            queryColumnList.add(ContactsContract.Data.DATA1);
            queryColumnList.add(ContactsContract.Data.DATA2);
            queryColumnList.add(ContactsContract.Data.DATA3);
            queryColumnList.add(ContactsContract.Data.DATA4);
            queryColumnList.add(ContactsContract.Data.DATA5);
            queryColumnList.add(ContactsContract.Data.DATA6);
            queryColumnList.add(ContactsContract.Data.DATA7);
            queryColumnList.add(ContactsContract.Data.DATA8);
            queryColumnList.add(ContactsContract.Data.DATA9);
            queryColumnList.add(ContactsContract.Data.DATA10);
            queryColumnList.add(ContactsContract.Data.DATA11);
            queryColumnList.add(ContactsContract.Data.DATA12);
            queryColumnList.add(ContactsContract.Data.DATA13);
            queryColumnList.add(ContactsContract.Data.DATA14);
            queryColumnList.add(ContactsContract.Data.DATA15);

            // Translate column name list to array.
            String[] queryColumnArr = queryColumnList.toArray(new String[queryColumnList.size()]);

            // Build query condition string. Query rows by contact id.
            StringBuffer whereClauseBuf = new StringBuffer();
            whereClauseBuf.append(ContactsContract.Data.RAW_CONTACT_ID);
            whereClauseBuf.append("=");
            whereClauseBuf.append(rawContactId);
            ContactDTO contactDTO = new ContactDTO();
            // Query data table and return related contact data.
            try {
                Cursor cursor = contentResolver.query(dataContentUri, queryColumnArr, whereClauseBuf.toString(), null, null);

            /* If this cursor return database table row data.
               If do not check cursor.getCount() then it will throw error
               android.database.CursorIndexOutOfBoundsException: Index 0 requested, with a size of 0.
               */
                if (cursor != null && cursor.getCount() > 0) {
                    StringBuffer lineBuf = new StringBuffer();
                    cursor.moveToFirst();

                    lineBuf.append("Raw Contact Id : ");
                    lineBuf.append(rawContactId);

                    long contactId = cursor.getLong(cursor.getColumnIndex(ContactsContract.Data.CONTACT_ID));
                    lineBuf.append(" , Contact Id : ");
                    lineBuf.append(contactId);

                    do {

                        // First get mimetype column value.
                        String mimeType = cursor.getString(cursor.getColumnIndex(ContactsContract.Data.MIMETYPE));
                        lineBuf.append(" \r\n , MimeType : ");
                        lineBuf.append(mimeType);

                        if (mimeType.equalsIgnoreCase(ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)) {
                            String emailAddress = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.ADDRESS));
                            DataDTO dataDTO = new DataDTO();
                            dataDTO.setDataType(0);
                            dataDTO.setDataValue(emailAddress);
                            ArrayList<DataDTO> list = new ArrayList<>();
                            list.add(dataDTO);
                            contactDTO.setEmailList(list);
                        }

                        if (mimeType.equalsIgnoreCase(ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)) {
                            String phoneNumber = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                            DataDTO dataDTO = new DataDTO();
                            dataDTO.setDataType(0);
                            dataDTO.setDataValue(phoneNumber);
                            ArrayList<DataDTO> list = new ArrayList<>();
                            list.add(dataDTO);
                            contactDTO.setPhoneList(list);
                        }


                        if (mimeType.equalsIgnoreCase(ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)) {
                            String displayName = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME));
                            contactDTO.setDisplayName(displayName);
                        }


                    } while (cursor.moveToNext());
                    cursor.close();

                    Log.d(TAG_ANDROID_CONTACTS, lineBuf.toString());
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
            ret.add(contactDTO);

            Log.d(TAG_ANDROID_CONTACTS, "=========================================================================");
        }

        return ret;
    }

//    private List<ContactDTO> getAllContacts() {
//        List<ContactDTO> ret = new ArrayList<ContactDTO>();
//
//        // Get all raw contacts id list.
//        List<Integer> rawContactsIdList = getRawContactsIdList();
//
//        int contactListSize = rawContactsIdList.size();
//
//        ContentResolver contentResolver = getContentResolver();
//
//        // Loop in the raw contacts list.
//        for (int i = 0; i < contactListSize; i++) {
//            // Get the raw contact id.
//            Integer rawContactId = rawContactsIdList.get(i);
//
//            Log.d(TAG_ANDROID_CONTACTS, "raw contact id : " + rawContactId.intValue());
//
//            // Data content uri (access data table. )
//            Uri dataContentUri = ContactsContract.Data.CONTENT_URI;
//
//            // Build query columns name array.
//            List<String> queryColumnList = new ArrayList<String>();
//
//            // ContactsContract.Data.CONTACT_ID = "contact_id";
//            queryColumnList.add(ContactsContract.Data.CONTACT_ID);
//
//            // ContactsContract.Data.MIMETYPE = "mimetype";
//            queryColumnList.add(ContactsContract.Data.MIMETYPE);
//
//            queryColumnList.add(ContactsContract.Data.DATA1);
//            queryColumnList.add(ContactsContract.Data.DATA2);
//            queryColumnList.add(ContactsContract.Data.DATA3);
//            queryColumnList.add(ContactsContract.Data.DATA4);
//            queryColumnList.add(ContactsContract.Data.DATA5);
//            queryColumnList.add(ContactsContract.Data.DATA6);
//            queryColumnList.add(ContactsContract.Data.DATA7);
//            queryColumnList.add(ContactsContract.Data.DATA8);
//            queryColumnList.add(ContactsContract.Data.DATA9);
//            queryColumnList.add(ContactsContract.Data.DATA10);
//            queryColumnList.add(ContactsContract.Data.DATA11);
//            queryColumnList.add(ContactsContract.Data.DATA12);
//            queryColumnList.add(ContactsContract.Data.DATA13);
//            queryColumnList.add(ContactsContract.Data.DATA14);
//            queryColumnList.add(ContactsContract.Data.DATA15);
//
//            // Translate column name list to array.
//            String queryColumnArr[] = queryColumnList.toArray(new String[queryColumnList.size()]);
//
//            // Build query condition string. Query rows by contact id.
//            StringBuffer whereClauseBuf = new StringBuffer();
//            whereClauseBuf.append(ContactsContract.Data.RAW_CONTACT_ID);
//            whereClauseBuf.append("=");
//            whereClauseBuf.append(rawContactId);
//
//            // Query data table and return related contact data.
//            try {
//                Cursor cursor = contentResolver.query(dataContentUri, queryColumnArr, whereClauseBuf.toString(), null, null);
//
//            /* If this cursor return database table row data.
//               If do not check cursor.getCount() then it will throw error
//               android.database.CursorIndexOutOfBoundsException: Index 0 requested, with a size of 0.
//               */
//                if (cursor != null && cursor.getCount() > 0) {
//                    StringBuffer lineBuf = new StringBuffer();
//                    cursor.moveToFirst();
//
//                    lineBuf.append("Raw Contact Id : ");
//                    lineBuf.append(rawContactId);
//
//                    long contactId = cursor.getLong(cursor.getColumnIndex(ContactsContract.Data.CONTACT_ID));
//                    lineBuf.append(" , Contact Id : ");
//                    lineBuf.append(contactId);
//
//                    do {
//                        // First get mimetype column value.
//                        String mimeType = cursor.getString(cursor.getColumnIndex(ContactsContract.Data.MIMETYPE));
//                        lineBuf.append(" \r\n , MimeType : ");
//                        lineBuf.append(mimeType);
//
//                        List<String> dataValueList = getColumnValueByMimetype(cursor, mimeType);
//                        int dataValueListSize = dataValueList.size();
//                        for (int j = 0; j < dataValueListSize; j++) {
//                            String dataValue = dataValueList.get(j);
//                            lineBuf.append(" , ");
//                            lineBuf.append(dataValue);
//                        }
//
//                    } while (cursor.moveToNext());
//                    cursor.close();
//
//                    Log.d(TAG_ANDROID_CONTACTS, lineBuf.toString());
//                }
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//
//
//            Log.d(TAG_ANDROID_CONTACTS, "=========================================================================");
//        }
//
//        return ret;
//    }

    private String getEmailTypeString(int dataType) {
        String ret = "";

        if (ContactsContract.CommonDataKinds.Email.TYPE_HOME == dataType) {
            ret = "Home";
        } else if (ContactsContract.CommonDataKinds.Email.TYPE_WORK == dataType) {
            ret = "Work";
        }
        return ret;
    }

    /*
     *  Get phone type related string format value.
     * */
    private String getPhoneTypeString(int dataType) {
        String ret = "";

        if (ContactsContract.CommonDataKinds.Phone.TYPE_HOME == dataType) {
            ret = "Home";
        } else if (ContactsContract.CommonDataKinds.Phone.TYPE_WORK == dataType) {
            ret = "Work";
        } else if (ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE == dataType) {
            ret = "Mobile";
        }
        return ret;
    }

    /*
     *  Return data column value by mimetype column value.
     *  Because for each mimetype there has not only one related value,
     *  such as Organization.CONTENT_ITEM_TYPE need return company, department, title, job description etc.
     *  So the return is a list string, each string for one column value.
     * */
    private List<String> getColumnValueByMimetype(Cursor cursor, String mimeType) {
        List<String> ret = new ArrayList<String>();

        switch (mimeType) {
            // Get email data.
            case ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE:
                // Email.ADDRESS == data1
                String emailAddress = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.ADDRESS));
                // Email.TYPE == data2
                int emailType = cursor.getInt(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.TYPE));
                String emailTypeStr = getEmailTypeString(emailType);

                ret.add("Email Address : " + emailAddress);
                ret.add("Email Int Type : " + emailType);
                ret.add("Email String Type : " + emailTypeStr);
                break;

            // Get im data.
            case ContactsContract.CommonDataKinds.Im.CONTENT_ITEM_TYPE:
                // Im.PROTOCOL == data5
                String imProtocol = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Im.PROTOCOL));
                // Im.DATA == data1
                String imId = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Im.DATA));

                ret.add("IM Protocol : " + imProtocol);
                ret.add("IM ID : " + imId);
                break;

            // Get nickname
            case ContactsContract.CommonDataKinds.Nickname.CONTENT_ITEM_TYPE:
                // Nickname.NAME == data1
                String nickName = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Nickname.NAME));
                ret.add("Nick name : " + nickName);
                break;

            // Get organization data.
            case ContactsContract.CommonDataKinds.Organization.CONTENT_ITEM_TYPE:
                // Organization.COMPANY == data1
                String company = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Organization.COMPANY));
                // Organization.DEPARTMENT == data5
                String department = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Organization.DEPARTMENT));
                // Organization.TITLE == data4
                String title = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Organization.TITLE));
                // Organization.JOB_DESCRIPTION == data6
                String jobDescription = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Organization.JOB_DESCRIPTION));
                // Organization.OFFICE_LOCATION == data9
                String officeLocation = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Organization.OFFICE_LOCATION));

                ret.add("Company : " + company);
                ret.add("department : " + department);
                ret.add("Title : " + title);
                ret.add("Job Description : " + jobDescription);
                ret.add("Office Location : " + officeLocation);
                break;

            // Get phone number.
            case ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE:
                // Phone.NUMBER == data1
                String phoneNumber = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                // Phone.TYPE == data2
                int phoneTypeInt = cursor.getInt(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE));
                String phoneTypeStr = getPhoneTypeString(phoneTypeInt);

                ret.add("Phone Number : " + phoneNumber);
                ret.add("Phone Type Integer : " + phoneTypeInt);
                ret.add("Phone Type String : " + phoneTypeStr);
                break;

            // Get sip address.
            case ContactsContract.CommonDataKinds.SipAddress.CONTENT_ITEM_TYPE:
                // SipAddress.SIP_ADDRESS == data1
                String address = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.SipAddress.SIP_ADDRESS));
                // SipAddress.TYPE == data2
                int addressTypeInt = cursor.getInt(cursor.getColumnIndex(ContactsContract.CommonDataKinds.SipAddress.TYPE));
                String addressTypeStr = getEmailTypeString(addressTypeInt);

                ret.add("Address : " + address);
                ret.add("Address Type Integer : " + addressTypeInt);
                ret.add("Address Type String : " + addressTypeStr);
                break;

            // Get display name.
            case ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE:
                // StructuredName.DISPLAY_NAME == data1
                String displayName = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME));
                // StructuredName.GIVEN_NAME == data2
                String givenName = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME));
                // StructuredName.FAMILY_NAME == data3
                String familyName = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME));

                ret.add("Display Name : " + displayName);
                ret.add("Given Name : " + givenName);
                ret.add("Family Name : " + familyName);
                break;

            // Get postal address.
            case ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE:
                // StructuredPostal.COUNTRY == data10
                String country = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.COUNTRY));
                // StructuredPostal.CITY == data7
                String city = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.CITY));
                // StructuredPostal.REGION == data8
                String region = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.REGION));
                // StructuredPostal.STREET == data4
                String street = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.STREET));
                // StructuredPostal.POSTCODE == data9
                String postcode = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.POSTCODE));
                // StructuredPostal.TYPE == data2
                int postType = cursor.getInt(cursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.TYPE));
                String postTypeStr = getEmailTypeString(postType);

                ret.add("Country : " + country);
                ret.add("City : " + city);
                ret.add("Region : " + region);
                ret.add("Street : " + street);
                ret.add("Postcode : " + postcode);
                ret.add("Post Type Integer : " + postType);
                ret.add("Post Type String : " + postTypeStr);
                break;

            // Get identity.
            case ContactsContract.CommonDataKinds.Identity.CONTENT_ITEM_TYPE:
                // Identity.IDENTITY == data1
                String identity = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Identity.IDENTITY));
                // Identity.NAMESPACE == data2
                String namespace = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Identity.NAMESPACE));

                ret.add("Identity : " + identity);
                ret.add("Identity Namespace : " + namespace);
                break;

            // Get photo.
            case ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE:
                // Photo.PHOTO == data15
//                String photo = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Photo.PHOTO));
//                // Photo.PHOTO_FILE_ID == data14
//                String photoFileId = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Photo.PHOTO_FILE_ID));

//                ret.add("Photo : " + photo);
//                ret.add("Photo File Id: " + photoFileId);
                ret.add("Photo : " + "");
                ret.add("Photo File Id: " + "");
                break;

            // Get group membership.
            case ContactsContract.CommonDataKinds.GroupMembership.CONTENT_ITEM_TYPE:
                // GroupMembership.GROUP_ROW_ID == data1
                int groupId = cursor.getInt(cursor.getColumnIndex(ContactsContract.CommonDataKinds.GroupMembership.GROUP_ROW_ID));
                ret.add("Group ID : " + groupId);
                break;

            // Get website.
            case ContactsContract.CommonDataKinds.Website.CONTENT_ITEM_TYPE:
                // Website.URL == data1
                String websiteUrl = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Website.URL));
                // Website.TYPE == data2
                int websiteTypeInt = cursor.getInt(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Website.TYPE));
                String websiteTypeStr = getEmailTypeString(websiteTypeInt);

                ret.add("Website Url : " + websiteUrl);
                ret.add("Website Type Integer : " + websiteTypeInt);
                ret.add("Website Type String : " + websiteTypeStr);
                break;

            // Get note.
            case ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE:
                // Note.NOTE == data1
                String note = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Note.NOTE));
                ret.add("Note : " + note);
                break;

        }

        return ret;
    }

    // Return all raw_contacts _id in a list.
    private List<Integer> getRawContactsIdList() {
        List<Integer> ret = new ArrayList<Integer>();

        ContentResolver contentResolver = getContentResolver();

        // Row contacts content uri( access raw_contacts table. ).
        Uri rawContactUri = ContactsContract.RawContacts.CONTENT_URI;
        // Return _id column in contacts raw_contacts table.
        String[] queryColumnArr = {ContactsContract.RawContacts._ID};
        // Query raw_contacts table and return raw_contacts table _id.
        Cursor cursor = contentResolver.query(rawContactUri, queryColumnArr, null, null, null);
        if (cursor != null && cursor.getCount() > 0) {
            cursor.moveToFirst();
            do {
                int idColumnIndex = cursor.getColumnIndex(ContactsContract.RawContacts._ID);
                int rawContactsId = cursor.getInt(idColumnIndex);
                ret.add(new Integer(rawContactsId));
            } while (cursor.moveToNext());
        }

        cursor.close();

        return ret;
    }

    private void SyncData(ArrayList<ContactDTO> list) {
        progressBar.setVisibility(View.VISIBLE);
        ArrayList<Map<String, Object>> jsonParams1 = new ArrayList<>();

        for (int i = 0; i < list.size(); i++) {
            Log.e("i", i + "");
            Map<String, Object> jsonParams = new HashMap<>();
            ContactDTO contactDTO = list.get(i);
            jsonParams.put("user_name", contactDTO.getDisplayName());
            ArrayList<String> mobList = new ArrayList<>();
            for (int j = 0; j < contactDTO.getPhoneList().size(); j++) {
                DataDTO dataDTO = contactDTO.getPhoneList().get(j);
                mobList.add(dataDTO.getDataValue().replaceAll("[^0-9]+", ""));
            }
            jsonParams.put("user_mobile", mobList);
            if (contactDTO.getEmailList().size() > 0)
                jsonParams.put("user_email", contactDTO.getEmailList().get(0).getDataValue());
            jsonParams1.add(jsonParams);
        }


        Map<String, Object> jsonParamsUserData = new HashMap<>();
        jsonParamsUserData.put("userData", jsonParams1);


        Map<String, Object> jsonParamsData = new HashMap<>();
        jsonParamsData.put("data", jsonParamsUserData);
        Log.e("request", jsonParamsData.toString());
        // syncData(jsonParamsData);
    }

    public void syncData(Map<String, Object> jsonParams) {
        APIInterface apiInterface = APIClient.getClient().create(APIInterface.class);
        RequestBody body = RequestBody.create(okhttp3.MediaType.parse("application/json; charset=utf-8"), (new JSONObject(jsonParams)).toString());
        Call<ResponseModel> response = apiInterface.sync(body);
        response.enqueue(new Callback<ResponseModel>() {
            @Override
            public void onResponse(Call<ResponseModel> call, retrofit2.Response<ResponseModel> rawResponse) {

                try {
                    //get your response....
                    Log.d("syncData: ", rawResponse.body() + "");
                    ResponseModel String = rawResponse.body();
//                    finish();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(Call<ResponseModel> call, Throwable throwable) {
                Log.e("exception", throwable + "");
                progressBar.setVisibility(View.GONE);
                call.cancel();
                Toast.makeText(MainActivity.this, throwable.getMessage(), Toast.LENGTH_SHORT).show();
//                CommonUtils.toastShort(getActivity(), R.string.alert_try_again);
            }
        });
    }

    class TestAsync extends AsyncTask<Void, Integer, String> {

        String TAG = getClass().getSimpleName();

        protected void onPreExecute() {
            super.onPreExecute();
            progressBar.setVisibility(View.VISIBLE);
            Log.d(TAG + " PreExceute", "On pre Exceute......");
        }

        protected String doInBackground(Void... arg0) {
            Log.d(TAG + " DoINBackGround", "On doInBackground...");

            list.addAll(getAllContacts());
            return "You are at PostExecute";
        }

        protected void onProgressUpdate(Integer... a) {
            super.onProgressUpdate(a);
            Log.d(TAG + " onProgressUpdate", "You are in progress update ... " + a[0]);
            txtPercentage.setText(a[0] + "%");
        }

        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            Log.d(TAG + " onPostExecute", "" + result);
            progressBar.setVisibility(View.GONE);
//            for (int i = 0; i < 5000; i++) {
//                list.add(list.get(0));
//            }
            txt.setText(list.size() + " Contacts");

            SyncData((ArrayList<ContactDTO>) list);
        }


    }

}
