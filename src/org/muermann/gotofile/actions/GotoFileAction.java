/*
    GotoFile Eclipse Plugin - Quicksearch for files in Eclipse IDE
    Copyright (C) 2004 Max Muermann

    This library is free software; you can redistribute it and/or
    modify it under the terms of the GNU Lesser General Public
    License as published by the Free Software Foundation; either
    version 2.1 of the License, or (at your option) any later version.

    This library is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
    Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public
    License along with this library; if not, write to the Free Software
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/
package org.muermann.gotofile.actions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.eclipse.core.internal.resources.File;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.PlatformUI;
import org.muermann.gotofile.GotoFileE30Plugin;
import org.muermann.gotofile.MatchComparatorFuzzy;
import org.muermann.gotofile.SearchResult;
import org.muermann.gotofile.preferences.GotoFilePreferencePage;
import org.muermann.gotofile.ui.SearchWindow;

/**
 * Our sample action implements workbench action delegate. The action proxy will
 * be created by the workbench and shown in the UI. When the user tries to use
 * the action, this delegate will be created and execution will be delegated to
 * it.
 *
 * @see IWorkbenchWindowActionDelegate
 */
@SuppressWarnings({ "restriction", "rawtypes", "unchecked" })
public class GotoFileAction implements IWorkbenchWindowActionDelegate, IPropertyChangeListener {

    private static final Pattern FALSE_PATTERN = Pattern.compile("$patternThatWillAlwaysReturnFalse^");

    IStructuredSelection selection;

    private SearchWindow dlg;
    private Pattern excludeFoldersPattern;
    private Pattern excludeFileExtensionsPattern;
    private Pattern searchPattern;
    private Map<String, SearchResult> currentlyOpenTabs;

    /**
     * We will cache window object in order to be able to provide parent shell
     * for the message dialog.
     *
     * @see IWorkbenchWindowActionDelegate#init
     */
    public void init(IWorkbenchWindow window)
    {
        GotoFileE30Plugin.getDefault().getPreferenceStore().addPropertyChangeListener( this );
    }

    /**
     * The action has been activated. The argument of the method represents the
     * 'real' action sitting in the workbench UI.
     *
     * @see IWorkbenchWindowActionDelegate#run
     */
    public void run(IAction action)
    {
        if (null == dlg)
        {
            initDlg();
        }

        // get current selectino
        ISelection sel = GotoFileE30Plugin.getActiveWorkbenchWindow().getSelectionService().getSelection();

        if (null != sel && sel instanceof TextSelection)
        {
            TextSelection ts = (TextSelection) sel;
            if (0!=ts.getLength())
                dlg.setSelection(ts.getText());
            else dlg.setSelection(null);
        }

        dlg.open();
    }

    protected void processResourcesFuzzy(IResource[] resources, List results, String searchTerm) throws CoreException {
        for (int i = 0; i < resources.length; i++) {
            if (results.size() > 100) {
                return;
            }

            if (resources[i] instanceof IContainer) {
                if (resources[i] instanceof IProject) {
                    if (!((IProject)resources[i]).isOpen()) {
                        continue;
                    }
                }

                // check for project
                if ( null != dlg.getEditorProject() && resources[i] instanceof IProject && dlg.isSearchInProject()) {
                    if (!resources[i].equals(dlg.getEditorProject())) {
                        continue;
                    }
                }

                if (filterOutByFolderPattern(resources[i])) {
                    continue;
                }

                processResourcesFuzzy(((IContainer)resources[i]).members(), results, searchTerm);
            } else if (resources[i] instanceof File) {
                IFile file = (IFile) resources[i];
                if (!file.exists()) {
                    continue;
                }
                if (filterOutByFileExtension(resources[i])) {
                    continue;
                }

                String resource = ((File)resources[i]).getProjectRelativePath().toString();
                String fileName = file.getName();
                int matchConsecutive = matchConsecutive(fileName, searchTerm); // CASE_INSENSITIVE match
                if (matchConsecutive < 0) {
                    continue;
                }
                boolean caseSensitiveMatch = searchPattern.matcher(fileName).matches(); // CASE_SENSITIVE match
                boolean isOpen = currentlyOpenTabs.containsKey(resource);
                int matchConsecutiveName = 0; // matchConsecutive(fileName, searchTerm);
                int lastMatchPos = resource.toLowerCase().lastIndexOf((searchTerm.charAt(searchTerm.length() - 1) + "").toLowerCase());

                results.add(new SearchResult(file, isOpen, caseSensitiveMatch, matchConsecutiveName, matchConsecutive, lastMatchPos));
            }
        }
    }

    private int matchConsecutive(String resourceName, String searchTerm) {
        String name = resourceName.toUpperCase();
        String term = searchTerm.toUpperCase();

        int matchConsecutive = 0;
        int charIndex = -1;

        while (term.length() != 0 && (charIndex = name.indexOf(term.charAt(0))) != -1) {
            if (charIndex == 0 && matchConsecutive > 0) {
                matchConsecutive++;
            }

            term = term.substring(1);
            name = name.substring(charIndex + 1);
        }

        if (term.length() != 0) {
            return -1;
        }
        return matchConsecutive;
    }

    private boolean filterOutByFolderPattern(IResource iResource) {
        String resource = iResource.getProjectRelativePath().toString();
        return excludeFoldersPattern.matcher(resource).find();
    }

    private boolean filterOutByFileExtension(IResource iResource) {
        String resource = iResource.getProjectRelativePath().toString();
        return excludeFileExtensionsPattern.matcher(resource).matches();
    }

    /**
     * Selection in the workbench has been changed. We can change the state of
     * the 'real' action here if we want, but this can only happen after the
     * delegate has been created.
     *
     * @see IWorkbenchWindowActionDelegate#selectionChanged
     */
    public void selectionChanged(IAction action, ISelection selection)
    {
        if ( selection instanceof IStructuredSelection)
            this.selection = (IStructuredSelection) selection;
        else
            this.selection = null;
    }

    /**
     * We can use this method to dispose of any system resources we previously
     * allocated.
     *
     * @see IWorkbenchWindowActionDelegate#dispose
     */
    public void dispose()
    {
    }

    public List runSearch(String search) {
        currentlyOpenTabs = currentlyOpenTabs();
        if (search == null || search.isEmpty() || search.startsWith(".")) {
            return sort(new ArrayList(currentlyOpenTabs.values()));
        }

        excludeFoldersPattern = excludeFoldersPattern();
        excludeFileExtensionsPattern = excludeFileExtensionsPattern();
        searchPattern = searchPattern(search);

        List results = new ArrayList();
        IWorkspaceRoot root = GotoFileE30Plugin.getWorkspace().getRoot();
        try {
            processResourcesFuzzy(root.members(), results, search);
        } catch (CoreException e1) {
            e1.printStackTrace();
        }

        return sort(results);
    }

    private List sort(List results) {
        Collections.sort(results, MatchComparatorFuzzy.getInstance());
        if (results.size() > 100)
            return results.subList(0, 100);
        return results;
    }

    private Map<String, SearchResult> currentlyOpenTabs() {
        Map<String, SearchResult> tabs = new LinkedHashMap<String, SearchResult>();
        try {
            IEditorReference[] ref = PlatformUI.getWorkbench()
                    .getActiveWorkbenchWindow().getActivePage()
                    .getEditorReferences();

            int pos = 0;
            for (IEditorReference reference : ref) {
                IFile file = (IFile) reference.getEditorInput().getAdapter(IFile.class);
                tabs.put(file.getProjectRelativePath().toString(), new SearchResult(file, pos++));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return tabs;
    }

    private Pattern excludeFoldersPattern() {
        if (excludeFoldersPattern != null) {
            return excludeFoldersPattern;
        }

        String folders = GotoFileE30Plugin.getDefault().getPreferenceStore().getString(GotoFilePreferencePage.P_FOLDERS);
        if (folders == null || folders.isEmpty()) {
            return FALSE_PATTERN;
        }
        try {
            folders = folders.replace(".", "\\.");
            String[] folder = folders.split(",");
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < folder.length; i++) {
                if (folder[i].isEmpty()) {
                    continue;
                }
                if (builder.length() != 0) {
                    builder.append("|");
                }
                builder.append("(:?^|/?)");
                // replace '*' with 'any word character'. works at start and end position (middle case not supported).
                builder.append(folder[i].replace("*", "[-+=a-zA-Z0-9_()!,.]+?"));
                builder.append("(:?$|/?)");
            }
            if (builder.length() == 0) {
                return FALSE_PATTERN;
            }
            return Pattern.compile(builder.toString());
        } catch (Exception e) {
            e.printStackTrace();
            return FALSE_PATTERN;
        }
    }

    private Pattern excludeFileExtensionsPattern() {
        if (excludeFileExtensionsPattern != null) {
            return excludeFileExtensionsPattern;
        }

        String files = GotoFileE30Plugin.getDefault().getPreferenceStore().getString(GotoFilePreferencePage.P_FILE_EXTENSIONS);
        if (files == null || files.isEmpty()) {
            return FALSE_PATTERN;
        }
        try {
            files = files.replace("*", "").replace(".", "\\.");
            String[] file = files.split(",");
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < file.length; i++) {
                if (file[i].isEmpty()) {
                    continue;
                }
                if (builder.length() != 0) {
                    builder.append("|");
                }
                // no regex supported syntax, only 'ends with extension'
                builder.append("^.*").append(file[i]).append("$");
            }
            if (builder.length() == 0) {
                return FALSE_PATTERN;
            }
            return Pattern.compile(builder.toString());
        } catch (Exception e) {
            e.printStackTrace();
            return FALSE_PATTERN;
        }
    }

    private Pattern searchPattern(String searchTerm) {
        StringBuilder builder = new StringBuilder("^");
        for (int i = 0; i < searchTerm.length(); i++) {
            builder.append(".*");
            String c = String.valueOf(searchTerm.charAt(i));
            if (c.equals(".")) {
                c = "\\.";
            }
            builder.append(c);
        }
        builder.append(".*$");
        try {
            return Pattern.compile(builder.toString());
        } catch (Exception e) {
            e.printStackTrace();
            return FALSE_PATTERN;
        }
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.util.IPropertyChangeListener#propertyChange(org.eclipse.jface.util.PropertyChangeEvent)
     */
    public void propertyChange(PropertyChangeEvent event)
    {
        // rebuild the dialog
        initDlg();
    }

    public void initDlg()
    {
        this.dlg = new SearchWindow(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), 7, this);
        dlg.setAction(this);
    }

}