/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ua.org.yurkiss.ecomap;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Vector;

import ua.org.yurkiss.ecomap.data.ProblemsContarct;
import ua.org.yurkiss.ecomap.data.ProblemsContarct.ProblemEntry;

public class FetchProblemsTask extends AsyncTask<Void, Void, Void> {

    private final String LOG_TAG = FetchProblemsTask.class.getSimpleName();

    private final Context mContext;
    private final GoogleMap mMap;
    private Vector<ContentValues> cVVector;

    public FetchProblemsTask(Context context, GoogleMap mMap) {
        this.mContext = context;
        this.mMap = mMap;
    }

    private boolean DEBUG = true;

    /**
     */
    private void getDataFromJson(String problemsJsonStr) throws JSONException {
        final String OPM_ID = "Id";
        final String OPM_TITLE = "Title";
        final String OPM_LATITUDE = "Latitude";
        final String OPM_LONGTITUDE = "Longtitude";
        final String OPM_PROBLEMTYPE = "ProblemTypes_Id";
        final String OPM_STATUS = "Status";
        final String OPM_DATE = "Date";

        try {
            JSONArray problemsJsonArray = new JSONArray(problemsJsonStr);

            //long locationId = addLocation(locationSetting, cityName, cityLatitude, cityLongitude);

            // Insert the new information into the database
            //Vector<ContentValues>

            cVVector = new Vector<ContentValues>(problemsJsonArray.length());

            for(int i = 0; i < problemsJsonArray.length(); i++) {
                // These are the values that will be collected.
                long id;
                String title;
                double latitude;
                double longtitude;
                long problemTypesId;
                int status;
                String date;

                // Get the JSON object representing the problem
                JSONObject problem = problemsJsonArray.getJSONObject(i);

                //JSONObject idObj = problem.getJSONObject(OPM_ID);
                id = problem.getLong(OPM_ID);
                title = problem.getString(OPM_TITLE);
                latitude = problem.getDouble(OPM_LATITUDE);
                longtitude = problem.getDouble(OPM_LONGTITUDE);
                problemTypesId = problem.getLong(OPM_PROBLEMTYPE);
                status = problem.getInt(OPM_STATUS);
                date = problem.getString(OPM_DATE);


                ContentValues weatherValues = new ContentValues();

                weatherValues.put(ProblemEntry.COLUMN_ECOMAP_ID, id);
                weatherValues.put(ProblemEntry.COLUMN_TITLE, title);
                weatherValues.put(ProblemEntry.COLUMN_LATITUDE, latitude);
                weatherValues.put(ProblemEntry.COLUMN_LONGTITUDE, longtitude);
                weatherValues.put(ProblemEntry.COLUMN_PROBLEMTYPEID, problemTypesId);
                weatherValues.put(ProblemEntry.COLUMN_STATUS, status);
                weatherValues.put(ProblemEntry.COLUMN_DATE, date);

                cVVector.add(weatherValues);
                //Log.v("getDataFromJson", weatherValues.toString());
            }

            int inserted = 0;
            // add to database
            if ( cVVector.size() > 0 ) {
                ContentValues[] cvArray = new ContentValues[cVVector.size()];
                cVVector.toArray(cvArray);
                //inserted = mContext.getContentResolver().bulkInsert(ProblemEntry.CONTENT_URI, cvArray);
            }

            Log.d(LOG_TAG, "FetchProblemsTask Complete. " + inserted + " Inserted");

        } catch (JSONException e) {
            Log.e(LOG_TAG, e.getMessage(), e);
            e.printStackTrace();
        }
    }

    @Override
    protected Void doInBackground(Void... params) {

//        // If there's no zip code, there's nothing to look up.  Verify size of params.
//        if (params.length == 0) {
//            return null;
//        }
//        String locationQuery = params[0];

        // These two need to be declared outside the try/catch
        // so that they can be closed in the finally block.
        HttpURLConnection urlConnection = null;
        BufferedReader reader = null;

        // Will contain the raw JSON response as a string.
        String problemsJsonStr = null;

        String format = "json";
        String units = "metric";
        int numDays = 14;

        try {
            // Construct the URL for the OpenWeatherMap query
            // Possible parameters are avaiable at OWM's forecast API page, at
            // http://openweathermap.org/API#forecast
            final String FORECAST_BASE_URL =
                    "http://ecomap.org/api/problems";

            Uri builtUri = Uri.parse(FORECAST_BASE_URL).buildUpon().build();

            URL url = new URL(builtUri.toString());

            // Create the request to OpenWeatherMap, and open the connection
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();

            // Read the input stream into a String
            InputStream inputStream = urlConnection.getInputStream();
            StringBuffer buffer = new StringBuffer();
            if (inputStream == null) {
                // Nothing to do.
                return null;
            }
            reader = new BufferedReader(new InputStreamReader(inputStream));

            String line;
            while ((line = reader.readLine()) != null) {
                // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                // But it does make debugging a *lot* easier if you print out the completed
                // buffer for debugging.
                buffer.append(line + "\n");
            }

            if (buffer.length() == 0) {
                // Stream was empty.  No point in parsing.
                return null;
            }
            problemsJsonStr = buffer.toString();
            getDataFromJson(problemsJsonStr);
        } catch (IOException e) {
            Log.e(LOG_TAG, "Error ", e);
            // If the code didn't successfully get the weather data, there's no point in attempting
            // to parse it.
        } catch (JSONException e) {
            Log.e(LOG_TAG, e.getMessage(), e);
            e.printStackTrace();
        } finally {
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
            if (reader != null) {
                try {
                    reader.close();
                } catch (final IOException e) {
                    Log.e(LOG_TAG, "Error closing stream", e);
                }
            }
        }
        return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);
        for(ContentValues cv : cVVector) {
            mMap.addMarker(new MarkerOptions()
                    .position(new LatLng(cv.getAsDouble(ProblemEntry.COLUMN_LATITUDE), cv.getAsDouble(ProblemEntry.COLUMN_LONGTITUDE)))
                    .title(cv.getAsString(ProblemEntry.COLUMN_TITLE)));
        }

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(50.461166, 30.417397), 5));


    }
}