package contactcollator.swing;

import clipgenerator.clipDisplay.ClipDisplayPanel;
import contactcollator.CollatorControl;
import contactcollator.CollatorStreamProcess;
import userDisplay.UserDisplayComponent;
import userDisplay.UserDisplayControl;
import userDisplay.UserDisplayProvider;

public class CollatorDisplayProvider  implements UserDisplayProvider {
	
	private CollatorControl collatorControl;
	CollatorStreamProcess collatorStreamProcess;


	public CollatorDisplayProvider(CollatorControl collatorControl,	CollatorStreamProcess collatorStreamProcess) {
		super();
		this.collatorControl = collatorControl;
		this.collatorStreamProcess = collatorStreamProcess;
	}

	@Override
	public String getName() {
		return collatorStreamProcess.getSetName() + " clips";
	}

	@Override
	public UserDisplayComponent getComponent(UserDisplayControl userDisplayControl, String uniqueDisplayName) {
		return new CollatorClipDisplayPanel(collatorStreamProcess);
	}

	@Override
	public Class getComponentClass() {
		return ClipDisplayPanel.class;
	}

	@Override
	public int getMaxDisplays() {
		return 0;
	}

	@Override
	public boolean canCreate() {
		return true;
	}

	@Override
	public void removeDisplay(UserDisplayComponent component) {
		// TODO Auto-generated method stub
		
	}

}
