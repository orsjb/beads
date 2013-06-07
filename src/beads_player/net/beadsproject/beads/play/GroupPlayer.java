package net.beadsproject.beads.play;

import java.util.ArrayList;
import java.util.List;
import net.beadsproject.beads.core.Bead;
import net.beadsproject.beads.ugens.Clock;

/**
 * The Class Player. Handles playing of groups. Does not know about GUI stuff.
 */
public class GroupPlayer {

	private SongGroup currentGroup = null;
	private SongGroup nextGroup;
	private ArrayList<SongPart> playingParts;
	private boolean withFadeOut;
	
	public GroupPlayer(Environment e) {
		playingParts = new ArrayList<SongPart>();
		withFadeOut = true;
		//Potential danger that if SongParts are also listening to this clock
		//they might miss out on the first 'beat'.
		//This is OK as long as the Player is the first thing to listen to the clock.
		//if it is then it will switch on the SongParts, and then immediately after that
		//the clock will trigger them.
		//However, it will be a problem if one were to change environments after initial startup.
		e.pathways.get("master clock").add(new Bead() {
			public void messageReceived(Bead message) {
				Clock c = (Clock)message;
				if(c.isBeat()) {
					if(nextGroup != null) {
						if(c.getBeatCount() % nextGroup.getFlipQuantisation() == 0) {
							doPlayGroupNow(nextGroup);
						}
					}
				}
			}
		});
	}
	
	public void playGroup(SongGroup newGroup) {
		playGroup(newGroup, true);
	}
	
	private void playGroup(SongGroup newGroup, boolean withFadeOut) {
		this.withFadeOut = withFadeOut;
		if(newGroup.getFlipQuantisation() < 1) {
			doPlayGroupNow(newGroup);
		} else {
			nextGroup = newGroup;
		}
	}
	
	public void playGroupNoFadeOut(SongGroup newGroup) {
		playGroup(newGroup, false);
	}
	
	private void doPlayGroupNow(SongGroup newGroup) {
//		System.out.println("Play group");
		if(currentGroup != null) {
			ArrayList<SongPart> incoming = new ArrayList<SongPart>();
			ArrayList<SongPart> outgoing = new ArrayList<SongPart>();
			for(SongPart p : currentGroup.parts()) {
				if(!newGroup.parts().contains(p)) {
					outgoing.add(p);
				}
			}
			for(SongPart p : newGroup.parts()) {
				if(!currentGroup.parts().contains(p)) {
					incoming.add(p);
				}
			}
			endPlayingList(outgoing);
			beginPlayingList(incoming);
		} else {
			beginPlayingList(newGroup.parts());
		}
		currentGroup = newGroup;
		nextGroup = null;
	}

	public void stop() {
		if(currentGroup != null) {
			endPlayingList(currentGroup.parts());
			currentGroup = null;
		}
	}
	
	private void beginPlayingList(List<SongPart> list) {
		for(SongPart p : list) {
			beginPlayingPart(p);
		}
	}
	
	private void beginPlayingPart(SongPart p) {
		p.enter();
		p.pause(false);
		playingParts.add(p);
	}
	
	private void endPlayingList(List<SongPart> list) {
		for(SongPart p : list) {
			endPlayingPart(p);
		}
	}
	
	private void endPlayingPart(SongPart p) {
		if(withFadeOut) {
			p.exit();
		} else {
			p.pause(true);
		}
		playingParts.remove(p);
	}
	
	public SongGroup getCurrentGroup() {
		if(nextGroup != null) return nextGroup;
		else return currentGroup;
	}

	public void setCurrentGroup(SongGroup currentGroup) {
		this.currentGroup = currentGroup;
	}
		
	public void notifyCurrentGroupUpdated() {
		if(currentGroup != null) {
			for(SongPart p : currentGroup.parts()) {
				if(!playingParts.contains(p)) {
					beginPlayingPart(p);
				}
			}
			for(SongPart p : (List<SongPart>)playingParts.clone()) {
				if(!currentGroup.contains(p)) {
					endPlayingPart(p);
				}
			}
		}
	}

}
