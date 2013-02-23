package src;

import processing.core.PApplet;
import ddf.minim.*;

public class AudioControl {
	private static AudioControl instance = null;
	
	Minim minim;
	AudioPlayer player;
	
	// 
	DataProfile theDataProfile;
	PApplet pApp;
	
	
	protected AudioControl() {
	      // Exists only to defeat instantiation.
	}
	public static AudioControl getInstance() {
	      if(instance == null) {
	         instance = new AudioControl();
	      }
	      return instance;
	}
	public void initAudio(){
	
	theDataProfile = theDataProfile.getInstance();
	pApp = theDataProfile.pApp;
	minim = new Minim(pApp);
	// load a file, give the AudioPlayer buffers that are 2048 samples long
	try{
	player = minim.loadFile("../data/powerup.mp3", 2048);
	} catch (Exception e){
		
	}
	// play the file
	
	}
	
	public void playAudio(){
		  player.play();
		
	}
	
	
	
	
}
