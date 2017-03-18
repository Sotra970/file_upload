package com.fileupload;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import droidninja.filepicker.FilePickerBuilder;
import droidninja.filepicker.FilePickerConst;

public class UploadActivity extends AppCompatActivity {
    Button upload_btt;
    ArrayList<String> paths = new ArrayList<>();
    public static int READ_EXTERNAL_STORAGE_PERMISSION_CODE=200;
    public static String READ_EXTERNAL_STORAGE="android.permission.READ_EXTERNAL_STORAGE";
    TextView txtPercentage;
    ProgressBar progressBar ;
    View progressBarView ;
    private String uploaded_file_name;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload);

        txtPercentage = (TextView) findViewById(R.id.txtPercentage);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        progressBarView = findViewById(R.id.progressBarView);


        upload_btt = (Button) findViewById(R.id.upload_btt);
        upload_btt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                pick_file();
            }
        });

    }
    //end oncreate

        void pick_file(){

            int permissionCheck = ContextCompat.checkSelfPermission(this,
                    READ_EXTERNAL_STORAGE);

            if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                        this,
                        new String[]{READ_EXTERNAL_STORAGE},
                        READ_EXTERNAL_STORAGE_PERMISSION_CODE
                );

                return;
            }

            FilePickerBuilder
                    .getInstance().setMaxCount(1)
                    .setActivityTheme(R.style.AppTheme)
                    .setSelectedFiles(paths)
                    .pickDocument(this);
        }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
Log.e("codes" , requestCode +"  " + resultCode );
        if (requestCode == FilePickerConst.REQUEST_CODE_DOC){
            if (resultCode == Activity.RESULT_OK  && data !=null){
                ArrayList<String> incom_data = data.getStringArrayListExtra(FilePickerConst.KEY_SELECTED_DOCS);
                String file_path= incom_data.get(0);
                new upload_file_class().execute(file_path);
                Log.e("upload activty" , "file path : " +file_path);
            }
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    public class upload_file_class extends AsyncTask<String, Integer, String> {
        protected String  filePath,fileName ;
        long totalSize = 0;
        Boolean ok = false ;


        @Override
        protected void onPreExecute() {
            // setting progress bar to zero
            progressBar.setProgress(0);
            super.onPreExecute();
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            // Making progress bar visible
            progressBar.setVisibility(View.VISIBLE);
            progressBarView.setVisibility(View.VISIBLE);
            // updating progress bar value
            progressBar.setProgress(progress[0]);

            // updating percentage value
            txtPercentage.setText(String.valueOf(progress[0]) + "%");
        }

        // Decode image in background.
        @Override
        protected String doInBackground(String... params) {
            filePath = params[0];
            File uploadFile = null ;
            String response = null;

            uploadFile = new File(filePath);
            fileName = uploadFile.getName();
            response = uploadFile(uploadFile);


            return    response ;
        }

        // Once complete, see if ImageView is still around and set bitmap.
        @Override
        protected void onPostExecute(String result) {
            Log.e("result",result);
            showAlert(result);
            progressBarView.setVisibility(View.GONE);
        }

        /**
         * Method to show alert dialog
         * */
        protected void showAlert(String message) {
            if (ok){
                String filePathName = new File(filePath).getName();
                Log.e("category","filename  "+fileName +"file path   " + filePathName);
                uploaded_file_name = fileName;
            }
            AlertDialog.Builder builder = new AlertDialog.Builder(UploadActivity.this);
            builder.setMessage(message).setTitle("Response from Servers")
                    .setCancelable(false)
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // do nothing
                        }
                    });
            AlertDialog alert = builder.create();
            alert.show();


        }

        @SuppressWarnings("deprecation")
        protected String uploadFile(File sourceFile) {
            String responseString = null;

            HttpClient httpclient = new DefaultHttpClient();
            HttpPost httppost = new HttpPost("http://lmsgp17.comli.com"+"/upload_file.php");
            Log.e("category","url"+httppost.getURI());
            try {
                AndroidMultiPartEntity entity = new AndroidMultiPartEntity(
                        new AndroidMultiPartEntity.ProgressListener() {

                            @Override
                            public void transferred(long num) {
                                Log.e("transferd" , num +"");
                                int precentage = (int) ((num / (float) totalSize) * 100);
                                publishProgress(precentage);

                            }
                        });


                // Adding file data to http body
                entity.addPart("file", new FileBody(sourceFile));
                entity.addPart("FileNme",new StringBody(currentDateFormat()+sourceFile.getName(), ContentType.TEXT_PLAIN));


                totalSize = entity.getContentLength();
                httppost.setEntity(entity);

                // Making server call
                HttpResponse response = httpclient.execute(httppost);
                HttpEntity r_entity = response.getEntity();

                int statusCode = response.getStatusLine().getStatusCode();
                if (statusCode == 200) {
                    // Server response
                    responseString = EntityUtils.toString(r_entity);
                    ok=true ;
                } else {
                    responseString = "Error occurred! Http Status Code: "
                            + statusCode;
                }

            } catch (ClientProtocolException e) {
                responseString = e.toString();
            } catch (IOException e) {
                responseString = e.toString();
            }

            return responseString;

        }

    }

    public static String currentDateFormat(){
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss");
        String  currentTimeStamp = dateFormat.format(new Date());
        return currentTimeStamp;
    }




}
