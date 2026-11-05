package com.alexandre.solicitations.repository;

import com.alexandre.solicitations.model.SolicitationDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface SolicitationSearchRepository extends ElasticsearchRepository<SolicitationDocument, String> {}
