package test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class ServletTest {
	public static void main(String[] args){
		try {
			//opening a connection to the webservice
			URL url = new URL("http://localhost:8080/Challenge_2/images.json");
			HttpURLConnection req = (HttpURLConnection) url.openConnection();
			req.connect();
			
			//parsing the response to a json object
			JsonParser jp = new JsonParser();
			JsonElement root = jp.parse(new InputStreamReader((InputStream)req.getContent()));
			JsonObject rootobj = root.getAsJsonObject();
			
			System.out.println("Received JSON\n" + rootobj.toString());
			
			//navigating through json object and extracting urls
			ArrayList<String> URLs = new ArrayList<String>();
			JsonArray inputArray = rootobj.getAsJsonArray("images");
			for(JsonElement obj : inputArray){
				URLs.add(((JsonObject)obj).get("original").getAsString());
				URLs.add(((JsonObject)obj).get("small").getAsString());
				URLs.add(((JsonObject)obj).get("medium").getAsString());
				URLs.add(((JsonObject)obj).get("large").getAsString());
			}

			//check for HTTP_OK on all urls
			boolean pass = true;
			for(String s : URLs){
				HttpURLConnection request = (HttpURLConnection) new URL(s).openConnection();
				request.setRequestMethod("HEAD");
				request.connect();
				int responseCode = request.getResponseCode();
				if(responseCode != 200){
					pass = false;
					System.out.println("Response code " + responseCode + " on " + s);
				}
			}
			if(pass)
				System.out.println("All URLs responded with response code 200");
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
