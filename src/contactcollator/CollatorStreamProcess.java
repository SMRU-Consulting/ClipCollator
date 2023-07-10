package contactcollator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import PamController.PamController;
import PamDetection.RawDataUnit;
import PamUtils.PamUtils;
import PamView.paneloverlay.overlaymark.OverlayMarkObservers;
import PamguardMVC.PamDataBlock;
import PamguardMVC.PamDataUnit;
import PamguardMVC.PamObservable;
import PamguardMVC.PamObserver;
import PamguardMVC.PamProcess;
import PamguardMVC.PamRawDataBlock;
import PamguardMVC.RawDataUnavailableException;
import PamguardMVC.dataSelector.DataSelector;
import clickDetector.ClickDetection;
import clipgenerator.ClipDataUnit;
import clipgenerator.ClipDisplayDataBlock;
import clipgenerator.clipDisplay.ClipDisplayDecorations;
import clipgenerator.clipDisplay.ClipDisplayParent;
import clipgenerator.clipDisplay.ClipDisplayUnit;
import contactcollator.bearings.BearingSummariser;
import contactcollator.bearings.BearingSummary;
import contactcollator.bearings.BearingSummaryLocalisation;
import contactcollator.bearings.HeadingHistogram;
import contactcollator.trigger.CollatorRateFilter;
import contactcollator.trigger.CollatorTrigger;
import contactcollator.trigger.CollatorTriggerData;
import contactcollator.trigger.CountingTrigger;
import decimator.DecimatorParams;
import decimator.DecimatorProcessW;
import decimator.DecimatorWorker;

public class CollatorStreamProcess extends PamProcess implements ClipDisplayParent{

	private CollatorControl collatorControl;
	private CollatorDataBlock collatorBlock;
	private CollatorParamSet parameterSet;
	private RawDataObserver rawDataObserver;
	private PamRawDataBlock rawDataBlock;
	private PamRawDataBlock rawDataCopy;
	private PamDataBlock detectorDatablock;
	private DataSelector detectionDataSelector;
	private CollatorTrigger collatorTrigger;
	private CollatorRateFilter collatorRateFilter;
	private DecimatorWorker decimator;
	private BearingSummariser bearingSummariser;
	
	private HeadingHistogram headingHistogram;

	public CollatorStreamProcess(CollatorControl collatorControl, CollatorDataBlock collatorBlock, CollatorParamSet parameterSet) {
		super(collatorControl, null);
		this.collatorControl = collatorControl;
		this.collatorBlock = collatorBlock;
		this.parameterSet = parameterSet;
		rawDataObserver = new RawDataObserver();
		bearingSummariser = new BearingSummariser();
		/*
		 *  make an internal copy of the data, it will not use much memory and means we don't have to
		 *  lock the main raw data block for too long while preparing output 
		 */		
		rawDataCopy = new PamRawDataBlock("internal copy", this, 0, sampleRate);
		collatorRateFilter = new CollatorRateFilter();
		
		headingHistogram = new HeadingHistogram(24, true);
		
		
	}

	@Override
	public void pamStart() {
		rawDataObserver.pause=false;

	}

	@Override
	public void pamStop() {
		rawDataObserver.pause=true;

	}
	
	@Override
	public void newData(PamObservable o, PamDataUnit dataUnit) {
		// see if we actually want it using the data selector
		if(PamController.getInstance().getRunMode()==PamController.RUN_NETWORKRECEIVER) {
			return;
		}
		if (wantDetectionData(dataUnit)) {
			if(dataUnit instanceof ClickDetection) {
				int x=0;
			}
			useDetectionData(dataUnit);
		}
	}
	
	

	/**
	 * Use the data selector built into the detection datablock to see if we want the incoming data. 
	 * @param dataUnit
	 * @return true if it scores OK
	 */
	private boolean wantDetectionData(PamDataUnit dataUnit) {
		if (detectionDataSelector != null) {
			double score = detectionDataSelector.scoreData(dataUnit);
			return score > 0;
		}
		return true;
	}

	/**
	 * Now use the data, potentially even creating output
	 * @param dataUnit incoming detection data unit. 
	 */
	private void useDetectionData(PamDataUnit dataUnit) {
		/**
		 * Fill the heading histrogram whether we're using this or not. 
		 */
		headingHistogram.addData(dataUnit);
		
		CollatorTriggerData trigger = collatorTrigger.newData(dataUnit);
		if (trigger != null) {
			// we want to do something
			newDetectionTrigger(trigger, dataUnit);
		}
	}

	/**
	 * the system has triggered and decided it want's to make an output data unit
	 * @param trigger trigger information
	 * @param dataUnit last data unit that went into the trigger. 
	 */
	private void newDetectionTrigger(CollatorTriggerData trigger, PamDataUnit dataUnit) {
		// TODO Auto-generated method stub
		int option = collatorRateFilter.judgeTriggerData(parameterSet, trigger);
		if (option == CollatorRateFilter.TRIGGER_SENDDATA) {
			// sort out all the data we'll be wanting and send or update output. Probably need to decimate it, etc. 
			// can we block the datablock here ? Or do I need to clone the clone ? 
			ArrayList<RawDataUnit> cloneCopy = null;
			/*synchronized (rawDataCopy.getSynchLock()) {
				cloneCopy = rawDataCopy.getDataCopy();
			}*/
			
			/*synchronized (rawDataBlock.getSynchLock()) {
				cloneCopy = rawDataBlock.getDataCopy();
			}*/
			
			
			/* 
			 * that's safe ! Freed copy, can now take time decimating those data, etc. though note that this may 
			 * still block the trigger data thread if the next stage takes more than a second or two, to may need 
			 * to make an entirely new thread to handle these final bits of the processing?? 
			 */
			int firstChannel = PamUtils.getLowestChannel(trigger.getDataList().get(0).getChannelBitmap());
			int[] channelList = new int[] {firstChannel};
			int bitmap = PamUtils.makeChannelMap(channelList);
			CollatorDataUnit newDataUnit = createOutputData(trigger, cloneCopy, bitmap);
			if (newDataUnit == null) {
				return;
			}
			BearingSummary bearingSummary = getBearingSummary(trigger);
			if (bearingSummary != null) {
				newDataUnit.setBearingSummary(new BearingSummaryLocalisation(newDataUnit, bearingSummary));
			}
			if (headingHistogram != null) {
				newDataUnit.setHeadingHistogram(headingHistogram.clone());
				headingHistogram.reset();
			}
			
			/**
			 * Synch adding data with collatorControl, but NOT the datablock since that will really 
			 * mess up some other threading stuff by blocking the datablock users for too long
			 * leading to a lock. 
			 */
			synchronized (collatorControl) {
				collatorBlock.addPamData(newDataUnit);
			}
			collatorTrigger.reset();
//			wav = rawDataCopy.
		}
		
	}
	
	/**
	 * Get a summary of bearing information that might be in the trigger. 
	 * @param trigger
	 */
	private BearingSummary getBearingSummary(CollatorTriggerData trigger) {
		/*
		 * Note that if data in the trigger are super dets, then we should pull the
		 * bearing information from all of the sub detections. 
		 * Here we loop through the data units that made up the trigger (may only be one). 
		 * Within bearing summariser it will loop through sub detections of these data units. 
		 */
		bearingSummariser.reset();
		List<PamDataUnit> dataList = trigger.getDataList();
		for (PamDataUnit dataUnit : dataList) {
			bearingSummariser.addData(dataUnit);
		}
		return bearingSummariser.getSummary();
	}

	/**
	 * Get waveform data from the store at full sample rate. 
	 * @param trigger
	 * @param cloneCopy 
	 * @param channelMap
	 */
	private CollatorDataUnit createOutputData(CollatorTriggerData trigger, ArrayList<RawDataUnit> cloneCopy, int channelMap) {
		int nChan = PamUtils.getNumChannels(channelMap);
		double[][] wavData = new double[nChan][];
		float fs = parameterSet.outputSampleRate;

		/**
		 * Stretch the start and end times slightly if they fit within the max length
		 */
		/*if (cloneCopy.size() == 0) {
			return null;
		}*/
		long triggerEndTime = trigger.getEndTime();
		long triggerStartTime = trigger.getStartTime();
		long triggerDurationMillis = triggerEndTime-triggerStartTime;
		int triggerDurationSamples = (int) (fs*triggerDurationMillis/1000);
		long triggerStartSample = trigger.getDataList().get(0).getStartSample();
		long triggerEndSample = triggerStartSample+triggerDurationSamples;
		//long wavEnd = cloneCopy.get(cloneCopy.size()-1).getEndTimeInMilliseconds();
		//long wavStart = cloneCopy.get(0).getTimeMilliseconds();
		//long wavLength = wavEnd-wavStart;
		long bufferSamples = (long) (parameterSet.minClipLengthS*fs)-(triggerDurationSamples);
		if(bufferSamples<0) {
			bufferSamples = (long) (parameterSet.outputClipLengthS*fs)-(triggerDurationSamples);
		}
		long prePost = (long) bufferSamples/2;
		
		long clipStartSample = triggerStartSample-prePost;
		long clipEndSample = triggerEndSample+prePost;
		
		int clipDurationSamples = (int) (clipEndSample-clipStartSample);
		
		long clipStartMillis = triggerStartTime-(long)(1000*prePost/fs);
		
		try {
			wavData = rawDataBlock.getSamples(clipStartSample, clipDurationSamples, channelMap);
		}
		catch (RawDataUnavailableException e) {
			e.printStackTrace();
			wavData[0] = new double[] {0};
		}
//		
		
		CollatorDataUnit newData = new CollatorDataUnit(clipStartMillis, channelMap, clipStartSample, parameterSet.outputSampleRate, wavData[0].length, trigger, parameterSet.setName, wavData);
				
		return newData;
	}

	public String getSetName() {
		return parameterSet.setName;
	}

	/**
	 * Set parameters and if necessary update the process settings (datablocks, etc) 
	 * @param paramSet
	 */
	public void setParameters(CollatorParamSet paramSet) {
		this.parameterSet = paramSet;
		collatorTrigger = new CountingTrigger(parameterSet);
	}
	
	private class RawDataObserver implements PamObserver {
		
		boolean pause = false;

		@Override
		public long getRequiredDataHistory(PamObservable observable, Object arg) {
			return 0;
		}

		@Override
		public void addData(PamObservable observable, PamDataUnit pamDataUnit) {
			/**
			 * Make a copy of the data (not the raw data) into a local array. won't need much memory, so OK. 
			 */
			if(pause) {
				return;
			}
			RawDataUnit in = (RawDataUnit) pamDataUnit;
			RawDataUnit copy = new RawDataUnit(in.getTimeMilliseconds(), in.getChannelBitmap(), in.getStartSample(), in.getSampleDuration());
			copy.setRawData(in.getRawData());
			synchronized (rawDataCopy.getSynchLock()) {
				rawDataCopy.addPamData(copy);	
			}
		}

		@Override
		public void updateData(PamObservable observable, PamDataUnit pamDataUnit) {
		}

		@Override
		public void removeObservable(PamObservable observable) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void setSampleRate(float sampleRate, boolean notify) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void noteNewSettings() {
			// TODO Auto-generated method stub
			
		}

		@Override
		public String getObserverName() {
			return collatorControl.getUnitName() + " " + parameterSet.setName;
		}

		@Override
		public void masterClockUpdate(long milliSeconds, long sampleNumber) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public PamObserver getObserverObject() {
			return this;
		}

		@Override
		public void receiveSourceNotification(int type, Object object) {
			
		}
		
	}

	@Override
	public void setupProcess() {
		
		detectorDatablock = PamController.getInstance().getDataBlockByLongName(parameterSet.detectionSource);
		setParentDataBlock(detectorDatablock);
		
		rawDataBlock = PamController.getInstance().getRawDataBlock(parameterSet.rawDataSource);
		if (rawDataBlock != null) {
			rawDataBlock.addObserver(rawDataObserver, false);
			rawDataCopy.setChannelMap(rawDataBlock.getChannelMap());
			rawDataCopy.setSampleRate(rawDataBlock.getSampleRate(), false);
			rawDataCopy.setNaturalLifetimeMillis((int) ((parameterSet.outputClipLengthS*1.1) * 1000)+20000);
			if (parameterSet.outputSampleRate == rawDataBlock.getSampleRate()) {
				decimator = null;
			}
			else {
				DecimatorParams dp = new DecimatorParams(parameterSet.outputSampleRate);
				decimator = new DecimatorWorker(dp, rawDataBlock.getChannelMap(), rawDataBlock.getSampleRate(), parameterSet.outputSampleRate);
			}
		}
		
		super.setupProcess();
	}

	@Override
	public void prepareProcess() {
		rawDataCopy.clearAll();
		
		if (detectorDatablock == null) {
			return;
		}
		detectionDataSelector = detectorDatablock.getDataSelector(collatorControl.getDataSelectorName(getSetName()), false);
		
		collatorTrigger = new CountingTrigger(parameterSet);
		collatorTrigger.reset();
		
		super.prepareProcess();
	}

	@Override
	public void destroyProcess() {
		/*
		 * super class should remove this observer from data block 
		 */
		super.destroyProcess();
		/*
		 * Stop the raw data monitor looking at it too ... 
		 */
		if (rawDataBlock != null) {
			rawDataBlock.deleteObserver(rawDataObserver);
			rawDataBlock = null;
		}
		if (rawDataCopy != null) {
			rawDataCopy.deleteObserver(rawDataObserver);
			rawDataCopy = null;
		}
		
	}

	@Override
	public String getProcessName() {
		return parameterSet.setName;
	}

	/**
	 * @return the parameterSet
	 */
	public CollatorParamSet getParameterSet() {
		return parameterSet;
	}

	@Override
	public ClipDisplayDataBlock getClipDataBlock() {
		// TODO Auto-generated method stub
		return collatorBlock;
	}

	@Override
	public String getDisplayName() {
		// TODO Auto-generated method stub
		return this.getSetName()+" Clips";
	}

	@Override
	public ClipDisplayDecorations getClipDecorations(ClipDisplayUnit clipDisplayUnit) {
		// TODO Auto-generated method stub
		return new ClipDisplayDecorations(clipDisplayUnit);
	}

	@Override
	public void displaySettingChange() {
		// TODO Auto-generated method stub
		
	}

}
