package contactcollator.io;

import Array.ArrayManager;
import Array.Streamer;
import PamguardMVC.DataUnitBaseData;
import PamguardMVC.PamDataBlock;
import PamguardMVC.PamDataUnit;
import contactcollator.CollatorDataUnit;
import contactcollator.trigger.CollatorTriggerData;
import jsonStorage.JSONDataStorageException;
import jsonStorage.JSONObjectData;
import jsonStorage.JSONObjectDataSource;

public class CollatorJsonDataSource extends JSONObjectDataSource<CollatorJsonData,CollatorDataUnit>{
	
	public CollatorJsonDataSource() {
		super(false);
		objectData = new CollatorJsonData();
	}

	@Override
	protected void sourceClassSpecificFields(PamDataUnit pamDataUnit) {
		
		CollatorDataUnit newUnit = (CollatorDataUnit) pamDataUnit;
		objectData.buoyId = getpbId(newUnit.getChannelBitmap());
		objectData.wavData = newUnit.getDataTransforms().getShortWaveData(0);
		objectData.wavFs = newUnit.getRawDataSampleRate();
		//objectData.wavFs = newUnit.getTriggerDataUnit().getParentDataBlock().getSampleRate();
		objectData.wavScalingFactor = (int) Math.pow(2, 15);
		if(newUnit.getBearingSummaryLocalisation()!=null) {
			objectData.centerBearingDegrees = newUnit.getBearingSummaryLocalisation().getRealWorldVectors()[0].getHeading();
			if(newUnit.getBearingSummaryLocalisation().getBearingSummary()!=null) {
				objectData.stdRadians = newUnit.getBearingSummaryLocalisation().getBearingSummary().getStdHeading();
			}
		}
		objectData.lowFrequency = newUnit.getTriggerData().getDataList().get(0).getFrequency()[0];
		//objectData.lowFrequency = newUnit.getTriggerDataUnit().getFrequency()[0];
		objectData.highFrequency = newUnit.getTriggerData().getDataList().get(0).getFrequency()[1];
		//objectData.highFrequency = newUnit.getTriggerDataUnit().getFrequency()[1];
		objectData.triggerSource = newUnit.triggerName;
		CollatorTriggerData triggerData = newUnit.findTriggerData();
		if(triggerData!=null && triggerData.getDataList().size()>0) {
			objectData.startTime = triggerData.getStartTime();
			objectData.endTime = triggerData.getEndTime();
			objectData.detectionCount = triggerData.getDataList().size();
			//newUnit.getHeadingHistogram().getData();
		}
		if(newUnit.getSpeciesID()!=null) {
			objectData.speciesAnnotation = newUnit.getSpeciesID();
		}else {
			objectData.speciesAnnotation = "not annotated";
		}
		
		//newUnit.
		
	}
	
	private String getpbId(int channelBitmap) {
		int channelIdx = PamUtils.PamUtils.getLowestChannel(channelBitmap);
		int stremerIdx = ArrayManager.getArrayManager().getCurrentArray().getStreamerForPhone(channelIdx);
		Streamer streamer = ArrayManager.getArrayManager().getCurrentArray().getStreamer(stremerIdx);
		String streamerName = streamer.getStreamerName();
		String pbId = "pb"+streamerName.substring(streamerName.length()-3);
		return pbId;
	}

	@Override
	protected void setObjectType(PamDataUnit pamDataUnit) {
		objectData.identifier = -1;
	}

	@Override
	protected CollatorDataUnit generatePamDataUnit(DataUnitBaseData baseData, CollatorJsonData jsonObjectData) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected void sinkData(CollatorDataUnit jsonObject, PamDataBlock pamDataBlock) throws JSONDataStorageException {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected CollatorJsonData initializeObjectData() {
		// TODO Auto-generated method stub
		return new CollatorJsonData();
	}

	@Override
	protected Class<? extends JSONObjectData> getJsonDataSourceObjectClass() {
		// TODO Auto-generated method stub
		return CollatorJsonData.class;
	}

}
