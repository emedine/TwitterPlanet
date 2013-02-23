package src;

import processing.core.PApplet;
import toxi.geom.Vec2D;
import toxi.geom.Vec3D;
import toxi.math.MathUtils;


public class GPSMarker {
	protected Vec2D gps;

	protected Vec3D pos;
	protected Vec2D screenPos = new Vec2D();
	protected boolean isVisible;
	
	public float theLong;
	public float theLat;
	
	int fillColor1;
	int fillColor2;
	
	int size1 = 12;
	int size2 = 6;

	MidiControl midiControl;
	
	//
	DataProfile dataProfile;
	//
	PApplet pApp;
	
	public int tAlpha = 125;

	// / pass the lat and long into the GPS marker
	public GPSMarker(float tLong, float tLat) {
	
		dataProfile = DataProfile.getInstance();
		pApp = dataProfile.pApp;
		theLong = tLong;
		theLat = tLat;
		
		pApp.println(theLong);
		// set the gps data
		this.gps = new Vec2D(theLong, theLat);

		
		/// midi control
		midiControl = MidiControl.getInstance();
		//
		/*
		int[] numbers = { 90, 150, 30 };
		int a = numbers[0] + numbers[1]; // Sets variable a to 240
		int b = numbers[1] + numbers[2]; // Sets variable b to 180
		*/
		//  20,30,40,62,63,60,61,64,46

	}

	/**
	 * Computes the position of the image on a sphere of the given radius.
	 * 
	 * @param earthRadius
	 * @return position in cartesian space
	 */
	public Vec3D computePosOnSphere(int earthRadius) {
		// build a spherical position from lat/lon and convert into XYZ space
		pos = new Vec3D(earthRadius, MathUtils.radians(gps.x) + MathUtils.PI, MathUtils.radians(gps.y)).toCartesian();
		return pos;
	}
	public void doHit(){
		// tAlpha = 255;
	    fillColor1 = 255;
	    size1 = 27;
	    
	    /// do sound
	    try{
	    	
	    	//// normalize the range of notes  between 0 and -180
	    	int SoundArray = midiControl.SoundArray.length;
			float tNote = pApp.map(theLong, -180f, 150f, 0f, new Float(SoundArray));
			int iNote = (int)tNote;
			pApp.println("longhit: " + theLong + " " + iNote + " " + iNote);
	    	midiControl.sendMultiNote(iNote);
	    	// theAudio.playAudio();
	    	
	    } catch (Exception e){
	    	pApp.println("no audio data");
	    }
	}
	/**
	 * Draws image at computed position in space.
	 * 
	 * @param app
	 *            parent applet
	 * @param size
	 *            image size
	 * @param showLabel
	 *            true, to draw image label
	 */
	public void drawAsImage(PApplet app, float size, boolean showLabel) {
		
		/// this resets the color gradually if the marker has been hit
		if(fillColor1 <= 0){
			fillColor1 = 0;
			size1 = 12;
		} else {
			fillColor1 -=20;
			size1 -=1;
			// tAlpha ++;
			
		}
		if (isVisible) {
			// fillColor1 = color((int) (rnd1*255), (rnd2*255),(rnd*255)); // color((int) (rnd1*255), (rnd2*255),(rnd*255)); 
			/// fillColor1 = int((int)(rnd1*255));
			
			// app.noStroke();
			app.fill(255, fillColor1, 0, tAlpha-75);
			app.ellipse(screenPos.x, screenPos.y, size1, size1);
			app.fill(255, fillColor1, 0, tAlpha);
			app.ellipse(screenPos.x, screenPos.y, size2, size2);
		}

	}
	
	public void updateScreenPos(PApplet app, Vec3D camPos) {
		screenPos.set(app.screenX(pos.x, pos.y, pos.z), app.screenY(pos.x, pos.y, pos.z));

		float dot = pos.getNormalized().dot(camPos);
		
		// if it's 'small', then it's 'behind' the globe
		isVisible = dot > 0.66;
	}
	
	///
}
