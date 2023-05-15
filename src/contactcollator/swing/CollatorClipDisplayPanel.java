package contactcollator.swing;

import java.awt.event.MouseEvent;

import PamguardMVC.PamDataBlock;
import PamguardMVC.PamProcess;
import clipgenerator.ClipDataUnit;
import clipgenerator.ClipProcess;
import clipgenerator.clipDisplay.ClipDisplayPanel;
import clipgenerator.clipDisplay.ClipDisplayParent;
import clipgenerator.clipDisplay.ClipDisplayUnit;
import contactcollator.CollatorDataUnit;
import contactcollator.CollatorStreamProcess;
import soundPlayback.ClipPlayback;

public class CollatorClipDisplayPanel extends ClipDisplayPanel{
	
	CollatorStreamProcess collatorStreamProcess;

	public CollatorClipDisplayPanel(ClipDisplayParent clipDisplayParent) {
		super(clipDisplayParent);
		this.collatorStreamProcess = (CollatorStreamProcess) clipDisplayParent;
		this.setSampleRate(collatorStreamProcess.getParameterSet().outputSampleRate);
		
	}
	
	@Override
	protected boolean shouldShowClip(ClipDisplayUnit dataUnit) {
		
		if(dataUnit.getClipDataUnit() instanceof CollatorDataUnit) {
			CollatorDataUnit du = (CollatorDataUnit) dataUnit.getClipDataUnit();
			if(du.getStreamName().equals(collatorStreamProcess.getSetName())) {
				return true;
			}
		}
		return false;
	}
	
	
	/**
	 * A notification from a ClipDisplayUnit
	 * that the mouse was clicked
	 */
	@Override
	public void mouseClicked(MouseEvent e, ClipDisplayUnit clipDisplayUnit) {
//		unitsMouse.mouseClicked(null);
		if (this.getClipDisplayMarker() == null) {
			return;
		}
		this.getClipDisplayMarker().mouseClicked(e, clipDisplayUnit);
		if (e.getClickCount() == 2) {
			playCollatedClip(clipDisplayUnit.getClipDataUnit());
		}
	}
	
	private void playCollatedClip(ClipDataUnit clipDataUnit) {
		if (clipDataUnit == null) {
			return;
		}
		PamDataBlock dataBlock = clipDataUnit.getParentDataBlock();
		if (dataBlock == null) return;
		double[][] playClip = clipDataUnit.getRawData();
		int samplePlay = collatorStreamProcess.getParameterSet().outputSampleRate;
		if(samplePlay==250000) {
			playClip = quickDecimator(playClip,250000,50000);
			samplePlay = 50000;
		}
		ClipPlayback.getInstance().playClip(playClip, samplePlay, true);
	}
	
	private double[][] quickDecimator(double[][] fromWav,int fromFs, int toFs){
		int decimateFactor = fromFs/toFs;
		int newLength = fromWav[0].length/decimateFactor;
		double[][] toWav = new double[fromWav.length][newLength];
		for(int chanIdx=0;chanIdx<fromWav.length;chanIdx++) {
			int toSampleIdx=0;
			for(int fromSampleIdx=0;fromSampleIdx<fromWav[chanIdx].length;fromSampleIdx++) {
				if(fromSampleIdx%decimateFactor==0) {
					if(toSampleIdx>newLength) {
						break;
					}
					toWav[chanIdx][toSampleIdx]=fromWav[chanIdx][fromSampleIdx];
					toSampleIdx++;
				}
			}
		}
		return toWav;
		
	}
	
	
	
	

}
