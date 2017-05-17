/*******************************************************************************
 * Copyright (c) 2017 Red Hat Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *  Mickael Istria (Red Hat Inc.) - Initial implementation
 *******************************************************************************/
package org.eclipse.acute.dotnetnew;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Set;

import org.eclipse.acute.AcutePlugin;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.dialogs.WorkingSetGroup;
import org.eclipse.ui.internal.WorkbenchPlugin;
import org.eclipse.ui.internal.registry.WorkingSetDescriptor;
import org.eclipse.ui.internal.registry.WorkingSetRegistry;

public class DotnetNewWizardPage extends WizardPage implements IWizardPage {

	private Set<IWorkingSet> workingSets;
	private File directory;
	private String projectName;

	private Text locationText;
	private Text projectNameText;
	private WorkingSetGroup workingSetsGroup;
	private Image linkImage;
	private Button linkButton;
	private Label locationInfo;
	private ControlDecoration locationControlDecoration;
	private Label projectNameInfo;
	private ControlDecoration projectNameControlDecoration;

	protected DotnetNewWizardPage() {
		super(DotnetNewWizardPage.class.getName());
		setTitle("Create new Dotnet project");
		setDescription("Create a new Dotnet project, using the `dotnet new` command");
	}

	@Override
	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NULL);
		setControl(container);
		container.setLayout(new GridLayout(4, false));

		Label locationnLabel = new Label(container, SWT.NONE);
		locationnLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		locationnLabel.setText("Location");

		locationText = new Text(container, SWT.BORDER);
		locationText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		locationText.addModifyListener(e -> {
			updateDirectory(new File(locationText.getText()));
			setPageComplete(isPageComplete());
		});

		Button browseButton = new Button(container, SWT.NONE);
		browseButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
		browseButton.setText("Browse...");
		browseButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				DirectoryDialog dialog = new DirectoryDialog(browseButton.getShell());
				String path = dialog.open();
				if (path != null) {
					updateDirectory(new File(path));
				}
				setPageComplete(isPageComplete());
			}
		});
		Composite linesAboveLink = new Composite(container, SWT.NONE);
		GridData linesAboveLinkLayoutData = new GridData(SWT.FILL, SWT.FILL);
		linesAboveLinkLayoutData.heightHint = linesAboveLinkLayoutData.widthHint = 30;
		linesAboveLink.setLayoutData(linesAboveLinkLayoutData);
		linesAboveLink.addPaintListener(e -> {
			e.gc.setForeground(((Control)e.widget).getDisplay().getSystemColor(SWT.COLOR_DARK_GRAY));
			e.gc.drawLine(0, e.height/2, e.width/2, e.height/2);
			e.gc.drawLine(e.width/2, e.height/2, e.width/2, e.height);
		});

		new Label(container, SWT.NONE);
		locationInfo = new Label(container, SWT.NONE);
		locationInfo.setText("locatioInfo");
		new Label(container, SWT.NONE);

		linkButton = new Button(container, SWT.TOGGLE);
		linkButton.setToolTipText("Link project name and folder name");
		try (InputStream iconStream = getClass().getResourceAsStream("/icons/link_obj.png")) {
			linkImage = new Image(linkButton.getDisplay(), iconStream);
			linkButton.setImage(linkImage);
		} catch (IOException e1) {
			AcutePlugin.logError(e1);
		}

		Label projectNameLabel = new Label(container, SWT.NONE);
		projectNameLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false, 1, 1));
		projectNameLabel.setText("Project name");

		projectNameText = new Text(container, SWT.BORDER);
		projectNameText.setEnabled(false);
		projectNameText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		projectNameText.addModifyListener(e -> {
			updateProjectName();
			setPageComplete(isPageComplete());
		});
		Composite linesBelowLink = new Composite(container, SWT.NONE);
		GridData linesBelowLinkLayoutData = new GridData(SWT.FILL, SWT.FILL);
		linesBelowLinkLayoutData.heightHint = linesBelowLinkLayoutData.widthHint = 30;
		linesBelowLink.setLayoutData(linesAboveLinkLayoutData);
		linesBelowLink.addPaintListener(e -> {
			e.gc.setForeground(((Control)e.widget).getDisplay().getSystemColor(SWT.COLOR_DARK_GRAY));
			e.gc.drawLine(0, e.height/2, e.width/2, e.height/2);
			e.gc.drawLine(e.width/2, e.height/2, e.width/2, 0);
		});
		new Label(container, SWT.NONE);

		projectNameInfo = new Label(container, SWT.NONE);
		projectNameInfo.setText("projectNameInfo");
		new Label(container, SWT.NONE);
		new Label(container, SWT.NONE);

		Label projectTemplateLabel = new Label(container, SWT.NONE);
		projectTemplateLabel.setText("Project Template");

		List list = new List(container, SWT.BORDER);
		list.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 2, 1));
		ListViewer templateViewer = new ListViewer(list);
		templateViewer.setContentProvider(new ArrayContentProvider());
		templateViewer.setInput(DotnetNewAccessor.getTemplates());
		new Label(container, SWT.NONE);

		Composite workingSetComposite = new Composite(container, SWT.NONE);
		GridData layoutData = new GridData(SWT.FILL, SWT.FILL, true, false, 4, 1);
		layoutData.verticalIndent = 20;
		workingSetComposite.setLayoutData(layoutData);
		workingSetComposite.setLayout(new GridLayout(1, false));
		WorkingSetRegistry registry = WorkbenchPlugin.getDefault().getWorkingSetRegistry();
		String[] workingSetIds = Arrays.stream(registry.getNewPageWorkingSetDescriptors())
				.map(WorkingSetDescriptor::getId).toArray(String[]::new);
		IStructuredSelection wsSel = null;
		if (this.workingSets != null) {
			wsSel = new StructuredSelection(this.workingSets.toArray());
		}
		this.workingSetsGroup = new WorkingSetGroup(workingSetComposite, wsSel, workingSetIds);
	}

	private void updateProjectName() {
		// TODO update fields (including location if linked)
	}

	private void updateDirectory(File file) {
		// TODO update fields (including projectName if linked)
	}

	@Override
	public boolean isPageComplete() {
		String locationError = "";
		String projectNameError = "";
		if (directory == null || directory.getPath().isEmpty()) {
			locationError = "Please specify a directory";
		} else if (projectName == null || projectName.isEmpty()) {
			projectNameError = "Please specify project name";
		} else if (directory.isFile()) {
			locationError = "Invalid location: it is an existing file.";
		} else if (!directory.exists() && !directory.getParentFile().canWrite()) {
			locationError = "Unable to create such directory";
		} else if (directory.exists() && !directory.canWrite()) {
			locationError = "Cannot write in this directory";
		} else {
			File dotProject = new File(directory, IProjectDescription.DESCRIPTION_FILE_NAME);
			if (dotProject.exists()) {
				IProjectDescription desc = null;
				try {
					desc = ResourcesPlugin.getWorkspace()
							.loadProjectDescription(Path.fromOSString(dotProject.getAbsolutePath()));
				} catch (CoreException e) {
					projectNameError = "Invalid .project file in directory";
				}
				if (!desc.getName().equals(projectName)) {
					projectNameError = "Project name must match one in .project file: " + desc.getName();
				}
			} else {
				IProject project = null;
				try {
					project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
					if (project.exists() && (project.getLocation() == null
							|| !directory.getAbsoluteFile().equals(project.getLocation().toFile().getAbsoluteFile()))) {
						projectNameError = "Another project with same name already exists in workspace.";
					}
				} catch (IllegalArgumentException ex) {
					projectNameError = "Invalid project name";
				}
			}
		}
		// TODO set locationInfo and place ControlDecorator if needed
		// TODO set projectNameInfo and place ControlDecorator if needed
		String error = locationError + '\n' + projectNameError;
		setErrorMessage(error);
		return error.isEmpty();
	}

	@Override
	public void dispose() {
		super.dispose();
		this.linkImage.dispose();
	}
}
