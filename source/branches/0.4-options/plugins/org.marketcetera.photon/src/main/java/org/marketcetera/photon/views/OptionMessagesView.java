package org.marketcetera.photon.views;

import java.util.HashMap;

import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.part.ViewPart;
import org.marketcetera.photon.core.MessageHolder;
import org.marketcetera.photon.marketdata.OptionMessageHolder;
import org.marketcetera.photon.marketdata.OptionMessageHolder.OptionPairKey;
import org.marketcetera.photon.ui.EnumTableFormat;
import org.marketcetera.photon.ui.EventListContentProvider;
import org.marketcetera.photon.ui.IndexedTableViewer;
import org.marketcetera.photon.ui.MessageListTableFormat;
import org.marketcetera.photon.ui.TableComparatorChooser;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.SortedList;

public abstract class OptionMessagesView extends ViewPart {

	public static final String COLUMN_ORDER_KEY = "COLUMN_ORDER";  //$NON-NLS-1$
	public static final String COLUMN_ORDER_DELIMITER = ",";  //$NON-NLS-1$

	public static final String SORT_BY_COLUMN_KEY = "SORT_BY_COLUMN";  //$NON-NLS-1$

	private Table messageTable;
	private IndexedTableViewer messagesViewer;
	private IToolBarManager toolBarManager;
	private EnumTableFormat<OptionMessageHolder> tableFormat;
	private TableComparatorChooser<OptionMessageHolder> chooser;
	private Clipboard clipboard;
	private CopyMessagesAction copyMessagesAction;
	private EventList<OptionMessageHolder> rawInputList;
	private final boolean sortableColumns;
	private IMemento viewStateMemento; 

	public OptionMessagesView()
	{
		this(true);
	}

    public OptionMessagesView(boolean sortableColumns) {
		this.sortableColumns = sortableColumns;
    	
    }

	protected void formatTable(Table messageTable) {
        messageTable.getVerticalBar().setEnabled(true);
        messageTable.setBackground(
        		messageTable.getDisplay().getSystemColor(
						SWT.COLOR_INFO_BACKGROUND));
        messageTable.setForeground(
        		messageTable.getDisplay().getSystemColor(
						SWT.COLOR_INFO_FOREGROUND));

        messageTable.setHeaderVisible(true);

		for (int i = 0; i < messageTable.getColumnCount(); i++) {
			messageTable.getColumn(i).setMoveable(true);
		}
    }

	@Override
	public void createPartControl(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		composite.setLayout(layout);
		
		GridData gridData = new GridData(GridData.FILL, GridData.FILL, true, true);
		composite.setLayoutData(gridData);
		
		layout.numColumns = 1;

        messageTable = createMessageTable(composite);
		messagesViewer = createTableViewer(messageTable, getEnumValues());
		
		tableFormat = (EnumTableFormat<OptionMessageHolder>)messagesViewer.getLabelProvider();
		formatTable(messageTable);
		packColumns(messageTable);
		restoreColumnOrder(viewStateMemento);
		
        toolBarManager = getViewSite().getActionBars().getToolBarManager();
		initializeToolBar(toolBarManager);
		
		copyMessagesAction = new CopyMessagesAction(getClipboard(),messageTable, "Copy");
		IWorkbench workbench = PlatformUI.getWorkbench();
		ISharedImages platformImages = workbench.getSharedImages();
		copyMessagesAction.setImageDescriptor(platformImages .getImageDescriptor(ISharedImages.IMG_TOOL_COPY));
		copyMessagesAction.setDisabledImageDescriptor(platformImages.getImageDescriptor(ISharedImages.IMG_TOOL_COPY_DISABLED));

		Object menuObj = messageTable.getData(MenuManager.class.toString());
		if (menuObj != null && menuObj instanceof MenuManager) {
			MenuManager menuManager = (MenuManager) menuObj;
			menuManager.add(copyMessagesAction);
		}
		
		hookGlobalActions();
	}

	protected abstract void initializeToolBar(IToolBarManager theToolBarManager);
	
	protected abstract Enum[] getEnumValues();
	
	
	protected void packColumns(final Table table) {
		for (int i = 0; i < table.getColumnCount(); i++) {
			table.getColumn(i).pack();
		}
	}

	@Override
	public void init(IViewSite site, IMemento memento) throws PartInitException {
		super.init(site, memento);
		
		this.viewStateMemento = memento;
	}

	@Override
	public void saveState(IMemento memento) {
		super.saveState(memento);
		
		saveColumnOrder(memento);
		saveSortByColumn(memento);
	}

	protected String serializeColumnOrder(int[] columnOrder) {
		StringBuilder sb = new StringBuilder();
		for(int columnNumber : columnOrder) {
			sb.append(columnNumber);
			sb.append(COLUMN_ORDER_DELIMITER);
		}
		return sb.toString();
	}
	
	protected int[] deserializeColumnOrder(String delimitedValue) {
		if (delimitedValue == null) {
			return new int[0];
		}
		String[] columnNumbers = delimitedValue.split(COLUMN_ORDER_DELIMITER);
		if (columnNumbers == null || columnNumbers.length == 0) {
			return new int[0];
		}
		int[] columnOrder = new int[columnNumbers.length];
		for(int index = 0; index < columnOrder.length; ++index)  {
			try {
				columnOrder[index] = Integer.parseInt(columnNumbers[index]);
			}
			catch(Exception anyException) {
				// TODO Log?
				// org.marketcetera.photon.PhotonPlugin.getMainConsoleLogger().warn("Failed to load column order.", anyException);
				return new int[0];
			}
		}
		return columnOrder;
	}
	
	protected void saveColumnOrder(IMemento memento) {
		if (memento == null) 
			return;
		int[] columnOrder = messageTable.getColumnOrder();
		String serializedColumnOrder = serializeColumnOrder(columnOrder);
		memento.putString(COLUMN_ORDER_KEY, serializedColumnOrder);
	}
	
	protected void restoreColumnOrder(IMemento memento) {
		try {
			if (memento == null)
				return;
			String delimitedColumnOrder = memento.getString(COLUMN_ORDER_KEY);
			int[] columnOrder = deserializeColumnOrder(delimitedColumnOrder);
			if(columnOrder != null && columnOrder.length > 0) {
				messageTable.setColumnOrder(columnOrder);
			}
		} catch (Throwable t){
			// do nothing
		}
	}

	
	protected void restoreSortByColumn(IMemento memento) {
		if (memento == null)
			return;
		String sortByColumn = memento.getString(SORT_BY_COLUMN_KEY);
		if (sortByColumn != null && sortByColumn.length() > 0 && chooser != null)
		{
			chooser.fromString(sortByColumn);
		}
	}

	protected void saveSortByColumn(IMemento memento) {
		if (memento == null) 
			return;
		memento.putString(SORT_BY_COLUMN_KEY, chooser.toString());
	}
	
			
    protected Table createMessageTable(Composite parent) {
        Table messageTable = new Table(parent, SWT.MULTI | SWT.FULL_SELECTION | SWT.V_SCROLL | SWT.BORDER);
        GridData messageTableLayout = new GridData();
        messageTableLayout.horizontalSpan = 2;
        messageTableLayout.verticalSpan = 1;
        messageTableLayout.horizontalAlignment = GridData.FILL;
        messageTableLayout.verticalAlignment = GridData.FILL;
        messageTableLayout.grabExcessHorizontalSpace = true;
        messageTableLayout.grabExcessVerticalSpace = true;
        messageTable.setLayoutData(messageTableLayout);
        return messageTable;
    }
    
	protected IndexedTableViewer createTableViewer(Table aMessageTable, Enum[] enums) {
		IndexedTableViewer aMessagesViewer = new IndexedTableViewer(aMessageTable);
		getSite().setSelectionProvider(aMessagesViewer);
		aMessagesViewer.setContentProvider(new EventListContentProvider<MessageHolder>());
		aMessagesViewer.setLabelProvider(new MessageListTableFormat(aMessageTable, enums, getSite()));
		return aMessagesViewer;
	}
	

	public IndexedTableViewer getMessagesViewer() {
		return messagesViewer;
	}
	
	public void setInput(EventList<OptionMessageHolder> input)
	{
		SortedList<OptionMessageHolder> extractedList = 
			new SortedList<OptionMessageHolder>(rawInputList = input);

		if (sortableColumns){
			if (chooser != null){
				chooser.dispose();
				chooser = null;
			}
			chooser = new TableComparatorChooser<OptionMessageHolder>(
								messageTable, 
								tableFormat,
								extractedList, false);
			restoreSortByColumn(viewStateMemento);
		}
		messagesViewer.setInput(extractedList);
	}
	
	public EventList<OptionMessageHolder> getInput()
	{
		return rawInputList;
	}

	
	private void hookGlobalActions(){
		getViewSite().getActionBars()
		.setGlobalActionHandler(ActionFactory.COPY.getId(), copyMessagesAction);
	}

	@Override
	public void dispose() {
		if (clipboard != null)
			clipboard.dispose();
		super.dispose();

	}
	
	protected Clipboard getClipboard()
	{
		if (clipboard == null){
			clipboard = new Clipboard(getSite().getShell().getDisplay());
		}
		return clipboard;
	}

	@Override
	public void setFocus() {
		// TODO Auto-generated method stub
		
	}

}