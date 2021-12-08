package com.example.tempokeeper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ElevJSONParser {

    /**
     * Receives a JSONObject and returns a list of lists containing latitude and longitude
     */
    public List<HashMap<String, String>> parse(JSONObject jObject) {

        List<HashMap<String, String>> elevations = new ArrayList<HashMap<String, String>>();
        JSONArray jResults = null;

        try {

            jResults = jObject.getJSONArray("results");

            /** Traversing all routes */
            for (int i = 0; i < jResults.length(); i++) {
                HashMap<String, String> hm = new HashMap<>();
                String elev = ((JSONObject) jResults.getJSONObject(i)).getString("elevation");
                hm.put("elev", elev);

                elevations.add(hm);
            }

        } catch (JSONException e) {
            e.printStackTrace();
        } catch (Exception e) {
        }

        return elevations;
    }
}