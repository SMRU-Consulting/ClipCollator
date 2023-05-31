package contactcollator;

import java.util.ListIterator;

import PamguardMVC.PamDataBlock;
import PamguardMVC.PamDataUnit;
import PamguardMVC.PamProcess;
import clipgenerator.ClipDisplayDataBlock;
import whistlesAndMoans.ConnectedRegionDataUnit;

public class CollatorDataBlock extends ClipDisplayDataBlock<CollatorDataUnit> {

	public CollatorDataBlock(String dataName, PamProcess parentProcess, int channelMap) {
		super(CollatorDataUnit.class, dataName, parentProcess, channelMap);
	}
	
	public CollatorDataUnit findClipFromCRDU(PamDataUnit du) {
		if(!(du instanceof ConnectedRegionDataUnit)) {
			return null;
		}
		ConnectedRegionDataUnit crdu = (ConnectedRegionDataUnit) du;
		
		CollatorDataUnit collatorDataUnit;
		
		synchronized (this.getSynchLock()) {
			
				ListIterator<CollatorDataUnit> iter = this.getListIterator(ITERATOR_END);
				while (iter.hasPrevious()) {
					collatorDataUnit = iter.previous();
					if(collatorDataUnit.triggerName.equals(crdu.getParentDataBlock().getLongDataName())
						&& collatorDataUnit.getTriggerUID()==crdu.getUID()) {
						return collatorDataUnit;
					}
				}
			}
		
		return null;
		
		
	}

}
