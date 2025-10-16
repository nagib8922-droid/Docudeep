package com.example.docudeep.api.mapper;


import com.example.docudeep.Applicant;
import com.example.docudeep.Application;
import com.example.docudeep.api.dto.ApplicationCreate;
import com.example.docudeep.api.dto.ApplicationDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;


@Mapper(componentModel = "spring")
public interface ApplicationMapper {
    @Mapping(target = "applicant", source = "applicant")
    @Mapping(target = "createdAt", expression = "java(java.time.OffsetDateTime.now())")
    @Mapping(target = "status", constant = "PENDING")
    Application toEntity(ApplicationCreate dto, Applicant applicant);


    @Mapping(target = "applicantId", source = "applicant.id")
    ApplicationDTO toDTO(Application app);
}
