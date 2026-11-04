package com.alexandre.solicitations.solicitation.search;

import com.alexandre.solicitations.solicitation.Address;
import com.alexandre.solicitations.solicitation.Solicitation;
import java.time.Instant;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

/** Search copy of a Solicitation. Free text uses the Portuguese ("brazilian") analyzer. */
@Document(indexName = "solicitations")
@Getter
@NoArgsConstructor
public class SolicitationDocument {

    @Id
    private String id;

    @Field(type = FieldType.Keyword)
    private String status;

    @Field(type = FieldType.Keyword)
    private String serviceType;

    @Field(type = FieldType.Text, analyzer = "brazilian")
    private String title;

    @Field(type = FieldType.Text, analyzer = "brazilian")
    private String description;

    @Field(type = FieldType.Text, analyzer = "brazilian")
    private String neighborhood;

    @Field(type = FieldType.Text, analyzer = "brazilian")
    private String city;

    @Field(type = FieldType.Keyword)
    private String state;

    @Field(type = FieldType.Keyword)
    private String priority;

    @Field(type = FieldType.Date, format = DateFormat.date)
    private LocalDate preferredDate;

    @Field(type = FieldType.Double)
    private Double estimatedValue;

    @Field(type = FieldType.Date, format = DateFormat.date_time)
    private Instant createdAt;

    static SolicitationDocument from(Solicitation s) {
        SolicitationDocument doc = new SolicitationDocument();
        doc.id = s.getId().toString();
        doc.status = s.getStatus().name();
        doc.serviceType = s.getServiceType() == null ? null : s.getServiceType().name();
        doc.title = s.getTitle();
        doc.description = s.getDescription();
        Address address = s.getAddress();
        if (address != null) {
            doc.neighborhood = address.getNeighborhood();
            doc.city = address.getCity();
            doc.state = address.getState().name();
        }
        doc.priority = s.getPriority() == null ? null : s.getPriority().name();
        doc.preferredDate = s.getPreferredDate();
        doc.estimatedValue =
                s.getEstimatedValue() == null ? null : s.getEstimatedValue().doubleValue();
        doc.createdAt = s.getCreatedAt();
        return doc;
    }
}
