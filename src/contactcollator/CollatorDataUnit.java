package contactcollator;

import java.util.ArrayList;
import java.util.ListIterator;

import PamController.PamController;
import PamguardMVC.PamDataBlock;
import PamguardMVC.PamDataUnit;
import PamguardMVC.RawDataHolder;
import PamguardMVC.RawDataTransforms;
import clipgenerator.ClipDataUnit;
import contactcollator.bearings.BearingSummary;
import contactcollator.bearings.BearingSummaryLocalisation;
import contactcollator.bearings.HeadingHistogram;
import contactcollator.trigger.CollatorTriggerData;

/**
 * Data output from Contact Collator. The datablock may contain these data units from multiple
 * output streams, each having a different sample rate which may make things a little confusing.
 * <p>Mostly this class is just a wrapper around the ClipDataUnit, so most data fields are accessed
 * through getters in the super class.  
 * @author dg50
 *
 */
public class CollatorDataUnit extends ClipDataUnit implements RawDataHolder {
		
	private CollatorTriggerData triggerData;
	
	private RawDataTransforms rawDataTransforms;

	private BearingSummaryLocalisation bearingSummaryLocalisation;

	private String streamName;
	
	private HeadingHistogram headingHistogram;
	
	private ArrayList<Long> triggerUTCs;

	/**
	 * Real time constructor
	 * @param timeMilliseconds
	 * @param channelBitmap
	 * @param startSample
	 * @param sampleRate
	 * @param durationSamples
	 * @param triggerData
	 * @param wavData
	 */
	public CollatorDataUnit(long timeMilliseconds, int channelBitmap, long startSample, float sampleRate, int durationSamples, CollatorTriggerData triggerData, String streamName, double[][] wavData) {
		super(timeMilliseconds, triggerData.getStartTime(), startSample, durationSamples, channelBitmap, null, triggerData.getTriggerName(), wavData, sampleRate);
		this.triggerData = triggerData;
		this.streamName = streamName;
	}

	/**
	 * For use reading back from binary files. 
	 * @param timeMilliseconds
	 * @param channelBitmap
	 * @param startSample
	 * @param sampleRate
	 * @param durationSamples
	 * @param triggerName
	 * @param triggerTime
	 * @param wavData
	 */
	public CollatorDataUnit(long timeMilliseconds, int channelBitmap, long startSample, float sampleRate, int durationSamples, String triggerName, long triggerTime, String streamName, double[][] wavData) {
		super(timeMilliseconds, triggerTime, startSample, durationSamples, channelBitmap, null, triggerName, wavData, sampleRate);
		this.streamName = streamName;
	}

	@Override
	public RawDataTransforms getDataTransforms() {
		if (rawDataTransforms == null) {
			rawDataTransforms = new RawDataTransforms(this, this);
		}
		return rawDataTransforms;
	}

	/**
	 * Set summary bearing information for the contact. 
	 * @param bearingSummaryLocalisation
	 */
	public void setBearingSummary(BearingSummaryLocalisation bearingSummaryLocalisation) {
		this.bearingSummaryLocalisation = bearingSummaryLocalisation;
		this.setLocalisation(bearingSummaryLocalisation);
	}

	/**
	 * Get bearing summary information for the contact in the form of a full localisation 
	 * object (instance of AbstractLocalisation, so can be used throughout PAMGuard). 
	 * @return the bearingSummaryLocalisation
	 */
	public BearingSummaryLocalisation getBearingSummaryLocalisation() {
		return bearingSummaryLocalisation;
	}
	
	/**
	 * Get bearing summary information for the contact
	 * @return the bearingSummary
	 */
	public BearingSummary getBearingSummary() {
		if (bearingSummaryLocalisation == null) {
			return null;
		}
		else {
			return bearingSummaryLocalisation.getBearingSummary();
		}
	}

	/**
	 * Get the data that were used to trigger the contact. <p>
	 * Contains info on the detector and which data units fed into the trigger. 
	 * @return the triggerData
	 */
	public CollatorTriggerData getTriggerData() {
		return triggerData;
	}
	
	public void setTriggerData(CollatorTriggerData triggerData) {
		this.triggerData = triggerData;
	}

	/**
	 * @return the streamName
	 */
	public String getStreamName() {
		return streamName;
	}

	/**
	 * @return the headingHistogram
	 */
	public HeadingHistogram getHeadingHistogram() {
		return headingHistogram;
	}

	/**
	 * @param headingHistogram the headingHistogram to set
	 */
	public void setHeadingHistogram(HeadingHistogram headingHistogram) {
		this.headingHistogram = headingHistogram;
	}
	
	public void setTriggerUTCs(ArrayList<Long> trigTimes) {
		this.triggerUTCs = trigTimes;
	}
	
	public CollatorTriggerData findTriggerData() {
		if (triggerData != null) {
			return triggerData;
		}
		
		String trigName = this.triggerName;
		long trigMillis = this.triggerMilliseconds;
//		long startMillis = clipDataUnit.getTimeMilliseconds();
		PamDataBlock<PamDataUnit> dataBlock = findTriggerDataBlock(trigName);
		if (dataBlock == null) {
			return null;
		}
		return triggerData = findTriggerData2(triggerUTCs,dataBlock, 20);
	}
	
	private CollatorTriggerData findTriggerData2(ArrayList<Long> trigTimes, PamDataBlock<PamDataUnit> dataBlock, int timeJitter) {
		long t1;
		long t2;
		
		ArrayList<PamDataUnit> detectorData = new ArrayList<PamDataUnit>();

		if(trigTimes==null) {
			return null;
		}
		
		synchronized (dataBlock.getSynchLock()) {
			ListIterator<PamDataUnit> iter = dataBlock.getListIterator(PamDataBlock.ITERATOR_END);
			while (iter.hasPrevious()) {
				
				for(long nextTrigTime:trigTimes) {
					t1 = nextTrigTime - timeJitter;
					t2 = nextTrigTime + timeJitter;
					PamDataUnit trigUnit = iter.previous();
					long trigTime = trigUnit.getTimeMilliseconds();
					if (trigTime >= t1 && trigTime <= t2 && (trigUnit.getChannelBitmap() & this.getChannelBitmap()) != 0) {
						detectorData.add(trigUnit);
					}
				}
				
				
			}
			
		}
		return new CollatorTriggerData(min(trigTimes),max(trigTimes),dataBlock.getLongDataName(),detectorData);

	}
	
	private String lastFoundName;
	private PamDataBlock<PamDataUnit> lastFoundBlock;
	
	public PamDataBlock<PamDataUnit> findTriggerDataBlock(String dataName){
		if (dataName == null) {
			return null;
		}
		if (dataName.equals(lastFoundName)) {
			return lastFoundBlock;
		}
		PamDataBlock<PamDataUnit> dataBlock = PamController.getInstance().getDetectorDataBlock(dataName);
		if (dataBlock == null) {
			return null;
		}
		lastFoundName = new String(dataName);
		lastFoundBlock = dataBlock;
		return dataBlock;
	}
	
	private long min(ArrayList<Long> arr) {
		long min=arr.get(0);
		for(long el:arr) {
			if(el<min) {
				min=el;
			}
		}
		return min;
	}
	
	private long max(ArrayList<Long> arr) {
		long max=arr.get(0);
		for(long el:arr) {
			if(el>max) {
				max=el;
			}
		}
		return max;
	}

}
