/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.drools.workbench.screens.dsltext.backend.server.indexing;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.search.Query;
import org.drools.workbench.screens.dsltext.type.DSLResourceTypeDefinition;
import org.guvnor.common.services.project.categories.Decision;
import org.junit.Test;
import org.kie.workbench.common.services.refactoring.backend.server.BaseIndexingTest;
import org.kie.workbench.common.services.refactoring.backend.server.TestIndexer;
import org.kie.workbench.common.services.refactoring.backend.server.query.builder.SingleTermQueryBuilder;
import org.kie.workbench.common.services.refactoring.model.index.terms.ModuleRootPathIndexTerm;
import org.kie.workbench.common.services.refactoring.model.index.terms.valueterms.ValueIndexTerm.TermSearchType;
import org.kie.workbench.common.services.refactoring.model.index.terms.valueterms.ValueReferenceIndexTerm;
import org.kie.workbench.common.services.refactoring.model.index.terms.valueterms.ValueResourceIndexTerm;
import org.kie.workbench.common.services.refactoring.service.ResourceType;
import org.uberfire.ext.metadata.backend.lucene.analyzer.FilenameAnalyzer;
import org.uberfire.ext.metadata.io.KObjectUtil;
import org.uberfire.java.nio.file.Path;

public class IndexDefaultPackageDslEntriesTest extends BaseIndexingTest<DSLResourceTypeDefinition> {

    @Test
    public void testDefaultPackageIndexDslEntries() throws IOException, InterruptedException {
        //Add test files
        final Path path1 = basePath.resolve("dsl1.dsl");
        final String dsl1 = loadText("dsl1.dsl");
        ioService().write(path1,
                          dsl1);

        Thread.sleep(5000); //wait for events to be consumed from jgit -> (notify changes -> watcher -> index) -> lucene index

        List<String> index = Arrays.asList(KObjectUtil.toKCluster(basePath.getFileSystem()).getClusterId());

        {
            final Query query = new SingleTermQueryBuilder(new ValueResourceIndexTerm("*",
                                                                                      ResourceType.RULE,
                                                                                      TermSearchType.WILDCARD))
                    .build();
            searchFor(index,
                      query,
                      0);
        }

        {
            final Query query = new SingleTermQueryBuilder(new ValueReferenceIndexTerm("org.drools.workbench.screens.dsltext.backend.server.indexing.classes.Applicant",
                                                                                       ResourceType.JAVA))
                    .build();
            searchFor(index,
                      query,
                      1,
                      path1);
        }
    }

    @Override
    protected TestIndexer getIndexer() {
        return new TestDslFileIndexer("");
    }

    @Override
    public Map<String, Analyzer> getAnalyzers() {
        return new HashMap<String, Analyzer>() {
            {
                put(ModuleRootPathIndexTerm.TERM,
                    new FilenameAnalyzer());
            }
        };
    }

    @Override
    protected DSLResourceTypeDefinition getResourceTypeDefinition() {
        return new DSLResourceTypeDefinition(new Decision());
    }

    @Override
    protected String getRepositoryName() {
        return this.getClass().getSimpleName();
    }
}
