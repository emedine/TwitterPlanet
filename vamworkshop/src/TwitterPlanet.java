package src;
/* 
 * Copyright (c) 2011 Karsten Schmidt
 * 
 * This demo & library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * http://creativecommons.org/licenses/LGPL/2.1/
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */


/*
 * TWITTER PLANET UPDATE
 * 
 * */

// import TextSpawn;
// import UserProfile;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/// osc stuff
import oscP5.*;
import rwmidi.MidiInput;
import rwmidi.MidiOutput;
// import src.SawWave;
// import netP5.*;

/// processing core libraries
import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PImage;

/// toxiclib for 3D
import toxi.geom.Vec3D;
import toxi.geom.mesh.Mesh3D;
import toxi.geom.mesh.SphereFunction;
import toxi.geom.mesh.SurfaceMeshBuilder;
import toxi.processing.ToxiclibsSupport;

//twitter libraries
import twitter4j.Status;
//import twitter4j.StatusAdapter;
import twitter4j.GeoLocation;
import twitter4j.IDs;
import twitter4j.StatusDeletionNotice;
import twitter4j.StatusListener;
import twitter4j.Twitter;
//import twitter4j.TwitterException;
import twitter4j.TwitterStream;
import twitter4j.TwitterStreamFactory;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;
import twitter4j.auth.RequestToken;
import twitter4j.FilterQuery;
import twitter4j.Paging;
import twitter4j.User;
//java libraries


/*
 * <ul>
 * <li>Move mouse to rotate view</li>
 * <li>Press '-' / '=' to adjust zoom</li>
 * </ul>
 * </p>
 */
@SuppressWarnings("serial")
public class TwitterPlanet extends PApplet {

	//Radius of our globe
	private static final int EARTH_RADIUS = 300;

	/// Image size in pixels for rendering
	private static final int IMG_SIZE = 32;
	
	////// FULL SCREEN HANDLER


	/**
	 * Main entry point to run as application
	 */
///*
	public static void main(String[] args) {
		PApplet.main(new String[] { "src.TwitterPlanet" });
	}
	// */

	/// Earth texture image
	private PImage earthTex;

	//Globe mesh
	private Mesh3D globe;

	//Toxiclibs helper class for rendering
	private ToxiclibsSupport gfx;

	//Camera rotation vector
	private final Vec3D camRot = new Vec3D();
	// is moving
	private boolean isMoving = false;
	//Zoom factors
	private float currZoom = 1;
	private float targetZoom = 1;

	//Render flag to show/hide labels
	private boolean showLabels = true;
	// //json data
	String jsonString = "../data/LatLongData.txt";
	JSONArray results;
	JSONObject dbData;
	
	////// Twitter Params
	int tweetLimit = 30; // 6; // upper limit for tweets
	int curTweetNum = 0;
	
	/// JSON STUFF FOR TWITTER
	JSONArray sentimentArray;
	JSONObject sentimentData;

	// Oauth info
	String OAuthConsumerKey = "4M5tIp8YTjua1fPgwXzbfw";
	String OAuthConsumerSecret = "GBHtClbEhdT72AcgUguRIDvmXKA6jxYlzasaTM9Hl8";
	// Access Token info
	static String AccessToken = "633343317-hrcN0DAfVTvIFcAhc6EduWN9lFkSEThXQ422RUsd";
	static String AccessTokenSecret = "doJ1GeGVpc6XUR10xoI8gXn4PdFrAAg8yN8JSBO17M";
	//

	String thePath = "http://api.twitter.com/1/users/show.json?user_id=";
	// if you enter keywords here it will filter, otherwise it will sample
	/// used barak obama because it returns a lot of results right away
	String keywords[] = { "zombies", "werewolves", "mermen"};
	
	// array lists for users and re-tweeters
	ArrayList<GPSMarker> GPSArray = new ArrayList();
	ArrayList<GPSMarker> UserArray = new ArrayList();
	ArrayList<GPSMarker> RTArray = new ArrayList();
	
	/// marker object
	GPSMarker theMarker;
	
	// Twitter objects
	TwitterStream twitter = new TwitterStreamFactory().getInstance();
	Twitter twitterF = new TwitterFactory().getInstance();
	
	
	// / lat and long arrays
	/// these are placeholders
	float[] latArray;
	float[] longArray;
	int LatLongLength;

	// / PApplet stuff
	PApplet pApp;
	// / init marker array
	// GPSMarker[] theMarker;
	
	/// OSC objects
	OscP5 oscP5;
	String oscMess;
	boolean hasOsc;
	float oscX0;
	float oscY0;
	float oscX1;
	float oscY1;

	/// MIDI objects
	MidiControl midiControl;
	
	// destroyer uses polygon builder
	Destroyer theDestroyer;
	boolean doAudio = false;
	// destroyer lata and long
	float theLat= 500;
	float theLong = 500;

	double theCamX = 2.4999995; /// initial camera position
	double theCamY = 3.1199994;
	double theOldCamX = theCamX;
	double theOldCamY = theCamY;
	
	DataProfile dataProfile;
	
	public void setup() {
		size(1024, 768, OPENGL);
		
		/// load search data
		loadSearchData();

		// load earth texture image
		earthTex = loadImage("../data/earth_outlines.jpg"); // ../data/earth_4096.jpg"); //
		// earthTex = loadImage("../data/earth_outlines.png");

		// build a sphere mesh with texture coordinates
		// sphere resolution set to 36 vertices
		globe = new SurfaceMeshBuilder(new SphereFunction()).createMesh(null, 36, EARTH_RADIUS);
		// compute surface orientation vectors for each mesh vertex
		// this is important for lighting calculations when rendering the mesh
		globe.computeVertexNormals();

		// setup helper class (assign to work with this applet)
		gfx = new ToxiclibsSupport(this);
		
		/// add the OSC listener object
		oscP5 = new OscP5(this,8000);

		textFont(createFont("SansSerif", 10));
		// initPoly();
		
		// init our instance of the data profile
		dataProfile =  DataProfile.getInstance();
		// set the papplet so we can get to it from any class
		// instead of passing it back and forth like a potato
		dataProfile.pApp = this; 
		
		/// midi control
		midiControl = MidiControl.getInstance();
		midiControl.initMidi();
		
		/// this populates a placeholder 
		/// location array with lat and lon values
		initLocations();
		
		//// let's do some twitter!
		connectTwitter();
		twitter.addListener(listener);
		if (keywords.length == 0) {
			twitter.sample();
		} else {
			twitter.filter(new FilterQuery().track(keywords));
		}
		
		
		//// this does locations from our original DB
		// initLocations();

	}
	
	public void draw() {

		
		background(165);

		renderGlobe();



	}
	
	/////////////////////////
	///// load keywords
	///////////////////////
	private void loadSearchData(){
		
		String lines[] = loadStrings("../data/search_data.txt");
		for (int i = 0 ; i < lines.length; i++) {
		  println(lines[i]);
		  String tWord = lines[i];
		  keywords[i] = tWord;
		}
	}
	
	/////////////////////////////////////////////
	// //init the location array 
	///// with ip addresses from the DB
	/////////////////////////////////////////////
	
		public void initLocations() {
			//*
			try {
				dbData = new JSONObject(join(loadStrings(jsonString), ""));
				// println("results: " + result);
				results = dbData.getJSONArray("latlong_data");
				// total = dbData.getInt("total");
				// / set length of arrays
				LatLongLength = results.length();
				// init our marker handler
				latArray = new float[results.length()];
				longArray = new float[results.length()];

				// println("LENGTH: " + results.length());
				
				// // let's print this mother out
				for (int i = 0; i < LatLongLength; i++) {

					String theLat = results.getJSONObject(i).getString("lat");
					String theLong = results.getJSONObject(i).getString("long");
					println(results.getJSONObject(i).getString("lat"));
					float lt = new Float(theLat);
					float lo = new Float(theLong);
					latArray[i] = lt;
					longArray[i] = lo;
					
				}

			} catch (JSONException e) {
				println(e);
			}

			//*/
			initDestroyer(); 
		}
	////set up markers and destroyer
		public void initDestroyer() {
			
			/// set up markers
			for (int i = 0; i < GPSArray.size(); i++) {
				// / add a new GPS marker, set its lat and long arrays
				// / and compute its position
				// theMarker[i] = new GPSMarker(longArray[i], latArray[i]);
				// theMarker[i].computePosOnSphere(EARTH_RADIUS);

			}
			
			//// init the destroyer
			//// placeholder lat and long
			float lt = new Float(34.024704);
			float lo = new Float(-84.5033);
			
			try {
			theDestroyer = new Destroyer(lo, lt);
			theDestroyer.computePosOnSphere(EARTH_RADIUS);
			
			} catch(Exception e){
				println(e);
			}
		}

	
	//////////////////////////////
	////// TWITTER STREAM ///////////
	///////////////////////////////
	// INITIALIZE CONNECTION
		void connectTwitter() {
			/// stream
			twitter.setOAuthConsumer(OAuthConsumerKey, OAuthConsumerSecret);
			AccessToken accessToken = loadAccessToken();
			twitter.setOAuthAccessToken(accessToken);
			/// factory
			twitterF.setOAuthConsumer(OAuthConsumerKey, OAuthConsumerSecret);
			twitterF.setOAuthAccessToken(accessToken);
		}

		// Loading up the access token
		private static AccessToken loadAccessToken() {
			return new AccessToken(AccessToken, AccessTokenSecret);
		}

		// STATUS LISTENER
		StatusListener listener = new StatusListener() {
			public void onStatus(Status status) {
				if(curTweetNum < tweetLimit){
					

					// println("@" + status.getUser().getScreenName() + " - " +
					/// checks for tweets using the keyword
					/// add user to the GPS array
					/// println("@" + status.getUser().getId() + " id: " + tweetID);
					// theUser = new UserProfile();
					// UserArray.add(theUser);
					// lat":"34.024704","long":"-84.5033",
					float lt = new Float(34.024704);
					float lo = new Float(-84.5033);
					//*
					if(status.getUser().isGeoEnabled()){
						
						status.getGeoLocation();
						// println("GEOLOC: " + status.getGeoLocation());

					} else {
						// println("NO GEOLOC: " + theMarker.theLocation);
					//// find random lat and long
						int tempLoc = (int)random(LatLongLength);
						//// populate!
						try{
							lt =latArray[tempLoc];
							lo =longArray[tempLoc];
						} catch (Exception e){
							println("Can't parse locations");
							lt = new Float(34.024704);//latArray[tempLoc];
							lo = new Float(-84.5033);//longArray[tempLoc];
						}
						
					}
			        if (status.getGeoLocation() != null) {
			        	
			            GeoLocation alocation =status.getGeoLocation();
			            String aloc = alocation.toString();
			            println("REAL GEO DATA: " + aloc);
			            
			        }
					
					theMarker = new GPSMarker(lo,lt);
					/// theMarker = new GPSMarker(longArray[i], latArray[i]);
					theMarker.computePosOnSphere(EARTH_RADIUS);
					GPSArray.add(theMarker);
					theMarker.doHit();
					/// add all data to user profile
					theMarker.userID = status.getUser().getId();
					theMarker.StatusID = status.getId();
					theMarker.userName = status.getUser().getName();
					theMarker.screenName = status.getUser().getScreenName();
					theMarker.tweetText = status.getText();
					theMarker.timeZone = status.getUser().getTimeZone();
					theMarker.followersCount = status.getUser().getFollowersCount();
					theMarker.friendsCount = status.getUser().getFriendsCount();
					theMarker.favoritesCount = status.getUser().getFollowersCount();
					theMarker.theLocation = status.getUser().getLocation();


					if(status.getUser().isGeoEnabled()){
						theMarker.hasGeo = true;
						status.getGeoLocation();
						println("GEOLOC: " + status.getGeoLocation());

					} else {
						println("NO GEOLOC: " + theMarker.theLocation);
					}
					
					
					curTweetNum +=1;
		
					// update max followers, favorites, and friends
					// this allows for scalable amount indicators
					if(dataProfile.maxFollowers <= theMarker.followersCount){
						dataProfile.maxFollowers = theMarker.followersCount;
		
					}
					if(dataProfile.maxFavorites <= theMarker.favoritesCount){
						dataProfile.maxFavorites = theMarker.favoritesCount;
		
					}
					if(dataProfile.maxFriends <= theMarker.friendsCount){
						dataProfile.maxFriends = theMarker.friendsCount;
		
					}
					/// REPLY CHECKS
					if(status.getInReplyToScreenName() != null){
						theMarker.replyToScreenName = status.getInReplyToScreenName();
						// println(theUser.screenName + " replied from: " + theUser.replyToScreenName);
					}
		
					//// RETWEET CHECKS
					try{
						boolean isReTweet = status.isRetweet();
						if(isReTweet == true){
							theMarker.reTweetCount = (int)status.getRetweetCount();
							theMarker.isReTweet = true;
							theMarker.reTweetToID = status.getInReplyToUserId();
							theMarker.replyToScreenName = status.getInReplyToScreenName();
		
							// println("Re tweeting: " + twitterF.getRetweetedByMe(new Paging(1)));
							/// if so, let's see who's been retweeting!
							//*
							// https://api.twitter.com/1/statuses/145140823560957952/retweeted_by.json?count=100&page=1
							// doAPIQuery("https://api.twitter.com/1/statuses/" + status.getId() + "/retweeted_by.json");
							// Twitter twitterRT = new TwitterFactory().getInstance();
							/// IDs ids = twitterF.getRetweetedByIDs(tweetID, new Paging(5));
							Status reTweetStat = status.getRetweetedStatus();
							long reTweetID = reTweetStat.getId();
							IDs ids = twitterF.getRetweetedByIDs(reTweetID, new Paging(5));
							/// println("RETWEETS: " + reTweetStat);
							// List<User> users = twitterRT.getRetweetedBy(status.getId(), new Paging(1));
							/// println(theUser.screenName + " Retweeted " + status.getId() + " " + theUser.reTweetCount + " times " + ids);
							 for (long id : ids.getIDs()) {
					                // println("RETWEETED BY: " + id);
					         }
							// println(status.getId() + " RETWEETED BY: " + twitterF.getRetweetedByIDs((long)status.getId(),  new Paging(1)));
							/// ids = twitter.getRetweetedByIDs(Long.parseLong(args[0]), new Paging(page, 100));
							/// add a re-tweet user
		
							 ///*/
							addRTUser(lo,lt);
		
						} else {
							/// theUser.isReTweet = false;
						}
					} catch (Exception e){
						println("retweet error: rate limited");
					}
					
		
					/// to get a more detailed user profile
					/// check to see if we're under the limit for twitter queries
		
		
					/// if so, do twitter query in separate thread
					// getUserInfo(theUser, newID);
				}

			}

			public void onDeletionNotice(StatusDeletionNotice statusDeletionNotice) {
				// System.out.println("Got a status deletion notice id:" +
				// statusDeletionNotice.getStatusId());
			}

			public void onTrackLimitationNotice(int numberOfLimitedStatuses) {
				// System.out.println("Got track limitation notice:" +
				// numberOfLimitedStatuses);
			}

			public void onScrubGeo(long userId, long upToStatusId) {
				System.out.println("Got scrub_geo event userId:" + userId
						+ " upToStatusId:" + upToStatusId);
			}

			public void onException(Exception ex) {
				ex.printStackTrace();
			}
		};

		/// this adds a user to the re-tweet array
		private void addRTUser(float lo, float lt){
			theMarker = new GPSMarker(lo,lt);
			RTArray.add(theMarker);
			// theUser.userID = userID;
			// theUser.StatusID = tweetID;

		}
		///
		private void doAPIQuery(String theString){
			String thePath = theString;
			String theXML[] = loadStrings(thePath);
			String theXMLString = join(theXML, "").replace("> <", "><");
			println("XML RETWEET: " + theXMLString);


			/* doesn't like json
			try{
				sentimentData = new JSONObject(join(loadStrings(thePath), ""));
				JSONObject tResult = sentimentData.getJSONObject("results");
				// println("Array: " + sentimentArray.toString());

				println("RESULT: " + sentimentData);

			} catch (Exception e){
				println("json error" + e);
			}
			*/

		}
		
		///// DETAILED QUERY /////////////////////
		public void getUserInfo(GPSMarker theMarker, long newID){
			// /*
			int tid = (int)newID;
			String tPath = thePath + tid;
			println("SEARCHING: " + thePath + tid);
			try{
				dbData = new JSONObject(join(loadStrings(tPath), ""));
				// results = dbData.getJSONArray("id");
				// println(results);
				
				//
				println("user name: " + theMarker.userName);
				println("screen name: " + theMarker.screenName);
				println("timestamp: " + theMarker.createdAt);
				println("geoCoords: " + theMarker.geoCoords);
				println("reply to ID: " + theMarker.replyToID);
				println("followers" + theMarker.followersCount);
				println("friends" + theMarker.friendsCount);
				println("favorites: " + theMarker.favoritesCount);
				println("time zone: " + theMarker.timeZone);
				//


				// numResults =  results.length();

			} catch (JSONException e){
				println("json error");
			}
			// */

		}


	///////////////////////////////
	/// this does the globe render
	////////////////////////////////
	private void renderGlobe(){
		// smoothly interpolate camera rotation
		// to new rotation vector based on mouse position
		// each frame we only approach that rotation by 25% (0.25 value)
		
		lights();
		ambientLight(255, 255, 255);
		specular(255, 255, 255);
		// store default 2D coordinate system
		pushMatrix();
		// switch to 3D coordinate system and rotate view based on mouse
		translate(width / 2, height / 2, 0);
		
		///// CHECK FOR MOUSE INPUT TO SET CAMERA
		if (mousePressed) {
			camRot.interpolateToSelf(new Vec3D(mouseY * 0.01f, mouseX * 0.01f, 0),0.25f / currZoom);
			// println("MOUSEX: " + mouseX);
			// println("MouseY " + mouseY);
			theCamX = camRot.x;
			theCamY = camRot.y;
			

		///// CHECK FOR OSC INPUT TO SET CAMERA
		} else if (hasOsc == true) {
			/// map(value, low1, high1, low2, high2)
			/*
			 * oscX0 = val0;
		    	oscY0 = val1;
		    */
			println("rotate dammit!");
			float oscX = map(oscX0, 0, 1, 0, 1024); ///// maps our input to 1024
			float oscY = map(oscY0, 0, 1, 0, 768); ///// 
			camRot.interpolateToSelf(new Vec3D(oscY * 0.01f, oscX * 0.01f, 0),0.25f / currZoom);
			theCamX = camRot.x;
			theCamY = camRot.y;
			// rotateX(camRot.x);
			// rotateY(camRot.y);
			// currZoom += (targetZoom - currZoom) * 0.25;
			//*/
			
		} 
		
		/// check to see if we've moved at all
		if(theOldCamX == theCamX && theOldCamY == theCamY){
			isMoving = false;
			try{
			
			/// saw.setAmp(0f);
			} catch (Exception e){
				// println("can't set amplitude: " + e);
			}
		} else {
			isMoving = true;
			try{
			/// saw.setAmp(1f);
				
			} catch (Exception e){
				// println("can't set amplitude: " + e);
			}
			
		}
		theOldCamX = theCamX;
		theOldCamY = theCamY;
		
		hasOsc = false; ///switch off osc input until we get another osc signal
		float newCamX = map(new Float(theCamX), 0,7,2,4); // narrow the range of vertical camera movement
		
		currZoom += (targetZoom - currZoom) * 0.25;
		// theCamX = newCamX;
		
		rotateX(new Float(theCamX));
		rotateY(new Float(theCamY));

		// apply zoom factor
		scale(currZoom);
		// compute the normalized camera position/direction
		// using the same rotation setting as for the coordinate system
		// this vector is used to figure out if images are visible or not
		// (see below)
		Vec3D camPos = new Vec3D(0, 0, 1).rotateX(new Float(theCamX)).rotateY(new Float(theCamY)); /// changed from cam.x and cam.y
		camPos.normalize();
		noStroke();
		fill(255);
		// use normalized UV texture coordinates (range 0.0 ... 1.0)
		textureMode(NORMAL);
		// draw earth
		gfx.texturedMesh(globe, earthTex, true);
		
		

		////////////////////////////////////////
		// /// SET GPS MARKERS ON THE SPHERE
		
		// check marker position
		for (int i = 0; i < GPSArray.size(); i++) {
			GPSArray.get(i).updateScreenPos(this, camPos);
		}
		// check destroyer position
		theDestroyer.updateScreenPos(this, camPos);
		
		/////////////////////////////////////////
		// switch back to 2D coordinate system
		popMatrix();
		// disable depth testing to ensure anything drawn next
		// will always be on top/infront of the globe
		hint(DISABLE_DEPTH_TEST);
		// draw images centered around the given positions
		imageMode(CENTER);

		// now that they're in position, draw them
		for (int i = 0; i < GPSArray.size(); i++) {
			GPSArray.get(i).drawAsImage(this, IMG_SIZE * currZoom * 0.9f, showLabels);
		}
		// draw the destroyer
		try{
			theDestroyer.drawAsImage(this, IMG_SIZE * currZoom * 0.9f, showLabels);
		} catch (Exception e){
			println("Cant draw destroyer" + e);
		}
		setDestroyer();
		////////////////////////////////////////
		// restore (default) depth testing
		hint(ENABLE_DEPTH_TEST);
	}
	
	//// draw lines between points ////////
	public void drawLines(){
		float prevX1;
		float prevY1;
		float prevX2;
		float prevY2;
		for (int i = 0; i < GPSArray.size() - 1; i++) {
			GPSMarker tMark = GPSArray.get(i);
			GPSMarker tMark2 = GPSArray.get(i + 1);
			
			prevX1 = tMark.theLat;
			prevY1 = tMark.theLong;
			prevX2 = tMark2.theLat;
			prevY2 = tMark2.theLong;
			
			stroke(255);
			strokeWeight(5);
			line(prevX1, prevY1, prevX2, prevY2);

		}
		/*
		 * {"lat":"33.590897","long":"-112.3311","date":"2011-12-08 12:25:57"},
{"lat":"48.199997","long":"16.3667","date":"2011-12-08 12:26:12"},
*/
		line(new Float(33.590897), new Float(-112.3311), new Float(48.199997), new Float(16.3667));
		
	}

	////////////////////////////////
	///////// SET CURSOR/DESTROYER 
	////////////////////////////////////

	
	public void setDestroyer() {
		// convert cur mouse pos to lat and long
		// map(value, low1, high1, low2, high2)
		
		
		if (hasOsc == true){
			theLat = map(oscY1, 1, 0, 0, 90);
			theLong = map(oscX1, 0, 1, -180, 180);
		} else {
			// theLat = map(mouseY, 600, 0, 0, 90);
			// theLong = map(mouseX, 200, 800, -180, 180);
		
		}
		if (!mousePressed) {
			theLat = map(mouseY, 0, 1024, 0, 90);
			theLong = map(mouseX, 0, 768, -180, 180);
		}
		theDestroyer.setSpherePosition(theLong, theLat); //sends lat and long converted from default
		theDestroyer.computePosOnSphere(EARTH_RADIUS);
		
		
		//// CHECK FOR INTERSECTION with other markers
		for(int i=0; i<GPSArray.size(); i++){
		// for(int i=0; i<2; i++){
			float dlat = theDestroyer.theLat;
			float dlong = theDestroyer.theLong;
			float mlat = GPSArray.get(i).theLat;
			float mlong = GPSArray.get(i).theLong;
			// println("dlat " + dlat + " mlat: " + mlat);
			// println("dlong " + dlong + " mlong: " + mlong);
			//// check to see if the destroyer is within the range of the current lat and long
			if (dlat >= (mlat -3) && dlat <= (mlat + 3) &&  dlong <= (mlong + 3) && dlong >= (mlong - 3)){
				
				GPSArray.get(i).doHit(); //// marker hit
			} else {
				/// println(">>");
			}
		
		}

	}

	/////////////////////////////////
	//////// OSC INPUT //////////////
	/////////////////////////////////
	public void oscEvent(OscMessage theOscMessage) {
		  /* print the address pattern of the received OscMessage */
		String addr = theOscMessage.addrPattern();
		/// set up floats for the 2 value depths of the input
		float val0 = theOscMessage.get(0).floatValue();
		float val1 = theOscMessage.get(1).floatValue();
		if(addr.indexOf("/3/xy1") !=-1){ // ){
			hasOsc = true;
			println(hasOsc);
		}
		if(addr.indexOf("/3/xy2") !=-1){ // ){
			hasOsc = true;
			println(hasOsc);
		}
		if(addr.indexOf("/3/xy1") !=-1){ // ){
			hasOsc = true;
			println(hasOsc);
		}
		if(addr.indexOf("/3/xy2") !=-1){ // ){
			hasOsc = true;
			println(hasOsc);
		}
		if(addr.indexOf("/1/xy1") !=-1){ // ){
			hasOsc = true;
			println(hasOsc);
		}
		if(addr.indexOf("/1/xy2") !=-1){ // ){
			hasOsc = true;
			println(hasOsc);
		}
		if(addr.indexOf("/TwitterPlanet/xy1") !=-1){ // ){
			hasOsc = true;
			println(hasOsc);
		}
		if(addr.indexOf("/TwitterPlanet/xy2") !=-1){ // ){
			hasOsc = true;
			println(hasOsc);
		}
		try{
		  // print("### received an osc message.");
		  // println(" typetag: "+theOscMessage.typetag())
		 println("tag type: "+theOscMessage.typetag());
		 println("addr type: " + theOscMessage.addrPattern()); // it was lowercase in the documentation
		 // println(" VALUE 0: "+theOscMessage.get(0).floatValue());
		   if(addr.equals("/1/fader1")){ 
			   println("v1 " + val0);
		   	} 
		    else if(addr.equals("/1/fader2")){ 
		    	println("v2 " + val0);
		    }
		    else if(addr.equals("/TwitterPlanet/xy1")){ 
		    	println("good job! 1x1 " + val0);
		    	println("1y1 " + val1);
		    	oscX0 = val0;
		    	oscY0 = val1;
		    }
		    else if(addr.equals("/TwitterPlanet/xy2")){ 
		    	println("good job! 1x2 " + val0);
		    	println("good job! 1y2 " + val1);
		    	oscX1 = val0;
		    	oscY1 = val1;
		    }
		    else if(addr.equals("/3/xy1")){ 
		    	println("3x1 " + val0);
		    	println("3y1 " + val1);
		    	oscX0 = val0;
		    	oscY0 = val1;
		    }
		    else if(addr.equals("/1/xy1")){ 
		    	println("good job! 1x1 " + val0);
		    	println("1y1 " + val1);
		    	oscX0 = val0;
		    	oscY0 = val1;
		    }
		    else if(addr.equals("/1/xy2")){ 
		    	println("good job! 1x2 " + val0);
		    	println("good job! 1y2 " + val1);
		    	oscX1 = val0;
		    	oscY1 = val1;
		    }
		    else if(addr.equals("/3/xy2")){ 
		    	println("3x2 " + val0);
		    	println("312 " + val1);
		    	oscX1 = val0;
		    	oscY1 = val1;
		    }
		    else if(addr.equals("/1/fader3")){ 
		    	println("v3 " + val0);
		    }
		    else if(addr.equals("/1/fader4")){ 
		    	println("v4 " + val0);
		    }
		    else if(addr.equals("/1/fader5")){ 
		    	//v_fader5 = val; 
		    }
		    else if(addr.equals("/1/toggle1")){ 
		    	
		    }
		    else if(addr.equals("/1/toggle2")){ 
		    	
		    }
		    else if(addr.equals("/1/toggle3")){ 
		    	
		    }
		    else if(addr.equals("/1/toggle4")){ 
		    	
		    }
		} catch (Exception e){
			println(" osc error: " + e);
		}
		  
		  
		  /// control x and y globe
		  
		  /// control x and y destroyer
	}
	//////// keyboard input
	public void keyPressed() {
		if (key == '-') {
			targetZoom = max(targetZoom - 0.1f, 0.5f);
		}
		if (key == '=') {
			targetZoom = min(targetZoom + 0.1f, 1.9f);
		}
		if (key == 'l') {
			showLabels = !showLabels;
		}
		if(key == 'm'){
			println("init Midi");
			midiControl.initMidi();
			midiControl.sendMidiNote();
		}
		if(key == 't'){
			println("test Midi");
			// midiControl.initMidi();
			midiControl.sendMidiNote();
		}
		/// this does nothing
		if (key == 'd') {

			
		}
		if (key == 'f') {
			
		}
	}
	

	// /
}