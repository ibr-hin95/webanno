/*
 * Copyright 2017
 * Ubiquitous Knowledge Processing (UKP) Lab and FG Language Technology
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.tudarmstadt.ukp.clarin.webanno.api.event;

import java.util.zip.ZipFile;

import org.springframework.context.ApplicationEvent;

import de.tudarmstadt.ukp.clarin.webanno.model.Project;

public class ProjectImportEvent
    extends ApplicationEvent
{
    private static final long serialVersionUID = 5604222911753768415L;

    private final ZipFile zip;
    private final de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedProject exportedProject;
    private final Project project;

    public ProjectImportEvent(Object aSource, ZipFile aZip,
            de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedProject aExportedProject,
            Project aProject)
    {
        super(aSource);
        zip = aZip;
        exportedProject = aExportedProject;
        project = aProject;
    }

    public Project getProject()
    {
        return project;
    }

    public ZipFile getZip()
    {
        return zip;
    }

    public de.tudarmstadt.ukp.clarin.webanno.export.model.ExportedProject getExportedProject()
    {
        return exportedProject;
    }
}
