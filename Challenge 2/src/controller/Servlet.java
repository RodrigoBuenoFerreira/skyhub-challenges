package controller;

import static com.mongodb.client.model.Filters.eq;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.bson.Document;
import org.bson.types.Binary;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

//java along with servlets was once again a choice based on familiarity. mongoDB was chosen over cloudant 
//both for being declared as preferred and for taking an opportunity to learn a bit more about it.
public class Servlet extends HttpServlet {
	
	//this location will be shown at the urls in the json. update accordingly to publish by different means
	private final String LOCATION = "http://localhost:8080/Challenge_2/";
	
	//webservice to be consumed to get the original image urls. expected {"images":[{"url","<url>"},(...)]}
	private final String CHALLENGE_WEBSERVICE = "http://54.152.221.29/images.json";
		
	private static final long serialVersionUID = 1L;
	
	//a singleton feels like an overkill
	//expecting a local mongodb instance running at the default port
	MongoClient mongoClient = new MongoClient("localhost",27017);
	MongoDatabase database = mongoClient.getDatabase("skyhub");
	MongoCollection<Document> collectionSmall = database.getCollection("small");
	MongoCollection<Document> collectionMedium = database.getCollection("medium");
	MongoCollection<Document> collectionLarge = database.getCollection("large");
       
    public Servlet() {
        super();
    }

	protected void doGet(HttpServletRequest request, HttpServletResponse response){
		String[] pathInfo = request.getPathInfo().split("/");

		//redirection to methods. expected uris:
		// /images.json -> returns the json with all resizes and original images
		// /image/<size>/<file> -> returns the image <file> resized to <size>
		if(pathInfo.length > 0){
			switch (pathInfo[1]){
				case "images.json":
					jsonGet(request, response);
					break;
				case "images":
					imageGet(request, response);
					break;
			}
		}
	}
	
	//handles calls for consuming the challenge's webservice's json, splitting the urls, checking for duplicates, 
	//calling uploads and generating the response json
	private void jsonGet(HttpServletRequest request, HttpServletResponse response){
		
		ArrayList<String> URLs = getURLs(request);
		
		JsonArray outputArray = new JsonArray();
		for(String s : URLs){
			
			//get the file name from the url and call for an upload if it doesn't exist in the database
			String fileName = s.split("/")[s.split("/").length-1];
			if(!exists(fileName)){
				upload(s, fileName);
			}
			
			//prepare the json object for each image and add it to the array
			JsonObject obj = new JsonObject();
			obj.addProperty("original", s);
			obj.addProperty("small", LOCATION + "images/small/" + fileName);
			obj.addProperty("medium", LOCATION + "images/medium/" + fileName);
			obj.addProperty("large", LOCATION + "images/large/" + fileName);
			outputArray.add(obj);
		}
		
		//deliver the result to the writer as a json object
		JsonObject outputJSON = new JsonObject();
		outputJSON.add("images", outputArray);
		
		response.setContentType("application/json");
		try {
			response.getWriter().write(outputJSON.toString());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	//handles delivery from database to user
	private void imageGet(HttpServletRequest request, HttpServletResponse response){
		//expected path = ["images",<size>,<file name>]	
		String[] path = request.getPathInfo().split("/");
		
		if(path.length < 4){
			response.setStatus(404);
			return;
		}
		
		String size = path[2];
		String name = path[3];
		
		//making sure the file exists
		if(!exists(name)){
			response.setStatus(404);
			return;
		}
		Document retrieved;
		
		//taking the right size
		switch(size){
			case "small":
				retrieved = collectionSmall.find(eq("name",name)).first();
				break;
			case "medium":
				retrieved = collectionMedium.find(eq("name",name)).first();
				break;
			case "large":
				retrieved = collectionLarge.find(eq("name",name)).first();
				break;
			
			//return if the size doesn't fit
			default:
				response.setStatus(404);
				return;
		}
		//turning the Document into a byte array to pass to the response's OutputStream
		byte[] retrievedBytes = ((Binary)retrieved.get("image")).getData();
		
		response.setContentType("image/jpg");
		
		try {
			response.getOutputStream().write(retrievedBytes);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	//consumes the challenge's json and returns an arraylist of the image urls
	private ArrayList<String> getURLs(HttpServletRequest request){
		ArrayList<String> URLs = new ArrayList<String>();
		try {
			//opening a connection to the webservice
			URL url = new URL(CHALLENGE_WEBSERVICE);
			HttpURLConnection req = (HttpURLConnection) url.openConnection();
			req.connect();
			
			//parsing the response to a json object
			JsonParser jp = new JsonParser();
			JsonElement root;
			root = jp.parse(new InputStreamReader((InputStream) req.getContent()));
			JsonObject rootobj = root.getAsJsonObject();
			
			//navigating through json object and extracting urls
			JsonArray inputArray = rootobj.getAsJsonArray("images");
			for(JsonElement obj : inputArray){
				URLs.add(((JsonObject)obj).get("url").getAsString());
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return URLs;
	}
	
	//check if a file exists in the database
	private boolean exists(String fileName){
		long count = collectionSmall.count(eq("name",fileName));
		if(count > 0)
			return true;
		return false;
	}
	
	//calls resizes and upload the results to mongo
	private void upload(String url, String fileName){
		try{
			//reading image from url
			BufferedImage originalImage = ImageIO.read(new URL(url));
			BufferedImage resizedImage;
			byte[] data;
			Binary bin;

			//uploading small
			resizedImage = getResizedImage(320, 240, originalImage);
			data = bufferedImageToByteArray(resizedImage);
			bin = new Binary(data);
			collectionSmall.insertOne(new Document("name", fileName).append("image", bin));
			
			//uploading medium
			resizedImage = getResizedImage(384, 288, originalImage);
			data = bufferedImageToByteArray(resizedImage);
			bin = new Binary(data);
			collectionMedium.insertOne(new Document("name", fileName).append("image", bin));
			
			//uploading large
			resizedImage = getResizedImage(640, 480, originalImage);
			data = bufferedImageToByteArray(resizedImage);
			bin = new Binary(data);
			collectionLarge.insertOne(new Document("name", fileName).append("image", bin));
		}
		catch(IOException e){
			e.printStackTrace();
		}
	}
	
	//method to resize the images before storing. getScaledInstance with SCALE_SMOOTH produced the best final quality between tested methods
	private BufferedImage getResizedImage(int width, int height, BufferedImage originalImage){
		BufferedImage resizedImage = new BufferedImage(width, height, originalImage.getType());
		Graphics2D g = resizedImage.createGraphics();
		g.drawImage(originalImage.getScaledInstance(width, height, Image.SCALE_SMOOTH), 0, 0, width, height, null);
		g.dispose();
		return resizedImage;
	}
	
	//converts BufferedImage to byte[]
	private byte[] bufferedImageToByteArray(BufferedImage image){
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			ImageIO.write(image, "jpg", baos);
			baos.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return baos.toByteArray();
	}
}