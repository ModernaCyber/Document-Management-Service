/*-
 * #%L
 * Alfresco Remote API
 * %%
 * Copyright (C) 2005 - 2016 Alfresco Software Limited
 * %%
 * This file is part of the Alfresco software. 
 * If the software was purchased under a paid Alfresco license, the terms of 
 * the paid license agreement will prevail.  Otherwise, the software is 
 * provided under the following open source license terms:
 * 
 * Alfresco is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Alfresco is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with Alfresco. If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */
package org.alfresco.rest.api.search;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertNull;
import static junit.framework.TestCase.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.notNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.alfresco.repo.search.EmptyResultSet;
import org.alfresco.repo.search.impl.lucene.SolrJSONResultSet;
import org.alfresco.repo.search.impl.solr.facet.facetsresponse.GenericBucket;
import org.alfresco.repo.search.impl.solr.facet.facetsresponse.GenericFacetResponse;
import org.alfresco.repo.search.impl.solr.facet.facetsresponse.GenericFacetResponse.FACET_TYPE;
import org.alfresco.repo.search.impl.solr.facet.facetsresponse.Metric.METRIC_TYPE;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.version.Version2Model;
import org.alfresco.repo.version.common.VersionImpl;
import org.alfresco.rest.api.DeletedNodes;
import org.alfresco.rest.api.impl.NodesImpl;
import org.alfresco.rest.api.lookups.PersonPropertyLookup;
import org.alfresco.rest.api.lookups.PropertyLookupRegistry;
import org.alfresco.rest.api.model.Node;
import org.alfresco.rest.api.model.UserInfo;
import org.alfresco.rest.api.nodes.NodeVersionsRelation;
import org.alfresco.rest.api.search.context.FacetFieldContext;
import org.alfresco.rest.api.search.context.FacetQueryContext;
import org.alfresco.rest.api.search.context.SearchContext;
import org.alfresco.rest.api.search.context.SearchRequestContext;
import org.alfresco.rest.api.search.context.SpellCheckContext;
import org.alfresco.rest.api.search.impl.ResultMapper;
import org.alfresco.rest.api.search.impl.SearchMapper;
import org.alfresco.rest.api.search.impl.StoreMapper;
import org.alfresco.rest.api.search.model.HighlightEntry;
import org.alfresco.rest.api.search.model.SearchQuery;
import org.alfresco.rest.framework.core.exceptions.EntityNotFoundException;
import org.alfresco.rest.framework.resource.parameters.CollectionWithPagingInfo;
import org.alfresco.rest.framework.resource.parameters.Params;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.FieldHighlightParameters;
import org.alfresco.service.cmr.search.GeneralHighlightParameters;
import org.alfresco.service.cmr.search.LimitBy;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchParameters;
import org.alfresco.service.cmr.version.Version;
import org.alfresco.service.cmr.version.VersionHistory;
import org.alfresco.service.cmr.version.VersionService;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.GUID;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.extensions.webscripts.WebScriptRequest;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Tests the ResultMapper class
 *
 * @author Gethin James
 */
public class ResultMapperTests
{
    static ResultMapper mapper;
    static SearchMapper searchMapper = new SearchMapper();
    public static final String JSON_REPONSE = "{\"responseHeader\":{\"status\":0,\"QTime\":9},\"_original_parameters_\":\"org.apache.solr.common.params.DefaultSolrParams:{params(df=TEXT&alternativeDic=DEFAULT_DICTIONARY&fl=DBID,score&start=0&fq={!afts}AUTHORITY_FILTER_FROM_JSON&fq={!afts}TENANT_FILTER_FROM_JSON&rows=1000&locale=en_US&wt=json),defaults(carrot.url=id&spellcheck.collateExtendedResults=true&carrot.produceSummary=true&spellcheck.maxCollations=3&spellcheck.maxCollationTries=5&spellcheck.alternativeTermCount=2&spellcheck.extendedResults=false&defType=afts&spellcheck.maxResultsForSuggest=5&spellcheck=false&carrot.outputSubClusters=false&spellcheck.count=5&carrot.title=mltext@m___t@{http://www.alfresco.org/model/content/1.0}title&carrot.snippet=content@s___t@{http://www.alfresco.org/model/content/1.0}content&spellcheck.collate=true)}\",\"_field_mappings_\":{},\"_date_mappings_\":{},\"_range_mappings_\":{},\"_pivot_mappings_\":{},\"_interval_mappings_\":{},\"_stats_field_mappings_\":{},\"_stats_facet_mappings_\":{},\"_facet_function_mappings_\":{},\"response\":{\"numFound\":6,\"start\":0,\"maxScore\":0.7849362,\"docs\":[{\"DBID\":565,\"score\":0.7849362},{\"DBID\":566,\"score\":0.7849362},{\"DBID\":521,\"score\":0.3540957},{\"DBID\":514,\"score\":0.33025497},{\"DBID\":420,\"score\":0.32440513},{\"DBID\":415,\"score\":0.2780319}]},"
                + "\"facet_counts\":{\"facet_queries\":{\"small\":0,\"large\":0,\"xtra small\":3,\"xtra large\":0,\"medium\":8,\"XX large\":0},"
                + "\"facet_fields\":{\"content.size\":[\"Big\",8,\"Brown\",3,\"Fox\",5,\"Jumped\",2,\"somewhere\",3]},"
                +"\"facet_dates\":{},"
                +"\"facet_ranges\":{},"
                +"\"facet_pivot\":{\"creator,modifier\":[{\"field\":\"creator\",\"count\":7,\"pivot\":[{\"field\":\"modifier\",\"count\":3,\"value\":\"mjackson\"},{\"field\":\"modifier\",\"count\":4,\"value\":\"admin\"}],\"value\":\"mjackson\"}]},"
                +"\"facet_intervals\":{\"creator\":{\"last\":4,\"first\":0},\"datetime@sd@{http://www.alfresco.org/model/content/1.0}created\":{\"earlier\":5,\"lastYear\":0,\"currentYear\":0}}"
                + "},"
                + "\"spellcheck\":{\"searchInsteadFor\":\"alfresco\"},"
                + "\"highlighting\": {"
                + "  \"_DEFAULT_!800001579e3d1964!800001579e3d1969\": {\"name\": [\"some very <al>long<fresco> name\"],\"title\": [\"title1 is very <al>long<fresco>\"], \"DBID\": \"521\"},"
                + " \"_DEFAULT_!800001579e3d1964!800001579e3d196a\": {\"name\": [\"this is some <al>long<fresco> text.  It\", \" has the word <al>long<fresco> in many places\", \".  In fact, it has <al>long<fresco> on some\", \" happens to <al>long<fresco> in this case.\"], \"DBID\": \"1475846153692\"}"
                + "},"
                + "\"processedDenies\":true, \"lastIndexedTx\":34}";
    public static final Params EMPTY_PARAMS = Params.valueOf((String)null,(String)null,(WebScriptRequest) null);
    public static final String FROZEN_ID = "frozen";
    public static final String FROZEN_VER = "1.1";
    private static final long VERSIONED_ID = 521l;

    private static SerializerTestHelper helper;

    @BeforeClass
    public static void setupTests() throws Exception
    {
        Map<String, UserInfo> mapUserInfo = new HashMap<>();
        mapUserInfo.put(AuthenticationUtil.getSystemUserName(), new UserInfo(AuthenticationUtil.getSystemUserName(), "sys", "sys"));
        Map<QName, Serializable> nodeProps = new HashMap<>();

        NodesImpl nodes = mock(NodesImpl.class);
        ServiceRegistry sr = mock(ServiceRegistry.class);
        DeletedNodes deletedNodes = mock(DeletedNodes.class);
        nodes.setServiceRegistry(sr);
        VersionService versionService = mock(VersionService.class);
        VersionHistory versionHistory = mock(VersionHistory.class);

        Map<String, Serializable> versionProperties = new HashMap<>();
        versionProperties.put(Version.PROP_DESCRIPTION, "ver desc");
        versionProperties.put(Version2Model.PROP_VERSION_TYPE, "v type");
        when(versionHistory.getVersion(anyString())).thenAnswer(invocation ->
        {
            return new VersionImpl(versionProperties,new NodeRef(StoreMapper.STORE_REF_VERSION2_SPACESSTORE, GUID.generate()));
        });
        NodeService nodeService = mock(NodeService.class);

        when(versionService.getVersionHistory(notNull(NodeRef.class))).thenAnswer(invocation ->
        {
            Object[] args = invocation.getArguments();
            NodeRef aNode = (NodeRef)args[0];
            return versionHistory;
        });

        when(nodeService.getProperties(notNull(NodeRef.class))).thenAnswer(invocation ->
        {
            Object[] args = invocation.getArguments();
            NodeRef aNode = (NodeRef)args[0];
            if (StoreMapper.STORE_REF_VERSION2_SPACESSTORE.equals(aNode.getStoreRef()))
            {
                nodeProps.put(Version2Model.PROP_QNAME_FROZEN_NODE_REF, new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, FROZEN_ID+aNode.getId()));
                nodeProps.put(Version2Model.PROP_QNAME_VERSION_LABEL, FROZEN_VER);
            }
            return nodeProps;
        });

        when(sr.getVersionService()).thenReturn(versionService);
        when(sr.getNodeService()).thenReturn(nodeService);

        when(nodes.validateOrLookupNode(notNull(String.class), anyString())).thenAnswer(invocation ->
        {
            Object[] args = invocation.getArguments();
            String aNode = (String)args[0];
            if (aNode.endsWith(""+VERSIONED_ID))
            {
                throw new EntityNotFoundException(""+VERSIONED_ID);
            }
            else
            {
                return new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, aNode);
            }
        });

        //        // NodeRef nodeRef = nodes.validateOrLookupNode(nodeId, null);
        when(nodes.getFolderOrDocument(notNull(NodeRef.class), any(), any(), any(), any())).thenAnswer(new Answer<Node>() {
            @Override
            public Node answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                NodeRef aNode = (NodeRef)args[0];
                if (StoreRef.STORE_REF_ARCHIVE_SPACESSTORE.equals(aNode.getStoreRef()))
                {
                    //Return NULL if its from the archive store.
                    return null;
                }
                return new Node(aNode, (NodeRef)args[1], nodeProps, mapUserInfo, sr);
            }
        });

        when(deletedNodes.getDeletedNode(notNull(String.class), any(), anyBoolean(), any())).thenAnswer(new Answer<Node>() {
            @Override
            public Node answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                String nodeId = (String)args[0];
                if (FROZEN_ID.equals(nodeId)) throw new EntityNotFoundException(nodeId);
                NodeRef aNode = new NodeRef(StoreRef.STORE_REF_ARCHIVE_SPACESSTORE, nodeId);
                return new Node(aNode, new NodeRef(StoreRef.STORE_REF_ARCHIVE_SPACESSTORE,"unknown"), nodeProps, mapUserInfo, sr);
            }
        });

        PersonPropertyLookup propertyLookups = mock(PersonPropertyLookup.class);
        when(propertyLookups.supports()).thenReturn(Stream.of("creator","modifier").collect(Collectors.toSet()));
        when(propertyLookups.lookup(notNull(String.class))).thenAnswer(invocation -> {
            Object[] args = invocation.getArguments();
            String value = (String)args[0];
            if ("mjackson".equals(value)) return "Michael Jackson";
            return null;
        });
        PropertyLookupRegistry propertyLookupRegistry = new PropertyLookupRegistry();
        propertyLookupRegistry.setLookups(Arrays.asList(propertyLookups));
        mapper = new ResultMapper();
        mapper.setNodes(nodes);
        mapper.setStoreMapper(new StoreMapper());
        mapper.setPropertyLookup(propertyLookupRegistry);
        mapper.setDeletedNodes(deletedNodes);
        mapper.setServiceRegistry(sr);
        NodeVersionsRelation nodeVersionsRelation = new NodeVersionsRelation();
        nodeVersionsRelation.setNodes(nodes);
        nodeVersionsRelation.setServiceRegistry(sr);
        nodeVersionsRelation.afterPropertiesSet();
        mapper.setNodeVersions(nodeVersionsRelation);

        helper = new SerializerTestHelper();
        searchMapper.setStoreMapper(new StoreMapper());
    }

    @Test
    public void testNoResults() throws Exception
    {
        CollectionWithPagingInfo<Node> collection =  mapper.toCollectionWithPagingInfo(EMPTY_PARAMS, null, null, new EmptyResultSet());
        assertNotNull(collection);
        assertFalse(collection.hasMoreItems());
        assertTrue(collection.getTotalItems() < 1);
        assertNull(collection.getContext());
    }

    @Test
    public void testToCollectionWithPagingInfo() throws Exception
    {
        ResultSet results = mockResultset(Arrays.asList(514l), Arrays.asList(566l, VERSIONED_ID));
        SearchRequestContext searchRequest = SearchRequestContext.from(SearchQuery.EMPTY);
        CollectionWithPagingInfo<Node> collectionWithPage =  mapper.toCollectionWithPagingInfo(EMPTY_PARAMS, searchRequest, SearchQuery.EMPTY, results);
        assertNotNull(collectionWithPage);
        Long found = results.getNumberFound();
        assertEquals(found.intValue(), collectionWithPage.getTotalItems().intValue());
        Node firstNode = collectionWithPage.getCollection().stream().findFirst().get();
        assertNotNull(firstNode.getSearch().getScore());
        assertEquals(StoreMapper.LIVE_NODES, firstNode.getLocation());
        collectionWithPage.getCollection().stream().forEach(aNode -> {
            List<HighlightEntry> high = aNode.getSearch().getHighlight();
            if (high != null)
            {
                assertEquals(2, high.size());
                HighlightEntry first = high.get(0);
                assertNotNull(first.getField());
                assertNotNull(first.getSnippets());
            }
        });
        //1 deleted node in the test data
        assertEquals(1l, collectionWithPage.getCollection().stream().filter(node -> StoreMapper.DELETED.equals(node.getLocation())).count());

        //1 version nodes in the test data (and 1 is not shown because it is in the archive store)
        assertEquals(1l, collectionWithPage.getCollection().stream().filter(node -> StoreMapper.VERSIONS.equals(node.getLocation())).count());
    }

    @Test
    public void testToSearchContext() throws Exception
    {
        ResultSet results = mockResultset(Collections.emptyList(),Collections.emptyList());
        SearchQuery searchQuery = helper.searchQueryFromJson();
        SearchRequestContext searchRequest = SearchRequestContext.from(searchQuery);
        SearchParameters searchParams = searchMapper.toSearchParameters(EMPTY_PARAMS, searchQuery, searchRequest);
        SearchContext searchContext = mapper.toSearchContext((SolrJSONResultSet) results, searchRequest, searchQuery, 0);
        assertEquals(34l, searchContext.getConsistency().getlastTxId());
        assertEquals(6, searchContext.getFacetQueries().size());
        assertEquals(0,searchContext.getFacetQueries().get(0).getCount());
        assertEquals("cm:created:bob",searchContext.getFacetQueries().get(0).getFilterQuery());
        assertEquals("small",searchContext.getFacetQueries().get(0).getLabel());
        assertEquals("searchInsteadFor",searchContext.getSpellCheck().getType());
        assertEquals(1,searchContext.getSpellCheck().getSuggestions().size());
        assertEquals("alfresco",searchContext.getSpellCheck().getSuggestions().get(0));
        assertEquals(1, searchContext.getFacetsFields().size());
        assertEquals("content.size",searchContext.getFacetsFields().get(0).getLabel());

        //Facet intervals
        List<GenericFacetResponse> intervalFacets = searchContext.getFacets().stream()
                    .filter(f -> f.getType().equals(FACET_TYPE.interval)).collect(Collectors.toList());
        assertEquals(2, intervalFacets.size());
        assertEquals("creator",intervalFacets.get(0).getLabel());
        assertEquals("last",intervalFacets.get(0).getBuckets().get(0).getLabel());
        assertEquals("cm:creator:(a,b]",intervalFacets.get(0).getBuckets().get(0).getFilterQuery());
        assertEquals(METRIC_TYPE.count,intervalFacets.get(0).getBuckets().get(0).getMetrics().get(0).getType());
        assertEquals(4,intervalFacets.get(0).getBuckets().get(0).getMetrics().get(0).getValue().get("count"));

        //Requests search Query
        assertNotNull(searchContext.getRequest());
        assertEquals("great", searchContext.getRequest().getQuery().getUserQuery());

        //Pivot
        assertEquals(3, searchContext.getFacets().size());
        GenericFacetResponse pivotFacet = searchContext.getFacets().get(2);
        assertEquals(FACET_TYPE.pivot,pivotFacet.getType());
        assertEquals("creator",pivotFacet.getLabel());
        assertEquals(1, pivotFacet.getBuckets().size());
        GenericBucket pivotBucket = pivotFacet.getBuckets().get(0);
        assertEquals("mjackson",pivotBucket.getLabel());
        assertEquals("creator:mjackson",pivotBucket.getFilterQuery());
        assertEquals("{count=7}",pivotBucket.getMetrics().get(0).getValue().toString());
        assertEquals(1,pivotBucket.getFacets().size());
        GenericFacetResponse nestedFacet = pivotBucket.getFacets().get(0);
        assertEquals(FACET_TYPE.pivot,nestedFacet.getType());
        assertEquals("mylabel",nestedFacet.getLabel());
        assertEquals(2,nestedFacet.getBuckets().size());
        GenericBucket nestedBucket = nestedFacet.getBuckets().get(0);
        assertEquals("mjackson",nestedBucket.getLabel());
        assertEquals("modifier:mjackson",nestedBucket.getFilterQuery());
        assertEquals("{count=3}",nestedBucket.getMetrics().get(0).getValue().toString());
        GenericBucket nestedBucket2 = nestedFacet.getBuckets().get(1);
        assertEquals("admin",nestedBucket2.getLabel());
        assertEquals("modifier:admin",nestedBucket2.getFilterQuery());
        assertEquals("{count=4}",nestedBucket2.getMetrics().get(0).getValue().toString());
    }

    @Test
    public void testIsNullContext() throws Exception
    {
        assertTrue(mapper.isNullContext(new SearchContext(0l,null,null,null,null, null)));
        assertFalse(mapper.isNullContext(new SearchContext(1l,null,null,null,null, null)));
        assertFalse(mapper.isNullContext(new SearchContext(0l,null,null,null,new SpellCheckContext(null, null), null)));
        assertFalse(mapper.isNullContext(new SearchContext(0l,null, Arrays.asList(new FacetQueryContext(null, null, 0)),null,null, null)));
        assertFalse(mapper.isNullContext(new SearchContext(0l,null,null,Arrays.asList(new FacetFieldContext(null, null)),null, null)));
        assertFalse(mapper.isNullContext(new SearchContext(0l,Arrays.asList(new GenericFacetResponse(null,null, null)),null,null, null, null)));
    }

    @Test
    public void testHighlight() throws Exception
    {
        SearchParameters sp = new SearchParameters();
        sp.setBulkFetchEnabled(false);
        GeneralHighlightParameters highlightParameters = new GeneralHighlightParameters(null,null,null,null,null,null,null,null);
        sp.setHighlight(highlightParameters);
        assertNull(sp.getHighlight().getMergeContiguous());
        assertNull(sp.getHighlight().getFields());

        List<FieldHighlightParameters> fields = new ArrayList<>(2);
        fields.add(new FieldHighlightParameters(null, null, null, null, null,null));
        fields.add(new FieldHighlightParameters("myfield", null, null, null, "(",")"));
        highlightParameters = new GeneralHighlightParameters(1,2,null,null,null,50,true,fields);
        sp.setHighlight(highlightParameters);
        assertEquals(2,sp.getHighlight().getFields().size());
        assertEquals(true,sp.getHighlight().getUsePhraseHighlighter().booleanValue());
        assertEquals(1,sp.getHighlight().getSnippetCount().intValue());
        assertEquals(50,sp.getHighlight().getMaxAnalyzedChars().intValue());
        assertEquals(2,sp.getHighlight().getFragmentSize().intValue());
        assertEquals("myfield",sp.getHighlight().getFields().get(1).getField());
        assertEquals("(",sp.getHighlight().getFields().get(1).getPrefix());
        assertEquals(")",sp.getHighlight().getFields().get(1).getPostfix());
    }
    
    @Test
    /**
     * Test facet group with out facet fields
     * @throws Exception
     */
    public void testFacetingGroupResponse() throws Exception
    {
        String jsonQuery = "{\"query\": {\"query\": \"alfresco\"},"
                    + "\"facetQueries\": [" 
                    + "{\"query\": \"content.size:[o TO 102400]\", \"label\": \"small\",\"group\":\"foo\"},"
                    + "{\"query\": \"content.size:[102400 TO 1048576]\", \"label\": \"medium\",\"group\":\"foo\"}," 
                    + "{\"query\": \"content.size:[1048576 TO 16777216]\", \"label\": \"large\",\"group\":\"foo\"}]"
                    + "}";
        
        String expectedResponse = "{\"responseHeader\":{\"status\":0,\"QTime\":9},\"_original_parameters_\":\"org.apache.solr.common.params.DefaultSolrParams:{params(df=TEXT&alternativeDic=DEFAULT_DICTIONARY&fl=DBID,score&start=0&fq={!afts}AUTHORITY_FILTER_FROM_JSON&fq={!afts}TENANT_FILTER_FROM_JSON&rows=1000&locale=en_US&wt=json),defaults(carrot.url=id&spellcheck.collateExtendedResults=true&carrot.produceSummary=true&spellcheck.maxCollations=3&spellcheck.maxCollationTries=5&spellcheck.alternativeTermCount=2&spellcheck.extendedResults=false&defType=afts&spellcheck.maxResultsForSuggest=5&spellcheck=false&carrot.outputSubClusters=false&spellcheck.count=5&carrot.title=mltext@m___t@{http://www.alfresco.org/model/content/1.0}title&carrot.snippet=content@s___t@{http://www.alfresco.org/model/content/1.0}content&spellcheck.collate=true)}\",\"_field_mappings_\":{},\"_date_mappings_\":{},\"_range_mappings_\":{},\"_pivot_mappings_\":{},\"_interval_mappings_\":{},\"_stats_field_mappings_\":{},\"_stats_facet_mappings_\":{},\"_facet_function_mappings_\":{},\"response\":{\"numFound\":6,\"start\":0,\"maxScore\":0.7849362,\"docs\":[{\"DBID\":565,\"score\":0.7849362},{\"DBID\":566,\"score\":0.7849362},{\"DBID\":521,\"score\":0.3540957},{\"DBID\":514,\"score\":0.33025497},{\"DBID\":420,\"score\":0.32440513},{\"DBID\":415,\"score\":0.2780319}]},"
                        + "\"spellcheck\":{\"searchInsteadFor\":\"alfresco\"},"
                        + "\"facet_counts\":{\"facet_queries\": {\"small\": 52,\"large\": 0,\"medium\": 0}},"
                        + "\"processedDenies\":true, \"lastIndexedTx\":34}";

        ResultSet results = mockResultset(expectedResponse);
        SearchQuery searchQuery = helper.extractFromJson(jsonQuery);
        SearchRequestContext searchRequest = SearchRequestContext.from(searchQuery);
        SearchContext searchContext = mapper.toSearchContext((SolrJSONResultSet) results, searchRequest, searchQuery, 0);
        assertEquals(34l, searchContext.getConsistency().getlastTxId());
        assertEquals(null, searchContext.getFacetQueries());
        assertEquals(1, searchContext.getFacetsFields().size());
        assertEquals(3,searchContext.getFacetsFields().get(0).getBuckets().size());
        assertEquals("small",searchContext.getFacetsFields().get(0).getBuckets().get(0).getLabel());
        assertEquals("content.size:[o TO 102400]",searchContext.getFacetsFields().get(0).getBuckets().get(0).getFilterQuery());
        assertNotNull(searchContext.getFacetsFields().get(0).getBuckets().get(0).getCount());
    }
    private ResultSet mockResultset(String json) throws Exception
    {
        NodeService nodeService = mock(NodeService.class);
        JSONObject jsonObj = new JSONObject(new JSONTokener(json));
        SearchParameters sp = new SearchParameters();
        sp.setBulkFetchEnabled(false);
        ResultSet results = new SolrJSONResultSet(jsonObj,
                                                  sp,
                                                  nodeService,
                                                  null,
                                                  LimitBy.FINAL_SIZE,
                                                  10);
        return results;
    }

    private ResultSet mockResultset(List<Long> archivedNodes, List<Long> versionNodes) throws JSONException
    {

        NodeService nodeService = mock(NodeService.class);
        when(nodeService.getNodeRef(any())).thenAnswer(new Answer<NodeRef>() {
            @Override
            public NodeRef answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                //If the DBID is in the list archivedNodes, instead of returning a noderef return achivestore noderef
                if (archivedNodes.contains(args[0])) return new NodeRef(StoreRef.STORE_REF_ARCHIVE_SPACESSTORE, GUID.generate());
                if (versionNodes.contains(args[0])) return new NodeRef(StoreMapper.STORE_REF_VERSION2_SPACESSTORE, GUID.generate()+args[0]);
                return new NodeRef(StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, GUID.generate());
            }
        });

        SearchParameters sp = new SearchParameters();
        sp.setBulkFetchEnabled(false);
        JSONObject json = new JSONObject(new JSONTokener(JSON_REPONSE));
        ResultSet results = new SolrJSONResultSet(json,sp,nodeService, null, LimitBy.FINAL_SIZE, 10);
        return results;
    }

}
