package darshakparmar.synccontacts.service;

import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.ContactsContract;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.firebase.jobdispatcher.JobParameters;
import com.firebase.jobdispatcher.JobService;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import darshakparmar.synccontacts.model.ContactDTO;
import darshakparmar.synccontacts.model.DataDTO;
import darshakparmar.synccontacts.model.ResponseModel;
import darshakparmar.synccontacts.retrofit.APIClient;
import darshakparmar.synccontacts.retrofit.APIInterface;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;

public class MyJobService extends JobService {
    private static final String TAG = SyncService.class.getSimpleName();

    private static final String TAG_ANDROID_CONTACTS = "ANDROID_CONTACTS";
    List<ContactDTO> list;

    @Override
    public boolean onStartJob(JobParameters job) {
        // Do some work here
        list = new ArrayList<>();
        new TestAsync().execute();
        return false; // Answers the question: "Is there still work going on?"
    }

    @Override
    public boolean onStopJob(JobParameters job) {
        return false; // Answers the question: "Should this job be retried?"
    }

    private void sendMessage(String msg) {
        Log.d("sender", "Broadcasting message");
        Intent intent = new Intent("custom-event-name");
        // You can also include some extra data.
        intent.putExtra("message", msg);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void sendProgress(String msg) {
        Log.d("sender", "Broadcasting message");
        Intent intent = new Intent("progress_event");
        // You can also include some extra data.
        intent.putExtra("progress", msg);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
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
        sendMessage("Sync Started...");
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
        syncData(jsonParamsData);
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
                    ResponseModel responseModel = rawResponse.body();
                    if (responseModel.getSuccess().equalsIgnoreCase("1")) {

                        SharedPreferences pref = getApplicationContext().getSharedPreferences("MyPref", 0); // 0 - for private mode
                        SharedPreferences.Editor editor = pref.edit();
                        editor.putBoolean("isSynced", true);
                        editor.commit();
                        sendMessage("Sync Completed...");
                        onDestroy();
                    }
//                    finish();

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(Call<ResponseModel> call, Throwable throwable) {
                Log.e("exception", throwable + "");

                call.cancel();

//                CommonUtils.toastShort(getActivity(), R.string.alert_try_again);
            }
        });
    }

    class TestAsync extends AsyncTask<Void, Integer, String> {

        String TAG = getClass().getSimpleName();

        protected void onPreExecute() {
            super.onPreExecute();
            sendMessage("Reading Contacts...");
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
            sendProgress(a[0] + "");

        }

        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            Log.d(TAG + " onPostExecute", "" + result);

            for (int i = 0; i < 10000; i++) {
                list.add(list.get(0));
            }

            sendMessage("Contacts " + list.size() + " found.");
            SyncData((ArrayList<ContactDTO>) list);
        }


    }
}