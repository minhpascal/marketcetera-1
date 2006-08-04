package org.marketcetera.photon.editors;


import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.action.GroupMarker;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.MultiPageEditorPart;
import org.marketcetera.core.ClassVersion;
import org.marketcetera.photon.PhotonAdapterFactory;
import org.marketcetera.photon.model.FIXMessageHistory;
import org.marketcetera.photon.model.MessageHolder;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.swt.EventTableViewer;

/**
 * An example showing how to create a multi-page editor. This example has 3
 * pages:
 * <ul>
 * <li>page 0 contains a nested text editor.
 * <li>page 1 allows you to change the font used in page 2
 * <li>page 2 shows the words in page 0 in sorted order
 * </ul>
 */
@ClassVersion("$Id$")
public class OrderHistoryEditor extends MultiPageEditorPart {

	public enum OpenOrderColumns {
		TRANSACTTIME("TransactTime"), CLORDID("ClOrdID"),
		ORDERID("OrderID"), ORDSTATUS("OrdStatus"), SIDE(
				"Side"), SYMBOL("Symbol"), ORDERQTY("OrderQty"), CUMQTY(
				"CumQty"), LEAVESQTY("LeavesQty"), Price("Price"), AVGPX(
				"AvgPx"), ACCOUNT("Account"), LASTSHARES("LastShares"), LASTPX(
				"LastPx"), LASTMKT("LastMkt");

		private String mName;

		OpenOrderColumns(String name) {
			mName = name;
		}

		public String toString() {
			return mName;
		}
	};
	
	public enum AvgPriceColumns {
		DIRECTION("D"), SIDE("Side"), SYMBOL("Symbol"), ORDERQTY("OrderQty"), CUMQTY("CumQty"), 
		AVGPX("AvgPx"), ACCOUNT("Account");

		private String mName;

		AvgPriceColumns(String name) {
			mName = name;
		}

		public String toString() {
			return mName;
		}
	};

	public enum MessageColumns {
		DIRECTION("D"), TRANSACTTIME("TransactTime"), MSGTYPE("MsgType"), CLORDID("ClOrdID"),
		ORDERID("OrderID"), ORICCLORDID("OrigClOrdID"), ORDSTATUS("OrdStatus"), SIDE(
				"Side"), SYMBOL("Symbol"), ORDERQTY("OrderQty"), CUMQTY(
				"CumQty"), LEAVESQTY("LeavesQty"), Price("Price"), AVGPX(
				"AvgPx"), ACCOUNT("Account"), LASTSHARES("LastShares"), LASTPX(
				"LastPx"), LASTMKT("LastMkt"), EXECID("ExecID");

		private String mName;

		MessageColumns(String name) {
			mName = name;
		}

		public String toString() {
			return mName;
		}
	};

	public enum FillColumns {
		CLORDID("ClOrdID"), ORDSTATUS("OrdStatus"), SIDE("Side"), SYMBOL("Symbol"), ORDERQTY(
				"OrderQty"), CUMQTY("CumQty"), LEAVESQTY("LeavesQty"), Price(
				"Price"), AVGPX("AvgPx"), STRATEGY("Strategy"), ACCOUNT(
				"Account"), LASTSHARES("LastShares"), LASTPX("LastPx"), LASTMKT(
				"LastMkt");

		private String mName;

		FillColumns(String name) {
			mName = name;
		}

		public String toString() {
			return mName;
		}
	};

	public static final String ID = "org.marketcetera.photon.editors.OrderHistoryEditor";

	private Table openOrderTable;
	private EventTableViewer openOrderViewer;
	private ViewerSelectionAdapter openOrderSelectionProvider;

	private Table averagePriceTable;
	private EventTableViewer averagePriceViewer;
	private ViewerSelectionAdapter averagePriceSelectionProvider;
	
	private Table messageTable;
	private EventTableViewer messagesViewer;
	private ViewerSelectionAdapter messagesSelectionProvider;

	private Table fillTable;
	private EventTableViewer fillsViewer;
	private ViewerSelectionAdapter fillsSelectionProvider;

	private IAdapterFactory adapterFactory = new PhotonAdapterFactory();

	private EventList<MessageHolder> allMessages;
	
	private EventList<MessageHolder> filteredMessages;

	private EventList<MessageHolder> fillMessages;
	
	private EventList<MessageHolder> openOrderMessages;

	private IWorkbenchWindow window;
	
	private static final int OPEN_ORDER_VIEWER_INDEX = 0;
	private static final int FILLS_VIEWER_INDEX = 1;
	private static final int AVERAGE_PRICE_VIEWER_INDEX = 2;
	private static final int MESSAGES_VIEWER_INDEX = 3;


	private EventList<MessageHolder> averagePriceList;


	private FIXMessageHistory messageHistory;


	/**
	 * Creates a multi-page editor example.
	 */
	public OrderHistoryEditor() {
		super();
		// ResourcesPlugin.getWorkspace().addResourceChangeListener(this);
		Platform.getAdapterManager().registerAdapters(adapterFactory,
				FIXMessageHistory.class);
		Platform.getAdapterManager().registerAdapters(adapterFactory,
				MessageHolder.class);
		Platform.getAdapterManager().registerAdapters(adapterFactory,
				quickfix.Message.class);
	}

	/**
	 * Creates page 0 of the multi-page editor, which contains the list of fills
	 */
	void createPage0() {
		Composite composite = new Composite(getContainer(), SWT.NONE);
		GridLayout layout = new GridLayout();
		composite.setLayout(layout);
		layout.numColumns = 1;

        openOrderTable = createMessageTable(composite);
        openOrderViewer = new EventTableViewer(openOrderMessages, openOrderTable, new EnumTableFormat(OpenOrderColumns.values()));
        openOrderSelectionProvider = new ViewerSelectionAdapter(openOrderViewer);
        openOrderTable = formatFillTable(openOrderTable);

        openOrderTable.setBackground(
        		openOrderTable.getDisplay().getSystemColor(
						SWT.COLOR_INFO_BACKGROUND));
        openOrderTable.setForeground(
        		openOrderTable.getDisplay().getSystemColor(
						SWT.COLOR_INFO_FOREGROUND));

        openOrderTable.setHeaderVisible(true);

		int index = addPage(composite);
		packColumns(openOrderTable);
		setPageText(index, "Open Orders");
	}
	
	void createPage1() {
		Composite composite = new Composite(getContainer(), SWT.NONE);
		GridLayout layout = new GridLayout();
		composite.setLayout(layout);
		layout.numColumns = 1;

        fillTable = createMessageTable(composite);
		fillsViewer = new EventTableViewer(fillMessages, fillTable, new EnumTableFormat(FillColumns.values()));
		fillsSelectionProvider = new ViewerSelectionAdapter(fillsViewer);
		fillTable = formatFillTable(fillTable);

        fillTable.setBackground(
        		fillTable.getDisplay().getSystemColor(
						SWT.COLOR_INFO_BACKGROUND));
        fillTable.setForeground(
        		fillTable.getDisplay().getSystemColor(
						SWT.COLOR_INFO_FOREGROUND));

        fillTable.setHeaderVisible(true);

		int index = addPage(composite);
		packColumns(fillTable);
		setPageText(index, "Fills");

	}

	/**
	 * @param table
	 */
	private void packColumns(final Table table) {
		for (int i = 0; i < table.getColumnCount(); i++) {
			table.getColumn(i).pack();
		}
	}
	
	/**
	 * Creates page 1 of the multi-page editor, which contains the list of messages
	 */
	void createPage2() {
		Composite composite = new Composite(getContainer(), SWT.NONE);
		GridLayout layout = new GridLayout();
		composite.setLayout(layout);
		layout.numColumns = 1;

        messageTable = createMessageTable(composite);
		messagesViewer = new EventTableViewer(filteredMessages, messageTable, new EnumTableFormat(MessageColumns.values()));
		messagesSelectionProvider = new ViewerSelectionAdapter(messagesViewer);
		messageTable = formatFillTable(messageTable);

        messageTable.setBackground(
        		messageTable.getDisplay().getSystemColor(
						SWT.COLOR_INFO_BACKGROUND));
        messageTable.setForeground(
        		messageTable.getDisplay().getSystemColor(
						SWT.COLOR_INFO_FOREGROUND));

        messageTable.setHeaderVisible(true);

		int index = addPage(composite);
		packColumns(messageTable);
		setPageText(index, "Messages");

	}


	/**
	 * Creates page 2 of the multi-page editor, which contains the list of average price fills
	 */
	void createPage3() {
		Composite composite = new Composite(getContainer(), SWT.NONE);
		GridLayout layout = new GridLayout();
		composite.setLayout(layout);
		layout.numColumns = 1;

        averagePriceTable = createMessageTable(composite);
		averagePriceViewer = new EventTableViewer(averagePriceList, averagePriceTable, new EnumTableFormat(AvgPriceColumns.values()));
		averagePriceSelectionProvider = new ViewerSelectionAdapter(averagePriceViewer);
		averagePriceTable = formatFillTable(averagePriceTable);

        averagePriceTable.setBackground(
        		averagePriceTable.getDisplay().getSystemColor(
						SWT.COLOR_INFO_BACKGROUND));
        averagePriceTable.setForeground(
        		averagePriceTable.getDisplay().getSystemColor(
						SWT.COLOR_INFO_FOREGROUND));

        averagePriceTable.setHeaderVisible(true);

		int index = addPage(composite);
		packColumns(averagePriceTable);
		setPageText(index, "Average Price");

	}

    private Table createMessageTable(Composite parent) {
        Table issuesTable = new Table(parent, SWT.MULTI | SWT.FULL_SELECTION | SWT.V_SCROLL | SWT.BORDER | SWT.VIRTUAL);
        GridData issuesTableLayout = new GridData();
        issuesTableLayout.horizontalSpan = 2;
        issuesTableLayout.verticalSpan = 1;
        issuesTableLayout.horizontalAlignment = GridData.FILL;
        issuesTableLayout.verticalAlignment = GridData.FILL;
        issuesTableLayout.grabExcessHorizontalSpace = true;
        issuesTableLayout.grabExcessVerticalSpace = true;
        issuesTable.setLayoutData(issuesTableLayout);
        return issuesTable;
    }

    private Table formatFillTable(Table issuesTable) {
        issuesTable.getVerticalBar().setEnabled(true);
//        issuesTable.getColumn(0).setWidth(30);
//        issuesTable.getColumn(1).setWidth(50);
//        issuesTable.getColumn(2).setWidth(46);
//        issuesTable.getColumn(3).setWidth(50);
//        issuesTable.getColumn(4).setWidth(60);
//        issuesTable.getColumn(5).setWidth(250);
        return issuesTable;
    }

	/**
	 * Creates the pages of the multi-page editor.
	 */
	protected void createPages() {
		createPage0();
		createPage1();
		createPage2();
		createPage3();
		makeActions();
	}

	private void makeActions() {

		createContextMenu("orderHistoryMessagePopup", messageTable, messagesSelectionProvider);

		createContextMenu("orderHistoryFillPopup", fillTable,fillsSelectionProvider);

		createContextMenu("orderHistoryOpenOrderPopup", openOrderTable, openOrderSelectionProvider);
		
		getSite().setSelectionProvider(new OrderHistorySelectionProvider(this));
	}
	
	private void createContextMenu(String name, Control table, ISelectionProvider selectionProvider)
	{
		MenuManager menuMgr = new MenuManager(name);
		Menu menu = menuMgr.createContextMenu(table);
		menuMgr.add(new GroupMarker(IWorkbenchActionConstants.MB_ADDITIONS));

		table.setMenu(menu);
		getSite().registerContextMenu(menuMgr, selectionProvider);
	}

	/**
	 * The <code>MultiPageEditorPart</code> implementation of this
	 * <code>IWorkbenchPart</code> method disposes all nested editors.
	 * Subclasses may extend.
	 */
	public void dispose() {
		// ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);
		super.dispose();
	}

	/**
	 * Saves the multi-page editor's document.
	 */
	public void doSave(IProgressMonitor monitor) {
		getEditor(0).doSave(monitor);
	}

	/**
	 * Saves the multi-page editor's document as another file. Also updates the
	 * text for page 0's tab, and updates this multi-page editor's allMessages to
	 * correspond to the nested editor's.
	 */
	public void doSaveAs() {
		IEditorPart editor = getEditor(0);
		editor.doSaveAs();
		setPageText(0, editor.getTitle());
		setInput(editor.getEditorInput());
	}

	/*
	 * (non-Javadoc) Method declared on IEditorPart
	 */
	public void gotoMarker(IMarker marker) {
		setActivePage(0);
	}

	/**
	 * The <code>MultiPageEditorExample</code> implementation of this method
	 * checks that the allMessages is an instance of <code>IFileEditorInput</code>.
	 */
	public void init(IEditorSite site, IEditorInput editorInput)
			throws PartInitException {
		if (!(editorInput instanceof OrderHistoryInput)) {
			throw new PartInitException(
					"Invalid Input: Must be IFileEditorInput");
		} else {
			messageHistory = ((OrderHistoryInput) editorInput).getHistory();
			allMessages = messageHistory.getAllMessages();
			filteredMessages = messageHistory.getFilteredMessages();
			fillMessages = messageHistory.getFills();
			averagePriceList = messageHistory.getAveragePriceHistory();
			openOrderMessages = messageHistory.getOpenOrders();
			
//			allMessages.add(new IncomingMessageHolder(
//					FIXMessageUtil.newExecutionReport(new InternalID("1001"), new InternalID("1"), "2001", ExecTransType.NEW, ExecType.NEW, OrdStatus.NEW, Side.BUY, new BigDecimal(1000), new BigDecimal(789), null, null, new BigDecimal(1000), BigDecimal.ZERO, BigDecimal.ZERO, new MSymbol("TESTSYM"))
//			));
		}
		window = site.getWorkbenchWindow();

		super.init(site, editorInput);
	}

	public ISelectionProvider getActiveSelectionProvider() {
		int pageIndex = getActivePage();
		switch (pageIndex) {
		case OPEN_ORDER_VIEWER_INDEX:
			return openOrderSelectionProvider;
		case FILLS_VIEWER_INDEX:
			return fillsSelectionProvider;
		case AVERAGE_PRICE_VIEWER_INDEX:
			return averagePriceSelectionProvider;
		case MESSAGES_VIEWER_INDEX:
			return messagesSelectionProvider;
		default:
			return null;
		}
	}
	

	
	/*
	 * (non-Javadoc) Method declared on IEditorPart.
	 */
	public boolean isSaveAsAllowed() {
		return true;
	}


	public void asyncExec(Runnable runnable) {
		Display display = this.getContainer().getDisplay();

		// If the display is disposed, you can't do anything with it!!!
		if (display == null || display.isDisposed())
			return;

		display.asyncExec(runnable);
	}
	
	protected void asyncRefresh()
	{
		asyncExec(new Runnable() {
			public void run() {
				refresh();
			}
		});
	}
	

	private void refresh() {
//		messagesViewer.refresh();
//		fillsViewer.refresh();
//		averagePriceViewer.refresh();
	}

	
}
