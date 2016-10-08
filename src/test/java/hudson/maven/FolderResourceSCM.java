/*
 * Copyright Olivier Lamy
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package hudson.maven;

import hudson.FilePath;
import hudson.Launcher;
import hudson.Util;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.scm.NullSCM;
import hudson.scm.SCMDescriptor;
import org.apache.commons.io.FileUtils;
import org.jvnet.hudson.test.ExtractChangeLogParser;
import org.jvnet.hudson.test.ExtractResourceWithChangesSCM;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;

/**
 * Simple scm which copy a project to the workspace (avoid unzip)
 */
public class FolderResourceSCM extends NullSCM
{


    private final String directory;

    private List<String> changelogFileNames;

    public FolderResourceSCM( String directory ) {
        if (!new File( directory ).isDirectory()){
            throw new IllegalArgumentException( directory + " must be an existing directory" );
        }
        this.directory = directory;
    }

    public boolean checkout( AbstractBuild<?, ?> build, Launcher launcher, FilePath workspace, BuildListener listener, File changeLogFile)
        throws IOException, InterruptedException {
        if(workspace.exists()) {
            listener.getLogger().println("Deleting existing workspace " + workspace.getRemote());
            workspace.deleteRecursive();
        }
        listener.getLogger().println("Staging " + this.directory);

        FileUtils.copyDirectory( new File( this.directory), //
                                 new File( workspace.getRemote()));

        ExtractChangeLogParser.ExtractChangeLogEntry changeLog = new ExtractChangeLogParser.ExtractChangeLogEntry();

        for (String fileName:this.changelogFileNames) {
            changeLog.addFile(new ExtractChangeLogParser.FileInZip(fileName));
        }

        saveToChangeLog( changeLogFile, changeLog );

        return true;
    }

    public FolderResourceSCM addChangelog(List<String> fileNames){
        this.changelogFileNames = fileNames;
        return this;
    }

    public void saveToChangeLog(File changeLogFile, ExtractChangeLogParser.ExtractChangeLogEntry changeLog) throws IOException {
        FileOutputStream outputStream = new FileOutputStream( changeLogFile);

        PrintStream stream = new PrintStream( outputStream, false, "UTF-8");

        stream.println("<?xml version='1.0' encoding='UTF-8'?>");
        stream.println("<extractChanges>");
        stream.println("<entry>");
        stream.println("<zipFile>" + escapeForXml(changeLog.getZipFile()) + "</zipFile>");

        for (String fileName : changeLog.getAffectedPaths()) {
            stream.println("<file>");
            stream.println("<fileName>" + escapeForXml(fileName) + "</fileName>");
            stream.println("</file>");
        }

        stream.println("</entry>");
        stream.println("</extractChanges>");

        stream.close();
    }

    private static String escapeForXml(String string) {
        return Util.xmlEscape( Util.fixNull( string));
    }

    @Override public SCMDescriptor<?> getDescriptor() {
        return new SCMDescriptor<FolderResourceSCM>( FolderResourceSCM.class, null) {
            @Override
            public String getDisplayName() {
                return "FolderResourceSCM";
            }
        };
    }
}
