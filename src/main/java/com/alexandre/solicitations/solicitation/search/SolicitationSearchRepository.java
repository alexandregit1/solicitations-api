package com.alexandre.solicitations.solicitation.search;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

interface SolicitationSearchRepository extends ElasticsearchRepository<SolicitationDocument, String> {}
