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

/* SOUND MACHINE BUILD BY ERIC MEDINE
 * added osc input 2/29/2012
 * added minim libraries 3/2/12
 * added midi libraries, electribe and sequencer control 3/15 12
 */

package src;

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

/// toxiclib 
import toxi.geom.Vec3D;
import toxi.geom.mesh.Mesh3D;
import toxi.geom.mesh.SphereFunction;
import toxi.geom.mesh.SurfaceMeshBuilder;
import toxi.processing.ToxiclibsSupport;

// minim libraries
import ddf.minim.*;
// import ddf.minim.signals.*;


/*
 * <ul>
 * <li>Move mouse to rotate view</li>
 * <li>Press '-' / '=' to adjust zoom</li>
 * </ul>
 * </p>
 */
@SuppressWarnings("serial")
public class SoundPlanet extends PApplet {

	//Radius of our globe
	private static final int EARTH_RADIUS = 300;

	/// Image size in pixels for rendering
	private static final int IMG_SIZE = 32;

	/**
	 * Main entry point to run as application
	 */
///*
	public static void main(String[] args) {
		PApplet.main(new String[] { "src.SoundPlanet" });
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
	// / lat and long arrays
	float[] latArray;
	float[] longArray;
	int LatLongLength;

	// / PApplet stuff
	PApplet pApp;
	// / init marker array
	GPSMarker[] theMarker;
	
	/// OSC objects
	OscP5 oscP5;
	String oscMess;
	boolean hasOsc;
	float oscX0;
	float oscY0;
	float oscX1;
	float oscY1;
	
	/// minim audio objects
	/// sine waves
	/**/
	Minim minim;
	AudioOutput out;
	SawWave saw;
	
	// AudioControl theAudio;
	
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
		size(800, 600, OPENGL);

		// load earth texture image
		// earthTex = loadImage("../data/earth_4096.jpg"); // earth_outlines.png");
		earthTex = loadImage("../data/earth_outlines.png");

		// build a sphere mesh with texture coordinates
		// sphere resolution set to 36 vertices
		globe = new SurfaceMeshBuilder(new SphereFunction()).createMesh(null, 36, EARTH_RADIUS);
		// compute surface orientation vectors for each mesh vertex
		// this is important for lighting calculations when rendering the mesh
		globe.computeVertexNormals();

		// setup helper class (assign to work with this applet)
		gfx = new ToxiclibsSupport(this);
		
		/// add the OSC listener object
		// oscP5 = new OscP5(this,8000);

		textFont(createFont("SansSerif", 10));
		// initPoly();
		
		// init our instance of the data profile
		dataProfile =  DataProfile.getInstance();
		// set the papplet so we can get to it from any class
		// instead of passing it back and forth like a potato
		dataProfile.pApp = this; 
		
		/// set up midi profiles
		minim = new Minim(this);
		// init the audio singleton
		// theAudio = theAudio.getInstance();
		// theAudio.initAudio();
		
		/// midi control
		midiControl = MidiControl.getInstance();
		midiControl.initMidi();
		
		initLocations();

	}
	
	public void draw() {
		midiControl.checkMidiCounter();
		// smoothly interpolate camera rotation
		// to new rotation vector based on mouse position
		// each frame we only approach that rotation by 25% (0.25 value)
		
		background(0);
		lights();
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
			
		//// do pitch changes
			midiControl.doPitchChange(new Float(theCamY) * 10);
				
			
		///// CHECK FOR OSC INPUT TO SET CAMERA
		} else if (hasOsc == true) {
			/// map(value, low1, high1, low2, high2)
			///*
			float oscX = map(oscX0, 0, 1, 0, 1024); ///// maps our input to 800x600
			float oscY = map(oscY0, 0, 1, 0, 7); ///// 
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
			
			saw.setAmp(0f);
			} catch (Exception e){
				// println("can't set amplitude: " + e);
			}
		} else {
			isMoving = true;
			try{
			saw.setAmp(1f);
				
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
		for (int i = 0; i < LatLongLength; i++) {
			theMarker[i].updateScreenPos(this, camPos);
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
		for (int i = 0; i < LatLongLength; i++) {
			theMarker[i].drawAsImage(this, IMG_SIZE * currZoom * 0.9f, showLabels);
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
		
		
		
		///// AUDIO STUFF
		if(doAudio == true){
			/*
		 // with portamento on the frequency will change smoothly
			// convert cam value to int
			int newX = (int)theCamX;
			int newY = (int)theCamY;
			// map(value, low1, high1, low2, high2)
		  float freq = map(new Float(theCamX), 2.00f, 4.00f, 60f, 1000f);
		  saw.setFreq(freq);
		  // pan always changes smoothly to avoid crackles getting into the signal
		  // note that we could call setPan on out, instead of on sine
		  // this would sound the same, but the waveforms in out would not reflect the panning
		  // float sampleRate = map(mouseX, 0, width, 0, 1);
		 // float peaks = map(mouseY, 0, height, 1, 20);
		  float pan = map(newY, 0, width, -1, 1);
		  saw.setPan(pan);
		  */
		}

	}


	

	// //init the location array with ip addresses from the DB
	public void initLocations() {
		try {
			dbData = new JSONObject(join(loadStrings(jsonString), ""));
			// println("results: " + result);
			results = dbData.getJSONArray("latlong_data");
			// total = dbData.getInt("total");
			// / set length of arrays
			LatLongLength = results.length();
			// init our marker handler
			theMarker = new GPSMarker[LatLongLength];
			latArray = new float[results.length()];
			longArray = new float[results.length()];

			// println("LENGTH: " + results.length());
			
			// // let's print this mother out
			for (int i = 0; i < LatLongLength; i++) {

				String theLat = results.getJSONObject(i).getString("lat");
				String theLong = results.getJSONObject(i).getString("long");
				// println(results.getJSONObject(i).getString("lat"));
				float lt = new Float(theLat);
				float lo = new Float(theLong);
				latArray[i] = lt;
				longArray[i] = lo;
				
			}

		} catch (JSONException e) {
			println(e);
		}
		initGPSMarkers();

	}
////set up markers and destroyer
	public void initGPSMarkers() {
		
		/// set up markers
		for (int i = 0; i < LatLongLength; i++) {
			// / add a new GPS marker, set its lat and long arrays
			// / and compute its position
			theMarker[i] = new GPSMarker(longArray[i], latArray[i]);
			theMarker[i].computePosOnSphere(EARTH_RADIUS);

		}
		
		//// init the destroyer
		try {
		theDestroyer = new Destroyer(longArray[0], latArray[0]);
		theDestroyer.computePosOnSphere(EARTH_RADIUS);
		
		} catch(Exception e){
			println(e);
		}
	}

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
		for(int i=0; i<theMarker.length; i++){
		// for(int i=0; i<2; i++){
			float dlat = theDestroyer.theLat;
			float dlong = theDestroyer.theLong;
			float mlat = theMarker[i].theLat;
			float mlong = theMarker[i].theLong;
			// println("dlat " + dlat + " mlat: " + mlat);
			// println("dlong " + dlong + " mlong: " + mlong);
			//// check to see if the destroyer is within the range of the current lat and long
			if (dlat >= (mlat -3) && dlat <= (mlat + 3) &&  dlong <= (mlong + 3) && dlong >= (mlong - 3)){
				
				theMarker[i].doHit(); //// marker hit
			} else {
				/// println(">>");
			}
		
		}

	}

	
	//////// osc input
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
		try{
		  print("### received an osc message.");
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
		    else if(addr.equals("/3/xy1")){ 
		    	println("3x1 " + val0);
		    	println("3y1 " + val1);
		    	oscX0 = val0;
		    	oscY0 = val1;
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
			doAudio = true;
			startOscillator();
			
		}
		if (key == 'f') {
			doAudio = false;
			stopOscillator();
		}
	}
	
///// start oscillator
	public void startOscillator()
	{
		try{
		// get a line out from Minim, default sample rate is 44100, bit depth is 16
		  out = minim.getLineOut(Minim.STEREO, 2048);
		 
		// create a sine wave Oscillator, set to 440 Hz, at 0.5 amplitude, sample rate to match the line out
		  saw = new SawWave(new Float(440), new Float(0.2), out.sampleRate());
		  // set the portamento speed on the oscillator to 200 milliseconds
		  saw.portamento(50);
		  // add the oscillator to the line out
		  out.addSignal(saw);
		  
		  // super.stop();
		} catch (Exception e){
			println("can't start!");
		}
	}
	
	///// stop oscillator
	public void stopOscillator()
	{
		try{
		  out.close();
		  minim.stop();
		  
		  // super.stop();
		} catch (Exception e){
			println("can't stop!");
		}
	}
	
	// /
}