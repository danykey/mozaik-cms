/**
 * This file is part of Mozaik CMS            www.mozaik.top
 * Copyright © 2016 Denis N Ivanchik       danykey@gmail.com
**/
package top.mozaik.frnd.admin.vm.wcm.document;

import java.util.HashMap;
import java.util.Map;

import org.zkoss.bind.annotation.AfterCompose;
import org.zkoss.bind.annotation.Command;
import org.zkoss.bind.annotation.ExecutionArgParam;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.WrongValueException;

import top.mozaik.bknd.api.ServicesFacade;
import top.mozaik.bknd.api.model.WcmDocumentFolder;
import top.mozaik.bknd.api.model.WcmTemplate;
import top.mozaik.bknd.api.service.WcmDocumentFolderService;
import top.mozaik.bknd.api.service.WcmTemplateService;
import top.mozaik.frnd.admin.bean.wcm.document.TreeDocumentFolder;
import top.mozaik.frnd.plus.callback.I_CallbackArg;
import top.mozaik.frnd.plus.zk.ZKUtils;
import top.mozaik.frnd.plus.zk.component.Dialog;
import top.mozaik.frnd.plus.zk.component.Notification;
import top.mozaik.frnd.plus.zk.event.I_CUDEventHandler;
import top.mozaik.frnd.plus.zk.vm.BaseVM;

public class EditFolderVM extends BaseVM {
	
	private final WcmDocumentFolderService folderService = ServicesFacade.$().getWcmDocumentFolderService();
	private final WcmTemplateService templateService = ServicesFacade.$().getWcmTemplateService();
	
	private I_CUDEventHandler<TreeDocumentFolder> eventHandler;
	private TreeDocumentFolder treeDocumentFolder;
	private WcmDocumentFolder bean;
	
	private WcmTemplate template;
	
	private EditDocumentFieldsVM documentFieldsController;
	
	private boolean loadTemplateValues;
	
	@AfterCompose(superclass=true)
	public void doAfterCompose(
			@ExecutionArgParam("eventHandler") I_CUDEventHandler<TreeDocumentFolder> eventHandler,
			@ExecutionArgParam("treeDocumentFolder") TreeDocumentFolder treeDocumentFolder) {
		this.eventHandler = eventHandler;
		this.treeDocumentFolder = treeDocumentFolder;
		this.bean = treeDocumentFolder.getValue();
		
		if(bean.getTemplateId() == null) return;
		
		template = templateService.read1(
				new WcmTemplate().setId(bean.getTemplateId()));
	}
	
	void setController(EditDocumentFieldsVM ctrl) {
		this.documentFieldsController = ctrl;
	}
	
	/// BINDING ///
	
	public WcmDocumentFolder getBean() {
		return bean;
	}
	
	public WcmTemplate getTemplate() {
		return template;
	}
	
	public boolean getLoadTemplateValues() {
		return loadTemplateValues;
	}
		
	/// COMMANDS ///
	
	@Command
	public void selectTemplate() {
		final Map<String, Object> args = new HashMap<String, Object>();
		args.put("callback", new I_CallbackArg<WcmTemplate>() {
			@Override
			public void call(WcmTemplate template) {
				EditFolderVM.this.template = template;
				EditFolderVM.this.loadTemplateValues = true;
				reloadComponent();
			}
		});
		Executions.createComponents("/WEB-INF/zul/wcm/common/selectTemplate.wnd.zul", null, args);
	}
	
	@Command
	public void deleteTemplate() {
		template = null;
		reloadComponent();
	}
	
	@Command
	public void validateAndSave() {
		try {
			ZKUtils.validate(getView());
			
			if(bean.getTemplateId() != null) {
				String message = null;
				if(template == null) {
					message = "Template was deleted. The document fields data of the previous template will be lost! Continue?";
				} else if(!bean.getTemplateId().equals(template.getId())) {
					message = "Template was changed. The document fields data of the previous template will be lost! Continue?";
				} else {
					getBinder().postCommand("save", null);
					return;
				}
				Dialog.confirm("Save", message, new Dialog.Confirmable() {
					@Override
					public void onConfirm() {
						getBinder().postCommand("save", null);
					}
					@Override
					public void onCancel() {}
				});
			} else {
				getBinder().postCommand("save", null);
			}
		} catch (WrongValueException e) {
			ZKUtils.openTabByConstraintError(e);
			throw e;
		}
	}
	
	@Command
	public void save(){
		if(template != null)
			bean.setTemplateId(template.getId());
		else {
			bean.setTemplateId(null);
		}
		try {
			
			folderService.startTransaction();
			
			if(documentFieldsController != null) {
				documentFieldsController.save(bean.getTemplateId()==null);
			}
			
			// TODO: if alias is changed then need update childrens path
			folderService.update1(bean);
			
			folderService.commit();
			
			eventHandler.onUpdate(treeDocumentFolder);
			Notification.showMessage("Folder saved succesfully");
		} catch (Exception e) {
			folderService.rollback();
			e.printStackTrace();
			Dialog.error("Error occured while save Folder", e);
		}
	}
}
