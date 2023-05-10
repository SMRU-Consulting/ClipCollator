package contactcollator;

import java.sql.Types;

import Array.ArrayManager;
import Array.Streamer;
import GPS.GpsData;
import PamUtils.PamUtils;
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
	 bearingError;

	protected CollatorExtendedLogging(CollatorControl moduleControl, CollatorDataBlock pamDataBlock) {
		
		super(moduleControl,pamDataBlock);
		
		
		PamTableDefinition tableDef = getTableDefinition();
		
		tableDef.addTableItem(buoyLatitude = new PamTableItem("buoyLatitude", Types.DOUBLE)); 
		tableDef.addTableItem(buoyLongitude = new PamTableItem("buoyLongitude", Types.DOUBLE)); 
		tableDef.addTableItem(buoyId = new PamTableItem("buoyId", Types.CHAR,10)); 
		tableDef.addTableItem(detectionUID = new PamTableItem("detectionUID", Types.BIGINT)); 
		tableDef.addTableItem(hasBearing = new PamTableItem("hasBearing", Types.BOOLEAN)); 
		tableDef.addTableItem(bearing0 = new PamTableItem("bearing0", Types.DOUBLE)); 
		tableDef.addTableItem(bearingError = new PamTableItem("bearingError", Types.DOUBLE)); 
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
		
	}


}
