/*
 * Copyright 2012 JBoss Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.drools.workbench.screens.guided.dtable.backend.server;

import java.util.Date;
import java.util.List;
import java.util.Set;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.inject.Named;

import org.drools.workbench.models.commons.shared.workitems.PortableWorkDefinition;
import org.drools.workbench.models.guided.dtable.backend.GuidedDTXMLPersistence;
import org.drools.workbench.models.guided.dtable.shared.model.GuidedDecisionTable52;
import org.drools.workbench.screens.guided.dtable.model.GuidedDecisionTableEditorContent;
import org.drools.workbench.screens.guided.dtable.service.GuidedDecisionTableEditorService;
import org.drools.workbench.screens.guided.dtable.type.GuidedDTableResourceTypeDefinition;
import org.drools.workbench.screens.workitems.service.WorkItemsEditorService;
import org.guvnor.common.services.backend.exceptions.ExceptionUtilities;
import org.guvnor.common.services.project.builder.events.InvalidateDMOProjectCacheEvent;
import org.guvnor.common.services.project.model.Package;
import org.guvnor.common.services.project.service.ProjectService;
import org.guvnor.common.services.shared.builder.BuildMessage;
import org.guvnor.common.services.shared.file.CopyService;
import org.guvnor.common.services.shared.file.DeleteService;
import org.guvnor.common.services.shared.file.RenameService;
import org.guvnor.common.services.shared.metadata.MetadataService;
import org.guvnor.common.services.shared.metadata.model.Metadata;
import org.jboss.errai.bus.server.annotations.Service;
import org.kie.commons.io.IOService;
import org.kie.commons.java.nio.base.options.CommentedOption;
import org.kie.workbench.common.services.backend.source.SourceServices;
import org.kie.workbench.common.services.datamodel.oracle.PackageDataModelOracle;
import org.kie.workbench.common.services.datamodel.service.DataModelService;
import org.uberfire.backend.server.util.Paths;
import org.uberfire.backend.vfs.Path;
import org.uberfire.security.Identity;
import org.uberfire.workbench.events.ResourceAddedEvent;
import org.uberfire.workbench.events.ResourceOpenedEvent;
import org.uberfire.workbench.events.ResourceUpdatedEvent;

@Service
@ApplicationScoped
public class GuidedDecisionTableEditorServiceImpl implements GuidedDecisionTableEditorService {

    @Inject
    @Named("ioStrategy")
    private IOService ioService;

    @Inject
    private MetadataService metadataService;

    @Inject
    private CopyService copyService;

    @Inject
    private DeleteService deleteService;

    @Inject
    private RenameService renameService;

    @Inject
    private Event<InvalidateDMOProjectCacheEvent> invalidateDMOProjectCache;

    @Inject
    private Event<ResourceOpenedEvent> resourceOpenedEvent;

    @Inject
    private Event<ResourceAddedEvent> resourceAddedEvent;

    @Inject
    private Event<ResourceUpdatedEvent> resourceUpdatedEvent;

    @Inject
    private Paths paths;

    @Inject
    private Identity identity;

    @Inject
    private DataModelService dataModelService;

    @Inject
    private SourceServices sourceServices;

    @Inject
    private WorkItemsEditorService workItemsService;

    @Inject
    private ProjectService projectService;

    @Inject
    private GuidedDTableResourceTypeDefinition resourceTypeDefinition;

    @Override
    public Path create( final Path context,
                        final String fileName,
                        final GuidedDecisionTable52 content,
                        final String comment ) {
        try {
            final Package pkg = projectService.resolvePackage( context );
            final String packageName = ( pkg == null ? null : pkg.getPackageName() );
            content.setPackageName( packageName );

            final org.kie.commons.java.nio.file.Path nioPath = paths.convert( context ).resolve( fileName );
            final Path newPath = paths.convert( nioPath,
                                                false );

            ioService.createFile( nioPath );
            ioService.write( nioPath,
                             GuidedDTXMLPersistence.getInstance().marshal( content ),
                             makeCommentedOption( comment ) );

            //Signal creation to interested parties
            resourceAddedEvent.fire( new ResourceAddedEvent( newPath ) );

            return newPath;

        } catch ( Exception e ) {
            throw ExceptionUtilities.handleException( e );
        }
    }

    @Override
    public GuidedDecisionTable52 load( final Path path ) {
        try {
            final String content = ioService.readAllString( paths.convert( path ) );

            //Signal opening to interested parties
            resourceOpenedEvent.fire( new ResourceOpenedEvent( path ) );

            return GuidedDTXMLPersistence.getInstance().unmarshal( content );

        } catch ( Exception e ) {
            throw ExceptionUtilities.handleException( e );
        }
    }

    @Override
    public GuidedDecisionTableEditorContent loadContent( final Path path ) {
        try {
            final GuidedDecisionTable52 model = load( path );
            final PackageDataModelOracle oracle = dataModelService.getDataModel( path );
            final Set<PortableWorkDefinition> workItemDefinitions = workItemsService.loadWorkItemDefinitions( path );
            return new GuidedDecisionTableEditorContent( oracle,
                                                         model,
                                                         workItemDefinitions );

        } catch ( Exception e ) {
            throw ExceptionUtilities.handleException( e );
        }
    }

    @Override
    public Path save( final Path resource,
                      final GuidedDecisionTable52 model,
                      final Metadata metadata,
                      final String comment ) {
        try {
            final Package pkg = projectService.resolvePackage( resource );
            final String packageName = ( pkg == null ? null : pkg.getPackageName() );
            model.setPackageName( packageName );

            ioService.write( paths.convert( resource ),
                             GuidedDTXMLPersistence.getInstance().marshal( model ),
                             metadataService.setUpAttributes( resource,
                                                              metadata ),
                             makeCommentedOption( comment ) );

            //Signal update to interested parties
            resourceUpdatedEvent.fire( new ResourceUpdatedEvent( resource ) );

            return resource;

        } catch ( Exception e ) {
            throw ExceptionUtilities.handleException( e );
        }
    }

    @Override
    public void delete( final Path path,
                        final String comment ) {
        try {
            deleteService.delete( path,
                                  comment );

        } catch ( Exception e ) {
            throw ExceptionUtilities.handleException( e );
        }
    }

    @Override
    public Path rename( final Path path,
                        final String newName,
                        final String comment ) {
        try {
            return renameService.rename( path,
                                         newName,
                                         comment );

        } catch ( Exception e ) {
            throw ExceptionUtilities.handleException( e );
        }
    }

    @Override
    public Path copy( final Path path,
                      final String newName,
                      final String comment ) {
        try {
            return copyService.copy( path,
                                     newName,
                                     comment );

        } catch ( Exception e ) {
            throw ExceptionUtilities.handleException( e );
        }
    }

    @Override
    public String toSource( final Path path,
                            final GuidedDecisionTable52 model ) {
        try {
            return sourceServices.getServiceFor( paths.convert( path ) ).getSource( paths.convert( path ),
                                                                                    model );

        } catch ( Exception e ) {
            throw ExceptionUtilities.handleException( e );
        }
    }

    @Override
    public boolean accepts( final Path path ) {
        return resourceTypeDefinition.accept( path );
    }

    @Override
    public List<BuildMessage> validate( final Path path ) {
        final GuidedDecisionTable52 content = load( path );
        return validate( path,
                         content );
    }

    @Override
    public List<BuildMessage> validate( final Path path,
                                        final GuidedDecisionTable52 content ) {
        //TODO {manstis} - Need to implement
        return null;
    }

    private CommentedOption makeCommentedOption( final String commitMessage ) {
        final String name = identity.getName();
        final Date when = new Date();
        final CommentedOption co = new CommentedOption( name,
                                                        null,
                                                        commitMessage,
                                                        when );
        return co;
    }

}
