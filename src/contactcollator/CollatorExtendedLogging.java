package contactcollator;

import java.sql.Types;

import Array.ArrayManager;
import Array.Streamer;
import GPS.GpsData;
import PamUtils.PamUtils;
import PamguardMVC.PamDataBlock;
import PamguardMVC.PamDataUnit;
import contactcollator.io.CollatorLogging;
import generalDatabase.PamTableDefinition;
import generalDatabase.PamTableItem;
import generalDatabase.SQLTypes;

public class CollatorExtendedLogging extends CollatorLogging{
	
	
	private PamTableItem 
	 buoyLatitude,
	 buoyLongitude,
	 buoyId,
	 clipBinaryFileName,
	 detectionUID,
	 hasBearing,
	 bearing0,
	 bearingError,
	 speciesId;

	protected CollatorExtendedLogging(CollatorControl moduleControl, PamDataBlock pamDataBlock) {
		
		super(moduleControl,pamDataBlock);
		
		
		
		table.addTableItem(buoyLatitude = new PamTableItem("buoyLatitude", Types.DOUBLE)); 
		table.addTableItem(buoyLongitude = new PamTableItem("buoyLongitude", Types.DOUBLE)); 
		table.addTableItem(buoyId = new PamTableItem("buoyId", Types.CHAR,10)); 
		table.addTableItem(clipBinaryFileName = new PamTableItem("clipBinaryFileName", Types.CHAR,100));
		table.addTableItem(detectionUID = new PamTableItem("detectionUID", Types.BIGINT)); 
		table.addTableItem(hasBearing = new PamTableItem("hasBearing", Types.BOOLEAN)); 
		table.addTableItem(bearing0 = new PamTableItem("bearing0", Types.DOUBLE)); 
		table.addTableItem(bearingError = new PamTableItem("bearingError", Types.DOUBLE)); 
		table.addTableItem(speciesId = new PamTableItem("speciesId", Types.CHAR,10)); 
		
		setTableDefinition(table);


	}

	@Override
	public void setTableData(SQLTypes sqlTypes, PamDataUnit pamDataUnit) {
		super.setTableData(sqlTypes, pamDataUnit);
		CollatorDataUnit newUnit = (CollatorDataUnit) pamDataUnit;
		int chIdx = PamUtils.getLowestChannel(newUnit.getChannelBitmap());
		int streamerId = ArrayManager.getArrayManager().getCurrentArray().getStreamerForPhone(chIdx);
		Streamer streamer = ArrayManager.getArrayManager().getCurrentArray().getStreamer(streamerId);
		GpsData streamerGps = streamer.getHydrophoneLocator().getStreamerLatLong(System.currentTimeMillis());
		double lat = streamerGps.getLatitude();
		double lon = streamerGps.getLongitude();
		String id = streamer.getStreamerName();
		buoyLatitude.setValue(lat);
		buoyLongitude.setValue(lon);
		buoyId.setValue(id);
		 if(newUnit.getParentDataBlock().getBinaryDataSource()!=null && newUnit.getParentDataBlock().getBinaryDataSource().getBinaryStorageStream()!=null) {
			 clipBinaryFileName.setValue(newUnit.getParentDataBlock().getBinaryDataSource().getBinaryStorageStream().getMainFileName());
		 }else if(newUnit.getBinaryFileName()!=null){
			 clipBinaryFileName.setValue(newUnit.getBinaryFileName());
		 }
		 if(newUnit.findTriggerData().getDataList().size()>0) {
			 detectionUID.setValue(newUnit.findTriggerData().getDataList().get(0).getUID());
		 }else {
			 detectionUID.setValue(-1);
		 }
		 if(newUnit.getLocalisation()==null) {
			 hasBearing.setValue(false);
		 }else {
			 hasBearing.setValue(true);
			 bearing0.setValue(newUnit.getLocalisation().getAngles()[0]);
			 if(newUnit.getLocalisation().getAngleErrors()!=null) {
				 bearingError.setValue(newUnit.getLocalisation().getAngleErrors()[0]);
			 }
		 }
		speciesId.setValue(newUnit.getSpeciesID());
	}


}
