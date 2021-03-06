/**
 * This file is part of Mozaik CMS            www.mozaik.top
 * Copyright © 2016 Denis N Ivanchik       danykey@gmail.com
**/
package top.mozaik.frnd.admin.vm.wcm.document;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.zkoss.bind.annotation.AfterCompose;
import org.zkoss.bind.annotation.BindingParam;
import org.zkoss.bind.annotation.Command;
import org.zkoss.bind.annotation.ExecutionArgParam;
import org.zkoss.bind.annotation.NotifyChange;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.event.DropEvent;
import org.zkoss.zk.ui.event.OpenEvent;
import org.zkoss.zk.ui.select.annotation.Wire;
import org.zkoss.zul.AbstractTreeModel;
import org.zkoss.zul.Menupopup;
import org.zkoss.zul.Tab;
import org.zkoss.zul.Tree;
import org.zkoss.zul.Treeitem;

import top.mozaik.bknd.api.ServicesFacade;
import top.mozaik.bknd.api.model.WcmDocument;
import top.mozaik.bknd.api.model.WcmDocumentFolder;
import top.mozaik.bknd.api.model.WcmLibrary;
import top.mozaik.bknd.api.service.WcmDocumentFolderService;
import top.mozaik.bknd.api.service.WcmDocumentService;
import top.mozaik.frnd.admin.bean.wcm.document.TreeDocument;
import top.mozaik.frnd.admin.bean.wcm.document.TreeDocumentFolder;
import top.mozaik.frnd.admin.bean.wcm.document.TreeLibraryDocumentFolder;
import top.mozaik.frnd.admin.contextmenu.wcm.DocumentTreeMenuBuilder;
import top.mozaik.frnd.admin.converter.wcm.DocumentTreeImageUrlConverter;
import top.mozaik.frnd.admin.enums.E_Icon;
import top.mozaik.frnd.admin.enums.E_WcmIcon;
import top.mozaik.frnd.admin.model.wcm.DocumentTreeModel;
import top.mozaik.frnd.admin.vm.wcm.WcmVM;
import top.mozaik.frnd.plus.zk.component.Dialog;
import top.mozaik.frnd.plus.zk.component.Notification;
import top.mozaik.frnd.plus.zk.converter.DateToStringConverter;
import top.mozaik.frnd.plus.zk.event.I_CUDEventHandler;
import top.mozaik.frnd.plus.zk.event.TreeCUDEventHandler;
import top.mozaik.frnd.plus.zk.tree.A_TreeElement;
import top.mozaik.frnd.plus.zk.tree.A_TreeNode;
import top.mozaik.frnd.plus.zk.vm.BaseVM;

public class DocumentsVM extends BaseVM {
	
	private final WcmDocumentFolderService folderService = ServicesFacade.$().getWcmDocumentFolderService();
	private final WcmDocumentService documentService = ServicesFacade.$().getWcmDocumentService();
	
	private final DateToStringConverter dateConverter = new DateToStringConverter("yyyy-MM-dd HH:mm");
	
	private I_CUDEventHandler<A_TreeElement> eventHandler;
	private DocumentTreeMenuBuilder treeItemContextMenuBuilder;
	
	@Wire
	Tree documentTree;
	
	private WcmVM wcmCtrl;
	
	@AfterCompose(superclass=true)
	public void doAfterCompose(
			@ExecutionArgParam("wcmCtrl") final WcmVM wcmCtrl) {
		this.wcmCtrl = wcmCtrl;
		eventHandler = new TreeCUDEventHandler<A_TreeElement>(documentTree){
			@Override
			public void onCreate(A_TreeElement e) {
				if(e instanceof TreeDocument) {
					editElement(e);
				}
				super.onCreate(e);
			}
			@Override
			public void onUpdate(A_TreeElement e) {
				// change tab label
				final Tab tab = wcmCtrl.getTab(e);
				if(tab != null) {
					tab.setLabel(e.toString());
				}
				super.onUpdate(e);
			}
		};
		treeItemContextMenuBuilder = new DocumentTreeMenuBuilder(this);
	}
	
	public void createFolder(A_TreeNode parentFolder) {
		final Map<String, Object> args = new HashMap<String, Object>();
		args.put("eventHandler", eventHandler);
		args.put("parentFolder", parentFolder);
		Executions.createComponents("/WEB-INF/zul/wcm/document/createFolder.wnd.zul", null, args);
	}
	
	public void deleteFolder(final TreeDocumentFolder folder) {
		Dialog.confirm("Delete", "Folder '" + folder.getValue().getTitle() + "' will be deleted. Continue?",
				new Dialog.Confirmable() {
			@Override
			public void onConfirm() {
				try {
					folderService.startTransaction();
					
					deleteFolderDeep(folder.getValue().getId());
					
					folderService.commit();
					eventHandler.onDelete(folder);
					Notification.showMessage("Folder deleted succesfully");
				} catch (Exception e) {
					folderService.rollback();
					Dialog.error("Error occured while delete: " + folder.getValue(), e);
					e.printStackTrace();
				}
			}
			@Override
			public void onCancel() {}
		});
	}
	
	private final WcmDocumentFolder _deleteFolderFilter = new WcmDocumentFolder();
	private final WcmDocument _deleteDocumentFilter = new WcmDocument();
	private void deleteFolderDeep(Integer folderId) {
		folderService.delete1(new WcmDocumentFolder().setId(folderId));
		
		/// TODO: CHECK DEPENDENCIES !!!
		/// WE CANT REMOVE TEMPLATES WHICH HAS DEPENDENTS (DOCUMENTS, VIEWS)
		documentService.delete(_deleteDocumentFilter.setFolderId(folderId), true);
		
		_deleteFolderFilter.setFolderId(folderId);
		final List<WcmDocumentFolder> folders = folderService.read(_deleteFolderFilter);
		for(WcmDocumentFolder f:folders) {
			deleteFolderDeep(f.getId());
		}
	}
	
	public void createDocument(A_TreeNode parentFolder) {
		final Map<String, Object> args = new HashMap<String, Object>();
		args.put("eventHandler", eventHandler);
		args.put("parentFolder", parentFolder);
		Executions.createComponents("/WEB-INF/zul/wcm/document/createDocument.wnd.zul", null, args);
	}
	
	public void deleteDocument(final TreeDocument treeDocument) {
		Dialog.confirm("Delete", "Document '" + treeDocument.getValue().getTitle() + "' will be deleted. Continue?",
				new Dialog.Confirmable() {
			@Override
			public void onConfirm() {
				try {
					documentService.delete1(treeDocument.getValue());
					
					eventHandler.onDelete(treeDocument);
					Notification.showMessage("Document deleted succesfully");
				} catch (Exception e) {
					Dialog.error("Error occured while delete: " + treeDocument.getValue(), e);
					e.printStackTrace();
				}
			}
			@Override
			public void onCancel() {}
		});
	}
	
	/// BINDING ///
	
	public DocumentTreeModel getDocumentTreeModel() throws Exception {
		return new DocumentTreeModel();
	}
	
	public DocumentTreeImageUrlConverter getTreeImageUrlConverter() {
		return DocumentTreeImageUrlConverter.getInstance();
	}
	
	public DateToStringConverter getDateConverter() {
		return dateConverter;
	}
	
	/// COMMANDS ///
	
	@Command
	public void drop(@BindingParam("event") DropEvent event) {
		final A_TreeElement draggedEl = ((Treeitem)event.getDragged()).getValue();
		final A_TreeElement toEl = ((Treeitem)event.getTarget()).getValue();
		if(draggedEl instanceof TreeDocumentFolder){
			final WcmDocumentFolder draggedFolder = ((TreeDocumentFolder) draggedEl).getValue();
			if(toEl instanceof TreeLibraryDocumentFolder) {
				final WcmLibrary toLibrary = ((TreeLibraryDocumentFolder)toEl).getValue();
				folderService.update1(
						draggedFolder
						//	.setLibraryId()
							.setFolderId(-toLibrary.getId())
				);
			} else if(toEl instanceof TreeDocumentFolder) {
				final WcmDocumentFolder toFolder = ((TreeDocumentFolder)toEl).getValue();
				folderService.update1(
						draggedFolder
							//.setLibraryId(toFolder.getLibraryId())
							.setFolderId(toFolder.getId())
				);
			}
		} else if(draggedEl instanceof TreeDocument){
			final WcmDocument draggedTemplate = ((TreeDocument) draggedEl).getValue();
			if(toEl instanceof TreeLibraryDocumentFolder) {
				final WcmLibrary toLibrary = ((TreeLibraryDocumentFolder)toEl).getValue();
				documentService.update1(
						draggedTemplate
							//.setLibraryId(toLibrary.getId())
							.setFolderId(-toLibrary.getId())
				);
			} else if(toEl instanceof TreeDocumentFolder) {
				final WcmDocumentFolder toFolder = ((TreeDocumentFolder)toEl).getValue();
				documentService.update1(
						draggedTemplate
							//.setLibraryId(toFolder.getLibraryId())
							.setFolderId(toFolder.getId())
				);
			}
		}
		
		AbstractTreeModel model = (AbstractTreeModel<?>) documentTree.getModel();
		final int[][] openedPaths = model.getOpenPaths();
		reloadComponent();
		model = (AbstractTreeModel<?>) documentTree.getModel();
		model.addOpenPaths(openedPaths);
	}
	
	@Command
	public void createDocument() {
		final Map<String, Object> args = new HashMap<String, Object>();
		args.put("eventHandler", eventHandler);
		Executions.createComponents("/WEB-INF/zul/wcm/document/createDocument.wnd.zul", null, args);
	}
	
	@Command
	public void editElement(@BindingParam("el") A_TreeElement el) {
		final Map<String, Object> args = new HashMap<String, Object>();
		args.put("eventHandler", eventHandler);
		
		if(el instanceof TreeDocumentFolder) {
			args.put("treeDocumentFolder", el);
			final TreeDocumentFolder folder = (TreeDocumentFolder) el;
			wcmCtrl.openTab(E_Icon.FOLDER.getPath(),folder.getValue().getTitle(),
					folder.getValue().getDescr(), el, "/WEB-INF/zul/wcm/document/editFolder.tab.zul", args);
		} else if(el instanceof TreeDocument) {
			args.put("treeDocument", el);
			final TreeDocument document = (TreeDocument) el;
			wcmCtrl.openTab(E_WcmIcon.DOCUMENT_SMALL.getPath(), document.getValue().getTitle(),
					document.getValue().getDescr(), el, "/WEB-INF/zul/wcm/document/editDocument.tab.zul", args);
		}
	}
	
	@Command
	public void showTreeContextMenu(@BindingParam("event") OpenEvent event) {
		final Menupopup menu = (Menupopup)event.getTarget();
		final Component ref = event.getReference();
		
		if(ref == null) {
			menu.getChildren().clear();
			return;
		}
		
		final Treeitem treeitem = (Treeitem)ref;
		treeItemContextMenuBuilder.build(menu, (A_TreeElement) treeitem.getValue());
	}
	
	@Command
	@NotifyChange("documentTreeModel")
	public void refresh() {
	}
}
