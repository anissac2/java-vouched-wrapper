package com.java.vouched;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.json.JSONObject;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.async.Callback;
import com.mashape.unirest.http.exceptions.UnirestException;

public class Verification {
	static Gson gson = new GsonBuilder().setPrettyPrinting().create();
	static JsonParser jp = new JsonParser();
	
	static Map<String, String> headers = new HashMap<>();
	static String APP_ID = "P#mh_rFrHERaT!slSg!RV8ttVjT1O@";
	
	public static void main(String[] args) throws UnirestException {
		//runs id verification then on completion runs facematch verification
		verificationRequest();
	}
	
	//creates request with required fields
	public static JSONObject createJsonRequest(boolean isId, String stage) {
		 String name = isId ? "test_id.jpg" : "test_photo.jpg";
		   File f =  new File(name);
		   String encodedstring = encodeFileToBase64Binary(f);
		   
		   JSONObject jsonObject = new JSONObject();
		   jsonObject.put("operationName", "postJobMutation");	
		   
		   JSONObject variables = new JSONObject();
		   variables.put("stage", stage);
		   variables.put("type", "id-verification"); 
		   
		   JSONObject params = new JSONObject();
		   params.put(isId ? "idPhoto" : "userPhoto", encodedstring);
		   
		   variables.put("params", params);
		   jsonObject.put("variables", variables);
		   
		   String idQuery = "mutation postJobMutation($type: String!, $callbackURL: String, $stage: String, $params: JobParams) {\n  postJob(stage: $stage, callbackURL: $callbackURL, type: $type, params: $params) {\n    ...FullJob\n    __typename\n  }\n}\n\nfragment FullJob on Job {\n  status\n  completed\n  request {\n    type\n    callbackURL\n    parameters {\n      userPhotoUrl\n      userDimensions {\n        width\n        height\n        __typename\n      }\n      idPhotoUrl\n      idDimensions {\n        width\n        height\n        __typename\n      }\n      firstName\n      lastName\n      dob\n      __typename\n    }\n    __typename\n  }\n  result {\n    idRequireSide\n    id\n    idFields {\n      name\n      __typename\n    }\n    type\n    state\n    country\n    firstName\n    lastName\n    birthDate\n    expireDate\n    success\n    confidences {\n      id\n      selfie\n      idMatch\n      faceMatch\n      __typename\n    }\n    __typename\n  }\n  errors {\n    ...Error\n    __typename\n  }\n  submitted\n  token\n  __typename\n}\n\nfragment Error on Error {\n  type\n  message\n  suggestion\n  __typename\n}\n";
		   String faceMatchQuery = "mutation postJobMutation($type: String!, $callbackURL: String, $stage: String, $jobConfigId: ID, $params: JobParams) {\n  postJob(stage: $stage, jobConfigId: $jobConfigId, callbackURL: $callbackURL, type: $type, params: $params) {\n    ...FullJob\n    __typename\n  }\n}\n\nfragment FullJob on Job {\n  status\n  completed\n  request {\n    type\n    callbackURL\n    parameters {\n      userPhotoUrl\n      userDimensions {\n        width\n        height\n        __typename\n      }\n      idPhotoUrl\n      idDimensions {\n        width\n        height\n        __typename\n      }\n      firstName\n      lastName\n      dob\n      __typename\n    }\n    __typename\n  }\n  result {\n    idRequireSide\n    id\n    idFields {\n      name\n      __typename\n    }\n    type\n    state\n    country\n    firstName\n    lastName\n    birthDate\n    expireDate\n    success\n    confidences {\n      id\n      selfie\n      idMatch\n      faceMatch\n      __typename\n    }\n    __typename\n  }\n  errors {\n    ...Error\n    __typename\n  }\n  submitted\n  token\n  __typename\n}\n\nfragment Error on Error {\n  type\n  message\n  suggestion\n  __typename\n}\n";
		   
		   jsonObject.put("query", isId ? idQuery : faceMatchQuery);	
		   return jsonObject;
	}
	
	public static void verificationRequest() {
		   headers.put("content-type", "application/json"); 
		   headers.put("x-api-id", APP_ID);
		   Unirest.post("https://verify.woollylabs.com/graphql")
		   	.headers(headers)
		   	.body(createJsonRequest(true, "id"))
		   	.asJsonAsync(new Callback<JsonNode>() {
		   		
		   		@Override
		   		public void completed(HttpResponse<JsonNode> response) {
		   			JsonElement je = jp.parse(response.getBody().toString());
		   			String prettyJson = gson.toJson(je);
		   			System.out.println(prettyJson + " id response");
		   			
		   			Gson gson = new GsonBuilder().create();
		   			JsonObject job = gson.fromJson(response.getBody().toString(), JsonObject.class);
		   			JsonElement jsonElement = job.getAsJsonObject("data").getAsJsonObject("postJob").get("token");
		   			String token = jsonElement.getAsString();
		   			
		   			//match selfie with id when token is retrieved
		   			faceMatchRequest(token);
		   		}
		   		
		   		@Override
		   		public void failed(UnirestException e) {
		   			e.printStackTrace();
		   		}
		   		
		   		@Override
		   		public void cancelled() {
		   			System.out.println("cancelled");
		   		}
		   	});
	   }
	
	public static void faceMatchRequest(String sessionToken) {	
		headers.put("content-type", "application/json"); 
		headers.put("x-api-id", APP_ID);
		headers.put("x-session-token", sessionToken);
		try {	
			HttpResponse<String> response = Unirest.post("https://verify.woollylabs.com/graphql")
					.headers(headers)
	          	    .body(createJsonRequest(false, "face_match"))
	          	    .asString();
			response.getBody();
			JsonElement je = jp.parse(response.getBody().toString());
			String prettyJson = gson.toJson(je);
			System.out.println(prettyJson + " facematch response");
			
		} catch (Exception e) { 
	     	   e.printStackTrace();
	    }
	}
	
	private static String encodeFileToBase64Binary(File file) {
		String encodedfile = null;
		try {
			FileInputStream fileInputStreamReader = new FileInputStream(file);
	        byte[] bytes = new byte[(int)file.length()];
	        fileInputStreamReader.read(bytes);
	        encodedfile = new String(Base64.encodeBase64(bytes), "UTF-8");
	    } catch (FileNotFoundException e) {
	            // TODO Auto-generated catch block
	            e.printStackTrace();
	    } catch (IOException e) {
	            // TODO Auto-generated catch block
	            e.printStackTrace();
	        } 
	        return encodedfile;
	}
}
